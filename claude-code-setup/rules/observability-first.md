# Observability-First: Sonden-, Logging- & Live-Monitoring-Standard (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-07. Gilt AUTOMATISCH in JEDER Session
> fuer JEDES qualifizierte Software-Projekt (siehe Abschnitt 0). Adressat dieser Direktive ist
> **Claude Code selbst**. Verbindlicher Standard AUSSERHALB der geschuetzten 3-Direktiven-Trinitaet
> (#1 Superintelligenz, #2 Selbstbeobachtung, #3 Resilient Bugfixing) — in `CLAUDE.md` referenziert,
> ins Repo gespiegelt (`claude-code-setup/rules/observability-first.md`).
>
> Eng verbunden mit Direktive #3 (Resilient Bugfixing: Root-Cause + Logik-Sonden) und der
> Debugging-Regel (`debugging-and-verification.md`, Stufe 2: Sonden VOR dem Raten einbauen).
>
> **Zusatz-Direktive:** [[observability-live-logic-probes]]
> (`~/.claude/rules/observability-live-logic-probes.md`) erweitert Abschnitt 2.3 und 4 um
> **Live-Logik-Sonden (Intent-Verifikation)** — benannte Checkpoints, die live „erwartet vs.
> tatsaechlich" melden und damit bestaetigen, ob die Logik so umgesetzt wurde wie im
> Bau-Prompt gemeint (nicht nur „ist etwas kaputt?", sondern „ist die Logik richtig angekommen?").

---

## 0. Geltungsbereich — ZUERST entscheiden

Diese Direktive gilt **verpflichtend fuer jedes Software-Projekt, das ueber einen Mini-Fix hinausgeht**. Bevor du irgendetwas programmierst, klassifizierst du die Aufgabe:

**Sonden einbauen (Direktive AKTIV), wenn mindestens eines zutrifft:**
- Mehr als eine Datei / ein Modul.
- Eine App mit Oberflaeche (Windows-Desktop, macOS-Desktop, Android-App auf dem Handy).
- Etwas, das Frank wiederholt benutzt, ausliefert oder ueber Zeit pflegt.
- Eigene Logik, eigener Zustand, Persistenz, Netzwerk- oder Datei-I/O.
- Faustregel: mehr als ~150 Zeilen oder mehr als eine Entwicklungssitzung.

**Sonden weglassen (Direktive AUS), wenn:**
- Einmaliges Wegwerf-Skript mit genau einer Aufgabe.
- Reiner Mini-Fix an bestehendem Code.
- Kurzes Hilfsskript, das nichts dauerhaft betreibt.

Im Zweifel zugunsten der Sonden entscheiden oder kurz nachfragen. Wenn du die Direktive bewusst weglaesst, sag in **einem Satz**, warum.

---

## 1. Kernprinzip: Observability-First

Bei einem qualifizierten Projekt ist der **allererste Entwicklungsschritt — vor jeder Feature-Arbeit — das Aufsetzen der Beobachtungsschicht.**
Features kommen erst, wenn das Projekt sich selbst beim Arbeiten beobachten kann. Das ist nicht optional und wird nicht "spaeter nachgeruestet".

Genauso wichtig: Diese Schicht ist **lebendig, nicht statisch.** Sie wird einmal aufgesetzt, aber danach **mit jedem Commit weitergepflegt** — neue Logik bringt neue Sonden, geaenderte Logik aktualisiert bestehende Sonden (siehe Abschnitt 6).

---

## 2. Was du einbaust — die Sonden

### 2.1 Strukturiertes Logging
- **Format: JSON Lines** (ein JSON-Objekt pro Zeile). Maschinen- und KI-lesbar, durchsuchbar per `grep`/`jq`, ideal um spaeter gezielt Fehler herauszuziehen.
- Jeder Eintrag enthaelt mindestens:
  - `ts` — ISO-Zeitstempel
  - `level` — DEBUG / INFO / WARN / ERROR / FATAL
  - `module` und `fn` — Modul + Funktion
  - `msg` — kurze Beschreibung
  - `ctx` — relevanter Zustand / Eingaben zum Zeitpunkt des Ereignisses
  - `trace` — bei Fehlern der vollstaendige Stacktrace
- Log-Level per Konfiguration / Umgebungsvariable umschaltbar (Standard: INFO; DEBUG bei Bedarf).
- **Fester, dokumentierter Log-Pfad**, der beim Start **einmal ausgegeben** wird (`Log: <Pfad>`). So weiss Frank immer, worauf er die KI ansetzen kann.
- **Log-Rotation** (nach Groesse und/oder Datum), damit die Datei nicht unbegrenzt waechst.
- Logs zusaetzlich auf **stdout/stderr** spiegeln → das ermoeglicht das Live-Tailing (Abschnitt 4).

### 2.2 Globaler Fehler-Faenger
- Ein zentraler Handler fuer unbehandelte Ausnahmen / Crashes, der den **vollstaendigen Kontext loggt, bevor** etwas abstuerzt.
- Grundsatz: **Nichts stirbt still.** Jeder Absturz hinterlaesst einen aussagekraeftigen Log-Eintrag.

### 2.3 Logik-Sonden — das Herzstueck (faengt auch Logikfehler, nicht nur Abstuerze)
Eine Logik-Sonde prueft eine **Annahme** und protokolliert deren Verletzung — auch wenn gar kein Crash passiert. Genau hier entstehen die stillen Bugs:
- **Vor- und Nachbedingungen** an den Grenzen der Kernfunktionen (Was muss reingehen? Was muss rauskommen?).
- **Invarianten**: Dinge, die immer wahr sein muessen (z. B. „Liste darf hier nie leer sein", „Summe == erwarteter Wert").
- **Zustandsuebergaenge** protokollieren (Zustand A → B), damit man im Nachhinein sieht, *warum* die App etwas Falsches getan hat.
- **Sanity-/Range-Checks** auf Werte (NaN, negativ, ausserhalb der erlaubten Grenzen).
- **Entscheidungs-Logging** an wichtigen Verzweigungen — gerade dort, wo „der falsche Pfad" ein stiller Logikfehler waere.

**Praktische Umsetzung:** eine Hilfsfunktion `probe(bedingung, meldung, kontext)`, die bei Verletzung ein WARN/ERROR mit vollem Kontext schreibt (per Schalter optional auch hart abbrechbar), im Normalbetrieb aber **nicht** crasht.

> **Erweiterung — Live-Logik-Sonden (Intent-Verifikation):** Die obigen Sonden sind
> *defensiv* (schlagen bei Annahme-Verletzung an). Zusaetzlich werden bei einem Bau-Prompt
> mit klarer Verhaltensabsicht **bestaetigende** Checkpoints verdrahtet, die zur Laufzeit
> „erwartet vs. tatsaechlich" in einen eigenen Kanal (`kind:CHECKPOINT`) schreiben — damit
> live pruefbar ist, ob jeder fachliche Schritt so umgesetzt wurde wie gemeint. Vollstaendig:
> [[observability-live-logic-probes]] (`~/.claude/rules/observability-live-logic-probes.md`).

---

## 3. Plattformspezifik

### Android (Handy)
- `android.util.Log` mit **einem festen TAG** (z. B. `FRANK_APP`), optional Timber. Zusaetzlich in eine JSON-Lines-Datei im App-Speicher schreiben.
- **Live:** `adb logcat -s FRANK_APP` — das ist logcat. Frank fuehrt Aktionen auf dem Handy aus, du siehst sie sofort im Terminal.

### Windows
- JSON-Lines-Logdatei + stdout.
- **Live:** PowerShell `Get-Content -Path <log> -Wait -Tail 20` (passt direkt zu Franks PowerShell- und Stream-Deck-Setup).

### macOS / Linux
- JSON-Lines-Logdatei + stdout. Optional `os_log` / unified logging auf macOS.
- **Live:** `tail -f <log>`.

> Sprachunabhaengig: Python `logging`, Node `pino` oder eigener Logger usw. — entscheidend ist immer: **JSON-Lines + fester, ausgegebener Pfad + stdout-Spiegelung.**

---

## 4. Live-Monitoring-Loop

**Ehrliche Einordnung:** Du (Claude Code) arbeitest zugbasiert — du reagierst, wenn du aufgerufen wirst, nicht permanent im Leerlauf. Aber innerhalb einer Debug-Sitzung entsteht ein echter Live-Loop:

1. Du startest einen Log-Stream / eine Aufzeichnung — je nach Plattform `adb logcat`, `tail -f` oder `Get-Content -Wait`.
2. Frank sagt „ich teste jetzt Funktion X" und bedient die App / das Handy.
3. Du liest die frisch aufgelaufenen Zeilen, erkennst Anomalien, sobald sie im Stream erscheinen, und korrelierst sie direkt mit Franks Aktion.

Am saubersten funktioniert das auf **Android (echtes logcat)**. Auf dem Desktop erreichst du dasselbe, indem du die App ans Terminal haengst oder die Live-Logdatei tailst.

> Optionaler Ausbau fuer spaeter (nur bei Bedarf, nicht ueberkonstruieren): Die App stellt einen winzigen lokalen Debug-Endpunkt oder eine Live-Event-Datei bereit, die du in Echtzeit mitliest.

---

## 5. Nachtraegliches Fixen — „durchsuche die Logdatei und fixe alle Probleme"

Wenn Frank sagt „durchsuche die Logdatei der App und fixe alle erkannten Probleme":

1. Logdatei am bekannten Pfad einlesen, JSON-Lines parsen.
2. Fehler nach Typ und Haeufigkeit gruppieren, nach Schwere priorisieren.
3. Fuer jeden Fehler die **Ursache** finden — kein Symptom-Pflaster, sondern Root-Cause-Fix (Direktive #3).
4. Fix umsetzen, dann **verifizieren**, dass genau dieser Fehler im Log nicht mehr auftaucht.
5. Erst aufhoeren, wenn **zwei aufeinanderfolgende Durchlaeufe sauber** sind.

---

## 6. Lebende Sonden — Co-Evolution mit jedem Commit

Die Beobachtungsschicht ist **kein einmaliges Geruest, das am Projektanfang steht und danach unveraendert bleibt.** Eine Software waechst ueber Dutzende bis Hunderte Commits, und jeder Commit kann neue Logik mitbringen. Das Sondensystem **waechst und altert mit dem Code mit.**

**Regel: Instrumentieren gehoert zu jeder Aufgabe dazu — als Teil der „fertig"-Definition des jeweiligen Commits, nicht als optionaler Extra-Schritt.**

Bei jedem Entwicklungsschritt, der Logik oder Funktionalitaet hinzufuegt oder aendert:

1. **Neue Logik → neue Sonden.** Jede neue Kernfunktion, jeder neue Zustandsuebergang, jede neue Verzweigung, jedes neue I/O bekommt passende Sonden (Vor-/Nachbedingungen, Invarianten, Sanity-Checks, Entscheidungs-Logging) — mit demselben Anspruch wie der urspruengliche Code.
2. **Geaenderte Logik → Sonden anpassen.** Aendert sich eine Annahme, wird die zugehoerige Sonde mitgezogen.
3. **Entfernte Logik → tote Sonden loeschen.** Sonden, deren Code es nicht mehr gibt, werden entfernt.
4. **Abdeckung waechst mit.** Ziel: Kein wichtiger Logikpfad bleibt unbeobachtet — egal, wie viele Commits dazukommen.
5. **Vollabdeckung als Pflicht — Bestandscode aktiv nachruesten (Frank-Direktive 2026-06-20).** Das Ziel ist nicht „die meisten" Logikpfade, sondern: **die App ist UEBERALL besondet — jeder fachliche Schritt ist im Nachhinein per Log debuggbar.** Stoesst man bei der Arbeit auf eine **uninstrumentierte Bestandsstelle** (kein Wegwerf-Code), wird sie **sofort mit nachgeruestet** — nicht „spaeter", nicht „nur die neue Logik". Die Live-Logik-Sonden (Abschnitt 2.3 + [[observability-live-logic-probes]]) decken dabei genau die stillen Fehler ab, die ohne Sonde unsichtbar bleiben. **Begruendung (Vorfall 2026-06-20):** Beim ID-Architektur-Live-Check war das Herz (`GenerateSuggestionsUseCase`) komplett uninstrumentiert (0 Logs) — erst die nachgeruesteten Checkpoint-Sonden machten den Live-Logik-Check moeglich und deckten eine echte, zentrale Multi-Device-Lecke auf (Herkunft ging im Backup verloren). Ohne Vollabdeckung waere die Lecke unentdeckt geblieben.

### Stale-Probe-Schutz (wichtig!)
Veraltete Sonden, die eine laengst geaenderte Annahme pruefen, erzeugen **Fehlalarme** im Log (False Positives). Das untergraebt genau den „durchsuche-das-Log-und-fixe"-Workflow aus Abschnitt 5, weil echte Fehler im Rauschen untergehen. Deshalb gilt: Bei jeder Logikaenderung **aktiv pruefen, welche bestehenden Sonden betroffen sind**, und sie mitziehen. Ein sauberes Log ist nur dann aussagekraeftig, wenn alle Sonden zur **aktuellen** Logik passen.

### Auf Zuruf: Sonden-Audit
Zusaetzlich zum laufenden Mitziehen kann Frank jederzeit einen Audit ausloesen — z. B. „auditiere die Sondenabdeckung". Dann:
1. Codebasis durchgehen und Logikpfade **ohne** Sonde finden.
2. Veraltete / tote Sonden identifizieren.
3. Beides melden und — nach kurzer Abstimmung — nachruesten bzw. aufraeumen.

Damit gibt es **zwei klare Hebel**, die Frank per Zuruf zieht:
- **„durchsuche das Log und fixe"** → behebt aufgetretene Fehler (Laufzeit-Loop, Abschnitt 5).
- **„auditiere die Sondenabdeckung"** → haelt das Sondensystem selbst aktuell und vollstaendig (Entwicklungs-Loop, dieser Abschnitt).

---

## 7. Verankerung als dauerhafter Standard

Damit das bei **jedem** qualifizierten Projekt automatisch befolgt wird:
- Diese Regel ist in `~/.claude/rules/observability-first.md` verankert (immer geladen) und in der **`CLAUDE.md`** referenziert — verbindlicher Bootstrap-Schritt (verankert 2026-06-07).
- Die Beobachtungsschicht ist immer der **erste** Schritt — vor Features.
- **Konsistente Struktur ueber alle Projekte:** gleiches Observability-Modul (z. B. Ordner `observability/`, bei kleineren Projekten eine einzelne Datei wie `logging_setup.py`), gleiches Log-Format, dokumentierter Pfad.
- **Selbst-Check vor „fertig" — gilt pro qualifiziertem Commit, nicht nur am Projektanfang:** Beweise dir selbst, dass
  (a) die Logschicht existiert,
  (b) ein **absichtlich provozierter** Fehler korrekt mit Kontext im Log landet,
  (c) der Live-Tail funktioniert,
  (d) **neue Logik dieses Commits instrumentiert** ist,
  (e) **von der Aenderung betroffene Bestands-Sonden aktualisiert** wurden,
  (f) **keine toten Sonden** (zu geloeschtem Code) uebrig sind.
  Erst dann gilt der Commit als sauber instrumentiert.

---

## 8. Sicherheit beim Loggen
- Niemals Geheimnisse, Tokens, Passwoerter oder personenbezogene Daten roh ins Log schreiben — sensible Felder maskieren/redacten.
- Log-Pfad in die `.gitignore` aufnehmen, damit keine Logs versehentlich nach GitHub wandern.

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| Direktive #3 (`resilient-bugfixing.md`) | Log-Auswertung (Abschnitt 5) liefert die Daten fuer Root-Cause-Fixes; Logik-Sonden sind angewandte Poka-Yoke (stille Fehler sichtbar machen) |
| `debugging-and-verification.md` (Hypothesen-Debugging) | Sonden VOR dem Raten einbauen (Stufe 2) ist die Laufzeit-Anwendung dieser Direktive |
| `secrets-in-sk-folder.md` / `git-workflow.md` (Secrets) | Abschnitt 8 ergaenzt: Logs nie mit Secrets, Log-Pfad in `.gitignore` |
| `lossless-context-principle.md` | Grosse Logs nie ungefiltert in den Kontext laden — per Pfad gezielt `grep`/`jq` (verlustfrei) |
| [[observability-live-logic-probes]] (Zusatz-Direktive) | Erweitert Abschnitt 2.3 + 4 um bestaetigende Intent-Checkpoints (`erwartet vs. tatsaechlich`) und den Live-Verifikations-Loop „ist die Logik so angekommen wie gemeint?" |

---

## Was NIEMALS passieren darf

- ❌ Bei einem qualifizierten Projekt mit Features beginnen, BEVOR die Beobachtungsschicht steht
- ❌ Einen Crash still sterben lassen (kein Log-Eintrag mit Kontext)
- ❌ Neue/geaenderte Logik committen, ohne die Sonden mitzuziehen (Stale-Probe-Fehlalarme)
- ❌ Tote Sonden zu geloeschtem Code stehen lassen
- ❌ Secrets/PII roh ins Log schreiben oder den Log-Pfad nicht in `.gitignore` aufnehmen
- ❌ Die Direktive bei einem qualifizierten Projekt weglassen, ohne in einem Satz zu begruenden warum

---

**Kurzfassung:** Bei jedem echten Software-Projekt (nicht bei Mini-Fixes) baust du als allererstes eine Beobachtungsschicht ein — strukturiertes JSON-Logging, globalen Fehler-Faenger und Logik-Sonden fuer stille Fehler — mit festem, ausgegebenem Log-Pfad. Du ermoeglichst Live-Mitschnitt per logcat / tail / Get-Content. Auf Zuruf durchsuchst du das Log und behebst Fehler an der Wurzel. **Die Sonden sind lebendig: Jeder Commit, der Logik hinzufuegt oder aendert, erweitert und aktualisiert das Sondensystem mit — es bleibt nie statisch.**
