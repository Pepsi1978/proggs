# Änderungen durch den Designer — Experimente

Stand: 09.08.2026 · Design-Paket: `Designs/Outbox/Experimente/` · Verglichen mit: `Specs/Experimente/v1/`

Der Rücklauf kam als **Vollausbau**: Werft Studio hat die drei Spec-Dateien selbst
fortgeschrieben **und** das Messpaket `WERFT-DESIGN/` mitgeliefert. Der Abgleich war deshalb
Prüfung, nicht Rekonstruktion.

**Zusammenfassung in fünf Zeilen:**
Alle neun Bildschirme wurden in beiden Erscheinungen gebaut, kein einziger entfiel.
Sämtliche 26 Farbwerte sind exakt so gemessen worden, wie v1 sie festgelegt hatte.
Neu ist genau **eine** Funktion: die Wisch-Navigation zwischen den Hauptbildschirmen.
Vier Bewegungen fehlten im Rücklauf, weil ein HTML-Export sie technisch nicht abbilden kann —
sie wurden auf Entscheidung des Benutzers aus v1 zurückgeholt.
Das Motion-Spec wuchs von 9 auf **75** Bewegungen, weil Werft jede CSS-Bewegung einzeln gemessen hat.

---

## 1. Bildschirme

| Kennung | v1 | im Design | Bewertung |
|---------|----|-----------|-----------|
| B-01 | Heute (Start) | Heute (Start), Werft-Nr. 1 | unverändert |
| B-02 | Gespräch | Gespräch, Werft-Nr. 2 | unverändert |
| B-03 | Auswertung | Auswertung, Werft-Nr. 3 | unverändert |
| B-04 | Wünsche & Ziele | Wünsche & Ziele, Werft-Nr. 9 | unverändert |
| B-05 | Merkliste | Merkliste, Werft-Nr. 8 | unverändert |
| B-06 | Erkenntnisse | Erkenntnisse, Werft-Nr. 6 | unverändert |
| B-07 | Logbuch | Logbuch, Werft-Nr. 7 | unverändert |
| B-08 | Einstellungen | Einstellungen, Werft-Nr. 4 | unverändert |
| B-09 | Selbstbild | Selbstbild, Werft-Nr. 5 | unverändert |

**Kein Bildschirm entfiel, keiner kam hinzu.** Die Werft-Nummern (1–9) sind die
Export-Reihenfolge und nicht mit den `B-`Kennungen zu verwechseln; die `B-`Kennungen wurden
im Rücklauf durchgehend korrekt geführt (auch in `navigiertZu` in `design-tokens.json`).

**Beide Erscheinungen vollständig:** `21dunkelstandard` (dark) und `22hell` (light),
je 9 Bildschirmdateien, je 13 Farbtokens.

**Meldung zu `vollstaendigkeit.nichtAufgebaut`:** Das Messpaket führt dort einen Eintrag
`"Experimente-SPEC-v1"` und meldet „10 Bildschirme im Design, 9 exportiert". Das ist **kein
fehlender App-Bildschirm**, sondern ein Artefakt des Imports — Werft hat aus dem
Importdateinamen einen zehnten Pseudo-Bildschirm gezählt. Alle neun echten Bildschirme sind
in beiden Erscheinungen vorhanden und beschrieben.

---

## 2. Neue Bedienelemente ohne Funktion in v1

Werft meldete sieben Elemente und vergab `F-27` bis `F-33`. Der Abgleich gegen v1 ergab:
**sechs davon sind keine neuen Funktionen** — sie standen bereits im v1-UI-Spec, dort aber in
Fließtext und ohne eigene `F-`Kennung, weshalb Werft sie nicht zuordnen konnte.

