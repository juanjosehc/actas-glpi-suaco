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

let _autocompleteResultadosUsuario = [];

let _autocompleteIndiceActivo = -1;

let _solicitudAutocompleteUsuario = null;

let _ultimoErrorAutocomplete = 0;

const AUTOCOMPLETE_MAX_RESULTADOS = 10;

const AUTOCOMPLETE_INTERVALO_ERROR = 2000;

/**
 * Retorna el correo del usuario seleccionado via autocompletado.
 *
 * El correo se lee SIEMPRE del dataset del campo que tiene el
 * autocompletado: cada campo guarda su propio correo (QA-20). No hay
 * estado global compartido entre campos; sin input no hay correo.
 *
 * @param {HTMLInputElement} [input] - Campo con autocompletado.
 * @returns {string} Correo capturado o cadena vacia.
 */
function getCorreoUsuarioSeleccionado(input) {
    if (input && input.dataset) {
        return input.dataset.correoUsuario || "";
    }
    return "";
}

/**
 * Valida el FORMATO de un campo (QA-13/QA-31/QA-35).
 *
 * Solo falla si el campo trae valor que no cumple el regex: los campos
 * vacios obligatorios se validan aparte con validarCampo. Para el correo
 * capturado por autocompletado se usa valorAlternativo (el valor del campo
 * es el nombre, no el correo). Marca is-invalid, muestra el helper-text,
 * hace scroll+focus y muestra mensaje.
 *
 * @param {string} id - ID del elemento.
 * @param {RegExp} regex - Expresion que debe cumplir el valor (si viene con valor).
 * @param {string} mensaje - Mensaje mostrado al usuario.
 * @param {string} [valorAlternativo] - Valor a validar en vez de campo.value.
 * @returns {boolean} true si esta vacio o cumple el formato.
 */
