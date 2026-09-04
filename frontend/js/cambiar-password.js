(() => {
    "use strict";

    const form = document.getElementById("cambiarPasswordForm");
    const actualInput = document.getElementById("passwordActual");
    const nuevaInput = document.getElementById("nuevaPassword");
    const confirmarInput = document.getElementById("confirmarPassword");
    const errorBox = document.getElementById("passError");
    const btnCambiar = document.getElementById("btnCambiar");
    const btnVolver = document.getElementById("btnVolver");
    const forzarBanner = document.getElementById("forzarBanner");

    // Cambio forzado (reset por admin) o voluntario.
    const forzar = new URLSearchParams(window.location.search).get("forzar") === "1";

    if (forzar) {
        forzarBanner.hidden = false;
        document.querySelector(".page-subtitle").textContent =
            "Su contrasena fue restablecida por un administrador.";
        btnVolver.style.display = "none";
    }

    function showError(msg) {
        errorBox.textContent = msg;
        errorBox.classList.add("visible");
    }

    function hideError() {
        errorBox.classList.remove("visible");
        errorBox.textContent = "";
    }

    function setLoading(loading) {
        btnCambiar.disabled = loading;
        btnCambiar.classList.toggle("loading", loading);
    }

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideError();

        const passwordActual = actualInput.value;
        const nuevaPassword = nuevaInput.value;
        const confirmar = confirmarInput.value;

        if (!passwordActual) { showError("La contrasena actual es obligatoria."); actualInput.focus(); return; }
        if (nuevaPassword.length < 8) { showError("La nueva contrasena debe tener minimo 8 caracteres."); nuevaInput.focus(); return; }
        if (nuevaPassword === passwordActual) { showError("La nueva contrasena debe ser distinta a la actual."); nuevaInput.focus(); return; }
        if (nuevaPassword !== confirmar) { showError("Las contrasenas no coinciden."); confirmarInput.focus(); return; }

        setLoading(true);
        try {
            const token = LoginService.obtenerToken();
            const resp = await fetch(`${API_BASE}/auth/cambiar-password`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                },
                body: JSON.stringify({ passwordActual, nuevaPassword }),
            });
            const body = await resp.json();

            if (resp.status === 401) {
                LoginService.cerrarSesion();
                window.location.href = ROUTES.LOGIN;
                return;
            }
            if (!body.success) {
                showError(body.mensaje || "No se pudo cambiar la contrasena.");
                return;
            }

            mostrarNotificacion("Contrasena cambiada correctamente.", "success");
            window.location.href = forzar ? ROUTES.HOME : "perfil.html";
        } catch (_) {
            showError("El servidor no esta disponible. Intente de nuevo.");
        } finally {
            setLoading(false);
        }
    });

    [actualInput, nuevaInput, confirmarInput].forEach((input) => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");
            if (errorBox.classList.contains("visible")) hideError();
        });
    });
})();