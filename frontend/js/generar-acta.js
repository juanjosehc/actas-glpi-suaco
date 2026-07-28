(function() {
    const API_BASE = "http://localhost:8001";

    const tipoEntrega = document.getElementById("tipoEntrega");
    const tipoDevolucion = document.getElementById("tipoDevolucion");
    const tipoError = document.getElementById("tipoError");
    const estadoEquipoGroup = document.getElementById("estadoEquipoGroup");
    const fCedula = document.getElementById("fCedula");
    const fNombre = document.getElementById("fNombre");
    const fCorreo = document.getElementById("fCorreo");
    const fCargo = document.getElementById("fCargo");
    const fLugar = document.getElementById("fLugar");
    const fEmpresa = document.getElementById("fEmpresa");
    const fDescripcion = document.getElementById("fDescripcion");
    const fMarcaModelo = document.getElementById("fMarcaModelo");
    const fSerial = document.getElementById("fSerial");
    const fPlaca = document.getElementById("fPlaca");
    const fProcesador = document.getElementById("fProcesador");
    const fMemoria = document.getElementById("fMemoria");
    const fDisco = document.getElementById("fDisco");
    const fSO = document.getElementById("fSO");
    const fMonitor = document.getElementById("fMonitor");
    const fAccesorios = document.getElementById("fAccesorios");
    const fEstadoEquipo = document.getElementById("fEstadoEquipo");
    const fTicket = document.getElementById("fTicket");
    const fObservaciones = document.getElementById("fObservaciones");
    const btnPreview = document.getElementById("btnPreview");
    const btnSave = document.getElementById("btnSave");
    const genPreview = document.getElementById("genPreview");
    const genDocument = document.getElementById("genDocument");
    const loadingOverlay = document.getElementById("loadingOverlay");
    const toastContainer = document.getElementById("toastContainer");

    let selectedTipo = null;

    function selectTipo(tipo) {
        selectedTipo = tipo;
        tipoEntrega.classList.toggle("active", tipo === "ENTREGA");
        tipoDevolucion.classList.toggle("active", tipo === "DEVOLUCION");
        tipoError.classList.remove("visible");
        estadoEquipoGroup.style.display = tipo === "DEVOLUCION" ? "block" : "none";
    }

    tipoEntrega.addEventListener("click", () => selectTipo("ENTREGA"));
    tipoDevolucion.addEventListener("click", () => selectTipo("DEVOLUCION"));

    function showToast(message, type) {
        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(() => { toast.remove(); }, 3500);
    }

    function getFormData() {
        return {
            cedulaUsuario: fCedula.value.trim(),
            nombreUsuario: fNombre.value.trim(),
            correoUsuario: fCorreo.value.trim(),
            cargo: fCargo.value.trim(),
            lugarTrabajo: fLugar.value.trim(),
            empresa: fEmpresa.value.trim(),
            descripcionEquipo: fDescripcion.value.trim(),
            marcaModelo: fMarcaModelo.value.trim(),
            serialEquipo: fSerial.value.trim(),
            placaEquipo: fPlaca.value.trim(),
            procesador: fProcesador.value.trim(),
            memoriaRam: fMemoria.value.trim(),
            discoDuro: fDisco.value.trim(),
            sistemaOperativo: fSO.value.trim(),
            monitor: fMonitor.value.trim(),
            accesorios: fAccesorios.value.trim(),
            estadoEquipo: selectedTipo === "DEVOLUCION" ? fEstadoEquipo.value : null,
            ticketGlpi: fTicket.value.trim(),
            observaciones: fObservaciones.value.trim()
        };
    }

    function validate(data) {
        if (!selectedTipo) {
            tipoError.classList.add("visible");
            return false;
        }
        if (!data.cedulaUsuario) { showToast("La cedula es obligatoria", "error"); return false; }
        if (!data.nombreUsuario) { showToast("El nombre es obligatorio", "error"); return false; }
        if (!data.correoUsuario) { showToast("El correo es obligatorio", "error"); return false; }
        if (!data.descripcionEquipo) { showToast("La descripcion del equipo es obligatoria", "error"); return false; }
        if (!data.serialEquipo) { showToast("El serial es obligatorio", "error"); return false; }
        if (!data.placaEquipo) { showToast("La placa interna es obligatoria", "error"); return false; }
        return true;
    }

    function loadTemplate(tipo) {
        const templateName = tipo === "ENTREGA" ? "acta-entrega.html" : "acta-devolucion.html";
        return fetch(`../templates/${templateName}`)
            .then(r => {
                if (!r.ok) throw new Error("No se pudo cargar la plantilla");
                return r.text();
            });
    }

    function fillTemplate(html, data) {
        const now = new Date();
        const fechaStr = now.toLocaleDateString("es-CO", {
            year: "numeric", month: "long", day: "numeric"
        });

        const idActa = `ACT-${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}-${String(Date.now()).slice(-4)}`;

        const vars = {
            idActa: idActa,
            fechaCreacion: fechaStr,
            nombreUsuario: data.nombreUsuario || "________________",
            cedulaUsuario: data.cedulaUsuario || "________________",
            correoUsuario: data.correoUsuario || "________________",
            cargo: data.cargo || "________________",
            lugarTrabajo: data.lugarTrabajo || "________________",
            empresa: data.empresa || "Coltefinanciera",
            ticketGlpi: data.ticketGlpi || "________________",
            descripcionEquipo: data.descripcionEquipo || "________________",
            marcaModelo: data.marcaModelo || "________________",
            serialEquipo: data.serialEquipo || "________________",
            placaEquipo: data.placaEquipo || "________________",
            procesador: data.procesador || "________________",
            memoriaRam: data.memoriaRam || "________________",
            discoDuro: data.discoDuro || "________________",
            sistemaOperativo: data.sistemaOperativo || "________________",
            monitor: data.monitor || "________________",
            accesorios: data.accesorios || "________________",
            estadoEquipo: data.estadoEquipo || "________________",
            observaciones: data.observaciones || "Ninguna",
            tecnicoNombre: "________________"
        };

        return html.replace(/\{\{(\w+)\}\}/g, (_, key) => vars[key] || "{{" + key + "}}");
    }

    function showPreview() {
        const data = getFormData();
        if (!validate(data)) return;

        loadingOverlay.classList.add("visible");
        loadingOverlay.querySelector(".loading-text").textContent = "Generando vista previa...";

        loadTemplate(selectedTipo)
            .then(tpl => {
                const rendered = fillTemplate(tpl, data);
                genDocument.innerHTML = rendered;
                genPreview.style.display = "block";
                genPreview.scrollIntoView({ behavior: "smooth", block: "start" });
            })
            .catch(err => {
                showToast("Error al cargar plantilla: " + err.message, "error");
            })
            .finally(() => {
                loadingOverlay.classList.remove("visible");
                loadingOverlay.querySelector(".loading-text").textContent = "Generando acta...";
            });
    }

    function saveActa() {
        const data = getFormData();
        if (!validate(data)) return;

        loadingOverlay.classList.add("visible");

        loadTemplate(selectedTipo)
            .then(tpl => {
                const rendered = fillTemplate(tpl, data);

                const payload = {
                    tipoActa: selectedTipo,
                    cedulaUsuario: data.cedulaUsuario,
                    nombreUsuario: data.nombreUsuario,
                    correoUsuario: data.correoUsuario,
                    cargo: data.cargo,
                    lugarTrabajo: data.lugarTrabajo,
                    empresa: data.empresa,
                    descripcionEquipo: data.descripcionEquipo,
                    marcaModelo: data.marcaModelo,
                    serialEquipo: data.serialEquipo,
                    placaEquipo: data.placaEquipo,
                    procesador: data.procesador,
                    memoriaRam: data.memoriaRam,
                    discoDuro: data.discoDuro,
                    sistemaOperativo: data.sistemaOperativo,
                    monitor: data.monitor,
                    accesorios: data.accesorios,
                    estadoEquipo: data.estadoEquipo,
                    ticketGlpi: data.ticketGlpi,
                    observaciones: data.observaciones,
                    contenidoHtml: rendered
                };

                const token = LoginService.obtenerToken();
                if (!token) {
                    window.location.href = ROUTES.LOGIN;
                    return;
                }

                return fetch(`${API_BASE}/actas`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`
                    },
                    body: JSON.stringify(payload)
                });
            })
            .then(resp => {
                if (!resp) return;
                if (resp.status === 401) {
                    LoginService.cerrarSesion();
                    window.location.href = ROUTES.LOGIN;
                    return;
                }
                if (!resp.ok) {
                    return resp.json().then(err => { throw new Error(err.message || "Error al crear acta"); });
                }
                return resp.json();
            })
            .then(acta => {
                if (!acta) return;
                showToast("Acta #" + acta.id + " generada exitosamente", "success");
                genDocument.innerHTML = acta.contenidoHtml || "";
                genPreview.style.display = "block";
                genPreview.scrollIntoView({ behavior: "smooth", block: "start" });
            })
            .catch(err => {
                showToast("Error: " + err.message, "error");
            })
            .finally(() => {
                loadingOverlay.classList.remove("visible");
            });
    }

    function checkAuth() {
        const token = LoginService.obtenerToken();
        if (!token) {
            window.location.href = ROUTES.LOGIN;
            return false;
        }
        return true;
    }

    function renderUserInfo() {
        const token = LoginService.obtenerToken();
        if (!token) return;
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
        if (!checkAuth()) return;
        renderUserInfo();
        const role = localStorage.getItem("role") || "ADMINISTRADOR";
        buildSidebar(role);

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

    btnPreview.addEventListener("click", showPreview);
    btnSave.addEventListener("click", saveActa);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
