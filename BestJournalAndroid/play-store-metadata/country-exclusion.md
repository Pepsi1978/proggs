# Google Play Console — Länderausschluss-Liste für BestJournalAndroid

**Stand:** 2026-04-21
**App-Version:** 0.5.1 (versionCode 51)
**Audit:** v2 vom 2026-04-20 + NK1/NH2-Fixes vom 2026-04-21 (Gemini+Edge-TTS Pre-Usage-Gate, CCPA 2026 Opt-Out-Bestätigung, Do-Not-Sell-Toggle)

---

## Beim Upload in der Play Console: Diese Länder DEAKTIVIEREN

### Automatisch durch Google ausgeschlossen (keine Aktion nötig)
Google Play ist in diesen Ländern nicht verfügbar — Apps werden automatisch nicht ausgeliefert:

- Iran (IR)
- Nordkorea (KP)
- Kuba (CU)
- Syrien (SY)
- Sudan (SD)

### Manuell ausschließen (DDA-Pflicht oder Rechtsrisiko)

| Land | Code | Grund | Risiko bei Nicht-Ausschluss |
|------|------|-------|----------------------------|
| **China** | CN | Play Store in CN nicht verfügbar, separater Markt | — |
| **Russland** | RU | Google Play Billing pausiert seit 10.03.2022, Seller Services suspendiert seit 12/2024 | Keine Zahlungsabwicklung |
| **Belarus** | BY | Entsprechend RU-Sanktionen | Keine Zahlungsabwicklung |
| **Türkei** | TR | VERBIS-Registrierungspflicht (Strafen bis 17 Mio. TRY), KVKK-SCCs seit 09/2024 | Ermessens-Bussgeld KVKK |
| **Südkorea** | KR | Koreanische DSE Pflicht (DeepSeek-Präzedenz 02/2025), PIPC namentliche Empfängerliste | PIPC Corrective Order |
| **Vietnam** | VN | Impact-Assessment-Dossier beim MPS binnen 60 Tagen, neues PDP-Gesetz ab 01.01.2026 | MPS-Ermittlung |
| **Saudi-Arabien** | SA | Arabische DSE + SDAIA-PDPL-Konformität, Voice als biometrisch | SDAIA bis 5 Mio. SAR |
| **Brasilien** | BR | Portugiesische DSE + ANPD-SCCs (seit 23.08.2025 Pflicht, EU-SCCs reichen nicht) | ANPD bis 2% Umsatz |
| **Vereinigtes Königreich** | GB | UK GDPR Art. 27 Vertreter-Pflicht ohne UK-Sitz (Mood = Art. 9 → keine Ausnahme); Entscheidung Benutzer 28.04.2026: UK ausschliessen statt Vertreter-Service buchen | ICO bis £17,5 Mio. Bussgeld |

---

## Freigegeben (nach Fix der Kritischen Audit-Punkte K1–K5)

### DACH + EU (FREI)
DE, AT, CH, NL, FR, IT, ES, PL, BE, SE, DK, FI, NO, IS, LU, IE, PT (nur pt-PT, nicht pt-BR), GR, CZ, SK, HU, RO, BG, HR, SI, EE, LV, LT, MT, CY

### Englischsprachiger Raum (FREI nach PRIVACY.en + Länder-Rights-Sections)
US, UK, CA (exkl. Quebec bis Französisch-Version vorhanden), AU, NZ, IE, ZA, IN, SG, PH, MY, HK, JP (bedingt — Japanisch empfohlen)

### Bedingt freigeben (Englisch reicht weitgehend)
MX (Spanisch empfohlen), AR, CL, CO, PE, KE, NG, EG, MA, AE, IL, ID (Bahasa empfohlen), TH (Thai empfohlen)

---

## Play-Console-Upload-Checkliste

### Data Safety Form
- [x] Alle erhobenen Daten deklariert (Personal info, App activity, Photos/Videos, Audio files, Financial, Device identifiers)
- [x] Alle geteilten Daten deklariert (Groq, Google Gemini, Microsoft Edge, Google Drive)
- [x] Encryption in transit: JA
- [x] Users can request data deletion: JA
- [x] Account-Deletion-Web-URL: `https://pepsi1978.github.io/bestjournal-deletion/` (H6, siehe docs/account-deletion.html)
- [x] In-App-Account-Deletion: JA (Settings → Datenschutz → Konto löschen)

### App-Inhaltsbewertung
- [x] Zielgruppe: 13+ (COPPA)
- [x] Mental Health / Mood: Disclaimer vorhanden, Kategorie "Lifestyle" (nicht "Medical")

### Target API
- [x] Target SDK 35 (Android 15) — Pflicht seit 31.08.2025 ✅
- [x] compileSdk 35 ✅
- [x] minSdk 26 ✅

