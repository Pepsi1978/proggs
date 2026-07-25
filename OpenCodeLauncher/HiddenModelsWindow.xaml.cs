using System.Windows;
using OpenCodeLauncher.ViewModels;

namespace OpenCodeLauncher;

public partial class HiddenModelsWindow : Window
{
    public HiddenModelsWindow(MainViewModel viewModel)
    {
        InitializeComponent();
        DataContext = viewModel;
    }

    private void Close_Click(object sender, RoutedEventArgs e) => Close();
}
