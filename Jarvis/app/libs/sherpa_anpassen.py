"""Passt eine offizielle sherpa-onnx-AAR an die ONNX Runtime von Microsoft an (siehe LIESMICH.md).

1. Entfernt jni/*/libonnxruntime.so: Jarvis lädt nur die eine, neueste ORT von Microsoft.
2. Trägt in libsherpa-onnx-jni.so die Symbolversion dieser ORT ein. ORT exportiert ihren einzigen
   Einstiegspunkt OrtGetApiBase mit einer Versionsmarke (VERS_1.28.2 usw.); sherpa verlangt sonst genau
   die ORT, gegen die es gebaut ist, und der Linker lehnt jede andere ab. Alles Weitere holt sherpa über
   OrtGetApiBase()->GetApi(<API-Nummer>), und das bedient jede neuere ORT (abwärtskompatibel).
   Geändert werden nur Name und ELF-Hash des Versionsbedarfs, wie ein Neubau gegen die neue ORT.

Aufruf: python sherpa_anpassen.py <sherpa-onnx-X.aar> <onnxruntime-android-Y.aar> <ziel.aar>
"""
import re
import struct
import sys
import zipfile

SHT_GNU_VERNEED = 0x6FFFFFFE


def elf_hash(name: bytes) -> int:
    h = 0
    for c in name:
        h = ((h << 4) + c) & 0xFFFFFFFF
        g = h & 0xF0000000
        if g:
            h ^= g >> 24
        h &= ~g & 0xFFFFFFFF
    return h


def ort_version(ort_so: bytes) -> bytes:
    marken = set(re.findall(rb"VERS_\d+\.\d+\.\d+", ort_so))
    if len(marken) != 1:
        raise SystemExit(f"ORT-Versionsmarke nicht eindeutig: {marken}")
    return marken.pop()


def versionsbedarf_ersetzen(elf: bytearray, neu: bytes) -> bytes:
    if elf[:4] != b"\x7fELF":
        raise SystemExit("keine ELF-Datei")
    is64 = elf[4] == 2
    if is64:
        shoff, = struct.unpack_from("<Q", elf, 0x28)
        shentsize, shnum = struct.unpack_from("<HH", elf, 0x3A)
    else:
        shoff, = struct.unpack_from("<I", elf, 0x20)
        shentsize, shnum = struct.unpack_from("<HH", elf, 0x2E)

    def abschnitt(i):
        o = shoff + i * shentsize
        if is64:
            _, typ, _, _, off, size, link = struct.unpack_from("<IIQQQQI", elf, o)
        else:
            _, typ, _, _, off, size, link = struct.unpack_from("<IIIIIII", elf, o)
        return typ, off, size, link

    alt = None
    for i in range(shnum):
        typ, off, size, link = abschnitt(i)
        if typ != SHT_GNU_VERNEED:
            continue
        _, stroff, _, _ = abschnitt(link)
        vn = off
        while True:
            _, cnt, datei, aux, naechst = struct.unpack_from("<HHIII", elf, vn)
            dateiname = elf[stroff + datei: elf.index(b"\0", stroff + datei)]
            va = vn + aux
            for _ in range(cnt):
                _, flags, other, name, va_naechst = struct.unpack_from("<IHHII", elf, va)
                pos = stroff + name
                ende = elf.index(b"\0", pos)
                marke = bytes(elf[pos:ende])
                if dateiname == b"libonnxruntime.so" and marke.startswith(b"VERS_"):
                    if len(marke) != len(neu):
                        raise SystemExit(f"{marke} und {neu} sind verschieden lang, bitte Skript erweitern")
                    elf[pos:ende] = neu
                    struct.pack_into("<I", elf, va, elf_hash(neu))
                    alt = marke
                if not va_naechst:
                    break
                va += va_naechst
            if not naechst:
                break
            vn += naechst
    return alt


def main(sherpa_aar, ort_aar, ziel):
    with zipfile.ZipFile(ort_aar) as z:
        neu = ort_version(z.read("jni/arm64-v8a/libonnxruntime.so"))
    with zipfile.ZipFile(sherpa_aar) as quelle, zipfile.ZipFile(ziel, "w", zipfile.ZIP_DEFLATED) as aus:
        for e in quelle.infolist():
            if re.fullmatch(r"jni/[^/]+/libonnxruntime\.so", e.filename):
                print("entfernt:", e.filename)
                continue
            daten = quelle.read(e.filename)
            if re.fullmatch(r"jni/[^/]+/libsherpa-onnx-[^/]+\.so", e.filename):
                elf = bytearray(daten)
                alt = versionsbedarf_ersetzen(elf, neu)
                if alt is not None:
                    print(f"{e.filename}: {alt.decode()} -> {neu.decode()}")
                    daten = bytes(elf)
                elif e.filename.endswith("-jni.so"):
                    raise SystemExit(f"{e.filename}: kein Versionsbedarf an libonnxruntime.so gefunden")
            aus.writestr(e, daten)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    main(*sys.argv[1:])
