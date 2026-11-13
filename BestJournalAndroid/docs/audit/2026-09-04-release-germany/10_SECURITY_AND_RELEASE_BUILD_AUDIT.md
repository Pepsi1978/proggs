# 10 – Sicherheit und Release-Build

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Ergebnisse je Prüfpunkt [T]
| # | Punkt | Ergebnis | Nachweis |
|---|---|---|---|
| 1 | Transportverschlüsselung | BESTANDEN | `network_security_config.xml` (cleartext false, System-CAs); alle Endpunkte HTTPS/WSS; keine Pinning (nicht erforderlich) |
| 2 | Lokale Verschlüsselung | teilweise | Einstellungen/Tokens: EncryptedSharedPreferences (deprecated API); Tagebuch-DB unverschlüsselt (Sandbox + Geräteverschlüsselung); DSE-Aussage präzisieren (DS-009/RT-003). App-Verschlüsselung der DB ist optional (SQLCipher würde Backup/Restore-Pfade brechen) |
| 3 | API-Schlüssel | **HOCH** | Groq-Key per Remote Config an Client (SEC-001); Gemini über Firebase + App Check (gut); Firebase-Android-Key im APK (bestimmungsgemäß, per API-Restriktion absichern [U]); Play-Service-Account nur als Firebase Secret (Function) ✔ |
| 4 | Debug-Logs im Release | MITTEL | Log.* nicht entfernt; PII in 7 Stellen (SEC-002); OkHttp-Body nur Debug ✔ |
| 5 | Inhalte in Logcat | MITTEL | Eintragstext (30 Z.), Transkript-Segmente (40 Z.), Titel, E-Mail (SEC-002) |
| 6 | Crashberichte | BESTANDEN | kein Crash-SDK |
| 7 | Screenshots/Recents | NIEDRIG | kein FLAG_SECURE (SEC-004) |
| 8 | Zwischenablage | BESTANDEN | keine programmatische Nutzung |
| 9 | Android-Backups | BESTANDEN mit Hinweis | Haupt-DB und Klartext-Prefs im System-Backup (in DSE offengelegt); EncryptedPrefs/Dashboard/Retro ausgeschlossen |
| 10 | Exportdateien | BESTANDEN | PDF über SAF (Nutzer wählt Ziel), keine Speicherberechtigung; Teilen per Chooser |
| 11 | Temporäre Audiodateien | BESTANDEN | `cacheDir/recording_*.wav` nach Transkription gelöscht (Erfolg/Fehler); bei App-Abbruch während Aufnahme verbleibt Datei bis Cache-Löschung/Kontolöschung (NIEDRIG: beim Start `cacheDir` von `recording_*` bereinigen) |
| 12 | Cache | BESTANDEN | kein HTTP-Cache konfiguriert; TTS-MP3 nach Wiedergabe gelöscht; `drive_merge_temp.db` bei Kontolöschung gelöscht |
| 13 | WebView | BESTANDEN | nur lokale Assets, JavaScript aus, externe Links im Browser, kein Datei-/Content-Zugriff |
| 14 | Zertifikate/Netz | BESTANDEN | Standard; TrustAll-Klasse nur in ungenutzter Bibliothek (SEC-005) |
| 15 | Klartextverbindungen | BESTANDEN | keine (`http://www.w3.org` ist nur SSML-Namespace) |
| 16 | Exportierte Komponenten | BESTANDEN | Launcher-Activity; Boot-Receiver ohne Extras-Auswertung; Bibliothekskomponenten mit Berechtigungen |
| 17 | Deep Links | nicht vorhanden | – |
| 18 | FileProvider | BESTANDEN | nur `photos/`, nicht exportiert, Grant-URIs |
| 19 | Datenbankzugriffe | BESTANDEN | Room; keine Roh-SQL mit Nutzereingaben (Restore-Pfad-Rewrite mit festen Strings) |
| 20 | Löschfunktionen | BESTANDEN mit Auflage | Eintrag (CASCADE), alle Daten, Konto (Drive zweistufig verifiziert, Cache, Fotos, Prefs); Galerie-Kopie bleibt (DS-008); Analytics-Reset fehlt (DS-010); System-Backup bleibt (DSE erklärt) |
| 21 | Wiederherstellung | BESTANDEN | Drive-Restore nur mit Google-Konto; Merge-Logik |
| 22 | Minimale Berechtigungen | HOCH (AD_ID) | GP-001; sonst minimal |
| 23 | Minifizierung/Obfuskation | BESTANDEN | R8 + Shrink; Mapping 93 MB |
| 24 | Bekannte Schwachstellen | NIEDRIG | OkHttp 4.12.0 ✔, Apache HttpClient 4.5.14 (letzte 4.x, transitiv, nur Drive), security-crypto alpha06 deprecated (TECH-001); kein OWASP-Dependency-Check-Werkzeug verfügbar → Empfehlung: `gradle-dependency-check` oder GitHub Dependabot |
| 25 | App-Sperre | BESTANDEN | Biometrie/PIN optional, 60-s-Toleranz |
| 26 | 16 KB | BESTANDEN | 24 `.so` alle 16384-ausgerichtet |

## 2. Geheimnis-Scan [T]
- Arbeitsbaum (getrackt): keine Schlüssel/Tokens außer Debug-Keystore-Standardpasswort „android“ und Property-Namen. Keystores/google-services.json gitignored und nur lokal aus `~/SK` synchronisiert.
- Historie (`git log -p --all`, maskiert): eine `google-services.json` (Debug) vom 24.04.2026 für ca. 90 Minuten enthalten; Firebase-Android-Key mit Präfix `AIzaSyCY…` – **abweichend** vom aktuellen Key (`AIzaSyAc…`, Hash-Vergleich). Empfehlung: alten Key in der Google Cloud Console prüfen/löschen (SEC-005). Keine Groq-/OpenAI-/Anthropic-Schlüssel, keine privaten Schlüssel.
- Werkzeuge: `gitleaks`/`trufflehog` nicht installiert (Muster-Scan ersatzweise).

## 3. Build- und Testläufe [T]
| Befehl | Ergebnis |
|---|---|
| `:app:dependencies --configuration releaseRuntimeClasspath` | OK, 1.414 Zeilen |
| `:app:lintRelease` | OK, 1.133 Meldungen (keine Errors; relevante Warnungen: MissingPermission ×2, TrustAllX509TrustManager ×2 (Bibliothek), GradleDependency ×41, MonochromeLauncherIcon ×2, ExifInterface ×1) |
| `:app:testReleaseUnitTest` | 34 Tests, 0 Fehler |
| `:app:bundleRelease` | OK, signiert (Release-Keystore aus ~/SK), `app-release.aab` 151 MB |
| AAB-Analyse | kein `debuggable`; 4 ABIs; Whisper-Modelle 151 MB (Play-Limit für AAB 200 MB – nahe; Empfehlung NIEDRIG: Modell als Play Asset Delivery oder Download) |
| Merged Manifest | AD_ID (GP-001), sonst wie Quelle |

## 4. Defense-in-Depth-Empfehlungen (priorisiert)
1. Groq-Proxy mit App Check (SEC-001) – beseitigt Schlüssel- und IP-Exposition.
2. Log-Stripping + PII-freie Logs (SEC-002).
3. `enforceAppCheck: true` (SEC-003).
4. Consent-Sperre zentral (DS-001/002) – verhindert auch künftige Umgehungen.
5. Optional: FLAG_SECURE-Schalter, Cache-Bereinigung beim Start, Dependabot.
