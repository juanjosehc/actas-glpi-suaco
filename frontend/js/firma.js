(() => {
    
    // DOM refs
    const $ = (id) => document.getElementById(id);
    const stateLoading = $("stateLoading");
    const stateError = $("stateError");
    const stateSuccess = $("stateSuccess");
    const stateRejected = $("stateRejected");
    const firmaCard = $("firmaCard");
    // OTP
    const stateOtp = $("stateOtp");
    const otpInput = $("otpInput");
    const otpCorreo = $("otpCorreo");
    const otpInfo = $("otpInfo");
    const otpError = $("otpError");
    const btnOtpValidar = $("btnOtpValidar");
    const btnOtpReenviar = $("btnOtpReenviar");
    let otpCountdown = null;
    const errorMessage = $("errorMessage");
    const btnSubmit = $("btnSubmit");
    const btnReject = $("btnReject");
    const signatureError = $("signatureError");
    const photoError = $("photoError");

    // Info fields
    const firmaEstado = $("firmaEstado");
    const infoTipoActa = $("infoTipoActa");
    const infoNombreUsuario = $("infoNombreUsuario");
    const infoCedula = $("infoCedula");
    const infoCedulaField = $("infoCedulaField");
    const infoCorreo = $("infoCorreo");
    const infoEquipo = $("infoEquipo");
    const infoSerial = $("infoSerial");
    const infoPlaca = $("infoPlaca");
    const infoTicket = $("infoTicket");

    // PDF viewer
    const pdfLoading = $("pdfLoading");
    const pdfError = $("pdfError");
    const pdfViewerWrap = $("pdfViewerWrap");
    const pdfViewer = $("pdfViewer");

    // Documentos a firmar (expediente ENTREGA: acta + checklist)
    const docActaCard = $("docActaCard");
    const docChecklistCard = $("docChecklistCard");
    const firmaDocsNotice = $("firmaDocsNotice");
    const firmaConfirmWrap = $("firmaConfirmWrap");
    const firmaConfirm = $("firmaConfirm");
    const confirmError = $("confirmError");
    let activeDoc = "acta";
    let tieneChecklist = false;

    // Reject modal
    const rejectOverlay = $("rejectOverlay");
    const rejectMotivo = $("rejectMotivo");
    const rejectError = $("rejectError");
    const btnCancelReject = $("btnCancelReject");
    const btnConfirmReject = $("btnConfirmReject");

    // Signature
    const canvas = $("signatureCanvas");
    const ctx = canvas.getContext("2d");
    let hasDrawn = false;

    // Camera
    const cameraPreview = $("cameraPreview");
    const cameraPlaceholder = $("cameraPlaceholder");
    const cameraCaptured = $("cameraCaptured");
    const btnOpenCamera = $("btnOpenCamera");
    const btnTakePhoto = $("btnTakePhoto");
    const btnRetake = $("btnRetake");
    const btnClearSignature = $("btnClearSignature");
    let mediaStream = null;
    let photoCaptured = false;

    // Token
    const token = new URLSearchParams(window.location.search).get("token");

    // Sesion OTP validada (sobrevive recargas de la misma pestana)
    let sesion = sessionStorage.getItem("otp_sesion_" + token);

    // Estado terminal por pestana: tras una firma exitosa la pantalla de exito
    // persiste ante recargas. Sin esta marca, recargar tras firmar re-ejecutaba
    // initOtp -> loadActa y el backend (token ya usado) respondia 401/400,
    // reemplazando la pantalla de exito por "Firma no disponible".
    // Una apertura nueva (otra pestana/navegador, sin marca) sigue mostrando
    // "Este enlace ya fue utilizado", que es la pantalla correcta.
    const FIRMA_HECHA_KEY = "firma_hecha_" + token;

    // TOAST (patron SAUCO; la pagina publica no carga system.css)
    function showToast(message, type) {
        var container = document.getElementById("firmaToasts");
        if (!container) {
            container = document.createElement("div");
            container.className = "firma-toast-container";
            container.id = "firmaToasts";
            document.body.appendChild(container);
        }
        var toast = document.createElement("div");
        toast.className = "firma-toast toast-" + (type || "info");
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(function () {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 4000);
    }

    // TRACE TEMPORAL: evidencia de ejecucion en consola (DevTools). Quitar al confirmar fix.
    const TRACE = true;
    function traza(tag, detalle) {
        if (TRACE) console.log("[TRACE][" + tag + "] " + (detalle || ""));
    }
    console.log("[firma.js] CACHE-BUST v20260825 | token=" + (token ? token.slice(0, 8) : "NULL"));

    // =========================
    //  INIT
    // =========================

    if (!token) {
        showError("El enlace de firma no es valido. Falta el token de seguridad.");
        return;
    }

    initOtp();

    // =========================
    //  OTP
    // =========================

    function otpHeaders() {
        return sesion ? { "X-OTP-Sesion": sesion } : {};
    }

    function initOtp() {
        traza("initOtp", "flag=" + String(sessionStorage.getItem(FIRMA_HECHA_KEY)) + " sesion=" + (sesion ? sesion.slice(0, 8) : "NULL"));
        // Firma ya hecha en esta pestana: estado terminal sin llamadas al backend
        // (cualquier GET ahora responderia "token ya usado").
        if (sessionStorage.getItem(FIRMA_HECHA_KEY)) {
            traza("initOtp", "FLAG presente -> mostrarExito");
            mostrarExito();
            return;
        }
        stopOtpCountdown();
        otpInput.value = "";
        hideFieldError(otpError);
        otpInfo.textContent = "";
        firmaCard.style.display = "none";
        stateOtp.style.display = "none";
        stateLoading.style.display = "flex";

        // Sesion en memoria/sessionStorage: cargar directo; el server la valida (401 -> volver a OTP).
        if (sesion) {
            traza("initOtp", "sesion presente -> loadActa directo");
            loadActa();
            return;
        }
        traza("initOtp", "sin sesion -> fetchOtpEstado");
        fetchOtpEstado();
    }

    function fetchOtpEstado() {
        traza("fetchOtpEstado", "GET /firma/.../otp/estado");
        if (sessionStorage.getItem(FIRMA_HECHA_KEY)) {
            traza("fetchOtpEstado", "FLAG presente -> mostrarExito");
            mostrarExito();
            return;
        }
        fetch(API_BASE + "/firma/" + encodeURIComponent(token) + "/otp/estado", { headers: otpHeaders() })
            .then(function (r) { return r.json(); })
            .then(function (body) {
                traza("fetchOtpEstado", "HTTP respuesta success=" + body.success + " mensaje=" + (body.mensaje || ""));
                if (!body.success) {
                    traza("fetchOtpEstado", "=> showError(" + body.mensaje + ") => PANTALLA FIRMA NO DISPONIBLE");
                    showError(body.mensaje || "El enlace de firma no es valido o ha expirado.");
                    return;
                }
                const d = body.data;
                if (d.valido && sesion) {
                    traza("fetchOtpEstado", "valido+sesion -> loadActa");
                    loadActa();
                    return;
                }
                traza("fetchOtpEstado", "-> renderOtpForm");
                renderOtpForm(d);
            })
            .catch(function () {
                traza("fetchOtpEstado", "catch -> showError (conexion)");
                showError("No se pudo conectar con el servidor. Verifique su conexion e intente de nuevo.");
            });
    }

    function renderOtpForm(d) {
        if (d.correoEnmascarado) {
            otpCorreo.textContent = d.correoEnmascarado;
        }

        btnOtpReenviar.classList.remove("btn-primary");
        btnOtpReenviar.classList.add("btn-outline");
        btnOtpReenviar.disabled = false;
        btnOtpValidar.classList.remove("loading");
        btnOtpValidar.disabled = false;
        stopOtpCountdown();

        if (d.cooldownSegundos > 0) {
            disableResend(d.cooldownSegundos);
        } else if (d.codigoVencido) {
            otpInfo.textContent = "El codigo expiro. Solicite uno nuevo.";
            btnOtpReenviar.classList.remove("btn-outline");
            btnOtpReenviar.classList.add("btn-primary");
        } else if (d.enviado === false) {
            otpInfo.textContent = "No se pudo enviar el codigo por correo. Puede solicitar un reenvio.";
        } else if (d.expiraSegundos != null) {
            let seg = d.expiraSegundos;
            otpInfo.textContent = "El codigo expira en " + seg + " segundos.";
            otpCountdown = setInterval(function () {
                seg--;
                if (seg <= 0) {
                    stopOtpCountdown();
                    otpInfo.textContent = "El codigo expiro. Solicite uno nuevo.";
                    btnOtpReenviar.classList.remove("btn-outline");
                    btnOtpReenviar.classList.add("btn-primary");
                } else {
                    otpInfo.textContent = "El codigo expira en " + seg + " segundos.";
                }
            }, 1000);
        } else {
            otpInfo.textContent = "";
        }

        if (d.reenviosRestantes === 0) {
            btnOtpReenviar.disabled = true;
        } else if (!otpInfo.textContent) {
            otpInfo.textContent = "Reenvios restantes: " + d.reenviosRestantes + ".";
        }

        stateLoading.style.display = "none";
        stateOtp.style.display = "flex";
        otpInput.focus();
    }

    function stopOtpCountdown() {
        if (otpCountdown) {
            clearInterval(otpCountdown);
            otpCountdown = null;
        }
    }

    btnOtpValidar.addEventListener("click", validarOtp);
    otpInput.addEventListener("keydown", function (e) {
        if (e.key === "Enter") validarOtp();
    });

    async function validarOtp() {
        const codigo = otpInput.value.trim();
        if (!/^\d{6}$/.test(codigo)) {
            otpError.textContent = "Ingrese el codigo de 6 digitos.";
            showFieldError(otpError);
            return;
        }
        hideFieldError(otpError);
        btnOtpValidar.classList.add("loading");
        btnOtpValidar.disabled = true;

        try {
            const resp = await fetch(API_BASE + "/firma/" + encodeURIComponent(token) + "/otp/validar", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ codigo })
            });
            const body = await resp.json();
            traza("validarOtp", "HTTP " + resp.status + " success=" + body.success + " sesion=" + (body.data && body.data.sesion ? String(body.data.sesion).slice(0, 8) : "-"));

            if (body.success) {
                sesion = body.data.sesion;
                sessionStorage.setItem("otp_sesion_" + token, sesion);
                traza("validarOtp", "sesion guardada -> loadActa");
                stopOtpCountdown();
                stateOtp.style.display = "none";
                loadActa();
                return;
            }

            otpInput.value = "";
            otpInput.focus();
            otpError.textContent = body.mensaje || "Codigo incorrecto o no valido.";
            showFieldError(otpError);
            if (String(body.mensaje || "").includes("expiro")) {
                btnOtpReenviar.classList.remove("btn-outline");
                btnOtpReenviar.classList.add("btn-primary");
                otpInfo.textContent = "El codigo expiro. Solicite uno nuevo.";
            }
        } catch {
            otpError.textContent = "No se pudo conectar con el servidor. Intente de nuevo.";
            showFieldError(otpError);
        } finally {
            btnOtpValidar.classList.remove("loading");
            btnOtpValidar.disabled = false;
        }
    }

    btnOtpReenviar.addEventListener("click", reenviarOtp);

    async function reenviarOtp() {
        btnOtpReenviar.disabled = true;
        try {
            const resp = await fetch(API_BASE + "/firma/" + encodeURIComponent(token) + "/otp/reenviar", {
                method: "POST",
                headers: { "Content-Type": "application/json" }
            });
            const body = await resp.json();
            if (body.success) {
                otpInput.value = "";
                hideFieldError(otpError);
                otpInfo.textContent = "Codigo enviado. Revise su correo.";
                fetchOtpEstado();
                return;
            }
            const msg = body.mensaje || "No se pudo reenviar el codigo.";
            const m = msg.match(/(\d+)/);
            if (msg.includes("Debe esperar") && m) {
                disableResend(parseInt(m[1], 10));
            } else {
                otpInfo.textContent = msg;
                btnOtpReenviar.disabled = false;
            }
        } catch {
            otpInfo.textContent = "No se pudo conectar con el servidor. Intente de nuevo.";
            btnOtpReenviar.disabled = false;
        }
    }

    function disableResend(segundos) {
        let restante = segundos;
        btnOtpReenviar.disabled = true;
        stopOtpCountdown();
        otpInfo.textContent = "Debe esperar para solicitar un nuevo codigo.";
        otpCountdown = setInterval(function () {
            restante--;
            if (restante <= 0) {
                stopOtpCountdown();
                btnOtpReenviar.disabled = false;
                otpInfo.textContent = "";
                fetchOtpEstado();
            } else {
                otpInfo.textContent = "Debe esperar " + restante + "s para solicitar un nuevo codigo.";
            }
        }, 1000);
    }

    function volverAOtp() {
        traza("volverAOtp", "sesion invalidada -> initOtp");
        sesion = null;
        sessionStorage.removeItem("otp_sesion_" + token);
        initOtp();
    }

    // =========================
    //  LOAD ACTA
    // =========================

    async function loadActa() {
        traza("loadActa", "GET /firma/" + token.slice(0, 8) + "...");
        try {
            const resp = await fetch(`${API_BASE}/firma/${encodeURIComponent(token)}`, { headers: otpHeaders() });
            traza("loadActa", "HTTP " + resp.status);

            if (resp.status === 401) {
                traza("loadActa", "401 -> volverAOtp");
                volverAOtp();
                return;
            }

            const body = await resp.json();

            if (!body.success) {
                traza("loadActa", "success=false -> showError(" + body.mensaje + ") => PANTALLA FIRMA NO DISPONIBLE");
                showError(body.mensaje || "El enlace de firma no es valido o ha expirado.");
                return;
            }

            const data = body.data;
            traza("loadActa", "200 ok estado=" + data.estado + " -> renderActa");
            renderActa(data);
        } catch (err) {
            const msg = err.message.includes("Failed to fetch")
                ? "No se pudo conectar con el servidor. Verifique su conexion e intente de nuevo."
                : "El enlace de firma no es valido o ha expirado.";
            traza("loadActa", "catch -> showError(" + msg + ")");
            showError(msg);
        }
    }

    function renderActa(data) {
        if (data.estado) {
            firmaEstado.innerHTML = '<span class="estado-badge">' + data.estado + '</span>';
        }

        infoTipoActa.textContent = data.tipoActa || "-";
        infoNombreUsuario.textContent = data.nombreUsuario || "-";
        infoCedula.textContent = data.cedulaUsuario || "-";
        if (infoCedulaField) {
            infoCedulaField.style.display = data.tipoActa === "DEVOLUCION" ? "" : "none";
        }
        infoCorreo.textContent = data.correoUsuario || "-";
        infoEquipo.textContent = data.descripcionEquipo || "-";
        infoSerial.textContent = data.serialEquipo || "-";
        infoPlaca.textContent = data.placaEquipo || "-";
        infoTicket.textContent = data.ticketGlpi != null ? String(data.ticketGlpi) : "-";

        setupDocs(data);

        stateLoading.style.display = "none";
        firmaCard.style.display = "block";

        setupCanvas();
    }

    /**
     * Expediente documental (ENTREGA): el firmante firma el acta y el checklist.
     * Ambos se muestran como documentos a firmar; el checklist solo existe en
     * ENTREGA (data.rutaPdfChecklist). Solo entonces se exige la confirmacion
     * explicita de revision antes de habilitar la firma.
     */
    function setupDocs(data) {
        tieneChecklist = data.tipoActa === "ENTREGA"
            && !!data.rutaPdfChecklist;

        if (docChecklistCard) {
            docChecklistCard.style.display = tieneChecklist ? "flex" : "none";
        }
        if (firmaDocsNotice) {
            firmaDocsNotice.style.display = tieneChecklist ? "block" : "none";
        }
        if (firmaConfirmWrap) {
            firmaConfirmWrap.style.display = tieneChecklist ? "flex" : "none";
        }
        if (firmaConfirm) {
            firmaConfirm.checked = false;
        }
        if (confirmError) {
            confirmError.style.display = "none";
        }

        setActiveDoc("acta");
        loadPdf("acta");
    }

    function setActiveDoc(doc) {
        activeDoc = doc;
        if (docActaCard) docActaCard.classList.toggle("doc-card--active", doc === "acta");
        if (docChecklistCard) docChecklistCard.classList.toggle("doc-card--active", doc === "checklist");
    }

    function cargarDocFirmar(doc) {
        setActiveDoc(doc);
        loadPdf(doc);
    }

    /**
     * Carga el PDF según el documento activo. El endpoint del checklist solo
     * existe para actas ENTREGA (el backend devuelve null en DEVOLUCION).
     */
    async function loadPdf(doc) {
        pdfLoading.style.display = "flex";
        pdfError.style.display = "none";
        pdfViewerWrap.style.display = "none";

        const url = doc === "checklist"
            ? API_BASE + "/firma/" + encodeURIComponent(token) + "/checklist/pdf"
            : API_BASE + "/firma/" + encodeURIComponent(token) + "/pdf";

        try {
            const r = await fetch(url, { headers: otpHeaders() });

            if (r.status === 401) {
                volverAOtp();
                return;
            }
            if (!r.ok) throw new Error("Error al cargar PDF");

            const blob = await r.blob();
            pdfLoading.style.display = "none";
            pdfViewer.src = URL.createObjectURL(blob);
            pdfViewerWrap.style.display = "block";
        } catch {
            pdfLoading.style.display = "none";
            pdfError.style.display = "block";
        }
    }

    function showError(msg) {
        traza("showError", "mensaje='" + msg + "' flag=" + String(sessionStorage.getItem(FIRMA_HECHA_KEY)));
        // Estado terminal por pestana: si la firma ya tuvo exito, ninguna
        // consulta posterior (recarga, timer, doble request) puede reemplazar
        // la pantalla de exito por un error derivado del token ya usado.
        if (sessionStorage.getItem(FIRMA_HECHA_KEY)) {
            traza("showError", "FLAG presente -> mostrarExito (exito preservado)");
            mostrarExito();
            return;
        }
        traza("showError", "=> PANTALLA FIRMA NO DISPONIBLE: '" + msg + "'");
        stateLoading.style.display = "none";
        errorMessage.textContent = msg;
        stateError.style.display = "flex";
    }

    function mostrarExito() {
        traza("mostrarExito", ">> PANTALLA EXITO (Firma Registrada) visible <<");
        stateLoading.style.display = "none";
        firmaCard.style.display = "none";
        stateOtp.style.display = "none";
        stateSuccess.style.display = "flex";
    }

    // =========================
    //  SIGNATURE CANVAS
    // =========================

    function setupCanvas() {
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;

        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);

        ctx.fillStyle = "#FFFFFF";
        ctx.fillRect(0, 0, rect.width, rect.height);

        ctx.strokeStyle = "#0F172A";
        ctx.lineWidth = 2.5;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";

        let drawing = false;
        hasDrawn = false;

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
            hideFieldError(signatureError);
        }

        function stop(e) {
            drawing = false;
        }

        canvas.addEventListener("mousedown", start);
        canvas.addEventListener("mousemove", move);
        canvas.addEventListener("mouseup", stop);
        canvas.addEventListener("mouseleave", stop);

        canvas.addEventListener("touchstart", start, { passive: false });
        canvas.addEventListener("touchmove", move, { passive: false });
        canvas.addEventListener("touchend", stop, { passive: false });
        canvas.addEventListener("touchcancel", stop, { passive: false });

        btnClearSignature.addEventListener("click", clearCanvas);

        window.addEventListener("resize", resizeCanvas);
    }

    function resizeCanvas() {
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        const tempData = canvas.toDataURL();

        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);

        const img = new Image();
        img.onload = () => {
            ctx.drawImage(img, 0, 0, rect.width, rect.height);
        };
        img.src = tempData;
    }

    function clearCanvas() {
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);
        ctx.fillStyle = "#FFFFFF";
        ctx.fillRect(0, 0, rect.width, rect.height);
        hasDrawn = false;
        hideFieldError(signatureError);
    }

    function isCanvasEmpty() {
        return !hasDrawn;
    }

    function getSignatureBase64() {
        return canvas.toDataURL("image/png").replace(/^data:image\/png;base64,/, "");
    }

    // =========================
    //  CAMERA
    // =========================

    btnOpenCamera.addEventListener("click", openCamera);

    async function openCamera() {
        if (mediaStream) return;

        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: "user", width: { ideal: 1280 }, height: { ideal: 720 } }
            });
            mediaStream = stream;
            cameraPreview.srcObject = stream;
            cameraPreview.style.display = "block";
            cameraPlaceholder.style.display = "none";
            cameraCaptured.style.display = "none";

            btnOpenCamera.style.display = "none";
            btnTakePhoto.style.display = "inline-flex";
            btnRetake.style.display = "none";
        } catch (err) {
            if (err.name === "NotAllowedError" || err.name === "PermissionDeniedError") {
                showToast("Debe permitir el acceso a la camara para tomarse una fotografia.", "warning");
            } else if (err.name === "NotFoundError") {
                showToast("No se detecto una camara en este dispositivo.", "warning");
            } else {
                showToast("No se pudo acceder a la camara. Verifique los permisos e intente de nuevo.", "error");
            }
        }
    }

    btnTakePhoto.addEventListener("click", takePhoto);

    function takePhoto() {
        if (!mediaStream) return;

        const track = mediaStream.getVideoTracks()[0];
        const settings = track.getSettings();
        const capCanvas = document.createElement("canvas");
        capCanvas.width = settings.width || 640;
        capCanvas.height = settings.height || 480;
        const capCtx = capCanvas.getContext("2d");
        capCtx.drawImage(cameraPreview, 0, 0, capCanvas.width, capCanvas.height);

        cameraCaptured.src = capCanvas.toDataURL("image/jpeg", 0.85);
        cameraCaptured.style.display = "block";
        cameraPreview.style.display = "none";
        stopCamera();

        btnTakePhoto.style.display = "none";
        btnRetake.style.display = "inline-flex";
        btnOpenCamera.style.display = "none";

        photoCaptured = true;
        hideFieldError(photoError);
    }

    btnRetake.addEventListener("click", retakePhoto);

    function retakePhoto() {
        cameraCaptured.style.display = "none";
        cameraCaptured.src = "";
        photoCaptured = false;
        openCamera();
    }

    function stopCamera() {
        if (mediaStream) {
            mediaStream.getTracks().forEach((t) => t.stop());
            mediaStream = null;
        }
    }

    function getPhotoBase64() {
        if (!photoCaptured || !cameraCaptured.src) return "";
        return cameraCaptured.src.replace(/^data:image\/jpeg;base64,/, "");
    }

    // =========================
    //  FIELD ERROR HELPERS
    // =========================

    function showFieldError(el) {
        el.style.display = "block";
    }

    function hideFieldError(el) {
        el.style.display = "none";
    }

    // =========================
    //  SUBMIT
    // =========================

    btnSubmit.addEventListener("click", submitFirma);

    if (docActaCard) docActaCard.addEventListener("click", () => cargarDocFirmar("acta"));
    if (docChecklistCard) docChecklistCard.addEventListener("click", () => cargarDocFirmar("checklist"));
    if (firmaConfirm) {
        firmaConfirm.addEventListener("change", () => { if (confirmError) confirmError.style.display = "none"; });
    }

    async function submitFirma() {
        traza("submitFirma", "CLICK Enviar: firma=" + String(!isCanvasEmpty()) + " foto=" + String(photoCaptured) + " checklist=" + String(tieneChecklist));
        let valid = true;

        if (isCanvasEmpty()) {
            showFieldError(signatureError);
            valid = false;
        } else {
            hideFieldError(signatureError);
        }

        if (!photoCaptured) {
            showFieldError(photoError);
            valid = false;
        } else {
            hideFieldError(photoError);
        }

        // Expediente ENTREGA: sin confirmacion explicita de revision de ambos
        // documentos, la firma no se habilita.
        if (tieneChecklist && firmaConfirm && !firmaConfirm.checked) {
            if (confirmError) showFieldError(confirmError);
            valid = false;
        } else if (confirmError) {
            hideFieldError(confirmError);
        }

        if (!valid) return;

        btnSubmit.classList.add("loading");
        btnSubmit.disabled = true;

        try {
            const firmaBase64 = getSignatureBase64();
            const fotoBase64 = getPhotoBase64();

            const resp = await fetch(`${API_BASE}/firma/${encodeURIComponent(token)}`, {
                method: "POST",
                headers: Object.assign({ "Content-Type": "application/json" }, otpHeaders()),
                body: JSON.stringify({ firmaBase64, fotoBase64 }),
            });

            const body = await resp.json();
            traza("submitFirma", "POST HTTP " + resp.status + " success=" + body.success + " mensaje=" + (body.mensaje || ""));

            if (resp.status === 401) {
                traza("submitFirma", "401 -> volverAOtp");
                volverAOtp();
                return;
            }

            if (body.success) {
                sessionStorage.setItem(FIRMA_HECHA_KEY, "1");
                traza("submitFirma", "EXITO: flag set -> PANTALLA FIRMA REGISTRADA");
                stopCamera();
                firmaCard.style.display = "none";
                stateSuccess.style.display = "flex";
            } else {
                showToast(body.mensaje || "Error al registrar la firma. Intente de nuevo.", "error");
            }
        } catch (err) {
            const msg = err.message.includes("Failed to fetch")
                ? "No se pudo conectar con el servidor. Verifique su conexion e intente de nuevo."
                : "Error al registrar la firma. Intente de nuevo.";
            showToast(msg, "error");
        } finally {
            btnSubmit.classList.remove("loading");
            btnSubmit.disabled = false;
        }
    }

    // =========================
    //  REJECT
    // =========================

    btnReject.addEventListener("click", openRejectModal);

    document.querySelectorAll(".reject-reason").forEach(function (btn) {
        btn.addEventListener("click", function () {
            document.querySelectorAll(".reject-reason").forEach(function (b) {
                b.classList.remove("selected");
            });
            btn.classList.add("selected");
            rejectMotivo.value = btn.dataset.reason;
            hideFieldError(rejectError);
        });
    });

    rejectMotivo.addEventListener("input", function () {
        hideFieldError(rejectError);
    });

    btnCancelReject.addEventListener("click", closeRejectModal);

    rejectOverlay.addEventListener("click", function (e) {
        if (e.target === rejectOverlay) closeRejectModal();
    });

    function openRejectModal() {
        rejectMotivo.value = "";
        document.querySelectorAll(".reject-reason").forEach(function (b) {
            b.classList.remove("selected");
        });
        hideFieldError(rejectError);
        rejectOverlay.style.display = "flex";
    }

    function closeRejectModal() {
        rejectOverlay.style.display = "none";
        btnConfirmReject.classList.remove("loading");
        btnConfirmReject.disabled = false;
    }

    btnConfirmReject.addEventListener("click", submitRechazo);

    async function submitRechazo() {
        const motivo = rejectMotivo.value.trim();

        if (!motivo) {
            showFieldError(rejectError);
            return;
        }

        btnConfirmReject.classList.add("loading");
        btnConfirmReject.disabled = true;

        try {
            const resp = await fetch(`${API_BASE}/firma/${encodeURIComponent(token)}/rechazar`, {
                method: "POST",
                headers: Object.assign({ "Content-Type": "application/json" }, otpHeaders()),
                body: JSON.stringify({ motivo }),
            });

            const body = await resp.json();

            if (resp.status === 401) {
                closeRejectModal();
                volverAOtp();
                return;
            }

            if (body.success) {
                stopCamera();
                closeRejectModal();
                firmaCard.style.display = "none";
                stateRejected.style.display = "flex";
            } else {
                closeRejectModal();
                showToast(body.mensaje || "Error al rechazar el acta. Intente de nuevo.", "error");
            }
        } catch (err) {
            closeRejectModal();
            const msg = err.message.includes("Failed to fetch")
                ? "No se pudo conectar con el servidor. Verifique su conexion e intente de nuevo."
                : "Error al rechazar el acta. Intente de nuevo.";
            showToast(msg, "error");
        }
    }
})();
