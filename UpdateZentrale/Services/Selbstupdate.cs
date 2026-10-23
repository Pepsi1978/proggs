using System.IO;

namespace UpdateZentrale.Services;

/// <summary>
/// The handshake with the helper of update-zentrale.ps1. The helper closes this app, copies the
/// new build in and starts it. It used to wait a fixed 20 s first so the app could finish its log
/// -- half a minute in which nothing visible happened, and the user closed the app by hand
/// thinking the update had died. Now the app says when it is done:
/// "selbstupdate-laeuft" before it starts the script, "selbstupdate-bereit" once the report of
/// the run is written. Without "laeuft" (the script started from outside) the helper closes the
/// app after one second.
/// </summary>
public static class Selbstupdate
{
    private static string Laeuft => Path.Combine(Pfade.BenutzerOrdner, "selbstupdate-laeuft");
    private static string BereitDatei => Path.Combine(Pfade.BenutzerOrdner, "selbstupdate-bereit");

    public static void LaeuftMelden() => Schreiben(Laeuft);

    /// <summary>The report is on disk: the helper may close the app now.</summary>
    public static void Bereit() => Schreiben(BereitDatei);

    public static void Aufraeumen()
    {
        try
        {
            File.Delete(Laeuft);
            File.Delete(BereitDatei);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "selbstupdate", Schwere.Debug);
        }
    }

    private static void Schreiben(string datei)
    {
        try
        {
            Directory.CreateDirectory(Pfade.BenutzerOrdner);
            File.WriteAllText(datei, DateTime.Now.ToString("O"));
        }
        catch (Exception diagAusnahme)
        {
            // Without the marker the helper simply falls back to its time limit.
            Diagnose.Gefangen(diagAusnahme, "selbstupdate", Schwere.Warnung);
        }
    }
}
