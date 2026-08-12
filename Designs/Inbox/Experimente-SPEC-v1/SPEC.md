# Experimente — Spec v1 (überarbeitet: Monitor + Effekte)
Stand: 12.08.2026, 12.06 Uhr · Plattform: Android (Kotlin / Jetpack Compose)
Erzeugt von: Werft Studio beim Herunterladen · überarbeitet von Claude Code am 12.08.2026

Diese Datei ist die Zusammenstellung aller vier Specs. Die Einzeldateien daneben sind wortgleich.

**Was in dieser Fassung neu ist:** der Monitor `B-10` als Startbildschirm, die Funktionen
`F-34` bis `F-41`, der Effekt-Katalog `E-01` bis `E-24` in Teil B §7, die Bewegungen `M-76`
bis `M-95` und die Abnahmekriterien `A-21` bis `A-30`. Einzelheiten in Teil D §0.

## Teil A — Funktions-Spec

# Funktions-Spec — Experimente

Stand: 12.08.2026, 12.06 Uhr · Stufe: v1, ueberarbeitet (Monitor + Effekte) · Plattform(en): Android

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
| **F-41 NEU** | **Effekt-Stärke einstellen** | **B-08** | **Kern** |

F-01 bis F-26 gehören zur ersten Fassung. F-27 bis F-33 kamen aus dem Design dazu.
**F-34 bis F-41 sind neu in dieser Fassung** und tragen den Monitor und die Effekte.
Es gibt kein „später" — alles ist Kern.

---

## 2. Funktionen im Einzelnen

### F-01 — Lage einsprechen

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

- **Auslöser:** Plus-Knopf auf B-05.
- **Ablauf:** Sprechknopf → Transkription (wie F-01) → bearbeitbares Feld mit F-02 →
  Speichern. Dauer und Stufe schätzt die KI beim Speichern mit.
- **Daten:** Geschrieben: `WatchlistItem` (Quelle: `eigen`).
- **Ergebnis:** Eine eigene Idee liegt für später bereit und kommt über den
  Merklisten-Platz (F-03) zurück.
- **Fehlerfall:** Wie F-01.
- **Regeln/Grenzen:** Eigene Einträge sind gleichwertig mit gemerkten Vorschlägen.

### F-19 — Merkliste: Eintrag löschen

- **Auslöser:** Wischen oder langer Druck auf einen Merklisten-Eintrag.
- **Ablauf:** Rückfrage, dann Löschung.
- **Daten:** Geschrieben: Löschung des `WatchlistItem`.
- **Ergebnis:** Der Eintrag kommt nicht mehr als Merklisten-Platz zurück.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Endgültig.

### F-20 — Wünsche & Ziele pflegen

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

- **Auslöser:** B-08, Abschnitt „Erscheinung", oder der Schnellschalter in der oberen Leiste von B-10 und B-01.
- **Ablauf:**
  1. In B-08 stehen die drei Möglichkeiten **Hell** · **Dunkel** · **Wie das System** direkt zur Wahl. Vorbelegung: **Dunkel**.
  2. Der Schnellschalter durchläuft sie in der festen Reihenfolge **Hell → Dunkel → Wie das System → Hell**. Sein Symbol kündigt den nächsten Modus eindeutig an: eine Sonne für Hell, eine klar gezeichnete Mondsichel für Dunkel und ein großes „A“ mit jeweils drei seitlichen Strahlen für Automatik.
  3. **Wie das System** übernimmt sofort die aktuelle Systemdarstellung, speichert den eigentlichen Moduswert `system` statt nur der gerade aufgelösten Farbe und folgt ohne Neustart jedem späteren Wechsel des Systems, solange dieser Modus aktiv bleibt.
- **Daten:** Geschrieben: Theme-Wert in den verschlüsselten Einstellungen.
- **Ergebnis:** Die Erscheinung wechselt sofort, ohne Neustart, und bleibt zwischen App-Starts erhalten.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Beide Erscheinungen sind gleichrangig und vollständig gebaut; der Automatikmodus muss sowohl beim Einschalten als auch bei späteren Systemwechseln wirksam sein.

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

### `Evaluation` — die Auswertung eines Tages zu einem Experiment

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `experimentId` | Long | ja | — | Room |
| `date` | LocalDate | ja | — | Room |
| `ownText` | String | ja | — | Room — Franks eigene Worte |
| `aiText` | String? | nein | null | Room — die KI-Auswertung |
| `isFinal` | Boolean | ja | false | Room — true am letzten Tag |

### `ChatTurn` — eine Runde im Gespräch

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `experimentId` | Long | ja | — | Room |
| `role` | Enum (ICH, KI) | ja | — | Room |
| `text` | String | ja | — | Room |
| `createdAt` | Instant | ja | jetzt | Room |

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
| `effect_level` | String (`voll` · `gedaempft` · `aus`) | `voll` — **neu**, F-41 |

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
| Microsoft Edge TTS | Vorlesen mit Standardstimmen | keiner | Text bleibt lesbar |

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
- **Nicht gelungene Hintergrundschritte** (F-14, F-15, F-17) merken sich, dass sie
  ausstehen, und laufen beim nächsten Start nach. Es geht nichts verloren.

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

## Teil B — UI-Spec

# UI-Spec — Experimente-SPEC-v1
Stand: 12.08.2026, 12.06 Uhr · Stufe: v1, ueberarbeitet (Monitor + Effekte) · Plattform: Android (Kotlin / Jetpack Compose)


Alle Werte sind **deterministisch aus dem Design gemessen**, nicht geschätzt. Sie sind verbindlich.

## 1. Gestalterische Grundhaltung

**Warm im Grundton, modern in der Erscheinung, reich an Bewegung.** Die Farbwelt und die
Serifen-Überschrift bleiben unverändert — sie sind das Gesicht der App. Was sich ändert, ist
die Oberfläche darüber: Sie ist nicht mehr still, sondern lebendig. Glasflächen, ein langsam
wandernder Lichtgrund, farbiger Schein an allem, was gerade wichtig ist, Federphysik unter
jedem Druck, Partikel an den Wendepunkten des Tages.

