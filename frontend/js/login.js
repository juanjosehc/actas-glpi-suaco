
const ROUTES = {
    LOGIN: "login.html",
    HOME: "home.html",
    USUARIOS: "usuarios.html",
    ACTAS: "actas.html"
};

const LoginService = {
    async login(username, password) {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password }),
        });

        const data = await response.json();

        if (!response.ok || data.success === false) {
            throw new Error(data.mensaje || "Credenciales invalidas");
        }

        return data.data;
    },

    // SEC-009: el JWT vive en sessionStorage, no en localStorage. Asi muere al
    // cerrar pestana/navegador (minimiza el tiempo en que un XSS puede robarlo)
    // y, si se roba en una pestana, no viaja a otras. username/role quedan en
    // localStorage porque admin-layout.js los lee ahi (render del sidebar).
    // La solucion ideal es cookie HttpOnly + SameSite (frontend nunca ve el
    // token), pero exigiria cambios en SecurityConfig, fuera de alcance del
    // sprint. Documentada en el informe de SEC-009.
    guardarSesion({ token, username, role }) {
        sessionStorage.setItem("token", token);
        localStorage.setItem("username", username);
        localStorage.setItem("role", role);
    },

    async cerrarSesion() {
        const token = this.obtenerToken();
        if (token) {
            try {
                // SEC-118: un solo endpoint de logout real. /sesiones/revocar
                // revoca el jti del JWT en la denylist y registra LOGOUT en
                // auditoria (best-effort: si el token ya vencio/revoco, la UI
                // sigue limpiando la sesion local).
                await fetch(`${API_BASE}/sesiones/revocar`, {
                    method: "POST",
                    headers: { "Authorization": `Bearer ${token}` },
                });
            } catch (_) {}
        }
        sessionStorage.removeItem("token");
        localStorage.removeItem("token");
        localStorage.removeItem("username");
        localStorage.removeItem("role");
    },

    obtenerToken() {
        return sessionStorage.getItem("token");
    },

    obtenerRol() {
        return localStorage.getItem("role");
    },

    obtenerUsuario() {
        return localStorage.getItem("username");
    },

    estaAutenticado() {
        return !!this.obtenerToken();
    },
};
