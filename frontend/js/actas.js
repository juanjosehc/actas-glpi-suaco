(() => {
    const API_BASE = "http://localhost:8001";

    let actas = [];
    let currentActaId = null;

    const searchInput = document.getElementById("searchInput");
    const searchClear = document.getElementById("searchClear");
    const actasBody = document.getElementById("actasBody");
    const emptyState = document.getElementById("emptyState");
    const loadingOverlay = document.getElementById("loadingOverlay");
    const detailModal = document.getElementById("detailModal");
    const modalBody = document.getElementById("modalBody");
    const modalActions = document.getElementById("modalActions");
    const modalClose = document.getElementById("modalClose");

    const rejectModal = document.getElementById("rejectModal");
    const rejectReason = document.getElementById("rejectReason");
    const rejectError = document.getElementById("rejectError");
    const rejectCancel = document.getElementById("rejectCancel");
    const rejectConfirm = document.getElementById("rejectConfirm");
    const rejectClose = document.getElementById("rejectClose");

    const toastContainer = document.getElementById("toastContainer");

    // =========================
    //  AUTH
    // =========================

    function checkAuth() {
        const token = LoginService.obtenerToken();
        if (!token) {
            window.location.href = ROUTES.LOGIN;
            return null;
        }
        return token;
    }

    function handle401(resp) {
        if (resp.status === 401) {
            LoginService.cerrarSesion();
            window.location.href = ROUTES.LOGIN;
            return true;
        }
        return false;
    }

    function authHeaders() {
        const token = LoginService.obtenerToken();
        return { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };
    }

    // =========================
    //  TOAST
    // =========================

    function showToast(message, type) {
        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(() => { toast.remove(); }, 3500);
    }

    // =========================
    //  USER INFO
    // =========================

    async function loadActas() {
        const token = checkAuth();
        if (!token) return;

        loadingOverlay.classList.add("visible");
        actasBody.innerHTML = "";
        emptyState.classList.remove("visible");

        try {
            const resp = await fetch(`${API_BASE}/actas`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;

            const body = await resp.json();
            if (!body.success) {
                showToast(body.mensaje || "Error al cargar actas", "error");
                return;
            }

            actas = body.data.content || body.data || [];
            renderTable(actas);
        } catch (err) {
            if (err.message.includes("Failed to fetch")) {
                showToast("El servidor no esta disponible. Verifique la conexion.", "error");
            } else {
                showToast("Error al cargar las actas.", "error");
            }
        } finally {
            loadingOverlay.classList.remove("visible");
        }
    }

    // =========================
    //  RENDER TABLE
    // =========================

    function getBadgeClass(estado) {
        const map = { GENERADA: "badge--GENERADA", ENVIADA: "badge--ENVIADA", FIRMADA: "badge--FIRMADA", APROBADA: "badge--APROBADA", RECHAZADA: "badge--RECHAZADA" };
        return map[estado] || "badge--GENERADA";
    }

    function formatDate(dateStr) {
        if (!dateStr) return "-";
        try {
            const d = new Date(dateStr);
            return d.toLocaleDateString("es-CO", { year: "numeric", month: "2-digit", day: "2-digit" });
        } catch (_) { return dateStr; }
    }

    function renderTable(data) {
        actasBody.innerHTML = "";

        if (!data || data.length === 0) {
            emptyState.classList.add("visible");
            return;
        }

        data.forEach((a) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="cell-id">${a.id}</td>
                <td class="cell-date">${formatDate(a.fechaCreacion)}</td>
                <td>${a.nombreUsuario || "-"}</td>
                <td>${a.descripcionEquipo || "-"}</td>
                <td class="cell-tipo">${a.tipoActa || "-"}</td>
                <td><span class="badge ${getBadgeClass(a.estado)}">${a.estado || "-"}</span></td>
                <td class="cell-actions">
                    <a class="btn btn-outline btn-sm" href="acta-view.html?id=${a.id}">Ver Documento</a>
                    <button class="btn btn-outline btn-sm" data-id="${a.id}">Ver</button>
                </td>
            `;
            tr.querySelector("[data-id]").addEventListener("click", () => openDetail(a.id));
            actasBody.appendChild(tr);
        });
    }

    // =========================
    //  SEARCH (client-side)
    // =========================

    function filterActas() {
        const q = searchInput.value.toLowerCase().trim();
        searchClear.classList.toggle("visible", q.length > 0);

        if (!q) {
            renderTable(actas);
            return;
        }

        const filtered = actas.filter((a) => {
            const id = String(a.id);
            const user = (a.nombreUsuario || "").toLowerCase();
            const equipo = (a.descripcionEquipo || "").toLowerCase();
            const estado = (a.estado || "").toLowerCase();
            return id.includes(q) || user.includes(q) || equipo.includes(q) || estado.includes(q);
        });
        renderTable(filtered);
    }

    searchInput.addEventListener("input", filterActas);
    searchClear.addEventListener("click", () => {
        searchInput.value = "";
        searchClear.classList.remove("visible");
        renderTable(actas);
        searchInput.focus();
    });

    // =========================
    //  DETAIL MODAL
    // =========================

    async function openDetail(id) {
        const token = checkAuth();
        if (!token) return;

        modalBody.innerHTML = '<div class="loading-spinner" style="margin:40px auto"></div>';
        modalActions.innerHTML = "";
        detailModal.classList.add("open");

        try {
            const resp = await fetch(`${API_BASE}/actas/${id}`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;

            const body = await resp.json();
            if (!body.success) {
                modalBody.innerHTML = `<p class="modal-desc">${body.mensaje || "Acta no encontrada."}</p>`;
                return;
            }

            const a = body.data;
            renderDetail(a);
        } catch (_) {
            modalBody.innerHTML = '<p class="modal-desc">Error al cargar el detalle del acta.</p>';
        }
    }

    function renderDetail(a) {
        const estado = a.estado || "";
        const badgeClass = getBadgeClass(estado);

        let htmlContent = "";
        if (a.contenidoHtml) {
            htmlContent = `
                <hr class="detail-divider">
                <div class="detail-field full">
                    <span class="detail-label">Contenido HTML</span>
                    <div class="detail-html">${a.contenidoHtml}</div>
                </div>`;
        }

        modalBody.innerHTML = `
            <div class="detail-grid">
                <div class="detail-field">
                    <span class="detail-label">ID</span>
                    <span class="detail-value">${a.id}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Estado</span>
                    <span class="badge ${badgeClass}">${estado}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Tipo Acta</span>
                    <span class="detail-value">${a.tipoActa || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Fecha Creacion</span>
                    <span class="detail-value">${formatDate(a.fechaCreacion)}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Usuario</span>
                    <span class="detail-value">${a.nombreUsuario || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Cedula</span>
                    <span class="detail-value">${a.cedulaUsuario || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Correo</span>
                    <span class="detail-value">${a.correoUsuario || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Ticket GLPI</span>
                    <span class="detail-value">${a.ticketGlpi || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Equipo</span>
                    <span class="detail-value">${a.descripcionEquipo || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Serial</span>
                    <span class="detail-value">${a.serialEquipo || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Placa</span>
                    <span class="detail-value">${a.placaEquipo || "-"}</span>
                </div>
                <div class="detail-field">
                    <span class="detail-label">Fecha Rechazo</span>
                    <span class="detail-value">${formatDate(a.fechaRechazo)}</span>
                </div>
                <div class="detail-field full">
                    <span class="detail-label">Observacion Rechazo</span>
                    <span class="detail-value">${a.observacionRechazo || "-"}</span>
                </div>
                ${htmlContent}
            </div>`;

        renderActions(a);
    }

    function renderActions(a) {
        modalActions.innerHTML = "";

        const btnDoc = document.createElement("a");
        btnDoc.className = "btn btn-outline";
        btnDoc.href = `acta-view.html?id=${a.id}`;
        btnDoc.textContent = "Ver Documento";
        modalActions.appendChild(btnDoc);

        if (a.estado === "GENERADA") {
            const btn = document.createElement("button");
            btn.className = "btn btn-primary";
            btn.textContent = "Enviar a Firma";
            btn.addEventListener("click", () => enviarActa(a.id));
            modalActions.appendChild(btn);
        }

        if (a.estado === "FIRMADA") {
            const btnApr = document.createElement("button");
            btnApr.className = "btn btn-success";
            btnApr.textContent = "Aprobar";
            btnApr.addEventListener("click", () => aprobarActa(a.id));
            modalActions.appendChild(btnApr);

            const btnRec = document.createElement("button");
            btnRec.className = "btn btn-danger";
            btnRec.textContent = "Rechazar";
            btnRec.addEventListener("click", () => openRejectModal(a.id));
            modalActions.appendChild(btnRec);
        }

        if (a.estado !== "GENERADA" && a.estado !== "FIRMADA") {
            const span = document.createElement("span");
            span.className = "modal-desc";
            span.textContent = "No hay acciones disponibles para este estado.";
            span.style.margin = "0";
            modalActions.appendChild(span);
        }
    }

    // =========================
    //  ENVIAR
    // =========================

    async function enviarActa(id) {
        const token = checkAuth();
        if (!token) return;
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/enviar`, { method: "POST", headers: authHeaders() });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast("Acta enviada para firma exitosamente.", "success");
                closeDetailModal();
                loadActas();
            } else {
                showToast(body.mensaje || "Error al enviar el acta.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al enviar el acta.", "error");
        }
    }

    // =========================
    //  APROBAR
    // =========================

    async function aprobarActa(id) {
        const token = checkAuth();
        if (!token) return;
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/aprobar`, { method: "POST", headers: authHeaders() });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast("Acta aprobada exitosamente.", "success");
                closeDetailModal();
                loadActas();
            } else {
                showToast(body.mensaje || "Error al aprobar el acta.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al aprobar el acta.", "error");
        }
    }

    // =========================
    //  RECHAZAR
    // =========================

    function openRejectModal(id) {
        currentActaId = id;
        rejectReason.value = "";
        rejectError.textContent = "";
        rejectError.classList.remove("visible");
        rejectModal.classList.add("open");
    }

    async function confirmReject() {
        const obs = rejectReason.value.trim();
        if (!obs) {
            rejectError.textContent = "Debe ingresar una observacion.";
            rejectError.classList.add("visible");
            return;
        }

        const token = checkAuth();
        if (!token) return;

        try {
            const resp = await fetch(`${API_BASE}/actas/${currentActaId}/rechazar`, {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ observacion: obs }),
            });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast("Acta rechazada exitosamente.", "success");
                closeRejectModal();
                closeDetailModal();
                loadActas();
            } else {
                showToast(body.mensaje || "Error al rechazar el acta.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al rechazar el acta.", "error");
        }
    }

    function closeRejectModal() {
        rejectModal.classList.remove("open");
        currentActaId = null;
    }

    // =========================
    //  MODAL HELPERS
    // =========================

    function closeDetailModal() {
        detailModal.classList.remove("open");
    }

    modalClose.addEventListener("click", closeDetailModal);
    detailModal.addEventListener("click", (e) => { if (e.target === detailModal) closeDetailModal(); });

    rejectClose.addEventListener("click", closeRejectModal);
    rejectCancel.addEventListener("click", closeRejectModal);
    rejectConfirm.addEventListener("click", confirmReject);
    rejectModal.addEventListener("click", (e) => { if (e.target === rejectModal) closeRejectModal(); });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            if (rejectModal.classList.contains("open")) closeRejectModal();
            else closeDetailModal();
        }
    });

    (async function init() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return; }
        await loadActas();
    })();
})();
