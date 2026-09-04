package com.empresa.actas.usuario.repository;

import com.empresa.actas.usuario.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByCedula(String cedula);

    Optional<Usuario> findFirstByRol_NombreOrderByIdUsuarioAsc(String nombre);

    /** Id + rol + nombre completo, para el visor de auditoria (evita lazy-load de asociaciones). */
    @Query("select u.idUsuario, r.nombre, concat(u.nombres, ' ', u.apellidos) from Usuario u join u.rol r")
    List<Object[]> findIdUsuarioRolNombre();

    Page<Usuario> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrNombreUsuarioContainingIgnoreCase(
            String nombre, String apellido, String username, Pageable pageable);
}
