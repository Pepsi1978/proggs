# News Kompass

Android-App (Kotlin, Jetpack Compose), die zweimal am Tag — um 5 und um 17 Uhr — die Neuigkeiten zu frei formulierten Themen zusammenstellt. Recherchiert wird mit Codex (ChatGPT-Konto, Anmeldung per Gerätecode aus vier plus fünf Zeichen) über die Websuche des Dienstes.

## Was die App macht

- **Themen:** frei formulierte Felder (Stichwort, Frage oder ganzer Satz), per Plus ergänzbar, jederzeit editierbar, per Langdruck am Griff verschiebbar (Modul M1.2). Der erste Block ist vorbelegt mit KI-News zu den Frontier-Modellen. Die Reihenfolge der Themen ist die Reihenfolge der Blöcke; Blöcke werden nie gemischt.
- **News-Logik:** pro Thema eine eigene Codex-Recherche mit Websuche, nur die letzten 24 bis 48 Stunden, nach Relevanz sortiert, zusammengefasste Doppelberichte. Was in den letzten 36 Stunden schon lief, wird nur als „Update“ wieder aufgenommen; Meldungen aus früheren Blöcken derselben Ausgabe werden nicht wiederholt. Jede Meldung trägt eine sprechbare Zeitangabe und ihre Quellen.
- **Bilder:** zuerst das Titelbild (`og:image`) der Quelle, sonst eine KI-Illustration über das Bildwerkzeug von Codex (`image_generation`). Lehnt der Dienst das Werkzeug ab, merkt sich die App das und bleibt bei Fotos. Modus und Höchstzahl in den Einstellungen.
- **Vorlesen:** vorlesefreundliches Deutsch mit Umlauten, ohne Zeichen und Adressen. Lautsprecher an jeder Meldung und an jedem Block. Absatz-Pipeline aus KompassKern: Während ein Absatz spricht, werden die nächsten zwei schon synthetisiert. Anbieter Google Chirp 3 HD, Microsoft Edge oder die eigenen Alibaba-Stimmen.
- **Modelle:** Live-Liste aus dem Codex-Konto (`/backend-api/codex/models`), Startbestand GPT-6 Astra/Sol/Luna, GPT-5.6 Sol/Terra/Luna, GPT-5.5; Denktiefe je Modell.
- **Zeitplan:** ein exakter AlarmManager-Wecker, der sich selbst neu stellt; die Arbeit läuft als WorkManager-Vordergrundauftrag. Versäumte Läufe werden beim Öffnen nachgeholt.
- **Mikrofon-Knopf:** schwebt unten rechts. Tippen, Frage sprechen („Was gibt es Neues aus der Weltpolitik?“), noch einmal tippen. Erkannt wird mit Groq Whisper Large V3 Turbo (Deutsch, Temperatur 0) und denselben vier Schichten gegen Stille-Halluzinationen wie in Perfect Moment: Pegelprüfung vor dem Hochladen, Konfidenz je Segment, Abgleich der Segmente mit dem gemessenen Ton, Floskel-Sperrliste. Die Frage wirkt wie ein Thema nur für diesen Moment: Sie landet in keiner Themenliste, sondern als eigener Block (mit Mikrofon statt Nummer) unten in der aktuellen Ausgabe; gibt es noch keine, entsteht eine kleine Ausgabe „Deine Fragen“. Die Antwort wird vorgelesen, sobald sie da ist, und der Block lässt sich per „Entfernen“ wieder herausnehmen. Mehrere Fragen stellen sich hintereinander an. Groq-Schlüssel unter Einstellungen → Spracheingabe.
- Hell-, Dunkel- und Systemmodus.

## Herkunft

`ai/CodexClient.kt`, `tts/*`, `network/`, `observability/` sind Kopien aus `KompassKern` (Paket umbenannt). `audio/*` und `network/OkHttpShutdown.kt` sind Kopien aus `PerfectMoment` (Paket umbenannt), ebenso der Test `WhisperHallucinationFilterTest`. Der Codex-Client ist um Werkzeuge, Quellen- und Bildsammlung sowie den Modellabruf erweitert.

## Bauen

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

Version und Stand kommen aus `app/src/main/assets/versionslog.json`.
