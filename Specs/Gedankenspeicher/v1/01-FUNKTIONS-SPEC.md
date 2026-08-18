# Funktions-Spec — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

## 1. Überblick der Funktionen

| Kennung | Funktion | Bildschirm(e) | Stufe |
|---------|----------|---------------|-------|
| F-01 | Notiz einsprechen | B-01 | Kern |
| F-02 | Notiz tippen | B-01 | Kern |
| F-03 | Transkription mit Halluzinations-Abwehr | — (im Hintergrund zu F-01) | Kern |
| F-04 | Aufnahme puffern und später nachreichen | B-01 | Kern |
| F-05 | Überschrift erzeugen | B-01 | Kern |
| F-06 | Notiz vorlesen | B-01 | Kern |
| F-07 | Notiztext von der KI verbessern | B-01 | Kern |
| F-08 | Notiz bearbeiten, löschen, verschieben, kopieren | B-01, B-08 | Kern |
| F-09 | KI-Auswertung mit Rückfrage | B-01, B-03 | Kern |
| F-10 | Auswertungsprofile verwalten | B-04, B-06 | Kern |
| F-11 | Codex verbinden (Gerätecode) | B-04, B-05 | Kern |
| F-12 | Sitzung anlegen, umbenennen, löschen | B-02 | Kern |
| F-13 | Sitzung wechseln | B-01, B-02 | Kern |
| F-14 | Über alle Sitzungen suchen | B-07 | Kern |
| F-15 | Erscheinung wechseln | B-04 | Kern |
| F-16 | Sitzung als Markdown exportieren | B-02 | Kern |
| F-17 | Sicherung nach Google Drive | B-04 | später |
| F-18 | Stimme und Sprachdienst wählen | B-04 | Kern |

## 2. Funktionen im Einzelnen

### F-01 — Notiz einsprechen

- **Auslöser** — Frank tippt den Aufnahmeknopf in der Fußleiste von B-01 an. Ein zweiter
  Tipp auf denselben Knopf beendet die Aufnahme. Es wird **nicht** gehalten.
- **Ablauf**
  1. Beim allerersten Mal wird `RECORD_AUDIO` abgefragt. Bei Ablehnung endet der Ablauf mit
     einem erklärenden Hinweis; die Notiz entsteht nicht.
  2. Die Aufnahme startet. Der Knopf wechselt in den Aufnahmezustand (M-05), der Ring um ihn
     pulsiert im Takt der gemessenen Lautstärke.
  3. Aufgenommen wird als 16 kHz Mono WAV in den Cache-Ordner der App.
  4. Beim zweiten Tipp endet die Aufnahme. Sofort erscheint im Verlauf eine Notizkarte im
     Zustand *transkribiert gerade* — mit Zeitstempel, ohne Text, ohne Überschrift.
  5. Die Datei geht an F-03. Kommt Text zurück, füllt sich die Karte und F-05 startet.
  6. Kein Netz: die Karte geht in den Zustand *wartet auf Transkription*, siehe F-04.
- **Daten** — Gelesen: Mikrofon. Geschrieben: `Notiz` mit `erstelltAm`, `zustand`,
  `audioPfad`; nach der Transkription `text` und `zustand = fertig`.
- **Ergebnis** — Eine neue Notiz am Ende des Verlaufs der offenen Sitzung.
- **Fehlerfall**
  - Kein Mikrofonrecht → Hinweis, keine Notiz.
  - Aufnahme kürzer als 0,4 Sekunden → verworfen, kurzer Hinweis, keine Notiz.
  - Groq antwortet mit Fehler → Karte geht in den Zustand *Transkription fehlgeschlagen*
    mit einem Wiederholen-Knopf; die Audiodatei bleibt erhalten.
  - Alle Segmente von der Halluzinations-Abwehr verworfen → Karte geht in den Zustand
    *nichts verstanden* mit Wiederholen-Knopf; der Text bleibt leer, es wird **nichts**
    erfunden.
  - Anruf oder anderer Ton greift auf das Mikrofon zu → Aufnahme wird beendet und wie ein
    zweiter Tipp behandelt, das bisher Gesprochene geht in die Transkription.
- **Regeln/Grenzen** — Höchstdauer einer Aufnahme: 10 Minuten; danach wird von selbst
  beendet und transkribiert. Es läuft nie mehr als eine Aufnahme gleichzeitig. Läuft gerade
  eine Sprachausgabe (F-06), wird sie beim Start der Aufnahme gestoppt.

### F-02 — Notiz tippen

- **Auslöser** — Frank tippt in das Textfeld der Fußleiste von B-01 und drückt Senden.
- **Ablauf**
  1. Text wird übernommen, sobald er nicht leer ist.
  2. Eine Notizkarte entsteht sofort im Zustand *fertig*, mit Zeitstempel und Text.
  3. F-05 startet für die Überschrift.
  4. Das Textfeld leert sich, die Tastatur bleibt offen für die nächste Notiz.
