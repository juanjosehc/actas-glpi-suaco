package com.empresa.actas.acta.repository;

import com.empresa.actas.acta.entity.Acta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {

    Page<Acta> findByTipoActa(String tipoActa, Pageable pageable);

    Page<Acta> findByEstado(String estado, Pageable pageable);

    Page<Acta> findByIdTecnico(Long idTecnico, Pageable pageable);
}
