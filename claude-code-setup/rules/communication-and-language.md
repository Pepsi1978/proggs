# Kommunikation, Sprache & Benutzer-Interaktion

## 1. Deutsche Umlaute IMMER (KRITISCH)

In ALLEN deutschen Ausgaben und Programmierarbeiten echte Umlaute/Sonderzeichen: **ä/ö/ü** (auch
Gross), **ß** (wo orthographisch korrekt) — NIE "ae/oe/ue/ss". ASCII-Substitution in deutschem Text
VERBOTEN, auch bei langen Antworten/Tabellen nicht "vergessen". Gilt: Chat-Antworten, deutsche
Code-Kommentare, UI-Strings (strings.xml/Resources), README/Doku, Memory-Eintraege, deutsche
Commit-Messages, Fehlermeldungen/Logs, neue Agents/Skills/Commands.
Ausnahme (ASCII bleibt erlaubt): Dateinamen/Pfade, Code-Variablen/Funktionsnamen (Konvention
Englisch), URLs/Git-Hashes/technische IDs, englische Texte + englische Commit-Messages, **bestehende
Regel-Dateien mit "ae/oe/ue" (nicht retroaktiv umschreiben)**.
Richtig: "Ich führe den Befehl für dich aus." · "Größe: 42 MB." — Falsch: "fuehre" · "Groesse".

## 2. Deutsch fuer selbst erstellte Agents/Skills/Commands

Komplett Deutsch (Beschreibungen, `<example>`-Bloecke, System-Prompts). Englisch nur: Tool-Namen,
Code-Variablen, technische Bezeichner (MCP/API/JSON), Commit-Messages. Externe Plugins NICHT
uebersetzen. Trigger-Map deutscher Anfragen → richtige Skills: `german-skill-triggers.md`.

## 3. Memory-Transparenz

Beim Speichern von Memories/Regeln/Config-Aenderungen IMMER auf Deutsch erklaeren was + warum:
`Gespeichert: **[Titel]** — [1-2 Saetze]`. Gilt fuer Feedback-/Projekt-/User-Memories, neue Regeln,
Config, Whiteboard.

## 4. Benutzer kennt seine Workflows besser als der Code

Sagt der Benutzer "Feature X nutze ich nicht" → NICHT mit "aber der Code hat es" widersprechen,
sondern: "Verstanden. Soll ich den ungenutzten Code entfernen?" oder direkt entfernen. Code-Existenz
beweist nur, dass jemand ihn geschrieben hat, nicht dass er benutzt wird.

## 5. Links automatisch oeffnen

Empfiehlt Claude eine Webseite, MUSS der Link SOFORT geoeffnet werden (nicht nur als Text):
direktesten/tiefsten Link bauen → `start "URL"` (Windows) / `open "URL"` (macOS) → danach erklaeren
was zu tun ist. Bekannte Direkt-Links: Google Cloud
`console.cloud.google.com/apis/credentials?project=PROJECT_ID` · Firebase
`console.firebase.google.com/project/PROJECT_ID/settings/general` · GitHub
`github.com/OWNER/REPO/settings`. NIE nur "Gehe zu [URL]" ohne zu oeffnen; nie nur die Startseite.
