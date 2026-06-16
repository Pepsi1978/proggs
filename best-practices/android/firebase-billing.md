# Firebase & Google Play Billing — Best Practices

**Stand:** 2026-06-02 (Best-Practices-Recherchelauf, 7 Researcher, offizielle Quellen zuerst).
**Versions-Anker (live ermittelt aus `~/proggs/BestJournalAndroid/gradle/libs.versions.toml`):**
- **Google Play Billing Library 7.1.1** (`com.android.billingclient:billing-ktx`)
- **Firebase BOM 34.11.0** — eingebunden: `firebase-ai` (Gemini), App Check (Play Integrity + Debug),
  Remote Config, Analytics, Cloud Functions
- **google-services Gradle-Plugin 4.4.2**, AGP 8.7.3, Kotlin 2.1.0, compileSdk/targetSdk 36 (Android 16),
  minSdk 26, **R8/Minify im Release aktiv**.
- **Proaktiv (noch NICHT eingebunden):** Crashlytics, FCM/Cloud Messaging, Firestore — die Teile 3, 4 und 5
  sind Zukunftswissen fuer den spaeteren Einbau und entsprechend markiert.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/android/firebase-billing.md`](../../bugs/android/firebase-billing.md)):
> der Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von vornherein
> richtig macht, damit der Bug gar nicht erst entsteht*. Quellen-Rangordnung: offizielle Google/Firebase-Quelle
> (developer.android.com, developers.google.com/android-publisher, firebase.google.com) = Grundwahrheit
> (`offiziell`); Community/Engineering-Blogs (RevenueCat, Adapty) = `extern` (sekundaer, ueberstimmt nie das
> Offizielle). Jeder Punkt traegt sein `offiziell`/`extern`-Label + Quelle.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | BillingClient bauen | Genau EIN App-Scope-Singleton, ein zentraler Listener | §1 |
| 2 | Client konfigurieren | `enablePendingPurchases(PendingPurchasesParams…)` Pflicht ab v7 | §1 |
| 3 | Verbindung pflegen | Lazy-Reconnect via `isReady()`, nicht im Disconnect-Callback | §1 |
| 4 | Kauf-Flow starten | Main-Thread, `offerToken` bei Abos, `setObfuscatedAccountId()` | §1 |
| 5 | `onPurchasesUpdated` | Nur `PURCHASED` freischalten, `PENDING` abwarten, null-safe | §1 |
| 6 | Kauf gewaehren | Binnen 3 Tagen idempotent acknowledgen; `consumeAsync` nur Consumables | §1 |
| 7 | App-Start/`onResume` | `queryPurchasesAsync` (SUBS+INAPP) zur Recovery | §1 |
| 8 | Entitlement-Wahrheit | Server ist Source of Truth, Client nur Anzeige | §2 |
| 9 | Token verifizieren | `purchases.subscriptionsv2.get` (NICHT v1) | §2 |
| 10 | RTDN verarbeiten | Idempotent per `messageId`, nach jeder Notification frischer `get` | §2 |
| 11 | Plan-Wechsel/Resignup | `linkedPurchaseToken` auswerten, kein Doppel-Entitlement | §2 |
| 12 | Firebase-Deps | Alle ueber BOM ohne Version; kein `-ktx` ab BOM 34 | §7 |
| 13 | App Check init | ZUERST in `Application.onCreate()`, vor allen Diensten | §6 |
| 14 | App Check Enforcement | Gestaffelt: erst Metriken, dann pro Dienst | §6 |
| 15 | Remote Config | XML-Defaults, nicht beim Start blockieren, Prod-Intervall ~12h | §6 |
| 16 | SHA-Keys | Debug+Upload+Play-App-Signing in Firebase, sonst Release still kaputt | §7 |
| 17 | DSGVO | Consent Mode v2: Default-Consent VOR Init (Manifest), keine PII | §7 |

---

## Querverweis: Bug-Almanach ↔ Best-Practice (beide Richtungen)

| Best-Practice-Abschnitt (diese Datei) | Bug-Almanach-Abschnitt (`bugs/android/firebase-billing.md`) |
|---------------------------------------|------------------------------------------------------|
| Teil 1 — Billing Client-Flows | Teil 1 (Bugs 1–28: Verbindung, Acknowledge, PENDING, Proration) |
| Teil 1 D + Teil 2 — Sicherheit/Server | Bugs 29–41 (Signature-Verifikation, RTDN, Developer API, Deadline) |
| Teil 2 — serverseitige Validierung | Bugs 29–37 + Teil 11 (Nachträge: Refund-Revoke, RTDN-Dedup, Replay-Schutz) |
| Teil 3 — Crashlytics | Teil 3 (Bugs 53–75) + Teil 9 (R8-Keep 113–119) |
| Teil 4 — FCM | Teil 4 (Bugs 76–82) + Teil 11 (FLAG_IMMUTABLE, Legacy-Key) |
| Teil 5 — Firestore Security Rules | Teil 12 (Firestore, proaktiv — neu) |
| Teil 6 — App Check / Remote Config / Functions | Teil 5–7 (Bugs 83–96) + Teil 11 (limited-use-Token, RC-Block) |
| Teil 7 — Setup / Init / BOM / Analytics / Consent | Teil 2 (Bugs 42–52) + Teil 11 (Consent-Mode-v2) |

---

# Teil 1 — Google Play Billing: sichere Client-Flows (7.1.1)

> Versions-Anker: `com.android.billingclient:billing-ktx` **7.1.1**, Kotlin 2.1.0 + Coroutines, minSdk 26, target/compileSdk 36, R8 im Release aktiv. Hinweise mit "(v8)" zeigen, was Library 8 zusaetzlich/anders empfiehlt — aber primaer fuer 7.x geschrieben.

## A. BillingClient-Architektur & Lebenszyklus

- **Genau EINE BillingClient-Instanz (App-Scope-Singleton):** Niemals pro Activity/Fragment instanziieren. Mehrere Instanzen fuehren zu mehrfachen `onPurchasesUpdated`-Callbacks fuer dasselbe Kauf-Ereignis. Den Client an einen Application-Scope-Lebenszyklus binden (z.B. Hilt `@Singleton` oder Repository im Application-Scope), NICHT an eine Activity. (`offiziell`, Quelle: https://developer.android.com/reference/com/android/billingclient/api/BillingClient)

- **Application-Context verwenden, nicht Activity-Context:** Beim Bauen `BillingClient.newBuilder(applicationContext)` nutzen, damit der Singleton keine Activity-Referenz haelt (Memory-Leak-Vermeidung). Den Activity-Context braucht man nur fuer `launchBillingFlow(activity, …)`. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Zentraler PurchasesUpdatedListener:** Den Listener EINMAL zentral am Singleton registrieren (`setListener(...)`). Ergebnisse intern als Coroutine/Flow weiterreichen (z.B. `MutableSharedFlow<Purchase>`), statt pro Aufrufer eigene Listener anzuhaengen. So bleibt nur eine Quelle der Wahrheit fuer Kauf-Updates. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **`enablePendingPurchases(...)` ist ab v7 PFLICHT beim Bauen:** Ohne diesen Aufruf baut der Client nicht korrekt. Ab v7 wird die Variante mit `PendingPurchasesParams` empfohlen (`enablePendingPurchases(PendingPurchasesParams.newBuilder()…build())`), damit auch Prepaid-Plaene/Pending-Transaktionen sauber abgebildet werden.
  ```kotlin
  val billingClient = BillingClient.newBuilder(applicationContext)
      .setListener(purchasesUpdatedListener)
      .enablePendingPurchases(
          PendingPurchasesParams.newBuilder()
              .enableOneTimeProducts()   // v7: explizit fuer Einmalkaeufe
              .build()
      )
      .build()
  ```
  (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **`billing-ktx` suspend-APIs nutzen statt Callbacks:** Mit der KTX-Variante gibt es suspend-Wrapper wie `queryProductDetails(params)` und `queryPurchasesAsync(params)`, die sich sauber in Coroutines/Flow einfuegen. Netz-/IO-lastige Calls auf `Dispatchers.IO` ausfuehren, das eigentliche `launchBillingFlow` aber vom Main-Thread (siehe B). (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

### Verbindungs-Lebenszyklus

- **Setup mit `startConnection` — Methoden erst NACH `onBillingSetupFinished(OK)` aufrufen:** Vor abgeschlossenem Setup darf kein anderer Call (Produkte/Query/Flow) erfolgen. Produkte erst im erfolgreichen `onBillingSetupFinished`-Callback laden.
  ```kotlin
  billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(r: BillingResult) {
          if (r.responseCode == BillingResponseCode.OK) queryProductDetails()
      }
      override fun onBillingServiceDisconnected() { /* siehe Reconnect */ }
  })
  ```
  (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Lazy-Reconnect: vor jedem Call `isReady()` pruefen, statt nur im Disconnect-Callback zu reagieren:** Ein verlaesslicher Pattern ist „Connection on demand" — vor jeder Operation pruefen, ob der Client `isReady()` ist, und sonst zuerst eine (Re)Connection herstellen. Den Disconnect-Callback NICHT als alleinigen Reconnect-Trigger missbrauchen. (`offiziell`, Quelle: https://developer.android.com/reference/com/android/billingclient/api/BillingClient)

- **Eigene Retry-/Backoff-Strategie bei v7:** Offiziell empfohlen ist eine eigene Connection-Retry-Policy mit `onBillingServiceDisconnected()`-Override und exponential Backoff fuer Hintergrund-Operationen (Acknowledge etc.), die nicht direkt die UX betreffen. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/errors)

- **(v8) `enableAutoServiceReconnection()`:** Ab Library 8 gibt es den Builder-Parameter `enableAutoServiceReconnection()`, der Reconnects automatisch erledigt — dann kann `onBillingServiceDisconnected()` als No-Op implementiert werden und manuelles `startConnection()` beim Disconnect entfaellt. Auf 7.1.1 noch nicht verfuegbar → dort weiter eigene Retry-Logik. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/migrate-gpblv8)

### Produkte laden

- **SUBS und INAPP getrennt abfragen:** `queryProductDetailsAsync` (bzw. KTX `queryProductDetails`) pro `ProductType` aufrufen — eine Query fuer `SUBS`, eine fuer `INAPP`. ProductDetails fuer die Paywall cachen, aber bei jeder Anzeige frisch validieren (Preise/Angebote koennen sich aendern). (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Paywall robust bauen:** Erst rendern, wenn ProductDetails wirklich geladen sind; bei leerem Ergebnis (kein Produkt verfuegbar, Land/Account-Problem) einen klaren Fallback-Zustand zeigen statt Preis „0,00" oder Crash. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

## B. Kauf-Flow & onPurchasesUpdated

- **`launchBillingFlow` korrekt vom Main-Thread (`@UiThread`) aufrufen:** Mit `BillingFlowParams` → Liste von `ProductDetailsParams`. Den Rueckgabe-`BillingResult` direkt pruefen (nicht alle Fehler kommen erst im Listener).
  ```kotlin
  val params = BillingFlowParams.ProductDetailsParams.newBuilder()
      .setProductDetails(productDetails)
      .setOfferToken(selectedOfferToken)  // siehe naechster Punkt
      .build()
  val flowParams = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(listOf(params))
      .build()
  val result = billingClient.launchBillingFlow(activity, flowParams) // Main-Thread!
  ```
  (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **`offerToken` ist bei Abos Pflicht:** Fuer SUBS den `offerToken` aus `ProductDetails.getSubscriptionOfferDetails()` setzen (der konkret gewaehlten Base-Plan-/Offer-Kombination). Bei Einmalkaeufen kommt das Angebot aus `getOneTimePurchaseOfferDetails()`. Falscher/fehlender Token → Flow scheitert. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Fraud-Schutz: `setObfuscatedAccountId` / `setObfuscatedProfileId` setzen:** Beim `BillingFlowParams`-Bauen die (obfuskierte, NICHT die echte) Account-/Profil-ID des Nutzers mitgeben. Google nutzt das zur Betrugserkennung (z.B. viele Geraete auf einem Account in kurzer Zeit). Niemals personenbezogene Klartext-IDs verwenden.
  ```kotlin
  BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(listOf(params))
      .setObfuscatedAccountId(hashedAccountId)
      .setObfuscatedProfileId(hashedProfileId)
      .build()
  ```
  (`offiziell`, Quelle: https://developer.android.com/google/play/billing/security)

- **`onPurchasesUpdated` strikt nach responseCode + PurchaseState verzweigen:**
  ```kotlin
  override fun onPurchasesUpdated(r: BillingResult, purchases: List<Purchase>?) {
      when {
          r.responseCode == BillingResponseCode.OK && purchases != null ->
              purchases.forEach { p ->
                  when (p.purchaseState) {
                      Purchase.PurchaseState.PURCHASED -> verifyThenGrantThenAck(p)
                      Purchase.PurchaseState.PENDING   -> persistPending(p) // NICHT freischalten
                      else -> { /* UNSPECIFIED_STATE: ignorieren */ }
                  }
              }
          r.responseCode == BillingResponseCode.USER_CANCELED -> { /* still, kein Fehlerdialog */ }
          else -> handleError(r) // ggf. retry
      }
  }
  ```
  Nur `PURCHASED` schaltet frei. `PENDING` speichern und auf den Uebergang zu `PURCHASED` warten (kommt spaeter via `onPurchasesUpdated` oder bei `queryPurchasesAsync`). `USER_CANCELED` still behandeln (kein Fehler-Toast). Immer null-safe gegen `purchases == null`. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Pending-Kaeufe nie als Verkauf werten:** Fuer PENDING kein Entitlement, kein Acknowledge — der 3-Tage-Acknowledge-Timer startet erst beim Uebergang PENDING→PURCHASED. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/lifecycle/one-time)

## C. Acknowledge / Consume / Recovery

- **Acknowledge binnen 3 Tagen — sonst Auto-Refund:** Jeder PURCHASED-Kauf (Abos und Nicht-Consumables) muss binnen 3 Tagen acknowledged werden, sonst storniert Google automatisch und der Nutzer bekommt sein Geld zurueck (Entitlement geht verloren). Acknowledge unmittelbar NACH dem Gewaehren des Entitlements. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Idempotent acknowledgen — `isAcknowledged()` zuerst pruefen:** Nie doppelt acknowledgen.
  ```kotlin
  if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
      val ackParams = AcknowledgePurchaseParams.newBuilder()
          .setPurchaseToken(purchase.purchaseToken).build()
      billingClient.acknowledgePurchase(ackParams) { /* result pruefen, ggf. backoff-retry */ }
  }
  ```
  (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **`consumeAsync` NUR fuer Consumables — niemals fuer Abos/Nicht-Consumables:** Consume macht das Produkt wieder kaufbar; auf einem Abo/Entitlement angewandt wuerde es den Anspruch zerstoeren. Consume impliziert bereits Acknowledge — also fuer Consumables consume statt acknowledge.
  ```kotlin
  val consumeParams = ConsumeParams.newBuilder()
      .setPurchaseToken(purchase.purchaseToken).build()
  billingClient.consumeAsync(consumeParams) { r, _ -> /* bei OK: erneut kaufbar */ }
  ```
  (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Recovery beim App-Start / `onResume`: `queryPurchasesAsync` aufrufen:** Kaeufe, die abgeschlossen wurden waehrend die App nicht lief (oder ein Acknowledge fehlschlug), per `queryPurchasesAsync(...)` (je ProductType) einsammeln und nachverarbeiten — unacknowledgte Kaeufe nachtraeglich acknowledgen, fehlendes Entitlement nachreichen. Dies ist die wichtigste Schutzschicht gegen verlorene Kaeufe. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/integrate)

- **Nach `ITEM_NOT_OWNED` / vor Re-Kauf `queryPurchasesAsync` checken:** Bei `ITEM_NOT_OWNED` zuerst pruefen, ob der Nutzer das Produkt nicht doch besitzt, bevor ein Re-Kauf (mit simpler Retry-Logik) gestartet wird — verhindert Doppelkaeufe. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/errors)

- **Hintergrund-Operationen mit exponential Backoff:** Acknowledge/Consume/Recovery passieren oft im Hintergrund — bei transienten Fehlern (Netz, SERVICE_UNAVAILABLE) exponential Backoff statt sofortiger Endlos-Retries. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/errors)

- **Client-Entitlement nur als „schnelle Anzeige", Wahrheit serverseitig:** Ein clientseitiger Entitlement-Cache (z.B. DataStore) ist OK fuer sofortiges UI-Feedback, darf aber NIE die alleinige Quelle sein — der Client ist manipulierbar. Verifikation/Acknowledge serverseitig via Play Developer API (das Server-Thema deckt Researcher 2 ab; hier nur der Grundsatz). (`offiziell`, Quelle: https://developer.android.com/google/play/billing/security)

## D. Testing

- **Lizenz-Tester verwenden (kostenlose Test-Kaeufe):** In der Play Console „License testing" Accounts eintragen — diese koennen den vollen Kauf-Flow inkl. Abos durchspielen, ohne echt zu zahlen, und Test-Renewals/Cancellations werden beschleunigt. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/test)

- **Gegen einen veroeffentlichten Track testen (Internal Testing), Release-AAB:** Billing funktioniert nur mit einer ueber die Play Console verteilten, korrekt signierten App (gleiche applicationId, hochgeladenes AAB). Billing/Paywall daher mit `bundleRelease` ueber Internal Testing testen — KEIN reines lokales Debug-APK. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/test)

- **Pending-Purchases gezielt testen:** Mit den Test-Instrumenten (z.B. Test-Karte „langsame Genehmigung") den PENDING→PURCHASED-Pfad und die `queryPurchasesAsync`-Recovery explizit durchspielen, damit Pending-Kaeufe nicht versehentlich sofort freischalten. (`offiziell`, Quelle: https://developer.android.com/google/play/billing/test)

---

# Teil 2 — Billing: serverseitige Validierung (Cloud Function + Play Developer API + RTDN)

> Best-Practices fuer BestJournalAndroid (Billing 7.1.1, Abos + evtl. Einmalkaeufe,
> `firebase-functions` callable vorhanden). Fokus: WIE man es richtig macht.
> Jeder Punkt mit Label `offiziell`/`extern` + Quelle.

---

## A. Grundprinzip: Server als Source of Truth

- **A1 — Client niemals vertrauen.** Der `purchaseToken` aus der App ist nur ein Trigger. Die
  autoritative Entitlement-Wahrheit lebt im Backend, nicht im Client. `queryPurchasesAsync()`
  zeigt nur den Geraete-Zustand und liefert z. B. **ON_HOLD nicht** (siehe E/H). `offiziell` —
  developer.android.com/google/play/billing/lifecycle/subscriptions
- **A2 — Verifikationsfluss (Do).** App kauft → uebergibt `purchaseToken` an die Cloud Function →
  Function ruft Play Developer API auf → bei `ACTIVE`/gueltig Entitlement in Firestore/RTDB
  schreiben → App liest Entitlement aus dem Backend, nicht aus eigenem Client-Cache. `offiziell` —
  developer.android.com/google/play/billing/lifecycle/subscriptions
- **A3 — Entitlement-Store.** Pro Nutzer einen Entitlement-Datensatz im Backend (Firestore/RTDB):
  `productId`, `subscriptionState`, `expiryTime`, `purchaseToken`, `acknowledged`, `linkedFrom`.
  Dieser Datensatz ist die Quelle fuer "ist Premium aktiv?", nicht der Client. `offiziell` —
  developer.android.com/google/play/billing/lifecycle/subscriptions
- **A4 — `subscriptionsv2.get` ist die Source of Truth.** Wortlaut Google: die v2-`get`-API mit dem
  `purchaseToken` aus der Notification "is considered the source of truth for subscription
  management". `offiziell` — developer.android.com/google/play/billing/lifecycle/subscriptions

---

## B. Play Developer API (subscriptionsv2.get, products.get/acknowledge)

- **B1 — Abos: `purchases.subscriptionsv2.get` (NICHT v1).** Endpoint:
  `GET androidpublisher/v3/applications/{packageName}/purchases/subscriptionsv2/tokens/{token}`.
  Das veraltete v1 `purchases.subscriptions.get` "should not be used for new integrations".
  `offiziell` — developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2/get
- **B2 — SubscriptionPurchaseV2-Modell.** Relevante Felder: `subscriptionState`, `latestOrderId`,
  `acknowledgementState`, `linkedPurchaseToken`, `externalAccountIdentifiers`
  (enthaelt `obfuscatedExternalAccountId`, `obfuscatedExternalProfileId`), `lineItems[]`,
  `regionCode`, `startTime`. `offiziell` — gleiche URL
- **B3 — `lineItems[]` (base plans / offers).** Pro Line-Item: `productId`, `expiryTime`,
  `autoRenewingPlan` (mit `autoRenewEnabled`), `offerDetails` (`basePlanId`, `offerId`, Tags),
  `offerPhase`. Auswertung pro Line-Item, nicht pauschal auf Abo-Ebene. `offiziell` — gleiche URL
- **B4 — `subscriptionState`-Werte.** `SUBSCRIPTION_STATE_ACTIVE`, `_IN_GRACE_PERIOD`, `_ON_HOLD`,
  `_PAUSED`, `_CANCELED`, `_EXPIRED`, `_PENDING`. Entitlement nur bei ACTIVE / IN_GRACE_PERIOD
  / CANCELED-aber-noch-nicht-abgelaufen gewaehren. `offiziell` — gleiche URL
- **B5 — Einmalkaeufe: `purchases.products.get` + `purchases.products.acknowledge`.** Fuer
  In-App-Produkte (kein Abo) separat per Products-API verifizieren und (falls non-consumable)
  serverseitig acknowledgen. `offiziell` — developers.google.com/android-publisher (Products-Ref)
- **B6 — OAuth-Scope.** Alle Aufrufe brauchen den Scope
  `https://www.googleapis.com/auth/androidpublisher`. `offiziell` —
  developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2/get
