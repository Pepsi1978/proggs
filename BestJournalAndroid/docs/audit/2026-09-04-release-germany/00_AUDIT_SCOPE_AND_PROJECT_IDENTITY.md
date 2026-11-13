# 00 – Prüfumfang und Projektidentität

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), Version 0.21.17 (versionCode 298)
**Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid` · **Ausschluss:** `C:\Users\barwa\proggs\BestJournalFrank\**` und `Designs/Outbox/Best Journal Frank-*` (nicht geprüft, nicht verändert)
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, Branch `main` · **Prüfdatum:** 04.09.2026 (16:56–17:30 Uhr) · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Eindeutige Zuordnung (Abschnitt 2 des Auftrags)

| Punkt | Feststellung | Beweis |
|---|---|---|
| Repository | Monorepo `C:\Users\barwa\proggs` (Git-Wurzel), Projekt liegt im Unterordner `BestJournalAndroid/` | `git rev-parse --show-toplevel` [T] |
| Abgrenzung zu „Best Journal Frank“ | Eigener Ordner `BestJournalFrank/` mit eigenem Gradle-Projekt (`rootProject`, eigene `app/`). Kein gemeinsames Modul, kein Product Flavor, kein Submodul. Ausschlussmuster in allen Such- und Analysebefehlen: Pfadpräfix `BestJournalAndroid/` bzw. Arbeitsverzeichnis `BestJournalAndroid`. Einziger Querbezug: ein Code-Kommentar in `MainActivity.kt:94` („1:1 wie BestJournalFrank“) – kein Import, keine Abhängigkeit. | `ls`, `settings.gradle.kts`, Grep [T] |
| Projektname | `rootProject.name = "BestJournal"`, Modul `:app` | `settings.gradle.kts:23-24` [T] |
| Application-ID | Release: `com.bestjournal.app` · Debug: `com.bestjournal.app.debug` (`applicationIdSuffix = ".debug"`) | `app/build.gradle.kts:86, 110` [T] |
| Namespace | `com.bestjournal.app` | `app/build.gradle.kts:50` [T] |
| Module | nur `:app` | `settings.gradle.kts` [T] |
| Build-Varianten | `debug` (nicht minifiziert, Suffix), `release` (R8 + Resource-Shrinking, Signatur aus `~/SK/BestJournalAndroid/keystore.properties`) | `app/build.gradle.kts:96-112` [T] |
| Product Flavors | keine | [T] |
| Git-Status vor Beginn | Branch `main`, Arbeitsbaum sauber bis auf `M OpenLauncher/models.json` (fremdes Projekt, nicht berührt). Während des Audits nur das Audit-Verzeichnis neu angelegt (`?? BestJournalAndroid/docs/audit/2026-09-04-release-germany/`). Keine App-Datei verändert, keine Git-Schreiboperation. | `git status --short` 16:53 und 17:19 [T] |
| Frühere Audits | `docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20…-2026-07-12-v8.md` (v2–v8.1). Sie wurden zur Kenntnis genommen, nicht als Beweis übernommen; alle wesentlichen Punkte wurden neu geprüft. | [T] |

## 2. Werkzeugstände

| Werkzeug | Stand | Fundstelle |
|---|---|---|
| Gradle Wrapper | 8.11.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.7.3 (Lint: 9.4.0 verfügbar) | `gradle/libs.versions.toml:2` |
| Kotlin | 2.1.0 (Compose-Compiler-Plugin), KSP 2.1.0-1.0.29 | `libs.versions.toml:3-4` |
| Compose BOM | 2025.01.01 (Compose 1.7.7, Material3 1.3.1) | `libs.versions.toml:5`, Abhängigkeitsbaum |
| JDK (Build) | Eclipse Adoptium 21.0.11 (JVM-Target 17) | PATH, `app/build.gradle.kts:117-125` |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 | `app/build.gradle.kts:51, 87-89` |
| versionCode / versionName | 298 / 0.21.17 | `app/build.gradle.kts:90-91` |
| Room | 2.7.0 (AppDatabase v12, DashboardDatabase v1, RetrospectiveDatabase v2) | `libs.versions.toml:6`, `data/local/*Database.kt` |
| Hilt | 2.55 · Retrofit 2.11.0 · OkHttp 4.12.0 · Moshi 1.15.1 | `libs.versions.toml` |
| Firebase BOM | 34.11.0 (firebase-ai 17.10.1, analytics 23.2.0, config 23.0.1, functions 22.1.0, appcheck-playintegrity 19.0.2) | Abhängigkeitsbaum |
| Play Billing | 7.1.1 · Play Review 2.0.2 · Play Integrity 1.4.0 | Abhängigkeitsbaum |
| sherpa-onnx | 1.12.34 (lokales AAR, 47 MB) + Whisper base int8 (Assets 151 MB) | `app/libs/`, `app/src/main/assets/whisper/` |
| security-crypto | 1.1.0-alpha06 (deprecated) · biometric 1.2.0-alpha05 | `libs.versions.toml:16-17` |
| Cloud Functions | Node 20, firebase-functions ^6, googleapis ^144, Region europe-west1, Projekt `bestjurnal-a15b9` | `functions/package.json`, `functions/index.js`, `.firebaserc` |

## 3. Prüfumfang und Methode

- **Phase 1 (dieses Audit):** Nur lesende Prüfung. Geschrieben wurde ausschließlich in `docs/audit/2026-09-04-release-germany/`. Build-Ausgaben liegen in `app/build/` (gitignored).
- **Ausgeführte Befehle:** Abhängigkeitsbaum (`:app:dependencies --configuration releaseRuntimeClasspath`), `:app:lintRelease`, `:app:testReleaseUnitTest`, `:app:bundleRelease` (signiert mit Release-Keystore aus `~/SK`), Analyse des AAB (Python: ZIP + ELF-Programmheader), Secret-Scan über Arbeitsbaum und Git-Historie (`git grep`, `git log -p`, maskierte Ausgabe), Live-Abruf der Website-URLs (curl). Vollständige Liste in `02_TECHNICAL_INVENTORY.md`, Anhang B.
- **Quellenverifikation:** Drei Recherche-Agenten haben am 04.09.2026 offizielle Quellen abgerufen (Ergebnisse in `15_SOURCES_AND_RETRIEVAL_DATES.md`). Quellen, die technisch nicht lesbar waren (EUR-Lex per WebFetch, Curia-Verfahrensseite), sind als [U] gekennzeichnet; DSK-Muss-Liste und WP248 wurden lokal aus PDF extrahiert [D].
- **Abweichung von der Vorgabe:** Der Auftrag nennt Google Sites als Hosting; tatsächlich ist GitHub Pages verlinkt (Quelle `proggs/docs/bestjournal/`). Der Auftraggeber hat während des Audits mitgeteilt, dass die Seite nicht erreichbar ist und auf einen Hostinger-Server umziehen soll.
- **Angaben des Auftraggebers – Verifikationsergebnis:** KI-Dienste: Groq (whisper-large-v3-turbo) und Google Gemini (gemini-2.5-flash-lite / gemini-3.1-flash-lite über Firebase AI Logic, Gemini-Developer-API-Backend) bestätigt [T]; zusätzlich Microsoft Edge Read-Aloud (TTS), lokales Whisper (sherpa-onnx), Firebase Analytics/Remote Config/App Check/Functions, Google Drive, Gmail-API, Play Billing/Review. Anbindung: Entwicklerschlüssel – Gemini über Firebase-Projekt mit App Check, Groq-Schlüssel über Remote Config an den Client (kein Proxy), kein Bring-Your-Own-Key. Monetarisierung: Abo monatlich/jährlich, Lifetime-Einmalkauf, 8-Tage-Testphase, keine Werbung [T]. Verantwortlicher: Frank Barwandt, c/o Impressumservice, 35410 Hungen (Hessen) → zuständige Aufsicht: Hessischer Beauftragter für Datenschutz und Informationsfreiheit (HBDI), nicht Brandenburg. Google-Play-Konto: „Barwandt Digital Labs“, Kontotyp/Datum [U].

## 4. Lesereihenfolge

1. `01_EXECUTIVE_RELEASE_DECISION_GERMANY.md` – Entscheidung und Blocker
2. `13_REQUIRED_CHANGES.md` – priorisierte Änderungen mit Vorher/Nachher
3. `19_CLAUDE_CODE_AUFTRAEGE.md` – fertig formulierte Aufträge für Phase 2
4. `12_EXTERNAL_EVIDENCE_REQUIRED.md` – was nur der Auftraggeber prüfen kann
5. danach die Fachberichte 02–11, 14–18 nach Bedarf
