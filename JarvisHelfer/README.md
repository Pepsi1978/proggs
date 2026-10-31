# Eigene Tools von Jarvis: Bauvorschrift für Helfer

Hier liegt der Quellstand aller Helfer, die Jarvis bauen lässt: `JarvisHelfer/<kennung>/`. Maßgeblich zur Laufzeit ist der Katalog auf dem Gerät
(Android: `files/helfer/`, Mac: `Application Support/JarvisMac/Helfer/`). Ein Helfer wird erst durch `helfer_registrieren` installiert, nie durch den Commit.

## Paket

```
JarvisHelfer/<kennung>/helfer.json     Steckbrief
JarvisHelfer/<kennung>/helfer.js       Handy-Skript (art=skript, ort=handy)
JarvisHelfer/<kennung>/main.py|main.sh Mac-Programm (art=skript, ort=mac)
```

`helfer.json`:

```json
{
  "kennung": "docx-schreiber",
  "name": "Word-Dateien schreiben",
  "zweck": "Ein Satz: wofür der Helfer da ist.",
  "faehigkeiten": ["DOCX aus Text erzeugen"],
  "schlagworte": ["word", "docx", "dokument"],
  "version": "1.0.0",
  "ort": "handy",
  "art": "skript",
  "datei": "helfer.js",
  "rolle": "",
  "eingabe": {"type": "object", "properties": {"titel": {"type": "string"}}, "required": ["titel"]},
  "ausgabe": "Ein Satz: was zurückkommt.",
  "voraussetzungen": [],
  "grenzen": ["Keine Bilder, keine Tabellen."],
  "braucht_app_update": false,
  "rechte": {
    "netz": ["api.example.com"],
    "konten": [{"id": "beispiel", "hosts": ["api.example.com"], "art": "bearer"}],
    "werkzeuge": ["kalender_lesen"],
    "extern": false
  },
  "tests": [
    {"name": "Normalfall", "eingabe": {"titel": "A"}, "erwartet": {"ok": true, "enthaelt": ["A"]}},
    {"name": "Fehlerfall", "eingabe": {}, "erwartet": {"ok": false}}
  ]
}
```

- `kennung`: klein, `a-z0-9-`, höchstens 40 Zeichen. `ort`: `handy` oder `mac`. `art`: `skript` oder `agent` (dann `rolle` statt `datei`, läuft mit dem Modell von Jarvis).
- `rechte.konten.art`: `bearer`, `kopf:<Name>` oder `query:<Name>`. `rechte.extern`: `true`, wenn der Helfer nach außen wirkt (senden, kaufen, veröffentlichen, löschen).
- Mindestens ein Normal- und ein Fehlerfall in `tests`. Die Tests laufen bei der Registrierung auf dem Zielgerät, sie dürfen nichts Externes auslösen und dürfen keinen Schlüssel brauchen.

## Rechte und Freigaben

Ohne Freigabe läuft nur, wer höchstens eigene Dateien schreibt, erklärte Hosts ohne Konto aufruft und nur lesende Werkzeuge nutzt.
Konten, schreibende Werkzeuge und `extern=true` brauchen Franks einmalige Freigabe in „Eigene Tools“, nur in der Oberfläche; ein Modell kann sie nicht erteilen.
Mehr Rechte in einer neuen Fassung bedeuten erneute Freigabe. Die Werkzeuge `programmieren`, `helfer_*`, `agent_starten`, `mail_senden`, Löschwerkzeuge und alles zu Einstellungen sind für Helfer gesperrt.

## Handy-Skript

ES5 plus `let/const`, Pfeilfunktionen und Vorlagentexte (Rhino, nur Interpreter). Kein `async`, keine Java-Klassen, kein Dateisystem außer `jarvis`.

```js
function ausfuehren(eingabe) {
  // Fehler: throw new Error("Grund für Frank")
  return { text: "Was Jarvis Frank sagen kann", daten: {}, dateien: [{ name: "a.docx", pfad: pfad }] };
}
```

- `jarvis.http({url, methode, kopf, rumpf, konto})` → `{code, text}`. Nur https und nur Hosts aus `rechte.netz` oder dem genannten Konto. `konto` ist die Kennung; der Schlüssel wird außerhalb des Skripts eingesetzt und in der Antwort geschwärzt.
- `jarvis.werkzeug(name, argumente)` → `{text, fehler}`. Nur aus `rechte.werkzeuge`.
- `jarvis.speicherLesen(schluessel)`, `jarvis.speicherSchreiben(schluessel, wert)`: eigener Speicher des Helfers, bleibt über Fassungen erhalten.
- `jarvis.textDatei(name, text)` → `pfad`; `jarvis.zipDatei(name, {"pfad/in/zip": "text"})` → `pfad` (DOCX und XLSX sind solche Archive).
- `jarvis.log(text)`.

