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
/// <param name="ErstNachNeustart">
/// The installer staged the update and completes it when the program next starts (Claude Desktop
/// does this). Re-checking right away would report the old version and look like a failure, so
/// the card keeps the "done" state instead.
/// </param>
public sealed record PruefErgebnis(
    UpdateZustand Zustand,
    string InstallierteVersion = "",
    string VerfuegbareVersion = "",
    string Meldung = "",
    string Protokoll = "",
    bool ErstNachNeustart = false);
