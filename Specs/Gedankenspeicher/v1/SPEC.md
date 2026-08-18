# SPEC — Gedankenspeicher

Stand: 18.08.2026 · Stufe: v1 · Plattform: Android (Galaxy Z Fold 8)

Dieses Dokument enthaelt die drei Specs vollstaendig und woertlich. Es ist allein
verstaendlich — es liegt keine weitere Datei vor, die man daneben lesen muesste.

- **Teil A** — Funktion: was die App kann
- **Teil B** — Aussehen: wie sie aussieht
- **Teil C** — Bewegung: wie sie sich bewegt
- **Teil D** — Rahmen und Abnahme: Plattform, Grenzen, wann sie fertig ist

Der Auftrag an den Designer und die Regeln fuer den Ruecklauf stehen in `LIESMICH.md`.

---

# TEIL A — FUNKTION


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


---

# TEIL B — AUSSEHEN

# UI-Spec — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/Gedankenspeicher/v2/messung/`. Widerspricht ein Satz
> von hier der Messung, ist der Satz überholt — nicht die Messung falsch.

## 1. Gestalterische Grundhaltung

Gedankenspeicher ist ein Ort zum Denken, kein Werkzeugkasten. Der Verlauf ist ruhig, dunkel
und gut lesbar; die Notizen liegen als schwebende Karten darin, mit weichem Schatten und
einem leisen Verlauf, der ihnen Tiefe gibt, ohne vom Text abzulenken.

Die Tiefe steckt in den Rändern, nicht in der Mitte: Kopf- und Fußleiste sind milchiges Glas
mit Unschärfe, unter dem der Verlauf durchscheint und hindurchgleitet. Der Aufnahmeknopf ist
das einzige Bauteil, das leuchtet — er trägt eine weiche Aura in der Akzentfarbe und ist
damit auch mit einem Blick von weitem zu finden.

Die goldenen Erscheinungen sind warmes Altgold, nicht glänzendes Messing: gedeckt, matt,
wertig. Gold ist Akzent, niemals Fläche — es sitzt auf Rändern, Symbolen, dem Aufnahmering
und dem KI-Knopf, und trägt nie einen ganzen Hintergrund.

Vier Erscheinungen sind **gleichrangig**. Jede ist vollständig durchgestaltet, keine ist
eine abgedunkelte Variante einer anderen.

## 2. Erscheinungen (Themes)

### 2.1 Gold-Dunkel *(Vorbelegung)*

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#0D0B07` | Grundfläche aller Bildschirme |
| `hintergrundErhoben` | `#17140D` | Notizkarten |
| `hintergrundGlas` | `rgba(23, 20, 13, 0.72)` | Kopf- und Fußleiste, Blätter (mit 24 px Unschärfe) |
| `rand` | `rgba(201, 162, 39, 0.22)` | Kartenränder, Trennlinien |
| `akzent` | `#C9A227` | Aufnahmering, KI-Knopf, aktive Symbole, Häkchen |
| `akzentGedeckt` | `rgba(201, 162, 39, 0.14)` | Aura, Hervorhebung des gesprochenen Absatzes |
| `textStark` | `#F4EFE2` | Notiztext, KI-Antworten |
| `textMittel` | `#B8AE97` | Überschriften der Karten |
| `textSchwach` | `#77705F` | Zeitstempel, Fußzeilen |
| `fehler` | `#E0645C` | Fehlerzustände |
| `erfolg` | `#7FB069` | Bestätigungen |
| `kiKarte` | `#1C1710` | Hintergrund der KI-Antwortkarte |
| `kiKarteRand` | `rgba(201, 162, 39, 0.45)` | Rand der KI-Antwortkarte — sie hebt sich ab |

### 2.2 Gold-Hell

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#FBF7EE` | Grundfläche |
| `hintergrundErhoben` | `#FFFFFF` | Notizkarten |
| `hintergrundGlas` | `rgba(251, 247, 238, 0.78)` | Leisten, Blätter (24 px Unschärfe) |
| `rand` | `rgba(166, 124, 0, 0.20)` | Kartenränder, Trennlinien |
| `akzent` | `#A67C00` | Aufnahmering, KI-Knopf, aktive Symbole |
| `akzentGedeckt` | `rgba(166, 124, 0, 0.12)` | Aura, Absatz-Hervorhebung |
| `textStark` | `#231E14` | Notiztext |
| `textMittel` | `#5E5647` | Kartenüberschriften |
| `textSchwach` | `#938A76` | Zeitstempel |
| `fehler` | `#B3261E` | Fehlerzustände |
| `erfolg` | `#4C7A34` | Bestätigungen |
| `kiKarte` | `#FDF9F0` | KI-Antwortkarte |
| `kiKarteRand` | `rgba(166, 124, 0, 0.42)` | Rand der KI-Antwortkarte |

### 2.3 Dunkel *(neutral)*

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#0B0C0E` | Grundfläche |
| `hintergrundErhoben` | `#16181C` | Notizkarten |
| `hintergrundGlas` | `rgba(22, 24, 28, 0.72)` | Leisten, Blätter |
| `rand` | `rgba(255, 255, 255, 0.10)` | Ränder, Trennlinien |
| `akzent` | `#3B82F6` | Aufnahmering, KI-Knopf, aktive Symbole |
| `akzentGedeckt` | `rgba(59, 130, 246, 0.16)` | Aura, Absatz-Hervorhebung |
| `textStark` | `#ECEFF4` | Notiztext |
| `textMittel` | `#A8AFBA` | Kartenüberschriften |
| `textSchwach` | `#6B7280` | Zeitstempel |
| `fehler` | `#EF4444` | Fehlerzustände |
| `erfolg` | `#22C55E` | Bestätigungen |
| `kiKarte` | `#111820` | KI-Antwortkarte |
| `kiKarteRand` | `rgba(59, 130, 246, 0.45)` | Rand der KI-Antwortkarte |

### 2.4 Hell *(neutral)*

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#F6F7F9` | Grundfläche |
| `hintergrundErhoben` | `#FFFFFF` | Notizkarten |
| `hintergrundGlas` | `rgba(246, 247, 249, 0.78)` | Leisten, Blätter |
| `rand` | `rgba(17, 24, 39, 0.10)` | Ränder, Trennlinien |
| `akzent` | `#2563EB` | Aufnahmering, KI-Knopf, aktive Symbole |
| `akzentGedeckt` | `rgba(37, 99, 235, 0.10)` | Aura, Absatz-Hervorhebung |
| `textStark` | `#111827` | Notiztext |
| `textMittel` | `#4B5563` | Kartenüberschriften |
| `textSchwach` | `#9CA3AF` | Zeitstempel |
| `fehler` | `#DC2626` | Fehlerzustände |
| `erfolg` | `#16A34A` | Bestätigungen |
| `kiKarte` | `#F8FAFF` | KI-Antwortkarte |
| `kiKarteRand` | `rgba(37, 99, 235, 0.40)` | Rand der KI-Antwortkarte |

## 3. Typografie

