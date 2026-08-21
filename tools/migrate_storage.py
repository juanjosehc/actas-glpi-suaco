#!/usr/bin/env python3
"""Migra los archivos del almacenamiento temporal antiguo al storage permanente.

Uso:
  python tools/migrate_storage.py                 # usa defaults (TEMP + cwd/storage)
  python tools/migrate_storage.py --root D:/ActasStorage
  python tools/migrate_storage.py --root /data/actas --old-temp C:/Users/x/AppData/Local/Temp
  python tools/migrate_storage.py --old-generated /ruta/antigua/generados --old-uploads /ruta/antigua/uploads
  python tools/migrate_storage.py --dry-run

Reglas:
  - No sobrescribe archivos existentes en el destino (salta con warning).
  - Las rutas en base de datos (uploads/...) NO cambian: el esquema virtual
    se conserva, solo se mueve la raiz del filesystem.
  - Idempotente: re-ejecutable sin dano.
"""

import argparse
import os
import shutil
import sys
from pathlib import Path

SUB_DIRS = ("pdf", "firmas", "fotos")


def old_temp_dir() -> Path:
    for key in ("TEMP", "TMP", "TMPDIR"):
        val = os.environ.get(key)
        if val:
            return Path(val)
    return Path("/tmp")


def find_old_sources(old_temp: Path):
    """Ubica los directorios antiguos existentes que valga la pena migrar.

    Solo se migra el almacenamiento temporal del SO (o directorios indicados
    explicitamente con --old-generated/--old-uploads). Jamas se toca el arbol
    del repositorio.
    """
    candidates = {
        "generated": [old_temp / "actas_glpi_generados"],
        "uploads": [old_temp / "actas_glpi_uploads"],
    }
    found = {}
    for kind, paths in candidates.items():
        existing = [p for p in paths if p.is_dir()]
        if existing:
            found[kind] = existing
    return found


def move_file(src: Path, dst: Path, dry_run: bool) -> str:
    if src.is_dir():
        dst.mkdir(parents=True, exist_ok=True)
        for child in sorted(src.iterdir()):
            yield from move_file(child, dst / child.name, dry_run)
        return
    if not src.is_file():
        return
    if dst.exists():
        yield f"SKIP  existe: {dst}  (origen: {src})"
        return
    if dry_run:
        yield f"MOVER {src} -> {dst}"
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(src), str(dst))
    yield f"MOVIO {src} -> {dst}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", help="Nuevo storage root (default: $STORAGE_ROOT o <cwd>/storage)")
    parser.add_argument("--old-temp", help="Directorio temp antiguo (default: $TEMP)")
    parser.add_argument("--old-generated", help="Directorio antiguo de generados (default: <temp>/actas_glpi_generados)")
    parser.add_argument("--old-uploads", help="Directorio antiguo de uploads (default: <temp>/actas_glpi_uploads)")
    parser.add_argument("--dry-run", action="store_true", help="Solo listar, no mover nada")
    args = parser.parse_args()

    root = Path(args.root or os.environ.get("STORAGE_ROOT") or str(Path.cwd() / "storage"))
    old_temp = Path(args.old_temp or old_temp_dir())

    sources = {
        "generated": [Path(args.old_generated)] if args.old_generated else [],
        "uploads": [Path(args.old_uploads)] if args.old_uploads else [],
    }
    if not any(sources.values()):
        sources = find_old_sources(old_temp)
    if not sources:
        print("No se encontraron archivos antiguos que migrar. Nada que hacer.")
        return 0

    moved = skipped = 0
    for kind, paths in sources.items():
        for old in paths:
            dst_dir = root / kind
            for result in move_file(old, dst_dir, args.dry_run):
                if result.startswith(("MOVIO", "MOVER")):
                    print(result)
                    moved += 1
                else:
                    print(result)
                    skipped += 1

    print(f"\nTotal: {moved} movidos, {skipped} omitidos (ya existian).")
    if args.dry_run:
        print("Dry-run: no se modifico nada. Re-ejecute sin --dry-run para migrar.")
    else:
        print(f"Archivos ahora en: {root}")
        print("Reinicie la aplicacion con STORAGE_ROOT apuntando a la misma raiz (o storage.root en application.yml).")
        print("Las rutas en la base de datos (uploads/...) siguen validas; no requieren actualizacion.")
    return 0


if __name__ == "__main__":
    sys.exit(main())