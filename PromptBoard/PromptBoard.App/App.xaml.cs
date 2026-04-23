using System;
using System.IO;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.UI.Xaml;
using PromptBoard.App.Services;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using PromptBoard.Data;
using PromptBoard.Data.Repositories;
using PromptBoard.ViewModels;
using Serilog;

namespace PromptBoard.App;

public partial class App : Application
{
    public static IHost Host { get; private set; } = null!;

    private Window? _window;

    public App()
    {
        InitializeComponent();
        ConfigureHost();
    }

    private static void ConfigureHost()
    {
        string appDataDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PromptBoard");
        Directory.CreateDirectory(appDataDir);

        string logPath = Path.Combine(appDataDir, "logs", "promptboard-.log");
        string dbPath = Path.Combine(appDataDir, "promptboard.db");

        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Information()
            .WriteTo.Console()
            .WriteTo.File(logPath,
                rollingInterval: RollingInterval.Day,
                retainedFileCountLimit: 14,
                outputTemplate: "{Timestamp:yyyy-MM-dd HH:mm:ss.fff zzz} [{Level:u3}] {SourceContext} {Message:lj}{NewLine}{Exception}")
            .CreateLogger();

        Host = Microsoft.Extensions.Hosting.Host
            .CreateDefaultBuilder()
            .ConfigureAppConfiguration((_, config) =>
            {
                config.SetBasePath(AppContext.BaseDirectory);
                config.AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);
            })
            .UseSerilog()
            .ConfigureServices((context, services) =>
            {
                // EF Core (scoped is the default; transient for WinUI so each
                // resolution gets a fresh unit-of-work, simpler for MVP).
                services.AddDbContext<PromptBoardDbContext>(options =>
                    options.UseSqlite($"Data Source={dbPath}"), ServiceLifetime.Transient);

                // Repositories
                services.AddTransient<ICategoryRepository, CategoryRepository>();
                services.AddTransient<IPromptRepository, PromptRepository>();
                services.AddTransient<IAiImprovementPromptRepository, AiImprovementPromptRepository>();
                services.AddTransient<IAppSettingsRepository, AppSettingsRepository>();

                // Utilities
                services.AddSingleton<IPastelColorGenerator, PastelColorGenerator>();
                services.AddSingleton<IDialogService, DialogService>();

                // ViewModels
                services.AddSingleton<MainViewModel>();

                // Window has a parameterless ctor; MainPage resolves MainViewModel
                // from the container lazily in its Loaded handler.
            })
            .Build();

        Log.Information("Host configured. DB path: {DbPath}", dbPath);
    }

    protected override void OnLaunched(Microsoft.UI.Xaml.LaunchActivatedEventArgs args)
    {
        try
        {
            using var scope = Host.Services.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<PromptBoardDbContext>();
            db.Database.EnsureCreated();
            Log.Information("Database verified/created.");
        }
        catch (Exception ex)
        {
            Log.Error(ex, "Failed to initialize database.");
        }

        _window = new MainWindow();
        _window.Activate();
    }
}
