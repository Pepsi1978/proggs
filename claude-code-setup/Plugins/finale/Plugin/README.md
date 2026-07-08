# finale

Closed-Loop-Pipeline, die eine deutsche Android-App vor der Google-Play-Veröffentlichung **vollständig durchleuchtet**, **rechtssicher macht** und **layout-stabil mehrsprachig übersetzt** — ausschließlich durch Text-Änderungen, niemals durch Funktions- oder Layout-Eingriffe, bis 0 offene Rechts-Findings übrig sind.

Das Plugin ist ein **dünner Koordinations-Shell** um vier ausgereifte Skills, die im User-Home leben und über Symlinks eingebunden werden. Jede Skill-Verbesserung wird beim nächsten Plugin-Lauf automatisch wirksam.

---

## Was das Plugin tut

In sechs Phasen:

| Phase | Inhalt |
|---|---|
| **0** | Verifiziert Symlinks der vier Skills, berechnet Hash/mtime, dokumentiert Versions-Delta zum letzten Lauf, löst Reload aus. Pflicht, kein Skip. |
| **1** | Vollständiger Audit: Roentgen-Skill scannt Struktur + alle Texte + Hardcodes, Rechtssicherheits-Skill prüft alle Texte gegen deutsche Rechtslage + Play-Policies + jede Ziel-Jurisdiktion. |
| **2** | Interaktiver Fix-Workflow mit Pflicht-Karten: pro Finding zeigt das Plugin Datei+Zeile, Risikoampel (🟥🟧🟨), aktueller Wortlaut, 3 rechtssichere Vorschläge mit Längen-Delta in %, und vier Alternative-Aktionen. |
| **3** | Delta-Pipeline: Strings-Skill extrahiert verbleibende Hardcodes nach `strings.xml`, Übersetzer-Skill übersetzt alle Sprachen parallel mit ±15% Längenbudget, Rechtssicherheits-Skill macht Cross-Lingual-Recheck. |
| **4** | Re-Audit: hat ein Fix neue Probleme erzeugt? |
| **5** | Loop-Entscheidung: 0 offene Findings → fertig, sonst zurück zu Phase 2. Max 5 Iterationen. |

Maximal 5 Iterationen, danach erzwungene Pause mit Stagnations-Diagnose.

---

## Was das Plugin **nicht** anfasst

Harte Regel: **nur Text-Änderungen.** Erlaubt:

- `res/values*/strings.xml` (alle Locales)
- `res/values*/arrays.xml`, `res/values*/plurals.xml`
- XML-Attribute `android:label`, `android:contentDescription`, `android:hint`, `android:text`
- Text-Literale in `.kt`/`.java` (nur über `stringResource(R.string.…)` bzw. `getString(R.string.…)`)
- `AndroidManifest.xml` nur `android:label` (nicht Permissions oder Components)

Verboten ohne explizite Karten-Bestätigung pro Fall:

- Layout-XML-Struktur, Compose-Code (außer Text-Argumente), Kotlin/Java-Funktionslogik
- Permissions, Gradle-Dependencies, Drawables, Farben, Themes, Styles, Navigation-Graphen

Wenn der Rechtssicherheits-Skill erkennt dass ein Finding ohne Layout-/Funktions-Eingriff nicht lösbar ist, erscheint die **erweiterte Invasiv-Karte** mit konkretem Code-Snippet-Vorschlag. Du entscheidest pro Fall.

---

## Slash-Commands

| Command | Modus | Was passiert |
|---|---|---|
| `/finale:run` | default | Phase 0 → 1 → 2 → 3 → 4 → 5 (voller Closed Loop) |
| `/finale:audit-only` | audit-only | Phase 0 → 1 → Report. Keine Änderungen an der App. |
| `/finale:fix-only` | fix-only | Phase 0 → lädt bestehendes Audit → 2 → 4 → 5. |
| `/finale:strings` | strings-only | Phase 0 → Roentgen → Strings-Skill. Kein Rechtsaudit, keine Übersetzung. |
| `/finale:translate` | translate-only | Phase 0 → Übersetzer-Skill (parallel pro Sprache) → Cross-Lingual-Rechtsprüfung. |

