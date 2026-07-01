# Bug-Almanach-System — Funktionsweise & Design

> Stand: 2026-06-01 (v1), erweitert 2026-06-02 (Kopplung mit Best-Practices, §9), erweitert
> 2026-06-07 (das LESEN der Best-Practices-Datei wird jetzt vom Guard MIT-erzwungen — erst Almanach,
> dann Best Practices, dann coden; §3/§4/§9), erweitert 2026-06-10 (**Digest-Modell**: Kurzcheck-
> Sektionen in allen Almanachen + Best-Practices, 3 Lese-Stufen A/B/C; §11). Entwickelt mit Frank, ausgeloest durch den
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
├── repair-bug-cases.py          ← Wartung: kaputte JSON-Zeilen in bug-cases.jsonl reparieren/quarantaenieren (--apply)
├── analyze-blocks.py            ← Auswertung: Block-Log -> Almanach-Kandidaten + aktivste Bereiche
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
| 1b | **Prompt-Trigger** (NEU 2026-06-15) | `bug-almanac-hint` Hook (UserPromptSubmit) scannt den User-Prompt auf bereichstypische Stichwoerter und injiziert EINMALIG pro Bereich/Session einen PASSIVEN `additionalContext`-Hinweis auf den passenden Almanach. Faengt **Konzept-/Planungsarbeit**, BEVOR eine Datei beruehrt wird (ergaenzt den Datei-Guard, Schicht 2). Stichwort-Heuristik (bewusst kein Embedding/MCP — in einem Hook nicht sauber aufrufbar), kein Block. Logik in `bug-almanac-hint.py` (Cross-Platform), `.ps1`/`.sh` sind duenne Wrapper. | bei jedem Prompt | Stufe 1: frueher Hinweis, kein Zwang |
| 2 | **Erzwingung** | `bug-almanac-guard` Hook (PreToolUse auf Read/Edit/Write/MultiEdit/Bash) **BLOCKIERT** Edit/Write/MultiEdit (`permissionDecision:deny`), solange der passende Almanach in dieser Session nicht gelesen wurde — seit 2026-06-10 genuegt dafuer der **Kurzcheck** (`Read` mit `limit=80`); HOCHRISIKO-Bereiche verlangen den Volltext (full-Marker, §11) — und (seit 2026-06-07) danach weiter, solange die zugehoerige `best-practices-<bereich>.md` (falls vorhanden) nicht gelesen wurde. Read einer `bugs/<X>.md` bzw. `best-practices-<X>.md` ODER ein Bash-`cat`/`bat`/`less` darauf setzt den jeweiligen "gelesen"-Marker und gibt frei (1x pro Bereich/Session; Reihenfolge erst Almanach, dann Best Practices). **Kein Almanach vorhanden (seit 2026-06-07): ebenfalls BLOCKIERT** — bis eine bewusste Quittung `bug-almanac-ack-<slug>.flag` (im TEMP) gesetzt ist (von mir NACH Franks Entscheidung: Recherche ODER bewusst verzichten) oder der Notaus aktiv ist. Damit ist der "neuer Bereich"-Trigger nicht mehr zahnlos (frueher nur `additionalContext`, wurde uebersehen — im Block-Log nie ein "kein-almanach"-Eintrag). Zusaetzlich erkennt der Guard **komplett neue Sprachen generisch** ueber die Endung (`.rs/.go/.rb/.java/.php/.lua/.c/.cpp/.h/.dart/.vue/.svelte/.ex/.clj/.scala/.hs/.zig/.nim/.pl/.groovy`), sodass auch eine erste Rust-/Go-Datei Zaehne bekommt statt still durchzurutschen. Jeder Block wird nach `~/.claude/state/bug-almanac-blocks.log` protokolliert. Notaus: `bug-almanac-disable.flag` im TEMP. FAIL-OPEN bei jedem Hook-Fehler. | sobald eine bereichstypische Datei angefasst wird | **Stufe 2 (Erzwingung)**: blockiert am konkreten Ausloeser |
| 2b | **Fehler-Bruecke** | `bug-case-auto-writer` Hook (PostToolUseFailure) verbindet die reaktive `bug-cases.jsonl` mit dem proaktiven Almanach: bei einem NEUEN Fehler (kein bekannter Fix) haengt er einen "ALMANACH-BRUECKE"-Hinweis an (passenden `bugs/<bereich>.md` pruefen; bei hartnaeckigem Fehler ohne Almanach → Recherche mit Franks OK). | bei jedem Tool-Fehler | Stufe 1–2: verbindet beide Ebenen |
| 3 | **Verhalten** | Regel `~/.claude/rules/known-bugs-before-coding.md` | immer geladen | Stufe 1: Verhaltensanweisung |