function validarFormatoCampo(id, regex, mensaje, valorAlternativo) {
    const campo = document.getElementById(id);
    if (!campo) return false;
    const valor =
        (valorAlternativo !== undefined
            ? valorAlternativo
            : campo.value || "") || "";
    const texto = valor.trim();
    if (texto === "" || regex.test(texto)) {
        campo.classList.remove("is-invalid");
        const helper = campo.parentElement.querySelector(".helper-text");
        if (helper) {
            helper.style.display = "none";
        }
        return true;
    }
    campo.classList.add("is-invalid");
    const helper = campo.parentElement.querySelector(".helper-text");
    if (helper) {
        helper.textContent = mensaje;
        helper.style.display = "block";
    }
    campo.scrollIntoView({ behavior: "smooth", block: "center" });
    setTimeout(() => campo.focus(), 300);
    mostrarMensaje(mensaje, "error");
    return false;
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

    input.addEventListener("focus", function () {

        const lista =
            document.getElementById("usuario-autocomplete");

        if (lista) {
            return;
        }

        const q =
            input.value.trim();

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

        const lista =
            document.getElementById("usuario-autocomplete");

        if (!lista) {
            return;
        }

        switch (e.key) {

            case "ArrowDown":

                e.preventDefault();

                if (_autocompleteResultadosUsuario.length > 0) {

                    _autocompleteIndiceActivo =
                        (_autocompleteIndiceActivo + 1) %
                        _autocompleteResultadosUsuario.length;

                }

                resaltarSugerenciaActiva(input);

                break;

            case "ArrowUp":

                e.preventDefault();

                if (_autocompleteResultadosUsuario.length > 0) {

                    _autocompleteIndiceActivo =
                        (_autocompleteIndiceActivo - 1 +
                            _autocompleteResultadosUsuario.length) %
                        _autocompleteResultadosUsuario.length;

                }

                resaltarSugerenciaActiva(input);

                break;

            case "Enter":

                e.preventDefault();

                const usuario =
                    _autocompleteResultadosUsuario[
                        _autocompleteIndiceActivo
                    ];

                if (usuario) {
                    seleccionarSugerenciaUsuario(
                        input,
                        usuario,
                        alSeleccionar
                    );
                }

                break;

            case "Escape":
                cerrarSugerenciasUsuario();
                break;

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

    if (_solicitudAutocompleteUsuario) {
        _solicitudAutocompleteUsuario.abort();
    }

    _solicitudAutocompleteUsuario =
        new AbortController();

    try {

        // QA-07/QA-18: /usuario?q= es endpoint protegido; debe ir con JWT.
        const token = LoginService.obtenerToken();
        if (!token) {
            cerrarSugerenciasUsuario();
            return;
        }

        const response = await fetch(
            API_BASE + "/usuario?q=" +
            encodeURIComponent(q),
            {
                signal: _solicitudAutocompleteUsuario.signal,
                headers: { "Authorization": `Bearer ${token}` }
            }
        );

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }

        const data = await response.json();

        if (!Array.isArray(data)) {
            throw new Error("Formato de respuesta inválido");
        }

        const items =
            data.slice(0, AUTOCOMPLETE_MAX_RESULTADOS);

        mostrarSugerenciasUsuario(
            input,
            items,
            alSeleccionar
        );

    } catch (error) {

        if (error.name === "AbortError") {
            return;
        }

        cerrarSugerenciasUsuario();

        const ahora = Date.now();

        if (
            ahora - _ultimoErrorAutocomplete >=
            AUTOCOMPLETE_INTERVALO_ERROR
        ) {

            _ultimoErrorAutocomplete = ahora;

            mostrarMensaje(
                "Error al consultar los usuarios",
                "error"
            );

        }

    } finally {

        _solicitudAutocompleteUsuario = null;

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

    const lista =
        document.createElement("ul");

    lista.id = "usuario-autocomplete";

    lista.style.cssText =
        "position:absolute;z-index:9999;list-style:none;margin:0;padding:4px 0;" +
        "background:#FFFFFF;border:1px solid #E2E8F0;border-radius:8px;" +
        "box-shadow:0 8px 24px rgba(0,0,0,0.12);max-height:260px;overflow-y:auto;";

    if (!usuarios.length) {

        const vacio =
            document.createElement("li");

        vacio.style.cssText =
            "padding:8px 12px;color:#64748B;font-size:13px;font-style:italic;";

        vacio.textContent = "Sin coincidencias";

        lista.appendChild(vacio);

    } else {

        _autocompleteResultadosUsuario = usuarios;

        _autocompleteIndiceActivo = -1;

        usuarios.forEach(function (usuario, indice) {

            const nombre =
                usuario.nombre || usuario.login || "";

            const correo =
                usuario.correo || "";

            const item =
                document.createElement("li");

            item.dataset.indice = String(indice);

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

                _autocompleteIndiceActivo = indice;

                seleccionarSugerenciaUsuario(
                    input,
                    usuario,
                    alSeleccionar
                );

            });

            lista.appendChild(item);

        });

    }

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
 * Escribe el nombre en el input y guarda el correo.
 *
 * @param {HTMLInputElement} input - Campo de texto.
 * @param {Object} usuario - Usuario GLPI { nombre, correo, login }.
 * @param {Function} [alSeleccionar] - Callback (nombre, correo).
 */
function seleccionarSugerenciaUsuario(input, usuario, alSeleccionar) {

    const nombre =
        usuario.nombre || usuario.login || "";

    const correo =
        usuario.correo || "";

    input.value = nombre;

    if (input.dataset) {
        input.dataset.correoUsuario = correo;
    }

    if (typeof alSeleccionar === "function") {
        alSeleccionar(nombre, correo);
    }

    cerrarSugerenciasUsuario();

}

/**
 * Resalta visualmente el ítem activo según el índice.
 *
 * @param {HTMLInputElement} input - Campo de texto (para scroll).
 */
function resaltarSugerenciaActiva(input) {

    const lista =
        document.getElementById("usuario-autocomplete");

    if (!lista) {
        return;
    }

    lista
        .querySelectorAll("li[data-indice]")
        .forEach(function (item, indice) {

            const activo =
                indice === _autocompleteIndiceActivo;

            item.style.backgroundColor =
                activo ? "#E2E8F0" : "";

            if (activo) {
                item.scrollIntoView({ block: "nearest" });
            }

        });

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

    _autocompleteResultadosUsuario = [];

    _autocompleteIndiceActivo = -1;

}
