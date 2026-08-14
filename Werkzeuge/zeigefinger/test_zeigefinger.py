from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from zeigefinger import (
    element_an_punkt,
    elemente_im_bereich,
    elementkette_an_punkt,
    normiere_kasten,
    sortiere_geraete,
)
from zeigefinger_overlay import (
    befehlstext,
    fenstergroesse,
    geraetepunkt,
    ist_kasten,
    videobereich,
)


XML = """<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
  <node text="" clickable="true" class="android.view.View" bounds="[10,20][210,120]">
    <node text="Speichern" clickable="false" class="android.widget.TextView" bounds="[30,40][180,90]" />
  </node>
</hierarchy>
"""


class ElementTest(unittest.TestCase):
    def test_kette_ordnet_inneres_element_vor_rahmen(self) -> None:
        kette = elementkette_an_punkt(XML, 80, 60)
        self.assertEqual([e["text"] for e in kette], ["Speichern", ""])

    def test_element_liefert_text_und_klickbaren_rahmen(self) -> None:
        gefunden = element_an_punkt(XML, 80, 60)
        self.assertIsNotNone(gefunden)
        assert gefunden is not None
        direkt, rahmen = gefunden
        self.assertEqual(direkt["text"], "Speichern")
        self.assertIsNotNone(rahmen)
        assert rahmen is not None
        self.assertTrue(rahmen["klickbar"])


class KoordinatenTest(unittest.TestCase):
    def test_videobereich_entfernt_letterboxing(self) -> None:
        self.assertEqual(videobereich(800, 1000, 400, 800), (150, 0, 500, 1000))

    def test_geraetepunkt_skaliert_in_android_koordinaten(self) -> None:
        self.assertEqual(geraetepunkt(250, 500, 500, 1000, 400, 800), (200, 400))

KASTEN_XML = """<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
  <node text="" class="android.widget.LinearLayout" bounds="[0,0][400,400]">
    <node text="Titel" class="android.widget.TextView" bounds="[20,20][300,60]" />
    <node text="Zeile A" class="android.widget.TextView" bounds="[20,80][300,120]" />
    <node text="Zeile B" class="android.widget.TextView" bounds="[20,140][300,180]" />
    <node text="Weit weg" class="android.widget.TextView" bounds="[20,340][300,380]" />
  </node>
</hierarchy>
"""


class KastenTest(unittest.TestCase):
    def test_kasten_liefert_inhalt_in_leserichtung(self) -> None:
        gefunden = elemente_im_bereich(KASTEN_XML, 10, 10, 320, 200)
        self.assertEqual(
            [e["text"] for e in gefunden if e["text"]],
            ["Titel", "Zeile A", "Zeile B"],
        )

    def test_kasten_laesst_weit_entferntes_aussen_vor(self) -> None:
        texte = [e["text"] for e in elemente_im_bereich(KASTEN_XML, 10, 10, 320, 200)]
        self.assertNotIn("Weit weg", texte)

    def test_teilweise_erfasstes_wird_als_solches_gekennzeichnet(self) -> None:
        gefunden = {e["text"]: e for e in elemente_im_bereich(KASTEN_XML, 10, 10, 320, 200)}
        self.assertTrue(gefunden["Titel"]["vollstaendig_im_kasten"])
        # Der umschliessende Rahmen ragt hinaus, gehoert aber zum Kontext.
        self.assertFalse(gefunden[""]["vollstaendig_im_kasten"])

    def test_wackeln_beim_klicken_bleibt_eine_punktauswahl(self) -> None:
        self.assertFalse(ist_kasten(100, 100, 103, 98))

    def test_flache_textzeile_zaehlt_als_kasten(self) -> None:
        self.assertTrue(ist_kasten(100, 100, 400, 104))

    def test_schmale_hohe_auswahl_zaehlt_als_kasten(self) -> None:
        self.assertTrue(ist_kasten(100, 100, 104, 400))

    def test_ziehrichtung_ist_egal(self) -> None:
        self.assertEqual(normiere_kasten(300, 200, 100, 50), (100, 50, 300, 200))
        rueckwaerts = elemente_im_bereich(KASTEN_XML, 320, 200, 10, 10)
        vorwaerts = elemente_im_bereich(KASTEN_XML, 10, 10, 320, 200)
        self.assertEqual(rueckwaerts, vorwaerts)


class GeraetewahlTest(unittest.TestCase):
    def test_overlay_nimmt_den_emulator_wenn_er_laeuft(self) -> None:
        self.assertEqual(
            sortiere_geraete(["R3GL7073MLM", "emulator-5554"], emulator_zuerst=True)[0],
            "emulator-5554",
        )

    def test_overlay_faellt_auf_das_angeschlossene_geraet_zurueck(self) -> None:
        self.assertEqual(
            sortiere_geraete(["R3GL7073MLM"], emulator_zuerst=True),
            ["R3GL7073MLM"],
        )

    def test_hover_modus_bevorzugt_das_echte_geraet(self) -> None:
        self.assertEqual(
            sortiere_geraete(["emulator-5554", "R3GL7073MLM"], emulator_zuerst=False)[0],
            "R3GL7073MLM",
        )


class FenstermassTest(unittest.TestCase):
    def test_zugeklapptes_fold8_behaelt_das_bisherige_mass(self) -> None:
        # 714 statt der frueher fest verdrahteten 715: massstabsgetreu, ohne 1-Pixel-Rand.
        self.assertEqual(fenstergroesse(1248, 1972), (714, 1129))

    def test_aufgeklapptes_fold8_bleibt_massstabsgetreu(self) -> None:
        breite, hoehe = fenstergroesse(2448, 1848)
        self.assertLessEqual(breite, 1200)
        self.assertLessEqual(hoehe, 1129)
        self.assertAlmostEqual(breite / hoehe, 2448 / 1848, places=2)


class UebergabeTest(unittest.TestCase):
    def test_befehl_enthaelt_eindeutigen_absoluten_pfad(self) -> None:
        with tempfile.TemporaryDirectory() as ordner:
            pfad = (Path(ordner) / "auswahl-1.json").resolve()
            text = befehlstext(pfad)
        self.assertIn(str(pfad), text)
        self.assertIn('"dort"', text)

    def test_kasten_wird_im_befehl_als_bereich_angesagt(self) -> None:
        text = befehlstext(Path("C:/tmp/auswahl-2.json"), art="kasten")
        self.assertIn("Kasten", text)


if __name__ == "__main__":
    unittest.main()