Alle Commands akzeptieren optional einen Pfad zum Android-Projekt-Root als Argument; ohne Argument wird das aktuelle Verzeichnis genommen.

---

## Architektur

```
finale/
├── .claude-plugin/plugin.json
├── commands/                                 ← Slash-Command-Eintritte
│   ├── run.md             (/finale:run)
│   ├── audit-only.md
│   ├── fix-only.md
│   ├── strings.md
│   └── translate.md
├── agents/
│   ├── orchestrator.md    (Opus, effort: max — Phase-0…5-Dirigent)
│   ├── fix-applier.md     (Opus, effort: max — einziger File-Writer)
│   ├── url-checker.md     (Opus — HTTP-HEAD-Checks)
│   └── researcher.md      (Opus, effort: max — Wissenslücken)
├── skills/                                   ← SYMLINKS, keine Kopien
│   ├── roentgen-skill          → ~/.claude/skills/app-roentgen
│   ├── rechtssicherheits-skill → ~/.claude/skills/rechtssicherheit
│   ├── strings-skill           → ~/.claude/skills/string-extraktor
│   └── uebersetzer-skill       → ~/.claude/skills/übersetzung
├── scripts/
│   └── verify-skills.sh   (Phase-0-Tor-Wächter; POSIX, JSON-Output)
├── hooks/
│   ├── hooks.json
│   ├── session-start.sh                   (SessionStart: Symlink-Status, Post-Install-Wizard)
│   ├── session-start.ps1                  (PowerShell-Pendant fuer Windows ohne Git-Bash)
│   ├── pretooluse-bash.sh                 (PreToolUse Bash: blockt rm -rf, cat>, tee, cp/mv, dd auf geschuetzte Dateien)
│   ├── pretooluse-bash.ps1                (PowerShell-Pendant)
│   ├── audit-only-write-guard.sh          (PreToolUse Edit/Write: blockt App-Schreibversuche im audit-only-Modus)
│   ├── audit-only-write-guard.ps1         (PowerShell-Pendant)
│   ├── posttooluse-strings-xml.sh         (PostToolUse: strings.xml Apostroph-Lint)
│   └── posttooluse-strings-xml.ps1        (PowerShell-Pendant)
└── README.md
```

### Cross-Platform-Hooks

Jeder Hook hat zwei Implementierungen: `.sh` fuer Git Bash / Unix-Shells und `.ps1`
fuer native PowerShell. **Standardmaessig sind in `hooks/hooks.json` nur die `.sh`-
Hooks registriert** — sie funktionieren auf macOS, Linux und Windows mit Git Bash.

Auf einem nativen Windows-System ohne Git Bash muessen die Hook-Aufrufe in
`hooks/hooks.json` manuell auf die `.ps1`-Variante umgestellt werden:

```diff
- "command": "bash \"${CLAUDE_PLUGIN_ROOT}/hooks/session-start.sh\""
+ "command": "pwsh -NoProfile -ExecutionPolicy Bypass -File \"${CLAUDE_PLUGIN_ROOT}/hooks/session-start.ps1\""
```

Beide Implementierungen haben **identische Logik** — bei Aenderung der Patterns
in der einen IMMER die andere mitziehen. Das ist Defense in Depth: die Skripte
sind synchronisiert, der Schutz bleibt aktiv unabhaengig von der gewaehlten Shell.

---

## Skill-Symlink-Strategie

Die vier Skills leben in `~/.claude/skills/` und werden vom Nutzer dort gepflegt. Das Plugin bindet sie ausschließlich per **Symlink** ein:

```bash
ln -s ~/.claude/skills/app-roentgen      skills/roentgen-skill
ln -s ~/.claude/skills/rechtssicherheit  skills/rechtssicherheits-skill
ln -s ~/.claude/skills/string-extraktor  skills/strings-skill
ln -s ~/.claude/skills/übersetzung       skills/uebersetzer-skill
```

Symlink statt Kopie aus drei Gründen:

