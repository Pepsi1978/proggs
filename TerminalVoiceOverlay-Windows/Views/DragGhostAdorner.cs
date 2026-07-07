using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Media;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// Floating semi-transparent preview that follows the mouse during a
/// PromptBoard drag. Hosts a single <see cref="Image"/> rendered from a
/// snapshot of the dragged row, then re-arranged each time the user
/// moves the cursor over the panel.
///
/// WPF has no built-in drag preview — without this adorner the user
/// just sees the cursor change while dragging, which makes drag&amp;drop
/// feel broken even when it is wired up correctly underneath.
/// </summary>
internal sealed class DragGhostAdorner : Adorner
{
    private readonly Image _image;
    private Point _location;
    private readonly double _width;
    private readonly double _height;

    /// <summary>Tunable: how far down-right of the cursor the ghost sits.</summary>
    private const double OffsetX = 8;
    private const double OffsetY = 8;

    public DragGhostAdorner(UIElement adornedElement, ImageSource snapshot,
                            double width, double height)
        : base(adornedElement)
    {
        _width = width;
        _height = height;
        _image = new Image
        {
            Source = snapshot,
            Width = width,
            Height = height,
            Opacity = 0.78,
            IsHitTestVisible = false,
        };
        IsHitTestVisible = false;
        // Slight rotation makes it more obvious that the row is "lifted",
        // not just duplicated. Subtle so it doesn't look glitchy.
        _image.RenderTransformOrigin = new Point(0.5, 0.5);
        _image.RenderTransform = new RotateTransform(-2);
        AddVisualChild(_image);
    }

    public void UpdateLocation(Point location)
    {
        _location = location;
        InvalidateArrange();
    }

    protected override int VisualChildrenCount => 1;
    protected override Visual GetVisualChild(int index) => _image;

    protected override Size MeasureOverride(Size constraint)
    {
        _image.Measure(new Size(_width, _height));
        return new Size(_width, _height);
    }

    protected override Size ArrangeOverride(Size finalSize)
    {
        var origin = new Point(_location.X + OffsetX, _location.Y + OffsetY);
        _image.Arrange(new Rect(origin, new Size(_width, _height)));
        return finalSize;
    }
}
