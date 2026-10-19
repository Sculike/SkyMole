# Modrinth upload sheet: SkyMole 1.0.0

## Project settings (when creating the project)

| Field | Value |
|---|---|
| Name | SkyMole |
| URL | `skymole` |
| Summary (short, under the name) | The mining helper for Hypixel SkyBlock: commission waypoints and routes, a Mining Spread block route, Pickobulus preview, powder, events and more. |
| Icon | `branding/icon-512.png` |
| Categories | Utility, Game Mechanics |
| Environment | Client: required. Server: unsupported |
| License | MIT |
| Source code | your GitHub link (after uploading) |
| Issues | your GitHub link + `/issues` |

## Version 1.0.0

| Field | Value |
|---|---|
| File | `build/libs/skymole-1.0.0.jar` (NOT the `-sources` one) |
| Version number | 1.0.0 |
| Release channel | Release (or Beta if you want early feedback first) |
| Loaders | Fabric |
| Game versions | 26.2 |
| Dependencies | Fabric API (required), Mod Menu (optional) |

Changelog:

```
First release! Dwarven Mines focused:
- Commission HUD, alerts, waypoints and fastest walking routes
- Mining Spread block route, Pickobulus preview, Titanium alert
- Powder tracker, event timers, forge reminder, Fetchur and Puzzler solvers
- Mob outlines, Crystal Hollows and Glacite basics
- Optional corpse roulette and big-drop celebration
```

---

## Description (paste into the big description box)

# ⛏ SkyMole

**The mining helper for Hypixel SkyBlock.** Built for the Dwarven Mines first, with more mining islands on the way.

Type `/skymole` to open the menu. Everything can be switched on or off.

## Commissions
- **Commission HUD** with progress bars, percentages or counts (`5/10`)
- **Instant "Commission done!" alerts** and a "go claim" reminder
- **Waypoints for every active commission**, placed where you actually want to stand
- **Right-click a commission** (chat open, or in the Commissions menu) for a **line along the fastest walking route**
- Reached a spot? It's saved as a **checkpoint** until the commission is done

## Mining
- **Best next block** for Mining Spread: **green** = mine now, **orange** = mine next. Follows you as you mine
- **Pickobulus preview**: shows the blocks it will break and calibrates itself from your own throws
- **Titanium alert**, pickaxe ability cooldown, drill fuel warning

## Powder, events and more
- **Powder tracker**: per type, per hour, pristine procs, treasure chests
- **Event timers** (2x Powder, Goblin Raid, Raffle, ...) plus Golden/Diamond Goblin and Powder Ghast alerts
- **Forge reminder** that works across restarts
- **Fetchur and Puzzler solvers**, Sky Mall perk on your HUD
- **Mob outlines** for mining mobs, Forge NPC outline
- Crystal Hollows (crystals, lobby day, Scatha worms) and Glacite (cold, Mineshafts) basics

## Fun (optional)
- **Corpse roulette**: a case-opening spin with live bazaar prices when you loot a Mineshaft corpse
- **Big drop**: a celebration with the real item when a drop is worth 10m+

## Fair play
SkyMole only shows what you could see yourself: **no macros, no automation, no x-ray**. Outlines are hidden behind blocks like normal block outlines. If Hypixel changes something, only that feature pauses; your game never crashes because of SkyMole.

## Setup tips
- Turn on the **Commissions, Powders, Pickaxe Ability, Forge and Stats** widgets in `/widget`
- Open **SkyBlock Menu > Your SkyBlock Profile** once so SkyMole knows your Mining Spread
- Move the HUD with `/skymole hud`

## Credits
Made by **sculike**. Marker locations and the Dwarven Mines walking network are based on [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT).

---

## Gallery: what to record (in this order of importance)

1. **GIF: Big drop** (`/skymole test bigdrop`): the eye-catcher, put it first
2. **GIF: the mining route**: green/orange blocks moving along as you mine Mithril
3. **GIF: right-click a commission → route line → arrive → checkpoint ✔**
4. **GIF: corpse roulette** (`/skymole test roulette`)
5. **Screenshot: the menu** (`/skymole`, Commissions tab)
6. **Screenshot: HUD while mining**: commissions, powder, tools panels
7. **Screenshot: Pickobulus preview** with the block count
8. **Screenshot: auto waypoints** with beams in the Dwarven Mines

Tips: hide F3, use GUI scale 2 or 3, record at 1080p. The SkyMole watermark in the big drop is already there for clips.
