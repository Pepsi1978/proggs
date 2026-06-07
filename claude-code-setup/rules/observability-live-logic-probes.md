# Live-Logik-Sonden: Intent-Verifikation in Echtzeit (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-07. Gilt AUTOMATISCH in JEDER Session
> fuer JEDES qualifizierte Software-Projekt. Adressat dieser Direktive ist **Claude Code selbst**.
> **Zusatz-Direktive** und direkte Erweiterung der Hauptdirektive
> [[observability-first]] (`~/.claude/rules/observability-first.md`) — sie schaerft deren
> **Abschnitt 2.3 (Logik-Sonden)** und **Abschnitt 4 (Live-Monitoring)** um einen Fokus, der
> dort noch nicht explizit war: **live pruefen, ob die Logik der Software genau das tut, was
> im urspruenglichen Bau-Prompt gemeint war.** Verbindlicher Standard AUSSERHALB der
> geschuetzten 3-Direktiven-Trinitaet (#1 Superintelligenz, #2 Selbstbeobachtung,
> #3 Resilient Bugfixing). In `CLAUDE.md` referenziert, ins Repo gespiegelt
> (`claude-code-setup/rules/observability-live-logic-probes.md`).

---

## 1. Worum es geht — der Unterschied zu „normalen" Sonden

Die Logik-Sonden aus der Hauptdirektive (Abschnitt 2.3) sind **defensiv**: Sie schlagen an,
wenn eine Annahme verletzt wird. Live-Logik-Sonden gehen einen Schritt weiter — sie sind
**bestaetigend**:

- Sie zeichnen **die Logik selbst** auf, waehrend die Software laeuft (nicht nur Fehler oder Crashes).
- Sie melden **live**, ob jeder fachliche Schritt **so umgesetzt** wurde, wie Frank ihn im
  Bau-Prompt beschrieben hat.
- Claude Code schaut ueber den Live-Stream sofort mit und bewertet **wie ein PR-Reviewer**:
  „Ist dieser Schritt logisch korrekt angekommen? Tut die App genau das, was gemeint war?"

Kurz: nicht „ist etwas kaputt?", sondern „**ist die Logik richtig angekommen?**" — und das
in Echtzeit beim ersten Start der neuen Software.

---

## 2. Kernmechanik — Intent-gebundene Checkpoints

Damit das funktioniert, muss die **Absicht** maschinen- und live-pruefbar werden. Deshalb:

**Beim Bauen der Software:**
- Leite aus Franks Bau-Prompt die **beabsichtigten Verhaltensweisen / Akzeptanzkriterien**
  ab (jeder „die App soll …"-Satz ist ein Kandidat).
- Verdrahte **jeden** davon als benannten **Live-Logik-Checkpoint** im Code — an genau der
  Stelle, wo dieser Schritt tatsaechlich passiert.
- Jeder Checkpoint gibt zur Laufzeit **erwartet vs. tatsaechlich** aus.

**Format eines Checkpoint-Eintrags (eigener Kanal, getrennt vom Fehler-Log):**
```json
{"ts":"…","kind":"CHECKPOINT","step":"Rabatt berechnen","intent":"10% Rabatt ab 3 Artikeln","expected":"0.10","actual":"0.00","ok":false,"ctx":{"items":4}}
```
- `step` — der fachliche Schritt (verstaendlicher Name)
- `intent` — was im Bau-Prompt gemeint war (im Klartext)
- `expected` / `actual` — Soll vs. Ist
- `ok` — stimmt es ueberein?
- `ctx` — relevanter Zustand

Diese Checkpoints bilden einen **lesbaren Live-Erzaehlstrang der App-Logik** — eigener
TAG/Kanal (z. B. `LOGIC` bzw. `CHECKPOINT`), damit man ihn live verfolgen kann, ohne im
Fehler-Rauschen zu suchen.

---

## 3. Der Live-Verifikations-Loop

1. Frank startet die neu gebaute Software.
2. Die Live-Logik-Sonden streamen ihren Checkpoint-Kanal — je nach Plattform ueber
   `adb logcat -s LOGIC` (Android), `tail -f` (macOS/Linux) oder `Get-Content -Wait`
   (Windows/PowerShell). (Befehle wie in Abschnitt 3/4 der Hauptdirektive.)
3. Frank bedient die App ganz normal.
4. Claude Code liest den Live-Strom mit und prueft **jeden Checkpoint gegen die
   urspruengliche Absicht**:
   - `ok:true` → „Schritt X logisch korrekt angekommen ✓"
   - `ok:false` → sofort melden: „Schritt Y weicht ab — erwartet …, tatsaechlich … →
     Logik nicht wie im Prompt gemeint."
5. Bei Abweichung: Ursache benennen (Root-Cause, Direktive #3), Fix vorschlagen/umsetzen und
   beim naechsten Lauf den Checkpoint erneut live verifizieren.

So sieht Claude Code **beim Benutzen** sofort, ob die Logik traegt — nicht erst hinterher
beim Log-Durchsuchen.

---

## 4. Was die Live-Logik-Sonden konkret aufzeichnen

Nicht nur Werte, sondern die **logische Substanz**:
- **Entscheidungen**: welcher Zweig wurde genommen, und war es der beabsichtigte?
- **Berechnungen**: Ergebnis vs. erwartetes Ergebnis.
- **Ablauf-Schritte**: wurde der vorgesehene Flow in der vorgesehenen Reihenfolge
  durchlaufen? (Schritt erreicht / uebersprungen)
- **Zustandsuebergaenge** gegen die Spezifikation (von A nach B — war B gewollt?).
- **Ein-/Ausgaben** an fachlichen Grenzen gegen die Erwartung.

---

## 5. Verankerung
- Bei jedem qualifizierten Projekt (Schwelle wie in der Hauptdirektive, Abschnitt 0) werden
  die Intent-Checkpoints **mitgebaut**, sobald die Software aus einem Prompt mit klarer
  Verhaltensabsicht entsteht.
- Sie unterliegen der **Co-Evolution** (Abschnitt 6 der Hauptdirektive): neuer/geaenderter
  Intent → neue/angepasste Checkpoints; weggefallener Intent → Checkpoint entfernen. Damit
  gilt der **Stale-Probe-Schutz** auch hier: ein Checkpoint, der eine laengst geaenderte
  Absicht prueft, erzeugt Fehlalarme und muss mitgezogen werden.
- Zusaetzlicher Hebel per Zuruf: **„starte den Live-Logik-Check"** → Claude Code startet den
  Checkpoint-Stream und verifiziert mit, waehrend Frank die App bedient.

---

## 6. Ehrliche Einordnung zur Machbarkeit (und was bewusst NICHT dabei ist)

- **Machbar, vollstaendig innerhalb von Claude Code CLI:** der Stream-mitlesen-und-
  verifizieren-Loop aus Abschnitt 3. Voraussetzung ist nur, dass die App ihre Checkpoints in
  einen Stream schreibt, den `logcat`/`tail`/`Get-Content` live ausgeben.
- **Zugbasierte Realitaet:** Claude Code schaut nicht permanent im Leerlauf zu, sondern
  reagiert, wenn es aufgerufen wird bzw. einen Capture-Lauf mitliest. Innerhalb einer Sitzung
  ergibt das praktisch eine Live-Verifikation; am saubersten auf Android (echtes logcat).
- **Bewusst ausgeklammert:** ein **separates externes Zwischentool**, in das Sonden-Daten
  hineinfliessen, optimiert an Claude Code zurueckgehen und ueber das Claude Code dann
  **automatisch** das Verhalten der App live umsteuert oder Code anstoesst. Das ist ein
  eigenes Tool/Projekt und nicht Teil dieser Direktive. (Als moeglicher naechster Schritt
  notiert — hier nicht umgesetzt.)

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| [[observability-first]] (Hauptdirektive) | Diese Zusatz-Direktive schaerft deren Abschnitt 2.3 (Logik-Sonden) + 4 (Live-Monitoring). Logging/Fehler-Faenger/Plattform-Streams/Co-Evolution/Secrets gelten unveraendert weiter |
| Direktive #3 (`resilient-bugfixing.md`) | Bei `ok:false` greift der Root-Cause-Fix; ein abweichender Checkpoint ist ein Logikfehler-Signal, kein Symptom-Pflaster |
| `debugging-and-verification.md` | Intent-Checkpoints sind Sonden VOR dem Raten (Stufe 2) — hier zusaetzlich bestaetigend statt nur defensiv |
| `metacognitive-process.md` (Spec-First) | Die aus dem Bau-Prompt abgeleiteten Akzeptanzkriterien sind dieselben Invarianten, die Spec-First definiert — Checkpoints verifizieren sie zur Laufzeit |
| `lossless-context-principle.md` | Den Checkpoint-Stream nie ungefiltert in den Kontext laden — gezielt per `grep`/`jq` auf `kind:CHECKPOINT` und `ok:false` |

---

## Was NIEMALS passieren darf

- ❌ Aus einem Bau-Prompt mit klarer Verhaltensabsicht eine App bauen, OHNE die
  beabsichtigten Schritte als Intent-Checkpoints zu verdrahten
- ❌ Einen `ok:false`-Checkpoint im Live-Strom sehen und nicht sofort melden + an der Wurzel fixen
- ❌ Checkpoints in denselben Kanal wie das Fehler-Log mischen, sodass der Erzaehlstrang im Rauschen untergeht
- ❌ Geaenderte Absicht committen, ohne den zugehoerigen Checkpoint mitzuziehen (Stale-Probe-Fehlalarm)
- ❌ Das automatische Rueck-Steuerungs-Tool (Abschnitt 6) als Teil dieser Direktive behandeln — es bleibt bewusst aussen vor

---

**Kurzfassung:** Live-Logik-Sonden = aus dem Bau-Prompt abgeleitete, benannte Checkpoints,
die zur Laufzeit „erwartet vs. tatsaechlich" in einen eigenen Live-Kanal schreiben. Frank
startet die App, Claude Code liest den Kanal live mit und bestaetigt Schritt fuer Schritt, ob
die Logik so angekommen ist wie gemeint — und meldet Abweichungen sofort. Reine CLI-Loesung;
das automatische Rueck-Steuerungs-Tool bleibt bewusst aussen vor.
