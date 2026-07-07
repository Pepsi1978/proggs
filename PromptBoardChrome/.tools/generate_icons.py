#!/usr/bin/env python3
"""Generate the Prompt Board toolbar icons (16/48/128 px) using only the
Python standard library. The icon is a rounded purple square with three white
bars that read as a list / prompt board, matching the in-panel SVG logo.

This script lives in a dot-prefixed folder (.tools) ON PURPOSE: Chrome ignores
any file/directory whose name starts with "." when loading an unpacked
extension, but REJECTS the whole extension if any name starts with "_".
Python tooling produces __pycache__ (underscore) when compiled, so keeping the
script outside the Chrome-scanned tree means that cache can never break loading.
(.tools rather than .build because the global gitignore ignores .build/, and we
want this tooling tracked in the repo.)

Run: python .tools/generate_icons.py   (writes into ../icons)
"""

import os
import struct
import zlib

BG = (124, 92, 231, 255)      # #7C5CE7
WHITE = (255, 255, 255, 255)
TRANSPARENT = (0, 0, 0, 0)


def _in_rounded(x, y, s, r):
    """True if pixel center is inside the rounded-rect silhouette."""
    px, py = x + 0.5, y + 0.5
    in_x = px < r or px > s - r
    in_y = py < r or py > s - r
    if in_x and in_y:
        cx = r if px < r else s - r
        cy = r if py < r else s - r
        return (px - cx) ** 2 + (py - cy) ** 2 <= r * r
    return True


def _in_bar(x, y, s):
    margin = int(s * 0.20)
    bar_h = max(2, int(s * 0.11))
    rows = [int(s * 0.27), int(s * 0.46), int(s * 0.65)]
    widths = [s - 2 * margin, s - 2 * margin, int((s - 2 * margin) * 0.6)]
    for by, w in zip(rows, widths):
        if by <= y < by + bar_h and margin <= x < margin + w:
            return True
    return False


def _png_chunk(tag, data):
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


def make_icon(path, s):
    radius = s * 0.22
    raw = bytearray()
    for y in range(s):
        raw.append(0)  # PNG filter type 0 (none) per scanline
        for x in range(s):
            if not _in_rounded(x, y, s, radius):
                color = TRANSPARENT
            elif _in_bar(x, y, s):
                color = WHITE
            else:
                color = BG
            raw += bytes(color)

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", s, s, 8, 6, 0, 0, 0)  # 8-bit RGBA
    idat = zlib.compress(bytes(raw), 9)
    with open(path, "wb") as f:
        f.write(sig)
        f.write(_png_chunk(b"IHDR", ihdr))
        f.write(_png_chunk(b"IDAT", idat))
        f.write(_png_chunk(b"IEND", b""))


def main():
    # Write into <extension-root>/icons, i.e. one level up from this .tools dir.
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    base = os.path.join(root, "icons")
    os.makedirs(base, exist_ok=True)
    for size in (16, 48, 128):
        make_icon(os.path.join(base, f"icon{size}.png"), size)
        print(f"wrote icons/icon{size}.png")


if __name__ == "__main__":
    main()
