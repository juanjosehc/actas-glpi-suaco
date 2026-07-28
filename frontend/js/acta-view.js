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

    function renderUserInfo() {
        const userName = document.getElementById("userName");
        const userRole = document.getElementById("userRole");
        const userAvatar = document.getElementById("userAvatar");
        if (userName) userName.textContent = localStorage.getItem("username") || "Usuario";
        if (userRole) userRole.textContent = localStorage.getItem("role") || "";
        if (userAvatar) {
            const name = localStorage.getItem("username") || "U";
            userAvatar.textContent = name.charAt(0).toUpperCase();
        }
    }

    function buildSidebar(role) {
        const nav = document.getElementById("sidebarNav");
        if (!nav) return;

        const sections = {
            ADMINISTRADOR: [
                {
                    section: "Gestion",
                    items: [
                        { label: "Usuarios", href: "usuarios.html", icon: "users" },
                        { label: "Actas", href: "actas.html", icon: "file-text" },
                        { label: "Firmas", href: "firmas.html", icon: "pen-tool" }
                    ]
                },
                {
                    section: "Actas",
                    items: [
                    { label: "Generar Acta", href: "generar-acta.html", icon: "plus-circle" },
                    { label: "Acta Entrega", href: "acta-entrega.html", icon: "file-text" },
                    { label: "Acta Devolucion", href: "acta-devolucion.html", icon: "file-text" }
                ]
            }
        ],
            TECNICO: [
                {
                    section: "Actas",
                    items: [
                        { label: "Generar Acta", href: "generar-acta.html", icon: "plus-circle" },
                        { label: "Acta Entrega", href: "acta-entrega.html", icon: "file-text" },
                        { label: "Acta Devolucion", href: "acta-devolucion.html", icon: "file-text" },
                        { label: "Mis Actas", href: "actas.html", icon: "list" }
                    ]
                }
            ],
            AUDITOR: [
                {
                    section: "Consultas",
                    items: [
                        { label: "Consultar Actas", href: "actas.html", icon: "search" }
                    ]
                }
            ]
        };

        const icons = {
            users: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
            "file-text": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
            "pen-tool": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>',
            "plus-circle": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>',
            list: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>',
            search: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>'
        };

        const currentFile = window.location.pathname.split("/").pop();

        nav.innerHTML = "";
        sections[role] && sections[role].forEach(group => {
            const label = document.createElement("div");
            label.style.cssText = "font-size:0.65rem;font-weight:700;color:#64748B;text-transform:uppercase;letter-spacing:1px;padding:16px 16px 6px;";
            label.textContent = group.section;
            nav.appendChild(label);

            group.items.forEach(item => {
                const el = document.createElement("a");
                el.href = item.href;
                el.className = "sidebar-item" + (item.href === currentFile ? " active" : "");
                el.innerHTML = icons[item.icon] || "" + '<span>' + item.label + '</span>';
                nav.appendChild(el);
            });
        });
    }

    function init() {
        const token = LoginService.obtenerToken();
        const { id, token: urlToken } = getActaFromUrl();

        if (urlToken) {
            renderUserInfo();
            const role = localStorage.getItem("role") || "ADMINISTRADOR";
            buildSidebar(role);
            setupUI();
            fetchActa();
        } else if (token) {
            renderUserInfo();
            const role = localStorage.getItem("role") || "ADMINISTRADOR";
            buildSidebar(role);
            setupUI();
            fetchActa();
        } else {
            window.location.href = ROUTES.LOGIN;
        }
    }

    function setupUI() {
        const logoutBtn = document.getElementById("logoutBtn");
        if (logoutBtn) {
            logoutBtn.addEventListener("click", () => {
                LoginService.cerrarSesion();
                window.location.href = ROUTES.LOGIN;
            });
        }

        const sidebarToggle = document.getElementById("sidebarToggle");
        const sidebar = document.getElementById("sidebar");
        if (sidebarToggle && sidebar) {
            sidebarToggle.addEventListener("click", () => {
                sidebar.classList.toggle("open");
            });
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