Grenzen: 60 s Laufzeit, keine Java-Brücke, 20 MB je Datei, 64 MB Archivinhalt vor dem Packen, 2.000 Aufrufebenen, 4 MB je HTTP-Antwort. Höchstens zwei Läufe gleichzeitig, mit niedriger Priorität.

Prüfung: alle Tests zusammen höchstens 2 Minuten, nach einem Zeitüberschreiten werden die übrigen übersprungen. Prüfläufe schreiben in einen eigenen Ordner. `enthaelt` vergleicht auch gegen `daten` als JSON-Text, und Android schreibt dort `/` als `\/`: Erwartungen mit Schrägstrich gehören in `text`.

## Mac-Programm

`main.py` (python3) oder `main.sh` (zsh). JSON-Eingabe auf stdin, Arbeitsordner frei beschreibbar, `HELFER_AUSGABE` ist der Ausgabeordner, `HELFER_SPEICHER` der dauerhafte Ordner.
Auf stdout genau ein JSON: `{"text": "...", "daten": {}, "dateien": [{"name": "a.docx", "pfad": "<in HELFER_AUSGABE>"}]}`. Fehler: Exit-Code ungleich 0, Grund auf stderr.
Freigegebene Konten stehen in `KONTO_<KENNUNG>` (Großbuchstaben, `-` wird `_`). Grenze 10 Minuten. Mac-Programme laufen unter Franks Benutzer und sind nicht abgeschottet: Das ist die ehrliche Grenze, das Review ist hier der Schutz.

## Registrierung durch die Programmier-Sitzung

Werkzeug `helfer_registrieren` (MCP-Server Jarvis):
`bau_schluessel`, `auftrag_id`, `manifest` (Inhalt von `helfer.json` als Text), `dateien` (Objekt Dateiname → Text), `review` (`{"befunde": n, "kritisch_offen": 0, "zusammenfassung": "..."}`).

Jarvis führt die Tests selbst auf dem Zielgerät aus. Bestanden: Zustand `bereit` (oder `wartet_auf_freigabe`). Durchgefallen: Der Helfer bleibt gesperrt, die letzte geprüfte Fassung gilt weiter.
Ist das Handy aus und der Mac das Ziel: Paket nach `~/Library/Application Support/JarvisMac/Helfer/eingang/<kennung>/` legen, dazu `registrierung.json` mit `bau_schluessel` und `review`.

## Kanal Handy und Mac (Relay `/kanal`, ohne Modell)

`text` ist jeweils JSON; Kennungen beginnen mit `helfer-`. Dieselbe Kennung wird nie zweimal ausgeführt.

| art | Richtung | text | Antwort (`art=antwort`, `antwort_auf`) |
|---|---|---|---|
| `helfer_liste` | Handy → Mac | `{}` | `{"helfer":[Katalogeintrag]}` |
| `helfer_katalog` | Mac → Handy | `{"helfer":[Katalogeintrag]}` | keine, wird bei Start und Änderung gesendet (nur mit `art` ungleich `info`, siehe unten) |
| `helfer_aufruf` | Handy → Mac | `{"helfer":"kennung","eingabe":{}}` | `{"ok":true,"text":"","daten":{},"dateien":[{"name":"","base64":""}],"dauer_ms":0}` oder `{"ok":false,"fehler":""}` |
| `helfer_installieren` | Handy → Mac | `{"manifest":{},"dateien":{},"review":{},"auftrag_id":""}` | `{"ok":true,"zustand":"bereit","pruefung":{"ergebnis":"","zeit":0}}` oder `{"ok":false,"fehler":""}` |
| `helfer_verwalten` | Handy → Mac | `{"helfer":"kennung","aktion":"aktivieren\|deaktivieren\|papierkorb\|wiederherstellen\|rueckrollen"}` | `{"ok":true,"text":""}` |
| `cli_recherche` | Handy → Mac | `{"frage":"","anzahl":7,"modell":"sonnet"}` | `{"ok":true,"text":""}` oder `{"ok":false,"fehler":""}` |

Katalogeintrag: `kennung, name, zweck, faehigkeiten, schlagworte, version, ort, art, eingabe, ausgabe, voraussetzungen, grenzen, braucht_app_update, rechte, zustand, aktiv, pruefung {zeit, ergebnis}, position`.
Zustände: `in_pruefung`, `bereit`, `wartet_auf_freigabe`, `fehlerhaft`, `papierkorb`. `aktiv=false` heißt deaktiviert. „Mac gerade aus“ ist kein Zustand, sondern die Erreichbarkeit, die das Handy getrennt anzeigt.