| Bildschirm | Element | Was es tut | Kennung |
|------------|---------|-----------|---------|
| B-01 Heute | waagerechte Wischgeste über die Hauptbildschirme | Wechselt zum nächsten bzw. vorigen Hauptbildschirm in der Reihenfolge der unteren Leiste; an den Enden kein Umlauf | **F-27** (neu vergeben) |
| B-01 Heute | Erscheinungs-Schnellschalter in der oberen Leiste | Durchläuft Hell → Dunkel → Wie das System | **F-26** (bestehend; Werft hat F-26 selbst um diesen Schalter erweitert) |
| B-01 Heute | „Lieber tippen" | Tastatur-Alternative zur Spracheingabe | **F-01** (bestehend) |
| B-03 Auswertung | bearbeitbares Auswertungsfeld | Transkript vor dem Absenden ändern | **F-10** (bestehend) |
| B-03 Auswertung | „Überspringen" | Zum nächsten offenen Experiment | **F-10** (bestehend) |
| B-07 Logbuch | Reiter „Letzte 15 Tage" / „Langzeit" | Ansichtswechsel innerhalb von B-07 | **B-07** (Navigation, keine Funktion) |
| B-05 Merkliste | „Abbrechen" im Lösch-Dialog | Schließt die Rückfrage, ohne zu löschen | **F-19** (bestehend) |

**`F-27` war im Rücklauf doppelt vergeben** — einmal in der Überblickstabelle für die
Wisch-Navigation, einmal in der Elementtabelle für den Erscheinungs-Schnellschalter. Beim
Rückimport aufgelöst: `F-27` = Wisch-Navigation. `F-28` bis `F-33` sind **nicht vergeben**
und bleiben frei.

