# Manuelle Fixes — ausstehend (finale-Lauf cbf88768, 2026-06-10/11)

## E1 — Grace-Period/On-Hold in SubscriptionState modellieren (einzige offene Aufgabe)
- Befund: SubscriptionState ist binaer (Free/Subscribed). Play-Billing-Staaten Grace Period,
  On-Hold, Paused werden nicht modelliert — zahlende Nutzer mit Zahlungs-Haenger koennten zu
  frueh auf Free degradiert werden (Play-Policy-Empfehlung + Verbraucherfreundlichkeit).
- Warum nicht in diesem Lauf: function-required (State-Machine-Umbau in BillingManager +
  UI-Verhalten pro State + Tests) — Frank-Entscheidung 2026-06-10: eigene Session.
- Einstieg: BillingManager.kt (SubscriptionState), Firebase Function getSubscriptionStatus
  (liefert bereits EXPIRED serverseitig), RTDN-Typen SUBSCRIPTION_IN_GRACE_PERIOD /
  SUBSCRIPTION_ON_HOLD / SUBSCRIPTION_PAUSED.
- Kein Release-Blocker.

## Dokumentierte bewusste Entscheidungen (KEINE offenen Fixes)
- E2 Restore-Button: bewusst NUR Auto-Restore (App-Start). Button existierte und wurde nach
  Recherche als redundant entfernt (Commit #1236). Erneut bestaetigt 2026-06-10.
- D2 Lifetime-Kauf: Google-Play-Sheet uebernimmt Kauf-Bestaetigung (#1233, v0.17.2-Entscheidung);
  paywall_legal_hint verweist auf Widerrufsbelehrung. Nur der stale XML-Kommentar wurde korrigiert.
- C6 PDF-Export premium-gated: DSGVO-Art.-20-Risiko gering — Daten liegen lokal beim Nutzer,
  Drive-Backup im EIGENEN Google Drive (kein Anbieter-Lock-in).
- Z1 Keine automatische Krisen-Erkennung: bewusste Produktentscheidung; CrisisHelpDialog +
  Krisen-Disclaimer + findahelpline.com-Link existieren (manuell erreichbar).
