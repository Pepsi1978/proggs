# Design: Token-Kalkulation, Abo-Preise & Kostenkontrolle

**Datum:** 2026-03-31
**Status:** Entwurf
**Projekt:** BestJournalAndroid

---

## 1. Ueberblick

Festlegung der Abo-Preise basierend auf realen Token-Kosten, plus Schutzmechanismen
gegen uebermassige Nutzung (Dashboard-Limit, Spam-Schutz, Entry-Limit).

---

## 2. Gemini 2.5 Flash Preise (Stand Maerz 2026)

| | Preis pro 1M Tokens |
|---|---|
| **Input** | $0.30 |
| **Output** | $2.50 |

Firebase AI Logic (Blaze Plan) hat keine eigenen Aufschlaege — identisch zur Gemini Developer API.
Google Play nimmt 15% Provision auf Abo-Einnahmen (bis $1M/Jahr).

---

## 3. Token-Verbrauch pro KI-Operation

### 3.1 Textverbesserung (ImproveTextUseCase)
- Input: ~500 Tokens (Prompt + User-Text)
- Output: ~200 Tokens
- **Kosten: ~$0.00065 pro Aufruf**

### 3.2 Zusammenfassung (SummarizeEntryUseCase)
- Input: ~400 Tokens (Prompt + User-Text)
- Output: ~80 Tokens
- **Kosten: ~$0.00032 pro Aufruf**

### 3.3 Dashboard-Analyse (AdviceRepository)
- System-Prompt: ~2.400 Tokens (fest)
- Input-Eintraege: variabel (siehe Entry-Limit)
- Output: ~3.000 Tokens (JSON mit Kategorien + Ratschlaegen)

| Eintraege | Input gesamt | Kosten pro Aufruf |
|---|---|---|
| 10 (Free) | ~4.400 | ~$0.006 |
| 30 (Abo/Trial) | ~8.400 | ~$0.010 |

---

## 4. Abo-Preise

| | Preis | Pro Monat | Ersparnis |
|---|---|---|---|
| **Monatsabo** | **€4,99** | €4,99 | — |
| **Jahresabo** | **€39,99** | €3,33 | ~33% gegenueber Monatsabo |

Google Play Produkt-IDs:
- Monatsabo: `bestjournal_ai_monthly`
- Jahresabo: `bestjournal_ai_yearly`

---

## 5. Entry-Limit fuer Dashboard-Analyse

Das Dashboard sendet Tagebucheintraege an die KI. Mehr Eintraege = hoehere Kosten.
40 Eintraege genuegen fuer eine qualitativ hochwertige Analyse.

| Phase | Max. Eintraege fuer Analyse | Aenderung |
|---|---|---|
| Trial (Tag 1-7) | **40** | War: unbegrenzt (Int.MAX_VALUE) — Kostenrisiko! |
| Free (ab Tag 8) | 10 | Keine Aenderung |
| Abo | **40** | War: 30 |

Betroffene Datei: `AiRateLimiter.kt` → `getMaxEntriesForAnalysis()`

---

## 6. Dashboard-Tageslimit (stilles Limit)

Die Dashboard-Aktualisierung ist der groesste Kostentreiber (~95% der Kosten).
Ein gestaffeltes Tageslimit schuetzt vor uebermaessiger Nutzung.

### 6.1 Stufenmodell

| Stufe | Dashboard-Updates | Was passiert |
|---|---|---|
| **Normal** | 1–16 | Alles funktioniert, kein Hinweis |
| **Warnung (intern)** | 17 | E-Mail an dev.app.support@gmail.com mit User-Info |
| **Cooldown** | 17–20 | User sieht: "System ausgelastet — bitte in 30 Minuten erneut versuchen" |
| **Tageslimit** | ab 21 | User sieht: "Neue Aktualisierungen morgen wieder verfuegbar" (feste Meldung, 24h-Sperre) |

### 6.2 Kostenberechnung mit Limit

| Szenario | Updates/Tag | Kosten/Monat |
|---|---|---|
| Normaler Nutzer (3-5/Tag) | 5 | ~$1.50 |
| Aktiver Nutzer (10-15/Tag) | 15 | ~$4.50 |
| Am Limit (16/Tag) | 16 | ~$4.80 |
| Worst Case (20/Tag) | 20 | ~$6.00 |

Worst Case $6.00 ist akzeptabel — wird durch die 24h-Sperre ab 21 verhindert.

### 6.3 Implementierung

Neuer Counter in SharedPreferences:
- `dashboard_daily_count`: Int (Anzahl Updates heute)
- `dashboard_daily_date`: String (heutiges Datum, fuer Reset)
- `dashboard_cooldown_until`: Long (Timestamp bis wann Cooldown laeuft)

Reset: Jeden Tag um Mitternacht (lokale Zeit) auf 0.

### 6.4 E-Mail-Warnung an Entwickler

