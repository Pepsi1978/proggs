"""Erzeugt das Symbol fuer den Handy-Abgleich (handy-abgleich.ico + Vorschau-PNG).

Gleiche Machart wie icon-erzeugen.py - vierfach gezeichnet und heruntergerechnet,
damit auch die 16-Pixel-Fassung in der Taskleiste sauber bleibt.

Motiv: ein Handy mit einer Note darauf, davor ein Kreisabzeichen mit dem
Abgleich-Pfeil. Die Farben sind bewusst andere als beim Downloader (Tuerkis statt
Magenta), damit man die beiden Symbole auf dem Desktop nicht verwechselt.
"""

import os
from PIL import Image, ImageDraw, ImageFilter

S = 2048                      # Zeichenflaeche
ECKE = 460                    # Eckenradius
FARBE_A = (0, 208, 190)       # Tuerkis
FARBE_B = (18, 92, 214)       # Blau
WEISS = (255, 255, 255, 255)
AKZENT = (14, 120, 190, 255)

HIER = os.path.dirname(os.path.abspath(__file__))
ZIEL = os.path.join(HIER, "handy-abgleich.ico")
VORSCHAU = os.path.join(HIER, "handy-abgleich.png")


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
    """Handy mit Note, dazu das Abgleich-Abzeichen unten rechts."""
    ebene = Image.new("RGBA", (groesse, groesse), (0, 0, 0, 0))
    z = ImageDraw.Draw(ebene)

    # Handy-Umriss: weisser Rahmen, innen der Hintergrund durchscheinend
    links, oben, rechts, unten = 620, 360, 1330, 1700
    z.rounded_rectangle([links, oben, rechts, unten], radius=120, fill=WEISS)
    z.rounded_rectangle([links + 70, oben + 130, rechts - 70, unten - 130], radius=54, fill=(0, 0, 0, 0))

    # Hoerermuschel oben und Knopf unten - macht es auch klein als Handy erkennbar
    z.rounded_rectangle([(links + rechts) // 2 - 95, oben + 52, (links + rechts) // 2 + 95, oben + 92],
                        radius=20, fill=(255, 255, 255, 0))
    z.ellipse([(links + rechts) // 2 - 42, unten - 108, (links + rechts) // 2 + 42, unten - 24],
              fill=(255, 255, 255, 0))

    # Achtelnote auf dem Bildschirm
    note = Image.new("RGBA", (groesse, groesse), (0, 0, 0, 0))
    zn = ImageDraw.Draw(note)
    zn.rounded_rectangle([975, 640, 1055, 1210], radius=40, fill=WEISS)
    zn.polygon([(1045, 648), (1225, 782), (1245, 980), (1168, 892), (1045, 826)], fill=WEISS)
    note = Image.alpha_composite(note, gedrehte_ellipse(groesse, (860, 1205), 210, 162, -20, WEISS))
    ebene = Image.alpha_composite(ebene, note)

    # Abzeichen unten rechts: Kreis mit zwei gegenlaeufigen Pfeilen (Abgleich)
    z = ImageDraw.Draw(ebene)
    mx, my, r = 1470, 1470, 385
    z.ellipse([mx - r, my - r, mx + r, my + r], fill=WEISS)

    # oberer Pfeil nach rechts
    z.rounded_rectangle([mx - 205, my - 148, mx + 95, my - 58], radius=45, fill=AKZENT)
    z.polygon([(mx + 60, my - 232), (mx + 235, my - 103), (mx + 60, my + 26)], fill=AKZENT)
    # unterer Pfeil nach links
    z.rounded_rectangle([mx - 95, my + 58, mx + 205, my + 148], radius=45, fill=AKZENT)
    z.polygon([(mx - 60, my - 26), (mx - 235, my + 103), (mx - 60, my + 232)], fill=AKZENT)

    return ebene


def main():
    maske = Image.new("L", (S, S), 0)
    ImageDraw.Draw(maske).rounded_rectangle([0, 0, S - 1, S - 1], radius=ECKE, fill=255)

    hintergrund = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hintergrund.paste(verlauf(S, FARBE_A, FARBE_B), (0, 0), maske)

    glanz = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glanz).ellipse([-500, -900, 1500, 700], fill=(255, 255, 255, 38))
    glanz = glanz.filter(ImageFilter.GaussianBlur(120))
    leer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hintergrund = Image.alpha_composite(hintergrund, Image.composite(glanz, leer, maske))

    motiv = zeichne_motiv(S)

    schatten = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    schatten.paste((0, 0, 0, 90), (0, 26), motiv.split()[3])
    schatten = schatten.filter(ImageFilter.GaussianBlur(28))

    bild = Image.alpha_composite(hintergrund, Image.composite(schatten, leer, maske))
    bild = Image.alpha_composite(bild, motiv)

    gross = bild.resize((1024, 1024), Image.LANCZOS)
    gross.save(VORSCHAU, "PNG")

    groessen = [256, 128, 96, 64, 48, 40, 32, 24, 20, 16]
    gross.save(ZIEL, format="ICO", sizes=[(g, g) for g in groessen])

    print("Symbol geschrieben:", ZIEL, os.path.getsize(ZIEL), "Byte")
    print("Vorschau:", VORSCHAU)


if __name__ == "__main__":
    main()
