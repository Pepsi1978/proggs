"""Schreibt Huawei-Zifferblätter (.hwt) ohne Theme Studio.

Das Format stammt aus der Analyse echter Theme-Studio-Exporte. tests/test_rundreise.py
prüft das Ergebnis Byte für Byte gegen solche Exporte.
"""
import hashlib
import io
import struct
import xml.etree.ElementTree as ET
import zipfile

from PIL import Image

RLE_MARKE = b"\x89\x67\x45\x23"
BILD_MAGIC = b"\x45\x23\x88\x88"
BILDBEREICH_KOPF = b"\x55\x55\x55\x55\x01\x00\x00\x01"
# Vierter Wert in hashCode: in allen untersuchten Exporten gleich, Herkunft unbekannt.
HASH_KONSTANTE = "0E0B408C4FA09BA786E0530DAAEBDB7D972C42EFB8710CDB9377B859B9468444"
ELEMENT_ID = {"background": 0, "time": 1, "date": 2, "widget": 3}


def _varint(n):
    aus = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        aus.append(b | (0x80 if n else 0))
        if not n:
            return bytes(aus)


def _zahl(feld, wert):
    return _varint(feld << 3) + _varint(wert)


def _block(feld, inhalt):
    if isinstance(inhalt, str):
        inhalt = inhalt.encode("utf-8")
    return _varint(feld << 3 | 2) + _varint(len(inhalt)) + inhalt


def _nummern(werte):
    return [int(w) for w in werte.split(",")]


def _punkt(werte):
    x, y = _nummern(werte)
    return _zahl(1, x) + _zahl(2, y)


def _rechteck(werte):
    return b"".join(_zahl(i + 1, w) for i, w in enumerate(_nummern(werte)))


def _farbe(wert):
    a, r, g, b = bytes.fromhex(wert.lstrip("#"))
    return _zahl(1, r) + _zahl(2, g) + _zahl(3, b) + _zahl(4, a)


def _res(name):
    return name.rsplit("_", 1)[1].split(".")[0]


def _text_teil(l, schriften):
    return (
        _block(1, _rechteck(l.get("text_rect")))
        + _block(2, _farbe(l.get("text_color")))
        + _zahl(3, schriften[(l.get("text_font"), l.get("text_size"))])
        + _zahl(4, {"left": 0, "center": 1, "right": 2}[l.get("text_align")])
    )


def _ebene(i, l, schriften):
    art = l.get("draw_type")
    amb = int(l.get("ambient_mode"))
    if art == "single_res":
        teil = _block(1, _res(l.get("res_name"))) + _block(2, _punkt(l.get("res_position")))
        return _zahl(1, i) + _zahl(2, 1) + _block(4, teil + _zahl(5, amb) + _zahl(6, 0))
    if art == "selected_res":
        teil = b"".join(_block(1, _res(n)) for n in l.get("res_name").split(","))
        teil += _block(2, _punkt(l.get("res_position"))) + _zahl(3, int(l.get("value_type")))
        return _zahl(1, i) + _zahl(2, 2) + _block(5, teil + _zahl(6, amb))
    if art == "hand_res":
        teil = (
            _block(1, _res(l.get("res_left")))
            + _block(2, _rechteck(l.get("rotate_rect")))
            + _block(3, _punkt(l.get("rotate_point_hand")))
            + _zahl(4, int(l.get("rotate_start_angel")))
            + _zahl(5, int(l.get("rotate_end_angel")))
            + _zahl(6, int(l.get("value_type")))
            + _zahl(9, amb)
        )
        return _zahl(1, i) + _zahl(2, 5) + _block(8, teil)
    if art == "text":
        teil = _text_teil(l, schriften) + _zahl(5, int(l.get("value_type"))) + _zahl(7, amb)
        return _zahl(1, i) + _zahl(2, 0) + _block(3, teil)
    if art == "text2_res":
        teil = (
            _text_teil(l, schriften)
            + _zahl(5, int(l.get("connect_type")))
            + _zahl(6, int(l.get("value_type_one")))
            + _zahl(7, int(l.get("value_type_two")))
            + _zahl(9, amb)
        )
        return _zahl(1, i) + _zahl(2, 10) + _block(11, teil)
    raise ValueError(f"unbekannter draw_type: {art}")