**Zu `F-33` („Abbrechen") wurde nicht gefragt**, obwohl Werft es als offen markiert hatte:
Das Markup beantwortet es eindeutig. Der Knopf steht in `<dialog class="b05-dialog">` mit der
Überschrift „Eintrag löschen?" direkt neben einem Knopf mit `data-werft-funktion="F-19"`.
Das ist abgelesen, nicht erfunden.

---

## 3. Geänderte Gestaltung

**Nichts hat sich verschoben.** Der Abgleich der gemessenen Werte gegen die v1-Absicht ergab
Deckungsgleichheit:

| Bereich | Ergebnis |
|---------|----------|
| Farben, Dunkel | Alle 13 Rollen exakt wie v1: `#151210` · `#201B17` · `#2A231D` · `#38302A` · `#2C251F` · `#F4EEE7` · `#A99C8F` · `#6E635A` · `#C4623C` · `#3A231A` · `#6F8F6A` · `#22301F` · `#D8A03C` |
| Farben, Hell | Alle 13 Rollen exakt wie v1: `#F8F4EE` · `#FFFFFF` · `#FFFFFF` · `#E6DCD0` · `#EFE8DF` · `#1E1915` · `#6C6157` · `#9C9186` · `#B0522E` · `#F6E6DD` · `#5A7A55` · `#E6EFE3` · `#9A6A12` |
| Schriften | Fraunces (serif), Inter (sans-serif), JetBrains Mono (monospace) — wie festgelegt |
| Radien | 14 px Eingabefeld · 20 px Karte · 24 px Dialog · 9999 px vollrund — wie festgelegt |
| Blasen im Gespräch | `20px 20px 6px 20px` bzw. `20px 20px 20px 6px` — die in v1 beschriebene abgeflachte Ecke, jetzt exakt gemessen |
| Bezugsgröße | 412 × 915 dp bei `density: 1` — die dp-Werte sind 1:1 nach Compose übertragbar |

**Zwei Eingriffe beim Rückimport, beide ohne Wertänderung:**

1. **§7 Ikonografie und §8 Texte fehlten im Rücklauf.** Die Gliederung sprang von §6 direkt
   zu §9. Beide Abschnitte wurden aus v1 zurückgeholt; das Messpaket schweigt dazu
   (`assets` und `texte` in `design-tokens.json` sind leere Listen), also gilt dort die
   Absicht aus v1.
2. **Unlesbare Zeilennamen gekürzt.** Werft hatte in §3, §4 und §5 an 49 Stellen einen
   internen Pfad (`.werft-generated/019fe79c-…/1/design.html`) als Rollennamen eingesetzt.
   Gekürzt auf `design.html`. **Kein gemessener Wert wurde angefasst** — nur der Pfad.

**Zum Feld `plattform: "web"` im Messpaket:** Das ist Werfts Browser-Vorschau, nicht das
Bauziel. Der Stempel ganz oben in `00-PROJEKT.md` — „Zielplattform für diesen Bau: Android
(Kotlin / Jetpack Compose), beim Herunterladen gewählt" — gilt vor und wurde nach v2
übernommen.

---

## 4. Geänderte Bewegung

Das Motion-Spec wuchs von **9** auf **75** Bewegungen. Werft hat jede Bewegung der
`bildschirme/design.css` einzeln gemessen und je Eintrag Selektor, Auslöser, Dauer,
Verzögerung, Kurve als vollständiges `cubic-bezier`, Wiederholung, Richtung, Quelle **und
einen fertigen Jetpack-Compose-Ausdruck** hinterlegt.

**Was gemessen wurde:**
- 10 eindeutige `@keyframes` in der CSS, davon 2 (`werft-screen-detail`, `werft-screen-fade`)
  Werfts eigene Vorschau-Mechanik und **nicht** Teil der App.
- 41 `transition:`-Angaben an App-Bauteilen (`werft-b01__mic`, `werft-b01__proposal`,
  `werft-b01__nav-item` …), aus denen 63 Einträge `M-10` bis `M-75` entstanden.

**Die fünf v1-Bewegungen, die als CSS darstellbar waren, wurden bestätigt:**

| Kennung | v1-Absicht | gemessen |
|---------|-----------|----------|
| M-02 | 3200 ms, `ease-in-out`, endlos, atmend | 3200 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos, `alternate` — **deckungsgleich** |
| M-04 | 240 ms `ruhig`, 40 ms Staffelung | im Design vorhanden |
| M-07 | 180 ms `haken` | im Design vorhanden |
| M-08 | 400 ms `weich` | im Design vorhanden |
| M-09 | 1800 ms `wandern`, endlos | im Design vorhanden |

**Zwei Eingriffe beim Rückimport:**

1. **Doppelt vergebene Kennungen zusammengeführt.** Der Rücklauf hatte **77 Einträge unter
   nur 71 Kennungen**: `M-02` sechsmal (dieselbe atmende Bewegung, in sechs CSS-Regeln
   gemessen — je Erscheinung und je Keyframe-Variante) und `M-08` zweimal. Zusammengeführt zu
   je einem Eintrag, der alle Fundstellen unter „Weitere Fundstellen derselben Bewegung"
   auflistet. **Keine Umnummerierung** — die Kennungen blieben, nur die Doppelungen sind weg.
   Ohne das wäre die Abhakliste in `BAU-AUFTRAG.md` §4 wertlos gewesen: „M-02 abgehakt"
   hätte sechs verschiedene Dinge bedeuten können.
2. **Vier Bewegungen aus v1 zurückgeholt** — auf ausdrückliche Entscheidung des Benutzers:

| Kennung | Bewegung | Warum sie im Rücklauf fehlte |
|---------|----------|------------------------------|
| M-01 | Karte sinkt beim Drücken auf 98 % ein | ein `:active`-Zustand — in einem statischen Export nicht enthalten |
| M-03 | Vibration bei Aufnahmebeginn und -ende | CSS kennt keine Haptik |
| M-05 | Vorschläge werden zweiphasig ausgetauscht | eine gesteuerte Abfolge, kein einzelner Übergang |
| M-06 | Haken zeichnet sich (SVG-Pfadlänge) | braucht eine Pfad-Animation, die der Export nicht mitbringt |

Diese vier wurden **nicht vom Designer gestrichen** — das Medium konnte sie nicht tragen.
Sie stehen in v2 unter „Aus v1 zurückgeholte Bewegungen" mit ausdrücklichem Vermerk, dass
ihre Quelle `Specs/Experimente/v1/03-MOTION-SPEC.md` ist und nicht das Design.

---

## 5. Entfallenes

**Nichts.** Kein Bildschirm, keine Funktion, kein Abnahmekriterium und keine Farbrolle aus v1
fehlt in v2.

Die einzigen v1-Elemente, die im Rücklauf nicht vorkamen, waren die vier Bewegungen aus
Abschnitt 4 — und die sind zurückgeholt worden, weil ihr Fehlen eine **Messlücke** und keine
Gestaltungsentscheidung war.

`v1` bleibt unverändert unter `Specs/Experimente/v1/` und im Übergabepaket
`Designs/Inbox/Experimente-SPEC-v1.zip` als Rückfallebene erhalten.

---

## 6. Nachtrag 10.08.2026, 11:00 — zweiter Rücklauf, Aktualität nachgewiesen

Der Benutzer hat den Entwurf erneut aus Werft Studio heruntergeladen und als
`Designs/Outbox/Experimente-SPEC-v2.zip` abgelegt (diesmal mit dem erwarteten Namen; der
erste Rücklauf hieß `Experimente-SPEC-v1-SPEC-v2.zip`, siehe Logbuch B-05).

**Nachweis, dass es derselbe Entwurf ist** — nicht behauptet, sondern gerechnet: alle 30
Dateien des Archivs wurden gegen den bereits verarbeiteten Rücklauf gehasht (SHA-256).

| Ergebnis | Anzahl | Welche |
|----------|--------|--------|
| byte-identisch | 26 | `WERFT-DESIGN/bildschirme/design.css`, alle 18 Bildschirm-HTMLs (9 × 2 Erscheinungen), `design-tokens.json`, `design.html` (beide Fassungen), `DESIGN-SPEC.md`, `LIESMICH.md` (beide), `01-FUNKTIONS-SPEC.md` |
| abweichend | 4 | `00-PROJEKT.md`, `02-UI-SPEC.md`, `03-MOTION-SPEC.md`, `SPEC.md` |
| nur im neuen / nur im alten | 0 | — |

Die vier Abweichungen sind **je eine Datumszeile**, sechs Zeilen insgesamt:
`Stand: 2026-08-09` → `Stand: 2026-08-10` und der Plattform-Stempel
„beim Herunterladen … am 2026-08-09 gewählt" → „… am 2026-08-10 gewählt".
**Kein gemessener Wert, kein Bildschirm, keine Kennung, keine Bewegung hat sich geändert.**

**Zeitstempel im Archiv:** 10.08.2026, 08:51:48 (+02:00) — der Export ist von heute Morgen
und damit der aktuellste Stand. Der Dateizeitstempel des ZIP (10:52) ist nur der Zeitpunkt
des Herunterladens.

**Zielplattform:** unverändert **Android (Kotlin / Jetpack Compose)** — der Stempel im Kopf
von `00-PROJEKT.md` nennt sie erneut, nur mit dem neuen Datum.

**Folge daraus:** `Specs/Experimente/v2/` bleibt gültig und wird **nicht** neu erzeugt. Es
gibt kein `v3`. Insbesondere bleiben `messung/` und `bilder/` verbindlich — sie stammen aus
Quelldateien, die byte-identisch nachgewiesen sind. Die Stand-Zeilen der v2-Dateien wurden
**nicht** auf den 10.08. gehoben: sie datieren die Erstellung von v2, nicht den Export.

**Ebenfalls in diesem Nachtrag gesichert:** die acht Messdateien von B-01, B-04, B-08 und
B-09 (beide Erscheinungen) tragen seit dem letzten Lauf je Element das Feld `wert` — die
Feldinhalte („08:00", „GPT 5.6 Terra", „de-DE-Chirp3-HD-Kore"), die der erste Messlauf nicht
mitgelesen hatte. Reine Erweiterung, geprüft per Schlüsselvergleich: kein Feld ist
weggefallen, hinzugekommen ist ausschließlich `/elemente[]/wert`.
