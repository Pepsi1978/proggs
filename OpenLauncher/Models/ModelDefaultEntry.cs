namespace OpenLauncher.Models;

/// <summary>
/// Von Hand gespeicherte Startauswahl EINES Modells: Profil, Arbeitsmodus und Effort/Thinking.
/// Wird ueber den Schalter "Standard speichern" im Profil-Bereich angelegt und beim Modellwechsel
/// (und damit auch beim Start) wieder vorausgewaehlt.
/// </summary>
public sealed class ModelDefaultEntry
{
    public string ProfileId { get; set; } = string.Empty;
    public string WorkModeId { get; set; } = string.Empty;

    /// <summary>
    /// Effort- bzw. Thinking-Stufe als Kleinbuchstaben-Wert ("high", "xhigh", …). Leer, wenn das
    /// Modell beim Speichern keine Stufe angeboten hat.
    /// </summary>
    public string ThinkingValue { get; set; } = string.Empty;
}
