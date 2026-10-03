# Architecture

## Package Map

```
src/main/java/dev/xpolion/xpotriad/
│
├── Main.java                          ← Plugin entry point
│
├── ability/
│   ├── Ability.java                   ← Data: weapon type + 3 fragment slots
│   ├── AbilityContext.java            ← Runtime execution record (source + ability + targeting)
│   ├── AbilityEngine.java             ← Stage sequencer + timing
│   ├── AbilityItem.java               ← PDC read/write for Ability on ItemStacks
│   ├── AbilityListener.java           ← Event listener (melee + ranged)
│   └── ActivationRegistry.java        ← UUID → AbilityContext map for projectile tracking
│
├── fragment/
│   ├── Fragment.java                  ← Abstract base: id, displayName, material, lore, glint, executionTime
│   ├── FragmentItem.java              ← PDC read/write for Fragment physical items
│   ├── FragmentRegistry.java          ← Static id → Fragment lookup
│   ├── InvisibilityFragment.java      ← executionTime = 10 ticks
│   ├── SpeedFragment.java             ← executionTime = 20 ticks
│   └── ExplosionFragment.java         ← executionTime = 0 ticks
│
├── effects/
│   ├── Effect.java                    ← Interface: void apply(AbilityContext)
│   ├── InvisibilityEffect.java        ← Applies INVISIBILITY potion to source
│   ├── SpeedEffect.java               ← Applies SPEED potion to source
│   └── ExplosionEffect.java           ← Creates explosion at targeting origin
│
└── targeting/
    ├── Targeting.java                 ← Interface: getType() + resolve(context)
    ├── TargetingType.java             ← Enum: SINGLE, AOE
    ├── SingleTargeting.java           ← One pre-determined entity (or null)
    ├── AoeTargeting.java              ← Origin + radius; resolves on demand
    └── TargetResolver.java            ← Static utility: entitiesNear(origin, radius)
```

---

## Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                        Detection                        │
│  EntityDamageByEntityEvent   PlayerInteractEvent        │
│          (MELEE)                  (RANGED)              │
└────────────────────────┬────────────────────────────────┘
                         │ creates
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    AbilityContext                        │
│   source: Player                                        │
│   ability: Ability  (WeaponType + 3 Fragment slots)     │
│   targeting: Targeting  (SINGLE or AOE)                 │
└────────────────────────┬────────────────────────────────┘
                         │ passed to
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    AbilityEngine                        │
│   PRE_CAST  → wait executionTime + 5 ticks              │
│   CAST      → wait executionTime + 5 ticks              │
│   POST_CAST → done                                      │
└────────────────────────┬────────────────────────────────┘
                         │ calls
                         ▼
┌─────────────────────────────────────────────────────────┐
│                      Fragment                           │
│   fragment.execute(context)                             │
│   (delegates to Effect)                                 │
└────────────────────────┬────────────────────────────────┘
                         │ calls
                         ▼
┌─────────────────────────────────────────────────────────┐
│                       Effect                            │
│   effect.apply(context)                                 │
│   may call context.getTargeting().resolve(context)      │
└────────────────────────┬────────────────────────────────┘
                         │ changes
                         ▼
                    Minecraft World
```

---

## Class Responsibilities

### `Ability`
- Stores `WeaponType` (`MELEE` / `RANGED`) and an `EnumMap<Stage, Fragment>`.
- Three stages: `PRE_CAST`, `CAST`, `POST_CAST`.
- Does **not** store delays or timing — those live in each `Fragment`.

### `AbilityContext`
- Represents **one active execution** of an Ability.
- Immutable fields: `source` (Player), `ability` (Ability), `targeting` (Targeting).
- The same instance is passed through all three stages.
- There is **no `AbilityActivation` wrapper** around it.

### `AbilityEngine`
- Receives an `AbilityContext` and sequences stages using the Paper scheduler.
- Per stage: execute fragment → schedule next stage after `executionTime + 5` ticks.
- Empty stage (no Fragment): advance immediately, no buffer.
- Never searches for targets, never does raytrace.

### `AbilityItem`
- Static utility: `engrave(item, ability)` writes PDC, `read(item)` reconstructs Ability.
- PDC keys: `ability`, `weapon_type`, `pre_cast_fragment`, `cast_fragment`, `post_cast_fragment`.
- No delay keys stored. Fragment execution times live in Fragment definitions only.

### `AbilityListener`
- **Melee**: listens on `EntityDamageByEntityEvent`. Reads MELEE ability from held item. Creates `SingleTargeting(victim)`. Runs engine.
- **Ranged**: listens on `PlayerInteractEvent` (right-click). Launches a Snowball projectile tagged with `xpotriad:context_id`. Stores context in `ActivationRegistry`.
- **Projectile hit**: listens on `ProjectileHitEvent`. Reads `context_id`, removes from registry, finalizes targeting (entity → Single, block → AoE), runs engine.

### `ActivationRegistry`
- `Map<UUID, AbilityContext>` backing a `ConcurrentHashMap`.
- Entries are removed on `ProjectileHitEvent` to prevent leaks.

### `Fragment`
- Abstract base. Immutable.
- Fields: `id`, `displayName`, `material`, `lore`, `glint`, `executionTime`.
- `execute(context)` is called by the engine. Implementations delegate to an `Effect`.

### `FragmentRegistry`
- Static class-level map of `id → Fragment`.
- Pre-registers: `invisibility`, `speed`, `explosion`.

### `FragmentItem`
- Static utility: `create(fragment)` makes a physical ItemStack, `isFragmentItem(item)` checks PDC.
- PDC keys: `item_type = "fragment"`, `fragment_id = <id>`.

### `Targeting` (interface)
- `getType()` → `TargetingType`
- `resolve(context)` → `List<Entity>` (safe copy, never exposes internals)

### `SingleTargeting`
- Holds one pre-determined `Entity` (may be `null` for block hits).
- `resolve()` returns `[entity]` or `[]`.

### `AoeTargeting`
- Holds a **cloned** `Location` and a `radius`.
- `resolve()` delegates to `TargetResolver.entitiesNear()`.
- Never mutates the caller's Location.

### `TargetResolver`
- `static entitiesNear(origin, radius)` — uses `World.getNearbyEntities()`, filters dead and non-LivingEntity entries, returns an immutable list.

### Effects

| Class | Target | Behaviour |
|---|---|---|
| `InvisibilityEffect` | `context.getSource()` | INVISIBILITY potion, 100t, amp 0, no particles |
| `SpeedEffect` | `context.getSource()` | SPEED potion, 100t, amp 1, no particles |
| `ExplosionEffect` | targeting origin | `createExplosion(loc, 4f, false, false, source)` |

`ExplosionEffect` location priority:
1. AOE origin (projectile impact)
2. Single-target entity location
3. Source player location (fallback)

---

## Boundary Rules

| Component | What it does NOT do |
|---|---|
| `AbilityEngine` | Search for targets, raytrace, inspect collisions |
| `Targeting` | Modify the engine or ability |
| `Fragment` | Know about targeting type or weapon type |
| `Effect` | Know about stages or timing |
| `AbilityItem` | Store execution times |
| `FragmentRegistry` | Store targeting configuration |
