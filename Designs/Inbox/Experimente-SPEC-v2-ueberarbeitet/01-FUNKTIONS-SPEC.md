# Funktions-Spec — Experimente

Stand: 14.08.2026, 11.36 Uhr · Stufe: **v2, ueberarbeitet (Stand der gebauten App)** · Plattform(en): Android

> **Woher diese Fassung kommt.** Sie ist kein Entwurf, sondern die **Beschreibung der App, wie
> sie heute wirklich läuft**. Zwischen dem 12.08. und dem 14.08.2026 wurde die App in
> siebzehn Schritten weitergebaut — unter anderem vollständig aus dem Fold-Außendisplay-Entwurf
> neu aufgebaut. Alles, was dabei entstanden ist, steht jetzt hier drin: **`F-42` bis `F-58`**
> sind neu, dazu kommen Änderungen an achtzehn Bestandsfunktionen, drei neue Einheiten-Felder
> im Datenmodell und ein dritter Reiter im Logbuch. Die vollständige Gegenüberstellung steht in
> `AENDERUNGEN-v1-zu-v2.md`.

---

## 1. Überblick der Funktionen

| Kennung | Funktion | Bildschirm(e) | Stufe |
|---------|----------|---------------|-------|
| F-01 | Lage einsprechen | B-01 | Kern |
| F-02 | Text mit KI verbessern | B-01, B-04, B-05, B-09 | Kern |
| F-03 | Fünf Vorschläge erzeugen | B-01 | Kern |
| F-04 | Vorschläge aktualisieren | B-01 | Kern |
| F-05 | Vorschlag auf die Merkliste legen | B-01 | Kern |
| F-06 | Experiment auswählen und starten | B-01 | Kern |
| F-07 | To-Do-Liste des Tages | B-01 | Kern |
| F-08 | Aufgabe abhaken | B-01 | Kern |
| F-09 | Gespräch zum Experiment | B-02 | Kern |
| F-10 | Auswertung einsprechen | B-03 | Kern |
| F-11 | KI-Auswertung erzeugen | B-03 | Kern |
| F-12 | Auswertung vorlesen | B-03 | Kern |
| F-13 | Experiment abschließen | B-03 | Kern |
| F-14 | Logbuch fortschreiben | — (Hintergrund) | Kern |
| F-15 | Tagesverdichtung nach 15 Tagen | — (Hintergrund) | Kern |
| F-16 | Logbuch-Eintrag ändern oder löschen | B-07 | Kern |
| F-17 | Erkenntnisse fortschreiben | — (Hintergrund) | Kern |
| F-18 | Merkliste: eigenes Experiment anlegen | B-05 | Kern |
| F-19 | Merkliste: Eintrag löschen | B-05 | Kern |
| F-20 | Wünsche & Ziele pflegen | B-04 | Kern |
| F-21 | Selbstbild pflegen | B-09 | Kern |
| F-22 | Modell und Effort wählen | B-08 | Kern |
| F-23 | Stimme und Vorlesen einstellen | B-08 | Kern |
| F-24 | Zugänge einrichten | B-08 | Kern |
| F-25 | Erinnerungen einstellen | B-08 | Kern |
| F-26 | Erscheinung umschalten | B-08 | Kern |
| F-27 NEU | Zwischen den Hauptbildschirmen wischen | B-10, B-01, B-04, B-05, B-06, B-07 | Kern |
| F-28 | Lage tippen statt sprechen | B-01 | Kern |
| F-29 | Auswertungstext bearbeiten | B-03 | Kern |
| F-30 | Auswertung überspringen | B-03 | Kern |
| F-31 | Logbuch: Reiter *Letzte 15 Tage* | B-07 | Kern |
| F-32 | Logbuch: Reiter *Langzeit* | B-07 | Kern |
| F-33 | Anlegen abbrechen | B-05, B-04, B-10 | Kern |
| **F-34 NEU** | **Monitor: sehen, was ansteht und was läuft** | **B-10** | **Kern** |
| **F-35 NEU** | **Monitor: eigenes Experiment anlegen** | **B-10** | **Kern** |
| **F-36 NEU** | **Einen KI-Vorschlag in den Monitor übernehmen** | **B-01** | **Kern** |
| **F-37 NEU** | **Ein Experiment starten** | **B-10, B-01** | **Kern** |
| **F-38 NEU** | **Reihenfolge im Monitor ändern** | **B-10** | **Kern** |
| **F-39 NEU** | **Ein Experiment aus dem Monitor nehmen** | **B-10** | **Kern** |
| **F-40 NEU** | **Monitor-Karte auf- und zuklappen** | **B-10** | **Kern** |
| **F-41** | **Effekt-Stärke einstellen** | **B-08** | **Kern** |
| **F-42 NEU** | **Dauer eines Experiments nachträglich ändern** | **B-10** | **Kern** |
| **F-43 NEU** | **Dauer beim Anlegen selbst bestimmen** | **B-10, B-05** | **Kern** |
| **F-44 NEU** | **Ein Experiment weiterführen statt abzuschließen** | **B-03** | **Kern** |
| **F-45 NEU** | **Der Verlauf: jede Auswertung bleibt einzeln erhalten** | **B-03** | **Kern** |
| **F-46 NEU** | **Logbuch-Reiter *Auswertungen*** | **B-07** | **Kern** |
| **F-47 NEU** | **Vorlesen an jeder Stelle** | **B-02, B-03, B-06, B-07** | **Kern** |
| **F-48 NEU** | **Stimme des Geräts als vierter Weg und Rückfallebene** | **B-08** | **Kern** |
| **F-49 NEU** | **Zwischen Morgen und Abend umschalten** | **B-01** | **Kern** |
| **F-50 NEU** | **Mikrofon-Erlaubnis erfragen und die Handlung fortsetzen** | alle Sprechknöpfe | **Kern** |
| **F-51 NEU** | **Zurück — Stapel, Zurück-Taste und Wischgeste** | alle | **Kern** |
| **F-52 NEU** | **Selbstbild ausdrücklich sichern** | **B-09** | **Kern** |
| **F-53 NEU** | **Eigene Stimmen verwalten** | **B-08** | **Kern** |
| **F-54 NEU** | **Gerätecode anzeigen, kopieren, Anmeldung abbrechen** | **B-08** | **Kern** |
| **F-55 NEU** | **Weckzeit an der Uhr stellen** | **B-08** | **Kern** |
| **F-56 NEU** | **Nachlauf: Liegengebliebenes wird nachgeholt** | — (Hintergrund) | **Kern** |
| **F-57 NEU** | **Tageswechsel im laufenden Betrieb** | — (Hintergrund) | **Kern** |
| **F-58 NEU** | **Anlegefläche beiseitelegen, ohne zu verwerfen** | **B-10, B-04, B-05** | **Kern** |

F-01 bis F-26 gehören zur ersten Fassung. F-27 bis F-33 kamen aus dem Design dazu.
**F-34 bis F-41 sind neu in dieser Fassung** und tragen den Monitor und die Effekte.
**F-42 bis F-58 sind neu in dieser Fassung** — sie sind beim Weiterbauen der App entstanden
und beschreiben, was sie heute wirklich kann. Es gibt kein „später" — alles ist Kern.

**Achtzehn Bestandsfunktionen haben sich geändert.** Sie behalten ihre Kennung und tragen im
Einzelnen unten einen Kasten **„Geändert in v2"**: `F-01` · `F-03` · `F-06` · `F-08` · `F-09` ·
`F-10` · `F-11` · `F-12` · `F-13` · `F-16` · `F-18` · `F-19` · `F-20` · `F-21` · `F-23` · `F-24` ·
`F-25` · `F-26` · `F-27` · `F-33` · `F-35` · `F-39` · `F-41`.

---

## 2. Funktionen im Einzelnen

### F-01 — Lage einsprechen

> **Geändert in v2.** Vor der Aufnahme wird zuerst geprüft, ob der **Groq-Schlüssel** hinterlegt
> ist — fehlt er, erscheint sofort „Für die Spracherkennung fehlt der Groq-Schlüssel. Er steht in
> den Einstellungen." statt einer Aufnahme, die nachher nirgends hinkommt. Die Erlaubnis wird
> jetzt wirklich **angefragt** (`F-50`). Während der Aufnahme laufen die **Wellenform** (`E-18`)
> und eine **Sekundenanzeige** („00:07"). Ein neuer Zwischenzustand `AUFNAHME` steht zwischen
> `LEER` und `LAGE_STEHT`.

- **Auslöser:** Frank drückt auf B-01 den großen Sprechknopf unter der Frage
  „Wie ist deine Lage heute?".
- **Ablauf:**
  1. Berechtigung `RECORD_AUDIO` prüfen, bei Bedarf anfragen.
  2. Aufnahme starten (16 kHz Mono WAV). Kurze Vibration. Der Ring um den Knopf atmet (M-02).
  3. Frank spricht frei: was für ein Tag heute ist, was vor ihm liegt, wie es ihm geht,
     ob Tage frei sind. Kein Zeitlimit, keine Vorgaben.
  4. Erneuter Druck beendet die Aufnahme. Kurze Vibration.
  5. Vorfilter: Enthält die Aufnahme zu wenig gesprochene Zeit, wird sie gar nicht
     hochgeladen (Verfahren aus `SpeechAnalyzer.kt`).
  6. Transkription über Groq (`whisper-large-v3-turbo`, Sprache `de`, `temperature=0`,
     `response_format=verbose_json`), danach Halluzinationsfilter
     (Verfahren aus `WhisperHallucinationFilter.kt`).
  7. Der Text erscheint in einem bearbeitbaren Feld. Frank kann tippen, korrigieren
     oder F-02 auslösen.
  8. „Weiter" schickt den Text an F-03.
- **Daten:** Gelesen: nichts. Geschrieben: `SituationEntry` (Text, Zeitpunkt) — wird
  Teil des heutigen Logbuch-Eintrags (F-14).
- **Ergebnis:** Die heutige Lage steht als Text fest und ist die Grundlage der Vorschläge.
- **Fehlerfall:**
  - Berechtigung abgelehnt → „Ohne Mikrofon kann ich dich nicht hören." mit Verweis in die
    Systemeinstellungen. Das Textfeld bleibt benutzbar.
  - Kein Netz → „Dafür brauche ich Netz." Die Aufnahme bleibt erhalten und kann später
    erneut gesendet werden.
  - Aufnahme leer oder nur Stille → „Da war nichts zu hören." Aufnahme verwerfen, Knopf
    bleibt bereit.
  - Groq antwortet nicht → Fehlertext mit Wiederholen-Knopf.
- **Regeln/Grenzen:** Aufnahme höchstens 25 MB (Groq-Grenze). Pro Tag genau **eine** Lage;
  eine erneute Eingabe am selben Tag ersetzt die vorige und erzeugt neue Vorschläge.

### F-02 — Text mit KI verbessern

- **Auslöser:** Knopf „Text mit KI verbessern" unter einem Textfeld (B-01 Lage, B-04 Ziel,
  B-05 eigenes Experiment, B-09 Selbstbild).
- **Ablauf:**
  1. Der aktuelle Text und alle bisherigen Fassungen dieses Feldes gehen an Codex
     (Verfahren aus `CodexAuthManager.improveWish()`).
  2. Die zurückkommende Fassung ersetzt den Feldinhalt; die vorige wird gemerkt.
  3. Der Knopf wird zu „Zurücknehmen". Ein Druck stellt die vorige Fassung wieder her und
     macht den Knopf wieder zu „Text mit KI verbessern".
  4. Erneutes Verbessern liefert eine **neue** Formulierung, keine Wiederholung — die
     bisherigen Fassungen werden mitgeschickt.
- **Daten:** Gelesen: Feldinhalt, bisherige Fassungen (nur im Speicher). Geschrieben: nichts
  bis zum Bestätigen des Feldes.
- **Ergebnis:** Der Text ist sprachlich klarer, der Inhalt unverändert.
- **Fehlerfall:** Kein Netz oder Dienst antwortet nicht → „Der Text konnte nicht verbessert
  werden." Der ursprüngliche Text bleibt unangetastet.
- **Regeln/Grenzen:** Verbessern ändert nie den Sinn. Kein Automatismus — nur auf Druck.

### F-03 — Fünf Vorschläge erzeugen

> **Geändert in v2.** Zwischen „Weiter" und den fünf Karten steht jetzt der eigene Zustand
> **`WARTET`** mit der Wartekarte „Ich sehe mir an, was ich über dich weiß …" (`M-09`, `E-14`).
> Die Zahl der laufenden Experimente wird **direkt aus der Ablage** gelesen, nicht aus dem
> beobachteten Strom — der ist beim Start noch leer und hätte drei Laufende übersehen.

- **Auslöser:** Nach F-01, oder beim Öffnen von B-01, wenn die Lage für heute bereits steht
  und noch keine Vorschläge vorliegen.
- **Ablauf:**
  1. Kontext zusammenstellen, in dieser Reihenfolge:
     **Selbstbild** (vollständig) · **Wünsche & Ziele** (vollständig) ·
     **Aktuelles Log** (letzte 15 Tage, ausführlich) · **Langzeit-Log** (vollständig,
     verdichtet) · **Erkenntnisse** (vollständig) · **laufende Experimente** ·
     **heutige Lage**.
  2. Anfrage an Codex mit dem für *Experimente* gewählten Modell und Effort (F-22).
  3. Die KI liefert genau fünf Vorschläge in dieser Zusammensetzung:
     - **zwei**, die zur heutigen Lage und zu Franks Geschichte passen
     - **zwei**, die völlig neu sind — aus Bereichen, mit denen er noch nichts zu tun hatte
     - **einen** von der Merkliste (F-05/F-18). Ist die Merkliste leer, wird dieser Platz
       ein weiterer neuer Vorschlag.
  4. Je Vorschlag: **Titel**, **Beschreibung** (was genau zu tun ist und warum es
     interessant sein könnte), **Dauer in Tagen** (1 oder mehr), **Stufe**
     (leicht · mittel · fordernd), **Aufgabenliste je Tag**.
  5. Die fünf Karten erscheinen gestaffelt (M-04).
- **Daten:** Gelesen: alle Speicher (siehe oben). Geschrieben: `Suggestion`-Sätze für heute.
- **Ergebnis:** Fünf wählbare Vorschläge auf B-01.
- **Fehlerfall:**
  - Kein Netz → „Dafür brauche ich Netz." mit Wiederholen-Knopf.
  - Codex-Anmeldung abgelaufen → „Deine Anmeldung ist abgelaufen." mit Verweis in die
    Einstellungen (B-08).
  - Kontingent aufgebraucht → „Dein Kontingent ist erschöpft." mit Hinweis auf einen
    späteren Versuch.
  - Antwort unvollständig oder unlesbar → einmal automatisch wiederholen, danach
    „Die Antwort war unbrauchbar." mit Wiederholen-Knopf.
