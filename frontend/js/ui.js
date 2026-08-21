/*
====================================================
UI UTILITIES - FRONTEND
====================================================

Propósito:

Funciones compartidas de presentación utilizadas
por ambas páginas (acta de entrega y acta de
devolución).

Dependencias:

- Ninguna. Este archivo se carga primero.

====================================================
*/

/**
 * Muestra un mensaje de notificación al usuario.
 *
 * Crea un elemento <div> dentro del contenedor #mensaje-app
 * con el texto y tipo indicados. Los mensajes anteriores
 * se eliminan automáticamente (solo uno visible a la vez).
 *
 * Tipos soportados:
 * - "success": Notificación de éxito (verde).
 * - "error": Notificación de error (rojo).
 * - "info": Informativo por defecto (azul).
 *
 * @param {string} mensaje - Texto a mostrar.
 * @param {string} [tipo="info"] - Tipo de notificación.
 */
function mostrarMensaje(
    mensaje,
    tipo = "info"
) {

    const contenedor =
        document.getElementById(
            "mensaje-app"
        );

    contenedor.innerHTML = "";

    const div =
        document.createElement("div");

    div.textContent = mensaje;

    div.dataset.tipo = tipo;

    contenedor.appendChild(div);

}

/*
----------------------------------------------------
AUTOCOMPLETADO DE USUARIO (GLPI)
----------------------------------------------------

Reutiliza el campo existente del formulario
("entregado_a" en entrega, "recibido_por" en
devolucion) para capturar internamente el correo
del usuario desde GLPI, sin agregar campos visibles.
*/

let _autocompleteUsuarioCorreo = "";

/**
 * Retorna el correo del usuario seleccionado via autocompletado.
 *
 * Si se pasa el input del campo, lee el correo capturado en ESE campo
 * (cada campo con autocompletado guarda su propio correo). Sin argumento
 * conserva el comportamiento anterior (ultimo seleccionado).
 *
 * @param {HTMLInputElement} [input] - Campo con autocompletado.
 * @returns {string} Correo capturado o cadena vacia.
 */
function getCorreoUsuarioSeleccionado(input) {
    if (input && input.dataset) {
        return input.dataset.correoUsuario || "";
    }
    return _autocompleteUsuarioCorreo;
}

/**
 * Vincula el autocompletado de usuarios GLPI a un input.
 *
 * Cuando el usuario escribe 3+ caracteres se consulta
 * GET /usuario?q= y se muestra una lista de sugerencias
 * (nombre + correo). Al seleccionar una opcion se guarda
 * el correo internamente y se actualiza el valor del campo.
 *
 * @param {HTMLInputElement} input - Campo de texto existente.
 * @param {Function} [alSeleccionar] - Callback (nombre, correo).
 */
function iniciarAutocompleteUsuario(input, alSeleccionar) {

    let timer = null;

    input.addEventListener("input", function () {

        if (input.dataset) {
            input.dataset.correoUsuario = "";
        }
        _autocompleteUsuarioCorreo = "";

        const q = input.value.trim();

        cerrarSugerenciasUsuario();

        if (q.length < 3) {
            return;
        }

        clearTimeout(timer);

        timer = setTimeout(function () {
            buscarUsuariosGlpi(input, q, alSeleccionar);
        }, 350);

    });

    input.addEventListener("blur", function () {
        setTimeout(cerrarSugerenciasUsuario, 200);
    });

    input.addEventListener("keydown", function (e) {
        if (e.key === "Escape") {
            cerrarSugerenciasUsuario();
        }
    });

}

/**
 * Consulta usuarios en GLPI y muestra las sugerencias.
 *
 * @param {HTMLInputElement} input - Campo de texto.
 * @param {string} q - Texto a buscar.
 * @param {Function} [alSeleccionar] - Callback (nombre, correo).
 */
async function buscarUsuariosGlpi(input, q, alSeleccionar) {

    try {

        const response = await fetch(
            "http://127.0.0.1:8001/usuario?q=" +
            encodeURIComponent(q)
        );

        if (!response.ok) {
            return;
        }

        const data = await response.json();

        mostrarSugerenciasUsuario(
            input,
            data || [],
            alSeleccionar
        );

    } catch (_) {
        cerrarSugerenciasUsuario();
    }

}

/**
 * Muestra la lista de sugerencias de usuarios bajo el input.
 *
 * @param {HTMLInputElement} input - Campo de texto.
 * @param {Array} usuarios - Usuarios GLPI { nombre, correo, login }.
 * @param {Function} [alSeleccionar] - Callback (nombre, correo).
 */
function mostrarSugerenciasUsuario(input, usuarios, alSeleccionar) {

    cerrarSugerenciasUsuario();

    if (!usuarios.length) {
        return;
    }

    const lista =
        document.createElement("ul");

    lista.id = "usuario-autocomplete";

    lista.style.cssText =
        "position:absolute;z-index:9999;list-style:none;margin:0;padding:4px 0;" +
        "background:#FFFFFF;border:1px solid #E2E8F0;border-radius:8px;" +
        "box-shadow:0 8px 24px rgba(0,0,0,0.12);max-height:260px;overflow-y:auto;";

    usuarios.forEach(function (usuario) {

        const nombre =
            usuario.nombre || usuario.login || "";

        const correo =
            usuario.correo || "";

        const item =
            document.createElement("li");

        item.style.cssText =
            "padding:8px 12px;cursor:pointer;display:flex;flex-direction:column;";

        item.addEventListener("mouseenter", function () {
            item.style.backgroundColor = "#F1F5F9";
        });

        item.addEventListener("mouseleave", function () {
            item.style.backgroundColor = "";
        });

        const nombreEl =
            document.createElement("span");

        nombreEl.textContent = nombre;

        nombreEl.style.cssText =
            "font-weight:600;color:#0F172A;font-size:13px;";

        const correoEl =
            document.createElement("span");

        correoEl.textContent = correo;

        correoEl.style.cssText =
            "color:#64748B;font-size:11px;";

        item.appendChild(nombreEl);
        item.appendChild(correoEl);

        item.addEventListener("mousedown", function (e) {

            e.preventDefault();

            input.value = nombre;

            if (input.dataset) {
                input.dataset.correoUsuario = correo;
            }
            _autocompleteUsuarioCorreo = correo;

            if (typeof alSeleccionar === "function") {
                alSeleccionar(nombre, correo);
            }

            cerrarSugerenciasUsuario();

        });

        lista.appendChild(item);

    });

    const rect =
        input.getBoundingClientRect();

    lista.style.left =
        (rect.left + window.pageXOffset) + "px";

    lista.style.top =
        (rect.bottom + window.pageYOffset) + "px";

    lista.style.width =
        Math.max(rect.width, 260) + "px";

    document.body.appendChild(lista);

}

/**
 * Elimina la lista de sugerencias del DOM.
 */
function cerrarSugerenciasUsuario() {

    const lista =
        document.getElementById("usuario-autocomplete");

    if (lista) {
        lista.remove();
    }

}