- **Daten** — Geschrieben: `Notiz` mit `erstelltAm`, `text`, `quelle = getippt`.
- **Ergebnis** — Eine neue Notiz am Ende des Verlaufs.
- **Fehlerfall** — Leerer oder nur aus Leerzeichen bestehender Text: Senden bleibt wirkungslos.
- **Regeln/Grenzen** — Keine Längenbegrenzung. Das Feld wächst bis 6 Zeilen und scrollt dann.

### F-03 — Transkription mit Halluzinations-Abwehr

- **Auslöser** — Eine fertige Audiodatei aus F-01 (oder aus dem Puffer, F-04).
- **Ablauf**
  1. Die Datei geht an die Groq-Schnittstelle, Modell `whisper-large-v3-turbo`, Sprache
     Deutsch, mit Segment- und Wortzeitstempeln.
  2. Parallel misst die App die Sprachanteile der Aufnahme selbst (Energie- und
     Nulldurchgangs-Analyse).
  3. Die Antwort läuft durch **alle vier Abwehr-Schichten**, unverändert übernommen aus
     `PerfectMoment/audio/WhisperHallucinationFilter.kt`:
     - **Schicht 1 — Vorfilter:** Aufnahmen ohne messbare Sprache gehen gar nicht erst an
       die Schnittstelle.
     - **Schicht 2 — Segment-Vertrauen:** Segmente mit hoher `no_speech`-Wahrscheinlichkeit,
       schlechter mittlerer Log-Wahrscheinlichkeit oder auffälligem Kompressionsverhältnis
       werden verworfen.
     - **Schicht 3 — Zeitabgleich:** Segmente, in deren Zeitfenster die eigene Messung keine
       Sprache fand, werden verworfen. Würden dadurch **alle** Segmente fallen, bleibt das
       Ergebnis von Schicht 2 stehen — dann ist der Zeitversatz die wahrscheinlichere
       Erklärung als eine durchgängige Halluzination.
     - **Schicht 4 — Floskel-Sperre:** Bekannte Whisper-Standardsätze („Untertitel von …",
       „Vielen Dank fürs Zuschauen" und Verwandte) werden verworfen, wenn die Messung dazu
       keine Sprache fand.
- **Daten** — Gelesen: Audiodatei, Groq-Schlüssel aus den Einstellungen. Geschrieben:
  `Notiz.text`, `Notiz.zustand`.
- **Ergebnis** — Sauberer Text oder ein ausdrücklich leeres Ergebnis.
- **Fehlerfall** — Kein Schlüssel hinterlegt → Karte im Zustand *kein Groq-Schlüssel*, die
  Einstellungen sind von dort aus mit einem Tipp erreichbar. Zeitüberschreitung nach
  120 Sekunden → Zustand *Transkription fehlgeschlagen* mit Wiederholen-Knopf.
- **Regeln/Grenzen** — Nie mehr als drei Transkriptionen gleichzeitig; weitere warten in
  der Schlange, damit schnell hintereinander gesprochene Notizen ihre Reihenfolge behalten.

### F-04 — Aufnahme puffern und später nachreichen

- **Auslöser** — F-01 endet, aber es ist kein Netz da (oder Groq ist nicht erreichbar).
- **Ablauf**
  1. Die Notizkarte entsteht trotzdem, mit Zeitstempel und dem Zustand
     *wartet auf Transkription*. Die Audiodatei wandert aus dem Cache in den dauerhaften
     App-Speicher, damit Android sie nicht wegräumt.
  2. Die App beobachtet die Netzverbindung. Kommt sie zurück, werden alle wartenden
     Aufnahmen in der Reihenfolge ihrer Entstehung an F-03 gegeben.
  3. Jede Karte füllt sich an ihrer Stelle im Verlauf — sie springt nicht ans Ende.
  4. Nach erfolgreicher Transkription wird die Audiodatei gelöscht und F-05 startet.
- **Daten** — Geschrieben: `Notiz.audioPfad`, `Notiz.zustand`.
- **Ergebnis** — Kein Gedanke geht verloren, nur weil kein Netz da war.
- **Fehlerfall** — Schlägt das Nachreichen dreimal fehl, bleibt die Karte im Zustand
  *Transkription fehlgeschlagen* stehen und wird erst auf ausdrücklichen Knopfdruck erneut
  versucht. Die Audiodatei bleibt, bis der Text steht.
- **Regeln/Grenzen** — Das Nachreichen läuft nur, während die App offen ist. Es gibt keinen
  Hintergrunddienst und keine Benachrichtigung (siehe 00-PROJEKT §6, O-03).

### F-05 — Überschrift erzeugen

- **Auslöser** — Eine Notiz erreicht den Zustand *fertig* (aus F-01, F-02 oder F-04) und hat
  noch keine Überschrift.
- **Ablauf**
  1. Bis die Überschrift da ist, zeigt die Karte an ihrer Stelle die Uhrzeit als Platzhalter.
  2. Ein kurzer Codex-Aufruf im Hintergrund fordert eine Überschrift von höchstens
     48 Zeichen an, ohne Anführungszeichen und ohne Punkt am Ende.
  3. Die Überschrift erscheint mit einem Überblenden an der Stelle des Platzhalters (M-08).
- **Daten** — Gelesen: `Notiz.text`, Codex-Anmeldung. Geschrieben: `Notiz.ueberschrift`.
- **Ergebnis** — Jede Notiz trägt eine Überschrift, die ihren Inhalt benennt.
- **Fehlerfall** — Kein Netz oder Codex nicht verbunden → die Uhrzeit bleibt stehen; die
  Überschrift wird beim nächsten App-Start erneut versucht. Antwortet Codex länger als
  30 Sekunden, wird abgebrochen und die Uhrzeit bleibt.
- **Regeln/Grenzen** — Die Überschrift ist jederzeit von Hand änderbar (F-08). Eine von Hand
  geänderte Überschrift wird **niemals** von der KI überschrieben, auch nicht nach einer
  Textverbesserung.

### F-06 — Notiz vorlesen

- **Auslöser** — Frank tippt den Lautsprecher an einer Notizkarte oder an einer
  KI-Antwortkarte an.
- **Ablauf**
  1. Läuft bereits eine Sprachausgabe, wird sie sofort gestoppt.
  2. War es dieselbe Karte, endet der Ablauf hier — derselbe Knopf schaltet an und aus.
  3. Der Text wird in Absätze zerlegt (Leerzeile als Trenner; ein Absatz über 1000 Zeichen
     wird zusätzlich an Satzgrenzen geteilt). Logik unverändert aus
     `Experimente/tts/Absaetze.kt`.
  4. Der erste Absatz wird an den gewählten Sprachdienst gegeben und abgespielt. **Während
     er spricht, werden schon die nächsten zwei Absätze synthetisiert** (`VORAUS = 2`, aus
     `Experimente/tts/Vorleser.kt`), damit zwischen zwei Absätzen keine Pause entsteht.
  5. Der gerade gesprochene Absatz wird in der Karte hervorgehoben und wandert mit (M-09).
  6. Nach dem letzten Absatz kehrt der Lautsprecher in den Ruhezustand zurück.
- **Daten** — Gelesen: `Notiz.text` bzw. `KiAntwort.text`, Stimm-Einstellungen. Geschrieben:
  nichts Dauerhaftes; die synthetisierten Dateien liegen im Cache und werden nach dem
  Abspielen gelöscht.
- **Ergebnis** — Der Text wird ohne Aussetzer vorgelesen.
- **Fehlerfall** — Sprachdienst nicht erreichbar → einmalige Rückfrage „Mit der Stimme des
  Geräts vorlesen?"; bei Ja läuft es ohne Netz weiter, bei Nein endet es mit einem Hinweis.
  Schlägt ein einzelner Absatz fehl, wird er übersprungen und der nächste gesprochen, nicht
  der ganze Vorlesevorgang abgebrochen.
- **Regeln/Grenzen** — Es spricht immer nur eine Stimme in der ganzen App. Beim Verlassen
  des Bildschirms oder beim Start einer Aufnahme wird gestoppt.

### F-07 — Notiztext von der KI verbessern

- **Auslöser** — Frank tippt den Verbessern-Knopf an einer Notizkarte an.
- **Ablauf**
  1. Der Knopf geht in den Wartezustand, die Karte bekommt das wandernde Leuchten (M-07).
  2. Codex bekommt den Text mit dem Auftrag: Rechtschreibung, Zeichensetzung und Satzbau in
     Ordnung bringen, Füllwörter und Verhaspler des Sprechens entfernen, Absätze setzen —
     **den Inhalt nicht verändern, nichts hinzuerfinden, nichts weglassen**.
  3. Der ursprüngliche Wortlaut wird in `Notiz.textOriginal` gesichert, sofern dort noch
     nichts steht.
  4. Der verbesserte Text tritt an die Stelle des alten; die Karte zeigt nun einen
     Rückgängig-Knopf.
  5. Rückgängig stellt `textOriginal` wieder her und entfernt den Knopf.
- **Daten** — Gelesen: `Notiz.text`. Geschrieben: `Notiz.text`, `Notiz.textOriginal`,
  `Notiz.istVerbessert`.
- **Ergebnis** — Ein sauber lesbarer Text, aus dem sich jederzeit der Originalwortlaut
  zurückholen lässt.
- **Fehlerfall** — Kein Netz oder Codex nicht verbunden → Hinweis, Text bleibt unverändert.
  Zeitüberschreitung nach 60 Sekunden → dasselbe.
- **Regeln/Grenzen** — Nur einmal verbesserbar: nach einer Verbesserung zeigt die Karte
  Rückgängig statt Verbessern. Erst nach Rückgängig ist Verbessern wieder möglich. So bleibt
  `textOriginal` immer der wirklich gesprochene Wortlaut.

### F-08 — Notiz bearbeiten, löschen, verschieben, kopieren

- **Auslöser** — Langer Druck auf eine Notizkarte öffnet ein Menü mit vier Einträgen.
- **Ablauf**
  - **Bearbeiten** → öffnet B-08 mit Überschrift und Text in Eingabefeldern. Speichern
    schreibt beides zurück; eine von Hand geänderte Überschrift wird als solche markiert
    (siehe F-05).
  - **Löschen** → Rückfrage „Diese Notiz löschen?" mit Löschen/Abbrechen. Bei Löschen
    verschwindet die Karte (M-04) und die Notiz wird aus der Datenbank entfernt.
  - **In andere Sitzung verschieben** → Blatt mit der Liste aller Sitzungen. Nach der Wahl
    verschwindet die Notiz hier und erscheint dort am Ende, mit unverändertem Zeitstempel.
  - **Text kopieren** → Text in die Zwischenablage, kurze Bestätigung.
- **Daten** — Geschrieben: `Notiz.text`, `Notiz.ueberschrift`, `Notiz.ueberschriftVonHand`,
  `Notiz.sitzungId`; oder Löschung des Datensatzes.
- **Ergebnis** — Der Verlauf enthält, was er enthalten soll.
- **Fehlerfall** — Verschieben in dieselbe Sitzung: der Eintrag ist ausgegraut.
- **Regeln/Grenzen** — Löschen ist endgültig, es gibt keinen Papierkorb. Eine gelöschte
  Notiz, die bereits Teil einer KI-Auswertung war, ändert die Auswertung nicht rückwirkend.

### F-09 — KI-Auswertung mit Rückfrage

**Das ist die Kernfunktion der App.**

- **Auslöser** — Frank tippt den KI-Knopf in der Fußleiste von B-01 an.
- **Ablauf**
  1. Die App sammelt den Kontext: **alle Notizen der offenen Sitzung, die nach der letzten
     KI-Antwort entstanden sind.** Gab es in dieser Sitzung noch keine KI-Antwort, sind es
     alle Notizen der Sitzung.
  2. Ist diese Menge leer, erscheint der Hinweis „Seit der letzten Auswertung sind keine
     neuen Notizen dazugekommen" mit dem Angebot, die ganze Sitzung auszuwerten.
  3. B-03 fährt als Blatt von unten herein (M-03). Darin: die Zahl der einbezogenen Notizen,
     der Schalter **„ganze Sitzung einbeziehen"** (Standard aus), der Schalter **Websuche**
     (vorbelegt aus der Grundeinstellung, für diese eine Auswertung überstimmbar) und die
     Anzeige des aktiven Auswertungsprofils.
  4. Codex bekommt den Kontext mit dem Auftrag, **zuerst eine einzige Rückfrage zu stellen**,
     die sich erkennbar auf den Inhalt der Notizen bezieht — keine allgemeine Standardfrage.
     Die Frage wird im Blatt angezeigt. Die Logik dafür wird aus
     `PerfectMoment/auth/IntroQuestionPolicy.kt`, `StreamingQuestionDecoder.kt` und
     `QuestionResponseValidator.kt` übernommen.
  5. Frank antwortet: getippt, oder eingesprochen über einen Mikrofonknopf im Blatt (dieselbe
     Kette F-01 → F-03, nur landet das Ergebnis im Antwortfeld statt im Verlauf).
  6. Codex bekommt Kontext, Rückfrage, Antwort und den **Text des aktiven Auswertungsprofils**
     als Anweisung. Die Antwort wird verlangt **in Absätzen von je 6 bis 15 Zeilen**, mit
     Leerzeile zwischen den Absätzen — damit sie sich absatzweise vorlesen lässt (F-06).
  7. Das Blatt schließt sich. Im Verlauf entsteht eine KI-Antwortkarte, die sich beim
     Eintreffen des Textes füllt; währenddessen wandert das Leuchten darüber (M-07).
  8. Die Antwortkarte trägt einen Lautsprecher (F-06) und eine Fußzeile mit dem verwendeten
     Profil, dem Modell, der Effort-Stufe und ob Websuche an war.
  9. Diese Antwort ist ab jetzt die neue Grenze für die nächste Auswertung.
- **Daten** — Gelesen: `Notiz`-Datensätze der Sitzung, aktives `Auswertungsprofil`,
  Codex-Einstellungen. Geschrieben: `KiAntwort` mit `sitzungId`, `erstelltAm`, `rueckfrage`,
  `antwortDesNutzers`, `text`, `profilName`, `modell`, `effort`, `websucheAn`.
- **Ergebnis** — Eine Auswertung im Verlauf, die genau die Notizen berücksichtigt, die noch
  nicht ausgewertet waren.
- **Fehlerfall**
  - Codex nicht verbunden → das Blatt zeigt statt der Rückfrage den Hinweis „Codex ist nicht
    verbunden" und einen Knopf, der direkt zu B-05 führt.
  - Kein Netz → Hinweis im Blatt, kein Kontextverlust: das Blatt bleibt offen.
  - Codex antwortet mit einem Kontext-Limit-Fehler → die App meldet das ausdrücklich und
    schlägt vor, die Auswertung ohne „ganze Sitzung" zu wiederholen. Es werden **niemals
    stillschweigend Notizen weggelassen** (00-PROJEKT §6, O-04).
  - Frank schließt das Blatt vor seiner Antwort → nichts wird gespeichert, keine Antwortkarte
    entsteht, die Notizen bleiben unausgewertet.
  - Zeitüberschreitung nach 300 Sekunden → die halbfertige Antwortkarte wird durch einen
    Fehlerhinweis mit Wiederholen-Knopf ersetzt.
- **Regeln/Grenzen** — Es läuft immer nur eine Auswertung gleichzeitig; solange eine läuft,
  ist der KI-Knopf im Wartezustand. Eine begonnene Auswertung überlebt das Drehen des Geräts
  und das Auf- und Zuklappen.

### F-10 — Auswertungsprofile verwalten

- **Auslöser** — Frank öffnet in B-04 den Eintrag „Auswertungsprofile" (führt zu B-06).
- **Ablauf**
  1. B-06 zeigt **genau sechs** Profile untereinander, jedes mit Namen, Häkchen und
     Textvorschau.
  2. Ein Tipp auf ein Häkchen macht dieses Profil zum aktiven. Das Häkchen des vorherigen
     verschwindet dabei von selbst — **es ist zu keinem Zeitpunkt möglich, zwei Häkchen
     gleichzeitig zu setzen, und ebenso wenig, gar keines zu haben.**
  3. Ein Tipp auf ein Profil öffnet den Editor: Name und Anweisungstext, beide frei änderbar.
  4. Speichern schreibt zurück; die Änderung gilt ab der nächsten Auswertung.
- **Daten** — Geschrieben: `Auswertungsprofil.name`, `.anweisung`, `.istAktiv`.
- **Ergebnis** — Die KI antwortet in der Machart, die Frank vorgegeben hat.
- **Fehlerfall** — Ein Profil mit leerem Anweisungstext lässt sich nicht aktivieren; das
  Häkchen bleibt beim bisherigen, mit kurzem Hinweis.
- **Regeln/Grenzen** — Die Zahl sechs ist fest: keine Profile hinzufügbar, keine löschbar.
  Ein Zurücksetzen-Knopf je Profil stellt den Auslieferungstext wieder her (bei den drei
  leeren: leert sie).
- **Auslieferungszustand**

  | Nr. | Name | Anweisungstext | Häkchen |
  |-----|------|---------------|---------|
  | 1 | Kurz | „Antworte in höchstens zwei Absätzen. Nur das Wesentliche, keine Einleitung." | — |
  | 2 | Normal | „Antworte in drei bis fünf Absätzen zu je 6–10 Zeilen. Ordne die Notizen, benenne Zusammenhänge." | **✓ aktiv** |
  | 3 | Ausführlich | „Denke gründlich nach. Antworte in mindestens sechs Absätzen zu je 8–15 Zeilen, mit Herleitung, Gegenargumenten und konkreten nächsten Schritten." | — |
  | 4 | Eigenes Profil 1 | *(leer)* | — |
  | 5 | Eigenes Profil 2 | *(leer)* | — |
  | 6 | Eigenes Profil 3 | *(leer)* | — |

### F-11 — Codex verbinden (Gerätecode)

- **Auslöser** — Frank tippt in B-04 auf „Codex verbinden" (führt zu B-05).
- **Ablauf** — Unverändert aus `PerfectMoment/auth/CodexAuthManager.kt` und
  `DeviceCodeFormat.kt`:
  1. Die App fordert einen Gerätecode an und zeigt ihn in zwei Blöcken — **vier Zeichen,
     Trennstrich, fünf Zeichen** — groß und gut ablesbar, dazu die Adresse zum Öffnen.
  2. Ein Knopf öffnet die Adresse im Browser, ein zweiter kopiert den Code.
  3. Die App fragt im Hintergrund regelmäßig nach, ob die Anmeldung bestätigt wurde.
  4. Sobald sie bestätigt ist, werden die Zeichen verschlüsselt im Android-Keystore
     abgelegt, B-05 schließt sich und B-04 zeigt „verbunden" mit dem Kontonamen.
  5. Ein Trennen-Knopf löscht die Anmeldung.
- **Daten** — Geschrieben: Zugangsdaten in `EncryptedSharedPreferences`.
- **Ergebnis** — Überschriften, Textverbesserung und Auswertung funktionieren.
- **Fehlerfall** — Code abgelaufen → neuer Code auf Knopfdruck. Anmeldung abgelehnt →
  Hinweis, nichts gespeichert. Kein Netz → Hinweis, der Bildschirm bleibt offen.
- **Regeln/Grenzen** — Zusätzlich in B-04 einstellbar: **Modell** (GPT 5.6 Sol / Terra /
  Luna) und **Effort-Stufe** (minimal / niedrig / mittel / hoch), beide aus
  `PerfectMoment/auth/CodexModels.kt`. Vorbelegung: Luna, Effort mittel.

### F-12 — Sitzung anlegen, umbenennen, löschen

- **Auslöser** — Knopf „Neue Sitzung" oben in der Schublade B-02; langer Druck auf eine
  Sitzung öffnet ihr Menü.
- **Ablauf**
  - **Anlegen** → eine leere Sitzung entsteht, wird geöffnet, die Schublade schließt sich.
    Bis zur ersten Notiz heißt sie „Neue Sitzung". Sobald die erste Notiz fertig ist,
    erzeugt Codex daraus einen Sitzungstitel von höchstens 40 Zeichen.
  - **Umbenennen** → Dialog mit Textfeld. Ein von Hand vergebener Titel wird **nie** wieder
    von der KI überschrieben.
  - **Löschen** → Rückfrage, die die Zahl der enthaltenen Notizen nennt. Bei Bestätigung
    werden Sitzung, Notizen und KI-Antworten gelöscht.
- **Daten** — Geschrieben/gelöscht: `Sitzung`, zugehörige `Notiz`- und `KiAntwort`-Datensätze.
- **Ergebnis** — Die Sitzungsliste bildet Franks Themen ab.
- **Fehlerfall** — Wird die letzte Sitzung gelöscht, legt die App sofort eine neue leere an
  und öffnet sie; die App steht nie ohne Sitzung da.
- **Regeln/Grenzen** — Sitzungen sind nach der Zeit der letzten Notiz sortiert, die zuletzt
  benutzte oben.

### F-13 — Sitzung wechseln

- **Auslöser** — Tipp auf eine Sitzung in B-02, oder Wischen von links am Bildschirmrand.
- **Ablauf** — Der Verlauf wird ausgetauscht, die Schublade schließt sich (M-02), der
  Verlauf springt an sein Ende (neueste Notiz sichtbar).
- **Daten** — Gelesen: Notizen und KI-Antworten der Sitzung. Geschrieben: `zuletztGeoeffnet`.
- **Ergebnis** — Der Verlauf zeigt das gewählte Thema.
- **Fehlerfall** — Läuft gerade eine Aufnahme oder eine Auswertung, ist der Wechsel gesperrt
  und die App sagt kurz, warum.
- **Regeln/Grenzen** — Die zuletzt geöffnete Sitzung wird beim nächsten App-Start wieder
  geöffnet.

### F-14 — Über alle Sitzungen suchen

- **Auslöser** — Tipp auf das Lupensymbol in der Kopfleiste von B-01 (führt zu B-07).
- **Ablauf**
  1. Eingabefeld, Suche ab dem zweiten Zeichen, bei jedem Tastendruck aktualisiert.
  2. Gesucht wird in Überschriften und Texten **aller** Notizen aller Sitzungen sowie in den
     Texten der KI-Antworten.
  3. Treffer nach Sitzung gruppiert, je Treffer Überschrift, Zeitstempel und die Textstelle
     mit hervorgehobenem Suchwort.
  4. Ein Tipp öffnet die Sitzung und scrollt genau zu dieser Notiz, die kurz aufleuchtet.
- **Daten** — Gelesen: alle Notizen und KI-Antworten (Room-Volltextsuche). Geschrieben: nichts.
- **Ergebnis** — Auch ein Wochen alter Gedanke ist in Sekunden auffindbar.
- **Fehlerfall** — Kein Treffer → freundlicher Leerzustand mit dem gesuchten Wort.
- **Regeln/Grenzen** — Groß- und Kleinschreibung wird nicht unterschieden. Es wird ohne Netz
  gesucht.

### F-15 — Erscheinung wechseln

- **Auslöser** — Vier Kacheln in B-04.
- **Ablauf** — Die gewählte Erscheinung greift sofort und überall, mit einem Überblenden
  von 240 ms (M-10).
- **Daten** — Geschrieben: `Einstellung.erscheinung`.
- **Ergebnis** — Die ganze App, einschließlich Schublade, Blätter und Dialoge, steht in der
  neuen Erscheinung.
- **Fehlerfall** — Keiner.
- **Regeln/Grenzen** — Vier Erscheinungen: Hell, Dunkel, Gold-Hell, Gold-Dunkel. Vorbelegung:
  Gold-Dunkel. Es gibt **keine** Kopplung an die Systemeinstellung — Frank wählt selbst.

### F-16 — Sitzung als Markdown exportieren

- **Auslöser** — Eintrag „Exportieren" im Sitzungsmenü (langer Druck in B-02).
- **Ablauf**
  1. Die App erzeugt eine Markdown-Datei: Sitzungstitel als Überschrift, danach je Notiz
     Zeitstempel, Überschrift und Text, dazwischen die KI-Antworten mit ihrer Rückfrage und
     Franks Antwort.
  2. Der Android-Teilen-Dialog öffnet sich mit der fertigen Datei.
- **Daten** — Gelesen: alles zur Sitzung. Geschrieben: eine Datei im Cache.
- **Ergebnis** — Die Sitzung liegt außerhalb der App vor.
- **Fehlerfall** — Kein Speicherplatz → Hinweis.
- **Regeln/Grenzen** — Dateiname: `<Sitzungstitel>-<JJJJ-MM-TT>.md`.

### F-17 — Sicherung nach Google Drive

- **Auslöser** — Schalter in B-04; danach automatisch.
- **Ablauf** — Übernommen aus `PerfectMoment/backup/DriveAuth.kt`:
  1. Einmalige Google-Anmeldung.
  2. Die Datenbankdatei wird in einen App-eigenen Drive-Ordner hochgeladen — beim ersten
     Einschalten sofort, danach jeweils beim Schließen der App, sofern sich etwas geändert
     hat und WLAN verfügbar ist.
  3. B-04 zeigt Zeitpunkt und Größe der letzten Sicherung.
  4. Ein Knopf „Aus Sicherung wiederherstellen" ersetzt nach ausdrücklicher Rückfrage den
     gesamten Datenbestand.
- **Daten** — Gelesen/geschrieben: die Room-Datenbankdatei.
- **Ergebnis** — Ein Gerätewechsel kostet keine Notizen.
- **Fehlerfall** — Anmeldung abgelaufen → Hinweis in B-04, Sicherung pausiert. Upload
  fehlgeschlagen → beim nächsten Schließen erneut versucht, kein Datenverlust.
- **Regeln/Grenzen** — Nur über WLAN, nie über Mobilfunk. Es werden fünf Stände vorgehalten,
  der älteste fällt heraus. Stufe: **später** — die App ist ohne diese Funktion vollständig.

### F-18 — Stimme und Sprachdienst wählen

- **Auslöser** — Eintrag „Stimme" in B-04.
- **Ablauf** — Auswahl unter vier Diensten, unverändert aus `Experimente/tts/TtsCatalog.kt`:

  | Dienst | Kennung | Stimmen | Netz nötig |
  |--------|---------|---------|-----------|
  | Microsoft Edge | `edge_tts` | 6 deutsche Stimmen, Vorbelegung Seraphina | ja |
  | Google Chirp 3 HD | `google_cloud` | 30+ deutsche Stimmen, Vorbelegung Kore | ja, mit Schlüssel |
  | Meine Stimme (Qwen-Klon) | `qwen_clone` | Franks eigene, aus einer Sprachprobe | ja, mit Schlüssel |
  | Stimme des Geräts | `geraet` | was Android mitbringt | nein |

  Ein Probe-Knopf spricht einen festen Beispielsatz.
- **Daten** — Geschrieben: `Einstellung.ttsDienst`, `.ttsStimme`.
- **Ergebnis** — Alles Vorlesen (F-06) läuft über den gewählten Dienst.
- **Fehlerfall** — Fehlender Schlüssel → der Dienst ist ausgegraut und nennt, was fehlt.
- **Regeln/Grenzen** — Vorbelegung: Microsoft Edge, Stimme Seraphina — sie braucht keinen
  Schlüssel und klingt am besten von den kostenlosen.

## 3. Datenmodell

### Sitzung
| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id` | Long | ja | automatisch | Room |
| `titel` | String | ja | „Neue Sitzung" | Room |
| `titelVonHand` | Boolean | ja | false | Room |
| `erstelltAm` | Long (ms) | ja | jetzt | Room |
| `zuletztGeoeffnet` | Long (ms) | ja | jetzt | Room |

### Notiz
| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id` | Long | ja | automatisch | Room |
| `sitzungId` | Long | ja | — | Room |
| `erstelltAm` | Long (ms) | ja | jetzt | Room |
| `text` | String | nein | „" | Room |
| `textOriginal` | String? | nein | null | Room |
| `ueberschrift` | String? | nein | null | Room |
| `ueberschriftVonHand` | Boolean | ja | false | Room |
| `quelle` | Enum (`gesprochen`, `getippt`) | ja | — | Room |
| `zustand` | Enum (siehe §4) | ja | — | Room |
| `audioPfad` | String? | nein | null | Dateisystem |
| `istVerbessert` | Boolean | ja | false | Room |
| `versucheTranskription` | Int | ja | 0 | Room |

### KiAntwort
| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `id` | Long | ja | automatisch | Room |
| `sitzungId` | Long | ja | — | Room |
| `erstelltAm` | Long (ms) | ja | jetzt | Room |
| `rueckfrage` | String | ja | — | Room |
| `antwortDesNutzers` | String | ja | — | Room |
| `text` | String | ja | — | Room |
| `profilName` | String | ja | — | Room |
| `modell` | String | ja | — | Room |
| `effort` | String | ja | — | Room |
| `websucheAn` | Boolean | ja | — | Room |
| `ganzeSitzung` | Boolean | ja | false | Room |

`Notiz` und `KiAntwort` erscheinen im Verlauf gemeinsam, nach `erstelltAm` sortiert.

### Auswertungsprofil
| Feld | Typ | Pflicht | Standard | Gespeichert |
|------|-----|---------|----------|-------------|
| `nummer` | Int (1–6) | ja | — | Room |
| `name` | String | ja | siehe F-10 | Room |
| `anweisung` | String | nein | siehe F-10 | Room |
| `istAktiv` | Boolean | ja | nur Nr. 2 | Room |

### Einstellung (einzeilige Tabelle)
| Feld | Typ | Standard |
|------|-----|----------|
| `erscheinung` | Enum (`hell`, `dunkel`, `goldHell`, `goldDunkel`) | `goldDunkel` |
| `ttsDienst` | Enum | `edge_tts` |
| `ttsStimme` | String | `de-DE-SeraphinaMultilingualNeural` |
| `codexModell` | Enum (Sol/Terra/Luna) | Luna |
| `codexEffort` | Enum (minimal/niedrig/mittel/hoch) | mittel |
| `websucheGrundhaltung` | Enum (`aus`, `immer`, `kiEntscheidet`) | `aus` |
| `driveSicherungAn` | Boolean | false |

Groq-Schlüssel, Google-Cloud-Schlüssel, Qwen-Schlüssel und die Codex-Anmeldung liegen
**nicht** in Room, sondern in `EncryptedSharedPreferences`.

## 4. Zustände und Übergänge

**Notiz-Zustand:**

```
aufnehmend ─▶ transkribiertGerade ─▶ fertig
     │                 │
     │                 ├─▶ transkriptionFehlgeschlagen ─(Wiederholen)─▶ transkribiertGerade
     │                 └─▶ nichtsVerstanden ────────────(Wiederholen)─▶ transkribiertGerade
     └─(kein Netz)─▶ wartetAufTranskription ─(Netz da)─▶ transkribiertGerade
```

Getippte Notizen beginnen direkt bei `fertig`.

**Auswertungs-Zustand (F-09):**

```
bereit ─(KI-Knopf)─▶ fragtNach ─(Antwort)─▶ wertetAus ─▶ bereit
                          │                     │
                          └─(Blatt zu)─▶ bereit └─(Fehler)─▶ bereit
```

**Vorlese-Zustand (F-06):** `still → spricht(Absatz n) → still`. Nur ein Vorgang zur Zeit;
jeder neue Start beendet den laufenden.

## 5. Externe Dienste

| Dienst | Wofür | Schlüssel / Anmeldung | Verhalten ohne Netz |
|--------|-------|----------------------|--------------------|
| Groq | Transkription (`whisper-large-v3-turbo`) | API-Schlüssel, von Hand in B-04 eingetragen, verschlüsselt abgelegt | Aufnahme wird gepuffert (F-04) |
| ChatGPT Codex | Überschriften (F-05), Textverbesserung (F-07), Auswertung (F-09), Sitzungstitel (F-12) | Gerätecode-Anmeldung (F-11) | Funktionen melden „kein Netz", nichts geht verloren |
| Microsoft Edge TTS | Vorlesen | keiner | Angebot, auf die Gerätestimme zu wechseln |
| Google Cloud TTS | Vorlesen (Chirp 3 HD) | API-Schlüssel in B-04 | dito |
| Qwen | Vorlesen mit geklonter Stimme | API-Schlüssel in B-04 | dito |
| Gerätestimme | Vorlesen | keiner | funktioniert ohne Netz |
| Google Drive | Sicherung (F-17) | Google-Anmeldung in B-04 | Sicherung pausiert |

## 6. Hintergrund und Lebenszyklus

| Lage | Verhalten |
|------|-----------|
| App geht in den Hintergrund **während einer Aufnahme** | Die Aufnahme wird beendet und wie ein zweiter Tipp behandelt: das bisher Gesprochene geht in die Transkription. Es entsteht **keine** halbe Notiz (A-13). |
| App geht in den Hintergrund **während des Vorlesens** | Die Sprachausgabe stoppt. Es wird nicht im Hintergrund weitergesprochen. |
| App geht in den Hintergrund **während einer Auswertung** | Der Codex-Aufruf läuft im ViewModel weiter. Kehrt Frank binnen der Prozesslebensdauer zurück, findet er die fertige Antwort vor. |
| App wird beendet | Nichts läuft weiter. Wartende Aufnahmen (F-04) bleiben auf der Platte und werden beim nächsten Start nachgereicht. |
| Gerät wird auf- oder zugeklappt | Der Zustand bleibt vollständig erhalten: offene Sitzung, Scrollposition, offenes Blatt, laufende Auswertung. |
| Gerät wird gedreht | Ebenso. |

## 7. Offene Fragen

Siehe `00-PROJEKT.md` §6 (O-01 bis O-04). Darüber hinaus ist im Funktionsbereich nichts offen.
