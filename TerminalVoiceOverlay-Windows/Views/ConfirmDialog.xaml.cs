using System.Windows;

namespace TerminalVoiceOverlay.Views;

public partial class ConfirmDialog : Window
{
    public bool Confirmed { get; private set; }

    public ConfirmDialog(string title, string message, string confirmLabel = "Loeschen")
    {
        InitializeComponent();
        TitleText.Text = title;
        MessageText.Text = message;
        BtnOk.Content = confirmLabel;
        BtnCancel.Click += (_, _) => { Confirmed = false; Close(); };
        BtnOk.Click += (_, _) => { Confirmed = true; Close(); };
    }

    public static bool Ask(Window owner, string title, string message, string confirmLabel = "Loeschen")
    {
        var dlg = new ConfirmDialog(title, message, confirmLabel) { Owner = owner };
        dlg.ShowDialog();
        return dlg.Confirmed;
    }
}
