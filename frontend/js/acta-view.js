(function () {
    var API_BASE = "http://localhost:8001";

    var viewDocument = document.getElementById("viewDocument");
    var viewTitle = document.getElementById("viewTitle");
    var viewSubtitle = document.getElementById("viewSubtitle");
    var viewEstado = document.getElementById("viewEstado");
    var viewTipo = document.getElementById("viewTipo");
    var viewUsuario = document.getElementById("viewUsuario");
    var viewEquipo = document.getElementById("viewEquipo");
    var viewFecha = document.getElementById("viewFecha");
    var viewEvidences = document.getElementById("viewEvidences");
    var evidencesGrid = document.getElementById("evidencesGrid");
    var toastContainer = document.getElementById("toastContainer");
    var viewInfoBar = document.getElementById("viewInfoBar");
    var downloadPdfBtn = document.getElementById("downloadPdfBtn");

    function showToast(message, type) {
        var toast = document.createElement("div");
        toast.className = "toast toast-" + type;
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(function () { toast.remove(); }, 3500);
    }

    function getBadgeClass(estado) {
        var map = {
            "GENERADA": "badge--GENERADA",
            "ENVIADA": "badge--ENVIADA",
            "FIRMADA": "badge--FIRMADA",
            "APROBADA": "badge--APROBADA",
            "RECHAZADA": "badge--RECHAZADA"
        };
        return map[estado] || "";
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

    function embedEvidencesInDocument(html, acta, evidencias) {
        var firmaUrl = null;
        var fotoUrl = null;

        if (evidencias && evidencias.length > 0) {
            for (var i = 0; i < evidencias.length; i++) {
                var ev = evidencias[i];
                if (ev.tipo === "FIRMA") firmaUrl = ev.rutaArchivo;
                if (ev.tipo === "FOTO") fotoUrl = ev.rutaArchivo;
            }
        }

        var estadoFirmado = acta.estado === "FIRMADA" || acta.estado === "APROBADA" || acta.estado === "RECHAZADA";

        var signatureHtml = "";
        if (estadoFirmado && (firmaUrl || fotoUrl || acta.fechaFirma)) {
            signatureHtml += '<div class="acta-evidences-section">';
            signatureHtml += '<h2 class="acta-subsection">EVIDENCIAS DE FIRMA</h2>';

            if (acta.fechaFirma) {
                signatureHtml += '<p class="acta-evidence-date">Fecha de firma: <strong>' + formatDateTime(acta.fechaFirma) + '</strong></p>';
            }

            signatureHtml += '<div class="acta-evidences-row">';

            if (firmaUrl) {
                signatureHtml += '<div class="acta-evidence-box">';
                signatureHtml += '<span class="acta-evidence-label">FIRMA DEL COLABORADOR</span>';
                signatureHtml += '<div class="acta-evidence-image-wrapper">';
                signatureHtml += '<img src="' + API_BASE + '/' + firmaUrl + '" alt="Firma" class="acta-evidence-img acta-evidence-img--firma" />';
                signatureHtml += '</div>';
                signatureHtml += '</div>';
            }

            if (fotoUrl) {
                signatureHtml += '<div class="acta-evidence-box">';
                signatureHtml += '<span class="acta-evidence-label">FOTOGRAFIA DEL COLABORADOR</span>';
                signatureHtml += '<div class="acta-evidence-image-wrapper">';
                signatureHtml += '<img src="' + API_BASE + '/' + fotoUrl + '" alt="Foto" class="acta-evidence-img acta-evidence-img--foto" />';
                signatureHtml += '</div>';
                signatureHtml += '</div>';
            }

            signatureHtml += '</div>';
            signatureHtml += '</div>';
        }

        if (!signatureHtml) return html;

        var footerIndex = html.lastIndexOf('<div class="acta-footer"');
        var watermarkIndex = html.lastIndexOf('<div class="acta-watermark"');

        if (footerIndex !== -1) {
            return html.slice(0, footerIndex) + signatureHtml + html.slice(footerIndex);
        } else if (watermarkIndex !== -1) {
            return html.slice(0, watermarkIndex) + signatureHtml + html.slice(watermarkIndex);
        } else {
            return html + signatureHtml;
        }
    }

    function renderDocument(acta, evidencias) {
        viewDocument.innerHTML = "";

        if (acta.contenidoHtml) {
            var finalHtml = embedEvidencesInDocument(acta.contenidoHtml, acta, evidencias);
            var div = document.createElement("div");
            div.innerHTML = finalHtml;
            viewDocument.appendChild(div);
        } else {
            viewDocument.innerHTML = '<div class="acta-document"><p style="color:#64748B;padding:20px;text-align:center;">No hay contenido HTML disponible para esta acta.</p></div>';
        }

        var estadoBadge = '<span class="badge ' + getBadgeClass(acta.estado) + '">' + acta.estado + '</span>';
        viewEstado.innerHTML = estadoBadge;
        viewTipo.textContent = acta.tipoActa || "-";
        viewUsuario.textContent = acta.nombreUsuario || "-";
        viewEquipo.textContent = acta.descripcionEquipo || "-";

        var fecha = acta.fechaCreacion ? formatDate(acta.fechaCreacion) : "-";
        viewFecha.textContent = fecha;

        viewTitle.textContent = "Acta #" + acta.id;
        viewSubtitle.textContent = (acta.tipoActa || "") + " - " + (acta.nombreUsuario || "");

        if (downloadPdfBtn && acta.rutaPdf && acta.estado === "APROBADA") {
            downloadPdfBtn.style.display = "inline-flex";
            downloadPdfBtn.href = API_BASE + "/" + acta.rutaPdf;
        } else if (downloadPdfBtn) {
            downloadPdfBtn.style.display = "none";
        }

        renderEvidencesGrid(evidencias);
    }

    function renderEvidencesGrid(evidencias) {
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

        for (var j = 0; j < imagesToShow.length; j++) {
            var e = imagesToShow[j];
            var item = document.createElement("div");
            item.className = "evidence-item";

            var img = document.createElement("img");
            img.src = API_BASE + "/" + e.rutaArchivo;
            img.alt = e.tipo === "FIRMA" ? "Firma" : "Fotografia";
            img.loading = "lazy";
            img.onerror = function () { this.style.display = "none"; };
            item.appendChild(img);

            var link = document.createElement("a");
            link.href = API_BASE + "/" + e.rutaArchivo;
            link.target = "_blank";
            link.textContent = e.tipo === "FIRMA" ? "Ver Firma" : "Ver Fotografia";
            item.appendChild(link);

            evidencesGrid.appendChild(item);
        }
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

    function loadByToken(token) {
        fetch(API_BASE + "/firma/" + encodeURIComponent(token))
            .then(function (r) {
                if (!r.ok) throw new Error("Token invalido o expirado");
                return r.json();
            })
            .then(function (data) {
                var acta = data.data || data;
                var evidencias = data.evidencias || [];
                renderDocument(acta, evidencias);
            })
            .catch(function (err) {
                viewDocument.innerHTML = '<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">Error: ' + err.message + '</p></div>';
            });
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
                    viewDocument.innerHTML = '<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">' + (body.mensaje || "Acta no encontrada.") + '</p></div>';
                    return;
                }

                var acta = body.data;

                loadEvidencias(acta.id, function (evidencias) {
                    renderDocument(acta, evidencias);
                });
            })
            .catch(function (err) {
                viewDocument.innerHTML = '<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">Error: ' + err.message + '</p></div>';
                showToast("Error al cargar acta", "error");
            });
    }

    function loadPreview() {
        var previewData = localStorage.getItem("actaPreview");
        if (!previewData) {
            viewDocument.innerHTML = '<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">No hay datos de vista previa disponibles.</p></div>';
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
                contenidoHtml: data.contenidoHtml || ""
            };

            viewTitle.textContent = "Vista Previa";
            viewSubtitle.textContent = (acta.tipoActa || "") + " - " + (acta.nombreUsuario || "");
            viewInfoBar.style.display = "none";

            renderDocument(acta, []);
        } catch (e) {
            viewDocument.innerHTML = '<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">Error al cargar la vista previa.</p></div>';
        }
    }

    function init() {
        var params = getActaFromUrl();

        if (params.preview === "true") {
            loadPreview();
        } else if (params.token) {
            loadByToken(params.token);
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
