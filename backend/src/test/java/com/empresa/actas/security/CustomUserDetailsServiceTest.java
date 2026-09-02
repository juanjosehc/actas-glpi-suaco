package com.empresa.actas.security;

import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SEC-003: un usuario bloqueado no obtiene UserDetails por NINGUNA via.
 * loadUserByUsername es invocado tanto por el login (DaoAuthenticationProvider)
 * como por JwtAuthenticationFilter en cada request, por lo que el bloqueo es
 * inmediato incluso con un JWT emitido antes del bloqueo.
 */
class CustomUserDetailsServiceTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(repo);

    @Test
    void usuarioBloqueadoLanzaLockedException() {
        Usuario usuario = Usuario.builder()
                .nombreUsuario("jdoe")
                .bloqueado(true)
                .build();
        when(repo.findByNombreUsuario("jdoe")).thenReturn(Optional.of(usuario));

        LockedException ex = assertThrows(LockedException.class,
                () -> service.loadUserByUsername("jdoe"));
        assertTrue(ex.getMessage().contains("jdoe"));
    }

    @Test
    void usuarioNoBloqueadoCargaNormal() {
        Usuario usuario = Usuario.builder()
                .nombreUsuario("jane")
                .bloqueado(false)
                .build();
        when(repo.findByNombreUsuario("jane")).thenReturn(Optional.of(usuario));

        assertTrue(service.loadUserByUsername("jane") instanceof UserSecurity);
    }

    @Test
    void usuarioInexistenteLanzaUsernameNotFound() {
        when(repo.findByNombreUsuario("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }
}