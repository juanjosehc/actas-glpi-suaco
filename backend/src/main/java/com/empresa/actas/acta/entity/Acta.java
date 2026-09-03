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

    @Column(name = "marca_modelo", length = 100)
    private String marcaModelo;

    @Column(name = "procesador", length = 100)
    private String procesador;

    @Column(name = "memoria_ram", length = 50)
    private String memoriaRam;

    @Column(name = "disco_duro", length = 100)
    private String discoDuro;

    @Column(name = "sistema_operativo", length = 100)
    private String sistemaOperativo;

    @Column(name = "monitor", length = 100)
    private String monitor;

    @Column(name = "accesorios", length = 255)
    private String accesorios;

    @Column(name = "estado_equipo", length = 50)
    private String estadoEquipo;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Column(name = "lugar_trabajo", length = 100)
    private String lugarTrabajo;

    @Column(name = "empresa", length = 100)
    private String empresa;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "contenido_html", columnDefinition = "text")
    private String contenidoHtml;

    @Column(name = "observacion_rechazo", length = 500)
    private String observacionRechazo;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

    @Column(name = "ruta_pdf", length = 500)
    private String rutaPdf;

    /** Ruta virtual del PDF del checklist de entrega (expediente documental). */
    @Column(name = "ruta_pdf_checklist", length = 500)
    private String rutaPdfChecklist;

    /** Nombre del ZIP almacenado en {@code app.generated-dir} (columna nueva, b014). */
    @Column(name = "ruta_zip", length = 500)
    private String rutaZip;

    @Column(name = "datos_originales", columnDefinition = "text")
    private String datosOriginales;

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