- **B7 — `expiryTime` ist dynamisch.** Bei Grace Period verlaengert sich `expiryTime` automatisch.
  Immer den Wert aus der API gegen `now` pruefen, nie einen alten Cache-Wert. `offiziell` —
  developer.android.com/google/play/billing/lifecycle/subscriptions

---

## C. RTDN ueber Pub/Sub → Cloud Function

- **C1 — RTDN ist nur ein Trigger.** Eine Notification sagt "etwas hat sich geaendert" + traegt
  `purchaseToken`. Danach IMMER `subscriptionsv2.get` aufrufen, um den echten Zustand zu holen —
  niemals den Notification-Typ allein als Wahrheit nehmen. `offiziell` —
  developer.android.com/google/play/billing/rtdn-reference
- **C2 — Pub/Sub-Topic + Publisher-Recht.** Pub/Sub-Topic anlegen, in Play Console als RTDN-Topic
  eintragen, und dem System-Service-Account
  `google-play-developer-notifications@system.gserviceaccount.com` die **Publish**-Rolle auf das
  Topic geben. `offiziell` — developer.android.com/google/play/billing/rtdn-reference
- **C3 — Push-Subscription auf HTTPS-Cloud-Function.** Push-Subscription des Topics auf den
  HTTPS-Endpoint einer Cloud Function zeigen lassen. Nachricht kommt im Pub/Sub-Wrapper
  (`message.data` ist base64 → dekodieren zur `DeveloperNotification`). `offiziell` — gleiche URL
- **C4 — DeveloperNotification-Struktur.** Felder: `version`, `packageName`, `eventTimeMillis`,
  + genau eines von `subscriptionNotification` / `oneTimeProductNotification` /
  `voidedPurchaseNotification` / `testNotification`. `offiziell` — gleiche URL
- **C5 — `notificationType`-Werte (Subscription).** 1 RECOVERED, 2 RENEWED, 3 CANCELED,
  4 PURCHASED, 5 ON_HOLD, 6 IN_GRACE_PERIOD, 7 RESTARTED, 8 PRICE_CHANGE_CONFIRMED (deprecated),
  9 DEFERRED, 10 PAUSED, 11 PAUSE_SCHEDULE_CHANGED, 12 REVOKED, 13 EXPIRED,
  17 ITEMS_CHANGED, 18 CANCELLATION_SCHEDULED, 19 PRICE_CHANGE_UPDATED,
  20 PENDING_PURCHASE_CANCELED, 22 PRICE_STEP_UP_CONSENT_UPDATED. `offiziell` — gleiche URL
- **C6 — OneTimeProductNotification.** Typen: 1 ONE_TIME_PRODUCT_PURCHASED,
  2 ONE_TIME_PRODUCT_CANCELED (mit `sku` + `purchaseToken`). `offiziell` — gleiche URL
- **C7 — Idempotenz per `messageId`.** Google: "Check message ID uniqueness to avoid duplicate
  processing and optimize API quota usage." Verarbeitete `messageId` (oder `purchaseToken` +
  `eventTimeMillis`) im Backend speichern und Duplikate verwerfen. `offiziell` — gleiche URL
- **C8 — Out-of-order-Handling (Do).** Pub/Sub liefert nicht garantiert in Reihenfolge. Da der
  echte Zustand ohnehin per `subscriptionsv2.get` geholt wird, immer den FRISCH geholten Zustand
  schreiben statt naiv den Notification-Typ; alte Events erkennt man am `eventTimeMillis` <
  zuletzt verarbeitet. `offiziell` (abgeleitet aus C1/C7) — gleiche URL
- **C9 — Pub/Sub-Push schnell ack'en.** Die Function muss schnell 2xx zurueckgeben, sonst
  re-delivered Pub/Sub (→ erneut Duplikat). Schwere Arbeit idempotent halten. `extern`
  (Pub/Sub-Push-Verhalten) — cloud.google.com/pubsub/docs/push
- **C10 — TestNotification.** Play Console kann eine `testNotification` (`{ "version": "1.0" }`)
  senden — zum Verifizieren des Topic→Function-Pfads vor Go-Live nutzen. `offiziell` — gleiche URL

---

## D. Refunds, linkedPurchaseToken, Account-Mapping

- **D1 — Voided Purchases API.** `purchases.voidedpurchases.list` liefert voided Orders aus
  Refunds, User-Cancellations, Chargebacks, Developer-Refund-mit-Revoke, Google-initiierten
  Refunds. Felder: `voidedTimeMillis`, `voidedSource` (0=user, 1=developer, 2=Google),
  `voidedReason`, `purchaseToken`, `orderId`. **Achtung:** Refunds OHNE "revoke"-Option tauchen
  NICHT auf. `offiziell` — developers.google.com/android-publisher/voided-purchases
