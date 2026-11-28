using System.IO;
using UpdateZentrale.Providers;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>Python-Suche der Jarvis-Verteilung: neueste Nebenversion zuerst, Free-Threading- und fremde Ordner nie.</summary>
public sealed class JarvisPythonSucheTests
{
    [Fact]
    public void InstallManagerOrdner_NachVersionAbsteigend_OhneFreeThreading()
    {
        var wurzel = Path.Combine(Path.GetTempPath(), "uz-py-" + Guid.NewGuid().ToString("N"));
        try
        {
            foreach (var n in new[] { "pythoncore-3.9-64", "pythoncore-3.14-64", "pythoncore-3.14t-64", "pythoncore-3.13-arm64", "_cache", "bin" })
                Directory.CreateDirectory(Path.Combine(wurzel, n));
            var namen = JarvisVerteilungAktualisierer.Versionsordner(wurzel, JarvisVerteilungAktualisierer.MusterInstallManager)
                                                     .Select(Path.GetFileName).ToArray();
            Assert.Equal(new[] { "pythoncore-3.14-64", "pythoncore-3.13-arm64", "pythoncore-3.9-64" }, namen);
        }
        finally { Directory.Delete(wurzel, true); }
    }

    [Fact]
    public void KlassischeOrdner_Python313GewinntGegenPython39()
    {
        var wurzel = Path.Combine(Path.GetTempPath(), "uz-py-" + Guid.NewGuid().ToString("N"));
        try
        {
            foreach (var n in new[] { "Python39", "Python313-32", "Python313t", "Python313" })
                Directory.CreateDirectory(Path.Combine(wurzel, n));
            var namen = JarvisVerteilungAktualisierer.Versionsordner(wurzel, JarvisVerteilungAktualisierer.MusterKlassisch)
                                                     .Select(Path.GetFileName).ToArray();
            Assert.Equal(new[] { "Python313", "Python313-32", "Python39" }, namen);
        }
        finally { Directory.Delete(wurzel, true); }
    }

    [Fact]
    public void UeberWurzelnHinweg_NeuesteVersionGewinnt()
    {
        var wurzel = Path.Combine(Path.GetTempPath(), "uz-py-" + Guid.NewGuid().ToString("N"));
        try
        {
            var klassisch = Path.Combine(wurzel, "Programs");
            var manager = Path.Combine(wurzel, "Python");
            Directory.CreateDirectory(Path.Combine(klassisch, "Python38"));
            Directory.CreateDirectory(Path.Combine(manager, "pythoncore-3.14-64"));
            Directory.CreateDirectory(Path.Combine(manager, "pythoncore-99999999999-64"));
            var namen = JarvisVerteilungAktualisierer.Versionsordner(
                    [(klassisch, JarvisVerteilungAktualisierer.MusterKlassisch), (manager, JarvisVerteilungAktualisierer.MusterInstallManager)])
                .Select(Path.GetFileName).ToArray();
            Assert.Equal(new[] { "pythoncore-3.14-64", "Python38" }, namen);
        }
        finally { Directory.Delete(wurzel, true); }
    }

    [Fact]
    public void FehlenderOrdner_LeereListe()
        => Assert.Empty(JarvisVerteilungAktualisierer.Versionsordner(Path.Combine(Path.GetTempPath(), "gibt-es-nicht-" + Guid.NewGuid().ToString("N")), JarvisVerteilungAktualisierer.MusterKlassisch));
}
