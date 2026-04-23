using System;
using System.Collections.Generic;
using System.Globalization;
using PromptBoard.Core.Services;

namespace PromptBoard.Tests;

public class PastelColorGeneratorTests
{
    [Fact]
    public void NextDistinctColor_WithNoExistingColors_ReturnsValidHexRgb()
    {
        var generator = new PastelColorGenerator(new Random(42));
        string color = generator.NextDistinctColor([]);

        Assert.StartsWith("#", color);
        Assert.Equal(7, color.Length);
        // Parseable as a number.
        _ = int.Parse(color[1..], NumberStyles.HexNumber, CultureInfo.InvariantCulture);
    }

    [Fact]
    public void NextDistinctColor_WithManyExisting_StillReturnsAColor()
    {
        var generator = new PastelColorGenerator(new Random(7));
        var existing = new List<string>();
        for (int i = 0; i < 12; i++)
        {
            string next = generator.NextDistinctColor(existing);
            Assert.DoesNotContain(next, existing);
            existing.Add(next);
        }
        Assert.Equal(12, existing.Count);
    }

    [Fact]
    public void NextDistinctColor_IgnoresMalformedHex_WithoutThrowing()
    {
        var generator = new PastelColorGenerator(new Random(1));
        string color = generator.NextDistinctColor(["not-a-color", "#XYZ", ""]);
        Assert.StartsWith("#", color);
    }
}
