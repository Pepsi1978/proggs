# Observability-First: Sonden-, Logging- & Live-Monitoring-Standard (KRITISCH)

> Verbindlicher Standard (gesetzt 2026-06-07) AUSSERHALB der 3 Direktiven, Adressat Claude Code selbst.
> Enthaelt auch die Live-Logik-Sonden (Intent-Verifikation, §5 — frueher eigene Zusatz-Direktive).

## 0. Geltungsbereich — ZUERST entscheiden

Sonden einbauen, wenn mindestens eines zutrifft: mehr als eine Datei/ein Modul · App mit Oberflaeche
(Windows-/macOS-Desktop, Android) · wird wiederholt benutzt/ausgeliefert/gepflegt · eigene Logik/
Zustand/Persistenz/Netzwerk-/Datei-I-O · Faustregel >~150 Zeilen oder >1 Sitzung. Weglassen nur bei
Wegwerf-Skript / Mini-Fix / kurzem Hilfsskript (bewusstes Weglassen in EINEM Satz begruenden). Im Zweifel: einbauen.

## 1. Kernprinzip

Bei einem qualifizierten Projekt ist der ALLERERSTE Schritt — vor jeder Feature-Arbeit — das Aufsetzen
der Beobachtungsschicht (nicht optional, nicht "spaeter nachgeruestet"). Sie ist lebendig: mit jedem
Commit weitergepflegt (§4).

## 2. Die 3 Sonden-Arten

- **2.1 Strukturiertes Logging** — JSON Lines (ein Objekt/Zeile), Felder: `ts` (ISO), `level`
  (DEBUG/INFO/WARN/ERROR/FATAL), `module`+`fn`, `msg`, `ctx` (Zustand/Eingaben), `trace` (Stacktrace bei
  Fehlern). Level per Env umschaltbar (Standard INFO). Fester Log-Pfad, beim Start EINMAL ausgeben
  (`Log: <Pfad>`). Log-Rotation. Zusaetzlich auf stdout/stderr spiegeln (ermoeglicht Live-Tailing).
- **2.2 Globaler Fehler-Faenger** — zentraler Handler fuer unbehandelte Ausnahmen, loggt vollen Kontext
  BEVOR etwas abstuerzt. Grundsatz: **nichts stirbt still.**
- **2.3 Logik-Sonden (Herzstueck — fangen STILLE Fehler, nicht nur Abstuerze)** — pruefen eine Annahme
  und protokollieren deren Verletzung, auch ohne Crash: Vor-/Nachbedingungen an Kernfunktions-Grenzen ·
  Invarianten · Zustandsuebergaenge (A→B) · Sanity-/Range-Checks (NaN, negativ, out-of-bounds) ·
  Entscheidungs-Logging an Verzweigungen (falscher Pfad = stiller Logikfehler). Umsetzung:
  `probe(bedingung, meldung, kontext)` → schreibt bei Verletzung WARN/ERROR mit vollem Kontext, crasht im Normalbetrieb nie.

## 3. Live-Monitoring

Log-Stream mitlesen waehrend Frank die App bedient, Anomalien mit seiner Aktion korrelieren: Android
`adb logcat -s FRANK_APP` (echtes logcat, am saubersten) · Windows `Get-Content <log> -Wait -Tail 20` ·
macOS/Linux `tail -f <log>`. Sprachunabhaengig (Python `logging`, Node `pino` o.ae.) — entscheidend
bleibt JSON-Lines + fester ausgegebener Pfad + stdout-Spiegelung.

## 4. Lebende Sonden — Co-Evolution mit jedem Commit

