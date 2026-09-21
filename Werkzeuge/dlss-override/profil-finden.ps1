# Looks up which NVIDIA driver profile (DRS) an exe belongs to, via NVAPI. No admin needed, read-only.
# Usage: .\profil-finden.ps1 'D:\...\bf6.exe' 'Cyberpunk2077.exe'
param([Parameter(Mandatory)][string[]]$Exe)
Add-Type -TypeDefinition @'
using System; using System.Runtime.InteropServices;
public static class Drs {
    [DllImport("nvapi64.dll", EntryPoint = "nvapi_QueryInterface")] static extern IntPtr Q(uint id);
    delegate int Init();
    delegate int CreateSession(out IntPtr s);
    delegate int LoadSettings(IntPtr s);
    delegate int FindApp(IntPtr s, [MarshalAs(UnmanagedType.LPWStr)] string name, out IntPtr p, IntPtr app);
    delegate int ProfileInfo(IntPtr s, IntPtr p, IntPtr info);
    static T F<T>(uint id) { return Marshal.GetDelegateForFunctionPointer<T>(Q(id)); }
    public static string Lookup(string exe) {
        F<Init>(0x0150E828)();
        IntPtr s; F<CreateSession>(0x0694D52E)(out s); F<LoadSettings>(0x375DBD6B)(s);
        const int appSize = 4 + 4 + 4 * 4096 + 4 + 4096; // NVDRS_APPLICATION_V4
        IntPtr app = Marshal.AllocHGlobal(appSize); for (int i = 0; i < appSize; i++) Marshal.WriteByte(app, i, 0);
        Marshal.WriteInt32(app, 0, appSize | (4 << 16));
        IntPtr p; int r = F<FindApp>(0xEEE566B2)(s, exe, out p, app);
        if (r != 0) return "(kein Profil, Code " + r + ")";
        const int profSize = 4 + 4096 + 16; // NVDRS_PROFILE_V1
        IntPtr info = Marshal.AllocHGlobal(profSize); for (int i = 0; i < profSize; i++) Marshal.WriteByte(info, i, 0);
        Marshal.WriteInt32(info, 0, profSize | (1 << 16));
        F<ProfileInfo>(0x61CD6FD6)(s, p, info);
        return Marshal.PtrToStringUni(info + 4) + (Marshal.ReadInt32(info, 4 + 4096 + 4) == 1 ? " [vordefiniert]" : " [eigenes]");
    }
}
'@
foreach ($e in $Exe) { "{0} -> {1}" -f $e, [Drs]::Lookup($e) }
