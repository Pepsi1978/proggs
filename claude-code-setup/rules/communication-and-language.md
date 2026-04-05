# Kommunikation, Sprache & Benutzer-Interaktion

> Konsolidiert aus: memory-transparency, trust-user-workflow-knowledge,
> auto-open-links, german-agents-skills
> (german-skill-triggers bleibt als eigene Datei — zu umfangreich zum Konsolidieren)

---

## 1. Memory-Transparenz: Immer erklaeren was gespeichert wird

**Regel:** Beim Speichern von Memories, Regeln oder Config-Aenderungen IMMER
dem Benutzer auf Deutsch erklaeren was gespeichert wurde und warum.

**Format:**
```
Gespeichert: **[Titel]** — [1-2 Saetze Erklaerung]
```

**Gilt fuer:** Feedback-Memories, Projekt-Memories, User-Praeferenzen,
neue Regeln, Config-Aenderungen, Whiteboard-Eintraege.

---

## 2. Benutzer kennt seine Workflows besser als der Code

**Regel:** Wenn der Benutzer sagt "Feature X nutze ich nicht" → NICHT mit
"aber der Code hat es" widersprechen.

Stattdessen: "Verstanden. Soll ich den ungenutzten Code entfernen?" oder direkt entfernen.

Die Existenz von Code beweist nur dass jemand ihn geschrieben hat, nicht dass er benutzt wird.
Der Benutzer ist die Autoritaet ueber seine eigenen Workflows.

> Vorfall 2026-03-28: Benutzer sagte "Translate braucht kein Gemini". Statt das zu
> akzeptieren wurde dagegen argumentiert. Das war falsch.

---

## 3. Links automatisch oeffnen (KRITISCH)

**Regel:** Wenn Claude eine Webseite empfiehlt, MUSS der Link SOFORT geoeffnet werden —
nicht nur als Text hingeschrieben.

### Pflicht-Ablauf

1. **Direktesten, tiefsten Link** zusammenbauen (exakte Unterseite, nicht Startseite)
2. **Sofort oeffnen:** `start "URL"` (Windows) oder `open "URL"` (macOS)
3. **Danach erklaeren** was der Benutzer auf der Seite tun soll

### Bekannte direkte Links

| Dienst | Direkt-Link |
|--------|------------|
| Google Cloud Credentials | `console.cloud.google.com/apis/credentials?project=PROJECT_ID` |
| Google Cloud APIs | `console.cloud.google.com/apis/library?project=PROJECT_ID` |
| Firebase Console | `console.firebase.google.com/project/PROJECT_ID/settings/general` |
| Play Store Console | `play.google.com/console/developers` |
| GitHub Repo Settings | `github.com/OWNER/REPO/settings` |
| GitHub Actions | `github.com/OWNER/REPO/actions` |

### Was NIEMALS passieren darf

- ❌ "Gehe zu [URL]" schreiben ohne den Link auch zu oeffnen
- ❌ Nur Startseite oeffnen wenn tiefere Unterseite bekannt ist
- ❌ Benutzer bitten, den Link selbst zu kopieren

---

## 4. Deutsche Sprache fuer Agents, Skills & Commands

**Regel:** Alle Custom Agents, Skills und Commands muessen komplett auf Deutsch geschrieben werden:
- Beschreibungen, Beispiele, System-Prompts: **Deutsch**
- Tool-Namen, Code-Variablen, technische Bezeichner (MCP, API, JSON): **Englisch erlaubt**
- Commit-Messages: **Englisch erlaubt**

---

## 5. Deutsche Skill-Trigger-Zuordnung

Die vollstaendige Skill-Trigger-Map mit allen 18 Abschnitten, exakten Skill-Namen,
Entscheidungshilfen, Whisper-Korrekturen und proaktiven Agents befindet sich in:
→ `~/.claude/rules/german-skill-triggers.md` (eigene Datei, 291 Zeilen)
