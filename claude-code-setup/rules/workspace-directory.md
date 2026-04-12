# Arbeitsverzeichnis: IMMER ~/proggs/ (KRITISCH)

## Regel

Das Arbeitsverzeichnis fuer Claude Code ist IMMER `C:\Users\barwa\proggs` (Windows)
bzw. `/Users/barwa/proggs` (macOS). NIEMALS `C:\Users\barwa` oder `/Users/barwa`.

## Warum

Das Home-Verzeichnis (`~`) ist NICHT das Arbeitsverzeichnis. Das Repository
`Pepsi1978/proggs` liegt in `~/proggs/`. Alle Projekte, alle Dateien, alle
Git-Operationen gehoeren dorthin. In `~` zu starten bedeutet im falschen Ordner zu sein.

## Was beim Session-Start passieren muss

1. **Pruefen**: Ist das aktuelle Verzeichnis `~/proggs/`?
2. **Wenn NEIN**: Sofort dem Benutzer melden und alle Operationen auf `~/proggs/` beziehen
3. **Alle Pfade**: Immer relativ zu `~/proggs/`, nie relativ zu `~/`

## Was NIEMALS passieren darf

- ❌ In `C:\Users\barwa` starten und dort arbeiten
- ❌ Dateien relativ zum Home-Verzeichnis statt zum Repo suchen
- ❌ Git-Befehle im Home-Verzeichnis ausfuehren (dort ist kein Repo!)
- ❌ Stillschweigend im falschen Verzeichnis arbeiten ohne den Benutzer zu warnen

## Technische Absicherung

Windows Terminal ist konfiguriert mit `startingDirectory: C:\Users\barwa\proggs`
fuer alle Profile. Wenn trotzdem ein falsches Verzeichnis erkannt wird, ist das
ein Konfigurationsfehler der sofort gemeldet werden muss.
