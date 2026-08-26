#!/usr/bin/env python3
"""Verifica mensajes de autenticacion en español (cuenta bloqueada) y endpoints de evidencias."""
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


def rand_username(prefix):
    import os
    return prefix + str(os.getpid())


def main():
    unames = []
    # --- USUARIOS DE PRUEBA ---
    s, b, _ = req("POST", "/auth/register", {
        "cedula": "9000000001", "nombres": "Admin", "apellidos": "Prueba",
        "username": "admtest2", "correo": "admtest2@tests.local",
        "password": "prueba123", "rol": "ADMINISTRADOR",
    })
    print("register admtest2:", s, b.get("success"), b.get("mensaje"))
    unames.append("admtest2")
    s, b, _ = req("POST", "/auth/register", {
        "cedula": "9000000002", "nombres": "Block", "apellidos": "Test",
        "username": "blocktest2", "correo": "blocktest2@tests.local",
        "password": "prueba123", "rol": "TECNICO",
    })
    print("register blocktest2:", s, b.get("success"), b.get("mensaje"))
    unames.append("blocktest2")

    # --- LOGIN ADMIN ---
    s, b, _ = req("POST", "/auth/login", {"username": "admtest2", "password": "prueba123"})
    print("login admtest2:", s)
    admin_tok = b["data"]["token"]

    # --- BLOQUEAR blocktest2 ---
    s, b, _ = req("GET", "/usuarios?page=0&size=200", token=admin_tok)
    usuarios = b.get("data", {}).get("content", [])
    if usuarios:
        print("keys usuario:", json.dumps(list(usuarios[0].keys())))
    bid = next((u.get("idUsuario") if "idUsuario" in u else u.get("id")
                for u in usuarios if u.get("username") == "blocktest2"), None)
    print("blocktest2 id:", bid)
    s, b, _ = req("PATCH", f"/usuarios/{bid}/bloquear", token=admin_tok)
    print("bloquear:", s, b.get("mensaje"))

    # --- LOGIN BLOQUEADO (esperado: 401 + mensaje en español) ---
    s, b, _ = req("POST", "/auth/login", {"username": "blocktest2", "password": "prueba123"})
    print("login blocktest2 TRAS bloqueo:", s, "| mensaje:", b.get("mensaje"))

    # --- EVIDENCIAS: buscar acta con evidencias ---
    s, b, _ = req("GET", "/actas", token=admin_tok)
    actas = b.get("data", {}).get("content", [])
    print("actas visibles:", len(actas))
    objetivo = None
    for a in actas:
        if a.get("estado") in ("FIRMADA", "APROBADA", "RECHAZADA"):
            objetivo = a
            break
    if not objetivo:
        print("NO hay acta con evidencias para probar el modal.")
        return

    aid = objetivo["id"]
    print("acta objetivo id:", aid, "| estado:", objetivo["estado"])
    s, b, _ = req("GET", f"/actas/{aid}/evidencias", token=admin_tok)
    tipos = [e.get("tipo") for e in (b.get("data") or [])]
    print("tipos de evidencia:", tipos)

    for ep in ("firma", "foto", "pdf"):
        s2, body, hdrs = req("GET", f"/actas/{aid}/{ep}", token=admin_tok, raw=True)
        print(f"GET /actas/{aid}/{ep}: {s2} | {hdrs.get('Content-Type')} | bytes: {len(body)}")

    print("UNUSERS:", ",".join(unames))


if __name__ == "__main__":
    main()