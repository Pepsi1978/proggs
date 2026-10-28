# Virtueller Bildschirm über den Parsec-Treiber (Parsec Virtual Display Adapter, einmalig: einrichten.ps1). Bei
# zugeklapptem Laptop schaltet Windows den eingebauten Bildschirm ab, Desktop Duplication liefert dann nichts
# („access denied“). Wie am Mac hängt die Fernsteuerung dann einen virtuellen Bildschirm an. Der Treiber hält ihn nur,
# solange alle 100 ms ein Lebenszeichen kommt: Endet die Sitzung oder stürzt der Dienst ab, verschwindet er von selbst,
# und beim Aufklappen landen keine Fenster auf einem unsichtbaren Bildschirm.
# Steuerung nach nomi-san/parsec-vdd (core/parsec-vdd.h), dort sind die IOCTL-Codes dokumentiert.

import ctypes
import ctypes.wintypes as wt
import threading
import time

IOCTL_ADD = 0x0022E004
IOCTL_REMOVE = 0x0022A008
IOCTL_UPDATE = 0x0022A00C

DIGCF_PRESENT = 0x02
DIGCF_DEVICEINTERFACE = 0x10
INVALID = ctypes.c_void_p(-1).value

setupapi = ctypes.WinDLL("setupapi", use_last_error=True)
kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)


class GUID(ctypes.Structure):
    _fields_ = [("d1", wt.DWORD), ("d2", wt.WORD), ("d3", wt.WORD), ("d4", ctypes.c_ubyte * 8)]


class SP_DEVICE_INTERFACE_DATA(ctypes.Structure):
    _fields_ = [("cbSize", wt.DWORD), ("guid", GUID), ("flags", wt.DWORD), ("reserved", ctypes.c_void_p)]


class OVERLAPPED(ctypes.Structure):
    _fields_ = [("Internal", ctypes.c_void_p), ("InternalHigh", ctypes.c_void_p), ("Offset", wt.DWORD),
                ("OffsetHigh", wt.DWORD), ("hEvent", wt.HANDLE)]


ADAPTER = GUID(0x00B41627, 0x04C4, 0x429E, (ctypes.c_ubyte * 8)(0xA2, 0x6E, 0x02, 0x65, 0xCF, 0x50, 0xC8, 0xFA))

setupapi.SetupDiGetClassDevsW.restype = ctypes.c_void_p
setupapi.SetupDiGetClassDevsW.argtypes = [ctypes.POINTER(GUID), wt.LPCWSTR, wt.HWND, wt.DWORD]
setupapi.SetupDiEnumDeviceInterfaces.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.POINTER(GUID), wt.DWORD,
                                                  ctypes.POINTER(SP_DEVICE_INTERFACE_DATA)]
setupapi.SetupDiGetDeviceInterfaceDetailW.argtypes = [ctypes.c_void_p, ctypes.POINTER(SP_DEVICE_INTERFACE_DATA),
                                                       ctypes.c_void_p, wt.DWORD, ctypes.POINTER(wt.DWORD), ctypes.c_void_p]
setupapi.SetupDiDestroyDeviceInfoList.argtypes = [ctypes.c_void_p]
kernel32.CreateFileW.restype = wt.HANDLE
kernel32.CreateFileW.argtypes = [wt.LPCWSTR, wt.DWORD, wt.DWORD, ctypes.c_void_p, wt.DWORD, wt.DWORD, wt.HANDLE]
kernel32.CreateEventW.restype = wt.HANDLE
kernel32.DeviceIoControl.argtypes = [wt.HANDLE, wt.DWORD, ctypes.c_void_p, wt.DWORD, ctypes.c_void_p, wt.DWORD,
                                     ctypes.POINTER(wt.DWORD), ctypes.POINTER(OVERLAPPED)]
kernel32.GetOverlappedResultEx.argtypes = [wt.HANDLE, ctypes.POINTER(OVERLAPPED), ctypes.POINTER(wt.DWORD), wt.DWORD, wt.BOOL]
kernel32.CloseHandle.argtypes = [wt.HANDLE]


