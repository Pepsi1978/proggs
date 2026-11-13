# 11 – Open-Source-Lizenzen, Marken, Inhalte

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Bibliotheken und Lizenzen (Auswahl, aus Abhängigkeitsbaum; Lizenzangaben nach allgemein bekannten Projektlizenzen [A], am Prüftag nicht einzeln abgerufen)
| Komponente | Lizenz | Copyleft? | Hinweispflicht |
|---|---|---|---|
| AndroidX, Compose, Room, Hilt/Dagger, Firebase-SDKs, Play-Bibliotheken, Google API Client, Coil, Lottie, OkHttp, Retrofit, Moshi, Okio, Ktor, kotlinx | Apache 2.0 | nein | Lizenztext/NOTICE beilegen |
| sherpa-onnx | Apache 2.0 | nein | ja |
| onnxruntime | MIT | nein | Lizenztext beilegen |
| Whisper-Modell (base, int8, via sherpa-onnx) | MIT (OpenAI Whisper) [A] | nein | Hinweis |
| Gson, Guava, error_prone, Tink | Apache 2.0 | nein | ja |
| Apache HttpClient/HttpCore, commons-codec/logging | Apache 2.0 | nein | ja |
| javax.inject / jakarta.inject | Apache 2.0 / EPL 2.0 | nein | ja |
| SLF4J | MIT | nein | ja |
| Caveat (Schrift) | SIL OFL 1.1 | nein (Reserved Font Name) | OFL-Text beilegen |
| Material Icons (Compose) | Apache 2.0 | nein | ja |

Keine GPL/LGPL-Komponenten gefunden [T]. **Befund OSS-001**: keine Anzeige der Hinweise in der App, META-INF-Lizenzdateien werden im Build ausgeschlossen, keine OFL-Datei.

## 2. Inhalte
- Splash-Hero-Bild „Gilded Sanctum“ (laut Kommentar „design by Google Stitch“) und Lottie-Animation `success_check.json`: Nutzungsrechte [U] – Nachweis (Quelle/Lizenz) in `evidence/assets-licenses.md` ablegen (KI-generierte Grafiken: Nutzungsbedingungen des Generators prüfen).
- Store-Assets (Icon 512 px, Feature-Grafik, Screenshots): nicht im Repo [U].
- Fremde Texte: Rechtstexte selbst erstellt (KI-unterstützt) – kein Fremdurheberrecht erkennbar.
- Anbieter-Namen (Google, Gemini, Groq, Whisper, Microsoft) werden nur beschreibend genannt; keine Logos [T].

## 3. Marke „Best Journal“
- Beschreibender Charakter („bestes Tagebuch“) → schwache Kennzeichnungskraft, Eintragung als Wortmarke fraglich, aber Nutzung als Titel zulässig, sofern keine ältere verwechslungsfähige Marke besteht.
- Registerprüfung (DPMA, EUIPO, Play-Store-Titel) am Prüftag **nicht durchgeführt** → offener externer Punkt (12, E-12) mit Anleitung.
- Metadata-Risiko: GP-002.
