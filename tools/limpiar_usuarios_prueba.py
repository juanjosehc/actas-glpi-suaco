#!/usr/bin/env python3
"""Elimina usuarios de prueba usados para verificar autenticacion/evidencias."""
import os

import psycopg2

CONN = dict(
    host=os.environ.get("PGHOST", "localhost"),
    port=int(os.environ.get("PGPORT", "5432")),
    dbname="SaucoDB",
    user="postgres",
    password=os.environ.get("PGPASSWORD", "Junio2026+"),
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