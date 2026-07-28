(() => {
    const form = document.getElementById("loginForm");
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const errorBox = document.getElementById("loginError");
    const errorText = document.getElementById("loginErrorText");
    const loginBtn = document.getElementById("loginBtn");

    function showError(message) {
        errorText.textContent = message;
        errorBox.classList.add("visible");
    }

    function hideError() {
        errorBox.classList.remove("visible");
        errorText.textContent = "";
    }

    function setLoading(loading) {
        loginBtn.disabled = loading;
        loginBtn.classList.toggle("loading", loading);
        usernameInput.disabled = loading;
        passwordInput.disabled = loading;
    }

    function clearInvalidStyles() {
        usernameInput.classList.remove("is-invalid");
        passwordInput.classList.remove("is-invalid");
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

            window.location.href = ROUTES.HOME;
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
})();
