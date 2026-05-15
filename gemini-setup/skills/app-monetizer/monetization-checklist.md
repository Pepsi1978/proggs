---
last_updated: "2026-04-06"
version: 2
update_count: 1
last_rotation_ids: []
---

# Monetarisierungs-Checkliste fuer Android-Apps

Diese Checkliste wird bei jedem Skill-Aufruf geprueft und alle 3 Tage via Researcher aktualisiert.
Jeder Punkt hat ein Gewicht (Hoch/Mittel/Niedrig) das den Impact auf die Monetarisierung angibt.
Die Gewichtung wird dynamisch durch den App-Reifegrad beeinflusst (siehe SKILL.md Phase 2).

---

## 1. Paywall & Abo-Praesentation

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 1.1 | Dedizierter Paywall-Screen existiert (nicht nur Settings-Sektion) | Hoch | Fullscreen-Overlay oder eigener Screen mit Hero-Image, Benefit-Liste, CTA-Buttons |
| 1.2 | Benefits sind emotional formuliert (nicht technisch) | Hoch | "Entdecke verborgene Muster in deinem Denken" statt "Unbegrenzte KI-Analysen" |
| 1.3 | Social Proof vorhanden (Bewertungen, Nutzerzahlen, Testimonials) | Hoch | "Von 50.000+ Nutzern vertraut" oder echte Store-Bewertungen eingeblendet |
| 1.4 | Visuelle Hierarchie: Jahresabo hervorgehoben (Anker-Pricing) | Hoch | Jahresabo groesser, farbig, "Beliebteste Wahl"-Badge; Monatsabo kleiner/grau |
| 1.5 | Ersparnis beim Jahresabo klar kommuniziert | Mittel | "Spare 37%" oder "Das sind nur 2,50 Euro/Monat" prominent angezeigt |
| 1.6 | Kostenlose Testphase angeboten (7 Tage Trial) | Hoch | "7 Tage kostenlos testen — jederzeit kuendbar" als primaerer CTA |
| 1.7 | CTA-Button ist gross, farbig, einladend | Mittel | Mindestens 48dp Hoehe, Primaerfarbe, abgerundete Ecken, ggf. mit Gradient |
| 1.8 | Schliessen/Ablehnen-Option dezent aber vorhanden | Mittel | Kleiner "Weiter ohne Premium"-Link, NICHT prominent (aber zugaenglich) |
| 1.9 | Keine Sackgasse nach Ablehnung | Mittel | Benutzer wird nicht bestraft, kann die App normal weiternutzen |
| 1.10 | Lottie-Animation oder Illustrationen auf der Paywall | Niedrig | Animierte Grafiken erhoehen die wahrgenommene Wertigkeit |

## 2. Onboarding & First Impression

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 2.1 | Onboarding-Flow existiert (3-5 Screens) | Hoch | Swipeable Screens die den Wert der App demonstrieren BEVOR Login/Registrierung |
| 2.2 | Aha-Moment innerhalb der ersten 2 Minuten | Hoch | Benutzer erlebt den Kernwert der App bevor er etwas bezahlen muss |
| 2.3 | Value-Demonstration im Onboarding | Hoch | Nicht nur "Willkommen", sondern zeigen was die App KANN (Demo-Daten, Beispiel-Analyse) |
| 2.4 | Soft-Paywall am Ende des Onboardings | Mittel | Nach dem Aha-Moment eine sanfte Einladung zum Trial — kein Zwang |
| 2.5 | Personalisierung im Onboarding | Mittel | "Was ist dein Ziel?" — macht die App persoenlich und erhoeht Bindung |
| 2.6 | Skip-Option fuer erfahrene Nutzer | Niedrig | "Direkt loslegen" fuer Nutzer die das Onboarding nicht brauchen |
| 2.7 | Splash-Screen professionell und schnell (<2s) | Niedrig | Logo-Animation, App-Name, sofortiger Uebergang |

