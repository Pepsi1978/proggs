# Schicht 7 — Werbeaussage-vs-Feature-Matrix

## Zweck und juristischer Rahmen

Schicht 7 ist die finale Synthese: Hier werden ALLE Werbeaussagen aus ALLEN Quellen gegen das Feature-Inventar (Schicht 1-6) geprueft. Output ist eine Matrix die fuer jede Aussage sagt: passt sie zum Code, oder ist sie irrefuehrend?

Rechtlicher Rahmen Stand 2026:

| Norm | Geltung | Was sie verbietet |
|------|---------|------------------|
| **UWG §5 (DE)** | Deutschland | Aktive Irrefuehrung (falsche Behauptungen) |
| **UWG §5a (DE)** | Deutschland | Vorenthalten wesentlicher Informationen |
| **EU UCPD** | EU | Unfair Commercial Practices, Dark Patterns, Fake Scarcity |
| **EU Digital Fairness Act** | EU (Entwurf) | Verschaerfung UCPD ab ~2029 |
| **Gesetz fuer faire Verbrauchervertraege (DE 2022)** | Deutschland | Online-Kuendigungsbutton-Pflicht |
| **Google Play Subscriptions Policy** | Play Store global | Pflicht-Disclosures Preis/Verlaengerung/Cancel |
| **Google Play Metadata Policy** | Play Store global | App-Beschreibung muss App akkurat repraesentieren |
| **FTC Endorsement Guides (US)** | USA | KI-Claims brauchen Substanz, Affiliate-Disclosure |
| **FTC Junk Fees Rule (US 2025)** | USA | Pflicht-Gebuehren upfront ausweisen |

## 7.0 Plattform-Hinweise (FIX T7 + AA7)

> **FIX AA7 (Audit 10) — Kotlin + Java:** Code-Realitaets-Patterns in Abschnitt 7.3 (`AdMob`, `GenerativeModel`, `FirebaseAnalytics` etc.) zeigen `--include='*.kt'`. Bei Apps mit Java-Anteilen `--include='*.java'` ergaenzen — Java-Legacy-Module nutzen diese SDKs haeufig direkt aus `.java`-Dateien.

> **Pfad-Annahme:** Die Bash-Snippets in diesem Layer benutzen aus Lesbarkeitsgruenden `app/src/main/...`. Bei **Multi-Module-Apps** (z.B. `feature/auth/`, `feature/journal/`, `core/ui/` mit jeweils eigenen `src/main/res/values/strings.xml`) MUSS jede Stelle die `app/src/main/res/values/...` enthaelt projektweit gesucht werden:
>
> ```bash
> # Statt:  cat app/src/main/res/values/strings.xml
> # Multi-Module-faehig:
> find . -path '*/src/main/res/values/strings.xml' -not -path '*/build/*' | xargs cat
> ```
>
> Das gleiche Muster gilt fuer `values-*/strings.xml` (alle Sprachen) und `AndroidManifest.xml` (jedes Modul kann eigene Permissions deklarieren). Das `feature-scan.sh` Skript hat dafuer die Helper `find_default_strings_xml`, `find_translated_strings_xml` und `find_locale_dirs` — diese werden automatisch verwendet wenn das Skript laeuft. Manuelle Audits muessen den Pfad anpassen.

## 7.1 Quellen aller Werbeaussagen sammeln

```bash
# 1. Alle UI-Strings (Hauptsprache) — bei Multi-Module siehe 7.0
cat app/src/main/res/values/strings.xml > /tmp/claims_de.txt

# 2. Alle uebersetzten Sprachen
ls app/src/main/res/values-*/strings.xml | while read f; do
  echo "===== $f =====" >> /tmp/claims_translated.txt
  cat "$f" >> /tmp/claims_translated.txt
done

# 3. Onboarding-Texte explizit
grep -E 'onboarding|welcome|intro' app/src/main/res/values/strings.xml -i

# 4. Paywall-Texte explizit
grep -E 'paywall|premium|pro|upgrade|trial|subscribe' app/src/main/res/values/strings.xml -i

# 5. Push-Notification-Templates
grep -E 'notification|push|reminder' app/src/main/res/values/strings.xml -i

# 6. Settings-Beschreibungen
grep -E 'settings|preference|description_' app/src/main/res/values/strings.xml -i

# 7. Hardcoded-Strings im Code (sollten fast 0 sein, aber pruefen)
grep -rn '"[A-Z][a-zA-Z ,!.?]\{20,\}"' --include='*.kt' . | grep -v 'test/' | head -50
```

