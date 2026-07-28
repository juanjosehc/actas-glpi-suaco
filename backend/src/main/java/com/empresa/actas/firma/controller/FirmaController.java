package com.empresa.actas.firma.controller;

import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.firma.dto.FirmaPublicaResponse;
import com.empresa.actas.firma.dto.FirmaRequest;
import com.empresa.actas.firma.service.FirmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        return ResponseEntity.ok(ErrorResponse.ok("Acta firmada exitosamente"));
    }
}
