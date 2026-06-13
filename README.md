# BlazeProgress

A **read-only** Fabric mod for Hypixel SkyBlock that compares your current gear
to a target **Blaze Slayer (Inferno Demonlord) T4 — Mage / Inferno** setup and
shows how close you are, as an in-game overlay.

It does **not** automate anything — no clicking, no movement, no combat. It only
reads your equipped items and shows progress, so it does not violate Hypixel's
rules the way a slayer macro would.

```
BlazeProgress — Mage Inferno — Blaze Slayer T4
Overall 62%  ▰▰▰▰▰▰▱▱▱▱
 - Helmet: 100%
 - Chestplate: 64%
 - Leggings: 0%
 - Boots: 100%
 - Fire Veil Wand: 100%
 - Pyrochaos Dagger: 0%
 - Golden Dragon Pet: 30%
 - Intelligence: 80%
```

## Target Minecraft version

**1.21.11**, Fabric. Requires:

- Fabric Loader
- Fabric API
- Java 21

Drop the built jar **and** Fabric API into your `.minecraft/mods` folder.

## ⚠️ Build status — read this

This mod's source is complete and the **comparison engine is unit-tested and
passing** (`selftest/`, runs with plain `javac` — no Minecraft needed). However
the project was assembled in an environment with **no internet access**, so:

- The Minecraft-facing code (HUD/scanner/command) could **not** be compiled
  against the real 1.21.11 game jar, so it is **not** runtime-verified.
- The Fabric/Yarn/Loom version strings in `gradle.properties` and `build.gradle`
  are **placeholders** — confirm them at <https://fabricmc.net/develop> (select
  1.21.11) before building.

### How to build

```bash
# 1. Set the correct versions in gradle.properties + build.gradle (see above).
# 2. Then:
./gradlew build
# Output: build/libs/blazeprogress-1.0.0.jar
```

### Likely things to fix when you build for 1.21.11

These client APIs changed across 1.21.5 → 1.21.11; if the compile fails, this is
where to look:

- **NBT getters** (`getString`/`getInt`/`getCompound`) return `Optional` on
  1.21.5+. Already handled in `client/GearScanner.java` (the only place NBT is
  read).
- **HUD rendering.** `ProgressHud` uses `HudRenderCallback` +
  `DrawContext.getMatrices()`. Newer Fabric API deprecated `HudRenderCallback`
  in favour of `HudElementRegistry`/`HudLayerRegistrationCallback`, and matrix
  handling on `DrawContext` changed. If it won't compile, port the render hook
  to the current HUD API — the drawing logic itself stays the same.

## Usage

- Overlay shows automatically in SkyBlock once you have gear on.
- **Keybind `B`** (rebindable in Controls) toggles the overlay.
- `/blazeprogress` — print the full breakdown to chat.
- `/blazeprogress details` — toggle the per-piece detail view in the overlay.
- `/blazeprogress toggle` — toggle the overlay.
- `/blazeprogress reload` — reload the config after editing it.
- **Open your in-game Pets menu once** so the mod can read your active pet
  (the active pet can't be read from the inventory).

## Configuring the "perfect" setup

The target is fully editable. After first launch, edit:

```
.minecraft/config/blazeprogress.json
```

Then run `/blazeprogress reload`. You can change item ids, desired reforge,
stars, Kuudra tier, the pet, stat thresholds, and the per-component weights —
the meta shifts, so tune it to whatever you consider "perfect".

The built-in default (Mage Inferno T4):

| Slot | Target | Reforge | Stars | Tier |
|------|--------|---------|-------|------|
| Helmet/Chest/Legs/Boots | Crimson armour | Necrotic | 5 | Crimson |
| Weapon | Fire Veil Wand | — | — | — |
| Weapon | Pyrochaos Dagger | — | — | — |
| Pet | Golden Dragon (Lvl 200, Legendary) | — | — | — |
| Stat | Intelligence ≥ 1500 | — | — | — |

## Project layout

```
src/main/java/dev/neu1/blazeprogress/
  core/     pure Java, no Minecraft — the comparison engine (unit-tested)
  config/   JSON config load/save
  client/   Minecraft-facing: scanner, HUD, command, keybind, trackers
selftest/   standalone javac test for the core engine
```

## Running the engine test (no Minecraft required)

```bash
javac -encoding UTF-8 -d /tmp/bp \
  $(find src/main/java/dev/neu1/blazeprogress/core -name '*.java') \
  selftest/dev/neu1/blazeprogress/core/CoreSelfTest.java
java -cp /tmp/bp dev.neu1.blazeprogress.core.CoreSelfTest
```
