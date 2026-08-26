#!/usr/bin/env python3
"""Verificacion runtime de la firma permanente del tecnico."""
import base64
import json
import os
import struct
import sys
import tempfile
import urllib.request

BASE = "http://127.0.0.1:8001"


def req(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    r = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}")


def make_png_png(width=200, height=60):
    """PNG minimo valido: fondo transparente + linea negra horizontal."""
    rows = b""
    for y in range(height):
        row = b"\x00"
        for x in range(width):
            if 15 <= y <= 45:
                row += bytes([0, 0, 0, 255])
            else:
                row += bytes([0, 0, 0, 0])
        rows += row
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        return c + struct.pack(">I", zlib_crc(typ + data))
    def zlib_crc(b):
        import zlib
        return zlib.crc32(b) & 0xFFFFFFFF
    import zlib
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(rows)
    return sig + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")


def main():
    uname = "firmatest"
    body = {
        "cedula": "9999999999", "nombres": "Firma", "apellidos": "Test",
        "username": uname, "correo": "firmatest@tests.local",
        "password": "prueba123", "cargo": "Ingeniero", "empresa": "Coltefinanciera",
        "lugarTrabajo": "Bogota", "rol": "TECNICO",
    }
    status, b = req("POST", "/auth/register", body)
    print("register:", status, b.get("success"), b.get("mensaje", ""))
    if not b.get("success"):
        print("  (usuario probablemente ya existe de un test previo)")

    status, b = req("POST", "/auth/login", {"username": uname, "password": "prueba123"})
    print("login:", status, b.get("success"))
    if not b.get("success"):
        sys.exit("Login fallo: " + str(b))
    token = b["data"]["token"]

    status, b = req("GET", "/usuarios/me/firma", token=token)
    print("GET me/firma (antes):", status, b.get("success"), b.get("data"))

    png = make_png_png()
    print("PNG bytes:", len(png), "magic ok:", png[:4] == b"\x89PNG")
    status, b = req("PUT", "/usuarios/me/firma",
                    {"firmaBase64": base64.b64encode(png).decode()}, token=token)
    print("PUT me/firma:", status, b.get("success"), b.get("mensaje", ""))
    if b.get("success"):
        ruta = b["data"]["ruta"]
        print("  ruta virtual:", ruta)

    status, b = req("GET", "/usuarios/me/firma", token=token)
    print("GET me/firma (despues):", status, b.get("success"), b.get("data"))

    # El archivo debe existir bajo el storage root del backend.
    if b.get("success") and b["data"].get("ruta"):
        candidates = [
            os.path.join("STORAGE", b["data"]["ruta"]),
            os.path.join("backend", "storage", b["data"]["ruta"].split("/", 1)[-1]),
            os.path.join("storage", b["data"]["ruta"].split("/", 1)[-1]),
        ]
        found = [c for c in candidates if os.path.exists(c)]
        for c in candidates:
            print("  buscar archivo:", os.path.abspath(c), "->", os.path.exists(c))

    status, b = req("DELETE", "/usuarios/me/firma", token=token)
    print("DELETE me/firma:", status, b.get("success"), b.get("mensaje", ""))

    status, b = req("GET", "/usuarios/me/firma", token=token)
    print("GET me/firma (tras borrar):", status, b.get("success"), b.get("data"))


if __name__ == "__main__":
    main()