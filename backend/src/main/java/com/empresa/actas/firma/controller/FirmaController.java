package com.empresa.actas.firma.controller;

import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.firma.dto.FirmaPublicaResponse;
import com.empresa.actas.firma.dto.FirmaRechazoRequest;
import com.empresa.actas.firma.dto.FirmaRequest;
import com.empresa.actas.firma.service.FirmaService;
import com.empresa.actas.service.SignedDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/firma")
@RequiredArgsConstructor
@Tag(name = "Firma Digital", description = "Endpoints publicos para firma de actas (sin autenticacion)")
public class FirmaController {

    private final FirmaService firmaService;
    private final SignedDocumentService signedDocumentService;

    @GetMapping("/{token}")
    @Operation(summary = "Obtener acta para firmar", description = "Retorna los datos del acta asociada al token de firma (acceso publico)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta encontrada"),
            @ApiResponse(responseCode = "404", description = "Token invalido o expirado")
    })
    public ResponseEntity<ErrorResponse> obtenerActa(@PathVariable String token) {
        FirmaPublicaResponse acta = firmaService.obtenerActaPorToken(token);
        return ResponseEntity.ok(ErrorResponse.ok("Acta encontrada", acta));
    }

    @GetMapping("/{token}/pdf")
    @Operation(summary = "Obtener PDF del acta para firmar", description = "Sirve el PDF del acta al firmante validando el token de firma (acceso publico con token)")
    public ResponseEntity<Resource> obtenerPdf(@PathVariable String token) {
        Resource recurso = firmaService.obtenerPdfPorToken(token);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"acta.pdf\"")
                .body(recurso);
    }

    @PostMapping("/{token}")
    @Operation(summary = "Firmar acta", description = "Registra la firma digital y foto del usuario, cambia estado a FIRMADA (acceso publico)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta firmada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Token invalido o expirado"),
            @ApiResponse(responseCode = "400", description = "Firma o foto invalidas")
    })
    public ResponseEntity<ErrorResponse> firmarActa(
            @PathVariable String token,
            @Valid @RequestBody FirmaRequest request) {
        firmaService.firmarActa(token, request);
        signedDocumentService.generarDocumentoFirmado(token);
        return ResponseEntity.ok(ErrorResponse.ok("Acta firmada exitosamente"));
    }

    @PostMapping("/{token}/rechazar")
    @Operation(summary = "Rechazar acta", description = "Rechaza el acta desde el portal publico de firma con un motivo, cambia estado a RECHAZADA (acceso publico)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta rechazada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Token invalido o expirado"),
            @ApiResponse(responseCode = "400", description = "Motivo invalido o acta no disponible")
    })
    public ResponseEntity<ErrorResponse> rechazarActa(
            @PathVariable String token,
            @Valid @RequestBody FirmaRechazoRequest request) {
        firmaService.rechazarActa(token, request.motivo());
        return ResponseEntity.ok(ErrorResponse.ok("Acta rechazada exitosamente"));
    }
}
