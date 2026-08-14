# LIESMICH — Experimente, Spec v2 (überarbeitet)

Stand: 14.08.2026, 11.36 Uhr · Erzeugt von: `spec-schmiede`, fortgeschrieben von Claude Code
Herkunft: `Designs/Inbox/Experimente-SPEC-v1-ueberarbeitet.zip` (12.08.2026) **plus** der Stand
der gebauten App vom 14.08.2026.

---

## Was diese Fassung ist

**Sie beschreibt die App, wie sie wirklich läuft** — nicht, wie sie werden soll.

Das v1-Spec war ein Auftrag: Der Monitor `B-10` war beschrieben, aber noch nicht gebaut. Seither
wurde die App in **siebzehn Schritten** weitergebaut, beginnend mit einem vollständigen Neuaufbau
aus dem **Fold-Außendisplay-Entwurf**. Alles, was dabei entstanden ist, steht jetzt im Spec.

**Warum das nötig war:** Ein veraltetes Spec ist schlimmer als keines. Wer nach ihm baut, baut
Funktionen weg, die es schon gibt — die Dauer-Änderung, den Verlauf der Auswertungen, den dritten
Logbuch-Reiter, das Vorlesen an jeder Stelle. Diese Fassung schließt die Lücke.

---

## Zielplattform

**Android** — gebaut und abgenommen auf dem **Samsung Galaxy Z Fold 8, Außendisplay**,
Hochformat; Kotlin + Jetpack Compose, Material 3. Das Außendisplay ist der schmalste Fall; was
dort passt, passt überall. `minSdk 26` · `targetSdk 36` · `compileSdk 36` · JVM-Ziel 17.

---

## Was hier drin liegt

| Datei | Inhalt |
|-------|--------|
| `SPEC.md` | **Die Datei zum Lesen.** Alle vier Specs in einem Dokument: Teil A (Funktion), Teil B (UI), Teil C (Motion), Teil D (Rahmen und Abnahme) |
| `AENDERUNGEN-v1-zu-v2.md` | **Die Gegenüberstellung.** Was neu ist, was sich geändert hat, und die fünf Stellen, an denen die App dem v1-Spec widerspricht |
| `00-PROJEKT.md` | Name, Plattform, Zweck, Rahmenbedingungen, Abnahme `A-01` … `A-45` |
| `01-FUNKTIONS-SPEC.md` | Was das Programm kann — `F-01` … `F-58`, Datenmodell, Zustände |
| `02-UI-SPEC.md` | Wie es aussieht — Farben, Schrift, Maße, zehn Bildschirme, Effekte `E-01` … `E-29`, feste Texte |
| `03-MOTION-SPEC.md` | Wie es sich bewegt — `M-01` … `M-100` |
| `WERFT-DESIGN/` | Das gemessene Werft-Paket: `B-01` bis `B-09` in beiden Erscheinungen, alle Farben, Maße, Schriften, Radien und Effekte maschinenlesbar |
| `CLAUDE-DESIGN/` | Der **Fold-Außendisplay-Entwurf** (`.dc.html` + `support.js`): daraus stammen `B-10`, die Effektschicht im laufenden Bild, die sechsfeldrige untere Leiste und die zwölf zusätzlichen Schriftrollen |
| `LIESMICH.md` | Diese Datei |

Die vier Einzeldateien sind mit den Teilen A–D in `SPEC.md` **wortgleich**. Sie liegen zusätzlich
bei, damit einzelne Teile maschinell gelesen werden können.

Es gibt **kein** `04-ONBOARDING-SPEC.md` und **kein** `05-RECHT-SPEC.md` — die App ist
ausschließlich für ihren Besitzer auf seinem eigenen Gerät. Kein Store, keine Weitergabe, keine
weiteren Nutzer.

---

## Was neu ist gegenüber v1 (überarbeitet)

**In Zahlen:**

