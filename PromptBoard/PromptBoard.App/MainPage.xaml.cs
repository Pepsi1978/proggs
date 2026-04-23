using System;
using System.Collections.Specialized;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PromptBoard.App.Services;
using PromptBoard.Core.Services;
using PromptBoard.ViewModels;
using Serilog;

namespace PromptBoard.App;

/// <summary>
/// Root content of MainWindow. Hosts the three-zone bar layout
/// (Standard, Projects, AiLibrary) plus the "+ Kategorie" button.
/// Wrapped in a UserControl because WinUI 3 Window does not play well
/// with x:Bind DataTemplates declared in window-level resources.
/// </summary>
public sealed partial class MainPage : UserControl
{
    public MainViewModel ViewModel { get; }

    private readonly IServiceProvider _services;

    public MainPage()
    {
        InitializeComponent();

        _services = App.Host.Services;
        ViewModel = _services.GetRequiredService<MainViewModel>();

        StandardList.ItemsSource = ViewModel.StandardCategories;
        ProjectList.ItemsSource = ViewModel.ProjectCategories;
        AiLibraryList.ItemsSource = ViewModel.AiLibraryCategories;

        AddCategoryButton.Click += async (_, _) =>
        {
            await ViewModel.AddCategoryCommand.ExecuteAsync(null);
            UpdateEmptyHint();
            UpdateSeparators();
        };

        BackupButton.Click += async (_, _) =>
        {
            var dialogs = _services.GetRequiredService<IDialogService>();
            bool restored = await dialogs.ShowBackupAsync();
            if (restored)
            {
                await ViewModel.InitializeAsync();
                UpdateEmptyHint();
                UpdateSeparators();
            }
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
                UpdateSeparators();

                NotifyCollectionChangedEventHandler refresh = (_, _) =>
                {
                    UpdateEmptyHint();
                    UpdateSeparators();
                };
                ViewModel.Categories.CollectionChanged += refresh;
                ViewModel.StandardCategories.CollectionChanged += refresh;
                ViewModel.ProjectCategories.CollectionChanged += refresh;
                ViewModel.AiLibraryCategories.CollectionChanged += refresh;
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

    /// <summary>
    /// Hide zone separators when the adjacent zone is empty, so the bar
    /// does not show orphan divider lines with nothing on either side.
    /// </summary>
    private void UpdateSeparators()
    {
        bool hasStandard = ViewModel.StandardCategories.Count > 0;
        bool hasProjects = ViewModel.ProjectCategories.Count > 0;
        bool hasAi = ViewModel.AiLibraryCategories.Count > 0;

        SeparatorAfterStandard.Visibility = hasStandard && (hasProjects || hasAi)
            ? Visibility.Visible
            : Visibility.Collapsed;

        SeparatorAfterProjects.Visibility = hasProjects && hasAi
            ? Visibility.Visible
            : Visibility.Collapsed;
    }
}