## 3. Upsell-Trigger & Touchpoints

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 3.1 | Kontextuelle Upsells bei Feature-Nutzung | Hoch | Wenn Nutzer ein Premium-Feature antippen: erklaeren was es tut + "Mit Premium freischalten" |
| 3.2 | Limit-Messaging ist positiv formuliert | Hoch | "Du hast diese Woche 5 tolle Analysen gemacht! Fuer mehr: Premium" statt "Limit erreicht" |
| 3.3 | Premium-Features sind sichtbar aber gesperrt (Feature-Gating) | Hoch | Nutzer SIEHT was Premium kann (ausgegraut, Schloss-Icon), nicht versteckt |
| 3.4 | Mindestens 3-5 natuerliche Upsell-Touchpoints | Mittel | Verschiedene Stellen in der App wo Premium sanft erwaehnt wird |
| 3.5 | Upsell nach Erfolgs-Moment | Mittel | Nach einer guten Analyse: "Moechtest du das taeglich?" — hohes Conversion-Potenzial |
| 3.6 | Banner/Hinweis waehrend Trial-Phase (Tag 4-7) | Mittel | Sanfte Erinnerung dass der Trial bald endet, mit Upgrade-Option |
| 3.7 | Kein aggressives Upselling (maximal 1x pro Session) | Mittel | Nutzer duerfen nicht genervt werden — das fuehrt zu Deinstallation |

## 4. Pricing & Packaging

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 4.1 | Mindestens 2 Preisstufen (Monat + Jahr) | Hoch | Monatsabo als Einstieg, Jahresabo als beste Wahl hervorgehoben |
| 4.2 | Jahres/Monats-Verhaeltnis optimal (max 60% des Jahrespreises) | Hoch | Monatsabo darf NICHT zu guenstig sein, sonst kein Anreiz fuer Jahresabo |
| 4.3 | Decoy-Pricing erwaegen (3. Option) | Mittel | Optionale 3. Stufe (z.B. "Lifetime") die das Jahresabo attraktiver macht |
| 4.4 | Preise enden auf .99 | Niedrig | 3,99 statt 4,00 — psychologischer Preiseffekt |
| 4.5 | Waehrung/Preis automatisch lokalisiert | Mittel | Google Play zeigt lokale Waehrung, aber die App-Texte muessen das auch tun |
| 4.6 | "Jederzeit kuendbar" prominent angezeigt | Hoch | Reduziert Kaufangst massiv — eines der wichtigsten Vertrauenssignale |
| 4.7 | Preisvergleich mit Alltags-Ausgaben | Niedrig | "Weniger als ein Kaffee pro Woche" — macht den Preis greifbar |
| 4.8 | Regionale Preise in Google Play Console eingerichtet | Mittel | Kaufkraft-angepasste Preise fuer verschiedene Maerkte (Indien, Brasilien, Osteuropa) |
| 4.9 | Introductory Offer / Einfuehrungspreis konfiguriert | Hoch | Google Play unterstuetzt "Erster Monat 50% Rabatt" oder "Erste 3 Monate guenstiger" — senkt die Einstiegshuerde massiv |
| 4.10 | Saisonale Promotions geplant (Neujahr, Black Friday, Back-to-School) | Mittel | Zeitlich begrenzte Angebote erhoehen Urgency und Conversion — in Google Play Console als Promotion konfigurierbar |

