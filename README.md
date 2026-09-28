# JavaGame

A top-down action RPG built with [libGDX](https://libgdx.com/) and [Tiled](https://www.mapeditor.org/): explore, clear dungeons and collect gear. Supports co-op LAN multiplayer.

## Modules

- `core`: all game code and the tests.
- `lwjgl3`: the desktop launcher (`com.game.main.lwjgl3.Lwjgl3Launcher`).

## Running

Run the launcher with the working directory at the repo root. Asset paths are `assets/...`, and saves are written to `saves/`.

- `./gradlew core:test`: run the test suite (headless, no window needed).
- `./gradlew lwjgl3:jar`: build a runnable jar into `lwjgl3/build/libs`.

To play multiplayer on one PC, start two instances. In the first: New Game → Esc → **Open to LAN**. In the second: **Multiplayer** → `localhost`, then pick or create a character. Pass `-Dgame.profile=NAME` to the second instance so it gets its own identity.

## Documentation

- `CLAUDE.md`: project overview and conventions (written for AI agents, but a good starting point for anyone).
- `docs/`: one guide per system (multiplayer, items/inventory/loot, saves, Tiled maps, pathfinding).
- `Notes/JavaGame/`: the owner's design notes (an Obsidian vault).
