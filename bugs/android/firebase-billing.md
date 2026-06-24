# Bekannte Bugs: Firebase & Google Play Billing (Android-Backend-Dienste)

> **PFLICHT-LESEN vor Arbeit an Firebase oder Google Play Billing in einer Android-App.**
> Proaktives Bug-Wissen (Poka-Yoke Stufe 3): bekannte Fehler VOR der Arbeit nachschlagen,
> statt sie hinterher teuer zu debuggen.
>
> **Stand:** recherchiert am **2026-06-02**, **re-recherchiert am 2026-06-24** (Engine A: Firecrawl+MiniMax)
> fuer die LIVE-Versionen aus
> **Anker:** billing=7.1.1  <!-- maschinenlesbar fuer check-version-anchor.py -->
> `~/proggs/BestJournalAndroid/gradle/libs.versions.toml`:
> - **Google Play Billing Library 7.1.1** (`com.android.billingclient:billing-ktx`)
> - **Firebase BOM 34.11.0** — eingebunden: `firebase-ai` (Gemini), App Check (Play Integrity + Debug),
>   Remote Config, Analytics, Cloud Functions
> - **Play In-App Review 2.0.2**, **google-services Gradle-Plugin 4.4.2**
> - AGP 8.7.3 · Kotlin 2.1.0 · compileSdk/targetSdk 36 · minSdk 26 · R8/ProGuard im Release **AKTIV**
>
> **Wichtig — proaktiv:** **Crashlytics**, **FCM/Cloud Messaging** und **Firestore** sind aktuell **NICHT**
> eingebunden. Ihre Bug-Sektionen (3, 4 + 12) sind Zukunftswissen fuer den spaeteren Einbau und entsprechend markiert.
>
> **Praeventions-Seite (zweite Seite der Medaille):** seit 2026-06-02 existiert die Best-Practices-Seite
> [`best-practices/android/firebase-billing.md`](../../best-practices/android/firebase-billing.md)
> (7-Researcher-Lauf) — *wie man es von vornherein richtig macht*. Wechselseitige Abschnitts-Kopplung am Dateiende.
> Teil 11 (Bugs 123–133) + Teil 12 (Firestore 134–138) wurden aus diesem Best-Practices-Lauf zurueckgekoppelt.
>
> **Versions-Anker:** Billing 7.1.1 ist NICHT die neueste Version (Billing 8.x existiert seit 2025-06-30).
> Das ist relevant — siehe Sektion 1 (v8-Migration) und die **Play-Store-Deadline 31.08.2026** (Bug 41).
> Beim naechsten Major-Sprung (Billing 8, Firebase-BOM-Bump): Re-Check dieses Almanachs.
>
> **Versions-Horizont (Re-Recherche 2026-06-24):**
> - **Play Billing Library:** 8.0.0 (2025-06-30), 8.1.0 (2025-11-06), 8.2.0 (2025-12-09), 8.2.1 (2025-12-15),
>   8.3.0 (2025-12-23) — und **PBL 9.0.0** (2026-05-19, kleinere API-Oberflaeche als v8, intern targetSdk 35).
>   Euer 7.1.1 muss bis zur Deadline auf **>= v8** (Bug 41).
> - **Firebase BOM:** 34.11.0 → 34.12.0 (2026-04-09) → 34.13.0 (2026-05-07) → 34.14.0 (2026-05-28) →
>   34.14.1 (2026-06-08) → **34.15.0 (2026-06-16)**; Firebase AI Logic 17.11.0 → **17.13.0**, App Check 19.2.0.
> - **Play-Gebuehren/Policy-Umbruch ("A new era for choice and openness", Maerz 2026):** eigene
>   Billing-Systeme + externes Web-Checkout in US/UK/EWR erlaubt; **neue Gebuehrenstruktur ab 2026-06-30**
>   (5% Billing-Fee + Service-Fees, 10% fuer wiederkehrende Subs; Rollout US/UK/EWR 06-30, AU 09-30,
>   KR/JP 12-31, Rest 2027). Awareness fuer Monetarisierung — kein Code-Bug, aber relevant.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter
> Arbeit hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der
> Schnell-Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Kauf wird PURCHASED | Binnen 3 Tagen acknowledgen, sonst Auto-Refund | Bug 14 |
| 2 | `PurchaseState.PENDING` | Nie freischalten, erst bei PURCHASED | Bug 18 |
| 3 | BillingClient bauen | `enablePendingPurchases(PendingPurchasesParams…)` Pflicht ab v7 | Bug 1 |
| 4 | Verbindung abgebrochen | Lazy-Reconnect via `isReady()`, nicht im Disconnect-Callback | Bug 2/3 |
| 5 | Abo kaufen | `offerToken` Pflicht, sonst schlaegt der Flow fehl | Bug 23 |
| 6 | Plan-Wechsel (Upgrade/Downgrade) | Als Replace mit `oldPurchaseToken`, nicht Neukauf | Bug 26 |
| 7 | App-Start | `queryPurchasesAsync` (SUBS+INAPP) zur Recovery laufen lassen | Bug 22 |
| 8 | Abo-Status pruefen | Nie nur lokal; ON_HOLD fehlt im Client, Server ist Wahrheit | Bug 30 |
| 9 | Entitlement gewaehren | Serverseitig per `subscriptionsv2.get` verifizieren (nicht v1) | Bug 29/36 |
| 10 | google-services.json geaendert | `./gradlew clean` (sonst stale) | Bug 44 |
| 11 | `applicationIdSuffix` (`.debug`) | Eigene JSON pro Variante (`src/debug/`) | Bug 42 |
| 12 | Release nach Store-Upload | App-Signing-Key-SHA in Firebase, sonst Auth/App-Check still kaputt | Bug 45 |
| 13 | Firebase-Deps | Alle ueber BOM ohne Version; kein `-ktx`-Suffix ab BOM 34 | Bug 46/47 |
| 14 | App Check Enforcement | Erst Client-Rollout, dann erzwingen; Debug-Token in Konsole | Bug 89/90 |
| 15 | R8/Release aktiv | Keep-Regeln fuer Firebase/Billing/Gemini; Release-Build testen | Bug 113 |
| 16 | Billing 7.1.1 im Einsatz | Vor 31.08.2026 auf Billing 8 migrieren | Bug 41 |

---

# Teil 1 — Google Play Billing Library (7.1.1)

## A. BillingClient: Verbindung & Lebenszyklus

## 1. enablePendingPurchases() ohne Parameter — deprecated/entfernt   ⭐ HAEUFIG
**Symptom:** Compile-Warnung ab v7, harter Fehler ("method not found") ab v8; Client baut nicht.
**Ursache:** Ab 7.0.0 ersetzt `PendingPurchasesParams` den parameterlosen Aufruf; ab 8.0.0 ist die no-arg-Variante entfernt.
**Versionen:** deprecated ab 7.0.0, entfernt ab 8.0.0 (betrifft 7.1.1 als Warnung).
**FIX:** `.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())`. Fuer Prepaid-Abos zusaetzlich `.enablePrepaidPlans()`. Exakt aequivalent — nichts geht verloren.
**Quelle:** developer.android.com/google/play/billing/release-notes

## 2. Fehlendes Reconnect nach onBillingServiceDisconnected   ⭐ HAEUFIG
**Symptom:** Nach Verbindungsabbruch schlagen alle Calls mit `SERVICE_DISCONNECTED` (-1) fehl; Produkte/Kaeufe laden nie wieder.
**Ursache:** `onBillingServiceDisconnected()` leer ohne Reconnect-Strategie.
**Versionen:** per Design alle; Auto-Reconnect erst ab 8.0.0.
**FIX:** Bis v7 (also 7.1.1): in `onBillingServiceDisconnected` `startConnection()` mit Backoff erneut aufrufen. Besser noch (Bug 3): lazy reconnect — vor jedem Call `isReady()` pruefen und bei Bedarf verbinden. Ab v8: `enableAutoServiceReconnection()` setzen, Callback nur fuer Logging/UI.
**Quelle:** developer.android.com/google/play/billing/integrate · /errors

## 3. Reconnect-Loop nach Geraete-Sprachwechsel   ⭐ HAEUFIG
**Symptom:** Endlosschleife: `onBillingSetupFinished` feuert ~9x (einmal pro Thread) mit wechselnd `BILLING_UNAVAILABLE`/`OK`, sofort gefolgt von `onBillingServiceDisconnected`. Reconnectet nie, bis Geraete-Neustart/Play-Cache-Leeren. Battery-Drain.
**Ursache:** Sprachwechsel waehrend bestehender Verbindung; BillingClient feuert den Disconnect-Callback dauerhaft.
**Versionen:** per Design v5–v7; ab v8 mit Auto-Reconnect entschaerft.
**FIX (RevenueCat-bewaehrt):** KEINE Reconnects im `onBillingServiceDisconnected`-Callback ausloesen. Stattdessen lazy reconnect vor jedem Call via `isReady()`. KEINE Retries auf `BILLING_UNAVAILABLE` waehrend Setup (verhindert den Loop).
**Quelle:** revenuecat.com/blog/engineering/error-handling-google-play-billing-library/

## 4. SERVICE_DISCONNECTED (-1) vs. SERVICE_UNAVAILABLE (2) — falsche Retry-Strategie
**Symptom:** Retries wirken nicht, weil bei -1 die ganze Verbindung weg ist (nicht nur die Operation).
**Ursache:** Verwechslung der beiden Codes.
**Versionen:** alle.
**FIX:** -1 → komplett reconnecten (bzw. Auto-Reconnect ab v8). 2 → nur die EINE Operation per Backoff wiederholen (Verbindung intakt). User-initiiert: simpler Retry; Background: exponential backoff.
**Quelle:** developer.android.com/google/play/billing/errors

## 5. SERVICE_TIMEOUT (-3) entfernt
**Symptom:** Code referenziert `SERVICE_TIMEOUT`, das nicht mehr geliefert wird; Timeout-Faelle laufen ins Leere.
**Ursache:** Ab 6.0.0 entfernt; Timeouts kommen jetzt als `SERVICE_UNAVAILABLE` (2).
**Versionen:** entfernt ab 6.0.0 (betrifft 7.1.1).
**FIX:** Timeout-Handling auf `SERVICE_UNAVAILABLE` umstellen → Operation per Backoff wiederholen.
**Quelle:** developer.android.com/google/play/billing/errors · /release-notes

## 6. queryProductDetailsAsync liefert leere Liste   ⭐ HAEUFIG
**Symptom:** Leere Produktliste, oft OHNE Fehlercode; Paywall bleibt leer (sporadisch oder konstant).
**Ursache:** (a) Call vor `isReady()`/`onBillingSetupFinished`; (b) Produkt-IDs != Play Console; (c) App in keinem Track veroeffentlicht; (d) Propagations-Delay (Minuten–Stunden); (e) falscher ProductType; (f) Land/Tester nicht eingerichtet (siehe Bug 38).
**Versionen:** per Design/Konfig, alle.
**FIX:** Erst nach `isReady()==true` queryen. IDs 1:1 gegen Console. App mind. in internem Test veroeffentlichen, Lizenz-Tester nutzen, Propagation abwarten. Paywall NIE "weglassen" — Logik absichern + Retry.
**Quelle:** support.google.com/googleplay/android-developer/thread/255042464 · flutter/flutter#149582

## 7. queryProductDetailsAsync — Response-Struktur geaendert (v8)
**Symptom:** Nach Upgrade auf 8.0 verhaelt sich der Response-Listener anders; nicht-ladbare Produkte tauchen jetzt auf.
**Ursache:** Bis 7.x wurden nicht abrufbare Produkte still weggelassen; ab 8.0.0 kommen ALLE Produkte mit produkt-level Status-Code (`QueryProductDetailsResult` mit `UnfetchedProduct`).
**Versionen:** Breaking ab 8.0.0 (bei v8-Migration beachten).
**FIX:** Listener auf die neue Struktur umstellen, pro Produkt den Status pruefen statt nur die Erfolgsliste. Verbessert die Diagnose (man sieht WARUM ein Produkt fehlt).
**Quelle:** developer.android.com/google/play/billing/migrate-gpblv8

## 8. SKU-/History-APIs entfernt (v8): querySkuDetailsAsync, queryPurchaseHistory, sync queryPurchases
**Symptom:** Nach v8-Migration fehlende Methoden, Build bricht; verpasste Kaeufe.
**Ursache:** SKU→Product-Migration abgeschlossen; in 8.0.0 endgueltig entfernt (`queryPurchaseHistory` schon ab 7.0 deprecated).
**Versionen:** deprecated 7.0.0, entfernt 8.0.0.
**FIX:** `querySkuDetailsAsync`→`queryProductDetailsAsync`; sync `queryPurchases`→`queryPurchasesAsync` mit `QueryPurchasesParams`. Statt History: aktive Kaeufe per `queryPurchasesAsync`, abgelaufene/konsumierte/stornierte serverseitig bzw. ueber Voided-Purchases-API tracken.
**Weitere v8-Entfernungen (Re-Recherche 2026-06-24, fuer Vollstaendigkeit):** `setOldSkuPurchaseToken`→`setOldPurchaseToken`,
`setReplaceProrationMode`/`setReplaceSkusProrationMode`→`setSubscriptionReplacementMode` (vgl. Bug 24/25);
Alternative-Billing umbenannt: `enableAlternativeBilling`→`enableUserChoiceBilling`, `AlternativeBillingListener`→`UserChoiceBillingListener`,
`AlternativeChoiceDetails`→`UserChoiceDetails`. Neu in v8: APIs fuer One-Time-Products, pending purchases bei Prepaid-Plans,
virtual installment subscriptions. Auch die Signatur von `ProductDetailsResponseListener.onProductDetailsResponse` aendert sich.
**Quelle:** developer.android.com/google/play/billing/migrate-gpblv8

## 9. endConnection() vergessen → Memory Leak
**Symptom:** LeakCanary zeigt geleakte Activity/Listener; Service-Binding + BroadcastReceiver bleiben.
**Ursache:** `endConnection()` nicht aufgerufen, bzw. BillingClient an Activity-Context gebunden.
**Versionen:** per Design, alle.
**FIX:** BillingClient als **Application-Scope-Singleton** fuehren (nicht pro Activity), Application-Context nutzen, Listener nach Gebrauch loesen. `endConnection()` wenn der Client wirklich nicht mehr gebraucht wird.
**Quelle:** developer.android.com/reference/com/android/billingclient/api/BillingClient · lightrun.com (BillingBroadcastReceiver Leak)
**Hinweis (nicht hart verifiziert):** Berichte ueber Rest-Leak (interne Handler halten Listener-Referenzen) auch nach `endConnection()` — Singleton-Ansatz macht den Leak einmalig statt pro Activity. Status offen.