Frank hat das ausdrücklich so gewollt: **maximale Effekte, überall.** Die frühere Fassung
dieses Abschnitts forderte das Gegenteil („keine Diagramme, keine Zähler … beruhigt sie, oder
drängt sie?"). Sie ist damit **überholt** und gilt nicht mehr. Was aus ihr bleibt, sind zwei
Punkte, die nichts mit Effekten zu tun haben:

- **Die App misst Frank nicht.** Keine Punkte, keine Serien, keine Abzeichen, keine Noten.
  Effekte feiern einen Moment — sie bewerten keine Leistung. Der Funke beim Start eines
  Experiments ist Freude, kein Belohnungssystem.
- **Kein Effekt trägt Information allein.** Alles, was ein Leuchten, eine Bewegung oder ein
  Partikel aussagt, steht zusätzlich als Text oder Form da. Auf der Stufe *Aus* (F-41) ist die
  App vollständig bedienbar.

Die neue Messlatte für jede gestalterische Entscheidung lautet: *Sieht das aus wie eine App
von heute — und bleibt es dabei ruhig genug, dass man morgens um sieben hineinschauen mag?*

**Der Monitor (B-10) ist das Gesicht der App.** Er wird beim Öffnen als Erstes gesehen und
bekommt deshalb die aufwendigste Behandlung: der Lichtgrund, die Glasleisten, die
Kipp-Parallaxe der Karten, der wandernde Lichtsaum um alles, was läuft.

Die vollständige Liste der verbindlichen Effekte steht in **§7 Effekte**.

---

## 2. Erscheinungen (Themes)

### 2.1 Dunkel (Standard) — `21dunkelstandard` (dark)

| Rolle | Wert |
|-------|------|
| `Grund` | `#151210` |
| `Fläche` | `#201B17` |
| `Erhöht` | `#2A231D` |
| `Rand` | `#38302A` |
| `Rand weich` | `#2C251F` |
| `Text` | `#F4EEE7` |
| `Gedämpft` | `#A99C8F` |
| `Blass` | `#6E635A` |
| `Aktion` | `#C4623C` |
| `Aktion gedeckt` | `#3A231A` |
| `Erledigt` | `#6F8F6A` |
| `Erledigt gedeckt` | `#22301F` |
| `Warnung` | `#D8A03C` |

### 2.2 Hell — `22hell` (light)

| Rolle | Wert |
|-------|------|
| `Grund` | `#F8F4EE` |
| `Fläche` | `#FFFFFF` |
| `Erhöht` | `#FFFFFF` |
| `Rand` | `#E6DCD0` |
| `Rand weich` | `#EFE8DF` |
| `Text` | `#1E1915` |
| `Gedämpft` | `#6C6157` |
| `Blass` | `#9C9186` |
| `Aktion` | `#B0522E` |
| `Aktion gedeckt` | `#F6E6DD` |
| `Erledigt` | `#5A7A55` |
| `Erledigt gedeckt` | `#E6EFE3` |
| `Warnung` | `#9A6A12` |

## 3. Typografie

| Rolle | Familie | Größe | Gewicht | Zeilenhöhe | Laufweite | Quelle |
|-------|---------|-------|---------|------------|-----------|--------|
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(3) ×61 | Inter, sans-serif | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(4) ×14 | Fraunces, serif | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(5) ×14 | "JetBrains Mono", monospace | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(0) ×3 | system-ui, -apple-system, "Segoe UI", Roboto, sans-serif | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| Bildschirmtitel | Fraunces | 28 | 600 | 34 | 0 | `02-UI-SPEC.md` |
| Abschnittstitel | Fraunces | 22 | 600 | 28 | 0 | `02-UI-SPEC.md` |
| Kartentitel | Fraunces | 19 | 600 | 25 | 0 | `02-UI-SPEC.md` |
| Fließtext | Inter | 16 | 400 | 25 | 0 | `02-UI-SPEC.md` |
| Fließtext klein | Inter | 14 | 400 | 21 | 0 | `02-UI-SPEC.md` |
| Knopfbeschriftung | Inter | 16 | 500 | 20 | 0.2 | `02-UI-SPEC.md` |
| Zwischenüberschrift | Inter | 13 | 600 | 17 | 0.6 | `02-UI-SPEC.md` |
| Daten und Zahlen | JetBrains Mono | 13 | 400 | 18 | 0 | `02-UI-SPEC.md` |
| Stufe / Dauer | JetBrains Mono | 12 | 400 | 16 | 0.4 | `02-UI-SPEC.md` |

## 4. Maße und Raster

| Name | px | Original | Quelle |
|------|----|----------|--------|
| --radius-eingabefeld ×5 | 14 | `14px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --radius-karte ×5 | 20 | `20px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --radius-dialog ×5 | 24 | `24px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --radius-vollrund ×4 | 9999 | `9999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --b07-radius-vollrund | 999 | `999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |

## 5. Formen und Tiefe

| Name | Radius | Quelle |
|------|--------|--------|
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(5) ×5 | `inherit` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(13) ×5 | `20px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(0) ×4 | `999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| Eingabefeld ×2 | `14 dp` | `02-UI-SPEC.md` |
| Reiter, Chip, Stufen-Etikett ×2 | `vollrund` | `02-UI-SPEC.md` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(14) ×2 | `24px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| Karte | `20 dp` | `02-UI-SPEC.md` |
| Dialog | `24 dp` | `02-UI-SPEC.md` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(4) | `50%` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(7) | `20px 20px 6px 20px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(8) | `20px 20px 20px 6px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(11) | `14px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(12) | `9999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(15) | `0` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |


| Effekt | Art | CSS | Quelle |
|--------|-----|-----|--------|
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:shadow(0) ×2 | shadow | `0 6px 24px rgba(0, 0, 0, 0.28)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(7) ×2 | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--Aktion-gedeckt) 88%, var(--Text) 6%), var(--Aktion-gedeckt))` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:shadow(2) | shadow | `0 0 20px color-mix(in srgb, currentColor 18%, transparent), inset 0 1px 0 color-mix(in srgb, currentColor 18%, transparent)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(3) | gradient | `background-image: linear-gradient(
        to right,
        var(--rand) 0 1px,
        transparent 1px calc(100% / 12)
      ),
      repeating-linear-gradient(
        to right,
        var(--gedaempft) 0 1px,
        transparent 1px calc(100% / 6)
      )` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(4) | gradient | `background-image: radial-gradient(circle at 92% 8%, color-mix(in srgb, var(--aktion) 18%, transparent) 0, transparent 32%),
    radial-gradient(circle at 0% 82%, color-mix(in srgb, var(--erledigt) 12%, transparent) 0, transparent 28%),
    var(--grund)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(5) | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--aktion), var(--text) 10%), var(--aktion) 58%, color-mix(in srgb, var(--aktion), #000000 16%))` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(6) | gradient | `background-image: radial-gradient(circle at 92% 8%, color-mix(in srgb, var(--Aktion) 18%, transparent) 0, transparent 32%),
    radial-gradient(circle at 0% 82%, color-mix(in srgb, var(--Erledigt) 12%, transparent) 0, transparent 28%),
    var(--Grund)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(9) | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--Aktion), var(--Text) 10%), var(--Aktion) 58%, color-mix(in srgb, var(--Aktion), #000000 16%))` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |

## 6. Bildschirme

> **Die untere Leiste — verbindlich für alle Hauptbildschirme.** Sie hat in dieser Fassung
> **sechs** Felder statt fünf, in dieser festen Reihenfolge:
> **Monitor · Heute · Ziele · Merkliste · Erkenntnisse · Logbuch.**
> Sie ist auf B-10, B-01, B-04, B-05, B-06 und B-07 identisch, schwebt mit 12 dp Rand und
> Radius 24 dp, ist eine Glasfläche (`E-03`), und jedes Feld ist mindestens 48 dp hoch. Bei
> 412 dp Breite bleiben je Feld 68 dp — die Beschriftung steht in Inter 11/14, das Symbol
> darüber misst 24 dp. F-27 (Wischen) läuft über alle sechs in genau dieser Reihenfolge.
> **Wo in den Bedienelement-Tabellen unten nur fünf Felder aufgeführt sind, ist das der
> Stand des alten Designs; das Monitor-Feld kommt überall als erstes hinzu.**

| Kennung | Bildschirm | Start | führt zu | Dateien je Erscheinung |
|---------|------------|-------|----------|------------------------|
| **B-10 NEU** | **Monitor (`B-10`)** | **ja** | **B-01, B-02, B-03, B-04, B-05, B-06, B-07, B-08** | **noch nicht gebaut — in beiden Erscheinungen zu gestalten** |
| B-01 | Heute (`B-01`) | nein — **war Start, ist es nicht mehr** | B-02, B-03, B-08, B-10 | `bildschirme/21dunkelstandard/…-heute.html`<br>`bildschirme/22hell/…-heute.html` |
| B-02 | Gespräch (`B-02`) | — | B-01 | `bildschirme/21dunkelstandard/…-gespr-ch.html`<br>`bildschirme/22hell/…-gespr-ch.html` |
| B-03 | Auswertung (`B-03`) | — | B-01 | `bildschirme/21dunkelstandard/…-auswertung.html`<br>`bildschirme/22hell/…-auswertung.html` |
| B-08 | Einstellungen (`B-08`) | — | B-09 | `bildschirme/21dunkelstandard/…-einstellungen.html`<br>`bildschirme/22hell/…-einstellungen.html` |
| B-09 | Selbstbild (`B-09`) | — | B-08 | `bildschirme/21dunkelstandard/…-selbstbild.html`<br>`bildschirme/22hell/…-selbstbild.html` |
| B-06 | Erkenntnisse (`B-06`) | — | — | `bildschirme/21dunkelstandard/…-erkenntnisse.html`<br>`bildschirme/22hell/…-erkenntnisse.html` |
| B-07 | Logbuch (`B-07`) | — | — | `bildschirme/21dunkelstandard/…-logbuch.html`<br>`bildschirme/22hell/…-logbuch.html` |
| B-05 | Merkliste (`B-05`) | — | — | `bildschirme/21dunkelstandard/…-merkliste.html`<br>`bildschirme/22hell/…-merkliste.html` |
| B-04 | Wünsche &amp; Ziele (`B-04`) | — | — | `bildschirme/21dunkelstandard/…-w-nsche-amp-ziele.html`<br>`bildschirme/22hell/…-w-nsche-amp-ziele.html` |

> **Achtung:** Der Eintrag „Experimente-SPEC-v1" in der Vollständigkeitsmessung des Designs
> ist ein Import-Artefakt (der Projektname selbst), kein fehlender Bildschirm.
>
> **Wirklich noch nicht gebaut ist `B-10` (Monitor)** — er ist in dieser Fassung neu
> hinzugekommen und muss im Design in **beiden** Erscheinungen aufgebaut werden.

### B-10 NEU — Monitor

Startbildschirm: **ja** · Quelle: neu in dieser Fassung, im Design noch nicht aufgebaut

> **An den Gestalter:** Dieser Bildschirm ist neu und muss in **beiden Erscheinungen**
> aufgebaut werden. Er ersetzt B-01 als Startbildschirm. Die hier genannten Maße sind aus dem
> bestehenden Design abgeleitet (Seitenrand 20, Kartenradius 20, Innenabstand 21, Knopfhöhe
> 48, schwebende Leiste 12/24) und gelten, bis sie im Design gemessen wurden.

**Aufbau von oben nach unten**

1. **Kopfleiste** (Glas, `E-03`; Höhe 64 dp, unter der Statusleiste)
   - links: Bildschirmtitel **„Monitor"** (Fraunces 28/34, 600)
   - rechts: Erscheinungsschalter (F-26) und Einstellungen (→ B-08), je 48 × 48 dp
2. **Zeile „Steht an: 4 · Läuft: 2"** (JetBrains Mono 13, gedämpft) — die Zahlen zählen bei
   Änderung hoch (`E-20`)
3. **Abschnitt „Läuft"** (Zwischenüberschrift Inter 13/17, 600, Laufweite 0.6)
   - je Experiment eine **Laufkarte**: Radius 20 dp, Innenabstand 21 dp, Fläche `Erhöht`,
     wandernder Lichtsaum am Rand (`E-06`), farbiger Schein nach außen (`E-05`)
   - Inhalt: Titel (Fraunces 19/25) · Etikett **Stufe** und **„Tag 2 von 3"** (JetBrains Mono
     12, vollrund) · Herkunftsetikett · Stand der heutigen Aufgaben („3 von 5") mit
     Fortschrittsring (56 dp, Farbe `Aktion`, Leuchtspur)
   - aufgeklappt (F-40): vollständige Beschreibung, Aufgaben je Tag, Knopf **„Gespräch"**
     (→ B-02) und **„Wie ist es gelaufen?"** (→ B-03)
   - sind keine vorhanden: leerer Zustand, Satz siehe §8
4. **Die eine To-Do-Liste des Tages** (F-07), nach Experimenten gruppiert — unverändert
   übernommen von B-01, mit Haken-Effekt `E-17`
5. **Abschnitt „Steht an"**
   - je Experiment eine **Wartekarte**: gleicher Radius, Fläche `Fläche`, **ohne** Lichtsaum
     (nur Laufendes leuchtet), Deckkraft der Fläche 92 %
   - Inhalt: Titel · Stufe · Dauer · Herkunftsetikett · Knopf **„Starten"** (Höhe 48 dp,
     vollrund, Verlauf `Aktion`)
   - ziehbar (F-38), nach links wischbar (F-39)
   - sind keine vorhanden: leerer Zustand, Satz siehe §8
6. **Schwebender Plus-Knopf** (F-35): 64 × 64 dp, vollrund, Verlauf `Aktion`, Schein `E-05`,
   Abstand 24 dp vom rechten Rand, 24 dp über der unteren Leiste
7. **Untere Leiste** (Glas, `E-03`; schwebend, 12 dp Rand, Radius 24 dp) — **sechs Felder**:
   **Monitor · Heute · Ziele · Merkliste · Erkenntnisse · Logbuch**

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Erscheinung umschalten | button | löst `F-26` aus |
| 2 | Einstellungen | a | führt zu `B-08` |
| 3 | Laufkarte auf-/zuklappen | article | löst `F-40` aus |
| 4 | Gespräch zum Experiment | a | führt zu `B-02` |
| 5 | Wie ist es gelaufen? | a | führt zu `B-03` |
| 6 | Aufgabe abhaken | button | löst `F-08` aus |
| 7 | Wartekarte auf-/zuklappen | article | löst `F-40` aus |
| 8 | Starten | button | löst `F-37` aus |
| 9 | Karte verschieben | article | löst `F-38` aus |
| 10 | Aus dem Monitor nehmen | button | löst `F-39` aus |
| 11 | Eigenes Experiment anlegen | button | löst `F-35` aus |
| 12 | Eigenes Experiment einsprechen | button | löst `F-35` aus |
| 13 | Eigenes Experiment | textarea | löst `F-35` aus |
| 14 | Text mit KI verbessern | button | löst `F-02` aus |
| 15 | Speichern | button | löst `F-35` aus |
| 16 | Monitor | a | führt zu `B-10` |
| 17 | Heute | a | führt zu `B-01` |
| 18 | Ziele | a | führt zu `B-04` |
| 19 | Merkliste | a | führt zu `B-05` |
| 20 | Erkenntnisse | a | führt zu `B-06` |
| 21 | Logbuch | a | führt zu `B-07` |

**Zustände**

| Zustand | Was B-10 zeigt |
|---------|----------------|
| `LEER` | Beide Abschnitte leer, die Sätze aus §8, der Plus-Knopf atmet (`M-79`) |
| `NUR_ANSTEHEND` | Nur „Steht an"; „Läuft" ist ganz ausgeblendet, nicht als leere Hülle |
| `LAEUFT` | Beide Abschnitte, dazu die To-Do-Liste des Tages |
| `VOLL` | Drei laufen — alle „Starten"-Knöpfe gesperrt mit dem Hinweis aus F-37 |
| `ANLEGEN` | Die Anlegefläche liegt über dem Bildschirm, der Grund ist weichgezeichnet (`E-03`) |
| *lädt* | Schimmer-Skelette statt Karten (`E-13`) |

**Bewegungen auf diesem Bildschirm**

