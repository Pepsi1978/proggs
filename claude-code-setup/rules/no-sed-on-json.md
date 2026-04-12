# JSON-Dateien: Kein sed, kein awk — NUR sichere Methoden (KRITISCH)

## Regel

JSON-Dateien (settings.json, hooks-*.json, .mcp.json, package.json, tsconfig.json,
manifest.json, etc.) duerfen NIEMALS mit `sed`, `awk`, `echo >>`, oder Bash-Textersetzung
bearbeitet werden.

## Erlaubte Methoden

1. **Edit-Tool** (bevorzugt) — zeigt exakten Kontext, macht praezise Ersetzungen
2. **Write-Tool** — fuer komplette Neuschreibungen nach vorherigem Read
3. **Python `json`-Modul** — wenn programmatisch noetig:
   ```python
   python3 -c "import json; d=json.load(open('PFAD')); d['key']='value'; json.dump(d, open('PFAD','w'), indent=2)"
   ```

## Pflicht: Validierung nach JEDER Aenderung

Nach JEDER Aenderung an einer JSON-Datei SOFORT validieren:
```bash
python3 -c "import json; json.load(open('PFAD')); print('OK')"
```
Kein Commit ohne bestandene Validierung.

## Warum

Am 2026-03-27 hat ein `sed`-Befehl unescapte Anfuehrungszeichen in die settings.json
eingefuegt und das JSON zerstoert. Kaputte settings.json = Claude Code startet nicht
richtig, alle Hooks, Permissions und Konfigurationen sind weg.

`sed` versteht keine JSON-Syntax. Es sieht nur Text und ersetzt blind. Anfuehrungszeichen,
Backslashes, verschachtelte Objekte, Unicode — alles kann schiefgehen. Python und das
Edit-Tool verstehen die Struktur und brechen bei Fehlern ab statt sie still einzufuegen.

## Keine Ausnahme

Auch nicht "nur fuer eine kleine Aenderung". Auch nicht "das ist nur ein einfacher String".
Die Regel gilt ausnahmslos fuer JEDE JSON-Datei.
