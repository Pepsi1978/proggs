# Task-Completion-Summary: Pflicht-Schema am Ende jeder Aufgabe (KRITISCH)

> **Diese Regel gilt fuer ALLE Aufgaben in ALLEN Projekten — ausnahmslos.**
> Am Ende JEDER abgeschlossenen Aufgabe MUSS zuerst ein kurzes 3-Punkte-Schema
> in leichtem Deutsch stehen, BEVOR alle anderen Bloecke kommen (Insights,
> Commit-Status, Intelligenz-Vorschlaege).

---

## Warum diese Regel existiert

Der Benutzer ist kein Programmierer. Er moechte nach jeder Aufgabe auf einen Blick
sehen koennen: Was wollte ich? Was wurde gemacht? Wie sieht es jetzt aus?
Ohne diese strukturierte Zusammenfassung muss er sich die Info aus langen
technischen Bloecken zusammensuchen.

---

## Das Pflicht-Schema (IMMER am Anfang der Abschluss-Antwort)

### Genaues Format

```
**Aufgabe:**
[Was der Benutzer gewollt hat — in 3-4 Saetzen maximal. Leichtes Deutsch.
Keine Fachbegriffe wenn vermeidbar. So wie man es einem Freund erklaeren wuerde.]

**Was wurde gemacht:**
[Was ich konkret umgesetzt habe — in 3-4 Saetzen maximal. Leichtes Deutsch.
Ohne technische Details wenn moeglich, aber ehrlich und konkret.]

**Wie funktioniert es jetzt:**
[Kurze Beschreibung der neuen Funktionalitaet. Wie sieht das Ergebnis aus?
Wie laeuft es aus Benutzersicht ab? Leichtes Deutsch.]
```

### Sprach-Regeln fuer dieses Schema

| Regel | Warum |
|-------|-------|
| **Leichtes Deutsch** — so als wuerde man es einem Freund erklaeren | Der Benutzer ist kein Programmierer |
| **Keine unnoetigen Fachbegriffe** (commit, push, apply, Flag, etc.) | Muss fuer Laien verstaendlich sein |
| **3-4 Saetze pro Punkt — NICHT mehr** | Knapp, nicht ausufernd |
| **Fettgedruckte Punkt-Namen** (`**Aufgabe:**`, `**Was wurde gemacht:**`, `**Wie funktioniert es jetzt:**`) | Sofort visuell erkennbar |
| **KEINE Emojis** in dem Block | Sauber und klar |
| **KEINE Tabellen** in dem Block | Reiner Fliesstext |

### Platzierung in der Antwort

Die Reihenfolge am Ende jeder Aufgabe ist:

```
1. (ganz oben) Technische Kurz-Erklaerung, was als Naechstes passiert (falls noetig)
2. Code-Aenderungen, Tool-Ausgaben etc. (waehrend der Arbeit)
3. ★ Insight-Block (wenn es Insights gibt)
...
AM ENDE:
────────────────────────────────────────
**Aufgabe:** [3-4 Saetze leichtes Deutsch]
**Was wurde gemacht:** [3-4 Saetze leichtes Deutsch]
**Wie funktioniert es jetzt:** [Kurze Beschreibung]
────────────────────────────────────────
[Ggf. weiterer ★ Insight-Block]
Committed und gepusht. / Committed, gepusht und plattformuebergreifend.
💡 Intelligenz-Vorschlag 1: ...
💡 Intelligenz-Vorschlag 2: ...
```

---

## Beispiel (gut)

```
**Aufgabe:**
Der Schreibimpuls des Tages ist bei beiden Tagebuch-Apps manchmal ein zweites
Mal am gleichen Tag wieder aufgetaucht, obwohl er vorher weggeklickt wurde.
Der soll nach dem Wegklicken wirklich bis Mitternacht verschwunden bleiben.

**Was wurde gemacht:**
Beide Apps speichern jetzt das Wegklick-Datum so, dass es garantiert auf der
Festplatte landet — auch wenn Android die App direkt danach schliesst.
Die Zeitzone wird dabei korrekt beachtet, der Tag geht also wirklich von
0 Uhr bis 24 Uhr Ortszeit.

**Wie funktioniert es jetzt:**
Wenn du den Schreibimpuls wegklickst, bleibt er den Rest des Tages weg. Am
naechsten Tag ab Mitternacht erscheint automatisch ein neuer. Das gilt fuer
die Android-Version und die Frank-Version gleich.
```

## Beispiel (schlecht — so nicht)

```
**Aufgabe:**
Bug im Dismiss-Persistence-Layer der DailyPromptBanner-Komponente fixen.
SharedPreferences.apply() war async und hat bei Process-Kill Daten verloren.

**Was wurde gemacht:**
.apply() auf .commit() umgestellt in den ViewModels beider Apps. Neue
SharedPreferences.Editor.commit()-Aufrufe blockieren synchron auf Disk-IO.

**Wie funktioniert es jetzt:**
Prefs werden jetzt synchron persistiert.
```

Das schlechte Beispiel ist technisch korrekt, aber der Benutzer versteht nichts.
Das gute Beispiel erzaehlt die gleiche Geschichte, aber aus Benutzer-Sicht.

---

## Wann das Schema ANGEWENDET wird

| Situation | Schema noetig? |
|-----------|---------------|
| Feature implementiert | **JA** |
| Bug gefixt | **JA** |
| Kleine Aenderung (1 Zeile) | **JA** — aber sehr knapp |
| UI angepasst | **JA** |
| Config/Settings geaendert | **JA** |
| Neues Projekt erstellt | **JA** |
| Nur Frage beantwortet (kein Code geaendert) | **NEIN** — nur bei ausgefuehrten Aufgaben |
| Nur Code gelesen/erklaert | **NEIN** |
| Recherche/Analyse (ohne Umsetzung) | **NEIN** — nur bei "Aufgabe erledigt"-Momenten |

**Faustregel:** Immer wenn "Committed und gepusht" oder "Ich habe weder committed
noch gepusht" im Status steht, MUSS davor das 3-Punkte-Schema stehen.

---

## Was NIEMALS passieren darf

- ❌ Abschluss-Antwort ohne das 3-Punkte-Schema
- ❌ Schema in technischer Sprache (Fachbegriffe, englische Begriffe)
- ❌ Punkt mit mehr als 4 Saetzen (zu lang)
- ❌ Schema nach den Intelligenz-Vorschlaegen (muss DAVOR stehen)
- ❌ Schema mit Emojis oder Tabellen (muss schlicht sein)
- ❌ Ein Punkt weggelassen (z.B. nur "Aufgabe" und "Was wurde gemacht")
- ❌ Die Platzhalter-Titel aendern (IMMER genau "Aufgabe", "Was wurde gemacht",
  "Wie funktioniert es jetzt")

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `cross-platform-pflicht` (Gemini.md) | Status-Meldung kommt NACH dem Schema |
| `intelligence-suggestions-format` | Intelligenz-Vorschlaege kommen NACH dem Schema |
| `selbstbeobachtung` (Direktive #2) | Rueckblick fliesst in "Was wurde gemacht" mit ein |
| `resilient-bugfixing` (Direktive #3) | Bei Bugfixes: Ursache in "Was wurde gemacht" kurz erwaehnen (aber ohne Fachbegriffe) |

