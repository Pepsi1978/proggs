# Privacy Policy for Best Journal

**Version:** 20 April 2026
**App:** Best Journal (Android)
**Developer:** Frank Barwandt

---

## 1. Data Controller under the GDPR

The controller responsible for personal data processing in the Best Journal app is:

**Frank Barwandt**
c/o Impressumservice Dein-Impressum
Stettiner Straße 41
35410 Hungen
Germany

E-Mail: dev.app.support@gmail.com

A Data Protection Officer is not legally required (Art. 37 GDPR, § 38 BDSG).

---

## 2. Overview: What data does the app process?

Best Journal is a diary app. The default state is **local on your device**. Journal
entries are stored in a protected SQLite database and only leave your device if you
actively use one of the following optional features:

- Cloud transcription (Groq) for voice recordings
- Cloud backup (Google Drive)
- AI features (Firebase AI / Google Gemini)
- Text-to-speech (Microsoft Edge TTS)
- Sign-in (Google Sign-In / Firebase Authentication)
- Usage analytics (Firebase Analytics, opt-in only)

Data categories processed by the app:

| Data category | Stored where | Purpose | Optional? |
|---------------|--------------|---------|-----------|
| Journal entries (text, audio, images) | Locally on your device | Core function | Core |
| Settings and preferences | Locally on your device | App configuration | Core |
| Voice recordings (cloud transcription) | Groq, Inc. (USA) | Speech to text | **Optional** |
| Journal data (backup) | Google Drive (app data folder) | Restore, device migration | **Optional** |
| Text snippets (read aloud) | Microsoft Bing Speech (USA) | Text to speech | **Optional** |
| AI requests (manual and automatic) | Firebase AI / Google Gemini (USA) | Dashboard, weekly/monthly/yearly reviews | **Optional** |
| Email address, sign-in ID | Google / Firebase Authentication | Account | **Optional** |
| Device info, IP address, advertising ID | Firebase Analytics | Usage statistics | **Opt-in** |
| Purchase data | Google Play Billing | In-app purchases | Only on purchase |

---

## 3. App permissions and their use

The app requests the following Android permissions. Each permission is used **only for
the stated function** and can be revoked at any time in Android system settings.

### 3.1 Internet (`INTERNET`) and network state (`ACCESS_NETWORK_STATE`)
**Purpose:** Cloud transcription (Groq), cloud backup (Google Drive), AI features,
in-app purchases, Firebase services.
**Note:** The app works without internet, only cloud and online AI features are
disabled. Local speech recognition (see 5.2) works offline as well.

### 3.2 Microphone (`RECORD_AUDIO`)
**Purpose:** Voice recordings for journal entries (dictation, voice notes).
**Processing:** Recordings are stored **locally** on your device by default. For
transcription, you can choose between **local** recognition (see 5.2) and **cloud
transcription** (see 5.1).
**Legal basis:** Consent (Art. 6(1)(a) GDPR).

### 3.3 Camera (`CAMERA`)
**Purpose:** Photo and video attachments for journal entries.
**Processing:** Photos and videos are stored locally only (app-private folder
`filesDir/photos/`) and are not uploaded automatically. With Google Drive backup
active, they are transferred encrypted to the Drive app data folder as part of the
backup.

### 3.4 Approximate location (`ACCESS_COARSE_LOCATION`)
**Purpose:** Exclusively for the optional design feature **"Sunrise/Sunset"** in
settings (dark mode follows the local sun position).
**Processing:** Location is determined **once** when the toggle is enabled, stored
locally in app settings, and **never** transmitted to external services. Sunrise and
sunset times are calculated entirely on your device.

### 3.5 Notifications (`POST_NOTIFICATIONS`)
**Purpose:** Reminders to write entries (if you enable reminders).
**Processing:** Generated locally on your device only.

### 3.6 Autostart after reboot (`RECEIVE_BOOT_COMPLETED`)
**Purpose:** Re-activation of scheduled reminders after a device restart.
**Processing:** Locally only, no data transmission.

---

## 3a. Scope and international availability

The app is available in **27 languages** (English, German, French, Spanish,
Italian, Dutch, Polish, Portuguese [Brazil and Portugal], Ukrainian, Turkish, Arabic,
Hindi, Bengali, Gujarati, Kannada, Malayalam, Marathi, Tamil, Telugu, Urdu, Indonesian,
Thai, Japanese, Korean, Chinese [Simplified and Traditional]) in the Google Play Store. The
language is chosen automatically based on your Android system language
(`Locale.getDefault()`), and the timezone from Android settings (`TimeZone.getDefault()`).
Both happen **on the device only**, **no additional location, language, or timezone
query is made to any server**.

