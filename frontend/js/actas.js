(() => {
    const API_BASE = "http://localhost:8001";

    let actas = [];
    let currentActaId = null;

    const sidebarToggle = document.getElementById("sidebarToggle");
    const sidebar = document.getElementById("sidebar");
    const logoutBtn = document.getElementById("logoutBtn");
    const searchInput = document.getElementById("searchInput");
    const searchClear = document.getElementById("searchClear");
    const actasBody = document.getElementById("actasBody");
    const emptyState = document.getElementById("emptyState");
    const loadingOverlay = document.getElementById("loadingOverlay");
    const userAvatar = document.getElementById("userAvatar");
    const userName = document.getElementById("userName");
    const userRole = document.getElementById("userRole");

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

    async function loadUserInfo() {
        const token = checkAuth();
        if (!token) return;
        try {
            const resp = await fetch(`${API_BASE}/usuarios/me`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                const u = body.data;
                userName.textContent = `${u.nombres} ${u.apellidos}`;
                userRole.textContent = u.rol;
                const a = (u.nombres || "")[0] || "";
                const b = (u.apellidos || "")[0] || "";
                userAvatar.textContent = (a + b).toUpperCase();
            }
        } catch (_) {}
    }

    // =========================
    //  LOAD ACTAS
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
                <td class="cell-actions"><button class="btn btn-outline btn-sm" data-id="${a.id}">Ver</button></td>
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

    // =========================
    //  SIDEBAR
    // =========================

    sidebarToggle.addEventListener("click", () => sidebar.classList.toggle("open"));
    document.addEventListener("click", (e) => {
        if (window.innerWidth <= 768 && sidebar.classList.contains("open") && !sidebar.contains(e.target) && !sidebarToggle.contains(e.target)) {
            sidebar.classList.remove("open");
        }
    });
    window.addEventListener("resize", () => { if (window.innerWidth > 768) sidebar.classList.remove("open"); });

    // =========================
    //  LOGOUT
    // =========================

    logoutBtn.addEventListener("click", () => { LoginService.cerrarSesion(); window.location.href = ROUTES.LOGIN; });

    // =========================
    //  BUILD SIDEBAR NAV
    // =========================

    function buildSidebar(role) {
        const nav = document.getElementById("sidebarNav");
        const sections = {
                    ADMINISTRADOR: [{ section: "Gestion", items: [{ label: "Usuarios", href: "usuarios.html", icon: "users" }, { label: "Actas", href: "actas.html", icon: "file-text" }, { label: "Firmas", href: "firmas.html", icon: "pen-tool" }] }, { section: "Actas", items: [{ label: "Generar Acta", href: "generar-acta.html", icon: "plus-circle" }, { label: "Acta Entrega", href: "acta-entrega.html", icon: "file-text" }, { label: "Acta Devolucion", href: "acta-devolucion.html", icon: "file-text" }] }],
            TECNICO: [{ section: "Actas", items: [{ label: "Generar Acta", href: "generar-acta.html", icon: "plus-circle" }, { label: "Acta Entrega", href: "acta-entrega.html", icon: "file-text" }, { label: "Acta Devolucion", href: "acta-devolucion.html", icon: "file-text" }, { label: "Mis Actas", href: "actas.html", icon: "list" }] }],
            AUDITOR: [{ section: "Consultas", items: [{ label: "Consultar Actas", href: "actas.html", icon: "search" }] }],
        };
        const icons = {
            users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
            "file-text": '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
            "pen-tool": '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>',
            "plus-circle": '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>',
            list: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>',
            search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>',
        };
        const sects = sections[role];
        if (!sects) return;
        const currentFile = window.location.pathname.split("/").pop();
        nav.innerHTML = "";
        sects.forEach((sec) => {
            const label = document.createElement("div");
            label.style.cssText = "font-size:0.7rem;font-weight:600;text-transform:uppercase;letter-spacing:0.8px;color:#64748B;padding:16px 12px 6px";
            label.textContent = sec.section;
            nav.appendChild(label);
            sec.items.forEach((item) => {
                const hasHref = item.href && item.href !== "#";
                const el = document.createElement(hasHref ? "a" : "button");
                el.className = "nav-item";
                if (hasHref) el.href = item.href;
                if (hasHref && item.href === currentFile) el.classList.add("active");
                el.innerHTML = `${icons[item.icon] || ""}<span>${item.label}</span>`;
                nav.appendChild(el);
            });
        });
    }

    // =========================
    //  INIT
    // =========================

    (async function init() {
        await loadUserInfo();
        buildSidebar(userRole.textContent || "ADMINISTRADOR");
        await loadActas();
    })();
})();
