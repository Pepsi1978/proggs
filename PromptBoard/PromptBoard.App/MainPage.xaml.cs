using System;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PromptBoard.App.Services;
using PromptBoard.Core.Services;
using PromptBoard.ViewModels;
using Serilog;

namespace PromptBoard.App;

/// <summary>
/// Root content of MainWindow. Hosts the bar layout. Wrapped in a
/// UserControl because WinUI 3 Window does not play well with x:Bind
/// DataTemplates declared in window-level resources.
/// </summary>
public sealed partial class MainPage : UserControl
{
    public MainViewModel ViewModel { get; }

    private readonly IServiceProvider _services;

    public MainPage()
    {
        InitializeComponent();

        // Resolve ViewModel from the app-wide DI container.
        _services = App.Host.Services;
        ViewModel = _services.GetRequiredService<MainViewModel>();
        CategoriesList.ItemsSource = ViewModel.Categories;

        AddCategoryButton.Click += async (_, _) =>
        {
            await ViewModel.AddCategoryCommand.ExecuteAsync(null);
            UpdateEmptyHint();
        };

        Loaded += async (_, _) =>
        {
            try
            {
                if (_services.GetRequiredService<IDialogService>() is DialogService concrete)
                {
                    concrete.Initialize(XamlRoot);
                }

                await ViewModel.InitializeAsync();
                UpdateEmptyHint();
                ViewModel.Categories.CollectionChanged += (_, _) => UpdateEmptyHint();
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Failed to initialize MainPage.");
            }
        };
    }

    private void UpdateEmptyHint()
    {
        EmptyHint.Visibility = ViewModel.Categories.Count == 0
            ? Visibility.Visible
            : Visibility.Collapsed;
    }
}
