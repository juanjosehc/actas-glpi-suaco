package com.empresa.actas.acta.repository;

import com.empresa.actas.acta.entity.Acta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {

    Page<Acta> findByTipoActa(String tipoActa, Pageable pageable);

    Page<Acta> findByEstado(String estado, Pageable pageable);

    Page<Acta> findByIdTecnico(Long idTecnico, Pageable pageable);

    /**
     * Busqueda global server-side: id, ticket, estado, tipo, usuario, equipo,
     * serial, placa o cedula. Si idTecnico viene dado (ROL TECNICO), restringe
     * a las actas de ese tecnico.
     */
    @Query("""
            SELECT a FROM Acta a WHERE
              (:idTecnico IS NULL OR a.idTecnico = :idTecnico)
              AND (
                   LOWER(CAST(a.estado AS string)) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(CAST(a.tipoActa AS string)) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.nombreUsuario, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.descripcionEquipo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.serialEquipo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.placaEquipo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.cedulaUsuario, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR CAST(a.idActa AS string) LIKE CONCAT('%', :q, '%')
                OR CAST(COALESCE(a.ticketGlpi, 0) AS string) LIKE CONCAT('%', :q, '%')
              )
            """)
    Page<Acta> buscar(@Param("q") String q, @Param("idTecnico") Long idTecnico, Pageable pageable);
}
