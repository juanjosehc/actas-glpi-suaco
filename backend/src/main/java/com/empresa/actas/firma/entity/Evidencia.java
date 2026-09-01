package com.empresa.actas.firma.entity;

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
@Table(name = "evidencia")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evidencia")
    private Long idEvidencia;

    @Column(name = "id_acta", nullable = false)
    private Long idActa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoEvidencia tipo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public enum TipoEvidencia {
        FIRMA,
        FOTO,
        PDF_FINAL,
        /** PDF del checklist de entrega firmado (expediente documental de ENTREGA). */
        CHECKLIST_FINAL
    }
}
