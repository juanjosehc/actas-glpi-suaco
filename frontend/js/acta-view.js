(function() {
    const API_BASE = "http://localhost:8001";

    const viewDocument = document.getElementById("viewDocument");
    const viewTitle = document.getElementById("viewTitle");
    const viewSubtitle = document.getElementById("viewSubtitle");
    const viewEstado = document.getElementById("viewEstado");
    const viewTipo = document.getElementById("viewTipo");
    const viewUsuario = document.getElementById("viewUsuario");
    const viewEquipo = document.getElementById("viewEquipo");
    const viewFecha = document.getElementById("viewFecha");
    const viewEvidences = document.getElementById("viewEvidences");
    const evidencesGrid = document.getElementById("evidencesGrid");
    const toastContainer = document.getElementById("toastContainer");

    function showToast(message, type) {
        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(() => { toast.remove(); }, 3500);
    }

    function getBadgeClass(estado) {
        const map = {
            "GENERADA": "badge--GENERADA",
            "ENVIADA": "badge--ENVIADA",
            "FIRMADA": "badge--FIRMADA",
            "APROBADA": "badge--APROBADA",
            "RECHAZADA": "badge--RECHAZADA"
        };
        return map[estado] || "";
    }

    function renderDocument(acta) {
        viewDocument.innerHTML = "";

        if (acta.contenidoHtml) {
            const div = document.createElement("div");
            div.innerHTML = acta.contenidoHtml;
            viewDocument.appendChild(div);
        } else {
            viewDocument.innerHTML = '<div class="acta-document"><p style="color:#64748B;padding:20px;text-align:center;">No hay contenido HTML disponible para esta acta.</p></div>';
        }

        const estadoBadge = `<span class="badge ${getBadgeClass(acta.estado)}">${acta.estado}</span>`;
        viewEstado.innerHTML = estadoBadge;
        viewTipo.textContent = acta.tipoActa || "-";
        viewUsuario.textContent = acta.nombreUsuario || "-";
        viewEquipo.textContent = acta.descripcionEquipo || "-";

        const fecha = acta.fechaCreacion ? new Date(acta.fechaCreacion).toLocaleDateString("es-CO") : "-";
        viewFecha.textContent = fecha;

        viewTitle.textContent = `Acta #${acta.id}`;
        viewSubtitle.textContent = `${acta.tipoActa || ""} - ${acta.nombreUsuario || ""}`;

        renderEvidences(acta);
    }

    function renderEvidences(acta) {
        const evidencias = [];

        if (acta.rutaFirma) evidencias.push({ label: "Firma", url: acta.rutaFirma, type: "image" });
        if (acta.rutaFoto) evidencias.push({ label: "Foto", url: acta.rutaFoto, type: "image" });
        if (acta.rutaPdf) evidencias.push({ label: "PDF", url: acta.rutaPdf, type: "pdf" });

        if (evidencias.length === 0) {
            viewEvidences.style.display = "none";
            return;
        }

        viewEvidences.style.display = "block";
        evidencesGrid.innerHTML = "";

        evidencias.forEach(ev => {
            const item = document.createElement("div");
            item.className = "evidence-item";

            if (ev.type === "image") {
                const img = document.createElement("img");
                img.src = `${API_BASE}${ev.url}`;
                img.alt = ev.label;
                img.loading = "lazy";
                img.onerror = () => { img.style.display = "none"; };
                item.appendChild(img);
            }

            const link = document.createElement("a");
            link.href = `${API_BASE}${ev.url}`;
            link.target = "_blank";
            link.textContent = ev.label;
            item.appendChild(link);

            evidencesGrid.appendChild(item);
        });
    }

    function getActaFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const id = params.get("id");
        const token = params.get("token");
        return { id, token };
    }

    function fetchActa() {
        const { id, token } = getActaFromUrl();

        if (!id && !token) {
            viewDocument.innerHTML = '<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">Error: No se especifico el ID del acta ni el token de firma.</p></div>';
            return;
        }

        if (token) {
            fetch(`${API_BASE}/firma/${encodeURIComponent(token)}`)
                .then(r => {
                    if (!r.ok) throw new Error("Token invalido o expirado");
                    return r.json();
                })
                .then(data => {
                    renderDocument(data);
                })
                .catch(err => {
                    viewDocument.innerHTML = `<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">Error: ${err.message}</p></div>`;
                });
        } else {
            const tokenAuth = LoginService.obtenerToken();
            if (!tokenAuth) {
                window.location.href = ROUTES.LOGIN;
                return;
            }

            fetch(`${API_BASE}/actas/${id}`, {
                headers: { "Authorization": `Bearer ${tokenAuth}` }
            })
                .then(r => {
                    if (r.status === 401) {
                        LoginService.cerrarSesion();
                        window.location.href = ROUTES.LOGIN;
                        return;
                    }
                    if (!r.ok) throw new Error("Error al cargar el acta");
                    return r.json();
                })
                .then(acta => {
                    if (acta) renderDocument(acta);
                })
                .catch(err => {
                    viewDocument.innerHTML = `<div class="acta-document"><p style="color:#DC2626;padding:20px;text-align:center;">Error: ${err.message}</p></div>`;
                    showToast("Error al cargar acta", "error");
                });
        }
    }

    function init() {
        const token = LoginService.obtenerToken();
        const { id, token: urlToken } = getActaFromUrl();

        if (urlToken) {
            fetchActa();
        } else if (token) {
            fetchActa();
        } else {
            window.location.href = ROUTES.LOGIN;
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
