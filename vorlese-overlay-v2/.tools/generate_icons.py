#!/usr/bin/env python3
"""Generate the Vorlese-Overlay toolbar icons (16/48/128 px) using only the
Python standard library. A white "volume_up" speaker (cone + two sound waves,
matching icons/speaker.svg) on a rounded teal square, so it's instantly
recognizable in the Chrome toolbar.

Rendered at 4x and box-averaged down for anti-aliased edges.

This script lives in .tools/ on purpose: Chrome ignores "."-prefixed folders
when loading an unpacked extension, so any __pycache__ produced here can never
break loading. (.tools, not .build, because the global gitignore ignores .build/.)

Run: python .tools/generate_icons.py   (writes into ../icons)
"""

import math
import os
import struct
import zlib

BG = (249, 115, 22)       # #F97316 orange — Frank uses orange for speaker/audio
WHITE = (255, 255, 255)
SCALE = 4                 # supersampling factor for anti-aliasing


def in_rounded(nx, ny, r=0.20):
    inx = nx < r or nx > 1 - r
    iny = ny < r or ny > 1 - r
    if inx and iny:
        cx = r if nx < r else 1 - r
        cy = r if ny < r else 1 - r
        return (nx - cx) ** 2 + (ny - cy) ** 2 <= r * r
    return True


def in_speaker(nx, ny):
    cy = 0.5
    # back box of the speaker
    if 0.17 <= nx <= 0.31 and 0.40 <= ny <= 0.60:
        return True
    # cone: trapezoid widening from the box to the apex
    if 0.31 <= nx <= 0.50:
        t = (nx - 0.31) / (0.50 - 0.31)
        half = 0.10 + t * (0.33 - 0.10)
        if abs(ny - cy) <= half:
            return True
    # two sound-wave arcs to the right
    dx = nx - 0.40
    dy = ny - 0.50
    if dx > 0.0 and abs(dy) <= dx * 1.25:
        d = math.hypot(dx, dy)
        if 0.205 <= d <= 0.25:   # inner wave
            return True
        if 0.305 <= d <= 0.35:   # outer wave
            return True
    return False


def pixel(nx, ny):
    if not in_rounded(nx, ny):
        return (0, 0, 0, 0)
    if in_speaker(nx, ny):
        return (WHITE[0], WHITE[1], WHITE[2], 255)
    return (BG[0], BG[1], BG[2], 255)


def _png_chunk(tag, data):
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


def make_icon(path, s):
    big = s * SCALE
    # Render the supersampled image as a flat list of RGBA tuples.
    hi = [pixel((x + 0.5) / big, (y + 0.5) / big) for y in range(big) for x in range(big)]

    raw = bytearray()
    n = SCALE * SCALE
    for ty in range(s):
        raw.append(0)  # PNG filter type 0 (none)
        for tx in range(s):
            r = g = b = a = 0
            for dy in range(SCALE):
                for dx in range(SCALE):
                    c = hi[(ty * SCALE + dy) * big + (tx * SCALE + dx)]
                    al = c[3]
                    r += c[0] * al
                    g += c[1] * al
                    b += c[2] * al
                    a += al
            A = a // n
            if a > 0:
                raw += bytes([r // a, g // a, b // a, A])
            else:
                raw += bytes([0, 0, 0, 0])

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", s, s, 8, 6, 0, 0, 0)  # 8-bit RGBA
    idat = zlib.compress(bytes(raw), 9)
    with open(path, "wb") as f:
        f.write(sig)
        f.write(_png_chunk(b"IHDR", ihdr))
        f.write(_png_chunk(b"IDAT", idat))
        f.write(_png_chunk(b"IEND", b""))


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    base = os.path.join(root, "icons")
    os.makedirs(base, exist_ok=True)
    for size in (16, 48, 128):
        make_icon(os.path.join(base, f"icon{size}.png"), size)
        print(f"wrote icons/icon{size}.png")


if __name__ == "__main__":
    main()
