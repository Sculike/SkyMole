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
| Source code | https://github.com/Sculike/SkyMole |
| Issues | https://github.com/Sculike/SkyMole/issues |

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

## Description

Paste the whole of [`DESCRIPTION.md`](DESCRIPTION.md) into the description box. It embeds the
gallery images straight from GitHub, so the page is illustrated from the start. The plain version
below is kept as a fallback.

<details>
<summary>Plain version without images</summary>


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

</details>

---

## Gallery

Upload the files from `release/gallery/` in this order. Modrinth lets you give each one a title
and a description; suggestions below.

| # | File | Title | Description |
|---|---|---|---|
| 1 | `01-big-drop.png` | Big drop celebration | A rare corpse drop worth more than 10m gets its own moment, with the real item and a rolling coin counter. |
| 2 | `02-overview.png` | Everything at a glance | Commissions, powder per hour, pickaxe ability, forge timers and a waypoint with its distance. |
| 3 | `03-corpse-roulette.png` | Corpse roulette | Looting a Mineshaft corpse spins a case-opening reel with live prices. |
| 4 | `04-mining-route.png` | Best next block | Green is the block to mine now, orange is the one after, based on your Mining Spread. |
| 5 | `05-pickobulus.png` | Pickobulus preview | See exactly which blocks your ability will break before you throw it. |
| 6 | `06-menu.png` | Settings | Press M or type /skymole. Every feature can be turned off. |
| 7 | `07-commission-waypoint.png` | Waypoints from the menu | Right-click a commission for a waypoint and the fastest walking route. |
| 8 | `08-titanium-alert.png` | Titanium alert | Get told the moment Titanium appears next to you. |
| 9 | `09-big-drop-full.png` | Big drop, in context | The same celebration with the HUD around it. |

Modrinth's featured image is the first one, and it also shows as a small thumbnail, so keep
`01-big-drop.png` first: it reads well even when small.

### Later, for a trailer or TikTok
GIFs of the mining route and of right-clicking a commission into a route line are the most
convincing moving shots. ShareX or ScreenToGif both record them for free.
