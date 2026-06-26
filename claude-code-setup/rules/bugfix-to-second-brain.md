# Funktionierende Bugfixes ins zweite Gehirn (Cortex/Second Brain) abspeichern (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-26. Gilt AUTOMATISCH in JEDER Session.
> Ergaenzt **Direktive #3 (Resilient Bugfixing)** um einen festen, geraete- und CLI-uebergreifenden
> Ablage-Schritt: Jeder **bestaetigt funktionierende** Bugfix wird zusaetzlich als strukturierter Fall
> ins zentrale **Second Brain** (Cortex, `second-brain-server`) geschrieben — ueber den
> `second-brain`-MCP. So entsteht EIN gemeinsames Fehler-Gedaechtnis ueber alle Werkzeuge hinweg
> (Claude Code UND OpenCode/Codex/andere Modelle), das per semantischer Suche (`recall`) und
> Kategorie-Drilldown (`bugfixes/<unterkategorie>`) abrufbar ist ("hatten wir sowas schon?").
>
> Spiegelung: `~/proggs/claude-code-setup/rules/bugfix-to-second-brain.md`. Die gleiche Regel steht
> fuer OpenCode (alle Modelle, auch schwache) in `~/proggs/AGENTS.md`. Ausloeser: Codex hat dieses
> Format am 2026-06-26 von sich aus erzeugt; Frank will es als geteilten Standard.

---

## Die eine Regel

**Sobald ein Bugfix als FUNKTIONIEREND gilt, wird er als ein Eintrag ins Second Brain geschrieben —
im festen Format, unter `bugfixes/<passende Unterkategorie>`, mit Titel inkl. Datum.** Nur
funktionierende Fixes. Niemals ein unbestaetigter/kaputter Fix.

Diese Regel ERSETZT die lokale Bug-Doku NICHT, sie ergaenzt sie: Der Repo-Bug-Almanach
(`~/proggs/bugs/<bereich>.md`) bleibt das **proaktiv vor der Arbeit gelesene, kuratierte
Technologie-Wissen**; das Second Brain `bugfixes/` ist die **reaktiv abrufbare Fall-Akte**
("welcher konkrete Bug, wann, wie geloest"). Lehrbuch-Seite (Almanach) + Fall-Akte (Gehirn).

---

## Wann gilt ein Bugfix als FUNKTIONIEREND (der entscheidende Punkt)

**Erst speichern, wenn bestaetigt — dann muss nie etwas geloescht werden.** Zwei Wege:

| Situation | Vorgehen |
|-----------|----------|
| **Objektiv verifizierbar** (Build gruen, Tests bestanden, Deploy `healthy`, das Symptom ist reproduzierbar WEG) | Selbst verifizieren → direkt speichern |
| **Nur der Benutzer kann es beurteilen** (Optik/UI, "fuehlt sich richtig an") ODER du bist unsicher | EINMAL kurz fragen: **"Hat der Fix funktioniert?"** → erst bei **Ja** speichern |

- **Default ist NICHT speichern**, bis bestaetigt. Kein Signal → kurz nachfragen. Niemals einen
  ungeprueften Fix "vorsichtshalber" ablegen.
- Sagt der Benutzer spaeter "hat doch nicht funktioniert": den zuletzt geschriebenen Eintrag wieder
  **entfernen** (MCP `forget` per Titel), erst nach dem echten Fix neu schreiben.
- KEINE komplizierte Zustandsverfolgung noetig — die Reihenfolge "erst bestaetigt, dann speichern"
  macht das von selbst sicher.

---

## Format (genau so, wie es Codex eingefuehrt hat — bewaehrt)

**Titel:** `Bugfix <App> <Bereich> <YYYY-MM-DD>`
- Beispiele: `Bugfix Cortex Vorlesen Toggle Layout 2026-06-26`,
  `Bugfix Cortex Gehirn Kategorie Drilldown 2026-06-26`.
- `<App>` = der Marken-/Projektname (z.B. Cortex = das Second-Brain-Dashboard, BestJournal, …).
- Datum macht mehrere Fixes am selben Bereich unterscheidbar. Titel muss fuer einen Menschen
  SOFORT verstaendlich sein ("was war das fuer ein Bug?").

