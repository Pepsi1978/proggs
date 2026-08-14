"""Erzeugt das Programm-Symbol fuer den Zeigefinger (zeigefinger.ico + Vorschau).

Gezeichnet wird in vierfacher Groesse und danach herunterskaliert - dadurch sind
alle Kanten glatt, auch in den kleinen Symbolgroessen der Taskleiste.
"""

import os
from PIL import Image, ImageDraw, ImageFilter

S = 2048                      # Zeichenflaeche
ECKE = 460                    # Eckenradius
FARBE_A = (34, 205, 255)      # Cyan
FARBE_B = (59, 79, 224)       # Indigo
WEISS = (255, 255, 255, 255)

HIER = os.path.dirname(os.path.abspath(__file__))
ZIEL = os.path.join(HIER, "zeigefinger.ico")
VORSCHAU = os.path.join(HIER, "zeigefinger.png")


def verlauf(groesse, oben_links, unten_rechts):
    """Diagonaler Farbverlauf."""
    bild = Image.new("RGB", (groesse, groesse))
    zeichner = ImageDraw.Draw(bild)
    for i in range(groesse * 2):
        t = i / (groesse * 2 - 1)
        farbe = tuple(int(oben_links[k] + (unten_rechts[k] - oben_links[k]) * t) for k in range(3))
        zeichner.line([(i, 0), (0, i)], fill=farbe, width=2)
    return bild


def gedrehtes_teil(groesse, kasten, radius, winkel, mitte, farbe):
    """Abgerundetes Rechteck mit Drehung - fuer den Daumen."""
    ebene = Image.new("RGBA", (groesse, groesse), (0, 0, 0, 0))
    ImageDraw.Draw(ebene).rounded_rectangle(kasten, radius=radius, fill=farbe)
    return ebene.rotate(winkel, center=mitte, resample=Image.BICUBIC)


def zeichne_motiv(groesse):
    """Zeigende Hand mit Tipp-Wellen."""
    ebene = Image.new("RGBA", (groesse, groesse), (0, 0, 0, 0))
    z = ImageDraw.Draw(ebene)

    # Tipp-Wellen ueber der Fingerspitze
    spitze = (905, 545)
    for radius, staerke, deckung in ((300, 46, 175), (450, 42, 110)):
        z.arc(
            [spitze[0] - radius, spitze[1] - radius, spitze[0] + radius, spitze[1] + radius],
            start=200, end=340, fill=(255, 255, 255, deckung), width=staerke,
        )

    # Handflaeche
    z.rounded_rectangle([760, 1010, 1395, 1650], radius=200, fill=WEISS)

    # Zeigefinger
    z.rounded_rectangle([815, 640, 1000, 1150], radius=92, fill=WEISS)

    # Die uebrigen Finger, gestaffelt eingeklappt
    z.rounded_rectangle([1015, 900, 1175, 1160], radius=80, fill=WEISS)
    z.rounded_rectangle([1185, 950, 1330, 1180], radius=72, fill=WEISS)
    z.rounded_rectangle([1330, 1010, 1450, 1220], radius=60, fill=WEISS)

    # Daumen, leicht angewinkelt
    daumen = gedrehtes_teil(groesse, [640, 1130, 830, 1400], 95, 22, (735, 1265), WEISS)
    return Image.alpha_composite(ebene, daumen)


def main():
    maske = Image.new("L", (S, S), 0)
    ImageDraw.Draw(maske).rounded_rectangle([0, 0, S - 1, S - 1], radius=ECKE, fill=255)

    leer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hintergrund = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hintergrund.paste(verlauf(S, FARBE_A, FARBE_B), (0, 0), maske)

    # Sanfter Glanz oben links
    glanz = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glanz).ellipse([-500, -900, 1500, 700], fill=(255, 255, 255, 38))
    glanz = glanz.filter(ImageFilter.GaussianBlur(120))
    hintergrund = Image.alpha_composite(hintergrund, Image.composite(glanz, leer, maske))

    motiv = zeichne_motiv(S)

    schatten = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    schatten.paste((0, 0, 0, 90), (0, 26), motiv.split()[3])
    schatten = schatten.filter(ImageFilter.GaussianBlur(28))

    bild = Image.alpha_composite(hintergrund, Image.composite(schatten, leer, maske))
    bild = Image.alpha_composite(bild, motiv)

    gross = bild.resize((1024, 1024), Image.LANCZOS)
    gross.save(VORSCHAU, "PNG")
    gross.save(ZIEL, format="ICO", sizes=[(g, g) for g in (256, 128, 96, 64, 48, 40, 32, 24, 20, 16)])

    print("Symbol geschrieben:", ZIEL, os.path.getsize(ZIEL), "Byte")


if __name__ == "__main__":
    main()