**Inter**, vier Schnitte: 400 Regular, 500 Medium, 600 SemiBold, 700 Bold. Als Schriftpaket
im Build, nicht vom System geladen.

| Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite |
|-------|-------|---------|-----------|-----------|
| Bildschirmtitel | 22 sp | 600 | 28 sp | −0,2 sp |
| Karten-Überschrift | 15 sp | 600 | 20 sp | 0 |
| Notiztext | 16 sp | 400 | 25 sp | 0 |
| KI-Antworttext | 16 sp | 400 | 26 sp | 0 |
| Zeitstempel / Fußzeile | 12 sp | 500 | 16 sp | +0,3 sp |
| Knopfbeschriftung | 15 sp | 600 | 20 sp | +0,1 sp |
| Sitzungsname (Schublade) | 15 sp | 500 | 20 sp | 0 |
| Eingabefeld | 16 sp | 400 | 24 sp | 0 |
| Gerätecode (B-05) | 34 sp | 700 | 40 sp | +4 sp |
| Einstellungs-Beschriftung | 15 sp | 500 | 20 sp | 0 |
| Einstellungs-Erklärung | 13 sp | 400 | 18 sp | 0 |

## 4. Maße und Raster

Grundraster **4 dp**. Alle Abstände sind Vielfache davon.

| Maß | Wert |
|-----|------|
| Seitenrand Bildschirm | 16 dp |
| Abstand zwischen Notizkarten | 12 dp |
| Innenabstand Notizkarte | 16 dp |
| Höhe Kopfleiste | 56 dp |
| Höhe Fußleiste (Ruhe) | 72 dp |
| Höhe Fußleiste (Tastatur offen, Feld gewachsen) | bis 168 dp |
| Aufnahmeknopf | 60 dp Durchmesser |
| Aura um den Aufnahmeknopf | 80 dp Durchmesser |
| KI-Knopf | 48 dp Durchmesser |
| Kleine Knöpfe an der Karte (Lautsprecher, Verbessern) | 36 dp Tippfläche, 20 dp Symbol |
| Schublade Cover-Display | 280 dp breit |
| Schublade Innendisplay | 320 dp breit |
| Höhe Sitzungszeile | 56 dp |
| Blatt (B-03) Höhe | 60 % des Bildschirms, wächst bis 88 % |
| Mindest-Tippfläche überall | 44 × 44 dp |

## 5. Formen und Tiefe

| Bauteil | Radius | Rand | Schatten / Verlauf |
|---------|--------|------|-------------------|
| `.notizkarte` | 20 dp | 1 dp `rand` | Schatten: 0 dp Y 6 dp Blur 18 dp, `rgba(0,0,0,0.28)` dunkel / `rgba(0,0,0,0.08)` hell. Verlauf 145° von `hintergrundErhoben` nach `hintergrundErhoben` +4 % Helligkeit |
| `.kikarte` | 20 dp | 1,5 dp `kiKarteRand` | Schatten wie Notizkarte, zusätzlich Aura außen 24 dp `akzentGedeckt` |
| `header.kopfleiste (glass)` | 0 | unten 1 dp `rand` | `hintergrundGlas` + 24 px Rückwärts-Unschärfe |
| `footer.fussleiste (glass)` | oben 24 dp | oben 1 dp `rand` | `hintergrundGlas` + 24 px Rückwärts-Unschärfe, Schatten nach oben 0 −4 dp 16 dp `rgba(0,0,0,0.24)` |
| `.aufnahmeknopf` | vollrund | 2 dp `akzent` | Radialverlauf von `akzentGedeckt` (Mitte) nach durchsichtig (Rand); im Aufnahmezustand zusätzlich pulsierende Aura (M-05) |
| `.kiknopf` | vollrund | 1,5 dp `akzent` | flach in Ruhe; im Wartezustand wanderndes Leuchten |
| `.eingabefeld` | 22 dp | 1 dp `rand` | keiner |
| `aside.schublade` | rechts 24 dp | rechts 1 dp `rand` | Schatten nach rechts 8 dp 0 32 dp `rgba(0,0,0,0.40)` |
| `.blatt` (B-03) | oben 28 dp | oben 1 dp `rand` | `hintergrundGlas` + 32 px Unschärfe, Schatten 0 −8 dp 32 dp |
| `.profilzeile` | 14 dp | 1 dp `rand` | keiner; aktive Zeile bekommt 1,5 dp `akzent` |
| `.einstellungsgruppe` | 16 dp | 1 dp `rand` | keiner |

**Ausdrücklich gewünscht:** schwebende Karten, Glasleisten, leuchtender Aufnahmeknopf.
**Ausdrücklich nicht gewünscht:** Karten, die sich beim Scrollen perspektivisch neigen —
der Verlauf soll beim Lesen ruhig bleiben.

## 6. Bildschirme

| Kennung | Bildschirm | Zweck | Start? | Führt zu |
|---------|-----------|-------|--------|----------|
| B-01 | Verlauf | Notizen sehen, aufnehmen, tippen, auswerten | **ja** | B-02, B-03, B-04, B-07, B-08 |
| B-02 | Sitzungs-Schublade | Sitzungen wechseln und verwalten | nein | B-01 |
| B-03 | KI-Blatt | Rückfrage und Antwort vor der Auswertung | nein | B-01, B-05 |
| B-04 | Einstellungen | Schlüssel, Codex, Stimme, Erscheinung, Sicherung | nein | B-05, B-06, B-01 |
| B-05 | Codex-Anmeldung | Gerätecode eingeben | nein | B-04 |
| B-06 | Auswertungsprofile | Sechs Profile ansehen und bearbeiten | nein | B-04 |
| B-07 | Suche | Über alle Sitzungen suchen | nein | B-01 |
| B-08 | Notiz bearbeiten | Überschrift und Text ändern | nein | B-01 |

### B-01 — Verlauf *(Startbildschirm)*

**Aufbau von oben nach unten:**

1. **Kopfleiste** (Glas, 56 dp): links das Schubladensymbol (☰), mittig der Titel der offenen
   Sitzung (einzeilig, gekürzt), rechts Lupe (→ B-07) und Zahnrad (→ B-04).
2. **Verlaufsliste**, scrollbar, unten beginnend (neueste unten, wie ein Chat). Enthält
   zeitlich sortiert:
   - **Notizkarte** — oben eine Zeile mit Überschrift (`textMittel`, 600) links und
     Zeitstempel (`textSchwach`) rechts; darunter der Notiztext (`textStark`); unten rechts
     zwei kleine Knöpfe: **Lautsprecher** (F-06) und **Verbessern** bzw. nach einer
     Verbesserung **Rückgängig** (F-07). Langer Druck öffnet das Menü (F-08).
   - **KI-Antwortkarte** — breiter Rand in `akzent`, oben eine Zeile „Auswertung" mit
     Sprechblasensymbol; darunter die Rückfrage in `textMittel` kursiv und Franks Antwort in
     `textSchwach`; darunter der Antworttext in Absätzen mit 12 dp Abstand; unten eine
     Fußzeile mit Profil, Modell, Effort und Websuche-Zustand sowie dem Lautsprecher.
