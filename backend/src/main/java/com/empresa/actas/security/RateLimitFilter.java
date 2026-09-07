package com.empresa.actas.security;

import com.empresa.actas.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de intentos por IP sobre {@code /auth/login} y {@code /auth/register}
 * (SEC-004). Ventana fija en memoria del proceso: suficiente para un despliegue
 * de un solo nodo; si la aplicacion escala a N nodos, mover el contador a un
 * almacenamiento compartido (Redis/PostgreSQL).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTER_PATH = "/auth/register";
    // SEC-104: prefijo compartido por /auth/recuperar/solicitar,
    // /auth/recuperar/validar-token y /auth/recuperar/cambiar-password.
    private static final String RECUPERAR_PATH = "/auth/recuperar";
    // SEC-119: throttle por IP del paso OTP del portal de firma (estado,
    // validar, reenviar). El brute force del codigo de 6 digitos ya esta
    // acotado a 5 intentos por fila; este limite zanja el escaneo por IP.
    private static final String FIRMA_OTP_PREFIX = "/firma/";
    private static final String FIRMA_OTP_SUB = "/otp/";

    /** Limpieza oportunista de ventanas viejas cuando el mapa de contadores crece. */
    private static final long LIMPIEZA_UMBRAL = 10_000;

    private final ConcurrentHashMap<String, AtomicInteger> contadores = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${security.rate-limit.login.max:20}")
    private int loginMax;
    @Value("${security.rate-limit.login.segundos:60}")
    private int loginSegundos;
    @Value("${security.rate-limit.registro.max:5}")
    private int registroMax;
    @Value("${security.rate-limit.registro.segundos:3600}")
    private int registroSegundos;
    @Value("${security.rate-limit.recuperar.max:5}")
    private int recuperarMax;
    @Value("${security.rate-limit.recuperar.segundos:900}")
    private int recuperarSegundos;
    @Value("${security.rate-limit.firma-otp.max:30}")
    private int firmaOtpMax;
    @Value("${security.rate-limit.firma-otp.segundos:300}")
    private int firmaOtpSegundos;
    /** Solamente detras de un proxy de confianza; si se expone directo, un
     *  atacante evita el limite falsificando X-Forwarded-For. */
    @Value("${security.rate-limit.trust-x-forwarded-for:false}")
    private boolean trustXForwardedFor;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Limite limite = limitePara(request);
        if (limite == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clave = resolverIp(request);
        if (!permitido(clave, limite)) {
            escribirExceso(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Limite limitePara(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            if (LOGIN_PATH.equals(path)) {
                return new Limite(loginMax, loginSegundos);
            }
            if (REGISTER_PATH.equals(path)) {
                return new Limite(registroMax, registroSegundos);
            }
            if (path.startsWith(RECUPERAR_PATH)) {
                return new Limite(recuperarMax, recuperarSegundos);
            }
        }
        // SEC-119: GET y POST del paso OTP comparten el contador por IP (el GET
        // /otp/estado ya no envia nada, pero sigue siendo un oraculo a acotar).
        if (path.startsWith(FIRMA_OTP_PREFIX) && path.contains(FIRMA_OTP_SUB)) {
            return new Limite(firmaOtpMax, firmaOtpSegundos);
        }
        return null;
    }

    private boolean permitido(String clave, Limite limite) {
        long ventana = Instant.now().getEpochSecond() / limite.segundos;
        // La ventana viaja en la clave: las entradas viejas se descartan al
        // sobreescribirlas o durante la limpieza oportunista.
        String claveVentana = clave + "|" + ventana;
        if (contadores.size() >= LIMPIEZA_UMBRAL) {
            limpiarVentanasViejas(ventana);
        }
        AtomicInteger contador = contadores.computeIfAbsent(claveVentana, k -> new AtomicInteger());
        return contador.incrementAndGet() <= limite.max;
    }

    private void limpiarVentanasViejas(long ventanaActual) {
        contadores.entrySet().removeIf(entrada -> {
            int separador = entrada.getKey().lastIndexOf('|');
            long ventanaClave = Long.parseLong(entrada.getKey().substring(separador + 1));
            return ventanaClave < ventanaActual;
        });
    }

    private String resolverIp(HttpServletRequest request) {
        if (trustXForwardedFor) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void escribirExceso(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI();
        String mensaje;
        if (REGISTER_PATH.equals(path)) {
            mensaje = "Se supero el limite de registros desde esta direccion. Intente mas tarde.";
        } else if (path.startsWith(RECUPERAR_PATH)) {
            mensaje = "Se supero el limite de solicitudes de recuperacion de contrasena. Intente mas tarde.";
        } else if (path.startsWith(FIRMA_OTP_PREFIX) && path.contains(FIRMA_OTP_SUB)) { // SEC-119
            mensaje = "Se supero el limite de solicitudes del codigo OTP. Intente mas tarde.";
        } else {
            mensaje = "Demasiados intentos de inicio de sesion. Espere unos minutos e intente nuevamente.";
        }
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(mensaje));
    }

    private record Limite(int max, int segundos) {
    }
}