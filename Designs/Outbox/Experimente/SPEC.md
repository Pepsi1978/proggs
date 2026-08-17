# Experimente-SPEC-v1 — Spec v2
Stand: 2026-08-10 · Plattform: Android (Kotlin / Jetpack Compose)
Erzeugt von: Werft Studio beim Herunterladen

Diese Datei ist die Zusammenstellung der drei Specs. Die Einzeldateien daneben sind wortgleich.

## Teil A — Funktions-Spec

# Funktions-Spec — Experimente

Stand: 09.08.2026 · Stufe: v1 · Plattform(en): Android

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
| F-27 NEU | Zwischen Hauptmonitoren wischen | B-01, B-04, B-05, B-06, B-07 | Kern |

F-01 bis F-26 gehören zur ersten Fassung; F-27 ist die neu ergänzte Wisch-Navigation. Es gibt kein „später".

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
  - Sind bereits **drei Experimente offen**, wird F-03 nicht ausgeführt (siehe F-06).
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

### F-06 — Experiment auswählen und starten

- **Auslöser:** Frank tippt eine Vorschlagskarte an und bestätigt.
- **Ablauf:**
  1. Aus dem Vorschlag wird ein `Experiment` mit Startdatum, Dauer, Stufe und der
     Aufgabenliste je Tag.
  2. Der Zustand wird `offen`. Die übrigen vier Vorschläge verschwinden.
  3. B-01 zeigt fortan das laufende Experiment und die To-Do-Liste (F-07).
  4. Ist der Vorschlag von der Merkliste gekommen, wird er dort entfernt.
- **Daten:** Geschrieben: `Experiment`, `Task`-Sätze. Gelesen: der gewählte `Suggestion`.
- **Ergebnis:** Ein laufendes Experiment, sichtbar auf B-01.
- **Fehlerfall:** Sind bereits drei Experimente offen, ist die Auswahl gesperrt und ein
  Hinweis erscheint: „Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues
  beginnst."
- **Regeln/Grenzen:** **Höchstens drei gleichzeitig offene Experimente.** Solange drei offen
  sind, erzeugt B-01 keine neuen Vorschläge (F-03 wird nicht ausgeführt).

### F-07 — To-Do-Liste des Tages

- **Auslöser:** Beim Anzeigen von B-01, wenn mindestens ein Experiment offen ist.
- **Ablauf:**
  1. Für jedes offene Experiment werden die Aufgaben **des heutigen Tages** ermittelt
     (bei mehrtägigen nur der Abschnitt dieses Tages).
  2. Es entsteht **eine einzige Liste** für den Tag, untereinander:
     Titel des Experiments 1 → seine heutigen Aufgaben → Titel des Experiments 2 → seine
     heutigen Aufgaben → und so weiter.
  3. Jede Aufgabe ist antippbar (F-08).
- **Daten:** Gelesen: `Experiment`, `Task` (gefiltert auf heute).
- **Ergebnis:** Frank sieht an einer Stelle, was heute zu tun ist.
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

- **Auslöser:** Gesprächs-Symbol an einem laufenden Experiment auf B-01 → öffnet B-02.
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

- **Auslöser:** Abends auf B-01 der Knopf „Wie ist es gelaufen?" oder die Abend-Erinnerung
  → öffnet B-03. Auch jederzeit manuell erreichbar.
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
  - **Umgesetzt:** Zustand wird `abgeschlossen`. Das Experiment wandert in die Chronik
    (F-14). Ein Platz der drei wird frei.
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

- **Auslöser:** B-08, Abschnitt „Erscheinung", oder der Schnellschalter in der oberen Leiste von B-01.
- **Ablauf:**
  1. In B-08 stehen die drei Möglichkeiten **Hell** · **Dunkel** · **Wie das System** direkt zur Wahl. Vorbelegung: **Dunkel**.
  2. Der Schnellschalter auf B-01 durchläuft sie in der festen Reihenfolge **Hell → Dunkel → Wie das System → Hell**. Sein Symbol kündigt den nächsten Modus eindeutig an: eine Sonne für Hell, eine klar gezeichnete Mondsichel für Dunkel und ein großes „A“ mit jeweils drei seitlichen Strahlen für Automatik.
  3. **Wie das System** übernimmt sofort die aktuelle Systemdarstellung, speichert den eigentlichen Moduswert `system` statt nur der gerade aufgelösten Farbe und folgt ohne Neustart jedem späteren Wechsel des Systems, solange dieser Modus aktiv bleibt.
