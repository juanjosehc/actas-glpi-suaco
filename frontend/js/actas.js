(() => {
    
    let actas = [];
    let currentActaId = null;

    // Paginacion server-side: el backend devuelve Page (content + totalPages).
    const PAGE_SIZE = 15;
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;

    // Busqueda global server-side: el backend filtra sobre TODAS las actas
    // (GET /actas?q=...), no solo la pagina activa.
    let query = "";
    let searchDebounce = null;

    const searchInput = document.getElementById("searchInput");
    const searchClear = document.getElementById("searchClear");
    const actasBody = document.getElementById("actasBody");
    const emptyState = document.getElementById("emptyState");
    const paginationEl = document.getElementById("pagination");
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

    const enviarModal = document.getElementById("enviarModal");
    const enviarCorreo = document.getElementById("enviarCorreo");
    const enviarError = document.getElementById("enviarError");
    const enviarCancel = document.getElementById("enviarCancel");
    const enviarConfirm = document.getElementById("enviarConfirm");
    const enviarClose = document.getElementById("enviarClose");

    
    // Rol: AUDITOR es solo consulta. Se ocultan los botones operativos y el
    // backend responde 403 si se intenta el POST directo.
    const ROL_USUARIO = (typeof LoginService !== "undefined" && LoginService.getRol ? LoginService.getRol() : localStorage.getItem("role")) || "";
    const PUEDE_OPERAR = ROL_USUARIO === "ADMINISTRADOR" || ROL_USUARIO === "TECNICO";

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
        return mostrarNotificacion(message, type);
    }

    // =========================
    //  USER INFO
    // =========================

    // Polling de la generacion async: mientras haya actas en
    // GENERANDO_DOCUMENTOS visibles en la pagina se re-consulta cada 3s.
    let pollingTimer = null;

    async function loadActas(esPolling) {
        const token = checkAuth();
        if (!token) return;

        // Snapshot del estado previo de la pagina actual: detecta la transicion
        // GENERANDO_DOCUMENTOS -> GENERADA para avisar "Documentos listos".
        const estadoAnterior = new Map(actas.map((a) => [a.id, a.estado]));

        if (!esPolling) {
            loadingOverlay.classList.add("visible");
            actasBody.innerHTML = "";
            emptyState.classList.remove("visible");
        }

        try {
            const qs = query ? `&q=${encodeURIComponent(query)}` : "";
            const resp = await fetch(`${API_BASE}/actas?page=${currentPage}&size=${PAGE_SIZE}${qs}`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;

            const body = await resp.json();
            if (!body.success) {
                showToast(body.mensaje || "Error al cargar actas", "error");
                return;
            }

            const data = body.data || {};
            actas = data.content || body.data || [];
            totalPages = data.totalPages ? data.totalPages : (data.content ? 1 : 0);
            totalElements = data.totalElements || actas.length;

            // Pagina huerfana (datos borrados entre cargas) → retroceder.
            if (actas.length === 0 && currentPage > 0) {
                currentPage--;
                await loadActas(esPolling);
                return;
            }

            renderTable(actas);
            renderPagination();

            // Transicion dentro de la pagina visible: generacion async termino.
            actas.forEach((a) => {
                if (estadoAnterior.get(a.id) === "GENERANDO_DOCUMENTOS" && a.estado === "GENERADA") {
                    showToast("Documentos listos. El acta quedó disponible.", "success");
                }
            });

            programarPolling();
        } catch (err) {
            if (err.message.includes("Failed to fetch")) {
                showToast("El servidor no esta disponible. Verifique la conexion.", "error");
            } else {
                showToast("Error al cargar las actas.", "error");
            }
        } finally {
            if (!esPolling) {
                loadingOverlay.classList.remove("visible");
            }
        }
    }

    /** Re-consulta el listado cada 3s mientras haya actas generando. */
    function programarPolling() {
        const hayGenerando = actas.some((a) => a.estado === "GENERANDO_DOCUMENTOS");
        if (hayGenerando && !pollingTimer) {
            pollingTimer = setTimeout(() => {
                pollingTimer = null;
                loadActas(true);
            }, 3000);
        } else if (!hayGenerando && pollingTimer) {
            clearTimeout(pollingTimer);
            pollingTimer = null;
        }
    }

    // =========================
    //  RENDER TABLE
    // =========================

    function getBadgeClass(estado) {
        const map = { GENERADA: "badge--GENERADA", ENVIADA: "badge--ENVIADA", FIRMADA: "badge--FIRMADA", APROBADA: "badge--APROBADA", RECHAZADA: "badge--RECHAZADA", GENERANDO_DOCUMENTOS: "badge--GENERANDO_DOCUMENTOS", GENERACION_FALLIDA: "badge--GENERACION_FALLIDA" };
        return map[estado] || "";
    }

    /** Etiqueta legible del estado (los estados async se muestran con texto amigable). */
    function estadoLabel(estado) {
        const map = {
            GENERANDO_DOCUMENTOS: "Generando...",
            GENERACION_FALLIDA: "Error de generación"
        };
        return map[estado] || estado || "-";
    }

    function formatDate(dateStr) {
        if (!dateStr) return "-";
        try {
            const d = new Date(dateStr);
            return d.toLocaleDateString("es-CO", { year: "numeric", month: "2-digit", day: "2-digit" });
        } catch (_) { return dateStr; }
    }

    /** Crea una celda <td> con texto plano: nunca interpreta contenido de usuario. */
    function createCell(className, value) {
        const td = document.createElement("td");
        if (className) td.className = className;
        td.textContent = value == null ? "" : String(value);
        return td;
    }

    function renderTable(data) {
        actasBody.innerHTML = "";

        if (!data || data.length === 0) {
            emptyState.classList.add("visible");
            return;
        }

        data.forEach((a) => {
            const tr = document.createElement("tr");
            tr.appendChild(createCell("cell-id", a.id));
            tr.appendChild(createCell("cell-date", formatDate(a.fechaCreacion)));
            tr.appendChild(createCell("", a.nombreUsuario || "-"));
            tr.appendChild(createCell("", a.descripcionEquipo || "-"));
            tr.appendChild(createCell("cell-tipo", a.tipoActa || "-"));

            const estadoCell = document.createElement("td");
            const badge = document.createElement("span");
            badge.className = `badge ${getBadgeClass(a.estado)}`;
            badge.textContent = estadoLabel(a.estado);
            estadoCell.appendChild(badge);
            tr.appendChild(estadoCell);

            const acciones = document.createElement("td");
            acciones.className = "cell-actions";
            const btnDocumento = document.createElement("a");
            btnDocumento.className = "btn-icon";
            btnDocumento.href = `acta-view.html?id=${a.id}`;
            btnDocumento.setAttribute("aria-label", "Ver documento");
            btnDocumento.setAttribute("data-tip", "Ver documento");
            btnDocumento.innerHTML = ICONOS.documento;
            acciones.appendChild(btnDocumento);
            acciones.appendChild(botonIcono("ojo", "Ver detalle", () => openDetail(a.id)));
            tr.appendChild(acciones);

            actasBody.appendChild(tr);
        });
    }

    // =========================
    //  SEARCH (server-side, global — filtra todas las actas, no la pagina)
    // =========================

    searchInput.addEventListener("input", () => {
        searchClear.classList.toggle("visible", searchInput.value.length > 0);
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => {
            query = searchInput.value.trim();
            currentPage = 0;
            loadActas();
        }, 300);
    });
    searchClear.addEventListener("click", () => {
        searchInput.value = "";
        searchClear.classList.remove("visible");
        clearTimeout(searchDebounce);
        query = "";
        currentPage = 0;
        loadActas();
        searchInput.focus();
    });

    // =========================
    //  PAGINACION
    // =========================

    function goToPage(page) {
        if (page < 0 || page >= totalPages) return;
        currentPage = page;
        loadActas();
    }

    function renderPagination() {
        if (!paginationEl) return;

        // Una sola pagina (o ninguna): no hay nada que paginar.
        if (totalPages <= 1) {
            paginationEl.innerHTML = "";
            return;
        }

        const btn = (label, page, disabled, title) =>
            `<button type="button" class="btn btn-outline btn-sm" ${disabled ? "disabled" : ""} data-page="${page}" title="${title}">${label}</button>`;

        paginationEl.innerHTML =
            btn("Inicio", 0, currentPage === 0, "Primera pagina") +
            btn("Anterior", currentPage - 1, currentPage === 0, "Pagina anterior") +
            `<span class="pagination-info">Pagina <strong>${currentPage + 1}</strong> de ${totalPages} · ${totalElements} actas</span>` +
            btn("Siguiente", currentPage + 1, currentPage >= totalPages - 1, "Pagina siguiente") +
            btn("Fin", totalPages - 1, currentPage >= totalPages - 1, "Ultima pagina");

        paginationEl.querySelectorAll("button[data-page]").forEach((b) => {
            b.addEventListener("click", () => goToPage(Number(b.dataset.page)));
        });
    }

    // =========================
    //  DETAIL MODAL
    // =========================

    let detailPollTimer = null;

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
                setModalMessage(body.mensaje || "Acta no encontrada.");
                return;
            }

            const a = body.data;
            renderDetail(a);

            // Generacion async en curso: el detalle se auto-refresca hasta que
            // el acta salga de GENERANDO_DOCUMENTOS.
            if (a.estado === "GENERANDO_DOCUMENTOS") {
                programarPollDetalle(id);
            }
        } catch (_) {
            setModalMessage("Error al cargar el detalle del acta.");
        }
    }

    /** Re-consulta el detalle cada 3s mientras la generacion este en curso. */
    function programarPollDetalle(id) {
        clearTimeout(detailPollTimer);
        detailPollTimer = setTimeout(async () => {
            detailPollTimer = null;
            const token = checkAuth();
            if (!token) return;
            try {
                const resp = await fetch(`${API_BASE}/actas/${id}`, { headers: { Authorization: `Bearer ${token}` } });
                if (handle401(resp)) return;
                const body = await resp.json();
                if (!body.success) {
                    setModalMessage(body.mensaje || "Acta no encontrada.");
                    return;
                }
                const a = body.data;
                renderDetail(a);
                if (a.estado === "GENERANDO_DOCUMENTOS") {
                    programarPollDetalle(id);
                } else if (a.estado === "GENERADA") {
                    showToast("Documentos listos. El acta quedó disponible.", "success");
                }
            } catch (_) { /* se reintenta en el siguiente ciclo */ }
        }, 3000);
    }

    /** Mensaje del modal en texto plano (sin innerHTML con datos del servidor). */
    function setModalMessage(mensaje) {
        modalBody.innerHTML = "";
        const p = document.createElement("p");
        p.className = "modal-desc";
        p.textContent = mensaje;
        modalBody.appendChild(p);
    }

    /** Campo del detalle: etiqueta + valor en texto plano (textContent). */
    function detailField(label, value, full) {
        const field = document.createElement("div");
        field.className = `detail-field${full ? " full" : ""}`;

        const labelEl = document.createElement("span");
        labelEl.className = "detail-label";
        labelEl.textContent = label;

        const valueEl = document.createElement("span");
        valueEl.className = "detail-value";
        if (value instanceof Node) {
            valueEl.appendChild(value);
        } else {
            valueEl.textContent = value == null || value === "" ? "-" : String(value);
        }

        field.appendChild(labelEl);
        field.appendChild(valueEl);
        return field;
    }

    function renderDetail(a) {
        const estado = a.estado || "";
        const badgeClass = getBadgeClass(estado);

        const grid = document.createElement("div");
        grid.className = "detail-grid";

        const badge = document.createElement("span");
        badge.className = `badge ${badgeClass}`;
        badge.textContent = estado;

        grid.appendChild(detailField("ID", a.id));
        grid.appendChild(detailField("Estado", badge));
        grid.appendChild(detailField("Tipo Acta", a.tipoActa || "-"));
        grid.appendChild(detailField("Fecha Creacion", formatDate(a.fechaCreacion)));
        grid.appendChild(detailField("Usuario", a.nombreUsuario || "-"));
        if (a.tipoActa === "DEVOLUCION") {
            grid.appendChild(detailField("Cedula", a.cedulaUsuario || "-"));
        }
        grid.appendChild(detailField("Ticket GLPI", a.ticketGlpi || "-"));
        grid.appendChild(detailField("Equipo", a.descripcionEquipo || "-"));
        grid.appendChild(detailField("Serial", a.serialEquipo || "-"));
        grid.appendChild(detailField("Placa", a.placaEquipo || "-"));
        grid.appendChild(detailField("Fecha Rechazo", formatDate(a.fechaRechazo)));
        grid.appendChild(detailField("Observacion Rechazo", a.observacionRechazo || "-", true));

        modalBody.innerHTML = "";
        modalBody.appendChild(grid);

        renderActions(a);
    }

    function renderActions(a) {
        modalActions.innerHTML = "";

        const btnDoc = document.createElement("a");
        btnDoc.className = "btn btn-outline";
        btnDoc.href = `acta-view.html?id=${a.id}`;
        btnDoc.textContent = "Ver Documento";
        modalActions.appendChild(btnDoc);

        if (a.estado === "GENERADA" && PUEDE_OPERAR) {
            const btn = document.createElement("button");
            btn.className = "btn btn-primary";
            btn.textContent = "Enviar a Firma";
            btn.addEventListener("click", () => openEnviarModal(a));
            modalActions.appendChild(btn);
        }

        if (a.estado === "FIRMADA" && PUEDE_OPERAR) {
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

        if (a.estado === "GENERACION_FALLIDA" && PUEDE_OPERAR) {
            const btnRetry = document.createElement("button");
            btnRetry.className = "btn btn-primary";
            btnRetry.textContent = "Reintentar generación";
            btnRetry.addEventListener("click", () => reintentarGeneracion(a.id));
            modalActions.appendChild(btnRetry);
        }

        if (a.estado === "GENERANDO_DOCUMENTOS") {
            const span = document.createElement("span");
            span.className = "modal-desc";
            span.textContent = "Generando documentos, se actualizará automáticamente...";
            span.style.margin = "0";
            modalActions.appendChild(span);
        } else if (a.estado !== "GENERADA" && a.estado !== "FIRMADA" && a.estado !== "GENERACION_FALLIDA") {
            const span = document.createElement("span");
            span.className = "modal-desc";
            span.textContent = "No hay acciones disponibles para este estado.";
            span.style.margin = "0";
            modalActions.appendChild(span);
        }
    }

    /** Re-encola la generacion de documentos tras una GENERACION_FALLIDA. */
    async function reintentarGeneracion(id) {
        const token = checkAuth();
        if (!token) return;
        try {
            const resp = await fetch(`${API_BASE}/actas/${id}/reintentar-generacion`, { method: "POST", headers: authHeaders() });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast("Generación de documentos re-encolada.", "success");
                openDetail(id);
                loadActas();
            } else {
                showToast(body.mensaje || "Error al reintentar la generación.", "error");
            }
        } catch (_) {
            showToast("Error de conexión al reintentar la generación.", "error");
        }
    }

    // =========================
    //  ENVIAR
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

        const token = checkAuth();
        if (!token) return;

        // Estado de carga en el boton mientras el backend intenta el envio real;
        // el modal no se cierra hasta recibir response (exito o error).
        const btnTexto = enviarConfirm.textContent;
        enviarConfirm.disabled = true;
        enviarConfirm.textContent = "Enviando...";
        try {
            const resp = await fetch(`${API_BASE}/actas/${currentActaId}/enviar`, {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ correo })
            });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast("Acta enviada para firma exitosamente.", "success");
                closeEnviarModal();
                closeDetailModal();
                loadActas();
            } else {
                showToast(body.mensaje || "Error al enviar el acta.", "error");
            }
        } catch (_) {
            showToast("Error de conexion al enviar el acta.", "error");
        } finally {
            enviarConfirm.disabled = false;
            enviarConfirm.textContent = btnTexto;
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
        clearTimeout(detailPollTimer);
        detailPollTimer = null;
        detailModal.classList.remove("open");
    }

    modalClose.addEventListener("click", closeDetailModal);
    detailModal.addEventListener("click", (e) => { if (e.target === detailModal) closeDetailModal(); });

    rejectClose.addEventListener("click", closeRejectModal);
    rejectCancel.addEventListener("click", closeRejectModal);
    rejectConfirm.addEventListener("click", confirmReject);
    rejectModal.addEventListener("click", (e) => { if (e.target === rejectModal) closeRejectModal(); });

    enviarClose.addEventListener("click", closeEnviarModal);
    enviarCancel.addEventListener("click", closeEnviarModal);
    enviarConfirm.addEventListener("click", confirmEnviar);
    enviarModal.addEventListener("click", (e) => { if (e.target === enviarModal) closeEnviarModal(); });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            if (rejectModal.classList.contains("open")) closeRejectModal();
            else if (enviarModal.classList.contains("open")) closeEnviarModal();
            else closeDetailModal();
        }
    });

    (async function init() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return; }
        await loadActas();
    })();
})();
