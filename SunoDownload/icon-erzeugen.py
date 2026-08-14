"""Erzeugt das Programm-Symbol (suno-backup.ico + Vorschau-PNG).

Gezeichnet wird in vierfacher Groesse und danach herunterskaliert - dadurch sind
alle Kanten glatt, auch in den kleinen Symbolgroessen der Taskleiste.
"""

import io
import os
from PIL import Image, ImageDraw, ImageFilter

S = 2048                      # Zeichenflaeche
ECKE = 460                    # Eckenradius
FARBE_A = (255, 46, 136)      # Magenta
FARBE_B = (124, 42, 232)      # Violett
WEISS = (255, 255, 255, 255)

ZIEL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "suno-backup.ico")
VORSCHAU = os.path.join(os.path.dirname(os.path.abspath(__file__)), "suno-backup.png")


def verlauf(groesse, oben_links, unten_rechts):
    """Diagonaler Farbverlauf."""
    bild = Image.new("RGB", (groesse, groesse))
    zeichner = ImageDraw.Draw(bild)
    for i in range(groesse * 2):
        t = i / (groesse * 2 - 1)
        farbe = tuple(int(oben_links[k] + (unten_rechts[k] - oben_links[k]) * t) for k in range(3))
        zeichner.line([(i, 0), (0, i)], fill=farbe, width=2)
    return bild


def gedrehte_ellipse(groesse, mitte, rx, ry, winkel, farbe):
    """Ellipse mit Drehung - fuer den Notenkopf."""
    ebene = Image.new("RGBA", (groesse, groesse), (0, 0, 0, 0))
    ImageDraw.Draw(ebene).ellipse(
        [mitte[0] - rx, mitte[1] - ry, mitte[0] + rx, mitte[1] + ry], fill=farbe
    )
    return ebene.rotate(winkel, center=mitte, resample=Image.BICUBIC)


def zeichne_motiv(groesse):
    """Achtelnote plus Download-Abzeichen, als eigene Ebene."""
    ebene = Image.new("RGBA", (groesse, groesse), (0, 0, 0, 0))
    z = ImageDraw.Draw(ebene)

    # Notenhals
    z.rounded_rectangle([950, 430, 1065, 1210], radius=57, fill=WEISS)

    # Notenfahne
    z.polygon(
        [(1050, 440), (1330, 640), (1360, 930), (1245, 800), (1050, 700)],
        fill=WEISS,
    )

    # Notenkopf
    ebene = Image.alpha_composite(ebene, gedrehte_ellipse(groesse, (760, 1200), 300, 232, -20, WEISS))
    z = ImageDraw.Draw(ebene)

    # Abzeichen: weisser Kreis mit Pfeil nach unten
    mx, my, r = 1455, 1455, 365
    z.ellipse([mx - r, my - r, mx + r, my + r], fill=WEISS)

    pfeil = (255, 46, 136, 255)
    z.rounded_rectangle([mx - 52, my - 265, mx + 52, my + 20], radius=40, fill=pfeil)
    z.polygon([(mx - 172, my - 30), (mx + 172, my - 30), (mx, my + 170)], fill=pfeil)
    z.rounded_rectangle([mx - 150, my + 228, mx + 150, my + 288], radius=30, fill=pfeil)

    return ebene


def main():
    # Hintergrund: abgerundetes Quadrat mit Verlauf
    maske = Image.new("L", (S, S), 0)
    ImageDraw.Draw(maske).rounded_rectangle([0, 0, S - 1, S - 1], radius=ECKE, fill=255)

    hintergrund = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hintergrund.paste(verlauf(S, FARBE_A, FARBE_B), (0, 0), maske)

    # Sanfter Glanz oben links
    glanz = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glanz).ellipse([-500, -900, 1500, 700], fill=(255, 255, 255, 38))
    glanz = glanz.filter(ImageFilter.GaussianBlur(120))
    hintergrund = Image.alpha_composite(hintergrund, Image.composite(glanz, Image.new("RGBA", (S, S), (0, 0, 0, 0)), maske))

    motiv = zeichne_motiv(S)

    # Weicher Schatten unter dem Motiv
    schatten = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    schatten.paste((0, 0, 0, 90), (0, 26), motiv.split()[3])
    schatten = schatten.filter(ImageFilter.GaussianBlur(28))

    bild = Image.alpha_composite(hintergrund, Image.composite(schatten, Image.new("RGBA", (S, S), (0, 0, 0, 0)), maske))
    bild = Image.alpha_composite(bild, motiv)

    # Herunterskalieren und als Symbol speichern
    gross = bild.resize((1024, 1024), Image.LANCZOS)
    gross.save(VORSCHAU, "PNG")

    groessen = [256, 128, 96, 64, 48, 40, 32, 24, 20, 16]
    gross.save(ZIEL, format="ICO", sizes=[(g, g) for g in groessen])

    print("Symbol geschrieben:", ZIEL, os.path.getsize(ZIEL), "Byte")
    print("Vorschau:", VORSCHAU)


if __name__ == "__main__":
    main()
