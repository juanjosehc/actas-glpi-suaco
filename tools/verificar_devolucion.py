#!/usr/bin/env python3
"""Verifica insercion de firma del tecnico en acta de devolucion generada."""
import json
import os
import tempfile
import urllib.request
import zipfile

BASE = "http://127.0.0.1:8001"


def req(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    r = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r) as x:
            return x.status, json.loads(x.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}")


def main():
    s, b = req("POST", "/auth/login", {"username": "firmatest", "password": "prueba123"})
    tok = b["data"]["token"]
    payload = {
        "fecha": "2026/08/25", "recibido_por": "Infraestructura", "entregado_por": "Maria Perez",
        "cargo_recibe": "Tecnico", "cedula": "12345", "area_recibe": "TI",
        "motivo": "devolucion-prueba", "cargo_entrega": "Auxiliar",
        "nombre_jefe": "Jefe", "cargo_jefe": "Lider",
        "equipos": [{"serial": "ZZZ999", "inventario": "INV001", "marca": "Dell",
                     "modelo": "OptiPlex 7090", "sistema_operativo": "Windows 10"}],
        "hardware": [{"nombre": "Monitor"}], "observaciones": "",
    }
    s, b = req("POST", "/generar-devolucion", payload, tok)
    print("generar-devolucion:", s, b.get("success"), b.get("mensaje", ""), b.get("nombre_zip", ""))

    zip_path = os.path.join("backend", "storage", "generated", b.get("nombre_zip", ""))
    z = zipfile.ZipFile(zip_path)
    tmp = tempfile.mkdtemp()
    z.extractall(tmp)
    docx = [os.path.join(tmp, n) for n in z.namelist() if n.endswith(".docx")][0]
    dz = zipfile.ZipFile(docx)
    xml = dz.read("word/document.xml").decode("utf-8")
    print("placeholder literal presente:", "{{firma_tecnico}}" in xml)
    media = [n for n in dz.namelist() if n.startswith("word/media/")]
    print("media:", media)
    for n in media:
        print(" ", n, dz.read(n).__len__(), "bytes")
    print("pic:pic count:", xml.count("<pic:pic"))
    print("Recepci presente:", "Recepci" in xml)


if __name__ == "__main__":
    main()