- `M-76` Lichtgrund wandert — 24000 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos
- `M-77` Glasleiste verdichtet sich beim Scrollen — 200 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-78` Karte kippt zur Neigung des Geräts — Federphysik, dauerhaft
- `M-79` Plus-Knopf atmet — 3200 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos
- `M-80` Anlegefläche fährt herein — 320 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-81` Neue Karte fliegt ein und funkelt — 480 ms, Federphysik, einmal
- `M-82` Übernommener Vorschlag fliegt zum Monitor-Feld — 520 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-83` Karte wandert von „Steht an" nach „Läuft" — 400 ms, Federphysik, einmal
- `M-84` Funken beim Start — 1200 ms, `linear`, einmal
- `M-85` Karte hebt sich beim Ziehen ab — 160 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-86` Karte klappt auf — 280 ms, Federphysik, einmal
- `M-87` Fortschrittsring füllt sich mit Leuchtspur — 600 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-88` Zahl zählt hoch — 400 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-89` Schimmer über dem Skelett — 1400 ms, `linear`, endlos
- `M-90` Lichtsaum wandert um die Laufkarte — 6000 ms, `linear`, endlos
- `M-91` Bildschirmwechsel mit Weichzeichnen und Skalieren — 260 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-92` Geteiltes Element beim Wechsel — 300 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-93` Lichtblüte beim Abschließen — 1600 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-94` Leistenfeld leuchtet auf — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-95` Gestaffeltes Erscheinen der Karten — 240 ms je Karte, 60 ms Versatz, `cubic-bezier(.2, 0, 0, 1)`, einmal

### B-01 — Heute

Startbildschirm: ja · Quelle: `B-01`

**Aufbau von oben nach unten**

