# Projekt — StackLabor
**Zielplattform für diesen Bau: Android (Kotlin + Jetpack Compose)** — beim Herunterladen gewählt.
Stand: 14.08.2026, 14:25 · Stufe: v2 · Plattform(en): Android

> Diese Datei ist aus `Specs/StackLabor/v1/00-PROJEKT.md` übernommen. Geändert wurde nur, was der
> Rücklauf umwirft — das ist unter §7 aufgeführt. `v1` bleibt unangetastet.

## 0. Was Frank am wichtigsten ist

> Der Auswertungsbereich. StackLabor soll ihm sagen, **ob seine Nahrungsergänzungsmittel die
> Ziele erreichen, die er sich gesetzt hat — und was ihnen im Weg steht.** Alles andere in
> dieser App dient dieser einen Auskunft.

## 1. Zweck in drei Sätzen

StackLabor verwaltet Franks Nahrungsergänzungsmittel-Stacks — die Gruppen von Mitteln, die er
zu einem bestimmten Zeitpunkt einnimmt — und lässt ihn zu jedem Stack beliebig viele Ziele
festlegen, die er per Drag & Drop nach Wichtigkeit ordnet. Eine KI-Auswertung über Codex prüft
den Stack gegen diese Ziele: Welches Ziel wird erreicht, welches nur teilweise, welches gar
nicht — und welches Mittel steht welchem Ziel im Weg. Ampeln an jedem Ziel und an jedem Mittel
machen das Ergebnis auf einen Blick lesbar, und ein Häkchen an jedem Mittel zeigt sofort, was
sich ändert, wenn Frank es weglässt.

**StackLabor ist ausdrücklich kein Einnahme-Tracker.** Das Abhaken „heute genommen" leistet die
bestehende App `NEMS`. StackLabor ist das Werkzeug zum *Komponieren und Prüfen* der Stacks.

## 2. Zielplattform(en)

> **⚠ Korrektur vom 14.08.2026, nach dem Bau auf dem Gerät nachgemessen.**
>
> Der Entwurfskopf nennt „Cover-Display 297 × 469 dp @ 420 dpi". Diese dp-Angabe ist
> **rechnerisch falsch** und wurde beim Schreiben dieses Spec ungeprüft übernommen.
> Nachgemessen auf dem Gerät (`wm size`, `wm density`, `dumpsys display`):
>
> | | Pixel | Dichte | reale dp |
> |---|---|---|---|
> | Cover (zugeklappt) | 1248 × 1972 | 2,625 | **475 × 751 dp** |
> | Innen (aufgeklappt) | 2448 × 1848 | 2,625 | **932 × 704 dp** |
>
> **Alle Maßangaben in diesem Paket bleiben trotzdem gültig** — sie sind im Entwurf
> gemessen und beziehen sich auf dessen Bezugsbreite von 297 dp. Damit sie auf dem Gerät
> genauso ankommen, rechnet die App in der Dichte des Entwurfs
> (`ui/theme/Massstab.kt`: die Dichte wird so gesetzt, dass die Bildschirmbreite exakt der
> Entwurfsbreite entspricht). Ohne diese Angleichung landet jeder Wert auf einem
> 1,6-mal breiteren Bildschirm: dasselbe Layout in falscher Größe — flache Karten,
> zu kleine Schrift, zu weite Abstände.


| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|---|---|---|---|
| Android | **Galaxy Z Fold 8 (SM-F971B), zugeklappt = Leitgröße**: 1248 × 1972 px @ 420 dpi = **297 × 469 dp** | Kotlin + Jetpack Compose | Pflicht |
| Android | Galaxy Z Fold 8 aufgeklappt: 1848 × 2448 px @ 420 dpi, 120 Hz = **440 × 583 dp** | Kotlin + Jetpack Compose, zweispaltig | Pflicht |

**Der Entwurf wurde bei 297 × 469 dp gebaut und genau dort vermessen.** Die Messung liegt in
`Specs/StackLabor/v2/messung/<erscheinung>/<B-xx>.json`, die Bilder in `bilder/…`.