**Externe Quellen die NICHT im Repo liegen, aber im Audit dazugehoeren:**
- Google Play Store Listing (Long Description, Short Description, Feature-Bullets)
- Hilfeseiten, Webseite
- E-Mail-Templates (Willkommen, Trial-End, Renewal-Reminder)
- Marketing-Materialien (Screenshots im Store, Promotional Videos)

Im Audit aufnehmen: Frank wird gebeten den Store-Listing-Text manuell beizutragen. Der Skill kann nur den Code-Teil automatisch pruefen.

## 7.2 Risiko-Keyword-Filterung

Diese Keywords haben hohes UWG-Risiko und MUESSEN systematisch geprueft werden:

### KRITISCH (sofort pruefen)
```
unlimited, unbegrenzt, all features, alle Features, jede Funktion,
always, immer, 24/7, never, niemals, nie, keine Limits, no limits,
forever, ewig, lifetime, lebenslang
```

### HOCH (eng pruefen)
```
AI, KI, artificial intelligence, kuenstliche Intelligenz, powered by AI, KI-gestuetzt,
smart, intelligent,
offline, ohne Internet,
private, privat, secure, sicher,
ad-free, werbefrei, no ads, keine Werbung,
encrypted, verschluesselt, end-to-end,
free, kostenlos, gratis
```

### MITTEL (im Kontext pruefen)
```
premium, pro, best, fastest, schnellste,
most, alle, every, jede,
complete, vollstaendig,
exclusive, exklusiv,
unique, einzigartig,
professional, profi
```

```bash
# Beispiel-Suche fuer KRITISCH-Keywords in strings.xml
grep -E '(unlimited|unbegrenzt|all features|alle Features|always|immer|24/7|forever|lifetime|lebenslang|niemals)' \
  app/src/main/res/values/strings.xml -i
```

## 7.3 Code-Realitaets-Pruefung

Fuer jede gefundene Werbeaussage: Wo lebt das Feature im Code?

### Beispiel: "Unbegrenzte KI-Analysen"

```bash
# Suche nach Limits in der KI-Logik
grep -rn 'maxAnalyses\|dailyLimit\|aiLimit\|MAX_ANALYSES\|FREE_DAILY_LIMIT' --include='*.kt' .
grep -rn 'analysisCount\|aiUsageCount' --include='*.kt' .
grep -rn 'requirePremium.*analy\|analy.*requirePremium' --include='*.kt' .
```

Wenn ein Limit gefunden wird (`if (count >= 150) return Result.LimitExceeded`):
- Werbeaussage: "unbegrenzt"
- Code-Realitaet: 150/Tag
- Risiko: KRITISCH UWG §5

### Beispiel: "Werbefrei"

```bash
grep -rn 'AdMob\|InterstitialAd\|RewardedAd\|BannerAd' --include='*.kt' .
grep -rn 'showAd\|loadAd' --include='*.kt' .
# Auch: AD_ID Permission im Manifest
grep -E 'AD_ID' app/src/main/AndroidManifest.xml
```

Wenn Werbung im Code aktiv ist trotz "ad-free"-Versprechen → KRITISCH UWG §5.

### Beispiel: "KI-gestuetzt"

```bash
# Echte LLM-Aufrufe?
grep -rn 'GenerativeModel\|GeminiClient\|OpenAI\|Anthropic\|Claude' --include='*.kt' .
grep -rn 'generateContent\|chat.completions\|messages\.create' --include='*.kt' .
```

