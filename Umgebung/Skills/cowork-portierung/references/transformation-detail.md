# Transformations-Detail: CLI-Skill → Cowork-Fassung

Diese Datei vertieft den Ablauf aus der SKILL.md mit einem konkreten Vorher/Nachher-Beispiel, einem
Pfad-Mapping und einer ausführlichen Checkliste. Lies sie, bevor du den ersten Skill portierst — danach
reicht meist die SKILL.md allein.

---

## 1. Die feste Abschnitts-Schablone der Cowork-Fassung

Jede portierte Cowork-`SKILL.md` folgt derselben Reihenfolge (so sehen `best-practices` und `research`
im `skills-src/` aus). Halte dich daran, dann sind alle Cowork-Skills konsistent:

```
---
name: <gleich wie Original = Ordnername>
description: "<≤200 Zeichen, einzeilig, Kernzweck zuerst, dann 'Trigger: …'>"
---

# <Lesbarer Name> (Cowork-Fassung) — <kurze Funktion>

<2–3 Sätze Intro. Endet mit: "Läuft in der Claude-Cowork-Desktop-App.">

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)
<Ergebnisse RELATIV im verbundenen Arbeitsordner; Ziel-Struktur als Tabelle/Baum; Ordner-anlegen-Pflicht.>

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)
<Mount-Schreibfalle (Dateiende prüfen); ~45s-Shell-Limit; Git NIEMALS nackt → cowork-git.sh.>

---

<DER FACHLICHE BODY — verlustfrei gekürzt aus dem Original. Alle Schritte/Regeln/Formate bleiben.>

## Sichern (Cowork-Git)
<bash ~/proggs/cowork-git.sh setup  +  push-files "#NNN - …" <relative pfade>>

## Was NIEMALS passieren darf
<fachliche Verbote aus dem Original + Cowork-Verbote (nacktes git, Dateiende, >7 Researcher …)>

## Referenzen
<relative Pfade im Arbeitsordner statt ~/proggs/…>
```

---

## 2. Frontmatter-Transformation (Vorher/Nachher, echtes Beispiel)

So wurde `best-practices` portiert — das ist das Muster für die `description`:

**VORHER (CLI-Original, `~/.claude/skills/best-practices/SKILL.md`):** mehrzeiliger YAML-`>`-Block,
weit über 200 Zeichen:
```yaml
description: >
  Recherchiert und pflegt Best Practices in zwei Bereichen: (1) die eigenen
  Claude-Code-Werkzeuge (den "Harness"): Hooks, Skills, Agents, Plugins, MCP-Server,
  Slash-Commands, Settings, Kontext-Management, Token-Effizienz, Arbeitsweise; und (2)
  die Software/Sprachen, die in den Projekten benutzt werden (Kotlin, Swift, Gradle, …)
  … [viele weitere Zeilen]
```

**NACHHER (Cowork-Fassung, `~/proggs/Cowork/skills-src/best-practices/SKILL.md`):** eine Zeile, in
Anführungszeichen, ≤ 200 Zeichen, Kernzweck vorn, Trigger hinten:
```yaml
description: "Recherchiert und pflegt Best Practices fuer Harness-Werkzeuge und Projekt-Software (Kotlin, Swift, Gradle), speichert sie im Arbeitsordner. Trigger: Best-Practices recherchieren, was ist neu in X."
```

Vorgehen: Den Kernzweck in einem Satz, dann die 2–3 stärksten Auslöse-Phrasen als `Trigger: …`.
Keine spitzen Klammern `<…>`, keine URLs (Cowork-Validator lehnt beides still ab). Danach Zeichen zählen.

---

## 3. Pfad-Mapping (CLI → Cowork)

Alle festen `~/proggs/…`-Pfade werden relativ — mit EINER Ausnahme. Typische Ersetzungen:

| CLI-Original (fest) | Cowork-Fassung (relativ zum Arbeitsordner) |
|---------------------|--------------------------------------------|
| `~/proggs/best-practices/<kat>/<x>.md` | `best-practices/<kat>/<x>.md` |
| `~/proggs/bugs/<kat>/<x>.md` | `bugs/<kat>/<x>.md` |
| `~/proggs/<projektordner>/…` | `<projektordner>/…` |
| Script-DataDir-Default `~/proggs/best-practices` | `./best-practices` (relativ) |
| **`~/proggs/cowork-git.sh`** | **bleibt `~/proggs/cowork-git.sh`** (Skript liegt dort, nicht im Mount) |