3. **Fußleiste** (Glas, 72 dp): links das **Textfeld** („Notiz tippen …", wächst bis 6
   Zeilen), rechts daneben der **KI-Knopf** (48 dp) und der **Aufnahmeknopf** (60 dp,
   leuchtend). Ist Text im Feld, verwandelt sich der Aufnahmeknopf in einen Senden-Knopf.

**Zustände:**

| Zustand | Darstellung |
|---------|-------------|
| **Leer** (neue Sitzung) | Mittig eine gedämpfte Zeichnung eines Mikrofons, darunter „Sprich einfach los." und kleiner „Alles, was dir zu diesem Thema einfällt — die KI fragst du später." |
| **Lädt** (Sitzungswechsel) | Drei Platzhalterkarten mit sanft wanderndem Schimmer |
| **Notiz transkribiert gerade** | Karte mit Zeitstempel, statt Text drei wandernde Punkte |
| **Notiz wartet auf Transkription** | Karte mit Zeitstempel und der Zeile „Wartet auf Netz" mit Wolkensymbol, in `textSchwach` |
| **Notiz fehlgeschlagen** | Karte mit `fehler`-Rand, Text „Transkription fehlgeschlagen" und Knopf „Nochmal versuchen" |
| **Nichts verstanden** | Karte mit Text „Nichts verstanden" in `textSchwach` und Knopf „Nochmal versuchen" |
| **Aufnahme läuft** | Aufnahmeknopf im Aufnahmezustand mit pulsierender Aura; Textfeld ausgegraut; über der Fußleiste eine Zeile mit der laufenden Dauer (mm:ss) |
| **Auswertung läuft** | KI-Knopf im Wartezustand; im Verlauf eine noch leere KI-Antwortkarte mit wanderndem Leuchten |
| **Vorlesen läuft** | Der betreffende Lautsprecher ist in `akzent` und zeigt ein Stopp-Symbol; der gerade gesprochene Absatz liegt auf `akzentGedeckt` |
| **Kein Mikrofonrecht** | Aufnahmeknopf ausgegraut; Antippen zeigt eine Meldung mit Knopf zu den Systemeinstellungen |

**Bedienelemente:**

| Element | Wirkung |
|---------|---------|
| ☰ Schubladensymbol | öffnet B-02 |
| Lupe | öffnet B-07 |
| Zahnrad | öffnet B-04 |
| Textfeld + Senden | F-02 |
| Aufnahmeknopf | F-01 |
| KI-Knopf | F-09 → öffnet B-03 |
| Lautsprecher an Karte | F-06 |
| Verbessern / Rückgängig an Karte | F-07 |
| Langer Druck auf Karte | Menü zu F-08 |
| Wischen von links am Rand | öffnet B-02 |

### B-02 — Sitzungs-Schublade

**Aufbau:** Kopf mit App-Namen „Gedankenspeicher" und darunter der Knopf **„+ Neue Sitzung"**
(volle Breite, Rand in `akzent`). Darunter die Sitzungsliste: je Zeile 56 dp mit Titel
(einzeilig gekürzt) und darunter in `textSchwach` die Zahl der Notizen und das Datum der
letzten. Die offene Sitzung trägt links einen 3 dp breiten Balken in `akzent` und liegt auf
`akzentGedeckt`. Ganz unten eine Trennlinie und der Eintrag „Einstellungen" mit Zahnrad.

**Zustände:** *Leer* gibt es nicht — es ist immer mindestens eine Sitzung da (F-12).
*Lädt:* fünf Platzhalterzeilen.

**Bedienelemente:** Tipp auf Sitzung → F-13 · Langer Druck → Menü mit *Umbenennen*, *Als
Markdown exportieren* (F-16), *Löschen* · „+ Neue Sitzung" → F-12 · „Einstellungen" → B-04 ·
Tipp auf die abgedunkelte Fläche rechts oder Wischen nach links → schließt.

### B-03 — KI-Blatt

Fährt von unten über B-01, das darunter abgedunkelt und leicht unscharf bleibt.

**Aufbau von oben nach unten:**

1. Ziehgriff (32 × 4 dp, `rand`, mittig).
2. Titel „Auswertung" und darunter in `textSchwach`: „N Notizen seit der letzten Auswertung".
3. Zwei Schalterzeilen: **„Ganze Sitzung einbeziehen"** (aus) und **„Websuche"**
   (vorbelegt aus der Grundeinstellung; steht sie auf *KI entscheidet*, zeigt die Zeile drei
   Wahlfelder statt eines Schalters).
4. Zeile „Profil: <Name des aktiven Profils>", antippbar → B-06.
5. **Die Rückfrage der KI** in `textStark`, 17 sp, 500 — der optisch wichtigste Text des
   Blattes.
6. **Antwortfeld** für Frank, mit Mikrofonknopf rechts darin (spricht die Antwort ein).
7. Knopf **„Auswerten"** (volle Breite, gefüllt in `akzent`, Text auf `hintergrund`).

**Zustände:** *Frage wird geholt* — an Stelle der Rückfrage drei wandernde Punkte, das
Antwortfeld ist gesperrt · *Frage da* — wie oben · *Codex nicht verbunden* — statt der Frage
der Hinweis „Codex ist nicht verbunden" und der Knopf „Jetzt verbinden" (→ B-05) · *Kein
Netz* — Hinweis mit Wiederholen-Knopf · *Antwort wird eingesprochen* — der Mikrofonknopf
pulsiert.

### B-04 — Einstellungen

Gruppen mit je 16 dp Radius, in dieser Reihenfolge:

1. **Erscheinung** — vier Kacheln nebeneinander (je 1:1), jede zeigt eine Miniatur ihrer
   Farbwelt; die gewählte trägt einen 2 dp Rand in `akzent` und ein Häkchen. (F-15)
2. **Codex** — Zeile „Verbindung" mit Zustand (*verbunden als …* / *nicht verbunden*) und
   Knopf *Verbinden* (→ B-05) bzw. *Trennen* · Zeile „Modell" mit drei Wahlfeldern (Sol,
   Terra, Luna) · Zeile „Effort" mit vier Wahlfeldern (minimal, niedrig, mittel, hoch) ·
   Zeile „Websuche" mit drei Wahlfeldern (aus, immer, KI entscheidet). (F-11)
3. **Auswertungsprofile** — eine Zeile mit dem Namen des aktiven Profils und einem Pfeil
   (→ B-06). (F-10)
4. **Transkription** — Feld für den Groq-Schlüssel (verdeckt, mit Augensymbol zum Anzeigen),
   darunter in `textSchwach` „Modell: whisper-large-v3-turbo" als reine Anzeige. (F-03)
5. **Stimme** — Zeile „Dienst" mit vier Wahlfeldern; darunter, je nach Wahl, die Stimmliste
   und die nötigen Schlüsselfelder; Knopf **„Probe hören"**. (F-18)
