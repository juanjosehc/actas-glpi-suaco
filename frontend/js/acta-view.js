(function () {
    
    var viewDocument = document.getElementById("viewDocument");
    var pdfViewer = document.getElementById("pdfViewer");
    var viewLoading = document.getElementById("viewLoading");
    var viewNoPdf = document.getElementById("viewNoPdf");
    var viewTitle = document.getElementById("viewTitle");
    var viewSubtitle = document.getElementById("viewSubtitle");
    var viewEstado = document.getElementById("viewEstado");
    var viewTipo = document.getElementById("viewTipo");
    var viewUsuario = document.getElementById("viewUsuario");
    var viewEquipo = document.getElementById("viewEquipo");
    var viewFecha = document.getElementById("viewFecha");
    var viewEvidences = document.getElementById("viewEvidences");
    var evidencesGrid = document.getElementById("evidencesGrid");
        var viewInfoBar = document.getElementById("viewInfoBar");
    var downloadPdfBtn = document.getElementById("downloadPdfBtn");
    var sendFirmaBtn = document.getElementById("sendFirmaBtn");
    var enviarModal = document.getElementById("enviarModal");
    var enviarCorreo = document.getElementById("enviarCorreo");
    var enviarError = document.getElementById("enviarError");
    var enviarCancel = document.getElementById("enviarCancel");
    var enviarConfirm = document.getElementById("enviarConfirm");
    var enviarClose = document.getElementById("enviarClose");
    var currentActaId = null;
    var currentActaCorreo = "";
    var viewDocs = document.getElementById("viewDocs");
    var docActaCard = document.getElementById("docActaCard");
    var docChecklistCard = document.getElementById("docChecklistCard");
    var pdfUrls = {};
    var activeDoc = "acta";

    function showToast(message, type) {
        return mostrarNotificacion(message, type);
    }

    function getBadgeClass(estado) {
        var map = {
            "GENERADA": "badge--GENERADA",
            "ENVIADA": "badge--ENVIADA",
            "FIRMADA": "badge--FIRMADA",
            "APROBADA": "badge--APROBADA",
            "RECHAZADA": "badge--RECHAZADA",
            "GENERANDO_DOCUMENTOS": "badge--GENERANDO_DOCUMENTOS",
            "GENERACION_FALLIDA": "badge--GENERACION_FALLIDA"
        };
        return map[estado] || "";
    }

    function estadoLabel(estado) {
        var map = {
            "GENERANDO_DOCUMENTOS": "Generando...",
            "GENERACION_FALLIDA": "Error de generación"
        };
        return map[estado] || estado || "-";
    }

    function formatDate(dateStr) {
        if (!dateStr) return "-";
        try {
            var d = new Date(dateStr);
            return d.toLocaleDateString("es-CO", {
                year: "numeric", month: "long", day: "numeric"
            });
        } catch (_) { return dateStr; }
    }

    function formatDateTime(dateStr) {
        if (!dateStr) return "-";
        try {
            var d = new Date(dateStr);
            return d.toLocaleDateString("es-CO", {
                year: "numeric", month: "long", day: "numeric",
                hour: "2-digit", minute: "2-digit"
            });
        } catch (_) { return dateStr; }
    }

    function fetchArchivoAutenticado(url) {
        var tokenAuth = LoginService.obtenerToken();
        return fetch(url, { headers: { "Authorization": "Bearer " + tokenAuth } });
    }

    function loadPdfViewer(fileUrl, useAuth) {
        viewLoading.style.display = "block";
        viewNoPdf.style.display = "none";
        viewDocument.style.display = "none";

        if (!fileUrl) {
            viewLoading.style.display = "none";
            viewNoPdf.style.display = "block";
            return;
        }

        var request = useAuth ? fetchArchivoAutenticado(fileUrl) : fetch(fileUrl);

        request.then(function (r) {
                if (!r.ok) throw new Error("Error al cargar PDF");
                return r.blob();
            })
            .then(function (blob) {
                viewLoading.style.display = "none";
                pdfViewer.src = URL.createObjectURL(blob);
                viewDocument.style.display = "block";
            })
            .catch(function () {
                viewLoading.style.display = "none";
                viewNoPdf.style.display = "block";
                viewNoPdf.innerHTML = '<p style="color:#DC2626;padding:20px;text-align:center;">Error al cargar el PDF.</p>';
            });
    }

    function renderDocument(acta, evidencias, pdfUrl, pdfAuth) {
        viewLoading.style.display = "none";
        clearTimeout(viewPollTimer);

        // Generacion async en curso: no hay PDF aun. Se muestra un placeholder
        // y se re-consulta hasta que la generacion termine.
        if (acta.estado === "GENERANDO_DOCUMENTOS") {
            renderGenerando(acta);
            return;
        }
        // Fallo de generacion: estado terminal, sin documento que mostrar.
        if (acta.estado === "GENERACION_FALLIDA") {
            renderFallida(acta);
            return;
        }

        var estadoBadge = '<span class="badge ' + getBadgeClass(acta.estado) + '">' + estadoLabel(acta.estado) + '</span>';
        viewEstado.innerHTML = estadoBadge;
        viewTipo.textContent = acta.tipoActa || "-";
        viewUsuario.textContent = acta.nombreUsuario || "-";
        viewEquipo.textContent = acta.descripcionEquipo || "-";

        var fecha = acta.fechaCreacion ? formatDate(acta.fechaCreacion) : "-";
        viewFecha.textContent = fecha;

        viewTitle.textContent = "Acta #" + acta.id;
        viewSubtitle.textContent = (acta.tipoActa || "") + " - " + (acta.nombreUsuario || "");

        var hasAuthToken = !!LoginService.obtenerToken();
        var hasRealId = typeof acta.id === "number" && isFinite(acta.id);
        if (hasAuthToken && hasRealId) {
            // Gestión: se guarda el id para acciones (enviar, descargar PDF).
            currentActaId = acta.id;
            currentActaCorreo = acta.correoUsuario || "";
        } else {
            // Portal público (token): sin JWT, sin acciones de gestion.
            currentActaId = null;
            currentActaCorreo = "";
        }

        setupDocsSection(acta, pdfUrl, pdfAuth);

        // Enviar a Firma es operativo: AUDITOR (solo lectura) no lo ve.
        var rolUsuario = typeof LoginService !== "undefined" && LoginService.getRol ? LoginService.getRol() : localStorage.getItem("role");
        var puedeOperar = rolUsuario === "ADMINISTRADOR" || rolUsuario === "TECNICO";
        if (sendFirmaBtn) {
            if (hasAuthToken && hasRealId && acta.estado === "GENERADA" && puedeOperar) {
                sendFirmaBtn.style.display = "inline-flex";
            } else {
                sendFirmaBtn.style.display = "none";
            }
        }

        renderEvidencesGrid(evidencias, currentActaId);
    }

    var viewPollTimer = null;

    /** Plancha de estado mientras la generacion async esta en curso. */
    function renderGenerando(acta) {
        currentActaId = acta.id;
        viewTitle.textContent = "Acta #" + acta.id;
        viewSubtitle.textContent = (acta.tipoActa || "") + " - " + (acta.nombreUsuario || "");
        viewEstado.innerHTML = '<span class="badge ' + getBadgeClass(acta.estado) + '">' + estadoLabel(acta.estado) + '</span>';
        if (viewInfoBar) viewInfoBar.style.display = "";
        if (viewDocs) viewDocs.style.display = "none";
        if (downloadPdfBtn) downloadPdfBtn.style.display = "none";
        if (sendFirmaBtn) sendFirmaBtn.style.display = "none";

        viewLoading.style.display = "none";
        viewDocument.style.display = "none";
        viewNoPdf.style.display = "block";
        viewNoPdf.innerHTML = '<p style="text-align:center;padding:32px 20px;color:#475569;">' +
            '<span class="loading-spinner" style="margin-bottom:12px"></span><br>' +
            'Generando documentos...<br>' +
            '<span style="font-size:13px;color:#94A3B8;">La página se actualizará automáticamente al terminar.</span></p>';

        pollView(acta.id);
    }

    /** Estado terminal de generacion fallida: sin documento, con reintento. */
    function renderFallida(acta) {
        currentActaId = acta.id;
        viewTitle.textContent = "Acta #" + acta.id;
        viewSubtitle.textContent = (acta.tipoActa || "") + " - " + (acta.nombreUsuario || "");
        viewEstado.innerHTML = '<span class="badge ' + getBadgeClass(acta.estado) + '">' + estadoLabel(acta.estado) + '</span>';
        if (viewInfoBar) viewInfoBar.style.display = "";
        if (viewDocs) viewDocs.style.display = "none";
        if (downloadPdfBtn) downloadPdfBtn.style.display = "none";
        if (sendFirmaBtn) sendFirmaBtn.style.display = "none";

        viewLoading.style.display = "none";
        viewDocument.style.display = "none";
        viewNoPdf.style.display = "block";
        viewNoPdf.innerHTML = '';
        var p = document.createElement("p");
        p.style.cssText = "color:#DC2626;padding:32px 20px;text-align:center;";
        p.textContent = "Error de generación. Los documentos (PDF/ZIP) no pudieron generarse.";
        viewNoPdf.appendChild(p);

        var rolUsuario = typeof LoginService !== "undefined" && LoginService.getRol ? LoginService.getRol() : localStorage.getItem("role");
        var puedeOperar = rolUsuario === "ADMINISTRADOR" || rolUsuario === "TECNICO";
        if (!!LoginService.obtenerToken() && puedeOperar) {
            var btnRetry = document.createElement("button");
            btnRetry.className = "btn btn-primary";
            btnRetry.type = "button";
            btnRetry.style.marginTop = "8px";
            btnRetry.textContent = "Reintentar generación";
            btnRetry.addEventListener("click", function () { reintentarGeneracion(acta.id); });
            viewNoPdf.appendChild(btnRetry);
        }
    }

    /** Re-consulta el detalle cada 3s mientras la generacion este en curso. */
    function pollView(id) {
        clearTimeout(viewPollTimer);
        viewPollTimer = setTimeout(function () {
            if (!LoginService.obtenerToken()) return;
            loadById(id);
        }, 3000);
    }

    function reintentarGeneracion(id) {
        var tokenAuth = LoginService.obtenerToken();
        fetch(API_BASE + "/actas/" + id + "/reintentar-generacion", {
            method: "POST",
            headers: { "Authorization": "Bearer " + tokenAuth }
        })
            .then(function (r) {
                if (r.status === 401) {
                    LoginService.cerrarSesion();
                    window.location.href = ROUTES.LOGIN;
                    return null;
                }
                return r.json();
            })
            .then(function (body) {
                if (!body) return;
                if (body.success) {
                    showToast("Generación de documentos re-encolada.", "success");
                    loadById(id);
                } else {
                    showToast(body.mensaje || "Error al reintentar la generación.", "error");
                }
            })
            .catch(function () { showToast("Error de conexión al reintentar la generación.", "error"); });
    }

    /**
     * Expediente documental: solo las actas ENTREGA tienen varios documentos
     * asociados (acta + checklist). Para DEVOLUCION/FORMATEO (y portal
     * publico) la seccion "Documentos Asociados" NO existe: se elimina del
     * DOM — nunca se oculta por CSS ni queda contenedor vacio — y el visor
     * muestra el unico documento. La condicion es por tipoActa, no por la
     * existencia del checklist.
     */
    function setupDocsSection(acta, pdfUrl, pdfAuth) {
        var esGestion = currentActaId != null;
        var esEntrega = acta.tipoActa === "ENTREGA";
        pdfUrls = { acta: pdfUrl };

        var tieneChecklist = esGestion && esEntrega && !!acta.rutaPdfChecklist;
        if (tieneChecklist) {
            pdfUrls.checklist = API_BASE + "/actas/" + currentActaId + "/checklist/pdf";
        }

        if (esGestion && esEntrega) {
            if (viewDocs) viewDocs.style.display = "block";
        } else if (viewDocs) {
            viewDocs.remove();
        }
        if (docChecklistCard) {
            docChecklistCard.style.display = tieneChecklist ? "flex" : "none";
        }
        if (docActaCard) {
            docActaCard.style.display = "flex";
        }

        // El acta inicia activa; el checklist se muestra si el usuario lo elige.
        setActiveDoc("acta");
        loadPdfViewer(pdfUrl, pdfAuth);

        if (downloadPdfBtn) {
            downloadPdfBtn.style.display = (esGestion && !!acta.rutaPdf) ? "inline-flex" : "none";
            downloadPdfBtn.href = "#";
        }
    }

    function setActiveDoc(doc) {
        activeDoc = doc;
        if (docActaCard) docActaCard.classList.toggle("doc-card--active", doc === "acta");
        if (docChecklistCard) docChecklistCard.classList.toggle("doc-card--active", doc === "checklist");
    }

    function cargarDocAsociado(doc) {
        if (!pdfUrls[doc]) return;
        setActiveDoc(doc);
        loadPdfViewer(pdfUrls[doc], true);
    }

    function renderEvidencesGrid(evidencias, actaId) {
        if (!evidencias || evidencias.length === 0) {
            viewEvidences.style.display = "none";
            return;
        }

        var imagesToShow = [];
        for (var i = 0; i < evidencias.length; i++) {
            var ev = evidencias[i];
            if (ev.tipo === "FIRMA" || ev.tipo === "FOTO") {
                imagesToShow.push(ev);
            }
        }

        if (imagesToShow.length === 0) {
            viewEvidences.style.display = "none";
            return;
        }

        viewEvidences.style.display = "block";
        evidencesGrid.innerHTML = "";

        imagesToShow.forEach(function (e) {
            var ruta = e.tipo === "FIRMA"
                ? "/actas/" + actaId + "/firma"
                : "/actas/" + actaId + "/foto";

            var item = document.createElement("div");
            item.className = "evidence-item";

            var img = document.createElement("img");
            img.alt = e.tipo === "FIRMA" ? "Firma" : "Fotografia";
            img.loading = "lazy";
            img.onerror = function () { this.style.display = "none"; };
            item.appendChild(img);

            fetchArchivoAutenticado(API_BASE + ruta)
                .then(function (r) {
                    if (!r.ok) throw new Error("Error al cargar");
                    return r.blob();
                })
                .then(function (blob) {
                    img.src = URL.createObjectURL(blob);
                })
                .catch(function () { img.style.display = "none"; });

            var link = document.createElement("a");
            link.href = "#";
            link.textContent = e.tipo === "FIRMA" ? "Ver Firma" : "Ver Fotografia";
            link.addEventListener("click", function (evClick) {
                evClick.preventDefault();
                fetchArchivoAutenticado(API_BASE + ruta)
                    .then(function (r) {
                        if (!r.ok) throw new Error("Error al cargar");
                        return r.blob();
                    })
                    .then(function (blob) {
                        window.open(URL.createObjectURL(blob), "_blank");
                    })
                    .catch(function () { showToast("No se pudo cargar el archivo", "error"); });
            });
            item.appendChild(link);

            evidencesGrid.appendChild(item);
        });
    }

    function getActaFromUrl() {
        var params = new URLSearchParams(window.location.search);
        var id = params.get("id");
        var token = params.get("token");
        var preview = params.get("preview");
        return { id: id, token: token, preview: preview };
    }

    function loadEvidencias(actaId, callback) {
        var tokenAuth = LoginService.obtenerToken();
        if (!tokenAuth) { callback([]); return; }

        fetch(API_BASE + "/actas/" + actaId + "/evidencias", {
            headers: { "Authorization": "Bearer " + tokenAuth }
        })
            .then(function (r) { return r.json(); })
            .then(function (body) {
                if (body.success && body.data) {
                    callback(body.data);
                } else {
                    callback([]);
                }
            })
            .catch(function () { callback([]); });
    }

    function loadById(id) {
        var tokenAuth = LoginService.obtenerToken();
        if (!tokenAuth) {
            window.location.href = ROUTES.LOGIN;
            return;
        }

        fetch(API_BASE + "/actas/" + id, {
            headers: { "Authorization": "Bearer " + tokenAuth }
        })
            .then(function (r) {
                if (r.status === 401) {
                    LoginService.cerrarSesion();
                    window.location.href = ROUTES.LOGIN;
                    return null;
                }
                if (!r.ok) throw new Error("Error al cargar el acta");
                return r.json();
            })
            .then(function (body) {
                if (!body) return;
                if (!body.success) {
                    viewLoading.style.display = "none";
                    viewNoPdf.style.display = "block";
                    viewNoPdf.innerHTML = '<p style="color:#DC2626;padding:20px;text-align:center;">' + (body.mensaje || "Acta no encontrada.") + '</p>';
                    return;
                }

                var acta = body.data;

                loadEvidencias(acta.id, function (evidencias) {
                    // Gestion: el PDF se sirve con JWT validando rol/propietario.
                    renderDocument(
                        acta,
                        evidencias,
                        API_BASE + "/actas/" + acta.id + "/pdf",
                        true
                    );
                });
            })
            .catch(function (err) {
                viewLoading.style.display = "none";
                viewNoPdf.style.display = "block";
                viewNoPdf.innerHTML = '<p style="color:#DC2626;padding:20px;text-align:center;">Error: ' + err.message + '</p>';
                showToast("Error al cargar acta", "error");
            });
    }

    function openEnviarModal() {
        enviarCorreo.value = currentActaCorreo;
        enviarError.textContent = "";
        enviarError.classList.remove("visible");
        enviarModal.classList.add("open");
        enviarCorreo.focus();
    }

    function closeEnviarModal() {
        enviarModal.classList.remove("open");
    }

    function enviarActa(id, correo) {
        var tokenAuth = LoginService.obtenerToken();
        if (!tokenAuth) {
            window.location.href = ROUTES.LOGIN;
            return;
        }

        if (sendFirmaBtn) sendFirmaBtn.disabled = true;

        fetch(API_BASE + "/actas/" + id + "/enviar", {
            method: "POST",
            headers: { "Authorization": "Bearer " + tokenAuth, "Content-Type": "application/json" },
            body: JSON.stringify({ correo: correo })
        })
            .then(function (r) {
                if (r.status === 401) {
                    LoginService.cerrarSesion();
                    window.location.href = ROUTES.LOGIN;
                    return null;
                }
                return r.json();
            })
            .then(function (body) {
                if (!body) return;
                if (body.success) {
                    closeEnviarModal();
                    showToast("Acta enviada para firma exitosamente.", "success");
                    loadById(id);
                } else {
                    showToast(body.mensaje || "Error al enviar el acta.", "error");
                    if (sendFirmaBtn) sendFirmaBtn.disabled = false;
                }
            })
            .catch(function () {
                showToast("Error de conexion al enviar el acta.", "error");
                if (sendFirmaBtn) sendFirmaBtn.disabled = false;
            });
    }

    function descargarActaPdf() {
        if (currentActaId == null) return;
        var esChecklist = activeDoc === "checklist";
        var url = esChecklist
            ? API_BASE + "/actas/" + currentActaId + "/checklist/pdf"
            : API_BASE + "/actas/" + currentActaId + "/pdf";
        var nombre = esChecklist
            ? "checklist_" + currentActaId + ".pdf"
            : "acta_" + currentActaId + ".pdf";
        fetchArchivoAutenticado(url)
            .then(function (r) {
                if (!r.ok) throw new Error("Error al descargar");
                return r.blob();
            })
            .then(function (blob) {
                var urlDesc = URL.createObjectURL(blob);
                var a = document.createElement("a");
                a.href = urlDesc;
                a.download = nombre;
                document.body.appendChild(a);
                a.click();
                a.remove();
                URL.revokeObjectURL(urlDesc);
            })
            .catch(function () { showToast("No se pudo descargar el PDF", "error"); });
    }

    function confirmEnviar() {
        var correo = enviarCorreo.value.trim();
        if (!correo) {
            enviarError.textContent = "Debe ingresar un correo para el envio.";
            enviarError.classList.add("visible");
            return;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
            enviarError.textContent = "Ingrese un correo valido.";
            enviarError.classList.add("visible");
            return;
        }
        if (currentActaId != null) {
            enviarActa(currentActaId, correo);
        }
    }

    function loadPreview() {
        var previewData = localStorage.getItem("actaPreview");
        if (!previewData) {
            viewLoading.style.display = "none";
            viewNoPdf.style.display = "block";
            viewNoPdf.innerHTML = '<p style="color:#DC2626;padding:20px;text-align:center;">No hay datos de vista previa disponibles.</p>';
            return;
        }

        localStorage.removeItem("actaPreview");

        try {
            var data = JSON.parse(previewData);
            var acta = {
                id: data.id || "PREVIEW",
                tipoActa: data.tipoActa || "-",
                estado: data.estado || "GENERADA",
                nombreUsuario: data.nombreUsuario || "-",
                descripcionEquipo: data.descripcionEquipo || "-",
                fechaCreacion: data.fechaCreacion || null,
                rutaPdf: data.rutaPdf || ""
            };

            viewTitle.textContent = "Vista Previa";
            viewSubtitle.textContent = (acta.tipoActa || "") + " - " + (acta.nombreUsuario || "");
            viewInfoBar.style.display = "none";

            loadPdfViewer(acta.rutaPdf);

            renderEvidencesGrid([]);
        } catch (e) {
            viewLoading.style.display = "none";
            viewNoPdf.style.display = "block";
            viewNoPdf.innerHTML = '<p style="color:#DC2626;padding:20px;text-align:center;">Error al cargar la vista previa.</p>';
        }
    }

    function init() {
        var params = getActaFromUrl();

        if (sendFirmaBtn) {
            sendFirmaBtn.addEventListener("click", function () {
                if (currentActaId != null) openEnviarModal();
            });
        }

        if (downloadPdfBtn) {
            downloadPdfBtn.addEventListener("click", function (e) {
                e.preventDefault();
                descargarActaPdf();
            });
        }

        if (docActaCard) {
            docActaCard.addEventListener("click", function () { cargarDocAsociado("acta"); });
        }
        if (docChecklistCard) {
            docChecklistCard.addEventListener("click", function () { cargarDocAsociado("checklist"); });
        }

        if (enviarClose) enviarClose.addEventListener("click", closeEnviarModal);
        if (enviarCancel) enviarCancel.addEventListener("click", closeEnviarModal);
        if (enviarConfirm) enviarConfirm.addEventListener("click", confirmEnviar);
        if (enviarModal) {
            enviarModal.addEventListener("click", function (e) {
                if (e.target === enviarModal) closeEnviarModal();
            });
        }

        if (params.preview === "true") {
            loadPreview();
        } else if (params.token) {
            // El acceso publico por token pasa por el portal de firma con OTP;
            // acta-view ya no muestra el documento sin la segunda capa de seguridad.
            window.location.href = "firma.html?token=" + encodeURIComponent(params.token);
        } else if (params.id) {
            loadById(params.id);
        } else {
            var token = LoginService.obtenerToken();
            if (token) {
                window.location.href = "actas.html";
            } else {
                window.location.href = ROUTES.LOGIN;
            }
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
