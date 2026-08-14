# StackLabor — Spec v1
Stand: 14.08.2026 · Plattform: Android · Zielgerät: Galaxy Z Fold 8 (SM-F971B), Leitgröße zugeklappt 1248 × 1972 px @ 420 dpi ≈ 297 × 469 dp · Sprache der Oberfläche: Deutsch
Herkunft: Specs/StackLabor/v1/ · Erzeugt von: spec-schmiede

> Diese Datei ist **allein verständlich**. Sie enthält den vollständigen Inhalt der drei
> Einzel-Specs wörtlich, damit der Designer keine weitere Datei braucht.


---

## 0. Was dieses Programm ist

> Der Auswertungsbereich. StackLabor soll ihm sagen, **ob seine Nahrungsergänzungsmittel die
> Ziele erreichen, die er sich gesetzt hat — und was ihnen im Weg steht.** Alles andere in
> dieser App dient dieser einen Auskunft.

---

### Zweck in drei Sätzen

StackLabor verwaltet Franks Nahrungsergänzungsmittel-Stacks — die Gruppen von Mitteln, die er
zu einem bestimmten Zeitpunkt einnimmt — und lässt ihn zu jedem Stack beliebig viele Ziele
festlegen, die er per Drag & Drop nach Wichtigkeit ordnet. Eine KI-Auswertung über Codex prüft
den Stack gegen diese Ziele: Welches Ziel wird erreicht, welches nur teilweise, welches gar
nicht — und welches Mittel steht welchem Ziel im Weg. Ampeln an jedem Ziel und an jedem Mittel
machen das Ergebnis auf einen Blick lesbar, und ein Häkchen an jedem Mittel zeigt sofort, was
sich ändert, wenn Frank es weglässt.

**StackLabor ist ausdrücklich kein Einnahme-Tracker.** Das Abhaken „heute genommen" leistet die
bestehende App `NEMS`. StackLabor ist das Werkzeug zum *Komponieren und Prüfen* der Stacks.

---

## 1. Auftrag an den Designer
### Vorgesehene Zielplattform

**Android.** Das ist die Absicht aus Stufe 1. Beim Herunterladen fragt Werft Studio das
Zielsystem noch einmal ausdrücklich ab und übersetzt die Specs in dessen Sprache. Weichen beide
ab, gilt die Wahl beim Herunterladen.

**Leitgröße ist das zugeklappte Cover-Display des Galaxy Z Fold 8:**
1248 × 1972 px @ 420 dpi ≈ **297 × 469 dp**. Nutzbar nach Status- und Gestenleiste: **421 dp**.
Zusätzlich ist ein zweispaltiges Layout für das aufgeklappte Innendisplay
(1848 × 2448 px ≈ **440 × 583 dp**, 120 Hz) vorgesehen — siehe Teil B §10.

Die Systemschrift des Benutzers steht auf **90 %**. Alle sp-Werte im Spec sind darauf gerechnet.

### Der Auftrag

**Baue jeden Bildschirm aus Teil B §6 in jeder Erscheinung auf.** Das sind 15 Bildschirme
(B-01 bis B-15) in zwei vollständigen Erscheinungen: **Hell ist der Standard**, Dunkel ist
gleichwertig und wird über einen Umschalter im Kopf von B-01 erreicht. Keine der beiden
Erscheinungen ist eine Nebensache — es darf keinen Bildschirm geben, der nur in einer gebaut ist.

Gestalterisch ist ausdrücklich viel gewünscht: Der Benutzer hat „sehr viele verschiedene
Spezialeffekte in jeder Hinsicht" verlangt. Teil B §5 und Teil C nennen die Effekt-Familien mit
ihrem Ort. **Eine einzige Einschränkung:** Weichzeichner (Glasflächen) nur auf **festen**
Flächen — Kopfleiste, Sockel, Blätter — **niemals** über einer scrollenden Liste. Das ist der
einzige Effekt, der auf 120 Hz messbar ins Stocken führt.

### Die drei Dinge, die nicht verhandelbar sind

1. **Die Ampelfarben sind die lautesten Elemente im Bild.** Kein Schmuck, kein Verlauf und
   keine Akzentfarbe darf mit ihnen um Aufmerksamkeit konkurrieren oder ihnen ähneln.
   Grün, Gelb, Rot und Grau bedeuten in dieser App jeweils genau eine Sache.
2. **Der Mittel-Eintrag ist zweizeilig.** Das ist keine Geschmacksfrage: Einzeilig bleibt für
   den Namen rechnerisch 1 dp übrig (die Rechnung steht in Teil B §6a). Wer ihn einzeilig
   entwirft, entwirft etwas Unbaubares.
3. **Der Ziel-Bereich in B-02 verdrängt die Mittel-Liste nicht**, sondern legt sich als
   Überlagerung darüber. Verdrängt er, bleiben 77 dp und damit 1,3 Einträge übrig.

### Regeln für Ergänzungen

- **Jedes neue Bedienelement braucht eine Aufgabe.** Wer einen Knopf hinzufügt, beschreibt in
  einem Satz, was er tun soll — sonst entsteht beim Bauen ein toter Knopf, und beim Rückimport
  muss nachgefragt werden.
- **Kennungen bleiben erhalten.** Ein Bildschirm, der hier `B-03` heißt, heißt auch im Rücklauf
  `B-03`. Dasselbe gilt für `F-`, `M-` und `A-`.
- **Neues bekommt die nächste freie Nummer** und wird als `NEU` gekennzeichnet.
- **Weggelassenes** wird als `ENTFALLEN` gekennzeichnet, mit einer kurzen Begründung — statt
  einfach zu fehlen.

### Was ausdrücklich nicht ins Design gehört

- Kein Erststart-Ablauf, keine Einführung, keine Tour. Die App ist ausschließlich für ihren
  einen Benutzer; er kennt sie.
- Keine Datenschutz-, Zustimmungs- oder Rechtstexte.
- Keine Anmeldung außer der Codex-Geräteanmeldung (B-11).
- Keine Fotos und keine Illustrationen. Die einzigen Bildelemente sind Ampeln,
  Löslichkeitspunkte und Symbole (Material Symbols Rounded).
- Kein Kalender und keine Einnahme-Historie. StackLabor ist ein Komponier-Werkzeug, kein
  Tracker — das leistet eine andere App.
- Keine Werbe-, Abo- oder Bezahlflächen.

---

## Teil A — Funktions-Spec

### 1. Überblick der Funktionen

| Kennung | Funktion | Bildschirm(e) | Stufe |
|---|---|---|---|
| F-01 | Stack anlegen, bearbeiten, löschen | B-01, B-13 | Kern |
| F-02 | Nahrungsergänzungsmittel zu einem Stack hinzufügen (mit nachlaufender Konkurrenzprüfung) | B-02, B-05, B-14 | Kern |
| F-03 | Nahrungsergänzungsmittel bearbeiten (Stammdaten + Stack-Dosis) | B-05 | Kern |
| F-04 | Nahrungsergänzungsmittel aus einem Stack entfernen | B-02 | Kern |
| F-05 | Nahrungsergänzungsmittel im Stack aktivieren/deaktivieren (Häkchen) | B-02 | Kern |
| F-06 | Sortierung umschalten: Löslichkeit ↔ Einnahme-Reihenfolge | B-02 | Kern |
| F-07 | Einnahme-Reihenfolge per Drag & Drop ändern | B-02 | Kern |
| F-08 | Ziel anlegen, umbenennen, löschen (global, einmalig) | B-03 | Kern |
| F-09 | Ziele für diesen Stack an- und abwählen | B-04 | Kern |
| F-10 | Ziele je Stack per Drag & Drop priorisieren | B-12 | Kern |
| F-11 | Eigene KI-Frage anlegen, ändern, löschen (je Stack) | B-08 | Kern |
| F-12 | Diesen Stack auswerten (Codex) | B-02 | Kern |
| F-13 | Alle Stacks zusammen prüfen (Codex) | B-09 | Kern |
| F-14 | Ampeln lokal neu berechnen | B-01, B-02, B-04 | Kern |
| F-15 | Aufschlüsselung öffnen (Mittel → Ziele oder Ziel → Mittel) | B-06 | Kern |
| F-16 | Auswertung vorlesen | B-07 | Kern |
| F-17 | Bei Codex anmelden / abmelden | B-11 | Kern |
| F-18 | Vorlese- und Codex-Einstellungen ändern | B-10 | Kern |
| F-19 | Alles exportieren | B-10 | Kern |
| F-20 | Aus Datei importieren | B-10 | Kern |
| F-21 | Startbestand einlesen | B-10 | Kern |
| F-22 | Zwischen Hell und Dunkel umschalten | B-01 | Kern |
| F-23 | Auswertung als „veraltet" markieren | B-01, B-02, B-09 | Kern |
| F-24 | Suchen in Stack-Liste und Katalog | B-02, B-03, B-14 | Kern |
| F-27 | Dosis-Variante umschalten (Frei / Dienst) | B-01, B-05 | Kern |
| F-28 | Kombi-Gruppe „zusammen einnehmen" bilden und auflösen | B-02, B-05 | Kern |
| F-29 | Auswertungs-Historie ansehen und vergleichen | B-15 | Kern |
| F-30 | Nahrungsergänzungsmittel im Katalog verwalten und zusammenführen | B-14 | Kern |
| F-25 | Mittel in einen anderen Stack verschieben oder kopieren | B-02, B-13 | später |
| F-26 | Stack duplizieren | B-01 | später |

### 2. Funktionen im Einzelnen

#### F-01 — Stack anlegen, bearbeiten, löschen

- **Auslöser** — Plus auf B-01 (anlegen) · Überlaufmenü im Kopf von B-02 oder langes Drücken einer Karte auf B-01 (bearbeiten/löschen)
- **Ablauf** — 1. Blatt B-13 öffnet. 2. Felder: Name (Pflicht), Zeitpunkt-Beschreibung (z. B. „60 Minuten nach dem Aufstehen"), Einnahme-Hinweis (z. B. „mit 1 EL Olivenöl und Wasser"). 3. Sichern schließt das Blatt. 4. Beim Löschen: Rückfrage mit Nennung der Anzahl enthaltener Mittel.
- **Daten** — schreibt `Stack`. Beim Löschen werden die `StackEintrag`-Zeilen, die Zielzuordnungen, die eigenen Fragen und alle Bewertungen dieses Stacks mitgelöscht. Die Mittel selbst bleiben im Katalog (F-30).
- **Ergebnis** — Der Stack erscheint als Karte auf B-01, einsortiert nach `sortierung`.
- **Fehlerfall** — Leerer Name: Sichern bleibt gesperrt, das Feld wird rot umrandet. Doppelter Name: erlaubt, aber Hinweiszeile „Es gibt bereits einen Stack mit diesem Namen".
- **Regeln/Grenzen** — Name max 60 Zeichen. Zeitpunkt und Einnahme-Hinweis je max 120 Zeichen. Beide gehen in die KI-Anfrage ein (F-12) und beeinflussen die Bewertung.

#### F-02 — Mittel zu einem Stack hinzufügen

- **Auslöser** — Plus in der NEM-Liste auf B-02.
- **Ablauf** — 1. B-14 öffnet mit Suchfeld über den Katalog. 2. Frank wählt ein vorhandenes Mittel oder legt über „Neu anlegen" eines an (führt zu B-05). 3. Er trägt die **Stack-Dosis** ein (Stückzahl × Menge je Stück, Einheit) — die Stammdaten kommen aus dem Katalog. 4. Der Eintrag steht **sofort** in der Liste, ohne Wartezeit. 5. Ab der letzten Eingabe läuft ein Ruhefenster von **3 Sekunden**; danach startet **eine** Konkurrenzprüfung für **alle** in diesem Fenster hinzugefügten Mittel. 6. Das Ergebnis erscheint als Schnipsel über dem Auswerten-Sockel: je neuem Mittel eine Zeile („stützt Ziel 1, 2 · stört Ziel 7: …"), dazu die Knöpfe **Behalten** und **Doch entfernen**.
- **Daten** — schreibt `StackEintrag`. Das Prüfergebnis wird als `offenerHinweis` am `StackEintrag` gespeichert, nicht nur angezeigt.
- **Ergebnis** — Das Mittel ist im Stack, aktiv (Häkchen gesetzt), und die Bewertung des Stacks ist als **veraltet** markiert (F-23).
- **Fehlerfall** — Kein Netz oder Codex nicht angemeldet: Das Mittel wird trotzdem aufgenommen; statt des Hinweises erscheint „Konkurrenzprüfung nicht möglich — beim nächsten Auswerten". Läuft bereits eine Prüfung, wird sie abgebrochen und mit der erweiterten Menge neu gestartet. Verlässt Frank B-02 vor dem Ergebnis, geht der Hinweis **nicht** verloren: Die Stack-Karte auf B-01 trägt einen Punkt, und beim nächsten Öffnen von B-02 erscheint das Schnipsel erneut.
- **Regeln/Grenzen** — Ein Mittel darf in **einem** Stack nur einmal vorkommen (in mehreren Stacks dagegen beliebig oft). Der Versuch, es erneut hinzuzufügen, springt zum vorhandenen Eintrag und hebt ihn kurz hervor.

#### F-03 — Mittel bearbeiten

- **Auslöser** — Tippen auf einen Eintrag in der NEM-Liste (B-02) oder auf ein Mittel im Katalog (B-14).
- **Ablauf** — Blatt B-05 öffnet mit zwei erkennbar getrennten Bereichen: **Stammdaten** (gelten überall) und **In diesem Stack** (gilt nur hier). Änderungen an Stammdaten werden mit dem Hinweis „gilt in N Stacks" versehen.
- **Daten** — Stammdaten schreiben `Mittel`; Dosis und Zusatzangaben schreiben `StackEintrag`.
- **Ergebnis** — Der Eintrag zeigt die neuen Werte; je nach geändertem Feld wird die Bewertung veraltet (siehe F-23).
- **Fehlerfall** — Menge ≤ 0 oder leer: Sichern gesperrt. Einheit ohne Menge: Sichern gesperrt.
- **Regeln/Grenzen** — Felder siehe §3 (Datenmodell). Der Zusatztext ist auf 300 Zeichen begrenzt und geht wörtlich in die KI-Anfrage ein.

#### F-04 — Mittel aus dem Stack entfernen