## 5. UI/UX & Benutzerfreundlichkeit

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 5.1 | Modernes Design (Material Design 3 / Material You) | Hoch | Dynamic Color, abgerundete Ecken, Glassmorphism-Elemente, saubere Typografie |
| 5.2 | Konsistentes Farbschema (max 3-4 Farben) | Mittel | Primaerfarbe + Akzentfarbe + Hintergrund + Text — nicht bunt durcheinander |
| 5.3 | Navigation intuitiv (Bottom Nav oder Tab-Leiste) | Hoch | Maximal 4-5 Hauptbereiche, klare Icons, Labels unter den Icons |
| 5.4 | Ladezeiten unter 1 Sekunde | Mittel | Skeleton-Screens oder Shimmer waehrend des Ladens, nie ein leerer Screen |
| 5.5 | Barrierefreiheit (Content Descriptions, Kontrast) | Niedrig | Mindestens 4.5:1 Kontrast, contentDescription fuer alle Icons |
| 5.6 | Microinteractions und Animationen | Mittel | Buttons reagieren auf Touch, Uebergaenge sind smooth, nicht steif |
| 5.7 | Professionelle Typografie | Mittel | Klare Hierarchie (Headline, Body, Caption), nicht zu viele verschiedene Schriften |
| 5.8 | Leerer Zustand (Empty State) ist gestaltet | Mittel | Wenn keine Daten da sind: Illustration + Erklaerung statt leerer Bildschirm |
| 5.9 | Pull-to-Refresh wo sinnvoll | Niedrig | Standard-Pattern das Nutzer erwarten |
| 5.10 | Haptic Feedback bei wichtigen Aktionen | Niedrig | Kurze Vibration bei Speichern, Loeschen, Toggle — erhoehen die wahrgenommene Qualitaet |

## 6. Retention & Engagement

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 6.1 | Push-Notifications (mit Opt-in) | Hoch | Taegliche Erinnerung, Motivationsnachricht, neue Insights — personalisiert |
| 6.2 | Streak-System oder Fortschrittsanzeige | Hoch | "5 Tage in Folge geschrieben!" — Gamification die zum Weitermachen motiviert |
| 6.3 | Wochen-/Monats-Rueckblick | Mittel | Zusammenfassung was der Nutzer erreicht hat — zeigt Wert der App |
| 6.4 | Widgets fuer den Homescreen | Mittel | Quick-Entry-Widget, Streak-Counter, Motivations-Zitat des Tages |
| 6.5 | Belohnungen/Meilensteine | Mittel | "100 Eintraege geschrieben!" mit Badge/Animation — feiert den Nutzer |
| 6.6 | Personalisierte Inhalte/Empfehlungen | Mittel | App lernt vom Nutzer und schlaegt relevante Aktionen vor |
| 6.7 | Community-Funktionen (optional) | Niedrig | Anonymes Teilen, Challenges — erhoecht Engagement, aber komplex |

## 7. Churn-Praevention & Win-Back

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 7.1 | Kuendigungs-Survey implementiert | Hoch | Wenn Nutzer kuendigt: "Was koennen wir verbessern?" — 3-5 vordefinierte Gruende + Freitext |
| 7.2 | Win-Back-Strategie nach Kuendigung | Hoch | Google Play steuert den Kuendigungs-Flow selbst — die App kann NICHT direkt beim Kuendigen abfangen. Stattdessen: Re-Engagement Push 7/14 Tage nach Kuendigung, Sonder-Angebot beim naechsten App-Start nach Abo-Ende, Google Play Resubscribe Deep Link |
| 7.3 | Grace Period nach Abo-Ende | Hoch | Nicht sofort ALLES sperren — 3-7 Tage eingeschraenkten Zugang lassen, sanfter Uebergang |
| 7.4 | Abo-Pause statt Kuendigung anbieten | Mittel | "Mach eine Pause statt zu kuendigen — deine Daten bleiben erhalten" |
| 7.5 | Re-Engagement Push fuer ehemalige Abonnenten | Mittel | 7/14/30 Tage nach Kuendigung: "Wir vermissen dich! Neues Feature X ist jetzt da" |
| 7.6 | Regel: 1 Woche vor Abo-Erneuerung Wert zeigen | Mittel | "Diese Woche hast du X dank Premium erreicht" — erinnert an den Wert kurz vor Zahlung |
| 7.7 | Kuendigungs-Gruende analysieren und beheben | Niedrig | Wenn 60% sagen "zu teuer": Pricing ueberdenken. Wenn 40% sagen "nutze es nicht": Engagement steigern |

