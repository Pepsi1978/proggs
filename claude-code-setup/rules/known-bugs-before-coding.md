# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Poka-Yoke Stufe 3. **Volltext (Pflicht-Ablauf, Automatik-Hooks, Harness-Tabelle):
> `claude-code-setup/docs/rules/known-bugs-before-coding.md`.** System: `~/proggs/bugs/SYSTEM.md`.

## Grundregel
Pro Technologie-Bereich gibt es einen kuratierten Bug-Almanach `~/proggs/bugs/<kat>/<bereich>.md`. BEVOR
an einem Bereich mit Almanach gearbeitet wird, MUSS dessen **Kurzcheck** ZUERST gelesen werden — noch vor
der ersten Code-Aenderung. So werden bekannte Fehler gar nicht erst gemacht. (`bug-cases.jsonl` ist
REAKTIV nach einem Fehler; der Almanach PROAKTIV vorher.)

## Digest-Modell: 4 Stufen
- **A — Kurzcheck vorab** (vor JEDER echten Bereichsarbeit): `Read limit=80` auf Almanach, DANN
  Best-Practices. Vom `bug-almanac-guard` erzwungen (Reihenfolge: erst Almanach, dann BP).
- **B — Volltext bei Fehler** (ab dem ERSTEN Fehler): Almanach-VOLLTEXT (`Read` ohne limit).
- **C — Volltext vorab** (Hochrisiko `r8`, `firebase-billing`, `claude-hooks`, `claude-config`): Almanach-VOLLTEXT VORAB.
- **D — Wiederkehrender Bug** (schon gefixt, tritt ERNEUT auf): Kurzcheck ueberspringen → VOLLTEXT von
  Almanach UND Best-Practices; keine Loesung → Grundproblem per `research`-Skill recherchieren.

Kein Almanach → Guard blockiert, Frank melden, auf OK warten, dann Skill `bug-almanach-recherche` starten
(nicht selbst ad hoc). Trivialer Kleinkram (String/Doku/Bump) ausgenommen. Gilt AUCH fuer Harness-Arbeit
(Hook → `claude-hooks.md` Stufe C; CLAUDE.md/Regel/Settings/Skill → `claude-config.md` Stufe C).

## Was NIEMALS
- An einem Bereich mit Almanach arbeiten ohne Kurzcheck (A) · nach einem Fehler ohne Volltext (B) · bei
  wiederkehrendem Bug nur Kurzcheck statt Volltext (D) · Best-Practices ueberspringen · einen erlebten
  Bug fixen ohne ihn im Almanach zu ergaenzen · einen Bug durch Entfernen von Funktionalitaet "loesen".
