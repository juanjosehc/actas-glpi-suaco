/*
====================================================
ACTA DE FORMATEO SEGURO - FRONTEND
====================================================

Responsabilidades:

- Gestión de datos del acta de formateo seguro.
- Administración dinámica de equipos (agregar/eliminar/buscar).
- Validaciones de formulario y equipos.
- Construcción del payload para el backend.
- Descarga automática del ZIP generado.

Diferencias con entrega/devolución:

- Incluye campo "GB" por cada equipo (obligatorio).
- No incluye checklist, sistema operativo ni hardware.
- Máximo 4 equipos (capacidad de la plantilla DOCX).

Endpoints utilizados:

- GET  /equipo/{serial}          → Consulta equipo en GLPI por serial.
- POST /generar-formateo-seguro  → Genera acta de formateo seguro (DOCX).
- GET  /descargar-acta/{archivo} → Descarga el ZIP generado.

Flujo principal:

1. Usuario completa campos obligatorios.
2. Click en "Generar Acta de Formateo Seguro" ejecuta generarFormateoSeguro().
3. Se validan campos y equipos (serial, inventario, GB).
4. Se construye el payload completo.
5. Se envía POST al backend.
6. Se recibe nombre del ZIP y se descarga automáticamente.

====================================================
*/

/*
----------------------------------------------------
LÍMITES DE REGISTROS (capacidad de plantillas DOCX)
----------------------------------------------------
*/
const MAX_EQUIPOS = 4;
const MSG_MAX_EQUIPOS = "Se alcanzó el máximo permitido de 4 equipos.";

/**
 * Genera el acta de formateo seguro.
 *
 * Flujo:
 * 1. Validar campos obligatorios (fecha, entregado_a, cargo_recibe, etc.).
 * 2. Validar que cada equipo tenga serial, inventario y GB.
 * 3. Construir objetos de equipos.
 * 4. Armar el payload completo.
 * 5. Enviar POST a /generar-formateo-seguro.
 * 6. Descargar el ZIP resultante vía /descargar-acta.
 */
