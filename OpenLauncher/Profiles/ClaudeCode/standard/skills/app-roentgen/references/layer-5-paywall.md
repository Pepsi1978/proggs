# Schicht 5 — Paywall-Tiefenanalyse: DER WICHTIGSTE BEREICH

> **FIX AA8 (Audit 11) — Kotlin + Java (Billing):** Patterns mit `--include='*.kt'` gelten primaer fuer `billing-ktx` (moderne Coroutine-API). Bei Apps mit Java-`BillingClient`-Implementierungen (Java-`PurchasesUpdatedListener`, Java-`BillingClientStateListener`, klassischer Callback-Stil) MUSS `--include='*.java'` ergaenzt werden. Der `queryPurchaseHistoryAsync`-Check (Section 5.0) ist sprachunabhaengig — auch in Java-Code pruefen.

## Warum diese Schicht besondere Aufmerksamkeit braucht

Werbeaussagen rund um Subscription-Modelle sind die haeufigste Quelle fuer rechtliche Probleme: UWG §5 (Irrefuehrung), EU UCPD (Unfair Commercial Practices), Google Play Subscription Policy. Diese Schicht muss JEDE Stufe der Paywall, JEDEN Abbruchpfad, JEDEN Trial-Schritt, JEDEN Promo-Code, JEDEN Win-Back-Flow erfassen — und gegen die Pflichtangaben pruefen.

Im finalen Audit-Bericht bekommt Schicht 5 einen eigenen Hauptabschnitt mit eigenem Inhaltsverzeichnis.

## 5.0 BillingClient-Version-Detection (PFLICHT, neu seit v8)

Vor allen anderen Patterns: zuerst die BillingClient-Version pruefen. Seit **Google Play Billing v8 (30. Juni 2025)** sind einige Aufrufe ENTFERNT worden. Wenn die App noch eine alte Version nutzt oder noch alte Aufrufe enthaelt, sind das eigene Audit-Befunde.

```bash
# Version aus Gradle-Catalog oder build.gradle.kts
grep -rn 'play-services-billing\|billing-ktx\|com\.android\.billingclient' \
  --include='*.gradle*' --include='*.toml' .

# Builder-Pattern (gibt indirekt die Major-Version preis durch verwendete APIs)
grep -rn 'BillingClient\.newBuilder\(\|BillingClient\.Builder' --include='*.kt' .
```

### Was sich mit v8 geaendert hat — IM AUDIT MARKIEREN

| Aufruf | v7 (alt) | v8 (06/2025) | Audit-Aktion |
|--------|----------|--------------|--------------|
| `queryPurchaseHistoryAsync` | vorhanden | **ENTFERNT (kein Ersatz)** | Wenn im Code vorhanden → **KRITISCH** (Code ist broken bei v8) |
| `setObfuscatedAccountId` | empfohlen | weiterhin empfohlen | OK |
| Auto-Reconnect | manuell via `startConnection` | **automatisch** | Wenn manuell mit Retry-Loop: kann entfernt werden |
| Kotlin-Extensions | partial | **vollstaendig** | Bei Java-Aufrufen: Modernisierungs-Hinweis |
| `RecurrenceMode` | enum | **erweitert** (neue Offer-Kombinationen) | Pruefen ob neue Phasen-Kombis behandelt werden |

```bash
# v8-Migration-Pflicht-Check: alte API darf nicht mehr aufgerufen werden
grep -rn 'queryPurchaseHistoryAsync' --include='*.kt' --include='*.java' .
# Wenn Treffer: AUDIT-BEFUND "KRITISCH — broken bei Play Billing v8"

# Connection-Pattern: bei v8 unnoetig (automatisch)
grep -rn 'startConnection\b' --include='*.kt' . | grep -v 'test/'
# Wenn Treffer + v8 erkannt: Hinweis "vereinfachbar mit v8 Auto-Reconnect"
```