- **Daten:** Geschrieben: Theme-Wert in den verschlüsselten Einstellungen.
- **Ergebnis:** Die Erscheinung wechselt sofort, ohne Neustart, und bleibt zwischen App-Starts erhalten.
- **Fehlerfall:** Keiner.
- **Regeln/Grenzen:** Beide Erscheinungen sind gleichrangig und vollständig gebaut; der Automatikmodus muss sowohl beim Einschalten als auch bei späteren Systemwechseln wirksam sein.

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

### `Experiment` — ein laufendes oder abgeschlossenes Experiment

| Feld | Typ | Pflicht | Standard | Wo |
|------|-----|---------|----------|-----|
| `id` | Long | ja | auto | Room |
| `title` | String | ja | — | Room |
| `description` | String | ja | — | Room |
| `days` | Int | ja | 1 | Room |
| `level` | Enum | ja | — | Room |
| `startedAt` | LocalDate | ja | — | Room |
| `state` | Enum (OFFEN, ABGESCHLOSSEN, NICHT_UMGESETZT) | ja | OFFEN | Room |
| `closedAt` | LocalDate? | nein | null | Room |

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

---

## 4. Zustände und Übergänge

### Der Tag auf B-01

```
LEER  ──F-01──▶  LAGE_STEHT  ──F-03──▶  VORSCHLAEGE  ──F-06──▶  LAEUFT
  ▲                                          │  ▲                   │
  │                                     F-04 └──┘                   │
  │                                                                 │
  └───────────── neuer Kalendertag ◀── ABEND ◀──F-10/F-11───────────┘
```

| Zustand | Was B-01 zeigt |
|---------|----------------|
| `LEER` | Frage „Wie ist deine Lage heute?" und der Sprechknopf |
| `LAGE_STEHT` | Der Lage-Text, bearbeitbar, mit „Text mit KI verbessern" und „Weiter" |
| `VORSCHLAEGE` | Fünf Karten, darunter „Andere Vorschläge" |
| `LAEUFT` | Bis zu drei laufende Experimente, darunter die eine To-Do-Liste des Tages |
| `ABEND` | Zusätzlich der Knopf „Wie ist es gelaufen?" (führt zu B-03) |

Sind drei Experimente offen, wird `VORSCHLAEGE` übersprungen — B-01 geht von `LAGE_STEHT`
direkt nach `LAEUFT` und zeigt den Hinweis, dass drei Experimente laufen.

### Ein Experiment

```
OFFEN ──letzte volle Auswertung──▶ ABGESCHLOSSEN
  │
  └──„Nicht umgesetzt"──▶ NICHT_UMGESETZT ──▶ zusätzlich zurück auf die Merkliste
```

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
| F-33 | Merkliste | Abbrechen | button | **OFFEN — beim Rückimport klären** |

> Zu den offenen Punkten wurde im Studio nicht gesagt, was sie tun sollen. Sie werden beim
> Rückimport erfragt und **nicht erfunden** — sonst entstünde beim Bauen ein toter Knopf.

## Navigation aus dem Design

Diese Elemente haben im Design bereits ein Ziel und brauchen keine eigene Funktion.

| Bildschirm | Element | führt zu |
|------------|---------|----------|
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
Stand: 2026-08-10 · Stufe: v2 · Plattform: Android (Kotlin / Jetpack Compose)


Alle Werte sind **deterministisch aus dem Design gemessen**, nicht geschätzt. Sie sind verbindlich.

## 1. Gestalterische Grundhaltung

Warm im Grundton, sachlich im Aufbau. Die Wärme kommt aus den Farben und der Serifen-Überschrift,
die Klarheit aus Typografie, Abständen und Ordnung — nicht aus zusätzlicher Farbe. Die App soll
sich anfühlen wie ein gut gemachtes Notizbuch, das zuhört, nicht wie ein Gerät, das misst: keine
Diagramme, keine Zähler, keine Abzeichen, kein Fortschrittsbalken. Frank benutzt sie morgens und
abends, und sie bittet ihn um Dinge, vor denen er sich womöglich drückt — eine Oberfläche, die
dabei zusätzlich antreibt, arbeitet gegen die Sache. Jede spätere Entscheidung misst sich daran:
*Beruhigt sie, oder drängt sie?*

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