Beide Hooks: Cross-Platform (`.ps1` + `.sh`). Der Index-Hook ist nicht blockierend
(injiziert nur Kontext). Der Guard-Hook ist seit 2026-06-02 **blockierend** (Stufe 2):
er stoppt Edit/Write per `permissionDecision:deny` + `exit 0` (NICHT `exit 2` — das
blockt Write/Edit nicht, siehe bugs/claude-tooling/claude-hooks.md 1.6), bis ZUERST der
Almanach UND DANN (seit 2026-06-07, falls vorhanden) die zugehoerige Best-Practices-Datei
des Bereichs in dieser Session gelesen wurde. Die Reihenfolge ist automatisch erzwungen: der
BP-Block wird erst erreicht, wenn der Almanach-Block bereits durchfiel (= Almanach gelesen).
Die BP-Datei wird per Pfad-Ableitung gesucht (`best-practices-<almKey>.md` rekursiv unter
`best-practices/`); existiert keine, zaehlt nur der Almanach. FAIL-OPEN: jeder
interne Hook-Fehler → durchlassen, nie faelschlich blockieren. Notaus via
`bug-almanac-disable.flag` im TEMP-Verzeichnis. Registriert in `~/.claude/settings.json`,
gespiegelt in `claude-code-setup/hooks/`.

---

## 4. Der vollstaendige Ablauf bei einer Aufgabe

