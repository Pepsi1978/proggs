# Bug-Almanach-System — Funktionsweise & Design

> Stand: 2026-06-01 (v1), erweitert 2026-06-02 (Kopplung mit Best-Practices, §9). Entwickelt mit Frank, ausgeloest durch den
> Chrome-Extension-Verschwind-Bug (~1h verloren, weil der dokumentierte Workaround
> nicht VORHER nachgeschlagen wurde). Dieses Dokument beschreibt, wie das System
> arbeitet. Es ist die Referenz fuer kuenftige Verbesserungen.

---

## 1. Zweck (in einem Satz)

**Bevor an einem technischen Bereich gearbeitet wird, liegen dessen bekannte Bugs
und ihre bewaehrten, funktionserhaltenden Loesungen bereits auf dem Tisch — statt
sie hinterher teuer zu debuggen.** Poka-Yoke Stufe 3: der „Stunde-verloren"-Fehler
kann strukturell nicht mehr passieren, weil das Wissen VOR der Arbeit praesent ist.

Das System ist **selbstwachsend**: jeder neue Bereich fuegt einen Almanach hinzu,
jeder erlebte Bug verdichtet einen bestehenden. Verwandt — aber getrennt:
- **`bugs/` (dieses System)** = PROAKTIV, pro Bereich kuratiert, VOR der Arbeit gelesen.
- **`.claude/agent-memory/shared/bug-cases.jsonl`** = REAKTIV, Auto-Match NACH einem Fehler.

---

## 2. Ordnerstruktur (seit 2026-06-03: Kategorie-Unterordner)

```
~/proggs/bugs/
├── README.md                    ← Inhaltsverzeichnis (nach Kategorie gruppiert), Trigger
├── SYSTEM.md                    ← dieses Dokument
├── OFFENE-ALMANACHE-PROMPTS.md  ← fertige Recherche-Prompts fuer offene Bereiche
├── check-coupling.py            ← Health-Check der Bug↔Best-Practices-Kopplung
├── android/                     ← Kategorie-Ordner (eine Datei = ein Thema)
│   ├── kotlin.md · jetpack-compose.md · android-platform.md · firebase-billing.md
├── android-build/               ← gradle.md · r8.md
├── desktop/                     ← dotnet-csharp.md · swift-appkit.md
├── web/                         ← chrome-extensions.md · typescript.md
├── peripherie/                  ← stream-deck.md
└── claude-tooling/              ← claude-hooks.md · mcp-server.md · python-windows.md
```

Almanache liegen in **Kategorie-Unterordnern** (`bugs/<kategorie>/<bereich>.md`), eine
Datei pro Thema. Die Kategorien gruppieren nach Software-Typ und halten den wachsenden
Bestand uebersichtlich. Die Hooks und `check-coupling.py` suchen **rekursiv** — der
Kategorie-Ordner einer Datei ist frei waehlbar und aenderbar, ohne dass ein Hook
angepasst werden muss. Aufbau einer Almanach-Datei: siehe Format-Vorlage in `README.md`.

---

## 3. Die drei Automatik-Schichten (Defense in Depth = Franks „100 %")

Eine reine Regel reicht nicht — sie baut darauf, dass jemand dran denkt (genau das
hat beim Chrome-Bug versagt). Erzwingen kann nur die Harness-Ebene (Hooks). Darum
drei Schichten, die unabhaengig voneinander greifen:

| # | Name | Mechanismus | Wann | Poka-Yoke |
|---|------|-------------|------|-----------|
| 1 | **Praesenz** | `bug-almanac-index` Hook (SessionStart) liest `README.md` und blendet die Liste der vorhandenen Almanache + erwarteten Bereiche ein | bei JEDEM Session-Start (auch nach Compaction) | Stufe 1–2: das System ist immer im Blick |
| 2 | **Erzwingung** | `bug-almanac-guard` Hook (PreToolUse auf Read/Edit/Write/MultiEdit/Bash) **BLOCKIERT** Edit/Write/MultiEdit (`permissionDecision:deny`), solange der passende Almanach in dieser Session nicht gelesen wurde. Read einer `bugs/<X>.md` ODER ein Bash-`cat`/`bat`/`less` darauf setzt den "gelesen"-Marker und gibt den Bereich frei (1x pro Bereich/Session). Kein Almanach vorhanden → nur erinnern (kein Block — Recherche braucht Franks OK). Jeder Block wird nach `~/.claude/state/bug-almanac-blocks.log` protokolliert. Notaus: `bug-almanac-disable.flag` im TEMP. FAIL-OPEN bei jedem Hook-Fehler. | sobald eine bereichstypische Datei angefasst wird | **Stufe 2 (Erzwingung)**: blockiert am konkreten Ausloeser |
| 3 | **Verhalten** | Regel `~/.claude/rules/known-bugs-before-coding.md` | immer geladen | Stufe 1: Verhaltensanweisung |

Beide Hooks: Cross-Platform (`.ps1` + `.sh`). Der Index-Hook ist nicht blockierend
(injiziert nur Kontext). Der Guard-Hook ist seit 2026-06-02 **blockierend** (Stufe 2):
er stoppt Edit/Write per `permissionDecision:deny` + `exit 0` (NICHT `exit 2` — das
blockt Write/Edit nicht, siehe bugs/claude-tooling/claude-hooks.md 1.6), bis der Almanach des Bereichs
in dieser Session gelesen wurde. FAIL-OPEN: jeder interne Hook-Fehler → durchlassen,
nie faelschlich blockieren. Notaus via `bug-almanac-disable.flag` im TEMP-Verzeichnis.
Registriert in `~/.claude/settings.json`, gespiegelt in
`claude-code-setup/hooks/`.

---

## 4. Der vollstaendige Ablauf bei einer Aufgabe

```
1. Bereich + (falls relevant) Version erkennen — woran arbeite ich?
2. Greift das System? (siehe §6 Schwelle) — bei trivialem Kleinkram: nein, weiter.
3. Almanach im Index vorhanden?
   ├─ JA  → komplett lesen → Version live abgleichen
   │        ├─ gleiche/aeltere Version als dokumentiert → arbeiten mit Bug-Wissen
   │        └─ neuere Version als dokumentiert → Frank melden, OK fuer kurzen
   │           Re-Check einholen (existieren die Bugs noch? neue dazugekommen?)
   └─ NEIN → Frank melden: „neuer Bereich X, kein Almanach — recherchieren?"
            → auf sein OK warten → 3–5 Researcher (gedeckelt, siehe §5)
            → Almanach anlegen → in README.md eintragen
4. ARBEITEN — mit den bekannten Bugs im Kopf, damit sie gar nicht erst eingebaut werden.
5. Tritt ein Fehler auf → ZUERST Almanach pruefen: ist das ein bekannter Bug?
   └─ ja → dokumentierte Loesung sofort anwenden (schnellster Pfad).
6. Neuen Bug erlebt (oder im Netz beste Loesung gefunden)
   → NUR wenn es ein HARTER, sicher bestaetigter Bug ist (reproduzierbar, eindeutig
     ein echter Fehler — keine Vermutung): Eintrag in den passenden
     `bugs/<bereich>.md`-Almanach: Bug + funktionserhaltende Loesung + Versionen,
     Stand-Header aktualisieren.
```

### Zwei Ebenen — was wohin (Qualitaetsschwelle)

Die rohe Fall-Datenbank und der kuratierte Almanach haben bewusst verschiedene Schwellen:

| | `bug-cases.jsonl` (Posteingang) | `bugs/<bereich>.md` (Almanach/Lehrbuch) |
|---|---|---|
| Wer schreibt | `bug-case-auto-writer`-Hook, automatisch bei JEDEM Tool-Fehler | ich, manuell, NACH der Aufgabe |
| Schwelle | ALLES — auch Unsicheres, Einmaliges, Rauschen (`auto_captured: true`) | **NUR HARTE, sicher bestaetigte Bugs** (reproduzierbar, eindeutig ein echter Fehler) |
| Zweck | reaktiver Auto-Match nach einem Fehler (RAG) | proaktive Vorab-Lektuere, hochwertig, wird gelesen bevor gearbeitet wird |