- **D2 — Refund-Erkennung: RTDN statt Polling.** Bevorzugt `voidedPurchaseNotification` via RTDN
  (`productType` 1=Sub/2=OneTime, `refundType` 1=Full/2=Partial) statt taeglichem Polling. Bei
  Erhalt Entitlement sofort entziehen. Polling-Quota: 6.000/Tag, 30 pro 30 s, max 1.000
  Results/Response. `offiziell` — developers.google.com/android-publisher/voided-purchases +
  developer.android.com/google/play/billing/rtdn-reference
- **D3 — `linkedPurchaseToken` bei Resignup (nach Ablauf).** Bei `SUBSCRIPTION_PURCHASED` nach
  vorherigem Ablauf enthaelt die v2-Response `linkedPurchaseToken` (alter Token) — den alten
  Nutzer-Account darueber finden, neuen Token zuordnen, **kein zweites Entitlement** anlegen.
  Zusatzfeld `outOfAppPurchaseContext.expiredExternalAccountIdentifiers` hilft beim Mapping.
  `offiziell` — developer.android.com/google/play/billing/lifecycle/subscriptions
- **D4 — `linkedPurchaseToken` bei Upgrade/Downgrade.** Plan-Wechsel erzeugt NEUEN Token, alter
  Token im `linkedPurchaseToken`. Backend muss das alte Entitlement migrieren/abloesen statt
  beides aktiv zu lassen. `offiziell` — gleiche URL
- **D5 — Restore (vor Ablauf) hat KEINEN linkedPurchaseToken.** "Resubscribe vor Ablauf" =
  gleicher Token, `SUBSCRIPTION_RESTARTED`, Cancellation-Felder werden geleert — nicht mit
  Resignup verwechseln. `offiziell` — gleiche URL
- **D6 — `obfuscatedAccountId` setzen (Do).** Beim Kauf in der App
  `setObfuscatedAccountId(userIdHash)` am `BillingFlowParams` setzen, damit die v2-Response den
  Nutzer ueber `externalAccountIdentifiers.obfuscatedExternalAccountId` zuordbar macht (nie die
  echte E-Mail/User-ID, nur ein Hash). `offiziell` (Account-Identifiers in v2-Response) —
  developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2/get

---

## E. Cloud Function als Verifikations-Endpoint (App Check, Secrets, Region)

- **E1 — App Check auf der callable Function erzwingen.** Den `verifyPurchase`-Callable so
  konfigurieren, dass nur Requests mit gueltigem App-Check-Token durchkommen
  (`enforceAppCheck: true` bei v2-`onCall`). Schuetzt den Verifikations-Endpoint vor Missbrauch
  durch fremde Clients. `offiziell` — firebase.google.com/docs/app-check
- **E2 — Replay-Schutz (limited-use Tokens) fuer Kauf-Verifikation.** Fuer sensible Operationen
  consumable/limited-use App-Check-Tokens nutzen: `verifyToken(token, { consume: true })` bzw.
  `consumeAppCheckToken: true` — `alreadyConsumed` → 401. Google empfiehlt Replay-Schutz
  ausdruecklich nur fuer "particularly sensitive endpoints" (Kauf-Verifikation zaehlt dazu), da
  ein Netzwerk-Roundtrip Latenz kostet. `offiziell` —
  firebase.google.com/docs/app-check/custom-resource-backend
- **E3 — Service-Account-Key sicher verwahren.** Den Play-API-Service-Account-Key NICHT ins Repo
  und nicht in den Function-Code hardcoden. In Cloud Functions als Secret (Secret Manager /
  `defineSecret`) hinterlegen; Google: Credentials "securely managed so they are not revealed to
  anyone that is not authorized". (Projektregel hier: Keys leben in `$HOME/SK/`, im Backend in
  Secret Manager.) `offiziell` — developers.google.com/android-publisher/getting_started
- **E4 — Region setzen.** Function-Region explizit setzen (z. B. naher Pub/Sub-/Firestore-Region)
  fuer geringe Latenz und stabile Pub/Sub-Push-Zustellung. `extern`/`offiziell` —
  firebase.google.com/docs/functions/locations
- **E5 — Fehlerbehandlung.** Bei API-Fehlern (Quota, 5xx, transient) NICHT blind Entitlement
  entziehen — retry/backoff, Zustand erst nach erfolgreichem `get` schreiben. Pub/Sub re-delivered
  bei Non-2xx, daher fatale vs. transiente Fehler trennen. `extern` (abgeleitet) —
  cloud.google.com/pubsub/docs/push
- **E6 — Acknowledge serverseitig (empfohlen).** Google empfiehlt Purchase-Processing im Backend
  "for better security". Acknowledge serverseitig (`purchases.subscriptions.acknowledge` /
  `products.acknowledge`) statt client-`acknowledgePurchase()`, weil es App-Crash/Reinstall
  ueberlebt und das 3-Tage-Fenster auch ohne App-Oeffnen einhaelt. `offiziell` —
  developer.android.com/google/play/billing/lifecycle/subscriptions
- **E7 — 3-Tage-Acknowledge-Fenster.** Neue Kaeufe innerhalb von 3 Tagen acknowledgen, sonst
  automatischer Refund + Revoke. Prepaid-Plaene: enger (3 Tage bei >=1 Woche Laufzeit, sonst
  halbe Laufzeit). `offiziell` — gleiche URL
- **E8 — Service-Account-Setup (least privilege).** Cloud-Projekt anlegen, "Google Play Android
  Developer API" aktivieren, Service-Account erstellen, dessen E-Mail in Play Console unter
  Users & Permissions einladen und NUR die noetigen Rechte geben: "View financial data, orders,
  and cancellation survey responses" (+ ggf. "Manage orders and subscriptions"). Keine
  Admin-Vollrechte. `offiziell` — developers.google.com/android-publisher/getting_started
- **E9 — Entitlement-Store = Firestore/RTDB.** Pro Nutzer Entitlement-Doc; die App liest Premium
  daraus (Realtime-Sync). Server-RTDN-Updates propagieren so direkt zur App ohne erneuten
  Client-Verify. `offiziell` (Architektur) —
  developer.android.com/google/play/billing/lifecycle/subscriptions

---

## F. Lifecycle-Aktionen pro State (Schnell-Referenz, `offiziell`)

Quelle durchgehend: developer.android.com/google/play/billing/lifecycle/subscriptions

| Notification / State | Aktion | Access |
|---|---|---|
| PURCHASED / ACTIVE | acknowledgen (server), Entitlement gewaehren | grant |
| RENEWED | `expiryTime` aktualisieren | grant |
| IN_GRACE_PERIOD | Zugang BEHALTEN, Zahlungs-Hinweis (In-App-Messaging) | keep |
| ON_HOLD | Zugang SOFORT sperren (queryPurchasesAsync zeigt ON_HOLD NICHT) | revoke |
| RECOVERED | Zugang wiederherstellen (aus Hold/Pause) | grant |
| PAUSED | Zugang sperren bis Resume | revoke |
| PAUSE_SCHEDULE_CHANGED | nichts; Zugang bleibt bis Zyklusende | keep |
| CANCELED | `expiryTime` vs now pruefen (Zugang bis Ablauf) | conditional |
| EXPIRED | Entitlement entfernen | revoke |
| REVOKED | sofort entziehen (Chargeback/Dev-Revoke) | revoke |
| DEFERRED | `expiryTime` verlaengert, Zugang bleibt | keep |
| RESTARTED | Cancellation aufgehoben, Zugang bleibt | grant |

---

---

# Teil 3 — Firebase Crashlytics richtig einrichten (PROAKTIV — noch nicht eingebunden)

> Status: Crashlytics ist in BestJournalAndroid (Firebase BOM 34.11.0) NOCH NICHT eingebunden.
> Diese Sektion ist Zukunftswissen fuer den spaeteren Einbau. Relevant: sherpa-onnx native
> `.so`-Libs (NDK-Symbolisierung), R8/Minify im Release AKTIV, AGP 8.7.3, google-services 4.4.2.
> Versionen anchored an: BOM 34.11.0 (vorhanden), Crashlytics-Plugin 3.0.7, google-services 4.4.2.

## A. Einrichtung & Plugin-Reihenfolge

- **[offiziell]** Mindestversionen: Gradle 8.0+, AGP 8.1.0+, google-services-Plugin 4.4.1+.
  BestJournalAndroid (AGP 8.7.3, google-services 4.4.2) erfuellt das.
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-started
- **[offiziell]** Dependencies im Modul-`build.gradle.kts` — Crashlytics OHNE Version (ueber BOM),
  Analytics empfohlen (liefert Breadcrumbs, crash-free users, velocity alerts):
  ```kotlin
  dependencies {
      implementation(platform("com.google.firebase:firebase-bom:34.11.0")) // vorhandene BOM
      implementation("com.google.firebase:firebase-crashlytics")
      implementation("com.google.firebase:firebase-analytics")
  }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-started
- **[offiziell]** Crashlytics-Gradle-Plugin `com.google.firebase.crashlytics` — Version **3.0.7**
  (Stand Get-Started-Doku). Im Root-`build.gradle.kts` mit `apply false`, im Modul anwenden:
  ```kotlin
  // root build.gradle.kts
  plugins {
      id("com.android.application") version "..." apply false
      id("com.google.gms.google-services") version "4.4.2" apply false
      id("com.google.firebase.crashlytics") version "3.0.7" apply false
  }
  // module build.gradle.kts
  plugins {
      id("com.android.application")
      id("com.google.gms.google-services")
      id("com.google.firebase.crashlytics")
  }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-started
- **[offiziell]** Plugin-Reihenfolge im `plugins{}`-Block: `com.android.application` ZUERST,
  dann `com.google.gms.google-services`, dann `com.google.firebase.crashlytics`. Das
  Crashlytics-Plugin haengt am google-services-Plugin (braucht die verarbeitete google-services.json).
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-started
- **[offiziell]** Verifikation: Test-Crash erzwingen (`throw RuntimeException("Test Crash")`),
  App neu starten, dann Crashlytics-Dashboard in der Firebase-Console binnen ~5 Minuten pruefen.
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-started

## B. Mapping-Upload (R8-Deobfuskierung)

- **[offiziell]** Das Crashlytics-Gradle-Plugin erkennt Obfuskierung (R8/ProGuard/DexGuard)
  AUTOMATISCH und laedt die Mapping-Datei waehrend des Builds hoch — Default-Verhalten, sobald
  `minifyEnabled = true` ist. `mappingFileUploadEnabled` ist dann implizit `true`. Da
  BestJournalAndroid R8 im Release aktiv hat, klappt der Mapping-Upload OHNE Extra-Config —
  vorausgesetzt das Crashlytics-Plugin ist angewendet.
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports
- **[offiziell]** Mapping-Upload pro BuildType/Flavor abschalten (z.B. fuer Debug-Builds mit
  Minify, um Build zu beschleunigen) ueber die `CrashlyticsExtension`:
  ```kotlin
  buildTypes {
      getByName("debug") {
          isMinifyEnabled = true
          configure<CrashlyticsExtension> {
              mappingFileUploadEnabled = false  // obfuskierte Stacktraces im Release-Fall NICHT setzen
          }
      }
  }
  ```
  WICHTIG fuer BestJournal: Im **Release** NICHT auf `false` setzen — sonst sind die
  Stacktraces in der Console obfuskiert und unbrauchbar.
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports
- **[offiziell]** ProGuard/R8-Regeln zum Erhalt der Stacktrace-Lesbarkeit empfohlen:
  ```
  -keepattributes SourceFile,LineNumberTable
  -keep public class * extends java.lang.Exception
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports
- **[offiziell]** Bei nicht-standard Obfuskierung: `mappingFile`-Parameter setzt einen
  abweichenden Pfad zur Mapping-Datei (pro defaultConfig/BuildType/Flavor moeglich).
  Quelle (Suche, firebase.google.com): https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports
- **[offiziell]** Verifikation der Deobfuskierung: nach einem Release-Build pruefen, dass die
  `uploadCrashlyticsMappingFile<Variant>`-Task lief (sie haengt am `assemble<Variant>`); im
  Dashboard erscheinen dann lesbare (deobfuskierte) Stacktraces statt `a.b.c()`-Muell.
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports

## C. NDK / native Symbole (sherpa-onnx)

> KRITISCH fuer BestJournal: sherpa-onnx liefert native `.so`-Libs. Ohne NDK-Crashlytics +
> Symbol-Upload sind native Crashes (Segfaults in onnxruntime/sherpa) in der Console nur als
> rohe Adressen sichtbar — nicht symbolisiert.

- **[offiziell]** Zusaetzliche Dependency fuer native Crashes:
  ```kotlin
  dependencies {
      implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
      implementation("com.google.firebase:firebase-crashlytics-ndk")
      implementation("com.google.firebase:firebase-analytics")
  }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/ndk-reports
