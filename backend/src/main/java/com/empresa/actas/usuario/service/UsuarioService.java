package com.empresa.actas.usuario.service;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.rol.repository.RolRepository;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.usuario.dto.ActualizarUsuarioRequest;
import com.empresa.actas.usuario.dto.CrearUsuarioRequest;
import com.empresa.actas.usuario.dto.FirmaTecnicoResponse;
import com.empresa.actas.usuario.dto.UsuarioResponse;
import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.entity.UsuarioFirma;
import com.empresa.actas.usuario.mapper.UsuarioMapper;
import com.empresa.actas.usuario.repository.UsuarioFirmaRepository;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    /** Cota de seguridad: firma PNG de ~5MB maximo (un canvas no pasa de ~100KB). */
    private static final long MAX_FIRMA_BYTES = 5L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioFirmaRepository usuarioFirmaRepository;
    private final AuditoriaService auditoriaService;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    /** Username del administrador principal protegido (configurable). Vacio = proteger al mas antiguo. */
    @Value("${app.admin-protegido-username:}")
    private String adminProtegidoUsername;

    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(usuario -> marcarProtegido(usuarioMapper.toResponse(usuario), usuario));
    }

    public UsuarioResponse obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));
        return marcarProtegido(usuarioMapper.toResponse(usuario), usuario);
    }

    public UsuarioResponse obtenerUsuarioActual() {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
        return marcarProtegido(usuarioMapper.toResponse(userSecurity.getUsuario()), userSecurity.getUsuario());
    }

    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByNombreUsuario(request.username())) {
            throw new IllegalArgumentException(
                    "El nombre de usuario ya existe: " + request.username());
        }

        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "El correo ya esta registrado: " + request.correo());
        }

        if (usuarioRepository.existsByCedula(request.cedula())) {
            throw new IllegalArgumentException(
                    "La cedula ya esta registrada: " + request.cedula());
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + request.rol()));

        Usuario usuario = Usuario.builder()
                .cedula(request.cedula())
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .nombreUsuario(request.username())
                .correo(request.correo())
                .passwordHash(passwordEncoder.encode(request.password()))
                .cargo(request.cargo())
                .empresa(request.empresa())
                .lugarTrabajo(request.lugarTrabajo())
                .bloqueado(false)
                .rol(rol)
                .build();

        return marcarProtegido(usuarioMapper.toResponse(usuarioRepository.save(usuario)), usuario);
    }

    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));

        if (esAdminPrincipal(usuario)
                && !request.rol().equals(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException(
                    "No esta permitido cambiar el rol del administrador principal del sistema.");
        }

        if (!usuario.getCorreo().equals(request.correo())
                && usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "El correo ya esta registrado: " + request.correo());
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + request.rol()));

        usuario.setNombres(request.nombres());
        usuario.setApellidos(request.apellidos());
        usuario.setCorreo(request.correo());
        usuario.setCargo(request.cargo());
        usuario.setEmpresa(request.empresa());
        usuario.setLugarTrabajo(request.lugarTrabajo());
        usuario.setRol(rol);

        return marcarProtegido(usuarioMapper.toResponse(usuarioRepository.save(usuario)), usuario);
    }

    public UsuarioResponse bloquearUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));

        if (esAdminPrincipal(usuario)) {
            throw new IllegalArgumentException(
                    "El administrador principal del sistema no puede ser bloqueado.");
        }

        usuario.setBloqueado(true);
        return marcarProtegido(usuarioMapper.toResponse(usuarioRepository.save(usuario)), usuario);
    }

    public UsuarioResponse desbloquearUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));

        usuario.setBloqueado(false);
        return marcarProtegido(usuarioMapper.toResponse(usuarioRepository.save(usuario)), usuario);
    }

    // ===================== Firma permanente del tecnico =====================

    /** Estado de la firma del tecnico autenticado (para el perfil). */
    public FirmaTecnicoResponse obtenerFirmaActual() {
        Long idUsuario = usuarioActual().getIdUsuario();
        return usuarioFirmaRepository.findByUsuarioId(idUsuario)
                .map(f -> new FirmaTecnicoResponse(true, f.getRutaFirma(), f.getFechaActualizacion()))
                .orElseGet(() -> new FirmaTecnicoResponse(false, null, null));
    }

    /**
     * Difunde el archivo PNG de la firma del tecnico autenticado para la vista
     * previa del perfil. Retorna null si el usuario no tiene firma o el archivo
     * ya no existe en disco.
     */
    public Resource obtenerFirmaArchivo() {
        Long idUsuario = usuarioActual().getIdUsuario();
        UsuarioFirma firma = usuarioFirmaRepository.findByUsuarioId(idUsuario).orElse(null);
        if (firma == null) {
            return null;
        }
        Path archivo = Paths.get(uploadsDir)
                .resolve(firma.getRutaFirma().substring("uploads/".length()));
        if (!Files.exists(archivo)) {
            log.warn("Archivo de firma tecnico {} no existe en disco: {}", idUsuario, archivo);
            return null;
        }
        return new FileSystemResource(archivo);
    }

    /**
     * Registra (o reemplaza) la firma permanente del tecnico autenticado.
     * La firma llega como PNG base64 desde el canvas del perfil; se guarda en
     * {@code uploads/firmas_tecnico/firma_tecnico_{id}.png} y se registra en
     * AUDITORIA_SISTEMA (FIRMA_TECNICO_REGISTRADA o FIRMA_TECNICO_ACTUALIZADA).
     */
    @Transactional
    public FirmaTecnicoResponse guardarFirma(String firmaBase64) {
        Usuario usuario = usuarioActual();
        Long idUsuario = usuario.getIdUsuario();

        byte[] bytes = decodificarPng(firmaBase64);

        Path dir = Paths.get(uploadsDir, "firmas_tecnico");
        Path archivo = dir.resolve("firma_tecnico_" + idUsuario + ".png");
        try {
            Files.createDirectories(dir);
            Files.write(archivo, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la firma del tecnico: " + e.getMessage());
        }

        String ruta = "uploads/firmas_tecnico/firma_tecnico_" + idUsuario + ".png";

        UsuarioFirma existente = usuarioFirmaRepository.findByUsuarioId(idUsuario).orElse(null);
        if (existente == null) {
            usuarioFirmaRepository.save(UsuarioFirma.builder()
                    .usuarioId(idUsuario)
                    .rutaFirma(ruta)
                    .fechaCreacion(LocalDateTime.now())
                    .fechaActualizacion(LocalDateTime.now())
                    .build());
            auditoriaService.registrar(TipoEventoAuditoria.FIRMA_TECNICO_REGISTRADA,
                    "USUARIO", String.valueOf(idUsuario), ruta,
                    "Firma permanente del tecnico registrada (" + idUsuario + ")");
        } else {
            existente.setRutaFirma(ruta);
            existente.setFechaActualizacion(LocalDateTime.now());
            usuarioFirmaRepository.save(existente);
            auditoriaService.registrar(TipoEventoAuditoria.FIRMA_TECNICO_ACTUALIZADA,
                    "USUARIO", String.valueOf(idUsuario), ruta,
                    "Firma permanente del tecnico actualizada (" + idUsuario + ")");
        }

        log.info("Firma tecnico {} guardada: {}", idUsuario, ruta);
        return new FirmaTecnicoResponse(true, ruta, LocalDateTime.now());
    }

    /** Elimina la firma permanente del tecnico autenticado (archivo + registro). */
    @Transactional
    public void eliminarFirma() {
        Long idUsuario = usuarioActual().getIdUsuario();
        UsuarioFirma firma = usuarioFirmaRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una firma registrada para este usuario"));

        Path archivo = Paths.get(uploadsDir)
                .resolve(firma.getRutaFirma().substring("uploads/".length()));
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo de firma {}: {}", firma.getRutaFirma(), e.getMessage());
        }

        usuarioFirmaRepository.delete(firma);

        auditoriaService.registrar(TipoEventoAuditoria.FIRMA_TECNICO_ELIMINADA,
                "USUARIO", String.valueOf(idUsuario), firma.getRutaFirma(),
                "Firma permanente del tecnico eliminada (" + idUsuario + ")");
    }

    /**
     * Devuelve los bytes de la firma permanente de un usuario (para insertarla
     * en los DOCX generados), o {@code null} si el usuario no tiene firma o el
     * archivo no existe.
     */
    public byte[] obtenerFirmaBytesDe(Long idUsuario) {
        if (idUsuario == null) return null;
        return usuarioFirmaRepository.findByUsuarioId(idUsuario)
                .map(f -> {
                    try {
                        Path archivo = Paths.get(uploadsDir)
                                .resolve(f.getRutaFirma().substring("uploads/".length()));
                        return Files.exists(archivo) ? Files.readAllBytes(archivo) : null;
                    } catch (IOException e) {
                        log.warn("No se pudo leer firma del tecnico {}: {}", idUsuario, e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }

    private byte[] decodificarPng(String firmaBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(firmaBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La firma no es un Base64 valido");
        }
        if (bytes.length == 0 || bytes.length > MAX_FIRMA_BYTES) {
            throw new IllegalArgumentException("La firma supera el tamano permitido");
        }
        if (!isPng(bytes)) {
            throw new IllegalArgumentException("La firma debe ser una imagen PNG");
        }
        return bytes;
    }

    private static boolean isPng(byte[] bytes) {
        if (bytes.length < PNG_MAGIC.length) return false;
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) return false;
        }
        return true;
    }

    /**
     * Username del administrador protegido: el configurado en
     * {@code app.admin-protegido-username}, o si esta vacio el ADMINISTRADOR
     * mas antiguo (menor id_usuario).
     */
    private String usernameAdminPrincipal() {
        if (adminProtegidoUsername != null && !adminProtegidoUsername.isBlank()) {
            return adminProtegidoUsername;
        }
        return usuarioRepository.findFirstByRol_NombreOrderByIdUsuarioAsc("ADMINISTRADOR")
                .map(Usuario::getNombreUsuario)
                .orElse(null);
    }

    /** true si el usuario es el administrador principal protegido. */
    private boolean esAdminPrincipal(Usuario usuario) {
        if (!"ADMINISTRADOR".equals(usuario.getRol().getNombre())) {
            return false;
        }
        String principal = usernameAdminPrincipal();
        return principal != null && principal.equalsIgnoreCase(usuario.getNombreUsuario());
    }

    private UsuarioResponse marcarProtegido(UsuarioResponse response, Usuario usuario) {
        response.setProtegido(esAdminPrincipal(usuario));
        return response;
    }

    private Usuario usuarioActual() {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
        return userSecurity.getUsuario();
    }
}