## 10. Callback auf falschem Thread / ANR
**Symptom:** ANRs oder UI-Crashes; UI-Updates aus dem Callback schlagen fehl.
**Ursache:** `@UiThread`-Methoden (z.B. `launchBillingFlow`) vom falschen Thread; schwere Arbeit/Netzwerk synchron im UI-Callback.
**Versionen:** per Design, alle (7.1.0 brachte Thread-Safety-Verbesserungen).
**FIX:** `@UiThread`-Methoden vom Main-Thread aufrufen; Validierung/Netzwerk im Callback in eine Coroutine/Background auslagern.
**Quelle:** developer.android.com/reference/com/android/billingclient/api/BillingClient

## 11. BILLING_UNAVAILABLE (3) faelschlich als wiederholbar behandelt
**Symptom:** Auto-Retries auf Code 3 → Loop, Kauf gelingt nie.
**Ursache:** Code 3 ist meist user-/umgebungsbedingt: veralteter Play Store, nicht unterstuetztes Land, Enterprise-Sperre, nicht belastbares Zahlungsmittel.
**Versionen:** per Design, alle.
**FIX:** KEIN Auto-Retry. Fehlermeldung + manueller "Erneut versuchen"-Button; User muss Ursache beheben.
**Quelle:** developer.android.com/google/play/billing/errors

## 12. FEATURE_NOT_SUPPORTED ohne vorherige Pruefung
**Symptom:** Feature-Call (In-App-Messaging, Prepaid …) schlaegt auf aelterem Play Store fehl.
**Ursache:** Feature auf dem Geraet nicht unterstuetzt, kein Check.
**Versionen:** per Design, alle.
**FIX:** Vor Nutzung `billingClient.isFeatureSupported(FeatureType.X)` pruefen, Feature nur dann anbieten (graceful fallback statt Crash).
**Quelle:** developer.android.com/google/play/billing/errors

## 13. Samsung "+999 intent connections"-Crash bei startConnection
**Symptom:** Crash beim `startConnection()` auf bestimmten Samsung-Geraeten.
**Ursache:** OEM-spezifischer Intent-Connection-Fehler.
**Versionen:** OEM-spezifisch, alle.
**FIX:** `startConnection` in try/catch wrappen, Exception als Verbindungsfehler behandeln (lazy retry) statt App-Crash. (Frank-relevant: Galaxy S23 Ultra / Fold 6 sind Samsung-Geraete!)
**Quelle:** revenuecat.com/blog/engineering/error-handling-google-play-billing-library/

## B. Purchase-Lifecycle: Acknowledge, Consume, Pending

## 14. Vergessenes acknowledgePurchase → Auto-Refund nach 3 Tagen   ⭐⭐ KRITISCH/HAEUFIG
**Symptom:** Nutzer bezahlt, bekommt das Feature, nach ~3 Tagen automatischer Refund + Entzug; "verschwundene" Kaeufe.
**Ursache:** Jeder Kauf mit neuem `purchaseToken` (Erstkauf, Plan-Wechsel, Re-Signup) MUSS binnen 3 Tagen acknowledged werden. Renewals brauchen KEIN erneutes Ack. Prepaid-Plaene <1 Woche: nur halbe Plan-Dauer als Fenster.
**Versionen:** per Design ab Billing 2.0, aktuell.
**FIX:** Nach Verifikation `acknowledgePurchase()` (oder serverseitig `purchases.products.acknowledge`). VORHER `purchase.isAcknowledged`/`acknowledgementState` pruefen (idempotent). Retry + erneut beim App-Start pruefen (transiente Fehler: SERVICE_UNAVAILABLE/DISCONNECTED/ERROR). Drei Researcher-Quellen bestaetigen diesen Bug unabhaengig.
**Quelle:** developer.android.com/google/play/billing/lifecycle/one-time · /lifecycle/subscriptions

## 15. 3-Tage-Acknowledge-Fenster startet erst bei PURCHASED
**Symptom:** Unsicherheit, ob PENDING-Kaeufe verfallen.
**Ursache:** Das Fenster laeuft NICHT waehrend `PENDING` — erst ab Wechsel zu `PURCHASED`.
**Versionen:** per Design.
**FIX:** PENDING nicht acknowledgen/freischalten; bei Wechsel zu PURCHASED (per RTDN/`queryPurchasesAsync`) dann acknowledgen.
**Quelle:** developer.android.com/google/play/billing/lifecycle/one-time

## 16. consumeAsync vs. acknowledgePurchase verwechselt
**Symptom:** Non-consumable wird "verbraucht" (geht verloren); ODER consumable kann nie wieder gekauft werden (ITEM_ALREADY_OWNED).
**Ursache:** Falsche API pro Produkttyp. `consumeAsync()` acknowledged automatisch.
**Versionen:** per Design.
**FIX:** Consumable → `consumeAsync()` (impliziert Ack, kein extra Ack noetig). Non-consumable/Abo → NUR `acknowledgePurchase()`.
**Quelle:** developer.android.com/google/play/billing/lifecycle/one-time

## 17. consumeAsync schlaegt fehl → "item not owned" beim Re-Kauf
**Symptom:** Nutzer hat Content erhalten, kann das gleiche Consumable aber nicht erneut kaufen (ITEM_ALREADY_OWNED).
**Ursache:** Consume nach Netzfehler nie durchgelaufen; Token noch in der Library des Nutzers.
**Versionen:** per Design.
**FIX:** Pending-Consume-Tokens lokal tracken, beim App-Start erneut consumen. `purchase.quantity` beachten (Multi-Quantity: nicht nur 1 Einheit gewaehren).
**Quelle:** revenuecat.com/blog/engineering/google-play-edge-cases/

## 18. PENDING-Kauf sofort faelschlich freigeschaltet   ⭐ HAEUFIG
**Symptom:** Bar/Ueberweisung/Slow-Test-Card: App schaltet sofort frei, Geld kommt evtl. nie.
**Ursache:** `PurchaseState.PENDING` nicht behandelt; nur `responseCode==OK` geprueft.
**Versionen:** per Design.
**FIX:** In `onPurchasesUpdated` `getPurchaseState()` pruefen, nur `PURCHASED` freischalten. PENDING-Token speichern, auf RTDN-Wechsel warten. `enablePendingPurchases(PendingPurchasesParams…)` ist ab v7 Pflicht (Bug 1).
**Quelle:** revenuecat.com/blog/engineering/google-play-edge-cases/

## 19. onPurchasesUpdated: purchases == null trotz OK
**Symptom:** Callback feuert mit `responseCode OK`, aber die purchases-Liste ist null → NPE.
**Ursache:** Seltener Edge-Case in bestimmten Fehlerzustaenden.
**Versionen:** per Design.
**FIX:** Immer null-safe (`purchases?.forEach`), zuerst `billingResult.responseCode` pruefen.
**Quelle:** revenuecat.com/blog/engineering/google-play-edge-cases/

## 20. USER_CANCELED als Fehler behandelt / Auto-Retry
**Symptom:** Fehlermeldung obwohl der Nutzer absichtlich abgebrochen hat; nervige Re-Open-Schleife.
**Ursache:** `USER_CANCELED` (1) nicht abgefangen.
**Versionen:** per Design.
**FIX:** `USER_CANCELED` still behandeln, KEIN Auto-Retry.
**Quelle:** developer.android.com/google/play/billing/errors

## 21. ITEM_ALREADY_OWNED nach Reinstall/Crash
**Symptom:** Kauf schlaegt mit "you already own this item" fehl, v.a. nach App-Neuinstallation oder Crash waehrend des Kaufs.
**Ursache:** Unacknowledged Kauf existiert noch im Account; ODER veralteter Play-Cache; ODER Consumable nicht consumed.
**Versionen:** per Design.
**FIX:** KEINE Fehlermeldung. Bei ITEM_ALREADY_OWNED `queryPurchasesAsync()` aufrufen, unacknowledged PURCHASED-Kaeufe verarbeiten/acknowledgen. Beim App-Start proaktiv recovern.
**Quelle:** adapty.io/blog/google-play-billing-library-in-app-purchase-error-codes/ · developer.android.com/google/play/billing/errors

## 22. queryPurchasesAsync vor Verbindung → leer / verpasste Kaeufe
**Symptom:** Aktive Abos werden nicht gefunden, Kaeufe gehen "verloren".
**Ursache:** Vor abgeschlossenem `startConnection()` oder nach Disconnect aufgerufen.
**Versionen:** per Design.
**FIX:** Nur in `onBillingSetupFinished()` aufrufen; beim App-Start IMMER laufen lassen (verpasste/unacknowledged Kaeufe einsammeln); Reconnect mit Backoff (ab v8 Auto-Reconnect).
**Quelle:** revenuecat.com/blog/engineering/google-play-edge-cases/

## C. Subscriptions & Proration (Billing-5-Modell)

## 23. offerToken vergessen → Abo-Kauf schlaegt fehl   ⭐ HAEUFIG
**Symptom:** `launchBillingFlow` fuer ein Abo schlaegt fehl / wirft bei leerem offerToken.
**Ursache:** Ab Billing 5 base plans + offers. `setOfferToken()` ist Pflicht; auch reine base plans liefern einen offerToken in `subscriptionOfferDetails`.
**Versionen:** ab Billing 5 (betrifft 7.1.1).
**FIX:** `productDetails.subscriptionOfferDetails?.get(idx)?.offerToken` holen und an `ProductDetailsParams` setzen, bevor `launchBillingFlow` aufgerufen wird.
**Quelle:** developer.android.com/reference/com/android/billingclient/api/ProductDetails.SubscriptionOfferDetails

## 24. replaceProrationMode / ProrationMode entfernt (v8)
**Symptom:** Build-Fehler nach v8-Migration; falsche/keine Proration bei Upgrade/Downgrade.
**Ursache:** `BillingFlowParams.ProrationMode`, `setReplaceProrationMode()`, `setReplaceSkusProrationMode()` in v8 entfernt.
**Versionen:** entfernt in 8.0.0 (2025-06-30); in 7.1.1 noch vorhanden.
**FIX:** `SubscriptionUpdateParams.ReplacementMode` + `setSubscriptionReplacementMode(int)` (WITH_TIME_PRORATION, CHARGE_FULL_PRICE, CHARGE_PRORATED_PRICE, DEFERRED, WITHOUT_PRORATION). Hinweis: `setSubscriptionReplacementMode` ist Richtung v9 selbst zugunsten `SubscriptionProductReplacementParams#setReplacementMode` deprecated.
**Quelle:** developer.android.com/google/play/billing/migrate-gpblv8

## 25. setOldSkuPurchaseToken entfernt (v8)
**Symptom:** Build-Fehler bei v8-Migration.
**Ursache:** `setOldSkuPurchaseToken()` entfernt.
**Versionen:** entfernt 8.0.0.
**FIX:** `setOldPurchaseToken(String)` verwenden.
**Quelle:** developer.android.com/google/play/billing/migrate-gpblv8

## 26. "You already own this item" beim Plan-Wechsel
**Symptom:** Upgrade/Downgrade scheitert mit ITEM_ALREADY_OWNED.
**Ursache:** Plan-Wechsel als Neukauf statt als Replace gestartet (`oldPurchaseToken`/`ReplacementMode` fehlt).
**Versionen:** per Design.
**FIX:** `SubscriptionUpdateParams` mit `oldPurchaseToken` + `ReplacementMode` setzen statt eines regulaeren Kaufs.
**Quelle:** developer.android.com/google/play/billing/subscriptions

## 27. DEFERRED-Downgrade missverstanden / linkedPurchaseToken ignoriert
**Symptom:** Downgrade zeigt weiter alte Features; ODER Subscriber-Zahlen/Entitlements doppeln sich.
**Ursache:** DEFERRED = Wechsel erst beim naechsten Renewal (altes Abo bleibt aktiv). Beim Wechsel entsteht ein neuer purchaseToken mit `linkedPurchaseToken` aufs alte → wird als Neukauf fehlinterpretiert.
**Versionen:** per Design.
**FIX:** Backend muss `linkedPurchaseToken` pruefen, Nutzer per altem Token finden, auf neuen migrieren, alten invalidieren (kein Doppel-Entitlement). UI: "Wechsel wirksam am [Datum]".
**Quelle:** revenuecat.com/blog/engineering/google-play-edge-cases/ · /google-proration/

## D. launchBillingFlow & Sicherheit

## 28. DEVELOPER_ERROR bei launchBillingFlow
**Symptom:** `launchBillingFlow` gibt DEVELOPER_ERROR (5, fatal).
**Ursache:** Falsche Parameter (leerer offerToken, fehlende `ProductDetailsParams`), null/zerstoerte Activity.
**Versionen:** per Design.
**FIX:** `debugMessage` lesen; gueltige (nicht zerstoerte) Activity uebergeben; Params korrekt bauen (Bug 23).
**Quelle:** developer.android.com/google/play/billing/errors

## 29. Rein clientseitiges Vertrauen / fehlende Signature-Verification + obfuscatedAccountId   ⭐ HAEUFIG
**Symptom:** Spoofing/Missbrauch auf gerooteten Geraeten; Kaeufe nicht eindeutig Nutzer zuordenbar; Refunds nicht erkannt.
**Ursache:** Entitlement nur aus dem In-App-Purchase-Objekt abgeleitet, ohne Server-Verifikation; `setObfuscatedAccountId()` nicht gesetzt.
**Versionen:** per Design.
**FIX:** purchaseToken serverseitig per Play Developer API verifizieren, Entitlement im Backend fuehren, RTDN als Quelle der Wahrheit (idempotent, Token-Dedup). `setObfuscatedAccountId(userId)` (+ optional ProfileId) im Flow fuer Fraud-Schutz. Client-Cache nur als Schnellanzeige. Voided-Purchases-API fuer Refund-Erkennung.
**Quelle:** developer.android.com/google/play/billing/integrate · /lifecycle/one-time

## E. Abo-Status-Sync, RTDN & Play Developer API