async function generarFormateoSeguro() {

    try {

        const camposObligatorios = [

            "fecha",
            "entregado_a",
            "cargo_recibe",
            "entregado_por",
            "cargo_entrega",
            "asunto"

        ];

        let primerCampoInvalido = null;

        camposObligatorios.forEach(id => {

            const valido = validarCampo(id);

            if (!valido && !primerCampoInvalido) {

                primerCampoInvalido =
                    document.getElementById(id);

            }

        });

        const errorEquipo = validarEquipos();

        if (primerCampoInvalido) {

            primerCampoInvalido.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });

            setTimeout(() => {
                primerCampoInvalido.focus();
            }, 300);

            mostrarMensaje(
                "Complete los campos obligatorios",
                "error"
            );

            return;
        }

        if (errorEquipo) {

            errorEquipo.elemento.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });

            setTimeout(() => {
                errorEquipo.elemento.focus();
            }, 300);

            mostrarMensaje(
                `Debe completar: ${errorEquipo.nombre}`,
                "error"
            );

            return;
        }

        // Formato (QA-13/QA-31): fecha ISO y correo del autocompletado
        // (en dataset, no en el valor del campo).
        const validoFormato =
            validarFormatoCampo(
                "fecha",
                /^\d{4}-\d{2}-\d{2}$/,
                "La fecha debe tener formato AAAA-MM-DD (ej. 2026-08-31)"
            ) &&
            validarFormatoCampo(
                "entregado_a",
                /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                "El correo capturado del usuario no es valido",
                getCorreoUsuarioSeleccionado(
                    document.getElementById("entregado_a")
                )
            );

        if (!validoFormato) {
            return;
        }

        const equipos = [];

        document
            .querySelectorAll(".equipo-item")
            .forEach(item => {

                equipos.push({

                    serial:
                        item.querySelector(
                            "[data-serial]"
                        ).value,

                    marca:
                        item.querySelector(
                            "[data-marca]"
                        ).value,

                    tipo:
                        item.querySelector(
                            "[data-tipo]"
                        ).value,

                    modelo:
                        item.querySelector(
                            "[data-modelo]"
                        ).value,

                    inventario:
                        item.querySelector(
                            "[data-inventario]"
                        ).value,

                    gb:
                        item.querySelector(
                            "[data-gb]"
                        ).value

                });

            });

        const payload = {

            fecha:
                document.getElementById("fecha")?.value || "",

            entregado_a:
                document.getElementById("entregado_a")?.value || "",

            cargo_recibe:
                document.getElementById("cargo_recibe")?.value || "",

            entregado_por:
                document.getElementById("entregado_por")?.value || "",

            cargo_entrega:
                document.getElementById("cargo_entrega")?.value || "",

            asunto:
                document.getElementById("asunto")?.value || "",

            // Usuario principal del formateo = quien ENTREGA el equipo
            // (entregado_por). El correo capturado del autocompletado es el
            // suyo, para que el enlace de firma le llegue a el.
            correo:
                getCorreoUsuarioSeleccionado(
                    document.getElementById("entregado_por")
                ) || "",

            equipos:
                equipos

        };

        const token = LoginService.obtenerToken();
        const btnGenerar = document.getElementById("btn-generar-acta");
        if (btnGenerar) {
            btnGenerar.disabled = true;
            btnGenerar.textContent = "Generando Acta...";
        }

        const response = await fetch(
            API_BASE + "/generar-formateo-seguro",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + (token || "")
                },
                body: JSON.stringify(payload)
            }
        );

        if (!response.ok) {

            const errorData =
                await response.json();

            throw new Error(
                errorData.mensaje ||
                "No fue posible generar la documentación"
            );

        }

        const result =
            await response.json();

        if (!result.success) {

            throw new Error(
                result.mensaje ||
                "Error generando la documentación"
            );

        }

        if (btnGenerar) {
            btnGenerar.textContent = "Acta creada ✓";
        }

        // Flujo async (Fase 1): el POST persiste el acta en GENERANDO_DOCUMENTOS
        // y la documentacion (DOCX/ZIP/PDF) se genera en segundo plano. Sin
        // espera síncrona en pantalla: se avisa y se redirige al listado, donde
        // el polling avisa "Documentos listos" cuando termina.
        mostrarMensaje(
            "Acta creada. Documentación en generación, se le avisará al terminar.",
            "success"
        );

        setTimeout(() => {
            window.location.href = "actas.html";
        }, 600);

        return;

    }

    catch (error) {

        const btnGenerar = document.getElementById("btn-generar-acta");
        if (btnGenerar) {
            btnGenerar.disabled = false;
            btnGenerar.textContent = "Generar Acta de Formateo Seguro";
        }

        mostrarMensaje(
            "Error generando la documentación: " + error.message,
            "error"
        );

    }

}


/*
----------------------------------------------------
INICIALIZACIÓN
----------------------------------------------------
*/

/**
 * Inicializa la página al cargar el DOM.
 *
 * Acciones:
 * - Vincula botones de agregar equipo.
 * - Crea un equipo vacío por defecto.
 * - Limpia errores de validación al escribir en cualquier campo.
 * - Inicializa el datepicker en el campo de fecha.
 * - Habilita autocompletado de usuarios GLPI en entregado_a y entregado_por.
 */
document.addEventListener("DOMContentLoaded", () => {

    const btnEquipo =
        document.getElementById("btn-add-equipo");

    if (btnEquipo) {
        btnEquipo.addEventListener("click", agregarEquipo);
    }

    agregarEquipo();

    document
        .querySelectorAll(".input, .textarea")
        .forEach(campo => {

            campo.addEventListener("input", () => {

                if (campo.value.trim()) {

                    campo.classList.remove("is-invalid");

                    const helper =
                        campo.parentElement.querySelector(
                            ".helper-text"
                        );

                    if (helper) {
                        helper.style.display = "none";
                    }
                }

            });

        });

    flatpickr("#fecha", {
        dateFormat: "Y-m-d",
        monthSelectorType: "static",
        allowInput: true
    });

    const campoEntregadoA =
        document.getElementById("entregado_a");

    if (campoEntregadoA) {
        iniciarAutocompleteUsuario(campoEntregadoA);
    }

    const campoEntregadoPor =
        document.getElementById("entregado_por");

    if (campoEntregadoPor) {
        iniciarAutocompleteUsuario(campoEntregadoPor);
    }

});

/*
----------------------------------------------------
CONSTRUCTORES DE DOM SEGUROS (SEC-014)
----------------------------------------------------
No se usa innerHTML: los elementos se crean con
document.createElement y los textos dinámicos van por
textContent, así ningún valor de campos del usuario
puede interpretarse como markup.
*/

