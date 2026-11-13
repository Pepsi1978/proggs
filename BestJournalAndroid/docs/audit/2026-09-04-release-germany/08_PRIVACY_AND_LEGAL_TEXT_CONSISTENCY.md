# 08 – Rechtstexte: Konsistenz mit dem Code

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Inventar der Texte [T]
| Dokument | Maßgebliche Quelle | Weitere Fassungen | Stand |
|---|---|---|---|
| Datenschutzerklärung DE | `app/src/main/assets/legal/de/DATENSCHUTZ.html` (Policy 3.2) | `docs/bestjournal/privacy-de.html` (identisch), `docs/DATENSCHUTZ.md/.html` (veraltet, April) | „12. Juli 2026“ / Fußzeile „20. April 2026“ (Inkonsistenz) |
| Nutzungsbedingungen DE | `assets/legal/de/NUTZUNGSBEDINGUNGEN.html` | gehostet identisch bis auf Navigationszeile; `docs/*.md` veraltet | 20. April 2026 |
| Impressum DE | `assets/legal/de/IMPRESSUM.html` | gehostet identisch bis auf Navigation | – |
| EN-Vollfassungen | `assets/legal/en/PRIVACY/TERMS/IMPRINT.html` | gehostet identisch | – |
| 24 weitere Sprachen | Kurzfassungen mit Link auf DE/EN (LegalDocumentScreen Tier 3) | – | – |
| Einwilligungstexte | `strings.xml` consent_*, privacy_gate_* (27 Sprachen) | – | – |
| KI-Hinweise | ai_generated_badge/tooltip, ai_output_health_disclaimer, ai_improved_suffix, ai_banner_* | – | – |
| Krisenhinweise | settings_crisis_* | – | – |
| Store-Texte, Screenshots | **nicht im Repo** | – | [U] |
| OSS-Lizenzhinweise | **fehlen** (OSS-001) | – | – |

## 2. Datenschutzerklärung (DE, in-App) – Abgleich Abschnitt für Abschnitt
| Abschnitt | Aussage | Code-Realität | Bewertung |
|---|---|---|---|
| 1 Verantwortlicher | Name, c/o-Anschrift, E-Mail; „DSB nicht erforderlich“ | – | DSB-Aussage falsch (DS-006); c/o RT-004 |
| 2 Überblick | Tabelle inkl. „Anmeldung (Google Sign-In / Firebase Authentication)“, „Werbe-ID“ | Kein Firebase Auth; Werbe-ID nur transitiv | RT-001(d), GP-001 |
| 3 Berechtigungen | INTERNET, RECORD_AUDIO, CAMERA, POST_NOTIFICATIONS, BOOT | stimmt; Kamera: Galerie-Kopie fehlt | DS-008 |
| 3a Geltungsbereich | „27 Sprachen im Play Store verfügbar“, weltweit | DE-only-Start | RT-003 |
| 4 Lokal | SQLite, Löschung, PDF-Export | stimmt; „geschützt“ = Sandbox (nicht verschlüsselt) – akzeptabel | ok |
| 5.1 Groq | „Nur wenn Cloud-Transkription aktiviert … Standard lokal“; „nach Verarbeitung gelöscht“; SCC | Standard Groq in Testphase/Abo ohne Consent-Check; 30 Tage; SCC [U] | DS-001, RT-001(b,e) |
| 5.2 Lokal | „Einmaliger Download des Sprachmodells (~100 MB)“ | Modell ist im AAB enthalten (151 MB), kein Download | falsch, NIEDRIG → korrigieren |
| 5.2a Edge-TTS | Art. 49-Einwilligung, „kein Vertrag“, „lokale Vorlese-Stimme“ | keine lokale Stimme | RT-001(a), DS-003 |
| 5.3 Drive | „Verschlüsseltes Backup“ | unverschlüsselte DB, TLS + Google at-rest | DS-009 |
| 5.3a System-Backup | Beschreibung korrekt inkl. Stimmungswert-Hinweis | stimmt | ok |
| 5.4 Sign-In | E-Mail, Name, Avatar; lit. b | lokal gespeichert; lit. b vertretbar | ok |
| 5.5 Firebase Authentication | beschrieben | nicht vorhanden | RT-001(d) |
| 5.6 Gemini | „Textausschnitte“, Pre-Usage-Gate vor **jeder** Übermittlung, Deaktivierung je Funktion, DPF+SCC, „nach Verarbeitung gelöscht, kein Training“ | alle Einträge; Gate nur manuell; Löschung: Missbrauchs-Logging | DS-002, DS-005, RT-003 |
| 5.7 Analytics | Opt-in, AAID, § 25 TDDDG | stimmt (AAID nach GP-001-Fix streichen) | ok |
| 5.7a Churn-Umfrage | 4 Kategorien | stimmt (`trackChurnReasonSelected`) | ok |
| 5.8 App Check, 5.9 Remote Config | lit. f | stimmt; Remote Config liefert Groq-Key (Hinweis) | ok/RT-003 |
| 5.9a Feedback (Gmail) | zwei E-Mails, 24 Monate | stimmt | ok; OAuth-Verifizierung [U] |
| 5.10 Review | lit. f | stimmt | ok |
| 5.11 Drittland | DPF Google, SCC Groq, Art. 49 Microsoft | Google [U], Groq [U] | ok, Nachweise 12 |
| 6 Käufe | Kaufstatus-Prüfung via Cloud Function EU | stimmt | ok |
| 7 Löschung | 7.1 „Authentifizierungsdaten (Firebase Auth)“ | nicht vorhanden; sonst korrekt (lokal, Cache, Drive) | RT-001(d) |
| 8 Rechte | E-Mail-Kontakt; HBDI-Adresse | Adresse veraltet | RT-003 |
| 8a International | CCPA/LGPD/… | für DE irrelevant | RT-003 |
| 9 Sicherheit | „Lokale Verschlüsselung sensibler Daten (Security Crypto)“ | nur Prefs, nicht DB | präzisieren (NIEDRIG) |
| 10 Kinder | ab 13 / 16 Elternzustimmung | keine Mechanik | ALT-001 |
| 11 Speicherdauer | Groq/Gemini/TTS „nach Verarbeitung gelöscht“; Serverlogs 30 Tage | Groq 30 Tage; Gemini Missbrauchs-Log; TTS unbekannt; Server = Cloud Function | RT-001(b), RT-003 |
| 12/12a/12b | Art. 22 nein; Art. 50; Mental-Health-Disclaimer, Nummern | stimmt; SB 243-Absatz für DE irrelevant | ok |
| 13 Pflicht | keine | stimmt | ok |
| 14 Änderungen | „in der App darauf hingewiesen“ | keine Re-Consent-Mechanik | DS-004 |