Wenn nur regex-basierte Logik gefunden wird, kein echter LLM-Aufruf → KRITISCH (FTC + UWG §5).

### Beispiel: "Funktioniert offline"

```bash
# Was passiert ohne Netzwerk?
grep -rn 'isNetworkAvailable\|hasNetwork\|networkAvailable' --include='*.kt' .
grep -rn 'NetworkCallback\|ConnectivityManager' --include='*.kt' .

# Cloud-Aufrufe finden — diese FEHLEN bei echtem Offline-Mode
grep -rn 'firebaseFunctions\|httpsCallable\|retrofit\|ktor' --include='*.kt' .
```

Wenn KI-Features oder Sync-Features Cloud-Calls brauchen, aber Werbung "vollstaendig offline" verspricht → HOCH UWG.

### Beispiel: "Ihre Daten bleiben privat"

```bash
# Cloud-Sync vorhanden?
grep -rn 'firestore\|cloudFunctions\|cloudSync\|uploadToCloud' --include='*.kt' .
# Verschluesselung?
grep -rn 'AES\|EncryptedSharedPreferences\|MasterKey\|sqlCipher' --include='*.kt' .
```

Wenn Cloud-Sync ohne E2E-Verschluesselung — Werbeaussage muss spezifischer sein.

### Beispiel: "Keine Daten werden gesammelt"

```bash
grep -rn 'FirebaseAnalytics\|Crashlytics\|FirebasePerformance' --include='*.kt' .
grep -rn 'logEvent\|setUserProperty' --include='*.kt' .
```

Wenn Analytics aktiv — KRITISCH wenn "keine Daten" verspricht.

## 7.4 Die 6-Felder-Matrix-Tabelle (zentrales Output-Format)

Pro Werbeaussage exakt diese 6 Felder erfassen:

```markdown
| # | Aussage (woertlich) | Quelle | Code-Realitaet | Luecke | Risiko + Norm | Fix-Vorschlag |
|---|---------------------|--------|---------------|--------|--------------|---------------|
| 1 | "Unbegrenzte KI-Analysen" | paywall_feature_unlimited_ai (strings.xml:42) | dailyLimit = 150 in AiUsageRepo.kt:18 | "unbegrenzt" steht aber Code limitiert auf 150/Tag | KRITISCH — UWG §5 + Google Play Policy + FTC | "Bis zu 150 KI-Analysen taeglich" ODER Limit aufheben |
| 2 | "5 Perspektiven" | paywall_feature_perspectives (strings.xml:43) | perspectives.size == 5 in AnalysisProfiles.kt:22 | OK | KEIN | — |
| 3 | "Werbefrei" | paywall_feature_no_ads (strings.xml:44) | Keine Ad-SDK gefunden | OK | KEIN | — |
| 4 | "Ihre Daten bleiben privat" | onboarding_privacy (strings.xml:88) | Firestore-Sync aktiv, keine E2E-Verschluesselung | "privat" suggeriert lokal, faktisch aber Cloud | HOCH UWG §5a | Praezisieren: "Cloud-gesichert mit verschluesselter Uebertragung" |
| 5 | "Funktioniert offline" | feature_offline (strings.xml:60) | KI-Analyse benoetigt Internet (geminiClient in OfflineMode crasht) | KI-Feature offline kaputt | HOCH UWG §5 | Praezisieren: "Eintraege sind offline verfuegbar, KI-Analyse braucht Internet" |
| ... | ... | ... | ... | ... | ... | ... |
```

## 7.5 Pflichtangaben-Pruefung pro Paywall-Bildschirm

Sub-Tabelle pro Paywall-Bildschirm:

```markdown
| Pflichtangabe | Vorhanden? | Quelle (Datei:String-Key) |
|--------------|-----------|--------------------------|
| Exakter Preis mit Waehrung | JA | paywall_price_monthly |
| Abrechnungsintervall | JA | paywall_billing_period |
| Auto-Verlaengerung | NEIN — KRITISCH | — |
| Kuendigung jederzeit | NEIN — KRITISCH | — |
| Trial-Ende + Folgepreis | (kein Trial auf diesem Screen) | — |
| Streichpreis-Realitaet | JA, Original-Preis aktiv von Jan-Mar | paywall_promo_savings |
```

## 7.6 Multi-Sprachen-Audit

KRITISCH: Werbeaussagen muessen in JEDER Sprache geprueft werden. Eine Korrektur in `values/strings.xml` (Deutsch) genuegt nicht, wenn `values-en/`, `values-fr/`, `values-pt/` etc. die alte Aussage haben.

```bash
# Alle Sprachen auflisten
ls -d app/src/main/res/values-* | sort

# Pro String-Key in allen Sprachen pruefen
grep -E '<string name="paywall_feature_unlimited_ai"' \
  app/src/main/res/values*/strings.xml
```

Im Audit pro kritische Aussage: Tabelle mit ALLEN Sprachversionen und ihrem Inhalt.

## 7.7 Store-Listing-Audit (manuell)

Da Store-Listing nicht im Repo liegt, im Audit-Bericht einen Block einfuegen:

```markdown
### 7.7 Store-Listing-Audit (Frank-Aufgabe)

Frank, bitte folgende Texte aus der Google Play Console kopieren und pruefen:

1. **Short Description** (max 80 Zeichen):
   - Aktueller Text: ___________
   - Werbeaussagen darin: ___________
   - Code-Beleg: ___________

2. **Long Description** (Bullet-Points + Text):
   - ___________

3. **Feature-Graphic-Slogan** (in Screenshot-Texten):
   - ___________

Pro Aussage gleiche 6-Felder-Matrix aus 7.4 anwenden.
```

## 7.8 Dynamische / per Server gesteuerte Werbeaussagen

Wenn Firebase Remote Config genutzt wird, koennen Werbeaussagen per Server geaendert werden ohne App-Update. Im Audit:

```bash
grep -rn 'remoteConfig\.getString\|remoteConfig\.fetchAndActivate' --include='*.kt' .
```

Pro Remote-Config-Key: Was ist der Default, was kann der Server-Wert sein, wo wird er angezeigt? Diese Werte muessen dem Audit ebenso unterzogen werden wie statische Strings.

## 7.9 Risiko-Klassifizierung

### KRITISCH (vor Release fixen)
- Absolut-Aussagen mit Limits ("unbegrenzt" + Limit)
- KI-Claims ohne Substanz ("KI" ohne LLM)
- "werbefrei" mit Werbung
- "kostenlos" mit Pflicht-Abo nach Trial ohne klaren Hinweis
- Auto-Verlaengerung nicht erwaehnt auf Paywall
- Cancel-Button als Dark Pattern

### HOCH (innerhalb 2 Wochen fixen)
- Datenschutz-Versprechen ohne Umsetzung
- Verfuegbarkeits-Claims (Offline, 24/7) mit Einschraenkungen
- Streichpreise ohne historische Basis
- "alle Features" mit Tier-Limits

### MITTEL (im naechsten Release-Zyklus)
- Vage Superlative ("beste", "schnellste") ohne Beleg
- "Premium" / "Pro" ohne klare Definition
- Missverstaendliche Formulierungen die korrekt sind aber irrefuehren koennten

### NIEDRIG (Nice-to-have)
- Stilistische Verbesserungen
- Klarere Disclaimer
- Konsistenz zwischen Sprachen

## 7.10 Fix-Strategien

Pro Aussage hat man drei Optionen:

| Option | Wann | Beispiel |
|--------|------|---------|
| **Aussage praezisieren** | Wenn Feature passt aber nicht 100% wie versprochen | "unbegrenzt" → "Bis zu 150/Tag" |
| **Feature anpassen** | Wenn Aussage strategisch wichtig ist | Limit aufheben um "unbegrenzt" zu halten |
| **Aussage entfernen** | Wenn Aussage problematisch und nicht ersetzbar | "KI-gestuetzt" raus, neutralere Formulierung |

