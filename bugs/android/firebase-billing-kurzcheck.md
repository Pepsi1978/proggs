# Firebase & Google Play Billing (Android-Backend-Dienste) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