**Kategorie:** `bugfixes/<unterkategorie>`
- ZUERST pruefen, ob es schon eine **sinnvolle** Unterkategorie gibt (MCP `list_memories` /
  `get_by_category` / Kategorie-Liste ansehen) — dort einordnen.
- Nur wenn keine passt: eine **neue, sprechende** Unterkategorie anlegen (z.B. `bugfixes/cortex-dashboard`,
  `bugfixes/bestjournal-android`, `bugfixes/brain-api`). Einheitlich, klein-mit-Bindestrich, pro App/Bereich.

**Inhalt (Klartext, dieselben Bausteine wie Direktive #3):**
```
Bugfix <YYYY-MM-DD>: <App> <Bereich>. Symptom: <was war sichtbar falsch>. Root Cause: <die
tiefste Ursache, konkret — Datei/Funktion/CSS-Klasse>. Fix: <was genau geaendert wurde, mit
Datei/Stelle>. Verwandte Pruefung: <gibt es weitere gleichartige Stellen?>. Verifikation: <wie
bestaetigt — Build/Test/Deploy/Symptom weg/Benutzer-Ja>. Funktionalitaets-Diff: <was bleibt
unveraendert erhalten>. [Poka-Yoke: <wie der Fehler kuenftig strukturell verhindert wird>].
```

---

## Ablauf (nach jedem Bugfix)

1. Bugfix nach **Direktive #3** umsetzen (Root Cause, funktionserhaltend, verifizieren).
2. Pruefen: gilt er als funktionierend? (objektiv verifiziert ODER Benutzer-Ja; sonst kurz fragen).
3. Passende `bugfixes/<unterkategorie>` finden (vorhandene bevorzugen) oder neu anlegen.
4. Eintrag im Format ueber den `second-brain`-MCP speichern (`remember` mit Titel + Kategorie + Text).
5. Dem Benutzer in EINEM Satz melden: "Im Gehirn dokumentiert: <Titel> [<Kategorie>]."

> Trockenlauf-Hinweis: Ist der `second-brain`-MCP gerade NICHT verbunden, den Eintrag NICHT verlieren —
> kurz melden und auf den naechsten verfuegbaren Moment / die lokale Bug-Doku ausweichen.

---

## Best-Practice-Effekt (der eigentliche Clou)

Sammeln sich unter einer `bugfixes/<unterkategorie>` mehrere **aehnliche** Faelle, ist das ein
**Muster** → daraus laesst sich eine **Best Practice** ableiten (die den Bug kuenftig ganz verhindert).
Genau das ist die Frage "hatten wir sowas schon — und gibt es schon eine bewaehrte Loesung?":
erst `bugfixes/` eingrenzen, dann den Fall (und ggf. die abgeleitete Regel) finden und wiederverwenden.

---

## Was NIEMALS passieren darf

- ❌ Einen **unbestaetigten** Bugfix speichern (erst Bestaetigung/Verifikation, dann speichern)
- ❌ Den Doku-Schritt nach einem funktionierenden Bugfix weglassen
- ❌ Ein abweichendes Format/Titel-Schema nutzen (bricht Auffindbarkeit + Drilldown)
- ❌ Blind eine neue Unterkategorie anlegen, obwohl eine passende schon existiert
- ❌ Einen kryptischen Titel waehlen, bei dem ein Mensch nicht sofort weiss, worum es ging

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| Direktive #3 (`resilient-bugfixing.md`) | Diese Regel ist der Ablage-Schritt NACH dem dort verlangten Fix + Verifikation |
| `~/proggs/bugs/` Bug-Almanach + `bug-cases.jsonl` | Proaktives Tech-Wissen / lokale Fall-DB; das Gehirn ist die zentrale, CLI-uebergreifende, semantisch durchsuchbare Fall-Akte |
| `~/proggs/AGENTS.md` | Traegt dieselbe Regel fuer OpenCode (alle Modelle, auch schwache) |
| `project-cortex-backup-system` (Memory) | Cortex/Second-Brain = `second-brain-server`, MCP-Zugang |