> **Umrechnung — wichtig für den Bau.** Der Entwurf wird im Browser doppelt so groß dargestellt,
> damit er am Rechner ablesbar ist. Deshalb gilt:
> - Die **`stil`-Werte** in der Messung (`fontSize: "15px"`, `borderRadius: "12px"`, `height: "56px"`)
>   sind **direkt die dp- bzw. sp-Werte** für Compose. `15px` → `15.sp`, `12px` → `12.dp`.
> - Die **`kasten`-Werte** (x, y, b, h) sind **doppelt so groß**. Für dp durch 2 teilen.
> - Nachgeprüft: Gerätebereich gemessen 594 × 938 → 297 × 469 dp. Mittel-Karte gemessen
>   546 × 112 bei `height: 56px` → 273 × 56 dp.

Nutzbare Höhe: 469 dp − 24 dp Statusleiste − 24 dp Gestenleiste = **421 dp** (gemessen bestätigt).
Franks Systemschrift steht auf **90 %**.

## 3. Rahmenbedingungen

| Punkt | Festlegung |
|---|---|
| Sprache der Oberfläche | Deutsch, einsprachig. Echte Umlaute |
| Offline/Online | Stacks, Ziele, eigene Fragen und **alle Ampelberechnungen** laufen vollständig offline. Netz braucht nur eine **neue** KI-Auswertung (F-12, F-13) und die Konkurrenzprüfung (F-02) |
| Konten/Anmeldung | Codex-OAuth im Geräte-Flow gegen Franks ChatGPT-Konto, wie in `PerfectMoment` umgesetzt |
| Berechtigungen | Netzzugriff · System-Dateiblatt für Export/Import · Vordergrund-Benachrichtigung für das Weiterlaufen des Vorlesens |
| Externe Dienste | **Codex** (`chatgpt.com/backend-api/codex/responses`) · **Microsoft Edge TTS**, **Google Cloud TTS (Chirp 3 HD)**, **Qwen-Stimmklon** — alle aus `PerfectMoment`/`EntropieReductor` |
| Zugangsschlüssel | Codex über OAuth (kein Schlüssel im Projekt). Google-Cloud-TTS-Schlüssel aus `$HOME/SK/` |
| Datenhaltung | Room-Datenbank auf dem Gerät + Export/Import als Datei. **Keine Cloud-Sicherung** |
| Verteilung | Privat, nur auf Franks eigenem Gerät |
| Startbestand Mittel | 6 Stacks, 72 Einträge, 63 Mittel — siehe `../v1/STARTBESTAND.md`. Liegt als `startbestand.json` in den Assets, nicht einkompiliert |
| Startbestand Ziele | **NEU in v2:** 12 Ziele samt Zuordnung und Reihenfolge je Stack — siehe §6 |
| Schrift | **Inter** (Gewichte 400/500/600) + **Material Symbols Rounded** — im Entwurf gemessen |

## 4. Ausdrücklich NICHT enthalten

- **Kein Einnahme-Tracking**, kein Kalender, keine Statistik über die Zeit — das leistet `NEMS`.
- **Keine Erinnerungen** zur Einnahme.
- **Keine Erstbenutzungs-Einführung**, keine Datenschutzerklärung, keine Store-Vorgaben.
- **Kein Mehrbenutzer-Konzept**, keine Profile.
- **Keine Cloud-Sicherung.**
- **Kein Zweck-Stack** — „Senolytika" und „Sport" sind Ziele, keine Stacks.
- **Die Bühne des Entwurfs gehört nicht zur App.** Bildschirm-Index links, Zustands-Schalter oben
  und die Erläuterungsspalte rechts sind reine Vorführhilfen des Designers (seine Anmerkung 1).
  Gebaut wird **ausschließlich** der Geräteinhalt.
- **Später, nicht in dieser Fassung:** ein Mittel in einen anderen Stack verschieben oder
  kopieren (F-25) · einen Stack duplizieren (F-26).

## 5. Abnahme — wann ist es fertig