Additional languages and countries are added continuously.

This Privacy Policy applies regardless of your country of residence. For users in
the **European Union** and the **EEA**, the GDPR applies. For users outside, the
protection standards stated here apply voluntarily as a self-commitment by the
controller. Users in specific jurisdictions have additional rights (see Section 8a).

---

## 4. Local data storage

All journal content (texts, voice recordings, images, moods, tags) is stored in a
local SQLite database (Android Room) exclusively on your device.

- **Storage location:** Internal app storage (protected by the operating system)
- **Access:** Best Journal app only
- **Deletion:** Uninstall the app or use "Settings → Delete all data"

### 4.1 PDF export (local)

The app offers a **PDF export** for journal entries including embedded photos.
Conversion happens **entirely on your device**. The PDF file is stored in the local
app or Downloads folder. The export itself does **not** transmit data to any third
party.

If you later share the PDF through the Android share menu with another app or a
cloud service (for example email, WhatsApp, Google Drive), that is your own choice.
The receiving service then processes the data under its own privacy policy.

---

## 5. Optional cloud services and third parties

The following services are used **only if you actively use or enable them**. The app
is fully usable without these services.

### 5.1 Groq, Inc. | Cloud transcription (optional)

**Provider:** Groq, Inc., 400 Castro Street, Mountain View, CA 94041, USA
**Purpose:** Converting your voice recordings into text (Whisper transcription).
**Data collected:** The audio file you upload for transcription. Metadata: file size,
format, language, IP address.
**Transfer:** Only when you enable **"Cloud transcription"** in settings or
explicitly choose the cloud option for a recording. Default is **local on-device
transcription** (see 5.2).
**Legal basis:** Consent (Art. 6(1)(a) GDPR).
**International transfer:** Processing takes place in the USA. Groq is self-certified
under the **EU-US Data Privacy Framework**. Additionally, Standard Contractual Clauses
(Art. 46 GDPR) apply.
**Retention at Groq:** Requests are not used for training purposes and are deleted
after processing, per the provider's policy.
**Withdrawal:** Switch to **"Local transcription"** in app settings. No audio data is
sent to Groq after that.
**Groq Privacy Policy:** https://groq.com/privacy-policy/

> **Important note:** Voice recordings may contain particularly sensitive personal
> data (Art. 9 GDPR). Use cloud transcription only if you agree to the transfer of
> your recording to Groq in the USA. Local transcription is the more privacy-friendly
> alternative.

### 5.2 Local speech recognition (on-device)

The app includes **local offline speech recognition** based on the open-source
project **sherpa-onnx**. Transcription happens entirely on your device.
**Data transmission:** None. No server is involved.
**Prerequisite:** One-time download of the language model (~100 MB).

### 5.2a Microsoft Edge Text-to-Speech (optional)

**Provider:** Microsoft Corporation, One Microsoft Way, Redmond, WA 98052, USA
**Service:** Bing Speech Service (endpoint: `speech.platform.bing.com`)
**Purpose:** Converting text to natural speech output ("read aloud"), for example
to listen to your entries or retrospectives.
**Transfer flow:**
1. You trigger the read-aloud function (for example on the dashboard or in an entry).
2. The text is transmitted over an encrypted WebSocket connection to Microsoft servers
   in the **USA**.
3. The generated audio is returned and played back locally on your device.
**Data collected:** The text sent to the service, selected voice, technical metadata
(IP address, timestamp).
**Transfer:** Only when you actively use the read-aloud function. No transfer to
Microsoft without your action.
**Legal basis:** Consent (Art. 6(1)(a) GDPR) by actively triggering the feature.
**International transfer:** USA, based on the **EU-US Data Privacy Framework**
(Microsoft is certified) and on **EU Standard Contractual Clauses** (Art. 46 GDPR).
**Withdrawal:** Simply do not use the read-aloud function, or disable it in settings.
Android also offers a native on-device TTS as an alternative.
**Microsoft Privacy Policy:** https://privacy.microsoft.com/en-us/privacystatement

> **Note:** Do not send particularly sensitive personal data of third parties through
> the read-aloud function.

### 5.3 Google Drive | Cloud backup (optional)

