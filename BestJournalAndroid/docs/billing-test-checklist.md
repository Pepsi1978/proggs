# BestJournal Billing Test Checklist

Vor jedem Release diesen Checklist manuell auf einem physischen Gerät durchgehen.
Google Play Billing kann **nicht** im Emulator mit echten Käufen getestet werden —
verwende ein Testgerät mit dem Google Play Billing Tester-Account (Play Console → Lizenz-Tester).

---

## 0. Vorbereitung

- [ ] Testgerät mit Lizenz-Tester-Account angemeldet (Play Console → Setup → Lizenz-Tester)
- [ ] App als **interne Testversion** hochgeladen und Testgerät in der Tester-Liste
- [ ] Produktionspreise in der Play Console eingestellt: monatlich €3,99, jährlich €29,99
- [ ] Google Play Store auf dem Gerät aktuell (Play Services ≥ 24.x)

---

## 1. Erstverbindung mit Google Play Billing

| # | Test | Erwartet |
|---|------|----------|
| 1.1 | App kalt starten | `BillingManager.init()` verbindet sich, keine Abstürze |
| 1.2 | App starten bei deaktiviertem Netz | `onBillingServiceDisconnected` wird ausgelöst, App bleibt stabil |
| 1.3 | Netz wieder aktivieren nach 1.2 | Automatische Reconnect-Logik greift, `BillingManager` stellt Verbindung her |
| 1.4 | App in Hintergrund schieben und nach 5 min zurückkehren | Billing-Verbindung wird bei Bedarf automatisch neu aufgebaut |

---

## 2. Abo-Kauf (Monatsabo)

| # | Test | Erwartet |
|---|------|----------|
| 2.1 | Abo-Sheet öffnen → Monatsabo antippen | Play-Kauf-Dialog erscheint mit Preis €3,99/Monat |
| 2.2 | Kauf bestätigen | `onPurchasesUpdated` mit `OK` ausgelöst |
| 2.3 | Nach Kaufbestätigung | `acknowledgePurchase` wird aufgerufen, `SubscriptionState = Subscribed` |
| 2.4 | App neu starten nach Kauf | `querySubscriptionsAsync` erkennt aktives Abo, UI zeigt Subscriber-Status |
| 2.5 | Kauf im Billing-Dialog abbrechen | `onPurchasesUpdated` mit `USER_CANCELED`, kein Fehler-Dialog, UI unverändert |

---

## 3. Abo-Kauf (Jahresabo)

| # | Test | Erwartet |
|---|------|----------|
| 3.1 | Jahresabo antippen | Play-Dialog zeigt €29,99/Jahr |
| 3.2 | Kauf bestätigen | Gleicher Ablauf wie 2.2–2.4, `isYearly = true` Pfad |
| 3.3 | Ersparnisanzeige | UI zeigt "37% Ersparnis" oder äquivalent |

---

## 4. Acknowledge-Verhalten

| # | Test | Erwartet |
|---|------|----------|
| 4.1 | Kauf kaufen und direkt App schließen vor Acknowledge | Beim nächsten Start wird `purchase.purchaseState == PURCHASED && !isAcknowledged` erkannt, Acknowledge erneut ausgelöst |
| 4.2 | Acknowledge schlägt fehl (Netz trennen kurz nach Kauf) | `Log.e("BillingManager", "Acknowledge failed: ...")` im Logcat, `SubscriptionState` bleibt `NotSubscribed` |
| 4.3 | Acknowledge erfolgreich | `SubscriptionState.Subscribed`, Logcat zeigt keinen Fehler |

---

## 5. Bestehende Abos beim App-Start

| # | Test | Erwartet |
|---|------|----------|
| 5.1 | Gerät mit aktivem Abo, App frisch installieren | `querySubscriptionsAsync` erkennt Abo, `Subscribed`-Status direkt beim Start |
| 5.2 | Abgelaufenes Abo (Kündigung im Play Store) | `querySubscriptionsAsync` gibt leere Liste, `NotSubscribed`-Status |
| 5.3 | Abo in der Google Play Console manuell widerrufen | Beim nächsten Start oder Check: `NotSubscribed` |

---

## 6. Subscriber-AI-Limits

| # | Test | Erwartet |
|---|------|----------|
| 6.1 | Mit Abo: erste 30 Dashboard-Refreshes | Flash 2.5 Modell (hohe Qualität) |
| 6.2 | Mit Abo: 31.–100. Dashboard-Refresh | Flash 2.5 Lite (kein UI-Hinweis) |
| 6.3 | Mit Abo: 101. Dashboard-Refresh | Cooldown-Meldung: "Du hast heute schon 100 Aktualisierungen gemacht — Pause, in 30 Minuten geht's weiter" |
| 6.4 | Mit Abo: nach 30 Minuten Warten | Weiter verwendbar (Lite-Modell) |
| 6.5 | Mit Abo: 151. Dashboard-Refresh | Hard-Limit-Meldung: "Userlimit erreicht — neue Aktualisierungen morgen wieder verfügbar" |
| 6.6 | Mit Abo: Text-Verbesserung Limits | Gleicher 4-Stufen-Ablauf wie Dashboard (30/100/150) |