def _oeffnen():
    info = setupapi.SetupDiGetClassDevsW(ctypes.byref(ADAPTER), None, None, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE)
    if not info or info == INVALID:
        return None
    try:
        daten = SP_DEVICE_INTERFACE_DATA(cbSize=ctypes.sizeof(SP_DEVICE_INTERFACE_DATA))
        i = 0
        while setupapi.SetupDiEnumDeviceInterfaces(info, None, ctypes.byref(ADAPTER), i, ctypes.byref(daten)):
            i += 1
            groesse = wt.DWORD()
            setupapi.SetupDiGetDeviceInterfaceDetailW(info, ctypes.byref(daten), None, 0, ctypes.byref(groesse), None)
            puffer = ctypes.create_string_buffer(groesse.value)
            # cbSize der festen Struktur: 8 unter 64 Bit (DWORD + WCHAR[1], ausgerichtet).
            ctypes.c_uint32.from_buffer(puffer).value = 8 if ctypes.sizeof(ctypes.c_void_p) == 8 else 6
            if not setupapi.SetupDiGetDeviceInterfaceDetailW(info, ctypes.byref(daten), puffer, groesse, None, None):
                continue
            pfad = ctypes.wstring_at(ctypes.addressof(puffer) + 4)
            h = kernel32.CreateFileW(pfad, 0xC0000000, 3, None, 3, 0x80 | 0x20000000 | 0x40000000 | 0x80000000, None)
            if h and h != INVALID:
                return h
        return None
    finally:
        setupapi.SetupDiDestroyDeviceInfoList(info)


def _ioctl(h, code, daten=b""):
    ein = ctypes.create_string_buffer(daten.ljust(32, b"\0")[:32], 32)
    aus = wt.DWORD(0)
    ov = OVERLAPPED(hEvent=kernel32.CreateEventW(None, True, False, None))
    try:
        kernel32.DeviceIoControl(h, code, ein, 32, ctypes.byref(aus), 4, None, ctypes.byref(ov))
        n = wt.DWORD()
        if not kernel32.GetOverlappedResultEx(h, ctypes.byref(ov), ctypes.byref(n), 5000, False):
            return -1
        return aus.value
    finally:
        kernel32.CloseHandle(ov.hEvent)


class VirtuellerBildschirm:
    """an() hängt einen virtuellen Bildschirm an und hält ihn per Lebenszeichen, aus() nimmt ihn wieder weg."""

    def __init__(self, log=lambda s: None):
        self.log = log
        self.h = None
        self.index = None
        self.sperre = threading.Lock()
        threading.Thread(target=self._puls, daemon=True, name="vdd-puls").start()

    @property
    def aktiv(self):
        return self.index is not None

    def an(self) -> bool:
        with self.sperre:
            if self.index is not None:
                return True
            if self.h is None:
                self.h = _oeffnen()
                if self.h is None:
                    self.log("Virtueller Bildschirm: Parsec-Treiber nicht gefunden (einrichten.ps1 ausführen)")
                    return False
            i = _ioctl(self.h, IOCTL_ADD)
            _ioctl(self.h, IOCTL_UPDATE)
            if i < 0 or i > 0xFFFF:
                self.log(f"Virtueller Bildschirm: Anlegen gescheitert ({i})")
                return False
            self.index = i
            self.log(f"Virtueller Bildschirm an (Index {i})")
            return True

    def aus(self):
        with self.sperre:
            if self.index is None:
                return
            i, self.index = self.index, None
            _ioctl(self.h, IOCTL_REMOVE, bytes([(i >> 8) & 0xFF, i & 0xFF]))
            _ioctl(self.h, IOCTL_UPDATE)
            self.log("Virtueller Bildschirm aus")

    def _puls(self):
        while True:
            time.sleep(0.05)
            if self.index is not None and self.h is not None:
                with self.sperre:
                    if self.index is not None:
                        _ioctl(self.h, IOCTL_UPDATE)


if __name__ == "__main__":
    import sys
    v = VirtuellerBildschirm(lambda s: print(s, flush=True))
    print("an:", v.an(), flush=True)
    time.sleep(float(sys.argv[1]) if len(sys.argv) > 1 else 8)
    v.aus()
