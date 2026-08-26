package com.empresa.actas.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Firma permanente del tecnico (una por usuario).
 *
 * El tecnico registra su firma una unica vez desde su perfil (canvas digital)
 * y el sistema la inserta automaticamente en todas las actas futuras que
 * genere. La ruta es virtual ({@code uploads/firmas_tecnico/...}), servida
 * por el backend bajo {@code /uploads/**}, igual que las demas evidencias.
 */
@Entity
@Table(name = "usuario_firma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioFirma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_firma")
    private Long idFirma;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(name = "ruta_firma", nullable = false, length = 255)
    private String rutaFirma;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}