```
1. Bereich + (falls relevant) Version erkennen — woran arbeite ich?
2. Greift das System? (siehe §6 Schwelle) — bei trivialem Kleinkram: nein, weiter.
3. Almanach im Index vorhanden?
   ├─ JA  → Kurzcheck lesen (Read mit limit=80; HOCHRISIKO-Bereich: Volltext, §11) → Version live abgleichen
   │        ├─ gleiche/aeltere Version als dokumentiert → weiter zu 3b
   │        └─ neuere Version als dokumentiert → Frank melden, OK fuer kurzen
   │           Re-Check einholen (existieren die Bugs noch? neue dazugekommen?) → weiter zu 3b
   └─ NEIN → der Guard BLOCKIERT den Edit (Quittungs-Mechanismus). Frank melden:
            „neuer Bereich X, kein Almanach — recherchieren?"
            ├─ JA (echte Bereichsarbeit) → auf sein OK warten → Skill `bug-almanach-recherche`
            │   (3–5 Researcher, gedeckelt, siehe §5) → Almanach anlegen → in README.md eintragen
            └─ NEIN / nur Kleinkram → bewusste Quittung `bug-almanac-ack-<slug>.flag` (TEMP) anlegen
                → Bereich fuer diese Session frei (NIE reflexhaft, nur nach Franks Entscheidung)
3b. Zweite Seite: existiert eine `best-practices/<kategorie>/<bereich>.md`?
    → JA: komplett lesen (wie man es von vornherein richtig macht). Der Guard erzwingt das
      ohnehin in der Reihenfolge erst Almanach, dann Best Practices. → NEIN: nur Almanach zaehlt.
4. ARBEITEN — mit den bekannten Bugs UND den Best Practices im Kopf, damit Fehler gar nicht erst entstehen.
5. Tritt ein Fehler auf → STUFE B: SOFORT den VOLLTEXT des Almanachs lesen (Read ohne limit)
   → ist das ein bekannter Bug?
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
- **Strukturiertes `Anker:`-Feld (seit 2026-06-15).** Software-gebundene Almanache tragen
  direkt unter dem `Stand:`-Vermerk ein maschinenlesbares Feld
  `> **Anker:** <label>=<version>` (mehrere kommagetrennt), z. B. `> **Anker:** claude-code=2.1.177`.
  `bugs/check-version-anchor.py` (mitgefuehrt von `health.py`) prueft je Eintrag seiner kuratierten
  Tabelle: (a) ist das Anker-Feld vorhanden? (b) — NUR wo die **installierte** Version == der fuer
  den Almanach relevanten ist (claude-code, python) — stimmt die **Live-Version** (Major.Minor) noch
  mit dem Anker? Bei projekt-gebundenen Bereichen (kotlin/gradle/compose/billing/dotnet/swift pinnt
  das Projekt eine eigene Version via Gradle/.csproj/Toolchain) gibt es bewusst KEINEN Live-Abgleich
  (sonst Falschalarm), nur die Anker-Vollstaendigkeit. So faellt ein Almanach auf, der zwar
  datums-frisch (< 180d), aber versions-veraltet ist (z. B. neue Major-Version erschienen).
  Neuen software-gebundenen Almanach anlegen → Eintrag in `ANCHORS` (in `check-version-anchor.py`)
  ergaenzen UND das `Anker:`-Feld im Header setzen.

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
4. Existiert eine `best-practices/<kategorie>/<bereich>.md`: die
   wechselseitige Bezugs-Tabelle in BEIDEN Dateien anlegen (siehe §9) und
   `python3 bugs/check-coupling.py` ausfuehren.

---

## 9. Kopplung mit den Best-Practices (zwei Seiten einer Medaille)

Der Bug-Almanach sagt *was schiefgeht und wie man es loest*; der Ordner
`~/proggs/best-practices/<kategorie>/<software>.md` sagt *wie man es von
vornherein richtig macht, damit der Bug nie entsteht*. Beide gehoeren zusammen und werden in
BEIDE Richtungen gepflegt — keine Einbahnstrasse:

**Seit 2026-06-07 ist die Kopplung auch beim LESEN erzwungen, nicht nur beim Pflegen:** Der
`bug-almanac-guard` (§3, Schicht 2) blockiert bereichstypische Edits, bis ZUERST der Almanach UND
DANN die zugehoerige Best-Practices-Datei in der Session gelesen wurde. Damit greift die "zwei
Seiten"-Idee genau dort, wo sie am meisten zaehlt: vor der ersten Code-Aenderung. Die folgenden
Pflege-Richtungen (Schreiben) bleiben unveraendert.

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

**Guard-Coverage-Check.** `python3 bugs/check-guard-coverage.py` prueft, ob JEDER Almanach vom
`bug-almanac-guard` ueber ein Dateimuster/Inhalts-Signal erzwungen wird (`[OK]`), bewusst als
Querschnitt gelistet ist (`[BEWUSST]`, Allowlist im Skript) oder ungemappt durchrutscht
(`[LUECKE]`). Rein lesend, immer `exit 0`. Nach jedem neuen Almanach ausfuehren: zeigt er `[LUECKE]`,
im Guard ein Signal ergaenzen ODER (bei echtem Querschnitt) in die Allowlist aufnehmen.

---

## 10. Bewusste Grenzen von v1 (kommende Verbesserungen)

- Das Dateimuster→Almanach-Mapping im Guard-Hook ist weiterhin hartkodiert (welcher
  Dateipfad/welche Endung zu welchem Almanach gehoert) — klein, erweiterbar. Der
  KATEGORIE-Pfad eines Almanachs ist hingegen seit 2026-06-03 NICHT mehr hartkodiert:
  der Hook sucht die Almanach-Datei rekursiv unter `bugs/` (kategorie-robust). Spaeter
  evtl. auch das Dateimuster-Mapping aus `README.md` auslesen, damit nur eine Stelle
  gepflegt werden muss.
- **GESCHLOSSEN am 2026-06-15 (Coverage-Selbsttest, Poka-Yoke Stufe 3):** Bislang konnte ein
  Almanach existieren, ohne dass der Guard ihn je erzwingt (kein passendes Dateimuster/Signal) —
  er rutschte still durch ODER wurde vom uebergeordneten Sprach-Almanach verdeckt (wie frueher
  `room` unter `android-platform`). Jetzt prueft `python3 bugs/check-guard-coverage.py` jeden
  Almanach gegen das Guard-Mapping und meldet `[OK]`/`[BEWUSST]`/`[LUECKE]` — kuenftige ungemappte
  Almanache fallen sofort auf. Im selben Zug 13 Luecken geschlossen: Inhalts-Erkennung im Guard
  fuer `voice-assistant-trigger`, `workmanager-notifications`, `google-drive-backup`,
  `3d-filament-android` (.kt), `windows-overlay`, `3d-dotnet-directx-windows`, `whisper-stt-lokal`
  (.cs), `macos-overlay`, `3d-metal-scenekit-macos` (.swift), `3d-threejs-webgpu` (.ts) + Dateiname-
  Zweige fuer `3d-godot` (.gd/.tscn/.gdshader) und `3d-rust-wgpu-bevy` (.rs/Cargo.toml mit Bevy/wgpu —
  behebt den frueheren `rust.md`-Fehlalarm). Strikt funktionserhaltend (service/worker.kt ohne
  Feature-Signal bleibt `android-platform`, reines Rust faellt weiter in den `rust.md`-Platzhalter,
  alle bestehenden Signale unveraendert). Bewusst NICHT datei-erzwungen bleiben Querschnitts-/
  Prozess-Almanache (`apis/*`, `agents/orchestrator-agent`, `cowork`, `claude-code-desktop-vs-cli`,
  `voice-pipeline`, `3d-visual-quality`, `play-store-release`) — sie stehen in der Allowlist des
  Coverage-Skripts und werden ueber Index + Stichworte gefunden.
- Schwellen-Erkennung („echte Bereichsarbeit vs. Kleinkram") laeuft ueber mein
  Urteil; falls das in der Praxis zu oft daneben liegt, schaerfen wir nach.
- **GESCHLOSSEN am 2026-06-10 (Digest-Modell, §11):** Der Volltext-Lese-Zwang fuer ALLE
  Bereiche kostete ~16k-23k Tokens pro Bereich/Session. Jetzt: Kurzcheck vorab (Stufe A),
  Volltext nur bei Fehler (B) und Hochrisiko (C).
- **GESCHLOSSEN am 2026-06-07:** Frueher rutschten (a) erkannte Bereiche OHNE Almanach
  und (b) komplett neue Sprachen ohne Mapping still durch (nur Hinweis bzw. gar nichts).
  Jetzt blockiert der Guard beide Faelle mit Quittung, und eine generische Endungs-Whitelist
  faengt neue Sprachen ab. Die generische Whitelist ist selbst hartkodiert (erweiterbar) —
  bei einer wirklich exotischen Endung ausserhalb der Liste greift der Trigger noch nicht;
  dann bewusst Almanach anlegen lassen. Restkante, bewusst akzeptiert.
- Das System wird in Aktion erprobt und nach Direktive #1 (Superintelligenz)
  iterativ verbessert.

- **W3-4 EVALUIERT (2026-06-15): Agent Skills als nativer Digest-Traeger?** Frage: Koennen native
  Claude *Agent Skills* das Digest-Modell (Kurzcheck/Volltext, §11) tragen statt der
  `Read`-mit-`limit` + Guard-Hook-Konstruktion?
  - **Konzeptionell passt es (PRO):** Ein Skill IST progressive disclosure — `SKILL.md` = der immer
    relevante Kern (≙ Kurzcheck Stufe A), referenzierte Dateien = on-demand Volltext (≙ Stufe B/C);
    `agent-requested`-Triggering (Claude laedt den Skill anhand der `description`) ≙ semantischer Trigger.
  - **Warum es das System NICHT ersetzt (CONTRA, entscheidend):** (1) **Keine Erzwingung** — Skills sind
    advisory (claude-config §1.1: Kontext, nicht enforced), Claude KANN sie ignorieren; der KERN hier ist
    der `bug-almanac-guard`, der Edits per `permissionDecision:deny` BLOCKIERT bis gelesen (Poka-Yoke
    Stufe 2) — genau das, was beim Chrome-Bug fehlte. (2) **Description-Budget** — 58 Almanache als 58
    Skills sprengen das 15000-Zeichen-Skill-/Command-Budget (claude-config §4.3) → Skills fallen still
    heraus. (3) **Menue-Flut + Session-Start-Discovery** (§4.4). (4) **Kein Versions-Abgleich** (das
    Datei-System hat Stand-Header + `check-version-anchor.py`, W3-1).
  - **EMPFEHLUNG: Almanache NICHT zu Skills umbauen.** Das Datei-Digest (§11) + Guard liefert das
    Skill-Prinzip (progressive disclosure) OHNE die Skill-Nachteile — und vor allem mit ERZWINGUNG
    statt advisory. Der `agent-requested`-Aspekt (semantisches Triggern VOR dem Datei-Edit) ist seit
    W3-2 durch den `bug-almanac-hint`-Prompt-Trigger (Schicht 1b) abgedeckt, ohne die Almanache selbst
    zu Skills zu machen. Einzelne sehr breite Bereiche koennten optional als ergaenzende Meta-Skills
    angeboten werden — als Zusatzschicht, nicht als Ersatz; derzeit kein Bedarf.


---

## 11. Das Digest-Modell (Stufe A/B/C — seit 2026-06-10)

**Problem (Frank, 2026-06-10):** Der Volltext-Lese-Zwang kostete pro Bereich und Session
~16.000-23.000 Tokens (Almanache sind 7-90 KB gross), obwohl die meisten Aufgaben fehlerfrei
durchlaufen. Zu viel immer-geladener Ballast senkt zudem die Genauigkeit (Context-Rot).

**Loesung:** Jeder Almanach und jede Best-Practices-Datei traegt oben eine kompakte
**„⚡ Kurzcheck"-Sektion** (Erkennungssignale + Sofort-Regeln). Drei Stufen regeln den Lese-Umfang:

| Stufe | Wann | Was lesen | Erzwingung |
|-------|------|-----------|------------|
| **A** | vor jeder echten Arbeit im Bereich | NUR den Kurzcheck (`Read` mit `limit=80`) — erst Almanach, dann Best Practices | Guard: read-Marker (jedes Read setzt ihn) |
| **B** | ab dem ERSTEN Fehler im Bereich | VOLLTEXT des Almanachs (`Read` ohne `limit`) | `bug-case-auto-writer` haengt an jeden Tool-Fehler den Stufe-B-Hinweis |
| **C** | Hochrisiko-Bereiche: `r8`, `firebase-billing`, `claude-hooks`, `claude-config` | VOLLTEXT schon VORAB | Guard: full-Marker — der Read-Zweig setzt ihn nur bei `Read` ohne `limit` (oder `limit>=500`); Bash-`cat`/`bat`/`less` zaehlt als Volltext |
| **D** | Bug ist WIEDERKEHREND (schon einmal gefixt, tritt erneut auf) | Kurzcheck ÜBERSPRINGEN, sofort VOLLTEXT von Almanach UND Best Practices; ohne Loesung dort → Grundproblem recherchieren (`research`-Skill) | Keine Hook-Erzwingung — Erkennung liegt beim Modell/Frank; verankert in `known-bugs-before-coding.md` |

**Format der Kurzcheck-Sektion (verbindlich, auch fuer neue Almanache):**

- Ueberschrift woertlich: `## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)`
- Position: direkt NACH dem einleitenden Blockquote (Stand-Header), VOR dem ersten `---`
- Blockquote mit Digest-Modell-Verweis auf §11 (Hochrisiko-Variante bei Stufe-C-Bereichen)
- Tabelle `| # | Signal / Situation | Sofort-Regel | Volltext |` (Best-Practices-Dateien:
  `| # | Situation | Best Practice (Kurzform) | Volltext |`), 5-15 Zeilen, wichtigste/
  haeufigste Fallen zuerst (⭐/KRITISCH), Volltext-Spalte = Abschnittsnummer der Datei
- Sektion gesamt <= 30 Zeilen und vollstaendig innerhalb der **ersten 80 Zeilen** der Datei
  (der Stufe-A-Read ist `limit=80` — was darunter liegt, wird nicht gesehen!)
- Pflege: Jeder neue Top-Bug wird AUCH im Kurzcheck ergaenzt; der `bug-almanach-recherche`-Skill
  legt die Sektion bei neuen Almanachen direkt mit an.

**Marker-Mechanik im Guard:** `bug-almanac-read-<key>.flag` (jedes Read) +
`bug-almanac-full-<key>.flag` (nur Volltext-Read; Transcript-Fallback erkennt ein Read ohne
`limit`-Feld). Beide loescht der Index-Hook bei Session-Start (Wildcard `bug-almanac-*.flag`).
Die Hochrisiko-Liste lebt in BEIDEN Guard-Varianten (`$highRiskKeys` in der .ps1, `case`-Liste
in der .sh) und in der Regel `known-bugs-before-coding.md` — alle drei synchron halten.

**Verlustfrei (lossless-context-principle):** Nichts wird weggeworfen — der Volltext bleibt die
Referenz, ist per Pfad jederzeit erreichbar und bei B/C weiterhin erzwungen. Der Kurzcheck ist
die Erkennungs-Antenne fuer stille Fehler bei ~3-5 % der frueheren Token-Kosten.
