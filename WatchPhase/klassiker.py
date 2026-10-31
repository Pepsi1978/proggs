"""Zeichnet die Zifferblätter SLAVA, EIGER SPEED EDITION und BERING und baut ihre .hwt-Dateien.

Vorlagen sind Franks Fotos der echten Uhren. Maße sind aus den Fotos auf 466 × 466 umgerechnet,
ohne deren Perspektive, Licht und Glasreflexe. Watch Phase bleibt in bauen.py unberührt.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import hwt
from bauen import GROESSE, MITTE, ORDNER, hand_ebene, jpg, png, rundmaske

K = 4  # Überabtastung
LICHT = np.array((-0.6, -0.8))  # Licht von oben links
TIMES = "/System/Library/Fonts/Supplemental/Times New Roman.ttf"
TIMES_FETT = "/System/Library/Fonts/Supplemental/Times New Roman Bold.ttf"
ARIAL = "/System/Library/Fonts/Supplemental/Arial.ttf"
ARIAL_FETT = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"


def mischen(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def leer(g=GROESSE):
    return Image.new("RGBA", (g * K, g * K), (0, 0, 0, 0))


def klein(bild):
    return bild.resize((bild.width // K, bild.height // K), Image.LANCZOS)


def radial(winkel, laengs, quer):
    """Punkt in Zifferblattpixeln: winkel im Uhrzeigersinn ab 12 Uhr, laengs nach außen, quer nach rechts."""
    w = math.radians(winkel)
    return (
        (MITTE + laengs * math.sin(w) + quer * math.cos(w)) * K,
        (MITTE - laengs * math.cos(w) + quer * math.sin(w)) * K,
    )


def strich(d, winkel, r0, r1, breite, farbe):
    d.polygon([radial(winkel, r0, -breite / 2), radial(winkel, r1, -breite / 2),
               radial(winkel, r1, breite / 2), radial(winkel, r0, breite / 2)], fill=farbe)


def text_mitte(ebene, xy, text, schrift, farbe, abstand=0.0):
    """Zeichnet Text zentriert, auf Wunsch mit Sperrung in Zifferblattpixeln."""
    d = ImageDraw.Draw(ebene)
    breiten = [d.textlength(z, font=schrift) for z in text]
    gesamt = sum(breiten) + abstand * K * (len(text) - 1)
    x = xy[0] * K - gesamt / 2
    for z, b in zip(text, breiten):
        d.text((x, xy[1] * K), z, font=schrift, fill=farbe, anchor="lm")
        x += b + abstand * K


def schatten(ebene, radius, staerke, versatz=(0, 0)):
    a = ebene.getchannel("A").filter(ImageFilter.GaussianBlur(radius * K))
    a = a.point(lambda v: int(v * staerke))
    s = Image.new("RGBA", ebene.size, (0, 0, 0, 0))
    s.putalpha(a)
    aus = Image.new("RGBA", ebene.size, (0, 0, 0, 0))
    aus.paste(s, (int(versatz[0] * K), int(versatz[1] * K)))
    return Image.alpha_composite(aus, ebene)


def zeiger(laenge, schweif, zeichnen):
    """Bild mit der Spitze nach oben. zeichnen(d, p) malt mit p(quer, laengs) als Umrechnung."""
    rand = 6
    halb = 20
    py = rand + laenge
    bild = Image.new("RGBA", (2 * halb * K, (laenge + schweif + 2 * rand) * K), (0, 0, 0, 0))
    zeichnen(ImageDraw.Draw(bild), lambda x, y: ((halb + x) * K, (py - y) * K))
    return klein(schatten(bild, 1.0, 0.35)), (halb, py)


def zeichnen(hg, teile, stunde, minute, sekunde, tag, tag_x):
    bild = hg.copy()
    if tag is not None:
        ziffern = teile["ziffern"]
        z = ziffern[0]
        bild.alpha_composite(ziffern[tag // 10], (tag_x - z.width, MITTE - z.height // 2))
        bild.alpha_composite(ziffern[tag % 10], (tag_x, MITTE - z.height // 2))
    sek = sekunde or 0
    paare = [(teile["h"], (stunde % 12 + minute / 60) * 30), (teile["m"], (minute + sek / 60) * 6)]
    if sekunde is not None and "s" in teile:
        paare.append((teile["s"], sekunde * 6))
    for (z, (px, py)), w in paare:
        leinwand = Image.new("RGBA", (GROESSE, GROESSE), (0, 0, 0, 0))
        leinwand.alpha_composite(z, (MITTE - px, MITTE - py))
        bild.alpha_composite(leinwand.rotate(-w, Image.BICUBIC, center=(MITTE, MITTE)))
    n = teile["nabe"]
    bild.alpha_composite(n, (MITTE - n.width // 2, MITTE - n.height // 2))
    return bild


def schreiben(dateiname, titel, beschreibung, hg, aod, teile, teile_a, tag_x):
    cover = rundmaske(zeichnen(hg, teile, 10, 8, 36, 12 if tag_x else None, tag_x))
    aod_bild = rundmaske(zeichnen(aod, teile_a, 10, 8, None, None, tag_x))
    ziffern = teile.get("ziffern") or []
    h, m, s, na = teile["h"], teile["m"], teile.get("s"), teile["nabe"]
    h_a, m_a, na_a = teile_a["h"], teile_a["m"], teile_a["nabe"]
    benannt = [
        ("vorschau", cover), ("hg", hg.convert("RGB")), ("aod", aod.convert("RGB")),
        *((f"ziffer{i}", z) for i, z in enumerate(ziffern)),
        ("h", h[0]), ("m", m[0]), *([("s", s[0])] if s else []), ("nabe", na),
        ("h_a", h_a[0]), ("m_a", m_a[0]), ("nabe_a", na_a),
    ]
    nr = {name: i for i, (name, _) in enumerate(benannt)}
    res = lambda name: f"A100_{nr[name]:03d}.png"
    nabe_xy = f"{MITTE - na.width // 2},{MITTE - na.height // 2}"
    datum = ""
    if ziffern:
        # Die Uhr behandelt res_position bei combined_res als Bildmitte, nicht als obere Kante:
        # mit der oberen Kante stand der Tag auf der Uhr über dem Feld.
        tag_y = MITTE
        tag = ",".join(res(f"ziffer{i}") for i in range(10))
        datum = (
            '  <element is_support_option="false" label="date" res_preview="">\n'
            f'    <layer index="1" draw_type="combined_res" res_name="{tag}" res_invalid="" res_default="" res_sign="" '
            f'x_offset="0" y_offset="0" res_position="{tag_x},{tag_y}" res_align="center" '
            'value_type="17" ambient_mode="0"/>\n  </element>\n'
        )
    config = f'''<?xml version="1.0" encoding="UTF-8"?>
<watchface>
  <element is_support_option="false" label="time" res_preview="">
    {hand_ebene(1, 150, 0, nr["h"], h[1])}
    {hand_ebene(2, 153, 0, nr["m"], m[1])}
    {hand_ebene(3, 154, 0, nr["s"], s[1]) if s else ""}
    <layer index="4" draw_type="single_res" res_name="{res("nabe")}" res_position="{nabe_xy}" ambient_mode="0"/>
    {hand_ebene(5, 150, 1, nr["h_a"], h_a[1])}
    {hand_ebene(6, 153, 1, nr["m_a"], m_a[1])}
    <layer index="7" draw_type="single_res" res_name="{res("nabe_a")}" res_position="{nabe_xy}" ambient_mode="1"/>
  </element>
{datum}  <element is_support_option="false" label="background" res_preview="{res("hg")}">
    <layer index="1" draw_type="single_res" res_name="{res("hg")}" res_position="0,0" ambient_mode="0"/>
    <layer index="2" draw_type="single_res" res_name="{res("aod")}" res_position="0,0" ambient_mode="1"/>
  </element>
</watchface>
'''.encode()
    description = f'''<?xml version="1.0" encoding="UTF-8"?>
<HwTheme>
  <title>{titel}</title>
  <title-cn>{titel}</title-cn>
  <author>Frank</author>
  <designer>Frank</designer>
  <screen>HWHD09</screen>
  <version>1.1.1</version>
  <font>Default</font>
  <font-cn>默认</font-cn>
  <briefinfo>{{"de_DE":"{beschreibung}"}}</briefinfo>
</HwTheme>
'''.encode()
    vorschau = {"cover.jpg": jpg(cover, 960), "aod.jpg": jpg(aod_bild, 960), "icon_small.jpg": jpg(cover, 390)}
    datei = hwt.hwt_bauen(config, [png(b) for _, b in benannt], description, vorschau)
    dist = os.path.join(ORDNER, "dist")
    with open(os.path.join(dist, f"{dateiname}.hwt"), "wb") as f:
        f.write(datei)
    cover.save(os.path.join(dist, f"vorschau-{dateiname}.png"))
    aod_bild.save(os.path.join(dist, f"vorschau-{dateiname}-aod.png"))
    print(dateiname, len(datei), "Bytes")


# ---------- SLAVA ----------

GOLD_HELL = (255, 228, 150)
GOLD_MITTE = (222, 180, 96)
GOLD_DUNKEL = (112, 78, 30)
CREME = (232, 224, 186)
TAG_SLAVA = MITTE + 148


def facette(normale):
    """Farbe einer Goldfläche aus ihrer Ausrichtung zum Licht."""
    n = np.array(normale, dtype=float)
    n /= np.linalg.norm(n)
    wert = 0.5 + 0.5 * float(n @ LICHT)
    return (*mischen(GOLD_DUNKEL, GOLD_HELL, wert ** 1.1), 255)


def slava_index(d, winkel, r0, r1, halb, dimm=1.0):
    """Facettierter Index: innen spitz, außen flach, mit schmalem Grat in der Mitte."""
    w = math.radians(winkel)
    aussen = np.array((math.sin(w), -math.cos(w)))
    rechts = np.array((math.cos(w), math.sin(w)))
    spitze = halb * 1.15
    g = halb * 0.36  # halbe Breite des Grats
    g0, g1 = r0 + spitze, r1 - halb * 0.45
    flaechen = [
        ([(r0 + spitze, -halb), (r1, -halb), (g1, -g), (g0, -g)], -rechts),
        ([(r0 + spitze, halb), (r1, halb), (g1, g), (g0, g)], rechts),
        ([(r1, -halb), (r1, halb), (g1, g), (g1, -g)], aussen),
        ([(r0, 0), (r0 + spitze, -halb), (g0, -g), (g0, 0)], -aussen * 0.6 - rechts),
        ([(r0, 0), (r0 + spitze, halb), (g0, g), (g0, 0)], -aussen * 0.6 + rechts),
    ]
    for punkte, normale in flaechen:
        f = facette(normale)
        d.polygon([radial(winkel, l, q) for l, q in punkte],
                  fill=(int(f[0] * dimm), int(f[1] * dimm), int(f[2] * dimm), 255))
    grat = (int(GOLD_MITTE[0] * dimm), int(GOLD_MITTE[1] * dimm), int(GOLD_MITTE[2] * dimm), 255)
    d.polygon([radial(winkel, l, q) for l, q in [(g0, -g), (g1, -g), (g1, g), (g0, g)]], fill=grat)


def slava_flaeche():
    """Anthrazit mit Sonnenschliff: feine radiale Bürstung, breite Lichtkeile."""
    rng = np.random.default_rng(1967)
    y, x = np.mgrid[0:GROESSE, 0:GROESSE]
    dx, dy = x + 0.5 - MITTE, y + 0.5 - MITTE
    r = np.hypot(dx, dy)
    theta = np.arctan2(dx, -dy)
    # Bürstung: Rauschen über dem Winkel, feiner werdend nach außen wirkt echt.
    n = 2880
    streifen = rng.normal(0, 1, n)
    streifen = (streifen + np.roll(streifen, 1) + np.roll(streifen, -1)) / 3
    i = ((theta + math.pi) / (2 * math.pi) * n).astype(int) % n
    buerste = streifen[i]
    keile = np.cos(2 * (theta - math.radians(32))) * 0.5 + 0.5
    hell = 50 + 26 * keile ** 1.6 + 5.5 * buerste
    hell *= 1.03 - 0.16 * np.clip(r / 233, 0, 1) ** 2
    aus = np.stack((hell * 1.0, hell * 0.965, hell * 0.95), axis=-1)
    return Image.fromarray(np.clip(aus, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def slava_datumsfenster(ebene):
    d = ImageDraw.Draw(ebene)
    cx, cy = TAG_SLAVA * K, MITTE * K
    ab, ah = 31 * K, 26 * K  # außen
    ib, ih = 23 * K, 18 * K  # weißes Feld
    d.rectangle((cx - ab, cy - ah, cx + ab, cy + ah), fill=(*GOLD_MITTE, 255))
    # Gerahmter Rand: oben und links hell, unten und rechts dunkel.
    d.polygon([(cx - ab, cy - ah), (cx + ab, cy - ah), (cx + ib, cy - ih), (cx - ib, cy - ih)], fill=(*GOLD_HELL, 255))
    d.polygon([(cx - ab, cy - ah), (cx - ib, cy - ih), (cx - ib, cy + ih), (cx - ab, cy + ah)], fill=(245, 212, 128, 255))
    d.polygon([(cx + ab, cy + ah), (cx - ab, cy + ah), (cx - ib, cy + ih), (cx + ib, cy + ih)], fill=(*GOLD_DUNKEL, 255))
    d.polygon([(cx + ab, cy + ah), (cx + ib, cy + ih), (cx + ib, cy - ih), (cx + ab, cy - ah)], fill=(150, 108, 48, 255))
    r = 1.2 * K
    d.rectangle((cx - ib - r, cy - ih - r, cx + ib + r, cy + ih + r), fill=(20, 20, 20, 255))
    d.rectangle((cx - ib, cy - ih, cx + ib, cy + ih), fill=(240, 237, 224, 255))


def slava_ziffer(n):
    schrift = ImageFont.truetype(ARIAL_FETT, 26 * K)
    bild = Image.new("RGBA", (16 * K, 30 * K), (0, 0, 0, 0))
    ImageDraw.Draw(bild).text((8 * K, 15 * K), str(n), font=schrift, fill=(26, 26, 30, 255), anchor="mm")
    return klein(bild)


def slava_ebene(d, ebene, aod=False):
    dimm = 0.62 if aod else 1.0
    for stunde in range(12):
        if stunde == 3:
            continue
        slava_index(d, stunde * 30, 158, 222, 11.5, dimm)
    if not aod:
        for i in range(60):
            if i % 5 or i == 15:
                strich(d, i * 6, 214, 229, 1.7, (*CREME, 255))
    farbe = (*mischen((0, 0, 0), CREME, dimm), 255)
    text_mitte(ebene, (MITTE, MITTE - 109), "SLAVA", ImageFont.truetype(ARIAL, 26 * K), farbe, abstand=7)
    if not aod:
        # Auf dem Foto teils verdeckt, gelesen als „21 Jewels“, wie auf Slava-Uhren üblich.
        text_mitte(ebene, (MITTE, MITTE + 104), "21 Jewels", ImageFont.truetype(ARIAL, 18 * K), farbe, abstand=0.6)


def slava_hg():
    bild = slava_flaeche()
    bild = bild.resize((GROESSE * K, GROESSE * K), Image.LANCZOS)
    ebene = leer()
    d = ImageDraw.Draw(ebene)
    slava_ebene(d, ebene)
    slava_datumsfenster(ebene)
    bild.alpha_composite(schatten(ebene, 1.2, 0.75, (1, 1.6)))
    return klein(bild)


def slava_aod():
    bild = Image.new("RGBA", (GROESSE * K, GROESSE * K), (0, 0, 0, 255))
    ebene = leer()
    slava_ebene(ImageDraw.Draw(ebene), ebene, aod=True)
    bild.alpha_composite(ebene)
    return klein(bild)


def slava_zeiger(laenge, schweif, basis, schulter, einlage, einlage_breit, aod=False, stumpf=False):
    dimm = 0.72 if aod else 1.0
    hell = (*mischen((0, 0, 0), GOLD_HELL, dimm), 255)
    dunkel = (*mischen((0, 0, 0), (168, 126, 58), dimm), 255)

    def malen(d, p):
        ende = 0.9 if stumpf else 0.8
        links = [p(0, -schweif), p(-basis * 0.6, -schweif), p(-basis, 0), p(-schulter, laenge * ende), p(0, laenge)]
        rechts = [p(0, -schweif), p(basis * 0.6, -schweif), p(basis, 0), p(schulter, laenge * ende), p(0, laenge)]
        d.polygon(links, fill=hell)
        d.polygon(rechts, fill=dunkel)
        e0, e1 = einlage
        d.rectangle((p(-einlage_breit, laenge * e1)[0], p(0, laenge * e1)[1],
                     p(einlage_breit, laenge * e0)[0], p(0, laenge * e0)[1]), fill=(14, 13, 12, 255))

    return zeiger(laenge, schweif, malen)


def slava_sekunde():
    def malen(d, p):
        d.polygon([p(-1.5, -42), p(1.5, -42), p(0.5, 192), p(-0.5, 192)], fill=(*GOLD_HELL, 255))
        d.polygon([p(0, -42), p(1.5, -42), p(0.5, 192), p(0, 192)], fill=(200, 158, 80, 255))

    return zeiger(192, 42, malen)


def slava_nabe(aod=False):
    g = 34
    bild = Image.new("RGBA", (g * K, g * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    m = g * K / 2
    stufen = [(13, GOLD_DUNKEL), (12, GOLD_MITTE), (7.5, GOLD_DUNKEL), (6.5, GOLD_HELL), (2.6, GOLD_MITTE)]
    for rad, f in stufen:
        q = rad * K
        f = mischen((0, 0, 0), f, 0.72) if aod else f
        d.ellipse((m - q, m - q, m + q, m + q), fill=(*f, 255))
    return klein(bild)


def slava():
    teile = {
        "h": slava_zeiger(124, 32, 8, 8, (0.22, 0.86), 3.0, stumpf=True),
        "m": slava_zeiger(196, 36, 9, 3.6, (0.28, 0.79), 2.2),
        "s": slava_sekunde(),
        "nabe": slava_nabe(),
        "ziffern": [slava_ziffer(i) for i in range(10)],
    }
    teile_a = {
        "h": slava_zeiger(124, 32, 8, 8, (0.22, 0.86), 3.0, aod=True, stumpf=True),
        "m": slava_zeiger(196, 36, 9, 3.6, (0.28, 0.79), 2.2, aod=True),
        "nabe": slava_nabe(aod=True),
    }
    schreiben("Slava", "SLAVA", "SLAVA für die Watch 3", slava_hg(), slava_aod(), teile, teile_a, TAG_SLAVA)


# ---------- EIGER SPEED EDITION ----------

ELFENBEIN = (246, 242, 228)
KUPFER = (212, 124, 82)
KUPFER_HELL = (242, 172, 128)
KUPFER_DUNKEL = (150, 80, 50)
SCHWARZ = (22, 22, 24)


def eiger_index(d, winkel, r0, r1, halb, innen):
    d.polygon([radial(winkel, r0, -halb), radial(winkel, r1, -halb),
               radial(winkel, r1, 0), radial(winkel, r0, 0)], fill=(*KUPFER_HELL, 255))
    d.polygon([radial(winkel, r0, 0), radial(winkel, r1, 0),
               radial(winkel, r1, halb), radial(winkel, r0, halb)], fill=(*KUPFER, 255))
    rand = 3.4
    d.polygon([radial(winkel, r0 + rand, -halb + rand), radial(winkel, r1 - rand, -halb + rand),
               radial(winkel, r1 - rand, halb - rand), radial(winkel, r0 + rand, halb - rand)], fill=(*innen, 255))


def eiger_schrift(ebene, schwarz, aod=False):
    d = ImageDraw.Draw(ebene)
    ziffer = ImageFont.truetype(ARIAL_FETT, 44 * K)
    for text, xy in (("12", (MITTE, MITTE - 155)), ("3", (MITTE + 159, MITTE)),
                     ("6", (MITTE, MITTE + 155)), ("9", (MITTE - 159, MITTE))):
        d.text((xy[0] * K, xy[1] * K), text, font=ziffer, fill=schwarz, anchor="mm")
    # EIGER in Kapitälchen: großes E, kleine Versalien auf gleicher Grundlinie.
    gross, klein_s = ImageFont.truetype(TIMES_FETT, 48 * K), ImageFont.truetype(TIMES_FETT, 37 * K)
    teile = [("E", gross), *((z, klein_s) for z in "IGER")]
    breite = sum(d.textlength(z, font=f) for z, f in teile) + 1.5 * K * (len(teile) - 1)
    x = MITTE * K - breite / 2
    grundlinie = (MITTE - 72) * K
    for z, f in teile:
        d.text((x, grundlinie), z, font=f, fill=schwarz, anchor="ls")
        x += d.textlength(z, font=f) + 1.5 * K
    if not aod:
        unten = ImageFont.truetype(TIMES, 30 * K)
        d.text((MITTE * K, (MITTE + 62) * K), "SPEED", font=unten, fill=schwarz, anchor="mm")
        d.text((MITTE * K, (MITTE + 100) * K), "EDITION", font=unten, fill=schwarz, anchor="mm")


def eiger_hg():
    y, x = np.mgrid[0:GROESSE, 0:GROESSE]
    r = np.hypot(x + 0.5 - MITTE, y + 0.5 - MITTE)
    schatten_rand = 1.0 - 0.05 * np.clip(r / 233, 0, 1) ** 3
    flaeche = np.stack([np.full(r.shape, c) * schatten_rand for c in ELFENBEIN], axis=-1)
    bild = Image.fromarray(flaeche.astype(np.uint8), "RGB").convert("RGBA").resize((GROESSE * K, GROESSE * K), Image.LANCZOS)
    ebene = leer()
    d = ImageDraw.Draw(ebene)
    for i in range(60):
        if i % 5:
            strich(d, i * 6, 213, 229, 1.9, (*SCHWARZ, 255))
        else:
            strich(d, i * 6, 205, 229, 2.8, (*SCHWARZ, 255))
    eiger_schrift(ebene, (*SCHWARZ, 255))
    bild.alpha_composite(ebene)
    indizes = leer()
    di = ImageDraw.Draw(indizes)
    innen = (247, 232, 214)
    for stunde in range(12):
        if stunde % 3:
            eiger_index(di, stunde * 30, 146, 199, 7.0, innen)
        else:
            eiger_index(di, stunde * 30, 181, 203, 7.4, innen)
    bild.alpha_composite(schatten(indizes, 0.7, 0.4, (0.6, 0.9)))
    return klein(bild)


def eiger_aod():
    bild = Image.new("RGBA", (GROESSE * K, GROESSE * K), (0, 0, 0, 255))
    ebene = leer()
    eiger_schrift(ebene, (188, 184, 172, 255), aod=True)
    d = ImageDraw.Draw(ebene)
    for stunde in range(12):
        if stunde % 3:
            eiger_index(d, stunde * 30, 146, 199, 7.0, (0, 0, 0))
        else:
            eiger_index(d, stunde * 30, 181, 203, 7.4, (0, 0, 0))
    bild.alpha_composite(ebene)
    return klein(bild)


def eiger_zeiger(laenge, schweif, basis, schulter, einlage, einlage_breit, aod=False):
    innen = (205, 200, 188, 255) if aod else (252, 249, 240, 255)

    def malen(d, p):
        d.polygon([p(0, -schweif), p(-basis * 0.8, -schweif), p(-basis, 0), p(-schulter, laenge * 0.9), p(0, laenge)],
                  fill=(*KUPFER_HELL, 255))
        d.polygon([p(0, -schweif), p(basis * 0.8, -schweif), p(basis, 0), p(schulter, laenge * 0.9), p(0, laenge)],
                  fill=(*KUPFER, 255))
        e0, e1 = einlage
        d.polygon([p(-einlage_breit, laenge * e0), p(einlage_breit, laenge * e0),
                   p(einlage_breit, laenge * e1), p(-einlage_breit, laenge * e1)], fill=innen)

    return zeiger(laenge, schweif, malen)


def eiger_sekunde():
    def malen(d, p):
        d.polygon([p(-1.3, -36), p(1.3, -36), p(0.45, 196), p(-0.45, 196)], fill=(*KUPFER, 255))

    return zeiger(196, 36, malen)


def eiger_nabe(aod=False):
    g = 28
    bild = Image.new("RGBA", (g * K, g * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    m = g * K / 2
    for rad, f in ((9.5, KUPFER_DUNKEL), (8.5, KUPFER), (3.6, KUPFER_DUNKEL), (2.6, KUPFER_HELL)):
        q = rad * K
        d.ellipse((m - q, m - q, m + q, m + q), fill=(*f, 255))
    return klein(bild)


def eiger():
    teile = {
        "h": eiger_zeiger(126, 26, 6.6, 5.6, (0.24, 0.84), 2.8),
        "m": eiger_zeiger(186, 32, 5.2, 4.2, (0.28, 0.88), 2.1),
        "s": eiger_sekunde(),
        "nabe": eiger_nabe(),
    }
    teile_a = {
        "h": eiger_zeiger(126, 26, 6.6, 5.6, (0.24, 0.84), 2.8, aod=True),
        "m": eiger_zeiger(186, 32, 5.2, 4.2, (0.28, 0.88), 2.1, aod=True),
        "nabe": eiger_nabe(aod=True),
    }
    schreiben("EigerSpeedEdition", "EIGER SPEED EDITION", "EIGER SPEED EDITION für die Watch 3",
              eiger_hg(), eiger_aod(), teile, teile_a, None)


# ---------- BERING ----------

BERING_BLATT = (245, 244, 234)
BERING_SCHWARZ = (28, 28, 32)


def bering_ebene(ebene, farbe, minuten=True, schrift_an=True):
    d = ImageDraw.Draw(ebene)
    for i in range(60):
        if i % 5 == 0:
            strich(d, i * 6, 152, 229, 2.5, farbe)
        elif minuten:
            strich(d, i * 6, 215, 229, 1.8, farbe)
    if schrift_an:
        text_mitte(ebene, (MITTE, MITTE - 72), "BERING", ImageFont.truetype(ARIAL, 18 * K), farbe, abstand=2.6)


def bering_hg():
    y, x = np.mgrid[0:GROESSE, 0:GROESSE]
    r = np.hypot(x + 0.5 - MITTE, y + 0.5 - MITTE)
    rand = 1.0 - 0.035 * np.clip(r / 233, 0, 1) ** 4
    flaeche = np.stack([np.full(r.shape, c) * rand for c in BERING_BLATT], axis=-1)
    bild = Image.fromarray(flaeche.astype(np.uint8), "RGB").convert("RGBA").resize((GROESSE * K, GROESSE * K), Image.LANCZOS)
    ebene = leer()
    bering_ebene(ebene, (*BERING_SCHWARZ, 255))
    bild.alpha_composite(ebene)
    return klein(bild)


def bering_aod():
    bild = Image.new("RGBA", (GROESSE * K, GROESSE * K), (0, 0, 0, 255))
    ebene = leer()
    bering_ebene(ebene, (170, 170, 166, 255), minuten=False)
    bild.alpha_composite(ebene)
    return klein(bild)


def bering_zeiger(laenge, basis, farbe):
    def malen(d, p):
        d.polygon([p(-basis * 0.7, -8), p(basis * 0.7, -8), p(basis, 0), p(basis * 0.42, laenge * 0.96), p(0, laenge),
                   p(-basis * 0.42, laenge * 0.96), p(-basis, 0)], fill=(*farbe, 255))

    return zeiger(laenge, 8, malen)


def bering_nabe(farbe, punkt):
    g = 34
    bild = Image.new("RGBA", (g * K, g * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    m = g * K / 2
    for rad, f in ((13, farbe), (4.8, punkt), (1.8, farbe)):
        q = rad * K
        d.ellipse((m - q, m - q, m + q, m + q), fill=(*f, 255))
    return klein(bild)


def bering():
    teile = {
        "h": bering_zeiger(143, 3.0, BERING_SCHWARZ),
        "m": bering_zeiger(211, 2.6, BERING_SCHWARZ),
        "nabe": bering_nabe(BERING_SCHWARZ, (176, 176, 172)),
    }
    hell = (200, 200, 196)
    teile_a = {"h": bering_zeiger(143, 3.0, hell), "m": bering_zeiger(211, 2.6, hell), "nabe": bering_nabe(hell, (40, 40, 40))}
    schreiben("Bering", "BERING", "BERING für die Watch 3", bering_hg(), bering_aod(), teile, teile_a, None)


def main():
    os.makedirs(os.path.join(ORDNER, "dist"), exist_ok=True)
    slava()
    eiger()
    bering()


if __name__ == "__main__":
    sys.exit(main())
