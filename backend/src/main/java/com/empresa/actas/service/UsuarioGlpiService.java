package com.empresa.actas.service;

import com.empresa.actas.dto.response.UsuarioGlpiResponse;
import com.empresa.actas.exception.GlpiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Servicio de integracion con la API de GLPI para consultar usuarios.
 *
 * Estrategia de busqueda (busqueda global, no por un unico campo):
 * 1. Carga en cache todos los usuarios ACTIVOS de GLPI (paginas de 100,
 *    TTL 10 min, refresco perezoso y sincronizado).
 * 2. La coincidencia se calcula LOCALMENTE sobre un indice por usuario:
 *    login, nombre, apellido, correo, nombre completo, apellido+nombre y
 *    una cadena compacta (concatenacion sin espacios) que permite hallar
 *    subcadenas que cruzan campos (ej. "avasq" en "adrianavasquez").
 * 3. Normalizacion: minusculas + sin acentos -> busqueda case-insensitive
 *    y tolerante a tildes.
 * 4. La consulta se divide en tokens; TODOS deben coincidir en algun campo
 *    (busqueda por multiples palabras en cualquier orden). Se puntua para
 *    ordenar: igualdad > prefijo > substring > compacta.
 * 5. Incluye usuarios con y sin correo (el correo queda vacio si no existe).
 *
 * Campos GLPI:
 * - Field 1: Inicio de sesion (login).
 * - Field 5: Correos electronicos.
 * - Field 8: Activo (is_active).
 * - Field 9: Nombre (firstname).
 * - Field 34: Apellido (realname).
 */