def _beschreibung(config_xml, titel, titel_cn, schriften):
    aus = b""
    for el in ET.fromstring(config_xml):
        vorschau = el.get("res_preview", "")
        inhalt = _zahl(1, ELEMENT_ID[el.get("label")]) + _zahl(2, 0) + _zahl(3, 0)
        inhalt += _block(4, _res(vorschau) if vorschau else "")
        for i, l in enumerate(el.findall("layer")):
            inhalt += _block(5, _ebene(i, l, schriften))
        aus += _block(1, inhalt)
    return aus + _block(2, titel) + _block(3, titel_cn)


def bild_kodieren(png_bytes):
    """BGRA mit gerader Alpha; Läufe ab vier gleichen Pixeln werden zusammengefasst."""
    bild = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
    b, g, r, a = (bild.getchannel(c).tobytes() for c in "BGRA")
    roh = bytes(x for p in zip(b, g, r, a) for x in p)
    aus = bytearray(BILD_MAGIC + struct.pack("<HH", *bild.size))
    i, n = 0, len(roh)
    while i < n:
        p = roh[i : i + 4]
        j = i + 4
        while j < n and roh[j : j + 4] == p:
            j += 4
        lauf = (j - i) // 4
        if lauf >= 4:
            aus += RLE_MARKE + p + struct.pack("<I", lauf)
        else:
            aus += p * lauf
        i = j
    return bytes(aus)


def watchface_bin(config_xml, bilder, titel, titel_cn, schriften=None):
    """bilder: Liste von PNG-Bytes in Reihenfolge A100_001, A100_002, ..."""
    pb = _beschreibung(config_xml, titel, titel_cn, schriften or {})
    kodiert = [bild_kodieren(b) for b in bilder]
    tabelle, pos = b"", len(BILDBEREICH_KOPF)
    for k in kodiert:
        tabelle += struct.pack("<II", pos, len(k))
        pos += len(k)
    kopf = struct.pack("<HHIII", 2, len(pb), len(tabelle), pos, 0)
    return kopf + pb + tabelle + BILDBEREICH_KOPF + b"".join(kodiert)


def _zip(eintraege):
    puffer = io.BytesIO()
    with zipfile.ZipFile(puffer, "w", zipfile.ZIP_DEFLATED) as z:
        for name, inhalt in eintraege:
            if name.endswith("/"):
                z.writestr(zipfile.ZipInfo(name), b"")
            else:
                z.writestr(name, inhalt)
    return puffer.getvalue()


def hwt_bauen(config_xml, bilder, description_xml, ability_xml, vorschau, titel, titel_cn):
    """vorschau: dict mit cover.jpg, aod.jpg, icon_small.jpg als Bytes."""
    innen = [("watchface/", b""), ("watchface/res/", b"")]
    innen += [(f"watchface/res/A100_{i + 1:03d}.png", b) for i, b in enumerate(bilder)]
    innen += [
        ("watchface/watch_face_config.xml", config_xml),
        ("watchface/watch_face_info.xml", description_xml),
        ("watchface.bin", watchface_bin(config_xml, bilder, titel, titel_cn)),
    ]
    gesicht = _zip(innen)
    pruef = [hashlib.sha256(x).hexdigest().upper() for x in (description_xml, gesicht, ability_xml)]
    aussen = [
        ("ability.xml", ability_xml),
        ("com.huawei.watchface", gesicht),
        ("description.xml", description_xml),
        ("hashCode", ",".join(pruef + [HASH_KONSTANTE]).encode()),
        ("preview/", b""),
    ] + [(f"preview/{k}", v) for k, v in sorted(vorschau.items())]
    return _zip(aussen)
