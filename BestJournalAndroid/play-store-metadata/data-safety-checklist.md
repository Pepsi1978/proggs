# Google Play Data Safety + Health Apps Declaration — Upload-Checkliste

**Stand:** 23.04.2026
**Basis:** Rechtssicherheits-Audit v5 (NB9)
**Verantwortlich:** Frank Barwandt
**App:** com.bestjournal.app

---

## Abschnitt 1 — Data Safety Section (Pflicht beim Upload)

Diese Kategorien muessen beim Play-Console-Upload genau so deklariert werden.
Quelle: Researcher #3 (v5-Audit) auf Basis aktueller Google Play Data Safety Form 2026.

### 1.1 Audio files

| Feld | Wert |
|------|------|
| Category | Audio files |
| Subcategory | **Voice or sound recordings** |
| Is this data collected? | Yes |
| Is this data shared? | **No (processed by Groq as a service provider, deleted after transcription)** |
| Is this data optional? | **Yes — only when user enables cloud transcription in Settings** |
| Is this data encrypted in transit? | Yes (TLS 1.2+) |
| Can users request deletion? | Yes (deleted after processing; user has account deletion in Settings) |
| Purposes | App functionality (speech-to-text transcription) |

### 1.2 App activity

| Feld | Wert |
|------|------|
| Category | App activity |
| Subcategory | **App interactions** |
| Is this data collected? | Yes (Firebase Analytics events) |
| Is this data shared? | **Yes — with Google (Firebase Analytics)** |
| Is this data optional? | **Yes — opt-in only, default OFF** |
| Is this data encrypted in transit? | Yes |
| Can users request deletion? | Yes (Settings — Privacy — revoke) |
| Purposes | Analytics |

| Feld | Wert |
|------|------|
| Category | App activity |
| Subcategory | **Other user-generated content** (AI-improved text + AI summaries) |
| Is this data collected? | Yes (transient — sent to Gemini for processing) |
| Is this data shared? | **No (Gemini acts as a service provider, deleted after processing)** |
| Is this data optional? | **Yes — opt-in via PrivacyGateDialog** |
| Is this data encrypted in transit? | Yes |
| Can users request deletion? | Yes |
| Purposes | App functionality |

### 1.3 Device or other IDs

| Feld | Wert |
|------|------|
| Category | Device or other IDs |
| Subcategory | **Device or other IDs** (Firebase Instance ID, App Check Play Integrity Token, Android Advertising ID) |
| Is this data collected? | Yes |
| Is this data shared? | **Yes — with Google Firebase / Google Play Integrity** |
| Is this data optional? | Advertising ID optional; Firebase Instance ID is operational |
| Is this data encrypted in transit? | Yes |
| Purposes | Security (App Check), Analytics (opt-in), Fraud prevention |

### 1.4 App info & performance

> **KORRIGIERT (Audit v8, 2026-07-12):** Firebase **Crashlytics ist NICHT eingebaut**
> (kein Eintrag in `gradle/libs.versions.toml`). Die fruehere Zeile "Crash logs: Yes
> (Firebase Crashlytics)" war eine Vorlagen-Altlast und haette beim Submit eine
> Falschdeklaration erzeugt.

| Feld | Wert |
|------|------|
| Category | App info & performance |
| Subcategory | **Crash logs** |
| Is this data collected? | **No** (kein Crashlytics-SDK; Play sammelt eigene Vitals unabhaengig von der App) |

### 1.5 Files & documents

