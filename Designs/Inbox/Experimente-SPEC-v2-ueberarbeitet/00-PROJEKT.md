# Projekt — Experimente

> **Zielplattform für diesen Bau: Android (Kotlin / Jetpack Compose).**
> Beim Herunterladen aus Werft Studio am 2026-08-10 gewählt. Sie gilt vor jeder abweichenden
> Angabe weiter unten in dieser Datei.

Stand: 14.08.2026, 11.36 Uhr · Stufe: **v2, ueberarbeitet (Stand der gebauten App)** · Plattform(en): Android

> **Das Wichtigste zuerst.** Wenn später etwas gegeneinander steht, gilt diese Reihenfolge:
> **1. Die KI muss Frank wirklich kennen.** Selbstbild, 15-Tage-Log, Langzeit-Log und
> Erkenntnisse müssen spürbar in jeden Vorschlag hineinwirken. Im Zweifel gewinnt der Kontext.
> **2. Nichts darf verlorengehen.** Was eingesprochen wurde, ist in einem Jahr noch da —
> verdichtet, aber vollständig.
> **3. Die Vorschläge müssen wirklich neu sein.** Dinge, auf die Frank selbst nie gekommen wäre.
> Die reine Sprachbedienung steht dazu nicht in Konkurrenz — sie wird einmal gebaut und läuft.

---

## 0. Was sich in dieser Fassung geändert hat

> **v2 beschreibt die App, wie sie läuft.** Die vorige Fassung (v1, überarbeitet, 12.08.2026)
> war ein Auftrag: der Monitor `B-10` war beschrieben, aber noch nicht gebaut. Seitdem wurde die
> App in **siebzehn Schritten** weitergebaut — unter anderem vollständig aus dem
> Fold-Außendisplay-Entwurf neu aufgesetzt. Diese Fassung trägt nach, was dabei entstanden ist.
>
> **In Zahlen:** 17 neue Funktionen (`F-42` bis `F-58`) · 7 nachgetragene, die in v1 nur in der
> Tabelle standen (`F-27` bis `F-33`) · 23 geänderte Bestandsfunktionen · 3 neue Felder im
> Datenmodell und eine Datenbank auf Version 4 · 12 zusätzliche Schriftrollen und eine
> 14. Farbrolle · 5 neue Effekte (`E-25` bis `E-29`) · 5 neue Bewegungen (`M-96` bis `M-100`) ·
> 15 neue Abnahmekriterien (`A-31` bis `A-45`). Die vollständige Gegenüberstellung steht in
> **`AENDERUNGEN-v1-zu-v2.md`**.

**Die sieben Punkte, auf die es ankommt:**

1. **`B-10` ist gebaut.** Der Monitor ist der Startbildschirm und in beiden Erscheinungen
   umgesetzt; die Werte im UI-Spec stammen jetzt aus dem gebauten Bildschirm.
2. **Die App beendet nichts mehr von selbst.** Die Auswertung am letzten Tag schloss ein
   Experiment vorher stillschweigend ab. Jetzt wird gefragt: **weiterführen** (`F-44`),
   abschließen, Zwischenstand oder „nicht umgesetzt".
3. **Nichts Eingesprochenes wird überschrieben.** Jede Auswertung ist eine eigene Zeile mit
   Datum, Uhrzeit und Versuchstag; alle sind über den Verlauf auf `B-03` und den neuen
   Logbuch-Reiter *Auswertungen* erreichbar (`F-45`, `F-46`).
4. **Die Dauer gehört Frank.** Sie lässt sich beim Anlegen wählen und jederzeit nachträglich
   ändern (`F-42`, `F-43`) — vorher schätzte die KI sie allein und es gab keinen Weg zurück.
5. **Vorlesen gibt es überall und es fällt nie aus.** Ein Lautsprecher an jeder Gesprächsrunde,
   jeder Erkenntnis, jedem Logbuch-Tag und jeder Auswertung (`F-47`); kommt der gewählte Weg
   nicht durch, übernimmt die **Stimme des Geräts** (`F-48`).
