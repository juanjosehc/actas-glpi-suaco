#!/usr/bin/env python3
"""Diagnostica la vista previa de la firma del tecnico: URL, status HTTP, ruta fisica y BD."""
import base64
import json
import os
import struct
import urllib.request
import zlib

BASE = "http://127.0.0.1:8001"


def req(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    target = path if path.startswith("http") else BASE + path
    r = urllib.request.Request(target, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r) as x:
            return x.status, json.loads(x.read().decode()), dict(x.headers)
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}"), dict(e.headers)


def make_png(w=180, h=50):
    rows = b""
    for y in range(h):
        row = b"\x00"
        for x in range(w):
            row += bytes([0, 0, 0, 255]) if 15 <= y <= 35 else bytes([0, 0, 0, 0])
        rows += row

    def ch(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + ch(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + ch(b"IDAT", zlib.compress(rows)) + ch(b"IEND", b""))


def main():
    s, b, _ = req("POST", "/auth/register", {
        "cedula": "8888888888", "nombres": "Preview", "apellidos": "Test",
        "username": "previewtest", "correo": "previewtest@tests.local",
        "password": "prueba123", "rol": "TECNICO",
    })
    print("register:", s, b.get("success"))
    s, b, _ = req("POST", "/auth/login", {"username": "previewtest", "password": "prueba123"})
    print("login:", s, b.get("success"))
    tok = b["data"]["token"]

    s, b, _ = req("PUT", "/usuarios/me/firma",
                  {"firmaBase64": base64.b64encode(make_png()).decode()}, tok)
    print("PUT firma:", s, b.get("success"))
    ruta = b.get("data", {}).get("ruta")
    print("1. ruta virtual (BD):", ruta)

    url = BASE + "/" + ruta
    print("2. URL que intenta cargar el navegador:", url)

    s, body, hdrs = req("GET", url)
    print("3. GET SIN Authorization:", s, "| content-type:", hdrs.get("Content-Type"),
          "| body len:", len(body) if isinstance(body, str) else "n/a")

    s2, body2, hdrs2 = req("GET", url, token=tok)
    print("4. GET CON Bearer:", s2, "| content-type:", hdrs2.get("Content-Type"),
          "| body len:", len(body2.encode()) if isinstance(body2, str) else "n/a")

    # ruta fisica
    for base in ["backend/storage", "storage", "."]:
        p = os.path.join(base, *ruta.split("/"))
        if os.path.exists(p):
            print("5. ruta fisica:", os.path.abspath(p), "| bytes:", os.path.getsize(p))
    else:
        print("5. archivo NO encontrado en backend/storage, storage ni .")


if __name__ == "__main__":
    main()