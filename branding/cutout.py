"""Lifts the mole out of its dirt-block tile, leaving a transparent background.

Colour alone cannot do this: the gold of the helmet is also the gold of the ore
specks in the dirt. So the background is found by flooding inward from the edges
and stopping at the mole's black outline, which encloses it completely.
"""
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import read_png

OUTLINE = (0x1A, 0x0F, 0x08)
# Everything the dirt tile is made of: its shades and the ore specks sitting in it.
TILE = {
    (0x5A, 0x3A, 0x20), (0x6A, 0x46, 0x28), (0x8A, 0x64, 0x38),
    (0x4F, 0xC9, 0xDE), (0x8C, 0x94, 0x9A), (0xFF, 0xC9, 0x3C), (0xD1, 0x8A, 0x12),
}


def cut(rows):
    h, w = len(rows), len(rows[0])
    clear = [[False] * w for _ in range(h)]
    stack = [(x, y) for x in range(w) for y in (0, h - 1)]
    stack += [(x, y) for y in range(h) for x in (0, w - 1)]

    while stack:
        x, y = stack.pop()
        if x < 0 or y < 0 or x >= w or y >= h or clear[y][x]:
            continue
        colour = rows[y][x][:3]
        # The outline is the wall: the fill never reaches the mole through it.
        if colour == OUTLINE or colour not in TILE:
            continue
        clear[y][x] = True
        stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]

    return [[(0, 0, 0, 0) if clear[y][x] else rows[y][x] for x in range(w)] for y in range(h)]


def chunk(tag, data):
    out = struct.pack(">I", len(data)) + tag + data
    return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def save(path, px, scale=1):
    h, w = len(px) * scale, len(px[0]) * scale
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        row = px[y // scale]
        for x in range(w):
            raw.extend(row[x // scale])
    with open(path, "wb") as f:
        f.write(bytes([137, 80, 78, 71, 13, 10, 26, 10])
                + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
                + chunk(b"IEND", b""))


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    rows = read_png.read(os.path.join(here, "icon-32.png"))
    cutout = cut(rows)
    kept = sum(1 for r in cutout for p in r if p[3])
    save(os.path.join(here, "mole-cutout.png"), cutout, scale=8)
    print("kept", kept, "of", len(rows) * len(rows[0]), "pixels")