## 7.11 Cancel-Flow-Audit

Eigener Sub-Block fuer Cancel-Flow (sehr UWG-relevant):

```markdown
### 7.11 Cancel-Flow-Audit

- Anzahl Klicks bis Cancel: N
- Cancel-Button-Visualitaet: gleich gross wie Purchase-Button? JA/NEIN
- Survey-Reasons werden gespeichert? JA/NEIN
- Werden Reasons in Datenschutzerklaerung erwaehnt? JA/NEIN
- Win-Back-Versuch nach Cancel? Wenn ja: 1 Versuch (OK) oder mehrere (Dark Pattern)?
- Bestaetigungs-Text nach Cancel: Klar dass Cancel erfolgreich war? JA/NEIN
- "Sub laeuft noch bis X"-Hinweis: vorhanden? JA/NEIN
- DE-Pflicht-Kuendigungsbutton in App: vorhanden? (Falls online abgeschlossen)
```

## 7.12 Audit-Befunde der Schicht 7 — Output-Format

```markdown
## Schicht 7 — Werbeaussage-vs-Feature-Matrix

### 7.1 Aussagen-Inventar

| Quelle | Anzahl Aussagen | Davon kritisch | Davon hoch | Davon mittel | Davon ok |
|--------|---------------|---------------|------------|-------------|---------|
| strings.xml (Hauptsprache) | N | n | n | n | n |
| strings.xml (Uebersetzungen, X Sprachen) | N | n | n | n | n |
| Onboarding | N | n | n | n | n |
| Paywall | N | n | n | n | n |
| Push-Notifications | N | n | n | n | n |
| Settings | N | n | n | n | n |
| Store-Listing (manuell) | TBD | TBD | TBD | TBD | TBD |

### 7.2 Hauptmatrix (alle Aussagen sortiert nach Risiko)

[6-Felder-Tabelle aus 7.4]

### 7.3 Pflichtangaben pro Paywall-Bildschirm

[Sub-Tabellen aus 7.5]

### 7.4 Multi-Sprach-Konsistenz

| Aussage-Key | DE | EN | FR | PT | ES | ... |
|------------|----|----|----|----|----|----|
| paywall_feature_unlimited_ai | "Unbegrenzte KI-Analysen" KRIT | "Unlimited AI analyses" KRIT | ... | ... | ... | ... |

### 7.5 Cancel-Flow-Audit

[Block aus 7.11]

### 7.6 Empfohlene Fix-Reihenfolge

1. KRIT-1: paywall_feature_unlimited_ai → praezisieren in 27 Sprachen
2. KRIT-2: paywall_auto_renewal_missing → Pflichttext hinzufuegen in 27 Sprachen
3. HOCH-1: ...
```

## Quellen (juristische Grundlagen)

- [§5 UWG — dejure.org](https://dejure.org/gesetze/UWG/5.html)
- [§5a UWG — dejure.org](https://dejure.org/gesetze/UWG/5a.html)
- [EU UCPD — European Commission](https://commission.europa.eu/law/law-topic/consumer-protection-law/unfair-commercial-practices-and-price-indication/unfair-commercial-practices-directive_en)
- [Google Play Subscriptions Policy](https://support.google.com/googleplay/android-developer/answer/9900533)
- [Google Play Metadata Policy](https://support.google.com/googleplay/android-developer/answer/16810878)
- [FTC Endorsement Guides](https://www.ftc.gov/business-guidance/resources/ftcs-endorsement-guides-what-people-are-asking)
- [FTC Junk Fees Rule (Mai 2025)](https://www.ftc.gov/news-events/news/press-releases/2025/05/ftc-rule-unfair-or-deceptive-fees-take-effect-may-12-2025)
- [Gesetz fuer faire Verbrauchervertraege (DE 2022)](https://www.gesetze-im-internet.de/bgb/__312k.html)
