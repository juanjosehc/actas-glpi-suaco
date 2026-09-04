#!/usr/bin/env python3
"""Elimina usuarios de prueba usados para verificar autenticacion/evidencias.

SEC-002: sin secretos commiteados. La password sale de la variable de entorno
PGPASSWORD o, si falta, de DB_PASSWORD en backend/.env (gitignoreado).
"""
import os
import pathlib

import psycopg2


def _password() -> str:
    pwd = os.environ.get("PGPASSWORD")
    if pwd:
        return pwd
    env_file = pathlib.Path(__file__).resolve().parent.parent / "backend" / ".env"
    if env_file.exists():
        for linea in env_file.read_text(encoding="utf-8").splitlines():
            clave, sep, valor = linea.partition("=")
            if clave.strip() == "DB_PASSWORD" and sep:
                return valor.strip()
    raise SystemExit(
        "PGPASSWORD no esta definida y backend/.env no tiene DB_PASSWORD. "
        "Define PGPASSWORD (o copia backend/.env.example a backend/.env)."
    )


CONN = dict(
    host=os.environ.get("PGHOST", "localhost"),
    port=int(os.environ.get("PGPORT", "5432")),
    dbname="SaucoDB",
    user="postgres",
    password=_password(),
)

USUARIOS = ("admtest2", "blocktest2")

with psycopg2.connect(**CONN) as conn:
    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM usuario WHERE nombre_usuario = ANY(%s) RETURNING id_usuario",
            (list(USUARIOS),),
        )
        print("usuarios eliminados:", cur.fetchall())

print("ok")