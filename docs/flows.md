# Execution Flows

All four scenarios share the same engine and Fragment pipeline.
They differ only in how `AbilityContext` is created and what `Targeting` is used.

---

## 1. Melee — Single Target

A player swings a MELEE Ability weapon and hits an entity.

```
Player A attacks Zombie B
        │
        ▼
EntityDamageByEntityEvent
  event.getDamager() → Player A
  event.getEntity()  → Zombie B
        │
        ▼  AbilityListener.onEntityDamage()
Read held item PDC
  isAbilityItem? → yes
  AbilityItem.read(item) → Ability (MELEE)
        │
        ▼
new AbilityContext(
    source    = Player A,
    ability   = Ability (MELEE),
    targeting = new SingleTargeting(Zombie B)
)
        │
        ▼
AbilityEngine.execute(context)
```

**Stage sequence:**

```
t=0     PRE_CAST
          InvisibilityFragment.execute(context)
            → InvisibilityEffect.apply(context)
               → Player A gets INVISIBILITY (100t, amp 0)

        wait 10 + 5 = 15 ticks

t=15    CAST
          SpeedFragment.execute(context)
            → SpeedEffect.apply(context)
               → Player A gets SPEED (100t, amp 1)

        wait 20 + 5 = 25 ticks

t=40    POST_CAST
          ExplosionFragment.execute(context)
            → ExplosionEffect.apply(context)
               targeting = SingleTargeting(Zombie B)
               → explosion at Zombie B's location, power 4

        done
```

---

## 2. Ranged — Projectile Hits an Entity

A player right-clicks a RANGED Ability weapon; the projectile hits a player.

### Phase A — Launch

```
Player A right-clicks RANGED Ability weapon
        │
        ▼  AbilityListener.onPlayerInteract()
  event.getAction() == RIGHT_CLICK_AIR | RIGHT_CLICK_BLOCK
  AbilityItem.read(item) → Ability (RANGED)
  event.setCancelled(true)   ← stops vanilla behaviour
        │
        ▼
UUID contextId = UUID.randomUUID()

new AbilityContext(
    source    = Player A,
    ability   = Ability (RANGED),
    targeting = new SingleTargeting(null)   ← placeholder
)

ActivationRegistry.register(contextId, context)

Snowball projectile = player.launchProjectile(Snowball.class)
projectile PDC: xpotriad:context_id = contextId.toString()
```

### Phase B — Hit

```
Snowball hits Player B
        │
        ▼  AbilityListener.onProjectileHit()
  projectile PDC: context_id → contextId
  ActivationRegistry.remove(contextId) → originalContext

  event.getHitEntity() == Player B  (non-null)
        │
        ▼
new AbilityContext(
    source    = Player A,              ← carried from original
    ability   = Ability (RANGED),      ← carried from original
    targeting = new SingleTargeting(Player B)   ← finalized
)
        │
        ▼
AbilityEngine.execute(finalContext)
```

**Stage sequence:** identical timing to melee; explosion lands at Player B's location.

---

## 3. Ranged — Projectile Hits a Block

Same Phase A as above. Different Phase B.

```
Snowball hits a stone wall
        │
        ▼  AbilityListener.onProjectileHit()
  event.getHitEntity() == null

  impactLocation = projectile.getLocation()
        │
        ▼
new AbilityContext(
    source    = Player A,
    ability   = Ability (RANGED),
    targeting = new AoeTargeting(impactLocation, 5.0)
)
        │
        ▼
AbilityEngine.execute(finalContext)
```

**Stage sequence:**

```
t=0     PRE_CAST  → InvisibilityEffect → Player A invisible

        wait 15 ticks

t=15    CAST      → SpeedEffect        → Player A fast

        wait 25 ticks

t=40    POST_CAST → ExplosionEffect
          targeting = AoeTargeting(impactLocation, 5.0)
          → ExplosionEffect resolves AOE origin
          → explosion at impactLocation, power 4
```

---

## 4. AOE Entity Resolution

When `ExplosionEffect` (or any effect) needs the entities inside an AOE:

```
ExplosionEffect.apply(context)
    context.getTargeting() → AoeTargeting(origin, 5.0)
    targeting.resolve(context)
        │
        ▼  AoeTargeting.resolve()
    TargetResolver.entitiesNear(origin, 5.0)
        │
        ▼  World.getNearbyEntities(origin, 5, 5, 5)
    filter: LivingEntity, not dead
        │
        ▼
    List<Entity> [ Zombie, Skeleton, Player B, ... ]
```

> **Note:** `ExplosionEffect` currently uses the AOE *origin* as the explosion point
> rather than iterating the entity list. The resolved list would be used by effects
> that need to apply a per-entity operation (e.g. knockback, debuff).

---

## 5. Empty Stage (No Fragment)

If a stage has no Fragment assigned, the engine skips it instantly.

```
Ability
  PRE_CAST  = null
  CAST      = SpeedFragment
  POST_CAST = null

t=0   PRE_CAST  — no fragment → advance immediately (0 delay)
t=0   CAST      — SpeedEffect applied → wait 20 + 5 = 25 ticks
t=25  POST_CAST — no fragment → done
```

No 5-tick buffer is added to an empty stage.

---

## Timing Reference

| Fragment | executionTime | Total wait before next stage |
|---|---|---|
| InvisibilityFragment | 10 ticks | 15 ticks |
| SpeedFragment | 20 ticks | 25 ticks |
| ExplosionFragment | 0 ticks | 5 ticks |
| *(empty stage)* | — | 0 ticks |

The 5-tick buffer is a framework constant in `AbilityEngine`:
```java
private static final long STAGE_BUFFER_TICKS = 5L;
```
