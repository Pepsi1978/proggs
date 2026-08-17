# Projekt — Experimente

> **Zielplattform für diesen Bau: Android (Kotlin / Jetpack Compose).**
> Beim Herunterladen aus Werft Studio am 2026-08-10 gewählt. Sie gilt vor jeder abweichenden
> Angabe weiter unten in dieser Datei.

Stand: 09.08.2026 · Stufe: v1 · Plattform(en): Android

> **Das Wichtigste zuerst.** Wenn später etwas gegeneinander steht, gilt diese Reihenfolge:
> **1. Die KI muss Frank wirklich kennen.** Selbstbild, 15-Tage-Log, Langzeit-Log und
> Erkenntnisse müssen spürbar in jeden Vorschlag hineinwirken. Im Zweifel gewinnt der Kontext.
> **2. Nichts darf verlorengehen.** Was eingesprochen wurde, ist in einem Jahr noch da —
> verdichtet, aber vollständig.
> **3. Die Vorschläge müssen wirklich neu sein.** Dinge, auf die Frank selbst nie gekommen wäre.
> Die reine Sprachbedienung steht dazu nicht in Konkurrenz — sie wird einmal gebaut und läuft.

---

## 1. Zweck in drei Sätzen

„Experimente" schlägt Frank jeden Tag fünf persönliche Experimente vor — Dinge, die er so
noch nie gemacht hat, aus beliebigen Lebensbereichen, zugeschnitten auf seine aktuelle Lage
und auf alles, was die App über ihn weiß. Er wählt eines aus (bis zu drei dürfen gleichzeitig
laufen), setzt es über den Tag um und spricht abends ein, was daraus geworden ist; die KI
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

---

## 6. Offene Fragen

Keine. Alle Punkte wurden im Grilling entschieden.

*Zur Sicherheit festgehalten, weil es beim Bauen auffallen wird:* Zum Zeitpunkt des Specs
war **kein Android-Gerät angeschlossen** (`adb devices` leer). Vor der Installation in
Schritt 6 der Pipeline muss das Gerät verbunden sein.