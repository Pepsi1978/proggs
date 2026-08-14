# Projekt — StackLabor
Stand: 14.08.2026 · Stufe: v1 · Plattform(en): Android

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

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|---|---|---|---|
| Android | **Galaxy Z Fold 8 (SM-F971B), zugeklappt = Leitgröße**: 1248 × 1972 px @ 420 dpi ≈ **297 × 469 dp** | Kotlin + Jetpack Compose | Pflicht |
| Android | Galaxy Z Fold 8 aufgeklappt (Innendisplay): 1848 × 2448 px @ 420 dpi, 120 Hz ≈ **440 × 583 dp** | Kotlin + Jetpack Compose, zweispaltiges Layout | Pflicht |

**Leitgröße ist das zugeklappte Cover-Display.** Was dort funktioniert, funktioniert aufgeklappt
sicher — umgekehrt nicht. Für das Innendisplay entsteht zusätzlich ein zweispaltiges Layout
(siehe `02-UI-SPEC.md` §11).

Franks Systemschrift steht auf **90 %**. Alle Maßrechnungen in diesem Paket berücksichtigen das.

Nutzbare Höhe auf dem Cover: 469 dp − 24 dp Statusleiste − 24 dp Gestenleiste = **421 dp**.
Kartenbreite: 297 dp − 2 × 12 dp Rand = **273 dp**.

## 3. Rahmenbedingungen

| Punkt | Festlegung |
|---|---|
| Sprache der Oberfläche | Deutsch, einsprachig. Echte Umlaute, keine Ersatzschreibung |
| Offline/Online | Stacks, Ziele, eigene Fragen und **alle Ampelberechnungen** laufen vollständig offline. Netz braucht nur eine **neue** KI-Auswertung (F-12, F-13) und die Konkurrenzprüfung (F-02) |
| Konten/Anmeldung | Codex-OAuth im Geräte-Flow gegen Franks ChatGPT-Konto, wie in `PerfectMoment` umgesetzt. Sonst kein Konto |
| Berechtigungen | Netzzugriff. Für den Export/Import das System-Dateiblatt (kein Speicher-Recht nötig). Für das Weiterlaufen des Vorlesens im Hintergrund eine Vordergrund-Benachrichtigung |
| Externe Dienste | **Codex** (`chatgpt.com/backend-api/codex/responses`) für die Auswertung · **Microsoft Edge TTS**, **Google Cloud TTS (Chirp 3 HD)** und **Qwen-Stimmklon** für das Vorlesen — alle drei aus `PerfectMoment`/`EntropieReductor` übernommen |
| Zugangsschlüssel | Codex über OAuth (kein Schlüssel im Projekt). Google-Cloud-TTS-Schlüssel wie in den Vorbildprojekten aus `$HOME/SK/` |
| Datenhaltung | Room-Datenbank auf dem Gerät. Zusätzlich Export/Import als Datei (F-19, F-20). **Keine Cloud-Sicherung** |
| Verteilung | Privat, nur auf Franks eigenem Gerät. Kein Store, keine Weitergabe |
| Startbestand | Aus `C:\Users\barwa\Meine Ablage\Dokumente\KI\Backup\Stack.docx` — 6 Stacks, 72 Einträge. Liegt als Datei in der App (nicht einkompiliert) und ist per Knopf neu einlesbar. Vollständige Liste in `STARTBESTAND.md` |

## 4. Ausdrücklich NICHT enthalten

- **Kein Einnahme-Tracking.** Kein Kalender, kein „heute genommen", keine Statistik über die
  Zeit — das leistet `NEMS`. StackLabor kennt keinen Tagesverlauf.
- **Keine Erinnerungen/Benachrichtigungen** zur Einnahme.
- **Keine Erstbenutzungs-Einführung**, keine Datenschutzerklärung, keine Store-Vorgaben —
  die App ist ausschließlich für Frank selbst.
- **Kein Mehrbenutzer-Konzept**, keine Profile.
- **Keine Cloud-Sicherung** (kein Google Drive), obwohl `NEMS` das kann.
- **Keine medizinische Beratung im Rechtssinne.** Die KI-Auswertung ist eine Arbeitshilfe für
  Franks eigene Entscheidungen.
- **Kein Zweck-Stack.** „Senolytika" und „Sport" sind Ziele, keine Stacks (siehe §5, A-01).
- **Später, nicht in dieser Fassung:** ein NEM in einen anderen Stack verschieben oder kopieren ·
  einen Stack duplizieren.

## 5. Abnahme — wann ist es fertig