- **[offiziell]** Native Symbol-Verarbeitung aktivieren ueber die `CrashlyticsExtension` im
  Release-BuildType:
  ```kotlin
  android {
      buildTypes {
          getByName("release") {
              configure<CrashlyticsExtension> {
                  nativeSymbolUploadEnabled = true
              }
          }
      }
  }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/ndk-reports
- **[offiziell]** Symbol-Upload-Task: nach dem Build die generierte Task ausfuehren —
  `./gradlew app:assembleRelease app:uploadCrashlyticsSymbolFileRelease` (Variant einsetzen).
  Quelle: https://firebase.google.com/docs/crashlytics/ndk-reports
- **[offiziell]** Fuer externe/Library-Module mit eigenen unstripped `.so` (sherpa-onnx liefert
  oft vorgebaute Libs): Pfad zu den UNSTRIPPED Native-Libs angeben, damit Symbolisierung klappt:
  ```kotlin
  configure<CrashlyticsExtension> {
      nativeSymbolUploadEnabled = true
      unstrippedNativeLibsDir = file("PATH/TO/UNSTRIPPED/DIRECTORY")
  }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/ndk-reports
- **[offiziell]** GNU build-id PFLICHT: native Binaries muessen GNU build IDs enthalten. Pruefen
  mit `readelf -n <binary>`. Fehlt sie, beim Linken `-Wl,--build-id` ergaenzen. Bei vorgebauten
  sherpa-onnx-Libs ist build-id meist schon vorhanden — vor Einbau mit `readelf -n` verifizieren.
  Quelle: https://firebase.google.com/docs/crashlytics/ndk-reports
- **[offiziell]** Non-Gradle-Alternative (z.B. CI / custom builds): Firebase CLI
  `firebase crashlytics:symbols:upload --app=FIREBASE_APP_ID PATH/TO/SYMBOLS` — erzeugt und laedt
  Breakpad-kompatible Symbol-Dateien ohne Gradle-Automatik.
  Quelle: https://firebase.google.com/docs/crashlytics/ndk-reports

## D. Sinnvolle Nutzung (Custom Keys, Logs, non-fatals, User-ID)

- **[offiziell]** Custom Keys (max **64** Paare, je **1 kB**) zum Filtern/Suchen im Dashboard —
  in BestJournal sinnvoll z.B. fuer aktive ASR-Engine, Eintrags-Count, Sync-Status:
  ```kotlin
  Firebase.crashlytics.setCustomKeys {
      key("asr_engine", "sherpa_onnx")
      key("entry_count", 42)
      key("premium", true)
  }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- **[offiziell]** Custom Logs (Limit **64 kB** pro Session) — Kontext-Breadcrumbs:
  `Firebase.crashlytics.log("Transcription started")`
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- **[offiziell]** Non-fatal Exceptions melden (gebatcht, gehen mit naechstem fatalen Event /
  App-Restart raus) — ideal fuer abgefangene Fehler in Sync/Whisper/sherpa:
  ```kotlin
  try { riskyOp() } catch (e: Exception) { Firebase.crashlytics.recordException(e) }
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- **[offiziell]** `setUserId("...")` — DSGVO-Hinweis der Doku ausdruecklich: KEINE PII / keine
  GDPR-relevanten Daten loggen. Nur anonyme/pseudonyme IDs. Fuer BestJournal: niemals echte
  Namen/E-Mails — hoechstens eine zufaellige Geraete-/Install-ID.
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- **[offiziell]** Breadcrumbs (automatische User-Interaktions-Spuren vor dem Crash) erfordern
  Google-Analytics-Integration — nur dann sichtbar. Gleiches gilt fuer crash-free users.
  Quelle: https://firebase.google.com/docs/crashlytics/customize-crash-reports

## E. Datenschutz / Consent (DSGVO)

- **[offiziell]** Opt-in-Modell (DSGVO-konform empfohlen): Auto-Collection im Manifest
  abschalten, dann zur Laufzeit nach Consent aktivieren:
  ```xml
  <meta-data android:name="firebase_crashlytics_collection_enabled" android:value="false" />
  ```
  ```kotlin
  Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)  // nach erteiltem Consent
  ```
  Die Einstellung **persistiert ueber alle Folge-Starts**; mit `false` wieder abschaltbar.
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- **[offiziell]** Bei deaktivierter Sammlung speichert Crashlytics Crash-Infos LOKAL auf dem
  Geraet. Mit `sendUnsentReports()` (nach Consent) gezielt hochladen, mit `deleteUnsentReports()`
  lokal verworfene Reports loeschen (z.B. wenn der Nutzer ablehnt).
  ```kotlin
  Firebase.crashlytics.sendUnsentReports()
  Firebase.crashlytics.deleteUnsentReports()
  ```
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- **[offiziell]** Hinweis: Deaktiviertes Auto-Reporting kann die Genauigkeit der
  crash-free-Metriken senken — beim Consent-Gating bewusst abwaegen.
  Quelle: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports

## F. Release-Health: ANR, Velocity Alerts, Regressionen

- **[offiziell]** Crashlytics meldet Crashes, non-fatals UND ANRs (Application Not Responding).
  Timing-Unterschied: Crashlytics meldet ANRs beim NAECHSTEN App-Start; Android Vitals meldet
  ANR-Daten direkt nach dem ANR. Beide ergaenzen sich. Eigene ANR-Debug-Tags im Dashboard.
  Quelle: https://firebase.google.com/docs/crashlytics/debug-anr-errors
- **[offiziell]** Velocity Alerts (benachrichtigen, wenn ein einzelnes Issue akut wird):
  erfordern mind. Crashlytics-SDK **v18.6.0+ (BoM v32.6.0+)** — BOM 34.11.0 erfuellt das locker.
  Trigger: in 30-Min-Fenster ueberschreitet ein Issue Prozent-Schwelle + Mindest-User-Zahl.
  Ein "Issue" gruppiert aehnliche Crashes/ANRs.
  Quelle: https://firebase.google.com/docs/crashlytics/velocity-alerts
- **[offiziell]** Regression-Detection: regressiert ein bereits geschlossenes Issue, sendet
  Crashlytics einen Regression-Alert und oeffnet das Issue erneut (Regression-Signal).
  Quelle: https://firebase.google.com/docs/crashlytics/alerts
- **[offiziell]** Crash-free users / Breadcrumbs / Velocity Alerts brauchen aktiviertes Google
  Analytics im Firebase-Projekt — Analytics-Dependency also nicht weglassen.
  Quelle: https://firebase.google.com/docs/crashlytics/android/get-started

---

# Teil 4 — Firebase Cloud Messaging (FCM) sauber einsetzen (PROAKTIV — noch nicht eingebunden)

> **PROAKTIV-HINWEIS:** FCM ist in BestJournalAndroid (Firebase BOM 34.11.0) NOCH NICHT
> eingebunden. Diese Sektion ist Zukunftswissen fuer den spaeteren Einbau eines Server-Push.
> Die App nutzt heute bereits `AlarmManager` fuer lokale Reminder — FCM kaeme dazu, nicht statt.
> App-Eckdaten: minSdk 26 (Android 8/O), targetSdk 36 (Android 16). Daraus folgen Pflichten:
> Notification-Channel (Android 8+) und POST_NOTIFICATIONS-Runtime-Permission (Android 13+).

---

## A. Einrichtung & Service

- **A1 — Dependency ueber BOM, kein Hardcode der Version** `offiziell`
  `firebase-messaging` immer ueber die Firebase BOM ziehen (kein Versions-Suffix), damit die
  Versionen aller Firebase-Libs konsistent bleiben:
  ```kotlin
  implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
  implementation("com.google.firebase:firebase-messaging")
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

- **A2 — FirebaseMessagingService im Manifest deklarieren** `offiziell`
  Service mit `android:exported="false"` und dem MESSAGING_EVENT-Intent-Filter:
  ```xml
  <service
      android:name=".MyFirebaseMessagingService"
      android:exported="false">
      <intent-filter>
          <action android:name="com.google.firebase.MESSAGING_EVENT" />
      </intent-filter>
  </service>
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

- **A3 — Default-Notification-Channel als Metadata setzen (Android 8+/O Pflicht)** `offiziell`
  Wenn vom SDK erzeugte Tray-Notifications keinen Channel mitbekommen, braucht es einen
  Default-Channel — sonst zeigt Android 8+ die Notification nicht korrekt. Der hier referenzierte
  Channel MUSS zur Laufzeit auch wirklich angelegt werden (siehe C5):
  ```xml
  <meta-data
      android:name="com.google.firebase.messaging.default_notification_channel_id"
      android:value="@string/default_notification_channel_id" />
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

- **A4 — Default-Icon und -Farbe als Metadata** `offiziell`
  Sorgt fuer korrektes Aussehen von Notification-Messages, die das System selbst rendert
  (App im Background). Icon sollte ein einfarbiges (white-on-transparent) Status-Icon sein:
  ```xml
  <meta-data
      android:name="com.google.firebase.messaging.default_notification_icon"
      android:resource="@drawable/ic_stat_ic_notification" />
  <meta-data
      android:name="com.google.firebase.messaging.default_notification_color"
      android:resource="@color/colorAccent" />
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

- **A5 — FirebaseInstanceIdService ist deprecated — NICHT mehr verwenden** `offiziell`
  Token-Aenderungen ausschliesslich ueber `onNewToken()` in FirebaseMessagingService behandeln.
  Den alten `FirebaseInstanceIdService` entfernen, sobald `onNewToken()` implementiert ist.
  Quelle: https://firebase.google.com/docs/reference/android/com/google/firebase/iid/FirebaseInstanceIdService

- **A6 — Auto-Init optional deaktivieren (Consent-Gating)** `offiziell`
  Wenn das Token erst nach Nutzer-Consent erzeugt werden soll, Auto-Init per Metadata
  abschalten und zur Laufzeit aktivieren:
  ```xml
  <meta-data android:name="firebase_messaging_auto_init_enabled" android:value="false" />
  ```
  Laufzeit: `FirebaseMessaging.getInstance().isAutoInitEnabled = true`.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

---

## B. Token-Management

- **B1 — Token beim Start aktiv holen, nicht nur passiv warten** `offiziell`
  `onNewToken()` feuert nicht garantiert bei jedem App-Start. Deshalb beim Start zusaetzlich
  aktiv holen:
  ```kotlin
  FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if (task.isSuccessful) sendTokenToServer(task.result)
  }
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

- **B2 — Token IMMER an den eigenen Server senden, nie nur lokal halten** `offiziell`
  Nur mit serverseitig gespeicherten Tokens kann man gezielt pushen. Token an den Server senden
  bei Erst-Start UND bei jeder Aenderung (Geraetewechsel, Reinstall, App-Daten-Clear, Ablauf).
  Quelle: https://firebase.google.com/docs/cloud-messaging/manage-tokens

- **B3 — Token IMMER mit Timestamp speichern (Client UND Server)** `offiziell`
  Firebase empfiehlt ausdruecklich einen Token-Timestamp und dessen regelmaessige Aktualisierung.
  Richtwert: einmal pro Monat aktualisieren — guter Kompromiss zwischen Akku-Last und Erkennung
  inaktiver Tokens.
  Quelle: https://firebase.google.com/docs/cloud-messaging/manage-tokens

- **B4 — Stale Tokens erkennen und loeschen** `offiziell`
  Android-Tokens laufen nach 270 Tagen Inaktivitaet ab. FCM antwortet beim Senden mit
  `UNREGISTERED` (HTTP 404) bzw. `INVALID_ARGUMENT` (HTTP 400) — diese Tokens serverseitig
  loeschen. Zusaetzlich Tokens aelter als das gewaehlte Staleness-Fenster (z.B. 30 Tage)
  entfernen, z.B. per taeglichem Cloud-Function-Check.
  Quelle: https://firebase.google.com/docs/cloud-messaging/manage-tokens

- **B5 — Token bei Logout/Account-Wechsel invalidieren** `offiziell`
  Bei Sign-out das Token serverseitig vom Nutzer entkoppeln und ggf. lokal loeschen
  (`FirebaseMessaging.getInstance().deleteToken()`), damit ein abgemeldeter Nutzer keine
  personalisierten Pushes mehr bekommt. (Fuer BestJournal relevant, falls je ein Account-System
  dazukommt.)
  Quelle: https://firebase.google.com/docs/cloud-messaging/manage-tokens

- **B6 — Multi-Device sauber abbilden** `offiziell`
  Ein Nutzer kann mehrere Geraete haben (Frank: S23 Ultra + Fold 6 + Tablet). Server speichert
  daher pro Nutzer eine **Liste** von Tokens, nicht ein einzelnes Token. Bei Topic-Abos:
  monatlich resubscriben und bei Token-Refresh; stale Tokens per Admin SDK von Topics
  abmelden, um Fanout-Overhead zu reduzieren.
  Quelle: https://firebase.google.com/docs/cloud-messaging/manage-tokens

---

## C. Notification- vs. Data-Messages & Channels

- **C1 — Verhalten Foreground vs. Background genau verstehen** `offiziell`

  | Szenario | onMessageReceived? | Wo landet die Anzeige |
  |----------|--------------------|-----------------------|
  | Foreground (egal welcher Typ) | JA | Du baust die Notification selbst |
  | Background, nur Notification | NEIN | System-Tray (vom SDK gerendert) |
  | Background, nur Data | JA | Du baust die Notification selbst |
  | Background, Notification + Data | NEIN | Notification in Tray, Data in Intent-Extras der Launcher-Activity beim Tap |

  Quelle: https://firebase.google.com/docs/cloud-messaging/android/receive

- **C2 — Empfehlung: data-only fuer volle Kontrolle** `offiziell`
  Wenn man IMMER selbst entscheiden will, wie/ob eine Notification erscheint (Deep-Link,
  Custom-Layout, Stummschaltung im Foreground), data-only-Messages senden — dann laeuft
  `onMessageReceived` in jedem App-Zustand. Nachteil: man muss die Anzeige komplett selbst bauen.
  `notification`+`data` ist der einfachere Weg, gibt aber im Background die Anzeige-Kontrolle ans System ab.
  Quelle: https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type

- **C3 — onMessageReceived hat ein kurzes Zeitfenster (~wenige Sekunden) → WorkManager** `offiziell`
  Laenger laufende Arbeit (Bild laden, Server-Call, DB-Schreiben) NICHT direkt in
  `onMessageReceived` erledigen, sondern per `WorkManager` (expedited bei high-priority,
  sonst normal) auslagern. Sonst Lifecycle-Verletzung → verpasste Notifications.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/receive

- **C4 — onDeletedMessages behandeln** `offiziell`
  FCM ruft `onDeletedMessages()` wenn es Nachrichten verworfen hat (100+ ausstehend ODER
  Geraet 1+ Monat offline). Dann clientseitig einen Full-Sync mit dem eigenen Server anstossen,
  statt sich auf die einzelnen Pushes zu verlassen.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/receive

- **C5 — Notification Channel zur Laufzeit anlegen (Android 8+/O Pflicht)** `offiziell`
  Ab Android 8 erscheint KEINE Notification ohne registrierten Channel. Channel mit passender
  Importance einmalig anlegen (idempotent), z.B. `IMPORTANCE_HIGH` fuer Reminder, die der Nutzer
  sofort sehen soll. Channel-ID MUSS zur Default-Channel-Metadata (A3) passen.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client +
  https://developer.android.com/develop/ui/views/notifications/channels

---

## D. POST_NOTIFICATIONS-Permission & PendingIntent

- **D1 — POST_NOTIFICATIONS Runtime-Permission (Android 13+/targetSdk 33+) — PFLICHT** `offiziell`
  Bei targetSdk 36 ist die Laufzeit-Abfrage zwingend. Das FCM-SDK deklariert die Permission
  bereits im Manifest; abgefragt werden muss sie zur Laufzeit:
  ```kotlin
  val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> }
  if (Build.VERSION.SDK_INT >= 33 &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }
  ```
  Best Practice: erst im richtigen Moment (z.B. wenn Nutzer Reminder aktiviert) und mit
  vorherigem Erklaer-Rationale fragen, nicht beim allerersten Start ungefragt.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client +
  https://developer.android.com/develop/ui/views/notifications/notification-permission

- **D2 — Anzeige mit NotificationCompat bauen** `offiziell`
  Im Foreground/Data-Fall die Notification selbst via `NotificationCompat.Builder(context, channelId)`
  bauen: SmallIcon, Title, Text, Priority/Importance ueber Channel, AutoCancel.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/receive

- **D3 — PendingIntent mit FLAG_IMMUTABLE (Android 12+/targetSdk 31+ PFLICHT)** `offiziell`
  Ab Android 12 muss jeder PendingIntent explizit `FLAG_IMMUTABLE` oder `FLAG_MUTABLE` tragen —
  sonst `IllegalArgumentException`. Fuer Deep-Links in Notifications fast immer IMMUTABLE:
  ```kotlin
  val pi = PendingIntent.getActivity(
      context, 0, deepLinkIntent,
      PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
  )
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/receive +
  https://developer.android.com/develop/ui/views/notifications

