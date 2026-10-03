# Code Guide

Annotated snippets for every key class.  
Imports are abbreviated; full sources live in `src/main/java/dev/xpolion/xpotriad/`.

---

## `Ability`
*`ability/Ability.java`*

An Ability represents weapon type, fragment composition, and static cooldown calculation.

```java
public final class Ability {

    public static final long BASE_COOLDOWN_TICKS = 20L;
    public static final long MIN_COOLDOWN_TICKS = 0L;
    public static final long MAX_COOLDOWN_TICKS = 300L;

    public enum Stage { PRE_CAST, CAST, POST_CAST }
    public enum WeaponType { MELEE, RANGED }

    private final WeaponType weaponType;
    private final Map<Stage, Fragment> fragments = new EnumMap<>(Stage.class);

    public Ability(WeaponType weaponType) { ... }

    public void setFragment(Stage stage, Fragment fragment) { ... }
    public Fragment getFragment(Stage stage) { ... }
    public WeaponType getWeaponType() { ... }

    /**
     * Calculates the cooldown: base 20 ticks + sum of fragment cooldown modifiers,
     * clamped to [0, 300] ticks.
     */
    public long calculateCooldown() {
        long cooldown = BASE_COOLDOWN_TICKS;
        for (Stage stage : Stage.values()) {
            Fragment fragment = fragments.get(stage);
            if (fragment != null) {
                cooldown += fragment.getCooldownModifier();
            }
        }
        return Math.clamp(cooldown, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
    }
}
```

> **No runtime cooldown state is stored here.** Cooldown state lives solely in the physical `AbilityItem` PDC.

---

## `AbilityContext`
*`ability/AbilityContext.java`*

Minimal context representing one active execution. Contains only source and ability.

```java
public final class AbilityContext {

    private final Player source;
    private final Ability ability;

    public AbilityContext(Player source, Ability ability) {
        this.source = source;
        this.ability = ability;
    }

    public Player getSource() { return source; }
    public Ability getAbility() { return ability; }
}
```

> **No targeting objects, targets, or cooldown data are stored in AbilityContext.**

---

## `AbilityItem`
*`ability/AbilityItem.java`*

Manages engraving, reading, and item-specific cooldown via Bukkit PersistentDataContainer (PDC).

```java
public final class AbilityItem {

    // PDC keys
    private static NamespacedKey abilityKey;       // "ability"
    private static NamespacedKey itemTypeKey;      // "item_type"
    private static NamespacedKey abilityIdKey;     // "ability_id"
    private static NamespacedKey weaponTypeKey;    // "weapon_type"
    private static NamespacedKey cooldownUntilKey; // "cooldown_until" (timestamp ms)
    private static NamespacedKey[] fragmentKeys;   // stage fragments

    // Reads Ability from item PDC, ignoring unknown fragment IDs safely
    public static Ability read(ItemStack item) { ... }

    // Item-specific cooldown management
    public static boolean isOnCooldown(ItemStack item) {
        return getRemainingCooldownTicks(item) > 0;
    }

    public static long getRemainingCooldownTicks(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Long until = meta.getPersistentDataContainer().get(cooldownUntilKey, PersistentDataType.LONG);
        if (until == null) return 0L;
        long remainingMillis = until - System.currentTimeMillis();
        return remainingMillis <= 0 ? 0L : (remainingMillis + 49) / 50;
    }

    public static void applyCooldown(ItemStack item, long cooldownTicks) {
        ItemMeta meta = item.getItemMeta();
        long until = System.currentTimeMillis() + (cooldownTicks * 50L);
        meta.getPersistentDataContainer().set(cooldownUntilKey, PersistentDataType.LONG, until);
        item.setItemMeta(meta); // applies directly to the held ItemStack
    }
}
```

---

## `AbilityListener`
*`ability/AbilityListener.java`*

Activates all abilities on right-click, checks item-specific cooldown, and starts the engine.