6. **Sicherung** — Schalter „Nach Google Drive sichern", darunter Zeitpunkt und Größe der
   letzten Sicherung, Knopf „Jetzt sichern" und Knopf „Aus Sicherung wiederherstellen"
   (in `fehler` gerandet). (F-17)
7. **Über** — Versionsnummer und Zeitpunkt des Bumps, als reine Anzeige.

**Zustände:** *Schlüssel fehlt* — das betroffene Feld trägt einen `fehler`-Rand und darunter
steht, wofür er gebraucht wird · *Probe läuft* — der Knopf zeigt ein Stopp-Symbol.

### B-05 — Codex-Anmeldung

Mittig auf leerer Fläche: die Überschrift „Codex verbinden", darunter der **Gerätecode** in
34 sp Bold als zwei Blöcke — **vier Zeichen, Trennstrich, fünf Zeichen** — mit großzügiger
Laufweite. Darunter die Adresse zum Öffnen, dann zwei Knöpfe: **„Im Browser öffnen"**
(gefüllt) und **„Code kopieren"** (nur gerandet). Ganz unten eine Zeile mit einem kleinen
Kreisel und dem Text „Warte auf Bestätigung …".

**Zustände:** *Code wird geholt* — an Stelle des Codes ein Platzhalter mit Schimmer ·
*Wartend* — wie oben · *Abgelaufen* — der Code ist durchgestrichen, darunter Knopf „Neuen
Code holen" · *Erfolg* — Häkchen in `erfolg`, „Verbunden", der Bildschirm schließt sich nach
1,2 s von selbst.

### B-06 — Auswertungsprofile

Sechs Zeilen untereinander, je 14 dp Radius, 12 dp Abstand. Eine Zeile enthält links ein
rundes Häkchenfeld (24 dp), daneben den Profilnamen (600) und darunter zweizeilig gekürzt
den Anweisungstext in `textSchwach`; rechts ein Stiftsymbol.

Die aktive Zeile trägt einen 1,5 dp Rand in `akzent` und liegt auf `akzentGedeckt`.

Ein Tipp auf den Stift (oder auf die Zeile) öffnet den **Editor** als Blatt: Feld
„Name" (einzeilig) und Feld „Anweisung an die KI" (mehrzeilig, wächst), darunter die Knöpfe
„Zurücksetzen" (nur gerandet) und „Speichern" (gefüllt).