- **Regeln/Grenzen:**
  - Es gibt **keine feste Bereichseinteilung**. Die KI zieht frei aus allem, was zu einem
    Menschenleben gehört (Körper, Geld, Ernährung, Umgang mit Menschen, Fähigkeiten,
    Lernen, Bewegung, Sinn, Alltag, Gewohnheiten, Kunst, Ordnung, Ruhe, Wagnis …).
  - Ein Vorschlag ist **immer etwas, das Frank so noch nie gemacht hat**. Das Langzeit-Log
    ist die Prüfliste dagegen.
  - **Die Stufe wird aus der Chronik dosiert:** anfangs leicht; mit jedem abgeschlossenen
    Experiment ein Stück mutiger; nach einem nicht umgesetzten Experiment wieder sanfter.
  - Sind bereits **drei Experimente am Laufen**, wird F-03 nicht ausgeführt (siehe F-37).
    Anstehende Experimente im Monitor zählen dabei **nicht** mit — sie sind unbegrenzt.
  - Nur der Merklisten-Vorschlag ist als solcher gekennzeichnet. Die vier anderen tragen
    **keine** Kennzeichnung „passend" oder „neu".

### F-04 — Vorschläge aktualisieren

- **Auslöser:** Knopf „Andere Vorschläge" unter den fünf Karten.
- **Ablauf:**
  1. Die fünf gezeigten Vorschläge gehen als „bereits gesehen und verworfen" in die Anfrage.
  2. F-03 läuft erneut, mit derselben Zusammensetzung (2 / 2 / 1), aber aus **anderen
     Bereichen** — nicht fünf Abwandlungen desselben Gedankens.
  3. Die alten fünf Karten gehen zuerst hinaus, dann kommen die neuen (M-05).
- **Daten:** Wie F-03, zusätzlich die Liste der verworfenen Titel des heutigen Tages.
- **Ergebnis:** Fünf andere Vorschläge.
- **Fehlerfall:** Wie F-03. Bei Fehlschlag bleiben die alten fünf stehen.
- **Regeln/Grenzen:** Beliebig oft wiederholbar. Alle an diesem Tag verworfenen Vorschläge
  bleiben bis Mitternacht ausgeschlossen.

### F-05 — Vorschlag auf die Merkliste legen

- **Auslöser:** Merken-Symbol auf einer Vorschlagskarte.
- **Ablauf:** Der Vorschlag wird vollständig (Titel, Beschreibung, Dauer, Stufe, Aufgaben)
  auf die Merkliste kopiert. Kurze Bestätigung. Die Karte bleibt wählbar.
- **Daten:** Geschrieben: `WatchlistItem`.
- **Ergebnis:** Der Vorschlag ist gesichert und kommt später als Merklisten-Platz zurück (F-03).
- **Fehlerfall:** Keiner — reine lokale Schreiboperation.
- **Regeln/Grenzen:** Ein bereits gemerkter Vorschlag lässt sich nicht doppelt merken; das
  Symbol zeigt dann den gemerkten Zustand.

### F-06 — Experiment auswählen und sofort starten

> **Geändert in v2.** Der Start löst zusätzlich das aufsteigende **Haptik-Muster** aus (`E-23`)
> und springt danach auf den Monitor, wo die Karte funkelt (`E-15`).

> **Geändert in dieser Fassung.** Eine Vorschlagskarte bietet jetzt **zwei** Wege:
> **„In den Monitor"** (F-36) legt sie zu den anstehenden Experimenten, ohne sie zu starten —
> das ist der übliche Weg. **„Jetzt starten"** ist der hier beschriebene Abkürzungsweg, der
> beide Schritte in einem ausführt.

- **Auslöser:** Frank drückt auf einer Vorschlagskarte **„Jetzt starten"** und bestätigt.
- **Ablauf:**
  1. Aus dem Vorschlag wird ein `Experiment` mit Startdatum, Dauer, Stufe, Herkunft und der
     Aufgabenliste je Tag — es entsteht also in einem Zug, was sonst F-36 und F-37 nacheinander
     tun.
  2. Der Zustand wird `LAEUFT`. Die übrigen vier Vorschläge **bleiben stehen** und lassen sich
     weiterhin in den Monitor übernehmen.
  3. Der Monitor (B-10) zeigt das Experiment fortan unter „Läuft", samt To-Do-Liste (F-07).
  4. Ist der Vorschlag von der Merkliste gekommen, wird er dort entfernt.
- **Daten:** Geschrieben: `Experiment` (`LAEUFT`), `Task`-Sätze. Gelesen: der gewählte
  `Suggestion`.
- **Ergebnis:** Ein laufendes Experiment, sichtbar im Monitor.
- **Fehlerfall:** Laufen bereits drei Experimente, ist **nur dieser Knopf** gesperrt und ein
  Hinweis erscheint: „Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues
  beginnst." **„In den Monitor" bleibt trotzdem benutzbar** — vormerken darf Frank immer.
- **Regeln/Grenzen:** **Höchstens drei gleichzeitig laufende Experimente.** Solange drei
  laufen, erzeugt B-01 keine neuen Vorschläge (F-03 wird nicht ausgeführt).

### F-07 — To-Do-Liste des Tages

- **Auslöser:** Beim Anzeigen von B-10, wenn mindestens ein Experiment läuft.
- **Ablauf:**
  1. Für jedes offene Experiment werden die Aufgaben **des heutigen Tages** ermittelt
     (bei mehrtägigen nur der Abschnitt dieses Tages).
  2. Es entsteht **eine einzige Liste** für den Tag, untereinander:
     Titel des Experiments 1 → seine heutigen Aufgaben → Titel des Experiments 2 → seine
     heutigen Aufgaben → und so weiter.
  3. Jede Aufgabe ist antippbar (F-08).
- **Daten:** Gelesen: `Experiment`, `Task` (gefiltert auf heute).
- **Ergebnis:** Frank sieht an einer Stelle, was heute zu tun ist — direkt unter den
  laufenden Experimenten im Monitor.
- **Fehlerfall:** Keiner — rein lokal.
- **Regeln/Grenzen:** **Eine Liste für den Tag, nicht eine Liste je Experiment.** Die
  Gruppierung erfolgt über die Zwischenüberschriften.

### F-08 — Aufgabe abhaken

> **Geändert in v2.** Jeder Haken gibt einen kurzen Stoß (10 ms, `E-23`). Die Liste steht im
> Monitor unter der Zwischenüberschrift **„Heute zu tun"**; auf `B-01` erscheinen dieselben
> Aufgaben abends auf der Abendkarte und sind auch dort abhakbar.

- **Auslöser:** Tippen auf eine Aufgabe der To-Do-Liste.
- **Ablauf:** Der Haken zeichnet sich (M-06), die Zeile wird gedämpft. Erneutes Tippen nimmt
  den Haken zurück.
