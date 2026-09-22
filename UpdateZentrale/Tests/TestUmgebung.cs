using System.IO;
using System.Runtime.CompilerServices;
using UpdateZentrale.Services;

namespace UpdateZentrale.Tests;

/// <summary>
/// Runs once when the test assembly loads, before any test: every log and history write goes
/// to a private temp folder instead of %LOCALAPPDATA%\UpdateZentrale\logs.
/// </summary>
internal static class TestUmgebung
{
    public static string LogOrdner { get; } = Path.Combine(Path.GetTempPath(), "uz-testlogs-" + Guid.NewGuid().ToString("N"));

    [ModuleInitializer]
    internal static void Einrichten()
    {
        Directory.CreateDirectory(LogOrdner);
        Protokollierung.Ordner = LogOrdner;
    }
}
