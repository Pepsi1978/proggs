"""Der Auftragstext kommt unverändert in der ersten Nachricht an: keine zusätzlichen Leerzeilen, keine verlorenen.
Aufruf: python3 -m unittest test_auftragstext (im Ordner pc-bruecke)."""
import unittest

from jarvis_pc_bruecke import auftragstext

ABSAETZE = "Erster Absatz,\nzweite Zeile.\n\nZweiter Absatz."
LISTE = "Anforderungen:\n- Punkt eins\n- Punkt zwei\n1. Schritt\n2. Schritt"
CODE = "Beispiel:\n```kotlin\nfor (;;) {\n\n    tu()\n}\n```\nDanach weiter."
TRENNER = "Aufgabe eins ; Aufgabe zwei\n\n;\n\nAufgabe drei"


def teil(text: str) -> str:
    """Der Abschnitt zwischen „AUFTRAG:“ und „VORGEHEN“, ohne den festen Absatz davor."""
    return text.split("AUFTRAG:\n", 1)[1].split("\n\nVORGEHEN\n", 1)[0]


class AuftragstextTest(unittest.TestCase):
    def test_metadaten_kompakt(self):
        text = auftragstext({"app": "Jarvis", "review": True, "auftrag": "Kurz."})
        self.assertIn("\n\nAPP: Jarvis\nAUFTRAG:\nKurz.\n\nVORGEHEN\n1. ", text)
        self.assertNotIn("\n\n\n", text)
        self.assertNotIn("\r", text)

    def test_auftrag_zeichengenau(self):
        for auftrag in (ABSAETZE, LISTE, CODE, TRENNER, "Zeile eins\nZeile zwei"):
            with self.subTest(auftrag=auftrag[:20]):
                self.assertEqual(auftrag, teil(auftragstext({"app": "Jarvis", "auftrag": auftrag})))

    def test_crlf_wird_lf_ohne_neue_leerzeilen(self):
        for auftrag in ("Zeile eins\r\nZeile zwei\r\n\r\nAbsatz", "Zeile eins\rZeile zwei\r\rAbsatz"):
            with self.subTest(auftrag=repr(auftrag)):
                text = auftragstext({"app": "Jarvis", "auftrag": auftrag})
                self.assertEqual("Zeile eins\nZeile zwei\n\nAbsatz", teil(text))
                self.assertNotIn("\r", text)

    def test_ohne_review_zeile(self):
        # Claude Code fragt am Ende selbst nach dem Review: Der Kopf nennt es nie, auch nicht mit review=true.
        self.assertNotIn("REVIEW", auftragstext({"app": "X", "review": True, "auftrag": "a"}))

    def test_null_wird_nicht_zu_none(self):
        text = auftragstext({"app": None, "auftrag": None})
        self.assertIn("APP: \n", text)
        self.assertNotIn("None", text)


if __name__ == "__main__":
    unittest.main()
