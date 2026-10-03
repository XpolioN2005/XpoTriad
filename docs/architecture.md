# Architecture

## Package Map

```
src/main/java/dev/xpolion/xpotriad/
│
├── Main.java                          ← Plugin entry point
│
├── ability/
│   ├── Ability.java                   ← Data: weapon type + 3 fragment slots + calculateCooldown()
│   ├── AbilityContext.java            ← Runtime execution record (source + ability)
│   ├── AbilityEngine.java             ← Stage sequencer + timing
│   ├── AbilityItem.java               ← PDC read/write and item-specific cooldown for Ability on ItemStacks
│   └── AbilityListener.java           ← Event listener (right-click activation & cooldown gating)
│
├── fragment/
│   ├── Fragment.java                  ← Base: id, displayName, material, lore, glint, executionTime, rarity, cooldownModifier, effect
│   ├── FragmentItem.java              ← PDC read/write for Fragment physical items
│   ├── FragmentRegistry.java          ← Static id → Fragment lookup
│   ├── InvisibilityFragment.java      ← executionTime = 10 ticks, UNCOMMON, cooldownModifier = +10 ticks
│   ├── SpeedFragment.java             ← executionTime = 20 ticks, COMMON, cooldownModifier = 0 ticks
│   └── ExplosionFragment.java         ← executionTime = 0 ticks, RARE, cooldownModifier = +40 ticks
│
├── effects/
│   ├── Effect.java                    ← Interface: void apply(AbilityContext)
│   ├── InvisibilityEffect.java        ← Applies INVISIBILITY potion to source
│   ├── SpeedEffect.java               ← Applies SPEED potion to source
│   └── ExplosionEffect.java           ← Creates explosion; queries TargetResolver for nearby entities
│
└── targeting/
    └── TargetResolver.java            ← Concrete spatial utility: raycast() and entitiesNear()
```

---

## Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                        Detection                        │
│           PlayerInteractEvent (Right-Click)             │
│                 (Main hand enforced)                    │
└────────────────────────┬────────────────────────────────┘
                         │ checks & applies
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Item-Specific Cooldown                 │
│   PDC: xpotriad:cooldown_until on held ItemStack        │
│   Base: 20 ticks (1s) + Fragment modifiers             │
│   Clamped: 0..300 ticks (0..15s)                        │
└────────────────────────┬────────────────────────────────┘
                         │ creates
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    AbilityContext                        │
│   source: Player                                        │
│   ability: Ability (WeaponType + 3 Fragment slots)      │
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
│   may call TargetResolver.raycast(...) or               │
│            TargetResolver.entitiesNear(...)             │
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
- Provides `calculateCooldown()`:
  - Base cooldown: 20 ticks.
  - Adds each fragment's `cooldownModifier`.
  - Clamps the result between 0 and 300 ticks.
- Does **not** store runtime cooldown state.

### `AbilityContext`
- Represents **one active execution** of an Ability.
- Contains only:
  - `Player source`
  - `Ability ability`
- Does not contain targeting, target lists, or cooldown state.
- Passed through all three stages.

### `AbilityEngine`
- Receives an `AbilityContext` and sequences stages using the Paper scheduler.
- Timing per stage: execute fragment → schedule next stage after `executionTime + 5` ticks buffer.
- Empty stage (no Fragment): advance immediately (0 ticks), no buffer.
- Never searches for targets, never performs raycasts, never manages cooldowns, and knows nothing about ItemStacks.

### `AbilityItem`
- Static utility for ItemStacks using Bukkit PDC.
- PDC keys:
  - `xpotriad:ability`: marker string
  - `xpotriad:item_type`: `"ability"`
  - `xpotriad:ability_id`: unique UUID
  - `xpotriad:weapon_type`: `"MELEE"` or `"RANGED"`
  - `xpotriad:pre_cast_fragment`, `xpotriad:cast_fragment`, `xpotriad:post_cast_fragment`
  - `xpotriad:cooldown_until`: epoch timestamp in milliseconds
- Handles item-specific cooldowns:
  - `isOnCooldown(ItemStack item)`
  - `getRemainingCooldownTicks(ItemStack item)`
  - `applyCooldown(ItemStack item, long cooldownTicks)` (modifies the actual held item's metadata)
- `read(item)` safely handles unknown fragment IDs and missing PDC values without crashing.

### `AbilityListener`
- Listens for `PlayerInteractEvent` on `EquipmentSlot.HAND`.
- Ignores left-clicks; activates **only on right-click** (air or block).
- All abilities (MELEE and RANGED) activate through right-click.
- If item is on cooldown: sends cooldown message directly to the player and stops.
- If available: calculates cooldown, writes cooldown onto the held ItemStack PDC, creates `AbilityContext`, and starts `AbilityEngine`.
- No melee damage events (`EntityDamageByEntityEvent`) or projectile events are used for activation.

### `Fragment`
- Base immutable gameplay definition.
- Fields:
  - `id`: registry key
  - `displayName`: formatted display name
  - `material`: item icon material
  - `lore`: item description
  - `glint`: visual enchantment glint
  - `executionTime`: minimum ticks the effect window occupies
  - `rarity`: enum (`COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`)
  - `cooldownModifier`: ticks contributed to ability cooldown
  - `effect`: concrete `Effect` executed when stage runs
- `execute(context)` delegates directly to its `effect.apply(context)`.

### `FragmentRegistry`
- Static registry mapping fragment IDs to `Fragment` instances.
- Pre-registers `invisibility`, `speed`, and `explosion`.

### `FragmentItem`
- Static utility to create and identify physical Fragment items using PDC:
  - `xpotriad:item_type = "fragment"`
  - `xpotriad:fragment_id = <id>`

### `TargetResolver`
- Concrete spatial query utility; not an abstract targeting layer.
- Implements:
  - `raycast(LivingEntity source, double range)`: view direction raycast, stopping at solid blocks, ignoring source, returning ordered `List<LivingEntity>`.
  - `raycast(Location origin, Vector direction, double range, Entity excluded)`: general raycast query.
  - `entitiesNear(Location center, double radius)`: spherical query returning `List<LivingEntity>`.
  - `entitiesNear(Location center, double radius, Entity excluded)`: spherical query excluding specific entity (e.g. source).
- Ignores dead/invalid entities and respects world boundaries.
- Returns actual `LivingEntity` objects, never UUIDs or custom Target wrappers.
- Does not modify world state or apply damage.

### Effects
- Implement `Effect`: `void apply(AbilityContext context)`.
- Effects query `TargetResolver` directly if geometry is needed and decide how to process the returned entities.
- `InvisibilityEffect`: applies INVISIBILITY to `context.getSource()`.
- `SpeedEffect`: applies SPEED to `context.getSource()`.
- `ExplosionEffect`: inspects `context.getAbility().getWeaponType()` (MELEE creates explosion at source location, RANGED targets raycast position), queries `TargetResolver.entitiesNear()` for affected living entities, and creates the explosion.

---

## Boundary Rules

| Component | What it does NOT do |
|---|---|
| `AbilityEngine` | Search for targets, raytrace, manage cooldowns, inspect ItemStacks |
| `TargetResolver` | Apply damage, knockback, effects, or modify gameplay state |
| `AbilityContext` | Store targeting, target lists, raycasts, or cooldown states |
| `Ability` | Store runtime cooldown timestamps or item state |
| `AbilityListener` | Handle melee damage events for ability activation |
