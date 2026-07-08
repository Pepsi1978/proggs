# Observability-First: Sonden-, Logging- & Live-Monitoring-Standard (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-07. Gilt AUTOMATISCH in JEDER Session
> fuer JEDES qualifizierte Software-Projekt. Adressat ist **Claude Code selbst**. Verbindlicher
> Standard AUSSERHALB der geschuetzten 3-Direktiven-Trinitaet (#1 Superintelligenz,
> #2 Selbstbeobachtung, #3 Resilient Bugfixing). In `CLAUDE.md` referenziert, ins Repo gespiegelt.
> Zusatz-Direktive: `~/.claude/rules/observability-live-logic-probes.md` (Live-Logik-Sonden).

---

## 0. Geltungsbereich — ZUERST entscheiden

**Sonden einbauen (Direktive AKTIV), wenn mindestens eines zutrifft:**
- Mehr als eine Datei / ein Modul.
- App mit Oberflaeche (Windows-/macOS-Desktop, Android-App).
- Etwas, das wiederholt benutzt, ausgeliefert oder ueber Zeit gepflegt wird.
- Eigene Logik, eigener Zustand, Persistenz, Netzwerk- oder Datei-I/O.
- Faustregel: mehr als ~150 Zeilen oder mehr als eine Sitzung.

**Weglassen nur bei:** einmaligem Wegwerf-Skript, reinem Mini-Fix, kurzem Hilfsskript.
Im Zweifel: einbauen. Bewusstes Weglassen in **einem Satz** begruenden.

---

## 1. Kernprinzip

Bei einem qualifizierten Projekt ist der **allererste Schritt — vor jeder Feature-Arbeit — das
Aufsetzen der Beobachtungsschicht.** Nicht optional, nicht "spaeter nachgeruestet". Die Schicht
ist **lebendig**: mit jedem Commit weitergepflegt (siehe Abschnitt 4).

---

## 2. Was du einbaust — die 3 Sonden-Arten

**2.1 Strukturiertes Logging** — Format **JSON Lines** (ein Objekt pro Zeile), Felder mindestens:
`ts` (ISO), `level` (DEBUG/INFO/WARN/ERROR/FATAL), `module`+`fn`, `msg`, `ctx` (Zustand/Eingaben),
`trace` (Stacktrace bei Fehlern). Level per Env umschaltbar (Standard INFO). **Fester Log-Pfad**,
beim Start EINMAL ausgegeben (`Log: <Pfad>`). Log-Rotation. Zusaetzlich auf **stdout/stderr**
spiegeln (ermoeglicht Live-Tailing).

**2.2 Globaler Fehler-Faenger** — zentraler Handler fuer unbehandelte Ausnahmen, loggt vollen
Kontext BEVOR etwas abstuerzt. Grundsatz: **Nichts stirbt still.**

**2.3 Logik-Sonden (Herzstueck — fangen STILLE Fehler, nicht nur Abstuerze)** — pruefen eine
**Annahme** und protokollieren deren Verletzung, auch ohne Crash:
- Vor-/Nachbedingungen an Kernfunktions-Grenzen.
- Invarianten (was immer wahr sein muss).
- Zustandsuebergaenge (A → B) protokollieren.
- Sanity-/Range-Checks (NaN, negativ, ausserhalb Grenzen).
- Entscheidungs-Logging an Verzweigungen (falscher Pfad = stiller Logikfehler).

Umsetzung: Hilfsfunktion `probe(bedingung, meldung, kontext)`, die bei Verletzung WARN/ERROR mit
vollem Kontext schreibt, im Normalbetrieb aber **nicht** crasht.

> **Live-Logik-Sonden (Zusatz-Direktive `observability-live-logic-probes.md`):** aus jedem Bau-Prompt
> mit klarer Verhaltensabsicht werden **bestaetigende** Checkpoints verdrahtet, die live
> „erwartet vs. tatsaechlich" in einen eigenen Kanal (`kind:CHECKPOINT`) schreiben. Zuruf:
> „starte den Live-Logik-Check".

---

## 3. Live-Monitoring

Log-Stream mitlesen, waehrend Frank die App bedient, Anomalien mit seiner Aktion korrelieren:
- **Android:** `adb logcat -s FRANK_APP` (echtes logcat — am saubersten).
- **Windows:** `Get-Content <log> -Wait -Tail 20`.
- **macOS/Linux:** `tail -f <log>`.

Sprachunabhaengig: Python `logging`, Node `pino` o.ae. — entscheidend bleibt JSON-Lines + fester
ausgegebener Pfad + stdout-Spiegelung.

---

## 4. Lebende Sonden — Co-Evolution mit jedem Commit

Instrumentieren gehoert zur „fertig"-Definition JEDES Commits, der Logik aendert:
- **Neue Logik → neue Sonden**, **geaenderte Logik → Sonden anpassen**, **entfernte Logik → tote
  Sonden loeschen.**
- **Stale-Probe-Schutz:** eine Sonde zu geaenderter Annahme erzeugt Fehlalarme und untergraebt den
  Log-Workflow → bei jeder Logikaenderung betroffene Sonden mitziehen.
- **Vollabdeckung als Ziel:** jeder fachliche Schritt per Log debuggbar. Uninstrumentierte
  Bestandsstellen bei Beruehrung SOFORT nachruesten (nicht „spaeter", nicht „nur neue Logik").

**Zwei Zuruf-Hebel:**
- **„durchsuche das Log und fixe"** → Log parsen, Fehler nach Schwere priorisieren, je Fehler
  Root-Cause-Fix (Direktive #3), verifizieren, dass er weg ist — bis **zwei Durchlaeufe sauber**.
- **„auditiere die Sondenabdeckung"** → Logikpfade ohne Sonde + tote Sonden finden, melden, nachruesten.

---

## 5. Sicherheit & Selbst-Check

**Sicherheit:** Keine Secrets/Tokens/PII roh ins Log (maskieren). Log-Pfad in `.gitignore`.

**Selbst-Check vor „fertig" (pro Commit):** (a) Logschicht existiert, (b) ein **absichtlich
provozierter** Fehler landet mit Kontext im Log, (c) Live-Tail funktioniert, (d) neue Logik
instrumentiert, (e) betroffene Bestands-Sonden aktualisiert, (f) keine toten Sonden.

Konsistente Struktur ueber alle Projekte: gleiches `observability/`-Modul (bei kleinen Projekten
eine Datei), gleiches Log-Format, dokumentierter Pfad.

---

## Zusammenspiel & Verbote

Anwendung von Direktive #3 (Log liefert Root-Cause-Daten; Sonden = angewandte Poka-Yoke) und der
Debugging-Regel (Sonden VOR dem Raten). Grosse Logs nie ungefiltert in den Kontext — gezielt per
`grep`/`jq` (`lossless-context-principle.md`).

**Was NIEMALS passieren darf:**
- ❌ Bei qualifiziertem Projekt mit Features beginnen, BEVOR die Beobachtungsschicht steht.
- ❌ Einen Crash still sterben lassen (kein Log-Eintrag mit Kontext).
- ❌ Logik committen, ohne die Sonden mitzuziehen (Stale-Probe-Fehlalarme) / tote Sonden stehen lassen.
- ❌ Secrets/PII roh ins Log oder Log-Pfad nicht in `.gitignore`.
- ❌ Die Direktive bei qualifiziertem Projekt weglassen, ohne es in einem Satz zu begruenden.