**Zustände:** *Leeres Profil* — Name in `textSchwach` („Eigenes Profil 1"), statt der
Textvorschau steht „Noch kein Text — antippen zum Ausfüllen", das Häkchenfeld ist ausgegraut.

### B-07 — Suche

Kopfleiste mit Zurückpfeil und einem sofort fokussierten Suchfeld. Darunter die Treffer,
nach Sitzung gruppiert: je Gruppe eine Kopfzeile mit Sitzungstitel und Trefferzahl, darunter
die Treffer als flache Zeilen mit Überschrift, Zeitstempel und der Textstelle, in der das
Suchwort in `akzent` unterlegt ist.

**Zustände:** *Noch nichts eingegeben* — „Suche in allen Notizen" mittig in `textSchwach` ·
*Kein Treffer* — „Nichts gefunden zu ‚<Suchwort>'" · *Sucht* — schmaler Fortschrittsbalken
unter dem Suchfeld.

### B-08 — Notiz bearbeiten

Kopfleiste mit Abbrechen (links) und Speichern (rechts, in `akzent`). Darunter das Feld
**Überschrift** (einzeilig) und das Feld **Text** (füllt den Rest, scrollt). Unter dem
Textfeld in `textSchwach` der unveränderliche Zeitstempel der Notiz.

**Zustände:** *Unverändert* — Speichern ist ausgegraut · *Geändert* — Speichern ist aktiv;
Zurück fragt „Änderungen verwerfen?".

## 7. Ikonografie und Bilder

**Material Symbols Rounded**, Strichstärke 2 dp, Größe 24 dp (an Karten 20 dp). Keine
gefüllten Varianten außer beim aktiven Zustand des Lautsprechers und beim Häkchen.

| Zweck | Symbol |
|-------|--------|
| Schublade | `menu` |
| Suche | `search` |
| Einstellungen | `settings` |
| Aufnehmen | `mic` |
| Aufnahme beenden | `stop` |
| Senden | `arrow_upward` |
| KI-Auswertung | `auto_awesome` |
| Vorlesen | `volume_up` (aktiv: `stop_circle`) |
| Verbessern | `auto_fix_high` |
| Rückgängig | `undo` |
| Neue Sitzung | `add` |
| Exportieren | `ios_share` |
| Löschen | `delete` |
| Verschieben | `drive_file_move` |
| Kopieren | `content_copy` |
| Wartet auf Netz | `cloud_off` |
| Bearbeiten | `edit` |

Keine Fotos, keine Illustrationen außer der gedämpften Mikrofon-Zeichnung im Leerzustand
von B-01.

## 8. Texte

Alle festen Beschriftungen wörtlich:

| Ort | Text |
|-----|------|
| B-01 Textfeld | „Notiz tippen …" |
| B-01 Leerzustand Titel | „Sprich einfach los." |
| B-01 Leerzustand Erklärung | „Alles, was dir zu diesem Thema einfällt — die KI fragst du später." |
| B-01 Notiz wartet | „Wartet auf Netz" |
| B-01 Notiz Fehler | „Transkription fehlgeschlagen" / Knopf „Nochmal versuchen" |
| B-01 Nichts verstanden | „Nichts verstanden" |
| B-01 kein Mikrofonrecht | „Ohne Mikrofon kann ich dich nicht hören." / Knopf „Einstellungen öffnen" |
| B-02 Kopf | „Gedankenspeicher" |
| B-02 Knopf | „+ Neue Sitzung" |
| B-02 Standardtitel | „Neue Sitzung" |
| B-02 Löschen-Rückfrage | „Sitzung mit {n} Notizen löschen? Das lässt sich nicht rückgängig machen." |
| B-03 Titel | „Auswertung" |
| B-03 Zähler | „{n} Notizen seit der letzten Auswertung" |
| B-03 Keine neuen | „Seit der letzten Auswertung sind keine neuen Notizen dazugekommen." |
| B-03 Schalter 1 | „Ganze Sitzung einbeziehen" |
| B-03 Schalter 2 | „Websuche" |
| B-03 Profilzeile | „Profil: {name}" |
| B-03 Antwortfeld | „Deine Antwort …" |
| B-03 Knopf | „Auswerten" |
| B-03 Codex fehlt | „Codex ist nicht verbunden." / Knopf „Jetzt verbinden" |
| B-04 Gruppen | „Erscheinung" · „Codex" · „Auswertungsprofile" · „Transkription" · „Stimme" · „Sicherung" · „Über" |
| B-04 Erscheinungen | „Hell" · „Dunkel" · „Gold-Hell" · „Gold-Dunkel" |
| B-04 Effort | „minimal" · „niedrig" · „mittel" · „hoch" |
| B-04 Websuche | „aus" · „immer" · „KI entscheidet" |
| B-04 Probe | „Probe hören" |
| B-05 Titel | „Codex verbinden" |
| B-05 Warten | „Warte auf Bestätigung …" |
| B-05 Knöpfe | „Im Browser öffnen" · „Code kopieren" · „Neuen Code holen" |
| B-06 Leeres Profil | „Noch kein Text — antippen zum Ausfüllen" |
| B-06 Editor | „Name" · „Anweisung an die KI" · „Zurücksetzen" · „Speichern" |
| B-07 Platzhalter | „Suche in allen Notizen" |
| B-07 Kein Treffer | „Nichts gefunden zu ‚{wort}'" |
| B-08 Knöpfe | „Abbrechen" · „Speichern" |
| B-08 Verwerfen | „Änderungen verwerfen?" |

Platzhalter stehen in geschweiften Klammern: `{n}`, `{name}`, `{wort}`.

## 9. Barrierefreiheit

- Mindest-Tippfläche **44 × 44 dp** überall, auch bei den 20 dp großen Kartensymbolen.
- Kontrast Text auf Hintergrund mindestens **4,5 : 1** in allen vier Erscheinungen; die
  angegebenen Werte erfüllen das (Gold-Dunkel: `#F4EFE2` auf `#17140D` ≈ 15,8 : 1;
  Gold-Hell: `#231E14` auf `#FFFFFF` ≈ 15,1 : 1).
- Der Akzent trägt **nie allein** eine Bedeutung: das aktive Profil hat Häkchen *und* Rand,
  die offene Sitzung Balken *und* Fläche.
- Bei großer Systemschrift wachsen alle Texte mit; Karten und Leisten wachsen in der Höhe
  mit, nichts wird abgeschnitten. Ab 130 % Schriftgröße bricht die Fußzeile der KI-Karte in
  zwei Zeilen um.
- Jedes Symbol ohne Beschriftung trägt eine Inhaltsbeschreibung für den Screenreader.

## 10. Offene Fragen

Siehe `00-PROJEKT.md` §6. Im Gestaltungsbereich ist nichts offen — alles Weitere entscheidet
der Designer, und seine Messung gilt.


---

# TEIL C — BEWEGUNG

# Motion-Spec — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/Gedankenspeicher/v2/messung/`. Widerspricht ein Satz
> von hier der Messung, ist der Satz überholt — nicht die Messung falsch.

## 1. Bewegungs-Grundhaltung

Zügig und weich. Alles startet sofort und bremst sanft aus — nichts zuckt, nichts lässt
warten. Grunddauer **240 ms**, Grundkurve **`cubic-bezier(0.2, 0, 0, 1)`**.

Frank spricht Notizen oft unterwegs und schnell hintereinander ein. Eine Bewegung, die ihn
aufhält, wäre schlimmer als gar keine; eine, die ruckt, ließe die App billig wirken. Deshalb
dieser Kompromiss: schnell genug, um nie im Weg zu sein, weich genug, damit die schwebenden
Karten und Glasleisten ihre Tiefe behalten.

**Was sich niemals bewegt:**

1. **Der Notiztext und der KI-Antworttext beim Lesen.** Kein Einfliegen von Buchstaben, kein
   Wandern, kein Umbrechen. Steht der Text, steht er still.
2. **Die Reihenfolge der Karten.** Eine Karte, die sich füllt (Transkription, Nachreichen),
   bleibt an ihrer Stelle und springt nicht ans Ende.
3. **Die Kopfleiste.** Sie steht fest, sie fährt beim Scrollen weder weg noch ein.

## 2. Kurven und Dauern

| Name | Dauer | Kurve | Wofür |
|------|-------|-------|-------|
| `standard` | 240 ms | `cubic-bezier(0.2, 0, 0, 1)` | Der Regelfall: Erscheinen, Verschwinden, Wechsel |
| `kurz` | 120 ms | `cubic-bezier(0.2, 0, 0, 1)` | Rückmeldung auf Bedienung, Symbolwechsel |
| `blatt` | 320 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Blätter und Schublade — etwas länger, weil mehr Fläche bewegt wird |
| `weich` | 400 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Erscheinungswechsel, Farbüberblendungen |
| `puls` | 1600 ms | `cubic-bezier(0.4, 0, 0.6, 1)`, endlos | Aufnahmering |
| `wandern` | 2400 ms | `linear`, endlos | Leuchten auf wartenden Karten und Platzhaltern |

Diese sechs Namen werden überall referenziert. Es wird keine siebte Dauer erfunden.

## 3. Bewegungen im Einzelnen

### M-01 — Notizkarte erscheint
- **Wo** — B-01, Verlaufsliste
- **Auslöser** — Eine neue Notiz entsteht (F-01, F-02)
- **Was sich ändert** — `translateY` 16 dp → 0 dp · `opacity` 0 → 1 · `scale` 0,97 → 1,0
- **Dauer / Kurve** — `standard` · keine Verzögerung · einmalig
- Die Liste scrollt gleichzeitig um die Kartenhöhe nach, mit derselben Dauer und Kurve, damit
  die neue Karte sichtbar wird, ohne dass zwei Bewegungen gegeneinander laufen.

### M-02 — Schublade öffnet und schließt
- **Wo** — B-02
- **Auslöser** — Schubladensymbol, Wischen von links, Tipp auf die abgedunkelte Fläche
- **Was sich ändert** — `translateX` −280 dp (Cover) bzw. −320 dp (Innen) → 0 dp; die
  abgedunkelte Fläche über B-01: `opacity` 0 → 0,52
- **Dauer / Kurve** — `blatt` · Schließen mit derselben Dauer rückwärts
- Beim Ziehen mit dem Finger folgt die Schublade unmittelbar; beim Loslassen läuft sie mit
  `blatt` in die nähere Endlage.

### M-03 — KI-Blatt fährt herein
- **Wo** — B-03
- **Auslöser** — KI-Knopf (F-09)
- **Was sich ändert** — `translateY` 100 % → 0 % · abgedunkelte Fläche `opacity` 0 → 0,52 ·
  B-01 dahinter `scale` 1,0 → 0,98 und Unschärfe 0 → 6 px
- **Dauer / Kurve** — `blatt`
- Beim Schließen rückwärts, in derselben Dauer.

### M-04 — Karte verschwindet
- **Wo** — B-01
- **Auslöser** — Notiz löschen oder verschieben (F-08)
- **Was sich ändert** — `opacity` 1 → 0 · `scale` 1,0 → 0,94 · danach schließt sich die
  Lücke: Höhe → 0 dp
- **Dauer / Kurve** — `standard` für das Verblassen, danach `standard` für das Schließen der
  Lücke — nacheinander, nicht gleichzeitig, sonst wirkt es hektisch

### M-05 — Aufnahmering pulsiert *(Dauerbewegung)*
- **Wo** — B-01, Aufnahmeknopf
- **Auslöser** — Aufnahme läuft (F-01)
- **Was sich ändert** — Die Aura um den Knopf: `scale` 1,0 → 1,22, `opacity` 0,50 → 0,12.
  **Die Amplitude folgt der gemessenen Lautstärke:** bei Stille bleibt sie bei `scale` 1,05,
  bei lautem Sprechen erreicht sie 1,22. So sieht Frank, dass das Mikrofon ihn hört.
- **Dauer / Kurve** — `puls`, endlos, bis die Aufnahme endet
- Beim Beenden läuft die Aura in einer `standard`-Bewegung auf `scale` 1,0 / `opacity` 0 aus.

### M-06 — Aufnahmeknopf wechselt sein Symbol
- **Wo** — B-01
- **Auslöser** — Aufnahme startet oder endet; Text im Feld erscheint oder verschwindet
- **Was sich ändert** — Das alte Symbol `rotate` 0° → −90° und `opacity` 1 → 0, das neue
  gleichzeitig `rotate` 90° → 0° und `opacity` 0 → 1
- **Dauer / Kurve** — `kurz`

### M-07 — Wanderndes Leuchten *(Dauerbewegung)*
- **Wo** — B-01, entstehende KI-Antwortkarte; Notizkarte während der Textverbesserung
- **Auslöser** — Codex arbeitet (F-07, F-09)
- **Was sich ändert** — Ein linearer Verlauf aus `akzentGedeckt` (Breite 40 % der Karte)
  wandert von `-40 %` nach `140 %` über die Kartenfläche
- **Dauer / Kurve** — `wandern`, endlos, bis die Antwort da ist
- Trifft der Text ein, hört das Leuchten am Ende seines laufenden Durchgangs auf — es wird
  nicht mitten in der Fläche abgeschnitten.

### M-08 — Überschrift erscheint
- **Wo** — B-01, Kopfzeile einer Notizkarte
- **Auslöser** — Die KI-Überschrift trifft ein (F-05)
- **Was sich ändert** — Die Uhrzeit als Platzhalter `opacity` 1 → 0, die Überschrift
  gleichzeitig `opacity` 0 → 1 und `translateY` 4 dp → 0 dp
- **Dauer / Kurve** — `standard`
- Der Zeitstempel rechts bleibt dabei unangetastet — er bewegt sich nicht.

### M-09 — Vorlese-Absatz wandert mit *(Dauerbewegung)*
- **Wo** — B-01, Notiz- und KI-Antwortkarten
- **Auslöser** — Vorlesen läuft (F-06)
- **Was sich ändert** — Der gerade gesprochene Absatz bekommt `akzentGedeckt` als
  Hintergrund mit 8 dp Radius. Beim Wechsel zum nächsten Absatz verblasst die Hervorhebung
  am alten und erscheint am neuen.
- **Dauer / Kurve** — `standard` je Wechsel
- Läuft der Absatz aus dem Bild, scrollt die Liste ihn sanft in die Mitte — mit `weich`, und
  nur wenn Frank nicht selbst gerade scrollt.

### M-10 — Erscheinung wechselt
- **Wo** — überall
- **Auslöser** — Kachel in B-04 (F-15)
- **Was sich ändert** — Alle Farbwerte überblenden von alt nach neu
- **Dauer / Kurve** — `weich`
- Kein Wischen, kein Aufziehen, kein Kreis, der sich ausbreitet — nur eine Überblendung.

### M-11 — Suchtreffer leuchtet auf
- **Wo** — B-01, nach dem Sprung aus B-07
- **Auslöser** — Frank tippt einen Suchtreffer an (F-14)
- **Was sich ändert** — Die Zielkarte: Hintergrund `hintergrundErhoben` → `akzentGedeckt` →
  `hintergrundErhoben`
- **Dauer / Kurve** — 240 ms hin (`standard`), 200 ms Halten, 400 ms zurück (`weich`)

### M-12 — Häkchen springt um
- **Wo** — B-06, Profilzeilen
- **Auslöser** — Frank tippt ein Häkchenfeld an (F-10)
- **Was sich ändert** — Das alte Häkchen `scale` 1,0 → 0 und `opacity` 1 → 0; das neue
  `scale` 0 → 1,0 mit einem leichten Überschwingen auf 1,12 und zurück; gleichzeitig
  wandert der Rand in `akzent` von der alten zur neuen Zeile (`opacity`-Überblendung)
- **Dauer / Kurve** — `kurz` für das Verschwinden, 240 ms `cubic-bezier(0.34, 1.56, 0.64, 1)`
  für das Erscheinen

## 4. Bildschirmwechsel

| Von | Nach | Art | Dauer | Kurve |
|-----|------|-----|-------|-------|
| B-01 | B-02 | Schublade schiebt von links herein (M-02) | 320 ms | `blatt` |
| B-02 | B-01 | Schublade schiebt nach links hinaus | 320 ms | `blatt` |
| B-01 | B-03 | Blatt fährt von unten herein (M-03) | 320 ms | `blatt` |
| B-03 | B-01 | Blatt fährt nach unten hinaus | 320 ms | `blatt` |
| B-01 / B-02 | B-04 | Schiebt von rechts herein, B-01 gleichzeitig 12 % nach links und `opacity` 1 → 0,6 | 240 ms | `standard` |
| B-04 | B-01 | Rückwärts, gleiche Werte | 240 ms | `standard` |
| B-04 | B-05, B-06 | Schiebt von rechts herein | 240 ms | `standard` |
| B-05, B-06 | B-04 | Rückwärts | 240 ms | `standard` |
| B-01 | B-07 | Überblenden mit `scale` 1,04 → 1,0; das Suchfeld erhält sofort den Fokus | 240 ms | `standard` |
| B-07 | B-01 | Überblenden mit `scale` 1,0 → 1,04 rückwärts | 240 ms | `standard` |
| B-01 | B-08 | Blatt fährt von unten herein | 320 ms | `blatt` |
| B-08 | B-01 | Blatt fährt nach unten hinaus | 320 ms | `blatt` |

Der Rücklauf ist überall der exakte Rückwärtsgang des Hinlaufs, in derselben Dauer.

## 5. Rückmeldung auf Bedienung

| Element | Was passiert | Dauer |
|---------|-------------|-------|
| Aufnahmeknopf | `scale` 1,0 → 0,92 beim Drücken, zurück beim Loslassen; zusätzlich eine kurze Vibration (`EFFECT_TICK`) beim Start **und** beim Ende der Aufnahme | `kurz` |
| KI-Knopf | `scale` 1,0 → 0,94, Rand hellt auf `akzent` +12 % auf | `kurz` |
| Gefüllte Knöpfe („Auswerten", „Speichern") | `scale` 1,0 → 0,97, Fläche dunkelt um 8 % ab | `kurz` |
| Kartensymbole (Lautsprecher, Verbessern) | Kreisförmige Welle in `akzentGedeckt` vom Berührungspunkt aus, 36 dp Durchmesser | 240 ms |
| Notizkarte (langer Druck) | `scale` 1,0 → 0,98 nach 180 ms Haltedauer, gleichzeitig eine Vibration (`EFFECT_HEAVY_CLICK`), danach öffnet das Menü | 180 ms Halten, dann `kurz` |
| Sitzungszeile | Fläche geht auf `akzentGedeckt`, kein Skalieren | `kurz` |
| Wahlfelder und Schalter in B-04 | Der Punkt gleitet, die Fläche überblendet | `standard` |
| Wischen an einer Sitzungszeile | Die Zeile folgt dem Finger; ab 96 dp erscheint dahinter das Löschsymbol in `fehler` | folgt dem Finger |

## 6. Dauerbewegung

Genau drei Dinge bewegen sich ohne Zutun — **sonst nichts**. Der Verlauf bleibt beim Lesen
still.

| Bewegung | Wann | Periode |
|----------|------|---------|
| M-05 Aufnahmering | nur während der Aufnahme | 1600 ms |
| M-07 Wanderndes Leuchten | nur während Codex arbeitet | 2400 ms |
| M-09 Vorlese-Hervorhebung | nur während vorgelesen wird | folgt den Absätzen |

Kein Atmen im Ruhezustand, kein wanderndes Hintergrundlicht, kein pulsierender Rand an
Karten, die nur dastehen.

## 7. Lade- und Wartezustände

| Lage | Ab wann | Was | Wie es verschwindet |
|------|---------|-----|--------------------|
| Sitzung wird geladen | sofort | Drei Platzhalterkarten mit `wandern`-Schimmer | Überblenden auf die echten Karten, `standard` |
| Notiz wird transkribiert | sofort nach dem Aufnahmeende | Drei Punkte in der Karte, die nacheinander auf `opacity` 1 gehen (je 160 ms versetzt, Periode 1200 ms) | Der Text erscheint mit `standard`-Überblendung an ihrer Stelle |
| Rückfrage wird geholt (B-03) | sofort | Dieselben drei Punkte an Stelle der Frage | Die Frage überblendet ein, `standard` |
| Auswertung läuft | sofort | Leere KI-Antwortkarte mit M-07 | Der Text erscheint absatzweise, jeder Absatz mit M-01 |
| Gerätecode wird geholt (B-05) | sofort | Platzhalterblock mit `wandern`-Schimmer | Der Code überblendet ein |
| Sprachprobe wird geholt (B-04) | nach 400 ms | Kleiner Kreisel im Knopf | Verschwindet, wenn der Ton beginnt |
| Suche läuft | nach 200 ms | Schmaler unbestimmter Balken (2 dp) unter dem Suchfeld | Verblasst mit `kurz` |

Ein Ladezustand, der kürzer als 200 ms dauern würde, wird gar nicht erst gezeigt — sonst
blitzt er nur auf und macht die App unruhig. Ausgenommen sind die Fälle, die oben mit
„sofort" stehen: dort ist von vornherein mit längerer Wartezeit zu rechnen.

## 8. Reduzierte Bewegung

Meldet das System „Bewegung reduzieren" (`ANIMATOR_DURATION_SCALE == 0` oder die
Barrierefreiheits-Einstellung):

