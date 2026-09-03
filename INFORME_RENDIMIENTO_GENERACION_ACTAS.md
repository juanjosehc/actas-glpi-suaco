# Informe de Rendimiento — Generación de Actas

**Fecha:** 2026-09-02 · **Método:** instrumentación temporal `[PERF]` en el flujo completo de generación + `curl` con tiempos HTTP reales. Sin optimizaciones aplicadas.
**Entorno:** backend corriendo en `127.0.0.1:8001`, Java 21, PostgreSQL SaucoDB local, LibreOffice portable.
**Precisión:** `System.nanoTime()` por etapa; cada tipo medido con payload de 1 equipo (caso típico). Runs de calentamiento descartados (JIT + primera conversión LO).

---

## 1. Tiempos detallados por etapa (ms)

| Etapa | ENTREGA | DEVOLUCIÓN | FORMATEO |
|---|---|---|---|
| 1. Recepción + validación + deserialización (filtro → controller) | 18 | 21 | 19 |
| 2. Construcción de datos (convertValue + arranque) | 1 | 1 | 1 |
| 3. Generación DOCX principal | 180 | 295 | 94 |
| 4. Inserción de imágenes (firma técnico + fijo/foto) | 415 | 572 | 189 |
| 5. Generación checklist DOCX **+** imágenes | 216 + 328 | — | — |
| 6. Generación ZIP | 50 | 33 | 30 |
| 7. Conversión PDF vía LibreOffice — acta | **8 566** | — | — |
| 7b. Conversión PDF vía LibreOffice — checklist | **20 908** | — | — |
| 7. Conversión PDF vía LibreOffice (única) | — | **8 088** | **6 807** |
| 8. Persistencia (Acta + Historial, transaccional) | 10 | 20 | 5 |
| **TOTAL servicio** | **30 682** | **9 013** | **7 132** |
| **TOTAL HTTP (POST → 200)** | **30 706** | **9 039** | **7 162** |

> Nota de veracidad: los sub-timers internos de LibreOffice (`espera-semáforo` = 0 ms, `arranque-proceso` = 21–48 ms, `conversion-proceso` = 0 ms) miden bien el spawn y no muestran contención, pero el tiempo real de ejecución del proceso soffice ocurre dentro de la espera de lectura de su salida, antes del timer `conversion-proceso`. El número **autoritativo** de la conversión es el timer de etapa `pdf-LLO` del orquestador (el que sale en la tabla). El arranque del proceso cuesta decenas de ms; el resto es el motor de LibreOffice convirtiendo.

### Segunda corrida (juego completo, 2 muestras por tipo)

| Tipo | Corrida 1 (ms) | Corrida 2 (ms) | Nota |
|---|---|---|---|
| ENTREGA | 29 547 | 30 706 | estable, ~30 s |
| DEVOLUCIÓN | 10 983 | 9 039 | estabiliza en 9 s |
| FORMATEO | 15 913 | 7 162 | 1ª corrida = LO en frío (perfil nuevo) |

Frío vs caliente (primera invocación de soffice con perfil aislado nuevo): FORMATEO pasa de **15 425 ms** a **6 807 ms** en la conversión (factor ~2.3×). DEVOLUCIÓN fría 9 999 → 8 088.

---

## 2. Cuello de botella: conversión PDF LibreOffice

| Tipo | Total servicio (ms) | Conversión PDF (ms) | % del tiempo |
|---|---|---|---|
| ENTREGA | 30 682 | 29 474 (acta + checklist) | **96.0 %** |
| DEVOLUCIÓN | 9 013 | 8 088 | **89.7 %** |
| FORMATEO | 7 132 | 6 807 | **95.4 %** |

Todo lo demás (validación, DOCX, imágenes, ZIP, persistencia, auditoría) suma **0.2 – 0.9 s** (3–10 % del total). Persistencia persistirActa + historial = 5–20 ms: despreciable.

Hallazgo secundario: dentro de ENTREGA, el checklist convierte **2.4× más lento** que el acta (20.9 s vs 8.6 s). Mismo soffice, mismo contexto — la plantilla del checklist (checkbox/items) es la variable.

---

## 3. Validación "acta persistida vs visible en el listado"