**Befoerderung (Posteingang → Lehrbuch).** Bestaetigt sich ein roher `jsonl`-Auto-Eintrag als
HARTER Bug (Ursache verstanden, reproduzierbar, funktionserhaltende Loesung bekannt), wird er
in den passenden `bugs/<bereich>.md`-Almanach uebernommen — mit Format Symptom/Ursache/
Versionen/FIX/Quelle. Nur so erscheint er bei der erzwungenen Vorab-Lektuere und kann den
Fehler kuenftig verhindern. Unsichere/spekulative Eintraege bleiben in der `jsonl` und
verschmutzen den Almanach NICHT — der Almanach bleibt vertrauenswuerdig, weil dort nur Hartes steht.

---

## 5. Recherche-Regeln

- **„Erst Franks OK"** gilt fuer die **gezielte Almanach-Recherche** (neuer Bereich
  oder Re-Check bei Versionssprung) — der Researcher-Schwarm. NICHT fuer ein
  normales kurzes Web-Lookup mitten im Debuggen einer laufenden Aufgabe (das wuerde
  jede Kleinigkeit blockieren).
- **Researcher-Limits** (aus `agent-and-researcher-rules.md`): 3–5 parallele
  Researcher, je max 50 Ergebnisse, max 15 Web-Fetches, max 10 Min. Auf Opus[1m].
- **Versionsgenau:** Es wird fuer die tatsaechlich benutzte Version recherchiert
  (live ermittelt: `chrome --version`, `./gradlew --version`, `package.json` …).
- **Loesungen sind Pflicht, nicht nur Bugs:** Zu jedem Bug die beste, von der
  Community bewaehrte Loesung suchen — und sie muss **funktionserhaltend** sein
  (Direktive #3): volle Funktionalitaet in Optik und Verhalten bleibt erhalten,
  niemals „Feature entfernen" als Schein-Fix.

---

## 6. Wann das System greift — und wann nicht

| Greift | Greift NICHT |
|--------|--------------|
| Echte Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle …) | Trivialer Kleinkram: einzelner String, Doku, Kommentar, Versions-Bump |
| Neues Feature, Bugfix, Refactoring in einem Bereich | Reine Frage-Antwort ohne Code-Aenderung |

Begruendung: Bei Kleinkram gibt es keine bereichsspezifischen Bugs — der Check
wuerde nur bremsen. Die Schwelle haelt das System schnell.

---

## 7. Versions-Denken

