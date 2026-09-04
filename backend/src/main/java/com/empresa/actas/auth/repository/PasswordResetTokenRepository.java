package com.empresa.actas.auth.repository;

import com.empresa.actas.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Invalida tokens anteriores del usuario (una sola recuperacion activa a la vez). */
    long deleteByIdUsuario(Long idUsuario);
}