**Provider:** Google Ireland Limited, Gordon House, Barrow Street, Dublin 4, Ireland
**Purpose:** Encrypted backup of your journal data for restoration on a new device or
after uninstallation.
**Data collected:** Complete backup of your journal database (entries, audio, images,
settings) as a single file.
**Storage location:** **App data folder in your personal Google Drive account** (scope:
`DRIVE_APPDATA`). This folder is protected by Google and accessible only to Best Journal.
Other apps and even you via the normal Drive UI cannot access it.
**Activation:** Only when you enable **"Google Drive Backup"** in settings and
explicitly grant access.
**Legal basis:** Consent (Art. 6(1)(a) GDPR).
**Withdrawal:** Disable the backup in app settings. Additionally, you can revoke the
connection entirely at **Google Account → Apps with access to your account**.
**Deleting the backup:** Via **"Settings → Backup → Delete cloud backup"** in the app.

#### 5.3a Android system backup (automatic)

In addition to the app-internal Drive backup, the app supports the **Android system
backup** (`allowBackup="true"` in the manifest). If you have automatic backup enabled
under **Android Settings → Google → Backup**, the main journal database
(`entropy_journal_db`) and normal app settings are automatically backed up to your
Google Drive storage (encrypted, max. 25 MB).

**What is excluded:** The backup rules (`backup_rules.xml` / `data_extraction_rules.xml`)
exclude the dashboard database, the retrospective database, and encrypted credentials
(EncryptedSharedPreferences with Google tokens) from the Android system backup.
These sensitive items never end up in the system backup.

**What is included:** Journal texts, mood tags, and normal app settings. Photos and
videos are usually not captured by the Android system backup due to the 25 MB size
limit. For media, you need the separate Google Drive backup from the app (5.3).

**Deactivation:** In Android system settings under "Google → Backup".
**Legal basis:** Consent through Google account settings (Art. 6(1)(a) GDPR).

### 5.4 Google Sign-In (optional)

**Provider:** Google Ireland Limited
**Purpose:** Convenient sign-in with your Google account (via Android Credential Manager).
**Data collected:** Email address, Google account ID, public name, profile picture URL.
**Legal basis:** Contract performance (Art. 6(1)(b) GDPR) if you sign in.
**Note:** Sign-in is **not required** to use the app.

### 5.5 Firebase Authentication (optional)

**Provider:** Google Ireland Limited
**Purpose:** User account management (links Google Sign-In with the app).
**Data collected:** Email address, sign-in ID, IP address, timestamp.
**Legal basis:** Contract performance (Art. 6(1)(b) GDPR).

### 5.6 Firebase AI / Google Gemini | Manual and automatic AI processing

**Provider:** Google Ireland Limited / Google LLC
**AI model:** Google Gemini (Firebase AI Logic SDK)
**Server location:** USA

The app uses Google Gemini for both **manually triggered** and **automatically
triggered** AI features. Both lead to the transmission of text snippets to Google
servers in the USA.

#### 5.6.1 Manually triggered AI features

When you actively start an AI function (for example "Improve text", "Summarize",
"Follow-up question"):
1. The relevant text snippet is transmitted to Google Gemini in the **USA**.
2. The result is returned to your device and shown to you for review.

#### 5.6.2 Automatically triggered AI features

The app generates **automatic** AI content when certain events happen. Relevant
journal data is then **automatically** sent to Google Gemini in the USA:

| Trigger | What happens |
|---------|--------------|
| **New journal entry** | Dashboard refresh, relevant entries are automatically sent to Gemini to generate a new dashboard summary (only if enabled in "Settings → AI automation → Auto dashboard update") |
| **New voice transcription** (if auto text improvement is on) | The transcribed text is automatically polished stylistically and grammatically by Gemini |
| **Profile switch in dashboard** | Dashboard is regenerated with the new profile prompt |

**Not automatic, only on your action:**
- **Weekly, monthly, and yearly reviews** are *not* generated automatically in the
  background. At the end of each period you get a local notification. The review is
  generated only when you open the app and go to the review view. Without your action,
  no transfer happens.

**Data collected:** Text snippets of your journal entries (never photos, never voice
recordings), time range, model parameters, technical metadata (IP address, timestamp).

**Disabling automatic AI features:**
Under **"Settings → AI automation"** you can individually disable:
- Automatic dashboard refresh (default: on)
- Automatic text improvement after transcription (default: off)

