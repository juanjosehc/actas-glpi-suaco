#!/usr/bin/env python3
"""Verifica el endpoint nuevo de vista previa de la firma del tecnico."""
import base64
import json
import urllib.request

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


def main():
    s, b, _ = req("POST", "/auth/login", {"username": "previewtest", "password": "prueba123"})
    print("login:", s, b.get("success"))
    tok = b["data"]["token"]

    s, b, _ = req("GET", "/usuarios/me/firma", token=tok)
    print("GET /usuarios/me/firma:", s, json.dumps(b.get("data")))

    s, body, hdrs = req("GET", "/usuarios/me/firma/archivo", token=tok, raw=True)
    print("GET /usuarios/me/firma/archivo (Bearer):", s,
          "| content-type:", hdrs.get("Content-Type"),
          "| content-disposition:", hdrs.get("Content-Disposition"),
          "| bytes:", len(body))
    print("  PNG magic ok:", body[:4] == b"\x89PNG")

    s2, _, hdrs2 = req("GET", "/usuarios/me/firma/archivo", raw=True)  # sin token
    print("GET /usuarios/me/firma/archivo (SIN token):", s2)

    # estado de la firma por GET /me/firma tras el flujo: preview debe existir en disco
    s3, b3, _ = req("GET", "/usuarios/me/firma", token=tok)
    ruta = b3.get("data", {}).get("ruta")
    print("ruta virtual en BD:", ruta)


if __name__ == "__main__":
    main()