using Android;
using Android.Content;
using Android.Content.PM;
using Android.Net;
using Android.OS;
using Android.Provider;
using Android.Widget;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay;

[Activity(Label = "@string/app_name", MainLauncher = true, Theme = "@style/AppTheme",
    ScreenOrientation = ScreenOrientation.Portrait)]
public class MainActivity : Activity
{
    private const int PermissionRequestCode = 1001;
    private const int OverlayPermissionRequestCode = 1002;

    private EditText? _editGroqKey;
    private EditText? _editGeminiKey;
    private EditText? _editWhisperModel;
    private EditText? _editWhisperLang;
    private EditText? _editGeminiModel;
    private Button? _btnSave;
    private Button? _btnToggle;
    private TextView? _statusText;

    private bool _isOverlayRunning;

    protected override void OnCreate(Bundle? savedInstanceState)
    {
        base.OnCreate(savedInstanceState);
        SetContentView(Resource.Layout.activity_main);

        // Find views
        _editGroqKey = FindViewById<EditText>(Resource.Id.edit_groq_key);
        _editGeminiKey = FindViewById<EditText>(Resource.Id.edit_gemini_key);
        _editWhisperModel = FindViewById<EditText>(Resource.Id.edit_whisper_model);
        _editWhisperLang = FindViewById<EditText>(Resource.Id.edit_whisper_lang);
        _editGeminiModel = FindViewById<EditText>(Resource.Id.edit_gemini_model);
        _btnSave = FindViewById<Button>(Resource.Id.btn_save);
        _btnToggle = FindViewById<Button>(Resource.Id.btn_toggle_overlay);
        _statusText = FindViewById<TextView>(Resource.Id.status_text);

        // Load saved settings
        LoadSettings();

        // Button clicks
        _btnSave!.Click += (_, _) => SaveSettings();
        _btnToggle!.Click += (_, _) => ToggleOverlay();

        // Request permissions
        RequestPermissions();
    }

    protected override void OnResume()
    {
        base.OnResume();
        UpdateStatus();
    }

    private void LoadSettings()
    {
        var prefs = GetSharedPreferences("VoiceOverlayPrefs", FileCreationMode.Private);
        if (prefs == null) return;

        _editGroqKey!.Text = prefs.GetString("groq_api_key", "") ?? "";
        _editGeminiKey!.Text = prefs.GetString("gemini_api_key", "") ?? "";
        _editWhisperModel!.Text = prefs.GetString("whisper_model", "whisper-large-v3") ?? "whisper-large-v3";
        _editWhisperLang!.Text = prefs.GetString("whisper_lang", "de") ?? "de";
        _editGeminiModel!.Text = prefs.GetString("gemini_model", "gemini-3.1-flash-lite-preview") ?? "gemini-3.1-flash-lite-preview";
    }

    private void SaveSettings()
    {
        var groqKey = _editGroqKey!.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(groqKey))
        {
            Toast.MakeText(this, GetString(Resource.String.error_no_groq_key), ToastLength.Short)?.Show();
            return;
        }

        Config.Save(this,
            groqKey,
            _editGeminiKey!.Text?.Trim(),
            _editWhisperModel!.Text?.Trim() ?? "whisper-large-v3",
            _editWhisperLang!.Text?.Trim() ?? "de",
            _editGeminiModel!.Text?.Trim() ?? "gemini-3.1-flash-lite-preview");

        Toast.MakeText(this, GetString(Resource.String.toast_settings_saved), ToastLength.Short)?.Show();
    }

    private void ToggleOverlay()
    {
        if (_isOverlayRunning)
        {
            StopOverlay();
        }
        else
        {
            StartOverlay();
        }
    }

    private void StartOverlay()
    {
        // Check overlay permission
        if (!Settings.CanDrawOverlays(this))
        {
            Toast.MakeText(this, GetString(Resource.String.error_overlay_permission), ToastLength.Long)?.Show();
            var intent = new Intent(Settings.ActionManageOverlayPermission,
                Android.Net.Uri.Parse($"package:{PackageName}"));
            StartActivityForResult(intent, OverlayPermissionRequestCode);
            return;
        }

        // Check mic permission
        if (CheckSelfPermission(Manifest.Permission.RecordAudio) != Permission.Granted)
        {
            Toast.MakeText(this, GetString(Resource.String.error_mic_permission), ToastLength.Long)?.Show();
            RequestPermissions(new[] { Manifest.Permission.RecordAudio }, PermissionRequestCode);
            return;
        }

        // Check Groq key
        if (!Config.HasGroqKey(this))
        {
            Toast.MakeText(this, GetString(Resource.String.error_no_groq_key), ToastLength.Short)?.Show();
            return;
        }

        // Save current settings before starting
        SaveSettings();

        // Start overlay service
        var serviceIntent = new Intent(this, typeof(OverlayService));
        if (Build.VERSION.SdkInt >= BuildVersionCodes.O)
            StartForegroundService(serviceIntent);
        else
            StartService(serviceIntent);

        _isOverlayRunning = true;
        UpdateStatus();
    }

    private void StopOverlay()
    {
        var serviceIntent = new Intent(this, typeof(OverlayService));
        StopService(serviceIntent);
        _isOverlayRunning = false;
        UpdateStatus();
    }

    private void UpdateStatus()
    {
        if (_statusText == null || _btnToggle == null) return;

        if (_isOverlayRunning)
        {
            _statusText.Text = GetString(Resource.String.status_overlay_running);
            _statusText.SetTextColor(Android.Graphics.Color.ParseColor("#43A047"));
            _btnToggle.Text = GetString(Resource.String.btn_stop_overlay);
            _btnToggle.BackgroundTintList = Android.Content.Res.ColorStateList.ValueOf(
                Android.Graphics.Color.ParseColor("#E53935"));
        }
        else
        {
            _statusText.Text = GetString(Resource.String.status_overlay_stopped);
            _statusText.SetTextColor(Android.Graphics.Color.ParseColor("#E53935"));
            _btnToggle.Text = GetString(Resource.String.btn_start_overlay);
            _btnToggle.BackgroundTintList = Android.Content.Res.ColorStateList.ValueOf(
                Android.Graphics.Color.ParseColor("#43A047"));
        }
    }

    private void RequestPermissions()
    {
        var permsNeeded = new List<string>();

        if (CheckSelfPermission(Manifest.Permission.RecordAudio) != Permission.Granted)
            permsNeeded.Add(Manifest.Permission.RecordAudio);

        if (Build.VERSION.SdkInt >= BuildVersionCodes.Tiramisu &&
            CheckSelfPermission(Manifest.Permission.PostNotifications) != Permission.Granted)
            permsNeeded.Add(Manifest.Permission.PostNotifications);

        if (permsNeeded.Count > 0)
            RequestPermissions(permsNeeded.ToArray(), PermissionRequestCode);
    }

    protected override void OnActivityResult(int requestCode, Result resultCode, Intent? data)
    {
        base.OnActivityResult(requestCode, resultCode, data);

        if (requestCode == OverlayPermissionRequestCode)
        {
            if (Settings.CanDrawOverlays(this))
                StartOverlay();
            else
                Toast.MakeText(this, GetString(Resource.String.error_overlay_permission), ToastLength.Long)?.Show();
        }
    }
}
