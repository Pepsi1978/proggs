# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Poka-Yoke Stufe 3. **Volltext (Pflicht-Ablauf, Automatik-Hooks):
> `claude-code-setup/docs/rules/known-bugs-before-coding.md`.**

## Grundregel
Pro Technologie-Bereich gibt es einen kuratierten Bug-Almanach `~/proggs/bugs/<kat>/<bereich>.md`. BEVOR
an einem Bereich mit Almanach gearbeitet wird, MUSS der **Kurzcheck** ZUERST gelesen werden — vor der
ersten Code-Aenderung. (`bug-cases.jsonl` = REAKTIV; Almanach = PROAKTIV.)

## Digest-Modell: 4 Stufen
- **A — Kurzcheck vorab** (vor JEDER Bereichsarbeit): `Read limit=80` Almanach, DANN Best-Practices; vom `bug-almanac-guard` erzwungen.
- **B — Volltext bei Fehler** (ab dem ERSTEN Fehler): Almanach-VOLLTEXT.
- **C — Volltext vorab** (Hochrisiko `r8`, `firebase-billing`, `claude-hooks`, `claude-config`): VOLLTEXT VORAB.
- **D — Wiederkehrender Bug**: Kurzcheck ueberspringen → VOLLTEXT Almanach + BP; keine Loesung → `research`-Skill.

Kein Almanach → Guard blockiert, Frank melden, auf OK warten, dann `bug-almanach-recherche` starten (nicht
ad hoc). Kleinkram (String/Doku/Bump) ausgenommen. Gilt AUCH fuer Harness (Hook → `claude-hooks.md` C; CLAUDE.md/Regel → `claude-config.md` C).

## Was NIEMALS
- Bereich mit Almanach ohne Kurzcheck (A) · nach Fehler ohne Volltext (B) · wiederkehrender Bug nur
  Kurzcheck (D) · Best-Practices ueberspringen · erlebten Bug nicht im Almanach ergaenzen · Bug durch Entfernen von Funktionalitaet "loesen".
