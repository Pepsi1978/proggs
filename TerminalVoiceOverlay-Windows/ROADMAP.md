# Voice Terminal Overlay (Windows) — Zukunftsplan

> **Stand:** 2026-04-29
> **Zweck dieser Datei:** Sammelort fuer alle naechsten Schritte zur Verbesserung
> der App. Was hier drinsteht, ist beschlossen oder ernsthaft erwogen — nicht
> jede Idee, sondern nur die, fuer die ein konkreter Mehrwert sichtbar ist.
> Reihenfolge der Sektionen: erst kurzfristig, dann mittelfristig, dann Backlog.

---

## Bereits erledigt

| Datum | Baustein | Effekt |
|-------|----------|--------|
| 2026-04-29 | **Live-Pegel-Anzeige im Mic-Button** (14 Striche, weiss, fliessen rechts→links) | Sofortige optische Bestaetigung dass das Mikro Ton einfaengt — kein Blind-Diktieren mehr |

---

## Kurzfristig (geplant fuer die naechsten Sessions)

Sortiert nach Wirkung pro investiertem Tag. Hotkey/Push-to-Talk wurden bewusst
gestrichen — fuer den maus-zentrierten Workflow von Frank kein Mehrwert.

### 1. Makros — Ein-Klick-Workflows (eigene Session)

| Aspekt | Inhalt |
|--------|--------|
| Was es ist | Ein gespeicherter Knopf im PromptBoard, der eine ganze Kette ausfuehrt: festes Pre-Prompt setzen, optionale Mitte (leer oder per Voice), festes Post-Prompt anhaengen, Auto-Enter ausloesen. |
| Wo es lebt | Neuer Tab oder eigene Spalte im PromptBoardPanel; im DI-Layer ein neues `IMacroService` mit Repository-Backing in der bestehenden SQLite-DB. |
| Wie es funktioniert | Macro = JSON-Struktur `{ preId, mid, postId, autoEnter }`. Beim Klick durchlaeuft der Code den bestehenden `OnInputSubmit`-Pfad — keine Parallel-Logik. |
| Aufwand | 2-3 Tage |
| Wirkung | Reduziert Frank's haeufigste 5 Workflows von 4 Klicks auf 1 Klick. **Groesste echte Zeitersparnis von allen Bausteinen.** |
| Beispiele aus Frank's Alltag | "Lint+Commit", "Cross-Platform-Sync", "PR machen", "Welche Dateien hat diese Session geaendert?", "Ueber das letzte Diktat reflektieren" |
| Status | **Eigene Session** — Frank moechte das separat angehen, mit eigener Architektur-Diskussion. |

### 2. KI-Profile — automatische Anpassung pro Ziel-KI

| Aspekt | Inhalt |
|--------|--------|
| Was es ist | Erkennt automatisch welche KI im aktiven Terminal laeuft (Claude Code / Codex / Gemini-CLI / Aider / etc.) und schaltet ein passendes Profil mit eigenen Pre/Post-Prompts und eigener Konfiguration ein. |
| Wo es lebt | Erweiterung des `TerminalWatcher` (kennt schon den Prozessnamen) plus neue Tabelle `Profiles` in der PromptBoard-DB. Pro Pre/Post-Eintrag ein Feld "Gilt fuer: alle / Claude / Codex / Gemini / ...". |
| Wie es funktioniert | Beim Build der AlwaysOn-Wrappers wird zusaetzlich nach Ziel-KI gefiltert. Frank pflegt einmal seine Profile, der Pillar zeigt klein welches Profil gerade aktiv ist (Mini-Icon ueber dem Stern-Knopf). |
| Aufwand | 3-4 Tage |
| Wirkung | Frank muss nie wieder daran denken, welcher Pre-Prompt zu welcher KI gehoert. Die App weiss es. |
| Voraussetzung | Erkennung welcher Befehl im Terminal laeuft — schwierig, da Windows-Terminal nur den Shell-Prozess zeigt. Loesung: Heuristik aus Window-Titel ("Claude Code" / "codex" / "gemini") plus Default-Fallback. |
| Status | Geplant nach Makros |

### 3. Voice Activity Detection — Auto-Stop bei Stille

| Aspekt | Inhalt |
|--------|--------|
| Was es ist | Aufnahme stoppt automatisch wenn 1.5 Sekunden Stille gemessen werden — der zweite Mausklick zum Stoppen entfaellt. |
| Wo es lebt | Im `AudioRecorder.cs` — der Pegel-Stream ist ja schon da (siehe Pegel-Anzeige), wir messen ihn nur mit. Sobald der Pegel unter eine Schwelle faellt und 1.5s dort bleibt: Aufnahme stoppen, Event feuern, das `OverlayWindow` triggert seinen normalen Transcribe-Pfad. |
| Wie es funktioniert | Einfacher Lautstaerke-Threshold (z.B. < 0.02 = Stille) oder schlauer mit WebRTC-VAD-Bibliothek (erkennt echte Sprache vs. Hintergrundgeraeusch). Toggle-Knopf im Settings: "Auto-Stop nach 1.5s Stille". |
| Aufwand | 1-2 Tage (Threshold-Variante), 3 Tage mit WebRTC-VAD |
| Wirkung | Spart pro Diktat einen Mausklick. Bei 30 Diktaten/Tag etwa 30 Sekunden plus mentalen Fokus, den der "Stop"-Klick sonst raubt. |
| Status | Geplant — Threshold-Variante zuerst, WebRTC-VAD spaeter falls Threshold zu unzuverlaessig ist. |

