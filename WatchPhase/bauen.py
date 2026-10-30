"""Zeichnet das Zifferblatt „Watch Phase“ in 466 × 466 und baut dist/WatchPhase.hwt."""
import io
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import hwt

GROESSE = 466
MITTE = GROESSE // 2
SS = 4  # Überabtastung
ORDNER = os.path.dirname(os.path.abspath(__file__))
TITEL = "Watch Phase"
TAG_X = 110  # Abstand des Datums von der Mitte
BLAU = (64, 170, 245)
LEUCHTEN = (70, 175, 255)
SCHRIFT = "/System/Library/Fonts/Supplemental/Times New Roman.ttf"  # nur macOS
SCHRIFT_TAG = "/System/Library/Fonts/Supplemental/Arial.ttf"  # nur macOS
# Lage der value_type-Werte in der Bitliste von ability.xml, aus Theme-Studio-Exporten abgeleitet.
WERT_BIT = {70: 64, 71: 65, 150: 141, 153: 144, 154: 145}
ROEMISCH = ["XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"]


def leer(g=GROESSE):
    return Image.new("RGBA", (g * SS, g * SS), (0, 0, 0, 0))


def klein(bild):
    return bild.resize((bild.width // SS, bild.height // SS), Image.LANCZOS)


def glimmen(bild, radius, staerke=1.0, farbe=LEUCHTEN):
    """Legt einen farbigen Schein um die deckenden Teile des Bildes."""
    a = bild.getchannel("A").filter(ImageFilter.GaussianBlur(radius * SS))
    a = a.point(lambda v: min(255, int(v * staerke)))
    schein = Image.new("RGBA", bild.size, (*farbe, 0))
    schein.putalpha(a)
    return Image.alpha_composite(schein, bild)


def scheibe():
    """Konzentrische Ringe in sechs abwechselnd hellen und dunklen Segmenten."""
    y, x = np.mgrid[0:GROESSE * SS, 0:GROESSE * SS]
    dx = (x + 0.5) / SS - MITTE
    dy = (y + 0.5) / SS - MITTE
    r = np.hypot(dx, dy)
    winkel = (np.degrees(np.arctan2(dx, -dy)) + 360) % 360
    segment = (((winkel + 50) % 360) // 60).astype(int)
    hell = segment % 2 == 0
    abstand = 4.2
    linie = np.abs((r % abstand) - abstand / 2) < 0.48
    # Ein schwarzes Fenster hält den Tag lesbar, wie auf dem Foto.
    fenster = (np.abs(dx - TAG_X) < 21) & (np.abs(dy) < 15)
    innen = (r < 144) & ~fenster
    hg = np.zeros((*r.shape, 3))
    hg[hell & innen] = (12, 20, 34)
    hg[~hell & innen] = (6, 10, 17)
    ring = np.where(hell[..., None], np.array([[[116, 148, 190]]]), np.array([[[72, 92, 124]]]))
    aus = np.where((linie & innen)[..., None], ring, hg)
    bild = Image.fromarray(aus.astype(np.uint8), "RGB").convert("RGBA")
    return bild


def ziffern_und_punkte(gedimmt=False):
    ebene = leer()
    punkte = ImageDraw.Draw(ebene)
    for i in range(60):
        w = math.radians(i * 6)
        px = (MITTE + 207 * math.sin(w)) * SS
        py = (MITTE - 207 * math.cos(w)) * SS
        if i % 5 == 0:
            f, rad = (255, 255, 255, 255), 5.6
        else:
            f, rad = (*BLAU, 255), 2.8
        if gedimmt and i % 5:
            continue
        rad *= SS
        punkte.ellipse((px - rad, py - rad, px + rad, py + rad), fill=f)
    schrift = ImageFont.truetype(SCHRIFT, 50 * SS)
    for stunde, text in enumerate(ROEMISCH):
        blatt = Image.new("RGBA", (180 * SS, 80 * SS), (0, 0, 0, 0))
        d = ImageDraw.Draw(blatt)
        d.text((90 * SS, 40 * SS), text, font=schrift, fill=(255, 255, 255, 255), anchor="mm")
        # Schmal wie auf dem Foto; 3, 6, 9 und 12 stehen gerade, die übrigen um 30 Grad geneigt.
        blatt = blatt.resize((int(blatt.width * 0.62), blatt.height), Image.LANCZOS)
        drehung = (stunde * 30 + 45) % 90 - 45
        blatt = blatt.rotate(-drehung, Image.BICUBIC, expand=True)
        w = math.radians(stunde * 30)
        px = (MITTE + 172 * math.sin(w)) * SS - blatt.width / 2
        py = (MITTE - 172 * math.cos(w)) * SS - blatt.height / 2
        ebene.alpha_composite(blatt, (int(px), int(py)))
    return glimmen(ebene, 1.8, 1.3)


def hintergrund():
    bild = scheibe()
    bild.alpha_composite(ziffern_und_punkte())
    return klein(bild)


def hintergrund_aod():
    bild = Image.new("RGBA", (GROESSE * SS, GROESSE * SS), (0, 0, 0, 255))
    z = ziffern_und_punkte(gedimmt=True)
    a = z.getchannel("A").point(lambda v: int(v * 0.55))
    z.putalpha(a)
    bild.alpha_composite(z)
    return klein(bild)


def zeiger(laenge, schweif, breite, ring_bei=None, ring=6, farbe=(255, 255, 255, 255), schein=LEUCHTEN, staerke=1.3):
    """Bild mit der Spitze nach oben; gibt das Bild und den Drehpunkt zurück."""
    rand = 7
    halb = int(max(breite, 2 * ring, 2 * 5.5) / 2 + rand + 3)
    b, h = 2 * halb, laenge + schweif + 2 * rand
    py = rand + laenge
    bild = Image.new("RGBA", (b * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    cx = halb * SS
    cy = py * SS
    w = breite * SS / 2
    d.polygon(
        [(cx - w * 0.6, cy + schweif * SS), (cx + w * 0.6, cy + schweif * SS), (cx + w, cy),
         (cx + w * 0.3, cy - laenge * SS), (cx - w * 0.3, cy - laenge * SS), (cx - w, cy)],
        fill=farbe,
    )
    if ring_bei:
        ry = cy - ring_bei * SS
        for rad, f in ((ring, farbe), (ring * 0.55, (0, 0, 0, 0))):
            q = rad * SS
            d.ellipse((cx - q, ry - q, cx + q, ry + q), fill=f)
    if schweif >= 20:
        q = 5.5 * SS
        ry = cy + schweif * SS
        d.ellipse((cx - q, ry - q, cx + q, ry + q), fill=farbe)
    if schein:
        bild = glimmen(bild, 1.8, staerke, schein)
    return klein(bild), (halb, py)


def nabe(aod=False):
    g = 34
    bild = Image.new("RGBA", (g * SS, g * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(bild)
    m = g * SS / 2
    for rad, f in ((12, (150, 215, 255, 255)), (8, (235, 245, 255, 255))):
        q = rad * SS
        d.ellipse((m - q, m - q, m + q, m + q), fill=f)
    if not aod:
        bild = glimmen(bild, 2.2, 1.3)
    return klein(bild)


def ziffer(n):
    schrift = ImageFont.truetype(SCHRIFT_TAG, 26 * SS)
    bild = Image.new("RGBA", (17 * SS, 28 * SS), (0, 0, 0, 0))
    ImageDraw.Draw(bild).text((8.5 * SS, 14 * SS), str(n), font=schrift, fill=(255, 255, 255, 255), anchor="mm")
    return klein(glimmen(bild, 1.2, 0.8))


def png(bild):
    puffer = io.BytesIO()
    bild.save(puffer, "PNG", optimize=True)
    return puffer.getvalue()


def zeichnen(hg, teile, stunde, minute, sekunde, tag):
    """Setzt das Bild so zusammen, wie die Uhr es aus der Konfiguration tut."""
    bild = hg.copy()
    if tag is not None:
        bild.alpha_composite(teile["ziffern"][tag // 10], (TAG_X + MITTE - 17, 219))
        bild.alpha_composite(teile["ziffern"][tag % 10], (TAG_X + MITTE, 219))
    sek = sekunde or 0
    paare = [(teile["h"], (stunde % 12 + minute / 60) * 30), (teile["m"], (minute + sek / 60) * 6)]
    if sekunde is not None:
        paare.append((teile["s"], sekunde * 6))
    for (z, (px, py)), w in paare:
        leinwand = Image.new("RGBA", (GROESSE, GROESSE), (0, 0, 0, 0))
        leinwand.alpha_composite(z, (MITTE - px, MITTE - py))
        bild.alpha_composite(leinwand.rotate(-w, Image.BICUBIC, center=(MITTE, MITTE)))
    n = teile["nabe"]
    bild.alpha_composite(n, (MITTE - n.width // 2, MITTE - n.height // 2))
    return bild


def jpg(bild, g):
    puffer = io.BytesIO()
    bild.convert("RGB").resize((g, g), Image.LANCZOS).save(puffer, "JPEG", quality=92)
    return puffer.getvalue()


def rundmaske(bild):
    maske = Image.new("L", (GROESSE * 4, GROESSE * 4), 0)
    ImageDraw.Draw(maske).ellipse((0, 0, GROESSE * 4, GROESSE * 4), fill=255)
    aus = Image.new("RGBA", bild.size, (0, 0, 0, 255))
    aus.paste(bild, (0, 0), maske.resize(bild.size, Image.LANCZOS))
    return aus


def hand_ebene(i, vt, amb, res, pivot):
    px, py = pivot
    seite = 2 * py
    o = MITTE - py
    return (
        f'<layer index="{i}" draw_type="hand_res" value_type="{vt}" ambient_mode="{amb}" res_left="A100_{res:03d}.png" '
        f'rotate_start_angel="0" rotate_end_angel="360" rotate_point_hand="{px},{py}" rotate_rect="{o},{o},{seite},{seite}"/>'
    )


def main():
    hg, aod = hintergrund(), hintergrund_aod()
    h, hp = zeiger(108, 14, 8, ring_bei=100, ring=9)
    m, mp = zeiger(178, 14, 7, ring_bei=147, ring=6)
    s, sp = zeiger(212, 34, 2.2, farbe=(255, 170, 192, 255), schein=(255, 60, 105), staerke=0.8)
    h_a, hap = zeiger(108, 14, 8, ring_bei=100, ring=9, farbe=(190, 215, 240, 255), schein=None)
    m_a, map_ = zeiger(178, 14, 7, ring_bei=147, ring=6, farbe=(190, 215, 240, 255), schein=None)
    na, na_a = nabe(), nabe(aod=True)
    ziffern = [ziffer(i) for i in range(10)]
    teile = {"h": (h, hp), "m": (m, mp), "s": (s, sp), "nabe": na, "ziffern": ziffern}
    teile_a = {"h": (h_a, hap), "m": (m_a, map_), "nabe": na_a, "ziffern": ziffern}

    cover = rundmaske(zeichnen(hg, teile, 10, 8, 36, 30))
    aod_bild = rundmaske(zeichnen(aod, teile_a, 10, 8, None, None))

    benannt = [
        ("vorschau", cover.convert("RGB").resize((250, 250), Image.LANCZOS)),
        ("hg", hg.convert("RGB")),
        ("aod", aod.convert("RGB")),
        *((f"ziffer{i}", z) for i, z in enumerate(ziffern)),
        ("h", h), ("m", m), ("s", s), ("nabe", na),
        ("h_a", h_a), ("m_a", m_a), ("nabe_a", na_a),
    ]
    bilder = [b for _, b in benannt]
    nr = {name: i + 1 for i, (name, _) in enumerate(benannt)}
    res = lambda name: f"A100_{nr[name]:03d}.png"
    einer = ",".join(res(f"ziffer{i}") for i in range(10))
    zehner = ",".join(res(f"ziffer{i}") for i in range(4))
    config = f'''<?xml version="1.0" encoding="utf-8"?>
<watchface>
    <element label="background" is_support_option="false" res_preview="">
        <layer index="1" draw_type="single_res" ambient_mode="0" res_name="{res("hg")}" res_position="0,0"/>
        <layer index="2" draw_type="single_res" ambient_mode="1" res_name="{res("aod")}" res_position="0,0"/>
    </element>
    <element label="date" is_support_option="false" res_preview="">
        <layer index="1" draw_type="selected_res" value_type="70" ambient_mode="0" res_name="{zehner}" res_position="{TAG_X + MITTE - 17},219"/>
        <layer index="2" draw_type="selected_res" value_type="71" ambient_mode="0" res_name="{einer}" res_position="{TAG_X + MITTE},219"/>
    </element>
    <element label="time" is_support_option="false" res_preview="">
        {hand_ebene(1, 150, 0, nr["h"], hp)}
        {hand_ebene(2, 153, 0, nr["m"], mp)}
        {hand_ebene(3, 154, 0, nr["s"], sp)}
        <layer index="4" draw_type="single_res" ambient_mode="0" res_name="{res("nabe")}" res_position="{MITTE - na.width // 2},{MITTE - na.height // 2}"/>
        {hand_ebene(5, 150, 1, nr["h_a"], hap)}
        {hand_ebene(6, 153, 1, nr["m_a"], map_)}
        <layer index="7" draw_type="single_res" ambient_mode="1" res_name="{res("nabe_a")}" res_position="{MITTE - na.width // 2},{MITTE - na.height // 2}"/>
    </element>
</watchface>
'''.encode()
    description = f'''<?xml version="1.0" encoding="UTF-8"?>
  <HwTheme>
        <title>{TITEL}</title>
        <title-cn>{TITEL}</title-cn>
        <author>Frank</author>
        <designer>Frank</designer>
        <screen>HWHD09</screen>
        <version>2.0.2</version>
        <font>Default</font>
        <font-cn>默认</font-cn>
        <briefinfo>{{"de_DE":"Watch Phase für die Watch 3"}}</briefinfo>
  </HwTheme>
'''.encode()
    nullen = "0" * 32
    gesetzt = set(WERT_BIT.values())
    wertarten = ["".join("1" if w * 32 + i in gesetzt else "0" for i in range(32)) for w in range(18)] + ["0000"]
    ability = f'''<?xml version="1.0" encoding="UTF-8"?>
        <attribute>
            <dataType>{nullen},{nullen},{"0" * 22}</dataType>
            <valueType>{",".join(wertarten)}</valueType>
            <xmlType>10001101000000000000000000000000,00</xmlType>
        </attribute>
'''.encode()
    vorschau = {"cover.jpg": jpg(cover, 960), "aod.jpg": jpg(aod_bild, 960), "icon_small.jpg": jpg(cover, 390)}
    datei = hwt.hwt_bauen(config, [png(b) for b in bilder], description, ability, vorschau, TITEL, TITEL)

    dist = os.path.join(ORDNER, "dist")
    os.makedirs(dist, exist_ok=True)
    with open(os.path.join(dist, "WatchPhase.hwt"), "wb") as f:
        f.write(datei)
    cover.save(os.path.join(dist, "vorschau.png"))
    rundmaske(zeichnen(hg, teile, 12, 3, 23, 1)).save(os.path.join(dist, "vergleich-foto.png"))
    aod_bild.save(os.path.join(dist, "vorschau-aod.png"))
    print(os.path.join(dist, "WatchPhase.hwt"), len(datei), "Bytes")


if __name__ == "__main__":
    sys.exit(main())
