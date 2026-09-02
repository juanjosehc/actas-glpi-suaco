package com.empresa.actas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SEC-011: cap de expiracion del JWT (aunque la config pida 24 h) y revocacion
 * por jti (logout efectivo). Cubre la logica que protege sin tocar el filtro.
 */
class JwtServiceTest {

    private static final long DIA_MS = 86_400_000L;      // 24 h (config del yaml)
    private static final long MAX_MS = 8L * 60 * 60 * 1000; // 8 h (tope en codigo)

    private JwtRevocadoRepository revocados;
    private JwtService servicio;

    @BeforeEach
    void setUp() {
        revocados = mock(JwtRevocadoRepository.class);
        when(revocados.existsByJti(anyString())).thenReturn(false);
        // Clave HS256 valida (>= 256 bits). 64 bytes ASCII en Base64.
        String secreto = Base64.getEncoder().encodeToString(
                "clave-secreta-de-prueba-clave-secreta-de-prueba-clave-secreta".getBytes(StandardCharsets.UTF_8));
        servicio = new JwtService(secreto, DIA_MS, revocados);
    }

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("x")
                .roles("ADMIN")
                .build();
    }

    @Test
    void config24h_peroExpiraEn8hMax_capEnCodigo() {
        String token = servicio.generarToken(user("admin"));

        long restante = servicio.extraerExpiracion(token).getTime() - System.currentTimeMillis();
        assertThat(restante).isLessThanOrEqualTo(MAX_MS);
        assertThat(restante).isGreaterThan(MAX_MS - 60_000); // ~8 h, no 24 h
    }

    @Test
    void tokenFirme_valido() {
        String token = servicio.generarToken(user("admin"));
        assertThat(servicio.validarToken(token, user("admin"))).isTrue();
    }

    @Test
    void tokenDeOtroUsuario_invalido() {
        String token = servicio.generarToken(user("admin"));
        assertThat(servicio.validarToken(token, user("otro"))).isFalse();
    }

    @Test
    void tokenRevocado_invalidoAunqueNoExpire_logoutEfectivo() {
        String token = servicio.generarToken(user("admin"));
        assertThat(servicio.validarToken(token, user("admin"))).isTrue();

        // El logout agrega el jti a la denylist; el mismo token deja de valer.
        when(revocados.existsByJti(anyString())).thenReturn(true);
        assertThat(servicio.validarToken(token, user("admin"))).isFalse();
    }

    @Test
    void tokenLLevaJti_unico() {
        String t1 = servicio.generarToken(user("admin"));
        String t2 = servicio.generarToken(user("admin"));
        assertThat(servicio.extraerJti(t1)).isNotBlank();
        assertThat(servicio.extraerJti(t2)).isNotBlank();
        assertThat(servicio.extraerJti(t1)).isNotEqualTo(servicio.extraerJti(t2));
    }

    @Test
    void extraerExpiracion_devuelveFecha() {
        String token = servicio.generarToken(user("admin"));
        Date exp = servicio.extraerExpiracion(token);
        assertThat(exp).isAfter(new Date(System.currentTimeMillis() - 1_000));
    }
}