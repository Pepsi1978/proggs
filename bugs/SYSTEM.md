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

## 2. Ordnerstruktur

```
~/proggs/bugs/
├── README.md            ← Inhaltsverzeichnis: vorhandene + erwartete Bereiche, Trigger
├── SYSTEM.md            ← dieses Dokument
├── chrome-extensions.md ← ein Almanach pro Bereich (flach, eine Datei = ein Thema)
└── …
```

Eine Datei pro Thema, flach. Kein Unterordner pro Thema, solange ein Thema in eine
Datei passt (YAGNI). Aufbau einer Almanach-Datei: siehe Format-Vorlage in `README.md`.

---

## 3. Die drei Automatik-Schichten (Defense in Depth = Franks „100 %")

Eine reine Regel reicht nicht — sie baut darauf, dass jemand dran denkt (genau das
hat beim Chrome-Bug versagt). Erzwingen kann nur die Harness-Ebene (Hooks). Darum
drei Schichten, die unabhaengig voneinander greifen:

| # | Name | Mechanismus | Wann | Poka-Yoke |
|---|------|-------------|------|-----------|
| 1 | **Praesenz** | `bug-almanac-index` Hook (SessionStart) liest `README.md` und blendet die Liste der vorhandenen Almanache + erwarteten Bereiche ein | bei JEDEM Session-Start (auch nach Compaction) | Stufe 1–2: das System ist immer im Blick |
| 2 | **Sicherheitsnetz** | `bug-almanac-guard` Hook (PreToolUse auf Edit/Write) erkennt bereichstypische Dateipfade und erinnert an den passenden Almanach | sobald eine bereichstypische Datei angefasst wird | Stufe 2–3: am konkreten Ausloeser |
| 3 | **Verhalten** | Regel `~/.claude/rules/known-bugs-before-coding.md` | immer geladen | Stufe 1: Verhaltensanweisung |

Beide Hooks: Cross-Platform (`.ps1` + `.sh`), `exit 0`, nicht blockierend
(injizieren nur Kontext/Erinnerung — sie stoppen nie die Arbeit und stoeren nie,
falls eine Datei fehlt). Registriert in `~/.claude/settings.json`, gespiegelt in
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
   → Eintrag in den Almanach: Bug + funktionserhaltende Loesung + Versionen,
     Stand-Header aktualisieren.
```

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

1. Datei `bugs/<bereich>.md` nach der Format-Vorlage anlegen.
2. In `README.md` aus „Bereiche ohne Almanach" nach „Vorhandene Almanache"
   verschieben (mit Stand, Bug-Anzahl, Trigger).
3. Im `bug-almanac-guard`-Hook das Pfad-Mapping ergaenzen (Dateimuster → Almanach).
4. Existiert eine `best-practices/projekt-code/<bereich>/best-practices.md`: die wechselseitige
   Bezugs-Tabelle in BEIDEN Dateien anlegen (siehe §9) und `python3 bugs/check-coupling.py` ausfuehren.

---

## 9. Kopplung mit den Best-Practices (zwei Seiten einer Medaille)

Der Bug-Almanach sagt *was schiefgeht und wie man es loest*; der Ordner
`~/proggs/best-practices/projekt-code/<software>/best-practices.md` sagt *wie man es von
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

- Das Pfad-Mapping im Guard-Hook ist aktuell hartkodiert (klein, erweiterbar).
  Spaeter evtl. aus `README.md` auslesen, damit nur eine Stelle gepflegt werden muss.
- Schwellen-Erkennung („echte Bereichsarbeit vs. Kleinkram") laeuft ueber mein
  Urteil; falls das in der Praxis zu oft daneben liegt, schaerfen wir nach.
- Das System wird in Aktion erprobt und nach Direktive #1 (Superintelligenz)
  iterativ verbessert.
