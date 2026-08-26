(function() {
    "use strict";

    if (window.AdminLayout) return;
    window.AdminLayout = {};

    var API_BASE = "http://localhost:8001";

    AdminLayout.init = function(opts) {
        opts = opts || {};

        var token = LoginService.obtenerToken();
        if (!token) {
            window.location.href = ROUTES.LOGIN;
            return;
        }

        renderUser(token);
        var role = localStorage.getItem("role") || "ADMINISTRADOR";
        buildSidebar(role, opts.currentFile);

        var logoutBtn = document.getElementById("logoutBtn");
        if (logoutBtn) {
            logoutBtn.addEventListener("click", function() {
                LoginService.cerrarSesion();
                window.location.href = ROUTES.LOGIN;
            });
        }

        var sidebarToggle = document.getElementById("sidebarToggle");
        var sidebar = document.getElementById("sidebar");
        if (sidebarToggle && sidebar) {
            sidebarToggle.addEventListener("click", function() {
                sidebar.classList.toggle("open");
            });
            document.addEventListener("click", function(e) {
                if (window.innerWidth <= 768 && sidebar.classList.contains("open") && !sidebar.contains(e.target) && !sidebarToggle.contains(e.target)) {
                    sidebar.classList.remove("open");
                }
            });
        }

        if (opts.onReady) opts.onReady();
    };

    function renderUser(token) {
        var userName = document.getElementById("userName");
        var userRole = document.getElementById("userRole");
        var userAvatar = document.getElementById("userAvatar");

        if (userName) userName.textContent = localStorage.getItem("username") || "Usuario";
        if (userRole) userRole.textContent = localStorage.getItem("role") || "";
        if (userAvatar) {
            var name = localStorage.getItem("username") || "U";
            userAvatar.textContent = name.charAt(0).toUpperCase();
        }

        var cachedName = localStorage.getItem("username");
        var cachedRole = localStorage.getItem("role");
        if (cachedName && userName) userName.textContent = cachedName;
        if (cachedRole && userRole) userRole.textContent = cachedRole;

        fetch(API_BASE + "/usuarios/me", {
            headers: { Authorization: "Bearer " + token }
        }).then(function(r) {
            if (r.status === 401) {
                LoginService.cerrarSesion();
                window.location.href = ROUTES.LOGIN;
                return null;
            }
            return r.json();
        }).then(function(body) {
            if (body && body.success) {
                var u = body.data;
                if (userName) userName.textContent = (u.nombres || "") + " " + (u.apellidos || "");
                if (userRole) userRole.textContent = u.rol || "";
                if (userAvatar) {
                    var a = (u.nombres || "")[0] || "";
                    var b = (u.apellidos || "")[0] || "";
                    userAvatar.textContent = (a + b).toUpperCase();
                }
                localStorage.setItem("username", (u.nombres || "") + " " + (u.apellidos || ""));
                localStorage.setItem("role", u.rol || "");
            }
        }).catch(function() {});
    }

    function buildSidebar(role, currentFile) {
        var nav = document.getElementById("sidebarNav");
        if (!nav) return;

        var sections = {
            ADMINISTRADOR: [
                {
                    section: "Gestion",
                    items: [
                        { label: "Usuarios", href: "usuarios.html", icon: "users" },
                        { label: "Firmas", href: "firmas.html", icon: "pen-tool" }
                    ]
                },
                {
                    section: "Actas",
                    items: [
                        { label: "Listado de Actas", href: "actas.html", icon: "list" },
                        { label: "Nueva Acta de Entrega", href: "acta-entrega.html", icon: "file-text" },
                        { label: "Nueva Acta de Devolución", href: "acta-devolucion.html", icon: "file-text" }
                    ]
                },
                {
                    section: "Cuenta",
                    items: [
                        { label: "Mi Perfil", href: "perfil.html", icon: "user" }
                    ]
                }
            ],
            TECNICO: [
                {
                    section: "Actas",
                    items: [
                        { label: "Listado de Actas", href: "actas.html", icon: "list" },
                        { label: "Nueva Acta de Entrega", href: "acta-entrega.html", icon: "file-text" },
                        { label: "Nueva Acta de Devolución", href: "acta-devolucion.html", icon: "file-text" }
                    ]
                },
                {
                    section: "Firmas",
                    items: [
                        // El rol TECNICO gestiona las firmas de sus propias actas
                        // (el backend ya restringe por propietario).
                        { label: "Firmas", href: "firmas.html", icon: "pen-tool" }
                    ]
                },
                {
                    section: "Cuenta",
                    items: [
                        { label: "Mi Perfil", href: "perfil.html", icon: "user" }
                    ]
                }
            ],
            AUDITOR: [
                {
                    section: "Consultas",
                    items: [
                        { label: "Consultar Actas", href: "actas.html", icon: "search" },
                        // Auditoria global de firmas (lectura; backend solo permite
                        // aprobar/rechazar a ADMINISTRADOR y al TECNICO dueno).
                        { label: "Firmas", href: "firmas.html", icon: "pen-tool" }
                    ]
                },
                {
                    section: "Cuenta",
                    items: [
                        { label: "Mi Perfil", href: "perfil.html", icon: "user" }
                    ]
                }
            ]
        };

        var icons = {
            users: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
            "file-text": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
            "pen-tool": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>',
            "plus-circle": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>',
            list: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>',
            search: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>',
            user: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
        };

        var sects = sections[role];
        if (!sects) sects = sections.ADMINISTRADOR;

        if (!currentFile) {
            currentFile = window.location.pathname.split("/").pop();
        }

        nav.innerHTML = "";
        sects.forEach(function(sec) {
            var label = document.createElement("div");
            label.className = "nav-section-label";
            label.textContent = sec.section;
            nav.appendChild(label);

            sec.items.forEach(function(item) {
                var el = document.createElement("a");
                el.href = item.href;
                el.className = "nav-item" + (item.href === currentFile ? " active" : "");
                el.innerHTML = (icons[item.icon] || "") + "<span>" + item.label + "</span>";
                nav.appendChild(el);
            });
        });
    }
})();
