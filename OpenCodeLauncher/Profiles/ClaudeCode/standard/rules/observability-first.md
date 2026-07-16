# Observability-First: Sonden-, Logging- & Live-Monitoring-Standard (KRITISCH)

> Fuer JEDES qualifizierte Software-Projekt. **Volltext: `claude-code-setup/docs/rules/observability-first.md`.**

## Geltungsbereich
Sonden einbauen bei: >1 Datei/Modul · App mit Oberflaeche · wird ausgeliefert/gepflegt · eigene
Logik/Zustand/Persistenz/I-O · >~150 Zeilen. Weglassen nur bei Wegwerf-Skript/Mini-Fix (in 1 Satz begruenden).

## Kernprinzip
ALLERERSTER Schritt vor jeder Feature-Arbeit = Beobachtungsschicht. Lebendig: jeder Logik-Commit zieht
die Sonden mit (neu→neu, geaendert→anpassen, entfernt→loeschen; Stale-Probe-Schutz).

## Die 3 Sonden-Arten
1. **Strukturiertes Logging** — JSON Lines (`ts`, `level`, `module`+`fn`, `msg`, `ctx`, `trace`), fester
   Log-Pfad beim Start EINMAL ausgeben, Rotation, stdout-Spiegelung.
2. **Globaler Fehler-Faenger** — Ausnahmen loggen vollen Kontext BEVOR etwas abstuerzt (nichts stirbt still).
3. **Logik-Sonden (Herzstueck, STILLE Fehler)** — `probe(bedingung, meldung, kontext)`: Vor-/Nachbedingungen,
   Invarianten, Zustandsuebergaenge, Sanity-Checks. Zusatz: Live-Logik-Checkpoints (erwartet vs.
   tatsaechlich, "starte den Live-Logik-Check"). Zuruf "durchsuche das Log und fixe".

## Was NIEMALS
- Features beginnen BEVOR die Beobachtungsschicht steht · Crash still sterben lassen · Logik ohne Sonden
  committen · Secrets/PII roh ins Log · Log-Pfad nicht in `.gitignore`.
