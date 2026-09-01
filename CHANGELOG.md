# Craftle Changelog

## [1.0.1] - 2026-08-31

### Added
- **The ingredient palette now carries what you've learned.** Each ingredient takes the
  best colour it has ever earned across your guesses — green once it has landed in the
  right cell, orange while it's known to be in the recipe somewhere, grey once it has been
  ruled out — so you can see at a glance what's confirmed and what's eliminated without
  re-reading every past guess. The best result always wins, so placing a duplicate one time
  too many can't downgrade an ingredient you've already confirmed.
- **A nudge on your first login** after the daily rolls over, with a clickable `/craftle`.
  It fires once per player per day and only if you haven't already started that day's
  puzzle, so it never nags.

### Changed
- **The puzzle pool is now a fixed list of 127 vanilla recipes**, baked into the mod
  instead of read from whatever recipes a server happens to have loaded. Previously a
  datapack or another mod that added a qualifying shaped recipe shifted the whole pool, so
  that server's daily silently diverged from everyone else's. Servers are still asked for
  each recipe's real layout, so the answer matches what actually crafts there.
- **Puzzles are dealt like a deck rather than drawn each day.** The pool is shuffled and
  handed out one per day until exhausted, then reshuffled. Every puzzle now appears exactly
  once per 127-day cycle, never on consecutive days, and no two cycles run in the same
  order. The old per-day draw repeated far sooner than it looked — with 127 puzzles a
  repeat was likely within a fortnight, while other recipes went months unseen.

### Note for existing players
Those last two changes **alter which recipe comes up on a given day**. Everyone needs to be
on the same version to be playing the same daily, so update server and clients together.
Puzzles already in progress are unaffected — the answer is stored with the game, not
looked up again.

## [1.0.0] - 2026-08-28

First release. A daily crafting-recipe guessing game played on a real 3x3 grid.

### The game
- **A global daily puzzle.** One secret shaped recipe per day, the same one for everyone.
  It resets at midnight UTC.
- **Ten guesses**, graded per cell: green for the right ingredient in the right cell,
  orange for an ingredient that's in the recipe but belongs elsewhere, grey for one that
  isn't in the recipe. Duplicates are handled Wordle-style — an ingredient only earns
  orange while unclaimed copies of it remain, so placing three sticks when the recipe
  wants one tells you exactly that.
- **Empty cells give nothing away**, and recipes smaller than 3x3 are anchored to the
  top-left of the grid.
- **127 puzzles**, every vanilla shaped recipe that can be built entirely from the
  18-ingredient palette. The list is fixed in the mod rather than read from whatever
  recipes a server has loaded, so a datapack or another mod can't shift the pool and hand
  one server a different daily from everyone else. Each server is still asked for the
  recipe's real layout, so the answer matches what actually crafts there.
- **Dealt like a deck, not drawn at random.** The pool is shuffled and handed out one per
  day until exhausted, then reshuffled. Every puzzle appears exactly once per 127-day
  cycle, never on consecutive days, and no two cycles run in the same order.
- **Practice mode** (`/craftle random`) with unlimited puzzles, which never deals today's
  daily — including swapping itself out if an unfinished practice puzzle *becomes* the
  daily overnight.
- **Chat announcements** when someone solves the daily or burns all ten guesses, without
  revealing the answer to anyone still playing.
- **Streaks and stats** — played, won, current streak, best streak — shown when you finish.
- **A nudge on your first login** after the puzzle rolls over, with a clickable `/craftle`.
  Once per player per day, and only if you haven't already started it.

### The board
- A custom screen drawn in vanilla's own style: the crafting grid, a live output slot, the
  ingredient palette, and every previous attempt as a colour-coded mini grid down the
  flanks — all ten visible at once for cross-referencing.
- **The output slot works like a crafting table**, showing what your current arrangement
  would actually craft.
- **Your last attempt stays on the board** as well as going into the history, so the next
  guess is a one-cell tweak rather than a rebuild. Craft is disabled while the grid still
  matches your last guess, so a stray double-click can't spend two turns on it.
- **The palette carries what you've learned** — each ingredient takes the best colour it
  has ever earned, so you can see at a glance what's confirmed, what's still in play, and
  what's ruled out.
- **A high contrast mode** for colourblind players, in the help page, swapping green and
  orange for a blue/orange pair. Remembered in `config/craftle.json`.

### Notes
- Required on **both client and server**, plus Fabric API. Players without it installed
  are told so when they run the command; nothing else breaks for them.
- **The server is the authority.** It holds the answer and grades every guess; the answer
  is never sent to your client until the game is over.
- Games and stats persist in the world save, so you can log out mid-puzzle.
- The game concept and rules come from the browser game
  [Minecraftle](https://minecraftle.zachmanson.com) by Tamura Boog, Zach Manson,
  Harrison Oates and Ivan Sossa Gongora. This is an independent fan recreation.