### 4. Inline-Vorschau-Toast — Whisper-Text 1.5s zeigen vor Insert

| Aspekt | Inhalt |
|--------|--------|
| Was es ist | Der transkribierte Text erscheint kurz als Sprechblase ueber dem Pillar bevor er ins Terminal geht. Esc verwirft, Enter beschleunigt, einfach warten = Auto-Insert nach 1.5s. |
| Wo es lebt | Neues `ToastWindow.xaml` (transparentes WPF-Fenster ohne Rahmen). Wird in `OverlayWindow.xaml.cs` zwischen `_groqClient.TranscribeAsync` und `TerminalController.PasteText` eingehaengt. |
| Wie es funktioniert | Dispatcher-Timer mit 1.5s, beim Tick wird der Text inseriert. Tastendruck-Listener fuer Esc/Enter waehrend die Toast offen ist. |
| Aufwand | 2 Tage |
| Wirkung | Tippfehler/Falsch-Hoerungen werden abgefangen **bevor** sie die KI verwirren. Besonders wertvoll bei aktivem Auto-Enter, wo heute der Fehler sofort an die KI geht. |
| Status | Geplant nach VAD |

### 5. Personal Dictionary mit Whisper-Hint (statt String-Replace)

| Aspekt | Inhalt |
|--------|--------|
| Was es ist | Eine Liste von Frank's typischen Programmier-Begriffen (Subagent, Codex, Bun, Playwright, Gemini, Claude, Tampermonkey, ...), die als `prompt`-Parameter an Groq Whisper mitgegeben wird. |
| Warum nicht String-Replace | Frank hat zu Recht abgelehnt: starres Ersetzen macht aus echtem "Schloss" auch "Subagent" und schleicht so neue Fehler ein. |
| Wie das hier anders ist | Whisper benutzt die Liste nur als **Wahrscheinlichkeits-Hinweis** und entscheidet kontextsensitiv. "Schloss" bleibt "Schloss" wenn der Satz davon handelt; wird zu "Subagent" wenn es passt. |
| Wo es lebt | `GroqWhisperClient.cs` — neuer Parameter `prompt` an die API uebergeben. Liste in einer `personal-vocabulary.json` neben der `.env`, oder im PromptBoard-Settings-Dialog editierbar. |
| Aufwand | 1 Tag |
| Wirkung | Reduziert Falsch-Hoerungen bei Programmier-Begriffen deutlich, ohne neue Fehler einzuschleichen. |
| Status | Geplant — koennte parallel zu VAD/Toast laufen, da unabhaengig. |

---

## Mittelfristig

### 6. DE/EN-Quick-Toggle

Aktuell ist `WHISPER_LANG=de` fix in der `.env`. Bei mischsprachigen Sessions (deutsche Anweisung mit englischen Code-Begriffen) kommt Whisper durcheinander. Loesung: ein Mini-Toggle im Pillar (entweder zusaetzlicher Mini-Knopf oder `Shift+Mic-Klick` = einmalig EN). **Aufwand:** 1 Tag.

### 7. Multi-Whisper-Undo (Stack statt 1 Schritt)

Heute funktioniert der `W`-Knopf nur einmal — `lastRawTranscript` wird auf null gesetzt. Mit einem Stack der letzten 10 Roh-Transkripte koennte Frank auch nach 3 Diktaten zum Original eines aelteren Diktats zurueck. Optional Rechtsklick auf W = Auswahl-Menue. **Aufwand:** 1 Tag.

### 8. Lokales Whisper-Fallback (whisper.cpp)

Heute geht JEDE Aufnahme an Groq in der Cloud — auch ein 2-Sekunden-"ja". Mit `whisper.cpp` lokal koennten kurze Aufnahmen (<5s) sofort verarbeitet werden, ohne Cloud-Latenz und ohne Internetabhaengigkeit. Lange Aufnahmen weiter an Groq fuer Qualitaet. **Aufwand:** 5-7 Tage (groesserer Baustein wegen Bibliothek-Integration und Modell-Verwaltung).

### 9. Reformulate-Stufe vor Submit

Optional vor dem finalen Insert: Gemini sieht den fertigen Prompt (Pre + Mitte + Post) und macht daraus *einen* sauberen Prompt statt der `;`-Kette. Hilfreich fuer komplexe Multi-Task-Prompts. Eigener Toggle-Knopf "Reformat". **Aufwand:** 2 Tage.

