package com.empresa.actas.security;

import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        // SEC-003: NO lanzar LockedException aqui. Lanzada desde loadUserByUsername,
        // DaoAuthenticationProvider la envuelve en InternalAuthenticationServiceException
        // (retrieveUser la captura en "catch (Exception)") y el cliente recibe un 500
        // generico "Error interno del servidor" en el login de una cuenta bloqueada.
        // En su lugar UserSecurity refleja el estado en isAccountNonLocked()/isEnabled():
        //  - Login: las pre-checks de DaoAuthenticationProvider lanzan LockedException
        //    ya desenvuelta -> 401 "Su cuenta ha sido bloqueada..." (GlobalExceptionHandler).
        //  - JWT: JwtAuthenticationFilter valida isAccountNonLocked() y responde
        //    401 codigo=CUENTA_BLOQUEADA sin crear contexto (SEC-003: un JWT emitido
        //    antes del bloqueo deja de ser valido en el siguiente request).
        return new UserSecurity(usuario);
    }
}
