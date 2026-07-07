# Windows-Tastenkombinationen (Referenz)

> Nachschlage-Sammlung wichtiger Tastenkombinationen unter Windows 11 — plus die
> **eigenen Hotkeys** der Voice-Overlays (TVO), die leicht vergessen werden.
> Zweck: Kombinationen mit **genauer Bezeichnung** festhalten, damit nichts wieder
> muehsam gesucht werden muss.
>
> **Stand:** 2026-06-21 · Plattform: Windows 11 (Frank: Stream Deck XL, Logitech G Hub)

---

## ⚡ Kurzcheck — die wichtigsten zuerst

| Was ich will | Kombination | Hinweis |
|--------------|-------------|---------|
| **Vollbild-Screenshot + sofort in CLI einfuegen** (alle Monitore) | **`Strg + Alt + P`** | Eigener TVO-Hotkey — macht alles in EINEM Druck. TVO muss laufen. |
| Nur den letzten Screenshot einfuegen | **`Strg + Alt + I`** | Eigener TVO-Hotkey |
| Vollbild → Zwischenablage (Windows pur) | `Druck` (`PrtScn`) | NUR wenn Snipping-Tool-Umstellung AUS ist |
| Aktives Fenster → Zwischenablage | `Alt + Druck` | Oeffnet nie das Snipping-Tool |
| Vollbild → als Datei | `Win + Druck` | Speichert in `Bilder\Screenshots` |
| Bereichs-Screenshot (Snipping-Tool) | `Win + Shift + S` | Interaktive Auswahl |

---

## 1. Eigene Hotkeys — Terminal Voice Overlay (TVO)

Diese Kombinationen sind **nicht von Windows**, sondern vom **Terminal Voice Overlay**
(`TerminalVoiceOverlay-Windows`) per globalem Tastatur-Hook registriert. Sie funktionieren
nur, solange das TVO laeuft. Quelle im Code: `Views/OverlayWindow.xaml.cs` (Hook-Handler).

| Kombination | Genaue Bezeichnung | Was sie macht |
|-------------|--------------------|---------------|
| **`Strg + Alt + P`** | One-Shot Screenshot + Insert | Vollbild ueber **alle Monitore** (`VirtualScreen`), speichert als PNG in `Bilder\Screenshots`, und fuegt den Pfad **sofort** in die aktive CLI ein. Genau ein Druck. |
| **`Strg + Alt + I`** | Insert Screenshot | Fuegt nur den **zuletzt** aufgenommenen Screenshot erneut ein (ohne neuen zu machen). |

**Stream-Deck-Einrichtung:** „Hotkey"-Aktion (Kategorie *System*) → `Strg + Alt + P` hinterlegen.
Ein Tastendruck = identisch zur Logitech-G4-Makrotaste. Kein Skript, keine Multiaktion noetig.

**Logitech G Hub:** Die G4-Maustaste ist als Makro hinterlegt, das `Strg + Alt + P` sendet.

---

## 2. Screenshots (Windows-Bordmittel)

| Kombination | Was sie macht | Snipping-Tool? |
|-------------|---------------|----------------|
| `Druck` / `PrtScn` | Gesamter Bildschirm → Zwischenablage | Unter Win11 standardmaessig auf Snipping-Tool umgebogen* |
| `Alt + Druck` | Aktives Fenster → Zwischenablage | Nein |
| `Win + Druck` | Gesamter Bildschirm → als Datei in `Bilder\Screenshots` | Nein |
| `Win + Shift + S` | Bereichs-/Fenster-/Vollbild-Auswahl → Zwischenablage | Ja (das ist das Snipping-Tool) |
| `Win + Shift + R` | Bildschirmaufnahme (Video) starten (Snipping-Tool) | Ja |

> *\* Umstellen unter:* **Einstellungen → Barrierefreiheit → Tastatur →**
> „Bildschirmtaste verwenden, um Bildschirmaufnahme zu oeffnen". Ist sie **AUS**, kopiert
> `Druck` wieder direkt das Vollbild in die Zwischenablage (Verhalten wie Windows 10).
> Es gibt KEINE separate zweite Kombi fuer Vollbild-in-Zwischenablage — es ist genau diese Taste.

---

## 3. Fenster & Desktop

| Kombination | Was sie macht |
|-------------|---------------|
| `Win + D` | Desktop anzeigen / alle Fenster minimieren (Toggle) |
| `Win + Pfeil links/rechts` | Fenster an den linken/rechten Bildschirmrand andocken (Snap) |
| `Win + Pfeil hoch/runter` | Fenster maximieren / minimieren |
| `Win + Shift + Pfeil links/rechts` | Fenster auf den anderen Monitor verschieben |
| `Win + Tab` | Task-Ansicht (alle Fenster + virtuelle Desktops) |
| `Alt + Tab` | Zwischen offenen Fenstern wechseln |
| `Win + Strg + D` | Neuen virtuellen Desktop erstellen |
| `Win + Strg + Pfeil links/rechts` | Zwischen virtuellen Desktops wechseln |
| `Win + Strg + F4` | Aktuellen virtuellen Desktop schliessen |
| `Win + Z` | Snap-Layouts oeffnen |

---

## 4. System & Werkzeuge

| Kombination | Was sie macht |
|-------------|---------------|
| `Win + L` | Bildschirm sperren |
| `Win + E` | Datei-Explorer oeffnen |
| `Win + I` | Einstellungen oeffnen |
| `Win + R` | „Ausfuehren"-Dialog |
| `Win + X` | Power-User-Menue (Geraete-Manager, PowerShell, etc.) |
| `Win + V` | Zwischenablage-Verlauf (mehrere kopierte Elemente) |
| `Win + .` (Punkt) | Emoji- / Symbol-Auswahl |
| `Strg + Shift + Esc` | Task-Manager direkt oeffnen |
| `Win + Pause` | Systeminfo (Info zu diesem Geraet) |
| `Win + +` / `Win + -` | Bildschirmlupe ein / aus |

---

## 5. Text & Bearbeiten (universell)

| Kombination | Was sie macht |
|-------------|---------------|
| `Strg + C` / `Strg + V` / `Strg + X` | Kopieren / Einfuegen / Ausschneiden |
| `Strg + Z` / `Strg + Y` | Rueckgaengig / Wiederholen |
| `Strg + A` | Alles markieren |
| `Strg + Shift + V` | Einfuegen ohne Formatierung (in vielen Apps) |
| `Strg + F` | Suchen |
| `Strg + Pfeil links/rechts` | Wortweise springen |
| `Pos1` / `Ende` | Zeilenanfang / Zeilenende |

---

## Quellen

- Microsoft — Screenshots unter Windows 11: `microsoft.com/en-us/windows/learning-center/how-to-screenshot-windows-11`
- Microsoft Q&A — Vollbild in Zwischenablage wie Win10: `learn.microsoft.com/en-us/answers/questions/5703328/`
- Eigener Code: `TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs` (Hotkey-Hook, `Strg+Alt+P` / `Strg+Alt+I`)
