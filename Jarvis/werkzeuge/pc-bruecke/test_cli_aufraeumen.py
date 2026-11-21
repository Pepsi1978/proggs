"""Prüft die Entscheidung des CLI-Aufräumens (jarvis_cli_aufraeumen.bewerte) und die Planung: python3 test_cli_aufraeumen.py"""
import time
import unittest

import jarvis_cli_aufraeumen as a

JETZT = 1_800_000_000.0
START = "Sat Nov 21 08:12:00 2026"


def sitzung(**neu):
    s = {"cli": "claude", "start": START, "register": {"kind": "interactive", "procStart": START, "status": "idle"},
         "werkzeug_laeuft": False, "cpu_delta": 0.0, "frage_offen": False, "zug": "fertig", "letzte": JETZT - 2 * 3600}
    for k, v in neu.items():
        if k == "status":
            s["register"] = {**s["register"], "status": v}
        else:
            s[k] = v
    return s


class Bewertung(unittest.TestCase):
    def test_beispiel_zehn_sitzungen(self):
        # 4 aktiv, 2 seit 40 Minuten ungenutzt, 4 seit mehr als einer Stunde inaktiv: 6 bleiben offen.
        alle = [sitzung(status="busy", letzte=JETZT - 60) for _ in range(4)] + \
               [sitzung(letzte=JETZT - 40 * 60) for _ in range(2)] + [sitzung(letzte=JETZT - (2 + i) * 3600) for i in range(4)]
        zustaende = [a.bewerte(s, JETZT)[0] for s in alle]
        self.assertEqual(zustaende.count("inaktiv"), 4)
        self.assertEqual(len(zustaende) - zustaende.count("inaktiv"), 6)

    def test_grenze_eine_stunde(self):
        self.assertEqual(a.bewerte(sitzung(letzte=JETZT - 59 * 60), JETZT)[0], "kuerzlich")
        self.assertEqual(a.bewerte(sitzung(letzte=JETZT - 3600), JETZT)[0], "kuerzlich")
        self.assertEqual(a.bewerte(sitzung(letzte=JETZT - 61 * 60), JETZT)[0], "inaktiv")

    def test_unklar_bleibt_offen(self):
        for s in (sitzung(cli="codex"), sitzung(register=None), sitzung(status="was-neues"), sitzung(status=None),
                  sitzung(start="Fri Nov 20 01:00:00 2026"), sitzung(zug="unklar"), sitzung(letzte=None), sitzung(cpu_delta=None)):
            self.assertNotEqual(a.bewerte(s, JETZT)[0], "inaktiv", s)

    def test_aktiv_und_wartend_bleibt_offen(self):
        for s in (sitzung(werkzeug_laeuft=True), sitzung(cpu_delta=3.0), sitzung(frage_offen=True), sitzung(zug="offen"),
                  sitzung(auftrag="WA99R", auftrag_fertig=False), sitzung(status="waiting")):
            self.assertNotEqual(a.bewerte(s, JETZT)[0], "inaktiv", s)
        self.assertEqual(a.bewerte(sitzung(auftrag="WA99R", auftrag_fertig=True), JETZT)[0], "inaktiv")


class Transkript(unittest.TestCase):
    def schreib(self, namen_und_eingaben):
        import json, tempfile, pathlib
        d = tempfile.mkdtemp()
        p = pathlib.Path(d) / "t.jsonl"
        p.write_text("\n".join(json.dumps({"type": "assistant", "message": {"content": [{"type": "tool_use", "name": n, "input": e}]}})
                               for n, e in namen_und_eingaben) + "\n")
        return p

    def test_cron_und_weckruf(self):
        self.assertTrue(a.plant_weiter(self.schreib([("CronCreate", {}), ("ScheduleWakeup", {"stop": True})])))
        self.assertFalse(a.plant_weiter(self.schreib([("CronCreate", {}), ("CronDelete", {})])))
        self.assertTrue(a.plant_weiter(self.schreib([("ScheduleWakeup", {"delaySeconds": 600})])))
        self.assertFalse(a.plant_weiter(self.schreib([("ScheduleWakeup", {"delaySeconds": 600}), ("ScheduleWakeup", {"stop": True})])))
        self.assertTrue(a.jarvis_abgeschlossen(self.schreib([("mcp__jarvis__programmier_fortschritt", {"stand": "fertig"})])))
        self.assertFalse(a.jarvis_abgeschlossen(self.schreib([("mcp__jarvis__programmier_fortschritt", {"stand": "laeuft"})])))


class Planung(unittest.TestCase):
    def test_aus_heisst_kein_lauf(self):
        self.assertFalse(a.faellig({"automatik": False, "naechster": JETZT - 10}, JETZT))
        self.assertFalse(a.faellig({"automatik": False, "naechster": None}, JETZT))
        self.assertTrue(a.faellig({"automatik": True, "naechster": JETZT - 10}, JETZT))
        self.assertFalse(a.faellig({"automatik": True, "naechster": JETZT + 10}, JETZT))

    def test_intervall_nur_1_bis_24(self):
        alt = a.EINSTELLUNG
        import tempfile, pathlib
        with tempfile.TemporaryDirectory() as d:
            a.EINSTELLUNG = pathlib.Path(d) / "e.json"
            try:
                for falsch in (0, 25, 1.5, True):
                    with self.assertRaises(ValueError):
                        a.setze_einstellung(None, falsch)
                e = a.setze_einstellung(True, 24)
                self.assertTrue(e["automatik"])
                self.assertAlmostEqual(e["naechster"], time.time() + 24 * 3600, delta=5)
                self.assertIsNone(a.setze_einstellung(False, None)["naechster"])
                self.assertEqual(a.lies_einstellung()["stunden"], 24)
            finally:
                a.EINSTELLUNG = alt


if __name__ == "__main__":
    unittest.main()
