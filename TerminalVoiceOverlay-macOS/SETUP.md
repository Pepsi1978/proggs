# TerminalVoiceOverlay-macOS — Setup nach Update vom 2026-04-30

> Diese Datei oeffnet sich automatisch nach dem ersten `bash build.sh`-Lauf,
> der die neuen Hotkey-Features enthaelt. Sobald du die unten beschriebenen
> Schritte einmal durchlaufen hast, wird sie nicht mehr automatisch geoeffnet
> (Marker: `~/.terminalvoiceoverlay-setup-seen`).

---

## Was sich geaendert hat

**Stand 2026-05-01:** Whisper bekommt kein Vokabel-Prompt mehr — der
Original-Whisper-Output geht direkt weiter (an Gemini, falls aktiv). Damit
greift jeder themenspezifische Sprachstil sauber, nicht nur Programmier-
Begriffe. Themenspezifische Profile werden kuenftig in Gemini abgebildet.

Es bleibt eine Config-Datei fuer Gemini:

- `~/SK/VoiceOverlays/gemini-correction-prompt.txt` — Gemini-Korrektur-Prompt
  (wirkt nur beim G-Button)

`build.sh` installiert dieses Template automatisch beim ersten Build, falls
es noch nicht vorhanden ist. Bestehende User-Anpassungen werden NIE
ueberschrieben. Eine eventuell vorhandene `~/SK/VoiceOverlays/voice-prompt.txt`
darf liegen bleiben — sie wird einfach nicht mehr gelesen.

**Stand 2026-04-30:** Die macOS-Version wurde mit allen Hotkey-Features der
Windows-Version gleichgezogen. Vorher konnte die App auf macOS nur per
Maus-Klick bedient werden — jetzt funktionieren globale Tastatur-Hotkeys
genau wie unter Windows.

| Feature | Status vor Update | Status jetzt |
|---------|------------------|--------------|
| Globale Tastatur-Hotkeys | ❌ keine | ✅ vollstaendig |
| Voice-Toggle per Hotkey | ❌ | ✅ Cmd+Shift+R |
| Screenshot per Hotkey | ❌ | ✅ Cmd+Shift+S |
| Insert-Screenshot per Hotkey | ❌ | ✅ Cmd+Shift+I |
| Finder-Shortcut zum Release-Bundle | ❌ | ✅ Cmd+Shift+E |
| Prompt-Hotkeys (1..9) | ❌ | ✅ Cmd+1 bis Cmd+9 |
| Hotkey-Picker im Edit-Dialog | ❌ | ✅ Dropdown "Kein"/"Cmd+1".."Cmd+9" |
| Hotkey-Badge auf Prompt-Cards | ❌ | ✅ "⌘N Title" |
| Backup-JSON-Kompatibilitaet zu Windows | Teilweise | ✅ HotkeyNumber wird gesynct |
| Reset-Timer-Race-Condition | ❌ Bug | ✅ gefixt |

---

## Hotkey-Uebersicht

| macOS | Aktion | Windows-Pendant |
|-------|--------|-----------------|
| **Cmd+Shift+R** | Voice-Aufnahme starten/stoppen | Alt+F12 |
| **Cmd+Shift+S** | Screenshot machen | Strg+Alt+P |
| **Cmd+Shift+I** | Letzten Screenshot-Pfad einfuegen | Strg+Alt+I |
| **Cmd+Shift+E** | Finder am Release-Bundle-Pfad oeffnen | Alt+F11 |
| **Cmd+1** bis **Cmd+9** | Den Prompt mit der jeweiligen Hotkey-Nummer einfuegen | Strg+1 bis Strg+9 |

Die Cmd+1..9 Hotkeys werden NUR aktiv wenn ein Prompt im PromptBoard
diese Nummer zugewiesen bekommen hat. Du weist Hotkeys im Edit-Dialog
zu (Stern-Button → Prompt rechtsklicken → "Bearbeiten" → Dropdown "Hotkey").

---

## Was du jetzt tun musst

### 1. App neu bauen

```bash
cd ~/proggs/TerminalVoiceOverlay-macOS
bash build.sh
```

`build.sh` ist Pflicht — `swiftc` direkt aufrufen funktioniert nicht, weil dann
TCC-Permissions (Accessibility) bei jedem Rebuild entfernt werden.

### 2. App starten

```bash
open ~/proggs/TerminalVoiceOverlay-macOS/build/TerminalVoiceOverlay.app
```

Falls die App schon laeuft: erst beenden (Status-Bar-Icon → Quit), dann neu
starten — die alten Hotkeys waeren sonst noch aktiv.

### 3. Permissions pruefen (falls noetig)

Die globalen Hotkeys nutzen die **Carbon-API** (`RegisterEventHotKey`). Diese
funktioniert OHNE zusaetzliche Permissions — die App bekommt die Hotkeys
direkt vom System geliefert.

