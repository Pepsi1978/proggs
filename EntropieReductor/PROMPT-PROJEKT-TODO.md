# Großprojekt: Eigene Prompts als Agentic-AI in EntropieReductor

**Status:** Geplant, noch nicht begonnen.
**Datum erstellt:** 2026-05-20
**Auf Wunsch von:** Frank
**Umsetzung:** Erst nach gemeinsamer Planung mit `superpowers:brainstorming` / `superpowers:writing-plans`.

---

## Vision

Eigene Prompts (Einstellungen → Eigene Prompts) sollen nicht mehr nur statische Verhaltensregeln im System-Prompt sein, sondern **vollwertige interaktive Agenten** die mit der gesamten App zusammenarbeiten. Frank's eigener Wortlaut:

> "Alle Prompts sollen aus der gesammten App Informationen beziehen können, mit KI Verarbeiten und eigenständige Aufgaben erledigen und zukünftige Prompts sollen das gesammte system verfeinern und auch mit den Ergebnissen der Prompts zusammenarbeiten können, diese dann wieder als Read nutzen können, volle Interaktivität innerhalb der App."

**Beispiel-Prompt den Frank konkret nennt:**

> *"Analysiere die Einträge in Entropie und verbinde sie mit den Einträgen in Thesen, bilde dann daraus Aufgaben und erstelle automatisch passende Aufgaben, schicke diese Aufgaben an den Forscher und lass diesen die Aufgaben auf alternative Reduzierung der Entropie erforschen."*

Das ist ein **Multi-Schritt-Workflow** der lesend UND schreibend auf mehrere App-Bereiche zugreift.

---

## Technische Grundlage: Gemini Function Calling

Gemini (die bereits genutzte API) unterstützt **Tool Use / Function Calling**: Der KI wird eine Liste von Funktionen mit Beschreibung und Parametern übergeben. Sie entscheidet selbst, welche sie wann aufruft. Das Modell kann mehrere Tools in Folge aufrufen ("Reasoning + Acting" / ReAct-Pattern).

Doku: https://ai.google.dev/gemini-api/docs/function-calling

---

## Phasen

### Stufe 1: Read-Only-Tools (Basis)

Prompts können Daten aus der gesamten App lesen, KI-Aufrufe machen und Text-Antworten zurückgeben. Keine schreibenden Aktionen. Ergebnis erscheint im Ausführungs-Dialog.

**Tools (alle nur lesen):**
- `read_entropie_eintraege(zeitraum: "letzte_7_tage" | "letzter_monat" | "alle")` → Liste Tagebuch-Einträge mit Titel, Text, Zusammenfassung, Nachträgen
- `read_thesen(zeitraum)` → Liste Thesen-Einträge analog Tagebuch
- `read_aufgaben(filter: "offen" | "erledigt" | "alle", kategorie?)` → Aufgaben mit Titel, Beschreibung, Kategorie, Prio, Status
- `read_memory(quelle?: "MANUELL" | "AUS_PROFIL" | "KI_VORSCHLAG")` → Gedächtnis-Einträge
- `read_profil()` → Persönliches Profil als String
- `read_insights(min_confidence?)` → Insight-Board
- `read_biomarker(metrik: "schlaf" | "hrv" | "stress" | ..., zeitraum)` → Whoop/Oura/Amazfit-Werte
- `read_hypothesen(status?)` → Forscher-Hypothesen
- `read_forscher_sessions(limit?)` → Forscher-Diskussions-Verläufe

**UI:** Pro Prompt ein "Ausführen"-Knopf. Ergebnis als formatierter Text in einem Dialog.

**Aufwand:** ~1-2 Tage.

---

### Stufe 2: Write-Tools mit Bestätigung

Tools die Daten anlegen oder ändern. Vor jeder Schreib-Aktion erscheint ein Bestätigungs-Dialog ("Soll ich diese 5 Aufgaben anlegen? [Vorschau] [Ja] [Nein]"). Frank kann einzelne Aktionen ablehnen.

**Tools (schreibend):**
- `create_aufgabe(titel, beschreibung, kategorie, prio, bucket)` → neue Aufgabe in Aufgaben-Liste
- `create_tagebuch_eintrag(text)` → neuer Entropie-Eintrag
- `create_these(text)` → neuer Thesen-Eintrag
- `create_hypothese(titel, beschreibung, rationale, geplantes_start_datum, geplantes_end_datum)` → neue Hypothese
- `create_forscher_session(thema, initial_kontext)` → neue Diskussion mit Forscher starten + erste Nachricht
- `add_memory(content, kategorie?)` → Gedächtnis-Eintrag
- `add_followup_to_aufgabe(aufgabe_id, text)` → Nachtrag zu Aufgabe
- `update_aufgabe_status(id, status: "OFFEN" | "IN_ARBEIT" | "REDUZIERT" | "ARCHIVIERT")`
- `update_profil(neuer_text)` → ersetzt Profil-Text (sehr kritisch — extra Sicherheit)
- `delete_aufgabe(id)` → erfordert doppelte Bestätigung