## 8. Referral & Virale Verbreitung

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 8.1 | Share-Funktion fuer Erfolge/Ergebnisse | Hoch | "Teile deine Wochenanalyse" mit schoener Grafik die man auf Social Media posten kann |
| 8.2 | Referral-Programm implementiert | Hoch | "Lade einen Freund ein — ihr bekommt beide 1 Monat Premium" oder aehnlich |
| 8.3 | Deep Links fuer Einladungen | Mittel | Freund klickt auf Link → landet direkt in der App (nicht nur im Store) |
| 8.4 | Social-Media-optimierte Shareables | Mittel | Generierte Grafiken/Cards die auf Instagram, WhatsApp, Twitter gut aussehen |
| 8.5 | Teilen nach Meilensteinen vorschlagen | Niedrig | "100 Tage Streak! Moechtest du das teilen?" — natuerlicher Anlass |
| 8.6 | Bewertungs-Aufforderung nach positivem Moment | Mittel | Google In-App Review API nach Erfolgs-Moment (nie nach Fehler oder Limit) |

## 9. Store-Listing & ASO (App Store Optimization)

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 9.1 | Screenshots zeigen den Kernwert (nicht den Splash-Screen) | Hoch | 6-8 Screenshots die die besten Features in Aktion zeigen, mit Text-Overlays |
| 9.2 | App-Beschreibung ist benefit-orientiert | Hoch | Erste 2 Zeilen muessen den Nutzer fesseln — danach liest niemand mehr |
| 9.3 | Keywords optimiert fuer die Kategorie | Mittel | Relevante Suchbegriffe in Titel und Beschreibung |
| 9.4 | Feature-Grafik (Banner) professionell | Mittel | 1024x500 Banner der im Store oben angezeigt wird |
| 9.5 | Bewertungen aktiv eingesammelt (In-App Review API) | Hoch | Nach positivem Moment um Bewertung bitten — NIEMALS nach einem Fehler |
| 9.6 | Auf negative Bewertungen reagieren | Mittel | Zeigt dass der Entwickler aktiv ist und zuhoert |
| 9.7 | Promo-Video im Store | Niedrig | 30s Video das den Wert der App demonstriert |

## 10. Fehlende Features (kategorie-spezifisch)

Dieser Abschnitt wird bei jedem Audit dynamisch gefuellt basierend auf der erkannten
App-Kategorie. Die Researcher suchen: "Was haben die Top-10-Apps dieser Kategorie
das diese App NICHT hat?"

## 11. Monetarisierungs-Modelle (ueber Abo hinaus)

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 11.1 | Abo-Modell ist implementiert und funktioniert | Hoch | Google Play Billing mit Monats- und Jahresabo, Acknowledge korrekt |
| 11.2 | Einmal-Kaeufe als Ergaenzung erwaegen | Mittel | Lifetime-Zugang, Theme-Packs, Icon-Packs — zusaetzliche Einnahmequelle |
| 11.3 | Consumables/Credits erwaegen | Mittel | KI-Credits, Extra-Analysen — fuer Nutzer die kein Abo wollen aber einzeln zahlen |
| 11.4 | Trinkgeld/Spenden-Option | Niedrig | "Unterstuetze den Entwickler" — funktioniert gut bei Indie-Apps mit treuer Community |
| 11.5 | Hybrid-Modell erwaegen | Mittel | Abo fuer Kernfunktionen + Einmalkaeufe fuer kosmetische Extras |
| 11.6 | Werbefreies Erlebnis als Premium-Vorteil | Mittel | Falls Werbung implementiert: "Premium = werbefrei" als starkes Verkaufsargument |
| 11.7 | Aktuelles Modell ist optimal fuer die Kategorie | Hoch | Nicht jede App braucht ein Abo — Einmal-Kauf kann bei Utility-Apps besser funktionieren |

