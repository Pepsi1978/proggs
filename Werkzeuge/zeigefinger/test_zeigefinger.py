from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from zeigefinger import element_an_punkt, elementkette_an_punkt
from zeigefinger_overlay import befehlstext, geraetepunkt, videobereich


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

class UebergabeTest(unittest.TestCase):
    def test_befehl_enthaelt_eindeutigen_absoluten_pfad(self) -> None:
        with tempfile.TemporaryDirectory() as ordner:
            pfad = (Path(ordner) / "auswahl-1.json").resolve()
            text = befehlstext(pfad)
        self.assertIn(str(pfad), text)
        self.assertIn('"dort"', text)


if __name__ == "__main__":
    unittest.main()