- **Auslöser** — Wischen des Eintrags nach links auf B-02.
- **Ablauf** — 1. Der Eintrag gleitet weg. 2. Eine Rückmeldung („Entfernt — Rückgängig") steht 6 Sekunden. 3. Danach ist er weg.
- **Daten** — löscht `StackEintrag` und die zugehörigen Bewertungszellen dieses Stacks. Das Mittel bleibt im Katalog.
- **Ergebnis** — Die Liste schließt die Lücke, Ampeln rechnen neu (F-14), Bewertung wird veraltet.
- **Fehlerfall** — Keiner. „Rückgängig" stellt den Eintrag samt Dosis und Position wieder her.
- **Regeln/Grenzen** — Gehört das Mittel zu einer Kombi-Gruppe (F-28), fragt die App, ob nur dieses Mittel oder die ganze Gruppe entfernt wird.

#### F-05 — Mittel aktivieren/deaktivieren (das Häkchen)

- **Auslöser** — Tippen auf das Häkchen-Kästchen eines Eintrags.
- **Ablauf** — 1. Das Kästchen wechselt seinen Zustand (leichte Haptik). 2. Der Eintrag wird ausgegraut bzw. wieder normal. 3. **Sofort** danach rechnet F-14 alle Ampeln neu — die Ziel-Ampeln überblenden gestaffelt von oben, die tatsächlich geänderten pulsen einmal auf. 4. Die Auswertungs-Karte ergänzt eine Zeile: „Ohne <Mittel>: Ziel 4 wird grün, Ziel 7 wird rot."
- **Daten** — schreibt `StackEintrag.aktiv`. **Dauerhaft gespeichert** — der Zustand überlebt das Schließen der App.
- **Ergebnis** — Das Mittel ist im Stack abgeschaltet, bleibt aber sichtbar. Es geht **nicht** in die nächste KI-Anfrage ein.
- **Fehlerfall** — Keiner. Funktioniert vollständig offline.
- **Regeln/Grenzen** — Das Häkchen macht die Bewertung **nicht** veraltet (F-23) — die Bewertungstabelle enthält die Beiträge aller Mittel und bleibt gültig. An einer Kombi-Gruppe schaltet das Gruppen-Häkchen alle Mitglieder; einzelne Mitglieder bleiben separat schaltbar, das Gruppen-Häkchen zeigt dann einen Teilzustand.

#### F-06 — Sortierung umschalten

- **Auslöser** — Zwei Chips in der Leiste über der Liste: „Löslichkeit" (Vorgabe) und „Einnahme".
- **Ablauf** — Die Liste ordnet sich um. In „Löslichkeit": erst alle wasserlöslichen, dann alle mit beidem, dann alle fettlöslichen; innerhalb jeder Gruppe die Einnahme-Reihenfolge. In „Einnahme": die gespeicherte Reihenfolge.
- **Daten** — schreibt nur `Einstellung.sortieransicht` (je Stack gemerkt). Die gespeicherte Reihenfolge wird **nie** verändert.
- **Ergebnis** — Andere Anzeige, gleiche Daten.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Kombi-Gruppen (F-28) bleiben in **beiden** Ansichten zusammen; die Gruppe wird nach der Löslichkeit ihres ersten Mitglieds einsortiert.

#### F-07 — Einnahme-Reihenfolge ändern

- **Auslöser** — Langes Drücken (300 ms) auf einen Eintrag — **nur** in der Ansicht „Einnahme".
- **Ablauf** — Aufnehmen, ziehen, ausweichen, loslassen (siehe `03-MOTION-SPEC.md`, M-18).
- **Daten** — schreibt `StackEintrag.reihenfolge` für alle betroffenen Zeilen.
- **Ergebnis** — Neue Einnahme-Reihenfolge, bleibt nach Neustart erhalten.
- **Fehlerfall** — Ziehen über den Listenrand rollt automatisch weiter; Abbruch fliegt zur Ausgangsposition zurück.
- **Regeln/Grenzen** — In der Ansicht „Löslichkeit" nimmt langes Drücken **nicht** auf, sondern öffnet ein Kontextmenü (Bearbeiten / Verschieben / Entfernen) und zeigt einmalig den Hinweis „Reihenfolge lässt sich nur in der Ansicht Einnahme ändern". Eine Kombi-Gruppe wird als Ganzes gezogen.

#### F-08 — Ziel anlegen, umbenennen, löschen

- **Auslöser** — Plus auf B-03 (Ziel-Katalog), erreichbar über das Zielscheiben-Symbol auf B-01.
- **Ablauf** — 1. Eingabefeld für den Zieltext. 2. Sichern legt das Ziel global an. 3. Die Liste zeigt zu jedem Ziel „in N Stacks verwendet".
- **Daten** — schreibt `Ziel`.
- **Ergebnis** — Das Ziel steht zur Auswahl in jedem Stack (F-09), ist aber zunächst nirgends aktiv.
- **Fehlerfall** — Leerer Text: Sichern gesperrt. Beim Löschen eines verwendeten Ziels: Warnung mit Nennung der betroffenen Stacks; nach Bestätigung werden auch dessen Bewertungszellen gelöscht.
- **Regeln/Grenzen** — Zieltext max 200 Zeichen, mehrzeilig erlaubt. **Keine Obergrenze für die Anzahl der Ziele** — 30 und mehr sind ausdrücklich vorgesehen.

#### F-09 — Ziele für diesen Stack an-/abwählen

- **Auslöser** — Tippen auf den Ziel-Streifen in B-02, dann Knopf „Ziele wählen" — oder direkt im Blatt B-04.
- **Ablauf** — 1. B-04 zeigt alle Ziele des Katalogs mit Kästchen. 2. Angehakte Ziele gelten in diesem Stack. 3. „Fertig" schließt das Blatt.
- **Daten** — schreibt `StackZiel` (Zuordnung Stack ↔ Ziel mit `rang`). Ein neu angehaktes Ziel bekommt den letzten Rang.
- **Ergebnis** — Das Ziel erscheint in der Ziel-Liste dieses Stacks und geht in die nächste Auswertung ein.
- **Fehlerfall** — Ist der Katalog leer, zeigt B-04 „Erst Ziele anlegen" mit Sprung zu B-03.
- **Regeln/Grenzen** — Das An- oder Abwählen macht die Bewertung **veraltet** (F-23), weil die KI dieses Ziel noch nie bewertet hat.

#### F-10 — Ziele priorisieren

- **Auslöser** — Knopf „Ordnen" im Ziel-Blatt B-04, öffnet das Vollbild B-12.
- **Ablauf** — 1. B-12 zeigt alle Ziele dieses Stacks, nummeriert 1, 2, 3 … 2. Langes Drücken nimmt ein Ziel auf. 3. Beim Ziehen weichen die anderen aus, und **die Nummern laufen live mit**. 4. Am Rand rollt die Liste automatisch weiter. 5. Loslassen rastet ein.
- **Daten** — schreibt `StackZiel.rang` für alle betroffenen Zeilen dieses Stacks. Die Ränge anderer Stacks bleiben unberührt.
- **Ergebnis** — Neue Prioritätsreihenfolge. **Alle Ampeln rechnen sofort neu** (F-14), weil das Zielgewicht in die Rechnung eingeht — ohne KI-Abfrage.
- **Fehlerfall** — Abbruch fliegt zurück.
- **Regeln/Grenzen** — Der Rang ist **je Stack** eigen: Dasselbe Ziel kann im Morgen-Stack Rang 1 und im Abend-Stack Rang 9 haben. Das Priorisieren macht die Bewertung **nicht** veraltet.

#### F-11 — Eigene KI-Fragen

- **Auslöser** — Knopf „Eigene Fragen" im Kopf von B-02, öffnet B-08.
- **Ablauf** — 1. B-08 listet die Fragen dieses Stacks. 2. Plus öffnet ein Eingabefeld. 3. Sichern legt die Frage an. 4. Wischen löscht sie.
- **Daten** — schreibt `EigeneFrage` (gehört zu genau einem Stack).
- **Ergebnis** — Die Frage geht bei **jeder** künftigen Auswertung dieses Stacks mit; ihre Antwort erscheint als eigener Abschnitt in der Auswertung.
- **Fehlerfall** — Leerer Text: Sichern gesperrt.
- **Regeln/Grenzen** — Fragetext max 300 Zeichen. **Keine Obergrenze für die Anzahl.** Anlegen, Ändern oder Löschen macht die Bewertung veraltet (F-23), weil die Antwort fehlt.

#### F-12 — Diesen Stack auswerten (die Kernfunktion)

- **Auslöser** — Knopf „Diesen Stack auswerten" im festen Sockel von B-02.
- **Ablauf** —
  1. Die App stellt die Anfrage zusammen (Inhalt siehe unten).
  2. Alle Ampeln entsättigen und pulsieren; ein Skelett zeigt, wo Text erscheinen wird.
  3. Die Anfrage geht an Codex (`chatgpt.com/backend-api/codex/responses`) mit dem in B-10 gewählten Modell und der gewählten Denkstufe.
  4. Der Fließtext läuft **streamend** ein und baut sich wortweise auf; darüber wechselt eine Fortschrittserzählung („prüfe Wechselwirkungen …", „gewichte Ziel 1–4 …").
  5. Am Ende wird das JSON geparst.
  6. Bewertungstabelle, Konkurrenzen, Antworten und Fließtext werden gespeichert.
  7. F-14 rechnet alle Ampeln aus der neuen Tabelle.
- **Was mitgeschickt wird** — Zeitpunkt und Einnahme-Hinweis des Stacks · je **aktivem** Mittel: Name, Gesamtdosis (Stückzahl × Menge), Darreichungsform, Löslichkeit, Frequenz, „alterniert mit", Hersteller, Durchfallrisiko, Zusatztext, Kombi-Gruppen-Zugehörigkeit · je Ziel: Rang und Text · alle eigenen Fragen dieses Stacks. **Deaktivierte Mittel (F-05) werden nicht mitgeschickt.**
- **Antwortformat** — erzwungenes JSON, **dünn besetzt**: Es werden **nur** Bewertungszellen gemeldet, die *nicht* neutral sind. Alles Nichtgenannte gilt als neutral.

```json
{ "zellen": [ { "nem": "<mittel_id>", "ziel": "<ziel_id>",
      "wirkung": "stuetzt|stoert",
      "staerke": 1,
      "grund": "max 140 Zeichen, ein Satz" } ],
  "konkurrenzen": [ { "nem_a": "<id>", "nem_b": "<id>",
      "art": "aufnahme|wirkung|zeitpunkt",
      "schwere": 2, "grund": "ein Satz" } ],
  "antworten": [ { "frage": "<id>", "text": "Antwort als Fließtext" } ],
  "gesamt": "Fließtext, Markdown — wird auf B-07 gezeigt und vorgelesen",
  "hinweise": [ "Einnahme- und Dosis-Warnungen, je ein Satz" ] }
```

  Verbindlich im Auftragstext: *Nur dieses JSON, kein Text davor oder danach. Nicht genannte Mittel-Ziel-Paare gelten als neutral. Als „alterniert mit" gekennzeichnete Paare nie als Konkurrenz melden.*
- **Daten** — schreibt `Bewertung` (mit Zeitstempel, Modell, Denkstufe und einer Prüfsumme über den bewerteten Inhalt), `Bewertungszelle`, `Konkurrenz`, `FrageAntwort`. Die vorherige Bewertung wandert in die Historie (F-29).
- **Ergebnis** — Jedes Ziel und jedes Mittel trägt eine Ampel; die Auswertungs-Karte zeigt einen dreizeiligen Auszug mit Zeitstempel; B-07 zeigt den vollen Text.
- **Fehlerfall** —
  - *Anmeldung abgelaufen* (`REAUTH`): Karte „Anmeldung abgelaufen" mit Knopf, der zu B-11 führt.
  - *Kontingent erschöpft* (`QUOTA`): Klartext mit der genannten Wartezeit, Knopf „Später erneut".
  - *Netzfehler* (`NETWORK`): ein automatischer zweiter Versuch nach 3 Sekunden, danach Karte „Erneut versuchen".
  - *Kaputtes oder unvollständiges JSON*: ein Wiederholversuch mit dem Zusatz „nur JSON, kein Fließtext". Scheitert auch der, wird der Fließtext gespeichert und die Tabelle bleibt leer — die Ampeln werden **grau** statt falsch.
  - In **allen** Fehlerfällen bleiben die zuletzt gültigen Ampeln sichtbar; nichts wird gelöscht.
- **Regeln/Grenzen** — Bei **mehr als 12** aktiven Zielen wird die Anfrage in zwei Läufe zerlegt (Ziele 1–12 und der Rest); die beiden Tabellen werden lokal vereinigt. Grund: 20 Mittel × 30 Ziele wären 600 Zellen und würden jede Antwort sprengen. Während ein Lauf läuft, ist der Knopf ein Abbruch-Knopf.

#### F-13 — Alle Stacks zusammen prüfen

- **Auslöser** — Leiste „Alle Stacks zusammen prüfen" auf B-01.
- **Ablauf** — Wie F-12, aber über alle Stacks. Zusätzlich rechnet die App **vorher** die Tagesgesamtdosis je Mittel aus: Summe über alle Stacks, in denen das Mittel aktiv ist, unter Berücksichtigung der Frequenz. Diese Summen gehen mit in die Anfrage.
- **Daten** — schreibt `Bewertung` mit `bereich = TAG` und Prüfsumme über alle Stacks.
- **Ergebnis** — B-09 zeigt zwei Abschnitte: **Tagesgesamtdosis** (je Wirkstoff eine Zeile mit Summe; Mehrfachvorkommen sind zusammengefasst und die beteiligten Stacks genannt) und **Konkurrenzen über Stacks hinweg**. Der Fließtext ist über denselben Vollbild-Bildschirm B-07 lesbar und vorlesbar wie bei F-12.
- **Fehlerfall** — Wie F-12, mit demselben vollständigen Zustandssatz (läuft, veraltet, offline, nicht angemeldet, Fehler).
- **Regeln/Grenzen** — Die Zusammenfassung erfolgt über die `mittel_id` aus dem Katalog, **nicht** über den Namen — sonst wären Tippfehler zwei verschiedene Wirkstoffe. Unterschiedliche Dosen werden addiert (1 Kapsel morgens + 2 Kapseln abends = 3 Kapseln = 465 mg); unterschiedliche Darreichungsformen werden nebeneinander genannt statt addiert.

#### F-14 — Ampeln lokal berechnen

- **Auslöser** — Nach jedem Ereignis, das die Anzeige betrifft: Häkchen (F-05), Priorität (F-10), Zielauswahl (F-09), Sortierung, neue Bewertung (F-12/F-13), Öffnen eines Bildschirms.
- **Ablauf** — Reine Rechnung auf den gespeicherten Bewertungszellen. **Kein Netz, keine Kosten, Ergebnis in Millisekunden.**
- **Die Regel:**
  - **Zielgewicht** aus dem Rang im jeweiligen Stack: Rang 1–3 → g = 3 · Rang 4–7 → g = 2 · ab Rang 8 → g = 1.
  - **Ampel eines Mittels** — über alle aktiven Ziele, in denen dieses Mittel stört: `p = g × staerke`.
    **rot** wenn max p ≥ 6 · **gelb** wenn max p ≥ 2 · **grün** wenn es kein Ziel stört.
    *Beispiel:* Ein Mittel stört ein Ziel auf Rang 2 (g = 3) mit Stärke 2 → p = 6 → rot. Rutscht dasselbe Ziel auf Rang 9 (g = 1), wird p = 2 → gelb. Genau das leistet das Umsortieren ohne KI-Abfrage.
  - **Ampel eines Ziels** — `S = Σ(staerke der stützenden aktiven Mittel) − Σ(staerke der störenden aktiven Mittel)`.
    **rot** wenn eine Störung der Stärke 3 vorliegt oder S ≤ −1 · **gelb** wenn S = 0 … 2 · **grün** wenn S ≥ 3 und keine Störung ≥ 2 · **grau „nicht bedient"** wenn **kein einziges** stützendes Mittel aktiv ist. Grau zählt in der Sammelampel wie gelb.
  - **Sammelampel eines Stacks** (B-01) — die schlechteste Ziel-Ampel dieses Stacks. Grau, solange keine Bewertung vorliegt.
- **Daten** — liest `Bewertungszelle`, `StackZiel.rang`, `StackEintrag.aktiv`. Schreibt nichts.
- **Ergebnis** — Alle Ampeln der aktuellen Ansicht überblenden auf ihren neuen Wert; die tatsächlich geänderten pulsen einmal auf.
- **Fehlerfall** — Liegt keine Bewertung vor, sind alle Ampeln grau und die Auswertungs-Karte sagt „Noch nicht ausgewertet".
- **Regeln/Grenzen** — Deaktivierte Mittel gehen **nicht** in die Rechnung ein. Ein Ziel ohne stützendes Mittel wird nie grün.

#### F-15 — Aufschlüsselung öffnen

- **Auslöser** — Tippen auf die Ampel eines Mittels (B-02) oder auf die Ampel eines Ziels (B-04/B-12).
- **Ablauf** — Blatt B-06 öffnet mit einem Richtungsparameter. Bei **Mittel → Ziele**: Kopf nennt das Mittel, darunter je Ziel eine Zeile (Nummer, Zieltext, Urteil *stützt/neutral/stört*, Begründung). Bei **Ziel → Mittel**: umgekehrt.
- **Daten** — liest `Bewertungszelle`.
- **Ergebnis** — Frank sieht die vollständige Begründung, die hinter der Ampel steht.
- **Fehlerfall** — Ohne Bewertung: „Dieser Stack wurde noch nicht ausgewertet" mit Knopf zum Auswerten. Bei veralteter Bewertung: Hinweiszeile im Kopf.
- **Regeln/Grenzen** — Neutrale Paare werden nur auf Wunsch eingeblendet („auch neutrale zeigen"), sonst wäre die Liste bei 30 Zielen unlesbar.

#### F-16 — Auswertung vorlesen

- **Auslöser** — Vorlese-Knopf im Sockel von B-07.
- **Ablauf** — 1. Der Fließtext wird an den in B-10 gewählten Anbieter geschickt. 2. Die Wiedergabe beginnt. 3. Der gerade gesprochene **Absatz** wird hinterlegt; am Knopf zeigen drei Pegelbalken die Lautstärke. 4. Pause und Stopp sind jederzeit möglich.
- **Daten** — liest `Bewertung.gesamt`. Schreibt den Verbrauch in den Zähler (`TtsUsageStore` aus `EntropieReductor`).
- **Ergebnis** — Der Text wird gesprochen; verlässt Frank den Bildschirm, läuft die Wiedergabe über einen Vordergrunddienst weiter, mit Stopp-Knopf in der Benachrichtigung.
- **Fehlerfall** — Anbieter nicht erreichbar oder Schlüssel fehlt: Klartextmeldung mit dem Vorschlag, den Anbieter zu wechseln. Kein Netz: Der Knopf ist ausgegraut mit Hinweis.
- **Regeln/Grenzen** — Vorgelesen wird **nur der Fließtext**, absatzweise — nicht die Bewertungstabelle und nicht die Zahlenwerte. Wortgenaue Markierung ist nicht vorgesehen, weil die vorhandene Sprachkette sie nicht zuverlässig liefert.

#### F-17 — Bei Codex anmelden / abmelden

- **Auslöser** — „Anmelden" in B-10, oder der Knopf aus einer `REAUTH`-Fehlerkarte.
- **Ablauf** — Geräte-Flow wie in `PerfectMoment`: 1. Die App holt einen Benutzercode. 2. B-11 zeigt den Code groß und die Verifizierungsadresse. 3. „Seite öffnen" startet den Browser. 4. Die App fragt im Takt nach, bis die Bestätigung vorliegt. 5. Zugangs- und Erneuerungsschlüssel werden verschlüsselt abgelegt.
- **Daten** — schreibt den Anmeldespeicher (`codex_oauth`): Zugangsschlüssel, Erneuerungsschlüssel, Ablaufzeitpunkt, Konto-Kennung, E-Mail.
- **Ergebnis** — B-10 zeigt das angemeldete Konto; die Auswerten-Knöpfe sind bereit.
- **Fehlerfall** — Code abgelaufen: neuer Code auf Knopfdruck. Verweigert: Klartext. Netzfehler: erneut versuchen.
- **Regeln/Grenzen** — Abmelden löscht alle Schlüssel, aber **keine** gespeicherten Bewertungen.

#### F-18 — Einstellungen ändern

- **Auslöser** — Zahnrad auf B-01.
- **Ablauf** — B-10 mit vier Rubriken: **Vorlesen** (Anbieter, Stimme, Tempo, Pause zwischen Absätzen, automatische Abschaltung, Verbrauchsanzeige) · **Codex** (Konto, Modell, Denkstufe) · **Daten** (Export, Import, Startbestand einlesen) · **Darstellung** (Hell/Dunkel, Bewegung reduzieren).
- **Daten** — schreibt `Einstellung`.
- **Ergebnis** — Die Änderung gilt sofort.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Vorgabe: Anbieter **Microsoft Edge**, Stimme `de-DE-SeraphinaMultilingualNeural`, Modell `gpt-5.6-terra`, Denkstufe `high`. Ein Wechsel von Modell oder Denkstufe macht bestehende Bewertungen **nicht** ungültig.

#### F-19 — Alles exportieren

- **Auslöser** — „Exportieren" in B-10.
- **Ablauf** — Die App schreibt eine Datei (JSON) mit Stacks, Katalog, Stack-Einträgen, Zielen, Zuordnungen samt Rängen, eigenen Fragen, Bewertungen samt Zellen und der Historie. Das System-Dateiblatt fragt nach dem Ablageort.
- **Daten** — liest alles. Die Datei trägt `schema_version` und einen Zeitstempel.
- **Ergebnis** — Eine Datei, die F-20 vollständig zurücklesen kann.
- **Fehlerfall** — Abbruch durch Frank: nichts geschieht. Schreibfehler: Klartextmeldung.
- **Regeln/Grenzen** — Keine Zugangsschlüssel in der Datei.

#### F-20 — Aus Datei importieren

- **Auslöser** — „Importieren" in B-10.
- **Ablauf** — 1. Dateiauswahl. 2. Die App legt **still eine Sicherung** des jetzigen Standes an. 3. Rückfrage mit drei Möglichkeiten: **Ersetzen** (alles Vorhandene wird verworfen) · **Dazu** (neue Stacks kommen hinzu; gleiche `mittel_id` und gleicher Zieltext werden zusammengeführt) · **Abbrechen**. 4. Der gewählte Weg wird ausgeführt.
- **Daten** — schreibt je nach Wahl.
- **Ergebnis** — Der Bestand entspricht der Datei bzw. der Vereinigung.
- **Fehlerfall** — Datei unlesbar oder kein StackLabor-Format: Abbruch mit Klartext, nichts wird verändert. **Ältere** `schema_version`: automatische Überführung. **Neuere** als die App: Abbruch mit dem Hinweis, die App sei älter als die Datei.
- **Regeln/Grenzen** — Der Import ist nicht rückgängig zu machen; die stille Sicherung aus Schritt 2 ist die Rückfallebene und wird in B-10 als „Letzte Sicherung wiederherstellen" angeboten.

#### F-21 — Startbestand einlesen

- **Auslöser** — Beim allerersten Start automatisch · später über „Startbestand einlesen" in B-10.
- **Ablauf** — Die App liest die mitgelieferte Datei (`startbestand.json` in den Assets) und legt daraus Katalog, Stacks und Stack-Einträge an. Beim späteren Aufruf erscheint vorher eine deutliche Warnung, dass vorhandene Stacks überschrieben werden.
- **Daten** — schreibt `Mittel`, `Stack`, `StackEintrag`. Ziele, eigene Fragen und Bewertungen bleiben unberührt.
- **Ergebnis** — Die 6 Stacks mit 72 Einträgen stehen bereit (siehe `STARTBESTAND.md`).
- **Fehlerfall** — Datei fehlt oder ist beschädigt: Klartextmeldung, die App startet leer und bleibt bedienbar.
- **Regeln/Grenzen** — Weil der Bestand als **Datei** und nicht einkompiliert vorliegt, lässt er sich aktualisieren, ohne die App neu zu bauen.

#### F-22 — Hell/Dunkel umschalten

- **Auslöser** — Sonne/Mond-Symbol im Kopf von B-01.
- **Ablauf** — Alle Farbwerte überblenden in 420 ms. Offene Blätter bleiben offen und blenden mit.
- **Daten** — schreibt `Einstellung.erscheinung`.
- **Ergebnis** — Die gesamte App wechselt die Erscheinung. **Vorgabe ist Hell.**
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Beide Fassungen sind vollständig; es gibt keinen Bildschirm, der nur in einer gebaut ist.

#### F-23 — „Veraltet"-Markierung

- **Auslöser** — Jede Änderung an einem Stack.
- **Ablauf** — Die App bildet eine Prüfsumme über den bewertungsrelevanten Inhalt und vergleicht sie mit der in der Bewertung gespeicherten. Bei Abweichung wird die Auswertungs-Karte bernsteinfarben umrandet und trägt „Stand veraltet — neu auswerten".
- **Daten** — liest `Bewertung.pruefsumme`.
- **Ergebnis** — Frank sieht, dass das Urteil nicht mehr zum Stack passt — **die Ampeln bleiben aber sichtbar**, damit er nicht ohne Anhalt dasteht.

| macht veraltet | macht **nicht** veraltet |
|---|---|
| Mittel hinzugefügt oder entfernt | Häkchen an/aus (F-05) |
| Dosis, Einheit oder Stückzahl geändert | Ziel-Priorität geändert (F-10) |
| Frequenz oder „alterniert mit" geändert | Sortierung umgeschaltet (F-06) |
| Zusatztext für die KI geändert | Einnahme-Reihenfolge geändert (F-07) |
| Ziel in diesem Stack an- oder abgewählt (F-09) | Ziel umbenannt (nur leiser Vermerk) |
| Neues Ziel angelegt und hier aktiviert | Hell/Dunkel, Stimme, Modellwahl |
| Eigene Frage angelegt/geändert/gelöscht (F-11) | Stack umbenannt |
| Zeitpunkt oder Einnahme-Hinweis geändert | Import, der diesen Stack nicht berührt |
| Darreichungsform oder Löslichkeit geändert | Auswertung eines anderen Stacks |
| Dosis-Variante gewechselt (F-27) | — |

#### F-24 — Suchen

- **Auslöser** — Lupe in der Leiste von B-02, Suchzeile in B-03 und B-14.
- **Ablauf** — Tippen filtert sofort auf Namensbestandteile, ohne Groß-/Kleinschreibung.
- **Daten** — liest nur.
- **Ergebnis** — Gefilterte Liste; die Trefferstelle im Namen wird hervorgehoben.
- **Fehlerfall** — Kein Treffer: „Nichts gefunden" mit Knopf „Neu anlegen".
- **Regeln/Grenzen** — In B-02 ist die Suche eingeklappt hinter der Lupe, um Höhe zu sparen. Während einer Suche ist das Ziehen (F-07) ausgesetzt.

#### F-27 — Dosis-Variante umschalten

- **Auslöser** — Umschalter „Frei / Dienst" im Kopf von B-01.
- **Ablauf** — Die App wechselt für alle Mittel mit zwei hinterlegten Dosen auf die jeweils andere.
- **Daten** — liest `StackEintrag.dosisVarianteA/B`, schreibt `Einstellung.dosisVariante`.
- **Ergebnis** — Angezeigte Dosis und Tagesgesamtdosis (F-13) rechnen mit der gewählten Variante.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Mittel ohne zweite Dosis bleiben unverändert. Der Wechsel macht die Bewertung **veraltet** (F-23), weil sich Wirkstoffmengen ändern. Im Startbestand betrifft das nur Venlafaxin (50 mg / 75 mg).

#### F-28 — Kombi-Gruppe „zusammen einnehmen"

- **Auslöser** — Mehrfachauswahl in der Liste (langes Drücken → „Gruppe bilden") oder Feld in B-05.
- **Ablauf** — Die gewählten Mittel bekommen dieselbe `gruppe_id`. In der Liste erscheinen sie mit einer 2 dp starken Klammerlinie links und einer Kopfzeile „zusammen einnehmen".
- **Daten** — schreibt `StackEintrag.gruppeId`.
- **Ergebnis** — Die Gruppe bleibt in **beiden** Sortieransichten zusammen; das Gruppen-Häkchen schaltet alle Mitglieder; Ziehen bewegt die ganze Gruppe.
- **Fehlerfall** — Keiner. Auflösen der Gruppe ist jederzeit möglich.
- **Regeln/Grenzen** — Eine Gruppe umfasst mindestens zwei Mittel. Im Startbestand betrifft das „EAAs + Kollagen" und „Whey-Protein + Kollagen + Vitamin C".

#### F-29 — Auswertungs-Historie

- **Auslöser** — Tippen auf den Zeitstempel der Auswertungs-Karte, öffnet B-15.
- **Ablauf** — Die letzten fünf Läufe dieses Stacks stehen untereinander mit Zeitpunkt, Modell und einer Kurzbilanz („4 grün · 1 gelb · 0 rot"). Zwei Läufe lassen sich vergleichen; die Unterschiede werden je Ziel benannt („Ziel 4: gelb → grün").
- **Daten** — liest `Bewertung` (Historie).
- **Ergebnis** — Frank sieht, was seine Änderung bewirkt hat.
- **Fehlerfall** — Weniger als zwei Läufe: Vergleich ist ausgegraut.
- **Regeln/Grenzen** — Es werden je Stack **fünf** Läufe aufbewahrt; der älteste fällt heraus.

#### F-30 — Katalog verwalten

- **Auslöser** — Katalog-Symbol auf B-01, öffnet B-14.
- **Ablauf** — Alle Mittel mit Stammdaten und der Angabe „in N Stacks". Anlegen, bearbeiten, löschen. Zwei Einträge lassen sich **zusammenführen** (falls doch einmal ein Dublett entstanden ist): Frank wählt den bleibenden, alle Stack-Einträge werden umgehängt.
- **Daten** — schreibt `Mittel`; beim Zusammenführen auch `StackEintrag.mittelId`.
- **Ergebnis** — Ein sauberer Bestand, auf dem die Tagesgesamtdosis (F-13) zuverlässig rechnet.
- **Fehlerfall** — Löschen eines verwendeten Mittels: Warnung mit Nennung der Stacks.
- **Regeln/Grenzen** — Das Zusammenführen behält die Dosis des jeweiligen Stack-Eintrags bei.

### 3. Datenmodell

| Einheit | Feld | Typ | Pflicht | Standard | Wo gespeichert |
|---|---|---|---|---|---|
| **Mittel** (Katalog) | `id` | Text | ja | erzeugt | Room |
| | `name` | Text (max 80) | ja | — | Room |
| | `loeslichkeit` | WASSER \| FETT \| BEIDES | ja | WASSER | Room |
| | `darreichungsform` | KAPSEL \| TABLETTE \| LOEFFEL \| TASSE \| PULVER \| SONSTIGE | ja | KAPSEL | Room |
| | `hersteller` | Text (max 80) | nein | leer | Room |
| | `durchfallrisiko` | Ja/Nein | ja | nein | Room |
| | `beistoffe` | Text (max 200) | nein | leer | Room |
| **Stack** | `id`, `name` | Text | ja | — | Room |
| | `zeitpunkt` | Text (max 120) | nein | leer | Room |
| | `einnahmeHinweis` | Text (max 120) | nein | leer | Room |
| | `sortierung` | Zahl | ja | ans Ende | Room |
| **StackEintrag** | `id`, `stackId`, `mittelId` | Text | ja | — | Room |
| | `stueckzahl` | Zahl | ja | 1 | Room |
| | `mengeJeStueck` | Kommazahl | ja | — | Room |
| | `einheit` | MG \| UG \| G \| ML \| IE | ja | MG | Room |
| | `dosisVarianteB` | Kommazahl | nein | leer | Room |
| | `frequenz` | TAEGLICH \| ALLE_N_TAGE(n) | ja | TAEGLICH | Room |
| | `alterniertMit` | Liste von `mittelId` | nein | leer | Room |
| | `aktiv` | Ja/Nein | ja | ja | Room |
| | `reihenfolge` | Zahl | ja | ans Ende | Room |
| | `gruppeId` | Text | nein | leer | Room |
| | `zusatztext` | Text (max 300) | nein | leer | Room |
| | `offenerHinweis` | Text | nein | leer | Room |
| **Ziel** | `id`, `text` (max 200) | Text | ja | — | Room |
| **StackZiel** | `stackId`, `zielId` | Text | ja | — | Room |
| | `rang` | Zahl | ja | ans Ende | Room |
| **EigeneFrage** | `id`, `stackId`, `text` (max 300) | Text | ja | — | Room |
| **Bewertung** | `id`, `bereich` (STACK \| TAG), `stackId` | Text | ja | — | Room |
| | `zeitpunkt`, `modell`, `denkstufe` | Text | ja | — | Room |
| | `pruefsumme` | Text | ja | — | Room |
| | `gesamt` (Fließtext), `hinweise` | Text | nein | leer | Room |
| **Bewertungszelle** | `bewertungId`, `mittelId`, `zielId` | Text | ja | — | Room |
| | `wirkung` (STUETZT \| STOERT), `staerke` (1–3), `grund` | — | ja | — | Room |
| **Konkurrenz** | `bewertungId`, `mittelA`, `mittelB`, `art`, `schwere`, `grund` | — | ja | — | Room |
| **FrageAntwort** | `bewertungId`, `frageId`, `text` | Text | ja | — | Room |
| **Einstellung** | `erscheinung` | HELL \| DUNKEL | ja | **HELL** | Room |
| | `ttsAnbieter` | EDGE \| GOOGLE_CLOUD \| QWEN_CLONE | ja | EDGE | Room |
| | `ttsStimme` | Text | ja | `de-DE-SeraphinaMultilingualNeural` | Room |
| | `codexModell` | Text | ja | `gpt-5.6-terra` | Room |
| | `codexDenkstufe` | Text | ja | `high` | Room |
| | `dosisVariante` | A \| B | ja | A | Room |
| | `bewegungReduziert` | Ja/Nein | ja | Systemwert | Room |
| **Anmeldung** | Zugangsschlüssel, Erneuerungsschlüssel, Ablauf, Konto, E-Mail | — | — | — | verschlüsselter Speicher `codex_oauth` |

### 4. Zustände und Übergänge

**Bewertung eines Stacks:**
`nie ausgewertet` → (F-12) → `läuft` → `gültig` → (Inhaltsänderung, F-23) → `veraltet` → (F-12) → `gültig`
`läuft` → (Fehler) → `fehlgeschlagen` (der vorherige Stand bleibt sichtbar) → (F-12) → `läuft`

**Konkurrenzprüfung eines neu hinzugefügten Mittels (F-02):**
`neu` → `Ruhefenster 3 s` → `prüft` → `Hinweis offen` → (Behalten / Doch entfernen) → `erledigt`
`prüft` → (kein Netz) → `nicht möglich` (wird beim nächsten Auswerten mit erledigt)

**Vorlesen (F-16):** `bereit` → `spricht` → `pausiert` ⇄ `spricht` → `beendet`; im Hintergrund über einen Vordergrunddienst.

### 5. Externe Dienste

| Dienst | Wofür | Schlüssel/Anmeldung | Verhalten ohne Netz |
|---|---|---|---|
| **Codex** — `chatgpt.com/backend-api/codex/responses`; OAuth-Geräte-Flow über `auth.openai.com/api/accounts/deviceauth/usercode` → `…/deviceauth/token`, Erneuerung über `auth.openai.com/oauth/token` | F-12, F-13, F-02 | OAuth im Geräte-Flow, wie `PerfectMoment/auth/CodexAuthManager.kt`. Kein Schlüssel im Projekt | Auswerten-Knopf ausgegraut mit Hinweis; alle Ampeln, Häkchen und Bearbeitungen laufen weiter |
| **Microsoft Edge TTS** | F-16 (Vorgabe) | keiner | Vorlesen ausgegraut |
| **Google Cloud TTS (Chirp 3 HD)** | F-16 | Schlüssel aus `$HOME/SK/` wie im Vorbildprojekt | Vorlesen ausgegraut |
| **Qwen-Stimmklon** („Meine Stimme") | F-16 | wie im Vorbildprojekt | Vorlesen ausgegraut |

Verfügbare Modelle: `gpt-5.6-sol` (Sol), `gpt-5.6-terra` (Terra, **Vorgabe**), `gpt-5.6-luna` (Luna).
Denkstufen: `low`, `medium`, `high` (**Vorgabe**), `xhigh`, `max`.
Fehlerklassen: `REAUTH`, `QUOTA`, `NETWORK` — mit Kennzeichen, ob ein Wiederholversuch sinnvoll ist.

### 6. Hintergrund und Lebenszyklus

- Eine laufende **Auswertung** (F-12/F-13) läuft weiter, wenn die App in den Hintergrund geht, und wird beim Zurückkehren fertig angezeigt. Wird die App beendet, gilt der Lauf als abgebrochen; der vorherige Stand bleibt.
- Eine laufende **Konkurrenzprüfung** (F-02) schreibt ihr Ergebnis in die Datenbank und geht nie verloren.
- Das **Vorlesen** (F-16) läuft über einen Vordergrunddienst weiter, mit Stopp-Knopf in der Benachrichtigung.
- Alle Bearbeitungen werden **sofort** geschrieben; es gibt kein „Sichern" auf Bildschirmebene außer in den Blättern.
- Beim **Faltvorgang** wechselt das Layout, ohne Zustand zu verlieren: Scrollposition, geöffnete Blätter und laufende Vorgänge bleiben.

### 7. Offene Fragen

Siehe `00-PROJEKT.md` §6 (O-01 bis O-04). Über die dort genannten hinaus ist auf der Funktionsseite nichts offen.

---

## Teil B — UI-Spec

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/StackLabor/v2/messung/`. Widerspricht ein Satz von
> hier der Messung, ist der Satz ueberholt — nicht die Messung falsch.

### 1. Gestalterische Grundhaltung

StackLabor ist ein **Laborinstrument, kein Ratgeber**. Es zeigt Zustände, nicht Meinungen: Was
grün ist, ist grün, und was nicht bedient wird, sieht auch so aus. Die Oberfläche ist hell,
klar und dicht — Frank arbeitet hier mit vielen Zeilen auf wenig Fläche und muss den Zustand
einer Liste in einem Blick erfassen können.

Die Gestaltung darf und soll aufwendig sein: Tiefe, Verläufe, Glas, Schimmer, Auren und
Bewegung sind ausdrücklich gewünscht. **Aber sie ordnen sich der Ablesbarkeit unter.** Die drei
Ampelfarben sind die lautesten Elemente im Bild; kein Schmuck darf mit ihnen um Aufmerksamkeit
konkurrieren. Deshalb ist die Akzentfarbe bewusst so gewählt, dass sie keiner Ampelfarbe
ähnelt.

Jede Zeile trägt sechs Informationen (Zustand, Name, Löslichkeit, Dosis, Frequenz, Wirkung auf
die Ziele). Der Weg dahin führt nicht über mehr Elemente nebeneinander, sondern über **zwei
Zeilen und eine ruhige Hierarchie**: Was Frank sucht, steht links oben; was er nur manchmal
braucht, klein darunter.

### 2. Erscheinungen (Themes)

Beide Erscheinungen sind **vollständig und gleichwertig**. Umgeschaltet wird über das
Sonne/Mond-Symbol im Kopf von B-01 (F-22).

#### 2.1 Hell — **Standard-Erscheinung**

| Rolle | Wert | Verwendung | Kontrast |
|---|---|---|---|
| Grund | `#F5F7FA` | Bildschirmhintergrund, deaktivierte Karte | — |
| Fläche / Karte | `#FFFFFF` | Stack-Karte, Mittel-Eintrag, Blätter | 1,05:1 auf Grund |
| Erhöhte Fläche | `#F1F5F9` | Sockel, Chips, Kopfleiste | 1,1:1 |
| Rand | `#E2E8F0` | Trenner 1 dp, Kartenkontur | 1,3:1 |
| Text stark | `#0F172A` | Mittel-Name, Titel, Zieltext | 17,4:1 auf Karte |
| Text schwach | `#64748B` | zweite Zeile, Zeitpunkte, Metazeile | 4,8:1 |
| Akzent | `#4F46E5` | Auswerten-Knopf, aktives Häkchen, Verweise | 7,6:1 |
| Ampel grün (Fläche) | `#047857` | Kantenbalken, Zählpunkt | 4,9:1 |
| Ampel gelb (Fläche) | `#D97706` | Kantenbalken, Zählpunkt | 3,3:1 |
| Ampel gelb (Text) | `#B45309` | Kurzgrund, Begründungszeile | 5,9:1 |
| Ampel rot | `#DC2626` | Kantenbalken, Aura, Warntext | 4,5:1 |
| Ampel rot (kräftig) | `#B91C1C` | Überschrift einer Fehlerkarte | 6,0:1 |
| Ampel grau („nicht bedient") | `#94A3B8` | Kantenbalken, Zählpunkt | 2,8:1 |
| Löslich wasser | `#059669` | 8 dp Punkt, gefüllt | 4,0:1 |
| Löslich fett | `#FFFFFF` mit 1,5 dp Rand `#64748B` | 8 dp Punkt, hohl | Rand 4,8:1 |
| Deaktiviert | `#CBD5E1` | Kantenbalken und Punkte im ausgegrauten Eintrag | 1,7:1 (bewusst schwach) |

> **Wichtig:** Die üblichen hellen Ampelfarben (`#34D399`, `#FBBF24`, `#F87171`) sind auf
> weißer Fläche zu blass und wurden deshalb für die helle Erscheinung nachgedunkelt.

#### 2.2 Dunkel — gleichwertige zweite Erscheinung

| Rolle | Wert | Verwendung |
|---|---|---|
| Grund | `#0B0E14` | Bildschirmhintergrund |
| Fläche / Karte | `#141A24` | Karten, Blätter |
| Erhöhte Fläche | `#1B2330` | Sockel, Chips, Kopfleiste |
| Rand | `#243040` | Trenner, Kontur |
| Text stark | `#E6EAF2` | Namen, Titel |
| Text schwach | `#9AA6B8` | zweite Zeile, Metazeile |
| Akzent | `#22D3EE` | Auswerten-Knopf, aktives Häkchen |
| Ampel grün | `#34D399` | Kantenbalken, Zählpunkt |
| Ampel gelb | `#FBBF24` | Kantenbalken, Zählpunkt |
| Ampel rot | `#F87171` | Kantenbalken, Aura |
| Ampel grau | `#64748B` | „nicht bedient" |
| Löslich wasser | `#34D399` | 8 dp Punkt, gefüllt |
| Löslich fett | `#FFFFFF` ohne Rand (auf dunkel selbst tragend) | 8 dp Punkt, gefüllt |
| Deaktiviert | `#334155` | ausgegrauter Eintrag |

### 3. Typografie

Schriftfamilie: **Inter** (oder die Systemschrift, wenn der Designer nichts anderes wählt),
Schnitte Regular 400, Medium 500, SemiBold 600.
Franks Systemschrift steht auf **90 %** — alle sp-Werte sind so gerechnet, dass sie damit passen.

| Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite |
|---|---|---|---|---|
| Bildschirmtitel (B-01) | 22 sp | 600 | 28 dp | −0,2 |
| Kopfzeile eines Bildschirms | 17 sp | 600 | 22 dp | 0 |
| Stack-Name auf der Karte | 16 sp | 600 | 20 dp | 0 |
| Mittel-Name (Zeile 1) | 15 sp | 500 | 20 dp | 0 |
| Zieltext | 14 sp | 500 | 18 dp | 0 |
| Zweite Zeile (Dosis, Frequenz) | 12 sp | 400 | 16 dp | +0,1 |
| Metazeile (Zeitstempel, Modell) | 12 sp | 400 | 16 dp | +0,1 |
| Ziel-Nummer im Kreis | 11 sp | 600 | — | 0 |
| Fließtext der Auswertung (B-07) | 15 sp | 400 | 22 dp | 0 |
| Geräte-Code (B-11) | 40 sp | 600 | 48 dp | +2,0 |

### 4. Maße und Raster

Grundraster **4 dp**. Alle Abstände sind Vielfache davon.

| Maß | Wert |
|---|---|
| Bildschirmrand seitlich | 12 dp |
| Kartenbreite (Cover) | 273 dp (= 297 − 2 × 12) |
| Nutzbare Höhe (Cover) | 421 dp (= 469 − 24 Statusleiste − 24 Gestenleiste) |
| Innenabstand einer Karte | 12 dp |
| Abstand zwischen Karten | 8 dp |
| Kopfleiste eines Unterbildschirms | 56 dp |
| Kopfbereich B-01 | 96 dp |
| Ziel-Streifen (zugeklappt) B-02 | 40 dp |
| Sortier-/Suchleiste B-02 | 36 dp |
| Auswerten-Sockel B-02 (fest) | 52 dp |
| **Verbleibende Listenhöhe B-02** | **237 dp = 4 Einträge** |
| Mittel-Eintrag | 273 × 56 dp, + 1 dp Trenner = **57 dp Takt** |
| Ziel-Eintrag | 273 × 40 dp |
| Stack-Karte B-01 | 273 × 76 dp |
| Mindest-Tippfläche | 44 × 44 dp |
| Schwebender Plus-Knopf | 56 dp, 16 dp vom Rand |

### 5. Formen und Tiefe

| Bauteil | Radius | Rand | Tiefe |
|---|---|---|---|
| Karte (Stack, Mittel) | 12 dp | 1 dp `Rand` | Schatten 2 dp, beim Ziehen 8 dp |
| Blatt (von unten) | 20 dp oben | keiner | Schatten 16 dp + Abdunklung 32 % |
| Knopf (Sockel) | 12 dp | keiner | Fläche `Akzent` |
| Chip (Sortierung) | vollrund | 1 dp bei inaktiv | keine |
| Ampel-Kantenbalken | 2 dp links | — | bei Rot zusätzlich Aura (siehe Motion M-21) |
| Löslichkeits-Punkt | vollrund, 8 dp Ø | 1,5 dp nur beim fettlöslichen (hell) | keine |
| Nummernkreis (Ziel) | vollrund, 20 dp Ø | 1 dp | keine |
| Eingabefeld | 12 dp | 1 dp, im Fehlerfall `Ampel rot` | keine |

**Verläufe:** Kartenrand als 1,5 dp starker Verlauf `Akzent` → transparent im Uhrzeigersinn.
Kopfbereich B-01 mit wanderndem Verlauf `Akzent` → `#0EA5E9` (siehe Motion M-16).
**Weichzeichner:** 24 dp, ausschließlich auf **festen** Flächen (Kopfleiste, Auswerten-Sockel,
Blätter) — **niemals** über der scrollenden Liste.

### 6. Bildschirme

| Kennung | Bildschirm | Zweck | Start? | führt zu |
|---|---|---|---|---|
| B-01 | Hauptbildschirm (Stack-Übersicht) | Alle Stacks auf einen Blick, Zugang zu allem | **ja** | B-02, B-03, B-09, B-10, B-13, B-14 |
| B-02 | Stack-Detail | Der Arbeitsplatz | nein | B-04, B-05, B-06, B-07, B-08, B-14, B-15 |
| B-03 | Ziel-Katalog | Ziele einmal anlegen | nein | — |
| B-04 | Ziele dieses Stacks (Blatt) | Ankreuzen, Ampeln und Gründe ansehen | nein | B-03, B-06, B-12 |
| B-05 | Mittel bearbeiten (Blatt) | Stammdaten + Stack-Dosis | nein | — |
| B-06 | Aufschlüsselung (Blatt) | Mittel → Ziele oder Ziel → Mittel | nein | — |
| B-07 | Auswertung im Vollbild | Voller Text, Vorlesen | nein | — |
| B-08 | Eigene Fragen (Blatt) | Fragen je Stack | nein | — |
| B-09 | Alle Stacks zusammen | Tagesgesamtdosis, übergreifende Konkurrenzen | nein | B-07 |
| B-10 | Einstellungen | Vorlesen, Codex, Daten, Darstellung | nein | B-11 |
| B-11 | Codex-Anmeldung | Geräte-Flow | nein | — |
| B-12 | Ziele ordnen (Vollbild) | Drag & Drop über die volle Höhe | nein | — |
| B-13 | Stack bearbeiten (Blatt) | Name, Zeitpunkt, Einnahme-Hinweis | nein | — |
| B-14 | Mittel-Katalog | Alle Mittel, Suche, Zusammenführen | nein | B-05 |
| B-15 | Auswertungs-Historie (Blatt) | Letzte fünf Läufe, Vergleich | nein | B-07 |

---

#### B-01 — Hauptbildschirm

**Aufbau von oben nach unten:**
1. **Kopfbereich 96 dp** — Titel „StackLabor" 22 sp bei x = 16. Rechts drei Symbolknöpfe à 44 dp mit 4 dp Zwischenraum (140 dp gesamt): Sonne/Mond (F-22), Zielscheibe → B-03, Zahnrad → B-10. Darunter der Umschalter **Frei / Dienst** (F-27) als Chip-Paar, nur sichtbar, wenn mindestens ein Mittel zwei Dosen hat.
2. **Leiste „Alle Stacks zusammen prüfen" 48 dp**, volle Breite, rechts der Zeitstempel der letzten Gesamtprüfung.
3. **Stack-Karten**, je 76 dp + 8 dp Abstand:
   - 3 dp Kantenbalken links = Sammelampel (schlechteste Ziel-Ampel)
   - Name 16 sp · darunter Zeitpunkt und Einnahme-Hinweis 12 sp `Text schwach`
   - Unten drei Zählpunkte à 8 dp („● 3 ● 1 ● 1") und „12 Mittel"
   - Ein kleiner Punkt rechts oben, wenn ein offener Hinweis aus F-02 vorliegt
4. **Schwebender Plus-Knopf 56 dp**, 16 dp vom rechten unteren Rand → B-13.

**Rechnung:** 421 − 96 − 48 = 277 dp → **3 Karten vollständig + 25 dp Anschnitt** (von sechs).

**Zustände:** leer (keine Stacks — Text „Noch kein Stack" mit Knopf) · nie ausgewertet (Balken grau) · veraltet (Balken gestrichelt) · Codex nicht angemeldet (Leiste zeigt „Anmelden") · offline (Leiste ausgegraut, Karten voll bedienbar).

---

#### B-02 — Stack-Detail

**Aufbau:**
1. **Kopfleiste 56 dp** — Zurück 44 dp · Stack-Name 17 sp, darunter Zeitpunkt 12 sp · Überlaufmenü 44 dp (Stack bearbeiten → B-13, Eigene Fragen → B-08, Historie → B-15).
2. **Ziel-Streifen 40 dp** — 3 dp Kantenbalken (schlechteste Ziel-Ampel) · „Ziele 5 · ● 3 ● 1 ● 1" · Pfeil rechts. Tippen öffnet B-04 als Überlagerung.
3. **Sortier- und Suchleiste 36 dp** — Chips „Löslichkeit" und „Einnahme" je 92 dp · Lupe 36 dp (klappt die Suchzeile aus).
4. **Mittel-Liste** — 237 dp, Takt 57 dp → **4 Einträge sichtbar** + 9 dp Anschnitt.
5. **Auswerten-Sockel 52 dp, fest am unteren Rand** — Knopf 273 × 44 dp, darüber eine 4 dp hohe Verlaufskante, damit die Liste sichtbar darunter verschwindet.

**Der Ziel-Bereich verdrängt die Liste nicht.** Aufgeklappt legt er sich als Überlagerung
(max 281 dp) darüber — würde er verdrängen, blieben 77 dp und damit 1,3 Mittel übrig.

**Zustände:** leer (keine Mittel) · lädt (Schimmer über 4 Platzhaltern) · Auswertung läuft (Sockel wird Fortschrittsbalken mit Abbrechen) · veraltet (Sockel bernstein, „Stand veraltet — neu auswerten") · Codex-Fehler (64 dp hohe Karte über dem Sockel, je nach Fehlerart mit passendem Knopf) · offline (Sockel ausgegraut, Ampeln und Häkchen arbeiten weiter) · offener Hinweis aus F-02 (Schnipsel 72 dp über dem Sockel, 20 Sekunden stehend, mit „Behalten" / „Doch entfernen").

---

#### B-03 — Ziel-Katalog

Kopfleiste 56 dp · Suchzeile 40 dp · Zeilen 56 dp (Zieltext 15 sp, darunter „in 4 Stacks
verwendet" 12 sp, rechts Stift 44 dp) · schwebender Plus-Knopf.
325 dp → 5 Zeilen + Anschnitt.
**Zustände:** leer · Löschwarnung, wenn das Ziel in Stacks verwendet wird (mit deren Namen).

---

#### B-04 — Ziele dieses Stacks (Blatt)

Griff 24 dp · Titel 40 dp („Ziele — Morgen-Stack Teil 1") · Zeilen 48 dp: Kästchen 22 dp links,
Nummer, Zieltext, Ampel als Kantenbalken. Maximale Höhe 70 % = 328 dp → 5 Ziele + Anschnitt.
Fußzeile 52 dp mit zwei Knöpfen: **„Ordnen"** (→ B-12) und **„Fertig"**.
Tippen auf eine Ampel öffnet B-06 (Richtung Ziel → Mittel). Tippen auf die Zeile klappt die
Begründung auf.
**Zustände:** leer (Katalog ohne Ziele — „Erst Ziele anlegen" mit Sprung zu B-03) · keine Bewertung (alle Ampeln grau).

---

#### B-05 — Mittel bearbeiten (Blatt)

Höhe 90 % = 421 dp, scrollend, zwei erkennbar getrennte Bereiche.

**Stammdaten** (mit Hinweis „gilt in N Stacks"): Name 72 dp · Löslichkeit als drei Chips 72 dp
(Wasser / Fett / Beides) · Darreichungsform 72 dp · Hersteller 72 dp · Durchfallrisiko-Schalter
56 dp · Beistoffe 72 dp.
**In diesem Stack**: eine 72-dp-Zeile mit drei Feldern nebeneinander — Stückzahl 88 dp ×
Menge 88 dp + Einheit 88 dp, darunter die Vorschau „2 × 80 mg = 160 mg" · zweite Dosis-Variante
72 dp (optional) · Frequenz 72 dp · „alterniert mit" 72 dp · Kombi-Gruppe 72 dp ·
Zusatztext für die KI 120 dp.
Fester Sockel 60 dp mit „Sichern".

**Zustände:** neu · bearbeiten · Pflichtfeld leer (Feld rot umrandet, Sichern gesperrt) · löschen.

---

#### B-06 — Aufschlüsselung (Blatt)

Höhe 70 %. Kopf 56 dp nennt den Gegenstand (Mittelname **oder** Zieltext). Je Gegenseite ein
Block von 76 dp: Nummer 20 dp · Text 15 sp · Urteilsmarke (stützt / neutral / stört) ·
Begründung zwei Zeilen 12 sp. Ein Schalter „auch neutrale zeigen".
**Zustände:** keine Bewertung vorhanden · Bewertung veraltet (Hinweiszeile im Kopf).

---

#### B-07 — Auswertung im Vollbild

Kopfleiste 56 dp · Metazeile 32 dp („ausgewertet 14.08. 12:28 · Terra · hoch") · Fließtext
15 sp mit 22 dp Zeilenhöhe, Rand 16 dp · fester Sockel 60 dp mit Vorlesen / Pause / Stopp und
drei Pegelbalken.
**Zustände:** lädt (Text baut sich wortweise auf, darüber die Fortschrittserzählung) · fertig ·
Vorlesen läuft · Vorlese-Fehler · offline (der zuletzt gespeicherte Text bleibt lesbar).

---

#### B-08 — Eigene Fragen (Blatt)

Kopf 56 dp · Zeilen 60 dp (Fragetext bis zwei Zeilen, Wischen löscht) · schwebender Plus-Knopf.
**Zustände:** leer („Ohne eigene Fragen antwortet die Auswertung allgemein").

---

#### B-09 — Alle Stacks zusammen

Kopfleiste 56 dp · Abschnitt **„Tagesgesamtdosis"** mit Zeilen à 44 dp (Wirkstoff 14 sp links,
Summe rechtsbündig auf 100 dp, darunter klein die beteiligten Stacks; 3 dp Balken bei
auffälliger Menge) · Abschnitt **„Konkurrenzen über Stacks hinweg"** als Karten à 72 dp ·
fester Sockel 52 dp „Alles prüfen".
Der Fließtext wird über **denselben** Vollbild-Bildschirm B-07 gelesen und vorgelesen.
**Zustände:** identisch zu B-02 (leer, lädt, veraltet, offline, nicht angemeldet, Fehler).

---

#### B-10 — Einstellungen

Kopfleiste 56 dp, danach vier Rubriken mit 32-dp-Überschriften und Zeilen à 56 dp:
- **Vorlesen** — Anbieter · Stimme · Tempo · Pause zwischen Absätzen · automatische Abschaltung · Verbrauch
- **Codex** — Konto (angemeldet als …) · Modell · Denkstufe
- **Daten** — Exportieren · Importieren · Startbestand einlesen · Letzte Sicherung wiederherstellen
- **Darstellung** — Hell/Dunkel · Bewegung reduzieren

**Zustände:** angemeldet / nicht angemeldet · Import läuft · Warnung vor dem Startbestand-Einlesen
(überschreibt 72 Einträge) · Verbrauchszähler leer.

---

#### B-11 — Codex-Anmeldung

Kopf 56 dp · Code 40 sp mittig in einem 96 dp hohen Feld · Verifizierungsadresse 44 dp ·
Knopf „Seite öffnen" 52 dp · Wartezeile 40 dp mit Pulsring.
**Zustände:** wartet · Code abgelaufen (neuer Code auf Knopfdruck) · verweigert · Netzfehler ·
bereits angemeldet (zeigt „Abmelden").

---

#### B-12 — Ziele ordnen (Vollbild)

Kopfleiste 56 dp („Ziele ordnen — <Stack>") · Liste über die **volle** verbleibende Höhe
(365 dp → 9 Ziele sichtbar) · kein Sockel, damit die Ablagefläche maximal ist.
Hier und nur hier findet das Ziehen der Ziele statt (M-01 bis M-06).
**Zustände:** weniger als zwei Ziele (Ziehen ausgesetzt, Hinweiszeile).

---

#### B-13 — Stack bearbeiten (Blatt)

Griff 24 dp · Titel 40 dp · Name 72 dp · Zeitpunkt 72 dp · Einnahme-Hinweis 72 dp ·
Sockel 60 dp mit „Sichern" und, beim Bearbeiten, „Stack löschen" in `Ampel rot`.
**Zustände:** neu · bearbeiten · Löschwarnung mit Anzahl der enthaltenen Mittel.

---

#### B-14 — Mittel-Katalog

Kopfleiste 56 dp · Suchzeile 40 dp · Zeilen 56 dp (Name 15 sp mit Löslichkeitspunkten, darunter
„in 3 Stacks · Kapsel · Thorne" 12 sp) · schwebender Plus-Knopf · Überlaufmenü mit „Zusammenführen".
**Zustände:** leer · Suche ohne Treffer (mit „Neu anlegen") · Zusammenführen-Auswahl (zwei
Einträge markiert, Frank wählt den bleibenden).

---

#### B-15 — Auswertungs-Historie (Blatt)

Kopf 56 dp · fünf Zeilen à 64 dp (Zeitpunkt, Modell, Kurzbilanz „4 grün · 1 gelb · 0 rot") ·
zwei Zeilen auswählbar → Vergleich als Liste der Unterschiede je Ziel („Ziel 4: gelb → grün").
**Zustände:** weniger als zwei Läufe (Vergleich ausgegraut).

---

### 6a. Der Mittel-Eintrag — exakt

Karte 273 × **56 dp**, Radius 12 dp, darunter 1 dp Trenner → **57 dp Takt**.

- **Kantenbalken 3 dp** ganz links, über die volle Höhe, Radius 2 dp links, Farbe = Ampel des Mittels.
- Inhalt beginnt bei **x = 13 dp** (3 dp Balken + 10 dp Luft).
- Rechts das **Häkchen-Kästchen 22 dp** in einer 44 × 44 dp Tippfläche, rechter Innenabstand 8 dp
  → **Textspalte 208 dp**.

**Zeile 1** (y = 8 … 28 dp):
Löslichkeitspunkte 8 dp Ø (ein Punkt belegt 14 dp, zwei Punkte 25 dp) · dann der **Name 15 sp,
`Text stark`, einzeilig, max 183 dp**.
Geprüft: „PEA (Palmitoylethanolamid)" ≈ 177 dp ✔ · „Acetyl-L-Carnitin (ALCAR)" ≈ 163 dp ✔ ·
„Nicotinamid-Ribosid (NR)" ≈ 163 dp ✔.
**Regel bei Überlauf:** erst den Klammerzusatz weglassen, dann Auslassungszeichen am Ende —
**nie** mitten im Wortstamm brechen.

**Zeile 2** (y = 30 … 48 dp, 12 sp, `Text schwach`):
Links die Dosis „2 × 80 mg = 160 mg" (max 130 dp) · „· Pulver" nur, wenn die Form nicht Kapsel
ist · „· alle 3 Tage" nur, wenn die Frequenz nicht täglich ist (verdrängt dann die Form) ·
rechtsbündig der **Kurzgrund auf 78 dp** in der Ampel-Textfarbe („stört 3, 7"), bei Grün leer.

**Deaktivierter Eintrag:** Fläche = `Grund` statt `Fläche` (nicht erhöht), Balken `#CBD5E1`,
alle Texte auf 38 % Deckkraft, Löslichkeitspunkte entsättigt auf `#94A3B8`, Kästchen leer,
kein Schatten, keine Aura.

**Kombi-Gruppe:** 2 dp starke Klammerlinie am linken Rand über alle Mitglieder, darüber eine
Kopfzeile 32 dp „zusammen einnehmen" mit dem Gruppen-Häkchen (zeigt einen Teilzustand, wenn nur
manche Mitglieder aktiv sind).

### 6b. Der Ziel-Eintrag — exakt

Zeile 273 × **40 dp**:
Kantenbalken 3 dp links (Ampel des Ziels) · **Nummernkreis 20 dp Ø** bei x = 13 dp, Zahl 11 sp ·
**Zieltext 14 sp ab x = 41 dp, Breite 192 dp**, einzeilig mit Auslassungszeichen ·
**Ziehgriff 24 dp** bei x = 241 dp in einer 44 dp Tippfläche (nur auf B-12).

**Begründung bei Rot oder Gelb:** eine **eigene aufklappende Zeile**, keine Sprechblase —
eine Blase würde die Nachbarn verdecken und mit dem Ziehen kollidieren. Tippen auf die Zeile
lässt die Höhe in 200 ms auf 40 + n × 16 + 8 dp wachsen; bei drei Zeilen zu 12 sp sind das
**96 dp**. Der Text beginnt bei x = 41 dp, Breite 216 dp, in `Text schwach`; der Kantenbalken
läuft über die gesamte aufgeklappte Höhe durch. Beim Ziehen klappt die Begründung automatisch zu.

### 7. Ikonografie und Bilder

Symbolsatz **Material Symbols, Variante Rounded, Strichstärke 400**, Größe 24 dp
(Kopfzeilen-Symbole 22 dp).

| Zweck | Symbol |
|---|---|
| Hell/Dunkel | `light_mode` / `dark_mode` |
| Ziel-Katalog | `target` (Zielscheibe) |
| Einstellungen | `settings` |
| Mittel-Katalog | `inventory_2` |
| Hinzufügen | `add` |
| Suchen | `search` |
| Ziehgriff | `drag_indicator` |
| Vorlesen / Pause / Stopp | `volume_up` / `pause` / `stop` |
| Auswerten | `auto_awesome` |
| Historie | `history` |
| Aufschlüsselung | `insights` |
| Zurück | `arrow_back` |
| Überlauf | `more_vert` |

Es gibt **keine Fotos und keine Illustrationen**. Die einzigen Bildelemente sind die Ampeln,
die Löslichkeitspunkte und die Symbole. App-Symbol: eine stilisierte Kapsel im Akzentton auf
hellem Grund, in der Dunkelfassung umgekehrt.

### 8. Texte

Alle festen Beschriftungen wörtlich, in dieser Schreibweise:

| Ort | Text |
|---|---|
| B-01 Titel | `StackLabor` |
| B-01 Leiste | `Alle Stacks zusammen prüfen` |
| B-01 leer | `Noch kein Stack. Tippe auf das Plus, um deinen ersten anzulegen.` |
| B-01 Dosis-Umschalter | `Frei` / `Dienst` |
| B-02 Sockel | `Diesen Stack auswerten` |
| B-02 Sockel veraltet | `Stand veraltet — neu auswerten` |
| B-02 Sockel läuft | `Auswertung läuft — Abbrechen` |
| B-02 Sortierchips | `Löslichkeit` / `Einnahme` |
| B-02 Ziel-Streifen | `Ziele {n}` |
| B-02 Hinweis F-02 | `{Mittel} stützt Ziel {…} · stört Ziel {…}` mit `Behalten` / `Doch entfernen` |
| B-02 Ziehen gesperrt | `Reihenfolge lässt sich nur in der Ansicht „Einnahme" ändern.` |
| B-02 leer | `Noch kein Mittel in diesem Stack.` |
| B-04 Knöpfe | `Ordnen` / `Fertig` |
| B-04 leer | `Erst Ziele anlegen` |
| B-05 Vorschau | `{Stückzahl} × {Menge} {Einheit} = {Gesamt} {Einheit}` |
| B-05 Stammdaten-Hinweis | `Gilt in {n} Stacks` |
| B-06 Schalter | `Auch neutrale zeigen` |
| B-06 ohne Bewertung | `Dieser Stack wurde noch nicht ausgewertet.` |
| B-07 Metazeile | `ausgewertet {Datum} {Uhrzeit} · {Modell} · {Denkstufe}` |
| B-08 leer | `Ohne eigene Fragen antwortet die Auswertung allgemein.` |
| B-09 Überschriften | `Tagesgesamtdosis` / `Konkurrenzen über Stacks hinweg` |
| B-10 Rubriken | `Vorlesen` · `Codex` · `Daten` · `Darstellung` |
| B-10 Warnung | `Der Startbestand überschreibt alle vorhandenen Stacks. Fortfahren?` |
| B-11 | `Öffne diese Seite und gib den Code ein:` / `Seite öffnen` |
| B-12 Titel | `Ziele ordnen — {Stack}` |
| Ampel grau | `nicht bedient` |
| Fehler REAUTH | `Anmeldung abgelaufen.` mit `Neu anmelden` |
| Fehler QUOTA | `Kontingent erschöpft. Wieder verfügbar in {Zeit}.` mit `Später erneut` |
| Fehler NETWORK | `Keine Verbindung.` mit `Erneut versuchen` |
| Offline-Hinweis | `Ohne Netz — Ampeln und Häkchen funktionieren weiter.` |
| Rückgängig | `Entfernt` mit `Rückgängig` |

Platzhalter stehen in geschweiften Klammern und werden zur Laufzeit ersetzt.

### 9. Barrierefreiheit

Die App ist nur für Frank; es gelten keine Store-Vorgaben. Trotzdem verbindlich:
- **Mindest-Tippfläche 44 × 44 dp** für jedes Bedienelement — auch dort, wo das sichtbare
  Element kleiner ist (Häkchen 22 dp, Ziehgriff 24 dp, Löslichkeitspunkt 8 dp).
- **Kontrast**: Text mindestens 4,5:1, Flächenfarben mindestens 3:1 — siehe die Tabellen in §2.
- **Farbe ist nie das einzige Merkmal.** Jede Ampel trägt zusätzlich den Kurzgrund als Text
  („stört 3, 7"), jede graue Ampel das Wort „nicht bedient". Der fettlösliche Punkt ist hohl,
  der wasserlösliche gefüllt — auch ohne Farbunterschied unterscheidbar.
- **Große Systemschrift**: Bis 130 % müssen alle Zeilen lesbar bleiben. Der Mittel-Eintrag darf
  dabei auf 64 dp wachsen; der Name bekommt dann eine zweite Zeile, statt abgeschnitten zu werden.
- Jedes Symbol ohne Beschriftung trägt eine Vorlesebeschreibung.

### 10. Zweispaltiges Layout (Innendisplay, 440 × 583 dp)

| Bildschirm | Aufteilung |
|---|---|
| B-01 | Kartenraster 2 × 212 dp — alle sechs Stacks ohne Scrollen |
| B-02 | Links Ziele 176 dp **dauerhaft offen** (die Überlagerung entfällt), rechts Mittel-Liste + Auswertung 264 dp |
| B-09 | Links Tagesgesamtdosis, rechts Konkurrenzen |
| B-10 | Links Rubriken 160 dp, rechts der Inhalt |
| B-03, B-07, B-11, B-12, B-14 | einspaltig, zentriert auf max 440 dp |
| Blätter (B-04, B-05, B-06, B-08, B-13, B-15) | bleiben Blätter, Breite gedeckelt auf 400 dp |

### 11. Offene Fragen

Keine über die in `00-PROJEKT.md` §6 genannten hinaus. Alle gestalterischen Aussagen dieses
Dokuments sind ohnehin Absicht und werden vom Entwurf des Designers überholt.

---

## Teil C — Motion-Spec

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/StackLabor/v2/messung/`. Widerspricht ein Satz von
> hier der Messung, ist der Satz ueberholt — nicht die Messung falsch.

### 1. Bewegungs-Grundhaltung

Das Innendisplay des Fold 8 läuft mit **120 Hz**. Bewegung, die auf 60 Hz stocken würde, ist
hier tragfähig — und ausdrücklich gewünscht: Frank hat „sehr viele verschiedene Spezialeffekte
in jeder Hinsicht" verlangt.

Das Grundtempo ist **straff, nicht verspielt**: 200–320 ms für alles, was auf eine Bedienung
antwortet. Die Leitkurve ist `cubic-bezier(0.2, 0, 0, 1)` — schneller Anlauf, weiches Ausrollen.

Zwei Bewegungen tragen in dieser App **Bedeutung** statt Schmuck, und sie sind deshalb die
wichtigsten des ganzen Dokuments:
1. **Der Ampel-Wechsel**, wenn Frank ein Häkchen wegnimmt oder ein Ziel höher zieht. Das ist der
   Moment, in dem die App ihren Wert zeigt.
2. **Das Ziehen der Ziele**, weil daran die gesamte Priorisierung hängt.

#### Was sich NIEMALS bewegen darf

- **Ampelfarben im Ruhezustand.** Kein Blinken, kein Pulsieren, keine Sättigungsschwankung.
  Ein pulsierendes Rot liest sich als „Alarm", nicht als Gestaltung. Einzige Ausnahme ist die
  bewusst gesetzte Aura an roten Ampeln (M-21) und der einmalige Puls nach einer Änderung (M-09).
- **Dosis- und Einheitszahlen.** Sie werden gelesen, nicht bestaunt.
- **Das Häkchen-Kästchen selbst.** Es wechselt seinen Zustand ohne Sprung oder Skalierung.
- **Die Ziel-Nummern**, außer während einer aktiven Umsortierung (M-03).
- **Der Begründungstext bei Rot oder Gelb.** Er klappt auf und steht dann still.

### 2. Kurven und Dauern

Diese Namen werden überall sonst nur noch referenziert, nie neu erfunden.

| Name | Dauer | Kurve | Wofür |
|---|---|---|---|
| `antwort` | 220 ms | `cubic-bezier(0.2, 0, 0, 1)` | Ausweichen, Umordnen, Listenbewegung |
| `zustand` | 320 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Ampel-Überblendung, Farbwechsel |
| `aufnehmen` | 140 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Anheben beim Ziehen |
| `einrasten` | 260 ms | Spring, Dämpfung 0,75 / Steifigkeit 380 | Loslassen |
| `blatt` | 300 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Blätter von unten |
| `aufklappen` | 380 ms | `cubic-bezier(0.2, 0, 0, 1)` | Ziel-Überlagerung, Begründungszeile |
| `erscheinung` | 420 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Hell/Dunkel-Wechsel |
| `puls` | 520 ms | `cubic-bezier(0, 0, 0, 1)` | einmaliger Ring an geänderten Ampeln |
| `atem` | 2400 ms | Sinus, endlos | Aura an roten Ampeln |
| `schimmer` | 1400 ms | linear, endlos | Ladeplatzhalter |
| `wandern` | 30 s | linear, endlos | Kopfverlauf B-01 |

### 3. Bewegungen im Einzelnen

#### M-01 — Ziel aufnehmen
- **Wo** B-12 · **Auslöser** langes Drücken, 300 ms
- **Was sich ändert** Skalierung 1,0 → 1,04 · Tiefe 1 → 8 dp · dazu ein haptischer Impuls (mittel)
- **Dauer/Kurve** 140 ms `aufnehmen`

#### M-02 — Ziele weichen aus
- **Wo** B-12 · **Auslöser** Ziehen über eine andere Zeile
- **Was sich ändert** Y-Verschiebung der Nachbarzeilen um ± 40 dp (Zeilenhöhe)
- **Dauer/Kurve** 220 ms `antwort`, je Zeile 12 ms versetzt

#### M-03 — Nummern laufen live mit
- **Wo** B-12 · **Auslöser** während des Ziehens, bei jedem Positionswechsel
- **Was sich ändert** Die Ziffer im Nummernkreis: Überblendung, dazu Y-Versatz 6 dp
- **Dauer/Kurve** 120 ms linear
- **Begründung** Frank sieht die neue Priorität, **während** er noch entscheidet — nicht erst danach.

#### M-04 — Loslassen und Einrasten
- **Wo** B-12 · **Auslöser** Finger heben
- **Was sich ändert** Position rastet auf den Zielplatz · Skalierung 1,04 → 1,0 · Tiefe 8 → 1 dp · haptischer Impuls (leicht)
- **Dauer/Kurve** 260 ms `einrasten`

#### M-05 — Automatisches Weiterrollen am Rand
- **Wo** B-12 · **Auslöser** Ziehen in die obere oder untere Randzone
- **Was sich ändert** Die Liste rollt weiter. Randzone **64 dp**, Geschwindigkeit linear von 0 auf **900 dp/s** über die Zonentiefe, Start erst nach 120 ms Verweilen (damit leichtes Zittern am Rand nichts auslöst)
- **Begründung** Ohne dies wäre ein Ziel von Position 28 auf Position 1 nicht in einem Zug zu bewegen.

#### M-06 — Abbruch
- **Wo** B-12, B-02 · **Auslöser** Ziehen über den Bildschirmrand hinaus und loslassen, oder Zurück-Geste
- **Was sich ändert** Rückflug zur Ausgangsposition · Tiefe 8 → 1 dp
- **Dauer/Kurve** 300 ms `antwort`

#### M-07 — Ampel-Überblendung
- **Wo** B-01, B-02, B-04 · **Auslöser** F-14 (Häkchen, Priorität, Zielauswahl, neue Bewertung)
- **Was sich ändert** Die Farbe des Kantenbalkens von der alten auf die neue Ampelfarbe, **niemals hart**. Der Farbweg macht die Richtung lesbar (rot → grün liest sich anders als grün → rot)
- **Dauer/Kurve** 320 ms `zustand`

#### M-08 — Ampeln gestaffelt
- **Wo** B-02, B-04 · **Auslöser** wie M-07, wenn mehrere Ampeln gleichzeitig wechseln
- **Was sich ändert** Die Überblendungen starten von oben nach unten versetzt, **45 ms** je Zeile, gedeckelt bei 10 Stufen (also höchstens 450 ms Gesamtwelle)
- **Begründung** Die Welle folgt der Prioritätsreihenfolge — Frank liest zuerst, was oben passiert.

#### M-09 — Puls nur an den geänderten Ampeln
- **Wo** B-02, B-04 · **Auslöser** F-14, aber nur für Ampeln, deren Wert sich **tatsächlich** geändert hat
- **Was sich ändert** Ein Ring um den Kantenbalken: Radius + 6 dp, Deckkraft 0,55 → 0
- **Dauer/Kurve** 520 ms `puls`, einmalig
- **Begründung** Bei 30 Zielen ginge der eine wichtige Wechsel sonst unter. Unveränderte Ampeln pulsen **nicht**.

#### M-10 — Verbindungsfarbe Mittel ↔ Ziel
- **Wo** B-06 · **Auslöser** Öffnen der Aufschlüsselung
- **Was sich ändert** Der Gegenstand im Kopf und die betroffenen Zeilen bekommen für 900 ms einen 2 dp starken Rand in derselben Farbe
- **Dauer/Kurve** Einblenden 180 ms `antwort`, Halten 520 ms, Ausblenden 400 ms

#### M-11 — Warte-Skelett mit Schimmer
- **Wo** B-02, B-07, B-09 · **Auslöser** F-12, F-13 laufen
- **Was sich ändert** Platzhalterflächen mit einem Lichtstreifen, der von links nach rechts wandert; Streifenbreite 40 % der Fläche
- **Dauer/Kurve** Periode 1400 ms `schimmer`, endlos bis zum Ergebnis

#### M-12 — Ampeln entsättigt und pulsierend
- **Wo** B-02, B-04, B-09 · **Auslöser** solange eine Auswertung läuft
- **Was sich ändert** Alle Ampeln werden entsättigt (grau) und atmen: Deckkraft 1,0 → 0,45 → 1,0
- **Dauer/Kurve** Periode 1600 ms, `cubic-bezier(0.4, 0, 0.6, 1)`
- **Begründung** Das ist der wichtigste Wartezustand: Frank sieht sofort, dass die angezeigten Farben **gerade nicht gelten** — sonst entscheidet er auf veralteten Ampeln.

#### M-13 — Streamender Antworttext
- **Wo** B-07, und als dreizeiliger Auszug auf B-02 · **Auslöser** F-12, F-13
- **Was sich ändert** Der Text erscheint wortweise: je Wort Einblenden + 6 dp Aufwärtsbewegung, 60 ms Abstand zwischen den Wörtern. Darüber läuft eine Fortschrittserzählung („prüfe Wechselwirkungen …", „gewichte Ziel 1–4 …"), deren Text alle paar Sekunden überblendet
- **Dauer/Kurve** je Wort 180 ms `aufnehmen`; Erzähler-Wechsel 240 ms Überblendung
- **Begründung** Aus 20 Sekunden Warten werden 20 Sekunden Lesen. Die Erzählung deckt die Zeit vor dem ersten Wort ab.

#### M-14 — Ziel-Überlagerung aufklappen
- **Wo** B-02 → B-04 · **Auslöser** Tippen auf den Ziel-Streifen
- **Was sich ändert** Höhe 0 → max 281 dp; die Zeilen erscheinen gestaffelt mit 30 ms Abstand, gedeckelt bei 12 sichtbaren Zeilen; der Pfeil dreht 180°
- **Dauer/Kurve** 380 ms `aufklappen`, Pfeil 300 ms
- **Begründung** Die Deckelung hält die Kosten gleich, ob 5 oder 30 Ziele vorliegen.

#### M-15 — Sprech-Markierung und Pegel
- **Wo** B-07 · **Auslöser** F-16 läuft
- **Was sich ändert** Der gerade gesprochene **Absatz** bekommt eine Hintergrundfläche, deren Kante mitgleitet · am Sprech-Knopf zeigen drei Balken den Pegel (aus `SpeechLoudness`), Nachlauf 90 ms
- **Dauer/Kurve** Kantenbewegung 200 ms `zustand`
- **Begründung** Wortgenaue Marken liefert die vorhandene Sprachkette nicht zuverlässig — absatzweise ist ehrlich und robust.

#### M-16 — Dauerbewegung
- **Wo** B-01, B-02 · **Auslöser** keiner, läuft ohne Zutun
- **Was sich ändert**
  - Kopfverlauf auf B-01 wandert `Akzent` → `#0EA5E9` und zurück, Periode **30 s**, nur bei sichtbarem Bildschirm
  - Glanzkante an Karten, Periode **8 s**
  - Der schwebende Plus-Knopf atmet, Skalierung 1,0 → 1,02, Periode **3200 ms**
- **Begründung** Alle Perioden liegen bei **3 Sekunden oder darüber**. Alles Schnellere wirkt unruhig und zieht den Blick von den Ampeln ab.

#### M-17 — Faltvorgang
- **Wo** alle Bildschirme · **Auslöser** Gerät wird auf- oder zugeklappt
- **Was sich ändert** Gemeinsame Elemente wandern an ihren neuen Platz; die zweite Spalte schiebt sich von rechts ein (X + 24 dp → 0, Deckkraft 0 → 1, 300 ms, 100 ms verzögert)
- **Dauer/Kurve** 400 ms `antwort`
- **Regel** Scrollposition, geöffnete Blätter und laufende Vorgänge bleiben erhalten — das Aufklappen fühlt sich wie ein Erweitern derselben Ansicht an, nicht wie ein Neustart.

#### M-18 — Mittel-Reihenfolge ziehen
- **Wo** B-02, **nur** in der Ansicht „Einnahme" · **Auslöser** langes Drücken, 300 ms
- **Was sich ändert** Wie M-01 bis M-06
- **Regel** In der Ansicht „Löslichkeit" nimmt langes Drücken **nicht** auf, sondern öffnet ein Kontextmenü — es gibt keinen toten Ziehversuch. Eine Kombi-Gruppe wird als Ganzes gezogen.

#### M-19 — Erscheinungswechsel
- **Wo** die ganze App · **Auslöser** F-22
- **Was sich ändert** Alle Farbwerte überblenden gleichzeitig
- **Dauer/Kurve** 420 ms `erscheinung`
- **Regel** Offene Blätter bleiben offen und blenden mit. Ein harter Sprung wäre bei Glasflächen und Auren ein sichtbarer Bruch.

#### M-20 — Blatt öffnen und schließen
- **Wo** B-04, B-05, B-06, B-08, B-13, B-15 · **Auslöser** F-03, F-09, F-11, F-15, F-29
- **Was sich ändert** Das Blatt fährt von unten ein; dahinter dunkelt der Grund auf 32 % ab
- **Dauer/Kurve** 300 ms `blatt`

#### M-21 — Atmende Aura an roten Ampeln
- **Wo** B-01, B-02 · **Auslöser** eine Ampel steht auf Rot
- **Was sich ändert** Ein Glühen um den Kantenbalken, Radius 4 → 8 dp
- **Dauer/Kurve** Periode 2400 ms `atem`, endlos
- **Regel** **Höchstens drei gleichzeitig** — die drei mit dem höchsten Rang. Sonst wird die Liste unruhig und Rot verliert seine Warnwirkung.

#### M-22 — Gestaffeltes Einblenden beim Öffnen
- **Wo** B-01, B-02, B-03, B-14 · **Auslöser** Bildschirm wird geöffnet
- **Was sich ändert** Die Einträge erscheinen von oben nach unten, je 40 ms versetzt, mit 12 dp Aufwärtsbewegung
- **Dauer/Kurve** je Eintrag 220 ms `antwort`, gedeckelt bei 8 Elementen

#### M-23 — Häkchen-Rückmeldung
- **Wo** B-02 · **Auslöser** F-05
- **Was sich ändert** Das Kästchen selbst bewegt sich **nicht** (siehe §1). Stattdessen: haptischer Impuls (leicht) und die Kartenfläche wechselt auf den ausgegrauten Zustand
- **Dauer/Kurve** 220 ms `antwort` für den Flächen- und Deckkraftwechsel

#### M-24 — Wischen zum Entfernen
- **Wo** B-02, B-03, B-08, B-14 · **Auslöser** F-04, F-08, F-11
- **Was sich ändert** Der Eintrag folgt dem Finger; hinter ihm erscheint eine rote Fläche mit Papierkorb. Nach dem Loslassen jenseits der halben Breite gleitet er aus dem Bild, die Lücke schließt sich, und die Rückgängig-Leiste fährt von unten ein
- **Dauer/Kurve** Ausgleiten 220 ms `antwort`, Lückenschluss 220 ms, Leiste 300 ms `blatt`, sichtbar 6 s · haptischer Impuls (schwer) beim Auslösen

### 4. Bildschirmwechsel

| Von | Nach | Art | Dauer | Kurve |
|---|---|---|---|---|
| B-01 | B-02 | Vorwärts: neuer Bildschirm schiebt von rechts (X + 32 dp → 0, Deckkraft 0 → 1); der alte weicht 16 dp nach links und dunkelt leicht ab | 300 ms | `antwort` |
| B-02 | B-01 | Rücklauf: umgekehrt, der alte schiebt nach rechts hinaus | 260 ms | `antwort` |
| B-01 | B-03, B-09, B-10, B-14 | wie B-01 → B-02 | 300 ms | `antwort` |
| B-02 | B-07, B-12 | Vollbild zieht von unten auf (Y + 48 dp → 0, Deckkraft 0 → 1) | 320 ms | `blatt` |
| B-07, B-12 | zurück | nach unten hinaus | 260 ms | `antwort` |
| beliebig | Blatt (B-04, B-05, B-06, B-08, B-13, B-15) | M-20 | 300 ms | `blatt` |
| B-10 | B-11 | wie B-01 → B-02 | 300 ms | `antwort` |

Der **Rücklauf ist immer 40 ms kürzer** als der Hinlauf. Zurückgehen soll sich schneller
anfühlen als Vorwärtsgehen.

### 5. Rückmeldung auf Bedienung

| Element | Rückmeldung |
|---|---|
| Knopf (Sockel, Blatt) | Fläche dunkelt 8 % ab, Skalierung 1,0 → 0,98, 120 ms `antwort`; beim Loslassen zurück |
| Karte (Stack, Mittel) | Wellenring vom Berührungspunkt, 320 ms, Deckkraft 0,12 → 0 |
| Häkchen | M-23 — keine Bewegung des Kästchens, nur Haptik und Flächenwechsel |
| Chip (Sortierung) | Füllung wechselt in 180 ms `antwort` |
| Symbolknopf | Wellenring in einem 40 dp Kreis, 280 ms |
| Langes Drücken | M-01 bzw. Kontextmenü, jeweils mit haptischem Impuls |
| Wischen | M-24 |
| Ziehen | M-01 bis M-06 |

**Haptik:** leicht beim Häkchen und beim Einrasten · mittel beim Aufnehmen eines Ziels ·
schwer beim Auslösen des Wischen-Löschens · doppelt, wenn eine Auswertung eine **neue rote**
Ampel hervorgebracht hat.

### 6. Dauerbewegung

Siehe M-16 (Kopfverlauf 30 s · Glanzkante 8 s · Plus-Knopf 3200 ms) und M-21 (Aura an roten
Ampeln, 2400 ms, höchstens drei gleichzeitig).

Alles andere steht still. Die Grenze ist bewusst gesetzt: **keine Dauerbewegung mit einer
Periode unter 3 Sekunden**, außer der Aura, die als Warnzeichen gemeint ist.

### 7. Lade- und Wartezustände

| Wartefall | Ab wann | Was sich zeigt | Wie es endet |
|---|---|---|---|
| Auswertung (F-12, F-13) | sofort | M-11 Skelett + M-12 entsättigte Ampeln + M-13 streamender Text mit Fortschrittserzählung | Skelett wird vom Inhalt ersetzt (Überblendung 220 ms), Ampeln sättigen sich mit M-07/M-08/M-09 |
| Konkurrenzprüfung (F-02) | nach dem 3-s-Ruhefenster | Ein schmaler unbestimmter Fortschrittsbalken 2 dp unter dem Sockel — **kein** Blockieren der Bedienung | Hinweis-Schnipsel fährt mit M-20 ein |
| Anmeldung (F-17) | sofort | Pulsring an der Wartezeile, Periode 1600 ms | Wechsel auf „angemeldet", Ring verschwindet |
| Vorlesen startet (F-16) | nach 400 ms | Pegelbalken beginnen zu schwingen | Erster Absatz wird markiert |
| Import/Export (F-19, F-20) | sofort | Bestimmter Fortschrittsbalken im Blatt | Meldung „Fertig" |

Dauert eine Auswertung **länger als 45 Sekunden**, ergänzt die Fortschrittserzählung eine
Zeile „Das dauert länger als gewöhnlich." — abgebrochen wird nicht von selbst.

### 8. Reduzierte Bewegung

Meldet das System „Animationen reduzieren" (oder ist der Schalter in B-10 gesetzt):

**Bleibt erhalten — weil es Bedeutung trägt:**
- M-07 Ampel-Überblendung, volle 320 ms. Ein harter Farbsprung wäre nicht nur hässlich, sondern
  ließe Frank die Änderung übersehen.
- M-02 Ausweichen beim Ziehen, volle 220 ms. Ohne sie wäre nicht erkennbar, wohin ein Ziel fällt.
- M-13 Der streamende Text — das ist **Inhalt**, keine Animation.

**Wird abgeschaltet:**
- Alle Dauerbewegungen (M-16, M-21) — vollständig.
- M-09 Puls, M-10 Verbindungsfarbe, M-22 gestaffeltes Einblenden.
- M-12 wird ein **statischer** Graustand statt eines Pulsierens — die Aussage „gilt gerade
  nicht" bleibt also erhalten, nur ohne Bewegung.
- M-11 Schimmer wird eine ruhige Fläche.

**Wird verkürzt auf 0 ms:** alle Bildschirmwechsel (§4), M-14, M-19, M-20, M-24 — die Zustände
wechseln unmittelbar.

### 9. Offene Fragen

Keine. Frank hat den vertiefenden Bewegungs-Durchgang zugunsten des Spec-Baus abgekürzt; alle
hier genannten Werte sind begründete Vorgaben an den Designer und werden von seiner Messung
überholt, sobald der Entwurf zurück ist.

---

## Teil D — Rahmen und Abnahme

### 2. Zielplattform(en)

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

### 3. Rahmenbedingungen

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

### 4. Ausdrücklich NICHT enthalten

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

### 5. Abnahme — wann ist es fertig

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

### 6. Offene Fragen

| Nr | Frage | Warum noch offen |
|---|---|---|
| O-01 | Wie genau soll der Auftragstext an Codex formuliert sein (Tonfall, Ausführlichkeit der Begründungen, ob Quellen genannt werden sollen)? | Frank hat den Schlussdurchgang zugunsten des Spec-Baus abgekürzt. Wird beim Bau nach bestem Wissen formuliert und ist danach in den Einstellungen nachjustierbar |
| O-02 | Ob die drei alternierenden Zyklen (Citicolin ↔ Uridin + Phosphatidylserin) als **Dreier**-Gruppe abgebildet werden müssen — das Feld „alterniert mit" trägt bisher nur Paare | Der Fall kommt im Startbestand genau einmal vor. Vorschlag für den Bau: das Feld nimmt mehrere Partner auf |
| O-03 | Ob die 🟡-Markierung „mittleres Durchfallrisiko" in der Auswertung eine eigene Rolle spielen soll (z. B. „zu viele risikobehaftete Mittel in einem Stack") oder nur informativ ist | Nicht gefragt worden. Wird zunächst nur als Feld geführt und der KI mitgeteilt |
| O-04 | Ob Frank Ziele auch **löschen** können soll, die noch in Stacks verwendet werden — und was dann mit deren Bewertungen geschieht | Vorschlag für den Bau: Warnung mit Nennung der betroffenen Stacks, danach Löschen samt zugehöriger Bewertungszellen |

---

## Anhang — Startbestand (echte Datenmengen)

> Der Designer soll mit den **echten** Zahlen entwerfen: 6 Stacks, 72 Einträge,
> längster Stack 19 Zeilen, längster Mittelname „PEA (Palmitoylethanolamid)".

Quelle: `C:\Users\barwa\Meine Ablage\Dokumente\KI\Backup\Stack.docx` (deckungsgleich mit
`~/proggs/NEMS/app/src/main/java/com/nems/app/data/local/SeedDataProvider.kt`, aber mit mehr
Angaben als dort übernommen wurden).

**6 Stacks, 72 Einträge, 63 verschiedene Mittel.** Der Bestand liegt als `startbestand.json` in
den Assets der App und wird über F-21 eingelesen — er ist **nicht einkompiliert**, damit er sich
aktualisieren lässt, ohne die App neu zu bauen.

Regel aus der Quelle, die für jeden Eintrag gilt:
> *„Alle Mengenangaben beziehen sich immer auf 1 Kapsel; wenn in Klammern 2 Kapseln steht, nimmt
> Frank die doppelte Menge der Mengenangabe — beachte dies konsequent bei jedem gelisteten NEM."*

Deshalb werden Stückzahl und Menge je Stück **getrennt** geführt (F-03, Datenmodell) und als
`2 × 80 mg = 160 mg` angezeigt.

Legende: 🟡 = mittleres Durchfallrisiko · 🟢 = Pulver · W = wasserlöslich · F = fettlöslich

### Die sechs Stacks

| id | Name | Zeitpunkt | Einnahme-Hinweis |
|---|---|---|---|
| `morning1` | Morgen-Stack Teil 1 | Direkt nach dem Aufstehen | nur mit Wasser |
| `morning2` | Morgen-Stack Teil 2 | 60 Minuten nach dem Aufstehen | mit Olivenöl und Wasser |
| `presport` | Pre-Sport-Stack | 45 Minuten vor dem Sport (später im Laufe des Tages) | mit Olivenöl und Wasser |
| `evening1` | Abend-Stack Teil 1 | 2 Stunden vor dem Schlafen | mit Wasser |
| `evening2` | Abend-Stack Teil 2 | 60 Minuten vor dem Schlafen | mit 1 EL Olivenöl und Wasser |
| `evening3` | Abend-Stack Teil 3 | Direkt vor dem Schlafen | mit Wasser |

Die Reihenfolge innerhalb jedes Stacks ist die **Einnahme-Reihenfolge** — die Quelle sagt
ausdrücklich „nimmt er … in dieser Reihenfolge ein". Sie wird als `reihenfolge` gespeichert
(F-07); die Löslichkeits-Ansicht (F-06) ist nur eine andere Anzeige derselben Daten.

### morning1 — Morgen-Stack Teil 1

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | Vitamin C | 2 | 80 mg | Kapsel | W | täglich | |
| 2 | Eisen (Bisglycinat) | 1 | 14 mg | Kapsel | W | täglich | enthält zusätzlich 80 mg Vitamin C |
| 3 | L-Theanin | 1 | 500 mg | Kapsel | W | täglich | |
| 4 | Venlafaxin | 1 | 50 mg / **75 mg** | Tablette | W | täglich | 🟡 · **Dosis-Variante**: 50 mg im Frei, 75 mg im Dienst |
| 5 | Hyaluronsäure | 2 | 600 mg | Kapsel | W | täglich | |
| 6 | Vitamin-B Komplex | 1 | — | Kapsel | W | täglich | 🟡 · Greenfood „B100" |
| 7 | Bor | 1 | 3 mg | Kapsel | W | alle 5 Tage | |
| 8 | Selen | 1 | 200 µg | Kapsel | W | alle 3 Tage | |
| 9 | Löwenmähne | 2 | 650 mg | Kapsel | W | täglich | |
| 10 | Uridin Monophosphat | 1 | 300 mg | Kapsel | W | alle 2 Tage | alterniert mit Citicolin |
| 11 | Citicolin | 1 | 250 mg (davon 50 mg Cholin) | Kapsel | W | alle 2 Tage | alterniert mit Uridin + Phosphatidylserin |
| 12 | EAAs | 1 | 10 g | Löffel | W | täglich | 🟢 · **Kombi**: zusammen mit Kollagen |
| 13 | Kollagen | 1 | 10 g | Löffel | W | täglich | 🟢 · **Kombi**: zusammen mit EAAs |
| 14 | Kreatin | 1 | 3 g | Löffel | W | täglich | 🟡 🟢 |
| 15 | Acetyl-L-Carnitin (ALCAR) | 1 | 750 mg | Löffel | W | täglich | 🟡 🟢 |
| 16 | Kaffee (Koffein) | 1 | 1 Tasse | Tasse | W | täglich | |

**Alternierungs-Zyklus** (aus der Quelle wörtlich):
> Tag A: Citicolin 250 mg (ohne Uridin, ohne Phosphatidylserin)
> Tag B: Uridin 300 mg + Phosphatidylserin 150 mg (ohne Citicolin)
> Huperzin-Tag: Phosphatidylserin-Variante (kein Citicolin, kein Uridin)

### morning2 — Morgen-Stack Teil 2

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | L-Tyrosin | 1 | 650 mg | Kapsel | W | täglich | |
| 2 | Vitamin E Komplex | 1 | — | Kapsel | F | täglich | ZENement „Natural Vitamin E Complex" |
| 3 | Curcumin Phytosome | 1 | 500 mg | Kapsel | F | täglich | Thorne |
| 4 | DHEA | 1 | 50 mg | Kapsel | F | alle 7 Tage | |
| 5 | Vitamin D3+K2 | 1 | 5000 IE + 100 µg | Kapsel | F | täglich | |
| 6 | Apigenin | 1 | 200 mg | Kapsel | F | täglich | |
| 7 | Nicotinamid-Ribosid (NR) | 1 | 300 mg | Kapsel | W | täglich | 🟡 |
| 8 | CoQ10 (Ubiquinol) | 1 | 200 mg | Kapsel | F | täglich | |
| 9 | Astaxanthin | 1 | 12 mg | Kapsel | F | täglich | |
| 10 | Ashwagandha KSM-66 | 2 | 600 mg | Kapsel | F | täglich | 🟡 |
| 11 | Magnesium (Bisglycinat) | 1 | 155 mg | Kapsel | W | täglich | 🟡 |
| 12 | Magnesium (L-Threonat) | 1 | 48 mg | Kapsel | W | täglich | 🟡 |
| 13 | Kupfer (Bisglycinat) | 1 | 2 mg | Kapsel | W | täglich | |
| 14 | Mangan (Bisglycinat) | 1 | 10 mg | Kapsel | W | alle 6 Tage | |
| 15 | Phosphatidylserin (PS) | 1 | 150 mg | Kapsel | F | alle 2 Tage | Teil des Alternierungs-Zyklus |
| 16 | Huperzin A | 1 | 200 µg | Kapsel | F | alle 4 Tage | |
| 17 | Ginkgo Biloba | 1 | — | Kapsel | F | alle 4 Tage | |
| 18 | TMG (Trimethylglycin) | 1 | 1 g | Löffel | W | täglich | 🟢 |
| 19 | MSM | 1 | 1 g | Löffel | W | täglich | 🟢 |

### presport — Pre-Sport-Stack

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | PQQ | 1 | 20 mg | Kapsel | W | täglich | |
| 2 | R-Alpha Liponsäure | 1 | 300 mg | Kapsel | W | täglich | 🟡 |
| 3 | Fucoxanthin | 1 | 50 mg | Kapsel | F | täglich | |
| 4 | Rhodiola Rosea Extrakt | 1 | 500 mg | Kapsel | W | täglich | 🟡 |
| 5 | Cordyceps Sinensis Extrakt | 1 | 700 mg | Kapsel | W | täglich | |
| 6 | AAKG | 1 | 6 g | Löffel | W | täglich | 🟢 · Verhältnis 4,2 : 1,8 (4,2 g Arginin + 1,8 g AKG) |
| 7 | Acetyl-L-Carnitin (ALCAR) | 1 | 750 mg | Löffel | W | täglich | 🟡 🟢 |
| 8 | Whey-Protein | 1 | 25 g | Löffel | W | täglich | 🟢 · **Kombi** mit Kollagen und Vitamin C |
| 9 | Kollagen | 1 | 10 g | Löffel | W | täglich | 🟢 · **Kombi** |
| 10 | Vitamin C | 1 | 80 mg | Kapsel | W | täglich | **Kombi** |

### evening1 — Abend-Stack Teil 1

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | L-Theanin | 1 | 500 mg | Kapsel | W | täglich | |
| 2 | GABA | 1 | 500 mg | Kapsel | W | täglich | |
| 3 | Melatonin | 1 | 1 mg | Kapsel | W | täglich | |
| 4 | Zink (Bisglycinat) | 1 | 25 mg | Kapsel | W | täglich | |
| 5 | Magnesium (Bisglycinat) | 2 | 155 mg | Kapsel | W | täglich | 🟡 |
| 6 | Magnesium (L-Threonat) | 1 | 48 mg | Kapsel | W | täglich | 🟡 |
| 7 | Glycin | 2 | 1 g | Kapsel | W | täglich | |
| 8 | MSM | 1 | 1 g | Löffel | W | täglich | 🟢 |

### evening2 — Abend-Stack Teil 2

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | Astragalus-Extrakt | 1 | 600 mg (20:1) | Kapsel | W | alle 4 Tage | |
| 2 | PEA (Palmitoylethanolamid) | 2 | 300 mg | Kapsel | F | täglich | |
| 3 | Pterostilben | 1 | 100 mg | Kapsel | F | täglich | **alterniert mit Trans-Resveratrol** |
| 4 | Trans-Resveratrol | 1 | 500 mg | Kapsel | F | täglich | **alterniert mit Pterostilben** |
| 5 | Liposomales Luteolin | 1 | 250 mg | Kapsel | F | täglich | |
| 6 | Curcumin Phytosome | 1 | 500 mg | Kapsel | F | täglich | 🟡 · Thorne |
| 7 | Spermidin | 1 | 6 mg | Kapsel | W | täglich | |
| 8 | Urolithin A | 1 | 500 mg | Kapsel | F | alle 3 Tage | |
| 9 | Weihrauch Extrakt | 1 | 500 mg (davon 425 mg Boswelliasäure) | Kapsel | F | täglich | |
| 10 | Grüntee-Extrakt | 1 | 700 mg | Kapsel | W | täglich | entkoffeiniert · **alterniert mit Brokkoli-Extrakt** |
| 11 | Brokkoli-Extrakt | 1 | 1000 mg | Kapsel | F | täglich | **alterniert mit Grüntee-Extrakt** |
| 12 | Löwenmähne | 2 | 650 mg | Kapsel | W | täglich | |
| 13 | Gotu Kola Extrakt | 1 | 435 mg | Kapsel | F | täglich | |
| 14 | Bacopa Monnieri Extrakt | 1 | 500 mg | Kapsel | F | täglich | |
| 15 | Ashwagandha KSM-66 | 2 | 600 mg | Kapsel | F | täglich | 🟡 |
| 16 | Omega 3 | 3 | 1 g | Kapsel | F | täglich | 🟡 |
| 17 | NAC | 1 | 800 mg | Kapsel | W | täglich | 🟡 |
| 18 | Glycin | 2 | 1 g | Kapsel | W | täglich | |

### evening3 — Abend-Stack Teil 3

| # | Mittel | Stück | Menge je Stück | Form | Lösl. | Frequenz | Besonderheit |
|---|---|---|---|---|---|---|---|
| 1 | Micellar Casein | 1 | 25 g in Wasser | Löffel | W | täglich | 🟢 |

### Mittel, die in mehreren Stacks vorkommen

Diese Zusammenstellung ist der Grund für den **Mittel-Katalog** (F-30) und für „Alle Stacks
zusammen prüfen" (F-13). Ohne stabile Kennung je Mittel wären diese Summen nicht berechenbar.

| Mittel | Stacks | Tagesgesamtmenge |
|---|---|---|
| Magnesium (Bisglycinat) | morning2 (1×155 mg), evening1 (2×155 mg) | **465 mg** |
| Magnesium (L-Threonat) | morning2, evening1 | 96 mg |
| Ashwagandha KSM-66 | morning2 (2×600 mg), evening2 (2×600 mg) | **2400 mg** |
| Curcumin Phytosome | morning2, evening2 | 1000 mg |
| Löwenmähne | morning1 (2×650 mg), evening2 (2×650 mg) | 2600 mg |
| Acetyl-L-Carnitin (ALCAR) | morning1, presport | 1500 mg |
| Vitamin C | morning1 (2×80 mg), presport (1×80 mg), + 80 mg im Eisen-Bisglycinat | 320 mg |
| Kollagen | morning1, presport | 20 g |
| Glycin | evening1 (2×1 g), evening2 (2×1 g) | 4 g |
| MSM | morning2, evening1 | 2 g |
| L-Theanin | morning1, evening1 | 1000 mg |

### Bekannte Gegenspieler im Bestand

Nur als Hinweis für die erste Auswertung — die Bewertung leistet Codex, nicht dieses Dokument:
- **Kaffee und Eisen** stehen beide in `morning1` (Positionen 2 und 16).
- **Zink** (evening1) und **Kupfer** (morning2) sind Gegenspieler an derselben Aufnahme.
- Drei ausdrücklich **alternierende** Paare bzw. Zyklen, die **nie** als Konkurrenz gemeldet
  werden dürfen: Pterostilben ⇄ Trans-Resveratrol · Grüntee-Extrakt ⇄ Brokkoli-Extrakt ·
  Citicolin ↔ Uridin + Phosphatidylserin.

---

## Z. Was ausdrücklich nicht ins Design gehört

- Kein Erststart-Ablauf, keine Einführung, keine Tour. Die App ist ausschließlich für ihren
  einen Benutzer; er kennt sie.
- Keine Datenschutz-, Zustimmungs- oder Rechtstexte.
- Keine Anmeldung außer der Codex-Geräteanmeldung (B-11).
- Keine Fotos und keine Illustrationen. Die einzigen Bildelemente sind Ampeln,
  Löslichkeitspunkte und Symbole (Material Symbols Rounded).
- Kein Kalender und keine Einnahme-Historie. StackLabor ist ein Komponier-Werkzeug, kein
  Tracker — das leistet eine andere App.
- Keine Werbe-, Abo- oder Bezahlflächen.
