using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;

namespace OpenLauncher;

/// <summary>Zeigt langen Text vollständig, mit Lesepausen und gleichmäßiger Bewegung.</summary>
public sealed class MarqueeText : Grid
{
    public static readonly DependencyProperty TextProperty = DependencyProperty.Register(
        nameof(Text), typeof(string), typeof(MarqueeText), new PropertyMetadata(string.Empty, Changed));
    public static readonly DependencyProperty IsActiveProperty = DependencyProperty.Register(
        nameof(IsActive), typeof(bool), typeof(MarqueeText), new PropertyMetadata(true, Changed));

    public string Text { get => (string)GetValue(TextProperty); set => SetValue(TextProperty, value); }
    public bool IsActive { get => (bool)GetValue(IsActiveProperty); set => SetValue(IsActiveProperty, value); }

    private readonly TextBlock _text = new() { TextWrapping = TextWrapping.NoWrap };
    private readonly Canvas _canvas = new();
    private readonly TranslateTransform _offset = new();

    public MarqueeText()
    {
        ClipToBounds = true;
        VerticalAlignment = VerticalAlignment.Center;
        _text.RenderTransform = _offset;
        _canvas.Children.Add(_text);
        Children.Add(_canvas);
        Loaded += (_, _) => Refresh();
        SizeChanged += (_, _) => Refresh();
        IsVisibleChanged += (_, _) => Refresh();
        Unloaded += (_, _) => _offset.BeginAnimation(TranslateTransform.XProperty, null);
    }

    private static void Changed(DependencyObject target, DependencyPropertyChangedEventArgs args) =>
        ((MarqueeText)target).Refresh();

    private void Refresh()
    {
        _offset.BeginAnimation(TranslateTransform.XProperty, null);
        _offset.X = 0;
        _text.Text = Text ?? string.Empty;
        _text.Measure(new Size(double.PositiveInfinity, double.PositiveInfinity));
        _canvas.Height = _text.DesiredSize.Height;
        var overflow = _text.DesiredSize.Width - ActualWidth;
        if (!IsLoaded || !IsVisible || !IsActive || ActualWidth <= 0 || overflow <= 1) return;

        // 28 DIP pro Sekunde: lange Pfade bleiben unabhängig von ihrer Länge lesbar.
        var travel = TimeSpan.FromSeconds(overflow / 28);
        var start = TimeSpan.FromSeconds(1.5);
        var end = start + travel;
        var cycle = end + TimeSpan.FromSeconds(2);
        var animation = new DoubleAnimationUsingKeyFrames
        {
            Duration = new Duration(cycle),
            RepeatBehavior = RepeatBehavior.Forever
        };
        animation.KeyFrames.Add(new DiscreteDoubleKeyFrame(0, KeyTime.FromTimeSpan(TimeSpan.Zero)));
        animation.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromTimeSpan(start)));
        animation.KeyFrames.Add(new LinearDoubleKeyFrame(-overflow, KeyTime.FromTimeSpan(end)));
        animation.KeyFrames.Add(new DiscreteDoubleKeyFrame(-overflow, KeyTime.FromTimeSpan(cycle)));
        _offset.BeginAnimation(TranslateTransform.XProperty, animation);
    }
}
