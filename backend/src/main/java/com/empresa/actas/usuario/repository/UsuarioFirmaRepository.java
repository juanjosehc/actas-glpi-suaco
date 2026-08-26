package com.empresa.actas.usuario.repository;

import com.empresa.actas.usuario.entity.UsuarioFirma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioFirmaRepository extends JpaRepository<UsuarioFirma, Long> {

    Optional<UsuarioFirma> findByUsuarioId(Long usuarioId);
}