## 30. ON_HOLD-Abos fehlen in queryPurchasesAsync()   ⭐ HAEUFIG
**Symptom:** App denkt, der Nutzer hat kein Abo, obwohl er nur in Zahlungsverzug (account hold) ist → falsche Re-Onboarding-Paywall.
**Ursache:** `queryPurchasesAsync()` liefert Subs im `ON_HOLD`-Zustand NICHT (Grace-Period-Abos werden geliefert, On-Hold nicht).
**Versionen:** per Design, alle.
**FIX:** On-Hold-Status NUR ueber Backend + Play Developer API (`subscriptionsv2.get`) oder RTDN erkennen, nicht ueber den Client. Bei On-Hold Zugang sperren, aber Recovery-CTA zeigen.
**Quelle:** developer.android.com/google/play/billing/lifecycle/subscriptions

## 31. Grace Period / Account Hold / Paused nicht behandelt → falsche Stornierungs-Erkennung
**Symptom:** Nutzer kuendigt/zahlt nicht, App merkt es zu spaet oder gar nicht; ODER App sperrt waehrend Grace faelschlich.
**Ursache:** App vertraut nur dem lokalen Entitlement-Cache, ignoriert `IN_GRACE_PERIOD`, `ON_HOLD`, `PAUSED`, `CANCELED`, `EXPIRED`.
**Versionen:** per Design.
**FIX:** RTDN-Typen behandeln: `SUBSCRIPTION_IN_GRACE_PERIOD`(6), `SUBSCRIPTION_ON_HOLD`(5), `SUBSCRIPTION_RECOVERED`(1), `SUBSCRIPTION_EXPIRED`(13), `SUBSCRIPTION_CANCELED`(3). Bei Grace: nicht-blockierender Banner, Zugang behalten. Bei On-Hold: Zugang aus + CTA.
**Quelle:** developer.android.com/google/play/billing/lifecycle/subscriptions · proandroiddev.com (account hold guide)

## 32. RTDN: Pub/Sub-Publisher-Rolle fehlt → keine Notifications
**Symptom:** Topic in Play Console eingetragen, aber es kommen nie Notifications; Renewals/Cancellations verpasst.
**Ursache:** Der Google-managed Account `google-play-developer-notifications@system.gserviceaccount.com` hat keine `roles/pubsub.publisher` auf dem Topic.
**Versionen:** serverseitig (library-unabhaengig).
**FIX:** `gcloud pubsub topics add-iam-policy-binding <topic> --member="serviceAccount:google-play-developer-notifications@system.gserviceaccount.com" --role="roles/pubsub.publisher"`, dann vollen Topic-Namen in Play Console eintragen.
**Quelle:** developer.android.com/google/play/billing/getting-ready

## 33. RTDN traegt keine Details — nur Trigger
**Symptom:** Backend bekommt die Notification, kennt aber nicht den vollen Abo-Zustand.
**Ursache:** RTDN signalisiert nur "etwas hat sich geaendert" (purchaseToken + notificationType).
**Versionen:** per Design.
**FIX:** Nach JEDER RTDN `purchases.subscriptionsv2.get` aufrufen, um den echten Zustand zu holen.
**Quelle:** developer.android.com/google/play/billing/rtdn-reference

## 34. Domain-Restricted-Sharing blockiert RTDN-Publisher
**Symptom:** IAM-Binding fuer den Google-System-Account schlaegt fehl (Org-Policy).
**Ursache:** Org-Policy `iam.allowedPolicyMemberDomains` verbietet externe Principals.
**Versionen:** serverseitig.
**FIX:** Ausnahme fuer den Google-Play-Service-Account in der Org-Policy hinzufuegen.
**Quelle:** revenuecat.com/docs/platform-resources/server-notifications/google-server-notifications

## 35. 403 "The current user has insufficient permissions" (Developer API)
**Symptom:** `subscriptionsv2.get`/`subscriptions.get` liefert 403, obwohl der Service-Account angeblich berechtigt ist.
**Ursache:** Service-Account nicht (korrekt) in Play Console; ODER Cloud-Projekt nicht mit dem Play-Account verknuepft; ODER API nicht aktiviert; ODER Propagations-Verzoegerung nach Rechtevergabe.
**Versionen:** serverseitig (v3).
**FIX:** 1) Cloud-Projekt mit Play Console verknuepfen, 2) "Google Play Android Developer API" aktivieren, 3) Service-Account in Play Console (Users & Permissions) mit Finanz-/App-Rechten, 4) Scope `androidpublisher`, 5) nach Aenderung bis zu 24h warten (Community-Erfahrung, keine Garantie).
**Quelle:** developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2/get

## 36. Veraltetes subscriptions.get statt subscriptionsv2.get
**Symptom:** Multi-BasePlan/Multi-Offer-Abos werden nicht korrekt abgebildet; Felder fehlen.
**Ursache:** `purchases.subscriptions.get` (v1) ist deprecated, kennt das neue Modell (`lineItems`, base plans, offers) nicht vollstaendig.
**Versionen:** Server-API v3.
**FIX:** Auf `purchases.subscriptionsv2.get` migrieren (`SubscriptionPurchaseV2` mit `lineItems[].productId`); Katalog ueber `monetization.subscriptions(.baseplans/.offers)` verwalten.
**Quelle:** developer.android.com/google/play/billing/play-developer-apis-deprecations

## 37. Restore: queryPurchasesAsync findet nach Reinstall/Geraetewechsel nicht alle Kaeufe
**Symptom:** "Restore purchases" findet das aktive Abo nicht.
**Ursache:** (a) On-Hold-Subs werden nicht geliefert (Bug 30); (b) `queryPurchaseHistoryAsync` in v7/v8 entfernt; (c) Restore gilt nur fuer den aktuell eingeloggten Google-Account.
**Versionen:** v7/v8 (Methodenwegfall), Verhalten alle.
**FIX:** Beim App-Start `queryPurchasesAsync` getrennt fuer `SUBS` + `INAPP`; Entitlement ergaenzend ueber Backend/Developer API per Account-Mapping; Nutzer auf korrekten Google-Account hinweisen.
**Quelle:** support.google.com/googleplay/android-developer/thread/275369787

## F. Produkte laden & Testing

## 38. Leere ProductDetails / "item not found" — vollstaendige Checkliste   ⭐ HAEUFIG
**Symptom:** `queryProductDetailsAsync` leer oder "item unavailable".
**Ursache (Checkliste):** App in keinem Track (internal/closed/open) veroeffentlicht · Produkt inaktiv · falsche ID · Propagations-Delay (Min–Std) · Produkt im Land des Testers nicht verfuegbar · Tester nicht eingeladen/Link nicht angenommen · lokaler Build hat anderes Package/Signing/versionCode als der hochgeladene · Offer/BasePlan beim Abo fehlt.
**Versionen:** alle; `UnfetchedProduct`-Diagnose ab v8 (Bug 7).
**FIX:** App mind. einmal in einen Track hochladen; Produkt aktiv; IDs exakt; Tester unter Setup > License Testing UND im Track; gleiches Package + Signing; bei Abos Offer/BasePlan konfigurieren; Propagation abwarten.
**Quelle:** developer.android.com/google/play/billing/integrate · nami.ml/blog/play-billing-response-code-item-unavailable

## 39. "This version of the application is not configured for billing through Google Play"
**Symptom:** Beim Kaufversuch (oft Debug/Sideload) erscheint dieser Fehler; kein Kauf moeglich.
**Ursache:** Release nicht veroeffentlicht; ODER Nutzer kein Lizenz-Tester; ODER lokaler versionCode != hochgeladener; ODER API-Access nicht verknuepft.
**Versionen:** alle.
**FIX:** Lizenz-Tester unter Settings > License Testing eintragen (umgehen den "muss hochgeladen sein"-Check, sehen Test-Zahlungsmittel, keine echten Gebuehren). App in einen Track laden, versionCode angleichen. (Frank-relevant: Memory `feedback_billing_release_only` — bei Billing-Tests NUR Release-AAB bauen, kein Debug-APK.)
**Quelle:** developer.android.com/google/play/billing/test · support.google.com/googleplay/android-developer/answer/6062777

## 40. Echte Belastung statt Testkarte bei falscher Konfiguration   ⭐ HAEUFIG
**Symptom:** Beim Test wird KEINE Testkarte angeboten — echtes Zahlungsmittel wuerde belastet.
**Ursache:** Das kaufende Konto ist nicht als Lizenz-Tester gelistet (nur Lizenz-Tester bekommen Test-Zahlungsmittel).
**Versionen:** alle.
**FIX:** Exakt das auf dem Testgeraet eingeloggte Konto als Lizenz-Tester hinzufuegen; Liste speichern; ggf. neu einloggen.
**Quelle:** support.google.com/googleplay/android-developer/thread/261166614

## G. Versions-Deadline

## 41. Billing-Library-Versions-Deadline → Update-Ablehnung   ⭐ HAEUFIG
**Symptom:** Update wird abgelehnt: "App must use Google Play Billing Library version X or later".
**Ursache:** 2-Jahres-Deprecation-Zyklus. Tabelle (Last date neue Apps/Updates · Extension-Deadline):
  - v6: 31.08.2025 · 01.11.2025 (abgelaufen)
  - **v7: 31.08.2026 · 01.11.2026** ← 7.1.1 ist bis dahin OK
  - v8: 31.08.2027 · 01.11.2027
  - v9: 31.08.2028 (PBL 9.0.0 erschien 2026-05-19) — bestaetigt Re-Recherche 2026-06-24
**Versionen:** 7.1.1 betroffen ab 31.08.2026.
**FIX:** Vor dem **31.08.2026** auf Billing 8 heben (oder Extension bis 01.11.2026 beantragen). Bereits veroeffentlichte v7-Binaries laufen weiter, aber JEDES Update (auch Security-Patch) erfordert dann v8. `com.google.android.play.billingclient.version` im Manifest setzen, um Warnungen zu vermeiden. v8-Migration: Bugs 7, 8, 24, 25 beachten.
**Quelle:** developer.android.com/google/play/billing/deprecation-faq

---

# Teil 2 — Firebase Setup: google-services.json, Plugin & Init (BOM 34.11.0)

## 42. "No matching client found for package name" bei applicationIdSuffix   ⭐ HAEUFIG
**Symptom:** Build/Sync-Fehler `No matching client found for package name 'com.bestjournal.app.debug'`, sobald ein Build-Type einen `applicationIdSuffix` (`.debug`) setzt.
**Ursache:** Das google-services-Plugin matcht `applicationId + Suffix` gegen `package_name` in der JSON; die Standard-JSON kennt nur das Basis-Package.
**Versionen:** alle Plugin-Versionen.
**FIX:** Zweite `google-services.json` in `app/src/debug/` mit `package_name = Basis + Suffix` (genau das macht BestJournalAndroid bereits: `google-services-debug.json` aus dem SK-Ordner → `src/debug/google-services.json`). ALTERNATIV: separaten Android-Client mit gesuffixtem Package in der Konsole anlegen + JSON neu laden. (Frank-relevant: Memory `feedback_performance_from_debug` nutzt `.debug`-Suffix — zwingend noetig.)
**Quelle:** developers.google.com/android/guides/google-services-plugin

## 43. "File google-services.json is missing from module root folder"
**Symptom:** Build-Fehler; Folgefehler `cannot find symbol R.string.gcm_defaultSenderId`.
**Ursache:** JSON auf Projekt-Ebene (Repo-Root) statt im **Modul** (`app/`), oder ganz fehlend.
**Versionen:** alle.
**FIX:** JSON nach `app/` (Modul-Root) bzw. `app/src/<buildType>/`. `applicationId` muss zum `package_name` passen. (BestJournalAndroid: der `syncSecretsFromSk`-Task kopiert die JSON beim `preBuild` aus `$HOME/SK/BestJournalAndroid/` — fehlt der SK-Ordner, schlaegt der Build kontrolliert fehl.)
**Quelle:** developers.google.com/android/guides/google-services-plugin

