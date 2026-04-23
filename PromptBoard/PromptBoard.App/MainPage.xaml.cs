using System;
using System.Collections.Specialized;
using System.ComponentModel;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using PromptBoard.App.Services;
using PromptBoard.Core.Models;
using PromptBoard.Core.Services;
using PromptBoard.ViewModels;
using Serilog;
using Windows.System;

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
    private Microsoft.UI.Dispatching.DispatcherQueueTimer? _undoTimer;

    public MainPage()
    {
        InitializeComponent();

        _services = App.Host.Services;
        ViewModel = _services.GetRequiredService<MainViewModel>();

        ViewModel.PropertyChanged += OnViewModelPropertyChanged;
        UndoButton.Click += async (_, _) =>
        {
            _undoTimer?.Stop();
            UndoBar.IsOpen = false;
            await ViewModel.RestoreUndoCommand.ExecuteAsync(null);
        };
        UndoBar.Closed += (_, _) =>
        {
            _undoTimer?.Stop();
            ViewModel.DismissUndo();
        };

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

    private void OnViewModelPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName != nameof(MainViewModel.PendingUndo))
        {
            return;
        }

        UndoAction? action = ViewModel.PendingUndo;
        if (action is null)
        {
            UndoBar.IsOpen = false;
            _undoTimer?.Stop();
            return;
        }

        UndoBar.Message = action.Message;
        UndoBar.IsOpen = true;

        _undoTimer ??= DispatcherQueue.CreateTimer();
        _undoTimer.Stop();
        _undoTimer.Interval = TimeSpan.FromSeconds(10);
        _undoTimer.IsRepeating = false;
        _undoTimer.Tick -= OnUndoTimerTick;
        _undoTimer.Tick += OnUndoTimerTick;
        _undoTimer.Start();
    }

    private void OnUndoTimerTick(Microsoft.UI.Dispatching.DispatcherQueueTimer sender, object args)
    {
        UndoBar.IsOpen = false;
        ViewModel.DismissUndo();
    }

    /// <summary>
    /// Keyboard shortcuts for individual prompts.
    /// F2 opens the editor, Delete removes the prompt (with confirmation inside the VM).
    /// Enter is handled by WinUI for free — the Insert-button is the default button.
    /// </summary>
    private void OnPromptKeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (sender is not FrameworkElement fe || fe.DataContext is not PromptViewModel pvm)
        {
            return;
        }

        switch (e.Key)
        {
            case VirtualKey.F2:
                if (pvm.EditCommand.CanExecute(null))
                {
                    pvm.EditCommand.Execute(null);
                    e.Handled = true;
                }
                break;

            case VirtualKey.Delete:
                if (pvm.DeleteCommand.CanExecute(null))
                {
                    pvm.DeleteCommand.Execute(null);
                    e.Handled = true;
                }
                break;
        }
    }

    /// <summary>
    /// Keyboard shortcuts for category headers (inline + project flyout).
    /// F2 triggers Rename, Delete triggers Delete (each command confirms in the VM).
    /// </summary>
    private void OnCategoryKeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (sender is not FrameworkElement fe || fe.DataContext is not CategoryViewModel cvm)
        {
            return;
        }

        switch (e.Key)
        {
            case VirtualKey.F2:
                if (cvm.RenameCommand.CanExecute(null))
                {
                    cvm.RenameCommand.Execute(null);
                    e.Handled = true;
                }
                break;

            case VirtualKey.Delete:
                if (cvm.DeleteCommand.CanExecute(null))
                {
                    cvm.DeleteCommand.Execute(null);
                    e.Handled = true;
                }
                break;
        }
    }
}