| Kennung | Kriterium |
|---|---|
| A-01 | Ich kann einen Stack anlegen, ihm Zeitpunkt und Einnahme-Hinweis geben, ihn umbenennen und löschen. Nach einem Neustart sind alle sechs Stacks aus meinem Startbestand da. |
| A-02 | Ich lege ein Ziel **einmal** im Ziel-Katalog an, hake es in drei Stacks an, und in jedem steht es an einer anderen Position — ohne es dreimal zu tippen. |
| A-03 | Im Ziel-Vollbild (B-12) ziehe ich ein Ziel von Position 12 auf 1. Die Nummern laufen sichtbar mit, beim Loslassen rastet es ein. Nach einem Neustart steht die Reihenfolge noch so. |
| A-04 | Nach dem Ziehen aus A-03 haben sich Ampeln geändert — **ohne KI-Abfrage.** Prüfbar im Flugzeugmodus. |
| A-05 | Ich nehme bei einem Mittel das Häkchen weg. Binnen einer Sekunde ändern sich die betroffenen Ziel-Ampeln, die geänderten pulsen einmal auf. Auch im Flugzeugmodus. |
| A-06 | Ich tippe „Diesen Stack auswerten". Während der Wartezeit sind die Ampeln entsättigt und pulsieren (`m12`), ein Schimmer zeigt, wo Text erscheint, der Antworttext baut sich wortweise auf. Am Ende trägt jedes Ziel und jedes Mittel eine Ampel. |
| A-07 | Ein Ziel, zu dem **kein einziges** Mittel beiträgt, zeigt eine **graue** Ampel mit „nicht bedient" — nicht grün. |
| A-08 | Ich tippe auf die Ampel eines Mittels und sehe, welches Ziel es stützt, welches es stört, je mit einem Satz Begründung. |
| A-09 | Ich füge ein Mittel hinzu. Es steht sofort in der Liste. Kurz darauf erscheint der Hinweis mit „Behalten" und „Doch entfernen". Verlasse ich den Stack vorher, ist er beim nächsten Öffnen noch da. |
| A-10 | Ich schreibe eine eigene Frage, speichere sie, und sie wird bei der nächsten Auswertung dieses Stacks beantwortet. Sie bleibt nach dem Neustart erhalten. |
| A-11 | Ich tippe „Alle Stacks zusammen prüfen" und sehe die Tagesgesamtdosis. Magnesium (Bisglycinat) erscheint **einmal** mit 465 mg, nicht zweimal. |
| A-12 | Ich schalte die Sortierung von „Löslichkeit" auf „Einnahme" um und zurück. Beide Ordnungen stimmen. |
| A-13 | Jedes Mittel trägt seine Löslichkeits-Punkte: grün gefüllt = wasserlöslich, weiß mit Rand = fettlöslich. Der weiße Punkt ist im **Hellmodus** deutlich zu erkennen. |
| A-14 | Ich lasse mir die Auswertung vorlesen, kann die Stimme wechseln, und das Vorlesen läuft weiter, wenn ich den Bildschirm verlasse. |
| A-15 | Ich schalte zwischen Hell und Dunkel um. Beide Fassungen sind vollständig; der Wechsel überblendet in 420 ms. |
| A-16 | Ich exportiere alles, lösche die App-Daten, importiere zurück — alles ist wieder da. |
| A-17 | Ich ändere eine Dosis → die Auswertungs-Karte wird „veraltet", die alten Ampeln bleiben sichtbar. Häkchen oder Ziel-Umsortieren markiert **nichts** als veraltet. |
| A-18 | Ich klappe das Gerät auf. Der Bildschirm wechselt zweispaltig, ohne Scrollposition oder geöffnetes Blatt zu verlieren. |
| A-19 | Bei „Animationen reduzieren" sind Dauerbewegung und Schmuck aus, Ampel-Überblendung und Ausweichen beim Ziehen laufen weiter. |
| A-20 | Codex nicht angemeldet: Der Knopf sagt es und führt zur Anmeldung. Kein Netz: Knopf ausgegraut, alle Ampeln und Häkchen funktionieren weiter. |
| A-21 | **Kein toter Knopf.** Jedes Bedienelement tut etwas Sichtbares. |
| A-22 | **NEU:** Jeder der 15 Bildschirme sieht in beiden Erscheinungen so aus wie das zugehörige Bild in `Specs/StackLabor/v2/bilder/<erscheinung>/<B-xx>.png`. |

