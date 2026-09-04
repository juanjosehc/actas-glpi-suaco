(() => {
    "use strict";

    const token = new URLSearchParams(window.location.search).get("token");

    const form = document.getElementById("recuperarForm");
    const nuevaInput = document.getElementById("nuevaPassword");
    const confirmarInput = document.getElementById("confirmarPassword");
    const formError = document.getElementById("formError");
    const formErrorText = document.getElementById("formErrorText");
    const pageError = document.getElementById("pageError");
    const pageErrorText = document.getElementById("pageErrorText");
    const btn = document.getElementById("recuperarBtn");
    const successPanel = document.getElementById("successPanel");

    function showFormError(msg) {
        formErrorText.textContent = msg;
        formError.classList.add("visible");
    }

    function hideFormError() {
        formError.classList.remove("visible");
        formErrorText.textContent = "";
    }

    if (!token) {
        form.hidden = true;
        pageErrorText.textContent =
            "Enlace de recuperacion invalido: falta el token. Solicite uno nuevo desde el inicio de sesion.";
        pageError.classList.add("visible");
    }

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideFormError();

        const nuevaPassword = nuevaInput.value;
        const confirmar = confirmarInput.value;

        if (nuevaPassword.length < 8) {
            showFormError("La contrasena debe tener minimo 8 caracteres.");
            nuevaInput.focus();
            return;
        }
        // SEC-016: misma politica que registro/creacion. Sin esta validacion el
        // backend responde 400 con "Errores de validacion" (mensaje generico).
        const sec016 = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,128}$/;
        if (!sec016.test(nuevaPassword)) {
            showFormError(
                "La contrasena debe tener entre 8 y 128 caracteres, con mayuscula, minuscula, numero y caracter especial.");
            nuevaInput.focus();
            return;
        }
        if (nuevaPassword !== confirmar) {
            showFormError("Las contrasenas no coinciden.");
            confirmarInput.focus();
            return;
        }

        btn.disabled = true;
        btn.classList.add("loading");

        try {
            const resp = await fetch(`${API_BASE}/auth/recuperar/confirmar`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ token, nuevaPassword }),
            });
            const body = await resp.json();

            if (!resp.ok || body.success === false) {
                // Token inexistente, expirado o ya usado: el backend lo rechaza
                // y audita como RECUPERACION_TOKEN_INVALIDO.
                throw new Error(body.mensaje || "El enlace no es valido o ya fue utilizado.");
            }

            form.hidden = true;
            successPanel.hidden = false;
        } catch (err) {
            const msg = err.message.includes("Failed to fetch")
                ? "El servidor no esta disponible. Intente de nuevo."
                : (err.message.includes("no es valido") || err.message.includes("ya fue")
                    ? err.message
                    : "No se pudo restablecer la contrasena. Solicite un nuevo enlace.");
            showFormError(msg);
        } finally {
            btn.disabled = false;
            btn.classList.remove("loading");
        }
    });

    [nuevaInput, confirmarInput].forEach((input) => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");
            if (formError.classList.contains("visible")) hideFormError();
        });
    });
})();