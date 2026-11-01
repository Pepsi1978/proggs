# Perfect Moment

Android-App für ruhige, KI-generierte Fragesitzungen. Das verbindliche Design liegt in
`../Designs/Perfect Moment Design-Brief/Perfect Moment.dc.html`; die vollständige
Funktionsspezifikation in `02-PROGRAMMIER-SPEC.md`.

## Build

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## Technik

- Kotlin, Jetpack Compose, MVVM, Room
- Codex Device-Code-Login und Responses-SSE
- Groq Whisper mit vierstufigem Halluzinationsfilter
- Microsoft Edge TTS und Google Chirp 3 HD
- Foreground-Service für Sitzungen
- optionale vollständige App-Sperre mit BiometricPrompt

API-Schlüssel werden ausschließlich verschlüsselt auf dem Gerät gespeichert.

## Leitsätze aus Ziele

Unter **Verlauf → Eigene Sessions** steht „Leitsätze“ fest an erster Stelle. Die Session übernimmt sämtliche nicht leeren Leitsätze aller Ziele wortwörtlich und zeigt jeden einzeln. Beim App-Start, Zurückkehren, Öffnen der Session, vor dem Vorlesen und bei Änderungen in Ziele wird automatisch lokal abgeglichen. Auch laufende und pausierte Leitsatz-Sitzungen übernehmen Änderungen, ohne die Restdauer oder den Pausenstatus zu verlieren. Vorlesen, Stimme, Pause, Wiederholungen, Dauer und Zufallsfolge funktionieren wie bei eigenen Sessions; die Texte werden in Ziele bearbeitet. Ist Ziele nicht erreichbar, bleibt der letzte Stand mit einem sichtbaren Hinweis erhalten, während die übrigen Sessions normal verfügbar sind. Wiederherstellungen erkennen die Session an ihrer Quelle und legen sie nicht doppelt an.
