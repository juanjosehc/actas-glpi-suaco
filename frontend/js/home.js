(() => {
    const API_BASE = "http://localhost:8001";
    const welcomeTitle = document.getElementById("welcomeTitle");
    const welcomeText = document.getElementById("welcomeText");

    async function loadUser() {
        const token = LoginService.obtenerToken();
        if (!token) { window.location.href = ROUTES.LOGIN; return; }
        try {
            const resp = await fetch(`${API_BASE}/usuarios/me`, { headers: { Authorization: `Bearer ${token}` } });
            const body = await resp.json();
            if (!resp.ok || body.success === false) throw new Error(body.mensaje || "Sesion expirada");
            return body.data;
        } catch (err) {
            if (err.message.includes("Failed to fetch")) throw new Error("El servidor no esta disponible.");
            LoginService.cerrarSesion();
            window.location.href = ROUTES.LOGIN;
        }
    }

    (async function init() {
        const user = await loadUser();
        if (!user) return;
        if (welcomeTitle) welcomeTitle.textContent = `Bienvenido, ${user.nombres} ${user.apellidos}`;
        if (welcomeText) welcomeText.textContent = `Rol: ${user.rol}`;
    })();
})();
