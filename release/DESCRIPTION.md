<p align="center">
<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/01-big-drop.png" width="700" alt="Big drop celebration">
</p>

<h1 align="center">SkyMole</h1>
<p align="center"><b>Mine smarter. The mining helper for Hypixel SkyBlock.</b></p>

Mining is half of SkyBlock, and most of it is guesswork. Where was that commission again? Which block do I hit next? Is my forge done? Was that drop actually worth anything?

SkyMole answers all of that on screen, while you keep mining. Press <kbd>M</kbd> or type `/skymole` to open it. Every single feature can be switched off.

---

## Stop hunting for your commissions

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/02-overview.png" width="900" alt="HUD with commissions, powder, tools and a waypoint">

Every active commission gets a waypoint. Right-click one, in chat or in Hypixel's own Commissions menu, and SkyMole draws a line along the **fastest walking route**: around corners, down the right ledges, never through a wall. Walk into it and it's ticked off until that commission is done.

- Commission HUD with progress bars, percentages or exact counts (`5/10`)
- **"Commission done!"** the instant Hypixel says so, not ten seconds later
- A nudge when all of them are sitting there waiting to be claimed

## Know which block to hit next

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/04-mining-route.png" width="640" alt="Green and orange blocks showing the mining route">

**Green: mine this now. Orange: mine that next.** SkyMole reads your Mining Spread, works out which block takes the most of the vein with it, and keeps the route moving as you break through.

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/05-pickobulus.png" width="500" alt="Pickobulus preview showing the blocks it will break">

And before you throw **Pickobulus**, see exactly which blocks it is going to break. It even calibrates itself from your own throws, so the count matches what Hypixel reports.

## Never miss anything again

| | |
|---|---|
| **Powder** | Per type, per hour, pristine procs and treasure chests |
| **Events** | 2x Powder, Goblin Raid, Raffle and the rest: where, and how long left |
| **Forge** | "Forge ready!" even if you logged off hours ago |
| **Alerts** | Titanium next to you, Golden and Diamond Goblins, Powder Ghast, freezing cold |
| **Solvers** | Fetchur, Puzzler and your Sky Mall perk |
| **Mobs** | Mining mobs outlined in their own colour, plus the Forge NPCs |

## And when you finally get lucky

<img src="https://raw.githubusercontent.com/Sculike/SkyMole/main/release/gallery/03-corpse-roulette.png" width="700" alt="Corpse loot roulette">

Loot a Mineshaft corpse and SkyMole spins it open like a case, with live prices on every card, landing on what you actually got.

And if it's worth more than 10 million? Then the whole screen celebrates with you.

---

## Setup takes a minute

1. Minecraft **26.2** with [Fabric](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop the jar in your `mods` folder
3. In game: turn on the **Commissions, Powders, Pickaxe Ability, Forge and Stats** widgets with `/widget`, and open **SkyBlock Menu → Your SkyBlock Profile** once so SkyMole knows your Mining Spread

Press <kbd>M</kbd> for the settings, and drag any panel wherever you like with `/skymole hud`.

## Fair play, by design

SkyMole only shows you what you could see yourself. **No macros, no automation, no x-ray**: outlines sit behind blocks exactly like a normal block outline, and nothing ever moves, aims or mines for you.

And if Hypixel changes something unexpected, the affected feature quietly pauses instead of crashing your game.

## Open source

SkyMole is MIT licensed and the full source is on [GitHub](https://github.com/Sculike/SkyMole). Found a bug? `/skymole debug` writes down exactly what SkyMole is reading, which makes a bug report take about ten seconds.

Marker locations and the Dwarven Mines walking network are based on [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT).

<p align="center"><i>Made by sculike.</i></p>