---

## E. Server-Seite (HTTP v1) & Topics

- **E1 — NUR HTTP v1 API verwenden — Legacy ist abgeschaltet** `offiziell`
  Die alten Legacy-HTTP/XMPP-APIs (Server-Key) wurden am 20.06.2023 deprecated, Shutdown ab
  22.07.2024. Seit Maerz 2020 werden gar keine neuen Legacy-Server-Keys mehr erzeugt. Neuentwicklung
  daher AUSSCHLIESSLICH HTTP v1.
  Quelle: https://firebase.google.com/docs/cloud-messaging/migrate-v1

- **E2 — Auth ueber OAuth 2.0 / Service-Account, nicht Server-Key** `offiziell`
  HTTP v1 nutzt kurzlebige OAuth-2.0-Access-Tokens (ca. 1h gueltig). In Google-Umgebungen
  (Cloud Functions) Application Default Credentials nutzen; sonst Service-Account-JSON. Der
  Server-Key aus der Console kann HTTP v1 NICHT autorisieren.
  Quelle: https://firebase.google.com/docs/cloud-messaging/auth-server

- **E3 — Empfehlung fuer BestJournal: Senden via Cloud Function** `offiziell`
  Das eigene Backend als Cloud Function bauen — dort Admin SDK + ADC, kein Schluessel-Handling
  im Client, Tokens/Topics serverseitig verwaltet. (Passt zur bereits geplanten Cloud Function
  fuer Subscription-Status.) NIEMALS Service-Account-JSON in die App packen.
  Quelle: https://firebase.google.com/docs/cloud-messaging/send/v1-api

- **E4 — Topic-Messaging: subscribe/unsubscribe** `offiziell`
  ```kotlin
  FirebaseMessaging.getInstance().subscribeToTopic("daily-reminder")
  FirebaseMessaging.getInstance().unsubscribeFromTopic("daily-reminder")
  ```
  Limits: max 2.000 Topics pro App-Instanz; Batch max 1.000 Instanzen/Request; Subscribe-Rate
  3.000 QPS/Projekt. Topic-Namen koennen optional mit `/topics/` praefigiert sein; gueltige
  Beispiele: `highScores`, `foo-bar`, `stock-GOOG` (alphanumerisch + Bindestrich).
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/topic-messaging +
  https://firebase.google.com/docs/cloud-messaging/send-topic-messages

- **E5 — Topics vs. direkter Token — richtig waehlen** `offiziell`
  Topics sind auf Durchsatz optimiert, NICHT auf Latenz — ungeeignet fuer zeitkritische
  Einzel-Pushes. Fuer schnelle, sichere Zustellung an einzelne Geraete/kleine Gruppen direkt
  per Registration-Token targeten. Topics fuer breite, nicht-dringende Ansagen (z.B. globale
  Hinweise) — fuer einen persoenlichen Tagebuch-Reminder eher Token-Targeting oder lokaler
  AlarmManager.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/topic-messaging

- **E6 — Condition-Syntax fuer kombinierte Topics** `offiziell`
  Boolesche Ausdruecke, max **5 Topics** pro Bedingung, Klammern werden zuerst ausgewertet:
  ```
  "'TopicA' in topics && ('TopicB' in topics || 'TopicC' in topics)"
  ```
  Quelle: https://firebase.google.com/docs/cloud-messaging/send-topic-messages

- **E7 — High- vs. Normal-Priority bewusst setzen** `offiziell`
  High-priority weckt das Geraet (auch in Doze) → nur fuer dringende, NUTZERSICHTBARE Inhalte
  (z.B. Reminder, der sofort sichtbar wird). Normal-priority fuer nicht-dringendes (Background-Sync).
  WICHTIG: Wer high-priority missbraucht (kein sichtbares Ergebnis), wird vom System
  herabgestuft/geproxyt. Demotion erkennbar via `getPriority()` vs. `getOriginalPriority()`.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android-message-priority

- **E8 — TTL, collapse_key, reservierte Keys** `offiziell`
  - **TTL**: 0 bis 2.419.200 s (28 Tage) — wie lange FCM die Message speichert/zustellt.
    Fuer fluechtige Reminder kleines TTL setzen (veraltete Nachricht nicht spaeter nachliefern).
  - **collapse_key**: Default = App-Package. Max **4** verschiedene collapsible Messages pro
    Geraet gleichzeitig; gleiche collapse_key → alte ausstehende Message wird ersetzt
    (gut gegen Notification-Spam bei Reconnect).
  - **Reservierte Data-Keys NICHT verwenden**: `from`, `message_type`, sowie alles mit Praefix
    `google.`, `gcm.`, `gcm.notification.`.
  - Payload-Limit beachten (Data-Messages kompakt halten).
  Quelle: https://firebase.google.com/docs/cloud-messaging/customize-messages/setting-message-lifespan +
  https://firebase.google.com/docs/cloud-messaging/customize-messages/collapsible-message-types

---

## F. Datenschutz / Consent (kurz)

- **F1 — Token-Erzeugung an Consent koppeln** `offiziell`
  Das FCM-Token ist ein persistenter Geraete-Identifier. Wenn Push optional ist, Auto-Init
  abschalten (A6) und erst nach Opt-in initialisieren — sauberer fuer DSGVO-Transparenz.
  Quelle: https://firebase.google.com/docs/cloud-messaging/android/client

- **F2 — POST_NOTIFICATIONS-Permission im Kontext erklaeren** `extern`
  Vor der Permission-Abfrage kurz erklaeren, WOFUER (z.B. Tagebuch-Erinnerungen) — verbessert
  Grant-Rate und ist transparenter. (Best-Practice-Empfehlung, nicht hart vorgeschrieben.)
  Quelle: https://developer.android.com/develop/ui/views/notifications/notification-permission

---

---

# Teil 5 — Firestore Security Rules & Data Modeling (PROAKTIV — Firestore nicht eingebunden)

> **PROAKTIV-Hinweis:** Firestore ist in BestJournalAndroid aktuell NICHT eingebunden.
> Lokale Daten leben in Room, Backup laeuft ueber Drive. Diese Sektion ist Zukunftswissen
> fuer den Fall, dass Cloud-Sync oder ein Entitlement-Store ueber Firestore (oder Realtime
> Database) kommt. App Check (Play Integrity) ist bereits aktiv — die App Check + Rules
> Kombination (Abschnitt C) ist also direkt anschlussfaehig.

---

## A. Security-Rules-Grundprinzipien (Default-Deny, Auth, Ownership)

- **[offiziell] Default-Deny ist das Fundament.** Jeder Request aus einer Client-Library
  wird VOR jedem Lese-/Schreibzugriff gegen die Rules geprueft. Was nicht explizit erlaubt
  ist, ist verboten. `allow read, write: if true` ist toedlich — es macht die ganze DB
  oeffentlich. Quelle: https://firebase.google.com/docs/firestore/security/get-started

- **[offiziell] Grund-Struktur.** Rules beginnen mit `service cloud.firestore` und
  `match /databases/{database}/documents { ... }`. `match`-Statements zeigen auf
  Dokument-Pfade (NICHT Collections). `{city}`-Wildcards binden an einzelne Dokumente.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-structure
  ```
  service cloud.firestore {
    match /databases/{database}/documents {
      match /cities/{city} {
        allow read, write: if <condition>;
      }
    }
  }
  ```

- **[offiziell] Auth-Check als Mindeststandard.** `request.auth != null` stellt sicher,
  dass nur authentifizierte Nutzer zugreifen. Reicht aber NICHT fuer privaten Nutzer-Daten
  — dafuer Ownership.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

- **[offiziell] Ownership-Checks pro Dokument.** Die uid des Requesters gegen ein Feld
  oder den Dokument-Pfad pruefen. Beispiel fuer private Nutzer-Daten:
  ```
  match /users/{userId} {
    allow read, update, delete: if request.auth != null
                                && request.auth.uid == userId;
  }
  ```
  Oder gegen ein Owner-Feld im Dokument: `request.auth.uid == resource.data.ownerId`.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

- **[offiziell] Granulare Operationen statt read/write.** `read` aufteilen in `get`
  (Einzeldokument) und `list` (Query); `write` in `create`, `update`, `delete`. Das
  ermoeglicht z.B. „jeder darf erstellen, aber nur der Owner darf loeschen".
  Quelle: https://firebase.google.com/docs/firestore/security/rules-structure
  ```
  match /cities/{city} {
    allow get:    if <condition>;
    allow list:   if <condition>;
    allow create: if <condition>;
    allow update: if <condition>;
    allow delete: if <condition>;
  }
  ```

- **[offiziell] Subcollections erben NICHT.** Rules kaskadieren nicht in Subcollections.
  Jede Ebene braucht ihre eigene `match`-Regel. Recursive-Wildcard `{document=**}` matcht
  beliebig tief — mit `rules_version = '2'` matcht es auch null Ebenen (sicherer).
  Quelle: https://firebase.google.com/docs/firestore/security/rules-structure

- **[offiziell] Ueberlappende Regeln sind OR-verknuepft.** Matchen mehrere Regeln dasselbe
  Dokument, ist der Zugriff erlaubt, wenn EINE Bedingung true ist. Wichtig: eine zu offene
  Regel kann eine strenge nicht „ueberstimmen" — sie oeffnet zusaetzlich.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-structure

- **[offiziell] Rules-Limits.** Max 10 `exists()`/`get()`/`getAfter()`-Calls pro
  Single-Doc-Request (20 bei Batch, 10 pro Op), max 10 verschachtelte match-Tiefen,
  max 100 Path-Segmente, 256 KB Ruleset-Groesse. Funktionen: nur ein return, keine
  Schleifen/Rekursion ueber Tiefe 10.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

- **[offiziell] Wiederverwendbare Funktionen** fuer DRY-Conditions:
  ```
  function isOwner() { return request.auth.uid == resource.data.ownerId; }
  ```
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

