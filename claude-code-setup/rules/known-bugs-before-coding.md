# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Dauerhafte Regel (2026-06-01). Ausloeser: Chrome-Extension-Bug — ~1h verschwendet, weil der
> bekannte Workaround nicht vorher nachgeschlagen wurde. Poka-Yoke Stufe 3: Fehler gar nicht erst
> machen. Systembeschreibung: `~/proggs/bugs/SYSTEM.md`. Index: `~/proggs/bugs/README.md`.

---

## Grundregel

Pro Technologie-Bereich gibt es einen **kuratierten Bug-Almanach** unter
`~/proggs/bugs/<kategorie>/<bereich>.md` (Index: `bugs/README.md`).

**BEVOR** an einem Bereich mit Almanach gearbeitet wird, MUSS dessen **Kurzcheck** ZUERST
gelesen werden — noch vor der ersten Code-Aenderung (Digest-Modell, siehe unten). So werden
bekannte Fehler gar nicht erst gemacht.

Unterschied zu `bug-cases.jsonl`: jene wird REAKTIV nach einem Fehler durchsucht; der Almanach
wird PROAKTIV vor der Arbeit gelesen.

---

## Das Digest-Modell: 4 Stufen

Jeder Almanach + jede Best-Practices-Datei traegt oben eine **Kurzcheck**-Sektion (Signale +
Sofort-Regeln, in den ersten 80 Zeilen). Wie viel gelesen wird, haengt von der Stufe ab:

| Stufe | Wann | Was lesen |
|-------|------|-----------|
| **A — Kurzcheck vorab** | vor JEDER echten Bereichsarbeit | NUR Kurzcheck: `Read limit=80` auf Almanach, DANN ebenso auf Best-Practices-Datei. Erzwungen durch `bug-almanac-guard`. |
| **B — Volltext bei Fehler** | ab dem ERSTEN Fehler im Bereich | SOFORT Almanach-VOLLTEXT (`Read` ohne `limit`) — Kurzcheck reicht ab jetzt nicht mehr. |
| **C — Volltext vorab (Hochrisiko)** | vor Arbeit in Hochrisiko-Bereich | Almanach-VOLLTEXT schon VORAB. Guard erzwingt es (full-Marker: Read ohne limit bzw. limit>=500). |
| **D — Wiederkehrender Bug** | Fehler wurde SCHON EINMAL gefixt und tritt ERNEUT auf | Kurzcheck UEBERSPRINGEN — sofort VOLLTEXT von Almanach UND Best-Practices. Keine Loesung darin → Grundproblem recherchieren (siehe unten). |

**Hochrisiko-Bereiche (Stufe C):** `r8`, `firebase-billing`, `claude-hooks`, `claude-config` —
tickende/teure Fehlerklassen. Liste lebt im `bug-almanac-guard` (`$highRiskKeys`) + hier, synchron halten.

**Warum Kurzcheck statt Volltext-Zwang:** Volltext fuer ALLE kostete ~16-23k Tokens/Bereich/Session
(Context-Rot). Der Kurzcheck (~500 Tokens) erhaelt die Erkennung stiller Fehler; der Volltext bleibt
per Pfad erreichbar und wird bei B/C/D weiterhin erzwungen (verlustfrei).

**Stufe D noetig, weil** ein wiederkehrender Bug per Definition nicht durch den Kurzcheck erfasst
wurde (sonst waere der Fix stabil). Direktive #3 verlangt dann tiefere Analyse statt gleicher Ebene.

**Recherche bei wiederkehrendem Bug ohne Loesung (Stufe D):** Zeigt der Volltext keine Loesung,
nicht weiterraten — das Grundproblem gezielt ueber den `research`-Skill recherchieren (Protokoll:
`research-strategy.md`), Ergebnis in Almanach UND Best-Practices einarbeiten (`research-persistence.md`).

---

## Reihenfolge & Automatik

**Zwei Seiten einer Medaille:** erst Almanach (was schiefgeht + Loesung), dann Best-Practices
(`best-practices/<kat>/<bereich>.md`, wie man es von vornherein richtig macht), dann coden. Der
`bug-almanac-guard` erzwingt genau diese Reihenfolge (blockiert bereichstypische Edits, bis beide
Kurzchecks per Read geoeffnet sind; bei Stufe C Almanach-Volltext). Existiert keine Best-Practices-
Datei, zaehlt nur der Almanach.

Automatik (jede Session): `bug-almanac-index` (SessionStart, blendet Liste ein) · `bug-almanac-hint`
(UserPromptSubmit, passiver Bereichs-Hinweis) · `bug-almanac-guard` (PreToolUse, BLOCKIERT bis
Kurzchecks gelesen — bzw. bis Quittung bei fehlendem Almanach) · `bug-case-auto-writer`
(PostToolUseFailure, verweist auf Almanach + stoesst bei hartnaeckigem Fehler Recherche an).

