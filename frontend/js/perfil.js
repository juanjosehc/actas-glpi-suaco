(() => {
    "use strict";

        const $ = (id) => document.getElementById(id);

    const token = LoginService.obtenerToken();
    if (!token) return;

    // El modulo Perfil es la firma permanente del tecnico. AUDITOR no envia
    // actas ni firma: sin acceso (el backend tambien responde 403 en
    // /usuarios/me/firma* para este rol).
    const rolPerfil = LoginService.getRol ? LoginService.getRol() : localStorage.getItem("role");
    if (rolPerfil === "AUDITOR") {
        window.location.href = "actas.html";
        return;
    }

    const canvas = $("firmaCanvas");
    const ctx = canvas.getContext("2d");
    let hasDrawn = false;
    let drawing = false;

    const errorBox = $("firmaError");
    const estadoEl = $("firmaEstado");
    const preview = $("firmaPreview");
    const drawZone = $("firmaDraw");

    
    const modalConfirm = $("modalConfirm");
    const modalConfirmTitle = $("modalConfirmTitle");
    const modalConfirmText = $("modalConfirmText");
    const modalConfirmClose = $("modalConfirmClose");
    const modalConfirmCancel = $("modalConfirmCancel");
    const modalConfirmOk = $("modalConfirmOk");
    let confirmAction = null;

    function authHeaders() {
        return { Authorization: "Bearer " + token };
    }

    // =========================
    //  TOAST (mismo esquema que actas/acta-view)
    // =========================

    function showToast(message, type) {
        return mostrarNotificacion(message, type);
    }

    // =========================
    //  MODAL CONFIRM (mismo esquema que usuarios)
    // =========================

    function openConfirm(title, text, action) {
        modalConfirmTitle.textContent = title;
        modalConfirmText.textContent = text;
        confirmAction = action;
        modalConfirm.classList.add("open");
    }

    function closeConfirm() {
        modalConfirm.classList.remove("open");
        confirmAction = null;
    }

    modalConfirmClose.addEventListener("click", closeConfirm);
    modalConfirmCancel.addEventListener("click", closeConfirm);
    modalConfirmOk.addEventListener("click", function () {
        const action = confirmAction;
        closeConfirm();
        if (action) action();
    });
    modalConfirm.addEventListener("click", function (e) {
        if (e.target === modalConfirm) closeConfirm();
    });

    function showError(msg) {
        errorBox.textContent = msg;
        errorBox.style.display = "block";
    }

    function hideError() {
        errorBox.style.display = "none";
    }

    // =========================
    //  CANVAS (background transparento)
    // =========================

    function initCanvas() {
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, rect.width, rect.height);

        ctx.strokeStyle = "#0F172A";
        ctx.lineWidth = 2.5;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
    }

    function getPos(e) {
        const r = canvas.getBoundingClientRect();
        if (e.touches && e.touches.length > 0) {
            return { x: e.touches[0].clientX - r.left, y: e.touches[0].clientY - r.top };
        }
        return { x: e.clientX - r.left, y: e.clientY - r.top };
    }

    function start(e) {
        e.preventDefault();
        drawing = true;
        const p = getPos(e);
        ctx.beginPath();
        ctx.moveTo(p.x, p.y);
    }

    function move(e) {
        if (!drawing) return;
        e.preventDefault();
        const p = getPos(e);
        ctx.lineTo(p.x, p.y);
        ctx.stroke();
        hasDrawn = true;
        hideError();
    }

    function stop() { drawing = false; }

    function clearCanvas() {
        initCanvas();
        hasDrawn = false;
        hideError();
    }

    function isCanvasEmpty() { return !hasDrawn; }

    function getSignatureBase64() {
        return canvas.toDataURL("image/png").replace(/^data:image\/png;base64,/, "");
    }

    canvas.addEventListener("mousedown", start);
    canvas.addEventListener("mousemove", move);
    canvas.addEventListener("mouseup", stop);
    canvas.addEventListener("mouseleave", stop);
    canvas.addEventListener("touchstart", start, { passive: false });
    canvas.addEventListener("touchmove", move, { passive: false });
    canvas.addEventListener("touchend", stop, { passive: false });
    canvas.addEventListener("touchcancel", stop, { passive: false });

    $("btnLimpiar").addEventListener("click", clearCanvas);

    // =========================
    //  VISTA PREVIA DE LA FIRMA
    //  La firma NO se sirve por /uploads (no hay handler estatico): se carga
    //  autenticada via /usuarios/me/firma/archivo y se muestra como blob.
    // =========================

    async function cargarPreview() {
        const img = $("firmaPreviewImg");
        try {
            const resp = await fetch(API_BASE + "/usuarios/me/firma/archivo", { headers: authHeaders() });
            if (!resp.ok) {
                estadoEl.textContent = "No se pudo cargar la imagen de la firma.";
                estadoEl.style.color = "#b91c1c";
                return;
            }
            const blob = await resp.blob();
            if (img.src && img.src.indexOf("blob:") === 0) URL.revokeObjectURL(img.src);
            img.src = URL.createObjectURL(blob);
        } catch {
            estadoEl.textContent = "No se pudo cargar la imagen de la firma.";
            estadoEl.style.color = "#b91c1c";
        }
    }

    function renderFirma(d) {
        if (d.tiene) {
            preview.style.display = "flex";
            drawZone.style.display = "none";
            $("firmaPreviewFecha").textContent = "Actualizada: " + (d.fechaActualizacion ? d.fechaActualizacion.replace("T", " ").slice(0, 16) : "-");
            estadoEl.textContent = "Tiene firma registrada.";
            estadoEl.style.color = "#16a34a";
            cargarPreview();
        } else {
            preview.style.display = "none";
            drawZone.style.display = "block";
            initCanvas();
            estadoEl.textContent = "No tiene firma registrada. Dibuje su firma abajo.";
            estadoEl.style.color = "#475569";
        }
    }

    $("btnReemplazar").addEventListener("click", function () {
        preview.style.display = "none";
        drawZone.style.display = "block";
        initCanvas();
    });

    $("btnEliminar").addEventListener("click", function () {
        openConfirm(
            "Eliminar firma",
            "¿Eliminar su firma permanente? Se quitara de futuras actas.",
            async function () {
                try {
                    const resp = await fetch(API_BASE + "/usuarios/me/firma", {
                        method: "DELETE",
                        headers: authHeaders()
                    });
                    const body = await resp.json();
                    if (body.success) {
                        showToast("Firma eliminada.", "success");
                        cargarFirma();
                    } else {
                        showToast(body.mensaje || "No se pudo eliminar la firma.", "error");
                    }
                } catch {
                    showToast("No se pudo conectar con el servidor.", "error");
                }
            }
        );
    });

    // =========================
    //  GUARDAR
    // =========================

    $("btnGuardar").addEventListener("click", async function () {
        if (isCanvasEmpty()) {
            showError("Dibuje su firma antes de guardar.");
            return;
        }
        const btn = this;
        btn.classList.add("loading");
        btn.disabled = true;
        try {
            const resp = await fetch(API_BASE + "/usuarios/me/firma", {
                method: "PUT",
                headers: Object.assign({ "Content-Type": "application/json" }, authHeaders()),
                body: JSON.stringify({ firmaBase64: getSignatureBase64() })
            });
            const body = await resp.json();
            if (body.success) {
                showToast("Firma guardada. Se usara en todas sus actas.", "success");
                clearCanvas();
                cargarFirma();
            } else {
                showToast(body.mensaje || "No se pudo guardar la firma.", "error");
            }
        } catch {
            showToast("No se pudo conectar con el servidor.", "error");
        } finally {
            btn.classList.remove("loading");
            btn.disabled = false;
        }
    });

    // =========================
    //  CARGA INICIAL
    // =========================

    async function cargarFirma() {
        try {
            const resp = await fetch(API_BASE + "/usuarios/me/firma", { headers: authHeaders() });
            if (resp.status === 401) {
                LoginService.cerrarSesion();
                window.location.href = ROUTES.LOGIN;
                return;
            }
            const body = await resp.json();
            if (body.success) {
                renderFirma(body.data);
            } else {
                showError(body.mensaje || "No se pudo consultar la firma.");
            }
        } catch {
            showError("No se pudo conectar con el servidor.");
        }
    }

    // Datos de la cuenta
    async function cargarDatos() {
        try {
            const resp = await fetch(API_BASE + "/usuarios/me", { headers: authHeaders() });
            const body = await resp.json();
            if (body.success) {
                const u = body.data;
                $("datosNombre").textContent = (u.nombres || "") + " " + (u.apellidos || "");
                $("datosCorreo").textContent = u.correo || "-";
                $("datosRol").textContent = u.rol || "-";
                $("datosCargo").textContent = u.cargo || "-";
            }
        } catch { /* noop */ }
    }

    document.addEventListener("DOMContentLoaded", function () {
        cargarFirma();
        cargarDatos();
    });
})();