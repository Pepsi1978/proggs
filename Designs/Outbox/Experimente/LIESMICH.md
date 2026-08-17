# LIESMICH — Experimente, Spec v1

Stand: 09.08.2026 · Erzeugt von: `spec-schmiede` · Herkunft: `Specs/Experimente/v1/`

---

## Zielplattform

**Android** — Samsung Galaxy S23 Ultra, Hochformat, Kotlin + Jetpack Compose, Material 3.

Das ist die Absicht aus Stufe 1. Beim Herunterladen fragt Werft Studio das Zielsystem noch
einmal ausdrücklich ab und übersetzt die Specs in dessen Sprache. **Weichen beide voneinander
ab, gilt die Wahl beim Herunterladen.**

---

## Was hier drin liegt

| Datei | Inhalt |
|-------|--------|
| `SPEC.md` | **Die Datei, die du liest.** Alle drei Specs in einem Dokument: Teil A (Funktion), Teil B (UI), Teil C (Motion), Teil D (Rahmen und Abnahme) |
| `00-PROJEKT.md` | Name, Plattform, Zweck, Rahmenbedingungen, Abnahme |
| `01-FUNKTIONS-SPEC.md` | Was das Programm kann — Verhalten, Daten, Regeln |
| `02-UI-SPEC.md` | Wie es aussieht — Farben, Schrift, Maße, Bildschirme |
| `03-MOTION-SPEC.md` | Wie es sich bewegt |
| `LIESMICH.md` | Diese Datei |

Die vier Einzeldateien sind mit den Teilen A–D in `SPEC.md` **wortgleich**. Sie liegen
zusätzlich bei, damit einzelne Teile maschinell gelesen werden können.

Es gibt **kein** `04-ONBOARDING-SPEC.md` und **kein** `05-RECHT-SPEC.md` — die App ist
ausschließlich für ihren Besitzer auf seinem eigenen Gerät. Kein Store, keine Weitergabe,
keine weiteren Nutzer.

---

## Der Auftrag

**Baue jeden Bildschirm aus Teil B §6 in jeder Erscheinung auf.**
Neun Bildschirme (`B-01` bis `B-09`), jeder einmal in **Dunkel** und einmal in **Hell**.
Beide Erscheinungen sind gleichrangig und vollständig zu bauen; Dunkel ist der Standard.

**Baue die Zwischenzustände mit.** Sie stehen in Teil B je Bildschirm und sind der
Unterschied zwischen einem hübschen Entwurf und einem benutzbaren Programm:

- `B-01` hat **sechs** Zustände — `LEER` · `AUFNAHME` · `LAGE_STEHT` · `VORSCHLAEGE` ·
  `LAEUFT` · `ABEND` — dazu *lädt*, *Fehler* und *kein Netz*.
- Jeder andere Bildschirm hat mindestens einen **leeren** Zustand mit dem in Teil B §8
  wörtlich festgelegten Satz.
- Der Wartezustand der KI (Teil C, `M-09`) kommt auf `B-01`, `B-02` und `B-03` vor.

---

## Regeln für den Rücklauf

**1. Jedes neue Bedienelement braucht eine Aufgabe.**
Ergänzt du einen Knopf, ein Symbol oder eine Fläche, die in Teil A keine Funktion hat, schreib
in **einem Satz** dazu, was er tun soll. Sonst muss beim Rückimport nachgefragt werden — oder
es entsteht beim Bauen ein toter Knopf.

**2. Kennungen bleiben erhalten.**
Ein Bildschirm, der hier `B-03` heißt, heißt im Rücklauf auch `B-03`. Dasselbe gilt für
`F-` (Funktionen), `M-` (Bewegungen) und `A-` (Abnahme). Diese Kennungen sind der Faden durch
die ganze Kette bis in den Quellcode.

**3. Neues wird als `NEU` gekennzeichnet.**
Jeder Bildschirm, jedes Bedienelement und jede Bewegung, die es in v1 nicht gab, bekommt die
**nächste freie Nummer** hinter der höchsten bisherigen (also ab `B-10`, `F-27`, `M-10`) und
die Kennzeichnung `NEU` — mit einem Satz, was es tun soll.

**4. Weggelassenes wird als `ENTFALLEN` gekennzeichnet.**
Was du bewusst weglässt, schreibst du mit `ENTFALLEN` und einer kurzen Begründung hin,
statt es einfach fehlen zu lassen. Über Streichungen wird beim Rückimport **nicht** gefragt —
sie fallen aus v2 heraus und werden nur festgehalten. v1 bleibt als Rückfallebene erhalten.

---

## Wohin der Rücklauf gehört

Beim Herunterladen in Werft Studio: **Projekt als ZIP herunterladen** → **Zielsystem wählen**
(Android) → **In die Outbox legen**.

Der Rücklauf muss heißen:

```
~/proggs/Designs/Outbox/Experimente-SPEC-v2.zip
```

Danach übernimmt `spec-rueckimport` (Stufe 2) und erzeugt daraus `Specs/Experimente/v2/`.
