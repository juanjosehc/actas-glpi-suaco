package com.empresa.actas.usuario.controller;

import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.usuario.dto.ActualizarUsuarioRequest;
import com.empresa.actas.usuario.dto.CrearUsuarioRequest;
import com.empresa.actas.usuario.dto.FirmaTecnicoResponse;
import com.empresa.actas.usuario.dto.GuardarFirmaRequest;
import com.empresa.actas.usuario.dto.UsuarioResponse;
import com.empresa.actas.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestion de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar usuarios", description = "Lista todos los usuarios con paginacion (solo ADMINISTRADOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios listados"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ErrorResponse> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idUsuario") String sort) {

        Page<UsuarioResponse> usuarios = usuarioService.listarUsuarios(
                PageRequest.of(page, size, Sort.by(sort).ascending()));
        return ResponseEntity.ok(ErrorResponse.ok("Usuarios listados", usuarios));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ErrorResponse> obtenerUsuario(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.obtenerUsuario(id);
        return ResponseEntity.ok(ErrorResponse.ok("Usuario encontrado", usuario));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener usuario autenticado", description = "Retorna los datos del usuario actualmente autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ErrorResponse> obtenerUsuarioActual() {
        UsuarioResponse usuario = usuarioService.obtenerUsuarioActual();
        return ResponseEntity.ok(ErrorResponse.ok("Usuario autenticado obtenido correctamente", usuario));
    }

    @GetMapping("/me/firma")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener firma permanente del tecnico", description = "Retorna el estado de la firma permanente del usuario autenticado (tiene, ruta virtual y fecha de actualizacion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la firma obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ErrorResponse> obtenerMiFirma() {
        FirmaTecnicoResponse firma = usuarioService.obtenerFirmaActual();
        return ResponseEntity.ok(ErrorResponse.ok("Firma del tecnico obtenida", firma));
    }

    @GetMapping("/me/firma/archivo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener archivo de la firma del tecnico", description = "Sirve el PNG de la firma del usuario autenticado para la vista previa en Mi Perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen de la firma"),
            @ApiResponse(responseCode = "404", description = "El usuario no tiene firma registrada"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Resource> obtenerMiFirmaArchivo() {
        Resource recurso = usuarioService.obtenerFirmaArchivo();
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"firma_tecnico.png\"")
                .body(recurso);
    }

    @PutMapping("/me/firma")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Registrar o reemplazar firma permanente del tecnico", description = "Guarda la firma PNG (base64) del usuario autenticado, registrandola en AUDITORIA_SISTEMA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firma guardada"),
            @ApiResponse(responseCode = "400", description = "Firma invalida"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ErrorResponse> guardarMiFirma(
            @Valid @RequestBody GuardarFirmaRequest request) {
        FirmaTecnicoResponse firma = usuarioService.guardarFirma(request.firmaBase64());
        return ResponseEntity.ok(ErrorResponse.ok("Firma del tecnico guardada", firma));
    }

    @DeleteMapping("/me/firma")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Eliminar firma permanente del tecnico", description = "Elimina el archivo y el registro de la firma del usuario autenticado, registrandolo en AUDITORIA_SISTEMA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firma eliminada"),
            @ApiResponse(responseCode = "404", description = "El usuario no tiene firma registrada"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ErrorResponse> eliminarMiFirma() {
        usuarioService.eliminarFirma();
        return ResponseEntity.ok(ErrorResponse.ok("Firma del tecnico eliminada"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario (solo ADMINISTRADOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Username o cedula ya existen")
    })
    public ResponseEntity<ErrorResponse> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ErrorResponse.ok("Usuario creado exitosamente", usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ErrorResponse> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.actualizarUsuario(id, request);
        return ResponseEntity.ok(ErrorResponse.ok("Usuario actualizado exitosamente", usuario));
    }

    @PatchMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Bloquear usuario", description = "Bloquea el acceso de un usuario al sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario bloqueado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ErrorResponse> bloquearUsuario(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.bloquearUsuario(id);
        return ResponseEntity.ok(ErrorResponse.ok("Usuario bloqueado exitosamente", usuario));
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desbloquear usuario", description = "Restaura el acceso de un usuario bloqueado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario desbloqueado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ErrorResponse> desbloquearUsuario(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.desbloquearUsuario(id);
        return ResponseEntity.ok(ErrorResponse.ok("Usuario desbloqueado exitosamente", usuario));
    }
}
