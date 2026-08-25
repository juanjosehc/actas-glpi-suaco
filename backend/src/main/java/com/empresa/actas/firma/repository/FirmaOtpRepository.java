package com.empresa.actas.firma.repository;

import com.empresa.actas.firma.entity.FirmaOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FirmaOtpRepository extends JpaRepository<FirmaOtp, Long> {

    Optional<FirmaOtp> findFirstByIdTokenFirmaOrderByFechaCreacionDesc(Long idTokenFirma);

    Optional<FirmaOtp> findBySesionAndIdTokenFirma(String sesion, Long idTokenFirma);

    /** Filas sin validar = ciclo de autenticacion actual; base para el limite de reenvios. */
    long countByIdTokenFirmaAndFechaValidacionIsNull(Long idTokenFirma);

    /**
     * Marca condicional que a la vez setea la sesion: un solo UPDATE atomico.
     * Devuelve 1 si gano el intento, 0 si la fila ya estaba usada (replay bajo concurrencia).
     * Importante: NO hacer {@code save()} sobre la entidad despues, un merge pisaria
     * el flags atomico con el estado detached (usado=false) y reabriria el replay.
     */
    @Modifying
    @Query("UPDATE FirmaOtp o SET o.usado = true, o.fechaValidacion = :fecha, o.sesion = :sesion "
            + "WHERE o.idOtp = :id AND o.usado = false")
    int validarSesionAtomico(@Param("id") Long id,
                             @Param("fecha") LocalDateTime fecha,
                             @Param("sesion") String sesion);

    /** Invalida las filas del ciclo actual (sin fecha_validacion) al emitir un codigo nuevo. */
    @Modifying
    @Query("UPDATE FirmaOtp o SET o.usado = true WHERE o.idTokenFirma = :idTokenFirma AND o.fechaValidacion IS NULL")
    int invalidarNoValidadas(@Param("idTokenFirma") Long idTokenFirma);

    /**
     * Incrementa el contador de intentos fallidos. Se ejecuta en transaccion propia
     * (ver OtpIntentoService): el throw posterior de {@code validar} no debe revertirlo.
     */
    @Modifying
    @Query("UPDATE FirmaOtp o SET o.intentos = o.intentos + 1 WHERE o.idOtp = :id")
    int incrementarIntentos(@Param("id") Long id);

    /** Lee el contador persistido (post-incremento) para el detalle de auditoria. */
    @Query("SELECT o.intentos FROM FirmaOtp o WHERE o.idOtp = :id")
    int obtenerIntentos(@Param("id") Long id);
}