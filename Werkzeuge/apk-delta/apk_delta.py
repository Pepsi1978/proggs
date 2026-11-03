#!/usr/bin/env python3
"""Delta zwischen zwei APKs für die Übertragung durch den langsamen Tunnel.

Aufruf: apk_delta.py <alt.apk> <neu.apk> <ausgabeordner>
Schreibt <ausgabeordner>/patch.bin (nur neue Bytes) und <ausgabeordner>/plan.sh. Am Handy setzt
`sh plan.sh <alt.apk> <patch.bin> <ziel.apk>` die neue APK byte-genau zusammen: unveränderte Einträge
(Dex, native Bibliotheken, Modelle) kommen per dd aus der dort installierten APK, alles andere aus patch.bin.
Ob das Ergebnis stimmt, prüft der Aufrufer per SHA-256; bei Abweichung überträgt er die ganze APK.
Letzte Zeile auf stdout: DELTA <patch-bytes> <neu-bytes> <operationen>
"""
import hashlib, os, struct, sys, zipfile


def datenbereiche(pfad):
    """(Daten-Start, Länge, Schlüssel) je Eintrag; Schlüssel = Inhalt der komprimierten Bytes."""
    erg = []
    with zipfile.ZipFile(pfad) as z, open(pfad, 'rb') as f:
        for info in z.infolist():
            f.seek(info.header_offset)
            kopf = f.read(30)
            if kopf[:4] != b'PK\x03\x04':
                raise ValueError('kaputter lokaler Kopf: ' + info.filename)
            n, e = struct.unpack('<HH', kopf[26:30])
            start = info.header_offset + 30 + n + e
            laenge = info.compress_size
            if laenge < 4096:
                continue  # kleine Einträge lohnen keinen eigenen dd-Aufruf
            f.seek(start)
            h = hashlib.sha256(f.read(laenge)).digest()
            erg.append((start, laenge, h))
    return erg


def main():
    alt, neu, aus = sys.argv[1:4]
    os.makedirs(aus, exist_ok=True)
    alt_index = {}
    for start, laenge, h in datenbereiche(alt):
        alt_index.setdefault((laenge, h), start)
    with open(neu, 'rb') as f:
        neu_bytes = f.read()
    with open(alt, 'rb') as f:
        alt_bytes = f.read()
    # Kopier-Abschnitte (neu_start, länge, alt_start), nach Position in der neuen APK.
    kopien = sorted((s, l, alt_index[(l, h)]) for s, l, h in datenbereiche(neu) if (l, h) in alt_index)
    ops = []  # ('c', alt_start, länge) oder ('l', patch_start, länge)
    patch = bytearray()
    pos = 0

    def literal(bis):
        nonlocal pos
        if bis <= pos:
            return
        stueck = neu_bytes[pos:bis]
        # Steht das Stück direkt vor/nach der letzten Kopie auch so in der alten APK, wird die Kopie verlängert.
        if ops and ops[-1][0] == 'c':
            _, a, l = ops[-1]
            if alt_bytes[a + l:a + l + len(stueck)] == stueck:
                ops[-1] = ('c', a, l + len(stueck))
                pos = bis
                return
        if ops and ops[-1][0] == 'l':
            ops[-1] = ('l', ops[-1][1], ops[-1][2] + len(stueck))
        else:
            ops.append(('l', len(patch), len(stueck)))
        patch.extend(stueck)
        pos = bis

    for s, l, a in kopien:
        literal(s)
        if ops and ops[-1][0] == 'c' and ops[-1][1] + ops[-1][2] == a:
            ops[-1] = ('c', ops[-1][1], ops[-1][2] + l)
        else:
            ops.append(('c', a, l))
        pos = s + l
    literal(len(neu_bytes))

    with open(os.path.join(aus, 'patch.bin'), 'wb') as f:
        f.write(patch)
    zeilen = ['#!/system/bin/sh', '# Erzeugt von apk_delta.py: $1 alte APK, $2 patch.bin, $3 Ziel', 'set -e',
              'A="$1"; P="$2"; Z="$3"', ': > "$Z"']
    for art, start, laenge in ops:
        quelle = '"$A"' if art == 'c' else '"$P"'
        zeilen.append(f'dd if={quelle} bs=1048576 iflag=skip_bytes,count_bytes skip={start} count={laenge} status=none >> "$Z"')
    with open(os.path.join(aus, 'plan.sh'), 'w') as f:
        f.write('\n'.join(zeilen) + '\n')
    print(f'DELTA {len(patch)} {len(neu_bytes)} {len(ops)}')


if __name__ == '__main__':
    main()
