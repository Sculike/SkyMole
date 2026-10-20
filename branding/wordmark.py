"""Renders the SkyMole wordmark and tagline as transparent PNGs for the stream overlay."""
import os
import struct
import zlib

import pixelfont

GOLD = (0xFF, 0xC9, 0x3C)
CREAM = (0xF5, 0xEF, 0xE6)
SHADOW = (0x14, 0x0D, 0x08)


def render(text, scale, color, shadow=True):
    """A transparent RGBA bitmap of one line of pixel text."""
    off = max(2, scale // 3) if shadow else 0
    w = pixelfont.measure(text, scale) + off
    h = pixelfont.HEIGHT * scale + off
    px = [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]

    def put(x, y, bw, bh, color_rgba):
        for yy in range(y, min(h, y + bh)):
            for xx in range(x, min(w, x + bw)):
                if xx >= 0 and yy >= 0:
                    px[yy][xx] = color_rgba

    if shadow:
        pixelfont.draw(lambda x, y, bw, bh, _c: put(x + off, y + off, bw, bh, SHADOW + (255,)),
                       text, 0, 0, scale, None)
    pixelfont.draw(lambda x, y, bw, bh, _c: put(x, y, bw, bh, color + (255,)),
                   text, 0, 0, scale, None)
    return px


def chunk(tag, data):
    out = struct.pack(">I", len(data)) + tag + data
    return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def save(path, px):
    h, w = len(px), len(px[0])
    raw = bytearray()
    for row in px:
        raw.append(0)
        for pixel in row:
            raw.extend(pixel)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
                + chunk(b"IEND", b""))


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    save(os.path.join(here, "wordmark.png"), render("SKYMOLE", 8, GOLD))
    save(os.path.join(here, "tagline.png"), render("A MINING MOD FOR HYPIXEL SKYBLOCK", 2, CREAM, shadow=False))
    save(os.path.join(here, "cta.png"), render("MODRINTH.COM - SEARCH SKYMOLE", 2, GOLD, shadow=False))
    print("wordmark, tagline and cta written")
