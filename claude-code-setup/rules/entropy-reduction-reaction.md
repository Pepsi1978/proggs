# Entropie-Reduktion: Bei viel Entropie entropie-reduzierend reagieren (KRITISCH)

> Quelle: Frank, 2026-06-02. Ausgeloest durch den Chrome-Extension-Verschwind-Bug
> (~1 Stunde Suche mit vielen wirkungslosen Fixes fuer einen Bug, den andere
> Internetnutzer laengst dokumentiert hatten). Frank wendet dieses Prinzip auch
> persoenlich auf sich an — und es ist der rote Faden hinter dem ganzen Bug-Almanach-
> System. Namensnah zu Franks App „Entropie Reductor".

---

## Grundprinzip

**Die Reaktion ist entscheidend.** Wenn VIEL ENTROPIE auftritt — Unordnung, Chaos, viele
gescheiterte Versuche, kein Fortschritt — MUSS die Reaktion lauten: **Entropie REDUZIEREN.**
Nicht weiter Chaos hinzufuegen. Hohe Entropie ist kein Grund weiterzuwursteln, sondern das
SIGNAL, die Strategie zu wechseln.

Kerngedanke (Franks Wortlaut sinngemaess): Es darf nicht sein, dass man eine Stunde sucht,
nur weil man einen Bug nicht kennt — der bei anderen laengst festgehalten ist.

---

## Entropie erkennen (die Signale)

- **3+ Fixes/Versuche, die alle nicht wirken** (= 3-Iterationen-Stop ist erreicht).
- **Langes Flailing ohne Fortschritt** am selben Symptom (Richtwert >15-30 Min).
- **Gleicher Fehler wiederholt**, wachsende Verwirrung, immer neue ungetestete Fixes.
- Das Gefuehl: „Das kann doch nicht sein, dass das so lange dauert."

---

## Entropie-reduzierend reagieren (statt mehr Chaos)

Mehr ungetestete Fixes = MEHR Entropie. Stattdessen, in dieser Reihenfolge:

1. **STOPP.** Aufhoeren, neue Fixes zu raten.
2. **Nachschlagen, ob der Bug bekannt ist** — andere hatten ihn fast sicher schon:
   - ZUERST lokal: Bug-Almanach (`~/proggs/bugs/<bereich>.md`) + `bug-cases.jsonl`.
   - DANN das Internet (offizielle Quellen, Issues, Foren) — der dokumentierte Workaround
     existiert meistens schon.
3. **Den EINFACHSTEN dokumentierten Fix zuerst** probieren (siehe `try-simplest-documented-fix-first`),
   nicht an dem festhalten, was man gerade angefasst hat.
4. **Vereinfachen statt verkomplizieren** — auf einen sauberen Ausgangszustand zurueck, eine
   Variable nach der anderen.

---

## Warum das wirkt

Entropie-Reduktion ist genau das Gegenteil von „panisch mehr ausprobieren". Jeder ungetestete
Fix in einem chaotischen Zustand erhoeht die Unordnung (neue Variablen, neue Seiteneffekte).
Nachschlagen + den bekannten, einfachen Fix anwenden senkt sie schlagartig. Die beste Form der
Reduktion ist Praevention: bekannte Bugs VOR der Arbeit nachschlagen (`known-bugs-before-coding`).

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| `known-bugs-before-coding.md` | Praevention = Entropie gar nicht erst entstehen lassen (vorher nachschlagen) |
| `feedback_try_simplest_documented_fix_first` | Den dokumentierten, einfachsten Fix zuerst statt weiter raten |
| 3-Iterationen-Stop | Die konkrete Schwelle, ab der „viel Entropie" gilt |
| Direktive #3 (Resilient Bugfixing) | Root Cause statt Symptom-Flailing; jeder Bug wird danach dokumentiert (kuenftige Entropie sinkt) |
| Bug-Almanach-System (`~/proggs/bugs/`) | Das materialisierte Entropie-Reduktions-Werkzeug |

---

## Was NIEMALS passieren darf

- ❌ Bei wachsender Entropie einfach weiter neue, ungetestete Fixes raten (= Entropie erhoehen).
- ❌ Lange flailing, ohne nachzuschlagen, ob der Bug bekannt/dokumentiert ist.
- ❌ Das Signal „viel Entropie" ignorieren und stur an einem Ansatz festhalten.