## 44. Geaenderte google-services.json wird nicht neu eingelesen (stale)   ⭐ HAEUFIG
**Symptom:** Nach Aenderung in der Firebase-Konsole + Neu-Download bleibt der alte Stand aktiv (z.B. neuer SHA wirkt nicht; Crashes/Analytics gehen ans alte Projekt bei gleichem package_name).
**Ursache:** Das Plugin liest die JSON ggf. zu frueh / der `process<Variant>GoogleServices`-Output ist gecacht. Wird die Datei NACH dem Plugin-Apply kopiert, wird sie ignoriert (play-services-plugins#74). Configuration-Cache kann zusaetzlich alte Werte halten (Bug 121).
**Versionen:** historisch/wiederkehrend (Plugin-Bug #74 belegt CLOSED).
**FIX:** `./gradlew clean` + Gradle-Cache leeren, dann Rebuild → erzwingt Neu-Lesen. Datei VOR dem Build platzieren. Bei aktivem configuration-cache zusaetzlich `--no-configuration-cache` einmalig oder `.gradle/configuration-cache` loeschen. (Frank-Memory `feedback_firebase_google_services_json_after_changes`.)
**Quelle:** github.com/google/play-services-plugins/issues/74 · firebase-android-sdk#2191

## 45. SHA-1/SHA-256: Play-App-Signing-Key fehlt → Auth/App-Check still kaputt in Prod   ⭐ HAEUFIG
**Symptom:** Google-Sign-In/Phone-Auth/App-Check/Dynamic-Links laufen im Debug, brechen NACH dem Play-Store-Release lautlos (`CONFIGURATION_NOT_FOUND`, silent reject).
**Ursache:** Im Store signiert Google mit dem **App-Signing-Key** (anderer Fingerprint als Upload-/Debug-Key); meist wird nur der Upload-/Debug-SHA in Firebase eingetragen.
**Versionen:** dienstuebergreifend.
**FIX:** ALLE relevanten SHAs in Firebase eintragen: Debug-Key, Upload-Key UND **App-Signing-Key** (Play Console → App integrity → Play app signing → SHA-1 + SHA-256). Danach `google-services.json` neu downloaden + einchecken (in BestJournalAndroid: in den SK-Ordner). Direkt relevant fuer App Check / Play Integrity (Bug 88).
**Quelle:** support.google.com/firebase/answer/9137403

## 46. KTX-Module ab BOM v34.0.0 entfernt (Juli 2025)   ⭐ HAEUFIG
**Symptom:** `firebase-*-ktx`-Artefakte nicht mehr aufloesbar / API-Verschiebungen nach BOM-Bump.
**Ursache:** Firebase hat die KTX-Module ab BOM v34.0.0 entfernt; die KTX-APIs sind in die Haupt-Module gewandert.
**Versionen:** BOM >= 34.0.0 → **betrifft 34.11.0 direkt**.
**FIX:** `firebase-xxx-ktx` → `firebase-xxx` (ohne `-ktx`); Imports `com.google.firebase.xxx.ktx.*` → `com.google.firebase.xxx.*`. (BestJournalAndroid nutzt in `libs.versions.toml` bereits durchgaengig die Nicht-KTX-Namen — gut. Bei kuenftigen Firebase-Deps darauf achten.)
**Quelle:** firebase.google.com/support/release-notes/android

## 47. "All gms/firebase libraries must use the exact same version" / BOM ausgehebelt
**Symptom:** Lint `[GradleCompatible] All gms/firebase libraries must use the exact same version specification`.
**Ursache:** Einzelne Firebase-Lib mit **expliziter Version** neben der BOM → BOM-Management ausgehebelt; oder transitive Mischung.
**Versionen:** wiederkehrend (firebase-android-sdk#5972 CLOSED).
**FIX:** BOM via `platform()` einbinden, ALLE Firebase-Deps OHNE Version listen (so macht es BestJournalAndroid). Keine einzelne Firebase-Lib hart pinnen. Bei transitivem Konflikt `./gradlew app:dependencies` pruefen.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/5972

## 48. "Default FirebaseApp is not initialized in this process"
**Symptom:** `IllegalStateException: Default FirebaseApp is not initialized in this process`.
**Ursache:** (a) fehlende/falsche google-services.json (FirebaseInitProvider kann nicht init); (b) Multidex — Provider nicht im main-dex; (c) R8 strippt den Provider; (d) separater Prozess ohne initialisierte Default-App (z.B. Crashlytics/Performance im Child-Process).
**Versionen:** alle (firebase-android-sdk#4693, #6039 CLOSED).
**FIX:** R8: `-keep class com.google.firebase.provider.FirebaseInitProvider { *; }`. Multi-Process: in jedem Prozess defensiv `FirebaseApp.initializeApp(context)`. Manuelle Init NUR in `Application.onCreate()`, nie in `Activity.onCreate()` (Race). minSdk 26 (BestJournalAndroid) → kein Legacy-Multidex, (b) entfaellt.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/4693 · /issues/6039

## 49. Release-Crash trotz funktionierendem Debug (R8 strippt Firebase)   ⭐ HAEUFIG
**Symptom:** `NoClassDefFoundError` / R8 `Missing class com.google.firebase.messaging.TopicOperation$TopicOperations`; Firebase-Komponente crasht NUR im Release.
**Ursache:** R8/Minify entfernt Firebase-Klassen ohne passende Keep-Regeln (das SDK liefert nicht fuer alles Consumer-Rules).
**Versionen:** wiederkehrend (#3740, #5746 CLOSED).
**FIX:** R8-`missing_rules.txt` (`build/outputs/mapping/<variant>/`) auswerten und Keep-Regeln in `proguard-rules.pro` ergaenzen. NICHT Minify dauerhaft deaktivieren (nur temporaer zum Eingrenzen). Release-Build IMMER testen. Siehe Sektion 9 + `best-practices/android-build/gradle.md §4`. (Frank-Memory `feedback_billing_release_only`.)
**Quelle:** github.com/firebase/firebase-android-sdk/issues/3740 · /issues/5746

## 50. google-services.json im Repo / API-Key-Exposition — was harmlos ist
**Symptom:** GitHub Secret-Scanning-Alert fuer den Google-API-Key in google-services.json.
**Ursache:** Der Firebase-`api_key` ist ein **App-Identifier**, KEIN Auth-Secret → muss an jeden Client. Secret-Scanning meldet das als False Positive.
**Versionen:** generell.
**FIX:** Der Key ist sicher (Schutz via Firebase Security Rules + App Check, nicht via Geheimhaltung). Trotzdem: NIEMALS Service-Account-Private-Keys committen; Keys fuer nicht-Firebase Google-Cloud-APIs (Maps etc.) separat + restringiert. **Frank-Regel `secrets-in-sk-folder.md`:** google-services.json liegt in `$HOME/SK/BestJournalAndroid/` — die `.gitignore`-Ausnahme `!app/src/debug/google-services.json` NIE wieder einbauen (war Root-Cause des frueheren Leaks).
**Quelle:** firebase.google.com/docs/projects/api-keys

## 51. Plugin-Apply-Reihenfolge / Plugin-Version
**Symptom:** `R.string.gcm_defaultSenderId` nicht gefunden; Plugin generiert keine values.xml.
**Ursache:** `com.google.gms.google-services` nicht/zu frueh/falsch im app-Modul angewandt.
**Versionen:** google-services-Plugin 4.4.x (BestJournalAndroid: 4.4.2).
**FIX:** Plugin im **App-Modul** anwenden (Android-Plugin zuerst) — via `plugins{}`-Block wie in BestJournalAndroid (`alias(libs.plugins.google.services)`). Plugin-Version aktuell halten (Doku nennt 4.4.x; 4.4.2 ist kompatibel, optionaler Bump auf 4.4.4).
**Quelle:** developers.google.com/android/guides/google-services-plugin

## 52. Multi-Flavor / mehrere Firebase-Projekte (dev/prod)
**Symptom:** Falsches Firebase-Projekt im Build, oder "No matching client" bei Flavors.
**Ursache:** Pro Flavor/BuildType braucht es jeweils eine eigene google-services.json mit passendem `package_name`.
**Versionen:** Plugin >= 2.2.0.
**FIX:** Variant-spezifische JSONs in `app/src/<flavor>/`, `app/src/<flavor><BuildType>/`. Plugin waehlt automatisch die passendste; jede JSON braucht einen Client mit dem effektiven applicationId.
**Quelle:** developers.google.com/android/guides/google-services-plugin

---

# Teil 3 — Firebase Crashlytics & Analytics

> **PROAKTIV:** Crashlytics ist in BOM 34.11.0 noch NICHT eingebunden. Diese Sektion ist Zukunftswissen
> fuer den spaeteren Einbau (Crashlytics-Gradle-Plugin + `firebase-crashlytics`). Analytics IST eingebunden.

## 53. Mapping-File nicht hochgeladen → obfuskierte Stacktraces   ⭐ HAEUFIG
**Symptom:** Stacktraces im Dashboard bleiben unleserlich/obfuskiert (R8 aktiv); `uploadCrashlyticsMappingFile*`-Tasks existieren nicht.
**Ursache:** Crashlytics-Gradle-Plugin nicht (korrekt) angewandt; bei bestimmten Plugin-Anordnungen werden die Upload-Tasks nicht erzeugt.
**Versionen:** historisch v2.x; in v3 weitgehend behoben (firebase-android-sdk#2046 CLOSED), Reihenfolge bleibt relevant.
**FIX:** Crashlytics-Plugin ans ENDE des `plugins{}`-Blocks (nach `com.android.application` + `com.google.gms.google-services`) ODER imperativ `apply(plugin = "com.google.firebase.crashlytics")`. Mit `gradlew tasks` pruefen, dass die Upload-Tasks existieren.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/2046

## 54. mappingFileUploadEnabled = false → bewusst obfuskiert
**Symptom:** Release-Traces unleserlich trotz korrektem Plugin.
**Ursache:** Flag steht auf `false` (oft fuer schnellere Builds gesetzt).
**Versionen:** alle.
**FIX:** Im Release-BuildType `firebaseCrashlytics { mappingFileUploadEnabled = true }` (Kotlin DSL: `configure<CrashlyticsExtension>`). Pro BuildType/Flavor unabhaengig.
**Quelle:** firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports

## 55. ProGuard/DexGuard: fehlende SourceFile/LineNumberTable
**Symptom:** Zeilennummern/Dateinamen fehlen in den Traces.
**Ursache:** R8 macht das automatisch; reines ProGuard/DexGuard nicht.
**Versionen:** ProGuard/DexGuard.
**FIX:** `-keepattributes SourceFile,LineNumberTable` + `-keep public class * extends java.lang.Exception`.
**Quelle:** firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports

## 56. uploadCrashlyticsMappingFileRelease schlaegt fehl nach Plugin 3.0.1
**Symptom:** Build-Fehler beim Upload-Task nach Plugin-Update auf 3.0.1.
**Ursache:** Regression im Crashlytics-Gradle-Plugin 3.0.1.
**Versionen:** **gefixt ab 3.0.2+ (nutze 3.0.7+)** — firebase-android-sdk#5962 CLOSED.
**FIX:** Crashlytics-Gradle-Plugin auf 3.0.7+ aktualisieren.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/5962

## 57. UploadSymbolFileTask schlaegt fehl ohne google-services-Plugin
**Symptom:** Symbol-/Mapping-Upload crasht, Firebase-App-ID nicht aufloesbar.
**Ursache:** Das Crashlytics-Plugin braucht `com.google.gms.google-services` zum Aufloesen der App-ID.
**Versionen:** firebase-android-sdk#6230 CLOSED.
**FIX:** google-services-Plugin anwenden + gueltige google-services.json (in BestJournalAndroid vorhanden).
**Quelle:** github.com/firebase/firebase-android-sdk/issues/6230

## 58. DexGuard: "Transport backend 'cct' is not registered"
**Symptom:** Crash beim Senden, Reports kommen nicht an.
**Ursache:** DexGuard entfernt cct-Transport-Metadaten.
**Versionen:** DexGuard.
**FIX:** `-keepresourcexmlelements manifest/application/service/meta-data@value=cct`.
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 59. NDK: native Stacks unsymbolisiert
**Symptom:** C/C++-Crashes als Hex-Adressen ohne Funktionsnamen. (Relevant fuer BestJournalAndroid wegen **sherpa-onnx** native `.so`!)
**Ursache:** `firebase-crashlytics-ndk` fehlt ODER `nativeSymbolUploadEnabled` nicht gesetzt ODER Symbol-Upload-Task nicht gelaufen.
**Versionen:** min Gradle 8.0, AGP 8.1.0, google-services 4.4.1.
**FIX:** `implementation("com.google.firebase:firebase-crashlytics-ndk")` + Release `configure<CrashlyticsExtension> { nativeSymbolUploadEnabled = true }` + nach Build `./gradlew app:assembleRelease app:uploadCrashlyticsSymbolFileRelease`.
**Quelle:** firebase.google.com/docs/crashlytics/ndk-reports

## 60. NDK: fehlende GNU Build-ID
**Symptom:** Symbol-Upload findet keine passenden Symbole / "missing BuildId".
**Ursache:** Linker erzeugt keine build-id; gestrippte Binaries.
**Versionen:** NDK-Builds.
**FIX:** Linker-Flag `-Wl,--build-id`; mit `readelf -n` pruefen; bei nicht-Standard-Pfad `unstrippedNativeLibsDir = file("PATH")`.
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 61. Custom-Build ohne Gradle → CLI-Symbol-Upload
**FIX (funktionserhaltend):** `firebase crashlytics:symbols:upload --app=FIREBASE_APP_ID PATH/TO/SYMBOLS`.
**Quelle:** firebase.google.com/docs/crashlytics/ndk-reports

## 62. Crash erscheint erst nach App-Neustart im Dashboard
**Symptom:** Test-Crash taucht nie auf, weil die App nach dem Crash nicht neu gestartet wurde.
**Ursache:** Crashlytics sendet den Report beim NAECHSTEN Start, nicht im Crash-Moment.
**Versionen:** per Design.
**FIX:** Nach `throw RuntimeException("Test Crash")` App manuell neu starten; ~5 Min warten; Debug-Logging aktivieren.
**Quelle:** firebase.google.com/docs/crashlytics/android/get-started

## 63. Datensammlung deaktiviert / unsent reports
**Symptom:** Keine Crashes trotz korrekter Einrichtung.
**Ursache:** Auto-Collection aus oder Opt-in aktiv ohne Consent (Bug 69).
**Versionen:** per Design.
**FIX:** `sendUnsentReports()` zum Senden gecachter Reports; Collection-Status pruefen.
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 64. Analytics-SDK fehlt → keine Breadcrumbs / instabil
**FIX:** `firebase-analytics` zusaetzlich einbinden (in BestJournalAndroid vorhanden).
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 65. App Check blockiert Crashlytics-Reports
**Symptom:** Crashes erreichen das Backend nicht.
**Ursache:** App-Check-Attestation lehnt legitimen Traffic ab / Token-Refresh kaputt (Bugs 88–91).
**Versionen:** per Design.
**FIX:** Attestation-Provider korrekt konfigurieren, Token-Refresh pruefen.
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 66. Dashboard "No crashes in last 90 days" nach SDK-Update
**Ursache:** SDK-Versions-Inkompatibilitaet.
**Versionen:** firebase-android-sdk#2265/#2198/#2163 (Status nicht alle hart geprueft).
**FIX:** BOM-aligned bleiben (alle Firebase-Libs ueber die BOM), Debug-Logging zur Sende-Verifikation.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/2265

## 67. Google Mobile Ads SDK kapert den ExceptionHandler
**Symptom:** Keine Crashlytics-Reports bei eingebundenem Ads-SDK.
**FIX:** `disableSDKCrashReporting()` im Mobile Ads SDK. (Aktuell kein Ads-SDK in BestJournalAndroid.)
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 68. ANRs werden nicht gemeldet
**Ursache:** ANR-Erfassung braucht `getHistoricalProcessExitReasons()` → nur Android 11+; SDK zu alt.
**Versionen:** SDK v18.3.5+ noetig; Android <11 erfasst keine ANRs.
**FIX:** Aktuelle BOM nutzen. (minSdk 26 → Geraete <Android 11 koennen vorkommen; kein Code-Fix moeglich.)
**Quelle:** firebase.google.com/docs/crashlytics/troubleshooting

## 69. Crashlytics-Collection vor Zustimmung abschalten (DSGVO)
**FIX (funktionserhaltend):** Manifest `<meta-data android:name="firebase_crashlytics_collection_enabled" android:value="false"/>`; nach Consent `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)` (persistiert, ueberschreibt Manifest).
**Quelle:** firebase.google.com/docs/reference/android/com/google/firebase/crashlytics/FirebaseCrashlytics

## 70. FirebaseInitProvider feuert trotz Manifest-Flag (Netzwerk-Calls vor Consent)
**Symptom:** Trotz `..._collection_enabled=false` gehen Calls an `firebaseinstallations.googleapis.com`.
**Ursache:** `FirebaseInitProvider` haengt im App-Lifecycle, laeuft vor `onCreate()`.
**Versionen:** firebase-android-sdk#5025 CLOSED.
**FIX:** Provider per Manifest entfernen (`tools:node="remove"` fuer `com.google.firebase.provider.FirebaseInitProvider`) und nach Consent manuell `FirebaseApp.initializeApp(context)`. Achtung: kollidiert mit Bug 48 — dann ueberall manuell initialisieren.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/5025

## 71. custom keys/logs / recordException zu spaet gesetzt
**Symptom:** Non-fatals ohne Kontext; Keys fehlen im Crash.
**FIX:** Keys/Logs frueh und kontinuierlich setzen; `recordException(e)` fuer Non-fatals.
**Quelle:** firebase.google.com/docs/crashlytics/android/get-started

## 72. Analytics DebugView leer / Geraet fehlt im Selektor
**Symptom:** Keine Echtzeit-Events; Device nicht im Dropdown.
**Ursache:** Debug-Modus nicht aktiv, App nicht neu gestartet, falsches Projekt, oder noch kein Event geloggt.
**Versionen:** per Design.
**FIX:** `adb shell setprop debug.firebase.analytics.app com.bestjournal.app` (aus: `...none.`), App neu starten, in der Konsole Analytics > DebugView. DebugView-Events fliessen NICHT in Produktion/BigQuery.
**Quelle:** firebase.google.com/docs/analytics/debugview

## 73. Analytics-Events verzoegert (bis ~24h) im Normalbetrieb
**Symptom:** Custom-Events fehlen kurz nach Release.
**Ursache:** Events werden ~1h gebatcht (Akku/Daten), Verarbeitung bis 24h. Kein Bug.
**Versionen:** per Design.
**FIX:** Fuer schnelle Pruefung DebugView nutzen; sonst auf Standardverarbeitung warten.
**Quelle:** firebase.google.com/docs/analytics/debugview

## 74. Reservierte/ungueltige Event-/Parameter-Namen werden still ignoriert
**Symptom:** Events/Params fehlen im Dashboard ohne Fehler.
**Ursache:** Name >40 Zeichen, beginnt mit Zahl/Leerzeichen, >25 Params/Event, Prefix `firebase_`/`google_`/`ga_`, String-Wert >100 Zeichen, >500 distinkte Params.
**Versionen:** per Design.
**FIX:** snake_case, <=40 Zeichen, keine reservierten Prefixe, max 25 Params/Event, Werte String/Long/Double/Boolean.
**Quelle:** firebase.google.com/docs/analytics (Event-/Param-Limits)

## 75. Analytics-Consent / Consent Mode v2 — first_open/session_start verloren
**Symptom:** Bei Consent-Mode gehen `first_open`/`session_start` verloren, wenn Consent erst spaet gewaehrt wird.
**Ursache:** Consent-Defaults im Manifest = false; Events vor Consent verworfen.
**Versionen:** firebase-android-sdk#5697 CLOSED.
**FIX:** Manifest `firebase_analytics_collection_enabled=false`; nach Consent `setAnalyticsCollectionEnabled(true)` (persistiert). Consent Mode v2: Defaults `google_analytics_default_allow_*` im Manifest, dann `Firebase.analytics.setConsent { ... }` moeglichst frueh.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/5697 · firebase.google.com/docs/analytics/android/configure-data-collection

---

# Teil 4 — Firebase Cloud Messaging (FCM)

> **PROAKTIV:** FCM/Messaging ist in BOM 34.11.0 noch NICHT eingebunden. Diese Sektion ist Zukunftswissen
> fuer den spaeteren Einbau (`firebase-messaging` + `FirebaseMessagingService`).

## 76. onMessageReceived wird im Background bei notification-messages NICHT aufgerufen   ⭐ HAEUFIG
**Symptom:** App im Background/killed empfaengt Push, aber `onMessageReceived()` feuert nie; Notification erscheint nur im System-Tray.
**Ursache:** Bei reinen `notification`-Payloads zeigt das SDK die Notification selbst und ruft den Callback NICHT auf. Kombiniertes `notification`+`data` landet nur in den Intent-Extras der Launcher-Activity.
**Versionen:** alle (Design-Verhalten).
**FIX:** Fuer eigene Logik in allen Zustaenden **data-only Messages** senden (kein `notification`-Key) und die Notification selbst in `onMessageReceived()` bauen. Foreground bekommt beide Typen.
**Quelle:** firebase.google.com/docs/cloud-messaging/android/receive-messages

## 77. POST_NOTIFICATIONS Runtime-Permission fehlt (Android 13 / API 33)   ⭐ HAEUFIG
**Symptom:** Auf Android 13+ werden Notifications still verworfen — keine Anzeige, kein Fehler.
**Ursache:** Ab API 33 ist `POST_NOTIFICATIONS` Runtime-Permission; nach Install per Default AUS.
**Versionen:** Android 13+ (gilt bei targetSdk 36).
**FIX:** `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` + zur Laufzeit anfragen, bevor Notifications gesendet werden.
**Quelle:** developer.android.com/develop/ui/views/notifications/notification-permission

## 78. Notification Channel fehlt / Erstkreation im Background blockiert (API 26)
**Symptom:** Keine Anzeige obwohl Push ankommt; besonders die erste Notification nach Install.
**Ursache:** Ab API 26 braucht jede Notification einen Channel; wird der erste Channel im Background angelegt, erlaubt Android die Anzeige nicht.
**Versionen:** Android 8+ (API 26+, also alle ab minSdk 26).
**FIX:** Channel proaktiv beim App-Start (`Application.onCreate`) anlegen, nicht erst beim Empfang.
**Quelle:** firebase.google.com/docs/cloud-messaging/android/get-started

## 79. Default-Notification-Channel/-Icon Meta-Data im Manifest fehlt
**Symptom:** Notifications im falschen Channel oder weisses Quadrat-Icon.
**FIX:** `<meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" .../>` + `default_notification_icon` setzen.
**Quelle:** firebase.google.com/docs/cloud-messaging/android/get-started

## 80. Notifications verspaetet/nicht zugestellt: Doze + Hersteller-Battery-Kills   ⭐ HAEUFIG
**Symptom:** Notifications kommen stark verzoegert oder gar nicht, v.a. Xiaomi/Huawei/Samsung/OnePlus.
**Ursache:** Doze/App-Standby batcht normal-priority Messages; Hersteller-Mechanismen killen den Prozess. (Frank-relevant: Samsung S23 Ultra / Fold 6.)
**Versionen:** alle; hersteller-abhaengig.
**FIX:** Fuer user-sichtbare Pushes `priority: high` setzen (umgeht Doze-Batching). User per `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` um Ausnahme bitten. High-priority NICHT fuer reine Background-Syncs missbrauchen (Quota). Siehe `best-practices/android/android-platform.md` (FGS-Start-Trigger: high-priority FCM vorher `remoteMessage.priority == PRIORITY_HIGH` pruefen — System kann downgraden).
**Quelle:** firebase.blog/posts/2025/04/fcm-on-android

## 81. FCM-Token-Rotation / onNewToken / getToken SERVICE_NOT_AVAILABLE
**Symptom:** Pushes an veraltete Tokens schlagen fehl; `getToken()` wirft `SERVICE_NOT_AVAILABLE`.
**Ursache:** Token rotiert (Restore, Reinstall, Datenloeschung); Server haelt alten Token. `getToken` braucht Verbindung.
**Versionen:** alle.
**FIX:** `onNewToken()` ueberschreiben und Token sofort serverseitig aktualisieren; `getToken` mit Backoff-Retry; serverseitig invalide Tokens loggen/entfernen.
**Quelle:** firebase.google.com/docs/cloud-messaging/android/receive-messages

## 82. FirebaseMessagingService-Deklaration im Manifest fehlt
**Symptom:** Kein Empfang trotz korrektem Setup.
**FIX:** `<service>` mit Intent-Filter `com.google.firebase.MESSAGING_EVENT` im Manifest deklarieren.
**Quelle:** firebase.google.com/docs/cloud-messaging/android/receive-messages

---

# Teil 5 — Firebase Remote Config (eingebunden)

## 83. fetchAndActivate aktualisiert Werte nicht: 12h-Throttle   ⭐ HAEUFIG
**Symptom:** Geaenderte Werte erscheinen nicht; wiederholtes Fetch bringt nichts.
**Ursache:** `minimumFetchInterval` Default = 12h; mehr als ein Backend-Fetch pro Fenster wird ignoriert.
**Versionen:** per Design, alle.
**FIX:** Im **Debug** `minimumFetchInterval = 0` (oder klein) via `FirebaseRemoteConfigSettings`. In Produktion bewusst niedrig genug halten oder Realtime nutzen. Werte NIE als sofort-aktuell annehmen.
**Quelle:** firebase.google.com/docs/remote-config/use-config-android

## 84. FirebaseRemoteConfigFetchThrottledException trotz langem Intervall
**Symptom:** Fetch wirft `FirebaseRemoteConfigFetchThrottledException`.
**Ursache:** Zu viele Fetches in kurzer Zeit (clientseitiges Throttling), unabhaengig vom gesetzten Intervall.
**Versionen:** firebase-android-sdk#5908 CLOSED.
**FIX:** Fetch-Frequenz reduzieren, Exception abfangen und mit gecachten Werten weiterarbeiten (nicht crashen).
**Quelle:** github.com/firebase/firebase-android-sdk/issues/5908

## 85. activate() vs. fetch() — Werte erst beim naechsten Start aktiv
**Symptom:** Frisch gefetchte Werte greifen erst nach App-Neustart.
**Ursache:** `fetch()` laedt nur in den Cache; ohne `activate()` werden die Werte nicht aktiv.
**Versionen:** per Design.
**FIX:** `fetchAndActivate()` nutzen ODER nach `fetch()` explizit `activate()` (auch im Realtime-`onUpdate`).
**Quelle:** firebase.google.com/docs/remote-config/use-config-android

## 86. setDefaultsAsync fehlt — App-Logik bricht ohne Netz
**Symptom:** Vor dem ersten erfolgreichen Fetch (kein Netz/Erststart) sind alle Werte leer/0.
**Ursache:** Keine lokalen Defaults gesetzt.
**Versionen:** per Design.
**FIX:** `setDefaultsAsync(R.xml.remote_config_defaults)` setzen.
**Quelle:** firebase.google.com/docs/remote-config/get-started?platform=android

## 87. Realtime addOnConfigUpdateListener feuert nicht
**Symptom:** `onUpdate` kommt nicht; published Werte landen nicht automatisch im Client.
**Ursache:** (a) "Firebase Remote Config Realtime API" in der Cloud Console nicht aktiviert; (b) nach Netz-Aus/An stoppt der Event-Stream (Resume nach ~3-5 min).
**Versionen:** Realtime ab SDK v21.3.0 / BOM v31.3.0; firebase-android-sdk#4864/#5040 CLOSED.
**FIX:** Realtime-API in der Cloud Console aktivieren; im `onUpdate` immer `activate()` aufrufen; transienten Stream-Stopp tolerieren (kein Crash).
**Quelle:** firebase.google.com/docs/remote-config/android/real-time

---

# Teil 6 — Firebase App Check (eingebunden, Play Integrity + Debug)

## 88. "App attestation failed" / 403 auf zertifizierten Geraeten   ⭐ KRITISCH — OFFEN
**Symptom:** Anfragen werden mit `403 ... App attestation failed` blockiert — teils auch auf zertifizierten, nicht-gerooteten Geraeten echter Nutzer.
**Ursache:** Play Integrity nicht korrekt konfiguriert (SHA-256 fehlt/falscher Signing-Key in der Firebase-Konsole, siehe Bug 45), Play-Integrity-API nicht aktiviert, ODER transiente Play-Integrity-Fehlschlaege.
**Versionen:** **firebase-android-sdk#7110 ist OFFEN (verifiziert 2026-06-02)** — kein SDK-Fix, Workaround bleibt aktiv.
**FIX (funktionserhaltend):** SHA-256 des **Play-App-Signing-Keys** (nicht nur Upload-Key) in der Firebase-Konsole hinterlegen, Play Integrity API aktivieren; transiente Fehler abfangen statt hart zu blockieren (Retry/Fallback, kein Crash).
**Quelle:** github.com/firebase/firebase-android-sdk/issues/7110 · firebase.google.com/docs/app-check/android/play-integrity-provider

## 88a. App-Check-`getToken()`-Fehlerkatalog (Play Integrity, transient & haeufig)  (Re-Recherche 2026-06-24)
**Symptom:** `getToken()` schlaegt bei einem messbaren Anteil echter Nutzer fehl (im gemeldeten Fall ~8 %
von 100.000 im Mai 2026) mit wechselnden Codes — kein dauerhafter Defekt, sondern transiente Realitaet.
**Ursache:** Play-Integrity-/App-Check-Infrastruktur ist netz-/Play-Store-/Server-abhaengig. Typische Codes:
`IntegrityServiceException -1` (API not available), `-2` (Play Store fehlt/inoffiziell), `-3` (Network error),
`-8` (Throttling, zu viele Requests), `-9` (Binding to Play Store service failed), `-12` (internal Google
server error); dazu `RemoteException "Binder has died"`, `FirebaseException 403 "App attestation failed"`
(siehe Bug 88), `UnknownHostException firebaseappcheck.googleapis.com`.
**Versionen:** firebase #8246 (**CLOSED COMPLETED 2026-06-16** — als *Frage* geschlossen, KEIN bestaetigter
SDK-Bug; die Codes sind erwartbares transientes Verhalten).
**FIX (funktionserhaltend):** Diese Fehler NIE hart blockieren/crashen — Exponential Backoff + Retry,
graceful Degradation (Feature kurz sperren statt App-Abbruch), `-8`-Throttling respektieren (Token cachen,
TTL ~1h, Refresh bei halber TTL). Bei `-2`/`-9` ggf. Hinweis "Play Store aktualisieren".
**Quelle:** github.com/firebase/firebase-android-sdk/issues/8246 · developer.android.com/google/play/integrity/error-codes

## 89. Debug-Build blockiert — Debug-Provider-Token nicht in der Konsole   ⭐ HAEUFIG
**Symptom:** Emulator/CI/Debug-Build wird von App Check abgelehnt ("does not pass basic integrity").
**Ursache:** `DebugAppCheckProviderFactory` erzeugt ein Debug-Token, das in der Firebase-Konsole unter "Manage debug tokens" hinterlegt sein muss; jedes Geraet erzeugt ein eigenes.
**Versionen:** alle.
**FIX:** Im Debug-Build `DebugAppCheckProviderFactory` registrieren (BestJournalAndroid hat `firebase-appcheck-debug` als `debugImplementation`), das im Logcat ausgegebene Token in der Konsole eintragen. NIE den Debug-Provider in Release. Auf CI als Env-Var `FIREBASE_APP_CHECK_DEBUG_TOKEN` (NICHT committen — SK-Regel).
**Quelle:** firebase.google.com/docs/app-check/android/debug-provider

## 90. Enforcement aktiviert BEVOR alle Clients aktualisiert sind → Functions/AI/Firestore brechen   ⭐ HAEUFIG
**Symptom:** Nach Aktivieren von Enforcement schlagen Aufrufe alter Clients (und Debug-Builds ohne Debug-Provider) komplett fehl.
**Ursache:** Enforcement verlangt gueltige App-Check-Token; alte/uninstrumentierte Clients senden keine.
**Versionen:** per Design.
**FIX:** ERST Client-Rollout abwarten (Metriken in der Konsole pruefen), DANN Enforcement aktivieren. Debug-Builds mit Debug-Provider versorgen. Besonders relevant, da BestJournalAndroid App Check + Functions + firebase-ai nutzt.
**Quelle:** firebase.google.com/docs/app-check/enable-enforcement

## 91. App-Check-Token erst nach App-Restart aktiv / Init-Reihenfolge
**Symptom:** Viele "invalid requests"; Dienste nehmen das frische Token erst nach Neustart.
**Ursache:** `installAppCheckProviderFactory()` zu spaet, oder andere Dienste vor App Check initialisiert.
**Versionen:** firebase-android-sdk#5190 CLOSED.
**FIX:** `FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...)` direkt nach `Firebase.initialize()` und VOR der ersten Nutzung anderer Firebase-Dienste aufrufen.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/5190

---

# Teil 7 — Firebase Cloud Functions (eingebunden, callable)

## 92. NOT_FOUND durch Region-Mismatch (getInstance(region) vergessen)   ⭐ HAEUFIG
**Symptom:** Callable-Funktion wird nie getriggert / liefert `NOT_FOUND`, obwohl der Client-Code laeuft.
**Ursache:** Funktion in nicht-default Region deployt (z.B. `europe-west1`), Client nutzt aber Default `us-central1`.
**Versionen:** alle.
**FIX:** Client an die Deploy-Region binden: `FirebaseFunctions.getInstance("europe-west1")`.
**Quelle:** firebase.google.com/docs/functions/locations

## 93. DEADLINE_EXCEEDED durch Cold Start
**Symptom:** "DEADLINE_EXCEEDED, could not connect to the server", v.a. nach Inaktivitaet.
**Ursache:** Cold Start der Funktion ueberschreitet das Client-Timeout.
**Versionen:** wiederkehrend (react-native-firebase#6263/#8043, gleiche Native-Schicht).
**FIX:** Client-Timeout erhoehen (`getHttpsCallable(...).setTimeout(...)`); serverseitig min-instances; clientseitig Retry mit Backoff.
**Quelle:** firebase.google.com/docs/functions (callable) · github.com/invertase/react-native-firebase/issues/8043

## 94. UNAUTHENTICATED trotz gueltigem Auth-User
**Symptom:** `UNAUTHENTICATED`, obwohl `auth.currentUser` davor gueltig ist.
**Ursache:** Auth-Token wird beim Aufruf nicht/zu spaet mitgegeben (Token-Refresh-Timing).
**Versionen:** wiederkehrend (react-native-firebase#8492).
**FIX:** Vor dem Call Token-Frische sichern (`getIdToken(true)`); Fehler ueber `FirebaseFunctionsException.Code` mappen und gezielt retrien.
**Quelle:** firebase.google.com/docs/reference/android/com/google/firebase/functions/FirebaseFunctionsException.Code

## 95. App-Check-Token blockiert Functions-Aufruf ("internal"/unauthorized)
**Symptom:** Callable bricht mit `INTERNAL`/unauthorized, sobald App-Check-Enforcement fuer Functions aktiv ist.
**Ursache:** Client sendet kein gueltiges App-Check-Token (Bugs 89/90).
**Versionen:** per Design.
**FIX:** App Check korrekt installieren (Bug 91), Debug-Token eintragen, Enforcement erst nach Client-Rollout.
**Quelle:** firebase.google.com/docs/app-check/cloud-functions

## 96. FirebaseFunctionsException nicht differenziert behandelt
**Symptom:** Generisches Crashen statt differenzierter Behandlung; `INTERNAL` verschleiert die echte Ursache.
**Ursache:** Server-Fehler kommen als opakes `INTERNAL`, wenn die Function keinen `HttpsError` wirft.
**Versionen:** per Design.
**FIX:** Serverseitig immer `HttpsError` mit definiertem Code werfen; clientseitig `FirebaseFunctionsException.getCode()` auswerten (NOT_FOUND, DEADLINE_EXCEEDED, UNAUTHENTICATED, PERMISSION_DENIED, INTERNAL …).
**Quelle:** firebase.google.com/docs/reference/android/com/google/firebase/functions/FirebaseFunctionsException.Code

---

# Teil 8 — Firebase AI Logic (firebase-ai / Gemini, eingebunden)

## 97. Umbenennung "Vertex AI in Firebase" → "Firebase AI Logic"   ⭐ HAEUFIG
**Symptom:** Alte Imports/Init (`FirebaseVertexAI.getInstance()…`) kompilieren nicht mehr / deprecated.
**Ursache:** Paket umbenannt (`firebase-vertexai` → `firebase-ai`), neue Init-API.
**Versionen:** alle firebase-ai (BOM 34.x).
**FIX:** Neu initialisieren: `Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel("gemini-2.5-flash")`. Fuer die kostenlose Gemini Developer API stattdessen `GenerativeBackend.googleAI()` (siehe Bug 106).
**Quelle:** firebase.google.com/docs/ai-logic/migrate-to-latest-sdk

## 98. Timeout aus RequestOptions entfernt
**Symptom:** `RequestOptions(timeout=…)` existiert nicht mehr → Code bricht beim Migrieren.
**Ursache:** Timeout aus RequestOptions entfernt.
**Versionen:** firebase-ai nach Migration.
**FIX:** Timeout-Parameter entfernen; eigenes Timeout via Coroutine `withTimeout {}` um den Aufruf legen.
**Quelle:** firebase.google.com/docs/ai-logic/migrate-to-latest-sdk

## 99. 400 "API key not valid"
**Ursache:** API-Key in der Firebase-Config existiert nicht / nicht fuer App/Projekt konfiguriert.
**FIX:** Frische google-services.json aus der Konsole ziehen und ersetzen; Key in Cloud Console > Credentials pruefen.
**Quelle:** firebase.google.com/docs/ai-logic/error-codes

## 100. 403 "Requests to firebasevertexai.googleapis.com … are blocked"
**Ursache:** API-Key hat Einschraenkungen, die den AI-Logic-Endpoint sperren.
**FIX:** In Cloud Console beim API-Key `firebasevertexai.googleapis.com` zur Allow-List hinzufuegen.
**Quelle:** firebase.google.com/docs/ai-logic/error-codes

## 101. 403 "PERMISSION_DENIED: The caller does not have permission"
**Ursache:** API-Key gehoert zu einem ANDEREN Firebase-Projekt (haeufig nach Projekt-/JSON-Tausch).
**FIX:** Frische Config-Datei des korrekten Projekts einspielen.
**Quelle:** firebase.google.com/docs/ai-logic/error-codes

## 102. 404 "Firebase AI Logic genai config not found"
**Ursache:** Projekt hat kein gueltiges Gemini-API-Setup.
**FIX:** Firebase Console → AI Services → AI Logic → Setup-Workflow durchlaufen.
**Quelle:** firebase.google.com/docs/ai-logic/error-codes

## 103. 404 "Publisher Model … was not found" (model not found)
**Symptom:** `Publisher Model … /models/[name] was not found`.
**Ursache:** (a) ungueltiger Modellname; (b) Preview-Modell braucht Location `global` statt `us-central1`; (c) Modell unterstuetzt die Location nicht.
**FIX:** Modellnamen gegen die supported-models-Liste pruefen; bei Preview Location `global`. Modell-Namen NIE hardcoden (Bug 105).
**Quelle:** firebase.google.com/docs/ai-logic/error-codes

## 104. 429 Quota / Rate-Limit / "Resource exhausted"
**Ursache:** Quota ueberschritten oder Modell ueberlastet.
**FIX:** Quota pruefen/erhoehen; Retry mit exponential backoff; Vertex = dynamic shared quota, Developer-Backend = explizite Limits.
**Quelle:** firebase.google.com/docs/ai-logic/error-codes

## 105. Modell-Versionen werden abgeschaltet (Shutdown-Daten)   ⭐ HAEUFIG
**Symptom:** Ploetzliche 404, App-Feature bricht in Produktion.
**Ursache:** Modell-Shutdown. Bekannte Daten: Gemini 1.5 Mai–Sep 2025; Gemini 2.0 Flash/Lite 31.03.2026; alle Imagen-Modelle 24.06.2026.
**Versionen:** laufend.
**FIX:** Modell-Namen NIE hardcoden — ueber **Firebase Remote Config** steuern, damit ein Wechsel ohne App-Update moeglich ist (BestJournalAndroid hat Remote Config bereits eingebunden — ideal dafuer).
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

## 106. Blaze-Plan zwingend (Vertex-Backend)   ⭐ wichtig
**Symptom:** Requests scheitern / Service nicht verfuegbar.
**Ursache:** AI Logic ueber das Vertex-Backend braucht Pay-as-you-go (Blaze). Spark (kostenlos) geht nur ueber das Gemini-Developer-Backend (`googleAI()`).
**Versionen:** per Design.
**FIX:** Blaze aktivieren ODER bewusst `GenerativeBackend.googleAI()` nutzen. (Entscheidung pro App treffen — fuer eine kostenfreie/private Variante ist `googleAI()` oft die bessere Wahl.)
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

## 107. Falsche/fehlende API-Aktivierung
**Symptom:** 404 / "API not enabled".
**Ursache:** Developer-Backend braucht `generativelanguage.googleapis.com` + `firebasevertexai.googleapis.com`; Vertex-Backend `aiplatform.googleapis.com` + `firebasevertexai.googleapis.com`.
**FIX:** Ueber den AI-Logic-Setup-Workflow auto-enablen oder manuell in der Cloud Console.
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

## 108. App Check als Voraussetzung — Requests in Produktion blockiert   ⭐ HAEUFIG
**Symptom:** Funktioniert in Dev, scheitert in Produktion (Attestation-Fehler).
**Ursache:** AI Logic ist ein Proxy-Gateway, das App Check erwartet; ohne integriertes App Check werden Calls abgelehnt, sobald Enforcement aktiv ist.
**Versionen:** per Design.
**FIX:** App Check mit Play-Integrity-Provider integrieren (in BestJournalAndroid vorhanden); App Check VOR den AI-Calls initialisieren (Bug 91). Enforcement-Rollout: Bug 90.
**Quelle:** firebase.google.com/docs/ai-logic/app-check

## 108a. Live API: `LiveGenerativeModel.connect()` haengt App-Check-Header NICHT an → 403  (Re-Recherche 2026-06-24)
**Symptom:** Gemini **Live API**-WebSocket-Requests werden bei aktivem App Check abgelehnt (Attestation/403),
obwohl normale `generateContent`-Calls durchgehen.
**Ursache:** Der WebSocket-`connect()` der Live API haengte den `X-Firebase-AppCheck`-Header NICHT an (SDK-Bug
in firebase-ai). Betraf u.a. BoM 34.8.0.
**Versionen:** firebase #8060 (**CLOSED COMPLETED 2026-04-27**) — gefixt in **Firebase AI Logic ab BoM 34.13.0**.
**FIX (funktionserhaltend):** Firebase-BOM auf **>= 34.13.0** heben (euer Stand war 34.11.0). Nur relevant, wenn
die Live API genutzt wird; normale Calls sind nicht betroffen.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/8060

## 109. Developer-API vs. Vertex-Backend — inkonsistentes Verhalten
**Symptom:** Feature laeuft mit einem Backend, scheitert mit dem anderen.
**Ursache:** Unterschiede: Developer-API = Spark moeglich, explizite Limits, KEINE Cloud-Storage-URLs, keine Location. Vertex = Blaze, dynamic shared quota, Cloud Storage, Location waehlbar.
**FIX:** Backend im Code bewusst setzen (`googleAI()` vs. `vertexAI()`) und gegen den Feature-Bedarf abgleichen.
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

## 110. Region/Location nur bei Vertex-Backend
**Symptom:** Location-Fehler / Modell pro Region nicht verfuegbar.
**Ursache:** Developer-API unterstuetzt KEINE Location-Angabe; Vertex schon.
**FIX:** Wenn Region wichtig → Vertex-Backend + passende Location.
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

## 111. Multimodale Inputs ohne MIME-Type
**Symptom:** Bild/Video/Audio/PDF-Analyse scheitert (teils still).
**Ursache:** MIME-Type bei jedem multimodalen Request Pflicht.
**FIX:** MIME-Type immer mitgeben (Ausnahme: Android-SDK konvertiert Inline-Bilder zu JPEG).
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

## 112. responseMimeType / Schema-Falle bei Structured Output
**Symptom:** JSON-Parsing schlaegt fehl / unerwartetes Format.
**Ursache:** `responseMimeType`/Schema nicht zum Modell passend.
**FIX:** `responseMimeType = "application/json"` setzen, Schema gegen die Gemini-Erwartung validieren, Modell-Support pruefen.
**Quelle:** firebase.google.com/docs/ai-logic/faq-and-troubleshooting

---

# Teil 9 — R8/ProGuard Keep-Regeln (Release) fuer Firebase + Billing

> BestJournalAndroid hat `isMinifyEnabled=true` + `isShrinkResources=true` im Release. Die folgenden Bugs
> manifestieren sich NUR im Release-Build (Debug laeuft). Querverweis: `best-practices/android-build/gradle.md §4`.

## 113. R8 fullMode ist Default ab AGP 8.0   ⭐ HAEUFIG
**Symptom:** Release crasht, Debug laeuft (NoClassDefFound, fehlende Reflection-Klassen) — obwohl frueher (AGP 7) ok.
**Ursache:** `android.enableR8.fullMode=true` ist Default seit AGP 8.0 (deutlich aggressiver). BestJournalAndroid: AGP 8.7.3.
**Versionen:** AGP 8.0+.
**FIX:** Keep-Regeln ergaenzen (114–117). Notfall-Diagnose: temporaer `android.enableR8.fullMode=false` in `gradle.properties`, um fullMode als Ursache zu bestaetigen — NICHT als Dauerloesung.
**Quelle:** developer.android.com/topic/performance/app-optimization/enable-app-optimization

## 114. Firebase braucht Keep-Regeln (Release-Crash)
**Symptom:** Release-Crash in Firebase-Komponenten (NoClassDefFound).
**Ursache:** Reflection → R8 entfernt scheinbar ungenutzte Klassen.
**FIX:** Gezielt die per Reflection genutzten Entrypoints keepen (eng halten, nicht ganze Pakete — siehe best-practices). Bei firebase-ai zusaetzlich generierte Response-Klassen (`GenerateContentResponse`) keepen. `missing_rules.txt` auswerten.
**Quelle:** developer.android.com/topic/performance/app-optimization/full-mode · firebase-android-sdk#2124

## 115. Play Billing Keep-Regeln (Release-Crash beim Kauf)   ⭐ HAEUFIG
**Symptom:** Release-Crash im Kauf-Flow / bei ProductDetails (Billing 7.1.1).
**Ursache:** Billing-Client-Klassen werden gestrippt/obfuskiert.
**Versionen:** alle Billing-Versionen mit R8.
**FIX:** `-keep class com.android.billingclient.api.** { *; }` + `-dontwarn com.android.billingclient.api.**`. (Billing bringt eigene consumer-proguard-Rules mit — bei Crash trotzdem explizit keepen.)
**Quelle:** developer.android.com/google/play/billing/release-notes

## 116. kotlinx-serialization unter R8 fullMode (firebase-ai nutzt es intern)
**Symptom:** Release-Crash / leere Felder bei `@Serializable`-Klassen; ab kotlinx-serialization-json 1.9.0 zusaetzlich R8-Warnings.
**Ursache:** fullMode + Reflection bei `@Serializable`-Klassen/Enums.
**Versionen:** kotlinx.serialization#3033 (R8 warning ab 1.9.0, CLOSED 2025-12), #2501 (fullMode + enums, CLOSED) — die noetigen Keep-Regeln sind inzwischen in der Lib enthalten, bei eigenen `@Serializable`-Modellen aber weiter explizit pruefen.
**FIX:** Offizielle kotlinx-serialization Keep-Regeln verwenden; eigene `@Serializable`-Modelle keepen; bei Enums die spezielle Regel.
**Quelle:** github.com/Kotlin/kotlinx.serialization/issues/3033 · /issues/2501

## 117. Firebase Auth Crash unter R8 fullMode
**Symptom:** Crash bei z.B. Google-Sign-In nur mit fullMode.
**Ursache:** fullMode-Obfuskation.
**Versionen:** firebase-android-sdk#2124 CLOSED — Consumer-Rules ergaenzt; bei Bedarf weiter eigene Keeps.
**FIX:** Keep-Regeln fuer Auth/GoogleSignIn; fullMode-Diagnose wie Bug 113.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/2124

## 118. isShrinkResources entfernt benoetigte Ressourcen
**Symptom:** Release fehlt eine Ressource (drawable/string), die nur per Name/Reflection referenziert wird.
**Ursache:** Resource-Shrinking erkennt namebasierte Zugriffe nicht.
**FIX:** `res/raw/keep.xml` mit `tools:keep="@.../name"` anlegen (statt Shrinking abzuschalten — funktionserhaltend).
**Quelle:** developer.android.com/topic/performance/app-optimization/enable-app-optimization

## 119. proguard-android.txt-Wegfall (AGP 9.0 voraus)
**Symptom (zukuenftig):** `getDefaultProguardFile("proguard-android.txt")` faellt in AGP 9.0 weg.
**Ursache:** R8 ersetzt die mitgelieferte Default-Datei.
**Versionen:** ab AGP 9.0. **BestJournalAndroid nutzt bereits `proguard-android-optimize.txt`** (die richtige Variante) — also nicht direkt betroffen, aber beim AGP-9-Upgrade pruefen.
**FIX:** Beim AGP-9-Upgrade auf die neuen R8-Defaults migrieren. (Memory `project_agp9_kotlin23_upgrade`.)
**Quelle:** developer.android.com/build/shrink-code

---

# Teil 10 — Plattform / Build-Cache (Windows + macOS)

## 120. google-services.json getauscht → Crashes/Analytics gehen ans alte Projekt
**Symptom:** Nach Wechsel der JSON (anderes Projekt, GLEICHER package_name) melden Crashlytics/Analytics weiter ans alte Projekt.
**Ursache:** Caching der generierten Resource-XMLs; aendert sich nur der Projektinhalt aber nicht der package_name, greift die Invalidierung nicht.
**Versionen:** firebase-android-sdk#2191 (Status nicht hart geprueft).
**FIX:** Nach JSON-Tausch `./gradlew clean` + Build-Cache/configuration-cache leeren (Bug 121); Gradle-Daemon ggf. stoppen.
**Quelle:** github.com/firebase/firebase-android-sdk/issues/2191

## 121. configuration-cache laedt alte JSON / bricht nach Cache-Clean
**Symptom:** Build nutzt veraltete google-services-Daten; oder Build bricht, nachdem nur der build-cache geleert wurde, der configuration-cache aber blieb.
**Ursache:** configuration-cache speichert berechnete Inputs; ein `build.gradle.kts`, das JSON/Werte zur Konfigurationszeit liest, wird nicht neu evaluiert.
**Versionen:** gradle#20693 CLOSED.
**FIX:** configuration-cache mit-invalidieren (`--no-configuration-cache` einmalig oder `.gradle/configuration-cache` loeschen) zusammen mit `clean`. (Anker: EntropieReductor hat configuration-cache bewusst auf `false` wegen `readText()` zur Config-Zeit — Memory `reference_entropie_config_cache_false`. BestJournalAndroid: bei JSON-Tausch im Blick behalten.)
**Quelle:** github.com/gradle/gradle/issues/20693

## 122. App-Check-Debug-Token auf CI / mehreren Geraeten
**Symptom:** 403 Attestation-Fehler im Emulator/CI/zweitem Geraet; Dienste blockiert.
**Ursache:** Play Integrity klassifiziert Emulator/CI als ungueltig; jedes Geraet erzeugt ein eigenes Debug-Token.
**Versionen:** per Design.
**FIX:** Debug-Provider + Debug-Token in der Konsole registrieren; auf CI als Env-Var `FIREBASE_APP_CHECK_DEBUG_TOKEN` injizieren (NICHT committen — SK-Regel). Pro Geraet ein Token; bei Leak sofort revoken.
**Quelle:** firebase.google.com/docs/app-check/android/debug-provider

---

# Teil 11 — Nachträge aus dem Best-Practices-Lauf (2026-06-02)

> Diese Bugs wurden bei der **Best-Practices-Recherche** (Praeventions-Seite,
> `best-practices/android/firebase-billing.md`) als echte, nicht-duplizierte
> Stolpersteine gefunden und hierher zurueckgekoppelt. Jeder verweist auf den passenden
> Best-Practice-Abschnitt.

## 123. Promo-Code-Kaeufe lassen sich nicht ueber orderId deduplizieren   ⭐ HAEUFIG
**Symptom:** Promo-Code-eingeloeste Kaeufe tauchen doppelt auf / die Dedup-Logik greift nicht.
**Ursache:** Deduplizierung ueber `orderId` — Promo-Code-Kaeufe (und manche Test-/Renewal-Faelle) erzeugen aber KEINEN `orderId`.
**Versionen:** per Design, alle.
**FIX:** `purchaseToken` (global eindeutig) als Dedup-Schluessel verwenden, NIE `orderId`. Siehe Best-Practices Teil 2 (Idempotenz). Ergaenzt Bug 29.
**Quelle:** developer.android.com/google/play/billing/security

## 124. Mehrere BillingClient-Instanzen → doppelte onPurchasesUpdated-Callbacks
**Symptom:** Entitlement-Logik laeuft mehrfach pro Kauf; doppelte Grants/Acknowledges.
**Ursache:** BillingClient pro Activity gebaut statt als App-Singleton → mehrere aktive Listener feuern fuer dasselbe Ereignis.
**Versionen:** per Design, alle.
**FIX:** Genau EIN App-Scope-Singleton (Application-Context), ein zentraler `PurchasesUpdatedListener`. Praezisiert Bug 9 (Memory-Leak/Singleton). Siehe Best-Practices Teil 1 A.
**Quelle:** developer.android.com/reference/com/android/billingclient/api/BillingClient

## 125. Refund OHNE Revoke erscheint nicht in der Voided Purchases API
**Symptom:** Per Refund erstattete Kaeufe ohne Entzug werden serverseitig nie als „void" erkannt; Entitlement bleibt aktiv.
**Ursache:** Die Voided Purchases API listet nur Refunds MIT Revoke; reine Erstattungen ohne Entzug fehlen.
**Versionen:** Server-API, per Design.
**FIX:** Zusaetzlich RTDN `voidedPurchaseNotification` verarbeiten UND die Refund-/Revoke-Policy in der Play Console pruefen; nicht allein auf Voided-API-Polling verlassen. Siehe Best-Practices Teil 2 D.
**Quelle:** developers.google.com/android-publisher/voided-purchases · developer.android.com/google/play/billing/rtdn-reference

## 126. RTDN-Duplikate / Out-of-order ohne Idempotenz → Doppel-Grants / Zustandsruecksetzer   ⭐ HAEUFIG
**Symptom:** Abo-Status springt zurueck oder Premium wird doppelt gewaehrt.
**Ursache:** Pub/Sub liefert mindestens einmal (Duplikate moeglich) und NICHT garantiert in Reihenfolge; ohne Dedup/Versionierung wird eine alte Notification spaeter verarbeitet.
**Versionen:** Pub/Sub, per Design.
**FIX:** Pro Event per `messageId`/`eventTimeMillis` deduplizieren; nach JEDER Notification frischen `subscriptionsv2.get` machen und den Zustand AUS dem get ableiten (nie aus dem Notification-Typ). Ergaenzt Bug 33. Siehe Best-Practices Teil 2 C.
**Quelle:** developer.android.com/google/play/billing/rtdn-reference · cloud.google.com/pubsub/docs/subscriber

## 127. Pub/Sub-Push-Endpoint antwortet nicht 2xx → Endlos-Re-Delivery + Quota-Verbrauch
**Symptom:** Dieselbe RTDN kommt endlos wieder; Function-Kosten/Quota steigen.
**Ursache:** Eine Push-Subscription wertet Non-2xx als Fehler und re-delivered; eine langsame/fehlerhafte Function antwortet nicht rechtzeitig mit 2xx.
**Versionen:** Pub/Sub Push, per Design.
**FIX:** In der Cloud Function SOFORT 2xx ack'en, die eigentliche Arbeit idempotent (ggf. asynchron) erledigen; nie blockierende Lang-Arbeit vor dem Ack. Siehe Best-Practices Teil 2 C/E.
**Quelle:** cloud.google.com/pubsub/docs/push

## 128. App-Check Replay-Schutz (limited-use/consume-Token) nur fuer Node.js-Backend
**Symptom:** Der erwartete Replay-Schutz fuer einen sensiblen Function-Aufruf greift auf einem nicht-Node-Backend nicht.
**Ursache:** Die token-`consume`-API (Replay-Schutz) ist Beta und nur im Node-Admin-SDK verfuegbar.
**Versionen:** aktuell, Beta.
**FIX:** Fuer Replay-kritische Aufrufe Node.js-Cloud-Functions + `consumeAppCheckToken` nutzen; auf anderen Runtimes eine eigene Nonce-/Idempotenz-Schicht bauen, nicht den limited-use-Replay-Schutz annehmen. Siehe Best-Practices Teil 6 C.
**Quelle:** firebase.google.com/docs/app-check/cloud-functions

## 129. PendingIntent ohne FLAG_IMMUTABLE → Crash ab Android 12 (FCM-Deep-Links)
**Symptom:** Notification-Tap/Deep-Link crasht mit `IllegalArgumentException` ab Android 12 / targetSdk 31+ (garantiert bei targetSdk 36).
**Ursache:** Ab Android 12 muss jeder PendingIntent explizit `FLAG_IMMUTABLE` oder `FLAG_MUTABLE` setzen.
**Versionen:** Android 12+ (targetSdk 31+).
**FIX:** PendingIntent mit `FLAG_IMMUTABLE` (oder bewusst MUTABLE) bauen. Gilt auch ausserhalb FCM — siehe `bugs/android/android-platform.md` (PendingIntent). Siehe Best-Practices Teil 4 D.
**Quelle:** developer.android.com/reference/android/app/PendingIntent

## 130. Legacy FCM Server-Key zum Senden → schlaegt fehl (HTTP v1 Pflicht)
**Symptom:** Server-Push schlaegt fehl/401, obwohl es frueher funktionierte.
**Ursache:** Die Legacy-HTTP-/XMPP-Server-Key-APIs wurden im Juli 2024 abgeschaltet.
**Versionen:** ab 2024-07.
**FIX:** Nur noch FCM HTTP v1 API mit OAuth2/Service-Account verwenden (z.B. ueber eine Cloud Function senden). Siehe Best-Practices Teil 4 E.
**Quelle:** firebase.google.com/docs/cloud-messaging/migrate-v1

## 131. Consent Mode v2: Default-Consent NACH der Init gesetzt → DSGVO-Luecke   ⭐ HAEUFIG
**Symptom:** Die ersten Analytics-Events laufen mit falschem Consent-Status raus (z.B. in der EU ohne Einwilligung).
**Ursache:** `setConsent(...)` erst nach der Firebase-Init aufgerufen, statt den Default-Consent VOR der Init zu setzen.
**Versionen:** per Design.
**FIX:** Default-Consent als Manifest-`meta-data` setzen (EU: denied, opt-in), erst nach Einwilligung per `setConsent()` updaten. Keine PII in Analytics. Siehe Best-Practices Teil 7 E.
**Quelle:** firebase.google.com/docs/analytics/android/consent · developers.google.com/tag-platform/security/concepts/consent

## 132. fetchAndActivate() blockiert den ersten App-Start (RC-Timeout)
**Symptom:** UI haengt beim ersten Start bis zu ~1 Minute.
**Ursache:** Synchron beim Erststart auf Remote Config gewartet, statt „load for next startup".
**Versionen:** per Design.
**FIX:** Beim Erststart XML-Defaults nutzen, RC asynchron laden und erst beim naechsten Start aktivieren; nie den UI-Thread auf den Fetch warten lassen. Ergaenzt Bug 83/86. Siehe Best-Practices Teil 6 B.
**Quelle:** firebase.google.com/docs/remote-config/loading

## 133. Lange Arbeit direkt in onMessageReceived → abgebrochen (Zeitfenster)
**Symptom:** Server-Call/Bild-Download direkt im Callback wird abgebrochen, Verarbeitung unvollstaendig.
**Ursache:** `onMessageReceived` hat nur ein kurzes Ausfuehrungsfenster (~Sekunden, Doze-abhaengig).
**Versionen:** per Design.
**FIX:** Im Callback nur kurz handeln; laengere Arbeit an WorkManager (expedited) delegieren. Ergaenzt Bug 76. Siehe Best-Practices Teil 4 C.
**Quelle:** firebase.google.com/docs/cloud-messaging/android/receive-messages

---

# Teil 12 — Firebase Firestore (PROAKTIV — noch nicht eingebunden)

> **PROAKTIV:** Firestore ist in BestJournalAndroid aktuell NICHT eingebunden (lokale Daten in Room,
> Backup via Drive). Diese Sektion ist Zukunftswissen, sobald ein Cloud-Sync- oder Entitlement-Store
> ueber Firestore kommt. App-Check-Enforcement-zu-frueh fuer Firestore ist bereits durch **Bug 90**
> (generell) abgedeckt — gestaffelt aktivieren. Volle Praevention: Best-Practices Teil 5.

## 134. Offene Security Rules (`allow read, write: if true`) → komplette Daten oeffentlich   ⭐⭐ KRITISCH
**Symptom:** Jeder kann alle Daten lesen/schreiben; Datenleck und -manipulation.
**Ursache:** Default-Deny verletzt — Test-Rules in Prod (`if true`) oder zu weit gefasste Bedingungen.
**Versionen:** per Design.
**FIX:** Default-Deny; mindestens `request.auth != null`; private Daten per Ownership (`request.auth.uid == resource.data.ownerId`); `read`/`write` granular in `get/list/create/update/delete` aufteilen. Subcollections erben Rules NICHT.
**Quelle:** firebase.google.com/docs/firestore/security/get-started

## 135. „Rules sind keine Filter" → PERMISSION_DENIED bei korrekt aussehender Query   ⭐ HAEUFIG
**Symptom:** Eine `list`-Query schlaegt komplett mit PERMISSION_DENIED fehl, obwohl die eigenen Daten regelkonform waeren.
**Ursache:** Security Rules filtern NICHT; die Query muss die Rule-Constraint per `where()` spiegeln, sonst wird die ganze Query verweigert.
**Versionen:** per Design.
**FIX:** Query-`where()` exakt an die Rule-Bedingung anpassen (z.B. `whereEqualTo("ownerId", uid)`).
**Quelle:** firebase.google.com/docs/firestore/security/rules-query

## 136. Entitlement-/Abo-Felder client-schreibbar → Premium clientseitig faelschbar   ⭐⭐ KRITISCH
**Symptom:** Ein Nutzer setzt sich selbst per direktem Firestore-Write auf Premium.
**Ursache:** Rules erlauben Client-Writes auf Abo-/Entitlement-Felder.
**Versionen:** per Design.
**FIX:** Entitlement-Dokumente fuer Clients `write: if false`; nur per Admin SDK / Cloud Function (umgeht Rules) schreiben. Client liest nur. Spiegelt die Billing-Server-Wahrheit (Teil 2).
**Quelle:** firebase.google.com/docs/firestore/security/rules-structure

## 137. Sequenzielle / monoton steigende Document-IDs → Hotspotting + Latenz
**Symptom:** Schreib-Latenz steigt bei hoher Frequenz; „500/50/5"-Regel verletzt.
**Ursache:** Monoton steigende IDs (z.B. uebernommene Room-Auto-Increment-IDs) konzentrieren Writes auf eine Partition.
**Versionen:** per Design.
**FIX:** Auto-IDs (`add()` / `collection().document()`) verwenden; bei Migration von Room IDs NICHT 1:1 als Doc-IDs uebernehmen.
**Quelle:** firebase.google.com/docs/firestore/best-practices

## 138. Offset-Pagination erzeugt versteckte Read-Kosten
**Symptom:** Pagination teurer als erwartet — Reads steigen ueberproportional.
**Ursache:** `offset(n)` laedt und berechnet die uebersprungenen n Dokumente intern als Reads.
**Versionen:** per Design.
**FIX:** Cursor-Pagination (`startAfter(lastDoc)`) statt `offset`. Offline-Cache (Android) ist per Default an — Listener nur wo noetig, sonst one-time `get()`.
**Quelle:** firebase.google.com/docs/firestore/best-practices

---

# Fix-Status (gh-verifiziert 2026-06-02)

> **Methodik (ehrlich):** GitHub-Issue-Status hart per `gh issue view` geprueft. **Wichtig:** "CLOSED/COMPLETED"
> heisst bei diesen Issues meist *beantwortet/dokumentiert* (viele beschreiben **per-Design-Verhalten** wie
> den 12h-RC-Throttle oder das Acknowledge-Fenster) — der dokumentierte **Workaround bleibt noetig**. Nur
> wenige sind echte SDK-Versions-Fixes. Die Spalte "Bedeutung" trennt das.

| Bug | Issue | Status (gh) | Bedeutung fuer den Workaround |
|-----|-------|-------------|-------------------------------|
| 88 | firebase-android-sdk#7110 | **OPEN** | Aktiver SDK-Bug — Workaround (App-Signing-SHA + transiente Fehler abfangen) bleibt zwingend |
| 56 | firebase-android-sdk#5962 | CLOSED 2024-06 | **Echter Fix:** Plugin-Regression 3.0.1 → 3.0.2+/3.0.7 behoben → Versions-Bump loest |
| 116 | kotlinx.serialization#2501, #3033 | CLOSED 2023-11 / 2025-12 | **Teil-Fix:** Lib liefert jetzt Keep-Regeln; eigene `@Serializable`-Modelle weiter selbst keepen |
| 117 | firebase-android-sdk#2124 | CLOSED 2023-07 | Consumer-Rules ergaenzt; bei Bedarf eigene Keeps |
| 49 | firebase-android-sdk#3740, #5746 | CLOSED 2022/2024 | Consumer-Rules verbessert; `missing_rules.txt` bleibt die Quelle der Wahrheit |
| 48 | firebase-android-sdk#4693, #6039 | CLOSED 2024 | Doku/Verhalten geklaert; Init-Reihenfolge-Workaround bleibt |
| 47 | firebase-android-sdk#5972 | CLOSED 2024-06 | Lint-Klaerung; BOM-Regel bleibt (keine Einzel-Versionen pinnen) |
| 91 | firebase-android-sdk#5190 | CLOSED 2023-08 | Init-Reihenfolge-Workaround bleibt |
| 84 | firebase-android-sdk#5908 | CLOSED 2024-05 | per Design (Throttle) — Workaround bleibt |
| 87 | firebase-android-sdk#4864, #5040 | CLOSED 2023 | geklaert; `activate()`-im-`onUpdate`-Workaround bleibt |
| 53 | firebase-android-sdk#2046 | CLOSED 2021-11 | weitgehend in Plugin v3 behoben; Reihenfolge bleibt relevant |
| 57 | firebase-android-sdk#6230 | CLOSED 2024-09 | geklaert; google-services-Plugin bleibt Voraussetzung |
| 70 | firebase-android-sdk#5025 | CLOSED 2023-06 | geklaert; Provider-Remove-Workaround bleibt |
| 75 | firebase-android-sdk#5697 | CLOSED 2024-02 | geklaert; Consent-Mode-frueh-setzen bleibt |
| 72 | firebase-android-sdk#3189 | CLOSED 2021-12 | per Design (DebugView) |
| 44 | google/play-services-plugins#74 | CLOSED 2019-11 | Plugin-Verhalten verbessert; `clean` nach JSON-Aenderung bleibt sicherste Loesung |
| 121 | gradle/gradle#20693 | CLOSED 2022-06 | geklaert; configuration-cache mit-invalidieren bleibt |

**Noch NICHT gefixt (Workaround bleibt aktiv):**
- **Bug 88 (#7110, OPEN)** — App Check Play Integrity 403 auf zertifizierten Geraeten.
- **Alle per-Design-Bugs** (acknowledge, PENDING, RC-Throttle, FCM-Background-notification, App-Check-Enforcement, Billing-Version-Deadline, R8-Keep-Regeln, Region-/Backend-Wahl …) — diese sind kein SDK-Bug, sondern erwartetes Verhalten; der dokumentierte Umgang bleibt dauerhaft noetig.

**Nicht hart verifiziert (ehrlich markiert):** Issues #2265/#2198/#2163 (Bug 66), #2191 (Bug 120) und die react-native-firebase-/flutterfire-Issues (Bugs 93/94, gleiche Native-Schicht) wurden nur aus Such-Snippets abgeleitet, nicht per `gh` geprueft. Das "max 20 IDs"-Limit bei `queryProductDetailsAsync` konnte NICHT belegt werden — separat in der `QueryProductDetailsParams`-Reference verifizieren.

---

# Pflicht-Checkliste vor Firebase-/Billing-Arbeit

- [ ] **Billing:** `enablePendingPurchases(PendingPurchasesParams…)` gesetzt? (Bug 1)
- [ ] **Billing:** `acknowledgePurchase()` binnen 3 Tagen nach jedem PURCHASED, idempotent? (Bug 14)
- [ ] **Billing:** `PurchaseState.PENDING` nie sofort freigeschaltet? (Bug 18)
- [ ] **Billing:** Abo-Kauf mit `offerToken` (Bug 23) und Plan-Wechsel als Replace (Bug 26)?
- [ ] **Billing:** queryPurchasesAsync beim App-Start + ON_HOLD ueber Backend (Bugs 22, 30)?
- [ ] **Billing:** Entitlement serverseitig verifiziert, `obfuscatedAccountId` gesetzt (Bug 29)?
- [ ] **Billing:** v8-Migration vor 31.08.2026 eingeplant (Bug 41)?
- [ ] **Billing-Test:** Lizenz-Tester eingetragen (Bugs 39, 40), nur Release-AAB?
- [ ] **Firebase:** google-services.json nach Konsolen-Aenderung `clean` gebaut (Bug 44)?
- [ ] **Firebase:** App-Signing-Key-SHA in der Konsole (Bug 45)?
- [ ] **Firebase:** debug-Variante hat eigene JSON wegen `.debug`-Suffix (Bug 42)?
- [ ] **App Check:** Debug-Token eingetragen (Bug 89), Enforcement erst nach Rollout (Bug 90), Init vor anderen Diensten (Bug 91)?
- [ ] **AI:** Modell-Name ueber Remote Config statt hardcoded (Bug 105), Backend bewusst gewaehlt (Bug 106)?
- [ ] **Release:** R8-Keep-Regeln fuer Firebase/Billing/Gemini geprueft, Release-Build getestet (Bugs 113–117)?

---

## Querverweis: Best Practices

**Hauptseite (Praeventions-Seite der Medaille):** [`best-practices/android/firebase-billing.md`](../../best-practices/android/firebase-billing.md)
— erstellt 2026-06-02 (7-Researcher-Best-Practices-Lauf). Wechselseitige Abschnitts-Kopplung:

| Bug-Almanach-Abschnitt | Best-Practice-Abschnitt (`firebase-billing/best-practices.md`) |
|------------------------|----------------------------------------------------------------|
| Teil 1 (Billing 1–28: Verbindung, Acknowledge, PENDING, Proration) | Teil 1 — sichere Client-Flows |
| Teil 1 D + E (29–41: Signature, RTDN, Developer API, Deadline) | Teil 1 D + Teil 2 — serverseitige Validierung |
| Teil 2 (42–52: google-services, Init, BOM, SHA) | Teil 7 — Setup/Init/BOM/Analytics/Consent |
| Teil 3 (53–75: Crashlytics/Analytics) | Teil 3 — Crashlytics richtig (proaktiv) |
| Teil 4 (76–82: FCM) | Teil 4 — FCM sauber (proaktiv) |
| Teil 5 (83–87: Remote Config) | Teil 6 B — Remote Config |
| Teil 6 (88–91: App Check) | Teil 6 A — App Check |
| Teil 7 (92–96: Cloud Functions) | Teil 6 C — Cloud Functions |
| Teil 11 (123–133: Nachträge) | Teil 1/2/4/6/7 (jeweils im Eintrag verlinkt) |
| Teil 12 (134–138: Firestore proaktiv) | Teil 5 — Firestore Security Rules & Data Modeling |

**Querverweise in andere Almanache/Best-Practices:**

| Bug-Almanach-Abschnitt | Best-Practice (Praevention) |
|------------------------|------------------------------|
| Teil 9 (R8/Keep 113–119) | `best-practices/android-build/gradle.md §4` (R8/Shrinking/Keep-Regeln) |
| Teil 4 (FCM, high-priority Start, Bug 80) | `best-practices/android/android-platform.md` (FGS-Start-Trigger) |
| Teil 2/10 (google-services.json, config-cache) | `best-practices/android-build/gradle.md` (Build-System) |
| Bug 129 (PendingIntent FLAG_IMMUTABLE) | `bugs/android/android-platform.md` (PendingIntent) + `best-practices/android/android-platform.md` |


---

## 🔗 Bezug zu den Best-Practices ([`best-practices/android/firebase-billing.md`](../../best-practices/android/firebase-billing.md))

Zweite Seite der Medaille: Hier steht *was schiefgeht und wie man es loest* — die
Best-Practices sagen *wie man den Fehler von vornherein vermeidet*. Nach jedem Fix
hier auch den passenden Praeventions-Abschnitt dort verankern, damit der Fehler nicht wiederkommt.
