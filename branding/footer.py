"""Builds the footer banner that closes the Modrinth description."""
import os
import struct
import zlib

import pixelfont

W, H = 620, 92
CEIL = 10
FLOOR = H - 12

VOID = (0x0D, 0x09, 0x06)
STONE_A = (0x12, 0x0C, 0x08)
STONE_B = (0x18, 0x11, 0x0B)

LAMP_GOLD = (0xFF, 0xC9, 0x3C)
LAMP_CORE = (0xFF, 0xF0, 0xB8)
MOON_CYAN = (0x4F, 0xC9, 0xDE)
TITANIUM = (0xC3, 0xCB, 0xD2)
CREAM = (0xF5, 0xEF, 0xE6)
DIM = (0x9B, 0x8C, 0x7A)
SHADOW = (0x08, 0x05, 0x03)

ROCK = (0x5A, 0x50, 0x48)
ROCK_EDGE = (0x74, 0x69, 0x5F)
ROCK_DARK = (0x3A, 0x33, 0x2D)
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
    for ty in range(0, H, 20):
        for tx in range(0, W, 20):
            rect(px, tx, ty, 20, 20, shades[pattern[((tx // 20) + (ty // 20) * 3) % len(pattern)]])


def strips(px):
    """Ceiling above and floor below, so the text sits inside the tunnel."""
    rect(px, 0, 0, W, CEIL, ROCK)
    rect(px, 0, CEIL - 3, W, 3, ROCK_EDGE)
    rect(px, 0, FLOOR, W, H - FLOOR, ROCK)
    rect(px, 0, FLOOR, W, 3, ROCK_EDGE)
    for x in range(0, W, 20):
        rect(px, x, 0, 1, CEIL, ROCK_DARK)
        rect(px, x, FLOOR, 1, H - FLOOR, ROCK_DARK)
    for x, color in ((70, MOON_CYAN), (210, TITANIUM), (350, MOON_CYAN),
                     (470, LAMP_GOLD), (560, MOON_CYAN)):
        rect(px, x, FLOOR + 5, 3, 3, color)
    for x, color in ((130, MOON_CYAN), (290, TITANIUM), (430, MOON_CYAN), (530, LAMP_GOLD)):
        rect(px, x, 3, 3, 3, color)


def lamp(px, x, y):
    rect(px, x, y, 10, 10, POST)
    rect(px, x + 1, y + 1, 8, 8, LAMP_GOLD)
    rect(px, x + 3, y + 3, 4, 4, LAMP_CORE)


def text(px, s, y, scale, color, shadow=True):
    width = pixelfont.measure(s, scale)
    x = (W - width) // 2

    def put(bx, by, bw, bh, c):
        rect(px, bx, by, bw, bh, c)

    if shadow:
        off = max(1, scale // 3)
        pixelfont.draw(lambda bx, by, bw, bh, _c: put(bx + off, by + off, bw, bh, SHADOW),
                       s, x, y, scale, SHADOW)
    pixelfont.draw(put, s, x, y, scale, color)
    return x, width


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


def build(top, bottom, accent):
    px = blank()
    backdrop(px)
    strips(px)
    x, width = text(px, top, 26, 4, accent)
    lamp(px, x - 26, 30)
    lamp(px, x + width + 16, 30)
    text(px, bottom, 60, 2, DIM, shadow=False)
    return px


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    out = here
    save(os.path.join(out, "banner-footer.png"),
         build("MADE BY SCULIKE", "MIT LICENSED - OPEN SOURCE ON GITHUB", LAMP_GOLD), 3)
    print("footer written")
