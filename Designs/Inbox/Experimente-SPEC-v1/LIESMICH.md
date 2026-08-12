# LIESMICH — Experimente, Spec v1 (überarbeitet)

Stand: 12.08.2026, 12.06 Uhr · Erzeugt von: `spec-schmiede`, überarbeitet von Claude Code
Herkunft: `Designs/Inbox/Experimente-SPEC-v1/`

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

Neu hinzugekommene Abschnitte: `00-PROJEKT.md` **§0** (was sich geändert hat),
`02-UI-SPEC.md` **§7 Effekte** und **§8 Leere Zustände und feste Texte**.

Die vier Einzeldateien sind mit den Teilen A–D in `SPEC.md` **wortgleich**. Sie liegen
zusätzlich bei, damit einzelne Teile maschinell gelesen werden können.

Es gibt **kein** `04-ONBOARDING-SPEC.md` und **kein** `05-RECHT-SPEC.md` — die App ist
ausschließlich für ihren Besitzer auf seinem eigenen Gerät. Kein Store, keine Weitergabe,
keine weiteren Nutzer.

---

## Was sich in dieser Fassung geändert hat

Frank hat das Spec am 12.08.2026 in drei Punkten geändert. **Lies zuerst `00-PROJEKT.md` §0** —
dort steht es vollständig. Kurz:

1. **Neu: `B-10` Monitor — der Startbildschirm.** Er sammelt alles, was Frank sich
   vorgenommen hat: selbst angelegte Experimente (`F-35`) und aus den KI-Vorschlägen
   übernommene (`F-36`), in zwei Abschnitten „Läuft" (höchstens drei) und „Steht an"
   (beliebig viele). **Dieser Bildschirm ist im Design noch nicht aufgebaut und ist der
   wichtigste Teil dieses Auftrags.** Sein Aufbau steht ausführlich in Teil B §6.
2. **`B-01` Heute bleibt**, ist aber nicht mehr der Start.
3. **Maximale Effekte, überall.** Neuer verbindlicher Abschnitt **Teil B §7** mit `E-01` bis
   `E-24` und zwanzig neuen Bewegungen `M-76` bis `M-95`. Die frühere gestalterische
   Zurückhaltung ist aufgehoben.

## Der Auftrag

**Baue jeden Bildschirm aus Teil B §6 in jeder Erscheinung auf.**
**Zehn** Bildschirme (`B-01` bis `B-10`), jeder einmal in **Dunkel** und einmal in **Hell**.
Beide Erscheinungen sind gleichrangig und vollständig zu bauen; Dunkel ist der Standard.

**`B-10` zuerst.** Er ist die Hauptseite und bekommt die aufwendigste Gestaltung: Lichtgrund
(`E-01`), Glasleisten (`E-03`), Schein (`E-05`), wandernder Rand um laufende Karten (`E-06`),
Kipp-Parallaxe (`E-08`).

**Die untere Leiste hat jetzt sechs Felder:** Monitor · Heute · Ziele · Merkliste ·
Erkenntnisse · Logbuch — auf jedem Hauptbildschirm gleich.

**Baue die Zwischenzustände mit.** Sie stehen in Teil B je Bildschirm und sind der
Unterschied zwischen einem hübschen Entwurf und einem benutzbaren Programm:

- `B-10` hat **fünf** Zustände — `LEER` · `NUR_ANSTEHEND` · `LAEUFT` · `VOLL` · `ANLEGEN` —
  dazu *lädt* (mit Schimmer-Skeletten, `E-13`).
- `B-01` hat **fünf** Zustände — `LEER` · `AUFNAHME` · `LAGE_STEHT` · `VORSCHLAEGE` ·
  `ABEND` — dazu *lädt*, *Fehler* und *kein Netz*. (`LAEUFT` ist auf `B-10` gewandert.)
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
Jeder Bildschirm, jedes Bedienelement und jede Bewegung, die es hier noch nicht gibt, bekommt
die **nächste freie Nummer** hinter der höchsten bisherigen und die Kennzeichnung `NEU` — mit
einem Satz, was es tun soll.

**Die höchsten vergebenen Nummern in dieser Fassung:**
Bildschirme bis `B-10` → **nächste freie: `B-11`** ·
Funktionen bis `F-41` → **nächste freie: `F-42`** ·
Bewegungen bis `M-95` → **nächste freie: `M-96`** ·
Effekte bis `E-24` → **nächste freie: `E-25`** ·
Abnahme bis `A-30` → **nächste freie: `A-31`**

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
