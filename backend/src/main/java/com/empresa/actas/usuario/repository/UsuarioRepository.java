package com.empresa.actas.usuario.repository;

import com.empresa.actas.usuario.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByCedula(String cedula);

    Page<Usuario> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrNombreUsuarioContainingIgnoreCase(
            String nombre, String apellido, String username, Pageable pageable);
}