### Privacy Policy URL
- [x] Play Store Listing: URL zu PRIVACY.en.html Public Hosting
- [x] In-App: Settings → Datenschutz → WebView
- [x] Consent-Screen pre-onboarding: Link vorhanden

### Subscription Policy
- [x] Free Trial klar kommuniziert (8 Tage)
- [x] Preise vor Kauf sichtbar (Google Play Billing handhabt)
- [x] Kündigung via Play Store Abos

### AI-Generated Content Policy
- [x] Report-Mechanismus für KI-Antworten (mailto-Intent in Dashboard)
- [x] AI-Safeguards: Gemini Content-Filter aktiv
- [x] KI-Hinweis in DSE (§ 12a)

---

## Empfohlene Rollout-Strategie (phased)

1. **Woche 1:** Nur DACH (DE, AT, CH) — deutsche Nutzer, deutsche Rechtstexte, niedriges Risiko
2. **Woche 2–3:** EU-weit — englische Rechtstexte werden bei Locale-Switch automatisch geladen
3. **Woche 4+:** USA, UK, CA (exkl. Quebec), AU, NZ — erfordert PRIVACY.en mit CCPA/UK ICO Sections
4. **Nach Monitoring:** Rest der Welt (außer ausgeschlossene Länder)

---

## Quebec (CA) — offene Entscheidung

**Rechtslage:** Loi 25/Loi 96 in Kraft seit 06/2025. Für Quebec gilt:
- App-UI und Rechtstexte MÜSSEN auf Französisch verfügbar sein
- Bei KI-Features: explizite Consent-Anzeige auf Französisch
- Data Protection Officer (DPO) bei >100.000 Nutzern Pflicht

**Aktueller Stand BestJournalAndroid:**
- ✅ App-UI Französisch: `values-fr/strings.xml` vorhanden
- ❌ Keine `values-fr-rCA` für quebec-spezifische Terminologie
- ❌ Keine französischen Rechtstexte (PRIVACY.fr, TERMS.fr, IMPRINT.fr)
- ❌ Kein DPO bestimmt

**Zwei Optionen:**

**Option A — Ganz Kanada ausschließen:**
- In Play Console: CA komplett deaktivieren
- Vorteil: Null Risiko Loi 25/96
- Nachteil: Auch englischsprachige Kanadier (Ontario, BC, Alberta) werden ausgeschlossen (~27 Mio. potenzielle Nutzer)
- Empfehlung wenn: Schnellster sauberer Launch Priorität hat

**Option B — Kanada freigeben, Quebec-Restrisiko akzeptieren:**
- In Play Console: CA aktiviert lassen
- Französisch UI ist da, aber Rechtstexte nur auf Deutsch+Englisch
- Realistisches Risiko: CAI (Commission d'accès à l'information) könnte bei Beschwerde eines quebec-französischen Nutzers ein Bußgeld bis 25 Mio. CAD oder 4% des weltweiten Umsatzes verhängen
- Wahrscheinlichkeit Bußgeld für Indie-App ohne gezielte Quebec-Werbung: Niedrig
- Empfehlung wenn: Englischsprachige Kanadier erreicht werden sollen und Restrisiko akzeptabel

**Meine Empfehlung:** Option A für Release 0.5.1, bis französische Rechtstexte vorhanden sind.
Wenn später ROI rechtfertigt, französische PRIVACY.fr/TERMS.fr/IMPRINT.fr erstellen und auf Option B wechseln.

**Entscheidung Benutzer (2026-04-21):** ☑ **Option A — Kanada komplett ausschließen**
⬜ Option B

### Geplantes späteres Kanada-Update (NICHT in 0.12.30)

Nach dem initialen Release soll Kanada in einer späteren dedizierten Session rechtssicher freigegeben werden.
**Für diese spätere Session ist geplant:**

1. `values-fr-rCA/strings.xml` erstellen (Quebec-spezifische Terminologie, z.B. „courriel" statt „e-mail")
2. Französische Rechtstexte erstellen:
   - `app/src/main/assets/legal/fr/PRIVACY.md` (inkl. Loi 25/96-spezifischer Abschnitte)
   - `app/src/main/assets/legal/fr/TERMS.md`
   - `app/src/main/assets/legal/fr/IMPRINT.md`
3. `LegalDocumentScreen` für französische Locale verdrahten
4. DPO (Data Protection Officer) bestimmen und in PRIVACY.fr kontaktbar machen
5. Consent-Screen auf Französisch testen
6. CAI-Meldepflichten bei Datenschutzverletzungen dokumentieren
7. Play Console: CA aktivieren, Quebec-Nutzern französische Version ausliefern

**Trigger für diese Session:** Benutzer sagt „wir machen jetzt das Kanada-Update" oder ähnlich.
**Tomorrow-Continue-Memory:** `project_quebec_canada_future_update.md`
