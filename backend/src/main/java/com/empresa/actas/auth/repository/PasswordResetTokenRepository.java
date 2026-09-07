package com.empresa.actas.auth.repository;

import com.empresa.actas.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * SEC-120: busqueda por digest SHA-256 del token recibido.
     * @Query explicito: la derivacion "findByTokenHash" falla porque no existe
     * una propiedad "tokenHash" en la entidad (la columna se llama "token").
     */
    @Query("select p from PasswordResetToken p where p.token = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    /** Invalida tokens anteriores del usuario (una sola recuperacion activa a la vez). */
    long deleteByIdUsuario(Long idUsuario);
}