# Subscription-State-Machine: Komplette Referenz fuer Google Play Billing

## Zweck

Diese Referenz dient zwei Zwecken:
1. Beim Audit pro App pruefen ob ALLE Subscription-States im Code behandelt werden
2. Pro State pruefen ob die UI dem Benutzer das Richtige anzeigt

Wenn ein State nicht im Code behandelt wird, sieht der Benutzer entweder eine kaputte UI oder bekommt unbeabsichtigt Premium-Zugang oder verliert ihn unbeabsichtigt — alles potenzielle UWG-Probleme.

## Die 7 Haupt-States (SubscriptionState aus billing-ktx)

| State | API-Konstante | Premium-Zugang | RTDN-Trigger | UI-Pflicht |
|-------|--------------|---------------|--------------|------------|
| **ACTIVE** | SUBSCRIPTION_STATE_ACTIVE | JA | Type 2 (RENEWED), Type 4 (PURCHASED) | Premium-UI, alle Features |
| **IN_GRACE_PERIOD** | SUBSCRIPTION_STATE_IN_GRACE_PERIOD | JA | Type 6 (IN_GRACE_PERIOD) | Premium-UI + Dunning-Banner "Zahlung fehlgeschlagen, bitte aktualisieren" |
| **ON_HOLD** | SUBSCRIPTION_STATE_ON_HOLD | NEIN | Type 5 (ON_HOLD) | Free-UI + Sperr-Screen mit "Zahlungsmethode aktualisieren"-Deep-Link |
| **PAUSED** | SUBSCRIPTION_STATE_PAUSED | NEIN | Type 10 (PAUSED) | Free-UI + Info "Pausiert bis X — Auto-Resume" |
| **CANCELED** | SUBSCRIPTION_STATE_CANCELED | JA bis expiryTime | Type 3 (CANCELED) | Premium-UI + "Laeuft ab am X" + ggf. Win-Back-Banner |
| **EXPIRED** | SUBSCRIPTION_STATE_EXPIRED | NEIN | Type 13 (EXPIRED) | Free-UI + Paywall (Win-Back-Offer wenn linkedPurchaseToken) |
| **PENDING** | SUBSCRIPTION_STATE_PENDING | NEIN (noch nicht aktiviert) | Type 20 (PENDING_CANCELED) bei Abbruch | Warte-UI "Zahlung ausstehend" |

**WICHTIG:** `queryPurchasesAsync()` gibt Subscriptions in den States PAUSED/ON_HOLD nur zurueck wenn `includeSuspendedSubscriptions=true` in `QueryPurchasesParams.Builder` gesetzt ist. Ohne diesen Flag werden suspended Subscriptions ignoriert.

## State-Uebergaenge (alle moeglichen Pfade)

```
ACTIVE
├── [Auto-Renewal erfolgreich]    → ACTIVE        (Type 2 RENEWED)
├── [User kuendigt]                → CANCELED     (Type 3 CANCELED)
├── [Zahlung fehlgeschlagen]       → IN_GRACE_PERIOD (Type 6)
├── [User pausiert]                → PAUSED       (Type 10)
└── [Pricing-Increase Consent verweigert] → CANCELED

IN_GRACE_PERIOD
├── [Retry erfolgreich]            → ACTIVE       (Type 1 RECOVERED)
└── [Retry-Fenster abgelaufen]     → ON_HOLD      (Type 5)

ON_HOLD
├── [User repariert Zahlung]       → ACTIVE       (Type 1 RECOVERED)
└── [Hold-Fenster abgelaufen, max 60d] → EXPIRED  (Type 13)

PAUSED
├── [Auto-Resume nach Ablauf]      → ACTIVE       (Type 1 RECOVERED)
├── [User reaktiviert vorzeitig]   → ACTIVE
└── [Pause-Plan-Aenderung]         → PAUSED       (Type 11)

CANCELED (Cancel ist gesetzt, Sub laeuft noch)
├── [User reaktiviert vor Ablauf]  → ACTIVE       (Type 7 RESTARTED)
└── [Sub-Periode endet]            → EXPIRED      (Type 13)

EXPIRED
├── [User kauft neu]               → ACTIVE       (Type 4 PURCHASED, mit linkedPurchaseToken = Win-Back-Signal)
└── (keine weiteren automatischen Uebergaenge)

PENDING (Prepaid, noch nicht aktiviert)
├── [Zahlung erfolgreich]          → ACTIVE       (Type 4 PURCHASED)
└── [Prepaid-Code abgelaufen]      → (verfaellt) (Type 20 PENDING_CANCELED)
```

## Die 22 RTDN Notification-Types (vollstaendig)

Real-Time Developer Notifications (RTDN) werden von Google Play an dein Backend (z.B. Cloud Pub/Sub) gesendet. Eine moderne App MUSS sie verarbeiten — entweder selbst oder via RevenueCat/Adapty/aehnliches.

