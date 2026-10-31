"""Zeichnet die Zifferblätter SLAVA, EIGER SPEED EDITION, BERING (hell und schwarz) und Bergmann 1956 und baut ihre .hwt-Dateien.

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


def bering_schwarz():
    weiss = (240, 240, 236)
    hg = Image.new("RGBA", (GROESSE * K, GROESSE * K), (0, 0, 0, 255))
    ebene = leer()
    bering_ebene(ebene, (*weiss, 255))
    hg.alpha_composite(ebene)
    teile = {
        "h": bering_zeiger(143, 3.0, weiss),
        "m": bering_zeiger(211, 2.6, weiss),
        "nabe": bering_nabe(weiss, (40, 40, 40)),
    }
    hell = (200, 200, 196)
    teile_a = {"h": bering_zeiger(143, 3.0, hell), "m": bering_zeiger(211, 2.6, hell), "nabe": bering_nabe(hell, (40, 40, 40))}
    schreiben("BeringSchwarz", "BERING Schwarz", "BERING in Schwarz für die Watch 3", klein(hg), bering_aod(), teile, teile_a, None)


# ---------- Bergmann 1956 ----------

BERG_HELL = (232, 230, 220)
SILBER_HELL = (240, 240, 236)
SILBER_DUNKEL = (138, 138, 136)
TAG_BERG = MITTE + 137
DIN = "/System/Library/Fonts/Supplemental/DIN Alternate Bold.ttf"
MENLO = "/System/Library/Fonts/Menlo.ttc"


def berg_zahl(ebene, winkel, r, text, schrift, farbe):
    """Text um die Mitte gedreht, Oberkante nach außen, wie die Minutenskala auf dem Foto."""
    blatt = Image.new("RGBA", (70 * K, 40 * K), (0, 0, 0, 0))
    ImageDraw.Draw(blatt).text((35 * K, 20 * K), text, font=schrift, fill=farbe, anchor="mm")
    blatt = blatt.rotate(-winkel, Image.BICUBIC, expand=True)
    x, y = radial(winkel, r, 0)
    ebene.alpha_composite(blatt, (int(x - blatt.width / 2), int(y - blatt.height / 2)))


def berg_ebene(ebene, aod=False, grund=BERG_HELL):
    d = ImageDraw.Draw(ebene)
    farbe = (*mischen((0, 0, 0), grund, 0.62 if aod else 1.0), 255)
    for i in range(60):
        if i % 5 == 0:
            innen = 170 if i == 15 else 160
            strich(d, i * 6, innen, 205, 4.2, farbe)
        elif not aod:
            strich(d, i * 6, 191, 205, 2.3, farbe)
    if not aod:
        skala = ImageFont.truetype(ARIAL, 16 * K)
        for i in range(12):
            text = "60" if i == 0 else f"{i * 5:02d}"
            berg_zahl(ebene, i * 30, 220, text, skala, farbe)
    zahl = ImageFont.truetype(DIN, 31 * K)
    for stunde in range(1, 13):
        if stunde == 3:
            continue
        x, y = radial(stunde * 30, 142, 0)
        d.text((x, y), str(stunde), font=zahl, fill=farbe, anchor="mm")
    text_mitte(ebene, (MITTE, MITTE - 63), "Bergmann", ImageFont.truetype(MENLO, 20 * K), farbe, abstand=2.2)
    text_mitte(ebene, (MITTE, MITTE - 36), "1956", ImageFont.truetype(MENLO, 18 * K), farbe, abstand=3.0)


def berg_hg():
    y, x = np.mgrid[0:GROESSE, 0:GROESSE]
    r = np.hypot(x + 0.5 - MITTE, y + 0.5 - MITTE)
    hell = 21 - 9 * np.clip(r / 233, 0, 1) ** 1.4
    flaeche = np.stack((hell, hell * 1.02, hell * 1.06), axis=-1)
    bild = Image.fromarray(flaeche.astype(np.uint8), "RGB").convert("RGBA").resize((GROESSE * K, GROESSE * K), Image.LANCZOS)
    ebene = leer()
    berg_ebene(ebene)
    d = ImageDraw.Draw(ebene)
    cx, cy = TAG_BERG * K, MITTE * K
    for b, h, f in ((26, 19, BERG_HELL), (24, 17, (8, 8, 9)), (20.5, 14.5, (120, 120, 118)), (19.5, 13.5, (4, 4, 5))):
        d.rectangle((cx - b * K, cy - h * K, cx + b * K, cy + h * K), fill=(*f, 255))
    bild.alpha_composite(schatten(ebene, 0.8, 0.5, (0.5, 0.8)))
    return klein(bild)


def berg_aod(grund=BERG_HELL):
    bild = Image.new("RGBA", (GROESSE * K, GROESSE * K), (0, 0, 0, 255))
    ebene = leer()
    berg_ebene(ebene, aod=True, grund=grund)
    bild.alpha_composite(ebene)
    return klein(bild)


def berg_ziffer(n):
    schrift = ImageFont.truetype(DIN, 22 * K)
    bild = Image.new("RGBA", (14 * K, 26 * K), (0, 0, 0, 0))
    ImageDraw.Draw(bild).text((7 * K, 13 * K), str(n), font=schrift, fill=(*BERG_HELL, 255), anchor="mm")
    return klein(bild)


def berg_zeiger(laenge, schweif, breit, aod=False):
    dimm = 0.72 if aod else 1.0
    hell = (*mischen((0, 0, 0), SILBER_HELL, dimm), 255)
    dunkel = (*mischen((0, 0, 0), SILBER_DUNKEL, dimm), 255)

    def malen(d, p):
        lanze = [(-breit * 0.75, 0), (-breit, laenge * 0.72), (0, laenge)]
        d.polygon([p(0, -schweif), p(-breit * 0.6, -schweif), *(p(x, y) for x, y in lanze)], fill=hell)
        d.polygon([p(0, -schweif), p(breit * 0.6, -schweif), *(p(-x, y) for x, y in lanze)], fill=dunkel)
        e = breit * 0.34
        d.polygon([p(-e, laenge * 0.14), p(e, laenge * 0.14), p(e, laenge * 0.8), p(0, laenge * 0.9), p(-e, laenge * 0.8)], fill=hell)

    return zeiger(laenge, schweif, malen)


def berg_sekunde():
    def malen(d, p):
        d.polygon([p(-1.6, -44), p(1.6, -44), p(0.5, 208), p(-0.5, 208)], fill=(*SILBER_HELL, 255))
        d.polygon([p(0, -44), p(1.6, -44), p(0.5, 208), p(0, 208)], fill=(176, 176, 172, 255))

    return zeiger(208, 44, malen)


def berg_nabe(aod=False):
    g = 34
    bild = Image.new("RGBA", (g * K, g * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    m = g * K / 2
    for rad, f in ((12.5, SILBER_DUNKEL), (11, (206, 206, 202)), (6.5, SILBER_DUNKEL), (5.2, SILBER_HELL), (2, SILBER_DUNKEL)):
        q = rad * K
        f = mischen((0, 0, 0), f, 0.72) if aod else f
        d.ellipse((m - q, m - q, m + q, m + q), fill=(*f, 255))
    return klein(bild)


def bergmann():
    teile = {
        "h": berg_zeiger(124, 18, 6.2),
        "m": berg_zeiger(198, 22, 5.2),
        "s": berg_sekunde(),
        "nabe": berg_nabe(),
        "ziffern": [berg_ziffer(i) for i in range(10)],
    }
    teile_a = {"h": berg_zeiger(124, 18, 6.2, aod=True), "m": berg_zeiger(198, 22, 5.2, aod=True), "nabe": berg_nabe(aod=True)}
    schreiben("Bergmann1956", "Bergmann 1956", "Bergmann 1956 für die Watch 3", berg_hg(), berg_aod(), teile, teile_a, TAG_BERG)


# ---------- Bergmann 1956 Gold ----------

GOLD_STUFEN = [(0.0, (98, 66, 22)), (0.35, (186, 142, 66)), (0.6, (236, 200, 120)), (0.82, (252, 230, 164)), (1.0, (255, 250, 222))]


def metall(ebene, kegel=True):
    """Färbt die deckenden Teile als helles Gold: Wölbung der Kanten, breite Lichtkeile, feiner Schliff."""
    a = np.asarray(ebene.getchannel("A"), dtype=float) / 255
    hoehe = np.asarray(ebene.getchannel("A").filter(ImageFilter.GaussianBlur(0.9 * K)), dtype=float) / 255
    gy, gx = np.gradient(hoehe)
    licht = -(gx * LICHT[0] + gy * LICHT[1])
    licht = np.clip(licht / max(np.percentile(np.abs(licht[a > 0.1]), 97), 1e-6), -1, 1) if (a > 0.1).any() else licht
    wert = 0.60 + 0.34 * licht
    y, x = np.mgrid[0:a.shape[0], 0:a.shape[1]]
    if kegel:
        theta = np.arctan2(x - a.shape[1] / 2, -(y - a.shape[0] / 2))
        wert += 0.17 * np.cos(2 * (theta - math.radians(35))) + 0.05 * np.cos(6 * theta + 1.0)
        streifen = np.random.default_rng(1956).normal(0, 1, 1440)
        wert += 0.022 * streifen[((theta + math.pi) / (2 * math.pi) * 1440).astype(int) % 1440]
    else:
        wert += 0.16 * (0.5 - y / a.shape[0])
    wert = np.clip(wert, 0, 1)
    stellen = [t for t, _ in GOLD_STUFEN]
    rgb = [np.interp(wert, stellen, [f[i] for _, f in GOLD_STUFEN]) for i in range(3)]
    aus = np.dstack((*rgb, a * 255)).clip(0, 255).astype(np.uint8)
    return Image.fromarray(aus, "RGBA")


def gold_hg():
    y, x = np.mgrid[0:GROESSE, 0:GROESSE]
    r = np.hypot(x + 0.5 - MITTE, y + 0.5 - MITTE)
    hell = 19 - 9 * np.clip(r / 233, 0, 1) ** 1.4
    flaeche = np.stack((hell * 1.04, hell * 1.0, hell * 0.96), axis=-1)
    bild = Image.fromarray(flaeche.astype(np.uint8), "RGB").convert("RGBA").resize((GROESSE * K, GROESSE * K), Image.LANCZOS)
    ebene = leer()
    berg_ebene(ebene)
    d = ImageDraw.Draw(ebene)
    cx, cy = TAG_BERG * K, MITTE * K
    # Doppelter Goldrahmen: außen, schmale Fuge, innen.
    rahmen = [(26.5, 19.5, 255), (23.8, 16.8, 0), (21, 14.8, 255), (19.8, 13.6, 0)]
    for b, h, wert in rahmen:
        d.rectangle((cx - b * K, cy - h * K, cx + b * K, cy + h * K), fill=(255, 255, 255, wert))
    bild.alpha_composite(schatten(metall(ebene), 1.0, 0.75, (0.8, 1.2)))
    return klein(bild)


def gold_ziffer(n):
    schrift = ImageFont.truetype(DIN, 22 * K)
    bild = Image.new("RGBA", (14 * K, 26 * K), (0, 0, 0, 0))
    ImageDraw.Draw(bild).text((7 * K, 13 * K), str(n), font=schrift, fill=(255, 255, 255, 255), anchor="mm")
    return klein(metall(bild, kegel=False))


def gold_zeiger(laenge, schweif, breit, aod=False):
    dimm = 0.72 if aod else 1.0
    hell = (*mischen((0, 0, 0), (255, 236, 170), dimm), 255)
    dunkel = (*mischen((0, 0, 0), (176, 132, 62), dimm), 255)
    glanz = (*mischen((0, 0, 0), (255, 250, 222), dimm), 255)
    mitte = (*mischen((0, 0, 0), (224, 184, 104), dimm), 255)

    def malen(d, p):
        lanze = [(-breit * 0.75, 0), (-breit, laenge * 0.72), (0, laenge)]
        d.polygon([p(0, -schweif), p(-breit * 0.6, -schweif), *(p(x, y) for x, y in lanze)], fill=hell)
        d.polygon([p(0, -schweif), p(breit * 0.6, -schweif), *(p(-x, y) for x, y in lanze)], fill=dunkel)
        e = breit * 0.36
        d.polygon([p(-e, laenge * 0.14), p(e, laenge * 0.14), p(e, laenge * 0.8), p(0, laenge * 0.9), p(-e, laenge * 0.8)], fill=mitte)
        q = breit * 0.13
        d.polygon([p(-q, laenge * 0.16), p(q, laenge * 0.16), p(q, laenge * 0.8), p(-q, laenge * 0.8)], fill=glanz)

    return zeiger(laenge, schweif, malen)


def gold_sekunde():
    def malen(d, p):
        d.polygon([p(-1.6, -44), p(1.6, -44), p(0.5, 208), p(-0.5, 208)], fill=(255, 236, 170, 255))
        d.polygon([p(0, -44), p(1.6, -44), p(0.5, 208), p(0, 208)], fill=(190, 148, 76, 255))

    return zeiger(208, 44, malen)


def gold_nabe(aod=False):
    g = 34
    bild = Image.new("RGBA", (g * K, g * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    m = g * K / 2
    for rad, f in ((12.5, (128, 92, 38)), (11, (232, 196, 118)), (6.5, (140, 102, 44)), (5.2, (255, 240, 190)), (2, (160, 118, 52))):
        q = rad * K
        f = mischen((0, 0, 0), f, 0.72) if aod else f
        d.ellipse((m - q, m - q, m + q, m + q), fill=(*f, 255))
    return klein(bild)


def bergmann_gold():
    teile = {
        "h": gold_zeiger(124, 18, 6.2),
        "m": gold_zeiger(198, 22, 5.2),
        "s": gold_sekunde(),
        "nabe": gold_nabe(),
        "ziffern": [gold_ziffer(i) for i in range(10)],
    }
    teile_a = {"h": gold_zeiger(124, 18, 6.2, aod=True), "m": gold_zeiger(198, 22, 5.2, aod=True), "nabe": gold_nabe(aod=True)}
    schreiben("Bergmann1956Gold", "Bergmann 1956 Gold", "Bergmann 1956 in Gold für die Watch 3", gold_hg(), berg_aod(grund=(236, 200, 120)), teile, teile_a, TAG_BERG)


# ---------- SLAVA Gold Glas ----------
# Streng symmetrisch und mit neutralem Licht: keine Lichtrichtung, jeder Index sieht gleich aus.
# Vom gewölbten Glas bleiben nur die Stauchung zum Rand und eine gleichmäßige Glaskante.

GLANZ_STUFEN = [(0.0, (66, 42, 10)), (0.28, (158, 114, 44)), (0.52, (230, 186, 98)), (0.76, (255, 226, 150)), (1.0, (255, 253, 236))]


def glanz_farbe(wert, alpha):
    """Macht aus einer Helligkeitskarte poliertes Gold."""
    stellen = [t for t, _ in GLANZ_STUFEN]
    wert = np.clip(wert, 0, 1)
    rgb = [np.interp(wert, stellen, [f[i] for _, f in GLANZ_STUFEN]) for i in range(3)]
    return Image.fromarray(np.dstack((*rgb, alpha)).clip(0, 255).astype(np.uint8), "RGBA")


def gold_neutral(ebene):
    """Gold ohne Lichtrichtung: Kanten dunkler, Mitte heller, ringsum gleich."""
    hoehe = np.asarray(ebene.getchannel("A").filter(ImageFilter.GaussianBlur(0.8 * K)), dtype=float) / 255
    return glanz_farbe(0.30 + 0.56 * hoehe, np.asarray(ebene.getchannel("A"), dtype=float))


def glas_index(wd, ad, winkel, r0, r1, halb):
    """Facettierter Index mit fester Helligkeit je Fläche, damit alle zwölf gleich aussehen."""
    spitze = halb * 1.2
    g = halb * 0.34
    g0, g1 = r0 + spitze, r1 - halb * 0.5
    flaechen = [
        ([(g0, -halb), (r1, -halb), (g1, -g), (g0, -g)], 0.50),
        ([(g0, halb), (r1, halb), (g1, g), (g0, g)], 0.50),
        ([(r1, -halb), (r1, halb), (g1, g), (g1, -g)], 0.36),
        ([(r0, 0), (g0, -halb), (g0, -g), (g0, 0)], 0.68),
        ([(r0, 0), (g0, halb), (g0, g), (g0, 0)], 0.68),
        ([(g0, -g), (g1, -g), (g1, g), (g0, g)], 0.80),
    ]
    for punkte, wert in flaechen:
        xy = [radial(winkel, l, q) for l, q in punkte]
        wd.polygon(xy, fill=int(wert * 255))
        ad.polygon(xy, fill=255)
    wd.line([radial(winkel, g0, 0), radial(winkel, g1, 0)], fill=250, width=int(0.5 * K))


def glas_blatt():
    """Einheitliches Anthrazit mit sehr feinem Sonnenschliff, ohne Lichtkeile."""
    rng = np.random.default_rng(1967)
    g = GROESSE * K
    y, x = np.mgrid[0:g, 0:g]
    dx, dy = (x + 0.5) / K - MITTE, (y + 0.5) / K - MITTE
    r = np.hypot(dx, dy)
    theta = np.arctan2(dx, -dy)
    n = 7200
    fein = rng.normal(0, 1, n)
    mittel = np.convolve(np.tile(rng.normal(0, 1, n), 3), np.ones(9) / 9, "same")[n:2 * n]
    i = ((theta + math.pi) / (2 * math.pi) * n).astype(int) % n
    hell = (58 + 4.5 * fein[i] + 6 * mittel[i]) * (1.03 - 0.10 * np.clip(r / 233, 0, 1) ** 2)
    aus = np.stack((hell * 1.02, hell * 0.975, hell * 0.94), axis=-1)
    return Image.fromarray(np.clip(aus, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def glas_indizes():
    g = GROESSE * K
    wk, ak = Image.new("L", (g, g), 0), Image.new("L", (g, g), 0)
    wd, ad = ImageDraw.Draw(wk), ImageDraw.Draw(ak)
    for stunde in range(12):
        if stunde != 3:
            glas_index(wd, ad, stunde * 30, 158, 222, 11.5)
    y, x = np.mgrid[0:g, 0:g]
    r = np.hypot(x + 0.5 - g / 2, y + 0.5 - g / 2) / K
    # Spiegelung läuft nur entlang des Index, deshalb ist sie bei allen gleich.
    wert = np.asarray(wk, dtype=float) / 255 + 0.13 * np.sin(r / 8.5) + 0.05 * np.sin(r / 3.1)
    return glanz_farbe(wert, np.asarray(ak, dtype=float))


def glas_datum(bild):
    cx, cy = TAG_SLAVA * K, MITTE * K
    rahmen = leer()
    d = ImageDraw.Draw(rahmen)
    ab, ah, ib, ih = 31 * K, 26 * K, 23 * K, 18 * K
    d.rectangle((cx - ab, cy - ah, cx + ab, cy + ah), fill=(255, 255, 255, 255))
    d.rectangle((cx - ib, cy - ih, cx + ib, cy + ih), fill=(0, 0, 0, 0))
    bild.alpha_composite(schatten(gold_neutral(rahmen), 1.2, 0.7))
    d = ImageDraw.Draw(bild)
    r = 1.2 * K
    d.rectangle((cx - ib - r, cy - ih - r, cx + ib + r, cy + ih + r), fill=(18, 16, 12, 255))
    d.rectangle((cx - ib, cy - ih, cx + ib, cy + ih), fill=(242, 238, 224, 255))


def woelben(bild):
    """Staucht das Blatt zum Rand hin, wie durch ein gewölbtes Glas gesehen; die Mitte bleibt unberührt."""
    q = np.asarray(bild.convert("RGB"), dtype=np.float32)
    g = q.shape[0]
    y, x = np.mgrid[0:g, 0:g]
    dx, dy = x + 0.5 - g / 2, y + 0.5 - g / 2
    ro = np.hypot(dx, dy) / K
    faktor = 1 + 0.052 * np.clip(ro / 233, 0, 1.1) ** 5
    sx = np.clip(g / 2 + dx * faktor - 0.5, 0, g - 1.001)
    sy = np.clip(g / 2 + dy * faktor - 0.5, 0, g - 1.001)
    x0, y0 = sx.astype(int), sy.astype(int)
    fx, fy = (sx - x0)[..., None], (sy - y0)[..., None]
    aus = (q[y0, x0] * (1 - fx) * (1 - fy) + q[y0, x0 + 1] * fx * (1 - fy)
           + q[y0 + 1, x0] * (1 - fx) * fy + q[y0 + 1, x0 + 1] * fx * fy)
    return Image.fromarray(aus.clip(0, 255).astype(np.uint8), "RGB").convert("RGBA")


def glaskante(bild):
    """Ringsum gleich: leichte Abdunklung zum Rand und eine feine helle Glaskante."""
    g = bild.width
    y, x = np.mgrid[0:g, 0:g]
    r = np.hypot(x + 0.5 - g / 2, y + 0.5 - g / 2) / (233 * K)
    q = np.asarray(bild.convert("RGB"), dtype=np.float32)
    t = np.clip((r - 0.90) / 0.075, 0, 1)
    dunkel = 1 - 0.42 * (t * t * (3 - 2 * t))
    hell = 58 * np.exp(-(((r - 0.962) / 0.0055) ** 2))
    aus = q * dunkel[..., None] + hell[..., None]
    return Image.fromarray(aus.clip(0, 255).astype(np.uint8), "RGB").convert("RGBA")


def glas_hg():
    bild = glas_blatt()
    ebene = leer()
    d = ImageDraw.Draw(ebene)
    for i in range(60):
        if i % 5 or i == 15:
            strich(d, i * 6, 214, 229, 1.5, (255, 255, 255, 255))
    text_mitte(ebene, (MITTE, MITTE - 109), "SLAVA", ImageFont.truetype(ARIAL, 26 * K), (255, 255, 255, 255), abstand=7)
    # Auf dem Foto teils verdeckt, gelesen als „21 Jewels“.
    text_mitte(ebene, (MITTE, MITTE + 104), "21 Jewels", ImageFont.truetype(ARIAL, 18 * K), (255, 255, 255, 255), abstand=0.6)
    bild.alpha_composite(schatten(gold_neutral(ebene), 0.9, 0.6))
    bild.alpha_composite(schatten(glas_indizes(), 1.4, 0.8))
    glas_datum(bild)
    bild = glaskante(woelben(bild))
    # Goldrand, auf dem das Glas sitzt.
    rand = leer()
    d = ImageDraw.Draw(rand)
    m = MITTE * K
    d.ellipse((m - 233 * K, m - 233 * K, m + 233 * K, m + 233 * K), fill=(255, 255, 255, 255))
    d.ellipse((m - 227.5 * K, m - 227.5 * K, m + 227.5 * K, m + 227.5 * K), fill=(0, 0, 0, 0))
    bild.alpha_composite(gold_neutral(rand))
    return klein(bild)


def glas_zeiger(laenge, schweif, basis, schulter, einlage, einlage_breit, stumpf=False):
    rand = 6
    halb = 20
    py = rand + laenge
    b, h = 2 * halb * K, (laenge + schweif + 2 * rand) * K
    wk, ak = Image.new("L", (b, h), 0), Image.new("L", (b, h), 0)
    wd, ad = ImageDraw.Draw(wk), ImageDraw.Draw(ak)
    p = lambda qx, qy: ((halb + qx) * K, (py - qy) * K)
    ende = 0.9 if stumpf else 0.8
    umriss = [p(-basis * 0.6, -schweif), p(-basis, 0), p(-schulter, laenge * ende), p(0, laenge),
              p(schulter, laenge * ende), p(basis, 0), p(basis * 0.6, -schweif)]
    wd.polygon(umriss, fill=168)
    ad.polygon(umriss, fill=255)
    wd.line([p(0, -schweif), p(0, laenge)], fill=250, width=int(0.5 * K))
    yy, _ = np.mgrid[0:h, 0:b] / K
    wert = np.asarray(wk, dtype=float) / 255 + 0.08 * np.sin(yy / 13)
    bild = glanz_farbe(wert, np.asarray(ak, dtype=float))
    d = ImageDraw.Draw(bild)
    e0, e1 = einlage
    pa, pb = p(-einlage_breit - 0.5, laenge * e1), p(einlage_breit + 0.5, laenge * e0)
    d.rectangle((pa[0], pa[1], pb[0], pb[1]), fill=(255, 232, 170, 255))
    pa, pb = p(-einlage_breit, laenge * e1 - 0.5), p(einlage_breit, laenge * e0 + 0.5)
    d.rectangle((pa[0], pa[1], pb[0], pb[1]), fill=(10, 9, 8, 255))
    return klein(schatten(bild, 1.2, 0.5)), (halb, py)


def glas_sekunde():
    def malen(d, p):
        d.polygon([p(-1.4, -42), p(1.4, -42), p(0.45, 192), p(-0.45, 192)], fill=(240, 204, 128, 255))

    return zeiger(192, 42, malen)


def glas_nabe():
    g = 34 * K
    y, x = np.mgrid[0:g, 0:g]
    r = np.hypot(x + 0.5 - g / 2, y + 0.5 - g / 2) / K
    wert = np.where(r < 2.4, 0.42, np.where(r < 6.4, 0.86, np.where(r < 7.6, 0.2, 0.62 + 0.12 * np.sin(r * 1.6))))
    return klein(glanz_farbe(wert, (r < 13) * 255.0))


def slava_gold_glas():
    teile = {
        "h": glas_zeiger(124, 32, 8, 8, (0.22, 0.86), 3.0, stumpf=True),
        "m": glas_zeiger(196, 36, 9, 3.6, (0.28, 0.79), 2.2),
        "s": glas_sekunde(),
        "nabe": glas_nabe(),
        "ziffern": [slava_ziffer(i) for i in range(10)],
    }
    teile_a = {
        "h": slava_zeiger(124, 32, 8, 8, (0.22, 0.86), 3.0, aod=True, stumpf=True),
        "m": slava_zeiger(196, 36, 9, 3.6, (0.28, 0.79), 2.2, aod=True),
        "nabe": slava_nabe(aod=True),
    }
    schreiben("SlavaGoldGlas", "SLAVA Gold Glas", "SLAVA in Gold mit gewölbtem Glas für die Watch 3",
              glas_hg(), slava_aod(), teile, teile_a, TAG_SLAVA)


def main():
    os.makedirs(os.path.join(ORDNER, "dist"), exist_ok=True)
    slava()
    eiger()
    bering()
    bering_schwarz()
    bergmann()
    bergmann_gold()
    slava_gold_glas()


if __name__ == "__main__":
    sys.exit(main())
