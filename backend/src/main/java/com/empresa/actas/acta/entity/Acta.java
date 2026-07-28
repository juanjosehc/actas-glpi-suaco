package com.empresa.actas.acta.entity;

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

@Entity
@Table(name = "acta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acta")
    private Long idActa;

    @Column(name = "id_asignacion")
    private Long idAsignacion;

    @Column(name = "id_tecnico", nullable = false)
    private Long idTecnico;

    @Column(name = "ticket_glpi")
    private Long ticketGlpi;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acta", nullable = false, length = 20)
    private TipoActa tipoActa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoActa estado = EstadoActa.GENERADA;

    @Column(name = "cedula_usuario", length = 20)
    private String cedulaUsuario;

    @Column(name = "nombre_usuario", length = 100)
    private String nombreUsuario;

    @Column(name = "correo_usuario", length = 100)
    private String correoUsuario;

    @Column(name = "serial_equipo", length = 50)
    private String serialEquipo;

    @Column(name = "placa_equipo", length = 50)
    private String placaEquipo;

    @Column(name = "descripcion_equipo", length = 255)
    private String descripcionEquipo;

    @Column(name = "contenido_html", columnDefinition = "text")
    private String contenidoHtml;

    @Column(name = "observacion_rechazo", length = 500)
    private String observacionRechazo;

    @Column(name = "ruta_pdf", length = 500)
    private String rutaPdf;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;
}
