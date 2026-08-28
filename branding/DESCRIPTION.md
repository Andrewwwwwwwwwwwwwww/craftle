# Craftle

**Summary (CurseForge summary field, 255 char max):**

The daily crafting-recipe guessing game, in Minecraft. Ten guesses to crack a secret recipe, with green / orange / grey feedback on every ingredient you place. Everyone gets the same puzzle each day.

---

# Description (paste into the CurseForge description editor in Markdown mode)

## Craftle

A daily crafting puzzle, played on a real crafting grid.

Every day there is one secret crafting recipe, and it's the **same recipe for everyone**. You get **ten guesses** to work out what it is: place ingredients into the 3x3 grid, press **Craft**, and every cell you filled comes back colour-coded.

## How to play

Run `/craftle`. Pick an ingredient from the palette, click the grid to place it, right-click a cell to clear it, then press **Craft** to submit a guess.

Each cell of your guess comes back as one of three colours:

| Colour | Meaning |
|---|---|
| **Green** | Right ingredient, and it's in the right cell |
| **Orange** | That ingredient is in the recipe, but it belongs somewhere else |
| **Grey** | That ingredient isn't in the recipe at all |

Empty cells give nothing away, and recipes smaller than 3x3 are always anchored to the **top-left** of the grid.

Every attempt you make stays on screen as a colour-coded mini grid down the sides of the board, so you can cross-reference all ten guesses at once. Your last attempt also stays on the crafting grid itself, so your next guess is a one-cell tweak rather than a rebuild.

The output slot works like a real crafting table: it shows you what your current arrangement would actually craft.

## Commands

| Command | What it does |
|---|---|
| `/craftle` | Today's daily puzzle |
| `/craftle random` | Practice mode, unlimited puzzles |
| `/craftle random new` | Abandon the current practice puzzle, deal a fresh one |

## Features

- **A global daily.** The puzzle is derived from the calendar day, so every server running the same Minecraft version has the same daily. It resets at midnight UTC.
- **Chat announcements.** Solve it and the server announces it — `Andrew solved today's Craftle in 4/10!` Burn all ten guesses and it announces that too, without giving the answer away to anyone still playing.
- **Streaks and stats.** Games played, games won, current streak and best streak, shown on the board when you finish.
- **Practice mode** that never deals you today's daily, so it can't spoil it.
- **High contrast mode** for colourblind players, in the help page — swaps green/orange for a blue/orange pair.
- **Nothing to cheat with.** The server holds the answer and grades your guesses; the answer is never sent to your client until the game is over.
- **Picks up your recipes.** Puzzles come from the server's own recipe list, so datapack recipes built from the ingredient palette are eligible too.
- **Saves your progress.** Log out mid-puzzle and pick up exactly where you left off.

## Installation

Craftle must be installed on **both the client and the server**, along with [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).

1. Install the [Fabric loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Drop the Craftle jar and the Fabric API jar into your `mods` folder
3. In singleplayer it just works. On a server, install it on both sides.

Players without the mod installed simply can't open the board — the command tells them what's missing, and nothing else breaks for them.

## Credits

Craftle is an independent fan recreation, built for in-game play, of the browser game **[Minecraftle](https://minecraftle.zachmanson.com)** by Tamura Boog, Zach Manson, Harrison Oates and Ivan Sossa Gongora. All credit for the game's concept and rules goes to them.
