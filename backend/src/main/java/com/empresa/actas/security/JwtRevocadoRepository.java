package com.empresa.actas.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Persistencia de la denylist de JWT (SEC-011). {@code deleteBy...Before}
 * permite limpiar registros cuyo token original ya expiro.
 */
@Repository
public interface JwtRevocadoRepository extends JpaRepository<JwtRevocado, String> {

    boolean existsByJti(String jti);

    long deleteByFechaExpiracionTokenBefore(LocalDateTime fecha);
}