Under **"Settings → Reminders"** you control whether you receive notifications for
weekly/monthly/yearly reviews (default: on). Independent of that, the review is only
generated when you open it.

After disabling, **no transfer to Google Gemini happens** for that function. The app
remains fully usable. You just lose the AI-generated summaries and reviews.

**Legal basis:** Consent (Art. 6(1)(a) GDPR). Consent is obtained on first app launch
with a clearly visible notice and covers both manual and automatic AI processing. It
can be withdrawn at any time in settings (Art. 7(3) GDPR).

**International transfer:** Processing takes place in the USA based on the **EU-US
Data Privacy Framework** (adequacy decision by the European Commission) and on **EU
Standard Contractual Clauses** (Art. 46 GDPR).

**Retention at Google:** According to Firebase AI policies, requests are not used for
training and are deleted after processing. Details:
https://firebase.google.com/support/privacy

> **Important:** Do not send particularly sensitive personal data of third parties
> (for example health data of others, names of third parties without their consent)
> through manual AI features. For automatic reviews as well: do not write content in
> your journal that you do not want transferred to Google Gemini, or disable automatic
> AI features.

### 5.7 Firebase Analytics (opt-in on first launch)

**Provider:** Google Ireland Limited / Google LLC
**Purpose:** Anonymous usage statistics for error analysis and product improvement.
**Data collected:** Device type, OS version, app version, usage frequency, approximate
region (country), Firebase Instance ID, **IP address (truncated)**, **Android
advertising ID (AAID)**, event data (for example entry created, dashboard opened,
premium purchase, TTS error).

**Legal basis:** Consent (Art. 6(1)(a) GDPR, § 25(1) TDDDG).

**How consent works:**
- On **first app launch**, a privacy screen appears before onboarding.
- If you choose "Agree and get started", you consent to anonymous usage statistics.
- If you choose "Disable statistics", Firebase Analytics stays off, technically via
  `setAnalyticsCollectionEnabled(false)`.
- You can change your decision at any time under **"Settings → Privacy → Anonymous
  statistics"**.

**Reset advertising ID:** In Android system settings under **"Settings → Privacy →
Ads"**.

### 5.8 Firebase App Check (Play Integrity)

**Purpose:** Protection against abuse and automated requests.
**Data collected:** Google Play device integrity token, app signature.
**Legal basis:** Legitimate interest in abuse protection (Art. 6(1)(f) GDPR).
**Balancing:** The legitimate interest in protecting Firebase endpoints from automated
attacks outweighs the minimal impact on the user, as the integrity token is
device-independent and allows no personal identification. No privacy-friendlier
alternative is technically available.
**International transfer:** Processing in the USA based on the EU-US Data Privacy
Framework and Standard Contractual Clauses (Art. 46 GDPR).

### 5.9 Firebase Remote Config

**Purpose:** Remote configuration (for example feature flags, texts).
**Data collected:** Anonymized app instance ID, app version.
**Legal basis:** Legitimate interest in proper operation (Art. 6(1)(f) GDPR).
**Balancing:** The legitimate interest in smooth app configuration prevails, as only
anonymized instance IDs are transferred and no tracing to individuals is possible.
**International transfer:** Processing in the USA based on the EU-US Data Privacy
Framework and Standard Contractual Clauses (Art. 46 GDPR).

### 5.9a Feedback feature (Gmail API, optional)

Under **"Settings → Send feedback"**, you can send us feedback about the app.

**Technical flow:**
1. You enter your feedback in the app and confirm sending.
2. The app asks once for the Google permission **"Send email"** (Gmail API scope
   `https://www.googleapis.com/auth/gmail.send`).
3. After your consent, **two emails are sent through your own Google account** via
   the Gmail API:
   - **Email 1 to us:** `dev.app.support@gmail.com`, contains your feedback text and
     your Google account email address as sender.
   - **Email 2 to yourself:** Confirmation with the feedback text you submitted, so
     you have a copy.

**Who processes what:**
- **Google** (Gmail infrastructure): transports both emails through its mail servers.
- **Us** (Frank Barwandt, contact details in Section 1): receive the first email in
  our `dev.app.support@gmail.com` inbox and store it to process your feedback.

**Data collected/transmitted:**
- Your Google account email address (as sender of both emails)
- The feedback text you entered
- Timestamp of sending

**Legal basis:**
- For transmission to us: Consent (Art. 6(1)(a) GDPR) by actively sending.
- For processing your request: Legitimate interest in improving the app (Art. 6(1)(f)
  GDPR).

