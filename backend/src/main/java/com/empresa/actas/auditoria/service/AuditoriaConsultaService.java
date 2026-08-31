package com.empresa.actas.auditoria.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.ActaHistorial;
import com.empresa.actas.acta.repository.ActaHistorialRepository;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.auditoria.dto.EventoAuditoriaResponse;
import com.empresa.actas.auditoria.dto.EventosAuditoriaResponse;
import com.empresa.actas.auditoria.entity.AuditoriaSistema;
import com.empresa.actas.auditoria.repository.AuditoriaSistemaRepository;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Consulta centralizada de auditoria. Fusiona en memoria las dos capas de
 * trazabilidad (acta_historial + auditoria_sistema) en una sola vista
 * homogenea, con filtros, orden por fecha descendente y paginacion.
 *
 * ponytail: agregacion en memoria porque el volumen es chico (cientos de
 * filas); si crece, mover el merge a una vista SQL o tabla de eventos.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaConsultaService {

    /** Tipos de seguridad/accesos para la categoria SEGURIDAD. */
    private static final Set<String> EVENTOS_SEGURIDAD = Set.of(
            "LOGIN_EXITOSO", "LOGIN_FALLIDO", "LOGOUT", "ACCESO_DENEGADO",
            "TOKEN_EXPIRADO", "TOKEN_INVALIDO",
            "OTP_GENERADO", "OTP_ENVIADO", "OTP_ENVIO_FALLIDO", "OTP_VALIDADO",
            "OTP_INVALIDO", "OTP_BLOQUEADO", "OTP_EXPIRADO", "OTP_REENVIADO");

    /** Tipos para contadores de la seccion estadisticas. */
    private static final Set<String> TIPOS_FIRMA = Set.of("ACTA_FIRMADA", "FIRMA_TECNICO_REGISTRADA");
    private static final String LOGIN_EXITOSO = "LOGIN_EXITOSO";
    private static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    private static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";

    private final AuditoriaSistemaRepository sistemaRepository;
    private final ActaHistorialRepository historialRepository;
    private final ActaRepository actaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Consulta paginada de eventos con filtros.
     */
    public EventosAuditoriaResponse consultar(LocalDate desde, LocalDate hasta,
                                              String usuario, String rol, String tipoEvento,
                                              Long idActa, String estado, String q,
                                              int page, int size) {
        List<EventoAuditoriaResponse> filtrados =
                filtrar(eventosFusionados(), desde, hasta, usuario, rol, tipoEvento, idActa, estado, q);
        filtrados.sort(Comparator.comparing(EventoAuditoriaResponse::getFecha).reversed());

        int total = filtrados.size();
        int tamano = Math.max(1, Math.min(size, 200));
        int pagina = Math.max(0, page);
        int desdeIdx = Math.min(pagina * tamano, total);
        int hastaIdx = Math.min(desdeIdx + tamano, total);
        List<EventoAuditoriaResponse> contenido =
                desdeIdx < total ? filtrados.subList(desdeIdx, hastaIdx) : List.of();
        int totalPaginas = (int) Math.ceil((double) total / tamano);

        return EventosAuditoriaResponse.builder()
                .eventos(contenido)
                .total(total)
                .pagina(pagina)
                .tamano(tamano)
                .totalPaginas(totalPaginas)
                .build();
    }

    /**
     * Estadisticas agregadas sobre los eventos que cumplen los filtros:
     * totales, ventanas de tiempo, eventos por dia, firmas, accesos, errores y accesos denegados.
     */
    public Map<String, Object> estadisticas(LocalDate desde, LocalDate hasta,
                                            String usuario, String rol, String tipoEvento,
                                            Long idActa, String estado, String q) {
        List<EventoAuditoriaResponse> lista =
                filtrar(eventosFusionados(), desde, hasta, usuario, rol, tipoEvento, idActa, estado, q);
        return construirEstadisticas(lista);
    }

    // ------------------------------------------------------------------
    // Fusion de las dos capas
    // ------------------------------------------------------------------

    /** Nombre legible del actor de un evento de acta: nombre completo si apunta a un usuario del sistema. */
    private String nombreActor(ActaHistorial h, Map<Long, String> nombrePorUsuario) {
        Long id = h.getActorId() != null ? h.getActorId() : parsearLong(h.getActorNombre());
        if (id != null) {
            String nombre = nombrePorUsuario.get(id);
            if (nombre != null && !nombre.isBlank()) return nombre;
        }
        return h.getActorNombre() != null ? h.getActorNombre() : h.getUsuarioAccion();
    }

    private List<EventoAuditoriaResponse> eventosFusionados() {
        Map<Long, String> rolPorUsuario = new HashMap<>();
        Map<Long, String> nombrePorUsuario = new HashMap<>();
        for (Object[] fila : usuarioRepository.findIdUsuarioRolNombre()) {
            if (fila[0] instanceof Number n) {
                rolPorUsuario.put(n.longValue(), fila[1] != null ? fila[1].toString() : null);
                nombrePorUsuario.put(n.longValue(), fila[2] != null ? fila[2].toString() : null);
            }
        }
        Map<Long, String> correoPorActa = correosPorActa();
        List<EventoAuditoriaResponse> lista = new ArrayList<>();
        for (ActaHistorial h : historialRepository.findAll()) {
            lista.add(fromHistorial(h, rolPorUsuario, nombrePorUsuario, correoPorActa));
        }
        for (AuditoriaSistema s : sistemaRepository.findAll()) {
            lista.add(fromSistema(s, rolPorUsuario));
        }
        return lista;
    }

    private EventoAuditoriaResponse fromHistorial(ActaHistorial h,
                                                  Map<Long, String> rolPorUsuario,
                                                  Map<Long, String> nombrePorUsuario,
                                                  Map<Long, String> correoPorActa) {
        String estadoAnterior = h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : null;
        String estadoNuevo = h.getEstadoNuevo() != null ? h.getEstadoNuevo().name() : null;
        String rol = resolverRol(
                h.getActorId() != null ? h.getActorId() : parsearLong(h.getActorNombre()),
                rolPorUsuario);
        return EventoAuditoriaResponse.builder()
                .id(h.getIdHistorial())
                .fecha(h.getFechaCambio())
                .tipoEvento(h.getTipoEvento() != null ? h.getTipoEvento().name() : null)
                .usuario(nombreActor(h, nombrePorUsuario))
                .rol(rol)
                .entidad("Acta")
                .entidadId(h.getIdActa())
                .accion(humanizar(h.getTipoEvento() != null ? h.getTipoEvento().name() : ""))
                .detalle(h.getObservacion())
                .informacionAdicional(infoAdicionalActa(estadoAnterior, estadoNuevo, h.getIdTokenFirma()))
                .estadoActa(estadoNuevo)
                .correo(correoPorActa.get(h.getIdActa()))
                .categoria(categoria(null, rol))
                .origen("ACTAS")
                .build();
    }

    private EventoAuditoriaResponse fromSistema(AuditoriaSistema s, Map<Long, String> rolPorUsuario) {
        String rol = resolverRol(s.getUsuarioId(), rolPorUsuario);
        List<String> extras = new ArrayList<>();
        if (s.getRecurso() != null && !s.getRecurso().isBlank()) extras.add("Recurso: " + s.getRecurso());
        if (s.getIpDireccion() != null && !s.getIpDireccion().isBlank()) extras.add("IP: " + s.getIpDireccion());
        return EventoAuditoriaResponse.builder()
                .id(s.getIdAuditoria())
                .fecha(s.getFechaEvento())
                .tipoEvento(s.getTipoEvento() != null ? s.getTipoEvento().name() : null)
                .usuario(s.getUsuarioNombre())
                .rol(rol)
                .entidad(s.getEntidad())
                .entidadId(parsearLong(s.getEntidadId()))
                .accion(humanizar(s.getTipoEvento() != null ? s.getTipoEvento().name() : ""))
                .detalle(s.getDetalle())
                .informacionAdicional(extras.isEmpty() ? null : String.join(" · ", extras))
                .estadoActa(null)
                .correo(null)
                .categoria(categoria(s.getTipoEvento() != null ? s.getTipoEvento().name() : null, rol))
                .origen("SISTEMA")
                .build();
    }

    // ------------------------------------------------------------------
    // Filtros
    // ------------------------------------------------------------------

    private List<EventoAuditoriaResponse> filtrar(List<EventoAuditoriaResponse> todos,
                                                  LocalDate desde, LocalDate hasta,
                                                  String usuario, String rol, String tipoEvento,
                                                  Long idActa, String estado, String q) {
        return todos.stream()
                .filter(e -> desde == null || e.getFecha() == null || !e.getFecha().isBefore(desde.atStartOfDay()))
                .filter(e -> hasta == null || e.getFecha() == null || !e.getFecha().isAfter(hasta.plusDays(1).atStartOfDay()))
                .filter(e -> usuario == null || usuario.isBlank() || contiene(e.getUsuario(), usuario))
                .filter(e -> rol == null || rol.isBlank() || rol.equalsIgnoreCase(e.getRol() == null ? "SISTEMA" : e.getRol()))
                .filter(e -> tipoEvento == null || tipoEvento.isBlank() || (e.getTipoEvento() != null && e.getTipoEvento().equalsIgnoreCase(tipoEvento.trim())))
                .filter(e -> idActa == null || idActa.equals(e.getEntidadId()))
                .filter(e -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(e.getEstadoActa() == null ? "" : e.getEstadoActa()))
                .filter(e -> coincideBusqueda(e, q))
                .collect(Collectors.toList());
    }

    private boolean coincideBusqueda(EventoAuditoriaResponse e, String q) {
        if (q == null || q.isBlank()) return true;
        String termino = q.trim().toLowerCase();
        return contiene(e.getUsuario(), q)
                || contiene(e.getEntidad(), q)
                || contiene(e.getTipoEvento(), q)
                || contiene(e.getAccion(), q)
                || contiene(e.getDetalle(), q)
                || contiene(e.getInformacionAdicional(), q)
                || contiene(e.getEstadoActa(), q)
                || contiene(e.getCorreo(), q)
                || (e.getEntidadId() != null && String.valueOf(e.getEntidadId()).contains(termino));
    }

    private boolean contiene(String s, String termino) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(termino.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------
    // Estadisticas
    // ------------------------------------------------------------------

    private Map<String, Object> construirEstadisticas(List<EventoAuditoriaResponse> lista) {
        LocalDate hoy = LocalDate.now();
        long hoyCount = 0, ult7 = 0, ult30 = 0;
        long firmas = 0, accesos = 0, errores = 0, denegados = 0;
        Map<String, Long> porDia = new TreeMap<>(Comparator.reverseOrder());

        for (EventoAuditoriaResponse e : lista) {
            if (e.getFecha() == null) continue;
            LocalDate dia = e.getFecha().toLocalDate();
            if (dia.isEqual(hoy)) hoyCount++;
            if (!dia.isBefore(hoy.minusDays(6))) ult7++;
            if (!dia.isBefore(hoy.minusDays(29))) ult30++;
            if (TIPOS_FIRMA.contains(e.getTipoEvento())) firmas++;
            if (LOGIN_EXITOSO.equals(e.getTipoEvento())) accesos++;
            if (LOGIN_FALLIDO.equals(e.getTipoEvento())) errores++;
            if (ACCESO_DENEGADO.equals(e.getTipoEvento())) denegados++;
            porDia.merge(dia.toString(), 1L, Long::sum);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalEventos", (long) lista.size());
        res.put("eventosHoy", hoyCount);
        res.put("eventosUltimos7Dias", ult7);
        res.put("eventosUltimos30Dias", ult30);
        res.put("firmas", firmas);
        res.put("accesosExitosos", accesos);
        res.put("erroresLogin", errores);
        res.put("accesosDenegados", denegados);
        res.put("eventosPorDia", porDia);
        return res;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Map<Long, String> correosPorActa() {
        Map<Long, String> mapa = new HashMap<>();
        for (Acta a : actaRepository.findAll()) {
            if (a.getCorreoUsuario() != null && !a.getCorreoUsuario().isBlank()) {
                mapa.put(a.getIdActa(), a.getCorreoUsuario());
            }
        }
        return mapa;
    }

    private String resolverRol(Long usuarioId, Map<Long, String> rolPorUsuario) {
        if (usuarioId != null) {
            String rol = rolPorUsuario.get(usuarioId);
            if (rol != null) return rol;
        }
        return "SISTEMA";
    }

    private String categoria(String tipoEvento, String rol) {
        if (tipoEvento != null && EVENTOS_SEGURIDAD.contains(tipoEvento)) return "SEGURIDAD";
        if ("SISTEMA".equals(rol)) return "SISTEMA";
        return "DOCUMENTOS";
    }

    private String infoAdicionalActa(String estadoAnterior, String estadoNuevo, Long idTokenFirma) {
        List<String> partes = new ArrayList<>();
        if (estadoAnterior != null || estadoNuevo != null) {
            partes.add("Estado: " + (estadoAnterior != null ? estadoAnterior : "-")
                    + " → " + (estadoNuevo != null ? estadoNuevo : "-"));
        }
        if (idTokenFirma != null) partes.add("Token firma: " + idTokenFirma);
        return partes.isEmpty() ? null : String.join(" · ", partes);
    }

    /** "ACTA_FIRMADA" -> "Acta Firmada". */
    private String humanizar(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        return Arrays.stream(nombre.toLowerCase(Locale.ROOT).split("_"))
                .map(p -> p.isEmpty() ? p : Character.toUpperCase(p.charAt(0)) + p.substring(1))
                .collect(Collectors.joining(" "));
    }

    private Long parsearLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}