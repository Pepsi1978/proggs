# 07 – Google-Play-Konformität

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Prüfpunkte

| # | Punkt | Ergebnis | Beweis/Befund |
|---|---|---|---|
| 1 | Datenschutz-URL aktiv, öffentlich, kein PDF | **NICHT BESTANDEN** | 404 (WEB-001) |
| 2 | Website erreichbar | NICHT BESTANDEN | 404 |
| 3 | Impressum/Kontakt | BESTANDEN (App) / offen (Web) | Asset vorhanden |
| 4 | Data Safety | siehe Tabelle 3 | GP-003 |
| 5 | Zielgruppe | 16–17 + 18+ | 06 |
| 6 | Inhaltsbewertung (IARC/USK) | Fragebogen ehrlich: keine Gewalt/Sex/Drogen-Inhalte der App selbst; KI-generierte Inhalte ja; nutzergenerierte Inhalte nur privat (kein Austausch); „reife Themen möglich“ | [R] |
| 7 | Generative KI | erfasst; Meldefunktion unzureichend | KI-001 |
| 8 | Nutzergenerierte Inhalte (UGC-Policy) | **nicht anwendbar** (keine Veröffentlichung/Austausch) | [T] |
| 9 | Melden von KI-Inhalten | NICHT BESTANDEN | KI-001 |
| 10 | Berechtigungen | RECORD_AUDIO (Diktat, Laufzeitabfrage), CAMERA (Foto), POST_NOTIFICATIONS (Erinnerungen), RECEIVE_BOOT_COMPLETED; **AD_ID transitiv** (GP-001); keine exakten Alarme (GP-004); keine Foreground-Services; kein All-files-access; Fotoauswahl über Systemauswahl (keine READ_MEDIA-Berechtigung) → Photo/Video-Policy nicht anwendbar; Paketsichtbarkeit nur GMS | [T] |
| 11 | Hintergrundzugriffe | keine | [T] |
| 12 | Health-Deklaration | Pflicht für alle; Antwort: „Health & wellness → Stress management/Mental wellness“; kein Medizinprodukt; keine Health-Connect-Nutzung; Gesundheitsdaten nicht für Werbung; Krisenressourcen vorhanden | [D]/[T] |
| 13 | Sensible Berechtigungen | keine (kein SMS/Call-Log/Location) | [T] |
| 14 | Kontoerstellung/-löschung | App erstellt kein eigenes Konto (Google-Sign-In wird nur lokal genutzt). Die Play-Anforderung „Account deletion“ greift nur bei In-App-Kontoerstellung [D] → strikt nicht anwendbar; da die App aber „Konto löschen“ anbietet und Cloud-Daten (Drive) anlegt, ist die Web-Löschseite freiwillig sinnvoll und in den Vorlagen bereits geplant → nach Hosting-Umzug angeben | [D]/[R] |
| 15 | Prominent Disclosure & Consent | Consent-Screen + Gates erfüllen die Form; **technische Durchsetzung fehlt** (DS-001/002) | [T] |
| 16 | Abos/In-App-Käufe | Monat `bestjournal_ai_monthly`, Jahr `bestjournal_ai_yearly`, Lifetime `bestjournal_lifetime`; Preise aus Play (Fallback-Anzeige 3,99/29,99/79,99 €); Retention-/Exit-Angebote; Testphase 8 Tage in-App; Play-Checkout | [T] |
| 17 | Werbung/Werbe-SDKs | keine Werbung; AD_ID nur transitiv | [T] |
| 18 | Irreführung | Onboarding/Paywall-Texte ehrlich („Die App funktioniert auch ohne Abo weiter“); Rechtstexte mit Falschangaben (RT-001) | [T] |
| 19 | Store-Beschreibung | **NICHT VERIFIZIERBAR** – keine Listing-Texte im Repo; Titel „Best Journal“ (12 Zeichen) Risiko GP-002 | [U] |
| 20 | Screenshots/Grafiken | NICHT VERIFIZIERBAR – keine Store-Assets im Repo (nur `screenshot.png`, `phone_screen.png`, Icon 512 px fehlt als Datei) | [U] |
| 21 | API-Level 36 / 16 KB | BESTANDEN [T]; 16-KB-Sperre für Updates ab 01.02.2027 [D] |
| 22 | Signierung | Release-Keystore in ~/SK; Play App Signing/Upload-Key-Status [U] |
| 23 | AAB | BESTANDEN (151 MB, unter 200-MB-Limit) |
| 24 | Release-Technik | R8/Shrink an, nicht debuggable, Mapping vorhanden → beim Upload hochladen |
| 25 | Entwicklerkonto | NICHT VERIFIZIERBAR (GP-005): Kontotyp, Datum, Testpflicht (12 Tester/14 Tage bei Privatkonto nach 13.11.2023 [D]), öffentliche Anzeige von Name/E-Mail/Anschrift |
| 26 | DSA-Händlerstatus | Unternehmer (Verkauf) → Händlerangaben inkl. Adresse, E-Mail und Telefon werden öffentlich angezeigt [U, Play-Hilfe nicht abrufbar]; c/o-Anschrift möglich? [U] |
| 27 | App-Inhalte-Deklarationen | Liste am Prüftag nicht verifiziert [U]; erwartet: Datenschutz-URL, App-Zugriff (kein Login nötig; Hinweis Premium-Testphase), Anzeigen (nein), Inhaltsbewertung, Zielgruppe, Nachrichten-App (nein), Data Safety, Behörden-App (nein), Finanzfunktionen (nein), Health (ja, wellness), Werbe-ID (nein nach Entfernen), Foto/Video (nicht anwendbar), Foreground-Service (nicht anwendbar) |

## 2. Console-Einstellungstabelle (Empfehlung)

