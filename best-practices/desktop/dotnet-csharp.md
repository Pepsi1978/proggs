# C#/.NET — Best Practices

**Stand:** 2026-06-02 (voller Best-Practices-Recherchelauf, 7 Researcher parallel, offizielle Microsoft-Quellen zuerst).
**Versions-Anker (live ermittelt):** .NET SDK **10.0.204**, TargetFramework **net8.0-windows** (.NET 8, C# 12),
WPF auf .NET, WinUI 3 / Windows App SDK **1.8**, Windows 10/11. Kontext: self-contained single-file Desktop-Overlays
(TerminalVoiceOverlay, ClaudeVoiceOverlay, PromptBoard) mit Audio-Aufnahme + HTTP-Calls (Groq Whisper, Google Gemini).

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/desktop/dotnet-csharp.md`](../../bugs/desktop/dotnet-csharp.md)):
> der Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von vornherein
> richtig macht*. Quellen-Rangordnung: offizielle Microsoft-Learn-Quelle = Grundwahrheit (`offiziell`),
> Community/anerkannte Experten (Cleary, Toub) = `extern` (sekundaer). Jeder Eintrag traegt Quelle + Flag.
> Punkte, die erst .NET 9/C# 13 bringen, sind als „Ausblick" markiert (Projekte laufen auf .NET 8).

---

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

---

## 🔗 Bezug zum Bug-Almanach ([`bugs/desktop/dotnet-csharp.md`](../../bugs/desktop/dotnet-csharp.md))

Jeder Best-Practice-Block ist die **Praevention** zu Almanach-Abschnitten:

| Best-Practice (hier) | Verhindert Bug(s) in `bugs/desktop/dotnet-csharp.md` |
|----------------------|----------------------------------------------|
| 1 Async/Concurrency | §7.1–7.13 (Deadlock, async void, ConfigureAwait, Cancellation, ValueTask, SemaphoreSlim, Channels, lock, AsyncLocal) |
| 2 HTTP/Resilience | §8.1/8.2 (Socket-Exhaustion, stale DNS) |
| 3 System.Text.Json | §10.1–10.4 (camelCase, Source-Gen, Polymorphie) |
| 4 .NET 8 / C# 12 | §11 (NRT, record-equality, struct-Mutation, Enum, Span, decimal, String, DateTime) |
| 5 WPF | §2 (Overlay/Topmost/Transparenz/Airspace), §3 (DPI), §9 (Leaks, Dispatcher) |
| 6 WinUI 3 | §12 (Single-File, Activate, DispatcherQueue, ContentDialog, Mica, kein Transparenz/Tray) |
| 7 Deployment/Performance | §1 (Single-File/Trimming), §13 (Breaking Changes), §14 (Manifest) |
| 8 Testing/Tooling | §9.6 (SafeHandle/Dispose), §11.11 (IEnumerable) — als Analyzer-Erzwingung |
| 9 DI/Config/Logging | §11.x (Lifetimes), Secrets (SK-Ordner-Regel) |

(Reziprok: die Gegenrichtung steht im Almanach unter „🔗 Bezug zu den Best Practices".)

---

## 1. Async / Concurrency

- **Async all the way:** nie `.Result`/`.Wait()`/`GetAwaiter().GetResult()` auf UI-/Sync-Kontext (Deadlock + Thread-Pool-Starvation). `offiziell`
- **`ConfigureAwait(false)` in Lib-/Service-Code** (Whisper/Gemini-Clients, Audio-Encoding) — konsequent bei JEDEM `await`, nicht nur am ersten. In UI-Code (ViewModels, die danach UI anfassen) **weglassen**. Analyzer **CA2007** in Lib-Projekten erzwingt das (in Desktop-UI-Projekten ist CA2007 Rauschen → dort aus). `offiziell`
- **`ConfigureAwaitOptions.SuppressThrowing`** (.NET 8) für fire-and-forget-Cleanup, dessen Exception bewusst ignoriert wird — allokationsfrei statt try/catch. `offiziell`
- **`CancellationToken` als LETZTER Parameter** (CA1068), an alle awaitbaren Aufrufe durchreichen. Timeout + Benutzer-Abbruch per `CancellationTokenSource.CreateLinkedTokenSource` + `CancelAfter` zu EINEM Token verschmelzen. `offiziell`
- **`System.Threading.Channels` für Producer/Consumer** (Audio-Frames → Encoding/Upload): `Channel.CreateBounded<T>(new BoundedChannelOptions(cap){ FullMode = Wait })` (Backpressure!), nicht unbounded; Producer ruft `Writer.Complete()` im `finally`. `offiziell`
- **`IAsyncEnumerable<T>` + `await foreach`** für Streaming (Audio-Chunks, Gemini-Tokens) statt Listen zu materialisieren; async-Iteratoren mit `[EnumeratorCancellation]` annotieren, sonst wird der Token still ignoriert. `offiziell`
- **`ValueTask` nur für Hot-Path-APIs** (z.B. Channel-Reads), genau EINMAL awaiten, nie cachen/zweimal awaiten. Öffentliche APIs: `Task`. `offiziell`
- **`SemaphoreSlim(1,1)` als async-Mutex** (`await WaitAsync(ct)` + `Release()` im `finally`) — `lock` geht nicht über `await`. Ausblick C# 13/.NET 9: `System.Threading.Lock` für rein synchrone Abschnitte. `offiziell`
- **`TimeProvider` injizieren** statt `DateTime.Now`/`Stopwatch`/`Timer`: `GetUtcNow()`, `GetElapsedTime()` (Audio-Dauer), `CreateTimer()`. `offiziell`
- **`BackgroundService`/`IHostedService`** für dauerhafte Loops (Audio-Pump, Upload-Worker); `ExecuteAsync(ct)` respektiert Token, keine blockierende Init. `offiziell`
- **`IAsyncDisposable` + `await using`** für Streams/Audio-Devices; `DisposeAsync` idempotent; nicht stapeln (sonst übersprungene Disposes bei Fehlern). `offiziell`
- **`Parallel.ForEachAsync`** (.NET 6+) für parallele I/O-Batches mit `MaxDegreeOfParallelism` (schont API-Rate-Limits) — nicht `Parallel.ForEach` mit async-Lambda (= async void). `offiziell`
- Quellen: https://devblogs.microsoft.com/dotnet/configureawait-faq/ · https://learn.microsoft.com/en-us/dotnet/core/extensions/channels · https://learn.microsoft.com/en-us/dotnet/standard/datetime/timeprovider-overview

## 2. HTTP / Networking / Resilience

- **Nie `new HttpClient()` pro Request.** Ein statischer Client mit `new SocketsHttpHandler { PooledConnectionLifetime = TimeSpan.FromMinutes(2-15) }` (gegen Socket-Exhaustion UND stale DNS) ODER `IHttpClientFactory`. `offiziell`
- **Typed Clients pro API** (`AddHttpClient<GroqClient>(c => c.BaseAddress = …)`) — getrennte Config/Resilience für Groq + Gemini. `offiziell`
- **`Microsoft.Extensions.Http.Resilience` → `AddStandardResilienceHandler()`** (RateLimiter → TotalTimeout 30s → Retry 3× exponential+Jitter → CircuitBreaker → AttemptTimeout 10s). Nur EINEN Resilience-Handler stacken. `offiziell`
- **Retries bei POST/PUT/PATCH abschalten:** `options.Retry.DisableForUnsafeHttpMethods()` — verhindert doppelte Whisper-Transkriptionen/Gemini-Calls. `offiziell`
- **Backoff + Jitter** (`DelayBackoffType.Exponential, UseJitter = true`) gegen Thundering-Herd bei 429. `offiziell`
- **Ohne DI** (reine Desktop-App): `ResiliencePipelineBuilder<HttpResponseMessage>` in `ResilienceHandler` wrappen mit `InnerHandler = socketHandler`. `offiziell`
- **Streaming:** `SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct)` + `GetFromJsonAsAsyncEnumerable<T>` / `DeserializeAsyncEnumerable<T>` (kein Voll-Puffern). SSE (Gemini-Token-Stream): `System.Net.ServerSentEvents.SseParser` — **Ausblick .NET 9**, in .NET 8 manuell/Lib. `offiziell`
- **API-Keys nie hardcoden** — via SK-Ordner/Env/user-secrets laden (siehe `~/.claude/rules/secrets-in-sk-folder.md`). `offiziell`
- Quellen: https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines · https://learn.microsoft.com/en-us/dotnet/core/resilience/http-resilience

## 3. System.Text.Json

- **`JsonSerializerOptions` als statische Singleton-Instanz** (thread-safe, nach erstem Gebrauch immutable); `MakeReadOnly()` zum bewussten Einfrieren. Analyzer **CA1869** warnt bei Single-Use. `offiziell`
- **`new JsonSerializerOptions(JsonSerializerDefaults.Web)`** für externe APIs: camelCase, `PropertyNameCaseInsensitive=true`, `NumberHandling=AllowReadingFromString` — passt zu Groq/Gemini. `offiziell`
- **Source-Gen-Context** (`[JsonSerializable(typeof(GeminiResponse))] partial class ApiJsonContext : JsonSerializerContext`) für Startup-Performance + Trim/AOT; mehrere Contexts via `JsonTypeInfoResolver.Combine(...)`. `offiziell`
- **`JsonStringEnumConverter<TEnum>`** (generisch, AOT-fest) statt der nicht-generischen Variante. `offiziell`
- Quellen: https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/configure-options · .../source-generation

## 4. .NET 8 / C# 12 Sprach- & Runtime-Features

- **Primary Constructors** für DI-Klassen (`class Service(IRepo repo)`) — weniger Boilerplate. Bei Nicht-Records: Parameter, der als State leben soll, explizit `private readonly`-Feld zuweisen (Capture-Falle, siehe Almanach). `offiziell`
- **Collection Expressions** `[..]` (Arrays/Spans/List, Spread). **`record`/`record struct`** für unveränderliche DTOs (aber Collection-Properties → Reference-Equality, manuell überschreiben). **`required`** members. **List Patterns** + exhaustive `switch`. `offiziell`
- **`FrozenDictionary`/`FrozenSet`** (`static readonly`, einmal gebaut, oft gelesen — z.B. Config-Maps); hoher Build-Cost → nicht für veränderliche Collections. `offiziell`
- **Keyed DI** (`AddKeyedSingleton<IFoo>("key", …)` + `[FromKeyedServices("key")]`) für mehrere Implementierungen eines Interface. `offiziell`
- **`GeneratedRegex` Source-Gen** (`[GeneratedRegex(...)] static partial Regex X();`) statt `new Regex()` — schneller + AOT-fest (Analyzer SYSLIB1045). **`LibraryImport`** statt `DllImport` (`StringMarshalling` statt `CharSet`, `static partial`; SYSLIB1054). **`UnsafeAccessor`** statt Reflection für private Member. `offiziell`
- **Guard-Helfer:** `ArgumentNullException.ThrowIfNull(x)`, `ArgumentException.ThrowIfNullOrWhiteSpace`, `ArgumentOutOfRangeException.ThrowIfNegative/…`, `ObjectDisposedException.ThrowIf` (CA1510/1512). `offiziell`
- **NRT als Default** (`<Nullable>enable</Nullable>`), Null-Analyse-Attribute (`[NotNullWhen]`, `[MemberNotNull]`). `decimal` für Geld, `CultureInfo.InvariantCulture` für Maschinendaten, `StringComparison.Ordinal(IgnoreCase)` explizit. `offiziell`
- **NICHT verwenden:** Interceptors (experimentell/Preview). `offiziell`
- Quellen: https://learn.microsoft.com/en-us/dotnet/csharp/whats-new/csharp-12 · .../core/whats-new/dotnet-8

## 5. WPF (MVVM, DI, Performance, Overlay)

- **MVVM mit `CommunityToolkit.Mvvm`:** `[ObservableProperty]` auf Felder, `[RelayCommand]` auf Methoden (Klasse `partial`, erbt `ObservableObject`). Async-Methode → `AsyncRelayCommand` (CancellationToken durchgereicht, `IncludeCancelCommand`, `AllowConcurrentExecutions=false` gegen Doppelklick). `CanExecute` via `[NotifyCanExecuteChangedFor]` (sonst aktualisiert sich der Button-State nicht). `offiziell`
- **DI via Generic Host:** `Host.CreateApplicationBuilder()` in `App.OnStartup` (StartupUri raus), Services + Windows als Singleton, `GetRequiredService<MainWindow>().Show()`, bei Exit `StopAsync()`. Constructor-Injection in Windows/ViewModels. `offiziell`
- **Performance:** UI-Virtualisierung (`VirtualizingStackPanel.IsVirtualizing`, `VirtualizationMode=Recycling`), `ScrollViewer.IsDeferredScrollingEnabled`, `CacheMode=BitmapCache` für komplexe statische Visuals, Templates schlank halten. **Virtualisierung wird still abgeschaltet** durch Container-direkt-adden, gemischte Container-Typen, `CanContentScroll=false`. `offiziell`
- **Dispatcher:** `InvokeAsync` (awaitable) statt `BeginInvoke`; `DispatcherPriority.Background` für unkritisches; teure Arbeit in echten Background-Thread. `offiziell`
- **`Freezable.Freeze()`** für Brushes/Geometries (kein Change-Monitoring, thread-übergreifend). `offiziell`
- **Memory-Leaks vermeiden:** an `DependencyProperty`/`INotifyPropertyChanged` binden (sonst `PropertyDescriptor`-Leak), `WeakEventManager` wenn Source länger lebt als Listener. `offiziell`
- **Overlay-Grundkonfiguration:** `Topmost`, `WindowStyle=None`, `AllowsTransparency`, `ShowInTaskbar=False`, `Background=Transparent`; Click-through + Topmost-Fix per `SetWindowLong(GWL_EXSTYLE, | WS_EX_TRANSPARENT|WS_EX_TOOLWINDOW)` und `SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)`. `extern`(MS Q&A)
- Quellen: https://learn.microsoft.com/en-us/dotnet/communitytoolkit/mvvm/generators/overview · .../desktop/wpf/app-development/how-to-use-host-builder · .../desktop/wpf/advanced/optimizing-performance-controls

## 6. WinUI 3 / Windows App SDK (Stand 1.8)

- **Wann WinUI vs WPF:** Neue native Windows-Apps → WinUI 3; **Overlays/Transparenz/Click-through → WPF** (WinUI kann das nicht). WASDK 1.8 = Current (2025-09-09), 1.6/1.7 supported; Min-OS Win10 1809. `offiziell`
- **Compiled Bindings `{x:Bind}`** statt `{Binding}` (typsicher, schneller). **MVVM** wie WPF mit CommunityToolkit. **Threading:** `DispatcherQueue.TryEnqueue` (kein WPF-Dispatcher), DispatcherQueue auf UI-Thread cachen. `offiziell`
- **`AppWindow`** für Fenster-Anpassung (Titlebar `ExtendsContentIntoTitleBar` + `SetTitleBar`, Presenter); HWND via `WindowNative.GetWindowHandle` kapseln. **Backdrop deklarativ:** `<Window.SystemBackdrop><MicaBackdrop/></>` (Mica = Fenster-Basis; Acrylic NUR für transiente Flyouts). **Theming:** `ThemeResource` statt `DynamicResource`. `offiziell`
- **Packaging:** Store → packaged MSIX framework-dependent; Direct-Download → unpackaged + self-contained; `PublishSingleFile` NUR für unpackaged+self-contained (ab 1.5). `offiziell`
- **Fehlt in WinUI 3:** kein First-Party `DataGrid`, kein `AdornerLayer`, **VS XAML-Designer nicht unterstützt** (Hot Reload nutzen). `WebBrowser` → `WebView2`. `offiziell`
- Quellen: https://learn.microsoft.com/en-us/windows/apps/windows-app-sdk/migrate-to-windows-app-sdk/wpf-patterns-winui3 · .../develop/ui/system-backdrops · .../package-and-deploy/

## 7. Deployment & Performance

- **Single-File:** `<PublishSingleFile>true</>` + `<RuntimeIdentifier>win-x64</>` + `<IncludeNativeLibrariesForSelfExtract>true</>` + `<DebugType>embedded</>`. `<EnableCompressionInSingleFile>` nur nach Messung (kleiner, aber langsamerer Cold-Start). `Environment.ProcessPath` statt `Assembly.Location`. `offiziell`
- **`PublishReadyToRun=true`** für Cold-Start (pre-JIT) — **.NET 8: impliziert NICHT mehr SelfContained**, beides explizit. Tiered Compilation + Dynamic PGO (Default in .NET 8) anlassen. `offiziell`
- **KEIN `PublishAot`, KEIN `PublishTrimmed` für WPF/WinUI/WinForms** (Reflection auf XAML). AOT nur Konsole/ASP.NET. `offiziell`
- **GC:** Workstation-GC (Default) für Desktop — `<ServerGarbageCollection>false</>`; `<ConcurrentGarbageCollection>true</>` (kurze Pausen); bei knappem RAM `System.GC.ConserveMemory` 5–7. `offiziell`
- **`app.manifest`:** `PerMonitorV2`, `longPathAware`, `supportedOS` Win10/11, `requestedExecutionLevel asInvoker`. **Code-Signing** (EV-Cert umgeht SmartScreen) NACH dem Bundling. `offiziell`
- Quellen: https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview · .../native-aot/ · .../runtime-config/garbage-collector

## 8. Testing / Code-Qualität / Tooling

- **Testing:** xUnit/NUnit weiter via `dotnet test` (VSTest); `Microsoft.Testing.Platform` nativ erst .NET 10 SDK — VSTest- und MTP-Projekte nicht mischen. **Mocking: NSubstitute** statt Moq (Moq 4.20.0 hatte SponsorLink-Datenleck). **Zeit-Tests:** `FakeTimeProvider` + `Advance()`. `offiziell`/`extern`
- **Build-Enforcement:** `<AnalysisLevel>latest-recommended</>`, `<EnforceCodeStyleInBuild>true</>`, `<TreatWarningsAsErrors>true</>` (+ `<OptimizeImplicitlyTriggeredBuild>`). Wichtige CA-Regeln: CA2007 (ConfigureAwait, in UI-App aus), CA1816 (Dispose), CA1851 (mehrfache Enumeration), CA2016 (CancellationToken), CA1848 (LoggerMessage), CA1510/1512 (Guards), CA1869 (JsonOptions). `offiziell`
- **Tooling:** Central Package Management (`Directory.Packages.props` + `<ManagePackageVersionsCentrally>`), `Directory.Build.props/.targets` für Repo-Defaults, `dotnet format --verify-no-changes` in CI, `NuGetAudit` mit `NuGetAuditMode=all`. `offiziell`
- Quellen: https://learn.microsoft.com/en-us/dotnet/core/testing/microsoft-testing-platform-vs-vstest · .../fundamentals/code-analysis/overview · .../nuget/consume-packages/central-package-management

## 9. DI / Konfiguration / Logging

- **Generic Host (`Microsoft.Extensions.Hosting`) auch im Desktop** — einheitliche DI/Config/Logging. Lifetimes bewusst (Transient/Scoped/Singleton); **Scoped-Service nie aus dem Root-Provider** auflösen (Captive-Dependency) → `IServiceScopeFactory.CreateScope()`. `offiziell`
- **Options-Pattern:** `IOptions<T>` (statisch), `IOptionsSnapshot<T>` (scoped, reload), `IOptionsMonitor<T>` (singleton, Live-Reload). Validierung `ValidateDataAnnotations()` + `ValidateOnStart()`. `offiziell`
- **Logging:** `[LoggerMessage]` Source-Gen (kein Boxing, Template einmal geparst) statt `ILogger`-Extension-Calls (CA1848). `offiziell`
- **Secrets:** `dotnet user-secrets` / SK-Ordner — nie in `appsettings.json`/Repo. `offiziell`
- Quellen: https://learn.microsoft.com/en-us/dotnet/core/extensions/generic-host · .../options · .../logging/source-generation

---

## Wartung
- Diese Datei + der Bug-Almanach sind gekoppelt: neue Praevention hier → ggf. Gegenstueck-Bug in
  [`bugs/desktop/dotnet-csharp.md`](../../bugs/desktop/dotnet-csharp.md) pflegen, Bezugs-Tabellen synchron halten
  (`python3 bugs/check-coupling.py`).
- Bei .NET-Versionssprung (.NET 9/10-Migration): Re-Check (neue Breaking Changes, neue Defaults, neue Source-Gen-Analyzer).
