const API_BASE = "http://localhost:8001";

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

    guardarSesion({ token, username, role }) {
        localStorage.setItem("token", token);
        localStorage.setItem("username", username);
        localStorage.setItem("role", role);
    },

    cerrarSesion() {
        localStorage.removeItem("token");
        localStorage.removeItem("username");
        localStorage.removeItem("role");
    },

    obtenerToken() {
        return localStorage.getItem("token");
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