6. **Was ohne Netz liegenbleibt, wird wirklich nachgeholt** (`F-56`) — und in **seinen** Tag
   geschrieben, nicht in den heutigen.
7. **Die Wege durch die App sind ganz:** Mikrofon-Erlaubnis wird erfragt (`F-50`), der Rückweg
   ist ein Stapel und die Zurück-Taste tut, was sie soll (`F-51`), das Selbstbild wird dreifach
   gesichert (`F-52`), und der Tageswechsel im laufenden Betrieb wird nachgezogen (`F-57`).

---

### Was v1 (überarbeitet) am 12.08.2026 geändert hatte

Frank hatte das Spec damals in drei Punkten grundlegend geändert. Alles Übrige galt
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

*Diese drei Punkte gelten in v2 unverändert weiter — sie sind umgesetzt.*

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
| Android | Samsung Galaxy Z Fold 8 — **Außendisplay**, Hochformat; zusätzlich Galaxy S23 Ultra | Kotlin + Jetpack Compose, Material 3 | **Pflicht** |

> **Geändert in v2.** Gebaut und abgenommen wird auf dem **Fold-Außendisplay** — daher der Name
> des Entwurfs. Das ist der schmalste Fall; was dort passt, passt überall. Die Oberfläche ist
> durchgehend fließend gebaut (Gewichte statt fester Breiten), das Aufklappen des Geräts erzeugt
> die Activity neu, und genau dabei durfte die App nicht in den Abend-Zustand zurückspringen
> (siehe Funktions-Spec §6).

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
| `POST_NOTIFICATIONS` | Erinnerungen morgens/abends | Beim ersten Einschalten einer Erinnerung | Erinnerungen bleiben aus, ein einmaliger Hinweis erscheint |
| `INTERNET` | Alle Dienste | — (normale Berechtigung) | — |
| `VIBRATE` **NEU in v2** | Die Haptik `E-23` | — (normale Berechtigung) | Ohne sie wirft das Rütteln eine SecurityException — **daran ist die App gestorben**, weil sie die Aufnahme mitriss. Sie steht jetzt im Manifest, und das Rütteln ist zusätzlich abgefangen |
| `MODIFY_AUDIO_SETTINGS` **NEU in v2** | Der Lautstärke-Heber hebt die Sprachausgabe um 12 dB an | — (normale Berechtigung) | Die rohen Sprachdateien der Anbieter liegen deutlich unter dem Pegel, den ein Telefonlautsprecher braucht — ohne sie ist das Vorlesen kaum hörbar |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` **NEU in v2** | Exakte Weckzeiten der Erinnerungen | — | Die App weckt **ungenau** statt gar nicht |
| `RECEIVE_BOOT_COMPLETED` **NEU in v2** | Weckzeiten nach einem Neustart des Geräts neu setzen | — | Die Erinnerungen wären nach einem Neustart weg |

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
| **A-31 NEU** | Die Tagesangabe auf einer Monitor-Karte lässt sich antippen; im Dialog „Wie lange?" wird die Dauer geändert. Beim Verlängern kommen die Aufgaben der neuen Tage dazu, beim Kürzen geht **nichts** verloren, und ein laufendes Experiment lässt sich nicht kürzer machen als der Tag, an dem es steht |
| **A-32 NEU** | Beim Anlegen eines eigenen Experiments (Monitor **und** Merkliste) steht die Tagewahl bereit; die gewählte Dauer gilt auch dann, wenn kein Netz da ist |
| **A-33 NEU** | Eine Auswertung am letzten Tag beendet das Experiment **nicht**. Es erscheint die Frage „Wie soll es weitergehen?" mit vier Wegen: Weiterführen · Abschließen · Zwischenstand · Nicht umgesetzt |
| **A-34 NEU** | Zwei Auswertungen zum selben Experiment am selben Kalendertag stehen **beide** im Verlauf, jede mit Datum, Uhrzeit und Versuchstag; keine überschreibt die andere |
| **A-35 NEU** | Der Logbuch-Reiter *Auswertungen* zeigt je Experiment ein Fach mit allen seinen Aufnahmen im vollen Wortlaut — auch bei einem abgeschlossenen und bei einem gelöschten Experiment |
| **A-36 NEU** | An jeder Gesprächsrunde, jeder Erkenntnis, jedem Logbuch-Tag und jeder Auswertung steht ein Lautsprecher. Es spricht immer nur einer; ein zweiter Druck auf denselben hält an |
| **A-37 NEU** | Ohne Netz, ohne Schlüssel oder bei abgelehntem Aufruf liest die **Stimme des Geräts** vor, und ein Satz sagt warum. Sie ist außerdem als vierter Anbieter wählbar |
| **A-38 NEU** | Der Umschalter auf „Heute" wechselt zwischen Morgen und Abend; die Abend-Erinnerung führt auf denselben Zustand, auch wenn die App schon im Speicher liegt |
| **A-39 NEU** | Der erste Druck auf einen Sprechknopf fragt die Mikrofon-Erlaubnis ab; nach der Zustimmung läuft die gewollte Handlung **weiter**, ohne dass noch einmal gedrückt werden muss |
| **A-40 NEU** | Die Zurück-Taste und die Wischgeste des Geräts führen denselben Weg zurück wie der Pfeil; kein Bildschirm ist eine Sackgasse; auf dem Monitor verlässt die Zurück-Taste die App |
| **A-41 NEU** | Ein Selbstbild-Text überlebt: den Speichern-Knopf, das Verlassen des Bildschirms **und** das Wegwischen der App aus dem Speicher. Der Stand steht ablesbar darunter |
| **A-42 NEU** | Was ohne Netz liegenbleibt — Aufgabenliste, Erkenntnis, Verdichtung, Logbuch-Rohstoff — ist nach dem nächsten Start mit Netz nachgetragen, und der Rohstoff steht im **richtigen** Tag |
| **A-43 NEU** | Bleibt die App über Mitternacht offen liegen, schreibt sie nach dem Zurückkehren in den **neuen** Tag, und die Verdichtung läuft |
| **A-44 NEU** | Ziele und Logbuch-Tage lassen sich ändern und löschen; das Löschen eines Logbuch-Tages fragt einmal zurück |
| **A-45 NEU** | In den Einstellungen stehen alle 31 Chirp-3-Stimmen und alle 6 Edge-Stimmen zur Wahl; die eigenen Stimmen werden bei Alibaba abgerufen, sind wählbar, aufnehmbar und löschbar; die Version steht sichtbar am Fuß der Seite |

---

## 6. Offene Fragen

Keine. Alle Punkte wurden im Grilling entschieden, und alles Weitere ist inzwischen gebaut.

**Was in v2 bewusst anders ist als im v1-Spec** — hier weicht die App ab, und die App gewinnt:

1. **`F-26`:** Das Symbol des Erscheinungs-Schnellschalters zeigt den **aktiven** Modus, nicht
   den nächsten. v1 forderte das Gegenteil; das las sich von außen wie eine Falschanzeige.
2. **`F-39`:** Kein Rückfrage-Dialog mit zwei Wegen mehr — Wischen und Kreuz legen das
   Experiment **auf die Merkliste**. Endgültig gelöscht wird dort (`F-19`).
3. **`F-35`:** Die **Stufe** wird beim Anlegen nicht mehr von Hand gewählt (die KI schätzt sie);
   an ihrer Stelle steht die **Dauer**, weil genau dort die Schätzung danebenlag.
4. **`F-10`:** `B-03` arbeitet **ein** Experiment ab, nicht alle offenen der Reihe nach.
5. **`F-41` / Motion §8:** Auf *Gedämpft* stehen Dauerbewegungen still und bleiben **sichtbar**
   (45 %); v1 sagte „aus".

*Zur Sicherheit festgehalten:* Für die Abnahme muss ein Android-Gerät angeschlossen sein
(`adb devices`). Gebaut und abgenommen wird auf dem **Fold-Außendisplay**.