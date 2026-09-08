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
- ⬜ **Grey** means that ingredient isn't in the recipe at all

Empty cells give nothing away, and recipes smaller than 3x3 are always anchored to the **top-left** of the grid. Duplicates work the way Wordle does: an ingredient only earns orange while unclaimed copies of it remain, so placing three sticks when the recipe wants one tells you exactly that.

## Screenshots

<!-- Upload each image on the project's Images tab (or paste it into the editor, which
     uploads it for you), then replace these URLs with the media.forgecdn.net links
     CurseForge gives back. -->

![The Craftle board](CF_IMAGE_URL_1)

*Ten guesses to work out the hidden recipe. The output slot shows what your grid would craft.*

![A solved daily](CF_IMAGE_URL_2)

*Every attempt stays on screen, and the palette carries what you've learned: green once placed correctly, orange for in the recipe somewhere, grey for ruled out.*

## How to play

Run `/craftle`. Pick an ingredient from the palette, click the grid to place it, right-click a cell to clear it, then press **Craft** to submit a guess.

| Result | Meaning |
|---|---|
| **Green cell** | Right ingredient, and it's in the right cell |
| **Orange cell** | That ingredient is in the recipe, but it belongs somewhere else |
| **Grey cell** | That ingredient isn't in the recipe, or all its copies are already accounted for |

The output slot works like a real crafting table and shows what your current arrangement would craft. Your last attempt stays on the grid, so the next guess can be a one-cell tweak rather than a rebuild, and Craft is disabled while the grid still matches your last guess so a stray double-click can't spend two turns.

Every attempt you make stays on screen as a colour-coded mini grid down the sides of the board, so you can cross-reference all ten guesses at once.

## Commands

| Command | What it does |
|---|---|
| `/craftle` | Today's daily puzzle (or your finished board, once done) |
| `/craftle random` | Practice mode, unlimited puzzles |
| `/craftle random new` | Abandon the current practice puzzle, deal a fresh one |

## Features

- **A global daily.** The puzzle is derived from the calendar day, so every server running the same version has the same daily. It resets at midnight US Eastern.
- **The palette carries what you've learned.** Each ingredient takes the best colour it has ever earned across your guesses, so you can see at a glance what's confirmed, what's still in play and what's ruled out.
- **Chat announcements.** Solve it and the server announces it: `Player solved today's Craftle in 4/10!` Burn all ten guesses and it announces that too, without giving the answer away to anyone still playing.
- **Streaks and stats.** Games played, games won, current streak and best streak, shown on the board when you finish.
- **Practice mode** that never deals you today's daily, so it can't spoil it.
- **High contrast mode** for colourblind players, in the help page. Swaps green and orange for a blue and orange pair.
- **Nothing to cheat with.** The server holds the answer and grades your guesses. The answer is never sent to your client until the game is over.
- **The same puzzle everywhere.** The pool is 127 vanilla recipes baked into the mod, not read from whatever a server happens to have loaded, so datapacks and other mods can't hand your server a different daily from everyone else's.
- **Dealt like a deck.** Every puzzle comes up exactly once per 127-day cycle, never two days running, and the order is reshuffled for each new pass.
- **Saves your progress.** Log out mid-puzzle and pick up exactly where you left off.
- **Login nudge.** A clickable `/craftle` in chat when a new daily is waiting. Once per player per day, and only if you haven't already started it.

## Installation

Craftle must be installed on **both the client and the server**, along with [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).

1. Install the [Fabric loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Drop the Craftle jar and the Fabric API jar into your `mods` folder
3. In singleplayer it just works. On a server, install it on both sides.

Players without the mod installed simply can't open the board. The command tells them what's missing, and nothing else breaks for them.

## Credits

Craftle is an independent fan recreation, built for in-game play, of the browser game **[Minecraftle](https://minecraftle.zachmanson.com)** by Tamura Boog, Zach Manson, Harrison Oates and Ivan Sossa Gongora. All credit for the game's concept and rules goes to them. Not affiliated with Mojang.