Begründung: Cowork mountet einen Arbeitsordner (üblicherweise `proggs`), aber NICHT unter dem festen
Pfad `~/proggs`. Ein fest verdrahteter Pfad schreibt am gemounteten Ordner vorbei. `cowork-git.sh` ist
die Ausnahme, weil das Wrapper-Skript bewusst außerhalb des Mounts auf der VM-Seite liegt.

---

## 4. Verlustfreie Kürzung — wie man es richtig macht

Der häufigste Fehler ist, beim Kürzen versehentlich Funktionalität wegzuwerfen. Merke:

- **Behalte jede Entscheidung und jede Regel.** Wenn das Original sagt „7 Researcher parallel, 429-Backoff,
  kein Findings-Cap, Quellen-Rangordnung", muss das in der Cowork-Fassung GENAUSO drinstehen — das ist die
  Substanz, nicht der Ballast.
- **Kürze nur Erklär-Text, Wiederholung und CLI-Mechanik.** Lange Begründungs-Absätze → ein Satz. Drei
  Tabellen, die dasselbe sagen → eine. Verweise auf CLI-Hooks (`bug-almanac-guard`, `subagent-context`),
  die es in Cowork nicht gibt → streichen oder durch eine direkte Anweisung ersetzen.
- **Test im Kopf:** „Kann die Cowork-Fassung noch alles, was das Original konnte?" Wenn nein → du hast zu
  viel gekürzt. Die ~40–50 % Größenreduktion entstehen durch Verdichtung, nicht durch Weglassen von Können.

Beispiel (research): Das CLI-Original erklärt die Persistenz-Regel über mehrere Absätze; die Cowork-Fassung
verdichtet sie auf eine Tabelle „Fund | Tauglich? | Ziel" + zwei Sätze — die **Entscheidungslogik bleibt
vollständig**, nur die Prosa schrumpft.

---

## 5. Ausführliche Checkliste (vor dem ZIP-Bau)

**Frontmatter**
- [ ] `name` = Ordnername, unverändert.
- [ ] `description` einzeilig in `"…"`, ≤ 200 Zeichen (gezählt!), Kernzweck vorn, `Trigger:` hinten.
- [ ] Keine `<…>`, keine URL in der `description`.
- [ ] Kein `paths:`-Feld (macht Skills undiscoverable).

**Aufbau**
- [ ] Titel trägt `(Cowork-Fassung)`.
- [ ] Intro endet mit „Läuft in der Claude-Cowork-Desktop-App."
- [ ] `## 0.` (Ablage relativ + Ordner-anlegen-Pflicht) vorhanden.
- [ ] `## 0a.` (Mount-Falle / ~45s / kein nacktes Git) vorhanden.

**Body (verlustfrei)**
- [ ] Alle Arbeitsschritte des Originals vorhanden.
- [ ] Alle inhaltlichen Regeln (Researcher-Regeln, Quellen-Rangordnung, Taxonomie, Formate) vorhanden.
- [ ] „Was NIEMALS passieren darf" enthält die fachlichen Verbote des Originals + Cowork-Verbote.

**Pfade & Mechanik**
- [ ] Feste `~/proggs`-Pfade → relativ (außer `~/proggs/cowork-git.sh`).
- [ ] Script-DataDir-Default relativ (`./…`).
- [ ] „Sichern (Cowork-Git)" nutzt `cowork-git.sh setup` + `push-files`.

**Begleitdateien**
- [ ] `scripts/` mitkopiert, Pfade relativ, LF.
- [ ] `references/`/`assets/` mitkopiert.

**Dann erst:** ZIP bauen (`build-cowork-zip.py <name>`), README-Tabelle pflegen, committen+pushen.

---

## 6. Häufige Fehler (und wie man sie vermeidet)

| Fehler | Folge in Cowork | Vermeidung |
|--------|-----------------|------------|
| `description` > 200 Zeichen | hinterer Teil (oft die Trigger) wird abgeschnitten → Skill triggert nicht | Zeichen zählen, hart kürzen |
| `<name>` / URL in `description` | „Plugin validation failed" (still) beim Upload | Platzhalter als `[name]`, URLs raus |
| ZIP mit `SKILL.md` im Root statt `<name>/SKILL.md` | Skill wird nach Upload nicht erkannt | `build-cowork-zip.py` nutzt die richtige Wurzel |
| CRLF / Backslash-Pfade im ZIP | bricht in der Linux-VM | `build-cowork-zip.py` normalisiert (nicht von Hand zippen) |
| Feste `~/proggs`-Pfade gelassen | Skill schreibt am Mount vorbei / findet nichts | Pfad-Mapping (§3) anwenden |
| Funktionalität „wegoptimiert" | Cowork-Skill kann weniger als das Original | verlustfrei kürzen (§4) |
