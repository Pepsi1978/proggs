---
name: cowork-portierung
description: "Portiert einen vorhandenen CLI-Skill in eine Cowork-taugliche Fassung (gekuerzt, Pfade relativ, Mount-Fallen) und legt Quelle + fertiges ZIP in proggs/Cowork/ ab. Nutze IMMER wenn der Benutzer sagt: portiere skill X zu cowork, mach X cowork-tauglich, Cowork-Version von X bauen."
---

# Cowork-Portierung — CLI-Skill → Cowork-Fassung + fertiges ZIP

Dieser Skill läuft in der **normalen Claude-Code-CLI** und *produziert* eine Cowork-taugliche
Fassung eines vorhandenen CLI-Skills. Ergebnis: die diffbare Quelle unter
`~/proggs/Cowork/skills-src/<name>/` **und** ein fertiges `~/proggs/Cowork/<name>.zip`, das der
Benutzer in der Cowork-Desktop-App nur noch per Drag&Drop hochziehen muss.

Warum es diesen Skill gibt: Cowork (Claude-Desktop-App) hat eine andere Umgebung als die CLI —
eine instabile Mount-Brücke, ein ~45s-Shell-Limit, kein nacktes Git, ein 200-Zeichen-Limit für
Skill-Beschreibungen und kein `~/proggs`-Pfad. Ein CLI-Skill 1:1 hochzuladen funktioniert nicht
zuverlässig. Diese Portierung passt genau diese Punkte an — **ohne fachliche Funktionalität zu
verlieren** (verlustfreie Kürzung, siehe §4).

---

## 0. Eingabe & Ablage-Orte (ZUERST klären)

**Eingabe:** Der Benutzer nennt einen Skill-Namen ("portiere best-practices zu cowork"). Das Original
liegt unter `~/.claude/skills/<name>/` und umfasst **alles** im Ordner: `SKILL.md`, `scripts/`,
`references/`, `assets/`. Findest du den Skill dort nicht → per `ls ~/.claude/skills/` prüfen und den
Benutzer um den genauen Namen bitten (nicht raten).

**Ausgabe (zwei Dinge, beide Pflicht):**

| Was | Pfad |
|-----|------|
| Diffbare Quelle (entpackt) | `~/proggs/Cowork/skills-src/<name>/` (SKILL.md + scripts/ + references/ …) |
| Fertiges Upload-ZIP | `~/proggs/Cowork/<name>.zip` (ZIP-Root = der Skill-Ordner) |

Beide gehören zusammen: Die Quelle ist die Wahrheit, das ZIP wird daraus gebaut (§5). Existiert eine
ältere Cowork-Version → überschreiben (Re-Portierung), aber vorher kurz `git status` ansehen, ob die
alte Version uncommittete Hand-Änderungen hat, die man nicht verlieren will.

---

## 1. Die Cowork-Restriktionen (das WARUM hinter jeder Anpassung)

Diese Punkte sind der Grund, warum portiert statt kopiert wird. Sie stammen aus dem Bug-Almanach
`bugs/claude-tooling/cowork.md` (§6 Skills) und `bugs/claude-tooling/cowork-git-push.md` — lies bei
Unsicherheit dort nach.

| # | Cowork-Eigenheit | Konsequenz für die Portierung |
|---|------------------|-------------------------------|
| 1 | `description` wird auf Claude.ai bei **200 Zeichen** abgeschnitten | description **≤ 200 Zeichen**, einzeilig in `"…"`, Trigger-Keywords front-loaden |
| 2 | Validator lehnt `<…>` und URLs im `description`-Feld still ab | KEINE spitzen Klammern, KEINE URLs in der description (Platzhalter als `[name]` schreiben) |
| 3 | `name` muss = Ordnername, nur `[a-z0-9-]`, keine reservierten Wörter | `name`-Feld unverändert lassen (Originale erfüllen das schon), Ordner = name |
| 4 | Gesamt-Beschreibungs-Budget im System-Prompt (~15k Zeichen) | description knapp halten — hilft dem Budget |
| 5 | **Mount-Brücke schneidet Dateienden ab** (Truncation beim Schreiben) | Im Skill-Text die Pflicht verankern: nach jedem Schreiben Dateiende prüfen (`tail -1`, `wc -l`) |
| 6 | **~45s-Shell-Limit**, Hintergrundprozesse überleben Aufruf-Wechsel nicht | Git-/Script-Schritte müssen in EINEM Aufruf laufen; Researcher laufen als Agenten (unkritisch) |
| 7 | **Kein nacktes Git** aus der VM (Locks, Mount-Fallen) | Abschluss IMMER über `bash ~/proggs/cowork-git.sh push-files …` |
| 8 | Kein fester `~/proggs`-Pfad — Cowork mountet einen Arbeitsordner | Alle Ablage-Pfade **relativ** zum verbundenen Arbeitsordner (Ausnahme: `~/proggs/cowork-git.sh`) |
| 9 | CLI-Hooks/Guards (`bug-almanac-guard`, `subagent-context` …) existieren in Cowork NICHT | Verweise auf CLI-Hooks streichen oder durch eine explizite Anweisung im Text ersetzen |
| 10 | Skills mit `references/`/`scripts/` brauchen den ZIP-Upload | scripts/references mitportieren, ZIP bauen (eine nackte `.md` reicht nur für reine Text-Skills) |
| 11 | Dateien müssen LF + UTF-8 (ohne BOM) sein | beim Schreiben LF halten; das ZIP-Skript normalisiert das |

