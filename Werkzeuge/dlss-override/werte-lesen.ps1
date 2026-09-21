# Shows the DLSS override values and the NVIDIA App "Override Reserved Key" settings per driver profile.
# Read-only, no admin. Without the reserved keys (written only by the NVIDIA App for whitelisted titles)
# driver 616.92 ignores the override (see bugs/gaming/nvidia-dlss-override.md B9).
param([string[]]$Profil = @((([xml](Get-Content "$PSScriptRoot\spiele-dlss.nip" -Raw)).ArrayOfProfile.Profile.ProfileName) + 'Call of Duty: Modern Warfare 2 (2022)'))
Add-Type -TypeDefinition @'
using System; using System.Runtime.InteropServices;
public static class DrsRead {
    [DllImport("nvapi64.dll", EntryPoint = "nvapi_QueryInterface")] static extern IntPtr Q(uint id);
    delegate int Init(); delegate int CreateSession(out IntPtr s); delegate int LoadSettings(IntPtr s);
    delegate int FindProfile(IntPtr s, [MarshalAs(UnmanagedType.LPWStr)] string name, out IntPtr p);
    delegate int GetSetting(IntPtr s, IntPtr p, uint id, IntPtr setting);
    static T F<T>(uint id) { return Marshal.GetDelegateForFunctionPointer<T>(Q(id)); }
    static IntPtr s;
    public static string Get(string name, uint id) {
        if (s == IntPtr.Zero) { F<Init>(0x0150E828)(); F<CreateSession>(0x0694D52E)(out s); F<LoadSettings>(0x375DBD6B)(s); }
        IntPtr p; if (F<FindProfile>(0x7E4A9A0B)(s, name, out p) != 0) return "kein Profil";
        IntPtr b = Marshal.AllocHGlobal(12320); for (int i = 0; i < 12320; i++) Marshal.WriteByte(b, i, 0); Marshal.WriteInt32(b, 0, 12320 | (1 << 16));
        int r = F<GetSetting>(0x73BF8338)(s, p, id, b);
        return r != 0 ? "-" : "0x" + ((uint)Marshal.ReadInt32(b, 8220)).ToString("X");
    }
}
'@
$ids = [ordered]@{ 'SR' = 0x10E41E01; 'RR' = 0x10E41E02; 'FG' = 0x10E41E03; 'SR-Key' = 0x10C7D684; 'RR-Key' = 0x10C7D86C; 'FG-Key' = 0x10C7D57E }
"{0,-40} {1}" -f 'Profil', (($ids.Keys | ForEach-Object { '{0,-11}' -f $_ }) -join '')
foreach ($n in $Profil) { "{0,-40} {1}" -f $n, (($ids.Keys | ForEach-Object { '{0,-11}' -f [DrsRead]::Get($n, [uint32]$ids[$_]) }) -join '') }