**Retention at our side:** Your feedback email stays in our inbox for a maximum of
24 months, then it is deleted. If directly related to a bug fix or feature, retention
can be longer. You can request deletion by email at any time.

**Revoking Gmail permission:** You can revoke Gmail Send access at any time at
**https://myaccount.google.com/permissions** → remove Best Journal. After removal,
the feedback feature no longer works. Otherwise the app is fully usable.

**Alternative:** You can also reach us directly by email at **dev.app.support@gmail.com**,
without any app permissions.

### 5.10 Google Play In-App Review API

**Purpose:** Showing the rating dialog in the Google Play Store.
**Data collected:** Technical metadata (app version, package name) to show the dialog.
The rating itself is not collected by the app.
**Legal basis:** Legitimate interest (Art. 6(1)(f) GDPR).
**Balancing:** The legitimate interest in user feedback prevails, as only technical
metadata without personal reference is processed and the rating itself takes place
exclusively in your Google Play account.
**International transfer:** Processing in the USA based on the EU-US Data Privacy
Framework and Standard Contractual Clauses (Art. 46 GDPR).

**More information on Google services:**
- Google Privacy Policy: https://policies.google.com/privacy
- Firebase Privacy: https://firebase.google.com/support/privacy
- Google Drive: https://policies.google.com/privacy#infocollect

### 5.11 International transfer (USA)

When cloud services are active (Groq, Firebase/Gemini, Google Drive, Microsoft
Edge TTS), data is processed in the USA. The transfer is based on:

- **EU-US Data Privacy Framework** (adequacy decision of the European Commission of
  10 July 2023), for Google/Firebase/Gemini and Microsoft
- **EU Standard Contractual Clauses** (Art. 46 GDPR), for Groq

---

## 6. In-app purchases (Google Play Billing)

For optional Premium features, the app uses **Google Play Billing** for payment
processing.

- **Provider:** Google Ireland Limited
- **Data collected:** Transaction ID, purchase token, purchased product, timestamp
- **Payment data:** Processed **exclusively by Google**. We do not receive credit
  card numbers, PayPal credentials, or bank details.
- **Legal basis:** Contract performance (Art. 6(1)(b) GDPR)
- **Google Play Privacy:** https://play.google.com/about/play-terms/

---

## 7. Account deletion and data deletion

Under Art. 17 GDPR and Google Play policies, you have the right to have your data
deleted at any time.

### 7.1 Delete account and cloud data (in the app)
**Settings → Account → Delete account**

The following is permanently removed:
- Local journal databases (main DB, dashboard DB, review DB)
- Local photos and videos of entries
- Encrypted app settings and stored Google tokens
- Firebase Authentication account (`FirebaseAuth.currentUser.delete()`)
- Google Drive app data backup (removed from Drive)

After completion, the app restarts and behaves like a fresh installation. The
Android system backup is **not** deleted by this action. You can remove it
separately in your Google account at
**myaccount.google.com → Data & privacy → Backups**.

### 7.2 Delete local journal data
**Settings → Data → Delete all data** or **uninstall the app**

### 7.3 Delete Drive backup manually
If you want the backup to persist even after uninstalling the app, you can delete
it separately under **"Settings → Backup → Delete cloud backup"**. After uninstalling
the app, you can remove the app data folder from your Google account at
**myaccount.google.com → Data & privacy → Third-party apps with account access**.

### 7.4 Request deletion by email
If you can no longer access the app: **dev.app.support@gmail.com**, subject:
"Best Journal account deletion". Response time: 30 days.

A public deletion request page is also available at
**https://pepsi1978.github.io/bestjournal-deletion/** (alternative to the in-app
deletion flow, required by Google Play).

---

## 8. Your rights under the GDPR

You have the following rights at all times:

| Right | Article | How to exercise |
|-------|---------|-----------------|
| Access | Art. 15 GDPR | Email to dev.app.support@gmail.com |
| Rectification | Art. 16 GDPR | Email or directly in the app |
| Erasure ("right to be forgotten") | Art. 17 GDPR | In the app under "Delete data" or uninstall |
| Restriction of processing | Art. 18 GDPR | Email to dev.app.support@gmail.com |
| Data portability | Art. 20 GDPR | Export function in the app or email |
| Objection | Art. 21 GDPR | Email to dev.app.support@gmail.com |
| Withdrawal of consent | Art. 7(3) GDPR | In app settings under "Privacy" |
| Lodge a complaint with a supervisory authority | Art. 77 GDPR | See below |

