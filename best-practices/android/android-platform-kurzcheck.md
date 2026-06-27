# Android-Framework / Platform-SDK Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | App-Struktur planen | 3 Layer (UI→Domain optional→Data), UDF, Single-Module ok | §1 |
| 2 | UI-State im ViewModel | Immutable `UiState`, `stateIn(WhileSubscribed(5000))`, kein Context | §1.3 |
| 3 | State in Compose lesen | Immer `collectAsStateWithLifecycle()`, nie `collectAsState()` | §2.3 |
| 4 | Flow im UI sammeln | `repeatOnLifecycle(STARTED)`, nie `launchWhenX`; Fragment: `viewLifecycleOwner` | §2.1 |
| 5 | `registerForActivityResult` / `registerX` | Frueh + symmetrisch registrieren; jedes hat ein `unregisterX` | §2.6 |
| 6 | Process Death absichern | `SavedStateHandle` + Persistenz; kein `Context`/`View` im ViewModel | §2.5 |
| 7 | `@Database`-`version` erhoeht | Echter Migrationspfad, `exportSchema=true`, `MigrationTestHelper` | §3.1, §3.3, §3.4 |
| 8 | DB-Backup / Drive-Upload | WAL-Checkpoint vorher; nie `fallbackToDestructiveMigration` in Prod | §3.5, §3.2 |
| 9 | DB-Instanz erzeugen / DAO | Hilt-`@Singleton`; DAO `suspend`/`Flow` (off-main) | §3.7, §3.8 |
| 10 | Garantierte Background-Arbeit | WorkManager (ueberlebt Prozess/Reboot); `getStopReason()` loggen | §4.1, §4.8 |
| 11 | `@HiltWorker` nutzen | `Configuration.Provider` + Default-Initializer im Manifest entfernen | §4.9 |
| 12 | Foreground-Service bauen | Drei Dinge gleichzeitig + `startForeground()` binnen ~5 s | §5.1, §5.2 |
| 13 | Notifications senden | Channel (ab 8) + `POST_NOTIFICATIONS` (ab 13) + kein Trampolin | §5.7, §5.8, §5.9 |
| 14 | Permission anfragen | `RequestPermission`-Flow, einzeln pruefen, permanently-denied erkennen | §6.1 |
| 15 | targetSdk-36-Migration | Edge-to-Edge, Predictive Back, adaptive Layouts, 16-KB, FileProvider | §7 |
