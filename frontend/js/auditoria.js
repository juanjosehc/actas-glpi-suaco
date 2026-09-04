(() => {
    "use strict";

        const PAGE_SIZE = 50;

    let currentPage = 0;

    const $ = (id) => document.getElementById(id);

    const auditoriaBody = $("auditoriaBody");
    const emptyState = $("emptyState");
    const loadingOverlay = $("loadingOverlay");
    const paginationBar = $("paginationBar");
    const paginationInfo = $("paginationInfo");
    const paginationCurrent = $("paginationCurrent");
    const btnPrev = $("btnPrev");
    const btnNext = $("btnNext");
    
    // =========================
    //  AUTH / ACCESO
    // =========================

    function checkAuth() {
        const token = LoginService.obtenerToken();
        if (!token) {
            window.location.href = ROUTES.LOGIN;
            return null;
        }
        // El backend tambien bloquea (403); aqui solo se avisa antes.
        const rol = localStorage.getItem("role") || "";
        if (rol !== "ADMINISTRADOR" && rol !== "AUDITOR") {
            showToast("El rol " + rol + " no tiene acceso al modulo de Auditoria.", "error");
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
        if (resp.status === 403) {
            showToast("No tiene permisos para consultar auditoria (rol sin acceso).", "error");
            return true;
        }
        return false;
    }

    // =========================
    //  TOAST / LOADING
    // =========================

    function showToast(message, type) {
        return mostrarNotificacion(message, type);
    }

    function setLoading(show, text) {
        if (show) {
            loadingOverlay.querySelector(".loading-text").textContent = text || "Cargando...";
            loadingOverlay.classList.add("visible");
        } else {
            loadingOverlay.classList.remove("visible");
        }
    }

    // =========================
    //  FILTROS -> QUERY STRING
    // =========================

    function queryParams(page) {
        const p = new URLSearchParams();
        p.set("page", String(page));
        p.set("size", String(PAGE_SIZE));
        const firma = (id) => $(id).value.trim();
        if (firma("fDesde")) p.set("desde", firma("fDesde"));
        if (firma("fHasta")) p.set("hasta", firma("fHasta"));
        if (firma("fUsuario")) p.set("usuario", firma("fUsuario"));
        if (firma("fRol")) p.set("rol", firma("fRol"));
        if (firma("fTipo")) p.set("tipoEvento", firma("fTipo"));
        if (firma("fActa")) p.set("idActa", firma("fActa"));
        if (firma("fEstado")) p.set("estado", firma("fEstado"));
        if (firma("fQ")) p.set("q", firma("fQ"));
        return p.toString();
    }

    // =========================
    //  CARGA DE DATOS
    // =========================

    async function loadEventos() {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Cargando eventos...");
        try {
            const resp = await fetch(`${API_BASE}/auditoria/eventos?${queryParams(currentPage)}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) {
                showToast(body.mensaje || "Error al cargar eventos", "error");
                return;
            }
            renderTabla(body.data);
        } catch (_) {
            showToast("El servidor no esta disponible.", "error");
        } finally {
            setLoading(false);
        }
    }

    async function loadEstadisticas() {
        const token = checkAuth();
        if (!token) return;
        try {
            const resp = await fetch(`${API_BASE}/auditoria/estadisticas?${queryParams(0)}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) return;
            const s = body.data || {};
            $("statTotal").textContent = s.totalEventos ?? "-";
            $("statHoy").textContent = s.eventosHoy ?? "-";
            $("stat7d").textContent = s.eventosUltimos7Dias ?? "-";
            $("statFirmas").textContent = s.firmas ?? "-";
            $("statAccesos").textContent = s.accesosExitosos ?? "-";
            $("statErrores").textContent = s.erroresLogin ?? "-";
            $("statDenegados").textContent = s.accesosDenegados ?? "-";
        } catch (_) { /* estadisticas son informativas; si fallan, no bloquear */ }
    }

    // =========================
    //  RENDER
    // =========================

    function formatFecha(iso) {
        if (!iso) return "-";
        try {
            const d = new Date(iso);
            return d.toLocaleString("es-CO", {
                year: "numeric", month: "2-digit", day: "2-digit",
                hour: "2-digit", minute: "2-digit"
            });
        } catch (_) { return iso; }
    }

    function categoriaBadge(cat) {
        const colores = {
            DOCUMENTOS: "#2563eb",
            SEGURIDAD: "#d97706",
            SISTEMA: "#6b7280"
        };
        const color = colores[cat] || "#6b7280";
        return `<span class="badge" style="background:${color}15;color:${color};border:1px solid ${color}55">${cat || "-"}</span>`;
    }

    function rolBadge(rol) {
        const colores = {
            ADMINISTRADOR: "#dc2626",
            AUDITOR: "#7c3aed",
            TECNICO: "#059669",
            SISTEMA: "#6b7280"
        };
        const color = colores[rol] || "#6b7280";
        return `<span class="badge" style="background:${color}15;color:${color};border:1px solid ${color}55">${rol || "-"}</span>`;
    }

    function renderTabla(data) {
        auditoriaBody.innerHTML = "";
        const eventos = (data && data.eventos) || [];
        if (eventos.length === 0) {
            emptyState.classList.add("visible");
            paginationBar.style.display = "none";
            return;
        }
        emptyState.classList.remove("visible");
        paginationBar.style.display = "flex";

        eventos.forEach((e) => {
            const tr = document.createElement("tr");

            const detalle = [];
            if (e.detalle) detalle.push(escapeHtml(e.detalle));
            if (e.correo) detalle.push(`<span class="aud-mini">correo: ${escapeHtml(e.correo)}</span>`);
            if (e.informacionAdicional) detalle.push(`<span class="aud-mini">${escapeHtml(e.informacionAdicional)}</span>`);

            tr.innerHTML = `
                <td class="aud-fecha">${formatFecha(e.fecha)}<div class="aud-mini">${e.origen || ""}</div></td>
                <td><span class="aud-evento">${e.tipoEvento || "-"}</span><div class="aud-mini">${e.accion || ""}</div></td>
                <td>${escapeHtml(e.usuario || "-")}<div class="aud-rol">${rolBadge(e.rol)}</div></td>
                <td>${escapeHtml(e.entidad || "-")}${e.entidadId != null ? `<div class="aud-mini">#${e.entidadId}</div>` : ""}</td>
                <td>${e.estadoActa || "-"}</td>
                <td>${categoriaBadge(e.categoria)}</td>
                <td class="aud-detalle">${detalle.length ? detalle.join("<br>") : "-"}</td>
            `;
            auditoriaBody.appendChild(tr);
        });

        const total = data.total != null ? data.total : eventos.length;
        const pagina = data.pagina != null ? data.pagina : currentPage;
        const totalPaginas = data.totalPaginas != null ? data.totalPaginas : 1;
        paginationInfo.textContent = `Total de eventos: ${total}`;
        paginationCurrent.textContent = `${pagina + 1}`;
        btnPrev.disabled = pagina <= 0;
        btnNext.disabled = pagina >= totalPaginas - 1;
    }

    function escapeHtml(s) {
        if (s == null) return "";
        return String(s).replace(/[&<>"']/g, (c) => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
        }[c]));
    }

    // =========================
    //  NAVEGACION
    // =========================

    function irPagina(nueva) {
        currentPage = nueva;
        loadEventos();
    }

    btnPrev.addEventListener("click", () => irPagina(Math.max(0, currentPage - 1)));
    btnNext.addEventListener("click", () => irPagina(currentPage + 1));

    $("btnFiltrar").addEventListener("click", () => {
        currentPage = 0;
        loadEventos();
        loadEstadisticas();
    });

    $("btnLimpiar").addEventListener("click", () => {
        ["fDesde", "fHasta", "fUsuario", "fRol", "fTipo", "fActa", "fEstado", "fQ"]
            .forEach((id) => $(id).value = "");
        currentPage = 0;
        loadEventos();
        loadEstadisticas();
    });

    // Enter en el buscador general aplica filtros
    $("fQ").addEventListener("keydown", (ev) => {
        if (ev.key === "Enter") {
            currentPage = 0;
            loadEventos();
            loadEstadisticas();
        }
    });

    // =========================
    //  INIT
    // =========================

    loadEstadisticas();
    loadEventos();
})();