---

## B. Validierung in Rules (Server-Validierung — NICHT die App)

- **[offiziell] Rules SIND die Server-Validierung.** Die App-seitige Validierung ist nur
  UX — die Rules sind die einzige durchgesetzte Schicht. `request.resource.data` enthaelt
  die EINGEHENDEN Daten (nach dem Write), `resource.data` die BESTEHENDEN.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

- **[offiziell] Daten-Validierung bei Update.** Werte und Typen pruefen, und Felder gegen
  Manipulation schuetzen (z.B. Name darf bei Update nicht geaendert werden):
  ```
  allow update: if request.resource.data.population > 0
                && request.resource.data.name == resource.data.name;
  ```
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

- **[offiziell] Sichtbarkeits-/Rollen-Felder pruefen** vor dem Zugriff:
  ```
  allow read: if resource.data.visibility == 'public';
  ```
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

- **[extern, abgeleitet aus offizieller Syntax]** Empfohlene Validierungs-Bausteine fuer
  einen Entitlement-/Sync-Store: Pflichtfelder erzwingen
  (`request.resource.data.keys().hasAll([...])`), Feld-Whitelisting
  (`request.resource.data.keys().hasOnly([...])`) gegen Schmuggeln zusaetzlicher Felder,
  Typen-Checks (`request.resource.data.x is string`), und Owner-Feld unveraenderlich halten
  (`request.resource.data.ownerId == request.auth.uid`). Diese Muster bauen direkt auf den
  offiziellen `request.resource.data`-Mechanismen auf.
  Quelle (Basis-Syntax): https://firebase.google.com/docs/firestore/security/rules-conditions

- **[offiziell] Cross-Document-Validierung** mit `exists()`/`get()`, z.B. „erstellen nur,
  wenn ein User-Profil existiert":
  ```
  allow create: if request.auth != null &&
    exists(/databases/$(database)/documents/users/$(request.auth.uid));
  ```
  Aber: kostet gegen das 10-Call-Limit und erzeugt Lese-Kosten.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-conditions

---

## C. App Check + Rules zusammen (beide noetig — direkt relevant fuer BestJournal)

- **[offiziell] Zwei verschiedene Fragen.** `request.auth` beantwortet WER (Firebase Auth,
  Nutzeridentitaet). App Check beantwortet WELCHE APP (legitimer, registrierter Client).
  Zusammen: „dieser authentifizierte Nutzer sendet aus einer legitimen, registrierten App".
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

- **[offiziell] App Check erscheint NICHT in der Rules-Syntax.** Enforcement wird im
  Firebase-Console-Bereich „App Check" pro Produkt aktiviert. Ist es fuer Firestore
  aktiv, validiert Firestore die App-Check-Tokens automatisch am Backend, BEVOR ueberhaupt
  Rules greifen — eine vorgelagerte Schicht.
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

- **[offiziell] Gestaffeltes Enforcement-Vorgehen (PFLICHT-Ablauf).** 1) Library
  installieren, 2) Metriken im App-Check-Dashboard sammeln (Traffic verstehen), 3) ALLE
  Apps registrieren (unregistrierte werden geblockt), 4) gruendlich testen, 5) erst dann
  Enforcement aktivieren. TTL 30 Min bis 7 Tage abwaegen (Sicherheit vs. Overhead).
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

- **[Anschluss BestJournal]** App Check / Play Integrity ist bereits aktiv. Falls Firestore
  spaeter kommt: Enforcement fuer Firestore erst NACH der Metrik-Phase aktivieren, sonst
  blockt man legitime Nutzer aus.

---

## D. Data Modeling & Kosten

- **[offiziell] Hierarchie:** Database → Collections → Documents → (optional) Subcollections.
  Firestore ist schemalos, aber konsistente Felder/Typen ueber Dokumente erleichtern Queries.
  Quelle: https://firebase.google.com/docs/firestore/data-model

- **[offiziell] Document-Groesse 1 MiB.** Harte Grenze pro Dokument.
  Quelle: https://firebase.google.com/docs/firestore/data-model

- **[offiziell] Wann was?**
  - **Subcollections:** fuer unbegrenzt wachsende Daten (z.B. Chat-Messages pro Raum) —
    verhindert oversized Dokumente.
  - **Nested Maps:** fuer begrenzte, strukturierte Daten in einem Dokument (z.B. `name`-Map).
  - **Root Collections:** fuer Top-Level-Entitaeten, die unabhaengig abgefragt werden.
  - **References statt Duplikation:** leichte Verweise auf andere Dokumente statt Daten
    duplizieren — schuetzt Integritaet und Groesse.
  Quelle: https://firebase.google.com/docs/firestore/data-model

- **[offiziell] Keine sequenziellen / monoton steigenden Document-IDs.** `Customer1`,
  `Customer2` … erzeugen Hotspots und Latenz. Firestores Auto-ID nutzt Scatter-Algorithmen
  fuer gleichmaessige Verteilung — IMMER bevorzugen.
  Quelle: https://firebase.google.com/docs/firestore/best-practices

- **[offiziell] 500/50/5-Regel** beim Hochfahren neuer Collections: mit 500 Ops/Sek starten,
  dann alle 5 Min um 50% steigern. Hotspotting entsteht durch monoton steigende Felder bei
  hoher Rate, schnelles Loeschen aus engem Bereich, oder Ueberspringen frisch geloeschter
  Daten in Queries.
  Quelle: https://firebase.google.com/docs/firestore/best-practices

- **[offiziell] Index-Bewusstsein.** Index-Fanout reduzieren: Collection-Level-Exemptions
  setzen (Descending/Array-Index per Default aus), High-Cardinality-Strings, TTL-Felder und
  grosse Array/Map-Felder von der Indizierung ausnehmen, wenn nicht query-kritisch.
  Quelle: https://firebase.google.com/docs/firestore/best-practices

- **[offiziell] Kosten-Optimierung Reads/Writes:** asynchrone statt synchrone Calls;
  **Cursor-basierte Pagination statt Offset** (Offset laedt uebersprungene Dokumente intern
  und berechnet sie als Reads!); Transaktions-Retries via SDK; BulkWriter statt atomarer
  Batches bei grossen Mengen.
  Quelle: https://firebase.google.com/docs/firestore/best-practices

- **[offiziell] Offline-Persistenz auf Android ist per Default AN.** Default-Cache 100 MB,
  danach Cleanup alter Dokumente. `CACHE_SIZE_UNLIMITED` moeglich (Cleanup aus), Min 1 MB.
  Quelle: https://firebase.google.com/docs/firestore/manage-data/enable-offline
  ```kotlin
  val settings = FirebaseFirestoreSettings.Builder()
      .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
      .build()
  db.firestoreSettings = settings
  ```

- **[offiziell] Listener vs. one-time get():** Snapshot-Listener liefern Live-Updates, halten
  aber aktive Subscriptions (Ressourcen). `get()` ist ein einmaliger Read ohne laufenden
  Overhead — fuer Einzel-Queries ohne Echtzeit-Bedarf bevorzugen. Offline-Queries gegen den
  lokalen Cache kosten NULL Netzwerk (nur Disk-I/O). `metadata.isFromCache` zeigt Quelle.
  Quelle: https://firebase.google.com/docs/firestore/manage-data/enable-offline

- **[Empfehlung fuer Entitlement-Store]** Entitlements/Abo-Status: kein Dauer-Listener noetig
  — one-time `get()` beim App-Start + Cache reicht meist. Real-time Listener nur, wo der
  Nutzer Live-Sync ueber mehrere Geraete WIRKLICH sieht.

---

## E. Rules testen (Emulator Suite)

- **[offiziell] Lokales Testen mit `@firebase/rules-unit-testing`** (v9 SDK) gegen die
  Emulator Suite. `firestore.rules` in `firebase.json` referenzieren.
  Quelle: https://firebase.google.com/docs/rules/unit-tests

- **[offiziell] Test-Workflow:**
  ```javascript
  import { initializeTestEnvironment, assertSucceeds, assertFails }
    from "@firebase/rules-unit-testing";

  const testEnv = await initializeTestEnvironment({
    projectId: "demo-project-1234",
    firestore: { rules: fs.readFileSync("firestore.rules", "utf8") },
  });
  // authentifizierter Nutzer
  const alice = testEnv.authenticatedContext("alice");
  await assertSucceeds(setDoc(alice.firestore().doc('/users/alice'), {...}));
  // unauthentifiziert -> MUSS scheitern (deny-by-default verifizieren)
  const anon = testEnv.unauthenticatedContext();
  await assertFails(anon.firestore().doc('/private').get());
  ```
  Quelle: https://firebase.google.com/docs/rules/unit-tests

- **[offiziell] Schluessel-Funktionen:** `assertSucceeds()` / `assertFails()` (erwartetes
  Allow/Deny), `withSecurityRulesDisabled()` (Test-Daten OHNE Rules anlegen),
  `clearFirestore()` (Reset zwischen Tests).
  Quelle: https://firebase.google.com/docs/rules/unit-tests

- **[offiziell] Strategie:** 1) Setup via Admin/disabled-Kontext Test-Daten anlegen,
  2) Operationen ueber auth/unauth-Kontexte ausfuehren, 3) Allow/Deny asserten,
  4) Cleanup. Deny-by-default EXPLIZIT testen (unauth-Zugriff muss scheitern).
  Quelle: https://firebase.google.com/docs/rules/unit-tests

- **[offiziell] Wichtig:** Rules-Deploys brauchen bis zu 1 Minute, bis sie neue Queries
  betreffen — beim manuellen Verifizieren einplanen.
  Quelle: https://firebase.google.com/docs/firestore/security/get-started

---

## F. „Rules sind keine Filter" (eigene Stolperfalle — Server-Verhalten)

- **[offiziell] Queries sind all-or-nothing.** Eine `list`/Query wird gegen die MOEGLICHE
  Ergebnismenge geprueft, nicht gegen die tatsaechlichen Dokumente. Koennte die Query
  THEORETISCH ein regelverletzendes Dokument liefern, schlaegt der GANZE Request fehl
  (permission-denied) — auch wenn die konkreten Daten gepasst haetten.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-query

- **[offiziell] Loesung: Query MUSS die Rule-Constraints spiegeln.** Erlauben die Rules nur
  `published == true` oder `author == uid`, dann muss die Query exakt diese `where()`-Klausel
  tragen:
  ```
  .where("published", "==", true).get()
  .where("author", "==", currentUser.uid).get()
  ```
  Quelle: https://firebase.google.com/docs/firestore/security/rules-query

- **[offiziell] Query-Constraints in Rules erzwingen** via `request.query.limit/orderBy/offset`,
  z.B. `allow list: if request.query.limit <= 10;` gegen Ressourcen-Erschoepfung.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-query

- **[offiziell] Pagination-Konsequenz:** Man kann nicht durch Daten paginieren, fuer die der
  Nutzer keine Blanket-Permission hat — Pagination-Logik muss dieselben Constraints wie die
  Rules respektieren.
  Quelle: https://firebase.google.com/docs/firestore/security/rules-query

---

## G. Admin SDK / Cloud Functions umgeht Rules (Entitlement-Writes serverseitig)

- **[extern, etablierte Praxis]** Das Admin SDK (in Cloud Functions) laeuft mit vollen
  Privilegien und UMGEHT Security Rules vollstaendig. Das ist GEWOLLT fuer vertrauenswuerdigen
  Server-Code. Konkrete Konsequenz fuer einen Entitlement-Store: Abo-/Entitlement-Felder
  NUR serverseitig schreiben (z.B. aus der Play-RTDN-/Functions-Verifikation), und in den
  Client-Rules `allow write: if false` fuer diese Felder/Collection setzen — der Client darf
  Entitlements nur LESEN, nie schreiben. Das verhindert clientseitige Manipulation des
  Premium-Status. BestJournal nutzt bereits firebase-functions — der natuerliche Ort dafuer.
  Quelle (Konzept): https://firebase.google.com/docs/firestore/security/get-started
  (Default-Deny fuer Client-Writes) + Admin-SDK-Privileg-Modell (allgemein dokumentiert).

---

---

# Teil 6 — App Check + Remote Config + Cloud Functions richtig nutzen (alle eingebunden)

> Stand 2026-06-02. Firebase BOM 34.11.0, minSdk 26, targetSdk 36, Kotlin 2.1.0 + Coroutines, R8 im Release.
> App Check: firebase-appcheck-playintegrity (Release) + firebase-appcheck-debug (Debug).
> Remote Config: firebase-config. Cloud Functions: firebase-functions (callable).

## A. App Check (Play Integrity + Debug, Enforcement)

### A1 — Provider VOR allen Firebase-Diensten installieren (Application.onCreate)
- **offiziell** Die Provider-Factory muss FRUEH im App-Lebenszyklus installiert werden, idealerweise in
  `Application.onCreate()` — BEVOR irgendein Firebase-Produkt (Functions, Firestore, RTDB, AI) den ersten
  Request macht. Sonst gehen frueh abgesetzte Requests OHNE App-Check-Token raus.
  Pattern (Kotlin KTX):
  ```kotlin
  Firebase.initialize(context)
  Firebase.appCheck.installAppCheckProviderFactory(
      PlayIntegrityAppCheckProviderFactory.getInstance()
  )
  ```
  Java-Aequivalent: `FirebaseApp.initializeApp(...)` + `FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...)`.
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

