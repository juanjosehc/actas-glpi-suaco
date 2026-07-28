(() => {
    const API_BASE = "http://localhost:8001";
    const PAGE_SIZE = 10;

    let allUsuarios = [];
    let filteredUsuarios = [];
    let currentPage = 1;
    let confirmAction = null;

    const sidebarToggle = document.getElementById("sidebarToggle");
    const sidebar = document.getElementById("sidebar");
    const logoutBtn = document.getElementById("logoutBtn");
    const searchInput = document.getElementById("searchInput");
    const searchClear = document.getElementById("searchClear");
    const usuariosBody = document.getElementById("usuariosBody");
    const emptyState = document.getElementById("emptyState");
    const loadingOverlay = document.getElementById("loadingOverlay");
    const paginationBar = document.getElementById("paginationBar");
    const paginationInfo = document.getElementById("paginationInfo");
    const paginationCurrent = document.getElementById("paginationCurrent");
    const btnPrev = document.getElementById("btnPrev");
    const btnNext = document.getElementById("btnNext");
    const userAvatar = document.getElementById("userAvatar");
    const userName = document.getElementById("userName");
    const userRole = document.getElementById("userRole");

    const modalUser = document.getElementById("modalUser");
    const modalUserTitle = document.getElementById("modalUserTitle");
    const modalUserBody = document.getElementById("modalUserBody");
    const modalUserClose = document.getElementById("modalUserClose");
    const modalUserCancel = document.getElementById("modalUserCancel");
    const modalUserSave = document.getElementById("modalUserSave");
    const userForm = document.getElementById("userForm");
    const formUserId = document.getElementById("formUserId");
    const formCedula = document.getElementById("formCedula");
    const formNombres = document.getElementById("formNombres");
    const formApellidos = document.getElementById("formApellidos");
    const formUsername = document.getElementById("formUsername");
    const formCorreo = document.getElementById("formCorreo");
    const formPassword = document.getElementById("formPassword");
    const formCargo = document.getElementById("formCargo");
    const formEmpresa = document.getElementById("formEmpresa");
    const formLugarTrabajo = document.getElementById("formLugarTrabajo");
    const formRol = document.getElementById("formRol");
    const passwordGroup = document.getElementById("passwordGroup");
    const formError = document.getElementById("formError");

    const modalView = document.getElementById("modalView");
    const modalViewBody = document.getElementById("modalViewBody");
    const modalViewClose = document.getElementById("modalViewClose");

    const modalConfirm = document.getElementById("modalConfirm");
    const modalConfirmTitle = document.getElementById("modalConfirmTitle");
    const modalConfirmText = document.getElementById("modalConfirmText");
    const modalConfirmClose = document.getElementById("modalConfirmClose");
    const modalConfirmCancel = document.getElementById("modalConfirmCancel");
    const modalConfirmOk = document.getElementById("modalConfirmOk");

    const toastContainer = document.getElementById("toastContainer");

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

    function showToast(message, type) {
        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        toastContainer.appendChild(toast);
        setTimeout(() => toast.remove(), 3500);
    }

    function setLoading(show, text) {
        if (show) {
            loadingOverlay.querySelector(".loading-text").textContent = text || "Cargando...";
            loadingOverlay.classList.add("visible");
        } else {
            loadingOverlay.classList.remove("visible");
        }
    }

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

    async function loadUsuarios() {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Cargando usuarios...");
        try {
            const resp = await fetch(`${API_BASE}/usuarios`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) { showToast(body.mensaje || "Error al cargar usuarios", "error"); return; }
            allUsuarios = body.data.content || body.data || [];
            filteredUsuarios = [...allUsuarios];
            currentPage = 1;
            render();
        } catch (_) {
            showToast("El servidor no esta disponible.", "error");
        } finally {
            setLoading(false);
        }
    }

    function render() {
        const start = (currentPage - 1) * PAGE_SIZE;
        const page = filteredUsuarios.slice(start, start + PAGE_SIZE);
        renderTable(page);
        renderPagination();
    }

    function renderTable(data) {
        usuariosBody.innerHTML = "";
        if (!data || data.length === 0) {
            emptyState.classList.add("visible");
            paginationBar.style.display = "none";
            return;
        }
        emptyState.classList.remove("visible");
        paginationBar.style.display = "flex";

        data.forEach((u) => {
            const estado = u.bloqueado ? "BLOQUEADO" : "ACTIVO";
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="cell-id">${u.id}</td>
                <td>${u.cedula || "-"}</td>
                <td>${u.nombres || "-"}</td>
                <td>${u.apellidos || "-"}</td>
                <td>${u.username || "-"}</td>
                <td>${u.correo || "-"}</td>
                <td>${u.cargo || "-"}</td>
                <td>${u.rol || "-"}</td>
                <td><span class="badge badge--${estado}">${estado}</span></td>
                <td class="cell-actions" data-id="${u.id}"></td>
            `;
            const actions = tr.querySelector(".cell-actions");
            actions.appendChild(actionBtn("Ver", "btn-outline", () => openView(u.id)));
            actions.appendChild(actionBtn("Editar", "btn-outline", () => openEdit(u.id)));
            if (u.bloqueado) {
                actions.appendChild(actionBtn("Desbloquear", "btn-warning", () => {
                    confirmAction = () => desbloquear(u.id);
                    openConfirm("Desbloquear Usuario", "¿Esta seguro de desbloquear este usuario?");
                }));
            } else {
                actions.appendChild(actionBtn("Bloquear", "btn-danger", () => {
                    confirmAction = () => bloquear(u.id);
                    openConfirm("Bloquear Usuario", "¿Esta seguro de bloquear este usuario?");
                }));
            }
            usuariosBody.appendChild(tr);
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
        const total = filteredUsuarios.length;
        const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;
        paginationCurrent.textContent = currentPage;
        const start = (currentPage - 1) * PAGE_SIZE + 1;
        const end = Math.min(currentPage * PAGE_SIZE, total);
        paginationInfo.textContent = total > 0 ? `${start}-${end} de ${total}` : "0 resultados";
        btnPrev.disabled = currentPage <= 1;
        btnNext.disabled = currentPage >= totalPages;
    }

    function filterUsuarios() {
        const q = searchInput.value.toLowerCase().trim();
        searchClear.classList.toggle("visible", q.length > 0);
        filteredUsuarios = q ? allUsuarios.filter((u) =>
            (u.cedula || "").toLowerCase().includes(q) ||
            (u.nombres || "").toLowerCase().includes(q) ||
            (u.apellidos || "").toLowerCase().includes(q) ||
            (u.username || "").toLowerCase().includes(q) ||
            (u.correo || "").toLowerCase().includes(q) ||
            (u.cargo || "").toLowerCase().includes(q)
        ) : [...allUsuarios];
        currentPage = 1;
        render();
    }

    searchInput.addEventListener("input", filterUsuarios);
    searchClear.addEventListener("click", () => { searchInput.value = ""; searchClear.classList.remove("visible"); filterUsuarios(); searchInput.focus(); });
    btnPrev.addEventListener("click", () => { if (currentPage > 1) { currentPage--; render(); } });
    btnNext.addEventListener("click", () => { const max = Math.ceil(filteredUsuarios.length / PAGE_SIZE); if (currentPage < max) { currentPage++; render(); } });

    function resetForm() {
        formUserId.value = "";
        userForm.reset();
        formError.classList.remove("visible");
        formError.textContent = "";
        passwordGroup.style.display = "block";
        formPassword.required = true;
        formPassword.value = "";
        formCedula.disabled = false;
        formUsername.disabled = false;
    }

    function openCreate() {
        resetForm();
        modalUserTitle.textContent = "Nuevo Usuario";
        modalUserSave.textContent = "Crear Usuario";
        modalUserSave.dataset.mode = "create";
        modalUser.classList.add("open");
    }

    document.getElementById("btnNuevo").addEventListener("click", openCreate);

    async function openEdit(id) {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Cargando datos...");
        try {
            const resp = await fetch(`${API_BASE}/usuarios/${id}`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) { showToast(body.mensaje || "Error al obtener usuario", "error"); return; }
            const u = body.data;
            resetForm();
            modalUserTitle.textContent = "Editar Usuario";
            modalUserSave.textContent = "Guardar Cambios";
            modalUserSave.dataset.mode = "edit";
            passwordGroup.style.display = "none";
            formPassword.required = false;
            formCedula.disabled = true;
            formUsername.disabled = true;
            formUserId.value = u.id;
            formCedula.value = u.cedula || "";
            formNombres.value = u.nombres || "";
            formApellidos.value = u.apellidos || "";
            formUsername.value = u.username || "";
            formCorreo.value = u.correo || "";
            formCargo.value = u.cargo || "";
            formEmpresa.value = u.empresa || "";
            formLugarTrabajo.value = u.lugarTrabajo || "";
            formRol.value = u.rol || "";
            modalUser.classList.add("open");
        } catch (_) {
            showToast("Error de conexion.", "error");
        } finally {
            setLoading(false);
        }
    }

    async function openView(id) {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, "Cargando datos...");
        try {
            const resp = await fetch(`${API_BASE}/usuarios/${id}`, { headers: { Authorization: `Bearer ${token}` } });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (!body.success) { showToast(body.mensaje || "Error al obtener usuario", "error"); return; }
            const u = body.data;
            const estado = u.bloqueado ? "BLOQUEADO" : "ACTIVO";
            modalViewBody.innerHTML = `
                <div class="detail-grid">
                    <div class="detail-field"><span class="detail-label">ID</span><span class="detail-value">${u.id}</span></div>
                    <div class="detail-field"><span class="detail-label">Estado</span><span class="badge badge--${estado}">${estado}</span></div>
                    <div class="detail-field"><span class="detail-label">Cedula</span><span class="detail-value">${u.cedula || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Nombres</span><span class="detail-value">${u.nombres || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Apellidos</span><span class="detail-value">${u.apellidos || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Username</span><span class="detail-value">${u.username || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Correo</span><span class="detail-value">${u.correo || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Cargo</span><span class="detail-value">${u.cargo || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Empresa</span><span class="detail-value">${u.empresa || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Lugar Trabajo</span><span class="detail-value">${u.lugarTrabajo || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Rol</span><span class="detail-value">${u.rol || "-"}</span></div>
                </div>`;
            modalView.classList.add("open");
        } catch (_) {
            showToast("Error de conexion.", "error");
        } finally {
            setLoading(false);
        }
    }

    async function bloquear(id) {
        await toggleBlock(id, `${API_BASE}/usuarios/${id}/bloquear`, "bloqueado");
    }

    async function desbloquear(id) {
        await toggleBlock(id, `${API_BASE}/usuarios/${id}/desbloquear`, "desbloqueado");
    }

    async function toggleBlock(id, url, verb) {
        const token = checkAuth();
        if (!token) return;
        setLoading(true, `Procesando...`);
        try {
            const resp = await fetch(url, { method: "PATCH", headers: authHeaders() });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast(`Usuario ${verb} exitosamente.`, "success");
                closeConfirm();
                await loadUsuarios();
            } else {
                showToast(body.mensaje || `Error al ${verb} usuario.`, "error");
            }
        } catch (_) {
            showToast("Error de conexion.", "error");
        } finally {
            setLoading(false);
        }
    }

    modalConfirmOk.addEventListener("click", () => {
        if (confirmAction) { const fn = confirmAction; confirmAction = null; fn(); }
    });

    function closeConfirm() {
        modalConfirm.classList.remove("open");
        confirmAction = null;
    }

    function openConfirm(title, text) {
        modalConfirmTitle.textContent = title;
        modalConfirmText.textContent = text;
        modalConfirm.classList.add("open");
    }

    async function handleFormSubmit() {
        const mode = modalUserSave.dataset.mode;
        formError.classList.remove("visible");
        formError.textContent = "";

        const cedula = formCedula.value.trim();
        const nombres = formNombres.value.trim();
        const apellidos = formApellidos.value.trim();
        const username = formUsername.value.trim();
        const correo = formCorreo.value.trim();
        const password = formPassword.value;
        const cargo = formCargo.value.trim();
        const empresa = formEmpresa.value.trim();
        const lugarTrabajo = formLugarTrabajo.value.trim();
        const rol = formRol.value;

        if (!cedula) { showError("La cedula es obligatoria."); return; }
        if (!nombres) { showError("Los nombres son obligatorios."); return; }
        if (!apellidos) { showError("Los apellidos son obligatorios."); return; }
        if (!username) { showError("El username es obligatorio."); return; }
        if (!correo) { showError("El correo es obligatorio."); return; }
        if (!rol) { showError("El rol es obligatorio."); return; }
        if (mode === "create" && !password) { showError("La password es obligatoria."); return; }

        const payload = { cedula, nombres, apellidos, username, correo, cargo, empresa, lugarTrabajo, rol };
        if (mode === "create") payload.password = password;

        const token = checkAuth();
        if (!token) return;
        setLoading(true, mode === "create" ? "Creando usuario..." : "Guardando cambios...");
        modalUserSave.disabled = true;

        try {
            const id = formUserId.value;
            const url = mode === "create" ? `${API_BASE}/usuarios` : `${API_BASE}/usuarios/${id}`;
            const method = mode === "create" ? "POST" : "PUT";
            const resp = await fetch(url, { method, headers: authHeaders(), body: JSON.stringify(payload) });
            if (handle401(resp)) return;
            const body = await resp.json();
            if (body.success) {
                showToast(mode === "create" ? "Usuario creado exitosamente." : "Usuario actualizado exitosamente.", "success");
                closeModalUser();
                await loadUsuarios();
            } else {
                showToast(body.mensaje || "Error al guardar usuario.", "error");
            }
        } catch (_) {
            showToast("Error de conexion.", "error");
        } finally {
            setLoading(false);
            modalUserSave.disabled = false;
        }
    }

    function showError(msg) {
        formError.textContent = msg;
        formError.classList.add("visible");
    }

    function closeModalUser() { modalUser.classList.remove("open"); }

    modalUserClose.addEventListener("click", closeModalUser);
    modalUserCancel.addEventListener("click", closeModalUser);
    modalUserSave.addEventListener("click", handleFormSubmit);
    modalUser.addEventListener("click", (e) => { if (e.target === modalUser) closeModalUser(); });

    modalViewClose.addEventListener("click", () => modalView.classList.remove("open"));
    modalView.addEventListener("click", (e) => { if (e.target === modalView) modalView.classList.remove("open"); });

    modalConfirmClose.addEventListener("click", closeConfirm);
    modalConfirmCancel.addEventListener("click", closeConfirm);
    modalConfirm.addEventListener("click", (e) => { if (e.target === modalConfirm) closeConfirm(); });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            if (modalConfirm.classList.contains("open")) closeConfirm();
            else if (modalUser.classList.contains("open")) closeModalUser();
            else if (modalView.classList.contains("open")) modalView.classList.remove("open");
        }
    });

    sidebarToggle.addEventListener("click", () => sidebar.classList.toggle("open"));
    document.addEventListener("click", (e) => {
        if (window.innerWidth <= 768 && sidebar.classList.contains("open") && !sidebar.contains(e.target) && !sidebarToggle.contains(e.target)) {
            sidebar.classList.remove("open");
        }
    });
    window.addEventListener("resize", () => { if (window.innerWidth > 768) sidebar.classList.remove("open"); });

    logoutBtn.addEventListener("click", () => { LoginService.cerrarSesion(); window.location.href = ROUTES.LOGIN; });

    function buildSidebar(role) {
        const nav = document.getElementById("sidebarNav");
        const sections = {
            ADMINISTRADOR: [{ section: "Gestion", items: [{ label: "Usuarios", href: "usuarios.html", icon: "users" }, { label: "Actas", href: "actas.html", icon: "file-text" },                     { label: "Firmas", href: "firmas.html", icon: "pen-tool" }] }, { section: "Actas", items: [{ label: "Generar Acta", href: "generar-acta.html", icon: "plus-circle" }, { label: "Acta Entrega", href: "acta-entrega.html", icon: "file-text" }, { label: "Acta Devolucion", href: "acta-devolucion.html", icon: "file-text" }] }],
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

    (async function init() {
        await loadUserInfo();
        buildSidebar(userRole.textContent || "ADMINISTRADOR");
        await loadUsuarios();
    })();
})();
