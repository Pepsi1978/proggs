namespace UpdateZentrale.Services;

/// <summary>
/// The app's part of its own update. update-zentrale.ps1 gets this process's id (-AppPid); its
/// helper waits for exactly this process to end, then copies the new build in and starts it. So
/// the app ends ITSELF -- but only once everything of the run is written: the report and history,
/// the card's state, and in a batch the restart line of sammellaeufe.jsonl. A fixed pause in the
/// helper (20 s, silent) made the update look dead; a marker file could go stale.
/// </summary>
public static class Selbstupdate
{
    /// <summary>The argument that tells the script who started it.</summary>
    public static string PidArgument => " -AppPid " + Environment.ProcessId;

    /// <summary>
    /// Ends the app after a short moment, so queued UI updates and log lines still run. The helper
    /// sees the process go and takes over.
    /// </summary>
    public static async Task AppBeendenAsync()
    {
        Diagnose.Ereignis(Schwere.Info, "selbstupdate", "selbstupdate.beenden", "Selbst-Update: die App beendet sich für den Helfer.");
        await Task.Delay(TimeSpan.FromMilliseconds(500));
        System.Windows.Application.Current?.Dispatcher.Invoke(() => System.Windows.Application.Current.Shutdown());
    }
}