### A2 — Debug-Provider NUR im Debug-Build, Debug-Token in Konsole eintragen
- **offiziell** Im lokalen Debug/Emulator nutzt man die `DebugAppCheckProviderFactory`. Beim ersten Start
  schreibt das SDK ein lokales Debug-Token ins Logcat; dieses Token MUSS in der Firebase-Konsole
  (App Check > Apps > Debug tokens) registriert werden. Debug-Provider NIEMALS in der Release-Variante
  ausliefern (Build-Variante/BuildConfig.DEBUG abfragen — sauber: Provider-Wahl ueber Build-Flavor/Source-Set
  trennen, nicht ueber Runtime-if im Release-Code).
  Quelle: https://firebase.google.com/docs/app-check/android/debug-provider

### A3 — Play Integrity Setup: Linkage + SHA
- **offiziell** Vor Nutzung des Play-Integrity-Providers:
  1. Cloud-Projekt im Play-Console-Bereich "Play Integrity API" mit DEMSELBEN Firebase-Projekt verknuepfen.
  2. App in der Firebase-Konsole fuer App Check mit Play-Integrity registrieren und den
     SHA-256-Fingerprint des Signing-Zertifikats hinterlegen (Release- UND ggf. Upload-Key; bei
     App-Signing-by-Google-Play den von Google verwalteten App-Signing-SHA-256 nehmen, nicht nur den Upload-Key).
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

### A4 — Gestaffeltes Enforcement (erst Metriken, dann erzwingen)
- **offiziell** Nach Einbau des SDK senden Clients App-Check-Tokens mit JEDEM Request, ABER Firebase
  erzwingt sie erst, wenn man Enforcement pro Dienst in der Konsole aktiviert. Empfohlene Reihenfolge:
  Client-Rollout abwarten -> Metriken (verified/unverified) in der Konsole beobachten -> erst wenn der
  Grossteil der Requests verifiziert ist, Enforcement pro Dienst (Functions, Firestore, Storage, RTDB,
  AI Logic) einschalten. Sonst sperrt man legitime Alt-Clients aus.
  Quelle: https://firebase.google.com/docs/app-check (Get started / Enforcement)

### A5 — Token-Auto-Refresh
- **offiziell** Das SDK refresht App-Check-Tokens automatisch im Hintergrund, solange die App laeuft.
  Auto-Refresh kann pro App via `setTokenAutoRefreshEnabled(true)` explizit gesteuert werden (Standard
  folgt der Data-Collection-Einstellung). Fuer eine App die App Check durchgehend nutzt: Auto-Refresh AN
  lassen, damit keine abgelaufenen Tokens an enforced Dienste gehen.
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

### A6 — Limited-Use-Tokens (Replay-Schutz) nur fuer sensible Aufrufe
- **offiziell** Fuer Replay-Schutz auf besonders sensiblen Endpunkten (typisch: bestimmte Cloud Functions)
  am Client `getLimitedUseAppCheckToken()` statt `getAppCheckToken()` verwenden bzw. bei callable Functions
  die Option `requireLimitedUseAppCheckTokens`/`limitedUseAppCheckTokens` setzen. Server konsumiert das Token
  (`consumeAppCheckToken: true`) -> danach unbrauchbar. NICHT universell aktivieren: kostet eine zusaetzliche
  Netzwerk-Roundtrip-Latenz. Hinweis: Token-Consumption auf der Server-Seite ist Beta und nur Node.js-Functions-SDK.
  Quelle: https://firebase.google.com/docs/app-check/cloud-functions ;
  https://firebase.google.com/docs/app-check/android/custom-resource

## B. Remote Config (Fetch/Activate, Realtime, Feature-Flags)

### B1 — In-App-Defaults via XML setzen (setDefaultsAsync)
- **offiziell** IMMER In-App-Defaults setzen (`setDefaultsAsync(R.xml.remote_config_defaults)`), damit die
  App auch offline/vor dem ersten erfolgreichen Fetch deterministisch funktioniert. Niemals annehmen, dass
  Netzwerk verfuegbar ist.
  Quelle: https://firebase.google.com/docs/remote-config/android/get-started ;
  https://firebase.google.com/docs/remote-config/loading

### B2 — Loading-Strategie: nicht beim ersten Start blockieren
- **offiziell** Empfohlene Default-Strategie "Load for next startup": beim Start sofort die zuvor
  gefetchten Werte aktivieren, neue Werte ASYNCHRON im Hintergrund holen und erst beim NAECHSTEN Start
  aktivieren. Minimiert Wartezeit und vermeidet ruckartige UI-Aenderungen. `fetchAndActivate()` beim ersten
  Start NUR mit Bedacht auf UI-Folgen — RC hat ein 1-Minuten-Timeout, das laenger sein kann als eine
  akzeptable Startdauer. Wenn ein Lade-Screen noetig ist (z.B. fuer A/B): eigenes Timeout setzen.
  UI nicht aktualisieren waehrend der Nutzer interagiert (ausser betrieblich kritisch).
  Quelle: https://firebase.google.com/docs/remote-config/loading

### B3 — minimumFetchInterval: hoch in Prod, niedrig nur in Dev
- **offiziell** In Produktion einen relativ hohen `minimumFetchIntervalInSeconds` (z.B. ~12h / 43200s)
  nutzen, um Throttling zu vermeiden. Niedrige Intervalle (Sekunden/Minuten) NUR im Dev-Build setzen —
  niemals in Produktion ausliefern. Throttling-Risiko in Prod ist mit vernuenftigem Intervall minimal.
  Quelle: https://firebase.google.com/docs/remote-config/android/get-started ;
  https://firebase.google.com/docs/remote-config/loading

### B4 — Realtime Remote Config (addOnConfigUpdateListener)
- **offiziell** `fetch()` einmal pro App-Start, danach `addOnConfigUpdateListener()` registrieren, um
  Server-Updates automatisch ohne haeufige manuelle Fetches zu erhalten; im `onUpdate`-Callback dann
  `activate()` aufrufen und neu rendern. Der Realtime-Fetch umgeht Caching/minimumFetchInterval. NICHT
  massenhaft gleichzeitig manuell fetchen — stattdessen Realtime nutzen (vermeidet Throttling).
  Verfuegbar ab Android-SDK v21.3.0+ (BoM v31.3.0+) — bei BOM 34.11.0 voll vorhanden.
  Quelle: https://firebase.google.com/docs/remote-config/android/real-time

### B5 — Typsichere Nutzung + Feature-Flags
- **offiziell** RC speichert intern alles als String, bietet aber typsichere Getter:
  `getString()`, `getBoolean()`, `getLong()`, `getDouble()` — diese immer passend zum erwarteten Typ
  verwenden (getBoolean castet korrekt). Parameter eignen sich als Feature-Flags, um Funktionen fuer
  Test/Dev sichtbar, fuer Produktions-User aber versteckt zu halten. Werte am Client validieren
  (Plausibilitaet/Enum-Whitelist), bevor sie das Verhalten steuern.
  Quelle: https://firebase.google.com/docs/reference/android/com/google/firebase/remoteconfig/FirebaseRemoteConfig ;
  https://firebase.google.com/docs/remote-config/use-cases

### B6 — Conditions, Rollouts, A/B
- **offiziell** Conditions/Parameter-Gruppen steuern Werte nach Zielgruppe; Prozent-Rollouts exponieren
  neue Funktionen schrittweise; A/B-Testing fuer Vergleich. Limit: pro Firebase-Projekt max. 24 laufende
  A/B-Experimente und RC-Rollouts kombiniert — bei der Feature-Flag-Planung beachten.
  Quelle: https://firebase.google.com/docs/remote-config/parameters ;
  https://firebase.google.com/docs/remote-config/products/

### B7 — App Check schuetzt auch Remote Config
- **offiziell** Remote Config ist ein App-Check-faehiger Dienst. Nach Client-Rollout + Metrik-Beobachtung
  Enforcement fuer RC aktivieren (gleicher gestaffelter Weg wie A4), damit nur attestierte Clients
  Config abrufen koennen.
  Quelle: https://firebase.google.com/docs/app-check (unterstuetzte Dienste / Enforcement)

## C. Cloud Functions (callable, App Check, Region, Secrets)

### C1 — Region am Client setzen (getInstance(region))
- **offiziell** Wenn die Function NICHT in der Projekt-Default-Region laeuft, am Client die Region
  explizit setzen: `FirebaseFunctions.getInstance("europe-west1")` (bzw. KTX `Firebase.functions("...")`).
  Client- und Backend-Region sollten zusammenpassen, sonst entsteht unnoetige Latenz/Fehlschlag.
  Quelle: https://firebase.google.com/docs/functions/callable ;
  https://firebase.google.com/docs/functions/locations

### C2 — getHttpsCallable + Daten/Antwort sauber typisieren
- **offiziell** `getHttpsCallable("name").call(data)` mit Map/serialisierbaren Daten aufrufen; die Antwort
  ueber Task-Continuation auswerten. Ergebnis-Felder defensiv casten (Server liefert generisches Objekt).
  Quelle: https://firebase.google.com/docs/functions/callable

### C3 — Fehlerbehandlung ueber FirebaseFunctionsException.Code
- **offiziell** Bei Fehlern wirft der Client `FirebaseFunctionsException`; `code` ist auf gRPC-Status
  gemappt (z.B. `INVALID_ARGUMENT`, `PERMISSION_DENIED`, `UNAUTHENTICATED`, `DEADLINE_EXCEEDED`, `INTERNAL`).
  Wirft der Server keinen typisierten `HttpsError`, bekommt der Client generisch `INTERNAL`. Recovery
  pro Code differenzieren (retry vs. UI-Hinweis vs. Login). Niemals Fehlerbehandlung weglassen.
  Quelle: https://firebase.google.com/docs/reference/kotlin/com/google/firebase/functions/FirebaseFunctionsException.Code ;
  https://firebase.google.com/docs/functions/callable

### C4 — App-Check-Enforcement fuer callable Functions
- **offiziell** Die Functions-Client-SDKs haengen App-Check-Tokens automatisch an callable Aufrufe an.
  Serverseitig `enforceAppCheck: true` setzen — danach werden ALLE unverifizierten Requests abgewiesen
  (keine manuelle Pruefung im Code noetig). Vor breitem Enforcement zuerst Metriken/Impact pruefen (A4).
  Quelle: https://firebase.google.com/docs/app-check/cloud-functions

### C5 — Replay-Schutz nur fuer sensible Endpunkte (siehe A6)
- **offiziell** `consumeAppCheckToken: true` am Server + Limited-Use-Token am Client; Service-Account
  braucht die Rolle "Firebase App Check Token Verifier". Latenzkosten bedenken — nicht universell.
  Quelle: https://firebase.google.com/docs/app-check/cloud-functions

### C6 — Secrets via Secret Manager, nicht im Code
- **offiziell** Geheimnisse (API-Keys, Modell-Keys) ueber Cloud Secret Manager bereitstellen und der
  Function als Secret binden — NIEMALS im Function-Code/Repo hardcoden. 2nd-gen Functions bevorzugen
  (bessere Concurrency/Cold-Start/Skalierung). Least-Privilege fuer den Function-Service-Account.
  Quelle: https://firebase.google.com/docs/functions/config-env ;
  https://firebase.google.com/docs/functions/manage-functions

### C7 — Cold-Start / Timeouts / Idempotenz
- **offiziell/extern** Cold-Starts und Timeouts einplanen: Client-seitig sinnvolle Timeouts/Retry-Strategie,
  Server-seitig idempotente Logik fuer den Fall von Retries (gleicher Aufruf darf nicht doppelt wirken,
  z.B. bei Abrechnungs-/Schreiboperationen). 2nd-gen: min-instances/Concurrency gegen Cold-Start.
  Quelle (offiziell): https://firebase.google.com/docs/functions/manage-functions ;
  https://firebase.google.com/docs/functions/callable

### C8 — SDK-Mindestversion fuer App Check + Streaming
- **offiziell** App-Check-Attestierung fuer Functions braucht Firebase-Android-Functions-SDK >= 22.1.1
  (in BOM 34.11.0 erfuellt). Fuer Streaming-Funktionen `.stream()` + `asFlow()` (benoetigt
  `kotlinx-coroutines-reactive`).
  Quelle: https://firebase.google.com/docs/functions/callable

## D. Zusammenspiel (RC steuert, Functions verifiziert, App Check schuetzt)

### D1 — Saubere Rollenverteilung
- **offiziell** Empfohlenes Muster fuer eine App wie BestJournalAndroid:
  - **Remote Config** = steuert Feature-Flags, Modellnamen/Parameter, Rollout-Prozente clientseitig
    (typsicher gelesen, mit XML-Defaults als Fallback).
  - **Cloud Functions (callable)** = sicherer Server-Endpunkt fuer alles, was Secrets oder
    serverseitige Logik braucht (z.B. KI-Aufrufe mit Secret-Manager-Keys), statt Keys im Client.
  - **App Check** = schuetzt BEIDE (RC-Abruf + Function-Aufruf), sodass nur attestierte App-Instanzen
    Zugriff haben; Enforcement fuer beide Dienste gestaffelt aktivieren (A4).
  Quelle: https://firebase.google.com/docs/app-check ;
  https://firebase.google.com/docs/remote-config/use-cases ;
  https://firebase.google.com/docs/functions/callable

