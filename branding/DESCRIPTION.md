# Craftle — listing copy

**GitHub description:**

The daily crafting-recipe guessing game, in Minecraft. Ten guesses to crack one secret recipe, the same one for everyone. A Fabric mod for 26.2.

**GitHub topics:**

minecraft, minecraft-mod, fabric, fabricmc, wordle, puzzle-game, daily-puzzle, crafting, minigame, java

**Summary (CurseForge summary field, 255 char max):**

Wordle for crafting recipes. Ten guesses to crack one secret recipe, with green, orange and grey feedback on every ingredient you place. Everyone gets the same puzzle each day, with a practice mode, stats and streaks.

---

# Description (paste into the CurseForge description editor in Markdown mode)

## Craftle

Wordle for crafting recipes, in Minecraft

Every day there is one secret crafting recipe, and it's the **same recipe for everyone**. You get **ten guesses** to work out what it is: place ingredients from an 18-item palette into the 3x3 grid, press **Craft**, and every cell you filled comes back colour-coded.

- 🟩 **Green** is the right ingredient in the right cell
- 🟧 **Orange** means that ingredient is in the recipe, but belongs somewhere else
- ⬜ **Grey** means that ingredient isn't in the recipe at all (or every copy of it is already placed)

Empty cells give nothing away, and recipes smaller than 3x3 are always anchored to the **top-left** of the grid.

## Screenshots

<!-- Upload each image on the project's Images tab, then replace these URLs with the
     media.forgecdn.net links CurseForge gives back. -->

![The Craftle board](CF_IMAGE_URL_1)

![A solved daily](CF_IMAGE_URL_2)

## How to play

Run `/craftle`. Pick an ingredient from the palette, click the grid to place it, right-click a cell to clear it, then press **Craft**.

| Result | Meaning |
|---|---|
| **Green cell** | Right ingredient, and it's in the right cell |
| **Orange cell** | That ingredient is in the recipe, but it belongs somewhere else |
| **Grey cell** | That ingredient isn't in the recipe, or all its copies are already accounted for |

The output slot shows what your current arrangement would actually craft. Your last attempt stays on the grid, so the next guess can be a one-cell tweak, and every attempt stays on screen beside the board for cross-referencing.

## Commands

| Command | What it does |
|---|---|
| `/craftle` | Today's daily puzzle |
| `/craftle random` | Practice mode, unlimited puzzles |
| `/craftle random new` | Abandon the current practice puzzle, deal a fresh one |

## Features

- **A global daily** that resets at midnight US Eastern. Every server on the same version has the same puzzle.
- **The palette carries what you've learned.** Each ingredient keeps the best colour it has earned, so you can see what's confirmed and what's ruled out at a glance.
- **Chat announcements** when someone solves the daily or runs out of guesses, without giving away the answer.
- **Streaks and stats.** Played, won, current streak and best streak.
- **Practice mode** that never deals you today's daily, so it can't spoil it.
- **High contrast mode** for colourblind players, swapping green and orange for blue and orange.
- **Nothing to cheat with.** The server holds the answer and grades your guesses.
- **127 vanilla recipes**, baked into the mod so datapacks can't change your server's daily, and dealt like a deck so none repeats within a cycle.
- **Saves your progress.** Log out mid-puzzle and pick up where you left off.

## Installation

Craftle must be installed on **both the client and the server**, along with [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).

1. Install the [Fabric loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Drop the Craftle jar and the Fabric API jar into your `mods` folder
3. In singleplayer it just works. On a server, install it on both sides.

Players without the mod can't open the board, and nothing else breaks for them.

## Credits

Craftle is an independent fan recreation of the browser game **[Minecraftle](https://minecraftle.zachmanson.com)** by Tamura Boog, Zach Manson, Harrison Oates and Ivan Sossa Gongora. All credit for the game's concept and rules goes to them. Not affiliated with Mojang.
