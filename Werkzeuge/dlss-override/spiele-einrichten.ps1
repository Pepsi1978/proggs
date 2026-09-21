# Writes the DLSS driver overrides from spiele-dlss.nip straight into the NVIDIA driver profiles via NVAPI
# (no Profile Inspector needed), then reads every value back. Needs admin (one UAC prompt). No game files are touched.
# MW2 (2022) is intentionally not part of this file, it has its own tool (einrichten.ps1 + mw2-dlss.nip).
param([string]$Nip = "$PSScriptRoot\spiele-dlss.nip")
$log = "$env:TEMP\dlss-spiele-einrichten.log"

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Start-Process powershell -Verb RunAs -Wait -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$PSCommandPath`"",'-Nip',"`"$Nip`"")
    if (Test-Path $log) { Get-Content $log }
    return
}

Add-Type -TypeDefinition @'
using System; using System.Runtime.InteropServices;
public static class DrsWrite {
    [DllImport("nvapi64.dll", EntryPoint = "nvapi_QueryInterface")] static extern IntPtr Q(uint id);
    delegate int Init(); delegate int Session(out IntPtr s); delegate int Handle(IntPtr s);
    delegate int FindProfile(IntPtr s, [MarshalAs(UnmanagedType.LPWStr)] string name, out IntPtr p);
    delegate int FindApp(IntPtr s, [MarshalAs(UnmanagedType.LPWStr)] string name, out IntPtr p, IntPtr app);
    delegate int CreateProfile(IntPtr s, IntPtr info, out IntPtr p);
    delegate int CreateApp(IntPtr s, IntPtr p, IntPtr app);
    delegate int Setting(IntPtr s, IntPtr p, IntPtr setting);
    delegate int GetSetting(IntPtr s, IntPtr p, uint id, IntPtr setting);
    static T F<T>(uint id) { return Marshal.GetDelegateForFunctionPointer<T>(Q(id)); }
    const int AppSize = 4 + 4 + 4 * 4096 + 4 + 4096, ProfSize = 4 + 4096 + 16, SetSize = 12320;
    static IntPtr s;
    static IntPtr Buf(int size, int ver) { IntPtr b = Marshal.AllocHGlobal(size); for (int i = 0; i < size; i++) Marshal.WriteByte(b, i, 0); Marshal.WriteInt32(b, 0, size | (ver << 16)); return b; }
    static void Str(IntPtr b, int off, string v) { var c = v.ToCharArray(); Marshal.Copy(c, 0, b + off, Math.Min(c.Length, 2047)); }
    public static void Open() { F<Init>(0x0150E828)(); F<Session>(0x0694D52E)(out s); F<Handle>(0x375DBD6B)(s); }
    public static int Save() { return F<Handle>(0xFCBC7E14)(s); }
    // Returns the profile handle; creates the profile if missing.
    public static IntPtr Profile(string name, out string note) {
        IntPtr p; int r = F<FindProfile>(0x7E4A9A0B)(s, name, out p);
        if (r == 0) { note = "vorhanden"; return p; }
        IntPtr info = Buf(ProfSize, 1); Str(info, 4, name);
        r = F<CreateProfile>(0xCC176068)(s, info, out p);
        note = r == 0 ? "neu angelegt" : "Anlegen fehlgeschlagen (" + r + ")"; return r == 0 ? p : IntPtr.Zero;
    }
    public static int BindExe(IntPtr p, string exe) {
        IntPtr found; IntPtr probe = Buf(AppSize, 4);
        if (F<FindApp>(0xEEE566B2)(s, exe, out found, probe) == 0) return found == p ? 1 : -1; // 1 = already bound here, -1 = bound elsewhere
        IntPtr app = Buf(AppSize, 4); Str(app, 8, exe);
        return F<CreateApp>(0x4347A9DE)(s, p, app);
    }
    public static int Set(IntPtr p, uint id, uint value) {
        IntPtr b = Buf(SetSize, 1); Marshal.WriteInt32(b, 4100, (int)id); Marshal.WriteInt32(b, 8220, (int)value);
        return F<Setting>(0x577DD202)(s, p, b);
    }
    public static string Get(IntPtr p, uint id) {
        IntPtr b = Buf(SetSize, 1); int r = F<GetSetting>(0x73BF8338)(s, p, id, b);
        return r != 0 ? "fehlt" : "0x" + ((uint)Marshal.ReadInt32(b, 8220)).ToString("X");
    }
}
'@

Start-Transcript -Path $log -Force | Out-Null
try {
    [DrsWrite]::Open()
    $profiles = ([xml](Get-Content $Nip -Raw)).ArrayOfProfile.Profile
    $handles = @{}
    foreach ($pr in $profiles) {
        $note = ''; $h = [DrsWrite]::Profile($pr.ProfileName, [ref]$note)
        "PROFIL $($pr.ProfileName): $note"
        if ($h -eq [IntPtr]::Zero) { continue }
        $handles[$pr.ProfileName] = $h
        foreach ($exe in @($pr.Executeables.string | Where-Object { $_ })) { "  Exe $exe binden: " + [DrsWrite]::BindExe($h, $exe) }
        foreach ($st in $pr.Settings.ProfileSetting) { "  {0} = {1}: rc {2}" -f $st.SettingNameInfo, $st.SettingValue, [DrsWrite]::Set($h, [uint32]$st.SettingID, [uint32]$st.SettingValue) }
    }
    "SPEICHERN: rc " + [DrsWrite]::Save()

    # Read back from a fresh session so we see what the driver really stored.
    [DrsWrite]::Open()
    $bad = 0
    foreach ($pr in $profiles) {
        $note = ''; $h = [DrsWrite]::Profile($pr.ProfileName, [ref]$note)
        $vals = foreach ($st in $pr.Settings.ProfileSetting) { $got = [DrsWrite]::Get($h, [uint32]$st.SettingID); if ($got -ne ('0x{0:X}' -f [uint32]$st.SettingValue)) { $bad++ }; "$($st.SettingNameInfo -replace '^DLSS-?','' -replace ' - ',' ')=$got" }
        "{0,-40} {1}" -f $pr.ProfileName, ($vals -join ' | ')
    }
    "VERIFIKATION: " + $(if ($bad -eq 0) { 'OK, alle Werte gesetzt' } else { "$bad Werte weichen ab" })
} catch { "FEHLER: $_" } finally { Stop-Transcript | Out-Null }
