package com.empresa.actas.auditoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Auditoria operacional, de seguridad y accesos (CAPA 2).
 * Tabla separada de {@code acta_historial} (ciclo de vida documental).
 *
 * Permite responder en una auditoria corporativa:
 * quien inicio sesion, quien fallo autenticandose, quien intento
 * acceder a actas ajenas, quien visualizo documentos/evidencias,
 * que token expiro o se intento usar invalido.
 */
@Entity
@Table(name = "auditoria_sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "fecha_evento", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEvento = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoAuditoria tipoEvento;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(name = "entidad", length = 50)
    private String entidad;

    @Column(name = "entidad_id", length = 100)
    private String entidadId;

    @Column(name = "recurso", length = 255)
    private String recurso;

    @Column(name = "detalle", length = 500)
    private String detalle;

    @Column(name = "ip_direccion", length = 45)
    private String ipDireccion;
}