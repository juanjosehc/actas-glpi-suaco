(() => {
    const API_BASE = "http://localhost:8001";
    const PAGE_SIZE = 10;

    let allUsuarios = [];
    let filteredUsuarios = [];
    let currentPage = 1;
    let confirmAction = null;

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
            const protegido = !!u.protegido;
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
                <td>
                    <span class="badge badge--${estado}">${estado}</span>
                    ${protegido ? '<span class="badge badge--ACTIVO" title="Administrador principal: no puede bloquearse ni cambiar su rol">PROTEGIDO</span>' : ""}
                </td>
                <td class="cell-actions" data-id="${u.id}"></td>
            `;
            const actions = tr.querySelector(".cell-actions");
            actions.appendChild(actionBtn("Ver", "btn-outline", () => openView(u.id)));
            actions.appendChild(actionBtn("Editar", "btn-outline", () => openEdit(u.id)));
            if (protegido) {
                actions.appendChild(actionBtn("🔒", "btn-outline", () => {}));
                actions.lastChild.title = "Administrador protegido: no puede bloquearse ni cambiar su rol";
            } else if (u.bloqueado) {
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
        formRol.disabled = false;
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
            if (u.protegido) {
                formRol.disabled = true;
                formError.classList.add("visible");
                formError.textContent = "Administrador principal: el rol no puede modificarse.";
            }
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
                    <div class="detail-field"><span class="detail-label">Estado</span><span class="badge badge--${estado}">${estado}</span>${u.protegido ? '<span class="badge badge--ACTIVO">PROTEGIDO</span>' : ""}</div>
                    <div class="detail-field"><span class="detail-label">Cedula</span><span class="detail-value">${u.cedula || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Nombres</span><span class="detail-value">${u.nombres || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Apellidos</span><span class="detail-value">${u.apellidos || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Username</span><span class="detail-value">${u.username || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Correo</span><span class="detail-value">${u.correo || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Cargo</span><span class="detail-value">${u.cargo || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Empresa</span><span class="detail-value">${u.empresa || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Lugar Trabajo</span><span class="detail-value">${u.lugarTrabajo || "-"}</span></div>
                    <div class="detail-field"><span class="detail-label">Rol</span><span class="detail-value">${u.rol || "-"}</span></div>
                </div>
                ${u.protegido ? '<p class="modal-desc" style="margin-top:8px;color:#B45309;">Administrador principal: no puede bloquearse, desactivarse ni cambiarsele el rol.</p>' : ""}`;
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

    (async function init() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return; }
        await loadUsuarios();
    })();
})();
