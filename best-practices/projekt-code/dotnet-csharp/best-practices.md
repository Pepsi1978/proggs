# C#/.NET — Best Practices

**Stand:** 2026-06-02 (abgeleitet aus dem Bug-Almanach-Recherchelauf, 5 Researcher, offizielle Quellen zuerst).
**Versions-Anker (live ermittelt):** .NET SDK **10.0.204**, TargetFramework **net8.0-windows** (.NET 8, C# 12),
WPF auf .NET, WinUI 3 / Windows App SDK, Windows 10/11. Kontext: self-contained single-file Desktop-Overlays.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/dotnet-csharp.md`](../../../bugs/dotnet-csharp.md)):
> der Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von vornherein
> richtig macht, damit der Bug nie entsteht*. Quellen-Rangordnung: offizielle Microsoft-Learn-Quelle =
> Grundwahrheit (`offiziell`), Community/Blogs = `extern` (sekundaer). Jeder Eintrag traegt Quelle +
> `offiziell`/`extern`.

---

## ⚡ TL;DR — die Defaults, die man einmal richtig setzt

1. **Async durchgaengig:** `await` statt `.Result`/`.Wait()`; in Library-Code `ConfigureAwait(false)`; `async Task` statt `async void` (ausser Event-Handler mit try/catch). `offiziell`
2. **Ein statischer `HttpClient`** mit `SocketsHttpHandler { PooledConnectionLifetime = TimeSpan.FromMinutes(2) }` — nie `new HttpClient()` pro Call. `offiziell`
3. **Single-File:** Pfade ueber `Environment.ProcessPath`, nie `Assembly.Location`. WPF: **kein** `PublishTrimmed`. Publish immer mit explizitem `--self-contained true`. `offiziell`
4. **Overlay = WPF, nicht WinUI 3** (WinUI kann kein echtes Transparenz-/Click-through-Overlay, kein Single-File). `offiziell`
5. **Clipboard immer mit Retry** (`SetDataObject(data, true, 10, 100)`) — kann jederzeit gesperrt sein. `offiziell`
6. **`app.manifest` mit PerMonitorV2** + `longPathAware` + `asInvoker`; Fenster-Positionierung ueber physische Pixel (`SetWindowPos`), nicht `Window.Left/Top`. `offiziell`
7. **Disposables konsequent:** `using`/`Dispose` fuer Streams/Audio/Timer; Event-Handler immer `-=`; `SafeHandle` statt eigenem Finalizer; `GC.SuppressFinalize(this)` im Dispose. `offiziell`
8. **JSON-Options als statische Instanz** mit `PropertyNameCaseInsensitive=true` (API-DTOs sind camelCase). `offiziell`
9. **`StringComparison.Ordinal(IgnoreCase)` explizit**; `CultureInfo.InvariantCulture` fuer Maschinendaten; `decimal` fuer Geld; `DateTimeOffset` fuer Zeitpunkte. `offiziell`
10. **`private readonly object _lock = new()`** — nie `lock(this)`/`lock(typeof)`/`lock("string")`. `offiziell`

---

## 🔗 Bezug zum Bug-Almanach ([`bugs/dotnet-csharp.md`](../../../bugs/dotnet-csharp.md))

Jeder Best-Practice-Block hier ist die **Praevention** zu einem Abschnitt im Almanach:

| Best-Practice (hier) | Verhindert Bug(s) in `bugs/dotnet-csharp.md` |
|----------------------|----------------------------------------------|
| 1 Async-Disziplin | §7.1 Deadlock, §7.2 async void, §7.3 ConfigureAwait, §7.4 fire-and-forget, §7.5 Cancellation |
| 2 HttpClient-Singleton | §8.1 Socket-Exhaustion, §8.2 stale DNS |
| 3 Single-File/Publish | §1.1 Assembly.Location, §1.2/1.3 native libs, §1.5 Trimming+WPF, §13.1 self-contained |
| 4 Overlay=WPF | §12.1 WinUI Single-File, §12.7 kein Transparenz/Tray |
| 5 Clipboard-Retry | §4.1 CLIPBRD_E_CANT_OPEN, §4.2 Restore-Timing |
| 6 Manifest + DPI + Positionierung | §3.1–3.5 DPI/Multi-Monitor, §2.1/2.2 Topmost, §14.3/14.6 Manifest |
| 7 Disposables/Leaks | §9.1 Event-Leak, §9.2 Timer-Leak, §9.4 GDI, §9.5 Audio, §9.6 SafeHandle |
| 8 JSON-Options | §10.1 camelCase, §10.2 Source-Gen, §10.3 Polymorphie |
| 9 Kultur/Typen | §11.5 decimal, §11.6 String-Vergleich, §11.12 DateTime/Culture |
| 10 Lock-Objekt | §7.10 lock-Anti-Pattern, §7.8 SemaphoreSlim |
| 11 Collections/Sprache | §11.3 struct-Mutation, §11.4 record-equality, §11.7 Enum, §11.10 mutable key, §11.11 IEnumerable |
| 12 WinUI-Threading | §12.3 Activate, §12.4 DispatcherQueue, §12.5 ContentDialog |

(Reziprok gepflegt — die Gegenrichtung steht im Almanach unter „🔗 Bezug zu den Best Practices".)

---

## 1. Async / Threading — von vornherein richtig
- **"async all the way":** Event-Handler `async Task` (oder `async void` NUR fuer Event-Handler, dann Body komplett in `try/catch`). Nie `.Result`/`.Wait()`/`.GetAwaiter().GetResult()` auf dem UI-Thread. `offiziell`
- **`ConfigureAwait(false)`** in jedem Service-/Library-Aufruf (HTTP zu Whisper/Gemini, Audio-Pipeline); in direktem UI-Code weglassen. `offiziell`
- **`CancellationToken`** durch die ganze Kette reichen + INNERHALB von `Task.Run`-Delegates `ThrowIfCancellationRequested()`. `offiziell`
- **async-Lock:** `SemaphoreSlim(1,1)` + `try/finally { Release() }`; nie `lock` um `await`. Fuer Producer-Consumer `Channel.CreateBounded` (Backpressure) + `Writer.Complete()` im finally. `offiziell`
- **`AsyncLocal<T>`** statt `ThreadLocal`/`[ThreadStatic]` ueber `await`-Grenzen. `offiziell`
- Quelle: https://devblogs.microsoft.com/dotnet/configureawait-faq/ · https://blog.stephencleary.com/2012/07/dont-block-on-async-code.html

## 2. HTTP / Netzwerk
- Ein langlebiger `HttpClient` (statisch oder `IHttpClientFactory`) mit `SocketsHttpHandler { PooledConnectionLifetime = TimeSpan.FromMinutes(2) }` — loest Socket-Exhaustion UND stale-DNS in einem. `offiziell`
- Quelle: https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines

## 3. Deployment / Single-File / Publish
- Pfade neben der EXE: `Path.GetDirectoryName(Environment.ProcessPath)`; nie `Assembly.Location`. `offiziell`
- WPF: `PublishTrimmed`/ReadyToRun **aus**. Native Libs bei WPF lieber lose neben die EXE statt `IncludeNativeLibrariesForSelfExtract`. `offiziell`
- Publish-Befehl immer mit explizitem `-r win-x64 --self-contained true` (RID allein impliziert es seit .NET 8 nicht mehr). `offiziell`
- Quelle: https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview

## 4. UI-Framework-Wahl & Overlays
- **Overlays in WPF** bauen: `AllowsTransparency` + Layered Window kann Per-Pixel-Transparenz + Click-through (toggelbar per `WS_EX_TRANSPARENT`). WinUI 3 kann das nicht und ist nicht single-file-faehig. `offiziell`
- Topmost defensiv: nach `SourceInitialized` per `SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)` und periodisch re-applizieren; transparente Flaeche klein halten (`CacheMode=BitmapCache`). `extern`
- Quelle: https://learn.microsoft.com/en-us/dotnet/api/system.windows.window.allowstransparency

## 5. Win32-Integration (Manifest, P/Invoke, Eingabe)
- `app.manifest`: `PerMonitorV2`, `longPathAware=true`, `requestedExecutionLevel asInvoker`, `supportedOS` Win10/11. WinForms-Teile zusaetzlich `Application.SetHighDpiMode(PerMonitorV2)`. `offiziell`
- P/Invoke: `[LibraryImport]` (Source-Gen) bevorzugen, sonst `[DllImport(CharSet=Unicode, SetLastError=true)]`; Fehler via `Marshal.GetLastPInvokeError()`. `offiziell`
- Tastatur-Simulation: `SendInput` (nicht `keybd_event`); fuer Text `KEYEVENTF_UNICODE` (layout-/IME-fest) oder Clipboard+Strg+V; vor dem Senden Ziel-Fenster `SetForegroundWindow`. `extern`
- Global-State-Resilienz: Tray-Icon + Taskbar-Progress nach `WM_TASKBARCREATED` neu setzen; Single-Instance-Mutex `static` + `Global\<GUID>`. `offiziell`
- Quelle: https://learn.microsoft.com/en-us/dotnet/standard/native-interop/best-practices

## 6. Ressourcen / Disposables / Leaks
- `using`/`await using` fuer Streams, Audio (`WasapiCapture`), `CancellationTokenSource`, GDI-Objekte. Event-Handler im `Dispose`/`Unloaded` immer `-=`; `DispatcherTimer.Stop()`. `offiziell`
- Unmanaged Handles in `SafeHandle`; Standard-Dispose-Pattern mit `GC.SuppressFinalize(this)`. Audio-Device-Wechsel ueber `RegisterEndpointNotificationCallback` abfangen. `offiziell`
- Quelle: https://learn.microsoft.com/en-us/dotnet/fundamentals/code-analysis/quality-rules/ca1816

## 7. Daten / Serialisierung / Sprache
- `JsonSerializerOptions` als statische Instanz, `PropertyNameCaseInsensitive=true` (+ CamelCase-Policy beim Schreiben); Polymorphie via `[JsonDerivedType]`. `offiziell`
- `decimal` fuer Geld, `DateTimeOffset` fuer Zeitpunkte, `CultureInfo.InvariantCulture` + `ParseExact` fuer Maschinendaten, `StringComparison.Ordinal(IgnoreCase)` explizit. `offiziell`
- Nullable Reference Types ernst nehmen (kein `!`-Spam); `switch`-Expressions exhaustiv (CS8509 als Error); `Enum.IsDefined` nach Parse; unveraenderliche Dictionary-Keys; `IEnumerable` frueh materialisieren. `offiziell`
- Quelle: https://learn.microsoft.com/en-us/dotnet/standard/base-types/best-practices-strings

---

## Wartung
- Diese Datei + der Bug-Almanach sind gekoppelt: neue Praevention hier → ggf. Gegenstueck-Bug in
  [`bugs/dotnet-csharp.md`](../../../bugs/dotnet-csharp.md) pflegen und die Bezugs-Tabellen synchron halten.
- Bei .NET-Versionssprung (z.B. .NET 9/10-Migration der Projekte): kurzer Re-Check (neue Breaking Changes, neue Defaults).