| Kennung | Bildschirm | Start | führt zu | Dateien je Erscheinung |
|---------|------------|-------|----------|------------------------|
| B-01 | Heute (`B-01`) | ja | B-02, B-03, B-08 | `bildschirme/21dunkelstandard/…-heute.html`<br>`bildschirme/22hell/…-heute.html` |
| B-02 | Gespräch (`B-02`) | — | B-01 | `bildschirme/21dunkelstandard/…-gespr-ch.html`<br>`bildschirme/22hell/…-gespr-ch.html` |
| B-03 | Auswertung (`B-03`) | — | B-01 | `bildschirme/21dunkelstandard/…-auswertung.html`<br>`bildschirme/22hell/…-auswertung.html` |
| B-08 | Einstellungen (`B-08`) | — | B-09 | `bildschirme/21dunkelstandard/…-einstellungen.html`<br>`bildschirme/22hell/…-einstellungen.html` |
| B-09 | Selbstbild (`B-09`) | — | B-08 | `bildschirme/21dunkelstandard/…-selbstbild.html`<br>`bildschirme/22hell/…-selbstbild.html` |
| B-06 | Erkenntnisse (`B-06`) | — | — | `bildschirme/21dunkelstandard/…-erkenntnisse.html`<br>`bildschirme/22hell/…-erkenntnisse.html` |
| B-07 | Logbuch (`B-07`) | — | — | `bildschirme/21dunkelstandard/…-logbuch.html`<br>`bildschirme/22hell/…-logbuch.html` |
| B-05 | Merkliste (`B-05`) | — | — | `bildschirme/21dunkelstandard/…-merkliste.html`<br>`bildschirme/22hell/…-merkliste.html` |
| B-04 | Wünsche &amp; Ziele (`B-04`) | — | — | `bildschirme/21dunkelstandard/…-w-nsche-amp-ziele.html`<br>`bildschirme/22hell/…-w-nsche-amp-ziele.html` |

> **Achtung:** 1 gemessene Bildschirme wurden im Design nicht aufgebaut und fehlen hier: Experimente-SPEC-v1.

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
| 5 | Heutige Lage | textarea | **ohne Ziel und ohne Aufgabe — beim Rückimport klären** |
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
| 11 | Abbrechen | button | **ohne Ziel und ohne Aufgabe — beim Rückimport klären** |
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

## 9. Barrierefreiheit

Die App ist ausschließlich für Frank. Es gelten keine Store-Vorgaben. Festgelegt wurde nur,
was sich aus der Gestaltung ergibt:

- **Mindest-Tippfläche 48 × 48 dp** für jedes bedienbare Element (folgt aus „luftig, große
  Tippflächen").
- **Kontrast:** Text auf Grund erreicht in beiden Erscheinungen mindestens 7:1
  (Dunkel: `#F4EEE7` auf `#151210` · Hell: `#1E1915` auf `#F8F4EE`). Gedämpfter Text
  mindestens 4,5:1.
- **Große Systemschrift** wird übernommen; Karten wachsen mit, Texte werden nie abgeschnitten.
- **Reduzierte Bewegung** wird beachtet — siehe Motion-Spec §8.

---

## 10. Offene Fragen

- Design-Fakten stammen aus dem Spec-Paket (00-PROJEKT.md, 01-FUNKTIONS-SPEC.md, 02-UI-SPEC.md, 03-MOTION-SPEC.md), nicht aus Quellcode: die Software existiert noch nicht.
- Funktionen aus dem Spec — das ausloesende Bedienelement traegt data-werft-funktion mit dieser Kennung: F-01 = Lage einsprechen; F-02 = Text mit KI verbessern; F-03 = Fünf Vorschläge erzeugen; F-04 = Vorschläge aktualisieren; F-05 = Vorschlag auf die Merkliste legen; F-06 = Experiment auswählen und starten; F-07 = To-Do-Liste des Tages; F-08 = Aufgabe abhaken; F-09 = Gespräch zum Experiment; F-10 = Auswertung einsprechen; F-11 = KI-Auswertung erzeugen; F-12 = Auswertung vorlesen; F-13 = Experiment abschließen; F-14 = Logbuch fortschreiben; F-15 = Tagesverdichtung nach 15 Tagen; F-16 = Logbuch-Eintrag ändern oder löschen; F-17 = Erkenntnisse fortschreiben; F-18 = Merkliste: eigenes Experiment anlegen; F-19 = Merkliste: Eintrag löschen; F-20 = Wünsche & Ziele pflegen; F-21 = Selbstbild pflegen; F-22 = Modell und Effort wählen; F-23 = Stimme und Vorlesen einstellen; F-24 = Zugänge einrichten; F-25 = Erinnerungen einstellen; F-26 = Erscheinung umschalten


## Teil C — Motion-Spec

# Motion-Spec — Experimente-SPEC-v1
Stand: 2026-08-10 · Stufe: v2 · Plattform: Android (Kotlin / Jetpack Compose)


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

## 8. Reduzierte Bewegung

Meldet das System „Bewegung reduzieren“: Dauerbewegung aus, Übergänge auf reines Überblenden,
Dauern halbiert. Diese Regel gilt, solange das Funktions-Spec nichts anderes festlegt.

