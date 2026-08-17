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
