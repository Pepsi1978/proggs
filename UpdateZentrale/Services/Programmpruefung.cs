using System.IO;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Prueft, ob ein Programm nach einem abgebrochenen Update noch vollstaendig da ist. Ein Installer,
/// der mitten im Ersetzen endet, laesst sonst eine fehlende oder leere Programmdatei zurueck, und die
/// Karte meldet nur "Zeitlimit".
/// </summary>
public static class Programmpruefung
{
    /// <summary>Null = vollstaendig (oder nichts zu pruefen); sonst was fehlt, als Satz fuer die Meldung.</summary>
    public static string? Vollstaendig(ProgrammEintrag eintrag)
    {
        if (string.IsNullOrWhiteSpace(eintrag.ExePfadWirksam)) return null;
        var ziel = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        if (string.IsNullOrWhiteSpace(ziel)) return null;
        try
        {
            var datei = new FileInfo(ziel);
            if (!datei.Exists) return eintrag.Name + " fehlt nach dem Update (" + ziel + ") – bitte neu installieren.";
            if (datei.Length == 0) return eintrag.Name + " ist nach dem Update unvollständig (" + ziel + " ist leer) – bitte neu installieren.";
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
            return null;
        }
        return null;
    }
}
