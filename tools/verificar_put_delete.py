#!/usr/bin/env python3
"""Verifica registrar/reemplazar/eliminar firma del tecnico contra el jar nuevo."""
import base64
import json
import struct
import urllib.request
import zlib

BASE = "http://127.0.0.1:8001"


def req(method, path, body=None, token=None, raw=False):
    data = json.dumps(body).encode() if body is not None else None
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    target = path if path.startswith("http") else BASE + path
    r = urllib.request.Request(target, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r) as x:
            if raw:
                return x.status, x.read(), dict(x.headers)
            return x.status, json.loads(x.read().decode()), dict(x.headers)
    except urllib.error.HTTPError as e:
        if raw:
            return e.code, e.read(), dict(e.headers)
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
    s, b, _ = req("POST", "/auth/login", {"username": "previewtest", "password": "prueba123"})
    print("login:", s)
    tok = b["data"]["token"]

    s, b, _ = req("PUT", "/usuarios/me/firma",
                  {"firmaBase64": base64.b64encode(make_png()).decode()}, tok)
    print("1. PUT registrar (sobre firma existente = reemplazo):", s, b.get("mensaje"))

    s2, b2, _ = req("GET", "/usuarios/me/firma", token=tok)
    print("2. estado tras PUT:", s2, json.dumps(b2.get("data")))

    s3, body3, hdrs3 = req("GET", "/usuarios/me/firma/archivo", token=tok, raw=True)
    print("3. vista previa tras reemplazo:", s3, "|", hdrs3.get("Content-Type"),
          "| bytes:", len(body3), "| png ok:", body3[:4] == b"\x89PNG")

    s4, b4, _ = req("DELETE", "/usuarios/me/firma", token=tok)
    print("4. DELETE eliminar:", s4, b4.get("mensaje"))

    s5, body5, _ = req("GET", "/usuarios/me/firma/archivo", token=tok, raw=True)
    print("5. vista previa tras DELETE (esperado 404):", s5, "| bytes:", len(body5))

    s6, b6, _ = req("GET", "/usuarios/me/firma", token=tok)
    print("6. estado tras DELETE:", s6, json.dumps(b6.get("data")))

    # Restaura firma para verificar la vista previa en navegador
    s7, b7, _ = req("PUT", "/usuarios/me/firma",
                    {"firmaBase64": base64.b64encode(make_png()).decode()}, tok)
    print("7. RESTAURAR firma:", s7, b7.get("mensaje"))

    s8, body8, hdrs8 = req("GET", "/usuarios/me/firma/archivo", token=tok, raw=True)
    print("8. vista previa final:", s8, "|", hdrs8.get("Content-Type"),
          "| bytes:", len(body8), "| png ok:", body8[:4] == b"\x89PNG")


if __name__ == "__main__":
    main()