### D2 — Reihenfolge der Initialisierung
- **offiziell** App Check zuerst (Application.onCreate, A1), erst danach RC-Fetch und Function-Aufrufe —
  sonst koennen frueh ausgeloeste RC-/Function-Requests ohne gueltiges App-Check-Token gegen einen bereits
  enforced Dienst laufen und scheitern.
  Quelle: https://firebase.google.com/docs/app-check/android/play-integrity-provider

---

# Teil 7 — Firebase-Fundament: Setup, Init, BOM, Analytics, Consent

> Researcher 7/7. App: BestJournalAndroid. Firebase BOM 34.11.0, google-services-Plugin 4.4.2,
> AGP 8.7.3, Kotlin 2.1.0, minSdk 26, target/compileSdk 36, R8 im Release.
> Label-Konvention: `[offiziell]` = Google/Firebase-Doku, `[extern]` = Community.

---

## A. BOM-Versionsmanagement & KTX-Konsolidierung

1. **BOM IMMER via `platform(...)` einbinden, alle Firebase-Deps OHNE Version.** `[offiziell]`
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
   implementation("com.google.firebase:firebase-analytics")   // KEINE Version
   implementation("com.google.firebase:firebase-config")
   ```
   "When you use the Firebase BoM ... all the individual library versions will be compatible."
   Quelle: https://firebase.google.com/docs/android/learn-more

2. **Einzelne Firebase-Libs NIEMALS hart pinnen.** `[offiziell]` Eine abweichende Version je Lib ist
   laut Doku "not recommended. The specified version may not be compatible with other Firebase
   library versions." Genau so entsteht der Fehler "all gms/firebase libraries must use the exact
   same version". Quelle: https://firebase.google.com/docs/android/learn-more

3. **BOM regelmaessig bumpen statt einzelne Libs.** `[offiziell]` Ein einziger Versions-Bump
   (z.B. 34.11.0 → aktuellste 34.14.x) hebt alle Firebase-Libs konsistent an. Vergleichs-Widget
   in der Doku zeigt das Mapping BOM-Version → Lib-Versionen.
   Quelle: https://firebase.google.com/docs/android/learn-more

4. **KTX-Module sind ab BOM 34.0.0 (Juli 2025) entfernt.** `[offiziell]` Immer das Hauptmodul
   `com.google.firebase:firebase-xxx` nutzen, NICHT `firebase-xxx-ktx`. "we strongly recommend
   that you migrate your app to use KTX APIs from the main modules instead." BestJournalAndroid auf
   34.11.0 darf also kein `-ktx`-Suffix mehr haben (sonst nicht aufloesbar).
   Quelle: https://firebase.google.com/docs/android/learn-more

5. **Prerequisites fuer aktuelle Firebase:** minSdk 23+ (BestJournal hat 26 ✓), compileSdk 28+
   (36 ✓), google-services-Plugin 7.3.0+ bzw. aktuell 4.4.x, AndroidX. `[offiziell]`
   Quelle: https://firebase.google.com/docs/android/setup

---

## B. google-services-Plugin & SHA-Keys

1. **Plugin korrekt registrieren: Root-Block `apply false`, App-Modul anwenden.** `[offiziell]`
   ```kotlin
   // settings/root build.gradle.kts
   id("com.google.gms.google-services") version "4.4.2" apply false
   // app/build.gradle.kts (NACH dem Android-Plugin)
   id("com.google.gms.google-services")
   ```
   Quelle: https://firebase.google.com/docs/android/setup

2. **Das Plugin verarbeitet `google-services.json` → generiert Android-Ressourcen** (`values.xml`
   mit `google_app_id`, `gcm_defaultSenderId`, `firebase_database_url` etc.). Diese Ressourcen
   speisen die Auto-Initialisierung. `[offiziell]`
   Quelle: https://developers.google.com/android/guides/google-services-plugin

3. **Pro Build-Variante eigene google-services.json bei abweichendem applicationId.** `[offiziell]`
   Das Plugin sucht variantenspezifisch: `app/google-services.json` (default), dann
   `app/src/<buildType>/google-services.json` (z.B. `src/debug/`, `src/release/`), Flavors
   `app/src/<flavor>/...`, kombiniert `app/src/<flavor>/<buildType>/`. Bei `applicationIdSuffix=".debug"`
   MUSS die Debug-JSON einen Client mit exakt der Suffix-applicationId enthalten — sonst
   "No matching client found for package name". BestJournal nutzt `.debug`-Suffix → eigene
   Debug-JSON ist Pflicht. Quelle: https://developers.google.com/android/guides/google-services-plugin

4. **package_name in der JSON MUSS exakt zur applicationId passen** (inkl. Suffix). Paketname
   aendern ohne JSON-Update bricht den Build. `[offiziell]`
   Quelle: https://developers.google.com/android/guides/google-services-plugin

5. **SHA-Keys: Debug-, Upload- UND Play-App-Signing-SHA-1+SHA-256 in Firebase eintragen.** `[extern, abgeleitet]`
   Bei Play App Signing signiert Google mit einem ANDEREN Key als dem Upload-Key. Auth (Google
   Sign-In) und App Check (Play Integrity) verifizieren gegen die in Firebase hinterlegten SHAs.
   Fehlt der Play-App-Signing-SHA, brechen diese Features NACH dem Release lautlos — lokal/Debug
   laeuft alles. Quelle (Play App Signing): https://developer.android.com/studio/publish/app-signing
   Praxis: nach SHA-Aenderung in der Firebase-Console die **google-services.json neu herunterladen**
   und **clean build** (`./gradlew clean`), weil die generierten Ressourcen sonst veraltet sind.
   `[extern]`

---

## C. Initialisierung & Reihenfolge

1. **FirebaseApp wird automatisch via `FirebaseInitProvider` (ContentProvider) initialisiert.** `[offiziell]`
   Kein manueller `FirebaseApp.initializeApp()` noetig, wenn `google-services.json` vorhanden ist.
   Quelle: https://firebase.google.com/docs/android/setup

2. **Manuelle Initialisierung NUR in `Application.onCreate()`, NIE in einer Activity.** `[offiziell, abgeleitet]`
   Init in einer Activity erzeugt Race-Conditions (FirebaseInitProvider laeuft schon vor `onCreate`
   der App). Wenn ueberhaupt manuell, dann in der `Application`-Klasse.

3. **App Check als ERSTES initialisieren** (vor der ersten geschuetzten Backend-Anfrage), idealerweise
   in `Application.onCreate()` direkt nach FirebaseApp. `[extern, Praxis]` Sonst koennen erste
   Requests (z.B. an Cloud Functions / Remote Config Fetch) ohne gueltiges App-Check-Token rausgehen.

4. **R8/Keep fuer FirebaseInitProvider & Provider-Komponenten.** `[extern]` Bei aggressivem R8 im
   Release sicherstellen, dass der ContentProvider nicht entfernt/umbenannt wird. Firebase liefert
   Consumer-Keep-Regeln mit; bei eigenen ueberaggressiven `-dontwarn`/Shrink-Regeln pruefen.

5. **Cold-Start: Firebase-Init nicht den UI-Thread blockieren.** `[extern, Praxis]` Die Provider-Init
   ist schlank, aber teure Folgeschritte (Remote Config `fetchAndActivate`, App-Check-Token-Holen,
   firebase-ai-Setup) gehoeren in Coroutinen/Background, nicht synchron in `onCreate`. `androidx.startup`
   (App Startup) kann genutzt werden, um eigene Initialisierer geordnet und lazy zu staffeln — Firebase
   selbst braucht es nicht, da es bereits einen eigenen Provider mitbringt.
   Quelle (App Startup): https://developer.android.com/topic/libraries/app-startup

---

## D. Analytics richtig (Events, DebugView, keine PII)

1. **Recommended Events bevorzugen, Custom Events nur wenn noetig.** `[offiziell]` Vordefinierte
   Events (z.B. Purchase, Share) bekommen mehr Auswertungs-Features. Quelle:
   https://firebase.google.com/docs/analytics/events

2. **Event-Namen sind case-sensitive.** `[offiziell]` `level_up` und `Level_Up` sind ZWEI Events.
   Konsistente snake_case-Konvention waehlen. Max **500 verschiedene Event-Typen** pro App
   (Event-Volumen unbegrenzt). Quelle: https://firebase.google.com/docs/analytics/events

3. **Reservierte Praefixe/Namen meiden:** `firebase_`, `google_`, `ga_` sind fuer System-Events
   reserviert und duerfen nicht als Custom-Event-/Parameter-/User-Property-Namen verwendet werden.
   `[extern, Doku-Konvention]`

4. **Custom-Parameter in der Console registrieren** (Analytics → Custom Definitions), sonst tauchen
   sie nicht in Reports auf. `setDefaultEventParameters()` fuer ueber alle Events gemeinsame Params.
   `[offiziell]` Quelle: https://firebase.google.com/docs/analytics/events

5. **NIEMALS PII in Analytics** (Name, E-Mail, Telefonnummer, exakter Standort) — weder als
   Event-Param, User-Property noch User-ID-Klartext. `[offiziell]`
   Quelle: https://firebase.google.com/docs/analytics/android/configure-data-collection

6. **Advertising ID abschaltbar** via Manifest `google_analytics_adid_collection_enabled=false`
   wenn keine Ads-Features genutzt werden (datensparsam). `[offiziell]`
   Quelle: https://firebase.google.com/docs/analytics/android/configure-data-collection

7. **DebugView / Verbose-Logging zum Testen** (Events erscheinen sonst erst nach ~24h im Dashboard).
   `[offiziell]` `adb shell setprop debug.firebase.analytics.app <package>`. Analytics-Events koennen
   zudem als Crashlytics-Breadcrumbs dienen. Quelle: https://firebase.google.com/docs/analytics/events

---

## E. DSGVO / Consent Mode v2

1. **`setConsent(Map<ConsentType, ConsentStatus>)` ist die Consent-Mode-v2-API.** `[offiziell]`
   ConsentType: `ANALYTICS_STORAGE`, `AD_STORAGE`, `AD_USER_DATA`, `AD_PERSONALIZATION`.
   ConsentStatus: `GRANTED`, `DENIED`. Quelle:
   https://firebase.google.com/docs/analytics/android/configure-data-collection
   und https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.ConsentType

2. **Default-Consent VOR der Init setzen — via AndroidManifest meta-data.** `[offiziell]`
   ```xml
   <meta-data android:name="google_analytics_default_allow_analytics_storage" android:value="false"/>
   <meta-data android:name="google_analytics_default_allow_ad_storage"        android:value="false"/>
   <meta-data android:name="google_analytics_default_allow_ad_user_data"      android:value="false"/>
   <meta-data android:name="google_analytics_default_allow_ad_personalization_signals" android:value="false"/>
   ```
   Fuer EU/DSGVO Default = `false` (denied), dann nach Consent-Dialog per `setConsent()` auf GRANTED
   hochstufen (opt-in). Quelle:
   https://firebase.google.com/docs/analytics/android/configure-data-collection

3. **`setAnalyticsCollectionEnabled(boolean)` zum dynamischen An/Aus.** `[offiziell]` Wert persistiert
   ueber App-Starts. Fuer ein reines Journal ohne Ads kann analytics_storage opt-in und ad_* dauerhaft
   denied sein. Quelle: https://firebase.google.com/docs/analytics/android/configure-data-collection

4. **Region-Defaults:** Strenge Defaults (denied) global setzen und nur nach aktivem Consent lockern
   ist der sichere DSGVO-Weg (opt-in fuer EU). `[extern, Praxis]`

5. **Widerruf:** Zieht der Nutzer analytics/ad-Storage zurueck, loescht GA alle User-Properties inkl.
   ad_personalization; vorherige Werte koennen per `setConsent()` wiederhergestellt werden. `[offiziell]`
   Quelle: https://firebase.google.com/docs/analytics/android/configure-data-collection

6. **`firebase_analytics_collection_deactivated=true`** im Manifest deaktiviert Analytics dauerhaft
   (hoechste Prioritaet) — nur falls Analytics komplett aus soll. `[offiziell]`

---

## F. Sicherer Umgang mit google-services.json

1. **google-services.json ist KEIN Auth-Secret** — sie enthaelt App-Identifier/Projekt-Config, keine
   privaten Schluessel. `[offiziell, abgeleitet]` Die Sicherheit liegt in Security Rules + App Check,
   nicht in der Geheimhaltung dieser Datei.

2. **Trotzdem nicht ins oeffentliche Repo committen** — BestJournal legt sie korrekt in `$HOME/SK/`
   (Secrets-Ordner) und synct sie zur Build-Zeit ins App-Modul. Pro Variante eigene JSON. `[Projekt-Regel]`

3. **Service-Account-Keys (Admin SDK / Cloud Functions Deploy) NIEMALS ins Repo** — das sind echte
   Secrets, im Gegensatz zur google-services.json. `[offiziell]`


---

## 🔗 Bezug zum Bug-Almanach ([`bugs/android/firebase-billing.md`](../../bugs/android/firebase-billing.md))

Zweite Seite der Medaille: Hier stehen die *Praeventions-Regeln* — im Bug-Almanach
*die konkreten Fehler und ihre Fixes*. Beide Seiten zusammen lesen: Praevention hier, Schadensbehebung dort.
