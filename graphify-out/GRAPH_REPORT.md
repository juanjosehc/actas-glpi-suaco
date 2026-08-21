# Graph Report - C:/Users/juanhern/OneDrive - COMPANIA DE FINANCIAMIENTO COMERCIAL COLTEFINANCIERA S.A/Documentos/actas-glpi-Suaco  (2026-08-21)

## Corpus Check
- 128 files · ~56,021 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 735 nodes · 1953 edges · 53 communities (38 shown, 15 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 120 edges (avg confidence: 0.8)
- Token cost: 0 input · 46,004 output

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 21
- Community 22
- Community 23
- Community 24
- Community 25
- Community 26
- Community 27
- Community 28
- Community 29
- Community 30
- Community 32
- Community 33
- Community 34
- Community 35
- Community 36
- Community 38
- Community 39
- Community 41
- Community 42
- Community 43
- Community 44
- Community 45
- Community 46
- Community 47
- Community 48
- Community 49
- Community 50
- Community 51
- Community 52

## God Nodes (most connected - your core abstractions)
1. `ErrorResponse` - 39 edges
2. `Acta` - 23 edges
3. `ActaService` - 21 edges
4. `Usuario` - 21 edges
5. `UsuarioGlpiService` - 20 edges
6. `FirmaService` - 18 edges
7. `TipoEventoActa` - 17 edges
8. `UserSecurity` - 17 edges
9. `UsuarioResponse` - 16 edges
10. `UsuarioService` - 16 edges

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
- **Flujo de generacion de acta de entrega (captura, template, descarga)** — frontend_pages_acta_entrega_page, frontend_pages_acta_entrega_vistapreviaacta, frontend_js_app_generaracta, frontend_pages_acta_view_page, arquitectura_flujo_descarga_zip, flujo_funcional_acta_entrega [INFERRED 0.85]
- **Flujo de firma digital (envio por correo, firma con canvas y foto, gestion)** — frontend_pages_acta_view_page, frontend_pages_firma_page, frontend_pages_firmas_page [INFERRED 0.85]
- **Ciclo de vida de un acta** — claude_acta, claude_firmaservice, claude_signeddocumentservice, claude_estados_acta, claude_auditoria_por_estado [EXTRACTED 1.00]
- **Pipeline de generación DOCX y conversión a PDF** — claude_documentowordservice, claude_docxtemplateengine, claude_docxactaservice, claude_libreofficepdfservice, claude_zipservice [EXTRACTED 1.00]
- **Configuración de almacenamiento persistente** — backend_src_main_resources_application_yml_storage_config, claude_storage_root, claude_rutas_virtuales, claude_migrate_storage [INFERRED 0.85]

## Communities (53 total, 15 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (54): ActaHistorialResponse, ActaResponse, CrearActaRequest, EvidenciaResponse, RechazarRequest, Acta, ActaHistorial, EstadoActa (+46 more)

### Community 1 - "Community 1"
Cohesion: 0.15
Nodes (25): ActaController, AuthController, DocxActaController, ErrorResponse, GlobalExceptionHandler, FirmaController, UsuarioController, io.swagger.v3.oas.annotations.Operation (+17 more)

### Community 2 - "Community 2"
Cohesion: 0.10
Nodes (17): ActaHistorialService, DevolucionController, ActaRequest, DevolucionRequest, EquipoItem, ActaResponse, DevolucionService, DocumentoWordService (+9 more)

### Community 3 - "Community 3"
Cohesion: 0.06
Nodes (46): Arquitectura cliente-servidor de dos capas (Frontend HTML/JS/CSS + Backend Spring Boot + GLPI), Backend Spring Boot (Java 21, Maven, Apache POI, Jackson, Jakarta Validation, Lombok), Checklist de 36 checkboxes en 6 secciones con acordeones, ARQUITECTURA.md - Arquitectura del Sistema, Proceso de descarga ZIP (enlace <a> dinamico con atributo download), Frontend hardcodeado a http://127.0.0.1:8001, Integracion GLPI API REST (App-Token + User-Token, campos 5/23/4/40/17), DocxTemplateEngine - reemplazo de placeholders a nivel de run (+38 more)

### Community 4 - "Community 4"
Cohesion: 0.10
Nodes (20): PdfService, DataInitializer, Override, DocxImageReplacer, DocxTemplateEngine, DocxToPdfService, com.lowagie.text.Document, com.lowagie.text.Font (+12 more)

### Community 5 - "Community 5"
Cohesion: 0.12
Nodes (8): EquipoController, UsuarioGlpiController, UsuarioGlpiResponse, EquipoService, UsuarioGlpiService, UsuarioIndexado, com.fasterxml.jackson.databind.JsonNode, java.net.http.HttpClient

### Community 6 - "Community 6"
Cohesion: 0.14
Nodes (7): TipoActa, DEVOLUCION, ENTREGA, FirmaService, MailService, org.springframework.mail.javamail.JavaMailSender, org.springframework.transaction.annotation.Transactional

### Community 7 - "Community 7"
Cohesion: 0.22
Nodes (25): actionBtn(), aprobarActa(), authHeaders(), checkAuth(), closeEnviarModal(), closeRejectModal(), confirmEnviar(), confirmReject() (+17 more)

### Community 8 - "Community 8"
Cohesion: 0.23
Nodes (23): actionBtn(), authHeaders(), bloquear(), checkAuth(), closeConfirm(), closeModalUser(), desbloquear(), filterUsuarios() (+15 more)

### Community 9 - "Community 9"
Cohesion: 0.27
Nodes (20): aprobarActa(), authHeaders(), checkAuth(), closeDetailModal(), closeEnviarModal(), closeRejectModal(), confirmEnviar(), confirmReject() (+12 more)

### Community 10 - "Community 10"
Cohesion: 0.20
Nodes (20): clearCanvas(), closeRejectModal(), getPhotoBase64(), getSignatureBase64(), hideFieldError(), isCanvasEmpty(), loadActa(), loadPdf() (+12 more)

### Community 11 - "Community 11"
Cohesion: 0.17
Nodes (11): AppConfig, CorsConfig, MailConfig, OpenApiConfig, io.swagger.v3.oas.models.OpenAPI, jakarta.annotation.PostConstruct, OpenAPI, org.springframework.context.annotation.Bean (+3 more)

### Community 12 - "Community 12"
Cohesion: 0.25
Nodes (16): closeEnviarModal(), confirmEnviar(), enviarActa(), formatDate(), getActaFromUrl(), getBadgeClass(), init(), loadById() (+8 more)

### Community 13 - "Community 13"
Cohesion: 0.12
Nodes (15): PG_DATABASE, PG_HOST, PG_PASSWORD, PG_PORT, PG_USER, mcp, postgres, command (+7 more)

### Community 14 - "Community 14"
Cohesion: 0.28
Nodes (8): SecurityConfig, org.springframework.security.authentication.AuthenticationProvider, org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.config.annotation.web.configuration.EnableWebSecurity, org.springframework.security.crypto.password.PasswordEncoder, org.springframework.security.web.SecurityFilterChain, org.springframework.web.cors.CorsConfigurationSource

### Community 15 - "Community 15"
Cohesion: 0.18
Nodes (11): TipoEventoActa, ACTA_ABIERTA_USUARIO, ACTA_APROBADA, ACTA_ENVIADA, ACTA_FIRMADA, ACTA_GENERADA, ACTA_RECHAZADA_ADMIN, ACTA_RECHAZADA_USUARIO (+3 more)

### Community 16 - "Community 16"
Cohesion: 0.31
Nodes (4): Override, UserSecurity, lombok.Getter, org.springframework.security.core.GrantedAuthority

### Community 17 - "Community 17"
Cohesion: 0.38
Nodes (7): fillTemplate(), getFormData(), loadTemplate(), saveActa(), showPreview(), showToast(), validate()

### Community 18 - "Community 18"
Cohesion: 0.33
Nodes (7): Override, JwtAuthenticationFilter, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, org.springframework.stereotype.Component, org.springframework.web.filter.OncePerRequestFilter

### Community 19 - "Community 19"
Cohesion: 0.28
Nodes (4): LoginResponse, AuthService, org.springframework.security.authentication.AuthenticationManager, org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration

### Community 20 - "Community 20"
Cohesion: 0.39
Nodes (3): JwtService, io.jsonwebtoken.Claims, javax.crypto.SecretKey

### Community 21 - "Community 21"
Cohesion: 0.22
Nodes (9): app.firma-url-base (URL pública del portal de firma), Sistema de gestión de actas de entrega/devolución, Coltefinanciera, CRUD de actas con historial, Firma digital pública por token, Generación de documentos Word (DOCX) desde templates, Evolución del sistema más allá de la generación de documentos, Backend Java 21 / Spring Boot 3.4.1 / Maven (+1 more)

### Community 22 - "Community 22"
Cohesion: 0.28
Nodes (9): mail (SMTP parametrizable, opcional), Acta (entidad, datosOriginales, contenidoHtml), ActaHistorial (entidad de auditoría por evento), Auditoría por estado (ActaHistorial con tipo_evento, actor y token), DocxImageReplacer (inserta firma/foto embebidas en DOCX), Estados de acta: GENERADA → ENVIADA → FIRMADA → APROBADA (o RECHAZADA), FirmaService.enviarActa (crea FirmaToken, estado ENVIADA, correo), FirmaToken (UUID de un solo uso con utilizado + fechaUtilizacion) (+1 more)

### Community 23 - "Community 23"
Cohesion: 0.22
Nodes (8): flyonui, dependencies, tailwindcss, @tailwindcss/cli, devDependencies, flyonui, tailwindcss, @tailwindcss/cli

### Community 24 - "Community 24"
Cohesion: 0.29
Nodes (8): glpi (url, app-token, user-token), EquipoService (integración GLPI REST por serial), GLPI (instancia de consulta de equipos por serial), Acta de Devolución (Devolucion_{serial}_{motivo}.zip), Acta de Entrega (ActaLista_{serial}_{asunto}.zip), Sistema de generación automatizada de actas GLPI, Lista de chequeo de entrega, GET /equipo/{serial} (consulta equipo en GLPI)

### Community 25 - "Community 25"
Cohesion: 0.29
Nodes (8): libreoffice.path (soffice.exe portable), Conversión DOCX→PDF vía LibreOffice portable, DocumentoWordService (prepara datos para templates), DocxActaService (orquesta generación de acta + checklist), DocxTemplateEngine (motor de templates DOCX a nivel run), DocxToPdfService (conversión OpenPDF, fuente Java pura), LibreOfficePdfService (conversión DOCX→PDF, semáforo global Semaphore(1)), ZipService (empaqueta DOCX en ZIP)

### Community 26 - "Community 26"
Cohesion: 0.38
Nodes (4): CustomUserDetailsService, Override, org.springframework.security.core.userdetails.UserDetails, org.springframework.security.core.userdetails.UserDetailsService

### Community 27 - "Community 27"
Cohesion: 0.29
Nodes (7): security.jwt (secret + expiration 24h), Autenticación JWT (jjwt 0.12.6) + Spring Security stateless, BCrypt, config/DataInitializer (siembra de roles), Frontend HTML/CSS/JS vanilla + Tailwind 4 + FlyonUI + Flatpickr, Roles (ADMINISTRADOR, TECNICO, AUDITOR), SecurityConfig / JwtAuthenticationFilter (PUBLIC_PATHS), Tailwind CSS 4 + FlyonUI + Flatpickr (frontend)

### Community 28 - "Community 28"
Cohesion: 0.52
Nodes (4): buscarUsuariosGlpi(), cerrarSugerenciasUsuario(), iniciarAutocompleteUsuario(), mostrarSugerenciasUsuario()

### Community 29 - "Community 29"
Cohesion: 0.57
Nodes (6): Path, find_old_sources(), main(), move_file(), old_temp_dir(), Ubica los directorios antiguos existentes que valga la pena migrar. Solo se…

### Community 30 - "Community 30"
Cohesion: 0.47
Nodes (6): storage.root (persistente) + app.generated-dir/uploads-dir, tools/migrate_storage.py (migración de almacenamiento, idempotente), Rutas virtuales uploads/... servidas por /uploads/**, Almacenamiento persistente (storage.root / STORAGE_ROOT), Almacenamiento persistente configurable (storage.root), Docker (referencia, volumen actas-storage)

### Community 32 - "Community 32"
Cohesion: 0.40
Nodes (5): vistaPreviaDevolucion() - renderiza plantilla acta-devolucion.html con {{vars}} y abre acta-view.html?preview, vistaPreviaActa() - renderiza plantilla acta-entrega.html con {{vars}} y abre acta-view.html?preview, acta-view.html - Vista de acta (visor PDF iframe, descargar PDF, Enviar a Firma, modal correo, evidencias), firma.html - Firma digital (canvas de firma, foto verificacion camara, PDF oficial, estados y rechazo con motivos), firmas.html - Gestion de Firmas (monitoreo del proceso, enlace de firma, enviar por correo, evidencias, rechazo)

### Community 34 - "Community 34"
Cohesion: 0.50
Nodes (4): TipoEvidencia, FIRMA, FOTO, PDF_FINAL

### Community 35 - "Community 35"
Cohesion: 0.50
Nodes (4): spring.datasource → PostgreSQL SaucoDB (localhost:5432), spring.jpa ddl-auto update + PostgreSQLDialect, JPA ddl-auto: update + migraciones manuales en sql/, PostgreSQL SaucoDB

### Community 36 - "Community 36"
Cohesion: 0.50
Nodes (3): mcp-postgres-server, dependencies, mcp-postgres-server

## Ambiguous Edges - Review These
- `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` → `acta_1.pdf - PDF de acta (1.2 KB, probable placeholder/vacio) - contenido no inspeccionable`  [AMBIGUOUS]
  backend/uploads/pdf/acta_1.pdf · relation: conceptually_related_to
- `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` → `acta_2.pdf - PDF de acta (155 KB, acta real renderizada) - contenido no inspeccionable`  [AMBIGUOUS]
  backend/uploads/pdf/acta_2.pdf · relation: conceptually_related_to
- `Signature Image (firma_2.png)` → `Signature (firma)`  [AMBIGUOUS]
  backend/uploads/firmas/firma_2.png · relation: conceptually_related_to

## Knowledge Gaps
- **81 isolated node(s):** `com.empresa:actas-glpi`, `GENERADA`, `ENVIADA`, `FIRMADA`, `APROBADA` (+76 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` and `acta_1.pdf - PDF de acta (1.2 KB, probable placeholder/vacio) - contenido no inspeccionable`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `generar-acta.html - Generar Acta (formulario unificado: tipo acta, datos usuario/equipo, ticket GLPI, vista previa)` and `acta_2.pdf - PDF de acta (155 KB, acta real renderizada) - contenido no inspeccionable`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Signature Image (firma_2.png)` and `Signature (firma)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `ErrorResponse` connect `Community 1` to `Community 0`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **Why does `UsuarioGlpiService` connect `Community 5` to `Community 2`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **Why does `TipoEventoActa` connect `Community 15` to `Community 0`, `Community 2`, `Community 6`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **What connects `com.empresa:actas-glpi`, `GENERADA`, `ENVIADA` to the rest of the system?**
  _81 weakly-connected nodes found - possible documentation gaps or missing edges._