package com.empresa.actas.firma.controller;

import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.firma.dto.FirmaOtpEstadoResponse;
import com.empresa.actas.firma.dto.FirmaOtpValidarRequest;
import com.empresa.actas.firma.dto.FirmaOtpValidarResponse;
import com.empresa.actas.firma.dto.FirmaPublicaResponse;
import com.empresa.actas.firma.dto.FirmaRechazoRequest;
import com.empresa.actas.firma.dto.FirmaRequest;
import com.empresa.actas.firma.service.FirmaService;
import com.empresa.actas.firma.service.OtpService;
import com.empresa.actas.service.SignedDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Firma Digital", description = "Endpoints publicos para firma de actas (sin autenticacion, con validacion OTP)")
public class FirmaController {

    /** Header con el UUID de sesion emitido tras validar el OTP correctamente. */
    private static final String OTP_SESION_HEADER = "X-OTP-Sesion";

    private final FirmaService firmaService;
    private final SignedDocumentService signedDocumentService;
    private final OtpService otpService;

    /**
     * Segunda capa de seguridad: ninguno de los endpoints de firma responde
     * sin una sesion OTP validada. El error es indistinto (no filtrar si es
     * el token o la sesion lo que fallo).
     */
    private boolean sesionValida(HttpServletRequest req, String token) {
        return otpService.verificarSesion(token, req.getHeader(OTP_SESION_HEADER));
    }

    @GetMapping("/{token}")
    @Operation(summary = "Obtener acta para firmar", description = "Devuelve los datos del acta asociada al token. Requiere sesion OTP validada (header X-OTP-Sesion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta encontrada"),
            @ApiResponse(responseCode = "401", description = "Sesion OTP invalida o expirada"),
            @ApiResponse(responseCode = "404", description = "Token invalido o expirado")
    })
    public ResponseEntity<ErrorResponse> obtenerActa(@PathVariable String token, HttpServletRequest request) {
        if (!sesionValida(request, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("Sesion OTP invalida o expirada"));
        }
        FirmaPublicaResponse acta = firmaService.obtenerActaPorToken(token);
        return ResponseEntity.ok(ErrorResponse.ok("Acta encontrada", acta));
    }

    @GetMapping("/{token}/pdf")
    @Operation(summary = "Obtener PDF del acta para firmar", description = "Sirve el PDF del acta al firmante. Requiere sesion OTP validada (header X-OTP-Sesion)")
    public ResponseEntity<Resource> obtenerPdf(@PathVariable String token, HttpServletRequest request) {
        if (!sesionValida(request, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
    @Operation(summary = "Firmar acta", description = "Registra la firma digital y foto del usuario, cambia estado a FIRMADA. Requiere sesion OTP validada (header X-OTP-Sesion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta firmada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Sesion OTP invalida o expirada"),
            @ApiResponse(responseCode = "404", description = "Token invalido o expirado"),
            @ApiResponse(responseCode = "400", description = "Firma o foto invalidas")
    })
    public ResponseEntity<ErrorResponse> firmarActa(
            @PathVariable String token,
            @Valid @RequestBody FirmaRequest request,
            HttpServletRequest httpRequest) {
        if (!sesionValida(httpRequest, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("Sesion OTP invalida o expirada"));
        }
        firmaService.firmarActa(token, request);
        signedDocumentService.generarDocumentoFirmado(token);
        return ResponseEntity.ok(ErrorResponse.ok("Acta firmada exitosamente"));
    }

    @PostMapping("/{token}/rechazar")
    @Operation(summary = "Rechazar acta", description = "Rechaza el acta desde el portal publico de firma con un motivo. Requiere sesion OTP validada (header X-OTP-Sesion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta rechazada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Sesion OTP invalida o expirada"),
            @ApiResponse(responseCode = "404", description = "Token invalido o expirado"),
            @ApiResponse(responseCode = "400", description = "Motivo invalido o acta no disponible")
    })
    public ResponseEntity<ErrorResponse> rechazarActa(
            @PathVariable String token,
            @Valid @RequestBody FirmaRechazoRequest request,
            HttpServletRequest httpRequest) {
        if (!sesionValida(httpRequest, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("Sesion OTP invalida o expirada"));
        }
        firmaService.rechazarActa(token, request.motivo());
        return ResponseEntity.ok(ErrorResponse.ok("Acta rechazada exitosamente"));
    }

    @GetMapping("/{token}/otp/estado")
    @Operation(summary = "Estado del OTP", description = "Estado del paso OTP del portal. GET con efecto lateral: si el token nunca tuvo OTP (actas legacy) se genera y envia uno")
    public ResponseEntity<ErrorResponse> estadoOtp(@PathVariable String token, HttpServletRequest request) {
        FirmaOtpEstadoResponse estado = otpService.estado(token, request.getHeader(OTP_SESION_HEADER));
        return ResponseEntity.ok(ErrorResponse.ok("Estado OTP", estado));
    }

    @PostMapping("/{token}/otp/validar")
    @Operation(summary = "Validar OTP", description = "Valida el codigo OTP y emite una sesion de firma (header X-OTP-Sesion). El codigo es de un solo uso")
    public ResponseEntity<ErrorResponse> validarOtp(
            @PathVariable String token,
            @Valid @RequestBody FirmaOtpValidarRequest request) {
        FirmaOtpValidarResponse respuesta = otpService.validar(token, request.codigo());
        return ResponseEntity.ok(ErrorResponse.ok("Codigo validado", respuesta));
    }

    @PostMapping("/{token}/otp/reenviar")
    @Operation(summary = "Reenviar OTP", description = "Genera y envia un nuevo codigo OTP, invalidando el anterior, con cooldown y limite de reenvios")
    public ResponseEntity<ErrorResponse> reenviarOtp(@PathVariable String token) {
        otpService.reenviar(token);
        return ResponseEntity.ok(ErrorResponse.ok("Codigo reenviado al correo del destinatario"));
    }
}