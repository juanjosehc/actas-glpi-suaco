(() => {
    const form = document.getElementById("loginForm");
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const errorBox = document.getElementById("loginError");
    const errorText = document.getElementById("loginErrorText");
    const loginBtn = document.getElementById("loginBtn");

    const forgotLink = document.getElementById("forgotLink");
    const recoverPanel = document.getElementById("recoverPanel");
    const backToLogin = document.getElementById("backToLogin");
    const recoverForm = document.getElementById("recoverForm");
    const recoverEmail = document.getElementById("recoverEmail");
    const recoverBtn = document.getElementById("recoverBtn");
    const recoverError = document.getElementById("recoverError");
    const recoverErrorText = document.getElementById("recoverErrorText");
    const recoverSuccess = document.getElementById("recoverSuccess");

    function showError(message) {
        errorText.textContent = message;
        errorBox.classList.add("visible");
    }

    function hideError() {
        errorBox.classList.remove("visible");
        errorText.textContent = "";
    }

    function showRecoverError(message) {
        recoverErrorText.textContent = message;
        recoverError.classList.add("visible");
    }

    function hideRecoverError() {
        recoverError.classList.remove("visible");
        recoverErrorText.textContent = "";
    }

    function setLoading(loading) {
        loginBtn.disabled = loading;
        loginBtn.classList.toggle("loading", loading);
        usernameInput.disabled = loading;
        passwordInput.disabled = loading;
    }

    function setRecoverLoading(loading) {
        recoverBtn.disabled = loading;
        recoverBtn.classList.toggle("loading", loading);
        recoverEmail.disabled = loading;
    }

    function clearInvalidStyles() {
        usernameInput.classList.remove("is-invalid");
        passwordInput.classList.remove("is-invalid");
        recoverEmail.classList.remove("is-invalid");
    }

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideError();
        clearInvalidStyles();

        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        if (!username) {
            usernameInput.classList.add("is-invalid");
            showError("El usuario es obligatorio");
            usernameInput.focus();
            return;
        }

        if (!password) {
            passwordInput.classList.add("is-invalid");
            showError("La contrasena es obligatoria");
            passwordInput.focus();
            return;
        }

        setLoading(true);

        try {
            const data = await LoginService.login(username, password);

            LoginService.guardarSesion({
                token: data.token,
                username: data.username,
                role: data.role,
            });

            // Reset por administrador (cambiarPasswordObligatorio=true): se
            // fuerza el cambio antes de entrar. El flag se limpia al cambiar.
            if (data.cambiarPasswordObligatorio) {
                window.location.href = "cambiar-password.html?forzar=1";
            } else {
                window.location.href = ROUTES.HOME;
            }
        } catch (err) {
            const msg = err.message.includes("Failed to fetch")
                ? "El servidor no esta disponible. Intente de nuevo."
                : err.message;
            showError(msg);
        } finally {
            setLoading(false);
        }
    });

    [usernameInput, passwordInput].forEach((input) => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");
            if (errorBox.classList.contains("visible")) {
                hideError();
            }
        });
    });

    // =========================
    //  RECUPERACION DE CONTRASENA
    // =========================

    function toggleRecover(show) {
        recoverPanel.hidden = !show;
        form.hidden = show;
        hideError();
        hideRecoverError();
        recoverSuccess.hidden = true;
        if (show) {
            recoverEmail.focus();
        } else {
            usernameInput.focus();
        }
    }

    forgotLink.addEventListener("click", () => toggleRecover(true));
    backToLogin.addEventListener("click", () => toggleRecover(false));

    recoverForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideRecoverError();
        recoverEmail.classList.remove("is-invalid");

        const correo = recoverEmail.value.trim();

        if (!correo) {
            recoverEmail.classList.add("is-invalid");
            showRecoverError("El correo es obligatorio");
            recoverEmail.focus();
            return;
        }

        // Respuesta genericamente exitosa para no revelar si el correo existe
        // (anti-enumeracion, espejo del backend).
        setRecoverLoading(true);
        try {
            const response = await fetch(`${API_BASE}/auth/recuperar`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ correo }),
            });
            const body = await response.json();
            if (!response.ok || body.success === false) {
                throw new Error(body.mensaje || "No se pudo procesar la solicitud");
            }
            recoverForm.hidden = true;
            recoverSuccess.hidden = false;
        } catch (err) {
            const msg = err.message.includes("Failed to fetch")
                ? "El servidor no esta disponible. Intente de nuevo."
                : err.message;
            showRecoverError(msg);
        } finally {
            setRecoverLoading(false);
        }
    });

    recoverEmail.addEventListener("input", () => {
        recoverEmail.classList.remove("is-invalid");
        if (recoverError.classList.contains("visible")) {
            hideRecoverError();
        }
    });
})();