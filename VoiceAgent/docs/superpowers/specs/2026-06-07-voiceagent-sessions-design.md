# VoiceAgent — Sessions, kontextsichere Einstellungen & Hintergrund-Komprimierung

**Datum:** 2026-06-07
**Status:** Design freigegeben (Frank, 2026-06-07)
**Betrifft:** `VoiceAgent` (C#/WPF, .NET 10)

---

## 1. Ziel & Hintergrund

Der VoiceAgent fuehrt aktuell genau ein Gespraech im Arbeitsspeicher (`BossAgent._history`).
Es gibt keine gespeicherten Sessions, keine Sidebar, und beim Speichern der Einstellungen
wird der Agent neu gebaut (`MainWindow.BuildAgents()` an `MainWindow.xaml.cs:229`), wodurch
der laufende Verlauf verloren geht.

Diese Erweiterung bringt drei zusammenhaengende Verbesserungen:

1. **Sessions-Oberflaeche** wie Hermes Desktop (Layout-Richtung **C — warm-minimal**):
   linke Sidebar mit allen Unterhaltungen, jede als gespeicherte Session, „Neue Session",
   Auswahl/Wechsel, Umbenennen/Anpinnen/Loeschen.
2. **Kontextsichere Einstellungen:** Das Oeffnen/Speichern der Einstellungen (z. B. Farbe
   auf Grau) darf die aktive Session und ihren Kontext NICHT loeschen.
3. **Pro-Session-Kontext mit Hintergrund-Komprimierung:** Das Wissen einer Session bleibt
   voll erhalten; erst ab einer Schwelle (~75 %) wird im Hintergrund „klug zusammengefasst"
   (aelteste Nachrichten verdichtet, letzte wortwoertlich behalten). Pro Session getrennt;
   neue Session = leeres Kontextfenster.

### Bestehende Bausteine (wiederverwenden)
- `Core/BossAgent.cs` — haelt `_history`, baut `BuildMessages()` (System + Verlauf), ruft `ILlmProvider.ChatAsync` auf.
- `Services/Llm/*` — `ILlmProvider`, `LlmMessage(LlmRole, Text)`, Gemini/Claude/OpenAI senden bereits Multi-Turn.
- `Core/AgentMemory.cs` — sessionuebergreifende Fakten/letzte Turns (JSON). Bleibt fuer Langzeit-Gedaechtnis; NICHT zu verwechseln mit der neuen Session-Persistenz.
- `Diagnostics/Log.cs`, `Diagnostics/Probe.cs`, `Diagnostics/TurnTrace.cs` — Observability-Schicht.
- `MainWindow.xaml(.cs)` — UI + Sprach-/Text-Eingangs-Loop.

---

## 2. Datenmodell

### `ChatSession` (neuer Record/Klasse, `Core/ChatSession.cs`)
| Feld | Typ | Zweck |
|------|-----|-------|
| `Id` | `string` (GUID) | eindeutige ID = Dateiname |
| `Title` | `string` | Anzeigename; Auto-Titel aus erster Nutzer-Nachricht |
| `CreatedAt` / `UpdatedAt` | `DateTimeOffset` | Sortierung in der Sidebar |
| `Pinned` | `bool` | Bereich „Angepinnt" |
| `History` | `List<LlmMessage>` | wortwoertlicher Verlauf (User/Assistant) |
| `Summary` | `string` | laufende Zusammenfassung der bereits komprimierten aelteren Turns ("" wenn keine) |
| `ApproxTokens` | `int` (berechnet) | Groessen-Schaetzung fuer die Kontext-Anzeige |

Persistenz: **eine JSON-Datei pro Session** unter
`%LOCALAPPDATA%\VoiceAgent\sessions\<id>.json`.
Begruendung: gleicher Ort wie Logs/Memory, gut lesbar, keine DB-Abhaengigkeit, einfache Backups.

---

## 3. Komponenten

### 3.1 `SessionStore` (`Core/SessionStore.cs`)
Reine Persistenz, kein UI-Bezug.
- `IReadOnlyList<SessionInfo> List()` — leichte Metadaten (Id, Title, UpdatedAt, Pinned) fuer die Sidebar, ohne den ganzen Verlauf zu laden.
- `ChatSession Load(string id)`, `void Save(ChatSession)`, `void Delete(string id)`, `void Rename(string id, string title)`, `void SetPinned(string id, bool)`.
- **Atomares Schreiben** (Temp-Datei → `File.Replace`/Rename), `encoding=utf-8`, defensiv gegen kaputte/fehlende Dateien (eine unlesbare Session darf die App nicht stoppen — Fehler loggen, Eintrag ueberspringen).

### 3.2 `SessionManager` (`Core/SessionManager.cs`)
Haelt die **aktive Session** und entkoppelt sie vom Anbieter-/Einstellungs-Lebenszyklus.
- `ChatSession Active { get; }`
- `event Action ActiveChanged`
- `NewSession()`, `Switch(string id)`, `Delete`, `Rename`, `SetPinned` (delegiert an `SessionStore`, hebt Auto-Save aus).
- Auto-Save nach jedem Turn (debounced/sofort, atomar).

### 3.3 `BossAgent` — Anpassung
- Statt eigenem `List<LlmMessage> _history` arbeitet der `BossAgent` auf der `History`/`Summary` der **aktiven Session** (per `SessionManager` oder uebergebener `ChatSession`).
- `BuildMessages()` = System-Prompt (+ Faehigkeiten + Zeit + Langzeit-Memory wie bisher) **+ `Summary` als zusaetzlicher System-/Kontextblock (falls vorhanden) + `History`**.
- `RespondAsync`/`HandleAsync` haengen Turn an die aktive Session an und stossen Auto-Save an.
- `Reset()` entfaellt zugunsten von „neue Session".

### 3.4 `ContextCompressor` (`Core/ContextCompressor.cs`)
- Groessen-Schaetzung: Zeichen/4-Heuristik (kein Tokenizer-Dependency), summiert ueber `Summary` + `History`. Budget konfigurierbar (Default z. B. 12.000 Tokens), Schwelle ~75 %.
- Wenn ueberschritten: **im Hintergrund** (Task, nicht im UI-Thread) die aeltesten N Nachrichten nehmen, per `ILlmProvider` zu Text verdichten und in `Summary` zusammenfuehren (bestehende Summary + neue → eine kompakte Summary), die verdichteten Nachrichten aus `History` entfernen, die letzten K Nachrichten wortwoertlich behalten. Danach Session speichern.
- Laeuft pro Session; **neue Session startet mit leerer Summary und leerer History**.
- Verlustarm im Sinne des Lossless-Prinzips: Wissen wandert in die Summary, wird nicht weggeworfen.

### 3.5 UI (`MainWindow.xaml` + neue Sidebar)
- **Sidebar (Layout C, warm-minimal):** „Neue Session" (Strg N), „Sessions durchsuchen…" (Titel-Suche), Bereich „Angepinnt", Bereich „Sessions" (nach `UpdatedAt` sortiert). Aktive Session hervorgehoben. ⋮/Rechtsklick-Menue: Umbenennen / Anpinnen / Loeschen. Sidebar einklappbar.
- **Hauptbereich:** Gespraechsverlauf als Sprechblasen (User rechts, Agent links), schwebendes Eingabefeld unten, **Kontext-Anzeige oben rechts** (Prozent + „wird komprimiert…"-Zustand).
- Warme Akzentfarbe (Terracotta), bestehende Farb-/Conversation-Einstellungen bleiben nutzbar.
- Sprach- und Text-Eingang laufen weiter ueber `RespondAndSpeakAsync`, aber gegen die aktive Session.

---

## 4. Fix: Einstellungen erhalten die Session

Heute: `SettingsButton_Click` → `BuildAgents()` baut `_agent` neu (Verlauf weg).
Neu: `BuildAgents()` baut nur **Anbieter, Endpoint-/Intent-Detektor, TTS/STT** neu. Die
**aktive Session bleibt im `SessionManager`** und wird dem neu gebauten `BossAgent`
wieder mitgegeben. Ergebnis: Farbe/Modell/Mikrofon aendern → Verlauf bleibt vollstaendig.

---

## 5. Datenfluss (ein Turn)

1. Eingabe (Sprache→STT oder Text) → `RespondAndSpeakAsync`.
2. `BossAgent` haengt User-Nachricht an `SessionManager.Active.History`.
3. `BuildMessages()` = System (+Summary) + History → `ILlmProvider.ChatAsync`.
4. Antwort anhaengen, Session **atomar speichern**, Sidebar-`UpdatedAt` aktualisieren.
5. `ContextCompressor` prueft Groesse; ueber Schwelle → Hintergrund-Komprimierung + erneutes Speichern.
6. TTS liest vor; Kontext-Anzeige aktualisiert.

---

## 6. Observability (nach Observability-First-Standard)

- Log-Ereignisse (JSON-Lines): Session erstellt/gewechselt/umbenannt/geloescht/gespeichert; Komprimierung gestartet/fertig (mit „vorher/nachher"-Groesse).
- **Live-Logik-Sonde / Checkpoint pro Turn:** `Probe`/`CHECKPOINT` „Kontext-Block enthaelt N Nachrichten (Summary vorhanden: ja/nein)". Genau diese Sonde haette den Grau-Bug sofort sichtbar gemacht (N waere nach Settings-Save auf 0 gefallen).
- Globaler Fehler-Faenger bleibt; eine unlesbare Session-Datei wird geloggt und uebersprungen, nie still verschluckt.

---

## 7. Tests (`VoiceAgent.Tests`)

- `SessionStore`: Speichern→Laden Round-Trip; atomare Schreibsemantik; Umbenennen/Loeschen/Anpinnen; defekte Datei wird uebersprungen.
- `SessionManager`: NewSession startet leer; Switch laedt korrekt; Auto-Save nach Turn.
- „Einstellungen erhalten die Session": nach Anbieter-Neuaufbau ist `History` unveraendert.
- `ContextCompressor`: unter Schwelle keine Aenderung; ueber Schwelle wird Summary erzeugt, aelteste Nachrichten entfernt, letzte K behalten; neue Session = 0.
- `BuildMessages`: enthaelt Summary-Block wenn vorhanden, sonst nur System + History.
- Auto-Titel aus erster Nachricht.

---

## 8. Bewusst NICHT im Umfang (YAGNI)

- Hermes-Menuepunkte „Skills & Tools", „Messaging", „Artifacts", „Cron", Modell-Wahl-Leiste.
- Volltextsuche ueber Gespraechsinhalte (nur Titel-Suche).
- Cloud-/Geraete-Sync der Sessions.
- Vektor-Retrieval. (Komprimierung ist reines LLM-Zusammenfassen.)

---

## 9. Dateien (neu/geaendert)

**Neu:** `Core/ChatSession.cs`, `Core/SessionStore.cs`, `Core/SessionManager.cs`,
`Core/ContextCompressor.cs`, Sidebar-View (XAML + Code-behind), zugehoerige Tests.
**Geaendert:** `Core/BossAgent.cs` (Session-gebunden), `MainWindow.xaml(.cs)`
(Sidebar, Transkript, Kontext-Anzeige, `BuildAgents` ohne Session-Verlust).

> Hinweis Umsetzung: Vor der Implementierung an WPF/C# werden Bug-Almanach
> (`bugs/desktop/dotnet-csharp.md`) und Best-Practices gelesen (Guard erzwingt das).
> Cross-Platform: VoiceAgent ist Windows-only — kein macOS-Gegenstueck noetig.
