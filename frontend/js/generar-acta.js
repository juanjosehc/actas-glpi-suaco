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

    function init() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return; }
    }

    btnPreview.addEventListener("click", showPreview);
    btnSave.addEventListener("click", saveActa);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
