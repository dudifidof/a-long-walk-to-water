# A Long Walk to Water

A Fabric mod for Minecraft that adds `/run <x> <y> <z>`: your character
automatically walks, jumps, mines through obstacles, and bridges gaps to
reach the given coordinates, driven by A* pathfinding. Also adds `/tnt`,
which drops a ring of primed TNT around you.

This is the **1.0.0** rewrite of the project. The pre-1.0.0 code (a
different, unfinished Forge project that never actually implemented `/run`)
has been archived under [`reference/legacy/`](reference/legacy/NOTE.md) —
see that folder and [`CHANGELOG.md`](CHANGELOG.md) for why.

## Requirements

- Minecraft **1.21.1**
- [Fabric Loader](https://fabricmc.net/use/) `0.16.9+`
- [Fabric API](https://modrinth.com/mod/fabric-api) for `1.21.1`
- Java 21 to build

## Building

```
./gradlew build
```

The built mod jar is written to `mod/build/libs/`. Install it, along with
Fabric API, into your `.minecraft/mods` folder.

To just run the fast, Minecraft-independent unit tests for the pathfinding
algorithm (see [Architecture](#architecture) below):

```
./gradlew :pathfinding:test
```

To launch a development client:

```
./gradlew :mod:runClient
```

> **Note on this sandbox:** this project was scaffolded and its pathfinding
> logic tested in an environment whose network policy blocks Mojang's and
> Fabric's servers, so `./gradlew build` (which needs to download the
> Minecraft jar, Yarn mappings, Fabric Loader, and Fabric API) could not be
> run end-to-end here. `:pathfinding:test` *was* run successfully here,
> since that module has no Minecraft/Fabric dependency at all. Run
> `./gradlew build` yourself the first time to confirm everything resolves
> — Yarn mapping method names occasionally shift between releases, so if a
> method in `mod/src/main/java/.../navigation/ClientWorldView.java` or
> `AutoWalker.java` doesn't compile against your toolchain's mappings, check
> the equivalent method name for your Yarn build.

## Usage

- `/run <x> <y> <z>` — start walking to the given block coordinates.
- `/run stop` — cancel the current run.
- `/tnt` — spawn a ring of primed TNT around you. Requires operator
  permission (or cheats enabled in singleplayer) — see
  [Architecture](#architecture) for why.

The walk auto-cancels and reports why (in chat) if you take unexpected
damage, if it gets stuck (no progress for ~5 seconds), or if it reaches the
destination.

## Architecture

- **`pathfinding/`** — a plain Java module with zero Minecraft/Fabric
  dependency. `PathfindingEngine` runs A* over the `WorldView` interface
  (`isWalkable`, `isHazard`, `isSolid`, `isBreakable`, `breakTicks`,
  `hasPlaceableBlock`), producing a list of `PathStep`s tagged with a
  `MoveType` (`WALK`, `JUMP_UP`, `DROP_DOWN`, `GAP_JUMP`, `MINE_THROUGH`,
  `BRIDGE_PLACE`). Mining and bridging carry large fixed cost penalties on
  top of their real cost, so A* only ever picks them when no
  walkable/jumpable route exists — they behave as a last resort without a
  separate search pass. Unbreakable blocks (bedrock, etc.) never get a
  `MINE_THROUGH` edge at all, so the planner routes around them instead.
  This module has its own JUnit test suite (`PathfindingEngineTest`) that
  exercises flat-ground walking, jumping a step, routing around lava,
  bridging a gap, mining through as a last resort, routing around an
  unbreakable wall instead of mining it, and reporting "unreachable" when
  neither jumping nor bridging can cross a gap.
- **`mod/`** — the actual Fabric mod, split into:
  - `command/RunCommand.java`, `command/TntCommand.java` — the two client
    commands.
  - `navigation/ClientWorldView.java` — implements the pathfinding module's
    `WorldView` against a live Minecraft `ClientWorld`/player.
  - `navigation/AutoWalker.java` — runs every client tick; turns the
    current `PathStep` into simulated key presses (`forwardKey`,
    `jumpKey`) and interactions (mining via `attackBlock`, placing via
    `interactBlock`), and handles cancellation, stuck-detection, and
    damage-triggered auto-abort.
  - `navigation/NavigationState.java` — the single source of truth for
    "is a run active, and where in the path are we."

`/run` is entirely **client-side**: it works in singleplayer and on any
server (including yours) without a server-side install, because it
literally presses movement/mining/placing keys for the local player the
same way you would — it doesn't teleport or spoof position packets.

`/tnt` has to work differently: spawning entities is server-authoritative,
and this mod has no server-side component. Instead, `/tnt` sends vanilla
`/execute ... summon` commands to the server on your behalf (like typing
them yourself), which is why it needs you to have operator permission (or
cheats enabled in singleplayer) to actually do anything.

## Known limitations

- Pathfinding doesn't cross dimensions or reason about portals.
- `MINE_THROUGH` assumes breaking one block is enough to pass; it doesn't
  model obstacles that need two blocks broken for headroom.
- `/tnt` requires operator permission / cheats, since it works via vanilla
  commands rather than a server-side component.
- The Minecraft-facing code (`ClientWorldView`, `AutoWalker`, the command
  classes) was written against Yarn mappings for 1.21.1 from training
  knowledge, not compiled against the real Minecraft/Fabric jars in this
  sandbox (see the build note above) — expect to fix at most a handful of
  renamed methods on your first real build.
