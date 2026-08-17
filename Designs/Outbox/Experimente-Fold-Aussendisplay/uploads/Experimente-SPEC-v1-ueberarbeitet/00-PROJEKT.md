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