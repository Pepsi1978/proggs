# Bekannte Reject-Gründe & Fallen: Play-Store-Release & Policy Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Reject.
> Sektionen: **T** Tracks/Testing · **R** Rollout/versionCode · **D** Data-Safety · **F** Foreground-Service ·
> **A** Accessibility/Permissions · **P** Pre-Launch-Report · **S** Signing/Integrity · **M** Metadaten/ASO/targetSdk.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | „Apply for production" ausgegraut | Neues Personal-Konto: 12 Tester / 14 zusammenhängende Tage Closed Test | T1 |
| 2 | „nur X von 12 Testern" | Einladung ≠ Opt-in; alle am selben Tag opt-in + installieren | T2, T3 |
| 3 | Production-Antrag abgelehnt | Fragebogen konkret (echtes Engagement/Feedback/Änderungen), nicht „App is good" | T5 |
| 4 | „Cannot create new release" | Ausstehenden Rollout auf 100 % bringen oder verwerfen | R1 |
| 5 | Kaputte Version draußen | Kein Rollback — `Halt` + neues höheres versionCode | R2 |
| 6 | versionCode-Upload abgelehnt | Streng monoton, nie wiederverwenden/senken | R4 |
| 7 | Update blockiert (Account-Deletion) | In-App + **Web-URL**-Kontolöschung | D3 |
| 8 | Reject Photo/Video-Permission | `READ_MEDIA_*` raus → Photo Picker; merged Manifest prüfen | D4 |
| 9 | Data-Safety „inaccurate" | Jedes SDK deklarieren; Formular↔Policy↔Verhalten | D1, D5 |
| 10 | Reject Prominent Disclosure | In-App-Dialog VOR Permission, affirmatives Consent | D6 |
| 11 | `dataSync`-FGS abgelehnt | Backup → WorkManager (auto) / UIDT (manuell) | F1 |
| 12 | „dataSync did not stop in timeout" | 6h/24h-Limit; `onTimeout()`→`stopSelf()`; besser UIDT | F2 |
| 13 | FGS-Deklaration abgelehnt | Demo-Video muss user-initiierte Auslösung zeigen | F3 |
| 14 | Removal: AccessibilityService | Kein Accessibility-Tool → TTS-API/Quick-Tile; sonst Non-Tool-Declaration | A1 |
| 15 | Removal: autonome Aktionen | Nur statische regelbasierte Automatisierung (seit Okt 2025) | A2 |
| 16 | Reject QUERY_ALL_PACKAGES | Entfernen → gezieltes `<queries>` | A4 |
| 17 | PLR: Inhalt hinter Login ungetestet | Test-Account hinterlegen (kein Produktiv-Konto) | P1 |
| 18 | Store-Build: Maps/OAuth tot | App-Signing-Key-SHA bei API-Provider eintragen | S2 |
| 19 | Crash-Stacktraces obfuskiert | AAB + AGP 4.1+ → mapping.txt automatisch | S5 |
| 20 | „muss API 35 targeten" | targetSdk 35+ (BestJournal konform); jährlich mitziehen | M1 |
| 21 | „Violation of Metadata policy" | Kein „#1/Best/Free", keine Emojis/Keyword-Listen (alle Sprachen) | M3 |
