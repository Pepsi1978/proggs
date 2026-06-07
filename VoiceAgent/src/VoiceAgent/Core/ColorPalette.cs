using System.Collections.Generic;

namespace VoiceAgent.Core
{
    /// <summary>Eine waehlbare Farbe (Anzeigename + Hex) fuer die Gespraechs-Farben.</summary>
    public sealed class ColorChoice
    {
        public string Name { get; set; } = "";
        public string Hex { get; set; } = "";

        public ColorChoice() { }
        public ColorChoice(string name, string hex) { Name = name; Hex = hex; }
    }

    /// <summary>Feste Farbpalette fuer die Auswahl in den Einstellungen (Du-/Agent-Farbe).</summary>
    public static class ColorPalette
    {
        public static readonly IReadOnlyList<ColorChoice> All = new List<ColorChoice>
        {
            new("Cyan",    "#4FC3F7"),
            new("Orange",  "#F97316"),
            new("Gruen",   "#66BB6A"),
            new("Lila",    "#AB47BC"),
            new("Gelb",    "#FFD54F"),
            new("Rot",     "#EF5350"),
            new("Pink",    "#EC407A"),
            new("Tuerkis", "#26C6DA"),
            new("Blau",    "#5C6BC0"),
            new("Grau",    "#9AA0AA"),
            new("Weiss",   "#E4E6EB"),
        };
    }
}
