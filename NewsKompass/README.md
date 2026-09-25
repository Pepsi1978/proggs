# News Kompass

Android-App (Kotlin, Jetpack Compose), die zweimal am Tag — um 5 und um 17 Uhr — die Neuigkeiten zu frei formulierten Themen zusammenstellt. Recherchiert wird mit Codex (ChatGPT-Konto, Anmeldung per Gerätecode aus vier plus fünf Zeichen) über die Websuche des Dienstes.

## Was die App macht

- **Themen:** frei formulierte Felder (Stichwort, Frage oder ganzer Satz), per Plus ergänzbar, jederzeit editierbar, per Langdruck am Griff verschiebbar (Modul M1.2). Der erste Block ist vorbelegt mit KI-News zu den Frontier-Modellen. Die Reihenfolge der Themen ist die Reihenfolge der Blöcke; Blöcke werden nie gemischt.
- **News-Logik:** pro Thema eine eigene Codex-Recherche mit Websuche, nur die letzten 24 bis 48 Stunden, nach Relevanz sortiert, zusammengefasste Doppelberichte. Was in den letzten 36 Stunden schon lief, wird nur als „Update“ wieder aufgenommen; Meldungen aus früheren Blöcken derselben Ausgabe werden nicht wiederholt. Jede Meldung trägt eine sprechbare Zeitangabe und ihre Quellen.
- **Bilder:** zuerst das Titelbild (`og:image`) der Quelle, sonst eine KI-Illustration über das Bildwerkzeug von Codex (`image_generation`). Lehnt der Dienst das Werkzeug ab, merkt sich die App das und bleibt bei Fotos. Modus und Höchstzahl in den Einstellungen.
- **Vorlesen:** vorlesefreundliches Deutsch mit Umlauten, ohne Zeichen und Adressen. Lautsprecher an jeder Meldung und an jedem Block. Absatz-Pipeline aus KompassKern: Während ein Absatz spricht, werden die nächsten zwei schon synthetisiert. Anbieter Google Chirp 3 HD, Microsoft Edge oder die eigenen Alibaba-Stimmen.
- **Modelle:** Live-Liste aus dem Codex-Konto (`/backend-api/codex/models`), Startbestand GPT-6 Astra/Sol/Luna, GPT-5.6 Sol/Terra/Luna, GPT-5.5; Denktiefe je Modell.
- **Zeitplan:** ein exakter AlarmManager-Wecker, der sich selbst neu stellt; die Arbeit läuft als WorkManager-Vordergrundauftrag. Versäumte Läufe werden beim Öffnen nachgeholt.
- Hell-, Dunkel- und Systemmodus.

## Herkunft

`ai/CodexClient.kt`, `tts/*`, `network/`, `observability/` sind Kopien aus `KompassKern` (Paket umbenannt). Der Codex-Client ist um Werkzeuge, Quellen- und Bildsammlung sowie den Modellabruf erweitert.

## Bauen

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

Version und Stand kommen aus `app/src/main/assets/versionslog.json`.