## 6. Ziel-Startbestand (NEU in v2)

Aus dem Entwurf übernommen und von Frank am 14.08.2026 bestätigt. Zwölf Ziele:

| Kennung | Ziel |
|---|---|
| `z-kogn` | Kognition und Fokus |
| `z-schlaf` | Schlafqualität |
| `z-senol` | Senolytika und Zellalterung |
| `z-sport` | Sport: Kraft und Regeneration |
| `z-entz` | Entzündungen senken |
| `z-mito` | Mitochondrien und Energie |
| `z-gelenk` | Gelenke und Bindegewebe |
| `z-haut` | Haut und Kollagen |
| `z-immun` | Immunsystem |
| `z-stress` | Stress und Stimmung |
| `z-herz` | Herz-Kreislauf |
| `z-eisen` | Eisenstatus und Sauerstoff |

Zuordnung und Reihenfolge je Stack (Position 1 = höchste Priorität):

| Stack | Ziele in ihrer Reihenfolge |
|---|---|
| `morning1` | Kognition und Fokus · Eisenstatus und Sauerstoff · Stress und Stimmung · Sport · Haut und Kollagen · Herz-Kreislauf |
| `morning2` | Senolytika und Zellalterung · Mitochondrien und Energie · Immunsystem · Entzündungen senken · Kognition und Fokus · Herz-Kreislauf · Gelenke und Bindegewebe |
| `presport` | Sport: Kraft und Regeneration · Mitochondrien und Energie · Stress und Stimmung · Gelenke und Bindegewebe |
| `evening1` | Schlafqualität · Stress und Stimmung · Gelenke und Bindegewebe |
| `evening2` | Entzündungen senken · Senolytika und Zellalterung · Kognition und Fokus · Immunsystem · Herz-Kreislauf · Haut und Kollagen · Schlafqualität |
| `evening3` | Sport: Kraft und Regeneration · Gelenke und Bindegewebe |

## 7. Was der Rücklauf gegenüber v1 umgeworfen hat

| Punkt | v1 | v2 |
|---|---|---|
| Ziel-Bestand | keiner genannt | 12 Ziele samt Zuordnung und Reihenfolge (§6) |
| Schriftfamilie | „Inter oder die Systemschrift" | **Inter**, gemessen, mit Material Symbols Rounded |
| Einzelne Maße | Kopf 96 dp, Leiste 48 dp, Stack-Karte 76 dp, Plus 56 dp | Kopf **97**, Leiste **49**, Stack-Karte **78**, Plus **57** dp (gemessen, inkl. Ränder) |
| Bewegungen | 24 beschriebene Kennungen | **15 gemessene Keyframes** — siehe `03-MOTION-SPEC.md` |
| Offene Frage O-02 | Dreier-Zyklus ungeklärt | gelöst: „alterniert mit" nimmt mehrere Partner auf |

Vollständig in `AENDERUNGEN.md`.

## 8. Offene Fragen

| Nr | Frage | Stand |
|---|---|---|
| O-01 | Genauer Wortlaut des Auftragstexts an Codex (Tonfall, Ausführlichkeit, Quellenangabe) | Wird beim Bau nach bestem Wissen formuliert; in B-10 später nachjustierbar. **Kein Hindernis für den Bau.** |
| O-03 | Ob die 🟡-Markierung „mittleres Durchfallrisiko" in der Auswertung eine eigene Rolle spielt | Wird als Feld geführt und der KI mitgeteilt; keine eigene Logik. **Kein Hindernis.** |
| O-04 | Was beim Löschen eines verwendeten Ziels mit dessen Bewertungen geschieht | Festgelegt: Warnung mit Nennung der betroffenen Stacks, danach Löschen samt Bewertungszellen. **Erledigt.** |

O-02 ist durch den Entwurf gelöst und entfällt.