---

## 7. Trial-AI-Limits (erste 7 Tage, kein Abo)

| # | Test | Erwartet |
|---|------|----------|
| 7.1 | Trial: erste 20 Dashboard-Refreshes | Flash 2.5 Modell |
| 7.2 | Trial: 21.–80. Dashboard-Refresh | Flash 2.5 Lite |
| 7.3 | Trial: 81. Dashboard-Refresh | Cooldown-Meldung mit "80 Aktualisierungen" und "30 Minuten" |
| 7.4 | Trial: nach Cooldown | Weiter bis 100, dann Hard-Limit |
| 7.5 | Trial: 101. Dashboard-Refresh | Hard-Limit-Meldung |
| 7.6 | Trial: Text-Verbesserung gleiche Limits (unabhängig von Dashboard) | 20/80/100 Limits, eigener Tageszähler |

---

## 8. Freemium-Limits (nach 7 Trial-Tagen, kein Abo)

| # | Test | Erwartet |
|---|------|----------|
| 8.1 | Free: 5 Dashboard-Refreshes | Alle erlaubt (Lite-Modell) |
| 8.2 | Free: 6. Dashboard-Refresh | `AiLimitReachedSheet` erscheint mit Abo-Angebot |
| 8.3 | Free: 5 Textverbesserungen | Alle erlaubt (Lite-Modell) |
| 8.4 | Free: 6. Textverbesserung | `AiLimitReachedSheet` erscheint |
| 8.5 | Free: Dashboard- und Text-Zähler sind getrennt | 5x Dashboard + 5x Text = 10 Aktionen total (nicht 5 geteilt) |
| 8.6 | Free: Nach 7 Tagen Reset | Zähler zurück auf 0, 5 neue Aktionen pro Feature |

---

## 9. Modell-Switching

| # | Test | Erwartet |
|---|------|----------|
| 9.1 | Flash-Tier aktiv | Antwortqualität merklich besser (komplexere Analyse) |
| 9.2 | Stiller Wechsel zu Lite | Kein UI-Dialog, kein Fehler — Modell wechselt ohne Nutzer-Benachrichtigung |
| 9.3 | Logcat prüfen | `generateContent` zeigt `gemini-2.5-flash` vs. `gemini-2.5-flash-lite` |

---

## 10. Entry-Analyse (AnalyzeEntropyUseCase)

| # | Test | Erwartet |
|---|------|----------|
| 10.1 | Trial mit 25 Einträgen | Nur 20 neueste Einträge werden analysiert |
| 10.2 | Subscriber mit 50 Einträgen | Nur 40 neueste Einträge werden analysiert |
| 10.3 | Freemium mit 30 Einträgen | Nur 20 neueste Einträge werden analysiert |

---

## 11. Edge Cases & Fehlerszenarien

| # | Test | Erwartet |
|---|------|----------|
| 11.1 | Kein Google-Play-Services auf Gerät | Billing-Setup schlägt fehl, App bleibt stabil (Fallback: kein Abo-Angebot) |
| 11.2 | `BILLING_UNAVAILABLE` (Gerät nicht kompatibel) | Kein Absturz, Abo-Button deaktiviert oder ausgeblendet |
| 11.3 | Netz während Kauf verlieren | Fehlermeldung, kein Doppelkauf |
| 11.4 | Gleichen Kauf zweimal versuchen | `ITEM_ALREADY_OWNED` — graceful Handling, kein zweifacher Charge |
| 11.5 | App Update mit aktivem Abo | Abo-Status bleibt erhalten nach Update |
| 11.6 | Abmelden (Sign-Out) mit aktivem Abo | `SubscriptionState` bleibt erhalten (Abo gehört zum Google-Konto, nicht zum lokalen Profil) |

---

## 12. Spam-Schutz

| # | Test | Erwartet |
|---|------|----------|
| 12.1 | 30 AI-Anfragen innerhalb 1 Stunde (alle Nutzertypen) | Cooldown-Meldung: "Kurze Pause, in 5 Minuten geht's weiter" |
| 12.2 | Nach 1 Stunde | Spam-Zähler reset, volle Nutzung wieder möglich |

---

## 13. Nach dem Test

- [ ] Test-Käufe in der Play Console stornieren (Testpurchases → Cancel)
- [ ] Changelog mit Testergebnis und Datum eintragen
- [ ] Kritische Fehler (> Priorität 2) als GitHub-Issue öffnen

---

*Letzte Aktualisierung: 2026-04-01*
*Billing-Library-Version: Google Play Billing 7.1.1*
