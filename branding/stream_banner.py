"""Builds stream-banner.html: a self-contained animated overlay for OBS.

The font and the icon are embedded as base64 so the file can be moved
anywhere without breaking, which matters because OBS stores only a path.
"""
import base64
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))

# Sized to the content: a corner badge, not an empty full-width bar.
W, H = 900, 260
# Seconds for one full rotation through the three lines.
CYCLE = 12

GOLD = "#FFC93C"
CYAN = "#4FC9DE"
PINK = "#F29BB0"
CREAM = "#F5EFE6"

LINES = [
    ("SkyMole", GOLD),
    ("By Sculike", CREAM),
    ("!Discord", GOLD),
]


def b64(name, mime):
    with open(os.path.join(HERE, name), "rb") as f:
        return "data:%s;base64,%s" % (mime, base64.b64encode(f.read()).decode())


def particles(count=20):
    """Powder drifting up, staggered so it never looks like a loop."""
    random.seed(7)  # fixed, so rebuilding does not reshuffle the look
    out = []
    for _ in range(count):
        left = random.uniform(2, 98)
        size = random.choice([4, 4, 6, 6, 8])
        colour = random.choice([GOLD, CYAN, CREAM, PINK])
        delay = random.uniform(0, 9)
        dur = random.uniform(7, 13)
        drift = random.uniform(-40, 40)
        out.append(
            f'<i style="left:{left:.1f}%;width:{size}px;height:{size}px;background:{colour};'
            f'animation-delay:-{delay:.1f}s;animation-duration:{dur:.1f}s;--drift:{drift:.0f}px"></i>')
    return "\n    ".join(out)


def slots():
    step = CYCLE / len(LINES)
    out = []
    for i, (text, colour) in enumerate(LINES):
        out.append(f'<span style="color:{colour};animation-delay:{i * step:.2f}s">{text}</span>')
    return "\n    ".join(out)


HTML = """<!doctype html>
<meta charset="utf-8">
<title>SkyMole stream banner</title>
<style>
  /* OBS: add a Browser source, tick Local file, point it here, size {W}x{H}.
     Transparent by default. Add ?solid to the URL for a dark bar behind it. */
  @font-face {{
    font-family: "MinecraftLocal";
    src: url("{FONT}") format("truetype");
    font-display: block;
  }}
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  html, body {{ width: {W}px; height: {H}px; overflow: hidden; background: transparent; }}
  body.solid {{ background: #120C08; }}
  img {{ image-rendering: pixelated; display: block; }}

  .stage {{ position: relative; width: {W}px; height: {H}px;
            display: flex; align-items: center; gap: 40px; padding: 0 56px; }}

  /* The lamp glow behind the mole, breathing rather than blinking. */
  .lamp {{ position: absolute; left: 30px; top: 50%; width: 280px; height: 280px;
           transform: translateY(-50%);
           background: radial-gradient(circle, rgba(255,201,60,.34) 0%, rgba(255,201,60,0) 68%);
           animation: breathe 4.2s ease-in-out infinite; }}

  .mole {{ position: relative; width: 150px; height: 150px; flex: none;
           animation: bob 3.4s ease-in-out infinite; }}

  /* One slot, three lines taking turns. Each is absolute so they share the space. */
  .slot {{ position: relative; flex: 1; height: 86px; }}
  .slot span {{
    position: absolute; left: 0; top: 50%;
    font-family: "MinecraftLocal", monospace;
    font-size: 62px; line-height: 1; white-space: nowrap;
    text-shadow: 4px 4px 0 #140D08;
    opacity: 0; transform: translateY(-50%);
    animation: turn {CYCLE}s ease-in-out infinite;
  }}

  /* Powder rising from the floor. */
  .dust {{ position: absolute; inset: 0; overflow: hidden; pointer-events: none; }}
  .dust i {{ position: absolute; bottom: -12px; animation-name: rise;
             animation-timing-function: linear; animation-iteration-count: infinite; }}

  @keyframes bob {{ 0%,100% {{ transform: translateY(0); }} 50% {{ transform: translateY(-14px); }} }}
  @keyframes breathe {{ 0%,100% {{ opacity: .55; transform: translateY(-50%) scale(1); }}
                        50% {{ opacity: 1; transform: translateY(-50%) scale(1.08); }} }}
  /* Fade up, hold, fade out, then wait while the other two take their turn. */
  @keyframes turn {{
    0%   {{ opacity: 0; transform: translateY(-20%); }}
    4%   {{ opacity: 1; transform: translateY(-50%); }}
    29%  {{ opacity: 1; transform: translateY(-50%); }}
    33%  {{ opacity: 0; transform: translateY(-80%); }}
    100% {{ opacity: 0; transform: translateY(-80%); }}
  }}
  @keyframes rise {{
    0%   {{ transform: translate(0, 0); opacity: 0; }}
    12%  {{ opacity: .95; }}
    88%  {{ opacity: .95; }}
    100% {{ transform: translate(var(--drift), -{H}px); opacity: 0; }}
  }}

  @media (prefers-reduced-motion: reduce) {{
    .mole, .lamp, .dust i {{ animation: none; }}
    .slot span {{ animation: none; opacity: 0; }}
    .slot span:first-child {{ opacity: 1; }}
  }}
</style>

<div class="stage">
  <div class="lamp"></div>
  <img class="mole" src="{ICON}" alt="SkyMole">
  <div class="slot">
    {SLOTS}
  </div>
  <div class="dust">
    {DUST}
  </div>
</div>

<script>
  // ?solid in the URL swaps the transparent background for a dark bar.
  if (location.search.includes("solid")) document.body.classList.add("solid");
</script>
"""


if __name__ == "__main__":
    html = HTML.format(W=W, H=H, CYCLE=CYCLE, FONT=b64("Minecraft.ttf", "font/ttf"),
                       ICON=b64("icon-128.png", "image/png"), SLOTS=slots(), DUST=particles())
    path = os.path.join(HERE, "stream-banner.html")
    with open(path, "w", encoding="utf-8") as f:
        f.write(html)
    print("wrote", path, "-", round(len(html) / 1024), "KB")