```java
public final class AbilityListener implements Listener {

    private final AbilityEngine abilityEngine;

    public AbilityListener(AbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AbilityItem.isAbilityItem(item)) return;

        Ability ability = AbilityItem.read(item);
        if (ability == null) return;

        event.setCancelled(true);

        // Check item cooldown
        if (AbilityItem.isOnCooldown(item)) {
            long remainingTicks = AbilityItem.getRemainingCooldownTicks(item);
            ((Audience) player).sendMessage(Component.text(
                String.format("Ability is on cooldown! (%.1fs remaining)", remainingTicks / 20.0),
                NamedTextColor.RED
            ));
            return;
        }

        // Apply cooldown to that specific item
        long cooldownTicks = ability.calculateCooldown();
        AbilityItem.applyCooldown(item, cooldownTicks);

        AbilityContext context = new AbilityContext(player, ability);
        abilityEngine.execute(context);
    }
}
```

---

## `AbilityEngine`
*`ability/AbilityEngine.java`*

Sequences stages using the Paper scheduler.

```java
public final class AbilityEngine {

    private static final long STAGE_BUFFER_TICKS = 5L;

    public void execute(AbilityContext context) {
        executeStage(context, Ability.Stage.PRE_CAST, 0L);
    }

    private void executeStage(AbilityContext context, Ability.Stage stage, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!context.getSource().isOnline()) return;

            Fragment fragment = context.getAbility().getFragment(stage);
            long nextDelay;

            if (fragment != null) {
                fragment.execute(context);
                nextDelay = fragment.getExecutionTime() + STAGE_BUFFER_TICKS;
            } else {
                nextDelay = 0L; // empty stage advances immediately
            }

            Ability.Stage nextStage = getNextStage(stage);
            if (nextStage != null) {
                executeStage(context, nextStage, nextDelay);
            }
        }, delayTicks);
    }
}
```

---

## `Fragment` & Concrete Subclasses
*`fragment/Fragment.java`*

```java
public abstract class Fragment {

    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final boolean glint;
    private final long executionTime;
    private final Rarity rarity;
    private final long cooldownModifier;
    private final Effect effect;

    public final void execute(AbilityContext context) {
        effect.apply(context);
    }
}
```

**Concrete definitions:**
- `InvisibilityFragment`: executionTime = 10L, Rarity.UNCOMMON, cooldownModifier = +10L, InvisibilityEffect
- `SpeedFragment`: executionTime = 20L, Rarity.COMMON, cooldownModifier = 0L, SpeedEffect
- `ExplosionFragment`: executionTime = 0L, Rarity.RARE, cooldownModifier = +40L, ExplosionEffect

---

## `TargetResolver`
*`targeting/TargetResolver.java`*

Concrete spatial query implementations returning `List<LivingEntity>`.

```java
public final class TargetResolver {

    // Spherical AOE query
    public static List<LivingEntity> entitiesNear(Location center, double radius, Entity excluded) {
        World world = center.getWorld();
        Collection<Entity> nearby = world.getNearbyEntities(center, radius, radius, radius);
        double radiusSquared = radius * radius;
        List<LivingEntity> result = new ArrayList<>();

        for (Entity entity : nearby) {
            if (entity == excluded) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.isDead() || !living.isValid()) continue;
            if (living.getLocation().distanceSquared(center) <= radiusSquared) {
                result.add(living);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // Directional raycast query stopping at solid blocks
    public static List<LivingEntity> raycast(LivingEntity source, double range) { ... }
}
```

---

## `ExplosionEffect`
*`effects/ExplosionEffect.java`*

Uses `WeaponType` to decide geometry and queries `TargetResolver.entitiesNear()`.

```java
public final class ExplosionEffect implements Effect {

    private static final double EXPLOSION_RADIUS = 4.0;
    private static final float EXPLOSION_POWER = 4.0f;

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();
        Location explosionLocation;

        if (context.getAbility().getWeaponType() == Ability.WeaponType.RANGED) {
            List<LivingEntity> targets = TargetResolver.raycast(source, 15.0);
            explosionLocation = !targets.isEmpty() ? targets.get(0).getLocation() : source.getLocation();
        } else {
            explosionLocation = source.getLocation();
        }

        // Query entities near explosion
        List<LivingEntity> affected = TargetResolver.entitiesNear(explosionLocation, EXPLOSION_RADIUS, source);

        explosionLocation.getWorld().createExplosion(
            explosionLocation, EXPLOSION_POWER, false, false, source
        );
    }
}
```