---

## 2. Der Transformations-Ablauf (Schritt für Schritt)

Arbeite die Schritte der Reihe nach ab. Lies **vor dem ersten Schreiben** einmal die Vorlage und das
ausführliche Vorher/Nachher-Beispiel:
- **Goldstandard-Vorlagen** (so SOLL das Ergebnis aussehen): `~/proggs/Cowork/skills-src/best-practices/SKILL.md`,
  `~/proggs/Cowork/skills-src/research/SKILL.md` — die konsistente Cowork-Struktur (§0 / §0a / Sichern / Was-NIEMALS).
- **Detail-Anleitung mit Diff-Beispiel + voller Checkliste:** `references/transformation-detail.md` in diesem Skill.

### 2.1 Original vollständig einlesen
`SKILL.md` lesen und den Ordner mit `ls -R ~/.claude/skills/<name>/` erfassen. Verstehe die **fachliche
Kern-Logik** (was tut der Skill, welche Schritte, welche Regeln) — die musst du erhalten. Erkenne, welche
Teile **CLI-spezifisch** sind (Hooks, `~/proggs`-Pfade, „falls Shell"-fremde Annahmen) — die werden
angepasst, nicht blind übernommen.

### 2.2 Frontmatter transformieren (der heikelste Teil)
- `name`: **unverändert** lassen (= Ordnername).
- `description`: aus dem (oft mehrzeiligen `>`/`|`-Block) **eine einzige Zeile in `"…"`** machen,
  **≤ 200 Zeichen**. Kernzweck zuerst, dann `Trigger: …` mit den wichtigsten Auslöse-Phrasen. KEINE
  `<…>`, KEINE URLs. Beispiel-Muster (aus `research`):
  `"Recherchiert ein Thema mit einem Researcher-Schwarm und persistiert Funde in best-practices/ und Bug-Almanach. Trigger: recherchiere X, tiefe Recherche."`
  Zähl die Zeichen (`awk` / Python `len`) — 200 ist hart.

### 2.3 Titel + Intro
- Titel: `# <Lesbarer Name> (Cowork-Fassung) — <kurze Funktion>`.
- Intro (2–3 Sätze): was der Skill tut + **„Läuft in der Claude-Cowork-Desktop-App."** Etwaige
  Abgrenzungen zu Schwester-Skills aus dem Original übernehmen (kurz).

### 2.4 Die zwei Cowork-Blöcke einsetzen (§0 + §0a)
Direkt nach dem Intro IMMER diese beiden Blöcke einfügen (Wortlaut an den Goldstandard angelehnt):
- **`## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)`**: Ergebnisse RELATIV im verbundenen
  Arbeitsordner; konkrete Ziel-Struktur des jeweiligen Skills als Tabelle/Baum (relativ!); Ordner-anlegen
  ist Pflicht und erlaubt (`mkdir -p`, falls Shell verfügbar) — nie abbrechen, weil ein Ordner fehlt.
- **`## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)`**: Mount-Schreibfalle (Dateiende
  prüfen), ~45s-Shell-Limit (Agenten ok, Git/Schreiben in EINEM Aufruf), Git NIEMALS nackt → `cowork-git.sh`.
  Verweis: `bugs/claude-tooling/cowork.md` + `cowork-git-push.md` im Arbeitsordner.

### 2.5 Body verlustfrei kürzen — Was bleibt / Was geht (KRITISCH)
Das Ziel ist eine **kürzere, fokussiertere** Fassung (Cowork-Skills sind ~40–50 % kleiner) — aber
**ohne fachlichen Funktionsverlust**. Kürzen heißt Ballast auslagern/verdichten, NICHT Fähigkeit
wegwerfen (Lossless-Prinzip). Orientierung:

| BLEIBT (fachliche Substanz — niemals streichen) | GEHT / WIRD VERDICHTET (CLI-Ballast) |
|--------------------------------------------------|--------------------------------------|
| Alle Arbeitsschritte des Skills (der eigentliche Ablauf) | Lange Erklär-Prosa, doppelte Begründungen, Wiederholungen |
| Alle inhaltlichen Regeln (z.B. Researcher-Regeln, Quellen-Rangordnung, Taxonomie) | Verweise auf CLI-Hooks/Guards, die es in Cowork nicht gibt |
| Ausgabe-/Report-Formate, Pflicht-Felder | Ausführliche Vorher/Nachher-Code-Beispiele → auf das Nötige kürzen |
| „Was NIEMALS passieren darf" (fachlich) | Feste `~/proggs`-Pfade (→ relativ), CLI-spezifische Pfad-Hinweise |
| Kopplungen/Abhängigkeiten zu anderen Skills/Dateien | Redundante Tabellen, die dasselbe zweimal sagen |

Faustregel: Wenn ein gekürzter Satz eine **Fähigkeit oder Entscheidung** des Skills entfernt → nicht
kürzen. Wenn er nur **erklärt/wiederholt/CLI-Mechanik beschreibt** → kürzen. Im Zweifel behalten.

### 2.6 Pfade umstellen
Feste `~/proggs/…`-Pfade → **relativ** zum Arbeitsordner (z.B. `best-practices/…`, `bugs/…`). EINZIGE
Ausnahme: `~/proggs/cowork-git.sh` bleibt absolut (das Skript liegt dort). Bei Skripten mit einem
Ziel-/DataDir-Default: Default auf den relativen Arbeitsordner setzen (`./best-practices`), sonst
schreibt das Skript am gemounteten Ordner vorbei.

### 2.7 Scripts / references mitportieren
- `scripts/` (falls vorhanden): mitkopieren; Pfad-Defaults wie in 2.6 relativ machen; LF halten.
- `references/` (falls vorhanden): meist 1:1 übernehmen; nur wenn sie CLI-spezifische Pfade enthalten,
  analog anpassen.
- `assets/`: 1:1 übernehmen.

### 2.8 Abschluss-Abschnitte
- **`## Sichern (Cowork-Git)`**: zeigt `bash ~/proggs/cowork-git.sh setup` (auf „Push-Zugang OK" warten)
  + `bash ~/proggs/cowork-git.sh push-files "#NNN - <text>" <nur eigene relative pfade>`. „Kein Git-Repo
  verbunden → nur speichern und Ablage-Pfad nennen."
- **`## Was NIEMALS passieren darf`**: die fachlichen Verbote aus dem Original + die Cowork-Verbote
  (nacktes `git` aus Cowork; Dateiende nicht prüfen; >7 Researcher; etc.).
- **`## Referenzen`**: relative Pfade im Arbeitsordner statt `~/proggs/...`.

---

## 3. Ergebnis schreiben (Quelle anlegen)
Schreibe die transformierte `SKILL.md` und alle Begleitdateien nach `~/proggs/Cowork/skills-src/<name>/`
(Ordner ggf. `mkdir -p`). LF-Zeilenenden, UTF-8 ohne BOM, exakter Dateiname `SKILL.md`.

**Verifiziere sofort:**
- `awk 'NR==2{print length}'` auf die description-Zeile bzw. Python `len` → **≤ 200**? (Frontmatter-Zeile 2 ist `description:`; zähle nur den String in den Quotes.)
- description einzeilig, in `"…"`, ohne `<…>` und ohne URL?
- `tail -1` / `wc -l` auf jede geschriebene Datei (Truncation-Schutz, auch in der CLI eine gute Gewohnheit).

---

## 4. ZIP bauen (deterministisch, per Skript)
Das ZIP muss exakt Coworks Erwartung treffen: **ZIP-Root = der Skill-Ordner** (`<name>/SKILL.md …`),
LF-Zeilenenden, Forward-Slash-Pfade. Cowork führt Skills in einer Linux-VM aus — Backslash-Pfade im ZIP
(wie sie PowerShells `Compress-Archive` teils erzeugt) brechen das. Deshalb das gebündelte
Python-Skript nutzen (`zipfile` garantiert Forward-Slash, normalisiert LF) — nicht von Hand zippen:

```bash
python "${CLAUDE_SKILL_DIR}/scripts/build-cowork-zip.py" <name>
```

Das Skript liest `~/proggs/Cowork/skills-src/<name>/`, normalisiert Textdateien auf LF und schreibt
`~/proggs/Cowork/<name>.zip` mit dem Ordner `<name>/` als ZIP-Wurzel. Danach prüft es selbst den
ZIP-Inhalt (`<name>/SKILL.md` muss enthalten sein, alle Pfade mit `/`) und meldet die Dateiliste.
Python ist auf beiden Plattformen verfügbar — ein Skript genügt, kein sh/ps1-Divergenz-Risiko.

> Hinweis: `${CLAUDE_SKILL_DIR}` zeigt auf diesen Skill-Ordner. Falls die Variable mal nicht gesetzt
> ist, der absolute Pfad: `~/.claude/skills/cowork-portierung/scripts/build-cowork-zip.py`.

---

## 5. README-Tabelle pflegen
`~/proggs/Cowork/README.md` enthält eine Tabelle „Die fertigen Cowork-Skill-ZIPs". Ergänze/aktualisiere
die Zeile für den portierten Skill: `| <name>.zip | <name> | <ein Satz, was er in Cowork tut> |`.
Bleibt eine Zeile schon vorhanden (Re-Portierung) → aktualisieren statt doppeln.

---

## 6. Committen + pushen (normaler CLI-Workflow)
Dieser Skill läuft in der CLI — also **normaler** Git-Workflow (NICHT `cowork-git.sh`; das gehört nur
in die *produzierten* Cowork-Skills). Nur die eigenen neuen/geänderten Pfade namentlich stagen:

```bash
git -C ~/proggs add Cowork/skills-src/<name> Cowork/<name>.zip Cowork/README.md
git -C ~/proggs commit -m "#NNN - cowork-portierung: <name> nach Cowork portiert (Quelle + ZIP)"
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main
git -C ~/proggs push
```

`#NNN` = nächste fortlaufende Commit-Nummer (aus `git log` ableiten).

---

## 7. Selbst-Check vor „fertig"
- [ ] `description` ≤ 200 Zeichen, einzeilig in `"…"`, ohne `<…>`/URL, Trigger front-geladen?
- [ ] `name` = Ordnername, unverändert?
- [ ] §0 + §0a (Cowork-Blöcke) vorhanden, Pfade relativ?
- [ ] Fachliche Kern-Logik vollständig erhalten (kein Schritt/keine Regel verloren)? Nur Ballast gekürzt?
- [ ] Feste `~/proggs`-Pfade ersetzt (außer `~/proggs/cowork-git.sh`)? Script-DataDir relativ?
- [ ] „Sichern (Cowork-Git)" über `cowork-git.sh push-files`, „Was NIEMALS" um Cowork-Verbote ergänzt?
- [ ] ZIP gebaut, Root = `<name>/`, `SKILL.md` enthalten, LF?
- [ ] README-Tabelle aktualisiert?
- [ ] Committed + gepusht (CLI-Workflow, nur eigene Pfade)?

Danach dem Benutzer melden: Quelle + ZIP-Pfad nennen, „zum Hochladen in Cowork einfach
`Cowork/<name>.zip` reinziehen."

---

## Was NIEMALS passieren darf
- Fachliche Funktionalität beim Kürzen entfernen (Schritte, Regeln, Formate) — Kürzung ist verlustfrei,
  sonst ist es ein Feature-Verlust getarnt als Portierung.
- `description` über 200 Zeichen, mehrzeilig, mit `<…>` oder URL (Cowork lehnt das still ab / schneidet ab).
- Feste `~/proggs`-Pfade im Cowork-Skill stehen lassen (außer `~/proggs/cowork-git.sh`).
- In den produzierten Cowork-Skill nacktes `git commit`/`git push` schreiben (immer `cowork-git.sh`).
- Das ZIP von Hand mit falscher Wurzel bauen (Skill-Ordner MUSS die ZIP-Wurzel sein) — das Skript nutzen.
- Den Skill als fertig melden, ohne Quelle UND ZIP angelegt + committet zu haben.

## Referenzen
- `references/transformation-detail.md` — ausführliches Vorher/Nachher-Beispiel, Pfad-Mapping, volle Checkliste.
- `scripts/build-cowork-zip.py` — baut das Upload-ZIP (LF, Forward-Slash, Root = Ordner; plattformunabhängig).
- Goldstandard: `~/proggs/Cowork/skills-src/best-practices/`, `~/proggs/Cowork/skills-src/research/`.
- Cowork-Regeln: `~/proggs/bugs/claude-tooling/cowork.md` (§6 Skills), `~/proggs/bugs/claude-tooling/cowork-git-push.md`.
- `~/proggs/Cowork/README.md` — Tabelle der fertigen ZIPs + Upload-Anleitung.
