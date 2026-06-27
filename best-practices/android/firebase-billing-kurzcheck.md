# Firebase & Google Play Billing Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
