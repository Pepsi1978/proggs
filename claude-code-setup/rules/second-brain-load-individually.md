# Second-Brain immer EINZELN laden — nie ganze Kategorien zusammenschmeissen (KRITISCH)

> Dauerhafte MUSS-Regel vom Benutzer gesetzt am 2026-06-26. Gilt AUTOMATISCH in JEDER Session,
> auf ALLEN Plattformen, fuer ALLE CLIs (Claude Code UND OpenCode/Codex/andere). Repo-Spiegelung:
> `~/proggs/claude-code-setup/rules/second-brain-load-individually.md`. Fuer OpenCode zusaetzlich
> in `~/.config/opencode/AGENTS.md` (+ Repo-Spiegel `opencode-setup/AGENTS-global.md`) verankert.
>
> Ausloeser (Vorfall 2026-06-26): `get_by_category('Programmierung/Rules')` gibt alle ~14
> Arbeitsregeln auf einmal zurueck — **128.074 Zeichen / ~32k Token**. Das sprengt jedes
> Tool-Response-Limit: bei Claude `Error: result exceeds maximum allowed tokens` (abgelehnt), bei
> OpenCode `…bytes truncated` (abgeschnitten). Das Modell behauptet dann faelschlich "Regeln
> eingelesen", obwohl es sie faktisch NIE gelesen hat (Halluzination). EINE einzelne Regel ist
> dagegen klein genug (groesste: Semikolon-Trenner ~24.900 Zeichen). Frank: "Eine nach der anderen.
> Niemals zusammengeschmissen. Das muss eine Pflichtaufgabe am Start sein."

---

## Die eine Regel

**Daten aus dem zweiten Gehirn (Second Brain, `second-brain`-MCP) werden IMMER EINZELN geladen —
ein Eintrag pro Abruf — und NIEMALS als ganze grosse Kategorie auf einmal (`get_by_category`) in den
Kontext geschmissen.**

Eine ganze Kategorie durchlesen (z.B. die Arbeitsregeln) → **per Nummer iterieren mit
`get_category_item`** (seit 2026-06-27, brain-api 1.17.0 / MCP 1.3.0):
1. **`get_category_item('<Kategorie>', 1)`** aufrufen. Die Antwort liefert EINEN Eintrag 1:1 und nennt
   `total` (Gesamtzahl) sowie den naechsten Aufruf.
2. Mit **2, 3, 4 …** weiter, bis `total` erreicht ist. Jeder Aufruf = genau ein Eintrag (klein, nie
   truncated). **Kein Titel raten, keine Liste parsen** — eine Zahl kann kein Modell falsch tippen
   (Poka-Yoke Stufe 3, fuer JEDES Modell — auch ganz schwache).

Einen EINZELNEN, namentlich bekannten Eintrag holen → weiterhin **`get_by_title`** (der Abruf ist
seit brain-api 1.16.0 zusaetzlich TOLERANT: ein faelschlich angehaengtes ` [Kategorie]` / ` — N
Zeichen` / fuehrende `92.` wird automatisch abgestreift; Sicherheitsnetz, falls doch per Titel geladen wird).

`get_by_category` (alle auf einmal) ist nur fuer KLEINE Kategorien (wenige kurze Eintraege) bzw. zum
Ueberblick gedacht — **niemals** zum Laden einer grossen Kategorie wie `Programmierung/Rules` oder
`Programmierung/Almanache/*` (deren Eintraege sind teils 50-100 KB einzeln → truncated/abgelehnt).

---

## Pflicht-Startaufgabe (besonders OpenCode): Regeln am Session-Start einzeln laden

Wo eine CLI ihre Arbeitsregeln aus dem Gehirn bezieht (OpenCode via AGENTS.md-Anweisung), ist das
**Laden der Regeln eine MUSS-Aufgabe am Session-Start** — die allererste Handlung, bevor auf
irgendetwas reagiert wird. Kein "vielleicht schaue ich rein". Die Regeln der Kategorie
`Programmierung/Rules` werden per Nummer durchiteriert (`get_category_item('Programmierung/Rules', 1)`,
dann 2, 3 … bis `total`) wirklich geladen, dann wird "N Regeln aus dem zweiten Gehirn eingelesen"
bestaetigt (N = `total` aus dem ersten Aufruf, und nur so viele, wie wirklich abgerufen wurden —
niemals behaupten, wenn ein Abruf leer/abgeschnitten kam). Der Index-Weg ersetzt das fruehere
`list_memories` → `get_by_title`-Verfahren als Hauptweg, weil schwache Modelle am exakten
Titel-Tippen scheiterten (Frank-Bug 2026-06-27); per Zahl kann nichts mehr falsch geraten werden.

