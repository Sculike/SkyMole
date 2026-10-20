The first release of SkyMole, the mining helper for Hypixel SkyBlock. Built for the Dwarven Mines, with more mining islands on the way.

Press **M** in game, or type `/skymole`, to open the settings. Every feature can be turned off.

## What it does

**Commissions**
- HUD with progress bars, percentages or counts, and instant "Commission done!" alerts
- A waypoint for every active commission; right-click one (chat open or in Hypixel's Commissions menu) for a line along the **fastest walking route**
- Reaching a spot saves it as a checkpoint until that commission is done

**Mining**
- **Best next block** for Mining Spread: green is the block to mine now, orange is the one after
- **Pickobulus preview** showing the blocks it will break, calibrated from your own throws
- Titanium alert, pickaxe ability cooldown, drill fuel warning, mining stats

**Powder, events, tools**
- Powder tracker with per-hour rates, pristine procs and treasure chests
- Event timers from the boss bar, plus Golden/Diamond Goblin and Powder Ghast alerts
- Forge reminder that survives restarts, Fetchur and Puzzler solvers, Sky Mall perk

**Islands**
- Crystal Hollows: zone, crystals found, lobby day, Scatha worm alert and cooldown
- Glacite: cold warning and Mineshaft alert
- Mob outlines for mining mobs, and the Forge NPCs while you're at the Forge

**Fun, optional**
- Corpse loot **roulette** with live prices
- **Big drop** celebration for drops worth 10m or more

Plus a drag-and-drop HUD editor (`/skymole hud`) and Mod Menu support.

## Install

1. Minecraft **26.2** with [Fabric](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Put `skymole-1.0.0.jar` in your `mods` folder
3. Optional but recommended: [Mod Menu](https://modrinth.com/mod/modmenu)

Some panels read Hypixel's tab widgets, so turn on Commissions, Powders, Pickaxe Ability, Forge and Stats in `/widget`. Open **SkyBlock Menu → Your SkyBlock Profile** once so SkyMole knows your Mining Spread.

## Fair play

SkyMole only shows what you could see yourself: no macros, no automation, no x-ray. Outlines are hidden behind blocks like a normal block outline. If Hypixel changes something, the affected feature pauses instead of crashing your game.

## Known limitations

- Routes and measured commission spots cover the **Dwarven Mines**; other islands have waypoints but no route lines yet
- Auction prices come from public services; if those are unavailable, auction items simply show no price
- Found a problem? `/skymole debug` writes what SkyMole reads to `config/skymole-debug.txt`, which is the most useful thing to attach to an issue

## Credits

Made by **sculike**. Marker locations and the Dwarven Mines walking network are based on [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT). See [NOTICE.md](NOTICE.md).
