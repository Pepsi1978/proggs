using System;
using System.Data.Common;
using System.IO;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
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

            // The AppSettingsRepository self-bootstraps the singleton
            // row on first GetAsync() call, so no explicit seeding here.

            // One-time migration: copy any Google OAuth secrets from the
            // legacy AppSettings table into the SK file, then null them in
            // the DB so they don't leak into the Drive backup JSON.
            // Idempotent: skipped on subsequent runs because the SK file
            // already holds the values.
            MigrateGoogleSecretsToSk();
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

            var repo = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();
            var dbSettings = repo.GetAsync().GetAwaiter().GetResult();
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
            repo.UpdateAsync(dbSettings).GetAwaiter().GetResult();
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
