# Play-Store-Release & Policy Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neues Personal-Konto → Production | Closed Test mit **≥12 Testern, 14 zusammenhängende Tage**, dann „Apply for production" | §1 |
| 2 | Track-Reihenfolge | Internal (sofort, ≤100) → Closed → Open → Production; Bundle aus Library promoten | §1 |
| 3 | Update ausrollen | Staged Rollout 5–10 % → Vitals beobachten → hochziehen; **kein Rollback**, nur höherer versionCode | §2 |
| 4 | versionCode | Streng monoton +1 (144→145…), nie wiederverwenden/senken; max 2.100.000.000 | §2 |
| 5 | Kritischen Fix erzwingen | In-App-Updates: Priority 5 (nur per Publisher-API) + Immediate-Flow | §3 |
| 6 | App mit Konto | In-App- **UND Web-URL**-Kontolöschung anbieten (Data-Safety-Pflichtfeld) | §4 |
| 7 | Fotos anhängen | **Photo Picker** statt `READ_MEDIA_IMAGES/VIDEO` (kein Permission, kein Reject) | §4 |
| 8 | Data-Safety-Formular | Jedes SDK (LLM/TTS/Crashlytics) deklarieren; Formular ↔ Privacy-Policy ↔ Verhalten 3-fach abgleichen | §4 |
| 9 | Mikrofon/sensible Daten | **Prominent Disclosure** VOR Permission-Dialog; Tagebuch-PII nie roh in Logs/Crash/Backup | §4, §6 |
| 10 | Drive-Backup-Service | **KEIN `dataSync`-FGS** → Auto-Backup = WorkManager, „Jetzt sichern" = UIDT (sonst Reject + 6h-Falle) | §5 |
| 11 | Mikrofon-FGS | `microphone`-Typ deklarieren, RECORD_AUDIO runtime, sichtbare Notification, Demo-Video | §5 |
| 12 | Vorlesen/Auslösen | AccessibilityService **vermeiden** (TTS-API/Quick-Tile/ACTION_ASSIST); sonst Non-Tool-Declaration | §6 |
| 13 | Pre-Launch-Report | Test-Account hinterlegen (kein Produktiv-Konto); Crashes/Accessibility-Warnungen fixen | §7 |
| 14 | App-Signing | Play App Signing (Google-Key); App-Signing-SHA bei externen APIs eintragen; Upload-Key-Backup (PEM) | §8 |
| 15 | Crash-Stacktraces | AAB + AGP 4.1+ → `mapping.txt` automatisch im Bundle; SafetyNet → **Play Integrity** | §8 |
| 16 | Store-Listing | Titel ≤30, Kurz ≤80, Voll ≤4000; kein „#1/Best/Free", keine Emojis/Keyword-Listen (alle Sprachen) | §9 |
| 17 | targetSdk/16 KB | targetSdk 35 = konform; 16-KB-Page-Size bei nativem Code im Bundle-Explorer prüfen | §9 |
