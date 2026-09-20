namespace UpdateZentrale.Models;

public enum UpdateZustand
{
    Unbekannt,
    Pruefe,
    Aktuell,
    UpdateVerfuegbar,
    NichtInstalliert,
    Laeuft,
    Fehler,
    Fertig,
    Abgebrochen
}

/// <param name="Zustand">Result of a check or an update run.</param>
/// <param name="InstallierteVersion">Empty when the program is not installed.</param>
/// <param name="VerfuegbareVersion">Empty when nothing newer is known.</param>
/// <param name="Meldung">Short line shown on the card; the long log goes to the detail pane.</param>
public sealed record PruefErgebnis(
    UpdateZustand Zustand,
    string InstallierteVersion = "",
    string VerfuegbareVersion = "",
    string Meldung = "",
    string Protokoll = "");