### 10. Cross-CLI-Info im Pillar

Mini-Icon ueber dem Stern oder neben dem Mic-Button, das anzeigt welche KI gerade Ziel ist (Claude/Codex/Gemini-CLI). Kommt automatisch aus dem KI-Profil-System (Baustein 2). **Aufwand:** halber Tag, **Voraussetzung:** Baustein 2 muss da sein.

---

## Backlog (gute Ideen, keine Eile)

| Idee | Kurzbeschreibung |
|------|-----------------|
| **Snippet-Variablen** | `{{datum}}`, `{{branch}}`, `{{datei}}`, `{{terminal}}` als Platzhalter in Pre/Post-Prompts und Macros. Werden zur Insert-Zeit aus Git/Filesystem aufgeloest. |
| **Multi-Terminal-Targets** | Dropdown der letzten 3 Terminals + Hotkey um zwischen ihnen zu schicken. Heute geht alles ans aktive Terminal. |
| **Prompt-Diff bei Gemini-Cleanup** | Nach Gemini-Korrektur die Aenderungen rot/gruen sehen, gezielt zurueckrollen. |
| **Persoenliche Statistiken** | "Heute 47 Diktate, 4823 Woerter, 6 Macros". Motivation + Erkenntnisse welche Knoepfe nie benutzt werden (rauswerfen). |
| **Prompt-Empfehlungen** | "Du hast diesen Prompt 3x getippt — als Macro speichern?". App lernt Frank's Muster. |
| **Audio-Streaming statt Stop-Whisper** | Whisper-Streaming-API: Text erscheint Wort fuer Wort im Toast. Gefuehlt halbierte Latenz. |
| **Session-Recording / Replay** | Voice-Inputs + Antworten einer Session aufzeichnen — fuer Workflow-Lernen oder Debug. |
| **Audio-Fallback bei Groq-Down** | Wenn Groq ausfaellt, automatisch zu OpenAI Whisper API wechseln. |
| **Per-Terminal-Dock-Position** | Heute: globale Pillar-Position. Besser: pro Terminal-Typ getrennt (Windows Terminal vs. Powershell-Standalone). |

---

## Cross-Platform-Sync (Schwester-Apps)

| App | Welle drin? | Naechster Schritt |
|-----|-------------|------------------|
| **TerminalVoiceOverlay-Windows** (diese App) | ✅ ja | Naechste Bausteine direkt hier umsetzen |
| **ClaudeVoiceOverlay-Windows** | ✅ ja | Bei Updates der TVO-Logik gleich mitziehen — siehe `CLAUDE.md`-Sister-Project-Hinweis |
| **TerminalVoiceOverlay-macOS** (Swift) | ❌ noch nicht | Welle nachziehen wenn Frank am Mac arbeitet — Konzept identisch, nur AVAudioEngine statt NAudio |
| **ClaudeCodexVoiceOverlay-macOS** (Swift) | ❌ noch nicht | Siehe oben |

**Pflicht:** Jede Aenderung an einem geteilten Service (AudioRecorder, GroqWhisperClient, GeminiClient, OverlayWindow-Logik) MUSS auch in den Schwester-Apps nachgezogen werden. Die spezifischen Unterschiede (Window-Watcher, Tastatur-Simulation, Zielfenster-Erkennung) bleiben getrennt.

---

## Lessons Learned (fuer mich selbst beim naechsten Mal)

| Datum | Erkenntnis |
|-------|-----------|
| 2026-04-29 | Frank arbeitet maus-zentriert — globale Hotkeys und Push-to-Talk haben bei ihm keinen Mehrwert. Erst klaeren welcher Workflow vorherrscht, bevor Tastatur-Loesungen vorgeschlagen werden. |
| 2026-04-29 | Starre Wort-Dictionaries (String-Replace) schleichen Fehler ein, wenn das "falsche" Wort auch ein echtes Wort sein kann. Statt dessen: Whisper `prompt`-Parameter als kontextsensitiver Hinweis. |
| 2026-04-29 | Auf Windows: `Get-Process -Name X` findet nicht immer alle Instanzen. `Get-CimInstance Win32_Process` ist robuster — besonders bei Apps mit Watchdog-Pattern (Haupt + Wacher). |
| 2026-04-29 | TVO und ClaudeVoiceOverlay-Windows teilen ~80% Code — bei Aenderungen IMMER beide updaten, sonst driften sie auseinander. |

---

## Wie dieser Plan gepflegt wird

- Frank entscheidet welcher Baustein als Naechstes drankommt — die Reihenfolge oben ist eine Empfehlung, kein Befehl.
- Erledigte Bausteine wandern in die "Bereits erledigt"-Tabelle ganz oben.
- Neue Ideen kommen zuerst ins **Backlog** und steigen nur auf, wenn sich konkreter Mehrwert zeigt.
- Diese Datei wird bei jeder relevanten Aenderung mit committed — sie ist Teil der App, nicht externe Doku.