ABER: Damit die App Text in Terminals einfuegen kann, braucht sie weiterhin
**Accessibility-Permission**. Falls die noch nicht erteilt ist:

1. Systemeinstellungen → Datenschutz & Sicherheit → Bedienungshilfen
2. TerminalVoiceOverlay.app finden, Schalter aktivieren
3. App einmal beenden + neu starten

Falls die Hotkeys NICHT feuern obwohl die App laeuft:

1. Pruefe Konflikte mit System-Hotkeys: Systemeinstellungen → Tastatur →
   Tastaturkurzbefehle. Wenn z.B. Cmd+Shift+S oder Cmd+Shift+I dort schon
   einer System-Aktion zugewiesen sind, gewinnt das System.
2. Console.app oeffnen, nach "TerminalVoiceOverlay" filtern. Beim App-Start
   sollte da stehen: `[App] Global hotkeys registered (Cmd+Shift+R/S/I/E + Cmd+1..9)`.
   Wenn ein einzelner Hotkey fehlschlaegt: `[HotkeyRegistry] FAILED keyCode=...`.

### 4. Hotkeys testen

| Test | Erwartetes Ergebnis |
|------|--------------------|
| Cmd+Shift+R druecken (irgendwo im System) | Aufnahme startet (Beep + rotes Mikrofon im Overlay) |
| Cmd+Shift+R nochmal | Aufnahme stoppt + Text wird in's aktive Terminal getippt |
| Cmd+Shift+S | Screenshot wird gemacht (Camera-Symbol blitzt im Overlay) |
| Cmd+Shift+I | Pfad zum letzten Screenshot wird eingefuegt |
| Cmd+Shift+E | Finder oeffnet `~/proggs/BestJournalAndroid/app/build/outputs/bundle/release` |

### 5. Prompt-Hotkeys konfigurieren

1. Im Overlay den **Stern-Button** klicken (oeffnet PromptBoard-Panel)
2. Einen Prompt mit Rechtsklick → "Bearbeiten"
3. Im Dialog ganz unten: Dropdown **"Hotkey:"** → "Cmd+1" auswaehlen
4. Speichern
5. Der Prompt zeigt jetzt im PromptBoard ein **⌘1**-Praefix vor dem Titel
6. Cmd+1 druecken → der Prompt-Text wird in's aktive Terminal getippt

**"Last wins"-Regel:** Wenn du einem zweiten Prompt Cmd+1 zuweist, verliert
der erste Prompt die Zuweisung automatisch. Jede Cmd+N-Kombi gehoert global
genau einem Prompt.

---

## Bekannte Limitierungen

- **Cmd+Shift+R** kann mit "Reload" in einigen Browsern oder IDEs kollidieren.
  Falls nervig: Hotkey-Konstanten in `HotkeyRegistry.swift` (`enum TVOHotkey`)
  anpassen, neu bauen.
- Die Hotkeys feuern systemweit — auch wenn du gerade in einem Spiel oder
  einer Vollbild-App bist. Das ist meistens das was du willst, aber bei
  Spielen die Cmd+Shift+R/S/I/E selber benutzen kann es Konflikte geben.
  Workaround: TerminalVoiceOverlay vor dem Spielen kurz beenden (Status-Bar
  → Quit), nach dem Spielen wieder starten.

---

## Falls etwas nicht funktioniert

- **Hotkey feuert gar nicht:** Console.app → Filter "TerminalVoiceOverlay"
  → Logs lesen. Sollte `[HotkeyRegistry] registered keyCode=... id=...`
  beim App-Start erscheinen. Wenn `FAILED status=-9878` — Hotkey ist schon
  von einer anderen App belegt.
- **Hotkey feuert aber nichts passiert:** Pruefe ob die App im Vordergrund
  IRGENDEIN Terminal hat (iTerm, Terminal.app, Warp). Ohne Terminal-Window
  hat die Paste-Aktion kein Ziel.
- **Cmd+1..9 macht nichts:** Du hast noch keinem Prompt eine Hotkey-Nummer
  zugewiesen. Siehe Schritt "Prompt-Hotkeys konfigurieren" oben.
- **Die App stuerzt beim Start ab:** Vermutlich SQLite-Migration-Problem.
  Backup deiner DB an `~/Library/Application Support/TerminalVoiceOverlay/`
  machen, dann die DB loeschen — die App legt sie beim naechsten Start
  frisch an. ALTERNATIV: Drive-Backup von Windows einspielen, dann ist
  alles synchron.

---

## Cross-Platform-Kompatibilitaet

Wenn du auf Windows einen Prompt-Hotkey setzt, wird er beim naechsten
Google-Drive-Backup (passiert automatisch) auf den Mac mitgenommen. Umgekehrt
genauso. Das `HotkeyNumber`-Feld ist in der SQLite-DB und im JSON-Backup
auf beiden Plattformen identisch — du kannst dieselbe DB-Datei auf beiden
Geraeten benutzen.
