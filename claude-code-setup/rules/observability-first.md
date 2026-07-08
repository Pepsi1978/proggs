# Observability-First: Sonden-, Logging- & Live-Monitoring-Standard (KRITISCH)

> Verbindlicher Standard fuer JEDES qualifizierte Software-Projekt (Adressat Claude Code selbst).
> **Volltext (Live-Monitoring-Befehle, Live-Logik-Sonden §5, Selbst-Check):
> `claude-code-setup/docs/rules/observability-first.md`.**

## Geltungsbereich
Sonden einbauen, wenn mind. eines zutrifft: >1 Datei/Modul · App mit Oberflaeche · wird wiederholt
benutzt/ausgeliefert/gepflegt · eigene Logik/Zustand/Persistenz/I-O · >~150 Zeilen oder >1 Sitzung.
Weglassen nur bei Wegwerf-Skript/Mini-Fix (in EINEM Satz begruenden). Im Zweifel: einbauen.

## Kernprinzip
Bei qualifiziertem Projekt ist der ALLERERSTE Schritt — vor jeder Feature-Arbeit — die Beobachtungsschicht
(nicht "spaeter nachgeruestet"). Lebendig: mit jedem Logik-Commit die Sonden mitziehen (neue → neue,
geaenderte → anpassen, entfernte → loeschen; Stale-Probe-Schutz gegen Fehlalarme).

## Die 3 Sonden-Arten
1. **Strukturiertes Logging** — JSON Lines (`ts`, `level`, `module`+`fn`, `msg`, `ctx`, `trace`), fester
   Log-Pfad beim Start EINMAL ausgeben (`Log: <Pfad>`), Rotation, stdout-Spiegelung, Level per Env.
2. **Globaler Fehler-Faenger** — unbehandelte Ausnahmen loggen vollen Kontext BEVOR etwas abstuerzt (nichts stirbt still).
3. **Logik-Sonden (Herzstueck, fangen STILLE Fehler)** — `probe(bedingung, meldung, kontext)`: Vor-/
   Nachbedingungen, Invarianten, Zustandsuebergaenge, Sanity-Checks, Entscheidungs-Logging. Zusatz:
   Live-Logik-Checkpoints (erwartet vs. tatsaechlich, eigener Kanal `kind:CHECKPOINT`) — Zuruf "starte den Live-Logik-Check".

Zuruf-Hebel: "durchsuche das Log und fixe" (Root-Cause-Fix bis 2 saubere Durchlaeufe) · "auditiere die Sondenabdeckung".

## Was NIEMALS
- Bei qualifiziertem Projekt mit Features beginnen BEVOR die Beobachtungsschicht steht · einen Crash still
  sterben lassen · Logik committen ohne Sonden mitzuziehen · Secrets/PII roh ins Log · Log-Pfad nicht in `.gitignore`.
