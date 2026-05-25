using System;
using System.Collections.Generic;
using System.Data.Common;
using System.IO;
using System.Linq;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using PromptBoard.Data;
using PromptBoard.Data.Repositories;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Tiny DI container that wires up the shared PromptBoard database
/// plus the always-on prefix service. Initialized once at overlay
/// startup and accessed as a process-wide singleton.
///
/// DB path mirrors the standalone PromptBoard.App so both programs
/// read and write the same SQLite file.
/// </summary>
public static class PromptBoardHost
{
    private static IServiceProvider? _provider;
    private static readonly object _gate = new();

    public static IServiceProvider Services =>
        _provider ?? throw new InvalidOperationException("PromptBoardHost is not initialized.");

    public static string DbPath { get; private set; } = string.Empty;

    public static void Initialize()
    {
        lock (_gate)
        {
            if (_provider is not null) return;

            string appDataDir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "PromptBoard");
            Directory.CreateDirectory(appDataDir);
            DbPath = Path.Combine(appDataDir, "promptboard.db");

            var services = new ServiceCollection();

            services.AddLogging();

            services.AddDbContext<PromptBoardDbContext>(
                options => options.UseSqlite($"Data Source={DbPath}"),
                ServiceLifetime.Transient);

            services.AddTransient<ICategoryRepository, CategoryRepository>();
            services.AddTransient<IPromptRepository, PromptRepository>();
            services.AddTransient<IAiImprovementPromptRepository, AiImprovementPromptRepository>();
            services.AddTransient<IAppSettingsRepository, AppSettingsRepository>();

            services.AddSingleton<IPromptChainBuilder, PromptChainBuilder>();
            services.AddSingleton<IPastelColorGenerator, PastelColorGenerator>();
            services.AddSingleton<IAlwaysOnPrefixService, AlwaysOnPrefixService>();

            // Google OAuth secrets live in $HOME/SK/PromptBoard/.env per the
            // secrets-in-sk-folder rule. The store is filesystem-backed and
            // safely cached as a singleton (no DbContext to capture).
            services.AddSingleton<PromptBoardSecretStore>();
            // GoogleDriveBackupService MUST be transient — a singleton would
            // capture the transient AppSettings repository (and its DbContext)
            // for the lifetime of the process, so freshly-saved Google client
            // credentials would never be visible to ConnectAsync(). Tested
            // 2026-04-25 by Frank: settings dialog stored ID+Secret correctly
            // but Connect threw GoogleDriveNotConfiguredException because the
            // singleton-captured DbContext still held the empty original row.
            services.AddTransient<IGoogleDriveBackupService, GoogleDriveBackupService>();

            _provider = services.BuildServiceProvider();

            // Schema bootstrap. Mirrors PromptBoardStore.createSchemaIfNeeded()
            // on macOS (PromptBoardStore.swift:49). Without this the DB file
            // gets created empty by SqliteConnection but no tables exist —
            // every read/write then crashes with "no such table: ...".
            // EnsureCreated is idempotent: it only creates tables that are
            // missing, so it's safe to call on every startup.
            using (var scope = _provider.CreateScope())
            {
                var ctx = scope.ServiceProvider.GetRequiredService<PromptBoardDbContext>();
                ctx.Database.EnsureCreated();
            }

            // Idempotent schema migration for older Windows DBs that
            // pre-date the per-prompt Pre/Post split (#1820 macOS, this
            // commit Windows). SQLite throws on duplicate-column ALTER
            // TABLE, which we swallow because we just want "the columns
            // exist" — exactly mirrors the macOS approach.
            EnsurePrePostColumns();

            // Strg+1..9 prompt hotkeys (this feature). Same idempotent
            // ALTER pattern: the column starts NULL on every existing
            // row so prior installs upgrade silently with all hotkeys
            // unassigned, exactly as if no one had ever set one.
            EnsureHotkeyColumn();

            // Win+Alt+<letter> prompt hotkeys (A-Z). Same idempotent ALTER
            // pattern as HotkeyNumber. Adds the column + index to legacy
            // SQLite files that pre-date this feature; first-run installs
            // already have it from EnsureCreated.
            EnsureHotkeyLetterColumn();

            // Overlay auto-hide toggle. Same idempotent ALTER pattern: existing
            // SQLite files that pre-date the auto-hide feature get the column
            // added with DEFAULT 1 (auto-hide on), so prior installs upgrade
            // silently with the feature enabled. First-run installs already
            // have it from EnsureCreated via the model property.
            EnsureAutoHideColumn();

            // Overlay orientation (vertical/horizontal). Same idempotent ALTER
            // pattern: existing DBs get the column with DEFAULT 'vertical', so
            // prior installs keep the classic vertical pill until the user
            // switches. First-run installs get it from EnsureCreated.
            EnsureOrientationColumn();

            // Overlay position persistence (diskette button). Same idempotent
            // ALTER pattern: existing DBs get PersistOverlayPosition (DEFAULT 0 =
            // off, classic behaviour) plus four nullable REAL columns for the
            // saved per-orientation positions. First-run installs get them from
            // EnsureCreated via the model properties.
            EnsurePersistPositionColumns();

            // The AppSettingsRepository self-bootstraps the singleton
            // row on first GetAsync() call, so no explicit seeding here.

            // One-time migration: copy any Google OAuth secrets from the
            // legacy AppSettings table into the SK file, then null them in
            // the DB so they don't leak into the Drive backup JSON.
            // Idempotent: skipped on subsequent runs because the SK file
            // already holds the values.
            MigrateGoogleSecretsToSk();

            // Strg+1..9 Hotkeys SOFORT scharfschalten, ohne dass der Benutzer
            // erst das Prompt-Board oeffnen muss. Frueher wurde die
            // HotkeyRegistry ausschliesslich vom PromptBoardPanel.Render()
            // befuellt — solange das Panel nie sichtbar war, blieb die
            // Registry leer und der Low-Level-Keyboard-Hook in OverlayWindow
            // hat alle Strg+1..9-Tastendruecke durchgereicht. Jetzt laden
            // wir den initialen Stand direkt aus der DB, danach
            // ueberschreibt jeder Panel-Render die Werte weiterhin im
            // Normalbetrieb.
            LoadHotkeyRegistryFromDb();
        }
    }

    /// <summary>
    /// Befuellt die globale <see cref="HotkeyRegistry"/> einmalig aus der
    /// SQLite-DB. Wird beim Overlay-Start aufgerufen, damit Strg+1..9 sofort
    /// funktionieren — auch ohne dass das PromptBoardPanel je geoeffnet wurde.
    /// Synchron implementiert (passt in den synchronen Initialize-Pfad).
    /// Fehler werden geschluckt: ein DB-Problem darf den Overlay-Start
    /// niemals blockieren — die Hotkeys bleiben in dem Fall einfach inert,
    /// genau wie vor diesem Fix.
    /// </summary>
    private static void LoadHotkeyRegistryFromDb()
    {
        try
        {
            using var scope = _provider!.CreateScope();
            var ctx = scope.ServiceProvider.GetRequiredService<PromptBoardDbContext>();
            // Beide Hotkey-Spalten in EINER Abfrage holen: ein Prompt kann
            // sowohl HotkeyNumber als auch HotkeyLetter gesetzt haben, und
            // wir brauchen ihn in beiden Faellen.
            var bound = ctx.Prompts
                .AsNoTracking()
                .Where(p => p.HotkeyNumber != null || p.HotkeyLetter != null)
                .ToList();

            var numberEntries = new List<KeyValuePair<int, HotkeyRegistry.Entry>>();
            var letterEntries = new List<KeyValuePair<char, HotkeyRegistry.Entry>>();
            foreach (var p in bound)
            {
                if (p.HotkeyNumber is int n && n >= 1 && n <= 9)
                {
                    numberEntries.Add(new KeyValuePair<int, HotkeyRegistry.Entry>(
                        n, new HotkeyRegistry.Entry(p.Id, p.EffectiveText())));
                }
                if (p.HotkeyLetter is char l)
                {
                    char upper = char.ToUpperInvariant(l);
                    if (upper >= 'A' && upper <= 'Z')
                    {
                        letterEntries.Add(new KeyValuePair<char, HotkeyRegistry.Entry>(
                            upper, new HotkeyRegistry.Entry(p.Id, p.EffectiveText())));
                    }
                }
            }
            HotkeyRegistry.Replace(numberEntries);
            HotkeyRegistry.ReplaceLetters(letterEntries);
            string msg = $"HotkeyRegistry bootstrap: {numberEntries.Count} Strg+N + {letterEntries.Count} Win+Alt+Buchstabe geladen.";
            Console.WriteLine(msg);
            // Auch ins Diagnose-Log schreiben damit wir ohne Console-Output
            // sehen ob der Bootstrap erfolgreich war.
            try
            {
                string logPath = Path.Combine(Path.GetTempPath(), "TVO-hotkey.log");
                File.AppendAllText(logPath, $"{DateTime.Now:HH:mm:ss.fff} BOOTSTRAP {msg}{Environment.NewLine}");
                foreach (var kv in letterEntries)
                {
                    File.AppendAllText(logPath, $"{DateTime.Now:HH:mm:ss.fff} BOOTSTRAP letter='{kv.Key}' textLen={kv.Value.EffectiveText.Length}{Environment.NewLine}");
                }
            }
            catch { /* never block startup */ }
        }
        catch (Exception ex)
        {
            // "Better stale than broken": bei DB-Fehler bleibt die Registry
            // leer, der Hook reicht Strg+1..9 ans Fokusfenster weiter — der
            // exakt gleiche Zustand wie vor diesem Fix.
            Console.WriteLine($"HotkeyRegistry bootstrap failed: {ex.Message}");
        }
    }

    private static void MigrateGoogleSecretsToSk()
    {
        try
        {
            using var scope = _provider!.CreateScope();
            var store = scope.ServiceProvider.GetRequiredService<PromptBoardSecretStore>();
            if (store.HasAnyValue())
                return; // already migrated — SK file is the source of truth

            // Frueher: repo.GetAsync().GetAwaiter().GetResult() — Sync-Over-Async
            // mitten im synchronen Initialize-Pfad. Funktional unproblematisch
            // weil kein UI-Thread-Dispatcher-Capture in den await-Continuations
            // (EF Core SQLite nutzt ConfigureAwait(false) intern), aber Code-
            // Smell und unnoetige Allocation einer Task + AwaiterPromise.
            // Jetzt: direkter synchroner DbContext-Zugriff. AppSettings.FirstOrDefault
            // (sync) statt FirstOrDefaultAsync; SaveChanges() statt SaveChangesAsync().
            // Wenn noch keine Settings-Row existiert, gibt es auch nichts zu
            // migrieren — early return (frueher legte AppSettingsRepository.GetAsync
            // eine leere Row an, was die Migration trivialerweise verfehlte;
            // gleiche Endbedingung jetzt ohne den Roundtrip).
            var ctx = scope.ServiceProvider.GetRequiredService<PromptBoardDbContext>();
            var dbSettings = ctx.AppSettings.FirstOrDefault();
            if (dbSettings is null) return;

            var hasDbSecrets =
                !string.IsNullOrEmpty(dbSettings.GoogleClientId)
                || !string.IsNullOrEmpty(dbSettings.GoogleClientSecret)
                || !string.IsNullOrEmpty(dbSettings.GoogleOAuthRefreshToken);
            if (!hasDbSecrets) return;

            // Copy DB → SK first. Only null the DB columns once the SK
            // write succeeded — otherwise a corrupted SK file would
            // permanently lose the user's OAuth refresh token.
            store.Save(new Secrets(
                GoogleClientId:           dbSettings.GoogleClientId,
                GoogleClientSecret:       dbSettings.GoogleClientSecret,
                GoogleOAuthRefreshToken:  dbSettings.GoogleOAuthRefreshToken,
                GoogleAccountEmail:       dbSettings.GoogleAccountEmail));

            dbSettings.GoogleClientId = null;
            dbSettings.GoogleClientSecret = null;
            dbSettings.GoogleOAuthRefreshToken = null;
            dbSettings.GoogleAccountEmail = null;
            ctx.SaveChanges();
        }
        catch
        {
            // Migration is best-effort; never crash startup over it. If it
            // fails the user will still see and edit their secrets through
            // the settings dialog — they just won't move out of the DB
            // until the next successful pass.
        }
    }

    private static void EnsurePrePostColumns()
    {
        try
        {
            using var conn = new SqliteConnection($"Data Source={DbPath}");
            conn.Open();
            TryRun(conn, "ALTER TABLE Prompts ADD COLUMN IsPrePrompt INTEGER NOT NULL DEFAULT 1");
            TryRun(conn, "ALTER TABLE Prompts ADD COLUMN IsPostPrompt INTEGER NOT NULL DEFAULT 0");
        }
        catch
        {
            // Database might not exist yet on first run — EF-Core will
            // create it from the model below with the columns already
            // in place. Either way, never block startup over the
            // migration check.
        }
    }

    private static void EnsureHotkeyColumn()
    {
        try
        {
            using var conn = new SqliteConnection($"Data Source={DbPath}");
            conn.Open();
            // Nullable INTEGER. CREATE INDEX IF NOT EXISTS is itself
            // idempotent so it's safe to run on every startup even
            // after EnsureCreated has already added the index.
            TryRun(conn, "ALTER TABLE Prompts ADD COLUMN HotkeyNumber INTEGER NULL");
            TryRun(conn, "CREATE INDEX IF NOT EXISTS IX_Prompts_HotkeyNumber ON Prompts (HotkeyNumber)");
        }
        catch
        {
            // First-run case: EF-Core EnsureCreated will have created
            // the column from the model already. Never block startup.
        }
    }

    private static void EnsureHotkeyLetterColumn()
    {
        try
        {
            using var conn = new SqliteConnection($"Data Source={DbPath}");
            conn.Open();
            // Nullable TEXT, one char (A-Z) per row. EF Core maps char? to
            // TEXT in SQLite. Index for the low-level hook's per-keystroke
            // lookup, same as HotkeyNumber.
            TryRun(conn, "ALTER TABLE Prompts ADD COLUMN HotkeyLetter TEXT NULL");
            TryRun(conn, "CREATE INDEX IF NOT EXISTS IX_Prompts_HotkeyLetter ON Prompts (HotkeyLetter)");
        }
        catch
        {
            // First-run case: EF-Core EnsureCreated will have created
            // the column from the model already. Never block startup.
        }
    }

    private static void EnsureAutoHideColumn()
    {
        try
        {
            using var conn = new SqliteConnection($"Data Source={DbPath}");
            conn.Open();
            // NOT NULL DEFAULT 1 → existing rows become auto-hide=on, the
            // intended default. Boolean maps to INTEGER 0/1 in SQLite, same
            // as the EF Core convention for the AppSettings.AutoHide property.
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN AutoHide INTEGER NOT NULL DEFAULT 1");
        }
        catch
        {
            // First-run case: EF-Core EnsureCreated will have created
            // the column from the model already. Never block startup.
        }
    }

    private static void EnsureOrientationColumn()
    {
        try
        {
            using var conn = new SqliteConnection($"Data Source={DbPath}");
            conn.Open();
            // TEXT NOT NULL DEFAULT 'vertical' → existing rows keep the classic
            // vertical layout. EF Core maps the string property to TEXT by
            // convention (AppSettings.Orientation).
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN Orientation TEXT NOT NULL DEFAULT 'vertical'");
        }
        catch
        {
            // First-run case: EF-Core EnsureCreated will have created
            // the column from the model already. Never block startup.
        }
    }

    private static void EnsurePersistPositionColumns()
    {
        try
        {
            using var conn = new SqliteConnection($"Data Source={DbPath}");
            conn.Open();
            // Bool maps to INTEGER 0/1; nullable double maps to REAL NULL —
            // same EF Core conventions as the AppSettings properties.
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN PersistOverlayPosition INTEGER NOT NULL DEFAULT 0");
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN OverlayVerticalLeft REAL NULL");
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN OverlayVerticalTop REAL NULL");
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN OverlayHorizontalLeft REAL NULL");
            TryRun(conn, "ALTER TABLE AppSettings ADD COLUMN OverlayHorizontalTop REAL NULL");
        }
        catch
        {
            // First-run case: EF-Core EnsureCreated will have created
            // the columns from the model already. Never block startup.
        }
    }

    private static void TryRun(DbConnection conn, string sql)
    {
        try
        {
            using var cmd = conn.CreateCommand();
            cmd.CommandText = sql;
            cmd.ExecuteNonQuery();
        }
        catch (SqliteException) { /* duplicate column — already migrated */ }
    }

    public static T Get<T>() where T : notnull => Services.GetRequiredService<T>();
}
