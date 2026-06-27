# C#/.NET 8 — WPF, WinUI 3, Windows-Desktop Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `.Result`/`.Wait()` auf UI-Thread | Durchgaengig `await`; in Lib `ConfigureAwait(false)` | §7.1 |
| 2 | Single-File: `Assembly.Location` leer | `Environment.ProcessPath` statt `Assembly.Location` nutzen | §1.1 |
| 3 | Content-Dateien fehlen nach Publish | Assets im Publish-Skript explizit neben EXE spiegeln | §1.9 |
| 4 | WPF veroeffentlichen | Kein `PublishTrimmed`/AOT bei WPF verwenden | §1.5 |
| 5 | Transparentes Click-through-Overlay | Overlay in WPF bauen, nicht WinUI 3 | §12.7 |
| 6 | Clipboard-Zugriff | IMMER Retry-Schleife um jeden Clipboard-Aufruf | §4.1 |
| 7 | Overlay liegt nicht obenauf | `SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)` periodisch setzen | §2.1 |
| 8 | Scharfe Anzeige ueber Monitore | `app.manifest PerMonitorV2`; Position per physische Pixel | §3.1 |
| 9 | Viele HTTP-Calls (Whisper/Gemini) | Ein statischer Client mit `PooledConnectionLifetime` | §8.1 |
| 10 | `SetForegroundWindow` wirkt nicht | `AttachThreadInput`-Trick oder Trigger aus eigenem Prozess | §5.5 |
| 11 | Event-Handler feuert beim XAML-Laden | `_ready`-Flag; Handler returnt solange false | §2.10 |
| 12 | `UseWindowsForms` + ImplicitUsings (CS0104) | Globale `Using Remove` fuer Drawing/Forms im csproj | §6.7 |
| 13 | `dotnet publish` self-contained | `--self-contained true` explizit (RID impliziert es nicht) | §13.1 |
| 14 | Memory-Leak bei Events/Timern | Im `Dispose`/`Unloaded` immer `-=` und `Stop()` | §9.1 |
| 15 | `Process.Start(url)` wirft | `new ProcessStartInfo(url){ UseShellExecute = true }` | §13.8 |
| 16 | One-Shot-Hotkey haengt nach mehrfacher Nutzung | KeyUp-Debounce-Flag durch zeitbasierten Cooldown ersetzen | §5.8 |