/**
 * Crea un campo input-floating (input + etiqueta) sin innerHTML.
 *
 * @param {Object} opts - { dato, etiqueta, deshabilitado, ultimo }
 *        dato establece el data-* que usan las validaciones (ej "tipo" => data-tipo).
 */
function crearCampo({ dato, etiqueta, deshabilitado = false, ultimo = false }) {

    const wrapper = document.createElement("div");
    wrapper.className =
        "input-floating w-full" + (ultimo ? "" : " mb-1");

    const input = document.createElement("input");
    input.type = "text";
    input.className = "input";
    input.placeholder = " ";

    if (dato) {
        input.dataset[dato] = "";
    }

    if (deshabilitado) {
        input.disabled = true;
    }

    const label = document.createElement("label");
    label.className = "input-floating-label";
    label.textContent = etiqueta;

    wrapper.append(input, label);

    return wrapper;
}

/**
 * Crea un botón (eliminar/buscar) sin innerHTML.
 *
 * @param {Object} opts - { dato, texto, extra }
 */
function crearBoton({ dato, texto, extra = "" }) {

    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "btn btn-outline" + (extra ? " " + extra : "");

    if (dato) {
        boton.dataset[dato] = "";
    }

    boton.textContent = texto;

    return boton;
}

/**
 * Crea el encabezado de una fila (título "Equipo N" + Eliminar).
 *
 * @returns {Object} { encabezado, titulo, botonEliminar }
 */
function crearEncabezadoFila(titulo) {

    const encabezado = document.createElement("div");
    encabezado.className = "item-header";

    const elementoTitulo = document.createElement("h4");
    elementoTitulo.textContent = titulo;

    const botonEliminar = crearBoton({
        dato: "eliminar",
        texto: "Eliminar"
    });

    encabezado.append(elementoTitulo, botonEliminar);

    return { encabezado, titulo: elementoTitulo, botonEliminar };
}

/*
----------------------------------------------------
ADMINISTRACIÓN DINÁMICA DE EQUIPOS
----------------------------------------------------
*/

/**
 * Agrega un nuevo bloque de equipo al formulario.
 *
 * Cada bloque contiene: serial, botón buscar, marca, tipo,
 * modelo, inventario y cantidad en GB. Marca/tipo/modelo se
 * autocompletan desde GLPI al hacer click en "Buscar".
 * Se validan serial, inventario y GB antes de enviar.
 * Límite máximo: 4 equipos (capacidad de la plantilla DOCX).
 * Límite mínimo: 1 equipo (no se puede eliminar el último).
 */
function agregarEquipo() {

    const container =
        document.getElementById("equipos-container");

    if (
        container.querySelectorAll(".equipo-item").length >= MAX_EQUIPOS
    ) {

        mostrarMensaje(
            MSG_MAX_EQUIPOS,
            "warning"
        );

        return;
    }

    const numeroEquipo =
        container.querySelectorAll(".equipo-item").length + 1;

    const equipo =
        document.createElement("div");

    equipo.className = "equipo-item";

    const card = document.createElement("div");
    card.className = "card border border-base-300 shadow-sm";

    const cuerpo = document.createElement("div");
    cuerpo.className = "card-body p-2";

    const { encabezado } = crearEncabezadoFila(
        `Equipo ${numeroEquipo}`
    );

    cuerpo.append(
        encabezado,
        crearCampo({ dato: "serial", etiqueta: "Serial" }),
        crearBoton({ dato: "buscar", texto: "Buscar", extra: "mb-4" }),
        crearCampo({ dato: "marca", etiqueta: "Marca", deshabilitado: true }),
        crearCampo({ dato: "tipo", etiqueta: "Tipo", deshabilitado: true }),
        crearCampo({ dato: "modelo", etiqueta: "Modelo", deshabilitado: true }),
        crearCampo({ dato: "inventario", etiqueta: "Inventario" }),
        crearCampo({ dato: "gb", etiqueta: "Cantidad en GB", ultimo: true })
    );

    card.append(cuerpo);
    equipo.append(card);

    container.appendChild(equipo);

    equipo
        .querySelectorAll(".input")
        .forEach(campo => {

            campo.addEventListener("input", () => {

                if (campo.value.trim()) {

                    campo.classList.remove("is-invalid");

                }

            });

        });

    renumerarEquipos();

    equipo
        .querySelector("[data-buscar]")
        .addEventListener("click", () => buscarEquipoBloque(equipo));

    equipo
        .querySelector("[data-eliminar]")
        .addEventListener("click", () => {

            if (
                document.querySelectorAll(".equipo-item").length === 1
            ) {

                mostrarMensaje(
                    "Debe existir al menos un equipo",
                    "warning"
                );

                return;

            }

            equipo.remove();

            renumerarEquipos();

        });

}

