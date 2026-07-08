# Changelog

All notable changes to this project are documented here. Format loosely
follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0] - 2026-07-08

Full rewrite from scratch. Everything before this version was archived to
[`reference/legacy/`](reference/legacy/NOTE.md) for reference and is no
longer part of the active project.

### Added

- `/run <x> <y> <z>` — the mod's core feature, and the first time it has
  actually existed in this repository. Automatically walks, jumps, mines
  through obstacles, and bridges gaps to reach the given coordinates, using
  A* pathfinding (`pathfinding/PathfindingEngine`). Mining and bridging are
  only used as a last resort when no walkable/jumpable route exists.
- `/run stop` — cancels an in-progress run.
- Automatic safety abort: a run cancels itself and reports why if the
  player takes unexpected damage or makes no progress for ~5 seconds.
- `/tnt` — spawns a ring of primed TNT around the player, cleanly
  implemented once (see Removed below for the bug this replaces).
- A JUnit test suite for the pathfinding engine (`pathfinding/src/test`),
  covering flat-ground walking, jumping a one-block step, routing around
  lava, bridging a gap, mining through as a last resort, routing around an
  unbreakable wall instead of mining it, and reporting a target as
  unreachable when no route exists.

### Changed

- Platform: **Forge (MC 1.20.1) → Fabric (MC 1.21.1)**, Java 21.
- Project layout: split into a Minecraft-independent `pathfinding` module
  and a Fabric-facing `mod` module, so the pathfinding logic can be unit
  tested without a running game.
- Version numbering is now consistent across `gradle.properties`,
  `build.gradle`, and `fabric.mod.json` (the old project had three
  different, disagreeing version strings across those files).
- The Gradle wrapper jar is committed again — the old repository had it
  deleted, which meant `./gradlew` could not run at all.

### Removed

- `/lockon` and `/unlock` (camera-tracking commands) — dropped entirely,
  not part of the 1.0.0 scope.
- `WebSocketTNTListener` — an unauthenticated WebSocket server on port 9001
  that let anyone trigger TNT spawns from a chat message. Security issue,
  unrelated to the mod's purpose, removed rather than carried forward.
- The old `/tnt` command, which was registered twice in two different
  classes (`TNTCommand.java` and `LockOnHandler.java`) — a real
  registration bug. Replaced by a single, cleanly implemented `/tnt`.
- Assorted unused Forge mod-development-kit boilerplate (example block,
  item, creative tab, config) that was never wired into the mod's actual
  purpose.