### Competent supervisory authorities

**Germany / EU / EEA (lead supervisory authority):**

The Hessian Commissioner for Data Protection and Freedom of Information
Gustav-Stresemann-Ring 1, 65189 Wiesbaden, Germany
Phone: +49 611 1408-0
Email: poststelle@datenschutz.hessen.de
Website: https://datenschutz.hessen.de

(Competent because the controller is based in Hesse.)

EU/EEA users can also contact the supervisory authority of their country of residence.

---

## 8a. International users, additional rights under local law

Best Journal is offered worldwide through the Google Play Store. The rights above
under the GDPR apply to users in all countries. Users from specific jurisdictions
have **additional** or **different** rights under local law, detailed below.

### 8a.1 Rights of California residents (CCPA / CPRA)

If you are a California resident, the California Consumer Privacy Act (CCPA), as
amended by the California Privacy Rights Act (CPRA), grants you the following rights:

- **Right to know:** What personal information we collected, used, and shared in the
  past 12 months, along with categories of sources and third parties. See Section 2
  for the complete list of data categories.
- **Right to delete:** Deletion of your personal information (see Section 7).
- **Right to correct:** Correction of inaccurate personal information.
- **Right to opt out of sale or sharing:** We do **not sell or share** your personal
  information in the meaning of the CCPA. No cross-context behavioral advertising
  takes place. Because there is no sale or sharing, there is no "Do Not Sell or
  Share My Personal Information" link needed, but you may still opt out of any
  analytics data processing under Section 5.7.
- **Right to limit the use of sensitive personal information (SPI):** Journal content
  may contain SPI (for example, health information, religious beliefs, sexual
  orientation). You can limit our use of such information by disabling cloud AI
  features (see Section 5.6) and cloud transcription (see Section 5.1). All journal
  content remains local on your device by default.
- **Right to non-discrimination:** We do not discriminate against you for exercising
  your rights.
- **Global Privacy Control (GPC):** We honor GPC browser signals as a valid opt-out
  request for any analytics processing.

**Exercise your rights:** Email dev.app.support@gmail.com with subject "CCPA Request".
Alternatively, delete your account directly in the app (Settings → Account → Delete
account). We respond within 45 days and may extend by 45 days with notice (Cal. Civ.
Code § 1798.130).

**Do Not Sell My Personal Information:** We do not sell personal information.
Disabling analytics (see Section 5.7) is available as an opt-out for data processing.

**Categories of personal information disclosed for a business purpose** (CCPA § 1798.140):
- Identifiers (email, Firebase Instance ID, advertising ID) — to Google, Groq,
  Microsoft as required for core services.
- Commercial information (purchase history) — to Google for Play Billing.
- Internet or electronic network activity (app usage events) — to Google for Firebase
  Analytics (opt-in only).
- Audio recordings — to Groq for transcription (opt-in only).
- Inferences (AI-generated text summaries) — processed by Google Gemini for dashboard
  and reviews (opt-in only).

**Authorized agent:** You may designate an authorized agent to submit requests on
your behalf. Provide written authorization signed by you.

### 8a.2 Rights of other US state residents

If you are a resident of one of the following states, you have comparable rights to
California residents under state data protection laws:

| State | Law | Effective |
|-------|-----|-----------|
| Virginia | VCDPA | 2023 |
| Colorado | CPA | 2023 |
| Connecticut | CTDPA | 2023 |
| Utah | UCPA | 2023 |
| Oregon | OCPA | 2024 |
| Montana | MCDPA | 2024 |
| Texas | TDPSA | 2024 (no minimum threshold) |
| Iowa | ICDPA | 2025 |
| Delaware | DPDPA | 2025 |
| New Jersey | NJDPA | 2025 |
| New Hampshire | NHPPA | 2025 |
| Rhode Island | RIDTPPA | 2026 |
| Indiana | ICDPA | 2026 |
| Kentucky | KCDPA | 2026 |
| Minnesota | MCDPA | In force |
| Tennessee | TIPA | In force |
| Nebraska | NDPA | In force |
| Maryland | MODPA | In force |

Your rights include access, deletion, correction, data portability, opt-out of
targeted advertising, and opt-out of profiling. For Texas residents, we honor GPC
signals as no minimum threshold applies to us. For Delaware and Rhode Island
residents, on request we can provide the names (not just categories) of third-party
recipients. To exercise, email dev.app.support@gmail.com.

