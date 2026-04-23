using System;
using System.Linq;
using PromptBoard.Core.Services;

namespace PromptBoard.Tests;

public class PromptChainBuilderTests
{
    private const string DefaultSeparator = "\n\n;\n\n";

    private readonly PromptChainBuilder _sut = new();

    [Fact]
    public void Build_WithNoAlwaysOn_ReturnsClickedText()
    {
        var clicked = new PromptChainItem(Guid.NewGuid(), "Hello");
        string result = _sut.Build([], clicked, DefaultSeparator);
        Assert.Equal("Hello", result);
    }

    [Fact]
    public void Build_WithAlwaysOnAndClicked_JoinsWithSeparator()
    {
        var a = new PromptChainItem(Guid.NewGuid(), "A");
        var b = new PromptChainItem(Guid.NewGuid(), "B");
        var clicked = new PromptChainItem(Guid.NewGuid(), "C");

        string result = _sut.Build([a, b], clicked, DefaultSeparator);

        Assert.Equal("A" + DefaultSeparator + "B" + DefaultSeparator + "C", result);
    }

    [Fact]
    public void Build_ClickedAlreadyInAlwaysOn_DoesNotDuplicate()
    {
        Guid sharedId = Guid.NewGuid();
        var a = new PromptChainItem(Guid.NewGuid(), "first");
        var b = new PromptChainItem(sharedId, "shared");
        var c = new PromptChainItem(Guid.NewGuid(), "last");

        string result = _sut.Build([a, b, c], new PromptChainItem(sharedId, "shared"), DefaultSeparator);

        // "shared" appears exactly once, in its natural position.
        Assert.Equal("first" + DefaultSeparator + "shared" + DefaultSeparator + "last", result);
        int occurrences = result.Split("shared").Length - 1;
        Assert.Equal(1, occurrences);
    }

    [Fact]
    public void Build_WithNullClicked_ReturnsOnlyAlwaysOn()
    {
        var a = new PromptChainItem(Guid.NewGuid(), "A");
        var b = new PromptChainItem(Guid.NewGuid(), "B");

        string result = _sut.Build([a, b], null, DefaultSeparator);

        Assert.Equal("A" + DefaultSeparator + "B", result);
    }

    [Fact]
    public void Build_WithEmptyEverything_ReturnsEmptyString()
    {
        string result = _sut.Build([], null, DefaultSeparator);
        Assert.Equal(string.Empty, result);
    }

    [Fact]
    public void Build_UsesSeparatorVerbatim_NoTrailingSeparator()
    {
        var a = new PromptChainItem(Guid.NewGuid(), "X");
        var clicked = new PromptChainItem(Guid.NewGuid(), "Y");

        string result = _sut.Build([a], clicked, DefaultSeparator);

        Assert.EndsWith("Y", result);
        Assert.DoesNotContain(DefaultSeparator + DefaultSeparator, result);
    }

    [Fact]
    public void Build_PreservesAlwaysOnOrder()
    {
        var items = Enumerable.Range(0, 5)
            .Select(i => new PromptChainItem(Guid.NewGuid(), $"P{i}"))
            .ToArray();
        var clicked = new PromptChainItem(Guid.NewGuid(), "Click");

        string result = _sut.Build(items, clicked, "|");
        Assert.Equal("P0|P1|P2|P3|P4|Click", result);
    }

    [Fact]
    public void Build_DuplicateIdsInAlwaysOn_AreDeduplicated()
    {
        Guid dup = Guid.NewGuid();
        var a = new PromptChainItem(dup, "first");
        var b = new PromptChainItem(dup, "second");
        var clicked = new PromptChainItem(Guid.NewGuid(), "Tail");

        string result = _sut.Build([a, b], clicked, "-");
        // Only the first occurrence keeps the text; second is skipped.
        Assert.Equal("first-Tail", result);
    }
}
