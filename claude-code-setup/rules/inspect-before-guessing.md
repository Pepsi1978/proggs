# Inspizieren statt Raten — IMMER zuerst den echten Zustand pruefen (KRITISCH)

## Regel

Wenn ein Problem mit UI-Erkennung, DOM-Struktur, Layout-Positionen oder
Eingabefeld-Targeting auftritt: IMMER ZUERST den echten Zustand inspizieren,
BEVOR Code geaendert wird.

## Warum

Am 2026-03-28 wurden 4 aufeinanderfolgende Fixes (#778-#781) fuer das Claude
Tampermonkey-Skript committed — jeder basierte auf falschen ANNAHMEN ueber die
DOM-Struktur. Erst als per Chrome DevTools die echte DOM-Struktur inspiziert wurde,
wurde die Root Cause in 30 Sekunden gefunden: Eingabefeld nur 22px hoch,
deutsches aria-label "Anfrage" statt "Nachricht", fieldset statt form.

Ein 30-Sekunden-DOM-Scan haette 4 fehlgeschlagene Fixes verhindert.

## Inspektions-Methoden nach Kontext

| Kontext | Inspektions-Methode |
|---------|---------------------|
| **Web/Browser** | `superpowers-chrome` Plugin → `eval` mit DOM-Query |
| **Electron-App** | Chrome DevTools Protocol (CDP) via `--remote-debugging-port` |
| **Desktop-App (Win32)** | `Spy++`, `Inspect.exe` (UI Automation), oder `Win32.EnumChildWindows` |
| **API/Netzwerk** | `curl` oder Playwright → tatsaechliche Response pruefen |
| **Dateisystem** | `ls`, `cat`, `hexdump` — nicht annehmen was drinsteht |
| **Prozesse** | `Get-Process`, `tasklist` — nicht annehmen was laeuft |

## Ablauf bei UI/DOM-Problemen

1. **STOP** — nicht sofort Code aendern
2. **Inspizieren** — echten Zustand sehen (DOM, Window, API-Response)
3. **Root Cause identifizieren** — basierend auf echten Daten, nicht Annahmen
4. **Einen Fix** — der auf den echten Daten basiert
5. **Verifizieren** — nach dem Fix nochmal inspizieren

## Typische falsche Annahmen (aus echten Fehlern)

| Annahme | Realitaet |
|---------|-----------|
| "contenteditable ist immer 'true'" | Claude nutzt 'plaintext-only' |
| "Das Eingabefeld ist mindestens 24px hoch" | Claude: 22px wenn leer |
| "aria-label ist auf Englisch" | Deutsche Claude-UI: "Schreiben Sie Ihre Anfrage" |
| "Das Eingabefeld ist in einem form-Tag" | Claude: fieldset, kein form |
| "Escape fokussiert das Eingabefeld" | In Claude Desktop schliesst Escape Panels |
| "Feste Koordinaten treffen das Eingabefeld" | Verschiedene Tabs haben verschiedene Layouts |
| "el.focus() aktiviert ProseMirror" | ProseMirror braucht mousedown+mouseup Events |

## Was NIEMALS passieren darf

- ❌ Annahmen ueber DOM-Struktur machen ohne sie zu pruefen
- ❌ Mehrere "Trial and Error"-Commits hintereinander ohne Inspektion
- ❌ "Das muesste so sein weil..." — pruefen statt vermuten
- ❌ Feste Koordinaten/Selektoren hardcoden ohne den echten Zustand gesehen zu haben
