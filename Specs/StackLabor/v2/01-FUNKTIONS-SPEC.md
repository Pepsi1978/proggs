# Funktions-Spec — StackLabor
Stand: 14.08.2026, 14:25 · Stufe: v2 · Plattform(en): Android

> Übernommen aus `v1`, ergänzt um das, was der Entwurf hinzugefügt hat. Beim **Verhalten**
> gewinnt dieses Dokument; bei **Aussehen und Bewegung** gewinnt die Messung in
> `Specs/StackLabor/v2/messung/`. Nichts aus v1 wurde gestrichen — der Designer hat keine
> Funktion weggelassen.

## 1. Überblick der Funktionen

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
| F-31 | **NEU** — Kontextmenü am Mittel (langes Drücken in der Ansicht „Löslichkeit") | B-02 | Kern |
| F-25 | Mittel in einen anderen Stack verschieben oder kopieren | B-02, B-13 | später |
| F-26 | Stack duplizieren | B-01 | später |

## 2. Funktionen im Einzelnen

### F-01 — Stack anlegen, bearbeiten, löschen

- **Auslöser** — Plus auf B-01 (anlegen) · Überlaufmenü im Kopf von B-02 oder langes Drücken einer Karte auf B-01 (bearbeiten/löschen)
- **Ablauf** — 1. Blatt B-13 öffnet. 2. Felder: Name (Pflicht), Zeitpunkt-Beschreibung (z. B. „60 Minuten nach dem Aufstehen"), Einnahme-Hinweis (z. B. „mit 1 EL Olivenöl und Wasser"). 3. Sichern schließt das Blatt. 4. Beim Löschen: Rückfrage mit Nennung der Anzahl enthaltener Mittel.
- **Daten** — schreibt `Stack`. Beim Löschen werden die `StackEintrag`-Zeilen, die Zielzuordnungen, die eigenen Fragen und alle Bewertungen dieses Stacks mitgelöscht. Die Mittel selbst bleiben im Katalog (F-30).
- **Ergebnis** — Der Stack erscheint als Karte auf B-01, einsortiert nach `sortierung`.
- **Fehlerfall** — Leerer Name: Sichern bleibt gesperrt, das Feld wird rot umrandet. Doppelter Name: erlaubt, aber Hinweiszeile „Es gibt bereits einen Stack mit diesem Namen".
- **Regeln/Grenzen** — Name max 60 Zeichen. Zeitpunkt und Einnahme-Hinweis je max 120 Zeichen. Beide gehen in die KI-Anfrage ein (F-12) und beeinflussen die Bewertung.

### F-02 — Mittel zu einem Stack hinzufügen

- **Auslöser** — Plus in der NEM-Liste auf B-02.
- **Ablauf** — 1. B-14 öffnet mit Suchfeld über den Katalog. 2. Frank wählt ein vorhandenes Mittel oder legt über „Neu anlegen" eines an (führt zu B-05). 3. Er trägt die **Stack-Dosis** ein (Stückzahl × Menge je Stück, Einheit) — die Stammdaten kommen aus dem Katalog. 4. Der Eintrag steht **sofort** in der Liste, ohne Wartezeit. 5. Ab der letzten Eingabe läuft ein Ruhefenster von **3 Sekunden**; danach startet **eine** Konkurrenzprüfung für **alle** in diesem Fenster hinzugefügten Mittel. 6. Das Ergebnis erscheint als Schnipsel über dem Auswerten-Sockel: je neuem Mittel eine Zeile („stützt Ziel 1, 2 · stört Ziel 7: …"), dazu die Knöpfe **Behalten** und **Doch entfernen**.
- **Daten** — schreibt `StackEintrag`. Das Prüfergebnis wird als `offenerHinweis` am `StackEintrag` gespeichert, nicht nur angezeigt.
- **Ergebnis** — Das Mittel ist im Stack, aktiv (Häkchen gesetzt), und die Bewertung des Stacks ist als **veraltet** markiert (F-23).
- **Fehlerfall** — Kein Netz oder Codex nicht angemeldet: Das Mittel wird trotzdem aufgenommen; statt des Hinweises erscheint „Konkurrenzprüfung nicht möglich — beim nächsten Auswerten". Läuft bereits eine Prüfung, wird sie abgebrochen und mit der erweiterten Menge neu gestartet. Verlässt Frank B-02 vor dem Ergebnis, geht der Hinweis **nicht** verloren: Die Stack-Karte auf B-01 trägt einen Punkt, und beim nächsten Öffnen von B-02 erscheint das Schnipsel erneut.
- **Regeln/Grenzen** — Ein Mittel darf in **einem** Stack nur einmal vorkommen (in mehreren Stacks dagegen beliebig oft). Der Versuch, es erneut hinzuzufügen, springt zum vorhandenen Eintrag und hebt ihn kurz hervor.