| Feld | Wert |
|------|------|
| Category | Files & documents |
| Subcategory | **Files and docs** (journal database backup) |
| Is this data collected? | No (user-controlled upload to Google Drive) |
| Is this data shared? | **Yes — with Google Drive (stored in user's own Drive app data folder)** |
| Is this data optional? | **Yes — only when user enables Cloud Backup** |
| Encrypted? | Yes |
| Purposes | Backup, Data portability |

### 1.6 Personal info

| Feld | Wert |
|------|------|
| Category | Personal info |
| Subcategory | **Email address** (Gmail OAuth for feedback) |
| Is this data collected? | Yes |
| Is this data shared? | **Yes — in the Feedback flow, the user's Gmail address appears as sender and is received by dev.app.support@gmail.com** |
| Is this data optional? | **Yes — only when user sends feedback** |
| Encrypted? | Yes |
| Purposes | Developer communications |

### 1.7 Sensitive content note

**Journal entries themselves** are NOT transmitted by default. They are only sent to cloud
services if the user explicitly enables optional features (AI analysis, Cloud Backup,
Cloud Transcription). This is disclosed in the Privacy Policy Section 5 and made
explicit via the PrivacyGateDialog (first-use consent).

Journal entries may contain GDPR Art. 9 special categories (health, religion,
political opinion). We treat this in the Privacy Policy by requesting **explicit
consent** in the Gemini Privacy Gate dialog (per Art. 9 (2) (a) GDPR).

---

## Abschnitt 2 — Health Apps Declaration Form

Seit August 2025 ist die Health Apps Declaration im Play Console PFLICHT fuer jede
App (auch "no health features"-Apps muessen sie einreichen).

### 2.1 Einordnung BestJournal

| Frage | Antwort |
|-------|---------|
| Does your app provide health features? | **Yes** |
| Which category? | **Stress Management, Relaxation, Mental Acuity** |
| Is it a Medical Device? | **No** — the app does not diagnose, treat, or cure any medical condition |
| Does the app provide medical advice? | **No** — AI outputs are described as "reflection support" / "Vorschlaege zur Reflexion", not advice |
| Does it rely on clinical validation? | **No** — no published clinical studies, no claimed treatment |
| Regulated health product/service? | **No** |
| Explicit mental health disclaimer present? | **Yes** — Privacy Policy Section 12b + ToS Section 4.1 + in-app "Crisis Help" dialog |

### 2.2 Warum NICHT "Medical"?

- Die App macht **keine Diagnose** (weder UI-seitig noch in AI-Output)
- Kein Therapie-Anspruch
- KI-Rueckblicke sind als "Vorschlaege zur Selbstreflexion" formuliert (nicht als "Beurteilung")
- HWG-konforme Sprache durchgaengig (siehe Researcher #5 Audit v5)
- Crisis-Helpline verweist auf Fachpersonen, ersetzt sie nicht

### 2.3 Warum "Stress Management / Mental Wellness"?

- KI-Rueckblicke analysieren Tagebucheintraege auf Themen/Stimmungen
- Meditations-aehnliche Funktion: taegliche Reflexion
- Ziel: Nutzer zur Selbstbeobachtung anregen (kein Behandlungsanspruch)

---

## Abschnitt 3 — AI-Generated Content Policy

| Frage | Antwort |
|-------|---------|
| Generates AI content? | Yes (Gemini für Zusammenfassungen, Textverbesserung, Retrospective) |
| Central chatbot feature? | **No** — AI is a productivity aid, not the central interaction |
| AI creates voice/video of real people? | **No** |
| In-app reporting mechanism for offensive AI output? | User can "report AI output" — bereits per `settings_report_ai_title` string impl. |
| AI labeled in UI? | **Yes — "KI-generiert" / "AI-generated" Badge** (Dashboard, Retrospective, Text-Improve) |

---

## Abschnitt 4 — Target Market / Country Exclusions

Bereits dokumentiert in `country-exclusion.md`. Beim Upload werden folgende
Laender aktiv deaktiviert:

- TR (Tuerkei)
- KR (Korea) — auch nach JP/KO-Volltext-Ergaenzung erstmal ausgeschlossen
- SA (Saudi-Arabien)
- BR (Brasilien) — auch nach PT-BR-Volltext
- VN (Vietnam)
- CN (China)
- RU (Russland)
- BY (Weissrussland)
- IR (Iran)
- KP (Nordkorea) — standard Google-Ausschluss
- CA (Kanada) — wg. Quebec Law 25 (Option A gewaehlt)

Japan bleibt aktiver Markt nach NB6-Volltext-Fix.

---

## Abschnitt 5 — Account Deletion URL

Pflicht seit 2024 im Play Console:

- **URL (verifiziert erreichbar, Audit v8 2026-07-12):** `https://pepsi1978.github.io/proggs/bestjournal/account-deletion.html` (Quelle im Repo: `proggs/docs/bestjournal/account-deletion.html`, GitHub Pages)
- **In-App:** Settings → Konto → Konto loeschen (funktional seit Fix #1699)
- Veraltete Angaben (GitHub-README / mailto / `pepsi1978.github.io/bestjournal-deletion/` = 404) NICHT mehr verwenden.

---

## Abschnitt 6 — Privacy Policy URL

Pflicht im Play Store Listing:

- **URL (verifiziert erreichbar, Audit v8 2026-07-12):** `https://pepsi1978.github.io/proggs/bestjournal/privacy-en.html` (Uebersichtsseite: `https://pepsi1978.github.io/proggs/bestjournal/` mit de/en/ko + AGB + Impressum + Account-Deletion; Quelle: `proggs/docs/bestjournal/`)
- Diese URLs sind identisch mit den in der App verlinkten (strings.xml:1475-1486, "Online-Version").

---

## Abschnitt 7 — Vor dem Upload zu pruefen

- [ ] Data Safety Form exakt wie oben ausfuellen
- [ ] Health Apps Declaration "Stress Management" ausfuellen (nicht Medical)
- [ ] AI Content: In-App-Badge + Report-Button bestaetigen
- [ ] Laender-Ausschluss-Liste setzen (TR, KR, SA, BR, VN, CN, RU, BY, IR, KP, CA)
- [ ] Privacy Policy URL in Store-Listing eintragen
- [ ] Account Deletion URL bestaetigen
- [ ] Content Rating: PEGI 12 / ESRB Teen (Tagebuch-Inhalte koennen intensiv sein, aber keine Gewalt)
- [ ] Pre-launch report: pruefen dass keine unbewusste Datensammlung stattfindet
- [ ] Alle Screenshots ohne personenbezogene Daten, ohne medizinische Claims