Instrumentieren gehoert zur "fertig"-Definition JEDES Commits, der Logik aendert: neue Logik → neue
Sonden, geaenderte → anpassen, entfernte → tote Sonden loeschen. **Stale-Probe-Schutz:** eine Sonde zu
geaenderter Annahme erzeugt Fehlalarme → bei jeder Logikaenderung betroffene Sonden mitziehen. Ziel
Vollabdeckung (jeder fachliche Schritt per Log debuggbar); uninstrumentierte Bestandsstellen bei
Beruehrung SOFORT nachruesten. Zwei Zuruf-Hebel: **"durchsuche das Log und fixe"** (Log parsen, Fehler
nach Schwere priorisieren, je Fehler Root-Cause-Fix nach Direktive #3, bis **zwei Durchlaeufe sauber**) ·
**"auditiere die Sondenabdeckung"** (Logikpfade ohne Sonde + tote Sonden finden, melden, nachruesten).

## 5. Live-Logik-Sonden — Intent-Verifikation in Echtzeit

Ergaenzt §2.3 um **bestaetigende** Checkpoints: Logik-Sonden sind defensiv (schlagen bei
Annahme-Verletzung an), Live-Logik-Sonden bestaetigend — "ist die Logik so angekommen wie im Bau-Prompt
gemeint?", live beim ersten Start. Aus jedem Bau-Prompt mit klarer Verhaltensabsicht jede beabsichtigte
Verhaltensweise / jedes Akzeptanzkriterium ("die App soll …") als benannten Checkpoint an genau der
Stelle verdrahten, wo der Schritt passiert; er schreibt zur Laufzeit **erwartet vs. tatsaechlich** in
einen EIGENEN Kanal (getrennt vom Fehler-Log):
```
{"ts":"…","kind":"CHECKPOINT","step":"Rabatt berechnen","intent":"10% ab 3 Artikeln","expected":"0.10","actual":"0.00","ok":false,"ctx":{"items":4}}
```
**Live-Verifikations-Loop:** Frank startet die App → Checkpoint-Kanal streamen (`adb logcat -s LOGIC` /
`Get-Content <log> -Wait` / `tail -f`) → Frank bedient normal → Claude prueft jeden Checkpoint gegen die
Absicht (`ok:true` → "Schritt korrekt ✓"; `ok:false` → SOFORT melden + Root-Cause-Fix nach Direktive #3,
naechster Lauf erneut verifizieren). Aufgezeichnet wird die logische Substanz: Entscheidungen (welcher
Zweig), Berechnungen (Ergebnis vs. erwartet), Ablauf/Reihenfolge (uebersprungen?), Zustandsuebergaenge
gegen Spec, Ein-/Ausgaben an fachlichen Grenzen. Co-Evolution wie §4. Zuruf: **"starte den Live-Logik-Check"**.

## 6. Sicherheit & Selbst-Check

Keine Secrets/Tokens/PII roh ins Log (maskieren); Log-Pfad in `.gitignore`. Selbst-Check vor "fertig"
(pro Commit): (a) Logschicht existiert, (b) ein absichtlich provozierter Fehler landet mit Kontext im
Log, (c) Live-Tail funktioniert, (d) neue Logik instrumentiert, (e) betroffene Bestands-Sonden
aktualisiert, (f) keine toten Sonden. Ueber alle Projekte konsistent: gleiches `observability/`-Modul
(bei kleinen Projekten eine Datei), gleiches Log-Format, dokumentierter Pfad.

## Verbote

NIEMALS: bei qualifiziertem Projekt mit Features beginnen BEVOR die Beobachtungsschicht steht · einen
Crash still sterben lassen (kein Log-Eintrag mit Kontext) · Logik committen ohne die Sonden/Checkpoints
mitzuziehen (Stale-Probe-Fehlalarme / tote Sonden) · `ok:false` sehen und nicht sofort melden + an der
Wurzel fixen · Checkpoints in denselben Kanal wie das Fehler-Log mischen · Secrets/PII roh ins Log oder
Log-Pfad nicht in `.gitignore`. Grosse Logs nie ungefiltert in den Kontext (gezielt per `grep`/`jq`).
Anwendung von Direktive #3 (Sonden = angewandte Poka-Yoke) + Debugging-Regel (Sonden VOR dem Raten).