| Kennung | Kriterium |
|---|---|
| A-01 | Ich kann einen Stack anlegen, ihm einen Zeitpunkt und einen Einnahme-Hinweis geben, ihn umbenennen und wieder löschen. Nach einem Neustart sind alle sechs Stacks aus meinem Startbestand da. |
| A-02 | Ich lege ein Ziel **einmal** im Ziel-Katalog an, hake es in drei verschiedenen Stacks an, und in jedem Stack steht es an einer anderen Position — ohne dass ich es dreimal getippt habe. |
| A-03 | Im Ziel-Vollbild ziehe ich ein Ziel von Position 12 auf Position 1. Die Nummern aller dazwischenliegenden Ziele laufen dabei sichtbar mit, und beim Loslassen rastet es ein. Die neue Reihenfolge steht nach einem Neustart noch so. |
| A-04 | Nach dem Ziehen aus A-03 haben sich Ampeln geändert — **ohne dass eine KI-Abfrage gelaufen ist.** Ich kann das prüfen, indem ich das Flugzeugmodus einschalte und es wiederhole. |
| A-05 | Ich nehme bei einem Nahrungsergänzungsmittel das Häkchen weg. Innerhalb einer Sekunde ändern sich die betroffenen Ziel-Ampeln, die geänderten pulsen einmal auf, und in der Auswertung steht, welche Ziele davon betroffen waren. Auch das funktioniert im Flugzeugmodus. |
| A-06 | Ich tippe auf „Diesen Stack auswerten". Während der Wartezeit sind die Ampeln entsättigt und pulsieren, ein Skelett zeigt, wo Text erscheinen wird, und der Antworttext baut sich wortweise auf. Am Ende trägt jedes Ziel und jedes Mittel eine Ampel. |
| A-07 | Ein Ziel, zu dem **kein einziges** Mittel in diesem Stack beiträgt, zeigt eine **graue** Ampel mit „nicht bedient" — nicht grün. |
| A-08 | Ich tippe auf die Ampel eines Mittels und sehe eine Liste: welches Ziel es stützt, welches es stört, jeweils mit einem Satz Begründung. |
| A-09 | Ich füge ein neues Mittel hinzu. Es steht sofort in der Liste, ich muss auf nichts warten. Kurz darauf erscheint ein Hinweis, ob es zu den Zielen passt, mit den Knöpfen „Behalten" und „Doch entfernen". Verlasse ich den Stack vorher, ist der Hinweis beim nächsten Öffnen noch da. |
| A-10 | Ich schreibe eine eigene Frage („Reicht mein Magnesium für die Schlafqualität?"), speichere sie, und sie wird bei der nächsten Auswertung dieses Stacks beantwortet. Die Frage bleibt nach dem Neustart erhalten. |
| A-11 | Ich tippe auf „Alle Stacks zusammen prüfen" und sehe die Tagesgesamtdosis je Wirkstoff. Magnesium (Bisglycinat) erscheint dort **einmal** mit der Summe aus Morgen- und Abend-Stack, nicht zweimal. |
| A-12 | Ich schalte die Sortierung von „Löslichkeit" auf „Einnahme" um. In der Löslichkeits-Ansicht stehen alle wasserlöslichen vor allen fettlöslichen; in der Einnahme-Ansicht steht die Reihenfolge, in der ich die Mittel tatsächlich nehme. |
| A-13 | Jedes Mittel trägt seine Löslichkeits-Punkte: grün gefüllt für wasserlöslich, weiß mit Rand für fettlöslich, beide für beides. Der weiße Punkt ist im **Hellmodus** deutlich zu erkennen. |
| A-14 | Ich lasse mir die Auswertung vorlesen. Ich kann die Stimme in den Einstellungen wechseln, und das Vorlesen läuft weiter, wenn ich den Bildschirm verlasse. |
| A-15 | Ich schalte auf dem Hauptbildschirm zwischen Hell und Dunkel um. Beide Fassungen sind vollständig — es gibt keinen Bildschirm und kein Blatt, das in einer der beiden falsch aussieht. |
| A-16 | Ich exportiere alles in eine Datei, lösche die App-Daten, importiere die Datei zurück — und alle Stacks, Ziele, eigenen Fragen und Bewertungen sind wieder da. |
| A-17 | Ich ändere die Dosis eines Mittels. Die Auswertungs-Karte markiert sich als „veraltet", die alten Ampeln bleiben aber sichtbar. Nehme ich dagegen nur ein Häkchen weg oder ziehe ein Ziel um, wird **nichts** als veraltet markiert. |
| A-18 | Ich klappe das Gerät auf. Der Bildschirm wechselt in das zweispaltige Layout, ohne dass meine Scrollposition oder ein geöffnetes Blatt verlorengeht. |
| A-19 | Ich schalte im System „Animationen reduzieren" ein. Dauerbewegung und Schmuck sind aus, aber die Ampel-Überblendung und das Ausweichen beim Ziehen laufen weiter. |
| A-20 | Codex ist nicht angemeldet: Der Auswerten-Knopf sagt das im Klartext und führt mich zur Anmeldung. Kein Netz: Der Knopf ist ausgegraut, aber alle Ampeln und Häkchen funktionieren weiter. |
| A-21 | **Kein toter Knopf.** Jedes Bedienelement auf jedem Bildschirm tut etwas Sichtbares. |

## 6. Offene Fragen

| Nr | Frage | Warum noch offen |
|---|---|---|
| O-01 | Wie genau soll der Auftragstext an Codex formuliert sein (Tonfall, Ausführlichkeit der Begründungen, ob Quellen genannt werden sollen)? | Frank hat den Schlussdurchgang zugunsten des Spec-Baus abgekürzt. Wird beim Bau nach bestem Wissen formuliert und ist danach in den Einstellungen nachjustierbar |
| O-02 | Ob die drei alternierenden Zyklen (Citicolin ↔ Uridin + Phosphatidylserin) als **Dreier**-Gruppe abgebildet werden müssen — das Feld „alterniert mit" trägt bisher nur Paare | Der Fall kommt im Startbestand genau einmal vor. Vorschlag für den Bau: das Feld nimmt mehrere Partner auf |
| O-03 | Ob die 🟡-Markierung „mittleres Durchfallrisiko" in der Auswertung eine eigene Rolle spielen soll (z. B. „zu viele risikobehaftete Mittel in einem Stack") oder nur informativ ist | Nicht gefragt worden. Wird zunächst nur als Feld geführt und der KI mitgeteilt |
| O-04 | Ob Frank Ziele auch **löschen** können soll, die noch in Stacks verwendet werden — und was dann mit deren Bewertungen geschieht | Vorschlag für den Bau: Warnung mit Nennung der betroffenen Stacks, danach Löschen samt zugehöriger Bewertungszellen |