| Einstellung | Wert | Verifizierbar? |
|---|---|---|
| Länder Produktion | **nur Deutschland** (Play Console → Produktion → Länder/Regionen → nur DE hinzufügen; interne/geschlossene Tests dürfen weitere Länder haben) | Console |
| Preis | kostenlos, In-App-Käufe ja | Console |
| Kategorie | Lifestyle (Alternative: Produktivität; nicht „Medizin“) | Console |
| Zielgruppe | 16–17, 18+ | Console |
| Inhaltsbewertung | IARC-Fragebogen (KI-Inhalte ja; UGC nein; reife Themen möglich) | Console |
| Anzeigen | nein | Console |
| Werbe-ID | nein (nach GP-001-Fix) | Manifest [T] |
| Data Safety | Tabelle 3 | Code [T] |
| Health apps | Health & wellness / Stress management, mental wellness; kein Medizinprodukt | Console |
| KI-Inhalte | ja; Meldefunktion ja (nach KI-001) | Code |
| App-Zugriff | „Alle Funktionen ohne besonderen Zugang“; Hinweis: Premium per Lizenztester | Console |
| Datenschutz-URL | neue Hostinger-URL | Web |
| Kontolöschungs-URL | neue Hostinger-URL (freiwillig, empfohlen) | Web |
| Testtrack | geschlossener Test mit ≥12 Testern über 14 Tage, falls Privatkonto nach 13.11.2023 | Console |

## 3. Data-Safety-Tabelle (aus dem Code abgeleitet, Definitionen der Play-Hilfe [D])

Begriffe: *Erhoben* = verlässt das Gerät; *Weitergegeben* = an Dritte, nicht an Auftragsverarbeiter des Entwicklers; *flüchtig* = nur zur Echtzeit-Bearbeitung, nicht länger gespeichert.

| Kategorie / Typ | Erhoben | Weitergegeben | Optional | Zweck | Verschlüsselt (Transit) | Löschbar | Begründung (Datei) |
|---|---|---|---|---|---|---|---|
| Audio → Sprach-/Tonaufnahmen | ja | nein (Groq = Dienstleister) | ja (Cloud-Transkription; nach DS-001-Fix Opt-in) | App-Funktion | ja | ja (Gerät; Anbieter löscht ≤30 Tage) | `TranscriptionRepository.kt:90-103` |
| App-Aktivität → sonstige nutzergenerierte Inhalte (Tagebuchtexte, Nutzer-Prompts) | ja | nein (Google Gemini = Dienstleister, DPA vorausgesetzt [U]) | ja (Opt-in) | App-Funktion | ja | ja | `AdviceRepository.kt`, `SummarizeEntryUseCase.kt`; **nicht** flüchtig (Missbrauchs-Logging) |
| App-Aktivität → sonstige nutzergenerierte Inhalte (Vorlesetexte) | ja | **ja** (Microsoft, kein Dienstleistervertrag) | ja | App-Funktion | ja | nein (Anbieter unbekannt) | `EdgeTtsPlayer.kt` – entfällt bei On-Device-TTS |
| App-Aktivität → App-Interaktionen | ja | nein | ja (Opt-in) | Analytics | ja | ja (Widerruf; Reset nach DS-010) | `AnalyticsTracker.kt` |
| Persönliche Daten → E-Mail-Adresse, Name | ja (nur bei Feedback/KI-Meldung/Widerruf: E-Mail an Entwickler) | nein | ja | Entwicklerkommunikation | ja | ja (auf Anfrage) | `FeedbackSender.kt` |
| Dateien und Dokumente (DB-Backup, Fotos/Videos) | ja (in das eigene Google Drive des Nutzers) | nein | ja (Opt-in) | Backup | ja | ja (in-App) | `SyncWithDriveUseCase.kt` |
| Fotos und Videos | ja (nur Drive-Backup) | nein | ja | Backup | ja | ja | `SyncWithDriveUseCase.kt:101-135` |
| Finanzinfo → Kaufhistorie | ja (Purchase-Token an eigene Cloud Function) | nein | nein (bei Kauf) | App-Funktion | ja | nein (Buchhaltung) | `SubscriptionStatusService.kt`, `functions/index.js` |
| Geräte- oder andere IDs | ja (Firebase-Installations-ID, App-Check-Token; Werbe-ID nur bis GP-001-Fix) | nein | nein | Sicherheit, Analytics | ja | teilweise | `BestJournalApp.kt` |
| Gesundheitsinformationen | **nicht als eigene Kategorie erhoben** (keine strukturierte Abfrage; freie Texte fallen unter „nutzergenerierte Inhalte“). Der Stimmungswert (`moodTag`) verlässt das Gerät nur als Teil der Backups/KI-Texte. Konservative Alternative: zusätzlich „Health info“ mit denselben Angaben deklarieren – empfohlen wegen Prompt-Auswertung von Gefühlen/Belastung [R] | | | | | | |
| Absturzprotokolle, Standort, Kontakte, Kalender, SMS | nein | | | | | | verifiziert [T] |
| Sicherheitspraktiken | Verschlüsselung bei Übertragung: ja · Löschung anfordern: ja (in-App + E-Mail) · unabhängige Sicherheitsprüfung: nein | | | | | | |

## 4. Bestätigung Vertriebsgebiet
Die Produktionsverfügbarkeit ist auf **Deutschland** zu beschränken: Play Console → Release → Produktion → Länder/Regionen → „Länder hinzufügen“ → nur Deutschland. Sprachressourcen bleiben im Bundle, ohne Vertrieb in den zugehörigen Ländern. Store-Eintrag nur in Deutsch (englische Übersetzung des Eintrags optional, kein Vertrieb).
