package com.empresa.actas.security;

import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
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

        // SEC-003: un usuario bloqueado no puede autenticarse por NINGUNA via.
        // Esto cubre el login (DaoAuthenticationProvider) y, criticamente, el
        // filtro JWT: como JwtAuthenticationFilter recarga al usuario de DB en
        // cada request, un JWT emitido antes del bloqueo deja de ser valido en
        // el siguiente request (la excepcion se ignora en el filtro y no se
        // crea SecurityContext -> 401/403 en todos los endpoints protegidos).
        if (usuario.getBloqueado()) {
            throw new LockedException("Usuario bloqueado: " + username);
        }

        return new UserSecurity(usuario);
    }
}