**Sicherheits-Schicht:**
- Vor JEDEM Schreib-Tool: User-Confirm-Dialog mit Diff-Vorschau
- Optionaler "Trust-Modus" pro Prompt: User kann sagen "diesem Prompt vertraue ich, frag mich nicht jedes Mal"
- Audit-Log: alle ausgeführten Tool-Calls werden in einer eigenen Tabelle protokolliert (für Rollback und Nachvollziehbarkeit)
- Quota: max N schreibende Aktionen pro Prompt-Ausführung (verhindert Run-Away-Loops)

**Aufwand:** ~1-1½ Tage.

---

### Stufe 3: Workflow-Loops + Ergebnis-Persistenz + Verkettung

Hier kommt die "volle Interaktivität" die Frank explizit nennt. Drei Kern-Features:

#### 3.1 Iterative Loops im Modell

Gemini bekommt nach jedem Tool-Call das Ergebnis zurück und kann entscheiden was als nächstes passiert. Der Workflow-Runner führt das in einer Loop aus, bis Gemini sagt "fertig" oder ein Limit erreicht ist (z.B. max 20 Tool-Calls pro Run).

#### 3.2 Ergebnis-Persistenz

Jede Prompt-Ausführung erzeugt ein **Prompt-Ergebnis-Objekt** mit:
- ID, Datum, ausgeführter Prompt (ID + Snapshot), Eingabe-Kontext
- Alle Tool-Calls (vollständig)
- Finale Antwort
- Erzeugte/Geänderte Entitäten (Liste von IDs)

Speicherung in neuer Room-Tabelle `prompt_executions`. Ins Backup mit aufnehmen.

#### 3.3 Cross-Prompt-Read

Neues Tool: `read_prompt_ergebnisse(prompt_name?, zeitraum?)` → Frühere Ausführungen anderer Prompts. Damit kann ein Prompt auf den Output eines anderen Prompts referenzieren.

**Beispiel-Kette:**
1. Prompt "Wochenanalyse" läuft → speichert Ergebnis als Bullet-Points
2. Prompt "Aufgaben-Generator" läuft → liest letzte Wochenanalyse + generiert Aufgaben daraus
3. Prompt "Forscher-Briefing" läuft → liest erstellte Aufgaben + startet Forscher-Sessions

#### 3.4 Trigger-System

Auto-Ausführung von Prompts:
- **Manuell** (User tippt auf Ausführen)
- **Zeitgesteuert** (cron-like: "jeden Sonntag 19:00")
- **Event-basiert** (z.B. "wenn neue Entropie-Einträge > 3 in 24h hinzukamen")
- **Kettenausführung** ("wenn Prompt X fertig ist, führe automatisch Prompt Y aus")

**Aufwand:** ~2-3 Tage.

---

## UI-Konzept (Zusammenfassung)

