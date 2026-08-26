# Graph Report - C:/Users/juanhern/OneDrive - COMPANIA DE FINANCIAMIENTO COMERCIAL COLTEFINANCIERA S.A/Documentos/actas-glpi-Suaco  (2026-08-25)

## Corpus Check
- 143 files · ~63,396 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 871 nodes · 2514 edges · 58 communities (36 shown, 22 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 182 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Acta DTO Responses
- Acta HTTP Controller
- Documentacion Arquitectura
- Autenticacion y Filtro JWT
- Config App y CORS
- Backend OTP Firma
- Reemplazo Firma Foto DOCX
- Portal Firma Frontend
- Modelo Acta y Estados
- Sistema y Seguridad Global
- Panel Firmas Frontend
- Panel Usuarios Frontend
- Requests y Eventos Acta
- PDF Final y Auditoria
- Integracion GLPI Usuarios
- Repositorios Acta Historial
- Generacion Devolucion
- Panel Actas Frontend
- Vista Acta Frontend
- Servicios Documento y Correo
- Servicios Consulta Acta
- Eventos Auditoria
- Config Integraciones OTP
- Config MCP PostgreSQL
- Arquitectura Generacion Doc
- Controller Docx Acta
- Motor Templates Documento
- Formulario Generar Acta
- Toolchain Frontend
- Equipo GLPI Serial
- Envio Correos SMTP
- Conversion LibreOffice PDF
- Utilidades UI Autocomplete
- Migracion Storage
- Entrada Aplicacion
- Tipos Evidencia
- Paginas Login y Usuarios
- MCP Server PostgreSQL
- Login JS y Rutas
- Previsualizacion Devolucion
- Imagenes Firma Placeholder
- Config Seguridad JWT
- Ruta PDF LibreOffice
- Placeholder Firma Imagen
- Placeholder Foto
- Foto Placeholder Sin Vision
- Logo Marca
- Logo PNG
- Pagina Gestion Actas
- Pagina Gestion Firmas
- Pagina Home
- Modulo Maven actas-glpi
- Endpoint Descarga ZIP
- Endpoint Generar Acta
- Endpoint Generar Devolucion

## God Nodes (most connected - your core abstractions)
1. `ErrorResponse` - 46 edges
2. `TipoEventoAuditoria` - 30 edges
3. `Acta` - 29 edges
4. `ActaService` - 28 edges
5. `OtpService` - 27 edges
6. `FirmaService` - 23 edges
7. `ActaRepository` - 21 edges
8. `AuditoriaService` - 21 edges
9. `UserSecurity` - 21 edges
10. `Usuario` - 21 edges

## Surprising Connections (you probably didn't know these)
- `ActaEntrega_123_ffff.pdf - PDF de acta de entrega generado (serial 123, asunto ffff) - contenido no inspeccionable` --conceptually_related_to--> `acta-entrega.html - Nueva Acta de Entrega (panel de datos, equipos, hardware, checklist 36 items, SO radio)`  [INFERRED]
  backend/uploads/pdf/ActaEntrega_123_ffff.pdf → frontend/pages/acta-entrega.html
- `ActaEntrega_123_ffffffff.pdf - PDF de acta de entrega generado (serial 123) - contenido no inspeccionable` --conceptually_related_to--> `acta-entrega.html - Nueva Acta de Entrega (panel de datos, equipos, hardware, checklist 36 items, SO radio)`  [INFERRED]
  backend/uploads/pdf/ActaEntrega_123_ffffffff.pdf → frontend/pages/acta-entrega.html
- `ActaEntrega_5CD2256W6H_ffff.pdf - PDF de acta de entrega generado (serial 5CD2256W6H) - contenido no inspeccionable` --conceptually_related_to--> `acta-entrega.html - Nueva Acta de Entrega (panel de datos, equipos, hardware, checklist 36 items, SO radio)`  [INFERRED]
  backend/uploads/pdf/ActaEntrega_5CD2256W6H_ffff.pdf → frontend/pages/acta-entrega.html
- `acta_1.pdf - PDF de acta (1.2 KB, probable placeholder/vacio) - contenido no inspeccionable` --conceptually_related_to--> `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)`  [AMBIGUOUS]
  backend/uploads/pdf/acta_1.pdf → frontend/pages/generar-acta.html
- `acta_2.pdf - PDF de acta (155 KB, acta real renderizada) - contenido no inspeccionable` --conceptually_related_to--> `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)`  [AMBIGUOUS]
  backend/uploads/pdf/acta_2.pdf → frontend/pages/generar-acta.html

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Ciclo de vida de un acta** — claude_acta, claude_firmaservice, claude_signeddocumentservice, claude_estados_acta, claude_auditoria_por_estado [EXTRACTED 1.00]
- **Pipeline de generación DOCX y conversión a PDF** — claude_documentowordservice, claude_docxtemplateengine, claude_docxactaservice, claude_libreofficepdfservice, claude_zipservice [EXTRACTED 1.00]
- **Flujo de generacion de acta de entrega (captura, template, descarga)** — frontend_pages_acta_entrega_page, frontend_pages_acta_entrega_vistapreviaacta, frontend_js_app_generaracta, frontend_pages_acta_view_page, arquitectura_flujo_descarga_zip, flujo_funcional_acta_entrega [INFERRED 0.85]
- **Public Firma Portal Flow (Token + OTP + Link)** — frontend_pages_firma_firma_page, frontend_pages_firma_stateotp, backend_src_main_resources_application_firma_url_base, backend_src_main_resources_application_firma_token, backend_src_main_resources_application_firma_otp [INFERRED 0.75]
- **Firma Evidence Collection and Persistence** — frontend_pages_firma_signature_canvas, frontend_pages_firma_camera, backend_src_main_resources_application_storage [INFERRED 0.75]
- **Backend External Integrations** — backend_src_main_resources_application_datasource, backend_src_main_resources_application_glpi, backend_src_main_resources_application_mail, backend_src_main_resources_application_libreoffice [INFERRED 0.75]

## Communities (58 total, 22 thin omitted)

### Community 0 - "Acta DTO Responses"
Cohesion: 0.07
Nodes (40): ActaHistorialResponse, ActaResponse, EvidenciaResponse, ActaHistorial, ActaMapper, AuditoriaSistema, LoginRequest, LoginResponse (+32 more)

### Community 1 - "Acta HTTP Controller"
Cohesion: 0.12
Nodes (29): ActaController, AuditoriaController, AuthController, DocxActaController, ErrorResponse, GlobalExceptionHandler, FirmaController, FirmaOtpValidarRequest (+21 more)

### Community 2 - "Documentacion Arquitectura"
Cohesion: 0.06
Nodes (46): Arquitectura cliente-servidor de dos capas (Frontend HTML/JS/CSS + Backend Spring Boot + GLPI), Backend Spring Boot (Java 21, Maven, Apache POI, Jackson, Jakarta Validation, Lombok), Checklist de 36 checkboxes en 6 secciones con acordeones, ARQUITECTURA.md - Arquitectura del Sistema, Proceso de descarga ZIP (enlace <a> dinamico con atributo download), Frontend hardcodeado a http://127.0.0.1:8001, Integracion GLPI API REST (App-Token + User-Token, campos 5/23/4/40/17), DocxTemplateEngine - reemplazo de placeholders a nivel de run (+38 more)

### Community 3 - "Autenticacion y Filtro JWT"
Cohesion: 0.10
Nodes (17): CustomUserDetailsService, Override, Override, JwtAuthenticationFilter, JwtService, Override, UserSecurity, io.jsonwebtoken.Claims (+9 more)

### Community 4 - "Config App y CORS"
Cohesion: 0.11
Nodes (20): AppConfig, CorsConfig, MailConfig, OpenApiConfig, SecurityConfig, io.swagger.v3.oas.models.OpenAPI, jakarta.annotation.PostConstruct, OpenAPI (+12 more)

### Community 5 - "Backend OTP Firma"
Cohesion: 0.13
Nodes (8): FirmaOtpEstadoResponse, FirmaOtpValidarResponse, FirmaOtpRepository, OtpIntentoService, OtpService, java.security.SecureRandom, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query

### Community 6 - "Reemplazo Firma Foto DOCX"
Cohesion: 0.14
Nodes (14): DocxImageReplacer, DocxTemplateEngine, DocxToPdfService, com.lowagie.text.Document, com.lowagie.text.Font, Document, java.util.regex.Pattern, org.apache.poi.xwpf.usermodel.XWPFDocument (+6 more)

### Community 7 - "Portal Firma Frontend"
Cohesion: 0.18
Nodes (31): clearCanvas(), closeRejectModal(), disableResend(), fetchOtpEstado(), getPhotoBase64(), getSignatureBase64(), hideFieldError(), initOtp() (+23 more)

### Community 8 - "Modelo Acta y Estados"
Cohesion: 0.13
Nodes (22): Acta, EstadoActa, APROBADA, ENVIADA, FIRMADA, GENERADA, RECHAZADA, TipoActa (+14 more)

### Community 9 - "Sistema y Seguridad Global"
Cohesion: 0.08
Nodes (28): Sistema de gestión de actas de entrega/devolución, Autenticación JWT (jjwt 0.12.6) + Spring Security stateless, BCrypt, Coltefinanciera, CRUD de actas con historial, config/DataInitializer (siembra de roles), JPA ddl-auto: update + migraciones manuales en sql/, EquipoService (integración GLPI REST por serial), Firma digital pública por token (+20 more)

### Community 10 - "Panel Firmas Frontend"
Cohesion: 0.21
Nodes (26): actionBtn(), aprobarActa(), authHeaders(), checkAuth(), closeEnviarModal(), closeRejectModal(), confirmEnviar(), confirmReject() (+18 more)

### Community 11 - "Panel Usuarios Frontend"
Cohesion: 0.23
Nodes (23): actionBtn(), authHeaders(), bloquear(), checkAuth(), closeConfirm(), closeModalUser(), desbloquear(), filterUsuarios() (+15 more)

### Community 12 - "Requests y Eventos Acta"
Cohesion: 0.17
Nodes (5): CrearActaRequest, RechazarRequest, FirmaRequest, FirmaService, org.springframework.transaction.annotation.Transactional

### Community 13 - "PDF Final y Auditoria"
Cohesion: 0.17
Nodes (10): PdfService, AuditoriaService, DataInitializer, TokenFirmaValidador, FirmaUrlBuilder, java.awt.Color, lombok.extern.slf4j.Slf4j, lombok.RequiredArgsConstructor (+2 more)

### Community 14 - "Integracion GLPI Usuarios"
Cohesion: 0.19
Nodes (5): UsuarioGlpiController, UsuarioGlpiResponse, UsuarioGlpiService, UsuarioIndexado, com.fasterxml.jackson.databind.JsonNode

### Community 15 - "Repositorios Acta Historial"
Cohesion: 0.21
Nodes (9): ActaHistorialRepository, AuditoriaSistemaRepository, EvidenciaRepository, FirmaTokenRepository, RolRepository, org.springframework.data.domain.Page, org.springframework.data.domain.Pageable, org.springframework.data.jpa.repository.JpaRepository (+1 more)

### Community 16 - "Generacion Devolucion"
Cohesion: 0.21
Nodes (4): DevolucionController, DevolucionRequest, ActaResponse, DevolucionService

### Community 17 - "Panel Actas Frontend"
Cohesion: 0.27
Nodes (20): aprobarActa(), authHeaders(), checkAuth(), closeDetailModal(), closeEnviarModal(), closeRejectModal(), confirmEnviar(), confirmReject() (+12 more)

### Community 18 - "Vista Acta Frontend"
Cohesion: 0.25
Nodes (17): closeEnviarModal(), confirmEnviar(), descargarActaPdf(), enviarActa(), fetchArchivoAutenticado(), formatDate(), getActaFromUrl(), getBadgeClass() (+9 more)

### Community 19 - "Servicios Documento y Correo"
Cohesion: 0.24
Nodes (8): ActaHistorialService, SignedDocumentService, ZipService, com.fasterxml.jackson.databind.ObjectMapper, java.net.http.HttpClient, java.nio.file.Path, org.slf4j.Logger, org.springframework.stereotype.Service

### Community 21 - "Eventos Auditoria"
Cohesion: 0.12
Nodes (17): TipoEventoAuditoria, ACCESO_DENEGADO, DOCUMENTO_VISTO, EVIDENCIA_VISTA, LOGIN_EXITOSO, LOGIN_FALLIDO, LOGOUT, OTP_BLOQUEADO (+9 more)

### Community 22 - "Config Integraciones OTP"
Cohesion: 0.14
Nodes (16): Firma OTP Security Configuration, Firma Token Expiration Config, Firma Portal URL Base, GLPI REST API Integration Configuration, SMTP Mail Configuration (Optional), OTP Second-Layer Security Rationale, SSL Trust Rationale (SMTP Relays), Persistent File Storage Root (+8 more)

### Community 23 - "Config MCP PostgreSQL"
Cohesion: 0.12
Nodes (15): PG_DATABASE, PG_HOST, PG_PASSWORD, PG_PORT, PG_USER, mcp, postgres, command (+7 more)

### Community 24 - "Arquitectura Generacion Doc"
Cohesion: 0.17
Nodes (15): Acta (entidad, datosOriginales, contenidoHtml), ActaHistorial (entidad de auditoría por evento), Conversión DOCX→PDF vía LibreOffice portable, Auditoría por estado (ActaHistorial con tipo_evento, actor y token), DocumentoWordService (prepara datos para templates), DocxActaService (orquesta generación de acta + checklist), DocxImageReplacer (inserta firma/foto embebidas en DOCX), DocxTemplateEngine (motor de templates DOCX a nivel run) (+7 more)

### Community 25 - "Controller Docx Acta"
Cohesion: 0.27
Nodes (3): ActaRequest, EquipoItem, DocxActaService

### Community 27 - "Formulario Generar Acta"
Cohesion: 0.38
Nodes (7): fillTemplate(), getFormData(), loadTemplate(), saveActa(), showPreview(), showToast(), validate()

### Community 28 - "Toolchain Frontend"
Cohesion: 0.22
Nodes (8): flyonui, dependencies, tailwindcss, @tailwindcss/cli, devDependencies, flyonui, tailwindcss, @tailwindcss/cli

### Community 32 - "Utilidades UI Autocomplete"
Cohesion: 0.52
Nodes (4): buscarUsuariosGlpi(), cerrarSugerenciasUsuario(), iniciarAutocompleteUsuario(), mostrarSugerenciasUsuario()

### Community 33 - "Migracion Storage"
Cohesion: 0.57
Nodes (6): Path, find_old_sources(), main(), move_file(), old_temp_dir(), Ubica los directorios antiguos existentes que valga la pena migrar. Solo se…

### Community 36 - "Tipos Evidencia"
Cohesion: 0.50
Nodes (4): TipoEvidencia, FIRMA, FOTO, PDF_FINAL

### Community 37 - "Paginas Login y Usuarios"
Cohesion: 0.50
Nodes (4): PostgreSQL DataSource (SaucoDB), Environment Override Rationale, login.html - Inicio de sesion (formulario usuario/password con JWT), usuarios.html - Gestion de Usuarios (CRUD con roles ADMINISTRADOR/TECNICO/AUDITOR)

### Community 38 - "MCP Server PostgreSQL"
Cohesion: 0.50
Nodes (3): mcp-postgres-server, dependencies, mcp-postgres-server

### Community 41 - "Previsualizacion Devolucion"
Cohesion: 0.67
Nodes (3): vistaPreviaDevolucion() - renderiza plantilla acta-devolucion.html con {{vars}} y abre acta-view.html?preview, vistaPreviaActa() - renderiza plantilla acta-entrega.html con {{vars}} y abre acta-view.html?preview, acta-view.html - Vista de acta (visor PDF iframe, descargar PDF, Enviar a Firma, modal correo, evidencias)

## Ambiguous Edges - Review These
- `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` → `acta_1.pdf - PDF de acta (1.2 KB, probable placeholder/vacio) - contenido no inspeccionable`  [AMBIGUOUS]
  backend/uploads/pdf/acta_1.pdf · relation: conceptually_related_to
- `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` → `acta_2.pdf - PDF de acta (155 KB, acta real renderizada) - contenido no inspeccionable`  [AMBIGUOUS]
  backend/uploads/pdf/acta_2.pdf · relation: conceptually_related_to
- `Signature Image (firma_2.png)` → `Signature (firma)`  [AMBIGUOUS]
  backend/uploads/firmas/firma_2.png · relation: conceptually_related_to

## Knowledge Gaps
- **97 isolated node(s):** `com.empresa:actas-glpi`, `GENERADA`, `ENVIADA`, `FIRMADA`, `APROBADA` (+92 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **22 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` and `acta_1.pdf - PDF de acta (1.2 KB, probable placeholder/vacio) - contenido no inspeccionable`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` and `acta_2.pdf - PDF de acta (155 KB, acta real renderizada) - contenido no inspeccionable`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Signature Image (firma_2.png)` and `Signature (firma)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `TipoEventoAuditoria` connect `Eventos Auditoria` to `Acta DTO Responses`, `Acta HTTP Controller`, `Modelo Acta y Estados`, `PDF Final y Auditoria`, `Repositorios Acta Historial`, `Servicios Consulta Acta`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **Why does `ErrorResponse` connect `Acta HTTP Controller` to `Acta DTO Responses`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._
- **Why does `UsuarioGlpiService` connect `Integracion GLPI Usuarios` to `Acta HTTP Controller`, `Servicios Documento y Correo`?**
  _High betweenness centrality (0.018) - this node is a cross-community bridge._
- **What connects `com.empresa:actas-glpi`, `GENERADA`, `ENVIADA` to the rest of the system?**
  _97 weakly-connected nodes found - possible documentation gaps or missing edges._