using System;
using System.IO;
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
        }
    }

    public static T Get<T>() where T : notnull => Services.GetRequiredService<T>();
}