- `<section.werft-b01>`
  - `<header.werft-b01__topbar>`
  - `<main.werft-b01__content>`
  - `<nav.werft-b01__bottomnav>`
  - `<script>` — „(() => { const root = document.currentScr“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Hellmodus einschalten | button | F-26 schaltet nacheinander auf Hellmodus, Dunkelmodus und Automatik um, zeigt dafür Sonne, Mondsichel oder A mit seitlichen Strahlen und lässt Automatik sofort sowie bei späteren Systemwechseln der Systemdarstellung folgen |
| 2 | Einstellungen | a | führt zu `B-08` |
| 3 | Lage einsprechen | button | löst `F-01` aus |
| 4 | Lieber tippen | button | Lage tippen |
| 5 | Heutige Lage | textarea | **Geklärt in dieser Fassung:** nimmt den transkribierten Text auf und bleibt frei bearbeitbar (F-01 Schritt 7, F-28) |
| 6 | Text mit KI verbessern | button | löst `F-02` aus |
| 7 | Weiter | button | löst `F-03` aus |
| 8 | Auf die Merkliste legen | article | löst `F-06` aus |
| 9 | Auf die Merkliste legen | button | löst `F-05` aus |
| 10 | Von der Merkliste nehmen | article | löst `F-06` aus |
| 11 | Von der Merkliste nehmen | button | löst `F-05` aus |
| 12 | Andere Vorschläge | button | löst `F-04` aus |
| 13 | Gespräch zum Experiment | a | führt zu `B-02` |
| 14 | Nicht umgesetzt | button | löst `F-13` aus |
| 15 | Eine Aufgabe auswählen, die heute wirklich fertig werden soll. | button | löst `F-08` aus |
| 16 | Alles andere beiseitelegen und den ersten Schritt machen. | button | löst `F-08` aus |
| 17 | Wie ist es gelaufen? | a | führt zu `B-03` |
| 18 | Heute | a | führt zu `B-01` |
| 19 | Ziele | a | führt zu `B-04` |
| 20 | Merkliste | a | führt zu `B-05` |
| 21 | Erkenntnisse | a | führt zu `B-06` |
| 22 | Logbuch | a | führt zu `B-07` |
| 23 **NEU** | In den Monitor | button | löst `F-36` aus — legt den Vorschlag unter „Steht an"; die übrigen Karten bleiben stehen |
| 24 **NEU** | Jetzt starten | button | löst `F-06` aus — übernimmt und startet in einem Zug; gesperrt, wenn drei laufen |
| 25 **NEU** | Monitor | a | führt zu `B-10` |

**Bewegungen auf diesem Bildschirm**

- `M-10` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-02` M-02 — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-11` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-12` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-04` M-04 — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-13` Übergang transform — 120 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-07` M-07 — 180 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-14` Übergang transform — 120 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-15` Übergang color — 120 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-16` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-41` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-42` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-52` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-53` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-54` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-55` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-56` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-57` Übergang opacity — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-58` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-02 — Gespräch

Startbildschirm: nein · Quelle: `B-02`

**Aufbau von oben nach unten**

- `<section.b02-screen>`
  - `<header.b02-topbar>`
  - `<main.b02-main>`
  - `<form.b02-composer>`
- `<script>` — „(() => { const screen = document.currentS“
  - `<span.b02-loading-dots>`

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Zurück zu Heute | button | führt zu `B-01` |
| 2 | Nachricht eingeben | input | löst `F-09` aus |
| 3 | Gespräch aufnehmen | button | löst `F-09` aus |

**Bewegungen auf diesem Bildschirm**

- `M-17` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-18` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-02` m-02-atmen — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-03 — Auswertung

Startbildschirm: nein · Quelle: `B-03`

**Aufbau von oben nach unten**

- `<section.werft-b03>`
  - `<header.werft-b03__topbar>`
  - `<main.werft-b03__main>`
- `<script>` — „(() => { const root = document.getElement“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Zurück | button | führt zu `B-01` |
| 2 | Auswertung einsprechen | button | löst `F-10` aus |
| 3 | Auswertungstext | textarea | Auswertungstext bearbeiten |
| 4 | Text mit KI verbessern | button | löst `F-02` aus |
| 5 | Weiter | button | löst `F-11` aus |
| 6 | Auswertung vorlesen | button | löst `F-12` aus |
| 7 | Nochmal versuchen | button | löst `F-10` aus |
| 8 | Überspringen | button | Zum nächsten Experiment springen |
| 9 | Fertig | button | führt zu `B-01` |

**Bewegungen auf diesem Bildschirm**

- `M-19` Übergang background-color — 120 ms, `ease`, einmal (siehe Motion-Spec)
- `M-02` m-02 — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-20` Übergang background-color — 120 ms, `ease`, einmal (siehe Motion-Spec)
- `M-21` Übergang color — 120 ms, `ease`, einmal (siehe Motion-Spec)
- `M-08` m-08 — 400 ms, `ease`, einmal (siehe Motion-Spec)
- `M-09` m-09 — 1800 ms, `linear`, endlos (siehe Motion-Spec)
- `M-22` Übergang opacity — 140 ms, `ease`, einmal (siehe Motion-Spec)
- `M-23` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-08 — Einstellungen

Startbildschirm: nein · Quelle: `B-08`

**Aufbau von oben nach unten**

- `<section.werft-b08>`
  - `<header.werft-b08__topbar>`
  - `<main.werft-b08__scroll>`
- `<script>` — „(() => { const root = document.getElement“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Modell für Experimente | select | löst `F-22` aus |
| 2 | Effort für Experimente | select | löst `F-22` aus |
| 3 | Modell für das Logbuch | select | löst `F-22` aus |
| 4 | Effort für das Logbuch | select | löst `F-22` aus |
| 5 | Stimmenanbieter | select | löst `F-23` aus |
| 6 | Google-Stimme | select | löst `F-23` aus |
| 7 | Stimme aufnehmen | button | löst `F-23` aus |
| 8 | Microsoft-Edge-Stimme | select | löst `F-23` aus |
| 9 | 1.0 | input | löst `F-23` aus |
| 10 | Probe hören | button | löst `F-23` aus |
| 11 | Anmelden | button | löst `F-24` aus |
| 12 | •••••••••••• | input | löst `F-24` aus |
| 13 | 08:00 | input | löst `F-25` aus |
| 14 | Erinnerung morgens | input | löst `F-25` aus |
| 15 | 20:30 | input | löst `F-25` aus |
| 16 | Erinnerung abends | input | löst `F-25` aus |
| 17 | werft-b08-theme | input | löst `F-26` aus |
| 18 | Selbstbild | button | führt zu `B-09` |

**Bewegungen auf diesem Bildschirm**

- `M-24` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-25` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-26` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-27` Übergang left — 200 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-28` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-29` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-30` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-31` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-09 — Selbstbild

Startbildschirm: nein · Quelle: `B-09`

**Aufbau von oben nach unten**

- `<section.b09-screen>`
  - `<header.b09-topbar>`
  - `<main.b09-content>`
  - `<script>` — „(() => { const screen = document.curr“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Zurück zu Einstellungen | button | führt zu `B-08` |
| 2 | Selbstbild | textarea | löst `F-21` aus |
| 3 | Text mit KI verbessern | button | löst `F-02` aus |
| 4 | Selbstbild einsprechen | button | löst `F-21` aus |

**Bewegungen auf diesem Bildschirm**

- `M-32` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-33` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-02` m-02-atmen — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-06 — Erkenntnisse

Startbildschirm: nein · Quelle: `B-06`

**Aufbau von oben nach unten**

- `<section.b06-screen>`
  - `<header.b06-topbar>`
  - `<main.b06-content>`
  - `<nav.b06-bottom-nav>`

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Heute | button | führt zu `B-01` |
| 2 | Ziele | button | führt zu `B-04` |
| 3 | Merkliste | button | führt zu `B-05` |
| 4 | Erkenntnisse | button | führt zu `B-06` |
| 5 | Logbuch | button | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-34` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-35` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-07 — Logbuch

Startbildschirm: nein · Quelle: `B-07`

**Aufbau von oben nach unten**

- `<section.b07-screen>`
  - `<header.b07-topbar>`
  - `<main.b07-content>`
  - `<nav.b07-bottom-nav>`
- `<script>` — „(() => { const screen = document.getEleme“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Letzte 15 Tage | button | Letzte 15 Tage anzeigen |
| 2 | Langzeit | button | Langzeit anzeigen |
| 3 | Heute | button | führt zu `B-01` |
| 4 | Ziele | button | führt zu `B-04` |
| 5 | Merkliste | button | führt zu `B-05` |
| 6 | Erkenntnisse | button | führt zu `B-06` |
| 7 | Logbuch | button | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-36` Übergang transform — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-37` Übergang color — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-38` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-39` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-40` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-41` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-42` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-05 — Merkliste

Startbildschirm: nein · Quelle: `B-05`

**Aufbau von oben nach unten**

- `<section.b05-screen>`
  - `<header.b05-topbar>`
  - `<main.b05-content>`
  - `<section.b05-create-surface>`
  - `<button.b05-fab>`
  - `<nav.b05-bottomnav>`
  - `<dialog.b05-dialog>`
- `<script>` — „(() => { const screen = document.currentS“
  - `<path>` — „'; return svg; }; const armDelete“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Eigenes Experiment einsprechen | button | löst `F-18` aus |
| 2 | Eigenes Experiment | textarea | löst `F-18` aus |
| 3 | Text mit KI verbessern | button | löst `F-02` aus |
| 4 | Speichern | button | löst `F-18` aus |
| 5 | Eigenes Experiment anlegen | button | löst `F-18` aus |
| 6 | Heute | button | führt zu `B-01` |
| 7 | Ziele | button | führt zu `B-04` |
| 8 | Merkliste | button | führt zu `B-05` |
| 9 | Erkenntnisse | button | führt zu `B-06` |
| 10 | Logbuch | button | führt zu `B-07` |
| 11 | Abbrechen | button | löst `F-33` aus — **geklärt in dieser Fassung:** schließt die Anlegefläche, ohne zu speichern; der Text wird verworfen |
| 12 | Löschen | button | löst `F-19` aus |

**Bewegungen auf diesem Bildschirm**

- `M-02` m-02-atmen — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-44` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-04 — Wünsche &amp; Ziele

Startbildschirm: nein · Quelle: `B-04`

**Aufbau von oben nach unten**

- `<section.b04-screen>`
  - `<header.b04-topbar>`
  - `<main.b04-content>`
  - `<section.b04-create-layer>`
  - `<button.b04-fab>`
  - `<nav.b04-bottom-nav>`
- `<script>` — „(() => { const screen = document.currentS“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Ziel einsprechen | button | löst `F-20` aus |
| 2 | Ziel | textarea | löst `F-20` aus |
| 3 | Text mit KI verbessern | button | löst `F-02` aus |
| 4 | Speichern | button | löst `F-20` aus |
| 5 | Ziel anlegen | button | löst `F-20` aus |
| 6 | Heute | button | führt zu `B-01` |
| 7 | Ziele | button | führt zu `B-04` |
| 8 | Merkliste | button | führt zu `B-05` |
| 9 | Erkenntnisse | button | führt zu `B-06` |
| 10 | Logbuch | button | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-41` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-42` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-47` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-02` m-02 — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-48` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-49` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

## 7. Effekte

Dieser Abschnitt ist **neu** und in dieser Fassung der Kern des Auftrags: Frank will eine
durchgehend moderne App mit maximalen Effekten. Jeder Effekt hat eine Kennung `E-…`, ist
verbindlich, und nennt seinen Weg in Jetpack Compose. Die zugehörigen Dauern und Kurven
stehen im Motion-Spec (`M-76` bis `M-95`).

**Drei Regeln stehen über allem:**

1. **Kein Effekt trägt Information allein.** Was ein Leuchten sagt, sagt daneben auch ein
   Wort oder eine Form.
2. **Jeder Effekt hat eine Rückfallebene**, die auf `minSdk 26` funktioniert und auf der
   Stufe *Aus* (F-41) greift.
3. **Bildrate vor Pracht.** Kein Effekt darf die App unter 60 fps drücken; auf Geräten mit
   120 Hz ist 120 fps das Ziel. Wo beides nicht geht, gewinnt die Bildrate.

### 7.1 Grund und Fläche

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-01` | **Lichtgrund** — zwei bis drei weiche Farbkreise (`Aktion` 18 %, `Erledigt` 12 %) über `Grund`, die langsam wandern (`M-76`) | `Brush.radialGradient` in einem `Canvas` hinter dem Inhalt, Mittelpunkte über `infiniteRepeatable` bewegt | Standbild derselben Verläufe |
| `E-02` | **Filmkorn** — feine Rauschtextur über dem Grund, Deckkraft 3 %, verhindert Farbstreifen in den Verläufen | gekacheltes Rausch-Bitmap, `BlendMode.Overlay` | entfällt ersatzlos |
| `E-03` | **Glasflächen** — Kopfleiste, untere Leiste, Anlegefläche und Dialoge zeichnen den Grund weich | ab API 31 `Modifier.graphicsLayer { renderEffect = BlurEffect(24f, 24f) }` auf einer Kopie des Hintergrunds | halbdurchlässige Fläche (`Erhöht`, 88 %) mit 1 dp Rand `Rand weich` |
| `E-04` | **Lichtsaum** — 1 dp Innenkante oben an jeder Karte, Verlauf von `Text` 18 % nach transparent | `Modifier.drawWithContent` mit `linearGradient` | einfacher Rand `Rand` |
| `E-05` | **Schein** — farbiger Schein nach außen an allem, was gerade aktiv ist (`Aktion` 18 %, Radius 20 dp) | `Modifier.shadow(20.dp, spotColor = Aktion, ambientColor = Aktion)` bzw. gezeichneter Radialverlauf | entfällt |
| `E-06` | **Wandernder Rand** — um laufende Karten läuft ein Lichtband einmal in 6 s herum (`M-90`) | `Brush.sweepGradient` mit rotierendem Winkel als Rahmen | statischer Rand `Aktion` 40 % |
| `E-24` | **Tageszeit-Stimmung** — der Farbton des Lichtgrunds wandert über den Tag: morgens wärmer, abends tiefer | Farbwerte aus der Uhrzeit interpoliert, Wechsel über 60 s | fester Farbton |

### 7.2 Berührung und Bewegung

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-07` | **Federphysik** — jeder Druck sinkt ein und schwingt zurück, statt linear zu skalieren | `animateFloatAsState(spring(dampingRatio = 0.55f, stiffness = 380f))` auf `scale` | `tween(120)` |
| `E-08` | **Kipp-Parallaxe** — Karten neigen sich mit dem Gerät, höchstens ±6°, Inhalt versetzt sich um bis zu 4 dp (`M-78`) | `SensorManager` (Rotationsvektor), gedämpft, auf `graphicsLayer { rotationX/rotationY }` | aus |
| `E-09` | **Scroll-Parallaxe** — Kopfbereich und Lichtgrund bewegen sich mit 0,4× der Scrollstrecke, die Glasleiste verdichtet sich (`M-77`) | `LazyListState.firstVisibleItemScrollOffset` → `graphicsLayer` und Deckkraft | aus |
| `E-10` | **Gestaffeltes Erscheinen** — Listen bauen sich mit 60 ms Versatz je Eintrag auf (`M-95`) | `AnimatedVisibility` mit index-abhängiger `delayMillis` | alles gleichzeitig |
| `E-11` | **Geteilte Elemente** — beim Öffnen einer Karte wandert sie sichtbar zum Zielbildschirm (`M-92`) | `SharedTransitionLayout` (Compose 1.7+) mit `sharedElement` auf Titel und Fläche | einfaches Überblenden |
| `E-12` | **Bildschirmwechsel** — der abgehende Bildschirm wird weichgezeichnet und auf 96 % verkleinert, der neue kommt scharf und auf 104 % beginnend herein (`M-91`) | `AnimatedContent` mit `scaleIn/scaleOut` plus `BlurEffect` ab API 31 | Überblenden |
| `E-22` | **Wischbare Vorschlagskarten** — Vorschlagskarten auf B-01 lassen sich mit dem Finger kippen und wegschieben; nach rechts = in den Monitor (F-36), nach links = verworfen | `Modifier.draggable` + `graphicsLayer { rotationZ = versatz * 0.06f }` | nur die Knöpfe |

### 7.3 Zustand und Rückmeldung

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-13` | **Schimmer-Skelett** — beim Laden stehen Kartenumrisse mit wanderndem Lichtstreifen (`M-89`) | `Brush.linearGradient` mit animiertem Versatz | ruhige graue Fläche |
| `E-14` | **Wartezustand der KI** — der bestehende `M-09` bekommt zusätzlich einen Lichtstreifen, der durch die entstehende Antwortfläche zieht | wie `E-13`, in `Aktion` | `M-09` allein |
| `E-15` | **Funken beim Start** — 24 kleine Lichtpunkte steigen aus der Karte auf und verlöschen (`M-84`) | `Canvas` mit Partikelliste, Lebensdauer 1200 ms, Schwerkraft nach oben | einmaliges Aufleuchten |
| `E-16` | **Lichtblüte beim Abschließen** — ein Ring aus Licht dehnt sich aus der abgeschlossenen Karte, dazu 40 Partikel (`M-93`) | `Canvas`, Radius über `tween(1600)` | einmaliges Aufleuchten |
| `E-17` | **Haken** — der Haken zeichnet sich (`M-06`), ein kurzer Lichtblitz läuft über die Zeile, die Zeile dämpft sich, dazu Haptik | `PathMeasure` auf dem Haken-Pfad + `linearGradient`-Blitz | nur der gezeichnete Haken |
| `E-18` | **Aufnahme-Wellenform** — während der Aufnahme zeigt ein Kranz aus 48 Balken den echten Mikrofonpegel | Pegel aus dem `AudioRecord`-Puffer (RMS, 32 ms), `Canvas` | nur der atmende Ring `M-02` |
| `E-19` | **Pulsringe** — drei Ringe steigen gestaffelt aus dem Sprechknopf auf | drei `infiniteRepeatable` mit 0/400/800 ms Versatz | ein Ring (`M-02`) |
| `E-20` | **Zählende Zahlen** — Anzahlen und Fortschritt zählen hoch statt zu springen (`M-88`) | `animateIntAsState` | Sprung |
| `E-21` | **Mitlesen beim Vorlesen** — der gerade gesprochene Abschnitt wird hervorgehoben | Zeitmarken des Anbieters, sonst gleichmäßig über die Dauer geschätzt | keine Hervorhebung |
| `E-23` | **Haptik** — feste Muster: kurz (10 ms) beim Abhaken · doppelt beim Aufnahmebeginn und -ende (`M-03`) · aufsteigend beim Starten · lang-weich beim Abschließen · Fehler = zwei harte Stöße | `HapticFeedbackConstants` bzw. `VibrationEffect.createWaveform` | aus |

### 7.4 Leistung, Akku und Grenzen

- **Dauerbewegungen halten an**, sobald der Bildschirm nicht sichtbar ist
  (`LifecycleEventEffect(ON_STOP)`), und laufen beim Zurückkehren weiter.
- **Höchstens zwei Dauerbewegungen** je Bildschirm gleichzeitig sichtbar. Der Lichtgrund
  (`E-01`) zählt als eine davon.
- **Partikel** (`E-15`, `E-16`) laufen nur auf ausdrückliche Handlung, nie automatisch, nie
  mehr als eine Wolke gleichzeitig, höchstens 40 Punkte.
- **Weichzeichnen** (`E-03`, `E-12`) nur über `RenderEffect` ab API 31 — **nie** durch
  wiederholtes Skalieren von Bitmaps.
- **Bei Energiesparmodus** (`PowerManager.isPowerSaveMode`) gilt automatisch mindestens die
  Stufe *Gedämpft* (F-41).
- **Bei „Bewegung reduzieren"** gilt Motion-Spec §8 und zusätzlich F-41.

## 8. Leere Zustände und feste Texte

Diese Sätze sind **wörtlich** zu übernehmen.

| Ort | Text |
|-----|------|
| B-10, beide Abschnitte leer | „Hier steht noch nichts. Leg dir eines an oder hol dir Vorschläge." |
| B-10, „Läuft" leer | „Noch läuft nichts. Starte eines von unten." |
| B-10, „Steht an" leer | „Nichts vorgemerkt." |
| B-10, drei laufen | „Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst." |
| B-01, keine Lage | „Wie ist deine Lage heute?" |
| B-04 leer | „Noch keine Ziele. Sprich eines ein." |
| B-05 leer | „Die Merkliste ist leer." |
| B-06 leer | „Noch keine Erkenntnisse. Sie wachsen aus den Auswertungen." |
| B-07, beide Reiter leer | „Das Logbuch beginnt mit dem ersten Tag." |
| B-09 leer | „Erzähl der App, wer du bist." |
| kein Netz | „Dafür brauche ich Netz." |
| Aufnahme ohne Ton | „Da war nichts zu hören." |
| Verbessern fehlgeschlagen | „Der Text konnte nicht verbessert werden." |
| Anmeldung abgelaufen | „Deine Anmeldung ist abgelaufen." |
| Kontingent erschöpft | „Dein Kontingent ist erschöpft." |
| Antwort unbrauchbar | „Die Antwort war unbrauchbar." |
| Mikrofon abgelehnt | „Ohne Mikrofon kann ich dich nicht hören." |
| Stimme ohne Schlüssel | „Für diese Stimme fehlt der Schlüssel." |

## 9. Barrierefreiheit

Die App ist ausschließlich für Frank. Es gelten keine Store-Vorgaben. Festgelegt wurde nur,
was sich aus der Gestaltung ergibt:

- **Mindest-Tippfläche 48 × 48 dp** für jedes bedienbare Element (folgt aus „luftig, große
  Tippflächen").
- **Kontrast:** Text auf Grund erreicht in beiden Erscheinungen mindestens 7:1
  (Dunkel: `#F4EEE7` auf `#151210` · Hell: `#1E1915` auf `#F8F4EE`). Gedämpfter Text
  mindestens 4,5:1.
- **Große Systemschrift** wird übernommen; Karten wachsen mit, Texte werden nie abgeschnitten.
- **Reduzierte Bewegung** wird beachtet — siehe Motion-Spec §8 und F-41. Meldet das System
  „Bewegung reduzieren", gilt mindestens die Effekt-Stufe *Gedämpft*, auch wenn *Voll*
  eingestellt ist.
- **Die sechs Felder der unteren Leiste** behalten trotz der zusätzlichen Spalte ihre
  Mindest-Tippfläche: 68 × 48 dp je Feld bei 412 dp Breite. Wird die Systemschrift sehr groß
  gestellt, entfällt zuerst die Beschriftung, nie das Symbol oder die Tippfläche.
- **Kein Effekt ist Bedingung für Bedienbarkeit.** Auf der Stufe *Aus* (F-41) ist jede
  Funktion vollständig erreichbar; kein Zustand wird ausschließlich durch Leuchten, Bewegung
  oder Farbe mitgeteilt.

---

## 10. Offene Fragen

- Design-Fakten stammen aus dem Spec-Paket (00-PROJEKT.md, 01-FUNKTIONS-SPEC.md, 02-UI-SPEC.md, 03-MOTION-SPEC.md), nicht aus Quellcode: die Software existiert noch nicht.
- Funktionen aus dem Spec — das ausloesende Bedienelement traegt data-werft-funktion mit dieser Kennung: F-01 = Lage einsprechen; F-02 = Text mit KI verbessern; F-03 = Fünf Vorschläge erzeugen; F-04 = Vorschläge aktualisieren; F-05 = Vorschlag auf die Merkliste legen; F-06 = Experiment auswählen und starten; F-07 = To-Do-Liste des Tages; F-08 = Aufgabe abhaken; F-09 = Gespräch zum Experiment; F-10 = Auswertung einsprechen; F-11 = KI-Auswertung erzeugen; F-12 = Auswertung vorlesen; F-13 = Experiment abschließen; F-14 = Logbuch fortschreiben; F-15 = Tagesverdichtung nach 15 Tagen; F-16 = Logbuch-Eintrag ändern oder löschen; F-17 = Erkenntnisse fortschreiben; F-18 = Merkliste: eigenes Experiment anlegen; F-19 = Merkliste: Eintrag löschen; F-20 = Wünsche & Ziele pflegen; F-21 = Selbstbild pflegen; F-22 = Modell und Effort wählen; F-23 = Stimme und Vorlesen einstellen; F-24 = Zugänge einrichten; F-25 = Erinnerungen einstellen; F-26 = Erscheinung umschalten; F-27 = Zwischen den Hauptbildschirmen wischen; F-28 = Lage tippen; F-29 = Auswertungstext bearbeiten; F-30 = Auswertung überspringen; F-31 = Logbuch, Reiter Letzte 15 Tage; F-32 = Logbuch, Reiter Langzeit; F-33 = Anlegen abbrechen; F-34 = Monitor sehen; F-35 = Monitor, eigenes Experiment anlegen; F-36 = Vorschlag in den Monitor übernehmen; F-37 = Experiment starten; F-38 = Reihenfolge im Monitor ändern; F-39 = Experiment aus dem Monitor nehmen; F-40 = Monitor-Karte auf- und zuklappen; F-41 = Effekt-Stärke einstellen

## Teil C — Motion-Spec

# Motion-Spec — Experimente-SPEC-v1
Stand: 12.08.2026, 12.06 Uhr · Stufe: v1, ueberarbeitet (Monitor + Effekte) · Plattform: Android (Kotlin / Jetpack Compose)


Jede Bewegung ist aus dem Design gemessen und für **Jetpack Compose** übersetzt.
Die angegebene Kurve ist verbindlich — eine eingebaute Standardkurve an ihrer Stelle gilt als nicht erfüllt.

## 2. Kurven und Dauern

| Kennung | Bewegung | Dauer | Kurve | Wiederholung |
|---------|----------|-------|-------|--------------|
| M-10 | Übergang opacity | 100 ms | `linear` | einmal |
| M-02 | M-02 | 3200 ms | `ease-in-out` | endlos |
| M-11 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-12 | Übergang background-color | 200 ms | `ease` | einmal |
| M-04 | M-04 | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-13 | Übergang transform | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-07 | M-07 | 180 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-14 | Übergang transform | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-15 | Übergang color | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-16 | Übergang color | 200 ms | `ease` | einmal |
| M-17 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-08 | m-08-antwort | 400 ms | `ease-out` | einmal |
| M-18 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-02 | m-02-atmen | 3200 ms | `ease-in-out` | endlos |
| M-19 | Übergang background-color | 120 ms | `ease` | einmal |
| M-02 | m-02 | 3200 ms | `ease-in-out` | endlos |
| M-20 | Übergang background-color | 120 ms | `ease` | einmal |
| M-21 | Übergang color | 120 ms | `ease` | einmal |
| M-08 | m-08 | 400 ms | `ease` | einmal |
| M-09 | m-09 | 1800 ms | `linear` | endlos |
| M-22 | Übergang opacity | 140 ms | `ease` | einmal |
| M-23 | Übergang opacity | 100 ms | `linear` | einmal |
| M-24 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-25 | Übergang border-color | 200 ms | `ease` | einmal |
| M-26 | Übergang background-color | 200 ms | `ease` | einmal |
| M-27 | Übergang left | 200 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-28 | Übergang background-color | 200 ms | `ease` | einmal |
| M-29 | Übergang color | 200 ms | `ease` | einmal |
| M-30 | Übergang background-color | 200 ms | `ease` | einmal |
| M-31 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-32 | Übergang opacity | 100 ms | `linear` | einmal |
| M-33 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-02 | m-02-atmen | 3200 ms | `ease-in-out` | endlos |
| M-34 | Übergang color | 200 ms | `ease` | einmal |
| M-35 | Übergang opacity | 100 ms | `linear` | einmal |
| M-36 | Übergang transform | 200 ms | `linear` | einmal |
| M-37 | Übergang color | 200 ms | `linear` | einmal |
| M-38 | Übergang opacity | 100 ms | `linear` | einmal |
| M-39 | Übergang opacity | 200 ms | `linear` | einmal |
| M-40 | Übergang visibility | 0 ms | `linear` | einmal |
| M-41 | Übergang opacity | 200 ms | `linear` | einmal |
| M-42 | Übergang visibility | 0 ms | `linear` | einmal |
| M-43 | Übergang transform | 120 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | einmal |
| M-02 | m-02-atmen | 3200 ms | `ease-in-out` | endlos |
| M-44 | Übergang opacity | 100 ms | `linear` | einmal |
| M-45 | Übergang transform | 120 ms | `ease-out` | einmal |
| M-46 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-47 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-02 | m-02 | 3200 ms | `ease-in-out` | endlos |
| M-48 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-49 | Übergang color | 200 ms | `ease` | einmal |
| M-50 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-51 | Übergang color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-52 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-53 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-54 | Übergang color | 200 ms | `ease` | einmal |
| M-55 | Übergang background-color | 200 ms | `ease` | einmal |
| M-56 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-57 | Übergang opacity | 200 ms | `ease` | einmal |
| M-58 | Übergang transform | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-59 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-60 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-61 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-62 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-63 | werft-screen-fade | 200 ms | `cubic-bezier(.4, 0, .6, 1)` | einmal |
| M-64 | werft-screen-detail | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-65 | Übergang transform | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-66 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-67 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-68 | Übergang box-shadow | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-69 | Übergang border-color | 200 ms | `ease` | einmal |
| M-70 | Übergang background-color | 200 ms | `ease` | einmal |
| M-71 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-72 | Übergang color | 200 ms | `ease` | einmal |
| M-73 | Übergang background-color | 200 ms | `ease` | einmal |
| M-74 | Übergang border-color | 200 ms | `ease` | einmal |
| M-75 | werft-screen-fade | 120 ms | `ease` | einmal |
| **M-76 NEU** | Lichtgrund wandert | 24000 ms | `cubic-bezier(0.42, 0, 0.58, 1)` | endlos, alternate |
| **M-77 NEU** | Glasleiste verdichtet sich beim Scrollen | 200 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-78 NEU** | Karte kippt zur Neigung des Geräts | Feder | `spring(0.75, 200)` | dauerhaft |
| **M-79 NEU** | Plus-Knopf atmet | 3200 ms | `cubic-bezier(0.42, 0, 0.58, 1)` | endlos, alternate |
| **M-80 NEU** | Anlegefläche fährt herein | 320 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-81 NEU** | Neue Karte fliegt ein und funkelt | 480 ms | `spring(0.6, 320)` | einmal |
| **M-82 NEU** | Übernommener Vorschlag fliegt zum Monitor-Feld | 520 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-83 NEU** | Karte wandert nach „Läuft" | 400 ms | `spring(0.7, 260)` | einmal |
| **M-84 NEU** | Funken beim Start | 1200 ms | `linear` | einmal |
| **M-85 NEU** | Karte hebt sich beim Ziehen ab | 160 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-86 NEU** | Karte klappt auf | 280 ms | `spring(0.8, 300)` | einmal |
| **M-87 NEU** | Fortschrittsring füllt sich | 600 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-88 NEU** | Zahl zählt hoch | 400 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-89 NEU** | Schimmer über dem Skelett | 1400 ms | `linear` | endlos |
| **M-90 NEU** | Lichtsaum wandert um die Laufkarte | 6000 ms | `linear` | endlos |
| **M-91 NEU** | Bildschirmwechsel mit Weichzeichnen | 260 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-92 NEU** | Geteiltes Element beim Wechsel | 300 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-93 NEU** | Lichtblüte beim Abschließen | 1600 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-94 NEU** | Leistenfeld leuchtet auf | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal, alternate |
| **M-95 NEU** | Gestaffeltes Erscheinen der Karten | 240 ms je Karte, 60 ms Versatz | `cubic-bezier(.2, 0, 0, 1)` | einmal |

## 3. Bewegungen im Einzelnen

### M-10 — Übergang opacity

- **Wo:** `.werft-b01__icon-action::after,
.werft-b01__text-action::after,
.werft-b01__primary-action::after,
.werft-b01__nav-item::after,
.werft-b01__todo::after,
.werft-b01__proposal::after,
.werft-b01__mic::after` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-02 — M-02

- **Wo:** `.werft-b01__mic-wrap.is-recording .werft-b01__recording-ring` — auf B-01
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes M-02 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-11 — Übergang transform

- **Wo:** `.werft-b01__mic` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-12 — Übergang background-color

- **Wo:** `.werft-b01__mic` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-04 — M-04

- **Wo:** `.werft-b01__proposal` — auf B-01
- **Auslöser:** erscheinen
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes M-04 in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-13 — Übergang transform

- **Wo:** `.werft-b01__proposal` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-07 — M-07

- **Wo:** `.werft-b01__bookmark.is-saved` — auf B-01
- **Auslöser:** erscheinen
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 180 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes M-07 in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 180, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-14 — Übergang transform

- **Wo:** `.werft-b01__todo` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-15 — Übergang color

- **Wo:** `.werft-b01__todo span` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 120 ms / 60 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, delayMillis = 60, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-16 — Übergang color

- **Wo:** `.werft-b01__nav-item` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-17 — Übergang opacity

- **Wo:** `.b02-back::after,
  .b02-mic-button::after` — auf B-02
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-08 — m-08-antwort

- **Wo:** `.b02-bubble-ai.b02-appearing`
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity, clip-path
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes m-08-antwort in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 400, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-18 — Übergang opacity

- **Wo:** `.b02-input-shell::after` — auf B-02
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-02 — m-02-atmen

- **Wo:** `.b02-mic-button[data-recording="true"] .b02-recording-ring` — auf B-02
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02-atmen in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-19 — Übergang background-color

- **Wo:** `.werft-b03__mic` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-02 — m-02

- **Wo:** `.werft-b03__mic.is-recording::before` — auf B-03
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-20 — Übergang background-color

- **Wo:** `.werft-b03__button` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-21 — Übergang color

- **Wo:** `.werft-b03__button` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-08 — m-08

- **Wo:** `.werft-b03__answer-shell` — auf B-03
- **Auslöser:** erscheinen
- **Was sich ändert:** grid-template-rows, opacity
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes m-08 in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 400, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-09 — m-09

- **Wo:** `.werft-b03__waiting-strip` — auf B-03
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 1800 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / normal
- **Quelle:** @keyframes m-09 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 1800, easing = LinearEasing), repeatMode = RepeatMode.Restart)`

### M-22 — Übergang opacity

- **Wo:** `.werft-b03__loading-card.is-ending` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 140 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 140, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-23 — Übergang opacity

- **Wo:** `.werft-b03__state-layer::after` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-24 — Übergang opacity

- **Wo:** `.werft-b08__control-shell::after,
  .werft-b08__action::after,
  .werft-b08__nav-row::after,
  .werft-b08__choice::after` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-25 — Übergang border-color

- **Wo:** `.werft-b08__switch-track` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-26 — Übergang background-color

- **Wo:** `.werft-b08__switch-track` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-27 — Übergang left

- **Wo:** `.werft-b08__switch-track::after` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** left
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-28 — Übergang background-color

- **Wo:** `.werft-b08__switch-track::after` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-29 — Übergang color

- **Wo:** `.werft-b08__choice span` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-30 — Übergang background-color

- **Wo:** `.werft-b08__choice span` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-31 — Übergang transform

- **Wo:** `.werft-b08__choice span` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-32 — Übergang opacity

- **Wo:** `.b09-back::after,
  .b09-improve::after,
  .b09-mic-button::after` — auf B-09
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-33 — Übergang opacity

- **Wo:** `.b09-improve:active` — auf B-09
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-02 — m-02-atmen

- **Wo:** `.b09-mic-wrap.is-recording .b09-mic-ring` — auf B-09
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02-atmen in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-34 — Übergang color

- **Wo:** `.b06-nav-item` — auf B-06
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-35 — Übergang opacity

- **Wo:** `.b06-nav-item::after` — auf B-06
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-36 — Übergang transform

- **Wo:** `.b07-tab-indicator` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-37 — Übergang color

- **Wo:** `.b07-tab` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-38 — Übergang opacity

- **Wo:** `.b07-tab::after,
  .b07-nav-item::after` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-39 — Übergang opacity

- **Wo:** `.b07-tab-panel` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-40 — Übergang visibility

- **Wo:** `.b07-tab-panel` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** visibility
- **Dauer / Verzögerung:** 0 ms / 200 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 0, delayMillis = 200, easing = LinearEasing)`

### M-41 — Übergang opacity

- **Wo:** `.b07-tab-panel.is-active` — auf B-01, B-07, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-42 — Übergang visibility

- **Wo:** `.b07-tab-panel.is-active` — auf B-01, B-07, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** visibility
- **Dauer / Verzögerung:** 0 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 0, easing = LinearEasing)`

### M-43 — Übergang transform

- **Wo:** `.b05-card`
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(0.4, 0, 0.2, 1)` → `cubic-bezier(0.4, 0, 0.2, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))`

### M-02 — m-02-atmen

- **Wo:** `.b05-capture.is-recording .b05-record-ring` — auf B-05
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02-atmen in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-44 — Übergang opacity

- **Wo:** `.b05-hit::after,
  .b05-card::after` — auf B-05
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-45 — Übergang transform

- **Wo:** `.b04-goal-card`
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-46 — Übergang opacity

- **Wo:** `.b04-goal-card::after`
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-47 — Übergang opacity

- **Wo:** `.b04-fab::after,
  .b04-mic::after,
  .b04-action-button::after,
  .b04-nav-item::after` — auf B-04
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-02 — m-02

- **Wo:** `.b04-record-state.is-recording .b04-record-ring` — auf B-04
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-48 — Übergang opacity

- **Wo:** `.b04-text-button` — auf B-04
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-49 — Übergang color

- **Wo:** `.b04-nav-item` — auf B-04
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-50 — Übergang background-color

- **Wo:** `.werft-b01,
.b02-screen,
.werft-b03,
.werft-b08,
.b09-screen,
.b06-screen,
.b07-screen,
.b05-screen,
.b04-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-51 — Übergang color

- **Wo:** `.werft-b01,
.b02-screen,
.werft-b03,
.werft-b08,
.b09-screen,
.b06-screen,
.b07-screen,
.b05-screen,
.b04-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-52 — Übergang border-color

- **Wo:** `.werft-b01__theme-control` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-53 — Übergang background-color

- **Wo:** `.werft-b01__theme-control` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-54 — Übergang color

- **Wo:** `.werft-b01__theme-button` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-55 — Übergang background-color

- **Wo:** `.werft-b01__theme-button` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-56 — Übergang transform

- **Wo:** `.werft-b01__theme-button` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-57 — Übergang opacity

- **Wo:** `.werft-b01__theme-button svg` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-58 — Übergang transform

- **Wo:** `.werft-b01__theme-button svg` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-59 — Übergang background-color

- **Wo:** `.werft-b01__topbar,
.b02-topbar,
.werft-b03__topbar,
.werft-b08__topbar,
.b09-topbar,
.b06-topbar,
.b07-topbar,
.b05-topbar,
.b04-topbar` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-60 — Übergang border-color

- **Wo:** `.werft-b01__topbar,
.b02-topbar,
.werft-b03__topbar,
.werft-b08__topbar,
.b09-topbar,
.b06-topbar,
.b07-topbar,
.b05-topbar,
.b04-topbar` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-61 — Übergang background-color

- **Wo:** `.werft-screen[data-screen-id="B-01"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-04"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-05"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-06"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-07"] nav.werft-primary-nav` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-62 — Übergang border-color

- **Wo:** `.werft-screen[data-screen-id="B-01"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-04"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-05"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-06"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-07"] nav.werft-primary-nav` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-63 — werft-screen-fade

- **Wo:** `.werft-screen[data-screen-id="B-01"][data-active="true"] .werft-b01,
.werft-screen[data-screen-id="B-04"][data-active="true"] .b04-screen,
.werft-screen[data-screen-id="B-05"][data-active="true"] .b05-screen,
.werft-screen[data-screen-id="B-06"][data-active="true"] .b06-screen,
.werft-screen[data-screen-id="B-07"][data-active="true"] .b07-screen,
.werft-screen[data-screen-id="B-08"][data-active="true"] .werft-b08` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `cubic-bezier(.4, 0, .6, 1)` → `cubic-bezier(0.4, 0, 0.6, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes werft-screen-fade in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f))`

### M-64 — werft-screen-detail

- **Wo:** `.werft-screen[data-screen-id="B-02"][data-active="true"] .b02-screen,
.werft-screen[data-screen-id="B-03"][data-active="true"] .werft-b03,
.werft-screen[data-screen-id="B-09"][data-active="true"] .b09-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity, transform
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes werft-screen-detail in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-65 — Übergang transform

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-66 — Übergang border-color

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-67 — Übergang background-color

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-68 — Übergang box-shadow

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** box-shadow
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-69 — Übergang border-color

- **Wo:** `.werft-b01__input,
.b02-input-shell,
.werft-b03__textarea,
.werft-b08__control-shell,
.b09-editor,
.b05-input,
.b04-goal-input` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-70 — Übergang background-color

- **Wo:** `.werft-b01__input,
.b02-input-shell,
.werft-b03__textarea,
.werft-b08__control-shell,
.b09-editor,
.b05-input,
.b04-goal-input` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-71 — Übergang transform

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-72 — Übergang color

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-73 — Übergang background-color

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-74 — Übergang border-color

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-75 — werft-screen-fade

- **Wo:** `.werft-screen[data-screen-id="B-01"][data-active="true"] .werft-b01,
  .werft-screen[data-screen-id="B-02"][data-active="true"] .b02-screen,
  .werft-screen[data-screen-id="B-03"][data-active="true"] .werft-b03,
  .werft-screen[data-screen-id="B-04"][data-active="true"] .b04-screen,
  .werft-screen[data-screen-id="B-05"][data-active="true"] .b05-screen,
  .werft-screen[data-screen-id="B-06"][data-active="true"] .b06-screen,
  .werft-screen[data-screen-id="B-07"][data-active="true"] .b07-screen,
  .werft-screen[data-screen-id="B-08"][data-active="true"] .werft-b08,
  .werft-screen[data-screen-id="B-09"][data-active="true"] .b09-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes werft-screen-fade in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-76 — Lichtgrund wandert

- **Wo:** Der Lichtgrund `E-01` — auf B-10, B-01 und allen Hauptbildschirmen
- **Auslöser:** dauerhaft
- **Was sich ändert:** Mittelpunkte der beiden Farbkreise (je bis zu 18 % der Breite)
- **Dauer / Verzögerung:** 24000 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** neu in dieser Fassung (Effekt `E-01`)
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 24000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-77 — Glasleiste verdichtet sich beim Scrollen

- **Wo:** Kopfleiste und untere Leiste (`E-03`, `E-09`) — auf B-10, B-01, B-04, B-05, B-06, B-07
- **Auslöser:** Scrollen
- **Was sich ändert:** Deckkraft der Glasfläche 60 % → 92 %, Weichzeichnung 12 → 24, Schatten
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung (Effekt `E-09`)
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-78 — Karte kippt zur Neigung des Geräts

- **Wo:** Lauf- und Wartekarten (`E-08`) — auf B-10
- **Auslöser:** dauerhaft, gesteuert vom Rotationsvektor des Geräts
- **Was sich ändert:** `rotationX`, `rotationY` (höchstens ±6°), Inhaltsversatz bis 4 dp
- **Dauer / Verzögerung:** Federphysik statt fester Dauer
- **Kurve:** `spring(dampingRatio = 0.75f, stiffness = 200f)`
- **Wiederholung / Richtung:** dauerhaft / folgt dem Sensor
- **Quelle:** neu in dieser Fassung (Effekt `E-08`)
- **Jetpack Compose:** `animateFloatAsState(targetValue = neigung, animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f))`

### M-79 — Plus-Knopf atmet

- **Wo:** Der schwebende Plus-Knopf (F-35) — auf B-10, nur im Zustand `LEER`
- **Auslöser:** dauerhaft
- **Was sich ändert:** Größe 100 % → 106 %, Schein 12 % → 22 %
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** neu in dieser Fassung, im Takt von `M-02`
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-80 — Anlegefläche fährt herein

- **Wo:** Die Anlegefläche (F-35) — auf B-10
- **Auslöser:** Druck auf den Plus-Knopf
- **Was sich ändert:** Versatz von unten (100 % → 0), Deckkraft, Weichzeichnung des Grundes 0 → 16
- **Dauer / Verzögerung:** 320 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-81 — Neue Karte fliegt ein und funkelt

- **Wo:** Eine neu angelegte Wartekarte (F-35) — auf B-10
- **Auslöser:** Speichern
- **Was sich ändert:** Größe 88 % → 100 %, Deckkraft 0 → 1, dazu `E-15` mit 12 Punkten
- **Dauer / Verzögerung:** 480 ms / 0 ms
- **Kurve:** `spring(dampingRatio = 0.6f, stiffness = 320f)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `spring(dampingRatio = 0.6f, stiffness = 320f)`

### M-82 — Übernommener Vorschlag fliegt zum Monitor-Feld

- **Wo:** Vorschlagskarte (F-36) — auf B-01
- **Auslöser:** Druck auf „In den Monitor"
- **Was sich ändert:** Position zur unteren Leiste, Größe 100 % → 24 %, Deckkraft 1 → 0
- **Dauer / Verzögerung:** 520 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung (Effekt `E-11`)
- **Jetpack Compose:** `tween(durationMillis = 520, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-83 — Karte wandert von „Steht an" nach „Läuft"

- **Wo:** Die gestartete Karte (F-37) — auf B-10
- **Auslöser:** Druck auf „Starten"
- **Was sich ändert:** Position, Flächenfarbe `Fläche` → `Erhöht`, Lichtsaum `E-06` setzt ein
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `spring(dampingRatio = 0.7f, stiffness = 260f)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `spring(dampingRatio = 0.7f, stiffness = 260f)`

### M-84 — Funken beim Start

- **Wo:** Über der gestarteten Karte (`E-15`) — auf B-10
- **Auslöser:** Druck auf „Starten"
- **Was sich ändert:** 24 Lichtpunkte steigen auf, Deckkraft 1 → 0, Größe 100 % → 40 %
- **Dauer / Verzögerung:** 1200 ms / 0 ms, je Punkt bis 200 ms Versatz
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 1200, easing = LinearEasing)`

### M-85 — Karte hebt sich beim Ziehen ab

- **Wo:** Wartekarte beim Verschieben (F-38) — auf B-10
- **Auslöser:** langer Druck
- **Was sich ändert:** Größe 100 % → 104 %, Schatten 6 → 24, Schein `E-05` setzt ein
- **Dauer / Verzögerung:** 160 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 160, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-86 — Karte klappt auf

- **Wo:** Lauf- und Wartekarten (F-40) — auf B-10
- **Auslöser:** Tippen
- **Was sich ändert:** Höhe, Deckkraft des zusätzlichen Inhalts, Drehung des Pfeils um 180°
- **Dauer / Verzögerung:** 280 ms / 0 ms
- **Kurve:** `spring(dampingRatio = 0.8f, stiffness = 300f)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `spring(dampingRatio = 0.8f, stiffness = 300f)`

### M-87 — Fortschrittsring füllt sich mit Leuchtspur

- **Wo:** Der Ring der heutigen Aufgaben auf einer Laufkarte — auf B-10
- **Auslöser:** Abhaken einer Aufgabe (F-08)
- **Was sich ändert:** Winkel des Rings, ein hellerer Punkt läuft der Kante voraus
- **Dauer / Verzögerung:** 600 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-88 — Zahl zählt hoch

- **Wo:** Die Zeile „Steht an: 4 · Läuft: 2" und alle Anzahlen (`E-20`) — auf B-10
- **Auslöser:** Änderung des Wertes
- **Was sich ändert:** der angezeigte Zahlenwert
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `animateIntAsState(targetValue = wert, animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))`

### M-89 — Schimmer über dem Skelett

- **Wo:** Ladeskelette (`E-13`) — auf allen Bildschirmen mit Ladezustand
- **Auslöser:** dauerhaft, solange geladen wird
- **Was sich ändert:** Versatz eines Lichtstreifens von −40 % nach 140 %
- **Dauer / Verzögerung:** 1400 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 1400, easing = LinearEasing), repeatMode = RepeatMode.Restart)`

### M-90 — Lichtsaum wandert um die Laufkarte

- **Wo:** Rand jeder laufenden Karte (`E-06`) — auf B-10
- **Auslöser:** dauerhaft, solange das Experiment läuft
- **Was sich ändert:** Winkel eines `sweepGradient` von 0° auf 360°
- **Dauer / Verzögerung:** 6000 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 6000, easing = LinearEasing), repeatMode = RepeatMode.Restart)`

### M-91 — Bildschirmwechsel mit Weichzeichnen und Skalieren

- **Wo:** Jeder Wechsel zwischen Bildschirmen (`E-12`) — alle Bildschirme
- **Auslöser:** Navigation
- **Was sich ändert:** abgehend Größe 100 % → 96 % und Weichzeichnung 0 → 12; ankommend 104 % → 100 % und Deckkraft 0 → 1
- **Dauer / Verzögerung:** 260 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung; ersetzt `M-63`/`M-75` (Werfts Vorschau-Blenden)
- **Jetpack Compose:** `tween(durationMillis = 260, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-92 — Geteiltes Element beim Wechsel

- **Wo:** Titel und Fläche einer Karte, die einen neuen Bildschirm öffnet (`E-11`) — B-10 → B-02, B-10 → B-03, B-01 → B-10
- **Auslöser:** Navigation aus einer Karte heraus
- **Was sich ändert:** Position, Größe und Eckenradius des geteilten Elements
- **Dauer / Verzögerung:** 300 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `SharedTransitionLayout` mit `sharedElement(rememberSharedContentState(key), boundsTransform = { _, _ -> tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)) })`

### M-93 — Lichtblüte beim Abschließen

- **Wo:** Über der abgeschlossenen Karte (`E-16`) — auf B-10 und B-03
- **Auslöser:** Abschluss eines Experiments (F-13)
- **Was sich ändert:** ein Lichtring dehnt sich von 0 auf 180 % der Kartenbreite, Deckkraft 0,6 → 0; dazu 40 Partikel
- **Dauer / Verzögerung:** 1600 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 1600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-94 — Leistenfeld leuchtet auf

- **Wo:** Ein Feld der unteren Leiste, wenn sich sein Inhalt ändert — alle Hauptbildschirme
- **Auslöser:** neues Experiment im Monitor (F-36), neue Erkenntnis (F-17)
- **Was sich ändert:** Schein `E-05` 0 % → 30 % → 0 %, Symbolgröße 100 % → 118 % → 100 %
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / alternate
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-95 — Gestaffeltes Erscheinen der Karten

- **Wo:** Alle Listen (`E-10`) — B-10, B-01, B-04, B-05, B-06, B-07
- **Auslöser:** erscheinen
- **Was sich ändert:** Deckkraft 0 → 1, Versatz von unten 16 dp → 0
- **Dauer / Verzögerung:** 240 ms / 60 ms je Eintrag, höchstens 480 ms Gesamtversatz
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung; verallgemeinert `M-04`
- **Jetpack Compose:** `tween(durationMillis = 240, delayMillis = (index * 60).coerceAtMost(480), easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

## 8. Reduzierte Bewegung

Meldet das System „Bewegung reduzieren“: Dauerbewegung aus, Übergänge auf reines Überblenden,
Dauern halbiert. Diese Regel gilt, solange das Funktions-Spec nichts anderes festlegt.

**Ergänzung dieser Fassung.** Zusätzlich zur Systemmeldung gibt es die Einstellung
**Effekt-Stärke** (F-41) mit den Stufen *Voll · Gedämpft · Aus*. Beide wirken zusammen:

| Lage | Was gilt |
|------|----------|
| System meldet „Bewegung reduzieren" | mindestens *Gedämpft*, auch wenn *Voll* gewählt ist |
| Energiesparmodus an | mindestens *Gedämpft* |
| *Gedämpft* | `M-76`, `M-78`, `M-79`, `M-84`, `M-89`, `M-90`, `M-93` aus; alle Übergänge bleiben |
| *Aus* | zusätzlich `M-77`, `M-80`, `M-81`, `M-82`, `M-83`, `M-85`, `M-86`, `M-87`, `M-91`, `M-92`, `M-95` auf reines Überblenden mit halbierter Dauer |

**Die Federbewegungen** (`M-78`, `M-81`, `M-83`, `M-86`) werden auf *Aus* durch
`tween(120, LinearEasing)` ersetzt, nicht ersatzlos gestrichen — sonst springt die Oberfläche.

**`M-63` und `M-75` (`werft-screen-fade`) sowie `M-64` (`werft-screen-detail`) gehören zu
Werfts Vorschau und sind nicht zu bauen.** An ihrer Stelle steht in dieser Fassung `M-91`.

## Teil D — Projekt, Rahmen und Abnahme

# Projekt — Experimente

> **Zielplattform für diesen Bau: Android (Kotlin / Jetpack Compose).**
> Beim Herunterladen aus Werft Studio am 2026-08-10 gewählt. Sie gilt vor jeder abweichenden
> Angabe weiter unten in dieser Datei.

Stand: 12.08.2026, 12.06 Uhr · Stufe: v1, ueberarbeitet (Monitor + Effekte) · Plattform(en): Android

> **Das Wichtigste zuerst.** Wenn später etwas gegeneinander steht, gilt diese Reihenfolge:
> **1. Die KI muss Frank wirklich kennen.** Selbstbild, 15-Tage-Log, Langzeit-Log und
> Erkenntnisse müssen spürbar in jeden Vorschlag hineinwirken. Im Zweifel gewinnt der Kontext.
> **2. Nichts darf verlorengehen.** Was eingesprochen wurde, ist in einem Jahr noch da —
> verdichtet, aber vollständig.
> **3. Die Vorschläge müssen wirklich neu sein.** Dinge, auf die Frank selbst nie gekommen wäre.
> Die reine Sprachbedienung steht dazu nicht in Konkurrenz — sie wird einmal gebaut und läuft.

---

## 0. Was sich in dieser Fassung geändert hat

Frank hat das Spec am 12.08.2026 in drei Punkten grundlegend geändert. Alles Übrige gilt
unverändert weiter.

**1. Ein neuer Bildschirm: der Monitor (`B-10`) — und er ist die Hauptseite.**
Der Monitor sammelt **alle Experimente, die Frank sich vorgenommen hat** — die er selbst
angelegt hat (`F-35`) genauso wie die, die er aus den KI-Vorschlägen übernommen hat (`F-36`).
Er zeigt sie in zwei Abschnitten: **„Läuft"** (höchstens drei) und **„Steht an"** (beliebig
viele). Beim Öffnen der App ist er als Erstes zu sehen.

**2. „Heute" (`B-01`) bleibt vollständig erhalten, ist aber nicht mehr der Start.**
Dort wird weiterhin die Lage eingesprochen und werden die fünf Vorschläge erzeugt. Neu ist
nur, dass ein gewählter Vorschlag jetzt **in den Monitor wandert**, statt sofort zu laufen —
gestartet wird dort (`F-37`). Der alte Weg „sofort starten" bleibt zusätzlich erhalten.

**3. Maximale Effekte, überall.** Die App soll durchgehend modern wirken. Dafür gibt es einen
neuen verbindlichen Abschnitt **Teil B §7 Effekte** mit den Kennungen `E-01` bis `E-24` und
zwanzig neue Bewegungen `M-76` bis `M-95`. Die frühere gestalterische Zurückhaltung
(„beruhigt sie, oder drängt sie?") ist damit **aufgehoben**. Zwei Grenzen bleiben: Die App
misst und bewertet Frank nicht, und kein Effekt trägt Information allein — auf der Stufe
*Aus* (`F-41`) ist alles vollständig bedienbar.

---

## 1. Zweck in drei Sätzen

„Experimente" schlägt Frank jeden Tag fünf persönliche Experimente vor — Dinge, die er so
noch nie gemacht hat, aus beliebigen Lebensbereichen, zugeschnitten auf seine aktuelle Lage
und auf alles, was die App über ihn weiß. Er übernimmt die, die ihn ansprechen, in seinen **Monitor** — den Bildschirm, auf dem
alles steht, was er sich vorgenommen hat, samt dem, was er sich selbst ausgedacht hat —,
startet von dort aus bis zu drei gleichzeitig, setzt sie über den Tag um und spricht abends
ein, was daraus geworden ist; die KI
schreibt daraufhin ihre eigene Einschätzung dazu. Aus diesen Auswertungen wachsen ein
Logbuch, eine dauerhafte Chronik und eine Erkenntnisliste, die die nächsten Vorschläge
immer genauer machen.

Anzeigename: **Experimente** · Kurzname (Ordner, Dateien): **Experimente**

---

## 2. Zielplattform(en)

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|-----------|----------------------|-------------|---------------------|
| Android | Samsung Galaxy S23 Ultra, Hochformat, 1440 × 3088 px (Standard 1080 × 2316) | Kotlin + Jetpack Compose, Material 3 | **Pflicht** |

`minSdk 26` · `targetSdk 36` · `compileSdk 36` · JVM-Ziel 17 — wie die übrigen
Android-Projekte im Repo (PerfectMoment, Cortex).

---

## 3. Rahmenbedingungen

**Sprache der Oberfläche:** Deutsch. Nur eine Sprache.

**Offline/Online:** Teilweise offline benutzbar.
- **Ohne Netz lesbar:** Logbuch (beide Reiter), Erkenntnisse, Merkliste, Wünsche & Ziele,
  Selbstbild, Einstellungen, laufende Experimente samt To-Do-Liste, Haken setzen.
- **Braucht Netz:** Vorschläge erzeugen und aktualisieren, Transkription, KI-Verbesserung,
  Gespräch, KI-Auswertung, Vorlesen, Logbuch-Fortschreibung.

**Konten/Anmeldung:** Einmalige Codex-Geräteanmeldung über Franks ChatGPT-Abo
(OAuth, Gerätecode-Verfahren wie in PerfectMoment). Kein eigenes Konto, keine Registrierung.

**Berechtigungen:**

| Berechtigung | Wofür | Wann gefragt | Bei Ablehnung |
|--------------|-------|--------------|---------------|
| `RECORD_AUDIO` | Alle Spracheingaben | Beim ersten Druck auf einen Sprechknopf | Hinweis mit Verweis in die Systemeinstellungen; die App bleibt bedienbar, aber ohne Sprache |
| `POST_NOTIFICATIONS` | Erinnerungen morgens/abends | Beim ersten Einschalten einer Erinnerung | Erinnerungen bleiben aus, sonst unverändert |
| `INTERNET` | Alle Dienste | — (normale Berechtigung) | — |

**Externe Dienste:**

| Dienst | Wofür | Zugang | Ohne Netz |
|--------|-------|--------|-----------|
| Codex (OpenAI, OAuth) | Vorschläge, Gespräch, Auswertung, Logbuch, KI-Verbesserung | Geräteanmeldung über das ChatGPT-Abo | Meldung „Dafür brauche ich Netz" |
| Groq | Transkription, `whisper-large-v3-turbo` | API-Schlüssel in den Einstellungen | dito |
| Google Cloud TTS | Vorlesen, Chirp 3 HD | API-Schlüssel in den Einstellungen | dito |
| Alibaba DashScope | Vorlesen mit Franks eigener Stimme | API-Schlüssel in den Einstellungen | dito |
| Microsoft Edge TTS | Vorlesen, Standardstimmen | kein Schlüssel nötig | dito |

**Datenhaltung:** Ausschließlich auf dem Gerät. Room-Datenbank für Inhalte, verschlüsselte
Einstellungen (`EncryptedSharedPreferences`, AES-256) für Schlüssel, Modellwahl und Theme —
wie `SecureSettings.kt` in PerfectMoment. Keine Cloud, kein Backup-Dienst, keine Übertragung
außer den oben genannten Aufrufen.

**Verteilung:** Privat. Kein Play Store, keine Weitergabe. Installation per `adb install -r`.

---

## 4. Ausdrücklich NICHT enthalten

- **Kein Onboarding und keine Einführung.** Die App ist nur für Frank; er weiß, was sie tut.
- **Keine Datenschutzerklärung, keine Einwilligungen, keine Store-Texte.**
- **Kein Mehrbenutzer-Betrieb**, kein Konto, kein Profilwechsel.
- **Keine Statistik, keine Diagramme, keine Streak-Zähler, keine Punkte, keine Abzeichen.**
  Die App misst Frank nicht und belohnt ihn nicht — sie schlägt vor und hört zu.
  *Das betrifft ausdrücklich **nicht** die Effekte aus Teil B §7:* Funken, Lichtblüten und
  Leuchtränder feiern einen Moment, sie zählen und bewerten nichts. Der Fortschrittsring auf
  einer Laufkarte zeigt den Stand der **heutigen** Aufgaben und nichts darüber hinaus — keine
  Serie, keine Quote, keinen Vergleich mit gestern.
- **Keine Erfolgs- oder Misserfolgsbewertung eines Experiments.** „Nicht gemacht" ist ein
  Ergebnis, kein Versagen.
- **Keine feste Bereichseinteilung des Lebens.** Es gibt keine sechs, zwölf oder zwanzig
  Kategorien — die KI zieht frei aus allem, was zu einem Menschenleben gehört.
- **Kein Export, kein Teilen, kein Drucken.**
- **Keine Cloud-Sicherung.**

---

## 5. Abnahme — wann ist es fertig

| Kennung | Kriterium |
|---------|-----------|
| **A-01** | Frank spricht morgens seine Lage ein, kann den Text mit KI verbessern lassen, und bekommt fünf Vorschläge: zwei zur Lage passend, zwei völlig neue, einer von der Merkliste |
| **A-02** | Der Aktualisieren-Knopf liefert fünf **andere** Vorschläge aus anderen Bereichen — keiner gleicht einem der zuvor gezeigten |
| **A-03** | Ein gewähltes Experiment erscheint als laufendes Experiment mit seiner Dauer und seiner Stufe |
| **A-04** | Bis zu **drei** Experimente sind gleichzeitig offen. Sind drei offen, kommen keine neuen Vorschläge, bis eines abgeschlossen ist |
| **A-05** | Die To-Do-Liste zeigt alle heutigen Aufgaben untereinander, nach Experimenten gruppiert mit dem Titel darüber. Gesetzte Haken überstehen einen Neustart |
| **A-06** | Frank führt zu einem laufenden Experiment ein Gespräch: sprechen, Antwort kommt und wird vorgelesen, mehrere Runden hintereinander |
| **A-07** | Abends werden alle offenen Experimente der Reihe nach ausgewertet. Mehrtägige bekommen einen Zwischenstand, am letzten Tag die volle KI-Auswertung |
| **A-08** | Die KI-Auswertung wird per Lautsprecher-Knopf vorgelesen — mit der in den Einstellungen gewählten Stimme |
| **A-09** | Ein nicht umgesetztes Experiment wird **trotzdem** ausgewertet und landet zurück auf der Merkliste |
| **A-10** | Das Logbuch zeigt die letzten 15 Tage ausführlich; ein Eintrag lässt sich ändern oder löschen |
| **A-11** | Ein Tag, der älter als 15 Tage wird, steht danach verdichtet im Langzeit-Log (höchstens 7 Zeilen) — er verschwindet nicht |
| **A-12** | Das Langzeit-Log enthält jedes gemachte Experiment mit Datum, Durchführung und Auswertung, dauerhaft |
| **A-13** | Die Erkenntnisliste wächst aus den Auswertungen und ist am Stück lesbar |
| **A-14** | In *Wünsche & Ziele* lassen sich mehrere Ziele hintereinander einsprechen, je mit KI-Verbesserung; die Vorschläge greifen sie auf |
| **A-15** | Auf der Merkliste lassen sich per Plus eigene Experiment-Ideen anlegen — einsprechen, verbessern lassen |
| **A-16** | In den Einstellungen sind Modell **und** Effort getrennt für Experimente und fürs Logbuch wählbar |
| **A-17** | Das Selbstbild nimmt beliebig viel Text auf und wirkt in jeden Vorschlag hinein |
| **A-18** | Morgens und abends erinnert die App zu den eingestellten Zeiten; beide Erinnerungen sind einzeln abschaltbar |
| **A-19** | Hell, Dunkel und Automatik sind vollständig umschaltbar; Automatik übernimmt sofort die Systemdarstellung und folgt späteren Systemwechseln ohne Neustart |
| **A-20** | Ohne Netz sind Logbuch, Chronik, Erkenntnisse, Merkliste und Ziele lesbar; alles Übrige meldet verständlich, dass es Netz braucht |
| **A-21** | Beim Starten der App erscheint der **Monitor** (`B-10`), nicht „Heute" |
| **A-22** | Der Monitor zeigt zwei Abschnitte: „Läuft" mit höchstens drei Experimenten und „Steht an" mit beliebig vielen; beide sind auch **ohne Netz** vollständig lesbar |
| **A-23** | Über den Plus-Knopf legt Frank ein **eigenes** Experiment an — einsprechen oder tippen, mit KI-Verbesserung — und es steht sofort unter „Steht an" |
| **A-24** | „In den Monitor" auf einer Vorschlagskarte legt den KI-Vorschlag unter „Steht an"; die übrigen vier Vorschläge bleiben stehen und lassen sich ebenfalls übernehmen |
| **A-25** | Beide Herkünfte stehen im selben Abschnitt gleichrangig nebeneinander und tragen ein sichtbares Herkunftsetikett |
| **A-26** | „Starten" macht aus einem anstehenden ein laufendes Experiment; laufen bereits drei, ist der Knopf gesperrt und nennt den Grund |
| **A-27** | Anstehende Karten lassen sich durch Ziehen umsortieren und nach links aus dem Monitor nehmen — wahlweise auf die Merkliste oder endgültig |
| **A-28** | Alle Effekte aus Teil B §7 sind am laufenden Programm zu sehen: Lichtgrund, Glasleisten, Schein, wandernder Rand, Federphysik, Kipp-Parallaxe, Funken beim Start, Lichtblüte beim Abschließen, Schimmer beim Laden, Wellenform bei der Aufnahme |
| **A-29** | In den Einstellungen stellt „Effekte" auf **Voll · Gedämpft · Aus** um; die Wahl wirkt sofort ohne Neustart, und auf *Aus* bleibt jede Funktion vollständig bedienbar |
| **A-30** | Meldet das System „Bewegung reduzieren" oder ist der Energiesparmodus an, gilt mindestens *Gedämpft*, auch wenn *Voll* eingestellt ist |

---

## 6. Offene Fragen

Keine. Alle Punkte wurden im Grilling entschieden.

*Zur Sicherheit festgehalten, weil es beim Bauen auffallen wird:* Zum Zeitpunkt des Specs
war **kein Android-Gerät angeschlossen** (`adb devices` leer). Vor der Installation in
Schritt 6 der Pipeline muss das Gerät verbunden sein.