1. **Alle Dauerbewegungen sind aus.** M-05 zeigt statt des Pulsierens einen statischen Ring
   in voller Deckkraft, solange aufgenommen wird. M-07 zeigt eine ruhige Fläche in
   `akzentGedeckt` statt des wandernden Verlaufs. M-09 hebt den Absatz weiterhin hervor —
   das ist eine Information, keine Zierde —, aber ohne Überblendung: die Hervorhebung
   springt.
2. **Alle Übergänge werden zu reinem Überblenden.** Kein Schieben, kein Skalieren, kein
   Rotieren. Die Schublade und die Blätter blenden ein und aus, statt zu fahren.
3. **Alle Dauern werden halbiert:** `standard` 120 ms, `kurz` 60 ms, `blatt` 160 ms,
   `weich` 200 ms.
4. **Die Vibrationen bleiben.** Sie sind Rückmeldung, keine Bewegung, und beim Aufnehmen die
   einzige Bestätigung, die auch ohne Hinsehen ankommt.
5. Die Schimmer der Platzhalter werden zu einer ruhigen Fläche in `akzentGedeckt`.

## 9. Offene Fragen

Siehe `00-PROJEKT.md` §6. Im Bewegungsbereich ist nichts offen.


---

# TEIL D — RAHMEN UND ABNAHME

