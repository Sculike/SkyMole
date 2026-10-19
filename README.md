<p align="center"><img src="branding/icon-512.png" width="128" alt="SkyMole"></p>

<h1 align="center">SkyMole</h1>
<p align="center"><b>The mining helper for Hypixel SkyBlock.</b><br>Dwarven Mines first, with more mining islands on the way.</p>

---

SkyMole is a client-side Fabric mod for Minecraft 26.2. Type `/skymole` in game to open the menu.

## Features

**Commissions**
- Commission HUD with progress bars and counts (e.g. `5/10`), instant "Commission done!" alerts, a "go claim" reminder
- Waypoints for every active commission; right-click a commission (chat open or in the Commissions menu) for the **fastest walking route**
- Reached waypoints are saved as checkpoints until the commission is done

**Mining**
- **Best next block** route for Mining Spread: green = mine now, orange = next
- **Pickobulus preview**: the blocks it will break, calibrated from your own throws
- Titanium alert, pickaxe ability cooldown, drill fuel, mining stats

**Powder, events, tools**
- Powder tracker (per hour, pristine procs, treasure chests)
- Event timers from the boss bar, Golden/Diamond Goblin and Powder Ghast alerts
- Forge reminder (survives restarts), Fetchur and Puzzler solvers, Sky Mall perk

**Islands**
- Crystal Hollows: zone, crystals, lobby day, Scatha worm alert and cooldown
- Glacite Tunnels: cold warning, Mineshaft alert
- Mob outlines for mining mobs, Forge NPC outline

**Fun (optional)**
- Corpse loot **roulette** with live bazaar prices
- **Big drop** celebration for drops worth 10m+

Plus a drag-and-drop HUD editor (`/skymole hud`), Mod Menu support, and crash safety: if Hypixel changes something, only that feature pauses.

## Hypixel rules

SkyMole only shows information you could see yourself: no macros, no automation, no x-ray. Outlines are hidden behind blocks like normal block outlines, so nothing shows through walls. Every feature can be switched off.

## Install

1. Install [Fabric](https://fabricmc.net/) for Minecraft 26.2 and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Put `skymole-<version>.jar` in your `mods` folder.
3. Recommended: [Mod Menu](https://modrinth.com/mod/modmenu).

Some features read Hypixel's tab list widgets; turn on Commissions, Powders, Pickaxe Ability, Forge and Stats in `/widget`.

## Commands

Press **M** to open the menu (rebind it in Options > Controls > Miscellaneous).

| Command | |
|---|---|
| `/skymole` | open the menu |
| `/skymole hud` | move and resize HUD panels |
| `/skymole marker <spot>` | mark a spot (e.g. `Fetchur`, `Royal Mines`) |
| `/skymole marker set <spot>` | move a spot to where you stand |
| `/skymole test roulette` / `bigdrop` | preview the animations |
| `/skymole debug` | save what SkyMole reads, for bug reports |

## Building

Requires Java 25.

```
./gradlew build
```

The jar ends up in `build/libs/`.

## Credits

- Made by **sculike**
- Marker locations and the Dwarven Mines walking network are based on [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT)
- Prices: Hypixel's public API and moulberry's lowest BIN list

See [NOTICE.md](NOTICE.md) for the details.

## License

[MIT](LICENSE)
