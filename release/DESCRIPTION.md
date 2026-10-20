<p align="center">
<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/01-big-drop.png" width="700" alt="Big drop">
</p>

# SkyMole

A mining mod for Hypixel SkyBlock. I spend most of my time in the Dwarven Mines and got tired of opening the wiki to remember where a commission gets done, so I made this.

Press <kbd>M</kbd> or type `/skymole` to open the settings. Anything you don't want can be turned off.

## Commissions

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/02-overview.png" width="900" alt="The HUD while mining">

Every commission you have gets a waypoint. Right-click one, either in chat or in Hypixel's own Commissions menu, and it draws a path there that follows the actual walkways instead of pointing straight through a wall. When you arrive it stops showing that one until the commission is finished.

The HUD shows progress as a bar, a percentage, or a count like `5/10`, whichever you pick. You get a title and a sound the moment one completes, and a reminder when they're all sitting there waiting to be claimed.

## Mining

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/04-mining-route.png" width="640" alt="Green and orange block route">

Green block is the one to mine, orange is the one after it. It uses your Mining Spread to figure out which block takes the most of the vein with it, then picks the next one as you break through.

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/05-pickobulus.png" width="500" alt="Pickobulus preview">

Before you throw Pickobulus it outlines every block the blast will break and counts them. Afterwards it compares that count to what Hypixel says it destroyed, and corrects itself if it was off.

Also: Titanium alert, pickaxe ability cooldown, drill fuel warning and your mining stats.

## Powder, events and the rest

- Powder tracker per type, per hour, with pristine procs and treasure chests
- Event timers read off the boss bar, including where the event is happening
- Forge reminder that still works if you log off and come back hours later
- Alerts for Golden and Diamond Goblins, the Powder Ghast, and cold in the Glacite Tunnels
- Fetchur and Puzzler answers, and your Sky Mall perk on screen
- Mining mobs outlined per type, plus the Forge NPCs while you're at the Forge
- Crystal Hollows: crystals found, lobby day and the Scatha worm timer

## Corpse loot

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/03-corpse-roulette.png" width="700" alt="Corpse loot roulette">

Loot a corpse in a Mineshaft and it opens like a case, with prices from the bazaar and auction house on every card. If something is worth more than 10 million you get the animation at the top of this page. Both are optional, in the Fun tab.

## Setup

1. Minecraft 26.2 with [Fabric](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Put the jar in your `mods` folder
3. In game, run `/widget` and turn on Commissions, Powders, Pickaxe Ability, Forge and Stats. SkyMole reads those
4. Open SkyBlock Menu → Your SkyBlock Profile once, so it can read your Mining Spread

Panels can be dragged anywhere with `/skymole hud`.

## Will this get me banned

No. Nothing in it is automated. It doesn't click, move, aim or mine for you, and it won't show you ores through walls. The outlines are hidden behind blocks the same way the normal block outline is. Everything it puts on screen comes from what Hypixel already sends your game anyway.

If Hypixel changes something and part of the mod breaks, that part switches itself off instead of crashing your game.

## Source

MIT licensed, code is at [github.com/Sculike/SkyMole](https://github.com/Sculike/SkyMole).

If something looks wrong, run `/skymole debug` and attach `config/skymole-debug.txt` to an issue. It writes down exactly what the mod is reading, which usually makes the problem obvious.

Waypoint locations and the Dwarven Mines path data are based on [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT).
