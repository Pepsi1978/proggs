# ThesenLoop — Konzept und Bauanleitung

Stand: 03.09.2026 · Version 2.0.0 · Plattformen: Android (Kotlin/Compose), Windows (C#/WPF), Apple (Swift/SwiftUI)

Der ThesenLoop ist eine Werkstatt für Behauptungen. Du gibst eine These hinein.
Sieben Helfer prüfen sie, suchen Belege, testen sie, schlagen Alternativen vor und
formulieren sie schärfer. Das wiederholt sich in Runden, bis die These so gut ist,
wie sie werden kann. Am Ende bekommst du nicht nur eine bessere These, sondern auch
eine Antwort auf die Frage: Was haben wir dabei Neues gelernt?

Das Dokument hat zwei Teile.
**Teil 1** erklärt das Konzept so, dass es jeder versteht.
**Teil 2** ist die Bauanleitung für Programmierer und für Claude, wenn eine App daraus wird.

---

# Teil 1 — Das Konzept, einfach erklärt

## 1. Was ist eine These?

Eine These ist eine Behauptung, die wahr oder falsch sein kann. Zum Beispiel:

> „Wer abends kein Handy mehr benutzt, schläft besser."

Das ist unser Beispiel für das ganze Dokument. Es klingt vernünftig. Aber stimmt es?
Für wen? Ab wann ist „abends"? Was heißt „besser"? Genau solche Fragen klärt der ThesenLoop.

Der ThesenLoop funktioniert für alle Bereiche des Lebens: Gesundheit, Schule, Geld,
Beziehungen, Technik, Politik, Hobbys. Er funktioniert für Behauptungen über die Welt
(„Kaffee erhöht den Blutdruck") genauso wie für Behauptungen über dich selbst
(„Wenn ich morgens laufe, bin ich konzentrierter").

## 2. Die fünf Arten von Thesen

Nicht jede These lässt sich gleich prüfen. Deshalb sortiert der ThesenLoop jede These
zuerst in eine von fünf Arten ein. Die Art bestimmt, was als Beleg zählt und wie getestet wird.

| Art | Beispiel | Was als Beleg zählt | Wie man testet |
|---|---|---|---|
| **Tatsache** | „Kaffee erhöht kurzfristig den Blutdruck." | Studien, Messungen, amtliche Zahlen | Quellen prüfen, nachmessen |
| **Vorhersage** | „Nächstes Jahr steigen die Mieten in Hamburg." | Trends, Daten, Einschätzungen von Fachleuten | Vorhersage aufschreiben, später vergleichen |
| **Persönlich** | „Wenn ich morgens laufe, bin ich konzentrierter." | Dein eigenes Protokoll | Selbstversuch über mehrere Tage |
| **Methode** | „Mit der Pomodoro-Technik lernt man besser." | Studien und eigene Erfahrung | Vergleich: eine Woche mit, eine Woche ohne |
| **Wert** | „Taschengeld sollte nicht von Noten abhängen." | Gute Gründe, Folgen, Grundsätze | Gedankenexperiment, Gegenseite anhören |

Bei einer Wert-These gibt es kein „wahr" oder „falsch". Hier fragt der Loop:
Wie tragfähig ist die Position? Hält sie starken Gegenargumenten stand?

Unser Beispiel ist eine **persönliche These mit einem Tatsachen-Kern**: Ob Handys den
Schlaf stören, ist eine Tatsachenfrage. Ob es bei dir wirkt, ist eine persönliche Frage.

## 3. Die sieben Helfer

Stell dir eine Werkstatt vor. Sieben Leute arbeiten an deiner These. Jeder hat genau
eine Aufgabe. Niemand macht die Arbeit eines anderen. Wer Fehler sucht, sucht keine
Belege. Wer Punkte vergibt, formuliert nicht um.

| Nr. | Helfer | Aufgabe in einem Satz | Bekommt | Liefert |
|---|---|---|---|---|
| R-01 | **Leiter** | Steuert alles, verteilt die Aufträge, entscheidet nach jeder Runde. | Deine These, dann alle Ergebnisse | Aufträge, Protokoll, Entscheidung |
| R-02 | **Formulierer** | Macht die These scharf: Was genau wird behauptet, für wen, wann, und was würde sie widerlegen? | Rohe These oder Kritik und Belege | Neue Fassung mit Änderungsliste |
| R-03 | **Skeptiker** | Sucht Schwächen, Lücken und Gegenargumente. | Aktuelle Fassung | Liste von Einwänden, jeder als hart oder weich markiert |
| R-04 | **Rechercheur** | Sucht Belege und Gegenbelege, nur aus seriösen Quellen, jede Aussage mit Quelle. | Offene Fragen aus den Einwänden | Belege mit Quellenstufe |
| R-05 | **Tester** | Entwirft einen Test, den du selbst machen kannst, und wertet das Ergebnis aus. | Aktuelle Fassung und Thesen-Art | Testanleitung, später Auswertung |
| R-06 | **Erfinder** | Schlägt Alternativen vor: Was wäre, wenn die These anders lautet? | Aktuelle Fassung, Einwände, Belege | Bis zu drei Konkurrenz-Thesen |
| R-07 | **Bewerter** | Vergibt Punkte in vier Kategorien und begründet sie. | Neue Fassung, Belege, Einwände, Testergebnis | Punkte 0–100 mit Begründung |

Und dann gibt es noch dich. Du bist der achte in der Werkstatt. Du bestätigst die
Klärung, du machst den Praxistest, du kannst jederzeit eingreifen oder stoppen.

## 4. Runde 0: Erst klären, dann forschen

Bevor der Loop losläuft, wird die These scharf gemacht. Das ist die wichtigste
Verbesserung gegenüber der alten Fassung: Eine unklare These kann man nicht prüfen.

Der Formulierer stellt vier Fragen:

1. **Was genau wird behauptet?** „Kein Handy" heißt: gar nicht anfassen? Oder nur nicht scrollen?
2. **Für wen und wann gilt es?** Für alle? Nur für Jugendliche? „Abends" heißt ab wann?
3. **Was würde die These widerlegen?** Wenn Leute mit Handy genauso gut schlafen, ist sie falsch.
4. **Sind das eigentlich zwei Thesen?** Dann werden sie getrennt.

Aus unserem Beispiel wird so:

> v0: „Wer abends kein Handy mehr benutzt, schläft besser."
>
> v1: „Wer ab 21 Uhr kein Handy mehr benutzt, schläft schneller ein und fühlt sich morgens ausgeruhter."

Dann fragt dich der Leiter: **„Meinst du das so?"** Erst wenn du bestätigst, startet Runde 1.
Der Bewerter vergibt für v1 einen Startwert. Im Beispiel: 48 Punkte.

## 5. Eine Runde, Schritt für Schritt

Jede Runde hat sechs Schritte. Sie laufen nacheinander, weil jeder Schritt das
Ergebnis des vorherigen braucht. Der Leiter schickt die Aufträge und sammelt ein.

| Schritt | Wer | Was passiert im Beispiel (Runde 1) | Was herauskommt |
|---|---|---|---|
| 1 Prüfen | Skeptiker | Findet vier Einwände. Zwei harte: „Liegt es am Handy oder an aufregenden Inhalten wie Chats und Spielen?" und „Vielleicht gehen die Leute einfach früher ins Bett." | 4 Einwände, 2 hart |
| 2 Belegen | Rechercheur | Sucht Studien zu Bildschirmlicht, Inhalten und Schlaf. Findet: Das Licht allein wirkt schwach. Aufregende Inhalte halten deutlich wach. Drei Quellen verworfen, weil unseriös. | 6 Belege, davon 4 aus Stufe A oder B |
| 3 Testen | Tester | Entwirft einen Selbstversuch: Zwei Wochen. Woche 1 wie immer, Woche 2 ab 21 Uhr kein Handy. Jeden Morgen Einschlafdauer und Ausgeruhtheit notieren. | Testanleitung für dich |
| 4 Alternativen | Erfinder | Schlägt drei Konkurrenz-Thesen vor: A „Ab 21 Uhr gar kein Handy." B „Nur keine aufregenden Inhalte in der letzten Stunde." C „Das Handy lädt nachts in einem anderen Raum." | 3 Alternativen |
| 5 Verfeinern | Formulierer | Baut Einwände und Belege ein. Alternative B ist am besten belegt und wird in die Hauptthese übernommen. | v2: „Wer in der letzten Stunde vor dem Schlafen keine aufregenden Inhalte am Handy konsumiert, schläft schneller ein." |
| 6 Bewerten | Bewerter | Klarheit 21, Belege 14, Widerstandskraft 12, Reichweite 17. | 64 Punkte, plus 16 |
| Entscheiden | Leiter | Kein Stopp-Grund erfüllt. Fragt dich: Weiter, stoppen, oder Hinweis geben? | Runde 2 startet |

Der Praxistest läuft parallel weiter. Du trägst nach zwei Wochen dein Ergebnis ein.
Solange der Test läuft, kann der Loop weitere Runden mit Recherche drehen oder auf dich warten.
Das entscheidet der Leiter, und du kannst es in den Einstellungen festlegen.

## 6. Die Punkte: Vier Kategorien statt einer Zahl

Die alte Fassung hatte eine einzige Zahl: „Wie wahrscheinlich ist die These?" Das hatte
einen Haken. Eine These wird leicht „wahrscheinlicher", wenn man sie immer kleiner macht.
„Manche Menschen schlafen manchmal besser ohne Handy" ist fast sicher wahr, aber nutzlos.

Deshalb gibt es jetzt vier Kategorien mit je 0 bis 25 Punkten. Zusammen ergeben sie 0 bis 100.

| Kategorie | Frage | 25 Punkte heißt |
|---|---|---|
| **Klarheit** | Ist klar, was behauptet wird und wann es falsch wäre? | Jedes Wort ist eindeutig, der Gegenbeweis ist benannt. |
| **Belegstärke** | Wie gut stützen Quellen und Tests die These? | Mehrere unabhängige Quellen der Stufe A, Praxistest bestätigt. |
| **Widerstandskraft** | Wie viele Einwände hat sie überstanden? Sind harte Einwände offen? | Alle harten Einwände entkräftet. |
| **Reichweite** | Sagt sie noch etwas Nützliches, das man anwenden kann? | Klare Handlungsregel, gilt für viele Fälle. |

Die Reichweite ist die Bremse gegen das Kleinmachen. Wer die These bis zur
Bedeutungslosigkeit schrumpft, verliert dort Punkte.

Bei Wert-Thesen heißt Belegstärke „Stärke der Gründe" und Widerstandskraft
„Hält sie den besten Gegenargumenten stand?".

## 7. Quellenstufen: Nicht jede Quelle zählt gleich

| Stufe | Was das ist | Beispiel | Zählt als |
|---|---|---|---|
| **A** | Originalquelle | Studie, amtliche Statistik, Gesetzestext, Messung | voller Beleg |
| **B** | Fachliche Aufbereitung | Lehrbuch, Fachzeitschrift, seriöse Redaktion | Beleg |
| **C** | Einordnung | Fachblog, Wikipedia als Einstieg, Herstellerangabe | Hinweis, braucht Bestätigung |
| **D** | Ungeprüft | Forum, Social Media, anonyme Seite | nur Spur, nie Beleg |

Eine Aussage gilt als „gut belegt", wenn mindestens zwei unabhängige Quellen der
Stufe A oder B sie stützen. Der Rechercheur schreibt zu jedem Beleg die Stufe dazu.

## 8. Alternativen: Der Wettkampf der Thesen

Der Erfinder schlägt in jeder Runde bis zu drei Konkurrenz-Thesen vor. Der Bewerter
bewertet sie mit denselben vier Kategorien. Ist eine Alternative mindestens 10 Punkte
besser als die Hauptthese, schlägt der Leiter dir den Wechsel vor. Du entscheidest.
Die alte Hauptthese bleibt im Protokoll stehen.

So findet der Loop nicht nur die beste Formulierung deiner These, sondern manchmal eine
bessere These, an die du nicht gedacht hast. Das ist der Forscher-Teil.

## 9. Du bist im Loop

Der Loop hält an vier Stellen an und fragt dich:

1. **Nach Runde 0:** „Meinst du das so?" Du bestätigst die scharfe Fassung oder korrigierst.
2. **Nach jeder Runde:** Weiter, stoppen, oder einen Hinweis geben („Ich meine nur Schulkinder").
3. **Beim Praxistest:** Du trägst dein Ergebnis ein.
4. **Beim Thesen-Wechsel:** Du entscheidest, ob eine Alternative die Hauptthese wird.

Du kannst in den Einstellungen wählen, ob der Loop nach jeder Runde fragt oder
selbstständig bis zum Ende läuft.

## 10. Wann der Loop anhält

Der Leiter prüft nach jeder Bewertung sechs Regeln. Greift eine, endet die Runde anders als mit „weiter".

| Nr. | Regel | Bedingung | Was passiert |
|---|---|---|---|
| H-01 | **Ziel erreicht** | 85 Punkte oder mehr und kein harter Einwand offen | Abschluss: bestätigt |
| H-02 | **Stillstand** | Zwei Runden nacheinander höchstens 3 Punkte mehr | Abschluss mit der besten Fassung |
| H-03 | **Rundengrenze** | Voreingestellt 5 Runden, einstellbar 1 bis 10 | Abschluss, offene Punkte werden notiert |
| H-04 | **Widerlegt** | Ein harter Einwand ist belegt und lässt sich nicht auflösen, unter 15 Punkte | Abbruch, Gegenbefund wird festgehalten |
| H-05 | **Wartet auf dich** | Praxistest läuft noch und Recherche ist ausgeschöpft | Pause, kein Abbruch |
| H-06 | **Du stoppst** | Du drückst Stopp | Abschluss mit dem aktuellen Stand |

## 11. Das Ergebnis: Fünf mögliche Ausgänge

Am Ende steht nicht nur „stimmt" oder „stimmt nicht". Es gibt fünf Ausgänge:

| Ausgang | Bedeutung | Beispiel |
|---|---|---|
| **Bestätigt** | Die These hält, gut belegt, keine offenen harten Einwände. | „Kaffee erhöht kurzfristig den Blutdruck." |
| **Gilt nur wenn** | Die These stimmt, aber nur unter Bedingungen. Die Bedingungen stehen jetzt drin. | Unser Handy-Beispiel |
| **Ersetzt** | Eine Alternative war besser und ist jetzt die Hauptthese. | „Wasser hilft beim Abnehmen" wird zu „Wasser statt Limo hilft beim Abnehmen" |
| **Widerlegt** | Ein harter Einwand ist belegt. Der Gegenbefund ist das Ergebnis. | „Wer viel Wasser trinkt, nimmt automatisch ab." |
| **Offen** | Nicht entscheidbar. Es fehlt ein bestimmter Beleg oder der Praxistest steht aus. | Eine Vorhersage, deren Datum noch nicht da ist |

Jedes Ergebnis enthält immer diese Bausteine:

- **Die beste Fassung** der These, mit allen Bedingungen.
- **Was ist neu?** Ein bis drei Sätze: Was wissen wir jetzt, was wir vorher nicht wussten?
- **Was du jetzt tun kannst.** Eine konkrete Handlungsregel, falls die These eine hergibt.
- **Offene Fragen.** Was ist noch unklar?
- **Quellenliste** mit Stufen.
- **Verlauf** aller Fassungen mit Punkten.

Im Beispiel lautet „Was ist neu?":

> Nicht das Handy an sich stört den Schlaf, sondern aufregende Inhalte kurz vor dem
> Einschlafen. Die einfachste Regel, die im Selbstversuch funktioniert hat: Das Handy
> lädt nachts in einem anderen Raum.

## 12. Der Verlauf im Beispiel

| Runde | Fassung | Wichtigste Änderung | Klarheit | Belege | Widerstand | Reichweite | Gesamt | Entscheidung |
|---|---|---|---|---|---|---|---|---|
| 0 | v1 | Geklärt: ab 21 Uhr, schneller einschlafen, ausgeruhter | 18 | 6 | 5 | 19 | 48 | Du bestätigst, Runde 1 |
| 1 | v2 | „Aufregende Inhalte" statt „Handy", Alternative B übernommen | 21 | 14 | 12 | 17 | 64 | Runde 2, Praxistest gestartet |
| 2 | v3 | Praxistest bestätigt: im Schnitt 12 Minuten schneller eingeschlafen | 22 | 19 | 16 | 17 | 74 | Runde 3 |
| 3 | v4 | Alternative C eingebaut: „am stärksten, wenn das Handy im anderen Raum liegt" | 22 | 21 | 19 | 18 | 80 | Runde 4 |
| 4 | v5 | Ein schwacher Beleg durch eine Stufe-A-Quelle ersetzt | 23 | 21 | 20 | 18 | 82 | Runde 5, Stillstand beobachten |
| 5 | v6 | Formulierung geglättet, nichts Neues gefunden | 23 | 21 | 21 | 18 | 83 | Stillstand, Abschluss: Gilt nur wenn |

Die Zahlen sind Beispielwerte. Sie zeigen, wie ein Verlauf aussieht.

---

# Teil 2 — Bauanleitung für Apps

Dieser Teil ist für den Bau. Wenn jemand sagt „Implementiere das ThesenLoop-Konzept für
Android/Windows/Swift", ist das hier die Vorlage. Die Kennungen (R-xx Rollen, F-xx
Funktionen, B-xx Bildschirme, H-xx Halteregeln) bleiben im Quellcode gleich.

## 13. Architektur in einem Satz

Ein plattformneutraler Kern (Zustandsautomat, Datenmodell, Rollen-Aufrufe, Halteregeln)
plus eine dünne Oberfläche pro Plattform plus zwei austauschbare Anschlüsse: ein
Sprachmodell-Anschluss und ein Websuche-Anschluss.

```
Oberfläche (Compose / WPF / SwiftUI)
        │
ThesenLoop-Kern  ── Zustandsautomat, Rollen, Halteregeln, Bewertung
        │
   ┌────┴─────┐
Modell-      Suche-        Speicher
Anschluss    Anschluss     (Room / SQLite / SwiftData)
```

Der Kern kennt keine Plattform. Jede Rolle ist eine Funktion: Eingabe-Objekt hinein,
Ausgabe-Objekt heraus. Das Sprachmodell wird pro Rolle mit einem festen Prompt aufgerufen
und muss ein JSON liefern, das zum Ausgabe-Schema passt. Antworten, die nicht zum Schema
passen, werden einmal mit Fehlerhinweis wiederholt, danach als Fehler protokolliert.

## 14. Zustandsautomat

```
ENTWURF ──(Nutzer gibt These ein)──▶ KLAEREN (R-02)
KLAEREN ──▶ WARTET_BESTAETIGUNG ──(Nutzer bestätigt)──▶ STARTBEWERTUNG (R-07) ──▶ RUNDE
                                 └─(Nutzer korrigiert)──▶ KLAEREN

RUNDE:  PRUEFEN (R-03) → BELEGEN (R-04) → TESTEN (R-05) → ALTERNATIVEN (R-06)
        → VERFEINERN (R-02) → BEWERTEN (R-07) → ENTSCHEIDEN (R-01)

ENTSCHEIDEN ──▶ WARTET_NUTZER      (wenn Einstellung „nach jeder Runde fragen")
            ──▶ WARTET_TEST        (H-05)
            ──▶ WARTET_WECHSEL     (Alternative ≥ 10 Punkte besser)
            ──▶ RUNDE              (kein Stopp-Grund)
            ──▶ FERTIG             (H-01, H-02, H-03, H-04, H-06)

WARTET_NUTZER ──(weiter / Hinweis)──▶ RUNDE     ──(Stopp)──▶ FERTIG
WARTET_TEST   ──(Ergebnis eingetragen)──▶ RUNDE (Tester wertet zuerst aus)
WARTET_WECHSEL ──(ja)──▶ RUNDE mit neuer Hauptthese ──(nein)──▶ RUNDE
FERTIG: Ergebnis-Objekt liegt vor; Projekt ist archivierbar, kann als neues Projekt geklont werden
```

Jeder Zustandswechsel wird mit Zeitstempel im Projekt gespeichert. Der Loop muss nach
App-Neustart im selben Zustand weiterlaufen können (Android: WorkManager, Windows:
Hintergrunddienst im Prozess, Apple: BackgroundTasks oder nur im Vordergrund).

## 15. Datenmodell

Plattformneutral beschrieben. Typen: Text, Zahl, Wahrheitswert, Zeit, Liste, Verweis.

```
Projekt
  id, titel, erstelltAm, zustand, einstellungen (Verweis), hauptfassungId,
  thesenArt: TATSACHE | VORHERSAGE | PERSOENLICH | METHODE | WERT
  ergebnis (Verweis, leer bis FERTIG)

Einstellungen
  maxRunden (1..10, Standard 5), nachJederRundeFragen (Wahrheitswert, Standard wahr),
  websucheAn (Wahrheitswert), modellProRolle (Tabelle R-xx → Modell-ID),
  stillstandSchwelle (Standard 3), zielSchwelle (Standard 85), widerlegtSchwelle (Standard 15),
  wechselSchwelle (Standard 10), sprache

Fassung
  id, projektId, nummer (v0, v1 …), rundeNr, text, geltungsbereich (Text),
  gegenbeweis (Text: was würde sie widerlegen), aenderungen (Liste Text),
  bewertungId, istHauptfassung, quelleFassungId (bei Alternative: woraus entstanden)

Runde
  id, projektId, nummer, gestartetAm, beendetAm, schrittStatus (Tabelle Schritt → offen/läuft/fertig/fehler),
  entscheidung: WEITER | WARTET_NUTZER | WARTET_TEST | WARTET_WECHSEL | FERTIG_H01 … FERTIG_H06,
  entscheidungBegruendung, nutzerHinweis (Text, optional)

Einwand
  id, rundeId, fassungId, text, haerte: HART | WEICH, betrifft: KLARHEIT | BELEG | LOGIK | REICHWEITE,
  status: OFFEN | ENTKRAEFTET | BELEGT, entkraeftetDurch (Verweis Beleg oder Fassung)

Beleg
  id, rundeId, aussage, richtung: STUETZT | WIDERSPRICHT, quelleId, zitatOderStelle,
  bezugEinwandId (optional), verlaesslichkeit (0..1, vom Rechercheur geschätzt)

Quelle
  id, titel, url, herausgeber, datum, stufe: A | B | C | D, unabhaengigVon (Liste Quelle-Ids)

Test
  id, rundeId, fassungId, art: SELBSTVERSUCH | VERGLEICH | VORHERSAGE_PROTOKOLL | GEDANKENEXPERIMENT | QUELLENPRUEFUNG,
  anleitung (Text, Schritt für Schritt), dauerTage, messgroesse (Text), erfolgsKriterium (Text),
  status: ENTWORFEN | LAEUFT | ABGESCHLOSSEN | UEBERSPRUNGEN, gestartetAm, faelligAm

TestErgebnis
  id, testId, eingetragenAm, rohdaten (Text oder Liste Messwerte), nutzerNotiz,
  auswertung (Text vom Tester), befund: STUETZT | WIDERSPRICHT | UNKLAR

Alternative
  id, rundeId, fassungId (die Alternative als eigene Fassung), begruendung, bewertungId,
  status: VORGESCHLAGEN | UEBERNOMMEN | VERWORFEN | EINGEBAUT

Bewertung
  id, fassungId, rundeId, klarheit (0..25), belegstaerke (0..25), widerstandskraft (0..25),
  reichweite (0..25), gesamt (Summe), begruendung (Text je Kategorie), offeneHarteEinwaende (Zahl)

Ergebnis
  id, projektId, ausgang: BESTAETIGT | GILT_NUR_WENN | ERSETZT | WIDERLEGT | OFFEN,
  besteFassungId, wasIstNeu (Text), handlungsregel (Text, optional), offeneFragen (Liste Text),
  quellen (Liste Quelle-Ids), halteregel: H-01 … H-06, erstelltAm

Protokoll
  id, projektId, zeit, rolle (R-xx oder NUTZER), ereignis, details (JSON-Text)
```

## 16. Die Rollen als Schnittstellen

Jede Rolle: `fuehreAus(eingabe) → ausgabe`. Eingabe und Ausgabe sind JSON. Das Modell
bekommt den Rollen-Prompt (Abschnitt 17) als System-Anweisung und die Eingabe als
Nutzer-Nachricht, und muss genau das Ausgabe-JSON zurückgeben.

### R-02 Formulierer

```json
Eingabe:  { "modus": "KLAEREN" | "VERFEINERN", "these": "…", "thesenArt": "…|null",
            "einwaende": [ … ], "belege": [ … ], "testErgebnis": { … } | null,
            "uebernommeneAlternative": { … } | null, "nutzerHinweis": "…|null" }
Ausgabe:  { "thesenArt": "TATSACHE|VORHERSAGE|PERSOENLICH|METHODE|WERT",
            "fassung": { "text": "…", "geltungsbereich": "…", "gegenbeweis": "…" },
            "aenderungen": [ "…" ],
            "aufgeteilt": [ { "text": "…", "grund": "…" } ],
            "rueckfragen": [ "…" ] }
```

`aufgeteilt` ist nur im Modus KLAEREN gefüllt, wenn die These aus mehreren Thesen besteht.
Die App legt dann für jede ein eigenes Projekt an und fragt den Nutzer, welches zuerst läuft.

### R-03 Skeptiker

```json
Eingabe:  { "fassung": { … }, "thesenArt": "…", "bisherigeEinwaende": [ … ] }
Ausgabe:  { "einwaende": [ { "text": "…", "haerte": "HART|WEICH",
                              "betrifft": "KLARHEIT|BELEG|LOGIK|REICHWEITE",
                              "pruefFrage": "…" } ] }
```

Höchstens 6 Einwände, davon höchstens 3 harte. Ein harter Einwand trifft den Kern:
Wenn er stimmt, ist die These falsch. Ein weicher Einwand macht sie ungenauer oder kleiner.

### R-04 Rechercheur

```json
Eingabe:  { "fassung": { … }, "pruefFragen": [ … ], "websucheAn": true|false,
            "bekannteQuellen": [ … ] }
Ausgabe:  { "belege": [ { "aussage": "…", "richtung": "STUETZT|WIDERSPRICHT",
                          "quelle": { "titel": "…", "url": "…", "herausgeber": "…",
                                      "datum": "…", "stufe": "A|B|C|D" },
                          "zitatOderStelle": "…", "bezugPruefFrage": "…",
                          "verlaesslichkeit": 0.0 } ],
            "verworfeneQuellen": [ { "url": "…", "grund": "…" } ],
            "nichtsGefundenZu": [ "…" ] }
```

Ohne Websuche (Einstellung aus) arbeitet der Rechercheur nur mit Modellwissen und
markiert jeden Beleg als Stufe C mit dem Hinweis „nicht online geprüft".

### R-05 Tester

```json
Eingabe:  { "modus": "ENTWERFEN" | "AUSWERTEN", "fassung": { … }, "thesenArt": "…",
            "test": { … } | null, "testErgebnis": { … } | null }
Ausgabe (ENTWERFEN): { "test": { "art": "…", "anleitung": [ "…" ], "dauerTage": 0,
                                  "messgroesse": "…", "erfolgsKriterium": "…" },
                       "warumDieserTest": "…" }
Ausgabe (AUSWERTEN): { "auswertung": "…", "befund": "STUETZT|WIDERSPRICHT|UNKLAR",
                       "hinweisFuerFormulierer": "…" }
```

Test-Art nach Thesen-Art: TATSACHE → QUELLENPRUEFUNG oder VERGLEICH; VORHERSAGE →
VORHERSAGE_PROTOKOLL; PERSOENLICH → SELBSTVERSUCH; METHODE → VERGLEICH; WERT →
GEDANKENEXPERIMENT. Der Tester entwirft nur Tests, die ein Laie ohne Geräte machen kann.

### R-06 Erfinder

```json
Eingabe:  { "fassung": { … }, "einwaende": [ … ], "belege": [ … ], "bisherigeAlternativen": [ … ] }
Ausgabe:  { "alternativen": [ { "text": "…", "geltungsbereich": "…", "gegenbeweis": "…",
                                "begruendung": "…", "artDerAenderung": "ENGER|WEITER|ANDERE_URSACHE|ANDERE_HANDLUNG|GEGENTEIL" } ] }
```

Höchstens 3 Alternativen. Mindestens eine muss eine andere Ursache oder eine andere
Handlung vorschlagen, nicht nur enger oder weiter fassen. Das Gegenteil der These ist
immer erlaubt.

### R-07 Bewerter

```json
Eingabe:  { "fassung": { … }, "thesenArt": "…", "einwaende": [ … ], "belege": [ … ],
            "testErgebnis": { … } | null, "vorherigeBewertung": { … } | null,
            "zuBewerten": "HAUPT" | "ALTERNATIVE" }
Ausgabe:  { "klarheit": 0, "belegstaerke": 0, "widerstandskraft": 0, "reichweite": 0,
            "gesamt": 0, "offeneHarteEinwaende": 0,
            "begruendung": { "klarheit": "…", "belegstaerke": "…",
                             "widerstandskraft": "…", "reichweite": "…" } }
```

Der Bewerter bewertet die Hauptfassung und jede Alternative getrennt, jeweils mit
einem eigenen Aufruf. Er sieht die vorherige Bewertung, damit die Skala stabil bleibt.

### R-01 Leiter

Der Leiter ist kein Modell-Aufruf, sondern Code. Er ruft die Rollen in der Reihenfolge
aus Abschnitt 14 auf, speichert jedes Ergebnis, prüft die Halteregeln (Abschnitt 18) und
setzt den Zustand. Nur für den Text „Was ist neu?" im Ergebnis darf er einmal das
Modell aufrufen (Prompt in Abschnitt 17).

## 17. Prompts je Rolle

Die Prompts sind auf Deutsch. Sie werden als System-Anweisung übergeben. `{…}`-Stellen
füllt der Kern aus. Jeder Prompt endet mit derselben Formatregel.

**Gemeinsame Formatregel (an jeden Prompt anhängen):**

> Antworte ausschließlich mit einem JSON-Objekt nach dem vorgegebenen Schema. Kein Text
> davor oder danach. Schreib alle Texte in einfachem Deutsch, kurze Sätze, keine Fachwörter
> ohne Erklärung. Erfinde keine Quellen. Wenn du etwas nicht weißt, sag es im dafür
> vorgesehenen Feld.

**R-02 Formulierer**

> Du bist der Formulierer im ThesenLoop. Deine Aufgabe: eine These so scharf machen, dass
> man sie prüfen kann. Im Modus KLAEREN bekommst du eine rohe These. Bestimme die Thesen-Art
> (Tatsache, Vorhersage, Persönlich, Methode, Wert). Kläre: Was genau wird behauptet? Für
> wen und wann gilt es? Was würde die These widerlegen? Besteht sie aus mehreren Thesen,
> teile sie auf. Ersetze unklare Wörter wie „besser", „viel", „oft" durch messbare Angaben.
> Im Modus VERFEINERN bekommst du die aktuelle Fassung, Einwände, Belege, ein Testergebnis
> und eventuell eine übernommene Alternative. Baue alles ein, was belegt ist. Streiche, was
> widerlegt ist. Grenze ein, wo es nötig ist, aber mache die These nicht kleiner als nötig:
> Sie soll noch etwas Nützliches sagen. Liste jede Änderung einzeln auf. Hat der Nutzer
> einen Hinweis gegeben, hat er Vorrang.

**R-03 Skeptiker**

> Du bist der Skeptiker im ThesenLoop. Deine einzige Aufgabe: Schwächen finden. Suche
> Widersprüche, Lücken, versteckte Annahmen, Verwechslung von Ursache und Zusammenhang,
> fehlende Gegenbeispiele, zu große oder zu kleine Reichweite. Formuliere höchstens sechs
> Einwände, davon höchstens drei harte. Ein harter Einwand bedeutet: Wenn er stimmt, ist die
> These falsch. Ein weicher Einwand bedeutet: Die These wird ungenauer oder kleiner. Zu jedem
> Einwand formulierst du eine Prüf-Frage, die der Rechercheur beantworten kann. Wiederhole
> keine Einwände aus früheren Runden, die schon entkräftet sind. Du suchst keine Belege und
> du formulierst die These nicht um.

**R-04 Rechercheur**

> Du bist der Rechercheur im ThesenLoop. Du beantwortest Prüf-Fragen mit Belegen aus
> seriösen Quellen. Jede Aussage bekommt eine Quelle mit Titel, Herausgeber, Datum, Link und
> Stufe: A = Originalquelle (Studie, amtliche Statistik, Gesetz, Messung), B = fachliche
> Aufbereitung (Lehrbuch, Fachzeitschrift, seriöse Redaktion), C = Einordnung (Fachblog,
> Wikipedia, Herstellerangabe), D = ungeprüft (Forum, Social Media). Stufe D ist nie ein
> Beleg, nur eine Spur. Suche Belege, die stützen, und Belege, die widersprechen, mit gleicher
> Mühe. Nenne Quellen, die du verworfen hast, mit Grund. Wenn du zu einer Frage nichts
> findest, schreib das. Erfinde nie eine Quelle. Steht dir keine Websuche zur Verfügung,
> markiere jeden Beleg als Stufe C mit dem Hinweis „nicht online geprüft".

**R-05 Tester**

> Du bist der Tester im ThesenLoop. Im Modus ENTWERFEN entwirfst du einen Test, den ein
> Laie ohne Geräte selbst machen kann. Wähle die Test-Art nach Thesen-Art: Tatsache →
> Quellenprüfung oder Vergleich, Vorhersage → Vorhersage-Protokoll mit Datum, Persönlich →
> Selbstversuch mit Vorher-Nachher-Wochen, Methode → Vergleich mit und ohne, Wert →
> Gedankenexperiment mit den Folgen für alle Beteiligten. Schreibe eine Schritt-für-Schritt-
> Anleitung, die Dauer in Tagen, was gemessen wird und ab wann der Test als bestanden gilt.
> Im Modus AUSWERTEN bekommst du das Ergebnis des Nutzers. Sag ehrlich, ob es die These
> stützt, ihr widerspricht oder unklar ist, und warum. Ein einzelner Selbstversuch ist ein
> Hinweis, kein Beweis. Sag das dazu.

**R-06 Erfinder**

> Du bist der Erfinder im ThesenLoop. Du schlägst bis zu drei Konkurrenz-Thesen vor, die
> dieselbe Frage besser beantworten könnten. Nutze die Einwände und Belege als Hinweise.
> Mindestens eine Alternative muss eine andere Ursache oder eine andere Handlung
> vorschlagen, nicht nur enger oder weiter fassen. Das Gegenteil der These ist erlaubt.
> Jede Alternative bekommt einen Geltungsbereich, einen Gegenbeweis und eine Begründung in
> zwei Sätzen. Wiederhole keine Alternative aus früheren Runden.

**R-07 Bewerter**

> Du bist der Bewerter im ThesenLoop. Du vergibst in vier Kategorien je 0 bis 25 Punkte.
> Klarheit: Ist eindeutig, was behauptet wird und was die These widerlegen würde?
> Belegstärke: Wie gut stützen Quellen und Testergebnis die These? Zwei unabhängige Quellen
> der Stufe A oder B sind die Messlatte für „gut belegt". Bei Wert-Thesen: Wie stark sind die
> Gründe? Widerstandskraft: Wie viele Einwände sind entkräftet, wie viele harte sind offen?
> Jeder offene harte Einwand kostet mindestens 8 Punkte in dieser Kategorie. Reichweite:
> Sagt die These noch etwas Nützliches und Anwendbares? Eine These, die fast nichts mehr
> behauptet, bekommt hier höchstens 5 Punkte. Begründe jede Kategorie in einem Satz. Du siehst
> die vorherige Bewertung: Bleib auf derselben Skala und begründe jede Änderung. Du
> formulierst nichts um und du suchst keine Belege.

**R-01 Leiter, nur für „Was ist neu?"**

> Du fasst das Ergebnis eines ThesenLoops zusammen. Du bekommst die Ausgangsthese, die beste
> Fassung, den Ausgang, die entkräfteten und offenen Einwände und die wichtigsten Belege.
> Schreibe drei Dinge: „Was ist neu?" in ein bis drei Sätzen, nur das, was am Anfang nicht
> bekannt war. „Was du jetzt tun kannst" als eine konkrete Regel, falls die These eine
> hergibt, sonst leer. „Offene Fragen" als kurze Liste. Einfaches Deutsch, keine Fachwörter.

## 18. Halteregeln als Code

Reihenfolge ist Vorrang. Die erste zutreffende Regel gewinnt.

```
pruefeHalteregeln(runde, bewertung, vorherigeBewertungen, einstellungen, test):
  wenn nutzerHatStoppGedrueckt                                   → FERTIG_H06
  wenn bewertung.gesamt < einstellungen.widerlegtSchwelle
       und existiert Einwand mit haerte=HART und status=BELEGT   → FERTIG_H04
  wenn bewertung.gesamt >= einstellungen.zielSchwelle
       und bewertung.offeneHarteEinwaende == 0                   → FERTIG_H01
  wenn runde.nummer >= einstellungen.maxRunden                   → FERTIG_H03
  wenn anzahl(vorherigeBewertungen) >= 2
       und delta(letzte) <= stillstandSchwelle
       und delta(vorletzte) <= stillstandSchwelle                → FERTIG_H02
  wenn test.status == LAEUFT und rechercheur.nichtsGefundenZu deckt alle Prüf-Fragen
                                                                 → WARTET_TEST (H-05)
  wenn besteAlternative.gesamt - bewertung.gesamt >= wechselSchwelle
                                                                 → WARTET_WECHSEL
  wenn einstellungen.nachJederRundeFragen                        → WARTET_NUTZER
  sonst                                                          → WEITER
```

Ausgang im Ergebnis:

```
ausgang(ergebnisRegel, fassungen, alternativen):
  H-04                                         → WIDERLEGT
  H-01                                         → BESTAETIGT
  Hauptfassung stammt aus übernommener Alternative → ERSETZT
  besteFassung.geltungsbereich enger als v1    → GILT_NUR_WENN
  Test läuft noch oder offene harte Einwände ohne Beleg → OFFEN
  sonst                                        → BESTAETIGT
```

Bei H-02, H-03, H-06 wird die Fassung mit der höchsten Gesamtpunktzahl zur besten Fassung,
nicht automatisch die letzte.

## 19. Bildschirme

| Kennung | Bildschirm | Inhalt | Aktionen |
|---|---|---|---|
| B-01 | Neue These | Ein Textfeld, Beispiel-Platzhalter, Wahl der Rundenzahl | Starten |
| B-02 | Klärung bestätigen | v0 und v1 nebeneinander, Thesen-Art, die vier Klär-Fragen mit Antworten, eventuell aufgeteilte Thesen | Passt so · Korrigieren (Textfeld) |
| B-03 | Runde läuft | Sechs Schritte als Leiste mit Status, darunter das Ergebnis des laufenden Schritts in einfachen Worten, Punkte-Anzeige mit vier Balken | Pause · Stopp |
| B-04 | Praxistest | Anleitung Schritt für Schritt, Dauer, Fälligkeit, Eingabe der Messwerte je Tag, Notizfeld | Test starten · Ergebnis eintragen · Überspringen |
| B-05 | Rundenende | Neue Fassung, Änderungsliste, Punkte mit Begründung, Alternativen mit Punkten | Weiter · Hinweis geben · Alternative übernehmen · Stopp |
| B-06 | Ergebnis | Ausgang groß, beste Fassung, Was ist neu, Was du jetzt tun kannst, offene Fragen, Quellenliste mit Stufen, Verlaufskurve | Teilen als Text · Als neues Projekt weiterführen |
| B-07 | Verlauf | Tabelle aller Fassungen mit vier Kategorien und Gesamt, Kurve über die Runden | Fassung antippen zeigt Details |
| B-08 | Projekte | Liste aller Thesen mit Zustand und Punktestand | Neu · Öffnen · Archivieren |
| B-09 | Einstellungen | Modell je Rolle, Websuche an/aus, Rundenzahl, nach jeder Runde fragen, Schwellen, Sprache, Schlüssel | Speichern |

Sprache der Oberfläche: Deutsch mit echten Umlauten. Alle Texte an den Nutzer auf dem
Niveau von Teil 1: kurze Sätze, keine Fachwörter ohne Erklärung. Fehler sagen, was
schiefging und was der Nutzer tun kann.

## 20. Funktionen

| Kennung | Funktion | Abnahme |
|---|---|---|
| F-01 | These eingeben und Projekt anlegen | Projekt erscheint in B-08 mit Zustand KLAEREN |
| F-02 | Klären mit Bestätigung (Runde 0) | v1 mit Thesen-Art, Gegenbeweis, Geltungsbereich liegt vor; Nutzer hat bestätigt |
| F-03 | Runde mit sechs Schritten ausführen | Jeder Schritt hinterlässt ein gespeichertes Objekt; Abbruch mittendrin ist wiederaufnehmbar |
| F-04 | Halteregeln H-01 bis H-06 | Jede Regel ist einzeln auslösbar und im Protokoll benannt |
| F-05 | Praxistest entwerfen, laufen lassen, auswerten | Fälligkeit wird angezeigt; Ergebnis fließt in die nächste Runde |
| F-06 | Alternativen bewerten und Wechsel anbieten | Wechsel-Vorschlag erscheint ab 10 Punkten Vorsprung; Nutzer entscheidet |
| F-07 | Vier-Kategorien-Bewertung | Gesamt ist immer die Summe der vier; Begründung je Kategorie sichtbar |
| F-08 | Ergebnis mit fünf Ausgängen und „Was ist neu?" | Ergebnis-Objekt vollständig; als Text teilbar |
| F-09 | Verlauf und Protokoll | Jede Fassung, jede Bewertung, jeder Zustandswechsel mit Zeit abrufbar |
| F-10 | Fortsetzen nach Neustart | App beenden während RUNDE; nach Neustart läuft der offene Schritt weiter |
| F-11 | Ohne Websuche arbeiten | Websuche aus: Loop läuft, Belege sind als Stufe C „nicht online geprüft" markiert |
| F-12 | Unter-Helfer (optional) | Eine Rolle darf für unabhängige Teilfragen parallele Modell-Aufrufe starten; nach oben geht nur das verdichtete Ergebnis |

## 21. Plattform-Zuordnung

| Baustein | Android | Windows | Apple |
|---|---|---|---|
| Sprache, Oberfläche | Kotlin, Jetpack Compose, Material 3 | C#, .NET 8, WPF | Swift, SwiftUI |
| Architektur | MVVM, Hilt, ein Modul `core` ohne Android-Abhängigkeit | MVVM, ein Projekt `ThesenLoop.Core` als Klassenbibliothek | MVVM, ein Swift Package `ThesenLoopCore` |
| Speicher | Room | SQLite über EF Core | SwiftData |
| Hintergrund | WorkManager für laufende Runden, Benachrichtigung bei Fälligkeit des Tests | Task im Prozess, Toast bei Fälligkeit | BackgroundTasks, lokale Benachrichtigung |
| Netz | OkHttp/Ktor | HttpClient | URLSession |
| Modell-Anschluss | Claude Messages API mit JSON-Ausgabe; Websuche über das Websuche-Werkzeug der API oder eine externe Such-API | gleich | gleich |
| Schlüssel | Verschlüsselt (EncryptedSharedPreferences) | DPAPI | Keychain |
| Version | `versionName` + `VERSION_BUMPED_AT` in build.gradle.kts | `<Version>` in .csproj | `MARKETING_VERSION` |

Modell je Rolle: Standard ist das jeweils aktuelle, leistungsfähigste Claude-Modell für
Formulierer, Bewerter und Leiter-Zusammenfassung; ein schnelleres Modell darf für
Skeptiker, Rechercheur, Tester und Erfinder eingestellt werden. Modell-IDs und der Name des
Websuche-Werkzeugs werden beim Bau aus der aktuellen API-Dokumentation übernommen, nicht
aus diesem Dokument.

## 22. Reihenfolge beim Bau

1. `core`: Datenmodell, Zustandsautomat, Halteregeln, Bewertungs-Summe. Ohne Modell, mit
   Stub-Rollen, die feste JSON-Antworten liefern.
2. Modell-Anschluss mit JSON-Prüfung und einmaliger Wiederholung bei Schema-Fehler.
3. Rollen R-02, R-03, R-04, R-07 mit Prompts. Damit läuft ein Loop ohne Test und ohne Alternativen.
4. R-05 Tester und B-04 Praxistest samt Wartezustand.
5. R-06 Erfinder und Wechsel-Logik.
6. Ergebnis mit „Was ist neu?" (B-06), Verlauf (B-07).
7. Fortsetzen nach Neustart (F-10), Einstellungen (B-09).

## 23. Was sich gegenüber Version 1 geändert hat

| Vorher (31.08.2026) | Jetzt (03.09.2026) | Warum |
|---|---|---|
| 5 Rollen, Loop startet sofort | 7 Rollen plus Nutzer, Runde 0 klärt zuerst | Unklare Thesen waren nicht prüfbar |
| Eine Zahl „Wahrscheinlichkeit" | Vier Kategorien je 25 Punkte | Die eine Zahl stieg, wenn die These kleiner wurde |
| Nur Web-Recherche | Recherche plus Praxistest durch den Nutzer | Persönliche Thesen lassen sich nur selbst testen |
| Eine These wird verfeinert | Alternativen treten gegeneinander an | „Was wäre die beste Alternative?" war unbeantwortet |
| Ergebnis: Fassung plus Score | Fünf Ausgänge plus „Was ist neu?" plus Handlungsregel | Der Forscher-Teil fehlte |
| 3 Halteregeln | 6 Halteregeln, darunter Pause für den Test und Ziel erreicht | Loop drehte weiter, obwohl fertig |
| Keine Thesen-Arten | Fünf Arten mit eigener Beleg- und Testmethode | „Alle Bereiche des Lebens" brauchen verschiedene Prüfwege |
| Nur Konzeptgrafik | Bauanleitung mit Datenmodell, Schnittstellen, Prompts, Bildschirmen, Plattform-Tabelle | Damit „implementiere das ThesenLoop-Konzept" ohne Nachfragen funktioniert |
