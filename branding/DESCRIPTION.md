# Craftle — CurseForge listing copy

**Summary (CurseForge summary field, 255 char max):**

Wordle type crafting puzzle. Ten guesses to crack a secret crafting recipe, with green / orange / grey feedback on every ingredient you place. Everyone gets the same puzzle each day.

---

# Description (paste into the CurseForge description editor in Markdown mode)

## Craftle

Wordle type crafting puzzle

Every day there is one secret crafting recipe, and it's the **same recipe for everyone**. You get **ten guesses** to work out what it is: place ingredients into the 3x3 grid, press **Craft**, and every cell you filled comes back colour-coded.

## Screenshots

<!-- Upload each image on the project's Images tab (or paste it into the editor, which
     uploads it for you), then replace these two URLs with the media.forgecdn.net links
     CurseForge gives back. -->

![The Craftle board](CF_IMAGE_URL_1)

*Ten guesses to work out the hidden recipe. The output slot shows what your grid would craft.*

![A solved daily](CF_IMAGE_URL_2)

*Every attempt stays on screen, and the ingredients carry what you have learned: green once
placed correctly, orange for in the recipe somewhere, grey for ruled out.*

## How to play

Run `/craftle`. Pick an ingredient from the palette, click the grid to place it, right-click a cell to clear it, then press **Craft** to submit a guess.

Each cell of your guess comes back as one of three colours:

| Colour | Meaning |
|---|---|
| **Green** | Right ingredient, and it's in the right cell |
| **Orange** | That ingredient is in the recipe, but it belongs somewhere else |
| **Grey** | That ingredient isn't in the recipe at all |

Empty cells give nothing away, and recipes smaller than 3x3 are always anchored to the **top-left** of the grid.

Every attempt you make stays on screen as a colour-coded mini grid down the sides of the board, so you can cross-reference all ten guesses. Your last attempt also stays on the crafting grid itself, so your next guess can be a one-cell tweak rather than a rebuild.

## Commands

| Command | What it does |
|---|---|
| `/craftle` | Today's daily puzzle |
| `/craftle random` | Practice mode, unlimited puzzles |
| `/craftle random new` | Abandon the current practice puzzle, deal a fresh one |

## Features

- **A global daily.** The puzzle is derived from the calendar day, so every server running the same Minecraft version has the same daily. It resets at midnight UTC.
- **Chat announcements.** Solve it and the server announces it — `Player solved today's Craftle in 4/10!` Burn all ten guesses and it announces that too, without giving the answer away to anyone still playing.
- **Streaks and stats.** Games played, games won, current streak and best streak, shown on the board when you finish.
- **Practice mode** that never deals you today's daily, so it can't spoil it.
- **High contrast mode** for colourblind players, in the help page — swaps green/orange for a blue/orange pair.
- **Nothing to cheat with.** The server holds the answer and grades your guesses; the answer is never sent to your client until the game is over.
- **The same puzzle everywhere.** The pool is 127 vanilla recipes baked into the mod, not read from whatever a server happens to have loaded, so datapacks and other mods can't hand your server a different daily from everyone else's.
- **Dealt like a deck.** Every puzzle comes up exactly once per 127-day cycle, never two days running, and the order is reshuffled for each new pass.
- **Saves your progress.** Log out mid-puzzle and pick up exactly where you left off.

## Installation

Craftle must be installed on **both the client and the server**, along with [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).

1. Install the [Fabric loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Drop the Craftle jar and the Fabric API jar into your `mods` folder
3. In singleplayer it just works. On a server, install it on both sides.

Players without the mod installed simply can't open the board — the command tells them what's missing, and nothing else breaks for them.

## Credits

Craftle is an independent fan recreation, built for in-game play, of the browser game **[Minecraftle](https://minecraftle.zachmanson.com)** by Tamura Boog, Zach Manson, Harrison Oates and Ivan Sossa Gongora. All credit for the game's concept and rules goes to them.