| Medida | Resultado |
|---|---|
| Persistencia dentro del POST | 5–20 ms (atómica, en-request) |
| `GET /actas` (query paginada) | 23–36 ms estables (158 ms el primero, JIT) |
| Recarga del listado tras generar | el frontend **no** recarga tras el POST; el usuario navega manualmente |
| Desfase persistido → listado | **≈ 0 ms** en backend; el listado renderiza lo ya escrito |

**El listado NO es el problema.** El backend termina y la acta está en DB en el mismo request que devuelve el 200; el listado lo consulta en < 40 ms. La demora percibida es **el propio POST bloqueante** (7–30 s según tipo) sin ninguna retroalimentación de carga en el frontend.

---

## 4. Comparativa entre los 3 tipos

| Métrica | ENTREGA | DEVOLUCIÓN | FORMATEO |
|---|---|---|---|
| Conversiones PDF | **2** (acta + checklist) | 1 | 1 |
| Total HTTP (caliente) | **~30.7 s** | ~9.0 s | ~7.2 s |
| % conversión | 96.0 % | 89.7 % | 95.4 % |
| DOCX + imágenes + ZIP | 1.2 s | 0.9 s | 0.3 s |

El costo extra de ENTREGA es **exactamente una segunda conversión LibreOffice** (~20.9 s). No es duplicación de DOCX/ZIP (eso suma ~1 s). La curva frio/caliente (~2.3×) pega fuerte en el primer uso del día.

---

## 5. Recomendación técnica (diagnóstico, sin cambios aplicados)

1. **La conversión LibreOffice es el 90–96 % del tiempo** en los 3 flujos. Cualquier mejora perceptible pasa por ahí, no por DTOs, persistencia ni ZIP (que ya son rápidos).
2. El patrón actual **spawna un soffice nuevo por conversión** con perfil aislado temporal. Opciones a evaluar después (no implementar aún):
   - **Reutilizar el perfil/proceso de LibreOffice** entre conversiones: evita el costo frío (~2.3×) y amortiza el arranque. Mantener el `Semaphore(1)` y su garantía de "nunca dos conversiones simultáneas".
   - **Responder antes del PDF**: generar ZIP + acta en DB (persistencia ya es ~10 ms), y convertir a PDF en background con estado intermedio; hoy el 200 se da después del PDF. Esto eliminaría los 7–30 s del POST sin tocar el motor de conversión.
   - **Sacar la conversión del checklist del POST crítico** (generarla diferida/paralela), porque es la más lenta y no bloquea la firma.
   - Reevaluar OpenPDF (fallback ya existente, conversión en-JVM sin proceso externo) para el checklist por su fidelidad al layout antes de prometer nada.
3. El listado no requiere trabajo: carga en < 40 ms.

---

## 6. Conclusión: ¿UX, rendimiento o ambos?

**Ambos — con rendimiento real como causa raíz.**

- **Es un problema de rendimiento real:** 30.7 s para ENTREGA es lento per se; 9 s / 7.2 s para Devolución/Formateo superan el umbral perceptivo (~2–3 s) pero son tolerables. La causa es única y dominante: la conversión PDF (90–96 %).
- **Es también un problema de UX:** el POST es **bloqueante y mudo** — no hay pantalla/cursor de carga, el usuario mira una página congelada 7–30 s y no sabe si la app falló o el botón no respondió. Eso explica la percepción de "el listado tarda".
- **La pantalla de carga por sí sola NO es la solución:** anima un problema real, no lo resuelve. Es necesaria como mitigación inmediata (feedback de que el proceso sigue), pero la mejora real está en (a) responder antes de la conversión PDF, y/o (b) acortar la conversión (perfil LO reutilizado / checklist diferido). ENTREGA además necesita verificación del extra 2.4× del checklist.
- Prioridad sugerida: 1) pantalla de carga (mitiga la percepción ya), 2) mover PDF a background o reutilizar LO (ataca el 90–96 %).

---

### Anexo: instrumentación utilizada (temporal, ya revertida)

- `Perf.java` / `PerfFilter.java` (nuevos, temporales): timers `nanoTime` + filtro HTTP `[PERF] HTTP <METHOD> <URI> TOTAL`.
- Etapas logueadas en los 3 orquestadores + sub-timers en `LibreOfficePdfService` + `PRE-CONTROLLER`/`CONTROLLER` en los controllers.