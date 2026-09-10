# Code-Reviewer Memory — BestJournalAndroid

## Erkenntnisse aus Code Reviews

- **2026-04-30 Billing-Refactoring**: PaywallViewModel schreibt `PREF_PROMO_TOTAL_MONTHS` direkt in SharedPreferences ohne `_promoTotalMonths` MutableStateFlow in BillingManager zu updaten → StateFlow-Desync, UI zeigt falsche Promo-Monate bis App-Neustart
- **2026-04-30 Billing-Refactoring**: `pendingPromoOfferToken = null` innerhalb der for-loop in `onPurchasesUpdated` — bei purchases.size > 1 kann Token zu früh gecleart werden
- **2026-04-30 Billing-Refactoring**: `launchRetentionOffer()` übergibt Retention-OfferToken als `promoOfferToken` → setzt fälschlicherweise Promo-Counter in `onPurchasesUpdated`
- **Allgemein Android/Billing**: Wenn mehrere ViewModels dieselben SharedPreferences-Keys schreiben wie ein @Singleton-Manager, immer prüfen ob der Manager einen StateFlow-Update benötigt — direktes `prefs.edit()` in ViewModels ist ein Anti-Pattern wenn der Singleton reaktive Konsumenten hat
- **2026-04-30 Final Sanity Check**: `Log.e` statt `Log.d` für diagnostische `=== ===`-Marker in `launchPurchaseFlow` — erzeugt Error-Level-Spam in Firebase Crashlytics. Vor jedem Release prüfen.
- **2026-04-30 Final Sanity Check**: Geteilter Cache-Key `PREF_LAST_CLOUD_STATUS_FETCH` zwischen `maybeRefreshActiveBasePlanFromCloud` und `syncPromoFromCloudExpiry` — bei Neuinstallation ohne lokalen BasePlan wird Promo-Renewal-Check für bis zu 1h geblockt. Muster: verschiedene Cloud-Funktionen brauchen eigene Cache-Keys.
- **Allgemein Android/Billing**: Geteilte throttle/cache Keys zwischen unabhängigen Cloud-Aufruf-Funktionen führen zu unerwarteten Blockierungen. Jede Funktion mit eigenem Rate-Limit braucht einen eigenen PREF-Key.
