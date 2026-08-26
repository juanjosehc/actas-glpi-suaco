#!/usr/bin/env python3
"""
Agrega el placeholder {{firma_tecnico}} a las plantillas DOCX de actas.

- Acta de Entrega: agrega al bloque de firma (despues de 'Cargo') dos filas:
  etiqueta 'Firma del Tecnico' + celda {{firma_tecnico}}.
- ActaDevolucion: escribe {{firma_tecnico}} en la celda vacia del bloque
  'Recepcion (Direccion de Infraestructura)' (el tecnico es quien recibe).

Idempotente: si {{firma_tecnico}} ya existe en la plantilla, no hace nada.
Uso: python tools/agregar_firma_tecnico_templates.py
"""
import os
import re
import shutil
import sys
import tempfile
import zipfile
import xml.etree.ElementTree as ET
from copy import deepcopy

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"


def q(tag):
    return "{%s}%s" % (W, tag)


def cell_text(tc):
    return "".join(t.text or "" for t in tc.iter(q("t")))


def cells_of(tr):
    return tr.findall(q("tc"))


def set_cell_text(tc, text):
    """Colapsa el texto del run a un solo valor (mantiene formato del primer run).
    Si la celda no tiene runs (p.ej. celda vacia de firma), crea uno."""
    ts = [t for t in tc.iter(q("t"))]
    if ts:
        ts[0].text = text
        for t in ts[1:]:
            t.text = None
        return
    # Celda sin texto: crear <w:r><w:t> en el primer parrafo de la celda.
    p = tc.find(q("p"))
    if p is None:
        return
    # Insertar el run despues de pPr (si existe) para respetar formato de parrafo.
    ppr = p.find(q("pPr"))
    if ppr is not None:
        run = ET.Element(q("r"))
        p.insert(list(p).index(ppr) + 1, run)
    else:
        run = ET.SubElement(p, q("r"))
    t = ET.SubElement(run, q("t"))
    t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    t.text = text


def find_tbl_by_cell_text(root, needle):
    """Retorna la tabla del cuerpo que contenga una celda con el texto dado."""
    body = root.find(q("body"))
    if body is None:
        return None
    for tbl in body.iter(q("tbl")):
        for tr in tbl.findall(q("tr")):
            for tc in tr.findall(q("tc")):
                if needle in cell_text(tc):
                    return tbl
    return None


def rows(tbl):
    return tbl.findall(q("tr"))


def insert_row_after(tbl, anchor_row, new_row):
    children = list(tbl)
    pos = children.index(anchor_row) + 1
    tbl.insert(pos, new_row)


def process_docx(path, entrega):
    tmp_xml = None
    zin = zipfile.ZipFile(path, "r")
    document_xml = zin.read("word/document.xml")
    zin.close()

    root = ET.fromstring(document_xml)
    changed = False

    if entrega:
        tbl = find_tbl_by_cell_text(root, "{{firma_usuario}}")
        if tbl is None:
            raise RuntimeError("Tabla de firma no encontrada en " + path)
        if any("{{firma_tecnico}}" in cell_text(tc) for tr in rows(tbl) for tc in cells_of(tr)):
            print("  ya tiene {{firma_tecnico}}, sin cambios")
            return

        # Clonar fila etiqueta ('Entrega') y fila firma ('{{firma_usuario}}').
        label_anchor = None
        firma_anchor = None
        cargo_row = None
        for tr in rows(tbl):
            txts = [cell_text(tc) for tc in cells_of(tr)]
            joined = "".join(txts)
            if "Entrega" in joined and "{{firma_usuario}}" not in joined:
                label_anchor = tr
            if "{{firma_usuario}}" in joined:
                firma_anchor = tr
            if "Cargo" in joined:
                cargo_row = tr
        if label_anchor is None or firma_anchor is None or cargo_row is None:
            raise RuntimeError("Filas de firma no localizadas en " + path)

        label_row = deepcopy(label_anchor)
        for tc in cells_of(label_row):
            set_cell_text(tc, "Firma del Tecnico")
        firma_row = deepcopy(firma_anchor)
        for tc in cells_of(firma_row):
            set_cell_text(tc, "{{firma_tecnico}}")

        insert_row_after(tbl, cargo_row, label_row)
        insert_row_after(tbl, label_row, firma_row)
        changed = True
    else:
        # Devolucion: celda vacia del bloque 'Recepcion (Direccion...)'.
        tbl = find_tbl_by_cell_text(root, "Recepci")
        if tbl is None:
            raise RuntimeError("Bloque Recepcion no encontrado en " + path)
        target = None
        prev = None
        for i, tr in enumerate(rows(tbl)):
            joined = "".join(cell_text(tc) for tc in cells_of(tr))
            if i == 1:
                target = tr  # segunda fila: celda de firma vacia
            prev = tr
        if target is None:
            raise RuntimeError("Fila de firma vacia no encontrada en " + path)

        firma_cell = cells_of(target)[0]
        if "{{firma_tecnico}}" in cell_text(firma_cell):
            print("  ya tiene {{firma_tecnico}}, sin cambios")
            return
        set_cell_text(firma_cell, "{{firma_tecnico}}")
        changed = True

    if changed:
        new_xml = ET.tostring(root, encoding="UTF-8", xml_declaration=True)
        # Reescribir el zip en temp y mover (transaccional ante errores).
        fd, tmp = tempfile.mkstemp(suffix=".docx", dir=os.path.dirname(path))
        os.close(fd)
        zin = zipfile.ZipFile(path, "r")
        zout = zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED)
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "word/document.xml":
                data = new_xml
            zout.writestr(item, data)
        zin.close()
        zout.close()
        shutil.move(tmp, path)
        print("  actualizada:", path)


def plantillas_dir():
    here = os.path.dirname(os.path.abspath(__file__))
    return os.path.normpath(os.path.join(here, "..", "backend", "src", "main", "resources", "plantillas"))


def main():
    base = os.path.abspath(os.path.join(os.getcwd(), "backend", "src", "main", "resources", "plantillas"))
    if not os.path.isdir(base):
        base = plantillas_dir()
    if not os.path.isdir(base):
        print("No encuentro el directorio de plantillas:", base, file=sys.stderr)
        sys.exit(1)

    process_docx(os.path.join(base, "Acta de Entrega 2 2 - copia.docx"), entrega=True)
    process_docx(os.path.join(base, "ActaDevolucion.docx"), entrega=False)
    print("Listo.")


if __name__ == "__main__":
    main()