Bei Erreichen von Stufe 17 wird eine E-Mail gesendet:
- An: dev.app.support@gmail.com
- Betreff: "[BestJournal] Dashboard-Limit Warnung"
- Inhalt: User-ID (anonymisiert/Firebase UID), Anzahl Updates heute, Datum
- Methode: Firebase Cloud Functions oder direkt via Gmail API (bereits in der App)

---

## 7. Spam-Schutz (KI-Aufrufe allgemein)

Unabhaengig vom Dashboard-Limit: Schutz gegen exzessives Spammen aller KI-Funktionen.

### 7.1 Rate-Limit

| Metrik | Schwelle | Aktion |
|---|---|---|
| KI-Aufrufe pro Stunde | > 20 | E-Mail-Warnung an Entwickler |
| KI-Aufrufe pro Stunde | > 30 | 30-Minuten-Pause: "System ausgelastet" |

### 7.2 User-facing Meldungen

- **Cooldown:** "Das System ist gerade ausgelastet. Bitte versuche es in ein paar Minuten erneut."
- **Tageslimit:** "Neue Aktualisierungen sind morgen wieder verfuegbar."
- **NIEMALS:** Worte wie "Spam", "Limit", "Missbrauch" — immer neutral formulieren.

---

## 8. Eintraege: Kein Limit

Textverbesserung und Zusammenfassung kosten ~$0.001 pro Eintrag.
Selbst 50 Eintraege/Tag kosten nur $0.05 — vernachlaessigbar.

**Eintraege bleiben komplett unlimitiert.** Kein Tageslimit, kein Wochenlimit.

---

## 9. Freemium-Phasen (zusammengefasst, mit Aenderungen)

| Phase | Bedingung | KI-Eintraege | Dashboard | Dashboard-Limit |
|---|---|---|---|---|
| **Honeymoon** | Tag 1–3 | Unbegrenzt | Unbegrenzt | 16/Tag (still) |
| **Aufklaerung** | Tag 4–7 | Unbegrenzt | Unbegrenzt | 16/Tag (still) |
| **Freemium** | Ab Tag 8 | 3/Woche | Zaehlt als KI-Nutzung | 16/Tag (still) |
| **Abo** | Abo aktiv | Unbegrenzt | Unbegrenzt | 16/Tag (still) |

Das Dashboard-Tageslimit (16 still / 20 mit Cooldown) gilt fuer ALLE Nutzer —
auch Abo-Nutzer. Es ist ein Kostenschutz, kein Feature-Limit.

---

## 10. Fair-Use-Klausel (AGB/Nutzungsbedingungen)

Im Play Store und in den Nutzungsbedingungen KEINE konkreten Zahlen nennen.
Stattdessen:

**Im Store-Listing:**
- "Umfangreiche KI-Analysen fuer dein Tagebuch"
- "KI-gestuetzte Textverbesserung und Lebensratschlaege"
- NICHT: "30 Analysen pro Tag" oder "unbegrenzte Updates"

**In den Nutzungsbedingungen (AGB):**
- "Die KI-Funktionen unterliegen einer angemessenen Nutzung (Fair Use).
  Der Anbieter behaelt sich vor, Nutzungslimits anzupassen, um die
  Servicequalitaet fuer alle Nutzer sicherzustellen."
- "Der Anbieter kann den Funktionsumfang mit angemessener Vorankuendigung aendern."
- "Bei uebermassiger oder missbaeuchlicher Nutzung kann der Zugang zu
  KI-Funktionen voruebergehend eingeschraenkt werden."

**Vorteil:** Du kannst Limits jederzeit anpassen (z.B. von 16 auf 10/Tag),
ohne dass Nutzer sich auf einen "Vertrag" berufen koennen.

---

## 11. Zusammenfassung der Aenderungen am Code

| Datei | Aenderung |
|---|---|
| `AiRateLimiter.kt` | Trial-Phase Entry-Limit: Int.MAX_VALUE → 40, Abo: 30 → 40 |
| `AiUsageTracker.kt` | Neuer Dashboard-Tages-Counter (16/20/21-Stufen) |
| `AiUsageTracker.kt` | Neuer Spam-Counter (KI-Aufrufe pro Stunde) |
| `BillingManager.kt` | Abo-Preise in Google Play Console: €4,99/Monat, €39,99/Jahr |
| `AdviceRepository.kt` | Dashboard-Limit pruefen vor API-Call |
| `JournalViewModel.kt` | Spam-Limit pruefen vor KI-Call |
| `FeedbackSender.kt` (oder neu) | E-Mail-Warnung an dev.app.support@gmail.com |
| Neues UI-Element | Cooldown-Meldung ("System ausgelastet") |
| Neues UI-Element | Tageslimit-Meldung ("Morgen wieder verfuegbar") |
| `Constants.kt` | Neue Konstanten fuer alle Limits |