- Jeder Bug-Eintrag traegt ein `Versionen:`-Feld (betrifft V1–V3, gefixt ab V4 —
  oder „per Design / unabhaengig").
- Jeder Almanach traegt oben einen **Stand**-Vermerk (zuletzt recherchiert am Datum
  fuer Version V).
- Die aktuell benutzte Version wird **live** ermittelt, nie in einer separaten Datei
  gepflegt (die wuerde veralten). Beim Lesen wird abgeglichen; ein Versionssprung
  loest einen kurzen Re-Check aus (mit Franks OK).

---

## 8. So erweiterst du das System (neuen Almanach hinzufuegen)

1. Passende **Kategorie** waehlen (android, android-build, desktop, web, peripherie,
   claude-tooling) — oder, wenn nichts passt, eine neue Kategorie anlegen. Datei
   `bugs/<kategorie>/<bereich>.md` nach der Format-Vorlage anlegen.
2. In `README.md` unter der passenden Kategorie eintragen (aus „Bereiche ohne Almanach"
   nach „Vorhandene Almanache"; Stand, Bug-Anzahl, Trigger).
3. Im `bug-almanac-guard`-Hook NUR dann etwas tun, wenn es ein NEUES Dateimuster gibt
   (Dateimuster → `<bereich>.md`, nur der Dateiname OHNE Kategorie — der Hook findet die
   Datei rekursiv). Ein blosser Kategorie-Wechsel braucht KEINE Hook-Aenderung.
4. Existiert eine `best-practices/projekt-code/<kategorie>/best-practices-<bereich>.md`: die
   wechselseitige Bezugs-Tabelle in BEIDEN Dateien anlegen (siehe §9) und
   `python3 bugs/check-coupling.py` ausfuehren.

---

## 9. Kopplung mit den Best-Practices (zwei Seiten einer Medaille)

Der Bug-Almanach sagt *was schiefgeht und wie man es loest*; der Ordner
`~/proggs/best-practices/projekt-code/<kategorie>/best-practices-<software>.md` sagt *wie man es von
vornherein richtig macht, damit der Bug nie entsteht*. Beide gehoeren zusammen und werden in
BEIDE Richtungen gepflegt — keine Einbahnstrasse:

| Richtung | Wer schreibt | Was |
|----------|--------------|-----|
| Bug → Best-Practice | `bug-almanach-recherche`-Skill (Schritt 4b) | findet er einen Bug, traegt er die allgemeine Praevention in best-practices ein |
| Best-Practice → Bug | `best-practices`-Skill (Abschnitt „Kopplung zum Bug-Almanach", Teil A) | foerdert ein Lauf einen Bug zutage, schreibt er ihn in `bugs/<bereich>.md` zurueck |

**Bezugs-Tabellen (die Verlinkung).** Existieren beide Dateien einer Software, traegt JEDE eine
wechselseitige Abschnitts-Bezugs-Tabelle „Best-Practice-Abschnitt ↔ Bug-Abschnitt". So springt
man von einer Best-Practice direkt zur konkreten Bug-Loesung und zurueck. Beide Skills (Schritt 4c
bzw. Teil B) halten diese Tabellen synchron — egal, welcher laeuft.

**Regel beim Zurueckschreiben (Direktive #3).** Bug-Eintraege werden gegen bestehende
DEDUPLIZIERT; gibt es noch keinen Almanach fuer den Bereich, wird KEIN halber angelegt (ihm
fehlte die Fix-Status-Pruefung per `gh`) — stattdessen `bug-almanach-recherche` vorgeschlagen.
Die Funktionalitaets-Erhaltung der Loesungen gilt unveraendert.

**Health-Check.** `python3 bugs/check-coupling.py` prueft fuer jede Projekt-Code-Software mit
beiden Dateien, ob die Bezugs-Tabelle in BEIDEN vorhanden ist. Rein lesend, immer `exit 0`
(blockiert nie eine Session), meldet Drift per `[DRIFT]`-Zeile. Manuell oder im Rahmen eines
Wartungslaufs ausfuehren, damit die Verlinkung nicht still auseinanderlaeuft.

---

## 10. Bewusste Grenzen von v1 (kommende Verbesserungen)

- Das Dateimuster→Almanach-Mapping im Guard-Hook ist weiterhin hartkodiert (welcher
  Dateipfad/welche Endung zu welchem Almanach gehoert) — klein, erweiterbar. Der
  KATEGORIE-Pfad eines Almanachs ist hingegen seit 2026-06-03 NICHT mehr hartkodiert:
  der Hook sucht die Almanach-Datei rekursiv unter `bugs/` (kategorie-robust). Spaeter
  evtl. auch das Dateimuster-Mapping aus `README.md` auslesen, damit nur eine Stelle
  gepflegt werden muss.
- Schwellen-Erkennung („echte Bereichsarbeit vs. Kleinkram") laeuft ueber mein
  Urteil; falls das in der Praxis zu oft daneben liegt, schaerfen wir nach.
- Das System wird in Aktion erprobt und nach Direktive #1 (Superintelligenz)
  iterativ verbessert.