## 12. Analytics & Messbarkeit

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 12.1 | Conversion-Funnel wird getrackt | Hoch | Events: app_opened, onboarding_completed, paywall_shown, trial_started, subscription_purchased |
| 12.2 | Paywall-Performance messbar | Hoch | paywall_shown, paywall_dismissed, cta_clicked, purchase_started, purchase_completed |
| 12.3 | Trial-Conversion getrackt | Hoch | trial_started, trial_day_1/3/5/7, trial_converted, trial_expired |
| 12.4 | Churn-Metriken erfasst | Mittel | subscription_cancelled, cancellation_reason, resubscribed, win_back_shown, win_back_accepted |
| 12.5 | Feature-Nutzung getrackt | Mittel | Welche Premium-Features werden am meisten genutzt? Welche nie? |
| 12.6 | Retention-Metriken | Mittel | Day 1/7/30 Retention, Weekly Active Users, Monthly Active Users |
| 12.7 | A/B-Testing-Infrastruktur vorhanden | Niedrig | Firebase Remote Config oder aehnlich fuer Paywall-Varianten, Pricing-Tests |

## 13. Funktionsweise & Technische Qualitaet

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 13.1 | App startet in unter 2 Sekunden (Cold Start) | Hoch | Kein weisser Bildschirm, sofort Splash oder erster Screen |
| 13.2 | Offline-Funktionalitaet (Kernfunktionen ohne Internet) | Mittel | Daten lokal gespeichert, Sync wenn wieder online |
| 13.3 | Fehlerbehandlung ist nutzerfreundlich | Mittel | Nie ein Crash, immer eine verstaendliche Fehlermeldung mit Handlungsoption |
| 13.4 | Daten-Backup/Export vorhanden | Mittel | Nutzer fuehlt sich sicher dass seine Daten nicht verloren gehen |
| 13.5 | App-Groesse unter 50MB (idealerweise unter 30MB) | Niedrig | Grosse Apps werden seltener heruntergeladen |
| 13.6 | Battery-Drain ist minimal | Niedrig | Keine unnoetige Hintergrund-Aktivitaet |
| 13.7 | Smooth Scrolling (60fps) | Mittel | Keine Ruckler, kein Lag, keine Verzoegerungen |

## 14. Emotionale Bindung & Branding

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 14.1 | App hat eine klare Persoenlichkeit/Stimme | Mittel | Texte sind warm, ermutigend, persoenlich — nicht kalt und technisch |
| 14.2 | Vertrauenssignale vorhanden | Hoch | Datenschutz-Hinweis, "Deine Daten gehoeren dir", Verschluesselung erwaehnt |
| 14.3 | Emotionale Texte bei Premium-Features | Hoch | "Verstehe dich selbst besser" statt "Mehr Analysen pro Tag" |
| 14.4 | Konsistentes Branding (Icon, Farben, Ton) | Mittel | Alles fuehlt sich wie eine Einheit an — Icon, App, Store-Listing |
| 14.5 | Delight-Momente (ueberraschende kleine Freuden) | Niedrig | Konfetti bei Meilensteinen, Easter Eggs, besondere Animationen |

