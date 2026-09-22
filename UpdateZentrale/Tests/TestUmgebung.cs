using System.IO;
using System.Runtime.CompilerServices;
using UpdateZentrale.Services;

namespace UpdateZentrale.Tests;

/// <summary>
/// Removes a test's temp folder. A process the test just killed (its exe copy lives in the
/// folder) can keep the file locked for a moment -- so retry briefly instead of leaving it behind.
/// </summary>
internal static class TestOrdner
{
    public static void Loeschen(string ordner)
    {
        for (var versuch = 0; versuch < 20 && Directory.Exists(ordner); versuch++)
        {
            try
            {
                Directory.Delete(ordner, recursive: true);
                return;
            }
            catch (IOException) { Thread.Sleep(150); }
            catch (UnauthorizedAccessException) { Thread.Sleep(150); }
        }
    }
}

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

        // The folder lives only as long as the test run: removed when the test host ends.
        AppDomain.CurrentDomain.ProcessExit += (_, _) =>
        {
            try { Directory.Delete(LogOrdner, recursive: true); } catch { /* best effort at shutdown */ }
        };
    }
}