Fehlende Angaben (Art. 13): Rechtsgrundlage der lokalen Speicherung/Vertragserfüllung; Hinweis auf Art. 11 (keine Identifizierung, Rechteausübung in-App); § 25 TDDDG-Ausnahme für technisch notwendige Kennungen; Galerie-Kopie; Umfang der Übermittlung; DSB-Kontakt (nach Bestellung).

## 3. Nutzungsbedingungen – Abgleich
Siehe VR-001, RT-002 und RT-001(c). Zusätzlich: Abschnitt 2 nennt „Videos“ – stimmt (`coil-video`, `isVideo`); Abschnitt 5.1 Testphase 8 Tage – stimmt (`TRIAL_USAGE_DAYS`); 5.2 Free-Limits – 5/Woche **je Profil**, 5 Textverbesserungen, 2 Wochenrückblicke – stimmt bis auf „je Profil“; 10a Aktualisierungspflicht – vorhanden; 13 § 36 VSBG – vorhanden; 16 Widerrufsbelehrung – Adressat (VR-001). Einbeziehung: Link im Consent-Screen und Paywall („Mit dem Abo akzeptierst du die Nutzungsbedingungen“) – für den Nutzungsvertrag ausreichend [R].

## 4. Impressum – Abgleich (§ 5 DDG)
Name ✔, Anschrift c/o (Restrisiko), E-Mail ✔, zweiter Kommunikationsweg: keiner (EuGH C-298/07: E-Mail mit zügiger Antwort genügt) [R], USt: § 19 UStG ✔ (Verkauf über Google Commerce; eigene USt-Pflicht [U]), § 36 VSBG ✔, keine OS-Plattform ✔, MStV-Absatz überflüssig. In der App erreichbar: Consent-Screen (vor Onboarding), Einstellungen → Rechtliches ✔; ständig verfügbar ✔.

## 5. App- vs. Web-Fassung
Textvergleich per Hash: DE/EN Datenschutz identisch; AGB/Impressum unterscheiden sich nur durch die Navigationszeile „← Zurück zur Übersicht“ [T]. Versionsstände identisch. Da die Website 404 liefert, ist der Vergleich nur für die Quelldateien gültig.

## 6. Übersetzungen der rechtsrelevanten Strings
Schlüsselabdeckung 100 % (nur nicht-übersetzbare Schlüssel fehlen). Krisen-Nummern in allen Sprachen deutsch (korrekt für DE). Bedeutungsprüfung nur stichprobenartig (EN Consent-Texte gelesen: inhaltsgleich). Die Kurzfassungen der 24 weiteren Sprachen verweisen auf DE/EN als maßgeblich – gut. Nach den Textkorrekturen (RT-001/003, DS-005, ALT-001) müssen **alle** Sprachfassungen der betroffenen Strings und die EN-Vollfassungen nachgezogen werden, sonst entstehen neue Widersprüche.
