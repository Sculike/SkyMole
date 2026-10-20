"""Builds stream-banner.html: a self-contained animated overlay for OBS.

Every image is embedded as base64 so the file can be moved anywhere without
breaking, which matters because OBS keeps only a path to it.
"""
import base64
import os

HERE = os.path.dirname(os.path.abspath(__file__))

# Sized to the content: a corner badge, not an empty full-width bar.
W, H = 980, 300
GOLD = "#FFC93C"
CYAN = "#4FC9DE"
PINK = "#F29BB0"
CREAM = "#F5EFE6"


def b64(name):
    with open(os.path.join(HERE, name), "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def particles(count=20):
    """Powder drifting up, staggered so it never looks like a loop."""
    import random
    random.seed(7)  # fixed, so rebuilding does not reshuffle the look
    out = []
    for i in range(count):
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


HTML = """<!doctype html>
<meta charset="utf-8">
<title>SkyMole stream banner</title>
<style>
  /* OBS: add a Browser source, point it at this file, size {W}x{H}.
     Transparent by default. Add ?solid to the URL for a dark bar behind it. */
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

  .mole {{ position: relative; width: 170px; height: 170px; flex: none;
           animation: bob 3.4s ease-in-out infinite; }}

  .words {{ position: relative; display: flex; flex-direction: column; gap: 20px; }}

  /* Shimmer: a soft band travelling across the wordmark, masked to the glyphs. */
  .mark {{ position: relative; width: 560px; }}
  .mark img {{ width: 100%; }}
  .mark::after {{
    content: ""; position: absolute; inset: 0;
    background: linear-gradient(105deg, transparent 38%, rgba(255,255,255,.85) 50%, transparent 62%);
    -webkit-mask: var(--mark) no-repeat center / 100% 100%;
    mask: var(--mark) no-repeat center / 100% 100%;
    animation: sweep 5.6s ease-in-out infinite;
  }}

  .tagline img {{ width: 470px; }}
  .cta {{ display: flex; align-items: center; gap: 14px; margin-top: 6px; }}
  .cta img {{ width: 360px; animation: pulse 2.6s ease-in-out infinite; }}
  .dot {{ width: 12px; height: 12px; background: {GOLD}; animation: pulse 2.6s ease-in-out infinite; }}

  /* Powder rising from the floor. */
  .dust {{ position: absolute; inset: 0; overflow: hidden; pointer-events: none; }}
  .dust i {{ position: absolute; bottom: -12px; animation-name: rise;
             animation-timing-function: linear; animation-iteration-count: infinite; }}

  @keyframes bob {{ 0%,100% {{ transform: translateY(0); }} 50% {{ transform: translateY(-14px); }} }}
  @keyframes breathe {{ 0%,100% {{ opacity: .55; transform: translateY(-50%) scale(1); }}
                        50% {{ opacity: 1; transform: translateY(-50%) scale(1.08); }} }}
  @keyframes sweep {{ 0%, 62% {{ transform: translateX(-120%); }} 100% {{ transform: translateX(120%); }} }}
  @keyframes pulse {{ 0%,100% {{ opacity: .55; }} 50% {{ opacity: 1; }} }}
  @keyframes rise {{
    0%   {{ transform: translate(0, 0); opacity: 0; }}
    12%  {{ opacity: .95; }}
    88%  {{ opacity: .95; }}
    100% {{ transform: translate(var(--drift), -{H}px); opacity: 0; }}
  }}

  @media (prefers-reduced-motion: reduce) {{
    .mole, .lamp, .mark::after, .cta img, .dot, .dust i {{ animation: none; }}
  }}
</style>

<div class="stage">
  <div class="lamp"></div>
  <img class="mole" src="{ICON}" alt="SkyMole">
  <div class="words">
    <div class="mark" style="--mark:url('{MARK}')"><img src="{MARK}" alt="SkyMole"></div>
    <div class="tagline"><img src="{TAG}" alt="A mining mod for Hypixel SkyBlock"></div>
    <div class="cta"><span class="dot"></span><img src="{CTA}" alt="Modrinth, search SkyMole"></div>
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
    html = HTML.format(W=W, H=H, GOLD=GOLD, ICON=b64("icon-128.png"), MARK=b64("wordmark.png"),
                       TAG=b64("tagline.png"), CTA=b64("cta.png"), DUST=particles())
    path = os.path.join(HERE, "stream-banner.html")
    with open(path, "w", encoding="utf-8") as f:
        f.write(html)
    print("wrote", path, "-", round(len(html) / 1024), "KB")