| | v1 | v2 |
|---|---|---|
| Funktionen | F-01 … F-41 (34 beschrieben) | **F-01 … F-58** (alle 58 beschrieben) |
| Bildschirme | `B-10` **nicht gebaut** | **alle zehn gebaut** |
| Effekte | E-01 … E-24 | **E-01 … E-29** |
| Bewegungen | M-01 … M-95 | **M-01 … M-100** |
| Abnahme | A-01 … A-30 | **A-01 … A-45** |
| Schriftrollen / Farbrollen | 10 / 13 | **22 / 14** |

**Die sieben Punkte, auf die es ankommt** (ausführlich in `00-PROJEKT.md` §0):

1. **`B-10` ist gebaut** und in beiden Erscheinungen umgesetzt.
2. **Die App beendet nichts mehr von selbst** — sie fragt: weiterführen, abschließen,
   Zwischenstand oder „nicht umgesetzt" (`F-44`).
3. **Nichts Eingesprochenes wird überschrieben** — jede Auswertung ist eine eigene Zeile mit
   Datum, Uhrzeit und Versuchstag (`F-45`, `F-46`).
4. **Die Dauer gehört Frank** — wählbar beim Anlegen, jederzeit änderbar (`F-42`, `F-43`).
5. **Vorlesen gibt es überall und es fällt nie aus** (`F-47`, `F-48`).
6. **Was ohne Netz liegenbleibt, wird wirklich nachgeholt** (`F-56`).
7. **Die Wege durch die App sind ganz** — Mikrofon-Erlaubnis, Zurück-Taste, Selbstbild,
   Tageswechsel (`F-50` bis `F-52`, `F-57`).

---

## Regeln für den Rücklauf

**1. Jedes neue Bedienelement braucht eine Aufgabe.**
Ergänzt du einen Knopf, ein Symbol oder eine Fläche, die in Teil A keine Funktion hat, schreib in
**einem Satz** dazu, was er tun soll. Sonst muss beim Rückimport nachgefragt werden — oder es
entsteht beim Bauen ein toter Knopf.

**2. Kennungen bleiben erhalten.**
Ein Bildschirm, der hier `B-03` heißt, heißt im Rücklauf auch `B-03`. Dasselbe gilt für `F-`
(Funktionen), `M-` (Bewegungen), `E-` (Effekte) und `A-` (Abnahme). Diese Kennungen sind der
Faden durch die ganze Kette bis in den Quellcode.

**3. Neues wird als `NEU` gekennzeichnet** — mit der nächsten freien Nummer und einem Satz, was
es tun soll.

**Die höchsten vergebenen Nummern in dieser Fassung:**
Bildschirme bis `B-10` → **nächste freie: `B-11`** ·
Funktionen bis `F-58` → **nächste freie: `F-59`** ·
Bewegungen bis `M-100` → **nächste freie: `M-101`** ·
Effekte bis `E-29` → **nächste freie: `E-30`** ·
Abnahme bis `A-45` → **nächste freie: `A-46`**

**4. Weggelassenes wird als `ENTFALLEN` gekennzeichnet**, mit kurzer Begründung. Über
Streichungen wird beim Rückimport **nicht** gefragt — sie fallen heraus und werden nur
festgehalten. v1 und v2 bleiben als Rückfallebene erhalten.

**5. Was gebaut ist, wird nicht wegentworfen.** Diese Fassung beschreibt eine laufende App. Wer
eine Funktion streichen will, schreibt `ENTFALLEN` dazu — sonst gilt sie weiter und muss beim
Umsetzen erhalten bleiben.

---

## Wohin der Rücklauf gehört

Beim Herunterladen in Werft Studio: **Projekt als ZIP herunterladen** → **Zielsystem wählen**
(Android) → **In die Outbox legen**:

```
~/proggs/Designs/Outbox/Experimente-SPEC-v3.zip
```

Danach übernimmt `spec-rueckimport` (Stufe 2) und erzeugt daraus `Specs/Experimente/v3/`.
