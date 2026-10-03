# XpoTriad Documentation

> Minecraft Paper plugin — Java 25 · Paper 26.3-140-main

---

## Contents

| Document | Description |
|---|---|
| [architecture.md](./architecture.md) | Layer overview, package map, class responsibilities |
| [flows.md](./flows.md) | Full execution flows: melee, ranged entity hit, ranged block hit, AOE |
| [code-guide.md](./code-guide.md) | Annotated code snippets for every key class |
| [build.md](./build.md) | Build instructions and project layout |

---

## Quick Overview

XpoTriad is an ability system for Minecraft.  
Players carry **Ability Items** (engraved weapons) that trigger a 3-stage pipeline when activated.

```
Ability Item
    │
    ▼
AbilityContext  (source + ability + targeting)
    │
    ▼
AbilityEngine   (PRE_CAST → CAST → POST_CAST, timed)
    │
    ▼
Fragment        (gameplay unit per stage)
    │
    ▼
Effect          (actual Minecraft behaviour)
```

There are two weapon types — **MELEE** and **RANGED** — which differ only in how the `AbilityContext` is created.  
After creation, both paths feed into the same engine.
