# Execution Flows

All abilities share the same right-click activation, item-specific cooldown check, and stage sequencing engine.

---

## 1. Right-Click Ability Activation

A player right-clicks while holding an AbilityItem (regardless of whether `WeaponType` is `MELEE` or `RANGED`).

```
Player right-clicks with AbilityItem in Main Hand
        │
        ▼
PlayerInteractEvent
  - Guard: event.getHand() == EquipmentSlot.HAND (prevents off-hand double trigger)
  - Guard: action == RIGHT_CLICK_AIR || action == RIGHT_CLICK_BLOCK
  - Guard: AbilityItem.isAbilityItem(item)
  - Read: AbilityItem.read(item) -> Ability
  - Cancel event to prevent vanilla interactions
        │
        ▼
Check Item Cooldown via AbilityItem PDC
  - AbilityItem.isOnCooldown(item)
  ├─► YES (cooling down):
  │     - Calculate remaining seconds: remainingTicks / 20.0
  │     - Send message only to player: "Ability is on cooldown! (X.Xs remaining)"
  │     - Stop execution.
  │
  └─► NO (ready):
        - Calculate cooldown: ability.calculateCooldown()
            base: 20 ticks
            + sum(fragment.cooldownModifier)
            clamped to [0, 300] ticks
        - Write cooldown timestamp onto held ItemStack PDC:
            AbilityItem.applyCooldown(item, cooldownTicks)
        - Create minimal context:
            new AbilityContext(player, ability)
        - Pass to engine:
            AbilityEngine.execute(context)
```

---

## 2. Stage Execution Flow (AbilityEngine)

```
AbilityEngine.execute(context)
        │
        ▼
t=0     PRE_CAST Stage
          Fragment preFragment = ability.getFragment(PRE_CAST)
          if (preFragment != null):
              preFragment.execute(context)  -> Effect.apply(context)
              nextDelay = preFragment.getExecutionTime() + 5 ticks buffer
          else:
              nextDelay = 0 ticks (immediate)
        │
        ▼ (scheduled via Paper Scheduler after nextDelay)
t=nextDelay  CAST Stage
          Fragment castFragment = ability.getFragment(CAST)
          if (castFragment != null):
              castFragment.execute(context) -> Effect.apply(context)
              nextDelay = castFragment.getExecutionTime() + 5 ticks buffer
          else:
              nextDelay = 0 ticks (immediate)
        │
        ▼ (scheduled via Paper Scheduler after nextDelay)
t=...   POST_CAST Stage
          Fragment postFragment = ability.getFragment(POST_CAST)
          if (postFragment != null):
              postFragment.execute(context) -> Effect.apply(context)
        │
        ▼
Execution Finished
```

### Stage Timing Example

For default fragments:
- `InvisibilityFragment`: executionTime = 10 ticks, UNCOMMON, modifier = +10 ticks
- `SpeedFragment`: executionTime = 20 ticks, COMMON, modifier = 0 ticks
- `ExplosionFragment`: executionTime = 0 ticks, RARE, modifier = +40 ticks

**Cooldown:**
`20 (base) + 10 (invis) + 0 (speed) + 40 (expl) = 70 ticks (3.5 seconds)`.

**Execution Sequence:**
- `t = 0`: PRE_CAST runs InvisibilityEffect (100t duration). Engine waits `10 + 5 = 15` ticks.
- `t = 15`: CAST runs SpeedEffect (100t duration). Engine waits `20 + 5 = 25` ticks.
- `t = 40`: POST_CAST runs ExplosionEffect.
- Execution completes.

---

## 3. Spatial Geometry Queries (TargetResolver)

`TargetResolver` is called by Effects when they need to resolve entities within geometry.

### AOE Query: `entitiesNear(center, radius, excluded)`

Used by `ExplosionEffect`:

```
ExplosionEffect.apply(context)
        │
        ▼
Resolve Explosion Origin:
  - If MELEE: player.getLocation()
  - If RANGED: raycast target location (up to 15 blocks) or forward block impact
        │
        ▼
TargetResolver.entitiesNear(explosionLocation, 4.0, sourcePlayer)
  1. Searches world.getNearbyEntities(center, 4, 4, 4)
  2. Filters out excluded entity (source player)
  3. Filters for valid, non-dead LivingEntity instances
  4. Confirms spherical distance (distanceSquared <= 16.0)
  5. Returns List<LivingEntity>
        │
        ▼
Create explosion at location:
  world.createExplosion(location, 4.0f, false, false, sourcePlayer)
```

### Raycast Query: `raycast(source, range)`

Available for directional/beam/ranged effects:

```
TargetResolver.raycast(source, range)
  1. Starts at source.getEyeLocation() along source view direction
  2. Performs block raytrace (rayTraceBlocks) to stop at solid obstacles
  3. Computes bounding box along effective ray length
  4. Checks entity bounding box raytrace (bb.rayTrace(start, dir, range))
  5. Excludes source, dead, or invalid entities
  6. Orders hits by distance (closest to furthest)
  7. Returns ordered List<LivingEntity>
```

---

## 4. Item-Specific Cooldown Demonstration

Two physical items with identical ability compositions in a player's inventory maintain completely separate cooldowns:

```
Player Inventory:
  Slot 1: Ability Sword A  (PDC: cooldown_until = current + 70 ticks)
  Slot 2: Ability Sword B  (PDC: cooldown_until not set)

1. Player right-clicks with Sword A:
   - Sword A goes on 70 tick cooldown.
   - Ability activates.

2. Player immediately switches to Sword B and right-clicks:
   - Sword B PDC checked: not cooling down.
   - Sword B goes on 70 tick cooldown.
   - Ability activates independently.

3. Player switches back to Sword A and right-clicks:
   - Sword A PDC checked: still cooling down!
   - Player receives message: "Ability is on cooldown! (X.Xs remaining)".
   - Activation blocked.
```