/**
 * Consulta GLPI por serial y auto completa marca, tipo y modelo.
 *
 * Endpoint: GET /equipo/{serial}
 * Los campos se actualizan dentro del bloque del equipo
 * al que pertenece el botón "Buscar".
 *
 * @param {HTMLElement} bloque - Elemento .equipo-item que contiene los campos.
 */
async function buscarEquipoBloque(bloque) {

    const serial =
        bloque.querySelector("[data-serial]").value;

    try {

        const response =
            await fetch(`${API_BASE}/equipo/${serial}`);

        if (!response.ok) {

            const glpiError = await response.json().catch(() => null);

            mostrarMensaje(
                (glpiError && glpiError.mensaje)
                    ? "GLPI: " + glpiError.mensaje
                    : "Respuesta no válida del servidor",
                "error"
            );

            return;

        }

        const data =
            await response.json();

        bloque.querySelector("[data-marca]").value =
            data.marca ?? "";

        bloque.querySelector("[data-tipo]").value =
            data.tipo ?? "";

        bloque.querySelector("[data-modelo]").value =
            data.modelo ?? "";

        if (data.marca || data.tipo || data.modelo) {

            mostrarMensaje(
                "Equipo encontrado correctamente",
                "success"
            );

        }

    } catch (error) {

        mostrarMensaje(
            "Error al consultar información del equipo",
            "error"
        );

    }

}

/*
----------------------------------------------------
UTILIDADES DE RENUMERACIÓN
----------------------------------------------------
*/

/**
 * Actualiza los títulos "Equipo N" después de agregar o eliminar.
 *
 * Recorre todos los .equipo-item y asigna el número
 * secuencial basado en su posición actual en el DOM.
 */
function renumerarEquipos() {

    document
        .querySelectorAll(".equipo-item")
        .forEach((equipo, index) => {

            equipo.querySelector("h4").textContent =
                `Equipo ${index + 1}`;

        });

}

/*
----------------------------------------------------
VALIDACIONES
----------------------------------------------------
*/

/**
 * Valida un campo obligatorio por su ID.
 *
 * Aplica la clase "is-invalid" y muestra el helper-text
 * si el campo está vacío. Remueve ambos si tiene valor.
 *
 * @param {string} id - ID del elemento input a validar.
 * @returns {boolean} true si el campo tiene valor, false si está vacío.
 */
function validarCampo(id) {

    const campo =
        document.getElementById(id);

    const helper =
        campo.parentElement.querySelector(".helper-text");

    const vacio =
        !campo.value.trim();

    if (vacio) {

        campo.classList.add("is-invalid");

        if (helper) {
            helper.style.display = "block";
        }

        return false;
    }

    campo.classList.remove("is-invalid");

    if (helper) {
        helper.style.display = "none";
    }

    return true;
}

/**
 * Valida los equipos agregados dinámicamente.
 *
 * En formateo seguro valida por equipo: serial, inventario y GB.
 * Retorna el primer error encontrado para permitir scroll
 * automático y foco en el campo inválido.
 *
 * @returns {Object|null} Primer error: { elemento, nombre } o null si todo es válido.
 */
function validarEquipos() {

    let primerError = null;

    document
        .querySelectorAll(".equipo-item")
        .forEach((equipo, index) => {

            const campos = [
                {
                    elemento: equipo.querySelector("[data-serial]"),
                    nombre: `Serial del Equipo ${index + 1}`
                },
                {
                    elemento: equipo.querySelector("[data-inventario]"),
                    nombre: `Inventario del Equipo ${index + 1}`
                },
                {
                    elemento: equipo.querySelector("[data-gb]"),
                    nombre: `Cantidad en GB del Equipo ${index + 1}`
                }
            ];

            campos.forEach(campo => {

                const vacio =
                    !campo.elemento?.value?.trim();

                if (vacio) {

                    campo.elemento.classList.add("is-invalid");

                    if (!primerError) {

                        primerError = {
                            elemento: campo.elemento,
                            nombre: campo.nombre
                        };

                    }

                } else {

                    campo.elemento.classList.remove("is-invalid");

                }

            });

        });

    return primerError;
}