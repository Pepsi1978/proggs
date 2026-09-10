using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Sorgt dafuer, dass auf jedem Rechner derselbe Android-Debug-Schluessel liegt. Das Android-Gradle-Plugin
/// signiert jeden Debug-Build ohne eigene signingConfig mit ~/.android/debug.keystore - der ist pro Rechner
/// zufaellig erzeugt, deshalb verweigerte das Handy nach einem Rechnerwechsel das Update
/// (INSTALL_FAILED_UPDATE_INCOMPATIBLE) und nur Deinstallieren (= Datenverlust) half.
/// Quelle ist ~/SK/Android/debug-shared.keystore (SHA-256 F7:82:13:1C...), Rueckfall Y:\Keystores\Android.
/// Ein abweichender alter Schluessel wird nie geloescht, sondern nach ~/SK/Android/alt gesichert: nur mit ihm
/// lassen sich damit signierte Apps spaeter per apksigner-Rotation ohne Deinstallation umziehen.
/// </summary>
public static class AndroidDebugKeystoreSync
{
    private const string FileName = "debug-shared.keystore";

    public static void Run()
    {
        var log = Logger.Instance;
        try
        {
            var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var skDir = Path.Combine(home, "SK", "Android");
            var skKey = Path.Combine(skDir, FileName);
            var driveKey = Path.Combine(@"Y:\Keystores\Android", FileName);

            if (!File.Exists(skKey))
            {
                if (!File.Exists(driveKey))
                {
                    log.Warn("AndroidKeystore", "Run", $"gemeinsamer Debug-Key fehlt ({skKey}, {driveKey}) - nichts geaendert");
                    return;
                }
                Directory.CreateDirectory(skDir);
                File.Copy(driveKey, skKey);
                log.Info("AndroidKeystore", "Run", $"gemeinsamer Debug-Key von {driveKey} nach {skKey} uebernommen");
            }

            var androidDir = Path.Combine(home, ".android");
            var target = Path.Combine(androidDir, "debug.keystore");
            var shared = File.ReadAllBytes(skKey);

            if (File.Exists(target))
            {
                if (File.ReadAllBytes(target).AsSpan().SequenceEqual(shared)) return;

                var altDir = Path.Combine(skDir, "alt");
                Directory.CreateDirectory(altDir);
                var backup = Path.Combine(altDir, $"debug-{Environment.MachineName}-{DateTime.Now:yyyyMMdd-HHmmss}.keystore");
                File.Copy(target, backup);
                log.Info("AndroidKeystore", "Run", $"abweichenden Debug-Key gesichert: {backup}");
            }

            Directory.CreateDirectory(androidDir);
            File.WriteAllBytes(target, shared);
            log.Info("AndroidKeystore", "Run", $"gemeinsamer Debug-Key nach {target} gesetzt");
        }
        catch (Exception ex)
        {
            log.Error("AndroidKeystore", "Run", ex);
        }
    }
}