| Typ | Konstante | Wann ausgeloest | Pflicht-Backend-Aktion |
|-----|-----------|---------------|----------------------|
| 1 | SUBSCRIPTION_RECOVERED | Sub aus IN_GRACE_PERIOD oder ON_HOLD wiederhergestellt | Premium reaktivieren |
| 2 | SUBSCRIPTION_RENEWED | Auto-Renewal erfolgreich | expiryTime aktualisieren |
| 3 | SUBSCRIPTION_CANCELED | User hat gekuendigt (laeuft noch bis Periodenende) | Cancellation tracken, expiry abwarten |
| 4 | SUBSCRIPTION_PURCHASED | Neuer Kauf | Entitlement anlegen, acknowledgePurchase |
| 5 | SUBSCRIPTION_ON_HOLD | Grace-Period abgelaufen, Zahlung weiter fehlgeschlagen | Premium sofort entziehen |
| 6 | SUBSCRIPTION_IN_GRACE_PERIOD | Erste fehlgeschlagene Zahlung | Dunning-UI vorbereiten, Premium behalten |
| 7 | SUBSCRIPTION_RESTARTED | User reaktiviert gekuendigtes Abo vor Ablauf | Cancellation aufheben |
| 8 | SUBSCRIPTION_PRICE_CHANGE_CONFIRMED | User hat Preiserhoehung bestaetigt | (informativ) |
| 9 | SUBSCRIPTION_DEFERRED | expiryTime per Developer-Aktion verschoben | expiryTime aktualisieren |
| 10 | SUBSCRIPTION_PAUSED | User hat pausiert | Premium pausieren |
| 11 | SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED | Pause-Plan geaendert | Pause-Daten aktualisieren |
| 12 | SUBSCRIPTION_REVOKED | Kauf revoziert (Refund, Chargeback) | Premium SOFORT entziehen |
| 13 | SUBSCRIPTION_EXPIRED | Sub ist abgelaufen | Premium entziehen |
| 14 | (reserviert) | — | — |
| 15 | (reserviert) | — | — |
| 16 | (reserviert) | — | — |
| 17 | SUBSCRIPTION_ITEMS_CHANGED | Items in Bundle-Sub geaendert | Bundle neu pruefen |
| 18 | SUBSCRIPTION_CANCELLATION_SCHEDULED | Cancellation fuer zukuenftiges Datum geplant | Datum tracken |
| 19 | (reserviert) | — | — |
| 20 | SUBSCRIPTION_PENDING_PURCHASE_CANCELED | PENDING-Kauf abgebrochen (z.B. Bargeld nicht eingezahlt) | Pending-State aufraeumen |
| 21 | (reserviert) | — | — |
| 22 | SUBSCRIPTION_PRICE_STEP_UP_CONSENT_UPDATED | Consent fuer Preiserhoehung aktualisiert | Consent-Status aktualisieren |

**Goldene Regel:** RTDN sind nur Trigger. Nach JEDER RTDN MUSS das Backend `purchases.subscriptionsv2.get` aufrufen um den verbindlichen State zu bekommen. RTDN-Inhalt allein ist nicht autorativ.

## PurchaseState (auf Client-Seite)

Wenn der Client `Purchase`-Objekte verarbeitet:

| State | Konstante | Bedeutung | Premium-Grant? |
|-------|-----------|-----------|---------------|
| 0 | UNSPECIFIED_STATE | Sollte nie passieren | NEIN |
| 1 | PURCHASED | Kauf erfolgreich, Sub aktiv | JA — `acknowledgePurchase` PFLICHT binnen 3 Tagen |
| 2 | PENDING | Zahlung ausstehend (Bargeld, Familienkauf-Zustimmung) | NEIN — Warte-UI |

## BillingResponseCode (Fehler-Codes)

Codes die `onPurchasesUpdated` und andere Callbacks zurueckgeben:

| Code | Konstante | Bedeutung | UI-Reaktion |
|------|-----------|-----------|------------|
| 0 | OK | Erfolg | Premium-Grant + Acknowledge |
| 1 | USER_CANCELED | User hat Kauf abgebrochen | Bleiben auf Paywall, kein Toast |
| 2 | SERVICE_UNAVAILABLE | Play Service down | Retry-Mechanismus |
| 3 | BILLING_UNAVAILABLE | Billing nicht verfuegbar (z.B. China) | Error-Screen mit Erklaerung |
| 4 | ITEM_UNAVAILABLE | Produkt nicht verfuegbar | Error-Screen, Produkt-Liste neu laden |
| 5 | DEVELOPER_ERROR | Ungueltige Argumente | Bug — loggen + Crashlytics |
| 6 | ERROR | Allgemeiner Fehler | Retry oder Error-Screen |
| 7 | ITEM_ALREADY_OWNED | User besitzt schon | Recovery: queryPurchasesAsync + acknowledge |
| 8 | ITEM_NOT_OWNED | Beim Konsumieren — User besitzt nicht | (selten relevant fuer Subs) |
| -1 | SERVICE_DISCONNECTED | BillingClient disconnected | Reconnect mit startConnection |
| -2 | FEATURE_NOT_SUPPORTED | Geraet unterstuetzt nicht | Feature-Disable |
| -3 | SERVICE_TIMEOUT | Timeout | Retry |

## Audit-Pruefraster: State-Coverage

Pflicht-Audit fuer jede App: Pro State pruefen ob er im Code behandelt wird.

