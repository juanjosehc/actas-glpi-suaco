package com.empresa.actas.acta.repository;

import com.empresa.actas.acta.entity.ActaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActaHistorialRepository extends JpaRepository<ActaHistorial, Long> {

    List<ActaHistorial> findByIdActaOrderByFechaCambioDesc(Long idActa);
}
