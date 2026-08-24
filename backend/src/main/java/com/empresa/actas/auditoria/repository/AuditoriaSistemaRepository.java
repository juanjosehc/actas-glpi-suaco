package com.empresa.actas.auditoria.repository;

import com.empresa.actas.auditoria.entity.AuditoriaSistema;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaSistemaRepository extends JpaRepository<AuditoriaSistema, Long> {

    Page<AuditoriaSistema> findByTipoEvento(TipoEventoAuditoria tipoEvento, Pageable pageable);
}