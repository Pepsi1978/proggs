# C#/.NET Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Async-Code schreiben | `await` statt `.Result`; in Lib `ConfigureAwait(false)`, `async Task` statt `void` | §1 |
| 2 | HTTP-Client anlegen | Ein langlebiger Client `PooledConnectionLifetime`; Typed Clients pro API | §2 |
| 3 | API-Calls absichern | `AddStandardResilienceHandler()`; bei POST `DisableForUnsafeHttpMethods()` | §2 |
| 4 | JSON (de)serialisieren | `JsonSerializerOptions` statisch, `JsonSerializerDefaults.Web`, `MakeReadOnly()` | §3 |
| 5 | Zeit/Timer im Code | `TimeProvider` injizieren statt `DateTime.Now`/`Timer` | §1 |
| 6 | Single-File veroeffentlichen | `Environment.ProcessPath`; WPF kein Trim/AOT; `--self-contained true` | §7 |
| 7 | Overlay/Transparenz bauen | Overlay = WPF, nie WinUI 3 (kein echtes Click-through) | §6 |
| 8 | App-Struktur/DI aufsetzen | Generic Host fuer DI/Config/Logging; Constructor-Injection | §9 |
| 9 | Projekt-Defaults setzen | `AnalysisLevel latest-recommended` + `TreatWarningsAsErrors` | §8 |
| 10 | Strings/Zahlen/Locks | `StringComparison.Ordinal`, `InvariantCulture`, `decimal`, `_lock`-Objekt | §4 |
| 11 | WPF MVVM bauen | `CommunityToolkit.Mvvm`: `[ObservableProperty]`, `[RelayCommand]` | §5 |
| 12 | Tests/Mocking | NSubstitute statt Moq; `FakeTimeProvider` fuer Zeit-Tests | §8 |