# Projekt — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

## 0. Das Wichtigste

Der **KI-Knopf** ist der Kern der App. Alles andere — Aufnehmen, Tippen, Überschriften,
Vorlesen, Seitenleiste — dient nur dazu, dass beim Druck auf diesen Knopf ein guter,
zusammenhängender Notiz-Kontext bereitliegt. Geht beim Bauen etwas verloren, darf es
niemals dieser Knopf sein.

## 1. Zweck in drei Sätzen

Gedankenspeicher ist ein Ort, an dem Frank über Tage oder Wochen Gedanken zu einem Thema
sammelt — eingesprochen oder getippt, so wie sie ihm einfallen, ohne sie sofort ordnen zu
müssen. Jede Notiz landet mit Zeitstempel und einer von der KI vergebenen Überschrift als
Karte in einem chatartigen Verlauf; jede Notiz lässt sich vorlesen, im Text verbessern und
nachträglich ändern. Erst wenn genug beisammen ist, schaltet er die KI zu: sie nimmt die
gesammelten Notizen als Kontext, fragt zuerst zurück, worauf sie sich konzentrieren soll,
und liefert danach eine Auswertung in der Länge und Machart des aktiven Auswertungsprofils.

## 2. Zielplattform(en)

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|-----------|----------------------|-------------|--------------------|
| Android | Galaxy Z Fold 8 (SM-F971B) — Cover 1248 × 1972 px @ 420 dpi (297 × 469 dp), Innen 1848 × 2448 px @ 420 dpi (440 × 583 dp) | Kotlin + Jetpack Compose | Pflicht |

**Beide Displays sind zu schmal für eine dauerhaft danebenstehende Seitenleiste.** Die
Sitzungsliste ist deshalb auf beiden Displays eine Schublade, die von links hereinfährt —
auf dem Innendisplay breiter (320 dp) als auf dem Cover (280 dp).

Paketname: `de.frank.gedankenspeicher` · Quellcode-Ordner: `~/proggs/Gedankenspeicher/`

## 3. Rahmenbedingungen