1. **Eine Quelle der Wahrheit.** Die echten Skills bleiben unter `~/.claude/skills/` und werden dort vom Nutzer mit dem Skill Creator iteriert. Das Plugin sieht sie automatisch.
2. **Kein Plugin-Rebuild bei Skill-Update.** Verbesserst du den Rechtssicherheits-Skill heute, läuft der nächste Plugin-Lauf morgen automatisch mit der neuen Version.
3. **Phase-0-Hash-Vergleich erkennt Skill-Änderungen.** `scripts/verify-skills.sh` berechnet den SHA-256 jeder `SKILL.md` und vergleicht mit `.android-shield/skill-versions.json` aus dem letzten Lauf. Geänderte Skills werden im Pre-Flight-Plan markiert.

### Symlink-Voraussetzung

Auf Windows ohne **Developer Mode** funktionieren native Symlinks nicht (Git-Bash fällt dann auf Kopien zurück — Hardlinks oder Junctions — was wir nicht wollen). Wenn die Symlinks bei dir nicht als `lrwxrwxrwx` angezeigt werden (`ls -la skills/`), aktiviere Developer Mode oder nutze das Plugin unter WSL/macOS/Linux.

Kopien sind bewusst nicht erlaubt — sie würden zu versioniertem Drift zwischen Skill-Quelle und Plugin-Version führen und Phase 0 nutzlos machen.

---

## Erstmalige Installation

```bash
# 1. Plugin liegt unter ~/.claude/plugins/local/finale/
# 2. Symlinks anlegen (mit nativen Windows-Symlinks)
cd ~/.claude/plugins/local/finale/skills
export MSYS=winsymlinks:nativestrict          # nur Git-Bash auf Windows
ln -sfn ~/.claude/skills/app-roentgen      roentgen-skill
ln -sfn ~/.claude/skills/rechtssicherheit  rechtssicherheits-skill
ln -sfn ~/.claude/skills/string-extraktor  strings-skill
ln -sfn ~/.claude/skills/übersetzung       uebersetzer-skill

# 3. Verifikation
bash ../scripts/verify-skills.sh
# Erwartet: {"ok": true, ...}  und Exit-Code 0

# 4. Plugin in Claude Code aktivieren (sofern noch nicht geschehen)
#    Die Datei liegt bereits unter ~/.claude/plugins/local/ — Claude Code lädt sie
#    beim nächsten Start automatisch.
```

---

## Maximum Intelligence

Damit alle Subagenten (Orchestrator, fix-applier, Übersetzer, researcher) tatsächlich auf höchstem Effort laufen, einmalig vor dem ersten Lauf setzen:

```bash
export CLAUDE_CODE_EFFORT_LEVEL=max
```

Hintergrund: Anthropic respektiert das `effort:`-Feld im Subagent-Frontmatter noch nicht zuverlässig (Stand 2026-05). Die Umgebungsvariable ist der Fallback. Sobald `effort:` im Frontmatter respektiert wird, kann die Variable wieder entfallen.

---

## Output-Dateien (pro App)

Alle Artefakte landen im **App-Root** unter `.android-shield/`:

```
<app-root>/.android-shield/
  skill-versions.json           Phase-0-Hash-Tabelle des letzten Laufs
  roentgen-report.json          vollständiges Strukturen+Strings-Inventar
  recht-report.json             alle Findings, Risiken, Fix-Vorschläge
  strings-plan.json             Hardcode-zu-strings.xml-Migrationsplan
  uebersetzungs-plan.json       Übersetzungsplan pro Sprache + Längen-Deltas
  audit-log.md                  Append-Only über alle Läufe (Mensch-lesbar)
  resume-state.json             nur bei Interrupt vorhanden
  manual-fixes-pending.md       akkumuliert manuell zu lösende Findings
```

`.android-shield/` solltest du **nicht ins App-Repository committen** — es enthält Audit-Stand-Aufnahmen und kann groß werden. Eintrag in `.gitignore` empfohlen.

---

## Beispiel-Läufe

### Beispiel 1 — Default Closed Loop nach Skill-Verbesserung

Du hast den Rechtssicherheits-Skill in `~/.claude/skills/rechtssicherheit/` gestern um eine neue Capability erweitert.

```
/finale:run ~/proggs/BestJournalAndroid
```

Phase 0 erkennt: Rechtssicherheits-Skill SHA hat sich seit letztem Lauf geändert. Im Pre-Flight-Plan steht:

```
Roentgen Skill           SHA: b8280e0754f6... (unverändert)
Rechtssicherheits Skill  SHA: 5956640d46ea... (geändert ✓ seit Lauf 2026-05-15)
Strings Skill            SHA: 29007fdc1187... (unverändert)
Übersetzer Skill         SHA: e7205988a2f3... (unverändert)
```

Du gibst frei mit `[F]`. Phase 1 läuft mit der neuen Version des Recht-Skills.

### Beispiel 2 — Toter Symlink

Du hast `~/.claude/skills/rechtssicherheit/` versehentlich umbenannt.

```
/finale:run
```

Phase 0 schlägt sofort fehl:

```
[verify-skills] FEHLER: Skill 'rechtssicherheits-skill': target-directory-missing
  (link=.../skills/rechtssicherheits-skill,
   expected target /home/.claude/skills/rechtssicherheit)

→ Orchestrator stoppt SOFORT.
→ Reparatur: ln -sfn ~/.claude/skills/<echter-name> .../skills/rechtssicherheits-skill
```

Es passiert nichts an der App — kein Lauf mit unvollständigem Skill-Set.

### Beispiel 3 — Wiederaufnahme nach Interrupt

Du hattest gestern Option `[7]` in der Fix-Karte gewählt um die Sitzung zu beenden. Heute:

```
/finale:fix-only ~/proggs/BestJournalAndroid
```

Phase 0 erkennt `resume-state.json` und schlägt Wiederaufnahme bei Finding #47 vor. Du bestätigst.

---

## Troubleshooting

**„Skill-Symlinks zeigen als reguläre Dateien, nicht als `lrwxrwxrwx`":** Du arbeitest auf Windows ohne Developer Mode. Aktiviere ihn (Settings → Privacy & Security → For Developers → Developer Mode), starte Git Bash neu, lege die Symlinks neu an. Falls das keine Option ist: WSL nutzen.

**„Phase 0 sagt `geändert ✓` obwohl ich nichts angefasst habe":** Der mtime der Datei hat sich geändert (z. B. weil ein Formatter durchlief) während der Inhalts-Hash gleich blieb. In diesem Fall ist `Δ unverändert` mit `mtime-only` aussagekräftig. Nicht beunruhigend.

**„Übersetzer-Längenbudget zu eng":** Bei manchen Sprachpaaren (z. B. DE → TR) ist ±15% nicht erreichbar. Der Übersetzer-Skill versucht Re-Phrasings; falls das scheitert, zeigt der Orchestrator eine Karte mit Alternative-Vorschlägen, die das Layout schonen.

**„Audit-Log wird sehr groß":** Append-Only ist Absicht — der Log ist die Historie aller Entscheidungen. Bei sehr großen Apps mit vielen Iterationen kannst du den Log nach einem erfolgreichen Release archivieren (`mv audit-log.md audit-log-pre-release-N.md`) und mit leerer Datei weitermachen.

---

## Erweiterbarkeit

- **Neuer Skill (z. B. accessibility-checker):** Lege ihn unter `~/.claude/skills/accessibility-checker/` an, ergänze einen Symlink unter `skills/` im Plugin, ergänze ihn in `scripts/verify-skills.sh` (Array `SKILL_NAMES` + Mapping `EXPECTED_TARGET`), und ergänze ihn im Orchestrator (Phase 1 Subagent-Liste).
- **Neue Capability in einem bestehenden Skill:** Direkt in `~/.claude/skills/<skill>/SKILL.md` ergänzen — kein Plugin-Update nötig. Phase 0 erkennt die neue Version automatisch.
- **Neuer Modus (z. B. `pre-release-check`):** Lege eine neue Datei unter `commands/` an, definiere den Modus im Orchestrator-Switch.

---

## Sprache

Alle Reports, Pre-Flight-Pläne, Karten, Logs und Nutzer-Dialoge sind **auf Deutsch**. Skill-Frontmatter-Descriptions sind englisch (damit Claude Codes Trigger-Mechanismus sie findet), aber die Trigger-Phrasen darin enthalten immer die offiziellen deutschen Namen: „Roentgen Skill", „Strings Skill", „Übersetzer Skill", „Rechtssicherheits Skill".

---

## Lizenz / Nutzung

Privat. Nicht für die Veröffentlichung im Marketplace gedacht.
