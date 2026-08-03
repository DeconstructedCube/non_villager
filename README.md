# NoN Villager Professions

A Fabric mod that adds custom villager professions — without relying on villager breeding.

## Content

### Naturalist (自然学家)

A villager profession themed around nature and life essence, with its work site at a
**powder snow cauldron** (细雪炼药锅).

> Note: registering the powder snow cauldron as the Naturalist's job site overrides the
> vanilla Leatherworker's claim on that block — leatherworkers will no longer recognize
> powder snow cauldrons (empty/water/lava cauldrons are unaffected).

**Trades** (Fabric API `TradeOfferHelper`, 5 levels):

| Level | Buys (from player) | Sells (to player) |
|---|---|---|
| 1 Novice | 3 Honey Bottle → 1 Emerald | 1 Emerald → Crimson Fungus |
| 2 Apprentice | 2 Honeycomb → 1 Emerald | 1 Emerald → Warped Fungus |
| 3 Journeyman | 1 Fertile Nectar potion → 5 Emeralds | 8 Emeralds → Mixed Liquid Bottle |
| 4 Expert | 2 Flower Mix → 3 Emeralds | 5 Emeralds → Flower Mix |
| 5 Master | — | 12 Emeralds → Entity Liquid Bottle |

Mod items are resolved lazily from the `needsofnature` registry at trade-generation time;
a warning is logged instead of crashing if the mod is missing.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader ≥ 0.19.3
- Fabric API
- Java 21
- [Needs of Nature](https://github.com/) (needsofnature) — source of the traded mod items, plus its default content pack for the liquid bottle textures

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Build

```bash
./gradlew build
```

The jar lands in `build/libs/`.

## License

CC0-1.0 — feel free to learn from it and incorporate it in your own projects.
