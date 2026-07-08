# Kommunikation, Sprache & Benutzer-Interaktion

## 1. Memory-Transparenz

Beim Speichern von Memories/Regeln/Config-Aenderungen IMMER dem Benutzer auf Deutsch erklaeren was
gespeichert wurde und warum: `Gespeichert: **[Titel]** — [1-2 Saetze]`. Gilt fuer Feedback-/Projekt-/
User-Memories, neue Regeln, Config, Whiteboard.

## 2. Benutzer kennt seine Workflows besser als der Code

Sagt der Benutzer "Feature X nutze ich nicht" → NICHT mit "aber der Code hat es" widersprechen.
Stattdessen: "Verstanden. Soll ich den ungenutzten Code entfernen?" oder direkt entfernen. Code-Existenz
beweist nur, dass jemand ihn geschrieben hat, nicht dass er benutzt wird.

## 3. Links automatisch oeffnen

Empfiehlt Claude eine Webseite, MUSS der Link SOFORT geoeffnet werden (nicht nur als Text): direktesten,
tiefsten Link bauen → `start "URL"` (Windows) / `open "URL"` (macOS) → danach erklaeren was zu tun ist.
Bekannte Direkt-Links: Google Cloud `console.cloud.google.com/apis/credentials?project=PROJECT_ID` ·
Firebase `console.firebase.google.com/project/PROJECT_ID/settings/general` · GitHub
`github.com/OWNER/REPO/settings`. NIE nur "Gehe zu [URL]" ohne zu oeffnen; nie nur die Startseite.

## 4. Deutsche Sprache fuer Agents, Skills & Commands

Alle selbst erstellten Agents/Skills/Commands komplett auf Deutsch (Beschreibungen, Beispiele,
System-Prompts). Englisch erlaubt: Tool-Namen, Code-Variablen, technische Bezeichner (MCP/API/JSON),
Commit-Messages.

## 5. Deutsche Skill-Trigger-Zuordnung

Vollstaendige Trigger-Map: `~/.claude/rules/german-skill-triggers.md` (eigene Datei).
