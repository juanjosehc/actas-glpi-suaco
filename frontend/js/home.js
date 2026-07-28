(() => {
    const SIDEBAR_ITEMS = {
        ADMINISTRADOR: [
            {
                section: "Gestion",
                items: [
                    { label: "Usuarios", href: "#", icon: "users" },
                    { label: "Actas", href: "#", icon: "file-text" },
                    { label: "Firmas", href: "#", icon: "pen-tool" },
                ],
            },
        ],
        TECNICO: [
            {
                section: "Actas",
                items: [
                    { label: "Generar Acta", href: "#", icon: "plus-circle" },
                    { label: "Mis Actas", href: "#", icon: "list" },
                ],
            },
        ],
        AUDITOR: [
            {
                section: "Consultas",
                items: [
                    { label: "Consultar Actas", href: "#", icon: "search" },
                ],
            },
        ],
    };

    const ICONS = {
        users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
        "file-text": '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
        "pen-tool": '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>',
        "plus-circle": '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>',
        list: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>',
        search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>',
    };

    const API_BASE = "http://localhost:8001";

    const sidebarNav = document.getElementById("sidebarNav");
    const userName = document.getElementById("userName");
    const userRole = document.getElementById("userRole");
    const userAvatar = document.getElementById("userAvatar");
    const welcomeTitle = document.getElementById("welcomeTitle");
    const welcomeText = document.getElementById("welcomeText");
    const logoutBtn = document.getElementById("logoutBtn");
    const sidebarToggle = document.getElementById("sidebarToggle");
    const sidebar = document.getElementById("sidebar");

    function checkAuth() {
        const token = LoginService.obtenerToken();
        if (!token) {
            window.location.href = "../login.html";
            return null;
        }
        return token;
    }

    async function loadUser() {
        const token = checkAuth();
        if (!token) return;

        try {
            const resp = await fetch(`${API_BASE}/usuarios/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            const body = await resp.json();

            if (!resp.ok || body.success === false) {
                throw new Error(body.mensaje || "Sesion expirada");
            }
            return body.data;
        } catch (err) {
            if (err.message.includes("Failed to fetch")) {
                throw new Error("El servidor no esta disponible.");
            }
            LoginService.cerrarSesion();
            window.location.href = "../login.html";
            return null;
        }
    }

    function getInitials(nombres, apellidos) {
        const a = (nombres || "")[0] || "";
        const b = (apellidos || "")[0] || "";
        return (a + b).toUpperCase();
    }

    function buildSidebar(role) {
        const sections = SIDEBAR_ITEMS[role];
        if (!sections) return;

        sidebarNav.innerHTML = "";

        sections.forEach((section) => {
            const label = document.createElement("div");
            label.className = "nav-section-label";
            label.textContent = section.section;
            sidebarNav.appendChild(label);

            section.items.forEach((item) => {
                const btn = document.createElement("button");
                btn.className = "nav-item";
                btn.innerHTML = `${ICONS[item.icon] || ""}<span>${item.label}</span>`;
                btn.addEventListener("click", () => {
                    document.querySelectorAll(".nav-item").forEach((n) => n.classList.remove("active"));
                    btn.classList.add("active");
                    welcomeTitle.textContent = item.label;
                    welcomeText.textContent = `Seccion "${item.label}" seleccionada.`;
                });
                sidebarNav.appendChild(btn);
            });
        });
    }

    function logout() {
        LoginService.cerrarSesion();
        window.location.href = "../login.html";
    }

    sidebarToggle.addEventListener("click", () => {
        sidebar.classList.toggle("open");
    });

    document.addEventListener("click", (e) => {
        if (window.innerWidth <= 768 && sidebar.classList.contains("open")) {
            if (!sidebar.contains(e.target) && e.target !== sidebarToggle && !sidebarToggle.contains(e.target)) {
                sidebar.classList.remove("open");
            }
        }
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > 768) {
            sidebar.classList.remove("open");
        }
    });

    logoutBtn.addEventListener("click", logout);

    (async function init() {
        const user = await loadUser();
        if (!user) return;

        userName.textContent = `${user.nombres} ${user.apellidos}`;
        userRole.textContent = user.rol;
        userAvatar.textContent = getInitials(user.nombres, user.apellidos);

        buildSidebar(user.rol);
    })();
})();