@Service
public class UsuarioGlpiService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioGlpiService.class);

    private static final long TTL_MILLIS = 10 * 60_000L;
    private static final int PAGE_SIZE = 100;
    private static final int MAX_RESULTADOS = 10;

    @Value("${glpi.url}")
    private String glpiUrl;

    @Value("${glpi.app-token}")
    private String appToken;

    @Value("${glpi.user-token}")
    private String userToken;

    private final GlpiHttpClient glpi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UsuarioGlpiService(GlpiHttpClient glpi) {
        this.glpi = glpi;
    }

    private volatile List<UsuarioIndexado> cache = List.of();
    private volatile long cacheCargadoEn = 0L;
    private volatile String ultimoError = "";
    private volatile int totalUsuarios = 0;
    private volatile int paginasCargadas = 0;
    private volatile Map<String, Object> trazaUltimaBusqueda = new java.util.LinkedHashMap<>();

    /**
     * Busca usuarios activos de GLPI por cualquier dato asociado
     * (login, nombre, apellido, nombre completo, correo, o combinaciones).
     *
     * @param consulta texto a buscar
     * @return lista de usuarios activos (maximo 10), con o sin correo
     */
    public List<UsuarioGlpiResponse> buscarUsuarios(String consulta) {
        Map<String, Object> traza = new java.util.LinkedHashMap<>();
        traza.put("consulta", consulta);
        if (consulta == null || consulta.isBlank()) {
            traza.put("motivo", "consulta vacia");
            trazaUltimaBusqueda = traza;
            return List.of();
        }

        List<UsuarioIndexado> corpus = corpus();
        traza.put("corpusUsuarios", corpus.size());
        traza.put("totalUsuariosGlpi", totalUsuarios);
        traza.put("paginasGlpi", paginasCargadas);
        traza.put("cacheVencidoEnMs", TTL_MILLIS);
        if (corpus.isEmpty()) {
            traza.put("motivo", "corpus vacio - ver ultimoError");
            trazaUltimaBusqueda = traza;
            return List.of();
        }

        List<String> tokens = tokenizar(consulta);
        traza.put("tokens", tokens);
        if (tokens.isEmpty()) {
            traza.put("motivo", "sin tokens");
            trazaUltimaBusqueda = traza;
            return List.of();
        }

        List<UsuarioIndexado> coincidencias = corpus.stream()
                .map(u -> u.puntuar(tokens))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingDouble((UsuarioIndexado u) -> -u.puntaje)
                        .thenComparing(u -> u.usuario.getNombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        traza.put("coincidenciasLocal", coincidencias.size());

        List<UsuarioGlpiResponse> resultado = coincidencias.stream()
                .limit(MAX_RESULTADOS)
                .map(UsuarioIndexado::getUsuario)
                .toList();
        traza.put("devueltos", resultado.size());
        trazaUltimaBusqueda = traza;
        return resultado;
    }

    /**
     * Retorna el corpus de usuarios activos, recargandolo si esta vencido
     * o vacio. La recarga es sincronizada para que un solo hilo la ejecute.
     * Un fallo de carga queda registrado en {@link #ultimoError} para
     * diagnosticar por que el autocompletado devuelve vacio.
     */
    private List<UsuarioIndexado> corpus() {
        List<UsuarioIndexado> actual = cache;
        long ahora = System.currentTimeMillis();
        if (ahora - cacheCargadoEn > TTL_MILLIS || actual.isEmpty()) {
            synchronized (this) {
                if (ahora - cacheCargadoEn > TTL_MILLIS || cache.isEmpty()) {
                    try {
                        List<UsuarioIndexado> cargados = cargarUsuariosActivos();
                        cache = cargados;
                        totalUsuarios = cargados.size();
                        ultimoError = "";
                        cacheCargadoEn = System.currentTimeMillis();
                    } catch (Exception e) {
                        ultimoError = String.valueOf(e.getMessage());
                        log.warn("Error cargando usuarios GLPI: {}", ultimoError);
                    }
                }
            }
        }
        return cache;
    }

    /**
     * Estado del cache para diagnostico en vivo.
     */
    public Map<String, Object> getDiagnostico() {
        Map<String, Object> diag = new java.util.LinkedHashMap<>();
        diag.put("cacheUsuarios", cache.size());
        diag.put("totalUsuariosGlpi", totalUsuarios);
        diag.put("paginasCargadas", paginasCargadas);
        diag.put("ultimaCarga", cacheCargadoEn == 0L ? null
                : java.time.Instant.ofEpochMilli(cacheCargadoEn).toString());
        diag.put("ultimoError", ultimoError.isEmpty() ? null : ultimoError);
        diag.put("trazaUltimaBusqueda", trazaUltimaBusqueda);
        return diag;
    }

    /**
     * Descarga todos los usuarios activos de GLPI paginando la busqueda.
     *
     * IMPORTANTE: la API de GLPI devuelve dos contadores en /search:
     * - totalcount: TOTAL de registros que coinciden (todas las paginas).
     * - count:      filas incluidas en ESTA respuesta (tamano de pagina).
     * Leer "count" como total trunca el corpus a la primera pagina.
     */
    private List<UsuarioIndexado> cargarUsuariosActivos() throws Exception {
        String sessionToken = iniciarSesion();
        List<UsuarioIndexado> todos = new ArrayList<>();
        int total = Integer.MAX_VALUE;
        int paginas = 0;

        for (int inicio = 0; inicio < total && todos.size() < total; inicio += PAGE_SIZE) {
            String url = glpiUrl + "/search/User"
                    + "?is_deleted=0"
                    + "&criteria[0][field]=8&criteria[0][searchtype]=equals&criteria[0][value]=1"
                    + "&forcedisplay[0]=1"
                    + "&forcedisplay[1]=5"
                    + "&forcedisplay[2]=9"
                    + "&forcedisplay[3]=34"
                    + "&range=" + inicio + "-" + (inicio + PAGE_SIZE - 1);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("App-Token", appToken)
                    .header("Session-Token", sessionToken)
                    .GET()
                    .build();

            HttpResponse<String> response = glpi.enviar(request);

            if (response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode()
                        + " en paginacion " + inicio + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            int totalCount = root.path("totalcount").asInt(-1);
            if (totalCount < 0) {
                totalCount = root.path("count").asInt(0);
            }
            total = totalCount;
            paginas++;

            JsonNode data = root.path("data");
            int antes = todos.size();
            if (data.isArray()) {
                for (JsonNode fila : data) {
                    agregarIndexado(todos, fila);
                }
            } else {
                Iterator<Map.Entry<String, JsonNode>> campos = data.fields();
                while (campos.hasNext()) {
                    agregarIndexado(todos, campos.next().getValue());
                }
            }
            log.debug("Pagina GLPI {}-{}: count={} totalcount={} filasNuevas={}",
                    inicio, inicio + PAGE_SIZE - 1, root.path("count").asInt(0),
                    totalCount, todos.size() - antes);
            if (todos.size() == antes) {
                break;
            }
        }

        paginasCargadas = paginas;
        log.info("Usuarios GLPI activos cargados en cache: {} ({} paginas)", todos.size(), paginas);
        return todos;
    }

    private void agregarIndexado(List<UsuarioIndexado> destino, JsonNode fila) {
        String login = getFieldValue(fila, "1");
        String correo = getPrimerCorreo(fila, "5");
        String primerNombre = getFieldValue(fila, "9");
        String apellido = getFieldValue(fila, "34");

        String nombre = concatenarNombre(primerNombre, apellido);
        if (nombre.isBlank()) {
            nombre = login;
        }
        if (nombre.isBlank()) {
            return;
        }

        destino.add(UsuarioIndexado.construir(new UsuarioGlpiResponse(login, nombre, correo)));
    }

    private String concatenarNombre(String primerNombre, String apellido) {
        StringBuilder sb = new StringBuilder();
        if (primerNombre != null && !primerNombre.isBlank()) {
            sb.append(primerNombre.trim());
        }
        if (apellido != null && !apellido.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(apellido.trim());
        }
        return sb.toString();
    }

    /**
     * Divide la consulta en tokens de busqueda: minusculas, sin acentos,
     * separando por espacios, puntos y arrobas (permite pegar el correo
     * completo como consulta).
     */
    private static List<String> tokenizar(String consulta) {
        String normalizada = normalizar(consulta).replaceAll("\\s+", " ");
        String[] partes = normalizada.split("[\\s.@]+");
        List<String> tokens = new ArrayList<>();
        for (String parte : partes) {
            if (!parte.isEmpty()) {
                tokens.add(parte);
            }
        }
        return tokens;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Inicia sesion en la API de GLPI y retorna el session token.
     */
    private String iniciarSesion() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(glpiUrl + "/initSession"))
                .header("App-Token", appToken)
                .header("Authorization", "user_token " + userToken)
                .GET()
                .build();

        HttpResponse<String> response = glpi.enviar(request);

        if (response.statusCode() >= 300) {
            throw new GlpiException("Autenticacion con GLPI fallo (HTTP " + response.statusCode()
                    + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String sessionToken = root.path("session_token").asText();
        if (sessionToken.isBlank()) {
            throw new GlpiException("GLPI no devolvio session_token. Verifique App-Token y User-Token.");
        }
        return sessionToken;
    }

    private String getPrimerCorreo(JsonNode node, String fieldId) {
        JsonNode valueNode = node.path(fieldId);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return "";
        }
        if (valueNode.isArray()) {
            for (JsonNode item : valueNode) {
                String valor = item.asText("").trim();
                if (!valor.isEmpty()) {
                    return valor;
                }
            }
            return "";
        }
        return valueNode.asText("").trim();
    }

    private String getFieldValue(JsonNode node, String fieldId) {
        JsonNode valueNode = node.path(fieldId);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return "";
        }
        if (valueNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : valueNode) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(item.asText(""));
            }
            return sb.toString().trim();
        }
        return valueNode.asText("");
    }

    /**
     * Usuario GLPI con indice de busqueda pre-calculado para coincidencia
     * rapida y deterministico sobre el corpus cacheado.
     */
    private static final class UsuarioIndexado {
        final UsuarioGlpiResponse usuario;
        final String loginN;
        final String nombreN;
        final String apellidoN;
        final String correoN;
        final String nombreCompletoN;
        final String nombreCompletoRevN;
        final String compactoN;
        double puntaje;

        private UsuarioIndexado(UsuarioGlpiResponse usuario,
                                String loginN, String nombreN, String apellidoN,
                                String correoN, String nombreCompletoN,
                                String nombreCompletoRevN, String compactoN) {
            this.usuario = usuario;
            this.loginN = loginN;
            this.nombreN = nombreN;
            this.apellidoN = apellidoN;
            this.correoN = correoN;
            this.nombreCompletoN = nombreCompletoN;
            this.nombreCompletoRevN = nombreCompletoRevN;
            this.compactoN = compactoN;
        }

        static UsuarioIndexado construir(UsuarioGlpiResponse usuario) {
            String nombre = normalizar(usuario.getNombre());
            String login = normalizar(usuario.getLogin());
            String correo = normalizar(usuario.getCorreo());
            String nombreN = nombre;
            String apellidoN = "";
            String nombreCompletoRev = nombre;
            int espacio = nombre.indexOf(' ');
            if (espacio > 0) {
                nombreN = nombre.substring(0, espacio);
                apellidoN = nombre.substring(espacio + 1);
                nombreCompletoRev = apellidoN + " " + nombreN;
            }
            String compacto = (login + nombre + correo).replace(" ", "");
            return new UsuarioIndexado(
                    usuario, login, nombreN, apellidoN, correo, nombre, nombreCompletoRev, compacto);
        }

        /**
         * Puntua el usuario frente a los tokens de la consulta.
         * Retorna null si ALGUN token no coincide con ningun campo.
         * Puntajes por token: 100 igualdad, 90 prefijo, 70 substring,
         * 50 solo presente en la cadena compacta (cruza campos).
         */
        UsuarioIndexado puntuar(List<String> tokens) {
            List<String> campos = List.of(loginN, nombreN, apellidoN, correoN, nombreCompletoN, nombreCompletoRevN);
            double total = 0;
            for (String token : tokens) {
                double mejor = 0;
                for (String campo : campos) {
                    if (campo.isEmpty()) {
                        continue;
                    }
                    if (campo.equals(token)) {
                        mejor = Math.max(mejor, 100);
                    } else if (campo.startsWith(token)) {
                        mejor = Math.max(mejor, 90);
                    } else if (campo.contains(token)) {
                        mejor = Math.max(mejor, 70);
                    }
                }
                if (mejor == 0 && compactoN.contains(token)) {
                    mejor = 50;
                }
                if (mejor == 0) {
                    return null;
                }
                total += mejor;
            }
            this.puntaje = total;
            return this;
        }

        UsuarioGlpiResponse getUsuario() {
            return usuario;
        }
    }
}