- **Daten:** Geschrieben: `Task.doneAt`.
- **Ergebnis:** Der Stand ist gespeichert und übersteht einen Neustart.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Haken **zählen für die Auswertung** — F-11 bekommt den Stand
  („drei von fünf, der letzte blieb liegen") mitgeteilt und fragt gezielt danach.
  Franks eigene gesprochene Auswertung (F-10) bleibt davon unberührt und ist Pflicht.

### F-09 — Gespräch zum Experiment

> **Geändert in v2.** Drei Punkte:
> 1. **An jeder Runde steht ein Lautsprecher** (`F-47`) — bei eigenen Runden links neben der
>    Blase, bei Antworten rechts, damit die Blase in ihrer Flucht bleibt.
> 2. **Auswertungen stehen nicht mehr als Blase im Gespräch.** Sie liegen im selben Faden und
>    gehen weiterhin in jede Anfrage ein, haben auf dem Bildschirm aber ihren eigenen Ort
>    (`F-45`). Getrennt wird über das neue Feld `ChatTurn.art`.
> 3. Neben dem Mikrofon steht ein **Textfeld mit Sende-Knopf** — das Gespräch geht auch getippt.

- **Auslöser:** Gesprächs-Knopf an einer aufgeklappten Laufkarte im Monitor (B-10) → öffnet B-02.
- **Ablauf:**
  1. B-02 zeigt den bisherigen Gesprächsfaden zu **diesem** Experiment.
  2. Frank drückt den Sprechknopf, spricht, drückt erneut.
  3. Transkription wie F-01 (Schritte 5–6).
  4. Die Frage geht mit dem vollen Kontext (wie F-03) **plus** dem Experiment und dem
     bisherigen Faden an Codex.
  5. Die Antwort erscheint und wird **sofort vorgelesen** — ohne weiteren Druck, mit der in
     B-08 gewählten Stimme.
  6. Der nächste Druck auf den Sprechknopf setzt das Gespräch fort. Beliebig viele Runden.
- **Daten:** Gelesen: alle Speicher, `Experiment`, `ChatTurn`-Sätze. Geschrieben:
  `ChatTurn` (Rolle, Text, Zeitpunkt).
- **Ergebnis:** Ein Gesprächsfaden am Experiment. Er geht in F-14 ins Logbuch ein und ist
  später an der Chronik nachlesbar.
- **Fehlerfall:** Wie F-01/F-03. Schlägt das Vorlesen fehl, bleibt die Antwort trotzdem
  lesbar stehen; ein Lautsprecher-Knopf erlaubt einen neuen Versuch.
- **Regeln/Grenzen:** Der Faden gehört genau einem Experiment. Es gibt kein freies,
  themenloses Gespräch.

### F-10 — Auswertung einsprechen

> **Geändert in v2, grundlegend.** `B-03` arbeitet **ein Experiment** ab, nicht mehr alle
> offenen der Reihe nach: geöffnet wird es aus der Laufkarte im Monitor oder von der Abendkarte
> auf `B-01`. **Jede Aufnahme ist eine eigene Zeile** — es gibt keine „Auswertung des Tages"
> mehr, die eine zweite Aufnahme überschreiben könnte (`F-45`). Der eingesprochene Text wird
> **vor** dem Netzaufruf in den Gesprächsfaden geschrieben; fällt das Netz aus, ist er trotzdem
> da. Der Text bleibt beim Zurückkehren zum selben Experiment stehen.

- **Auslöser:** Abends im Monitor (B-10) der Knopf „Wie ist es gelaufen?" an einer Laufkarte
  oder die Abend-Erinnerung → öffnet B-03. Auch jederzeit manuell erreichbar.
- **Ablauf:**
  1. B-03 zeigt **alle offenen Experimente der Reihe nach**, jeweils mit Titel, Tag
     (z. B. „Tag 2 von 3") und dem heutigen Haken-Stand.
  2. Zu jedem spricht Frank ein, was war. Transkription wie F-01, mit bearbeitbarem
     Textfeld und F-02.
  3. Nach jedem Eintrag läuft F-11.
- **Daten:** Geschrieben: `Evaluation` (Experiment, Datum, Franks Text).
- **Ergebnis:** Zu jedem offenen Experiment liegt der heutige Stand in Franks eigenen Worten
  vor.
- **Fehlerfall:** Wie F-01.
- **Regeln/Grenzen:** Frank darf einzelne Experimente überspringen; sie bleiben dann offen
  und erscheinen am nächsten Abend erneut. Die eigene gesprochene Auswertung wird **nie**
  durch die Haken ersetzt.

### F-11 — KI-Auswertung erzeugen

> **Geändert in v2, grundlegend.** Die Auswertung **schließt das Experiment nicht mehr ab.**
> Vorher setzte sie am letzten Tag stillschweigend den Zustand `ABGESCHLOSSEN` — wer bei
> „Tag 2 von 2" erzählte, wie es gelaufen ist, hatte es damit beendet, auch wenn er es
> fortführen wollte. Erzählen ist nicht Beenden. Über das Ende entscheidet Frank, mit
> `F-44` (weiterführen), `F-13` (abschließen) oder „Zwischenstand" (offen lassen).
> Die Einschätzung geht an **genau die eben angelegte Zeile**, nicht an „die von heute".

- **Auslöser:** Direkt nach einem Eintrag aus F-10.
- **Ablauf:**
  1. Kontext: alle Speicher (wie F-03) · das Experiment · der Gesprächsfaden (F-09) ·
     der Haken-Stand (F-08) · Franks Auswertung (F-10).
  2. Anfrage an Codex mit dem für *Experimente* gewählten Modell und Effort.
  3. **Zwischentag eines mehrtägigen Experiments:** Es entsteht nur ein kurzer
     Zwischenstand (zwei bis drei Sätze). Keine volle Auswertung.
  4. **Letzter Tag oder eintägiges Experiment:** Es entsteht die **vollständige
     KI-Auswertung** — was sie über den Verlauf denkt, was Frank gemacht und was er nicht
     gemacht hat, wo er sich womöglich nicht getraut hat, was das Gute und Positive daran
     war und wie es ins Gesamtbild passt.
  5. Die Auswertung erscheint unter Franks Text (M-10), mit Lautsprecher-Knopf (F-12).
  6. Danach laufen F-14 (Logbuch) und — bei vollständiger Auswertung — F-17 (Erkenntnisse).
- **Daten:** Gelesen: alle Speicher. Geschrieben: `Evaluation.aiText`.
- **Ergebnis:** Zu jedem ausgewerteten Tag steht neben Franks Worten die Sicht der KI.
- **Fehlerfall:** Wie F-03. Franks eigener Text bleibt in jedem Fall gespeichert.
- **Regeln/Grenzen:** Die KI bewertet nicht mit Noten, Punkten oder „gut/schlecht". Sie
  beschreibt, ordnet ein und benennt, was sie sieht.

### F-12 — Auswertung vorlesen

> **Geändert in v2.** Vorlesen gibt es jetzt **überall** (`F-47`), und es fällt nie mehr ganz
> aus: kommt der gewählte Weg nicht durch, übernimmt die **Stimme des Geräts** (`F-48`) und
> Frank hört einen Satz dazu. Technische Fehlertexte („Google TTS error 403: {error…") werden
> in lesbare Sätze übersetzt.

- **Auslöser:** Lautsprecher-Symbol an einer KI-Auswertung (B-03) oder an einem
  Chronik-Eintrag (B-07).
- **Ablauf:**
  1. Der Text geht an den in B-08 gewählten Anbieter:
     **Google Chirp 3 HD** (`texttospeech.googleapis.com/v1/text:synthesize`, Stimme aus dem
     Katalog, `de-DE`, MP3) · **Meine Stimme** (Alibaba DashScope, registrierte Stimme) ·
     **Microsoft Edge** (Standardstimmen).
  2. Wiedergabe startet. Das Symbol zeigt den laufenden Zustand.
  3. Erneuter Druck hält an; ein weiterer setzt fort.
- **Daten:** Gelesen: Auswertungstext, Stimmeinstellung, Schlüssel.
- **Ergebnis:** Der Text wird gesprochen.
- **Fehlerfall:** Fehlender Schlüssel → „Für diese Stimme fehlt der Schlüssel." mit Verweis
  in die Einstellungen. Kein Netz oder Dienst antwortet nicht → Fehlertext, der Text bleibt
  lesbar.
- **Regeln/Grenzen:** Nur eine Wiedergabe gleichzeitig. Ein neuer Vorlesevorgang bricht den
  laufenden ab.

### F-13 — Experiment abschließen

> **Geändert in v2.** Der Abschluss geschieht **ausschließlich auf ausdrücklichen Knopf** —
> „Abschließen" auf `B-03`. Er trägt den Zustand `ABGESCHLOSSEN` ein, setzt den Abschluss-Vermerk
> an die **jüngste** Auswertung (nicht an „die von heute") und stößt erst danach `F-17` an.
> „Nicht umgesetzt" ist von `B-03` **und** von der Abendkarte auf `B-01` erreichbar und schreibt
> jetzt auch einen Logbuch-Eintrag — bis hierher stand er dort nie, obwohl die Oberfläche es sagte.

- **Auslöser:** Automatisch, wenn die vollständige KI-Auswertung (F-11, letzter Tag)
  vorliegt. Oder manuell über „Nicht umgesetzt" an einem offenen Experiment.
- **Ablauf:**
  - **Umgesetzt:** Zustand wird `ABGESCHLOSSEN`. Die Karte verlässt den Monitor mit der
    Lichtblüte (M-93, Effekt `E-16`). Das Experiment wandert in die Chronik (F-14). Ein Platz
    der drei wird frei — gesperrte „Starten"-Knöpfe im Monitor werden sofort wieder aktiv.
  - **Nicht umgesetzt:** Frank spricht trotzdem ein, warum es nicht kam (F-10), die KI
    wertet aus (F-11). Danach **beides**: Der Vorgang wandert mit dem Ausgang
    „nicht umgesetzt" in die Chronik **und** das Experiment kommt zurück auf die
    Merkliste — mit dem Vermerk, was beim letzten Mal im Weg stand.
- **Daten:** Geschrieben: `Experiment.state`, `Experiment.closedAt`, ggf. `WatchlistItem`.
- **Ergebnis:** Der Platz ist frei, die Chronik gewachsen, die Merkliste ggf. ergänzt.
- **Fehlerfall:** Keiner — rein lokal.
- **Regeln/Grenzen:** Ein abgeschlossenes Experiment lässt sich nicht wieder öffnen. Kommt
  es später über die Merkliste zurück, ist es ein neuer Durchlauf, der den alten kennt.

### F-14 — Logbuch fortschreiben

- **Auslöser:** Nach F-01 (Lage), nach jeder Auswertung (F-11) und nach jedem
  abgeschlossenen Gespräch (F-09).
- **Ablauf:**
  1. Alles Neue des Tages geht an Codex — mit dem für das **Logbuch** gewählten Modell und
     Effort (F-22), nicht mit dem für Experimente.
  2. Die KI erfasst, fasst zusammen und schreibt den **ausführlichen** Eintrag des Tages
     fort: Lage, laufende Experimente, was getan wurde, Gesprächsinhalte, Auswertungen.
  3. Zielumfang: **20–30 Zeilen pro Tag.**
- **Daten:** Gelesen: der bisherige heutige Eintrag, alle neuen Inhalte. Geschrieben:
  `LogDay.detailText`.
- **Ergebnis:** Der Tag steht ausführlich im aktuellen Log.
- **Fehlerfall:** Kein Netz → Der Rohstoff bleibt gespeichert und wird beim nächsten
  erfolgreichen Lauf nachgetragen. Es geht nichts verloren.
- **Regeln/Grenzen:** Ein Tag = ein Eintrag. Die KI ergänzt einen bestehenden Eintrag,
  statt mehrere für denselben Tag anzulegen.

### F-15 — Tagesverdichtung nach 15 Tagen

- **Auslöser:** Beim ersten Öffnen der App an einem neuen Kalendertag, **bevor** irgendetwas
  anderes geschieht.
- **Ablauf:**
  1. Alle Tage im aktuellen Log, die älter als 15 Tage sind, werden ermittelt.
  2. Jeder wird an Codex geschickt (Logbuch-Modell) und **verdichtet**: Datum, Experiment,
     wie es durchgeführt wurde, die Auswertung, wichtige Punkte.
  3. Zielumfang: **höchstens 7 Zeilen.**
  4. Der verdichtete Text wandert ins Langzeit-Log, der ausführliche Eintrag wird gelöscht.
- **Daten:** Gelesen: `LogDay.detailText`. Geschrieben: `LogDay.compactText`,
  `LogDay.detailText = null`.
- **Ergebnis:** Das aktuelle Log bleibt bei 15 Tagen, das Langzeit-Log wächst. **Nichts
  fällt heraus.**
- **Fehlerfall:** Kein Netz → Die Verdichtung wird verschoben; der Tag bleibt so lange
  ausführlich stehen. Kein Datenverlust.
- **Regeln/Grenzen:** Die Verdichtung ist **unumkehrbar**. Sie läuft nie automatisch, ohne
  dass sie gelungen ist — erst wenn der verdichtete Text vorliegt, wird der ausführliche
  gelöscht.

### F-16 — Logbuch-Eintrag ändern oder löschen

> **Geändert in v2.** Beide Wege waren im Modell fertig gebaut, aber an **keinem Knopf**
> angeschlossen: das Logbuch war nur lesbar, und eine falsch verstandene Zeile wirkte dauerhaft
> in jede Anfrage weiter. Jetzt trägt jeder Eintrag „Ändern" und einen Papierkorb; das Löschen
> fragt einmal zurück („Wirklich löschen" · „Behalten"). Ausgelöst wird über Knöpfe, nicht über
> einen langen Druck.

- **Auslöser:** Langer Druck auf einen Eintrag auf B-07.
- **Ablauf:** Der Text wird bearbeitbar. Speichern übernimmt die Änderung; Löschen entfernt
  den Eintrag nach Rückfrage.
- **Daten:** Geschrieben: `LogDay.detailText` bzw. `LogDay.compactText`, oder Löschung.
- **Ergebnis:** Ein falsch verstandener Eintrag wirkt nicht weiter auf künftige Vorschläge.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Gilt für beide Reiter. Eine Löschung ist endgültig.

### F-17 — Erkenntnisse fortschreiben

- **Auslöser:** Nach jeder **vollständigen** KI-Auswertung (F-11, letzter Tag).
- **Ablauf:**
  1. Die bisherige Erkenntnisliste und die neue Auswertung gehen an Codex
     (Logbuch-Modell).
  2. Die KI **ergänzt einen neuen Satz oder schärft einen bestehenden** — sie hängt nicht
     stumpf an. Doppelungen werden zusammengeführt.
  3. Jede Erkenntnis trägt das Datum ihrer letzten Änderung.
- **Daten:** Gelesen: `Insight`-Sätze, `Evaluation`. Geschrieben: `Insight`.
- **Ergebnis:** Eine am Stück lesbare Liste dessen, was Frank über sich gelernt hat.
- **Fehlerfall:** Kein Netz → wird beim nächsten Lauf nachgeholt.
- **Regeln/Grenzen:** Die Liste wächst langsam und bleibt lesbar. Keine Bewertung, keine
  Punkte — Sätze über Frank, nicht über seine Leistung.

### F-18 — Merkliste: eigenes Experiment anlegen

> **Geändert in v2.** Die Anlegefläche ist dieselbe wie auf `B-10` und trägt die **Tagewahl**
> (`F-43`). **Ohne Netz wird trotzdem gespeichert** — vorher warf diese Stelle den Fehler nach
> oben durch und legte gar nichts an: eine eingesprochene Idee war nach der Störungsmeldung
> nirgends abgelegt.

- **Auslöser:** Plus-Knopf auf B-05.
- **Ablauf:** Sprechknopf → Transkription (wie F-01) → bearbeitbares Feld mit F-02 →
  Speichern. Dauer und Stufe schätzt die KI beim Speichern mit.
- **Daten:** Geschrieben: `WatchlistItem` (Quelle: `eigen`).
- **Ergebnis:** Eine eigene Idee liegt für später bereit und kommt über den
  Merklisten-Platz (F-03) zurück.
- **Fehlerfall:** Wie F-01.
- **Regeln/Grenzen:** Eigene Einträge sind gleichwertig mit gemerkten Vorschlägen.

### F-19 — Merkliste: Eintrag löschen

> **Geändert in v2.** Gelöscht wird über den Papierkorb an der Karte, **ohne** Rückfrage — ein
> Merklisten-Eintrag ist eine Notiz, kein Verlauf. Wischen und langer Druck lösen nichts aus.

- **Auslöser:** Wischen oder langer Druck auf einen Merklisten-Eintrag.
- **Ablauf:** Rückfrage, dann Löschung.
- **Daten:** Geschrieben: Löschung des `WatchlistItem`.
- **Ergebnis:** Der Eintrag kommt nicht mehr als Merklisten-Platz zurück.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Endgültig.

### F-20 — Wünsche & Ziele pflegen

> **Geändert in v2.** Ändern und Löschen waren im Modell fertig, aber von keinem Knopf aus
> erreichbar — ein einmal eingesprochenes Ziel ließ sich nie mehr berichtigen, obwohl es in jede
> Anfrage eingeht. Jede Zielkarte trägt jetzt „Ändern" (macht daraus ein Eingabefeld mit
> „Abbrechen" und „Sichern") und einen Papierkorb, dazu die Zeile „seit TT.MM.JJJJ".
> Das Anlegeblatt **bleibt nach dem Speichern offen**, damit mehrere Ziele hintereinander
> eingesprochen werden können.

- **Auslöser:** B-04, Plus-Knopf.
- **Ablauf:**
  1. Sprechknopf → Transkription → bearbeitbares Feld mit F-02 → Speichern.
  2. **Mehrere hintereinander weg:** Nach dem Speichern steht der Sprechknopf sofort wieder
     bereit, ohne Zwischenschritt.
  3. Bestehende Ziele lassen sich antippen, ändern und löschen.
- **Daten:** Geschrieben: `Goal` (Text, angelegt am, geändert am).
- **Ergebnis:** Die Ziele stehen fest und gehen als voller Text in jede Vorschlagsanfrage
  (F-03) ein.
- **Fehlerfall:** Wie F-01.
- **Regeln/Grenzen:** Keine Mengenbegrenzung. Kein Fortschrittsbalken, kein Fälligkeitsdatum
  — es sind Ziele, keine Aufgaben.

### F-21 — Selbstbild pflegen

> **Geändert in v2.** Siehe `F-52`: Es gibt einen ausdrücklichen **Speichern**-Knopf, eine
> Standanzeige („Noch nicht gespeichert." / „Gespeichert.") und drei Sicherungsschichten. Das
> Selbstbild war die undichteste Stelle der App — es wurde einzig beim Druck auf den Zurück-Pfeil
> gespeichert.

- **Auslöser:** B-09, erreichbar aus B-08.
- **Ablauf:** Ein großes, frei beschreibbares Textfeld. Tippen oder einsprechen (F-01-Weg),
  F-02 verfügbar. Wird beim Verlassen gespeichert.
- **Daten:** Geschrieben: `SelfImage.text`.
- **Ergebnis:** Alles, was die App dauerhaft über Frank wissen soll, steht an einer Stelle
  und geht als **erster** Block in jede Vorschlagsanfrage ein.
- **Fehlerfall:** Wie F-01 (nur beim Einsprechen).
- **Regeln/Grenzen:** Keine Längenbegrenzung. Kein Raster, keine Felder, keine Fragen —
  ein Fließtext.

### F-22 — Modell und Effort wählen

- **Auslöser:** B-08, Abschnitt „KI".
- **Ablauf:** Zwei getrennte Paare von Auswahlfeldern:
  - **Experimente:** Modell (GPT 5.6 Sol · GPT 5.6 Terra · GPT 5.6 Luna) + Effort
    (Niedrig · Mittel · Hoch · Sehr hoch · Maximal)
  - **Logbuch:** Modell + Effort, unabhängig davon
- **Daten:** Geschrieben: vier Werte in den verschlüsselten Einstellungen.
- **Ergebnis:** Die Wahl wirkt ab der nächsten Anfrage.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Werte und Bezeichnungen exakt wie `CodexModels.kt` in PerfectMoment
  (`gpt-5.6-sol`, `gpt-5.6-terra`, `gpt-5.6-luna`; `low`, `medium`, `high`, `xhigh`, `max`).
  Vorbelegung: Experimente = Terra / Hoch · Logbuch = Luna / Mittel.

### F-23 — Stimme und Vorlesen einstellen

> **Geändert in v2.** Vier Punkte:
> 1. Es gibt einen **vierten Anbieter**: die **Stimme des Geräts** (`F-48`).
> 2. Anbieter und Stimmen kommen **ausschließlich aus `TtsCatalog`** — derselben Quelle, aus der
>    auch der Vorleser liest. Vorher stand im Einstellungs-Bildschirm eine zweite, von Hand
>    getippte Liste mit abweichenden Kennungen (`qwen` statt `qwen_clone`); damit fiel
>    „Meine Stimme" beim Vorlesen stumm auf Edge zurück. **31 Chirp-3-Stimmen, 6 Edge-Stimmen.**
> 3. Die eigenen Stimmen werden bei Alibaba **abgerufen und verwaltet** (`F-53`).
> 4. Die Vorbelegung ist **Microsoft Edge**, nicht mehr Google — Edge braucht nur Netz, keinen
>    Schlüssel. Wer die App frisch benutzte, bekam vorher auf jeden Lautsprecher eine Fehlermeldung.
>    Sprechgeschwindigkeit 0,70 bis 1,30 in Schritten von 0,05.

- **Auslöser:** B-08, Abschnitt „Stimme".
- **Ablauf:**
  1. Anbieter wählen: **Google Chirp 3 HD** · **Meine Stimme** · **Microsoft Edge**.
  2. Bei Chirp 3 HD: eine der 31 deutschen Stimmen aus dem Katalog wählen
     (Vorbelegung `de-DE-Chirp3-HD-Kore`).
  3. Bei „Meine Stimme": Stimme aufnehmen und registrieren (Verfahren aus
     `QwenVoiceEnrollment.kt`), bestehende Stimmen verwalten und löschen.
  4. Bei Edge: eine der Standardstimmen wählen (Vorbelegung
     `de-DE-SeraphinaMultilingualNeural`).
  5. Sprechgeschwindigkeit einstellbar (0,7 bis 1,3).
  6. Probe-Knopf liest einen Beispielsatz vor.
- **Daten:** Geschrieben: Anbieter, Stimmkennung, Geschwindigkeit.
- **Ergebnis:** Alles Vorgelesene klingt wie gewählt.
- **Fehlerfall:** Registrierung schlägt fehl → Fehlertext von DashScope, bisherige Stimmen
  bleiben.
- **Regeln/Grenzen:** Genau wie in PerfectMoment aufgebaut.

### F-24 — Zugänge einrichten

> **Geändert in v2.** Siehe `F-54`: Der Benutzercode steht groß auf dem Bildschirm, lässt sich
> kopieren, die Seite erneut öffnen und die laufende Anmeldung abbrechen. Die Schlüsselfelder
> tragen ein **Auge** zum Sichtbarmachen. Ist die App angemeldet, steht die Adresse dabei.

- **Auslöser:** B-08, Abschnitt „Zugänge".
- **Ablauf:**
  - **Codex:** Geräteanmeldung starten → Benutzercode und Adresse werden angezeigt →
    nach Bestätigung im Browser meldet die App den angemeldeten Zustand. Abmelden möglich.
  - **Schlüssel:** Groq · Google Cloud · Alibaba DashScope. Je ein Feld, verdeckt angezeigt.
- **Daten:** Geschrieben: verschlüsselte Einstellungen.
- **Ergebnis:** Die Dienste sind nutzbar.
- **Fehlerfall:** Anmeldung abgelehnt oder abgelaufen → klarer Text mit erneutem Versuch.
- **Regeln/Grenzen:** Kein Schlüssel steht im Quellcode. Alles wird auf dem Gerät
  verschlüsselt abgelegt.

### F-25 — Erinnerungen einstellen

> **Geändert in v2.** Die Uhrzeit wird an einer echten **Uhr** gestellt (`F-55`), nicht getippt.
> Wer die Uhr stellt, will geweckt werden: die Erinnerung schaltet sich dabei mit ein. Darf die
> App ab Android 12 keine exakte Weckzeit setzen, weckt sie **ungenau** statt gar nicht.

- **Auslöser:** B-08, Abschnitt „Erinnerungen".
- **Ablauf:**
  1. Zwei Schalter mit je einer Uhrzeit:
     **morgens** (Vorbelegung 08:00) — „Wie ist deine Lage heute?"
     **abends** (Vorbelegung 20:30) — „Wie ist es gelaufen?"
  2. Beim ersten Einschalten wird `POST_NOTIFICATIONS` angefragt.
  3. Die Benachrichtigung öffnet beim Antippen B-01 im passenden Zustand.
- **Daten:** Geschrieben: zwei Schalter, zwei Uhrzeiten.
- **Ergebnis:** Die App meldet sich zu den gewählten Zeiten.
- **Fehlerfall:** Berechtigung abgelehnt → Schalter bleiben aus, Hinweis erscheint einmalig.
- **Regeln/Grenzen:** Beide einzeln abschaltbar. Keine weiteren Benachrichtigungen — die App
  meldet sich sonst **nie** von selbst.

### F-26 — Erscheinung umschalten

> **Geändert in v2 — hier weicht die App bewusst vom v1-Spec ab.** Das Symbol des
> Schnellschalters zeigt den **gerade aktiven** Modus, nicht mehr den nächsten. Ein
> Anzeigeelement soll den Zustand berichten: bei hellem Bildschirm stand dort vorher der Mond,
> und wer die Sonne sah, saß im Dunkeln. Wohin der nächste Druck führt, steht in der
> Beschriftung, die die Sprachausgabe vorliest („Dunkelmodus aktiv — weiter zu Automatik").
> Die Reihenfolge des Durchlaufs bleibt **Hell → Dunkel → Wie das System → Hell**; in `B-08`
> stehen die Pillen in der Reihenfolge **Dunkel · Hell · Wie das System**.

- **Auslöser:** B-08, Abschnitt „Erscheinung", oder der Schnellschalter in der oberen Leiste von B-10 und B-01.
- **Ablauf:**
  1. In B-08 stehen die drei Möglichkeiten **Hell** · **Dunkel** · **Wie das System** direkt zur Wahl. Vorbelegung: **Dunkel**.
  2. Der Schnellschalter durchläuft sie in der festen Reihenfolge **Hell → Dunkel → Wie das System → Hell**. Sein Symbol kündigt den nächsten Modus eindeutig an: eine Sonne für Hell, eine klar gezeichnete Mondsichel für Dunkel und ein großes „A“ mit jeweils drei seitlichen Strahlen für Automatik.
  3. **Wie das System** übernimmt sofort die aktuelle Systemdarstellung, speichert den eigentlichen Moduswert `system` statt nur der gerade aufgelösten Farbe und folgt ohne Neustart jedem späteren Wechsel des Systems, solange dieser Modus aktiv bleibt.
- **Daten:** Geschrieben: Theme-Wert in den verschlüsselten Einstellungen.
- **Ergebnis:** Die Erscheinung wechselt sofort, ohne Neustart, und bleibt zwischen App-Starts erhalten.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Beide Erscheinungen sind gleichrangig und vollständig gebaut; der Automatikmodus muss sowohl beim Einschalten als auch bei späteren Systemwechseln wirksam sein.

### F-27 — Zwischen den Hauptbildschirmen wischen

> **Nachgetragen in v2.** `F-27` bis `F-33` standen im v1-Spec nur in der Übersichtstabelle und
> im Anhang „Neu aus dem Design" — beschrieben war keine von ihnen. Sie sind gebaut, also stehen
> sie hier.

- **Auslöser:** Waagerechtes Wischen auf einem der sechs Hauptbildschirme.
- **Ablauf:** Ab **90 px** Wischweite wechselt der Bildschirm — nach links zum nächsten Feld der
  unteren Leiste, nach rechts zum vorigen. Darunter war es ein Antippen.
- **Daten:** Keine.
- **Ergebnis:** Die sechs Hauptbildschirme sind ohne Zielen auf die Leiste erreichbar.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Bewusst **kein** Pager — der würde das Schieben des Inhalts erzwingen. Das
  Wischen ist eine eigene Geste, die den Bildschirm **wechselt**, statt ihn mitzuziehen. An den
  Enden der Reihe passiert nichts; es wird **nicht umlaufend** gewechselt. `B-02`, `B-03`, `B-08`
  und `B-09` reagieren nicht auf Wischen — dort führt die Geste des Systems zurück (`F-51`).

### F-28 — Lage tippen statt sprechen

- **Auslöser:** „Lieber tippen" unter dem großen Sprechknopf auf `B-01`.
- **Ablauf:** Der Zustand springt ohne Aufnahme auf `LAGE_STEHT`; das Eingabefeld steht mit dem
  Platzhalter „Was für ein Tag ist das? Was liegt vor dir?" bereit.
- **Daten:** Wie `F-01`, nur ohne Aufnahme.
- **Ergebnis:** Die Lage lässt sich auch ohne Netz und ohne Mikrofon festhalten.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Derselbe Weg steht auf `B-03` unter dem Sprechknopf („Lieber tippen").

### F-29 — Auswertungstext bearbeiten

- **Auslöser:** Das Eingabefeld auf `B-03`, sobald ein Text da ist.
- **Ablauf:** Der transkribierte Text steht bearbeitbar da (Mindesthöhe 140 dp) und lässt sich
  vor „Weiter" beliebig ändern; „Text mit KI verbessern" (`F-02`) steht daneben.
- **Daten:** Geht als `Evaluation.ownText` ein.
- **Ergebnis:** Ein Verhörer der Spracherkennung wirkt nicht in die Einschätzung hinein.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Ist das Feld leer, meldet „Weiter": „Da steht noch nichts. Sprich etwas ein
  oder tipp es."

### F-30 — Auswertung überspringen

- **Auslöser:** Der Zurück-Weg von `B-03`, ohne „Weiter" zu drücken.
- **Ablauf:** Die Anzeige wird geräumt, das Experiment bleibt **laufend** und erscheint am
  nächsten Abend erneut.
- **Daten:** Geschrieben: nichts — das bereits Gesagte ist längst gespeichert.
- **Ergebnis:** Ein Abend darf ausfallen, ohne dass etwas endet.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** **Geändert in v2:** Der Text bleibt beim Zurückkehren zum **selben**
  Experiment stehen — er ist Arbeit, und ein Fehlgriff auf den Zurück-Pfeil darf ihn nicht
  kosten. Geleert wird nur, wenn ein *anderes* Experiment geöffnet wird.

### F-31 — Logbuch: Reiter *15 Tage*

- **Auslöser:** Der erste Reiter auf `B-07`.
- **Ablauf:** Zeigt alle Tage mit ausführlichem Text (`detailText`), der jüngste oben.
- **Daten:** Gelesen: `LogDay`.
- **Ergebnis:** Die letzten fünfzehn Tage stehen ausführlich da.
- **Fehlerfall:** Keiner — ohne Netz lesbar.
- **Regeln/Grenzen:** **Geändert in v2:** Der Reiter heißt kurz **„15 Tage"** statt „Letzte 15
  Tage" — zu dritt bleibt je Reiter ein Drittel der Breite.

### F-32 — Logbuch: Reiter *Langzeit*

- **Auslöser:** Der zweite Reiter auf `B-07`.
- **Ablauf:** Zeigt alle verdichteten Tage (`compactText`), der jüngste oben.
- **Daten:** Gelesen: `LogDay`.
- **Ergebnis:** Die ganze Chronik ist am Stück lesbar. **Nichts fällt heraus.**
- **Fehlerfall:** Keiner — ohne Netz lesbar.
- **Regeln/Grenzen:** Ändern und Löschen (`F-16`) gelten in beiden Reitern.

### F-33 — Anlegen abbrechen

- **Auslöser:** Der Knopf „Abbrechen" in jeder Anlegefläche (`B-10`, `B-04`, `B-05`).
- **Ablauf:** Die Fläche schließt, **ohne zu speichern**; der eingesprochene Text wird verworfen.
- **Daten:** Keine.
- **Ergebnis:** Ein begonnener Eintrag lässt sich fallen lassen.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** **Geändert in v2:** Nur dieser Knopf verwirft. **Ein Druck neben die Fläche
  verwirft nicht** — er legt sie nur beiseite (`F-58`).

### F-34 — Monitor: sehen, was ansteht und was läuft

- **Auslöser:** Beim Starten der App. **B-10 ist der Startbildschirm.** Außerdem über das
  erste Feld der unteren Leiste, das auf jedem Hauptbildschirm vorhanden ist.
- **Ablauf:**
  1. Der Monitor lädt alle Experimente mit dem Zustand `LAEUFT` und `ANSTEHEND` und zeigt
     sie in **zwei Abschnitten untereinander**:
     - **„Läuft"** — die höchstens drei laufenden Experimente, das jüngst gestartete oben.
     - **„Steht an"** — beliebig viele vorgemerkte Experimente in Franks eigener
       Reihenfolge (F-38).
  2. Jede Karte zeigt: **Titel** · **Stufe** · **Dauer** · **Herkunft** (von der KI
     vorgeschlagen · selbst angelegt · von der Merkliste) und bei laufenden zusätzlich
     **„Tag 2 von 3"** und den **Stand der heutigen Aufgaben** („3 von 5").
  3. Unter dem Abschnitt „Läuft" steht die **eine To-Do-Liste des Tages** (F-07),
     unverändert nach Experimenten gruppiert.
  4. Ist nichts vorhanden, steht dort der Satz aus Teil B §8.
- **Daten:** Gelesen: `Experiment` (Zustand `LAEUFT`, `ANSTEHEND`), `Task` (heute).
  Geschrieben: nichts.
- **Ergebnis:** Frank sieht beim Öffnen der App an einer Stelle alles, was er sich
  vorgenommen hat — unabhängig davon, ob er es selbst erdacht oder von der KI übernommen hat.
- **Fehlerfall:** Keiner — rein lokal. Der Monitor ist **ohne Netz vollständig benutzbar**.
- **Regeln/Grenzen:**
  - **Der Monitor ist die Hauptseite.** „Heute" (B-01) bleibt vollständig erhalten, ist aber
    nicht mehr der Startbildschirm; es ist der Ort, an dem die Lage eingesprochen und
    Vorschläge erzeugt werden.
  - **Anstehende Experimente sind unbegrenzt.** Die Grenze von drei gilt weiterhin
    ausschließlich für **laufende** (`LAEUFT`) Experimente.
  - Der Monitor **misst nicht**: keine Punkte, keine Serien, keine Abzeichen, keine Bewertung.
    Der Stand der Aufgaben ist eine Zustandsanzeige, keine Note.

### F-35 — Monitor: eigenes Experiment anlegen

> **Geändert in v2.** Die **Stufe** wird nicht mehr von Hand eingestellt — sie schätzt die KI.
> Statt ihrer steht die **Tagewahl** in der Fläche (`F-43`), weil genau dort die Schätzung
> danebenlag. Nach dem Speichern springt die App auf den Monitor, die neue Karte funkelt, und
> die Meldung nennt die Dauer: „Steht jetzt unter ‚Steht an' — 3 Tage."

- **Auslöser:** Der schwebende Plus-Knopf auf B-10.
- **Ablauf:**
  1. Die Anlegefläche fährt von unten herein (M-80). Sprechknopf und Textfeld stehen bereit.
  2. Frank spricht sein Experiment ein — Transkription wie F-01 —, **oder** er tippt es.
     „Text mit KI verbessern" (F-02) steht zur Verfügung.
  3. Optional stellt er **Dauer in Tagen** und **Stufe** selbst ein. Lässt er beides
     unangetastet, ergänzt die KI sie beim Speichern zusammen mit einer **Aufgabenliste je
     Tag** — mit dem für *Experimente* gewählten Modell und Effort (F-22).
  4. „Speichern" legt das Experiment als `ANSTEHEND` **oben** im Abschnitt „Steht an" ab.
     Die Karte erscheint mit dem Einflug-Funkeln (M-81).
- **Daten:** Geschrieben: `Experiment` (Zustand `ANSTEHEND`, Herkunft `EIGEN`,
  `addedAt`, `order`), `Task`-Sätze.
- **Ergebnis:** Franks eigene Idee steht gleichrangig neben den übernommenen KI-Vorschlägen
  im Monitor.
- **Fehlerfall:**
  - Wie F-01 beim Einsprechen.
  - **Kein Netz:** Das Experiment wird **trotzdem gespeichert** — mit dem eingetippten Text,
    Dauer 1 und Stufe *mittel*, sofern Frank nichts anderes gewählt hat. Die Aufgabenliste
    wird beim nächsten erfolgreichen Lauf nachgetragen. Es geht nichts verloren.
- **Regeln/Grenzen:** Eigene Experimente sind **vollwertige Experimente**, keine Merkzettel.
  Sie werden ausgewertet (F-10, F-11), gehen ins Logbuch (F-14) und in die Erkenntnisse
  (F-17) ein wie jedes andere. Der Unterschied zur Merkliste (F-18): Was hier angelegt wird,
  ist bereits vorgemerkt; die Merkliste bleibt der Ort für Ideen ohne Vorsatz.

### F-36 — Einen KI-Vorschlag in den Monitor übernehmen

- **Auslöser:** Der Knopf **„In den Monitor"** auf einer Vorschlagskarte (B-01).
- **Ablauf:**
  1. Aus dem `Suggestion` wird ein `Experiment` mit Zustand `ANSTEHEND`, Herkunft
     `KI_VORSCHLAG` (bzw. `MERKLISTE`, wenn der Vorschlag von dort kam) und der vollständigen
     Aufgabenliste je Tag.
  2. Die Karte fliegt sichtbar in Richtung des Monitor-Feldes der unteren Leiste (M-82);
     das Feld leuchtet kurz auf und trägt danach die neue Anzahl.
  3. Die übrigen Vorschläge **bleiben stehen** — Frank kann mehrere hintereinander
     übernehmen.
  4. Kam der Vorschlag von der Merkliste, wird er dort entfernt.
- **Daten:** Gelesen: `Suggestion`. Geschrieben: `Experiment` (`ANSTEHEND`), `Task`-Sätze,
  ggf. Löschung des `WatchlistItem`.
- **Ergebnis:** Der Vorschlag steht im Monitor und ist damit Franks Vorhaben, nicht mehr nur
  ein Angebot.
- **Fehlerfall:** Keiner — rein lokal.
- **Regeln/Grenzen:** Übernehmen ist **nicht** starten. Es zählt nicht gegen die Grenze von
  drei; die greift erst beim Start (F-37). Ein Vorschlag lässt sich nicht doppelt übernehmen;
  der Knopf zeigt dann den übernommenen Zustand.

### F-37 — Ein Experiment starten

- **Auslöser:** Der Knopf **„Starten"** auf einer anstehenden Karte im Monitor. Zusätzlich
  weiterhin **„Jetzt starten"** direkt auf einer Vorschlagskarte auf B-01 (F-06), das beide
  Schritte in einem ausführt.
- **Ablauf:**
  1. Der Zustand wechselt von `ANSTEHEND` auf `LAEUFT`, `startedAt` wird auf heute gesetzt,
     die Aufgaben werden auf die Tage verteilt.
  2. Die Karte wandert mit einer sichtbaren Bewegung aus „Steht an" nach oben in „Läuft"
     (M-83), begleitet von Funken (M-84) und einem kurzen Haptik-Muster.
  3. Die To-Do-Liste des Tages (F-07) nimmt die heutigen Aufgaben auf.
- **Daten:** Geschrieben: `Experiment.state`, `Experiment.startedAt`, `Task.dayIndex`.
- **Ergebnis:** Ein weiteres Experiment läuft und erscheint abends in der Auswertung (F-10).
- **Fehlerfall:** Laufen bereits drei, ist der Knopf gesperrt und trägt den Hinweis
  „Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst." Die Karte
  bleibt anstehend.
- **Regeln/Grenzen:** **Höchstens drei gleichzeitig laufende Experimente** — unverändert.
  Wird eines abgeschlossen (F-13), wird ein Platz frei und die gesperrten Starten-Knöpfe
  werden wieder aktiv.

### F-38 — Reihenfolge im Monitor ändern

- **Auslöser:** Langer Druck auf eine anstehende Karte, dann Ziehen.
- **Ablauf:** Die Karte hebt sich sichtbar ab (M-85), die übrigen weichen aus. Beim Loslassen
  rastet sie an der neuen Stelle ein.
- **Daten:** Geschrieben: `Experiment.order` aller betroffenen Sätze.
- **Ergebnis:** Frank bestimmt selbst, was als Nächstes drankommt.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Nur der Abschnitt „Steht an" ist sortierbar. Laufende Experimente
  ordnen sich nach ihrem Startzeitpunkt und sind nicht verschiebbar.

### F-39 — Ein Experiment aus dem Monitor nehmen

> **Geändert in v2.** Es gibt **keine Rückfrage mit zwei Wegen** mehr. Sowohl das Wischen nach
> links als auch das Kreuz an der Karte legen das Experiment **auf die Merkliste** — der
> erhaltende Weg ist der richtige Standard, und eine Rückfrage bei jeder Wischgeste hielt den
> Fluss an. Endgültig gelöscht wird danach auf der Merkliste (`F-19`). Beim Wischen scheint ein
> Warnton durch (`E-25`), damit die Geste nicht versehentlich zu Ende geht.

- **Auslöser:** Wischen einer anstehenden Karte nach links, oder langer Druck → „Entfernen".
- **Ablauf:** Rückfrage mit zwei Wegen:
  - **„Auf die Merkliste"** — das Experiment wandert vollständig zurück auf die Merkliste
    (B-05) und bleibt erhalten.
  - **„Löschen"** — es wird endgültig entfernt.
- **Daten:** Geschrieben: Löschung des `Experiment` und seiner `Task`-Sätze, ggf. neuer
  `WatchlistItem`.
- **Ergebnis:** Der Monitor zeigt nur, was Frank wirklich vorhat.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Gilt **nur für anstehende** Experimente. Ein laufendes wird über
  „Nicht umgesetzt" (F-13) beendet, nicht gelöscht — sonst ginge seine Geschichte verloren.

### F-40 — Monitor-Karte auf- und zuklappen

- **Auslöser:** Tippen auf eine Karte im Monitor.
- **Ablauf:** Die Karte klappt weich auf (M-86) und zeigt die vollständige Beschreibung, alle
  Aufgaben je Tag und — bei laufenden — den Gesprächs-Knopf (→ B-02) und „Wie ist es
  gelaufen?" (→ B-03). Erneutes Tippen klappt sie zu. Der Zustand überlebt einen
  Bildschirmwechsel, aber nicht den App-Neustart.
- **Daten:** Gelesen: `Experiment`, `Task`. Geschrieben: nichts.
- **Ergebnis:** Der Monitor bleibt übersichtlich und liefert die Einzelheiten auf Wunsch.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Es ist immer höchstens **eine** Karte aufgeklappt; das Öffnen einer
  zweiten schließt die erste.

### F-41 — Effekt-Stärke einstellen

> **Geändert in v2 — die Stufen sind schärfer gefasst**, so wie der Entwurf sie als
> `data-effekte` am Wurzelelement trägt:
> - **Voll** — alle Effekte aus Teil B §7 laufen.
> - **Gedämpft** — `[data-dauerbewegung]{animation:none; opacity:.45}`: Dauerbewegungen,
>   Partikel und die Kipp-Parallaxe **stehen still, bleiben aber sichtbar** (45 % Deckkraft).
>   Übergänge, Weichzeichnen und Verläufe bleiben.
> - **Aus** — `*{animation:none; transition:none}` und `[data-dauerbewegung]{display:none}`:
>   Dauerbewegungs-Elemente verschwinden ganz, jede gemessene Dauer wird auf 0 ms gesetzt,
>   die Haptik schweigt.

- **Auslöser:** B-08, Abschnitt „Darstellung".
- **Ablauf:** Drei Möglichkeiten stehen direkt zur Wahl:
  - **Voll** (Vorbelegung) — alle Effekte aus Teil B §7 sind aktiv.
  - **Gedämpft** — Dauerbewegungen, Partikel und die Kipp-Parallaxe sind aus; Übergänge,
    Weichzeichnen und Verläufe bleiben.
  - **Aus** — nur einfache Überblendungen, keine Verläufe in Bewegung, keine Partikel.
- **Daten:** Geschrieben: `effect_level` in den verschlüsselten Einstellungen.
- **Ergebnis:** Die Wahl wirkt **sofort**, ohne Neustart, auf allen Bildschirmen.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:**
  - Meldet das System **„Bewegung reduzieren"**, gilt mindestens *Gedämpft*, auch wenn *Voll*
    eingestellt ist. Frank kann nicht versehentlich gegen seine Systemeinstellung laufen.
  - Auf **Aus** bleibt jede Funktion vollständig bedienbar. Kein Effekt trägt Information,
    die es sonst nirgends gibt.


### F-42 NEU — Dauer eines Experiments nachträglich ändern

- **Auslöser:** Antippen der Tagesangabe auf einer Karte im Monitor — „Tag 4 von 5" an einer
  Laufkarte, „fordernd · 3 Tage" an einer anstehenden. Ein antippbares Etikett trägt einen Rand
  in *Aktion* (`E-26`) und ist 32 dp hoch statt 24.
- **Ablauf:**
  1. Der Dialog **„Wie lange?"** öffnet sich (Radius 20 dp, Fläche *Fläche*) und nennt den Titel.
  2. Die **Tagewahl** (`F-43`) stellt die neue **Gesamtdauer** ein.
  3. Darunter steht in einem Satz, was das bedeutet: „Die Aufgaben für die neuen Tage kommen
     dazu." · „Es endet früher. Die Aufgaben der späteren Tage bleiben gespeichert." ·
     „Unverändert."
  4. „Übernehmen" ist gesperrt, solange sich nichts geändert hat.
- **Daten:** Geschrieben: `Experiment.days`; bei Verlängerung neue `Task`-Sätze.
- **Ergebnis:** Die Dauer stimmt wieder mit dem überein, was Frank vorhat.
- **Fehlerfall:** Kommt die KI für die neuen Tage nicht durch, bleibt die Verlängerung stehen und
  die Aufgaben werden vorgemerkt (`F-56`, Merker `aufgaben:<id>`).
- **Regeln/Grenzen:**
  - **Ein laufendes Experiment lässt sich nicht kürzer machen als der Tag, an dem es steht.**
    Der Dialog sagt es: „Es läuft im Moment an Tag 4 — kürzer geht es nicht."
  - **Beim Kürzen wird nichts gelöscht.** Die Aufgaben der wegfallenden Tage bleiben in der
    Ablage; wird später wieder verlängert, sind sie unverändert da.
  - Höchstens **60 Tage** (`Ablage.MAX_TAGE`). Keine fachliche Grenze — eine Schranke gegen
    Vertipper („70" statt „7"), damit nicht siebzig Tagesspalten entstehen.
  - Der Grund für diese Funktion: Die KI schätzte die Dauer allein, und sie ließ sich danach
    **nirgends** berichtigen — aus „die nächsten sechs, sieben Tage" wurden zwei, und dabei blieb es.

### F-43 NEU — Dauer beim Anlegen selbst bestimmen

- **Auslöser:** Die **Tagewahl** in jeder Anlegefläche mit Dauer (`B-10` F-35, `B-05` F-18) sowie
  in den Dialogen von `F-42` und `F-44`.
- **Ablauf:** Ein Minus- und ein Plus-Knopf (je 48 dp, vollrund) links und rechts der Zahl; die
  Zahl steht in JetBrains Mono 15/20 in *Aktion*, darunter „Tag" oder „Tage". Eine Reihe
  Schnellsprünge darunter: **1 · 3 · 7 · 14 · 30**, der aktive in *Aktion gedeckt* mit Rand
  *Aktion*.
- **Daten:** Geht als `days` in das entstehende `Experiment` bzw. `WatchlistItem` ein.
- **Ergebnis:** Die Dauer kommt von Frank, nicht aus einer Schätzung.
- **Fehlerfall:** Keiner — reine Eingabe.
- **Regeln/Grenzen:** Anfangswert **3 Tage** (nicht 1) — er ist ein Anfangswert, keine Vorgabe.
  Grenzen 1 bis 60. **Auch ohne Netz gilt die gewählte Dauer** — sie kommt von Frank, nicht vom
  Netz; nur Titel, Beschreibung und Aufgabenliste werden dann nachgetragen.

### F-44 NEU — Ein Experiment weiterführen statt abzuschließen

- **Auslöser:** Der Knopf **„Weiterführen"** auf `B-03`, sobald eine Einschätzung vorliegt.
- **Ablauf:**
  1. Unter der Einschätzung steht eine Trennlinie und die Frage: **„Wie soll es weitergehen?"** —
     am letzten geplanten Tag: **„Der letzte geplante Tag ist erreicht. Wie soll es
     weitergehen?"**
  2. Vier Wege stehen zur Wahl:
     - **„Weiterführen"** — der Dialog fragt nach den **zusätzlichen** Tagen (Anfangswert 3),
       nicht nach der neuen Gesamtdauer: aus Franks Sicht geht es weiter, und „noch drei Tage"
       ist die Zahl, die er im Kopf hat. Darunter steht die Rechnung: „Danach läuft es 8 Tage."
     - **„Abschließen"** — `F-13`, mit der Lichtblüte (`E-16`).
     - **„Zwischenstand"** (am letzten Tag: **„Später entscheiden"**) — der Tag ist ausgewertet,
       das Experiment läuft unverändert weiter.
     - **„Nicht umgesetzt"** — `F-13`, zweiter Weg.
  3. Danach kehrt die App auf den Monitor zurück und meldet, was gilt: „Läuft weiter — jetzt
     8 Tage." · „Zwischenstand gespeichert. Es läuft weiter." · „Abgeschlossen. Die Auswertung
     steht im Logbuch."
- **Daten:** Geschrieben: `Experiment.days`, neue `Task`-Sätze für die neuen Tage.
- **Ergebnis:** **Die App beendet nichts von selbst.** Über das Ende entscheidet Frank.
- **Fehlerfall:** Wie `F-42`.
- **Regeln/Grenzen:** Zusätzliche Tage höchstens bis zur Gesamtgrenze von 60.

### F-45 NEU — Der Verlauf: jede Auswertung bleibt einzeln erhalten

- **Auslöser:** Beim Öffnen von `B-03` — unter der heutigen Eingabe.
- **Ablauf:**
  1. Unter der Überschrift **„VERLAUF · 4 AUFNAHMEN"** liegt jede je erzeugte Auswertung dieses
     Experiments als eigenes Klappfach, **die jüngste oben**. Sie steht offen da; die älteren
     sind zugeklappt.
  2. Zugeklappt zeigt ein Fach: **„Tag 2"** in *Aktion*, darunter **„13.08.2026, 19:41 Uhr"** in
     *Blass*, rechts ein Lautsprecher (`F-47`) und — falls es der Abschluss war — das Wort
     **„Abschluss"** in *Erledigt*.
  3. Aufgeklappt: **„WAS ICH ERZÄHLT HABE"** mit Franks vollem Wortlaut, darunter
     **„EINSCHÄTZUNG"** mit dem Text der KI.
- **Daten:** Gelesen: alle `Evaluation`-Sätze zu diesem Experiment. Geschrieben: nichts.
- **Ergebnis:** Nichts Eingesprochenes wird unsichtbar.
- **Fehlerfall:** Keiner — rein lokal, ohne Netz vollständig lesbar.
- **Regeln/Grenzen:**
  - **Es gibt keine „Auswertung des Tages" mehr.** Vorher suchte die App die Auswertung *dieses
    Kalendertages* und überschrieb sie: `ownText` ersetzt, `aiText` gelöscht. Wer nachts um halb
    eins erzählte und am folgenden Abend noch einmal — für ihn zwei verschiedene Tage — verlor
    die erste Aufnahme restlos, samt Einschätzung.
  - Ein Experiment ist ein **fortlaufender Vorgang**: jede Aufnahme ist ein Stand, und ein Stand
    wird nicht korrigiert, sondern ergänzt.
  - Fehlt einer alten Aufnahme die Uhrzeit (Bestand aus der Zeit vor `createdAt`), steht nur der
    Kalendertag da — eine erfundene Uhrzeit wäre schlimmer als keine.
  - Die Laufkarte im Monitor weist den Verlauf aus: „4 Auswertungen bisher — jede einzeln unter
    ‚Wie ist es gelaufen?'".

### F-46 NEU — Logbuch-Reiter *Auswertungen*

- **Auslöser:** Der dritte Reiter auf `B-07`. Die drei heißen kurz **„15 Tage" · „Langzeit" ·
  „Auswertungen"** — zu dritt bleibt je Reiter ein Drittel der Breite, und ein abgeschnittenes
  „Letzte 15 Ta…" sagt weniger als „15 Tage".
- **Ablauf:**
  1. Je Experiment ein **Fach** zum Aufklappen: Titel, darunter „4 Aufnahmen · zuletzt
     13.08.2026, 19:41 Uhr". Das jüngste Fach steht offen.
  2. Aufgeklappt liegen darin dieselben Klappfächer wie in `F-45`, die jüngste Aufnahme oben.
  3. Ein Experiment, das es nicht mehr gibt, heißt „Gelöschtes Experiment" — seine Auswertungen
     bleiben trotzdem lesbar.
- **Daten:** Gelesen: alle `Evaluation`-Sätze mit dem Titel ihres Experiments. Geschrieben: nichts.
- **Ergebnis:** Keine Auswertung ist mehr unauffindbar.
- **Fehlerfall:** Keiner — ohne Netz vollständig lesbar.
- **Regeln/Grenzen:** Der Reiter kam dazu, weil eine abgeschlossene Auswertung sonst nirgends
  mehr zu finden war: mit dem Abschluss verschwindet die Karte aus dem Monitor, und über sie
  führte der einzige Weg dorthin. **Die verdichtete Fassung im Tageseintrag ersetzt den Wortlaut
  nicht.** Untereinander gereiht wäre die Liste mit jedem Tag gewachsen und die Zugehörigkeit
  verlorengegangen — bei drei Experimenten über zwei Wochen stehen vierzig Auswertungen bunt
  gemischt; deshalb die Fächer. Leerer Zustand: „Noch keine Auswertung. Sie entsteht, wenn du
  erzählst, wie es gelaufen ist."

### F-47 NEU — Vorlesen an jeder Stelle

- **Auslöser:** Ein Lautsprecher an jedem Text, der aus Sprache entstanden ist oder zu Sprache
  taugt: an **jeder Gesprächsrunde** (`B-02`, eigene wie Antworten), an **jeder Auswertung im
  Verlauf** (`B-03`, `B-07`), an **jeder Erkenntnis** (`B-06`) und an **jedem Logbuch-Tag**
  (`B-07`, beide Reiter).
- **Ablauf:**
  1. Ein Druck liest den Text mit der in `B-08` gewählten Stimme vor.
  2. Ein **zweiter Druck auf denselben Knopf hält an**; ein Druck auf einen **anderen** wechselt
     zum neuen Text.
  3. Der gerade sprechende Knopf färbt sich in *Aktion* und legt die Fläche *Aktion gedeckt*
     darunter; sein Symbol wird zum **Stopp-Quadrat** — die Farbe trägt die Information nicht
     allein.
- **Daten:** Gelesen: der jeweilige Text. Geschrieben: nichts.
- **Ergebnis:** Die App ist auf Sprache gebaut — jetzt auch beim Zuhören.
- **Fehlerfall:** Wie `F-12`, mit dem Rückfall auf die Gerätestimme (`F-48`). Ist nichts da:
  „Hier ist nichts zum Vorlesen."
- **Regeln/Grenzen:** **Nur eine Wiedergabe gleichzeitig.** Jeder Knopf trägt eine eigene Kennung
  (etwa `erkenntnis-7`), damit auf einem Bildschirm mit vielen Lautsprechern jeder weiß, ob
  **er** gemeint ist. Endet eine Wiedergabe von selbst, hört der zugehörige Knopf auf zu
  leuchten — dafür sorgt **ein** Beobachter für alle, nicht einer je Druck.

### F-48 NEU — Stimme des Geräts als vierter Weg und Rückfallebene

- **Auslöser:** In `B-08` als vierter Anbieter wählbar: **„Stimme des Geräts (ohne Netz)"**.
  Zusätzlich springt sie **von selbst** ein, wenn einer der drei anderen Wege nicht durchkommt.
- **Ablauf:**
  1. Der eingestellte Weg kommt zuerst.
  2. Kommt er nicht durch — kein Netz, kein Schlüssel, abgelehnter Aufruf —, übernimmt die
     Sprachausgabe von Android, und Frank hört einen Satz dazu:
     „Google Chirp 3 HD: der Schlüssel wird abgelehnt. Ich lese mit der Stimme des Geräts vor."
  3. Dieser Satz ist ein **Hinweis**, keine Störung: er erscheint unten und verschwindet nach
     2600 ms von allein.
- **Daten:** `tts_provider = geraet`.
- **Ergebnis:** Vorlesen fällt nie ganz aus. Vorher endete jeder Fehlschlag in Stille und einer
  technischen Meldung.
- **Fehlerfall:** Antwortet auch das Gerät nicht: „Auch die Stimme des Geräts antwortet nicht.
  Sie lässt sich in den Android-Einstellungen unter ‚Sprachausgabe' einrichten."
- **Regeln/Grenzen:** Die Gerätestimme selbst hat **keinen** Rückfall. Androids Sprachausgabe
  kennt kein Fortsetzen — ein Druck beendet sie, statt zu pausieren. Sie wird **erst beim ersten
  Bedarf** eingerichtet, damit nicht jede App-Sitzung einen Dienst hochfährt, in der niemand
  vorliest.

### F-49 NEU — Zwischen Morgen und Abend umschalten

- **Auslöser:** Der Umschalter rechts oben auf `B-01`, neben dem Datum.
- **Ablauf:** Zwei Pillen **„Morgen"** und **„Abend"** in einer Spur `color-mix(Text 8%)`, je
  36 dp hoch. „Morgen" zeigt Lage und Vorschläge, „Abend" die Abendkarten der laufenden
  Experimente.
- **Daten:** Nur Anzeige.
- **Ergebnis:** Der Abend-Zustand ist erreichbar, ohne auf die Erinnerung zu warten.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** „Abend" zeigt nur etwas, wenn mindestens ein Experiment läuft. Die
  Abend-Erinnerung (`F-25`) führt auf denselben Zustand. Vorher fehlte die Ableitung des
  Zustands aus dem Merker: der Abend-Abschnitt erschien **nie**, auch nicht über die Erinnerung.

### F-50 NEU — Mikrofon-Erlaubnis erfragen und die Handlung fortsetzen

- **Auslöser:** Jeder erste Druck auf einen Sprechknopf ohne erteilte Erlaubnis.
- **Ablauf:**
  1. Das Modell setzt ein Signal, die Oberfläche stellt die **Systemfrage** und meldet die
     Antwort zurück.
  2. Bei Zustimmung läuft die **ursprünglich gewollte Handlung** weiter, ohne dass Frank noch
     einmal drücken muss.
  3. Bei Ablehnung: „Ohne Mikrofon kann ich dich nicht hören. Die Erlaubnis steht in den
     Systemeinstellungen."
- **Daten:** Keine.
- **Ergebnis:** Spracheingabe funktioniert überhaupt.
- **Fehlerfall:** Siehe oben; das Textfeld bleibt in jedem Fall benutzbar.
- **Regeln/Grenzen:** Genau das Anfragen fehlte bisher: die Aufnahmeschicht **prüfte** die
  Erlaubnis nur und gab `false` zurück — die App meldete „Ohne Mikrofon kann ich dich nicht
  hören", ohne sie je erfragt zu haben. Damit war **jede** Spracheingabe tot: `F-01`, `F-09`,
  `F-10`, `F-18`, `F-20`, `F-21` und `F-23`.

### F-51 NEU — Zurück: Stapel, Zurück-Taste und Wischgeste

- **Auslöser:** Der Zurück-Pfeil in der Kopfleiste, die Zurück-Taste des Geräts, die Wischgeste
  des Systems.
- **Ablauf:**
  1. Jeder Bildschirm, der aus einem anderen geöffnet wird (`B-02`, `B-03`, `B-08`, `B-09`),
     legt seine Herkunft auf einen **Stapel**; der Rückweg nimmt sie wieder herunter.
  2. Ein **Hauptbildschirm ist immer ein Neuanfang** — von dort führt der Rückweg nirgendwo hin,
     und alte Zwischenstationen wären nur Ballast; der Stapel wird geleert.
  3. Ist der Stapel leer, gilt der vernünftige Ausgang: von `B-09` nach `B-08`, von allem
     anderen auf den Monitor. **Auf dem Monitor verlässt die Zurück-Taste die App.**
  4. Ein offener Selbstbild-Text wird auch auf diesem Weg gesichert.
- **Daten:** Keine.
- **Ergebnis:** Kein Bildschirm ist mehr eine Sackgasse.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Der Stapel ist höchstens **8** tief; ein bereits enthaltenes Ziel wird bis
  zu seiner Stelle abgeräumt, damit kein Kreis entstehen kann. Vorher merkte sich die App genau
  *einen* Herkunftsbildschirm — ein einziger Besuch im Selbstbild machte den Zurück-Pfeil der
  Einstellungen zur Sackgasse, aus der nur half, die App aus dem Speicher zu werfen. Die
  Zurück-Geste wurde bis dahin **überhaupt nicht** behandelt.

### F-52 NEU — Selbstbild ausdrücklich sichern

- **Auslöser:** Der Knopf **„Speichern"** auf `B-09`; zusätzlich das Verlassen des Bildschirms
  und das Verlassen der App.
- **Ablauf:**
  1. Unter dem Feld steht der Stand: **„Noch nicht gespeichert."** in *Warnung* oder
     **„Gespeichert."** in *Erledigt*. Er ist ablesbar, nicht zu erraten.
  2. „Speichern" sichert und meldet „Selbstbild gespeichert." — oder „Schon gespeichert.", wenn
     sich nichts geändert hat.
  3. Beim Betreten wird der Text **direkt aus der Ablage** gelesen, nicht aus dem beobachteten
     Strom; ein noch ungesicherter Entwurf im Feld gewinnt und wird nicht überschrieben.
  4. Unten steht, wozu das gut ist: „Dein Selbstbild geht als erster Block in jede Anfrage ein —
     es prägt alle Vorschläge und Einschätzungen."
- **Daten:** Geschrieben: `SelfImage.text`.
- **Ergebnis:** Ein langer, eingesprochener Text geht nicht mehr verloren.
- **Fehlerfall:** Schlägt das Speichern fehl, **bleibt der Text im Feld stehen** und es erscheint
  eine Störung — nie stillschweigend verlieren.
- **Regeln/Grenzen:** **Drei Schichten**: der ausdrückliche Knopf, das Verlassen des Bildschirms
  (auch über die Zurück-Taste), das Verlassen der App. Vorher wurde einzig beim Druck auf den
  Zurück-Pfeil gespeichert.

### F-53 NEU — Eigene Stimmen verwalten

- **Auslöser:** `B-08`, Abschnitt „Stimme", sobald **„Meine Stimme"** gewählt ist.
- **Ablauf:**
  1. Die bei Alibaba registrierten Stimmen werden **abgerufen** und stehen als Auswahl mit
     Namen — ohne sie müsste Frank die 46 Zeichen lange Kennung auf dem Telefon eintippen.
  2. **„Neue Stimme aufnehmen"** nimmt eine Probe auf (zweiter Druck beendet) und registriert
     sie; die Liste wird danach sofort neu geholt.
  3. **„Neu laden"** holt die Liste erneut, **„Gewählte Stimme löschen"** entfernt sie bei
     Alibaba.
  4. Steht noch keine Stimme fest, wird die jüngste vorbelegt.
- **Daten:** Geschrieben: `tts_voice_qwen`.
- **Ergebnis:** Die eigene Stimme ist einrichtbar, ohne Kennungen abzutippen.
- **Fehlerfall:** Ohne Schlüssel bleibt die Liste leer und der Platzhalter sagt es („noch keine
  Stimme aufgenommen"). Fehler beim Laden erscheinen als Störung mit Grund.
- **Regeln/Grenzen:** Die **Stimmprobe wird in der höheren Aufnahmerate** aufgenommen, mit der
  das Klonen gut funktioniert — nicht in Diktier-Qualität. Diese Rate stand bereit, wurde aber
  nirgends übergeben.

### F-54 NEU — Gerätecode anzeigen, kopieren, Anmeldung abbrechen

- **Auslöser:** „Anmelden" in `B-08`, Abschnitt „Zugänge".
- **Ablauf:**
  1. Der Benutzercode erscheint in einer eigenen Karte (Radius 14 dp, Rand *Aktion* 55 %):
     **groß in JetBrains Mono 26/32 mit 3 sp Laufweite**, damit man ihn beim Abtippen nicht
     verliest. Er wird **wörtlich** angezeigt, nicht umformatiert.
  2. Daneben **„Kopieren"**; ein Druck auf den Code selbst kopiert ebenfalls — die Fläche ist
     größer als der Knopf. Bestätigung: „Code kopiert."
  3. Darunter die Adresse und **„Seite erneut öffnen"** — hat Frank den Browser geschlossen,
     kommt er hierüber zurück, ohne die Anmeldung neu zu starten (der Code bliebe sonst nicht
     derselbe).
  4. **„Abbrechen"** beendet die laufende Anmeldung; sonst wartet sie bis zu 15 Minuten weiter.
  5. Der Code bleibt sichtbar, **solange** die App auf die Bestätigung wartet.
- **Daten:** Geschrieben: die Anmeldung.
- **Ergebnis:** Die Geräteanmeldung ist durchführbar. Ohne den sichtbaren Code kann auf der
  OpenAI-Seite nichts eingetippt werden.
- **Fehlerfall:** Lässt sich die Seite nicht öffnen: „Die Seite ließ sich nicht öffnen: …".
- **Regeln/Grenzen:** Ist die App angemeldet, steht die Adresse dabei („angemeldet · name@…").
  Die Schlüsselfelder darunter tragen ein **Auge** zum Sichtbarmachen.

### F-55 NEU — Weckzeit an der Uhr stellen

- **Auslöser:** Antippen von Wecker-Symbol **oder** Uhrzeit in einer Erinnerungszeile — beides
  ist **eine** Tippfläche, denn das Symbol allein wäre mit 24 dp zu klein.
- **Ablauf:**
  1. Ein Dialog mit der **24-Stunden-Uhr** öffnet sich, weil die Zeiten als `HH:mm` gespeichert
     werden und Frank sie in der Zeile genauso wiederfindet.
  2. „Übernehmen" schreibt die Zeit, setzt die Weckzeiten neu und meldet „Morgens um 07:30 Uhr."
  3. **Wer die Uhr stellt, will geweckt werden:** war die Erinnerung aus, schaltet sie sich dabei
     ein — eine eingestellte Zeit ohne eingeschaltete Erinnerung wäre wirkungslos.
- **Daten:** Geschrieben: `reminder_*_time`, ggf. `reminder_*_on`.
- **Ergebnis:** Die Weckzeiten sind einstellbar.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Darf die App ab Android 12 keine **exakte** Weckzeit setzen, weckt sie
  ungenau statt gar nicht.

### F-56 NEU — Nachlauf: Liegengebliebenes wird nachgeholt

- **Auslöser:** Beim Start der App und bei jedem Tageswechsel (`F-57`), still im Hintergrund.
- **Ablauf:** Jeder Merker wird der Reihe nach abgearbeitet:
  | Merker | Was nachgeholt wird |
  |--------|---------------------|
  | `eigenes:<Text>` | Titel, Beschreibung, Dauer, Stufe und Aufgabenliste eines ohne Netz angelegten Experiments |
  | `aufgaben:<id>` | die Aufgaben der Tage, die nach einer Verlängerung noch leer sind |
  | `erkenntnis:<Text>` | eine Erkenntnis, die am Netz gescheitert ist |
  | `verdichtung:<Tag>` | die ausstehende Tagesverdichtung |
  | `log:<Tag>\|<Text>` | der Logbuch-Rohstoff — **in seinen eigenen Tag**, nicht in den heutigen |
- **Daten:** Geschrieben: was der jeweilige Schritt schreibt; der Merker wird gelöst.
- **Ergebnis:** „Es geht nichts verloren" stimmt jetzt wirklich.
- **Fehlerfall:** Was jetzt nicht geht, **bleibt stehen** und kommt beim nächsten Mal wieder
  dran. Kein Eintrag wird verworfen, ohne erledigt zu sein.
- **Regeln/Grenzen:** Der Merker wurde an fünf Stellen gesetzt, und überall stand im Kommentar
  „läuft beim nächsten Start nach". **Nachgeholt hat es nie jemand** — die Einträge sammelten
  sich an, ohne je wieder gelesen zu werden. Ein eigenes Experiment ohne Netz blieb für immer
  ohne Aufgabenliste. Beim Rohstoff trägt der Merker jetzt sein **Datum**: was am Vorabend am
  fehlenden Netz scheiterte, wurde vorher in den Eintrag des *nächsten* Tages geschrieben — der
  Vortag blieb für immer leer und sein Erlebtes stand unter falschem Datum.

### F-57 NEU — Tageswechsel im laufenden Betrieb

- **Auslöser:** Jede Rückkehr der App in den Vordergrund.
- **Ablauf:** Hat der Kalendertag gewechselt, wird alles Tagesbezogene nachgezogen: das Datum,
  der Abend-Merker, das Lage-Feld, die fällige Verdichtung (`F-15`), der Nachlauf (`F-56`) und
  der Zustand von `B-01`. Die tagesbezogenen Datenströme hängen am Datum und zeigen sofort den
  neuen Tag.
- **Daten:** Wie die angestoßenen Schritte.
- **Ergebnis:** Wer die App abends offen liegen lässt und morgens weitermacht, schreibt in den
  **richtigen** Tag.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Der heutige Tag war vorher eine feste Größe, einmal beim Start gesetzt —
  Lage, Auswertung und Logbuch landeten noch immer im **Vortag**, und die Tagesverdichtung hielt
  sich für erledigt.

### F-58 NEU — Anlegefläche beiseitelegen, ohne zu verwerfen

- **Auslöser:** Ein Druck **neben** die Anlegefläche (auf den weichgezeichneten Grund).
- **Ablauf:** Die Fläche schließt, **der Text bleibt** und steht beim nächsten Öffnen wieder da.
- **Daten:** Keine.
- **Ergebnis:** Eine fertige Transkription geht nicht durch einen Fehlgriff verloren.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Verworfen wird **nur** auf den ausdrücklichen Knopf „Abbrechen" (`F-33`).
  Genau daran ging eine fertige Transkription verloren: der Druck galt eigentlich einer Störung,
  die unsichtbar hinter der Fläche lag. Seither liegen die Meldungen **über** der Anlegefläche.

---

## 3. Datenmodell

Alles in einer lokalen Room-Datenbank, außer den Einstellungen.

### `SelfImage` — das Selbstbild (genau ein Satz)

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Int | ja | 1 | Room |
| `text` | String | ja | „" | Room |
| `updatedAt` | Instant | ja | jetzt | Room |

### `Goal` — Wünsche & Ziele

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `text` | String | ja | — | Room |
| `createdAt` | Instant | ja | jetzt | Room |
| `updatedAt` | Instant | ja | jetzt | Room |

### `LogDay` — ein Tag im Logbuch

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `date` | LocalDate | ja | — (Schlüssel) | Room |
| `detailText` | String? | nein | null | Room — ausführlich, 20–30 Zeilen; null nach Verdichtung |
| `compactText` | String? | nein | null | Room — verdichtet, ≤ 7 Zeilen; null solange ausführlich |
| `compactedAt` | Instant? | nein | null | Room |

Genau eines von `detailText` / `compactText` ist gesetzt. Reiter *Letzte 15 Tage* zeigt die
Sätze mit `detailText`, Reiter *Langzeit* die mit `compactText`.

### `Lage` — die eingesprochene Lage eines Tages

> **Ergänzt in dieser Fassung.** F-01 schreibt seit jeher einen `SituationEntry`, der im
> Datenmodell fehlte. Er wird hier nachgetragen, damit beim Bauen keine Einheit erfunden
> werden muss.

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `date` | LocalDate | ja | — (Schlüssel) | Room |
| `text` | String | ja | — | Room |
| `createdAt` | Instant | ja | jetzt | Room |

Pro Tag genau ein Satz; eine erneute Eingabe am selben Tag ersetzt ihn (F-01).

### `Suggestion` — ein Vorschlag des Tages

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `date` | LocalDate | ja | — | Room |
| `title` | String | ja | — | Room |
| `description` | String | ja | — | Room |
| `days` | Int | ja | 1 | Room |
| `level` | Enum (LEICHT, MITTEL, FORDERND) | ja | — | Room |
| `fromWatchlist` | Boolean | ja | false | Room |
| `tasksJson` | String | ja | — | Room — Aufgaben je Tag |
| `discardedAt` | Instant? | nein | null | Room — gesetzt beim Aktualisieren |

### `Experiment` — ein anstehendes, laufendes oder abgeschlossenes Experiment

> **Geändert in dieser Fassung:** neuer Zustand `ANSTEHEND`, neue Felder `origin`, `order`
> und `addedAt`; `startedAt` ist jetzt leer, solange das Experiment nur ansteht.

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `title` | String | ja | — | Room |
| `description` | String | ja | — | Room |
| `days` | Int | ja | 1 | Room |
| `level` | Enum | ja | — | Room |
| `origin` | Enum (KI_VORSCHLAG, EIGEN, MERKLISTE) | ja | — | Room — **neu**, trägt das Herkunftsetikett im Monitor |
| `addedAt` | Instant | ja | jetzt | Room — **neu**, wann es in den Monitor kam |
| `order` | Int | ja | 0 | Room — **neu**, Franks Reihenfolge unter „Steht an" (F-38) |
| `startedAt` | LocalDate? | nein | null | Room — **geändert**, erst beim Start gesetzt (F-37) |
| `state` | Enum (ANSTEHEND, LAEUFT, ABGESCHLOSSEN, NICHT_UMGESETZT) | ja | ANSTEHEND | Room — **geändert**, `OFFEN` heißt jetzt `LAEUFT` |
| `closedAt` | LocalDate? | nein | null | Room |

**Wo im Spec „offenes Experiment" steht, ist `LAEUFT` gemeint** — also eines, das gestartet
wurde und ausgewertet werden will. Anstehende Experimente sind nicht „offen" in diesem Sinne:
sie erscheinen nicht in der Abend-Auswertung und zählen nicht gegen die Grenze von drei.

### `Task` — eine Aufgabe eines Experiments

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `experimentId` | Long | ja | — | Room |
| `dayIndex` | Int | ja | 1 | Room — 1 = erster Tag |
| `text` | String | ja | — | Room |
| `doneAt` | Instant? | nein | null | Room |
| `order` | Int | ja | — | Room |

### `Evaluation` — **eine einzelne** Auswertung zu einem Experiment

> **Geändert in v2.** Zwei neue Felder, und die Bedeutung der Einheit selbst hat sich geändert:
> **Jede Aufnahme ist eine eigene Zeile — es gibt keine „Auswertung des Tages" mehr** (`F-45`).

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `experimentId` | Long | ja | — | Room |
| `date` | LocalDate | ja | — | Room |
| `ownText` | String | ja | — | Room — Franks eigene Worte |
| `aiText` | String? | nein | null | Room — die KI-Auswertung |
| `isFinal` | Boolean | ja | false | Room — true am letzten Tag |
| `createdAt` | Instant? | nein | null | Room — **neu**, der Zeitpunkt der Aufnahme; an einem Tag können mehrere entstehen, und ohne Uhrzeit stünden sie ununterscheidbar untereinander. `null` beim Bestand aus der Zeit davor |
| `dayIndex` | Int? | nein | null | Room — **neu**, der wievielte Versuchstag es war. Er lässt sich später nicht mehr ausrechnen, weil die Dauer nachträglich änderbar ist (`F-42`) |

### `ChatTurn` — eine Runde im Faden

> **Geändert in v2.** Der Faden trägt **zweierlei**: das freie Gespräch (`F-09`) und die
> Auswertungen (`F-10`/`F-11`). Für die KI gehört beides zusammen — sie soll beim Antworten alles
> kennen. Auf dem Bildschirm gehört es das nicht: die Auswertung eines Tages ist kein
> Gesprächsbeitrag. Das neue Feld `art` trennt die **Anzeige**, ohne den Faden zu zerreißen.

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `experimentId` | Long | ja | — | Room |
| `role` | Enum (ICH, KI) | ja | — | Room |
| `text` | String | ja | — | Room |
| `createdAt` | Instant | ja | jetzt | Room |
| `art` | Enum (GESPRAECH, AUSWERTUNG) | ja | GESPRAECH | Room — **neu**; `B-02` zeigt nur `GESPRAECH`, die KI bekommt beides |

### `WatchlistItem` — ein Eintrag der Merkliste

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `title` | String | ja | — | Room |
| `description` | String | ja | — | Room |
| `days` | Int | ja | 1 | Room |
| `level` | Enum | ja | — | Room |
| `tasksJson` | String | ja | — | Room |
| `source` | Enum (GEMERKT, EIGEN, NICHT_UMGESETZT) | ja | — | Room |
| `note` | String? | nein | null | Room — was beim letzten Mal im Weg stand |
| `createdAt` | Instant | ja | jetzt | Room |

### `Insight` — eine Erkenntnis

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `text` | String | ja | — | Room |
| `updatedAt` | Instant | ja | jetzt | Room |

### Einstellungen (`EncryptedSharedPreferences`, AES-256)

| Schlüssel | Typ | Standard |
|-----------|-----|----------|
| `model_experiments` | String | `gpt-5.6-terra` |
| `effort_experiments` | String | `high` |
| `model_logbook` | String | `gpt-5.6-luna` |
| `effort_logbook` | String | `medium` |
| `tts_provider` | String | `google_cloud` |
| `tts_voice_google` | String | `de-DE-Chirp3-HD-Kore` |
| `tts_voice_edge` | String | `de-DE-SeraphinaMultilingualNeural` |
| `tts_voice_qwen` | String | „" |
| `tts_rate` | Float | 1.0 |
| `groq_api_key` | String | „" |
| `google_tts_api_key` | String | „" |
| `qwen_api_key` | String | „" |
| `reminder_morning_on` | Boolean | false |
| `reminder_morning_time` | String | `08:00` |
| `reminder_evening_on` | Boolean | false |
| `reminder_evening_time` | String | `20:30` |
| `theme` | String (`light` · `dark` · `system`) | `dark` |
| `effect_level` | String (`voll` · `gedaempft` · `aus`) | `voll` — F-41 |
| `ausstehend` | Set&lt;String&gt; | leer — **neu in v2**, die Merker des Nachlaufs (F-56) |
| `verdichtet_am` | String | „" — **neu in v2**, der Tag, an dem F-15 zuletzt lief |
| `hinweis_benachrichtigung` | Boolean | false — **neu in v2**, der einmalige Hinweis bei abgelehnten Benachrichtigungen |

> **Geändert in v2:** `tts_provider` ist jetzt auf **`edge_tts`** vorbelegt, nicht mehr auf
> `google_cloud`. Google kann ohne hinterlegten Schlüssel **niemals** sprechen — wer die App
> frisch benutzte, bekam auf jeden Druck auf einen Lautsprecher eine Fehlermeldung. Edge braucht
> nur Netz. Verkürzte Kennungen aus einer früheren Fassung (`qwen`, `edge`) stehen auf dem Gerät
> noch in den Einstellungen und werden beim **Lesen einmalig geradegezogen** (`qwen_clone`,
> `edge_tts`), statt als „unbekannt" zu gelten und stumm bei der falschen Stimme zu landen.

### Die Datenbank

Room, **Version 4**, mit drei Wanderungen, die den Bestand mitnehmen:

| Von → nach | Was sie tut |
|------------|-------------|
| 1 → 2 | Die Tabelle `experimente` wird neu gebaut: `origin`, `addedAt`, `order_index` kommen dazu, `startedAt` wird nullbar, `OFFEN` heißt `LAEUFT` |
| 2 → 3 | `auswertungen` bekommt `createdAt` |
| 3 → 4 | `auswertungen` bekommt `dayIndex`, `gespraech` bekommt `art` (Vorgabe `GESPRAECH`) |

`order` heißt in SQL `order_index`, weil `ORDER` ein Schlüsselwort ist — der Spec-Name bleibt am
Kotlin-Feld erhalten.

---

## 4. Zustände und Übergänge

### Der Monitor B-10 — der Startbildschirm

```
      ┌──────────────── F-35 (selbst anlegen) ───────────────┐
      │                                                      ▼
   B-01: VORSCHLAEGE ──F-36 (übernehmen)──▶  STEHT AN  ──F-37──▶  LÄUFT
                                                │                     │
                                       F-39 (entfernen)      F-13 (abschließen)
                                                │                     │
                                                ▼                     ▼
                                          Merkliste /            Chronik
                                           gelöscht          (+ ggf. Merkliste)
```

| Zustand | Was B-10 zeigt |
|---------|----------------|
| `LEER` | Beide Abschnitte leer, die Sätze aus Teil B §8, der Plus-Knopf atmet |
| `NUR_ANSTEHEND` | Nur „Steht an"; „Läuft" ist ganz ausgeblendet |
| `LAEUFT` | Beide Abschnitte, dazu die eine To-Do-Liste des Tages (F-07) |
| `VOLL` | Drei laufen — alle „Starten"-Knöpfe gesperrt, mit Begründung |
| `ANLEGEN` | Die Anlegefläche liegt über dem Bildschirm (F-35) |

### Der Tag auf B-01

```
LEER  ──F-01──▶  LAGE_STEHT  ──F-03──▶  VORSCHLAEGE  ──F-36──▶  (in den Monitor)
  ▲                                          │  ▲                        │
  │                                     F-04 └──┘                   F-06 │ (jetzt starten)
  │                                                                      ▼
  └────────────────── neuer Kalendertag ◀────────────────────────── (B-10 zeigt es)
```

| Zustand | Was B-01 zeigt |
|---------|----------------|
| `LEER` | Frage „Wie ist deine Lage heute?" und der Sprechknopf |
| `LAGE_STEHT` | Der Lage-Text, bearbeitbar, mit „Text mit KI verbessern" und „Weiter" |
| `VORSCHLAEGE` | Fünf Karten, je mit „In den Monitor" und „Jetzt starten", darunter „Andere Vorschläge" |
| `ABEND` | Zusätzlich der Hinweis auf die Auswertung (führt zu B-03) |

Laufen bereits drei Experimente, wird `VORSCHLAEGE` übersprungen — B-01 zeigt nach
`LAGE_STEHT` den Hinweis, dass drei Experimente laufen, und verweist auf den Monitor.
**Der Zustand `LAEUFT` ist von B-01 auf B-10 gewandert**: Laufende Experimente und die
To-Do-Liste stehen jetzt im Monitor.

### Ein Experiment

```
ANSTEHEND ──F-37 (starten)──▶ LAEUFT ──letzte volle Auswertung──▶ ABGESCHLOSSEN
    │                            │
    │                            └──„Nicht umgesetzt"──▶ NICHT_UMGESETZT
    │                                                       │
    └──F-39──▶ Merkliste oder gelöscht                      └──▶ zusätzlich zurück auf die Merkliste
```

`ANSTEHEND` ist der neue Anfangszustand: Jedes Experiment entsteht im Monitor, egal ob es aus
einem KI-Vorschlag stammt (F-36) oder selbst angelegt wurde (F-35). Nur `F-06` („Jetzt
starten") überspringt ihn.

### Ein Logbuch-Tag

```
AUSFUEHRLICH (detailText, 20–30 Zeilen)
      │
      │  F-15, sobald älter als 15 Tage — nur bei erfolgreicher Verdichtung
      ▼
VERDICHTET (compactText, ≤ 7 Zeilen) — dauerhaft
```

---

## 5. Externe Dienste

| Dienst | Wofür | Schlüssel/Anmeldung | Verhalten ohne Netz |
|--------|-------|---------------------|---------------------|
| Codex (OpenAI) | F-02, F-03, F-04, F-09, F-11, F-14, F-15, F-17 | OAuth-Geräteanmeldung über das ChatGPT-Abo, Verfahren aus `CodexAuthManager.kt` | Klare Meldung; F-14/F-15/F-17 holen beim nächsten Lauf nach |
| Groq | Transkription aller Spracheingaben, `whisper-large-v3-turbo`, `de`, `temperature=0`, `verbose_json`, max 25 MB | API-Schlüssel (B-08) | Aufnahme bleibt erhalten, erneuter Versuch möglich |
| Google Cloud TTS | Vorlesen mit Chirp 3 HD, `text:synthesize`, `de-DE`, MP3 | API-Schlüssel (B-08) | Text bleibt lesbar |
| Alibaba DashScope | Vorlesen mit Franks eigener Stimme; Registrierung über `qwen-voice-enrollment`, Ziel `qwen3-tts-vc-2026-01-22` | API-Schlüssel (B-08) | Text bleibt lesbar |
| Microsoft Edge TTS | Vorlesen mit Standardstimmen (6 deutsche), **Vorbelegung** | keiner | Text bleibt lesbar |
| **Stimme des Geräts** (Android) | **neu in v2** — vierter Weg **und** Rückfallebene aller anderen (F-48) | keiner | **funktioniert ohne Netz** |

**Zwei Filterstufen vor jeder Transkription**, übernommen aus PerfectMoment:
`SpeechAnalyzer` verwirft zu stille Aufnahmen **vor** dem Hochladen;
`WhisperHallucinationFilter` entfernt danach typische Halluzinationen.

---

## 6. Hintergrund und Lebenszyklus

- **Beim ersten Öffnen an einem neuen Kalendertag** läuft F-15 (Verdichtung), bevor
  irgendetwas anderes angezeigt wird. Dauert das länger als 400 ms, zeigt sich der
  Wartezustand.
- **Erinnerungen** laufen über `AlarmManager` mit exakten Weckzeiten und werden nach jedem
  Neustart des Geräts neu gesetzt (`BOOT_COMPLETED`).
- **Geht die App in den Hintergrund**, wird eine laufende Aufnahme beendet und verworfen,
  eine laufende Wiedergabe gestoppt. Laufende KI-Anfragen laufen zu Ende und ihr Ergebnis
  wird gespeichert.
- **Nach dem Beenden** läuft nichts weiter außer den Erinnerungen. Es gibt keinen
  Hintergrunddienst, keine Synchronisierung, kein Nachladen.
- **Nicht gelungene Hintergrundschritte** (F-14, F-15, F-17, F-35, F-42) merken sich, dass sie
  ausstehen, und werden vom **Nachlauf** (`F-56`) beim nächsten Start und bei jedem Tageswechsel
  wirklich nachgeholt. Es geht nichts verloren.
- **Wechselt der Kalendertag, während die App im Hintergrund liegt**, wird beim Zurückkehren
  alles Tagesbezogene nachgezogen (`F-57`).
- **Die Abend-Erinnerung öffnet die App über `FLAG_ACTIVITY_CLEAR_TOP`.** Läuft sie bereits, gibt
  es dabei **kein** `onCreate` — die Absicht wird deshalb auch beim erneuten Eintreffen
  ausgewertet, und ihr Merker danach entfernt: sonst sprang die App bei jeder Neuerzeugung der
  Activity (Systemtheme-Wechsel, Drehen, Aufklappen des Foldables) zurück in den Abend, auch
  mitten am Tag.
- **Die Haptik darf nie etwas zum Absturz bringen.** Fehlt die Erlaubnis oder hat das Gerät
  keinen Vibrator, wird das vermerkt und weitergemacht. (Genau daran ist die App gestorben:
  `VIBRATE` fehlte im Manifest, das Rütteln warf eine SecurityException — und riss die Aufnahme
  mit, obwohl mit der Aufnahme alles in Ordnung war.)

---

## 7. Offene Fragen

Keine.

**Annahmen dieser Fassung**, die beim Bauen gelten, falls Frank nichts anderes sagt:

1. **Die Grenze von drei gilt fürs Laufen, nicht fürs Vormerken.** Der Monitor darf beliebig
   viele anstehende Experimente enthalten; höchstens drei davon laufen gleichzeitig. Alles
   andere hätte den Monitor zu einer zweiten Merkliste mit Deckel gemacht.
2. **„Heute" (B-01) bleibt vollständig erhalten** und verliert nur die Rolle als
   Startbildschirm und die Anzeige der laufenden Experimente.
3. **Übernehmen ist nicht starten.** Ein KI-Vorschlag landet erst im Monitor und wird dort
   bewusst gestartet. Der alte Sofort-Weg bleibt als „Jetzt starten" erhalten.
4. **Die Merkliste bleibt bestehen** — sie ist der Ort für Ideen *ohne* Vorsatz, der Monitor
   der für Vorhaben *mit* Vorsatz.

**Vier Annahmen sind in v2 dazugekommen:**

5. **Die App beendet nichts von selbst.** Weder die Auswertung am letzten Tag noch der Kalender
   schließt ein Experiment ab — das tut nur Frank (`F-44`, `F-13`).
6. **Nichts Eingesprochenes wird je überschrieben.** Jede Aufnahme ist ein eigener Stand
   (`F-45`). Eine Korrektur ist eine Ergänzung.
7. **Die Dauer gehört Frank, nicht der Schätzung** (`F-42`, `F-43`).
8. **Kein Weg endet in Stille.** Wo eine Stimme, ein Netz oder ein Schlüssel fehlt, gibt es eine
   Rückfallebene und einen Satz dazu (`F-48`, `F-56`).

## Neu aus dem Design

Beim Gestalten sind 7 Bedienelement(e) dazugekommen, die es im Erst-Spec noch nicht gab.
Davon 6 mit beschriebener Aufgabe, 1 ohne.
Jedes bekommt eine Kennung, damit der Umsetzer es nicht als Attrappe baut.

| Kennung | Bildschirm | Element | Art | Aufgabe |
|---------|------------|---------|-----|---------|
| F-27 | Heute | Hellmodus einschalten | button | F-26 schaltet nacheinander auf Hellmodus, Dunkelmodus und Automatik um, zeigt dafür Sonne, Mondsichel oder A mit seitlichen Strahlen und lässt Automatik sofort sowie bei späteren Systemwechseln der Systemdarstellung folgen |
| F-28 | Heute | Lieber tippen | button | Lage tippen |
| F-29 | Auswertung | Auswertungstext | textarea | Auswertungstext bearbeiten |
| F-30 | Auswertung | Überspringen | button | Zum nächsten Experiment springen |
| F-31 | Logbuch | Letzte 15 Tage | button | Letzte 15 Tage anzeigen |
| F-32 | Logbuch | Langzeit | button | Langzeit anzeigen |
| F-33 | Merkliste | Abbrechen | button | **Geklärt in dieser Fassung:** schließt die Anlegefläche, ohne zu speichern; der eingesprochene Text wird verworfen. Gilt gleichlautend auf B-04 und auf der neuen Anlegefläche von B-10 |

> Zu den offenen Punkten wurde im Studio nicht gesagt, was sie tun sollen. Sie werden beim
> Rückimport erfragt und **nicht erfunden** — sonst entstünde beim Bauen ein toter Knopf.
>
> **Stand dieser Fassung: keiner ist mehr offen.** F-33 ist oben geklärt.

## Navigation aus dem Design

Diese Elemente haben im Design bereits ein Ziel und brauchen keine eigene Funktion.

> **Neu in dieser Fassung:** Die untere Leiste hat **sechs** Felder statt fünf. Ganz links
> steht **Monitor** (`B-10`), danach unverändert Heute, Ziele, Merkliste, Erkenntnisse,
> Logbuch. Die Leiste ist auf **jedem** Hauptbildschirm gleich, und F-27 (Wischen) läuft über
> alle sechs in genau dieser Reihenfolge.

| Bildschirm | Element | führt zu |
|------------|---------|----------|
| Monitor | Einstellungen | `B-08` |
| Monitor | Gespräch zum Experiment | `B-02` |
| Monitor | Wie ist es gelaufen? | `B-03` |
| Monitor | Monitor | `B-10` |
| Monitor | Heute | `B-01` |
| Monitor | Ziele | `B-04` |
| Monitor | Merkliste | `B-05` |
| Monitor | Erkenntnisse | `B-06` |
| Monitor | Logbuch | `B-07` |
| Heute | Monitor | `B-10` |
| Gespräch | Zurück zum Monitor | `B-10` |
| Auswertung | Zurück zum Monitor | `B-10` |
| Erkenntnisse | Monitor | `B-10` |
| Logbuch | Monitor | `B-10` |
| Merkliste | Monitor | `B-10` |
| Wünsche &amp; Ziele | Monitor | `B-10` |
| Heute | Einstellungen | `B-08` |
| Heute | Gespräch zum Experiment | `B-02` |
| Heute | Wie ist es gelaufen? | `B-03` |
| Heute | Heute | `B-01` |
| Heute | Ziele | `B-04` |
| Heute | Merkliste | `B-05` |
| Heute | Erkenntnisse | `B-06` |
| Heute | Logbuch | `B-07` |
| Gespräch | Zurück zu Heute | `B-01` |
| Auswertung | Zurück | `B-01` |
| Auswertung | Fertig | `B-01` |
| Einstellungen | Selbstbild | `B-09` |
| Selbstbild | Zurück zu Einstellungen | `B-08` |
| Erkenntnisse | Heute | `B-01` |
| Erkenntnisse | Ziele | `B-04` |
| Erkenntnisse | Merkliste | `B-05` |
| Erkenntnisse | Erkenntnisse | `B-06` |
| Erkenntnisse | Logbuch | `B-07` |
| Logbuch | Heute | `B-01` |
| Logbuch | Ziele | `B-04` |
| Logbuch | Merkliste | `B-05` |
| Logbuch | Erkenntnisse | `B-06` |
| Logbuch | Logbuch | `B-07` |
| Merkliste | Heute | `B-01` |
| Merkliste | Ziele | `B-04` |
| Merkliste | Merkliste | `B-05` |
| Merkliste | Erkenntnisse | `B-06` |
| Merkliste | Logbuch | `B-07` |
| Wünsche &amp; Ziele | Heute | `B-01` |
| Wünsche &amp; Ziele | Ziele | `B-04` |
| Wünsche &amp; Ziele | Merkliste | `B-05` |
| Wünsche &amp; Ziele | Erkenntnisse | `B-06` |
| Wünsche &amp; Ziele | Logbuch | `B-07` |