**Eigene-Prompts-Screen:**
- Akkordeons pro Kategorie (schon vorhanden seit Commit #909)
- Pro Prompt-Karte zusätzlich:
  - "Ausführen"-Knopf
  - "Verlauf"-Knopf (zeigt frühere Ergebnisse)
  - "Auto-Trigger einstellen" (Stufe 3)

**Prompt-Editor erweitert:**
- Welche Tools darf dieser Prompt aufrufen? (Default: alle Read-Only, Write-Tools manuell freischalten)
- Trust-Modus an/aus
- Auto-Trigger konfigurieren (zeitlich/event-basiert)

**Ausführungs-Dialog:**
- Live-Anzeige der Tool-Calls während der Ausführung (transparent)
- Bestätigungs-Dialog vor schreibenden Aktionen mit Diff-Vorschau
- Endergebnis als formatierte Antwort
- Speichern + Teilen + Erneut ausführen

**Audit-Log-Screen** (in Einstellungen):
- Alle Prompt-Ausführungen mit Datum, Dauer, Ergebnis
- Filter nach Prompt-Name, Kategorie, Erfolg/Fehler
- Rollback-Option für einzelne Tool-Calls

---

## Sicherheits-Konzept

| Risiko | Schutz |
|--------|--------|
| KI löscht versehentlich Daten | Lösch-Tools brauchen doppelte Bestätigung |
| Endlos-Loop verbraucht Quota | Max 20 Tool-Calls pro Ausführung, max 100k Tokens |
| KI ändert Profil unkontrolliert | `update_profil` ist nicht in Tool-Default-Liste — muss explizit pro Prompt aktiviert werden |
| Versehentlich vertrauliche Daten an Gemini | Read-Tools liefern Diff-Vorschau bevor an Gemini geschickt wird |
| Workflow-Run hängt | Timeout 60 Sekunden pro Tool-Call, gesamt max 5 Minuten |
| Schadhafter Prompt (z.B. Prompt-Injection in Profil) | Tool-Calls werden mit System-Constraints umrahmt, nicht direkt vom User-Prompt durchgereicht |

---

## Datenmodell-Erweiterungen (Stufe 3)

```kotlin
@Entity("prompt_executions")
data class PromptExecutionEntity(
    @PrimaryKey val id: String,
    val promptId: String,           // FK auf SavedPromptEntity (oder snapshot, falls Prompt gelöscht)
    val promptName: String,         // Snapshot
    val promptContent: String,       // Snapshot
    val promptCategory: PromptCategory,
    val startedAt: Long,
    val finishedAt: Long?,
    val toolCallsJson: String,       // Serialisierte Liste aller Tool-Calls
    val finalAnswer: String?,
    val createdEntityIds: List<String>,  // IDs von erzeugten Entitäten
    val updatedEntityIds: List<String>,
    val status: ExecutionStatus,     // RUNNING, SUCCESS, FAILED, CANCELLED
    val errorMessage: String?,
    val tokenUsage: Int,
)

@Entity("prompt_tool_permissions")
data class PromptToolPermissionEntity(
    @PrimaryKey val id: String,
    val promptId: String,
    val toolName: String,            // z.B. "create_aufgabe"
    val granted: Boolean,            // ob diesem Prompt der Aufruf erlaubt ist
    val trustMode: Boolean,          // ohne Confirm-Dialog ausführen
)
```

---

## Erweiterung des bestehenden Kategorien-Systems

Aktuell (seit Commit #909) wirken Prompts pro Bereich als Verhaltensregeln. Mit Agentic-AI:

| Kategorie | Bedeutung als Verhaltensregel (aktuell) | Bedeutung als Agent (Großprojekt) |
|-----------|------------------------------------------|-------------------------------------|
| AUFGABEN | Beeinflusst Auto-Bewertung von neuen Aufgaben | Kann Aufgaben analysieren, neue erstellen, Status ändern |
| ENTROPIE | (noch nicht angeschlossen) | Kann Tagebuch lesen, neue Einträge anlegen, KI-Zusammenfassungen aktualisieren |
| THESEN | (noch nicht angeschlossen) | Kann Thesen lesen, neue anlegen, Verbindungen zu Entropie ziehen |
| ANALYSE | Beeinflusst Analyse-Texte | Kann eigenständige Analysen erstellen und ins Insight-Board schreiben |
| FORSCHER | Beeinflusst Forscher-Chat | Kann eigenständig Forscher-Sessions starten und Hypothesen anlegen |
| CODEX | Beeinflusst Codex-Synthese | Kann eigenständig neue Codex-Versionen generieren |

---

## Reihenfolge der Umsetzung

1. **Vorbereitung** (mit `superpowers:brainstorming` + `superpowers:writing-plans`):
   - Genaue Tool-Liste finalisieren (welche Tools braucht Frank zuerst?)
   - UI-Mockups für den Ausführungs-Dialog
   - Sicherheits-Schicht spezifizieren
   - Edge Cases sammeln (was passiert wenn Gemini-Quota leer? Was bei Netzfehler mitten im Workflow?)
2. **Stufe 1** (Read-Only): ~1-2 Tage
3. **Test-Phase**: Frank nutzt eine Woche, sammelt Wünsche
4. **Stufe 2** (Write mit Confirm): ~1-1½ Tage
5. **Test-Phase**: Frank nutzt eine Woche
6. **Stufe 3** (Loops + Trigger + Verkettung): ~2-3 Tage

Gesamt-Aufwand: 4-6 Arbeitstage über mehrere Wochen verteilt.

---

## Offene Fragen für die Planungs-Session

- Welche Tools sind absolut zwingend in Stufe 1?
- Sollen Prompts pro Kategorie nur Tools dieser Kategorie aufrufen dürfen (Sicherheit) oder kategorieübergreifend (Flexibilität)?
- Auto-Trigger: Sollen die im Hintergrund laufen (auch wenn App geschlossen) oder nur wenn App offen ist? (Hintergrund = WorkManager + Battery-Optimierung)
- Multi-Model: Soll ein Prompt zwischen Gemini-Modellen wählen können (Pro für komplexe Analysen, Flash für schnelle Tools)?
- Wie wird der Token-Verbrauch budgetiert? (z.B. monatliches Limit pro Prompt-Kategorie?)
- Soll es eine "Prompts-Vorlagen-Galerie" geben mit fertigen Workflow-Vorlagen?

---

## Notizen aus dem Gespräch mit Frank (2026-05-20)

- "ich möchte quasi nicht nur Prompts erstellen, die an KI geschickt werden, sondern die in der App intelligent arbeiten können, abhängig davon, was ich eintippe"
- "ALLEN Bereichen, also überall in der Lage sein Informationen abzurufen und in alle Bereiche der App auch reinarbeiten können"
- "zukünftige Prompts sollen das gesammte system verfeinern und auch mit den Ergebnissen der Prompts zusammenarbeiten können, diese dann wieder als Read nutzen können"
- "volle Interaktivität innerhalb der App"
- "speicher dir das gut ab als Großprojekt"

**Wichtige Implikation:** Frank will Prompts die sich gegenseitig aufrufen können (Prompt-Verkettung). Das ist Stufe 3 — kritisch für das Selbstverfeinerungs-Konzept.
