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