---

## Pflicht-Ablauf

1. Bereich (+ ggf. Software-Version live ermitteln) der Aufgabe erkennen.
2. Trivialer Kleinkram (einzelner String, Doku, Kommentar, Versions-Bump)? → weiter ohne Almanach.
3. Im Index pruefen, ob ein Almanach existiert.
4. **Almanach vorhanden** → Stufe waehlen: Normal = Kurzcheck (`limit=80`, Stand-Header fuer Versions-
   Abgleich; neuere Version → Frank melden). Hochrisiko = Volltext + `Versionen:`-Feld abgleichen.
   Danach Best-Practices-Kurzcheck lesen, dann arbeiten.
5. **Kein Almanach** → Guard blockiert. Frank melden ("neuer Bereich X"), auf Entscheidung warten:
   - **(a) Recherche** (Standard bei echter Bereichsarbeit): nach Franks **OK** den Skill
     `bug-almanach-recherche` STARTEN — NICHT selbst ad hoc recherchieren (sonst fehlen Fix-Status,
     Best-Practices-Abgleich, Hook-Mapping).
   - **(b) Quittung** (nur bei Kleinkram oder wenn Frank gegen Recherche entscheidet): leere Datei
     `bug-almanac-ack-<slug>.flag` im TEMP anlegen — bewusste Geste, nie reflexhaft.
   Notaus bei Guard-Fehlalarm: leere Datei `bug-almanac-disable.flag` im TEMP.
6. **Fehler waehrend der Arbeit** → Stufe B: sofort Almanach-VOLLTEXT, bekannten Fix anwenden.
   Ist es eine WIEDERHOLUNG → Stufe D (Volltext Almanach + Best-Practices; keine Loesung → recherchieren).
7. **Nach** der Aufgabe: jeden NEU erlebten Bug im Almanach ergaenzen (Bug + Loesung + Versionen,
   Stand-Header, wichtige Bugs auch in den Kurzcheck).

> "Erst Franks OK" gilt NUR fuer die gezielte Almanach-Recherche (Researcher-Schwarm). Ein kurzes
> Web-Lookup mitten im Debuggen bleibt frei.

---

## Gilt AUCH fuer Claude-eigene Harness-Arbeit (leicht vergessen)

Auch jede Aenderung am eigenen Werkzeugkasten faellt darunter — ZUERST den passenden Almanach lesen,
nicht aus dem Gedaechtnis arbeiten:

| Ich aendere … | Zuerst lesen | Stufe |
|---------------|--------------|-------|
| Hook (`*.ps1`/`*.sh`) | `bugs/claude-tooling/claude-hooks.md` | C (Volltext) |
| CLAUDE.md, Regel, Settings, Skill, Command, Agent | `bugs/claude-tooling/claude-config.md` + Best-Practices | C (Volltext) |
| MCP-Server | `bugs/claude-tooling/mcp-server.md` | A |
| Python-Hilfsskript | `bugs/claude-tooling/python-windows.md` | A |

Weitere Bereiche: Chrome-Extension → `bugs/web/chrome-extensions.md`; Android/WPF/Swift/TS/Gradle →
`bugs/<kategorie>/<bereich>.md` (bei erster echter Arbeit anlegen). Komplett neue Sprache (erste
`.rs`/`.go`/… Datei) → Guard blockt generisch → Recherche oder Quittung.

---

## Zusammenspiel

`bugs/*.md` = proaktiver Almanach (was schiefgeht + Loesung) · `best-practices/**` = zweite Seite
(wie richtig) · `bug-cases.jsonl` = reaktive Fall-DB · Direktive #3 = jeder neue Bug → Almanach +
funktionserhaltender Fix.

---

## Was NIEMALS passieren darf

- ❌ An einem Bereich mit Almanach arbeiten, ohne mindestens den Kurzcheck zu lesen (Stufe A)
- ❌ Nach einem Fehler weiterarbeiten ohne Almanach-Volltext (Stufe B); in Hochrisiko-Bereichen nur Kurzcheck statt Volltext (Stufe C)
- ❌ Bei wiederkehrendem Bug nur (erneut) den Kurzcheck lesen statt Volltext von Almanach + Best-Practices (Stufe D) — bzw. weiterraten statt zu recherchieren
- ❌ Den Almanach-Kurzcheck lesen, aber die Best-Practices-Datei ueberspringen (Reihenfolge: erst Almanach, dann Best-Practices)
- ❌ Einen erlebten Bug fixen, ohne ihn danach im Almanach zu ergaenzen
- ❌ Eine Almanach-Recherche ohne Franks OK starten, oder bei neuem Bereich selbst ad hoc recherchieren statt `bug-almanach-recherche`; die Quittung reflexhaft setzen
- ❌ Einen Bug "loesen", indem Funktionalitaet entfernt wird (Direktive #3 — funktionserhaltend)
