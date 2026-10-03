# XpoTriad Documentation

> Minecraft Paper plugin — Java 25 · Paper 26.3-140-main

---

## Contents

| Document | Description |
|---|---|
| [architecture.md](./architecture.md) | Layer overview, package map, class responsibilities, cooldown & targeting design |
| [flows.md](./flows.md) | Full execution flows: right-click activation, cooldown checking/application, stage sequencing, spatial querying |
| [code-guide.md](./code-guide.md) | Annotated code snippets for every key class |
| [build.md](./build.md) | Build instructions, project layout, and PDC key reference |

---

## Quick Overview

XpoTriad is an ability system for Minecraft.  
Players carry **Ability Items** (engraved weapons) that trigger a 3-stage pipeline when activated.

All abilities activate exclusively via **right-click**.

```
Right-Click Ability Item
    │
    ▼
Item Cooldown Check & Write (AbilityItem PDC)
    │
    ▼
AbilityContext  (source + ability)
    │
    ▼
AbilityEngine   (PRE_CAST → CAST → POST_CAST, timed)
    │
    ▼
Fragment        (gameplay unit per stage)
    │
    ▼
Effect          (actual Minecraft behaviour, queries TargetResolver if needed)
```

Weapon types (`MELEE` and `RANGED`) do not dictate the activation method; instead, they inform how individual Fragments and Effects behave during execution.
Cooldowns are item-specific, tracked on the physical ItemStack PDC, and calculated from a base 20 ticks plus Fragment modifiers (clamped to 0..300 ticks).
Targeting geometry is provided directly by `TargetResolver` which returns concrete `List<LivingEntity>` collections.
