"""Renders the stream banner to an animated GIF for Streamlabs' Media source.

The HTML version is better if OBS can take a browser source, because it stays
sharp at any size. This exists for Media / Image sources, which want a file.
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

import stream_banner as cfg

HERE = os.path.dirname(os.path.abspath(__file__))

W, H = cfg.W, cfg.H
FPS = 12
CYCLE = 9.0                      # seconds for one pass through the three lines
FRAMES = int(CYCLE * FPS)
FADE = 0.35                      # seconds of fade at each end of a line

WHITE = (255, 255, 255)
SHADOW = (10, 20, 36)
GOLD = (255, 201, 60)


def backdrop():
    """The same blue block field the HTML version draws, as a flat image."""
    random.seed(19)
    cols, rows = W // cfg.BLOCK + 1, H // cfg.BLOCK + 1
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    for row in range(rows):
        for col in range(cols):
            edge = min(col, cols - 1 - col) / (cols / 2) + min(row, rows - 1 - row) / (rows / 2)
            base = int(edge * 3.2) + random.randint(0, 3)
            colour = cfg.BLUES[max(0, min(len(cfg.BLUES) - 1, base))]
            d.rectangle([col * cfg.BLOCK, row * cfg.BLOCK,
                         col * cfg.BLOCK + cfg.BLOCK - 1, row * cfg.BLOCK + cfg.BLOCK - 1], fill=colour)
    return img


def glow(size=300):
    """A soft warm disc for the lamp, built once and faded per frame."""
    g = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    mid = size / 2
    for r in range(int(mid), 0, -1):
        # Falls off towards the edge; squared so the centre stays bright.
        a = int(90 * (1 - r / mid) ** 2)
        d.ellipse([mid - r, mid - r, mid + r, mid + r], fill=(255, 201, 60, a))
    return g


def crisp_text(text, font, colour):
    """Pixel fonts look wrong with antialiasing, so the mask is thresholded."""
    box = font.getbbox(text)
    w, h = box[2] - box[0] + 10, box[3] - box[1] + 10
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).text((-box[0] + 5, -box[1] + 5), text, font=font, fill=255)
    mask = mask.point(lambda v: 255 if v > 110 else 0)
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    layer.paste(colour + (255,), mask=mask)
    return layer, mask


def particles(count=16):
    random.seed(7)
    out = []
    for _ in range(count):
        out.append({
            "x": random.uniform(0, W),
            "size": random.choice([4, 4, 6, 8]),
            "colour": random.choice([GOLD, (79, 201, 222), (245, 239, 230), (242, 155, 176)]),
            "phase": random.uniform(0, 1),
            "speed": random.uniform(0.55, 1.0),
            "drift": random.uniform(-30, 30),
        })
    return out


def build():
    base = backdrop()
    lamp = glow()
    mole = Image.open(os.path.join(HERE, "mole-cutout.png")).convert("RGBA")
    mole = mole.resize((180, 180), Image.NEAREST)

    font = ImageFont.truetype(os.path.join(HERE, "Minecraft.ttf"), 76)
    lines = [crisp_text(text, font, WHITE) for text, _ in cfg.LINES]
    shadows = [crisp_text(text, font, SHADOW)[0] for text, _ in cfg.LINES]

    dust = particles()
    step = CYCLE / len(cfg.LINES)
    frames = []

    for i in range(FRAMES):
        t = i / FPS
        frame = base.copy()

        # Lamp first, so everything else sits on top of the glow.
        pulse = 0.55 + 0.45 * (0.5 + 0.5 * math.sin(2 * math.pi * t / 4.2))
        lit = lamp.copy()
        lit.putalpha(lit.getchannel("A").point(lambda v: int(v * pulse)))
        frame.paste(lit, (6, H // 2 - lit.height // 2), lit)

        bob = int(round(7 * math.sin(2 * math.pi * t / 3.4)))
        frame.paste(mole, (56, H // 2 - mole.height // 2 - bob), mole)

        for p in dust:
            # Each mote runs its own loop up the frame, wrapping round.
            prog = (p["phase"] + t * p["speed"] / 9) % 1
            y = int(H + 12 - prog * (H + 40))
            x = int(p["x"] + p["drift"] * prog)
            a = 0 if prog < 0.1 or prog > 0.92 else 1
            if a and 0 <= x < W:
                ImageDraw.Draw(frame).rectangle([x, y, x + p["size"], y + p["size"]], fill=p["colour"])

        # Whichever line's turn it is, fading at both ends.
        slot = int(t // step) % len(cfg.LINES)
        into = t - slot * step
        if into < FADE:
            vis = into / FADE
        elif into > step - FADE:
            vis = (step - into) / FADE
        else:
            vis = 1.0
        if vis > 0.02:
            layer, _ = lines[slot]
            shadow = shadows[slot]
            x, y = 280, H // 2 - layer.height // 2 + int((1 - vis) * -16)
            for img, dx, dy in ((shadow, 5, 5), (layer, 0, 0)):
                faded = img.copy()
                faded.putalpha(faded.getchannel("A").point(lambda v: int(v * vis)))
                frame.paste(faded, (x + dx, y + dy), faded)

        frames.append(frame.convert("P", palette=Image.ADAPTIVE, colors=128))

    out = os.path.join(HERE, "stream-banner.gif")
    frames[0].save(out, save_all=True, append_images=frames[1:],
                   duration=int(1000 / FPS), loop=0, optimize=True, disposal=2)
    return out


if __name__ == "__main__":
    path = build()
    print("wrote", path, "-", round(os.path.getsize(path) / 1024), "KB,", FRAMES, "frames")
