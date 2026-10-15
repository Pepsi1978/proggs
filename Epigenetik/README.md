# Epigenetik

Android-App (Kotlin, Jetpack Compose, Room, WorkManager), Paket `de.frank.epigenetik`. Findet heraus, welche
Nahrungsergänzungsmittel (Hauptfokus), Nahrungsmittel und Verhaltensweisen welche epigenetischen Schalter
an- oder abschalten – und umgekehrt, was eine gewünschte Wirkung oder ein Ziel erreicht.

## Bedienung

- **Start:** ein Eingabefeld für alles (tippen oder Mikrofon). Die App erkennt, ob ein Mittel, eine Wirkung, ein Ziel
  oder eine Frage gemeint ist (Chips zum Umschalten), bei Mitteln auch Supplement/Nahrung/Verhalten. Mehrere Mittel mit
  Komma trennen. Laufende Recherchen mit Live-Balken, „Neu für dich“, stärkste Mittel, Schnellstart-Chips.
- **Wissen:** Bibliothek aller Recherchen, Suche auch nach Genen/Schaltern, Filter, Sortierung, Favoriten.
- **Atlas:** festes Vokabular aus 47 Schaltern in 12 Gruppen (`data/Schalter.kt`). Zeigt offline, welche recherchierten
  Mittel einen Schalter in welche Richtung bewegen; ein Tipp startet die Rückwärtssuche.
- **Ziele:** eigene Ziele einsprechen oder Vorlagen antippen (Langlebigkeit, Krebsprävention …) → Schalter, beste Mittel,
  Stack-Vorschlag, messbare Marker.
- **Stack:** was du nimmst; Abdeckung der Schalter (offline) und KI-Analyse mit Synergien, Lücken, Risiken, Tagesplan.
- **Detail:** Potenzial 0–100, Evidenz, Schalter mit Richtung/Stärke/Evidenz/Mechanismus, Dosierung, Einnahme,
  Risiken, Synergien (ein Tipp recherchiert sie), Quellen, Notiz. Vorlesen, Teilen, neu recherchieren.

## Technik

- **KI:** ChatGPT über die Codex-Anmeldung (Gerätecode), Websuche an, Antwort als JSON (`ki/EpigenetikKi.kt`).
  Prompts mit Evidenz-Regeln: Zellkultur/Tier ≠ Mensch, Stärke ohne Humanstudie höchstens 40, keine erfundenen Quellen.
- **Recherchen** laufen als WorkManager-Expedited-Jobs (`ki/Forscher.kt`), parallel (Standard 3), mit Benachrichtigung
  beim Abschluss – auch wenn Jarvis sie bei geschlossener App startet.
- **Spracheingabe:** ohne Schlüssel die Spracheingabe des Handys, mit Groq-Schlüssel Whisper mit Halluzinationsfiltern.
- **Vorlesen:** Edge-Stimmen ohne Schlüssel (Standard) oder Google Cloud. Optik und TTS aus „Geniale Aufgaben“.

## Brücke zu Jarvis

ContentProvider `content://de.frank.epigenetik.jarvis`, Erlaubnis `de.frank.jarvis.permission.BRUECKE` (signature),
`call(methode, Bundle("json"))`. Methoden: `info`, `lesen` (mit `recherchieren_wenn_fehlt`, `warte_ms`), `recherchieren`,
`status`, `liste`, `schalter`, `stack`, `stack_setzen`. Jarvis-Werkzeuge: `epigenetik_lesen`, `epigenetik_recherchieren`,
`epigenetik_schalter`, `epigenetik_uebersicht` (`Jarvis/.../faehigkeit/EpigenetikFaehigkeit.kt`).

## Bauen

Version nur über `app/src/main/assets/versionslog.json` (neuester Eintrag unten). `./gradlew :app:assembleDebug`,
signiert mit dem gemeinsamen Debug-Key aus `~/SK/Android/debug-shared.keystore`.