## 15. Dark Mode & Farbschema

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 15.1 | Dark Mode vollstaendig implementiert | Mittel | Alle Screens, alle Dialoge, alle Bottom Sheets — keine weissen Flaechen im Dark Mode |
| 15.2 | AMOLED-optimiert (echtes Schwarz #000000) | Niedrig | Spart Batterie auf OLED-Displays, sieht premium aus |
| 15.3 | Farbkontraste im Dark Mode geprueft | Mittel | Texte gut lesbar, Icons erkennbar, keine zu hellen Elemente die blenden |
| 15.4 | Premium-Elemente visuell abgesetzt | Mittel | Gold, Gradient oder andere visuelle Markierung fuer Premium-Features |
| 15.5 | Automatischer Wechsel (System/Uhrzeit) | Niedrig | Folgt dem System-Theme oder Sonnenauf-/untergang |

## 16. Google Play Compliance

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 16.1 | Transparente Preisdarstellung | Hoch | Preis muss VOR dem Kauf klar angezeigt werden, keine versteckten Kosten |
| 16.2 | Kuendigungsmoeglichkeit klar kommuniziert | Hoch | "Jederzeit kuendbar" + Link zur Abo-Verwaltung im Play Store |
| 16.3 | Keine Dark Patterns bei Abo-Buttons | Hoch | Schliessen-Button muss sichtbar sein, kein versehentliches Abo moeglich |
| 16.4 | In-App Review API korrekt verwendet | Mittel | Max 3x pro Jahr pro Nutzer, nur nach positivem Moment, nicht erzwingen |
| 16.5 | Billing Library auf aktuellem Stand | Mittel | Google prueft dies bei Reviews — veraltete Billing-Version kann zu Ablehnung fuehren |
| 16.6 | Trial-Bedingungen transparent | Hoch | Was passiert nach Trial-Ende MUSS klar sein (automatische Verlaengerung, Preis) |
| 16.7 | Datenschutzerklaerung verlinkt und aktuell | Mittel | Pflicht fuer Apps mit Abo/Zahlung, Link in Settings und Store-Listing |
| 16.8 | Data Safety Section im Store korrekt | Mittel | Muss exakt widerspiegeln welche Daten gesammelt/geteilt werden |

## 17. Lokalisierung & Mehrsprachigkeit

| # | Pruefpunkt | Gewicht | Wie "gut" aussieht |
|---|-----------|---------|-------------------|
| 17.1 | App ist mindestens auf Englisch verfuegbar | Hoch | Nur-Deutsch limitiert den Markt auf DACH (~100M). Englisch oeffnet 2+ Milliarden potenzielle Nutzer |
| 17.2 | Store-Listing in mehreren Sprachen | Hoch | Titel, Beschreibung, Screenshots in den Top-5-Zielsprachen — erhoecht Downloads in diesen Maerkten massiv |
| 17.3 | App-Texte sind externalisiert (strings.xml) | Mittel | Alle Texte in strings.xml, keine hardcoded Strings in Composables — Voraussetzung fuer Uebersetzung |
| 17.4 | Mindestens 3-5 Sprachen unterstuetzt | Mittel | Deutsch + Englisch + Spanisch + Franzoesisch + Portugiesisch decken einen Grossteil der zahlungsbereiten Maerkte ab |
| 17.5 | RTL-Unterstuetzung (Arabisch, Hebraeisch) erwaegen | Niedrig | Grosse Maerkte, aber technisch aufwaendig — nur bei klarem Marktpotenzial |

---

## Meta-Informationen

- **Erstellt:** 2026-04-06
- **Letzte Aktualisierung:** 2026-04-06
- **Version:** 3 (v3: Beispiel-Prompt, Aufwand/Vorher-Nachher, Intro-Offers, Lokalisierung, Saisonale Promos, Korrekturen)
- **Gesamtanzahl Pruefpunkte:** 117
- **Kategorien:** 17 (davon 7 in v2 hinzugefuegt, 1 in v3)
- **Quellen der initialen Version:** Eigenes Wissen basierend auf Android-Monetarisierung Best Practices,
  RevenueCat State of Subscriptions Reports, Adapty Paywall Guides, Google Play Billing Docs,
  Material Design 3 Guidelines. Wird bei erster Researcher-Aktualisierung mit aktuellen
  Web-Quellen angereichert und verifiziert.
- **Hinweis:** Die Rotations-Fragen-IDs werden im Frontmatter (`last_rotation_ids`) getrackt
  damit bei jedem Update andere Fragen gestellt werden.
