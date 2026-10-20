"""Minimal PNG reader, just enough to dump a 16x16 item texture."""
import struct
import sys
import zlib


def read(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
    pos = 8
    idat = b""
    width = height = depth = color = 0
    palette = []
    trns = []
    while pos < len(data):
        length, tag = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", body[:10])
        elif tag == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif tag == b"tRNS":
            trns = list(body)
        elif tag == b"IDAT":
            idat += body
        elif tag == b"IEND":
            break

    assert depth == 8, "only 8-bit supported, got %d" % depth
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    raw = zlib.decompress(idat)
    stride = width * channels
    out = []
    prev = bytearray(stride)
    at = 0
    for _ in range(height):
        filt = raw[at]
        line = bytearray(raw[at + 1:at + 1 + stride])
        at += 1 + stride
        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = prev[i]
            c = prev[i - channels] if i >= channels else 0
            if filt == 1:
                line[i] = (line[i] + a) & 0xFF
            elif filt == 2:
                line[i] = (line[i] + b) & 0xFF
            elif filt == 3:
                line[i] = (line[i] + (a + b) // 2) & 0xFF
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pred) & 0xFF
        row = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            if color == 6:
                row.append((px[0], px[1], px[2], px[3]))
            elif color == 2:
                row.append((px[0], px[1], px[2], 255))
            elif color == 3:
                idx = px[0]
                r, g, b = palette[idx]
                a = trns[idx] if idx < len(trns) else 255
                row.append((r, g, b, a))
            elif color == 4:
                row.append((px[0], px[0], px[0], px[1]))
            else:
                row.append((px[0], px[0], px[0], 255))
        out.append(row)
        prev = line
    return out


if __name__ == "__main__":
    rows = read(sys.argv[1])
    seen = {}
    for row in rows:
        for p in row:
            if p[3]:
                seen[p[:3]] = seen.get(p[:3], 0) + 1
    keys = sorted(seen, key=lambda c: -sum(c))
    letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    legend = {c: letters[i] for i, c in enumerate(keys)}
    print("size", len(rows[0]), "x", len(rows))
    for y, row in enumerate(rows):
        line = "".join("." if p[3] == 0 else legend[p[:3]] for p in row)
        print("%2d %s" % (y, line))
    print()
    for c in keys:
        print(legend[c], "#%02X%02X%02X" % c, seen[c])
