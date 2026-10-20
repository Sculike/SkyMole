"""Builds the SkyMole banner: icon, wordmark, subtitle, and a Dwarven Mines scene."""
import os
import struct
import zlib

import pixelfont
import read_png

W, H = 620, 140
GROUND = 120

VOID = (0x0D, 0x09, 0x06)
STONE_A = (0x12, 0x0C, 0x08)
STONE_B = (0x18, 0x11, 0x0B)

LAMP_GOLD = (0xFF, 0xC9, 0x3C)
LAMP_CORE = (0xFF, 0xF0, 0xB8)
MITHRIL = (0x4F, 0xC9, 0xDE)
TITANIUM = (0xC3, 0xCB, 0xD2)
CREAM = (0xF5, 0xEF, 0xE6)
DIM = (0xA8, 0x9A, 0x88)
SHADOW = (0x08, 0x05, 0x03)

ROCK = (0x5A, 0x50, 0x48)
ROCK_TOP = (0x74, 0x69, 0x5F)
ROCK_DARK = (0x3A, 0x33, 0x2D)
DIRT = (0x5C, 0x3A, 0x22)
DIRT_TOP = (0x7A, 0x4E, 0x2F)
POST = (0x2A, 0x20, 0x18)


def blank():
    return [[VOID for _ in range(W)] for _ in range(H)]


def rect(px, x, y, w, h, color):
    for yy in range(max(0, y), min(H, y + h)):
        row = px[yy]
        for xx in range(max(0, x), min(W, x + w)):
            row[xx] = color


def backdrop(px):
    pattern = [0, 1, 0, 2, 1, 0, 2, 0, 1, 2, 0, 1, 0, 2, 1, 0]
    shades = [VOID, STONE_A, STONE_B]
    for ty in range(0, GROUND, 20):
        for tx in range(0, W, 20):
            shade = shades[pattern[((tx // 20) + (ty // 20) * 3) % len(pattern)]]
            rect(px, tx, ty, 20, 20, shade)


def particles(px):
    spots = [(150, 8, 4, LAMP_GOLD), (300, 6, 3, MITHRIL), (420, 10, 3, TITANIUM),
             (236, 12, 3, LAMP_GOLD), (364, 8, 3, MITHRIL), (90, 8, 3, TITANIUM),
             (516, 16, 4, TITANIUM), (584, 30, 3, MITHRIL), (608, 58, 3, LAMP_GOLD),
             (506, 70, 3, LAMP_GOLD), (6, 22, 3, MITHRIL), (8, 104, 3, LAMP_GOLD),
             (612, 14, 3, LAMP_GOLD), (552, 48, 3, TITANIUM)]
    for x, y, size, color in spots:
        rect(px, x, y, size, size, color)


def ground(px):
    kinds = [0, 0, 1, 0, 2, 0, 0, 3, 0, 1, 0, 0, 2, 0, 0, 1,
             0, 3, 0, 0, 1, 0, 2, 0, 0, 0, 1, 0, 3, 0, 0]
    for i, kind in enumerate(kinds):
        x = i * 20
        top, body = (ROCK_TOP, ROCK) if kind != 1 else (DIRT_TOP, DIRT)
        rect(px, x, GROUND, 20, 4, top)
        rect(px, x, GROUND + 4, 20, H - GROUND - 4, body)
        if kind == 2:
            for ox, oy in ((4, 8), (12, 6), (8, 14), (14, 15)):
                rect(px, x + ox, GROUND + oy, 3, 3, MITHRIL)
        if kind == 3:
            for ox, oy in ((5, 7), (13, 10), (9, 15)):
                rect(px, x + ox, GROUND + oy, 3, 3, TITANIUM)
        rect(px, x, GROUND, 1, H - GROUND, ROCK_DARK)


def lamp(px, x, top):
    """A Dwarven Mines lamp post: dark post, gold housing, bright core."""
    rect(px, x + 5, top + 16, 6, GROUND - top - 16, POST)
    rect(px, x, top, 16, 16, POST)
    rect(px, x + 2, top + 2, 12, 12, LAMP_GOLD)
    rect(px, x + 5, top + 5, 6, 6, LAMP_CORE)
    rect(px, x + 2, top + 14, 12, 2, POST)


def ore_cluster(px, x):
    rect(px, x, GROUND - 16, 40, 16, ROCK)
    rect(px, x, GROUND - 16, 40, 3, ROCK_TOP)
    rect(px, x + 8, GROUND - 26, 22, 10, ROCK)
    rect(px, x + 8, GROUND - 26, 22, 3, ROCK_TOP)
    for ox, oy in ((6, -12), (18, -22), (28, -8), (14, -6), (24, -18)):
        rect(px, x + ox, GROUND + oy, 4, 4, MITHRIL)


def icon(px, path, x, y, scale):
    rows = read_png.read(path)
    for gy, row in enumerate(rows):
        for gx, (r, g, b, a) in enumerate(row):
            if a:
                rect(px, x + gx * scale, y + gy * scale, scale, scale, (r, g, b))


def text(px, s, x, y, scale, color, shadow=True):
    def put(bx, by, bw, bh, c):
        rect(px, bx, by, bw, bh, c)

    if shadow:
        off = max(2, scale // 3)
        pixelfont.draw(lambda bx, by, bw, bh, _c: put(bx + off, by + off, bw, bh, SHADOW),
                       s, x, y, scale, SHADOW)
    pixelfont.draw(put, s, x, y, scale, color)


def chunk(tag, data):
    out = struct.pack(">I", len(data)) + tag + data
    return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def save(path, px, scale):
    w, h = W * scale, H * scale
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        row = px[y // scale]
        for x in range(w):
            raw.extend(row[x // scale])
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
                + chunk(b"IEND", b""))


def build(icon_path, title, subtitle, title_color, subtitle_color):
    px = blank()
    backdrop(px)
    particles(px)
    lamp(px, 532, 62)
    ore_cluster(px, 560)
    ground(px)
    icon(px, icon_path, 18, 38, 2)
    text(px, title, 100, 24, 9, title_color)
    text(px, subtitle, 100, 100, 2, subtitle_color, shadow=False)
    return px


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    src = os.path.join(here, "icon-32.png")
    sub = "A MINING MOD FOR HYPIXEL SKYBLOCK"
    save(os.path.join(here, "banner.png"),
         build(src, "SKYMOLE", sub, CREAM, LAMP_GOLD), 3)
    save(os.path.join(here, "banner-gold.png"),
         build(src, "SKYMOLE", sub, LAMP_GOLD, DIM), 3)
    print("title width", pixelfont.measure("SKYMOLE", 9),
          "subtitle width", pixelfont.measure(sub, 2))
