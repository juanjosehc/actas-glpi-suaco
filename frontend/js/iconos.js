// iconos.js — libreria de iconos SVG inline para acciones de listados.
// Sin dependencias ni webfonts: mismos trazos (stroke=currentColor, viewBox 24)
// que los SVG ya usados en topbar/botones del resto de la UI.
// Uso: `botonIcono("ojo", "Ver detalle", () => ...)` — devuelve un <button>
// cuadrado con el icono, aria-label + data-tip con la etiqueta accesible.
// El tooltip lo renderiza este mismo modulo como div fijo al body (ver abajo),
// reposicionado contra el viewport: nunca se recorta en los bordes.
// Para enlaces: `ICONOS.documento` da el SVG crudo para el href propio.

const ICONOS = {
    ojo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/></svg>',
    lapiz: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.83 2.83 0 0 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/></svg>',
    llave: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg>',
    candado: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>',
    candadoAbierto: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>',
    check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    rechazar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
    enviar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>',
    enlace: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>',
    evidencias: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>',
    documento: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>'
};

/**
 * Construye un boton de icono accesible para columnas de accion de listados.
 * @param {string} nombre  Clave en ICONOS.
 * @param {string} etiqueta Texto del tooltip/aria-label (ej. "Ver detalle").
 * @param {Function} [onClick] Handler; recibe el evento.
 * @param {string} [claseMod] Modificador visual, ej. "--danger" para rechazo.
 */
function botonIcono(nombre, etiqueta, onClick, claseMod) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "btn-icon" + (claseMod ? " btn-icon" + claseMod : "");
    btn.setAttribute("aria-label", etiqueta);
    btn.setAttribute("data-tip", etiqueta);
    btn.innerHTML = ICONOS[nombre] || "";
    if (onClick) {
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            onClick(e);
        });
    }
    return btn;
}

// =========================
//  TOOLTIP DE LISTADO (componente base)
//  Un div fijo al body, fuera de contenedores con overflow (las tablas usan
//  overflow-x:auto y recortaban el tooltip CSS ::after en el borde derecho).
//  Se reposiciona contra el viewport: clamp horizontal izquierda/derecha y si
//  no cabe arriba se vuelca abajo. Soporta dos lineas con "\n" en data-tip
//  (titulo en negrita + descripcion) para mensajes tipo "Titulo.\nDesc.".
// =========================
(function () {
    const DELAY = 150;
    const GAP = 6;
    const PAD = 8;

    const tip = document.createElement("div");
    tip.id = "sauco-tooltip";
    document.body.appendChild(tip);

    let timer = null;
    let actual = null;

    function escapeHtml(s) {
        return String(s).replace(/[&<>"']/g, (c) => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
        }[c]));
    }

    function mostrar(trg) {
        const raw = trg.getAttribute("data-tip") || "";
        let html;
        if (raw.indexOf("\n") !== -1) {
            const partes = raw.split("\n");
            const titulo = partes.shift();
            html = "<strong>" + escapeHtml(titulo) + "</strong><span>" + escapeHtml(partes.join(" ")) + "</span>";
        } else {
            html = "<span>" + escapeHtml(raw) + "</span>";
        }
        tip.innerHTML = html;
        tip.classList.add("visible");
        tip.removeAttribute("style");
        const tw = tip.offsetWidth;
        const th = tip.offsetHeight;

        const r = trg.getBoundingClientRect();
        // X: centrado sobre el disparador, pero siempre dentro del viewport.
        let x = r.left + r.width / 2 - tw / 2;
        x = Math.max(PAD, Math.min(x, window.innerWidth - tw - PAD));

        // Y: arriba del disparador; si no alcanza, se vuelca abajo.
        const arriba = r.top - th - GAP;
        tip.style.left = x + "px";
        tip.style.top = (arriba >= PAD ? arriba : r.bottom + GAP) + "px";
    }

    function ocultar() {
        clearTimeout(timer);
        timer = null;
        actual = null;
        tip.classList.remove("visible");
    }

    function enlazar(trg) {
        clearTimeout(timer);
        actual = trg;
        timer = setTimeout(() => { if (actual === trg) mostrar(trg); }, DELAY);
    }

    document.addEventListener("mouseover", (e) => {
        const t = e.target.closest ? e.target.closest("[data-tip]") : null;
        if (t) enlazar(t);
    });
    document.addEventListener("mouseout", (e) => {
        if (e.target.closest && e.target.closest("[data-tip]")) ocultar();
    });
    document.addEventListener("focusin", (e) => {
        const t = e.target.closest ? e.target.closest("[data-tip]") : null;
        if (t) enlazar(t);
    });
    document.addEventListener("focusout", ocultar);
    window.addEventListener("scroll", ocultar, true);
    window.addEventListener("resize", ocultar);
})();