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
    /// <param name="pfadVorLauf">
    /// Der vor dem Update wirksame Pfad. Fehlt die Datei danach, faellt ExePfadWirksam auf den
    /// Katalog-Pfad zurueck, und die Meldung naennte einen Ort, an dem das Programm nie lag. Ein jetzt
    /// vorhandener Pfad geht vor: das Update kann das Programm an einen anderen Katalog-Ort gelegt haben.
    /// </param>
    public static string? Vollstaendig(ProgrammEintrag eintrag, string? pfadVorLauf = null)
    {
        var wirksam = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        var ziel = string.IsNullOrWhiteSpace(pfadVorLauf) || File.Exists(wirksam) ? wirksam : pfadVorLauf;
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
