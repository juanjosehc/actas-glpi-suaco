(() => {
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

    const enviarModal = $("enviarModal");
    const enviarCorreo = $("enviarCorreo");
    const enviarError = $("enviarError");
    const enviarCancel = $("enviarCancel");
    const enviarConfirm = $("enviarConfirm");
    const enviarClose = $("enviarClose");

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
    //  ROL — solo lectura para AUDITOR
    //  El backend tambien bloquea los POST operativos (403). Aqui se ocultan
    //  los botones para que un auditor no vea acciones que no puede ejecutar.
    // =========================

    const ROL_USUARIO = (typeof LoginService !== "undefined" && LoginService.getRol ? LoginService.getRol() : localStorage.getItem("role")) || "";
    const PUEDE_OPERAR = ROL_USUARIO === "ADMINISTRADOR" || ROL_USUARIO === "TECNICO";

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

            if (a.estado === "GENERADA" && PUEDE_OPERAR) {
                actions.appendChild(actionBtn("Enviar", "btn-primary", () => openEnviarModal(a)));
            }
            if (a.estado === "ENVIADA") {
                actions.appendChild(actionBtn("Enlace", "btn-outline", () => openLinkModal(a)));
            }
            if (a.estado === "FIRMADA" && PUEDE_OPERAR) {
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
                ${a.tipoActa === "DEVOLUCION" ? `<div class="detail-field"><span class="detail-label">Cedula</span><span class="detail-value">${a.cedulaUsuario || "-"}</span></div>` : ""}
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

    function openEnviarModal(a) {
        currentActaId = a.id;
        enviarCorreo.value = a.correoUsuario || "";
        enviarError.textContent = "";
        enviarError.classList.remove("visible");
        enviarModal.classList.add("open");
        enviarCorreo.focus();
    }

    function closeEnviarModal() {
        enviarModal.classList.remove("open");
    }

    async function confirmEnviar() {
        const correo = enviarCorreo.value.trim();
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
        await enviarActa(currentActaId, correo);
    }

    async function enviarActa(id, correo) {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Enviando a firma...");
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/enviar`, {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ correo })
            });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                closeEnviarModal();
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

    enviarClose.addEventListener("click", closeEnviarModal);
    enviarCancel.addEventListener("click", closeEnviarModal);
    enviarConfirm.addEventListener("click", confirmEnviar);
    enviarModal.addEventListener("click", (e) => { if (e.target === enviarModal) closeEnviarModal(); });

    // =========================
    //  EVIDENCES
    // =========================

    // Abre la evidencia en una pestana (mismo mecanismo de ampliar de acta-view).
    // El archivo se sirve por endpoint protegido: fetch con Bearer -> blob -> objectURL.
    async function abrirEvidencia(url) {
        const token = checkAuth();
        if (!token) return;
        try {
            const resp = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            if (!resp.ok) { showToast("No se pudo cargar la evidencia.", "error"); return; }
            const blob = await resp.blob();
            window.open(URL.createObjectURL(blob), "_blank");
        } catch (_) {
            showToast("Error al cargar la evidencia.", "error");
        }
    }

    // Vista previa de firma o foto dentro del modal. Si la imagen no se puede
    // cargar se muestra "No disponible"; el click amplia (abre en pestana).
    function cargarImagenEvidencia(id, tipo, imgId, emptyId) {
        const img = $(imgId);
        const empty = $(emptyId);
        fetch(`${API_BASE}/actas/${id}/${tipo}`, { headers: { Authorization: `Bearer ${checkAuth()}` } })
            .then((r) => { if (!r.ok) throw new Error("no"); return r.blob(); })
            .then((blob) => {
                img.src = URL.createObjectURL(blob);
                img.style.display = "block";
            })
            .catch(() => {
                empty.textContent = "No se pudo cargar la imagen.";
                empty.style.display = "block";
            });
        img.addEventListener("click", () => abrirEvidencia(`${API_BASE}/actas/${id}/${tipo}`));
    }

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
            const tipos = body.data.map((ev) => ev.tipo).filter(Boolean);
            evidenceModalBody.innerHTML = `
                <div class="ev-section">
                    <h4 class="ev-section-title">Firma Digital</h4>
                    <div class="ev-img-box">
                        <img id="evFirmaImg" class="ev-img--firma" alt="Firma digital" style="display:none">
                        <span id="evFirmaEmpty" class="ev-empty" style="display:none"></span>
                    </div>
                </div>
                <div class="ev-section">
                    <h4 class="ev-section-title">Foto de Verificacion</h4>
                    <div class="ev-img-box">
                        <img id="evFotoImg" class="ev-img--foto" alt="Foto de verificacion" style="display:none">
                        <span id="evFotoEmpty" class="ev-empty" style="display:none"></span>
                    </div>
                </div>
                <div class="ev-section" id="evDocSection">
                    <h4 class="ev-section-title">Documento Final</h4>
                    <div class="ev-btn-wrap">
                        <a class="btn btn-primary" href="acta-view.html?id=${id}">Ver Documento</a>
                    </div>
                </div>`;

            if (tipos.includes("FIRMA")) {
                cargarImagenEvidencia(id, "firma", "evFirmaImg", "evFirmaEmpty");
            } else {
                $("evFirmaEmpty").textContent = "Sin firma registrada.";
                $("evFirmaEmpty").style.display = "block";
            }
            if (tipos.includes("FOTO")) {
                cargarImagenEvidencia(id, "foto", "evFotoImg", "evFotoEmpty");
            } else {
                $("evFotoEmpty").textContent = "Sin fotografia registrada.";
                $("evFotoEmpty").style.display = "block";
            }
            if (!tipos.includes("PDF_FINAL")) {
                $("evDocSection").style.display = "none";
            }
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
            else if (enviarModal.classList.contains("open")) closeEnviarModal();
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
