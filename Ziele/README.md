# Ziele

Android-App für Franks persönliche Ziele (Kotlin, Jetpack Compose, Room). Paket `de.frank.ziele`.
Teil des „Mehrfachgestirns“ mit Geniale Aufgaben (`de.frank.aufgaben`), Genialer Wecker (`de.frank.genialerwecker.app`) und Jarvis (`de.frank.jarvis`).
Abgeleitet aus Geniale Aufgaben: KI (OpenAI über ChatGPT-Konto, Anthropic über API-Schlüssel), Vorlesen (Edge, Google Chirp 3 HD,
Gemini 3.8 Flash/Flash-Lite, Azure, eigene Stimme) und Diktat (Groq Whisper) sind dieselben Bausteine wie dort und in Jarvis.

## Bedienung

- **Ziel anlegen:** Mikrofon unten rechts, frei sprechen („Ich will bis Juni 10 kg abnehmen, weil …“). Die KI ordnet in Titel,
  Beschreibung, Warum, Frist und Symbol. Plus daneben = tippen.
- **Auswerten:** jede Zielkarte und die Detailseite haben einen eigenen Knopf. Der Aufgaben-Agent recherchiert zuerst
  (Websuche des Modells), wie das Ziel am besten erreicht wird, und leitet daraus Aufgaben ab, getrennt in **Notwendig**
  und **Unterstützend**. Er läuft im Prozess weiter, auch wenn man den Bildschirm verlässt. Neu auswerten behält
  übernommene und ausgeblendete Vorschläge und schlägt nichts Doppeltes vor.
- **Plus** an jedem Vorschlag legt ihn in Geniale Aufgaben an (Priorität: notwendig mit Wichtigkeit ≥ 4 = Hoch, sonst Mittel;
  unterstützend = Gering). „Alle“ übernimmt alle notwendigen in Reihenfolge der Voraussetzungen.
  Der Erledigt-Stand kommt beim Öffnen zurück (Haken grün, Fortschrittsring).
- **Sortierung:** Wichtigkeit, Reihenfolge (Voraussetzungen zuerst, topologisch), Aufwand, Hebel (Wirkung je Aufwand), Zeitpunkt (Phase).
- **Kontextdatei** (Notizsymbol oben): einsprechen hängt einen datierten Absatz an, tippen ändert direkt. Geht in jede Auswertung,
  dazu Jarvis' Live-Gedächtnis (Schalter in den Einstellungen).
- **Hell/Dunkel/Automatik** oben rechts, Gold in beiden Modi.
- **Einstellungen → Schlüssel aus Jarvis übernehmen:** holt Groq-, Anthropic- und Stimmen-Schlüssel über die Jarvis-Brücke.

## Brücken

| Richtung | Adresse | Schutz | Methoden |
|---|---|---|---|
| Jarvis → Ziele | `content://de.frank.ziele.jarvis` (`call`) | `de.frank.ziele.permission.JARVIS` (Signatur) | info, lesen, anlegen, aendern, loeschen, auswerten, auswertung_stand, uebernehmen, verwerfen, kontext_lesen, kontext_ergaenzen, kontext_ersetzen, einstellungen |
| Ziele → Geniale Aufgaben | `content://de.frank.aufgaben.jarvis` (`call`) | `de.frank.aufgaben.permission.JARVIS` (Signatur) | anlegen, lesen |
| Ziele → Jarvis | `content://de.frank.jarvis.ziele` (`call`) | `de.frank.jarvis.permission.BRUECKE` (Signatur) | kontext (Live-Gedächtnis kurz), zugang (Schlüssel) |
| Wecker → Ziele | `content://de.frank.ziele.wecker/aktuell` bzw. `/vorlesen` (`query`) | keiner (Wecker hat eigenen Signierschlüssel) | nur lesen |

Anfrage und Antwort bei `call`: Bundle mit `"json"`. Fachliche Fehler kommen als `{"fehler": "…"}`.

## Version

Einzige Quelle: `app/src/main/assets/versionslog.json` (siehe Profilregel 19). Angezeigt unten in den Einstellungen.
