using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;

namespace PromptBoard.Core.Services;

/// <summary>
/// Default implementation. Picks hue at random, with fixed pastel
/// lightness (85–92 %) and saturation (30–50 %). When the hue wheel
/// becomes crowded, the algorithm progressively halves its minimum
/// distance requirement so it always finds a fresh color instead of
/// falling back to a shared gray that would duplicate on subsequent calls.
/// </summary>
public sealed class PastelColorGenerator : IPastelColorGenerator
{
    private readonly Random _random;
    private const double PreferredMinHueDistance = 30.0;

    public PastelColorGenerator() : this(new Random())
    {
    }

    public PastelColorGenerator(Random random)
    {
        _random = random;
    }

    public string NextDistinctColor(IEnumerable<string> existingHex)
    {
        string[] existingList = existingHex?.Where(h => !string.IsNullOrWhiteSpace(h)).ToArray() ?? [];
        double[] existingHues = existingList
            .Select(TryParseHue)
            .Where(h => h is not null)
            .Select(h => h!.Value)
            .ToArray();

        // Random attempts at the preferred hue distance.
        for (int attempt = 0; attempt < 32; attempt++)
        {
            double hue = _random.NextDouble() * 360.0;
            if (IsFarEnough(hue, existingHues, PreferredMinHueDistance))
            {
                return MakePastelHex(hue, existingList);
            }
        }

        // Fallback: deterministic grid walk with progressively smaller
        // min-distance, so even a crowded wheel always yields a fresh color.
        for (double minDistance = PreferredMinHueDistance; minDistance > 0.5; minDistance /= 2.0)
        {
            for (double hue = 0.0; hue < 360.0; hue += 5.0)
            {
                if (IsFarEnough(hue, existingHues, minDistance))
                {
                    string candidate = MakePastelHex(hue, existingList);
                    if (!existingList.Contains(candidate, StringComparer.OrdinalIgnoreCase))
                    {
                        return candidate;
                    }
                }
            }
        }

        // Last resort: perturb a random hue and vary lightness slightly.
        for (int attempt = 0; attempt < 16; attempt++)
        {
            double hue = _random.NextDouble() * 360.0;
            string candidate = HslToHex(
                hue,
                0.30 + (_random.NextDouble() * 0.20),
                0.83 + (_random.NextDouble() * 0.09));
            if (!existingList.Contains(candidate, StringComparer.OrdinalIgnoreCase))
            {
                return candidate;
            }
        }

        // Absolute fallback (should not happen in practice): bump saturation
        // until we produce a hex value not in the existing set.
        return HslToHex(_random.NextDouble() * 360.0, 0.49, 0.92);
    }

    private string MakePastelHex(double hue, IReadOnlyCollection<string> existing)
    {
        // Try a few saturation/lightness variations to avoid accidental duplicates
        // when the same hue has already been persisted previously.
        for (int attempt = 0; attempt < 8; attempt++)
        {
            double saturation = 0.30 + (_random.NextDouble() * 0.20);
            double lightness = 0.85 + (_random.NextDouble() * 0.07);
            string hex = HslToHex(hue, saturation, lightness);
            if (!existing.Contains(hex, StringComparer.OrdinalIgnoreCase))
            {
                return hex;
            }
        }
        // Fall through: return with default pastel values.
        return HslToHex(hue, 0.40, 0.88);
    }

    private static bool IsFarEnough(double candidate, IReadOnlyList<double> existing, double minDistance)
    {
        foreach (double e in existing)
        {
            double diff = Math.Abs(candidate - e);
            double wrapped = Math.Min(diff, 360.0 - diff);
            if (wrapped < minDistance)
            {
                return false;
            }
        }
        return true;
    }

    private static double? TryParseHue(string hex)
    {
        (double r, double g, double b)? rgb = TryParseRgb(hex);
        if (rgb is null)
        {
            return null;
        }

        double max = Math.Max(rgb.Value.r, Math.Max(rgb.Value.g, rgb.Value.b));
        double min = Math.Min(rgb.Value.r, Math.Min(rgb.Value.g, rgb.Value.b));
        double delta = max - min;
        if (delta < 0.0001)
        {
            return 0.0;
        }

        double h;
        if (Math.Abs(max - rgb.Value.r) < 0.0001)
        {
            h = 60.0 * (((rgb.Value.g - rgb.Value.b) / delta) % 6.0);
        }
        else if (Math.Abs(max - rgb.Value.g) < 0.0001)
        {
            h = 60.0 * (((rgb.Value.b - rgb.Value.r) / delta) + 2.0);
        }
        else
        {
            h = 60.0 * (((rgb.Value.r - rgb.Value.g) / delta) + 4.0);
        }

        return h < 0 ? h + 360.0 : h;
    }

    private static (double r, double g, double b)? TryParseRgb(string hex)
    {
        if (string.IsNullOrWhiteSpace(hex))
        {
            return null;
        }

        string stripped = hex.StartsWith('#') ? hex[1..] : hex;
        if (stripped.Length != 6)
        {
            return null;
        }

        if (!byte.TryParse(stripped.AsSpan(0, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out byte r)
            || !byte.TryParse(stripped.AsSpan(2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out byte g)
            || !byte.TryParse(stripped.AsSpan(4, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out byte b))
        {
            return null;
        }

        return (r / 255.0, g / 255.0, b / 255.0);
    }

    private static string HslToHex(double hue, double saturation, double lightness)
    {
        double c = (1.0 - Math.Abs((2.0 * lightness) - 1.0)) * saturation;
        double hPrime = hue / 60.0;
        double x = c * (1.0 - Math.Abs((hPrime % 2.0) - 1.0));

        double r1, g1, b1;
        if (hPrime < 1) { r1 = c; g1 = x; b1 = 0; }
        else if (hPrime < 2) { r1 = x; g1 = c; b1 = 0; }
        else if (hPrime < 3) { r1 = 0; g1 = c; b1 = x; }
        else if (hPrime < 4) { r1 = 0; g1 = x; b1 = c; }
        else if (hPrime < 5) { r1 = x; g1 = 0; b1 = c; }
        else { r1 = c; g1 = 0; b1 = x; }

        double m = lightness - (c / 2.0);
        byte r = (byte)Math.Round((r1 + m) * 255.0);
        byte g = (byte)Math.Round((g1 + m) * 255.0);
        byte b = (byte)Math.Round((b1 + m) * 255.0);

        return $"#{r:X2}{g:X2}{b:X2}";
    }
}
