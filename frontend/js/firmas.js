(() => {
    const API_BASE = "http://localhost:8001";
    const PAGE_SIZE = 10;
    const BASE_URL = window.location.origin + window.location.pathname.replace(/[^/]*$/, "");

    let allActas = [];
    let filteredActas = [];
    let currentPage = 1;
    let currentActaId = null;

    const $ = (id) => document.getElementById(id);

    const searchInput = $("searchInput");
    const searchClear = $("searchClear");
    const firmasBody = $("firmasBody");
    const emptyState = $("emptyState");
    const loadingOverlay = $("loadingOverlay");
    const paginationBar = $("paginationBar");
    const paginationInfo = $("paginationInfo");
    const paginationCurrent = $("paginationCurrent");
    const btnPrev = $("btnPrev");
    const btnNext = $("btnNext");
    const detailModal = $("detailModal");
    const detailModalBody = $("detailModalBody");
    const detailModalActions = $("detailModalActions");
    const detailModalClose = $("detailModalClose");

    const sendLinkModal = $("sendLinkModal");
    const sendLinkModalClose = $("sendLinkModalClose");
    const linkUrl = $("linkUrl");
    const btnCopyLink = $("btnCopyLink");
    const linkHint = $("linkHint");

    const evidenceModal = $("evidenceModal");
    const evidenceModalClose = $("evidenceModalClose");
    const evidenceModalBody = $("evidenceModalBody");

    const rejectModal = $("rejectModal");
    const rejectReason = $("rejectReason");
    const rejectError = $("rejectError");
    const rejectModalClose = $("rejectModalClose");
    const rejectModalCancel = $("rejectModalCancel");
    const rejectModalConfirm = $("rejectModalConfirm");

    const toastContainer = $("toastContainer");

    // =========================
    //  AUTH
    // =========================

    function checkAuth() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return null; }
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
        setTimeout(() => toast.remove(), 3500);
    }

    // =========================
    //  LOADING
    // =========================

    function setLoading(show, text) {
        if (show) {
            loadingOverlay.querySelector(".loading-text").textContent = text || "Cargando...";
            loadingOverlay.classList.add("visible");
        } else {
            loadingOverlay.classList.remove("visible");
        }
    }

    // =========================
    //  USER INFO
    // =========================

    async function loadActas() {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Cargando actas...");
        try {
            const resp = await fetch(`${API_BASE}/actas`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) { showToast(body.mensaje || "Error al cargar actas", "error"); return; }
            allActas = body.data.content || body.data || [];
            filteredActas = [...allActas];
            currentPage = 1;
            render();
        } catch (_) {
            showToast("El servidor no esta disponible.", "error");
        } finally {
            setLoading(false);
        }
    }

    // =========================
    //  RENDER
    // =========================

    function render() {
        const start = (currentPage - 1) * PAGE_SIZE;
        const page = filteredActas.slice(start, start + PAGE_SIZE);
        renderTable(page);
        renderPagination();
    }

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
        firmasBody.innerHTML = "";
        if (!data || data.length === 0) {
            emptyState.classList.add("visible");
            paginationBar.style.display = "none";
            return;
        }
        emptyState.classList.remove("visible");
        paginationBar.style.display = "flex";

        data.forEach((a) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="cell-id">${a.id}</td>
                <td>${formatDate(a.fechaCreacion)}</td>
                <td>${a.nombreUsuario || "-"}</td>
                <td>${a.descripcionEquipo || "-"}</td>
                <td>${a.tipoActa || "-"}</td>
                <td><span class="badge ${getBadgeClass(a.estado)}">${a.estado || "-"}</span></td>
                <td class="cell-actions" data-id="${a.id}"></td>
            `;
            const actions = tr.querySelector(".cell-actions");
            actions.appendChild(actionBtn("Ver", "btn-outline", () => openDetail(a.id)));

            if (a.estado === "GENERADA") {
                actions.appendChild(actionBtn("Enviar", "btn-primary", () => enviarActa(a.id)));
            }
            if (a.estado === "ENVIADA") {
                actions.appendChild(actionBtn("Enlace", "btn-outline", () => openLinkModal(a)));
            }
            if (a.estado === "FIRMADA") {
                actions.appendChild(actionBtn("Aprobar", "btn-success", () => aprobarActa(a.id)));
                actions.appendChild(actionBtn("Rechazar", "btn-danger", () => openRejectModal(a.id)));
            }
            if (a.estado === "FIRMADA" || a.estado === "APROBADA" || a.estado === "RECHAZADA") {
                actions.appendChild(actionBtn("Evidencias", "btn-outline", () => openEvidencias(a.id)));
            }

            firmasBody.appendChild(tr);
        });
    }

    function actionBtn(label, cls, onClick) {
        const btn = document.createElement("button");
        btn.className = `btn ${cls} btn-sm`;
        btn.textContent = label;
        btn.addEventListener("click", (e) => { e.stopPropagation(); onClick(); });
        return btn;
    }

    function renderPagination() {
        const total = filteredActas.length;
        const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;
        paginationCurrent.textContent = currentPage;
        const start = (currentPage - 1) * PAGE_SIZE + 1;
        const end = Math.min(currentPage * PAGE_SIZE, total);
        paginationInfo.textContent = total > 0 ? `${start}-${end} de ${total}` : "0 resultados";
        btnPrev.disabled = currentPage <= 1;
        btnNext.disabled = currentPage >= totalPages;
    }

    // =========================
    //  SEARCH
    // =========================

    function filterActas() {
        const q = searchInput.value.toLowerCase().trim();
        searchClear.classList.toggle("visible", q.length > 0);
        const isNumeric = /^\d+$/.test(q);
        filteredActas = q ? allActas.filter((a) => {
            if (isNumeric && String(a.id).includes(q)) return true;
            if ((a.nombreUsuario || "").toLowerCase().includes(q)) return true;
            if ((a.descripcionEquipo || "").toLowerCase().includes(q)) return true;
            if ((a.estado || "").toLowerCase().includes(q)) return true;
            return false;
        }) : [...allActas];
        currentPage = 1;
        render();
    }

    searchInput.addEventListener("input", filterActas);
    searchClear.addEventListener("click", () => { searchInput.value = ""; searchClear.classList.remove("visible"); filterActas(); searchInput.focus(); });
    btnPrev.addEventListener("click", () => { if (currentPage > 1) { currentPage--; render(); } });
    btnNext.addEventListener("click", () => { const max = Math.ceil(filteredActas.length / PAGE_SIZE); if (currentPage < max) { currentPage++; render(); } });

    // =========================
    //  DETAIL MODAL
    // =========================

    async function openDetail(id) {
        const token = checkAuth();
        if (!token) return;
        detailModalBody.innerHTML = '<div class="loading-spinner" style="margin:40px auto"></div>';
        detailModalActions.innerHTML = "";
        detailModal.classList.add("open");
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) {
                detailModalBody.innerHTML = `<p class="modal-desc">${body.mensaje || "Acta no encontrada."}</p>`;
                return;
            }
            renderDetail(body.data);
        } catch (_) {
            detailModalBody.innerHTML = '<p class="modal-desc">Error al cargar el detalle del acta.</p>';
        }
    }

    function renderDetail(a) {
        const badgeClass = getBadgeClass(a.estado);
        let htmlContent = "";
        if (a.contenidoHtml) {
            htmlContent = `
                <hr class="detail-divider">
                <div class="detail-field full">
                    <span class="detail-label">Contenido HTML</span>
                    <div class="detail-html">${a.contenidoHtml}</div>
                </div>`;
        }
        detailModalBody.innerHTML = `
            <div class="detail-grid">
                <div class="detail-field"><span class="detail-label">ID</span><span class="detail-value">${a.id}</span></div>
                <div class="detail-field"><span class="detail-label">Estado</span><span class="badge ${badgeClass}">${a.estado || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Tipo Acta</span><span class="detail-value">${a.tipoActa || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Ticket GLPI</span><span class="detail-value">${a.ticketGlpi != null ? a.ticketGlpi : "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Usuario</span><span class="detail-value">${a.nombreUsuario || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Cedula</span><span class="detail-value">${a.cedulaUsuario || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Correo</span><span class="detail-value">${a.correoUsuario || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Equipo</span><span class="detail-value">${a.descripcionEquipo || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Serial</span><span class="detail-value">${a.serialEquipo || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Placa</span><span class="detail-value">${a.placaEquipo || "-"}</span></div>
                <div class="detail-field"><span class="detail-label">Fecha Creacion</span><span class="detail-value">${formatDate(a.fechaCreacion)}</span></div>
                <div class="detail-field"><span class="detail-label">Fecha Envio</span><span class="detail-value">${formatDate(a.fechaEnvio)}</span></div>
                <div class="detail-field"><span class="detail-label">Fecha Firma</span><span class="detail-value">${formatDate(a.fechaFirma)}</span></div>
                <div class="detail-field"><span class="detail-label">Fecha Aprobacion</span><span class="detail-value">${formatDate(a.fechaAprobacion)}</span></div>
                <div class="detail-field"><span class="detail-label">Fecha Rechazo</span><span class="detail-value">${formatDate(a.fechaRechazo)}</span></div>
                <div class="detail-field full"><span class="detail-label">Observacion Rechazo</span><span class="detail-value">${a.observacionRechazo || "-"}</span></div>
                ${htmlContent}
            </div>`;
        const btnDoc = document.createElement("a");
        btnDoc.className = "btn btn-outline";
        btnDoc.href = `acta-view.html?id=${a.id}`;
        btnDoc.textContent = "Ver Documento";
        detailModalActions.appendChild(btnDoc);
    }

    detailModalClose.addEventListener("click", () => detailModal.classList.remove("open"));
    detailModal.addEventListener("click", (e) => { if (e.target === detailModal) detailModal.classList.remove("open"); });

    // =========================
    //  SEND TO FIRMA
    // =========================

    async function enviarActa(id) {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Enviando a firma...");
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/enviar`, { method: "POST", headers: authHeaders() });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                const data = body.data;
                const url = BASE_URL + "firma.html?token=" + (data.token || "");
                linkUrl.value = url;
                linkHint.classList.remove("visible");
                sendLinkModal.classList.add("open");
                await loadActas();
            } else {
                showToast(body.mensaje || "Error al enviar a firma.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al enviar a firma.", "error");
        } finally {
            setLoading(false);
        }
    }

    // =========================
    //  LINK MODAL
    // =========================

    function openLinkModal(a) {
        const url = BASE_URL + "firma.html?token=" + (a.tokenFirma || "");
        linkUrl.value = url;
        linkHint.classList.remove("visible");
        sendLinkModal.classList.add("open");
    }

    btnCopyLink.addEventListener("click", async () => {
        try {
            await navigator.clipboard.writeText(linkUrl.value);
            linkHint.classList.add("visible");
        } catch (_) {
            linkUrl.select();
            document.execCommand("copy");
            linkHint.classList.add("visible");
        }
    });

    sendLinkModalClose.addEventListener("click", () => sendLinkModal.classList.remove("open"));
    sendLinkModal.addEventListener("click", (e) => { if (e.target === sendLinkModal) sendLinkModal.classList.remove("open"); });

    // =========================
    //  EVIDENCES
    // =========================

    async function openEvidencias(id) {
        const token = checkAuth();
        if (!token) return;
        evidenceModalBody.innerHTML = '<div class="loading-spinner" style="margin:40px auto"></div>';
        evidenceModal.classList.add("open");
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/evidencias`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success || !body.data || body.data.length === 0) {
                evidenceModalBody.innerHTML = '<p class="modal-desc">No hay evidencias registradas para esta acta.</p>';
                return;
            }
            const labelMap = { FIRMA: "Firma Digital", FOTO: "Foto de Verificacion", PDF_FINAL: "PDF Final" };
            const iconMap = { FIRMA: "firma", FOTO: "foto", PDF_FINAL: "pdf" };
            const list = document.createElement("div");
            list.className = "evidence-list";
            body.data.forEach((ev) => {
                const tipo = ev.tipo || "";
                const label = labelMap[tipo] || tipo;
                const fileUrl = `${API_BASE}/${ev.rutaArchivo}`;
                const item = document.createElement("div");
                item.className = "evidence-item";
                item.innerHTML = `
                    <div class="evidence-info">
                        <span class="evidence-type">${label}</span>
                        <span class="evidence-path">${ev.rutaArchivo || "-"}</span>
                    </div>
                    <a class="btn btn-outline btn-sm" href="${fileUrl}" target="_blank" download>Ver</a>
                `;
                list.appendChild(item);
            });
            evidenceModalBody.innerHTML = "";
            evidenceModalBody.appendChild(list);
        } catch (_) {
            evidenceModalBody.innerHTML = '<p class="modal-desc">Error al cargar evidencias.</p>';
        }
    }

    evidenceModalClose.addEventListener("click", () => evidenceModal.classList.remove("open"));
    evidenceModal.addEventListener("click", (e) => { if (e.target === evidenceModal) evidenceModal.classList.remove("open"); });

    // =========================
    //  APROBAR
    // =========================

    async function aprobarActa(id) {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Aprobando acta...");
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/aprobar`, { method: "POST", headers: authHeaders() });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast("Acta aprobada exitosamente.", "success");
                await loadActas();
            } else {
                showToast(body.mensaje || "Error al aprobar el acta.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al aprobar.", "error");
        } finally {
            setLoading(false);
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
        setLoading(true, "Rechazando acta...");
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
                await loadActas();
            } else {
                showToast(body.mensaje || "Error al rechazar el acta.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al rechazar.", "error");
        } finally {
            setLoading(false);
        }
    }

    function closeRejectModal() {
        rejectModal.classList.remove("open");
        currentActaId = null;
    }

    rejectModalClose.addEventListener("click", closeRejectModal);
    rejectModalCancel.addEventListener("click", closeRejectModal);
    rejectModalConfirm.addEventListener("click", confirmReject);
    rejectModal.addEventListener("click", (e) => { if (e.target === rejectModal) closeRejectModal(); });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            if (rejectModal.classList.contains("open")) closeRejectModal();
            else if (detailModal.classList.contains("open")) detailModal.classList.remove("open");
            else if (sendLinkModal.classList.contains("open")) sendLinkModal.classList.remove("open");
            else if (evidenceModal.classList.contains("open")) evidenceModal.classList.remove("open");
        }
    });

    // =========================
    //  SIDEBAR / LOGOUT
    // =========================

    (async function init() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return; }
        await loadActas();
    })();
})();