Quellen:
- [RevenueCat — Play Billing 8 Migration Guide (Juli 2025)](https://www.revenuecat.com/blog/engineering/play-billing-8-migration/)
- [Qonversion — BillingClient v8 Release Notes](https://qonversion.io/blog/google-play-billing-library-8-0-release-notes-and-action-checklist)

## 5.1 Alle Paywall-relevanten Code-Pfade finden

```bash
# Core Billing-Setup (Google Play Billing Library)
grep -rln 'BillingClient\|BillingFlowParams\|ProductDetails\|SkuDetails' --include='*.kt' . | sort

# Verbindung zum Billing-Service (bei v8 oft nicht mehr noetig)
grep -rn 'startConnection\|endConnection\|onBillingSetupFinished' --include='*.kt' .

# Produkt-Abfrage (queryPurchaseHistoryAsync wurde in v8 ENTFERNT, siehe 5.0!)
grep -rn 'queryProductDetailsAsync\|querySkuDetailsAsync\|queryPurchasesAsync' --include='*.kt' .
grep -rn 'queryPurchaseHistoryAsync' --include='*.kt' .  # MUSS bei v8 leer sein

# Subscription-spezifische Felder
grep -rn 'SubscriptionOfferDetails\|offerToken\|basePlanId\|pricingPhases' --include='*.kt' .

# Kauf-Flow
grep -rn 'launchBillingFlow\|setProductDetailsParamsList' --include='*.kt' .

# Identifizierung des Nutzers in der Transaktion
grep -rn 'obfuscatedAccountId\|obfuscatedProfileId\|setObfuscated' --include='*.kt' .

# Purchase-Verarbeitung
grep -rn 'onPurchasesUpdated\|PurchaseState\|isAcknowledged' --include='*.kt' .

# Bestaetigung des Kaufs (Pflicht binnen 3 Tagen)
grep -rn 'acknowledgePurchase\|consumeAsync' --include='*.kt' .

# Subscription-Status-Pruefung
grep -rn 'isSubscribed\|isPremium\|hasSubscription\|hasActivePremium' --include='*.kt' . -i

# Premium-Feature-Gates
grep -rn 'requirePremium\|requirePro\|gateBehindPremium\|premiumOnly' --include='*.kt' . -i
```

## 5.2 Alle Paywall-Bildschirme katalogisieren

Eine moderne Subscription-App hat oft DEUTLICH MEHR Paywall-Bildschirme als man auf den ersten Blick denkt. Im Audit jeden EINZELNEN dokumentieren.

Typische Paywall-Bildschirme die zu pruefen sind:

| Bildschirm-Typ | Wann erscheint er | Aufmerksamkeitspunkt |
|---------------|-------------------|---------------------|
| **Hauptpaywall** | Manueller Klick auf "Upgrade" oder Feature-Gate | Pflichtangaben (Preis, Laufzeit, Auto-Renewal, Kuendigung) |
| **Onboarding-Paywall** | Im Onboarding-Flow nach N Screens | Auto-Trial-Conversion klar deklariert? |
| **Trial-Paywall** | Variante mit Trial-Hervorhebung | Enddatum + Folgepreis sichtbar |
| **Soft-Paywall** | Nach X freien Aktionen, dismissable | "Vielleicht spaeter"-Option vorhanden? |
| **Hard-Paywall** | Feature komplett gesperrt, nicht dismissable | Nur "Premium kaufen" oder "App beenden"? |
| **Promo-Paywall** | Sonderaktion (z.B. 50% Yearly) | Streichpreis ehrlich? |
| **Win-Back-Paywall** | Fuer ehemalige Subscriber (linkedPurchaseToken) | Spezial-Offer korrekt angezeigt? |
| **Renewal-Paywall** | 7 Tage vor Auto-Renewal | EU UCPD-Pflicht-Reminder? |
| **Paused-Subscription-Banner** | State PAUSED | Reaktivieren-Pfad? |
| **Grace-Period-Banner** | State IN_GRACE_PERIOD | Dunning-UI: Zahlung reparieren? |
| **Account-Hold-Screen** | State ON_HOLD | Premium gesperrt + Payment-Fix-Deep-Link |
| **Cancel-Survey** | Beim Klick "Abo kuendigen" | Welche Reasons gefragt? Speicherung DSGVO? |
| **Cancel-Bestaetigung** | Nach Survey | "Bis X laeuft das Abo noch" |
| **Cancellation-Scheduled-Banner** | Nach Cancel, vor Ablauf | Reaktivieren moeglich? |
| **Expired-Banner** | State EXPIRED | Win-Back-Offer? |
| **Pricing-Increase-Notice** | Bei Preiserhoehung (Consent) | Pflicht-Consent-UI |
| **Restore-Purchase-Button** | In Settings oder Login | Funktion getestet? |
| **Subscription-Management** | Eigener Settings-Bereich | "Abo verwalten"-Link zu Play Store |

Pro Bildschirm zu pruefen (Sub-Tabelle im Bericht):

```markdown
### Paywall-Bildschirm: <Name>

- **Datei:** PaywallScreen.kt:42, PaywallViewModel.kt
- **Trigger:** [wo wird er ausgeloest, alle moeglichen Quellen auflisten]
- **Angezeigte Plaene:**
  - Monthly: 4,99 €/Monat
  - Yearly: 29,99 €/Jahr (entspricht 2,49 €/Monat)
  - Promo Yearly: 22,49 €/Jahr (entspricht 1,87 €/Monat)
  - Trial: 7 Tage kostenlos, danach Yearly 29,99 €/Jahr
- **Pflichtangaben-Checkliste (siehe Schicht 7):**
  - [ ] Preis mit Waehrung explizit
  - [ ] Abrechnungsintervall klar
  - [ ] Auto-Verlaengerung erwaehnt
  - [ ] Kuendigungsmoeglichkeit erwaehnt
  - [ ] Trial-Ende und Folgepreis (falls Trial)
  - [ ] Streichpreis-Realitaet (falls Promo)
- **Buttons / Aktionen:**
  - "Premium starten" → launchBillingFlow(offerToken=...)
  - "Spaeter erinnern" → snooze 7 Tage, popBackStack
  - "X" (Schliessen) → popBackStack
  - "Kaufoption wiederherstellen" → queryPurchasesAsync(...)
- **Side-Effects:**
  - Beim Erfolg: BillingResponseCode.OK + PurchaseState.PURCHASED → acknowledgePurchase, navigate("paywall_success")
  - Bei USER_CANCELED: bleibt auf Paywall, kein Toast
  - Bei BILLING_UNAVAILABLE: ErrorScreen mit Retry
  - Bei PENDING: Warte-Screen, kein Premium-Grant
- **Werbeaussagen auf diesem Bildschirm (Stringformat):**
  - "Unbegrenzte KI-Analysen" (paywall_feature_unlimited_ai)
  - "5 Analyse-Perspektiven" (paywall_feature_perspectives)
  - "Werbefrei" (paywall_feature_no_ads)
  - ... (zur Pruefung in Schicht 7)
- **Wortlaut-Block (PFLICHT — jeder Slot 1:1 zitieren):**
```

### 5.2b Wortlaut-Block pro Paywall-Bildschirm (PFLICHT)

Jeder Paywall-Bildschirm bekommt eine eigene vollstaendige Wortlaut-Tabelle. Es reicht NICHT, nur die Feature-Bullets zu listen — die rechtlichen Probleme entstehen oft in Disclaimern, Hinweistexten, Footer-Zeilen und Button-Beschriftungen.

| Slot | String-Key | Wortlaut (DE 1:1) | Wortlaut (EN 1:1) | Quelle |
|------|-----------|-------------------|-------------------|--------|
| **Headline (Top)** | `paywall_main_headline` | "Unlock BestJournal Premium" | ... | `strings.xml:140` |
| **Subhead** | `paywall_main_subhead` | "Mit Premium nutzt du alle KI-Analysen, ohne Werbung." | ... | `strings.xml:141` |
| **Feature-Bullet 1** | `paywall_feature_1` | "Unbegrenzte KI-Analysen" | ... | `strings.xml:142` |
| **Feature-Bullet 2** | `paywall_feature_2` | "5 Analyse-Perspektiven" | ... | `strings.xml:143` |
| **Feature-Bullet 3** | `paywall_feature_3` | "Werbefrei" | ... | `strings.xml:144` |
| **Plan-Karte Monthly Title** | `paywall_plan_monthly_title` | "Monatlich" | ... | `strings.xml:150` |
| **Plan-Karte Monthly Price** | `paywall_plan_monthly_price` | "4,99 € / Monat" | ... | (Live-Wert aus Billing) |
| **Plan-Karte Yearly Title** | `paywall_plan_yearly_title` | "Jaehrlich" | ... | `strings.xml:151` |
| **Plan-Karte Yearly Price** | `paywall_plan_yearly_price` | "29,99 € / Jahr (entspricht 2,49 € / Monat)" | ... | (Live-Wert) |
| **Plan-Karte Yearly Badge** | `paywall_plan_yearly_badge` | "Beste Wahl" | ... | `strings.xml:152` |
| **Trial-Hinweis** | `paywall_trial_disclaimer` | "7 Tage kostenlos testen, danach 29,99 € pro Jahr. Jederzeit kuendbar." | ... | `strings.xml:160` |
| **Auto-Renewal-Hinweis** | `paywall_auto_renewal_notice` | "Das Abo verlaengert sich automatisch zum Ende der Laufzeit, wenn nicht 24 Stunden vor Ablauf gekuendigt wird." | ... | `strings.xml:161` |
| **Cancel-Hinweis** | `paywall_cancel_notice` | "Verwaltung und Kuendigung in Google Play moeglich." | ... | `strings.xml:162` |
| **Streichpreis-Hinweis** (falls Promo) | `paywall_strike_notice` | "Statt 39,99 € jetzt 29,99 € (-25 %)" | ... | `strings.xml:170` |
| **CTA-Primaer-Button** | `paywall_cta_primary` | "Jetzt Premium starten" | ... | `strings.xml:180` |
| **CTA-Sekundaer-Link** | `paywall_cta_restore` | "Kaufoption wiederherstellen" | ... | `strings.xml:181` |
| **Schliessen-Icon (a11y)** | `paywall_close_cd` | "Paywall schliessen" | ... | `strings.xml:182` |
| **Footer Disclaimer** | `paywall_footer_disclaimer` | "Es gelten unsere AGB. Datenschutzhinweise findest du in den Einstellungen." | ... | `strings.xml:190` |
| **Link-Text AGB** | `paywall_link_terms` | "Allgemeine Geschaeftsbedingungen" | ... | `strings.xml:191` |
| **Link-Text Datenschutz** | `paywall_link_privacy` | "Datenschutzerklaerung" | ... | `strings.xml:192` |
| **Fehlertext bei Kauf-Fehler** | `paywall_error_purchase` | "Der Kauf konnte nicht abgeschlossen werden. Bitte versuche es spaeter erneut." | ... | `strings.xml:200` |
| **Fehlertext Billing nicht verfuegbar** | `paywall_error_billing_unavail` | "Google Play ist gerade nicht erreichbar." | ... | `strings.xml:201` |

Wichtig: Wenn ein Paywall-Bildschirm ZUSAETZLICHE Tooltips, Info-Icons, expandierbare FAQ-Sektionen oder Bottom-Sheets enthaelt, bekommen DIESE eigene Sub-Tabellen unter der Haupt-Tabelle.

### 5.2c Wortlaut-Block fuer Cancel-Flow (PFLICHT)

Der Cancel-Flow ist UWG-kritisch und bekommt eine eigene Wortlaut-Tabelle:

| Slot | String-Key | Wortlaut (DE 1:1) |
|------|-----------|-------------------|
| Settings-Eintrag "Abo verwalten" | `settings_subscription_manage` | "Abonnement verwalten" |
| Subscription-Detail TopBar | `subscription_detail_title` | "Dein Premium-Abo" |
| Plan-Anzeige | `subscription_detail_plan_label` | "Aktueller Plan" |
| Naechste-Abbuchung-Label | `subscription_detail_next_billing` | "Naechste Abbuchung am %1$s" |
| Cancel-Link-Button | `subscription_cancel_button` | "Abo kuendigen" |
| Cancel-Survey-Title | `churn_survey_title` | "Schade dass du gehst — kurz: warum?" |
| Cancel-Reason-Option 1 | `churn_reason_too_expensive` | "Zu teuer" |
| Cancel-Reason-Option 2 | `churn_reason_not_using` | "Nutze die App zu selten" |
| Cancel-Reason-Option 3 | `churn_reason_missing_features` | "Mir fehlen Funktionen" |
| Cancel-Reason-Option 4 | `churn_reason_other` | "Anderer Grund" |
| Cancel-Confirm-Title | `churn_confirm_title` | "Wirklich kuendigen?" |
| Cancel-Confirm-Body | `churn_confirm_body` | "Du verlierst Zugriff auf alle Premium-Funktionen am %1$s. Bis dahin kannst du Premium weiter nutzen." |
| Cancel-Confirm-Confirm-Button | `churn_confirm_yes` | "Ja, kuendigen" |
| Cancel-Confirm-Dismiss-Button | `churn_confirm_no` | "Doch Premium behalten" |
| Win-Back-Headline (nach Cancel-Tap) | `winback_headline` | "Bleib mit 50% Rabatt fuer 3 Monate" |
| Win-Back-Accept-Button | `winback_accept` | "Angebot annehmen" |
| Win-Back-Decline-Button | `winback_decline` | "Trotzdem kuendigen" |
| Cancel-Erfolg-Snackbar | `churn_success_snack` | "Abo wurde gekuendigt. Premium laeuft noch bis %1$s." |

### 5.2d Wortlaut-Block fuer Subscription-State-Banner (PFLICHT)

Pro Subscription-State der eigene UI hat (PAUSED, IN_GRACE_PERIOD, ON_HOLD, CANCELED, EXPIRED) MUSS eine Wortlaut-Tabelle erstellt werden:

| State | Slot | String-Key | Wortlaut (DE 1:1) |
|-------|------|-----------|-------------------|
| PAUSED | Banner-Title | `state_paused_title` | "Dein Abo ist pausiert" |
| PAUSED | Banner-Body | `state_paused_body` | "Es startet automatisch am %1$s wieder." |
| PAUSED | Action-Button | `state_paused_action_resume` | "Jetzt fortsetzen" |
| IN_GRACE_PERIOD | Banner-Title | `state_grace_title` | "Zahlung fehlgeschlagen" |
| IN_GRACE_PERIOD | Banner-Body | `state_grace_body` | "Wir versuchen es nochmal. Du behaeltst Premium fuer %1$d Tage." |
| IN_GRACE_PERIOD | Action-Button | `state_grace_action_fix` | "Zahlungsmethode aktualisieren" |
| ON_HOLD | Banner-Title | `state_hold_title` | "Premium gesperrt" |
| ... | ... | ... | ... |

## 5.3 Trial-Mechanik (Free-Trial + Intro-Pricing)

Trial-Logik liegt in den `pricingPhases` der `SubscriptionOfferDetails`:

```kotlin
productDetails.subscriptionOfferDetails?.forEach { offer ->
    offer.pricingPhases.pricingPhaseList.forEach { phase ->
        // FREE_TRIAL erkennt man an: phase.priceAmountMicros == 0L
        // INTRO_PRICING: phase.recurrenceMode == FINITE_RECURRING + niedriger Preis als Standard
        // STANDARD: phase.recurrenceMode == INFINITE_RECURRING
    }
}
```

Suche-Patterns:

```bash
grep -rn 'pricingPhaseList\|priceAmountMicros\|billingPeriod' --include='*.kt' .
grep -rn 'RecurrenceMode\|FINITE_RECURRING\|INFINITE_RECURRING\|NON_RECURRING' --include='*.kt' .
grep -rn 'introductoryPrice\|introductoryPricePeriod' --include='*.kt' .
grep -rn 'offerTags\|offerTag' --include='*.kt' .
```

Im Audit fuer JEDE Trial-Konfiguration dokumentieren:

| Offer-Tag | Phasen | Was sieht der Nutzer | Audit-Pruefung |
|-----------|--------|---------------------|---------------|
| free-trial-7 | Phase 1: 7 Tage 0 €, Phase 2: 4,99 €/Monat | "7 Tage kostenlos, danach 4,99 € pro Monat" | Steht der Folgepreis sichtbar? |
| intro-pricing-3m | Phase 1: 3 Monate 1,99 €, Phase 2: 4,99 €/Monat | "3 Monate zu 1,99 €, danach 4,99 €" | Standardpreis sichtbar? |
| win-back-2026 | Phase 1: 1 Monat 50% rabattiert | "Sonderangebot: 1 Monat fuer 2,49 €" | Reduziert nur fuer Ehemalige? |

## 5.4 Promo-Codes und Win-Back

```bash
grep -rn 'linkedPurchaseToken' --include='*.kt' .
grep -rn 'PromotionalCode\|promoCode\|PROMO_' --include='*.kt' . -i
grep -rn 'winback\|win_back\|win-back' --include='*.kt' . -i
grep -rn 'isEligibleForOffer\|offerEligibility' --include='*.kt' .
```

`linkedPurchaseToken` im neuen Purchase ist das Schluessel-Signal: Nutzer war frueher Subscriber. Wenn die App das nutzt, gibt es einen Win-Back-Pfad zu pruefen.

## 5.5 Cancel-Flow (Churn)

```bash
grep -rn 'churn\|cancel\|kuendig' --include='*.kt' . -i | grep -v 'test/' | grep -v '//'
grep -rn 'ChurnReason\|CancelReason\|cancellationReason' --include='*.kt' .
grep -rn 'ChurnSurvey\|CancelSurvey\|cancelDialog' --include='*.kt' .
```

Cancel-Flow ist rechtlich sensibel:
- Muss EU-konform "so einfach wie der Abschluss" sein
- DE: Pflicht-Kündigungsbutton (Onlineverträge, seit 2022)
- Kein Dark Pattern (versteckter Cancel-Button, "wirklich, wirklich, wirklich abbrechen?")

Pro Cancel-Bildschirm dokumentieren:
- Wie kommt der Nutzer dorthin (von Settings? Tief versteckt?)
- Wie viele Klicks
- Wird ein Survey gezeigt (Reasons)
- Werden die Reasons gespeichert / hochgeladen (DSGVO)
- Win-Back-Versuch nach Cancel? (1 Versuch ist ok, mehrere = Dark Pattern)
- Bestaetigungs-UI nach Cancel: Klar dass es jetzt gekuendigt ist?

## 5.6 Server-Side Validation und Subscription-Status

Moderne Apps validieren Subscription-Status server-side via Cloud Function + Google Play Developer API:

```bash
# Cloud-Function-Aufrufe
grep -rn 'callFirebaseFunction\|FirebaseFunctions\.getInstance\|httpsCallable' --include='*.kt' .

# REST-Aufrufe an eigene Backend
grep -rn '/subscription\|/billing\|subscription-status' --include='*.kt'

# Real-Time Developer Notifications (im Backend, hier nicht direkt)
# Aber: AppCheck / Auth-Token muss vor Aufruf gesetzt werden
grep -rn 'AppCheck\|FirebaseAuth\.getCurrentUser' --include='*.kt' .
```

Im Audit dokumentieren:
- Wo wird Subscription-Status server-side geprueft?
- Welche Felder kommen zurueck (expiryTime, autoRenewing, state)?
- Was wenn Server nicht erreichbar (Offline)?
  - Cached letzter Status? Wie lange?
  - Wird Premium grosszuegig (offen) oder restriktiv (gesperrt) angenommen?

## 5.7 Edge-Cases die jeder Audit pruefen muss

```markdown
| Edge-Case | Code-Pattern | Im Audit pruefen |
|-----------|-------------|----------------|
| Nicht-bestaetigter Kauf (3-Tage-Frist) | acknowledgePurchase aufgerufen? | JA bei jedem Purchase-State.PURCHASED |
| PENDING-State (z.B. Bargeldzahlung) | PurchaseState.PENDING gehandhabt? | Warte-UI, kein Premium-Grant |
| ITEM_ALREADY_OWNED | onPurchasesUpdated → Recovery? | queryPurchasesAsync + Acknowledge |
| Restore-Purchase-Button | Erreichbar in Settings? | Pflicht laut Google Policy |
| BILLING_UNAVAILABLE | Error-Screen vorhanden? | Retry-Mechanismus? |
| includeSuspendedSubscriptions | In QueryPurchasesParams gesetzt? | Sonst Suspended-Subs nicht erkannt |
| obfuscatedAccountId gesetzt? | setObfuscatedAccountId | Schutz vor Multi-Account-Missbrauch |
| Reconnect nach Background | startConnection nach Resume? | Ohne: stale BillingClient |
| Token-basierte Renewal-Detection | Stamp-Mechanismus / linkedPurchaseToken | Promos werden korrekt erkannt? |
```

## 5.8 Cross-Reference mit allen 7 Subscription-States

Pflicht: Fuer jeden der 7 Hauptstates pruefen welcher UI-Bildschirm dazugehoert. Volle Tabelle in `subscription-state-machine.md`. Kurzform:

```
ACTIVE              → Premium-UI, alle Features
IN_GRACE_PERIOD     → Premium-UI + Dunning-Banner (Zahlung reparieren!)
ON_HOLD             → Free-UI + Sperr-Screen (Payment-Fix-Deep-Link)
PAUSED              → Free-UI + Pause-Info (Auto-Resume am X)
CANCELED (aktiv)    → Premium-UI + "Laeuft ab am X" + ggf. Win-Back-Angebot
EXPIRED             → Free-UI + Paywall (Win-Back-Offer wenn linkedPurchaseToken)
PENDING             → Warte-Screen (Zahlung ausstehend)
```

Wenn ein State im Code nicht behandelt wird → Audit-Befund.

## 5.9 Werbeaussage-Pflichtangaben pro Paywall-Bildschirm

Diese Pflichtangaben MUESSEN auf JEDER Paywall-Variante stehen (laut UWG, EU UCPD, Google Play Policy):

| # | Pflichtangabe | Bedingung | Audit-Frage |
|---|--------------|-----------|------------|
| 1 | Exakter Preis mit Waehrung | Immer | "9,99 €" sichtbar? |
| 2 | Abrechnungsintervall | Immer | "/Monat", "/Jahr" sichtbar? |
| 3 | Automatische Verlaengerung | Immer (Auto-Renewal) | "verlaengert sich automatisch" Text vorhanden? |
| 4 | Kuendigungsmoeglichkeit | Immer | "jederzeit kuendbar" Text vorhanden? |
| 5 | Trial-Enddatum + Folgepreis | Bei Trial | "Nach 7 Tagen: 4,99 €/Monat" sichtbar? |
| 6 | Intro-Periode + Folgepreis | Bei Intro-Pricing | "3 Monate zu 1,99 €, danach 4,99 €" sichtbar? |
| 7 | Streichpreis-Realitaet | Bei Promo mit Streichpreis | Original-Preis war wirklich aktiv? |
| 8 | Jahresgesamtbetrag bei Yearly | Bei Yearly-Plaenen | "29,99 €/Jahr" und nicht nur "2,49 €/Monat"? |
| 9 | Sprache + Waehrung passend | Immer | DE-Nutzer bekommt EUR und Deutsch? |
| 10 | Kein Dark Pattern beim Cancel | Bei Cancel-Flow | Cancel-Button gleich gross wie Purchase-Button? |

## 5.10 Output-Format fuer Schicht 5 (im Bericht)

```markdown
## Schicht 5 — Paywall-Tiefenanalyse

### 5.1 Paywall-Architektur-Uebersicht

[Mermaid-Diagramm mit allen Subscription-States und UI-Mappings]

### 5.2 Inventar aller Paywall-Bildschirme (N gefunden)

#### 5.2.1 Hauptpaywall (PaywallScreen.kt:42)
[Detail-Tabelle wie in 5.2 oben]

#### 5.2.2 Onboarding-Paywall (OnboardingPaywallScreen.kt:18)
[Detail-Tabelle]

#### 5.2.3 Cancel-Survey (ChurnFlowDialog.kt:25)
[Detail-Tabelle]

[... fuer alle Paywall-Bildschirme ...]

### 5.3 Subscription-State-zu-UI-Mapping

| State | UI-Bildschirm/Komponente | Datei | Status |
|-------|------------------------|-------|--------|
| ACTIVE | DashboardScreen mit Premium-Badge | DashboardScreen.kt:78 | implementiert |
| IN_GRACE_PERIOD | GracePeriodBanner (oben) | DashboardScreen.kt:92 | implementiert |
| ON_HOLD | AccountHoldScreen | AccountHoldScreen.kt:22 | implementiert |
| PAUSED | (kein Banner) | — | NICHT implementiert — KRITISCH |
| ... | ... | ... | ... |

### 5.4 Trial- und Promo-Mechaniken

[Tabellen wie in 5.3 oben]

### 5.5 Cancel-Flow (Churn)

[Detail-Beschreibung mit Klick-Counter und DSGVO-Bewertung]

### 5.6 Edge-Case-Pruefungen (Tabelle aus 5.7)

### 5.7 Pflichtangaben-Pruefung pro Paywall-Bildschirm

[Tabelle aus 5.9 mit Status pro Bildschirm]

### 5.8 Server-Side Validation

[Beschreibung wie geprueft + Caching-Verhalten]

### 5.9 Audit-Befunde Schicht 5

| # | Befund | Risiko | Datei | Empfehlung |
|---|--------|--------|-------|-----------|
| 1 | PAUSED-State nicht im UI gehandhabt | MITTEL | — | Banner einfuehren |
| 2 | Auf Onboarding-Paywall fehlt Hinweis "verlaengert sich automatisch" | KRITISCH UWG §5 | OnboardingPaywallScreen.kt:42 | Text-Korrektur in strings.xml |
| ... | ... | ... | ... | ... |
```

## Typische Fehlerquellen in Schicht 5

- **Promo-Paywall vergessen** weil sie nur fuer bestimmte User-Gruppen erscheint (linkedPurchaseToken oder Cohort-basiert).
- **Cancel-Survey-Reasons werden DSGVO-relevant uebermittelt** ohne Erwaehnung in der Datenschutzerklaerung.
- **PAUSED-State nicht behandelt** — User pausiert Abo, kommt zurueck und sieht "kaputte" UI.
- **Server-Side-Validation hat keine Offline-Strategie** — App entzieht Premium wenn Server kurz nicht antwortet.
- **acknowledgePurchase nicht aufgerufen** — Google refunded automatisch nach 3 Tagen.
- **Multi-Currency-Anzeige fehlt** — App zeigt nur EUR auch in nicht-EUR-Laendern.
- **Cancel-Button Dark Pattern** — visuell schwaecher als der Purchase-Button (graue Schrift, kleinere Groesse).
