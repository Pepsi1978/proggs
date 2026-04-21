# Google Play — Health Apps Declaration (Checkliste)

**Stand:** 2026-04-21
**Audit-Referenz:** M8 aus `docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v4.md`
**Quelle:** [Play Console Help — Health Content &amp; Services Policy](https://support.google.com/googleplay/android-developer/answer/16679511?hl=en)

---

## Worum geht es

Google Play verlangt seit **August 2025** von Entwicklern eine *Health apps declaration form* im Play Console,
wenn eine App **Gesundheitsdaten verarbeitet** — auch dann, wenn diese Daten nur durch
KI-Analyse **inferiert** werden (z.B. Stimmungs-/Mental-Health-Auswertung aus Journaling-Texten).

BestJournal ist ein **Journaling-App mit KI-Analyse** und faellt damit potenziell unter
die Health-Content-Policy. Die Kategorisierung ist ein Grenzfall (Lifestyle vs.
Mental-Health).

---

## Empfohlene Vorgehensweise beim Play-Console-Upload

### 1. Health-Declaration-Formular suchen

Beim Upload einer neuen Version:
Play Console → App → *App content* → *Health apps declaration form*
(erscheint in der Linkliste, wenn Google die App als gesundheitsbezogen einstuft)

### 2. Wenn das Formular angezeigt wird

Folgende Fragen ausfuellen:

| Frage | Unsere Antwort |
|-------|----------------|
| Sammelt die App Gesundheitsdaten (Health Connect API)? | **Nein** — BestJournal nutzt keine Health Connect API |
| Interpretiert/inferiert die App Gesundheitsdaten? | **Ja** — Gemini-basierte Stimmungs-/Muster-Analyse aus Tagebuchtexten |
| Werden biometrische Daten verarbeitet? | **Nein** — Whisper macht keine Voiceprint-Erstellung (nur STT); biometrische App-Sperre ist hardware-seitig (Secure Element) |
| Teilt die App Gesundheitsdaten mit Dritten fuer Werbung? | **Nein** — Werbung gibt es nicht. Firebase Analytics ist Opt-In und uebertraegt keine Health-Inferenz-Daten |
| Gibt es eine Notruffunktion/Crisis-Intervention? | **Ja** — Settings → Krisenhilfe bietet Telefonseelsorge, Notruf 112, findahelpline.com |
| Data Safety Form mit Gesundheitsdaten-Kategorie? | **Nein** — primaere Kategorie ist "Personal messages/journaling", nicht "Health and fitness" |

### 3. Wenn das Formular NICHT angezeigt wird

Trotzdem dokumentieren: "Formular wurde nicht angefordert am TT.MM.JJJJ"
→ Screenshot der App-Content-Seite als Nachweis aufbewahren.

---

## Data Safety Form — Gesundheitsbezogene Deklaration

Das Data Safety Form hat keine explizite "Health-inferred"-Kategorie.
Empfohlen wird:

| Data Type | Collected? | Shared? | Purpose | Optional? |
|-----------|-----------|---------|---------|-----------|
| Personal messages (user-generated content) | Yes | Yes (Groq, Google Gemini) | App functionality: transcription, AI summaries | Yes, user opts into each |
| Audio: voice/sound recordings | Yes | Yes (Groq, optional) | Transcription only; deleted after processing | Yes, local transcription available |
| Other: AI-generated reflections | Created | No (processed by Google Gemini, not shared) | AI summaries, reviews | Yes |

**Wichtig:** *Personal messages* ist Google-Terminologie fuer frei geschriebene Texte.
Wenn der Nutzer Gesundheitsinfo schreibt, bleibt es "personal messages" und wird
NICHT als "Health and fitness" deklariert, solange die App Gesundheit nicht als
Kernfunktion hat.

---

## Verbotene Verwendungen (Google Play Health Policy)

Wir versichern hiermit dokumentarisch: BestJournal verwendet Gesundheits-relevante
Inferenzen **NICHT** fuer:

- ❌ Versicherungseignung / -Beitraege
- ❌ Beschaeftigungsentscheidungen
- ❌ Kredit- oder Finanzdienstleistungen
- ❌ Profiling fuer personalisierte Werbung
- ❌ Weitergabe an Datenbroker
- ❌ Weitergabe an Arbeitgeber/Schulen/Behoerden

Alle Inferenzen bleiben **auf dem Geraet** (Dashboard/Retrospective-DB lokal) oder
werden nur bei aktiv angefragter KI-Funktion **fluechtig** an Google Gemini uebermittelt
(nicht persistiert laut Firebase-AI-Policy).

---

## Best Practices fuer Mental-Health-Apps (Play Store)

Quelle: Google Play Best Practices fuer Mental Health Apps (informell, 2025)

- [x] Disclaimer "kein Ersatz fuer Therapie" — implementiert in ToS § 4.1 und DSE § 12b
- [x] Crisis-Ressourcen in der App — implementiert in Settings → Krisenhilfe
- [x] In-App KI-Kennzeichnung — implementiert mit "KI-generiert"-Badge (AI Act Art. 50)
- [x] Einwilligung fuer sensitive Daten — PrivacyGateDialog fuer Gemini mit Art.-9-DSGVO-Hinweis
- [x] Data Minimization — Bild-/Audio-Daten werden NICHT an Gemini gesendet (nur Text-Snippets)
- [x] Opt-Out der KI-Funktion — Settings → Datenschutz → KI-Funktion

---

## Naechste Pruefung

Bei jedem Play-Console-Release (oder mindestens quartalsweise) diese Checkliste abarbeiten.
Bei Aenderungen der Google Play Policies: Datei aktualisieren und in Referenz-Datei
`~/proggs/rechtssicherheit.md` verweisen.
