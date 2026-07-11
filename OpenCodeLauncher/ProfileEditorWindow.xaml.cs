using System.Windows;
using System.Windows.Input;
using OpenCodeLauncher.Services;

namespace OpenCodeLauncher;

public partial class ProfileEditorWindow : Window
{
    public string GlobalText => GlobalEditor.Text;
    public string ProjectText => ProjectEditor.Text;

    public ProfileEditorWindow(InstructionProfileDocuments documents, bool isClaudeCode, string profileName)
    {
        InitializeComponent();
        TitleText.Text = $"Profil {profileName} bearbeiten";
        CliText.Text = isClaudeCode ? "Claude Code · CLAUDE.md" : "OpenCode · Profilquellen";
        GlobalPathText.Text = documents.GlobalPath;
        ProjectPathText.Text = documents.ProjectPath;
        GlobalEditor.Text = documents.GlobalText;
        ProjectEditor.Text = documents.ProjectText;
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ChangedButton != MouseButton.Left) return;
        if (e.ClickCount >= 2)
            WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        else
            DragMove();
    }

    private void Save_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = true;
        Close();
    }

    private void Cancel_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
        Close();
    }
}