```markdown
| State | Code-Behandlung gefunden | Datei:Zeile | UI-Verhalten | Status |
|-------|------------------------|------------|-------------|--------|
| ACTIVE | JA | SubscriptionStatusService.kt:45 | Premium-UI komplett | OK |
| IN_GRACE_PERIOD | JA | SubscriptionStatusService.kt:62 | Banner gezeigt | OK |
| ON_HOLD | JA | SubscriptionStatusService.kt:78 | Sperr-Screen gezeigt | OK |
| PAUSED | NEIN | — | (keine UI) | KRITISCH FEHLT |
| CANCELED | JA | SubscriptionStatusService.kt:91 | Banner mit expiryTime | OK |
| EXPIRED | JA | SubscriptionStatusService.kt:104 | Paywall mit Win-Back | OK |
| PENDING | TEILWEISE | BillingManager.kt:155 | Toast "Zahlung wird geprueft" | UNVOLLSTAENDIG |
```

## Quervergleich: subscriptionsv2.get-Felder

Wichtigste Felder die der Server zurueckliefert (Google Play Developer API):

```
SubscriptionPurchaseV2 {
  lineItems[].productId                       // welches Produkt
  lineItems[].expiryTime                      // wann laeuft es ab
  lineItems[].autoRenewingPlan.autoRenewEnabled  // verlaengert sich automatisch?
  lineItems[].prepaidPlan.allowExtendAfterTime   // prepaid-Verlaengerung?
  lineItems[].offerDetails.basePlanId         // welcher Base-Plan
  lineItems[].offerDetails.offerId            // welcher Offer (Trial/Promo)
  lineItems[].offerDetails.offerTags          // Tags
  
  subscriptionState                           // ACTIVE/CANCELED/etc.
  
  canceledStateContext {                      // bei CANCELED nur:
    userInitiatedCancellation {
      cancelTime
      cancelSurveyResult { reason, reasonUserInput }
    }
    systemInitiatedCancellation { ... }       // Payment Failure
    developerInitiatedCancellation { ... }
    replacementCancellation { ... }            // bei Plan-Wechsel
  }
  
  pausedStateContext { autoResumeTime }       // bei PAUSED
  
  acknowledgementState                        // ACKNOWLEDGED / UNACKNOWLEDGED
  
  linkedPurchaseToken                         // Win-Back-Signal
  
  externalAccountIdentifiers {
    obfuscatedExternalAccountId
    obfuscatedExternalProfileId
  }
  
  testPurchase                                // Sandbox?
  
  paused {                                    // Pause-Details
    autoResumeTime
  }
}
```

## Cancel-Survey-Reasons (UI-Anforderung)

Wenn die App einen eigenen Cancel-Survey vor dem Cancel-Klick zeigt, sind diese Reasons typisch:

```
- TOO_EXPENSIVE (cancelSurveyReason = 0)
- DONT_USE_IT_ENOUGH (1)
- TECHNICAL_ISSUES (2)
- NEW_BETTER_APP_FOUND (3)
- CONTENT_NOT_USEFUL (4)
- OTHERS (5, mit reasonUserInput Freitext)
```

Im Audit pruefen:
- Werden Reasons an das Backend uebermittelt? (DSGVO-Hinweis erforderlich)
- Werden sie nur in Firebase Analytics geloggt? (Auch DSGVO-relevant)
- Wird Free-Text gespeichert?

## Pflicht-UI-Snippets pro State

Jede dieser Komponenten sollte in einer modernen App existieren:

```kotlin
@Composable fun PremiumBadge() // ACTIVE
@Composable fun GracePeriodBanner(daysLeft: Int, onFixPayment: () -> Unit) // IN_GRACE_PERIOD
@Composable fun AccountHoldScreen(onFixPayment: () -> Unit) // ON_HOLD
@Composable fun PausedSubscriptionInfo(autoResumeAt: Date) // PAUSED
@Composable fun CancellationScheduledBanner(expiryDate: Date, onReactivate: () -> Unit) // CANCELED
@Composable fun WinBackPaywall(linkedPurchaseToken: String) // EXPIRED + linkedPurchaseToken
@Composable fun PendingPurchaseWaiting() // PENDING
```

Im Audit pruefen ob fuer jeden State eine UI-Komponente existiert.

## Quellen

- [Subscription lifecycle | Android Developers](https://developer.android.com/google/play/billing/lifecycle/subscriptions)
- [RTDN Reference | Android Developers](https://developer.android.com/google/play/billing/rtdn-reference)
- [purchases.subscriptionsv2 | Play Developer API](https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2)
- [Migrate to Billing Library 8 (Juni 2025) | Android Developers](https://developer.android.com/google/play/billing/migrate-gpblv8)
- [Migrate to Billing Library 7 (Vorgaenger, Archiv)](https://developer.android.com/google/play/billing/migrate-gpblv7)
- [RevenueCat — Play Billing 8 Migration Guide](https://www.revenuecat.com/blog/engineering/play-billing-8-migration/)
- [Google Play Subscription Policy](https://support.google.com/googleplay/android-developer/answer/9900533)