| Punkt | Festlegung |
|-------|-----------|
| **Sprache der Oberfläche** | Deutsch, einsprachig. Umlaute echt (ä ö ü ß). |
| **Offline / Online** | Teilweise offline. Lesen, Tippen, Bearbeiten, Suchen, Löschen und Vorlesen mit der Gerätestimme gehen ohne Netz. Aufnehmen geht ebenfalls ohne Netz — die Aufnahme wird gepuffert und automatisch nachtranskribiert, sobald Netz da ist (F-04). Transkription, Überschriften, Textverbesserung, KI-Auswertung und die drei Netz-Stimmen brauchen Netz. |
| **Konten / Anmeldung** | Keine App-eigene Anmeldung. Zwei fremde Anmeldungen in den Einstellungen: Codex per Gerätecode (F-11) und Google Drive für die Sicherung (F-17). |
| **Berechtigungen** | `RECORD_AUDIO` — wird beim ersten Druck auf den Aufnahmeknopf abgefragt, nicht beim Start. Bei Ablehnung bleibt die App voll benutzbar, nur der Aufnahmeknopf ist ausgegraut und erklärt beim Antippen, was fehlt. `INTERNET` und `ACCESS_NETWORK_STATE` sind Installationsrechte ohne Abfrage. |
| **Externe Dienste** | Groq (Transkription, `whisper-large-v3-turbo`) · ChatGPT Codex (Überschriften, Textverbesserung, Auswertung) · Microsoft Edge TTS · Google Cloud TTS (Chirp 3 HD) · Qwen (Stimmklon) · Google Drive (Sicherung). Schlüssel und Anmeldungen siehe `01-FUNKTIONS-SPEC.md` §5. |
| **Datenhaltung** | Alles auf dem Gerät in einer Room-Datenbank. Zusätzlich: Export je Sitzung als Markdown-Datei (F-16) und automatische Sicherung nach Google Drive (F-17). |
| **Verteilung** | Privat. Die App wird per `adb install -r` auf Franks Gerät installiert, nicht veröffentlicht. Deshalb: kein Onboarding, keine Datenschutzerklärung, keine Store-Pflichten — `04-ONBOARDING-SPEC.md` und `05-RECHT-SPEC.md` entfallen. |

## 4. Ausdrücklich NICHT enthalten

1. **Kein freies Chatten mit der KI.** Die KI meldet sich ausschließlich über den KI-Knopf
   (F-09) und ausschließlich zu den Notizen, die als Kontext übergeben wurden. Es gibt
   keinen offenen Chat-Eingabeschlitz und keinen Weg, die KI ohne Notiz-Kontext etwas zu
   fragen. Der einzige Text, den Frank direkt an die KI schickt, ist seine Antwort auf
   deren Rückfrage.
2. **Kein automatisches Auswerten.** Die KI läuft niemals von selbst los, weder nach einer
   bestimmten Anzahl Notizen noch nach Zeit. Sammeln und Auswerten sind zwei getrennte
   Vorgänge, und den zweiten löst immer Frank aus.
3. **Keine zweite Sortierebene über den Sitzungen.** Es gibt Sitzungen und darin Notizen —
   keine Ordner über den Sitzungen.

*(Was darüber hinaus nicht hineingehört — Bilder und Anhänge, Erinnerungen und
Benachrichtigungen, Tags — steht bewusst unter §6 Offene Fragen und ist nicht entschieden.)*

## 5. Abnahme — wann ist es fertig

| Kennung | Kriterium |
|---------|-----------|
| A-01 | Ich lege eine neue Sitzung an, spreche eine Notiz ein und sehe binnen weniger Sekunden eine Karte mit Zeitstempel, KI-Überschrift und dem transkribierten Text. |
| A-02 | Ich spreche fünf Notizen hintereinander ein. Alle fünf stehen untereinander im Verlauf, in der Reihenfolge, in der ich sie gesprochen habe. |
| A-03 | Ich schalte den Flugzeugmodus ein, spreche eine Notiz und sehe eine Karte mit „wartet auf Transkription". Schalte ich das Netz wieder ein, füllt sich die Karte binnen einer Minute von selbst mit dem Text. |
| A-04 | Ich tippe den Lautsprecher an einer langen Notiz an. Sie wird vorgelesen, Absatz für Absatz, ohne Aussetzer zwischen den Absätzen. Tippe ich denselben Knopf erneut an, hört sie auf. |
| A-05 | Ich tippe den KI-Knopf an. Die KI stellt eine Rückfrage, die erkennbar auf meine Notizen Bezug nimmt — keine allgemeine Standardfrage. |
| A-06 | Nach meiner Antwort erscheint eine KI-Antwortkarte im Verlauf, in Absätzen, deren Länge zum aktiven Auswertungsprofil passt. |
| A-07 | Ich spreche danach drei weitere Notizen ein und drücke erneut den KI-Knopf. Die zweite Auswertung bezieht sich nur auf diese drei Notizen, nicht auf die davor. |
| A-08 | Im KI-Dialog schalte ich „ganze Sitzung" ein. Nun bezieht sich die Auswertung nachweislich auch auf die früheren Notizen. |
| A-09 | Ich wechsle in den Einstellungen zwischen den vier Erscheinungen. Jede ist vollständig durchgefärbt — kein Bildschirm, kein Dialog, keine Schublade bleibt in der alten Erscheinung stehen. |
| A-10 | Ich ändere ein Auswertungsprofil, setze das Häkchen darauf und werte aus. Die Antwort folgt sichtbar dem geänderten Text. Es ist zu keinem Zeitpunkt möglich, zwei Häkchen gleichzeitig zu setzen. |
| A-11 | Ich drücke an einer Notiz den Verbessern-Knopf. Der Text wird sauberer. Der Rückgängig-Knopf stellt exakt den ursprünglichen Wortlaut wieder her. |
| A-12 | Ich suche nach einem Wort, das ich vor Wochen in einer anderen Sitzung gesagt habe, und finde die Notiz über das Suchfeld. |
| A-13 | Ich schließe die App während einer laufenden Aufnahme und öffne sie wieder. Es steht keine halbe, kaputte Notiz im Verlauf. |
| A-14 | Die App läuft auf dem Fold 8 zugeklappt und aufgeklappt, ohne dass Text abgeschnitten wird oder Knöpfe aus dem Bild rutschen. |
| A-15 | Kein Knopf in der ganzen App ist ohne Wirkung. |

## 6. Offene Fragen

| Nr. | Frage | Warum sie offen ist |
|-----|-------|--------------------|
| O-01 | Sollen Notizen Bilder, Dateien oder Anhänge aufnehmen können? | Frank hat die Frage bewusst nicht beantwortet. Bis zu einer Entscheidung wird **nichts davon gebaut**: Notizen sind reiner Text. Der Designer soll dafür auch keine Fläche vorsehen. |
| O-02 | Soll es Tags oder Kategorien innerhalb einer Sitzung geben? | Ebenso offengelassen. Bis zu einer Entscheidung nicht gebaut — die Sitzung ist die einzige Ordnungsebene. |
| O-03 | Soll die App Erinnerungen oder Benachrichtigungen schicken? | Ebenso offengelassen. Bis zu einer Entscheidung wird **keine** Benachrichtigung gesendet und kein Benachrichtigungsrecht angefordert. |
| O-04 | Wie viele Notizen darf eine Auswertung höchstens umfassen, bevor der Kontext zu groß wird? | Hängt vom Token-Fenster des gewählten Codex-Modells ab und lässt sich erst am laufenden System messen. Bis dahin gilt: es wird nicht gekürzt, und läuft der Aufruf in ein Kontext-Limit, meldet die App das offen (F-09, Fehlerfall) statt stillschweigend Notizen wegzulassen. |

