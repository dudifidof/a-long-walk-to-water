# Legacy reference material

This directory holds the pre-1.0.0 state of the project, kept only as
historical reference. It is **not part of the active build** and is not
maintained going forward.

## What's here

- `OurMod/` — the original Minecraft **Forge** project (MC 1.20.1). It does
  not build as committed (its Gradle wrapper jar was deleted in an earlier
  commit), and it never implemented the `/run` auto-walk feature the
  project was meant to have.
- `README-original.md` — the original root README, written as an informal
  project log rather than user-facing documentation.

## Why it was archived

The 1.0.0 rewrite (see the root `README.md` and `CHANGELOG.md`) starts
fresh on **Fabric** for Minecraft 1.21.1, since:

- No `/run` command or navigation/pathfinding code existed anywhere in
  `OurMod/` — the mod's core feature was never implemented.
- The project didn't build (missing Gradle wrapper jar).
- `/tnt` was registered twice in two different classes, a real
  registration bug.
- `WebSocketTNTListener` ran an unauthenticated WebSocket server on port
  9001 that let anyone trigger TNT spawns from chat — a security issue
  unrelated to the mod's purpose.
- Version numbers were inconsistent across `build.gradle`, `mods.toml`,
  and `gradle.properties`.

Full git history for this code is still available via `git log` on this
repository if deeper archaeology is ever needed.
