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
            services.AddSingleton<IGoogleDriveBackupService, GoogleDriveBackupService>();

            _provider = services.BuildServiceProvider();

            // Idempotent schema migration for older Windows DBs that
            // pre-date the per-prompt Pre/Post split (#1820 macOS, this
            // commit Windows). SQLite throws on duplicate-column ALTER
            // TABLE, which we swallow because we just want "the columns
            // exist" — exactly mirrors the macOS approach.
            EnsurePrePostColumns();
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