### F-03 — Mittel bearbeiten

- **Auslöser** — Tippen auf einen Eintrag in der NEM-Liste (B-02) oder auf ein Mittel im Katalog (B-14).
- **Ablauf** — Blatt B-05 öffnet mit zwei erkennbar getrennten Bereichen: **Stammdaten** (gelten überall) und **In diesem Stack** (gilt nur hier). Änderungen an Stammdaten werden mit dem Hinweis „gilt in N Stacks" versehen.
- **Daten** — Stammdaten schreiben `Mittel`; Dosis und Zusatzangaben schreiben `StackEintrag`.
- **Ergebnis** — Der Eintrag zeigt die neuen Werte; je nach geändertem Feld wird die Bewertung veraltet (siehe F-23).
- **Fehlerfall** — Menge ≤ 0 oder leer: Sichern gesperrt. Einheit ohne Menge: Sichern gesperrt.
- **Regeln/Grenzen** — Felder siehe §3 (Datenmodell). Der Zusatztext ist auf 300 Zeichen begrenzt und geht wörtlich in die KI-Anfrage ein.

### F-04 — Mittel aus dem Stack entfernen

- **Auslöser** — Wischen des Eintrags nach links auf B-02.
- **Ablauf** — 1. Der Eintrag gleitet weg. 2. Eine Rückmeldung („Entfernt — Rückgängig") steht 6 Sekunden. 3. Danach ist er weg.
- **Daten** — löscht `StackEintrag` und die zugehörigen Bewertungszellen dieses Stacks. Das Mittel bleibt im Katalog.
- **Ergebnis** — Die Liste schließt die Lücke, Ampeln rechnen neu (F-14), Bewertung wird veraltet.
- **Fehlerfall** — Keiner. „Rückgängig" stellt den Eintrag samt Dosis und Position wieder her.
- **Regeln/Grenzen** — Gehört das Mittel zu einer Kombi-Gruppe (F-28), fragt die App, ob nur dieses Mittel oder die ganze Gruppe entfernt wird.

### F-05 — Mittel aktivieren/deaktivieren (das Häkchen)

- **Auslöser** — Tippen auf das Häkchen-Kästchen eines Eintrags.
- **Ablauf** — 1. Das Kästchen wechselt seinen Zustand (leichte Haptik). 2. Der Eintrag wird ausgegraut bzw. wieder normal. 3. **Sofort** danach rechnet F-14 alle Ampeln neu — die Ziel-Ampeln überblenden gestaffelt von oben, die tatsächlich geänderten pulsen einmal auf. 4. Die Auswertungs-Karte ergänzt eine Zeile: „Ohne <Mittel>: Ziel 4 wird grün, Ziel 7 wird rot."
- **Daten** — schreibt `StackEintrag.aktiv`. **Dauerhaft gespeichert** — der Zustand überlebt das Schließen der App.
- **Ergebnis** — Das Mittel ist im Stack abgeschaltet, bleibt aber sichtbar. Es geht **nicht** in die nächste KI-Anfrage ein.
- **Fehlerfall** — Keiner. Funktioniert vollständig offline.
- **Regeln/Grenzen** — Das Häkchen macht die Bewertung **nicht** veraltet (F-23) — die Bewertungstabelle enthält die Beiträge aller Mittel und bleibt gültig. An einer Kombi-Gruppe schaltet das Gruppen-Häkchen alle Mitglieder; einzelne Mitglieder bleiben separat schaltbar, das Gruppen-Häkchen zeigt dann einen Teilzustand.

### F-06 — Sortierung umschalten

- **Auslöser** — Zwei Chips in der Leiste über der Liste: „Löslichkeit" (Vorgabe) und „Einnahme".
- **Ablauf** — Die Liste ordnet sich um. In „Löslichkeit": erst alle wasserlöslichen, dann alle mit beidem, dann alle fettlöslichen; innerhalb jeder Gruppe die Einnahme-Reihenfolge. In „Einnahme": die gespeicherte Reihenfolge.
- **Daten** — schreibt nur `Einstellung.sortieransicht` (je Stack gemerkt). Die gespeicherte Reihenfolge wird **nie** verändert.
- **Ergebnis** — Andere Anzeige, gleiche Daten.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Kombi-Gruppen (F-28) bleiben in **beiden** Ansichten zusammen; die Gruppe wird nach der Löslichkeit ihres ersten Mitglieds einsortiert.

### F-07 — Einnahme-Reihenfolge ändern

- **Auslöser** — Langes Drücken (300 ms) auf einen Eintrag — **nur** in der Ansicht „Einnahme".
- **Ablauf** — Aufnehmen, ziehen, ausweichen, loslassen (siehe `03-MOTION-SPEC.md`, M-18).
- **Daten** — schreibt `StackEintrag.reihenfolge` für alle betroffenen Zeilen.
- **Ergebnis** — Neue Einnahme-Reihenfolge, bleibt nach Neustart erhalten.
- **Fehlerfall** — Ziehen über den Listenrand rollt automatisch weiter; Abbruch fliegt zur Ausgangsposition zurück.
- **Regeln/Grenzen** — In der Ansicht „Löslichkeit" nimmt langes Drücken **nicht** auf, sondern öffnet ein Kontextmenü (Bearbeiten / Verschieben / Entfernen) und zeigt einmalig den Hinweis „Reihenfolge lässt sich nur in der Ansicht Einnahme ändern". Eine Kombi-Gruppe wird als Ganzes gezogen.

### F-08 — Ziel anlegen, umbenennen, löschen

- **Auslöser** — Plus auf B-03 (Ziel-Katalog), erreichbar über das Zielscheiben-Symbol auf B-01.
- **Ablauf** — 1. Eingabefeld für den Zieltext. 2. Sichern legt das Ziel global an. 3. Die Liste zeigt zu jedem Ziel „in N Stacks verwendet".
- **Daten** — schreibt `Ziel`.
- **Ergebnis** — Das Ziel steht zur Auswahl in jedem Stack (F-09), ist aber zunächst nirgends aktiv.
- **Fehlerfall** — Leerer Text: Sichern gesperrt. Beim Löschen eines verwendeten Ziels: Warnung mit Nennung der betroffenen Stacks; nach Bestätigung werden auch dessen Bewertungszellen gelöscht.
- **Regeln/Grenzen** — Zieltext max 200 Zeichen, mehrzeilig erlaubt. **Keine Obergrenze für die Anzahl der Ziele** — 30 und mehr sind ausdrücklich vorgesehen.

### F-09 — Ziele für diesen Stack an-/abwählen

- **Auslöser** — Tippen auf den Ziel-Streifen in B-02, dann Knopf „Ziele wählen" — oder direkt im Blatt B-04.
- **Ablauf** — 1. B-04 zeigt alle Ziele des Katalogs mit Kästchen. 2. Angehakte Ziele gelten in diesem Stack. 3. „Fertig" schließt das Blatt.
- **Daten** — schreibt `StackZiel` (Zuordnung Stack ↔ Ziel mit `rang`). Ein neu angehaktes Ziel bekommt den letzten Rang.
- **Ergebnis** — Das Ziel erscheint in der Ziel-Liste dieses Stacks und geht in die nächste Auswertung ein.
- **Fehlerfall** — Ist der Katalog leer, zeigt B-04 „Erst Ziele anlegen" mit Sprung zu B-03.
- **Regeln/Grenzen** — Das An- oder Abwählen macht die Bewertung **veraltet** (F-23), weil die KI dieses Ziel noch nie bewertet hat.

### F-10 — Ziele priorisieren

- **Auslöser** — Knopf „Ordnen" im Ziel-Blatt B-04, öffnet das Vollbild B-12.
- **Ablauf** — 1. B-12 zeigt alle Ziele dieses Stacks, nummeriert 1, 2, 3 … 2. Langes Drücken nimmt ein Ziel auf. 3. Beim Ziehen weichen die anderen aus, und **die Nummern laufen live mit**. 4. Am Rand rollt die Liste automatisch weiter. 5. Loslassen rastet ein.
- **Daten** — schreibt `StackZiel.rang` für alle betroffenen Zeilen dieses Stacks. Die Ränge anderer Stacks bleiben unberührt.
- **Ergebnis** — Neue Prioritätsreihenfolge. **Alle Ampeln rechnen sofort neu** (F-14), weil das Zielgewicht in die Rechnung eingeht — ohne KI-Abfrage.
- **Fehlerfall** — Abbruch fliegt zurück.
- **Regeln/Grenzen** — Der Rang ist **je Stack** eigen: Dasselbe Ziel kann im Morgen-Stack Rang 1 und im Abend-Stack Rang 9 haben. Das Priorisieren macht die Bewertung **nicht** veraltet.

### F-11 — Eigene KI-Fragen

- **Auslöser** — Knopf „Eigene Fragen" im Kopf von B-02, öffnet B-08.
- **Ablauf** — 1. B-08 listet die Fragen dieses Stacks. 2. Plus öffnet ein Eingabefeld. 3. Sichern legt die Frage an. 4. Wischen löscht sie.
- **Daten** — schreibt `EigeneFrage` (gehört zu genau einem Stack).
- **Ergebnis** — Die Frage geht bei **jeder** künftigen Auswertung dieses Stacks mit; ihre Antwort erscheint als eigener Abschnitt in der Auswertung.
- **Fehlerfall** — Leerer Text: Sichern gesperrt.
- **Regeln/Grenzen** — Fragetext max 300 Zeichen. **Keine Obergrenze für die Anzahl.** Anlegen, Ändern oder Löschen macht die Bewertung veraltet (F-23), weil die Antwort fehlt.

### F-12 — Diesen Stack auswerten (die Kernfunktion)

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

### F-13 — Alle Stacks zusammen prüfen

- **Auslöser** — Leiste „Alle Stacks zusammen prüfen" auf B-01.
- **Ablauf** — Wie F-12, aber über alle Stacks. Zusätzlich rechnet die App **vorher** die Tagesgesamtdosis je Mittel aus: Summe über alle Stacks, in denen das Mittel aktiv ist, unter Berücksichtigung der Frequenz. Diese Summen gehen mit in die Anfrage.
- **Daten** — schreibt `Bewertung` mit `bereich = TAG` und Prüfsumme über alle Stacks.
- **Ergebnis** — B-09 zeigt zwei Abschnitte: **Tagesgesamtdosis** (je Wirkstoff eine Zeile mit Summe; Mehrfachvorkommen sind zusammengefasst und die beteiligten Stacks genannt) und **Konkurrenzen über Stacks hinweg**. Der Fließtext ist über denselben Vollbild-Bildschirm B-07 lesbar und vorlesbar wie bei F-12.
- **Fehlerfall** — Wie F-12, mit demselben vollständigen Zustandssatz (läuft, veraltet, offline, nicht angemeldet, Fehler).
- **Regeln/Grenzen** — Die Zusammenfassung erfolgt über die `mittel_id` aus dem Katalog, **nicht** über den Namen — sonst wären Tippfehler zwei verschiedene Wirkstoffe. Unterschiedliche Dosen werden addiert (1 Kapsel morgens + 2 Kapseln abends = 3 Kapseln = 465 mg); unterschiedliche Darreichungsformen werden nebeneinander genannt statt addiert.

### F-14 — Ampeln lokal berechnen

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

### F-15 — Aufschlüsselung öffnen

- **Auslöser** — Tippen auf die Ampel eines Mittels (B-02) oder auf die Ampel eines Ziels (B-04/B-12).
- **Ablauf** — Blatt B-06 öffnet mit einem Richtungsparameter. Bei **Mittel → Ziele**: Kopf nennt das Mittel, darunter je Ziel eine Zeile (Nummer, Zieltext, Urteil *stützt/neutral/stört*, Begründung). Bei **Ziel → Mittel**: umgekehrt.
- **Daten** — liest `Bewertungszelle`.
- **Ergebnis** — Frank sieht die vollständige Begründung, die hinter der Ampel steht.
- **Fehlerfall** — Ohne Bewertung: „Dieser Stack wurde noch nicht ausgewertet" mit Knopf zum Auswerten. Bei veralteter Bewertung: Hinweiszeile im Kopf.
- **Regeln/Grenzen** — Neutrale Paare werden nur auf Wunsch eingeblendet („auch neutrale zeigen"), sonst wäre die Liste bei 30 Zielen unlesbar.

### F-16 — Auswertung vorlesen

- **Auslöser** — Vorlese-Knopf im Sockel von B-07.
- **Ablauf** — 1. Der Fließtext wird an den in B-10 gewählten Anbieter geschickt. 2. Die Wiedergabe beginnt. 3. Der gerade gesprochene **Absatz** wird hinterlegt; am Knopf zeigen drei Pegelbalken die Lautstärke. 4. Pause und Stopp sind jederzeit möglich.
- **Daten** — liest `Bewertung.gesamt`. Schreibt den Verbrauch in den Zähler (`TtsUsageStore` aus `EntropieReductor`).
- **Ergebnis** — Der Text wird gesprochen; verlässt Frank den Bildschirm, läuft die Wiedergabe über einen Vordergrunddienst weiter, mit Stopp-Knopf in der Benachrichtigung.
- **Fehlerfall** — Anbieter nicht erreichbar oder Schlüssel fehlt: Klartextmeldung mit dem Vorschlag, den Anbieter zu wechseln. Kein Netz: Der Knopf ist ausgegraut mit Hinweis.
- **Regeln/Grenzen** — Vorgelesen wird **nur der Fließtext**, absatzweise — nicht die Bewertungstabelle und nicht die Zahlenwerte. Wortgenaue Markierung ist nicht vorgesehen, weil die vorhandene Sprachkette sie nicht zuverlässig liefert.

### F-17 — Bei Codex anmelden / abmelden

- **Auslöser** — „Anmelden" in B-10, oder der Knopf aus einer `REAUTH`-Fehlerkarte.
- **Ablauf** — Geräte-Flow wie in `PerfectMoment`: 1. Die App holt einen Benutzercode. 2. B-11 zeigt den Code groß und die Verifizierungsadresse. 3. „Seite öffnen" startet den Browser. 4. Die App fragt im Takt nach, bis die Bestätigung vorliegt. 5. Zugangs- und Erneuerungsschlüssel werden verschlüsselt abgelegt.
- **Daten** — schreibt den Anmeldespeicher (`codex_oauth`): Zugangsschlüssel, Erneuerungsschlüssel, Ablaufzeitpunkt, Konto-Kennung, E-Mail.
- **Ergebnis** — B-10 zeigt das angemeldete Konto; die Auswerten-Knöpfe sind bereit.
- **Fehlerfall** — Code abgelaufen: neuer Code auf Knopfdruck. Verweigert: Klartext. Netzfehler: erneut versuchen.
- **Regeln/Grenzen** — Abmelden löscht alle Schlüssel, aber **keine** gespeicherten Bewertungen.

### F-18 — Einstellungen ändern

- **Auslöser** — Zahnrad auf B-01.
- **Ablauf** — B-10 mit vier Rubriken: **Vorlesen** (Anbieter, Stimme, Tempo, Pause zwischen Absätzen, automatische Abschaltung, Verbrauchsanzeige) · **Codex** (Konto, Modell, Denkstufe) · **Daten** (Export, Import, Startbestand einlesen) · **Darstellung** (Hell/Dunkel, Bewegung reduzieren).
- **Daten** — schreibt `Einstellung`.
- **Ergebnis** — Die Änderung gilt sofort.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Vorgabe: Anbieter **Microsoft Edge**, Stimme `de-DE-SeraphinaMultilingualNeural`, Modell `gpt-5.6-terra`, Denkstufe `high`. Ein Wechsel von Modell oder Denkstufe macht bestehende Bewertungen **nicht** ungültig.

### F-19 — Alles exportieren

- **Auslöser** — „Exportieren" in B-10.
- **Ablauf** — Die App schreibt eine Datei (JSON) mit Stacks, Katalog, Stack-Einträgen, Zielen, Zuordnungen samt Rängen, eigenen Fragen, Bewertungen samt Zellen und der Historie. Das System-Dateiblatt fragt nach dem Ablageort.
- **Daten** — liest alles. Die Datei trägt `schema_version` und einen Zeitstempel.
- **Ergebnis** — Eine Datei, die F-20 vollständig zurücklesen kann.
- **Fehlerfall** — Abbruch durch Frank: nichts geschieht. Schreibfehler: Klartextmeldung.
- **Regeln/Grenzen** — Keine Zugangsschlüssel in der Datei.

### F-20 — Aus Datei importieren

- **Auslöser** — „Importieren" in B-10.
- **Ablauf** — 1. Dateiauswahl. 2. Die App legt **still eine Sicherung** des jetzigen Standes an. 3. Rückfrage mit drei Möglichkeiten: **Ersetzen** (alles Vorhandene wird verworfen) · **Dazu** (neue Stacks kommen hinzu; gleiche `mittel_id` und gleicher Zieltext werden zusammengeführt) · **Abbrechen**. 4. Der gewählte Weg wird ausgeführt.
- **Daten** — schreibt je nach Wahl.
- **Ergebnis** — Der Bestand entspricht der Datei bzw. der Vereinigung.
- **Fehlerfall** — Datei unlesbar oder kein StackLabor-Format: Abbruch mit Klartext, nichts wird verändert. **Ältere** `schema_version`: automatische Überführung. **Neuere** als die App: Abbruch mit dem Hinweis, die App sei älter als die Datei.
- **Regeln/Grenzen** — Der Import ist nicht rückgängig zu machen; die stille Sicherung aus Schritt 2 ist die Rückfallebene und wird in B-10 als „Letzte Sicherung wiederherstellen" angeboten.

### F-21 — Startbestand einlesen

- **Auslöser** — Beim allerersten Start automatisch · später über „Startbestand einlesen" in B-10.
- **Ablauf** — Die App liest die mitgelieferte Datei (`startbestand.json` in den Assets) und legt daraus Katalog, Stacks und Stack-Einträge an. Beim späteren Aufruf erscheint vorher eine deutliche Warnung, dass vorhandene Stacks überschrieben werden.
- **Daten** — schreibt `Mittel`, `Stack`, `StackEintrag` **und (NEU in v2) `Ziel` samt `StackZiel`**. Eigene Fragen und Bewertungen bleiben unberührt.
- **Ergebnis** — Die 6 Stacks mit 72 Einträgen stehen bereit (siehe `../v1/STARTBESTAND.md`), **dazu die 12 Ziele mit ihrer Zuordnung und Reihenfolge je Stack** (siehe `00-PROJEKT.md` §6). Damit trägt die App vom ersten Start an Ampeln, sobald einmal ausgewertet wurde.
- **Fehlerfall** — Datei fehlt oder ist beschädigt: Klartextmeldung, die App startet leer und bleibt bedienbar.
- **Regeln/Grenzen** — Weil der Bestand als **Datei** und nicht einkompiliert vorliegt, lässt er sich aktualisieren, ohne die App neu zu bauen.

### F-22 — Hell/Dunkel umschalten

- **Auslöser** — Sonne/Mond-Symbol im Kopf von B-01.
- **Ablauf** — Alle Farbwerte überblenden in 420 ms. Offene Blätter bleiben offen und blenden mit.
- **Daten** — schreibt `Einstellung.erscheinung`.
- **Ergebnis** — Die gesamte App wechselt die Erscheinung. **Vorgabe ist Hell.**
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Beide Fassungen sind vollständig; es gibt keinen Bildschirm, der nur in einer gebaut ist.

### F-23 — „Veraltet"-Markierung

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

### F-24 — Suchen

- **Auslöser** — Lupe in der Leiste von B-02, Suchzeile in B-03 und B-14.
- **Ablauf** — Tippen filtert sofort auf Namensbestandteile, ohne Groß-/Kleinschreibung.
- **Daten** — liest nur.
- **Ergebnis** — Gefilterte Liste; die Trefferstelle im Namen wird hervorgehoben.
- **Fehlerfall** — Kein Treffer: „Nichts gefunden" mit Knopf „Neu anlegen".
- **Regeln/Grenzen** — In B-02 ist die Suche eingeklappt hinter der Lupe, um Höhe zu sparen. Während einer Suche ist das Ziehen (F-07) ausgesetzt.

### F-27 — Dosis-Variante umschalten

- **Auslöser** — Umschalter „Frei / Dienst" im Kopf von B-01.
- **Ablauf** — Die App wechselt für alle Mittel mit zwei hinterlegten Dosen auf die jeweils andere.
- **Daten** — liest `StackEintrag.dosisVarianteA/B`, schreibt `Einstellung.dosisVariante`.
- **Ergebnis** — Angezeigte Dosis und Tagesgesamtdosis (F-13) rechnen mit der gewählten Variante.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Mittel ohne zweite Dosis bleiben unverändert. Der Wechsel macht die Bewertung **veraltet** (F-23), weil sich Wirkstoffmengen ändern. Im Startbestand betrifft das nur Venlafaxin (50 mg / 75 mg).

### F-28 — Kombi-Gruppe „zusammen einnehmen"

- **Auslöser** — Mehrfachauswahl in der Liste (langes Drücken → „Gruppe bilden") oder Feld in B-05.
- **Ablauf** — Die gewählten Mittel bekommen dieselbe `gruppe_id`. In der Liste erscheinen sie mit einer 2 dp starken Klammerlinie links und einer Kopfzeile „zusammen einnehmen".
- **Daten** — schreibt `StackEintrag.gruppeId`.
- **Ergebnis** — Die Gruppe bleibt in **beiden** Sortieransichten zusammen; das Gruppen-Häkchen schaltet alle Mitglieder; Ziehen bewegt die ganze Gruppe.
- **Fehlerfall** — Keiner. Auflösen der Gruppe ist jederzeit möglich.
- **Regeln/Grenzen** — Eine Gruppe umfasst mindestens zwei Mittel. Im Startbestand betrifft das „EAAs + Kollagen" und „Whey-Protein + Kollagen + Vitamin C".

### F-29 — Auswertungs-Historie

- **Auslöser** — Tippen auf den Zeitstempel der Auswertungs-Karte, öffnet B-15.
- **Ablauf** — Die letzten fünf Läufe dieses Stacks stehen untereinander mit Zeitpunkt, Modell und einer Kurzbilanz („4 grün · 1 gelb · 0 rot"). Zwei Läufe lassen sich vergleichen; die Unterschiede werden je Ziel benannt („Ziel 4: gelb → grün").
- **Daten** — liest `Bewertung` (Historie).
- **Ergebnis** — Frank sieht, was seine Änderung bewirkt hat.
- **Fehlerfall** — Weniger als zwei Läufe: Vergleich ist ausgegraut.
- **Regeln/Grenzen** — Es werden je Stack **fünf** Läufe aufbewahrt; der älteste fällt heraus.

### F-30 — Katalog verwalten

- **Auslöser** — Katalog-Symbol auf B-01, öffnet B-14.
- **Ablauf** — Alle Mittel mit Stammdaten und der Angabe „in N Stacks". Anlegen, bearbeiten, löschen. Zwei Einträge lassen sich **zusammenführen** (falls doch einmal ein Dublett entstanden ist): Frank wählt den bleibenden, alle Stack-Einträge werden umgehängt.
- **Daten** — schreibt `Mittel`; beim Zusammenführen auch `StackEintrag.mittelId`.
- **Ergebnis** — Ein sauberer Bestand, auf dem die Tagesgesamtdosis (F-13) zuverlässig rechnet.
- **Fehlerfall** — Löschen eines verwendeten Mittels: Warnung mit Nennung der Stacks.
- **Regeln/Grenzen** — Das Zusammenführen behält die Dosis des jeweiligen Stack-Eintrags bei.

### F-31 — Kontextmenü am Mittel (NEU aus dem Entwurf)

- **Auslöser** — Langes Drücken (300 ms) auf einen Eintrag in der Ansicht **„Löslichkeit"**.
- **Ablauf** — 1. Ein Menü öffnet am Berührungspunkt mit drei Einträgen: **Bearbeiten** (→ B-05),
  **Gruppe bilden** (→ F-28), **Entfernen** (→ F-04 mit Rückgängig-Leiste). 2. Tippen daneben
  schließt es.
- **Daten** — schreibt nichts selbst; ruft F-03, F-28 oder F-04 auf.
- **Ergebnis** — Der gewählte Weg wird ausgeführt.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — In der Ansicht **„Einnahme"** öffnet langes Drücken **kein** Menü, sondern
  nimmt den Eintrag zum Ziehen auf (F-07). Damit gibt es in keiner der beiden Ansichten einen
  toten Zustand. Zusätzlich erscheint beim ersten Mal in der Löslichkeits-Ansicht die Kurzmeldung
  „Reihenfolge lässt sich nur in der Ansicht Einnahme ändern."
- **Herkunft** — Anmerkung 3 des Designers im Rücklauf.

## 3. Datenmodell

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

## 4. Zustände und Übergänge

**Bewertung eines Stacks:**
`nie ausgewertet` → (F-12) → `läuft` → `gültig` → (Inhaltsänderung, F-23) → `veraltet` → (F-12) → `gültig`
`läuft` → (Fehler) → `fehlgeschlagen` (der vorherige Stand bleibt sichtbar) → (F-12) → `läuft`

**Konkurrenzprüfung eines neu hinzugefügten Mittels (F-02):**
`neu` → `Ruhefenster 3 s` → `prüft` → `Hinweis offen` → (Behalten / Doch entfernen) → `erledigt`
`prüft` → (kein Netz) → `nicht möglich` (wird beim nächsten Auswerten mit erledigt)

**Vorlesen (F-16):** `bereit` → `spricht` → `pausiert` ⇄ `spricht` → `beendet`; im Hintergrund über einen Vordergrunddienst.

## 5. Externe Dienste

| Dienst | Wofür | Schlüssel/Anmeldung | Verhalten ohne Netz |
|---|---|---|---|
| **Codex** — `chatgpt.com/backend-api/codex/responses`; OAuth-Geräte-Flow über `auth.openai.com/api/accounts/deviceauth/usercode` → `…/deviceauth/token`, Erneuerung über `auth.openai.com/oauth/token` | F-12, F-13, F-02 | OAuth im Geräte-Flow, wie `PerfectMoment/auth/CodexAuthManager.kt`. Kein Schlüssel im Projekt | Auswerten-Knopf ausgegraut mit Hinweis; alle Ampeln, Häkchen und Bearbeitungen laufen weiter |
| **Microsoft Edge TTS** | F-16 (Vorgabe) | keiner | Vorlesen ausgegraut |
| **Google Cloud TTS (Chirp 3 HD)** | F-16 | Schlüssel aus `$HOME/SK/` wie im Vorbildprojekt | Vorlesen ausgegraut |
| **Qwen-Stimmklon** („Meine Stimme") | F-16 | wie im Vorbildprojekt | Vorlesen ausgegraut |

Verfügbare Modelle: `gpt-5.6-sol` (Sol), `gpt-5.6-terra` (Terra, **Vorgabe**), `gpt-5.6-luna` (Luna).
Denkstufen: `low`, `medium`, `high` (**Vorgabe**), `xhigh`, `max`.
Fehlerklassen: `REAUTH`, `QUOTA`, `NETWORK` — mit Kennzeichen, ob ein Wiederholversuch sinnvoll ist.

## 6. Hintergrund und Lebenszyklus

- Eine laufende **Auswertung** (F-12/F-13) läuft weiter, wenn die App in den Hintergrund geht, und wird beim Zurückkehren fertig angezeigt. Wird die App beendet, gilt der Lauf als abgebrochen; der vorherige Stand bleibt.
- Eine laufende **Konkurrenzprüfung** (F-02) schreibt ihr Ergebnis in die Datenbank und geht nie verloren.
- Das **Vorlesen** (F-16) läuft über einen Vordergrunddienst weiter, mit Stopp-Knopf in der Benachrichtigung.
- Alle Bearbeitungen werden **sofort** geschrieben; es gibt kein „Sichern" auf Bildschirmebene außer in den Blättern.
- Beim **Faltvorgang** wechselt das Layout, ohne Zustand zu verlieren: Scrollposition, geöffnete Blätter und laufende Vorgänge bleiben.

## 7. Offene Fragen

Siehe `00-PROJEKT.md` §6 (O-01 bis O-04). Über die dort genannten hinaus ist auf der Funktionsseite nichts offen.
