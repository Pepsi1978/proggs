# Entropie-Reduktion: Bei viel Entropie entropie-reduzierend reagieren (KRITISCH)

> Ausgeloest durch den Chrome-Extension-Bug (~1h Suche mit wirkungslosen Fixes fuer einen laengst
> dokumentierten Bug). Roter Faden hinter dem ganzen Bug-Almanach-System.

## Grundprinzip

Wenn VIEL ENTROPIE auftritt (Chaos, viele gescheiterte Versuche, kein Fortschritt), MUSS die Reaktion
lauten: **Entropie REDUZIEREN** — nicht weiter Chaos hinzufuegen. Hohe Entropie ist das SIGNAL, die
Strategie zu wechseln, nicht weiterzuwursteln.

## Entropie erkennen

3+ Fixes/Versuche die alle nicht wirken (= 3-Iterationen-Stop) · langes Flailing ohne Fortschritt am
selben Symptom (>15-30 Min) · gleicher Fehler wiederholt, immer neue ungetestete Fixes · "das kann doch
nicht sein, dass das so lange dauert".

## Entropie-reduzierend reagieren (statt mehr Chaos)

1. **STOPP** — aufhoeren, neue Fixes zu raten.
2. **Nachschlagen ob der Bug bekannt ist** — ZUERST lokal (Bug-Almanach `~/proggs/bugs/<bereich>.md` +
   `bug-cases.jsonl`), DANN Internet (offizielle Quellen/Issues/Foren — der Workaround existiert meist schon).
3. **Den EINFACHSTEN dokumentierten Fix zuerst** probieren, nicht an dem festhalten was man gerade angefasst hat.
4. **Vereinfachen statt verkomplizieren** — auf sauberen Ausgangszustand zurueck, eine Variable nach der anderen.

Beste Reduktion = Praevention: bekannte Bugs VOR der Arbeit nachschlagen (`known-bugs-before-coding`).

## Was NIEMALS passieren darf

- Bei wachsender Entropie weiter neue, ungetestete Fixes raten (= Entropie erhoehen)
- Lange flailing ohne nachzuschlagen ob der Bug bekannt/dokumentiert ist · das Signal "viel Entropie" ignorieren
