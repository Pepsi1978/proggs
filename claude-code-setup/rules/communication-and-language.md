# Kommunikation, Sprache & Benutzer-Interaktion

## 1. Deutsche Umlaute IMMER (KRITISCH)

In ALLEN deutschen Ausgaben + Programmierarbeiten echte Umlaute: **ä/ö/ü/ß** — NIE "ae/oe/ue/ss", auch
nicht bei langen Antworten/Tabellen. Gilt: Chat, deutsche Code-Kommentare, UI-Strings, README/Doku,
Memory, deutsche Commits, Logs, neue Agents/Skills/Commands. ASCII bleibt erlaubt: Dateinamen/Pfade,
Code-Variablen/Funktionsnamen, URLs/Hashes/IDs, englische Texte + englische Commits, **bestehende
Regel-Dateien mit "ae/oe/ue" (nicht retroaktiv umschreiben)**. Richtig: "führe … Größe … Übersetzung".

## 2. Deutsch fuer selbst erstellte Agents/Skills/Commands

Komplett Deutsch (Beschreibung, `<example>`, System-Prompt). Englisch nur: Tool-Namen, Code-Variablen,
technische Bezeichner (MCP/API/JSON), Commits. Externe Plugins NICHT uebersetzen. Trigger-Map deutscher
Anfragen → richtige Skills: `german-skill-triggers.md`.

## 3. Memory-Transparenz

Beim Speichern von Memories/Regeln/Config IMMER auf Deutsch erklaeren was + warum:
`Gespeichert: **[Titel]** — [1-2 Saetze]`.

## 4. Benutzer kennt seine Workflows besser als der Code

"Feature X nutze ich nicht" → NICHT "aber der Code hat es", sondern "Soll ich den ungenutzten Code
entfernen?" oder direkt entfernen. Code-Existenz beweist nur, dass jemand ihn schrieb, nicht dass er benutzt wird.

## 5. Links automatisch oeffnen

Empfohlene Webseite SOFORT oeffnen (nicht nur als Text): direktesten/tiefsten Link bauen →
`start "URL"` (Windows) / `open "URL"` (macOS) → dann erklaeren. Direkt-Links: Google Cloud
`console.cloud.google.com/apis/credentials?project=PROJECT_ID` · Firebase
`console.firebase.google.com/project/PROJECT_ID/settings/general` · GitHub `github.com/OWNER/REPO/settings`.
Nie nur "Gehe zu [URL]", nie nur die Startseite.