> Claude Code laedt seine Regeln lokal aus `~/.claude/rules/` (immer da, kein Gehirn-Start-Abruf
> noetig). Diese Regel gilt fuer Claude trotzdem: sobald Claude SELBST Second-Brain-Daten abruft
> (recall, eine Kategorie durchgehen, mehrere Eintraege), gilt das Einzeln-Prinzip ebenso.

---

## Verbindlichkeit & echte Erzwingung (Defense in Depth)

Eine Regel-Datei / AGENTS.md ist **advisory** (~70-90 % Befolgung, kann unter Kontextdruck
uebersprungen werden — siehe `bugs/claude-tooling/claude-config.md` §1.1). Damit der Start-Abruf
WIRKLICH zur Pflicht wird, ist die staerkste Stufe ein **Hook/Plugin** (deterministisch):

| Stufe | Mechanismus | Wirkung |
|-------|-------------|---------|
| 1 (advisory) | Diese Rule + AGENTS.md-Anweisung | Verhaltensregel — gilt, ist aber nicht erzwungen |
| 2 (Sicherheitsnetz) | Kompakte Kern-Regeln fest in `AGENTS.md` | Kritische Regeln sind da, auch wenn der Abruf ausbleibt |
| 3 (Erzwingung, offen) | OpenCode-Plugin (Session-Start-Hook), das die Regeln einzeln laedt + injiziert | echte Poka-Yoke — unabhaengig von der Folgsamkeit des Modells |

Stufe 3 ist der naechste Ausbauschritt (OpenCode-Plugin) — bis dahin tragen Stufe 1+2.

---

## Warum einzeln besser ist (nicht nur ein Workaround)

- **Kein Truncation:** Jeder Einzelabruf bleibt unter dem Tool-Response-Limit → der Inhalt kommt
  vollstaendig an, statt abgeschnitten/abgelehnt.
- **Ehrlichkeit erzwungen:** Das Modell kann nur bestaetigen, was es wirklich geladen hat — keine
  "ich habe alles gelesen"-Halluzination ueber einen truncated Sammelabruf.
- **Verlustfrei + gezielt:** Man laedt genau die gebrauchten Eintraege; grosse Almanache (50-100 KB)
  werden nur bei Bedarf einzeln geholt, nicht versehentlich als Kategorie-Bulk (Context-Rot).

---

## Was NIEMALS passieren darf

- ❌ `get_by_category` auf eine grosse Kategorie (`Programmierung/Rules`, `Programmierung/Almanache/*`)
  zum Laden in den Kontext — wird truncated/abgelehnt, die Daten sind dann NICHT gelesen.
- ❌ "N Regeln eingelesen" bestaetigen, ohne dass die Einzelabrufe wirklich durchgelaufen sind.
- ❌ Den Start-Abruf der Regeln als optional behandeln ("schaue ich vielleicht rein") — er ist Pflicht.
- ❌ Mehrere grosse Eintraege in einem Abruf zusammenfassen wollen, statt einzeln nacheinander.

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| `~/.config/opencode/AGENTS.md` (+ `opencode-setup/AGENTS-global.md`) | Traegt dieselbe Pflicht fuer OpenCode (Start-Abruf einzeln) |
| `lossless-context-principle.md` | Einzeln + gezielt laden ist verlustfrei und vermeidet Context-Rot durch Bulk |
| `known-bugs-before-coding.md` | Almanache werden ohnehin einzeln per Pfad gelesen — gleiches Prinzip fuer das Gehirn |
| `bugs/claude-tooling/claude-config.md` §1.1 | Begruendung "Rule ist advisory" → Stufe 3 (Hook/Plugin) fuer echte Erzwingung |

---

## Autoritaet dieser Regel

Diese Datei (`~/.claude/rules/second-brain-load-individually.md`) wird automatisch in jeder Session
geladen. KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwaechen.