### 8a.3 Rights of Illinois residents (BIPA)

The Illinois Biometric Information Privacy Act (BIPA) protects biometric identifiers
including voiceprints. We want to be transparent:

- **Voice recordings** sent for cloud transcription (Section 5.1) are processed by
  Groq using the Whisper model for speech-to-text only. To our knowledge, Groq
  Whisper does **not** create a voiceprint (a biometric template used to identify
  an individual from their voice). Only transcription (speech to text) is performed.
  If this changes, we will update this notice and obtain written consent before any
  voiceprint processing.
- **Device biometric app lock** (fingerprint, face unlock) is handled entirely by
  the Android operating system in the device's secure hardware enclave. We never
  receive biometric data.
- **Retention policy:** Voice recordings transferred to Groq are not retained after
  transcription (per Groq's policy). Local voice recordings on your device are
  retained only until you delete them, or up to 3 years at most as required under
  BIPA.

If you are an Illinois resident and want to opt out of cloud transcription entirely,
switch to "Local transcription" in settings (Section 5.1).

### 8a.5 Rights of Canadian residents (PIPEDA)

Under the Personal Information Protection and Electronic Documents Act (PIPEDA),
Canadian users have the right of access and correction. To exercise, email
dev.app.support@gmail.com.

You may also file a complaint with the Office of the Privacy Commissioner of Canada:
https://www.priv.gc.ca

### 8a.6 Rights of Quebec residents (Law 25)

If you are a Quebec resident, under the Act respecting the protection of personal
information in the private sector (Law 25, CQLR c. P-39.1), you have these rights:

- **Right of access** to your personal information
- **Right of rectification**
- **Right to deletion and de-indexation** of online-available personal information
- **Right to data portability**
- **Right to be informed about automated decisions** that could significantly affect
  you (we do not make such decisions, see Section 12)

For transfers outside Quebec (to the USA), we have conducted a Privacy Impact
Assessment and rely on contractual protections. You may file a complaint with the
Commission d'accès à l'information du Québec (CAI): https://www.cai.gouv.qc.ca

### 8a.7 Rights of Australian residents (Privacy Act 1988, Australian Privacy Principles)

Under the Australian Privacy Act 1988 and the Australian Privacy Principles (APPs),
Australian users have these rights:

- **APP 12: Access to personal information**
- **APP 13: Correction of personal information**
- **APP 8 (Overseas Disclosure):** Your data is transferred to the USA (Groq,
  Google/Gemini, Microsoft) and Ireland (Google). We take reasonable steps to ensure
  comparable protection through contractual safeguards. By using the cloud features,
  you consent to the overseas disclosure on the basis of this information.

Note: The Privacy Act does not provide a general right of deletion equivalent to the
GDPR. However, we voluntarily honor deletion requests (see Section 7).

Lodge a complaint with the Office of the Australian Information Commissioner (OAIC):
https://www.oaic.gov.au

### 8a.8 Rights of New Zealand residents (Privacy Act 2020)

Under the New Zealand Privacy Act 2020, you have rights of access (IPP 6) and
correction (IPP 7). You can lodge a complaint with the Privacy Commissioner:
https://www.privacy.org.nz

Following the Privacy Amendment Act 2025 (IPP 3A, effective 1 May 2026), we inform
you that AI services (Google Gemini, Groq Whisper, Microsoft Edge TTS) may process
your inputs and return results, which can constitute indirect collection. This
notice serves as the required IPP 3A disclosure.

### 8a.9 Rights of Brazilian residents (LGPD)

Under the Lei Geral de Proteção de Dados (LGPD, Law No. 13.709/2018), Brazilian
users have rights of access, rectification, anonymization, blocking or deletion,
data portability, and objection. These are handled equivalently to GDPR rights.

Supervisory authority: Autoridade Nacional de Proteção de Dados (ANPD):
https://www.gov.br/anpd/pt-br

### 8a.10 Rights of Japanese residents (APPI)

Under the Act on the Protection of Personal Information (APPI), Japanese users have
the right to disclosure, correction, suspension of use, and deletion. For
cross-border transfers to the USA, we inform you that the USA is not on the PPC
whitelist of jurisdictions with equivalent protection; transfers are based on your
consent and on contractual safeguards with the recipient. You can contact the
Personal Information Protection Commission (PPC): https://www.ppc.go.jp

### 8a.11 Rights of South African residents (POPIA)

Under the Protection of Personal Information Act (POPIA), South African users have
rights of access, correction, and objection. Regulator: Information Regulator,
https://inforegulator.org.za

### 8a.12 Other jurisdictions

For users from jurisdictions not specifically named, the GDPR-compliant rights above
apply as a voluntary self-commitment, as well as any mandatory rights under local
law. For questions or to exercise local rights, email dev.app.support@gmail.com.

---

## 9. Data security

- Local database in protected app storage (Android sandboxing)
- Encrypted transmission (HTTPS/TLS 1.2+) on all network connections
- Google Drive backup only in the protected app data folder (no access by other apps)
- Firebase App Check for abuse protection
- Local encryption of sensitive data (AndroidX Security Crypto)
- No storage of unencrypted credentials
- Automatic security updates via the Google Play Store

### 9.1 Biometric app lock (optional)

You can optionally protect the app with **biometric authentication** (fingerprint,
face unlock) or with a **device PIN** (AndroidX Biometric Library).

**Important:** Biometric data (for example fingerprint patterns or face features) is
processed **exclusively by the Android operating system in the secured hardware
enclave** of your device (Trusted Execution Environment / Secure Element). It never
leaves your device and is not accessible to the app. The app only receives a result
of "authentication successful" or "authentication failed", never the biometric
feature itself.

**Legal basis:** Consent by enabling the lock (Art. 6(1)(a) GDPR).
**Withdrawal:** Disable the lock in app settings, or remove your biometrics in Android
system settings.

---

## 10. Children and minors

The app is intended for users **aged 13 and older**. For minors under 16, consent by
a legal guardian is required (Art. 8 GDPR) if sign-in is used or cloud services
(Groq, Drive, AI) are activated. The app does not knowingly collect data from
children under 13 (COPPA compliance, 15 U.S.C. §§ 6501-6506).

---

## 11. Retention periods

| Data | Duration |
|------|----------|
| Local journal entries | Until deletion by you or uninstall |
| Google Drive backup | Until deletion by you or revocation of Drive access |
| Android system backup | Per Google policies (typically until deactivation of system backup) |
| Microsoft Edge TTS | Deleted after processing (per Microsoft policy) |
| Groq transcription requests | Deleted after processing (per Groq policy) |
| Firebase Analytics | 14 months (Google default), then automatic deletion |
| Firebase Authentication | Until account deletion |
| AI requests (Firebase AI) | Not stored permanently per Google policies |
| Purchase data | Per statutory retention periods (up to 10 years, § 147 AO German tax law) |
| Feedback emails | Max. 24 months in our inbox, then deletion |
| Server logs (IP addresses) | Max. 30 days, then automatic deletion |

---

## 12. No automated decision-making

There is **no automated decision-making** in the meaning of Art. 22 GDPR that has
legal effect on you or significantly affects you. AI features only generate texts or
summaries that are not legally binding for you.

---

## 12a. Notice on AI systems (Art. 50 AI Act)

This app uses AI systems to process your input:

- **Google Gemini** (Firebase AI): AI-powered text analysis, summaries, dashboard,
  reviews and stylistic text improvement
- **Groq Whisper**: Speech-to-text transcription
- **Microsoft Edge Text-to-Speech**: Text-to-speech read-aloud

Pursuant to Art. 50 of Regulation (EU) 2024/1689 (AI Act), we inform you that the
features listed above are based on artificial intelligence. AI systems can produce
**inaccurate, incomplete, or misleading outputs** ("hallucinations"). Verify
important statements independently and consult qualified professionals for health,
legal, or financial concerns.

AI outputs are **not binding statements**. They are suggestions and summaries to
support your own reflection.

---

## 13. Obligation to provide personal data

You are not obliged to provide us with personal data. The app is fully usable without
sign-in, without cloud services, and without analytics. Without the optional cloud
features, you only miss the corresponding convenience functions (cloud transcription,
cloud backup, cross-device usage, AI features).

---

## 14. Changes to this Privacy Policy

This Privacy Policy may be updated when the app changes or the legal situation
changes. The current version is always available in the app under
**Settings → Privacy** and in the Google Play Store. You will be informed in the app
of material changes.

**Last updated:** 20 April 2026

---

## 15. Contact

For questions about privacy or to exercise your rights:

**Email:** dev.app.support@gmail.com
**Postal address:** See Section 1 (Data Controller)
