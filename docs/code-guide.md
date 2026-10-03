# Code Guide

Annotated snippets for every key class.  
Imports are abbreviated; full sources live in `src/main/java/dev/xpolion/xpotriad/`.

---

## `Ability`
*`ability/Ability.java`*

An Ability is a pure data object: a weapon type and three fragment slots.

```java
public final class Ability {

    public enum Stage { PRE_CAST, CAST, POST_CAST }
    public enum WeaponType { MELEE, RANGED }

    private final WeaponType weaponType;

    // One Fragment per stage; null means "empty stage"
    private final Map<Stage, Fragment> fragments = new EnumMap<>(Stage.class);

    public Ability(WeaponType weaponType) { ... }

    public void setFragment(Stage stage, Fragment fragment) { ... }
    public Fragment getFragment(Stage stage) { ... }   // returns null if unset
    public WeaponType getWeaponType() { ... }
}
```

> **No delays stored here.** Timing is owned by each `Fragment` via `executionTime`.

---

## `Fragment`
*`fragment/Fragment.java`*

Abstract base. All fields are immutable. `executionTime` is new — the engine reads it to schedule the next stage.

```java
public abstract class Fragment {

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;      // defensive copy via List.copyOf
    private final boolean glint;
    private final long executionTime;     // ticks; must be >= 0

    // Convenience constructors chain to the full constructor:
    protected Fragment(String id, String displayName, Material material) { ... }
    protected Fragment(String id, String displayName, Material material, List<String> lore) { ... }
    protected Fragment(String id, String displayName, Material material,
                       List<String> lore, boolean glint) { ... }
    protected Fragment(String id, String displayName, Material material,
                       List<String> lore, boolean glint, long executionTime) { ... }

    public final long getExecutionTime() { return executionTime; }

    // Implemented by concrete subclasses
    public abstract void execute(AbilityContext context);
}
```

**Concrete fragments:**

```java
// InvisibilityFragment
super("invisibility", ChatColor.LIGHT_PURPLE + "Invisibility Fragment",
      Material.AMETHYST_SHARD, List.of(...), true, 10L);

// SpeedFragment
super("speed", ChatColor.AQUA + "Speed Fragment",
      Material.FEATHER, List.of(...), true, 20L);

// ExplosionFragment
super("explosion", ChatColor.RED + "Explosion Fragment",
      Material.FIRE_CHARGE, List.of(...), true, 0L);
```

Each fragment's `execute()` simply delegates to its effect:

```java
@Override
public void execute(AbilityContext context) {
    effect.apply(context);
}
```

---

## `AbilityContext`
*`ability/AbilityContext.java`*

One object, one execution. Created at the event site, passed through all stages unchanged (targeting may be replaced with a new instance for ranged hits).

```java
public final class AbilityContext {

    private final Player source;       // the casting player
    private final Ability ability;     // what ability is running
    private final Targeting targeting; // how it targets — SINGLE or AOE

    public AbilityContext(Player source, Ability ability, Targeting targeting) { ... }

    public Player    getSource()    { return source; }
    public Ability   getAbility()   { return ability; }
    public Targeting getTargeting() { return targeting; }
}
```

---

## `AbilityEngine`
*`ability/AbilityEngine.java`*

Sequences stages using the Paper scheduler. Non-blocking — each stage schedules the next.

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
                nextDelay = 0L;   // empty stage: advance immediately, no buffer
            }

            Ability.Stage nextStage = getNextStage(stage);
            if (nextStage != null) {
                executeStage(context, nextStage, nextDelay);
            }

        }, delayTicks);
    }
}
```

**Timing example for default fragments:**

```
PRE_CAST  (Invisibility, executionTime=10) → schedules CAST  after 15 ticks
CAST      (Speed,        executionTime=20) → schedules POST_CAST after 25 ticks
POST_CAST (Explosion,    executionTime=0 ) → schedules nothing
```

---

## `Targeting` Interface
*`targeting/Targeting.java`*

```java
public interface Targeting {
    TargetingType getType();
    List<Entity> resolve(AbilityContext context);
}
```

Implementations own their own geometry. The engine and Fragment never call `resolve()` directly — only `Effect` implementations do.

---

## `SingleTargeting`
*`targeting/SingleTargeting.java`*

```java
public final class SingleTargeting implements Targeting {

    private final Entity primaryTarget;  // may be null

    public SingleTargeting(Entity target) { this.primaryTarget = target; }

    @Override public TargetingType getType() { return TargetingType.SINGLE; }

    @Override
    public List<Entity> resolve(AbilityContext context) {
        if (primaryTarget == null) return Collections.emptyList();
        return Collections.singletonList(primaryTarget);
    }
}
```

Used for:
- Melee hit → `SingleTargeting(victim)`
- Projectile hits entity → `SingleTargeting(hitEntity)`
- Projectile hits block → `SingleTargeting(null)` *(then replaced by AoeTargeting)*

---

## `AoeTargeting`
*`targeting/AoeTargeting.java`*

```java
public final class AoeTargeting implements Targeting {

    private final Location origin;  // cloned on construction
    private final double radius;    // must be >= 0

    public AoeTargeting(Location origin, double radius) {
        this.origin = origin.clone();   // never mutates caller's Location
        this.radius = radius;
    }

    @Override public TargetingType getType() { return TargetingType.AOE; }

    @Override
    public List<Entity> resolve(AbilityContext context) {
        return TargetResolver.entitiesNear(origin, radius);
    }

    public Location getOrigin() { return origin.clone(); }  // defensive copy
}
```

---

## `TargetResolver`
*`targeting/TargetResolver.java`*

```java
public final class TargetResolver {

    public static List<Entity> entitiesNear(Location origin, double radius) {
        World world = origin.getWorld();
        Collection<Entity> nearby = world.getNearbyEntities(origin, radius, radius, radius);

        List<Entity> result = new ArrayList<>();
        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.isDead()) continue;
            result.add(entity);
        }
        return List.copyOf(result);  // safe, immutable
    }
}
```

---

## `ActivationRegistry`
*`ability/ActivationRegistry.java`*

```java
public final class ActivationRegistry {

    private final Map<UUID, AbilityContext> registry = new ConcurrentHashMap<>();

    public void register(UUID id, AbilityContext context) { registry.put(id, context); }
    public AbilityContext get(UUID id)    { return registry.get(id); }
    public AbilityContext remove(UUID id) { return registry.remove(id); }  // ← call on hit
    public boolean contains(UUID id)     { return registry.containsKey(id); }
}
```

Entries are always removed in `ProjectileHitEvent` to prevent leaks.

---

## `AbilityItem`
*`ability/AbilityItem.java`*

**Engrave** writes 4 keys into item PDC:

```java
pdc.set(abilityKey,    STRING, "ability");           // presence marker
pdc.set(weaponTypeKey, STRING, ability.getWeaponType().name());
// for each stage:
pdc.set(fragmentKeys[stage.ordinal()], STRING, fragment.getId());
```

**Read** reconstructs an Ability from PDC:

```java
Ability.WeaponType weaponType = Ability.WeaponType.valueOf(pdc.get(weaponTypeKey, STRING));
Ability ability = new Ability(weaponType);
for (Stage stage : Stage.values()) {
    String fragmentId = pdc.get(fragmentKeys[stage.ordinal()], STRING);
    if (fragmentId != null) {
        Fragment fragment = FragmentRegistry.get(fragmentId);
        ability.setFragment(stage, fragment);
    }
}
```

No delay keys. Fragment execution times come from `FragmentRegistry`, not PDC.

---

## `AbilityListener`
*`ability/AbilityListener.java`*

### Melee path

```java
@EventHandler
public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) return;

    ItemStack item = player.getInventory().getItemInMainHand();
    if (!AbilityItem.isAbilityItem(item)) return;

    Ability ability = AbilityItem.read(item);
    if (ability == null || ability.getWeaponType() != MELEE) return;

    Entity victim = event.getEntity();

    AbilityContext context = new AbilityContext(
        player, ability, new SingleTargeting(victim)
    );
    abilityEngine.execute(context);
}
```

### Ranged launch path

```java
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    // guard: HAND, RIGHT_CLICK_AIR | RIGHT_CLICK_BLOCK, RANGED ability item
    event.setCancelled(true);

    UUID contextId = UUID.randomUUID();
    AbilityContext context = new AbilityContext(
        player, ability, new SingleTargeting(null)  // placeholder
    );
    activationRegistry.register(contextId, context);

    Projectile projectile = player.launchProjectile(Snowball.class);
    projectile.getPersistentDataContainer()
              .set(contextIdKey, STRING, contextId.toString());
}
```

### Projectile hit path

```java
@EventHandler
public void onProjectileHit(ProjectileHitEvent event) {
    // read context_id from projectile PDC
    AbilityContext original = activationRegistry.remove(contextId);

    AbilityContext finalContext;
    if (event.getHitEntity() != null) {
        finalContext = new AbilityContext(
            original.getSource(), original.getAbility(),
            new SingleTargeting(event.getHitEntity())
        );
    } else {
        finalContext = new AbilityContext(
            original.getSource(), original.getAbility(),
            new AoeTargeting(projectile.getLocation(), DEFAULT_AOE_RADIUS)
        );
    }
    abilityEngine.execute(finalContext);
}
```

---

## Effects

### `InvisibilityEffect`

```java
public void apply(AbilityContext context) {
    context.getSource().addPotionEffect(
        new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false, false)
    );
}
```

### `SpeedEffect`

```java
public void apply(AbilityContext context) {
    context.getSource().addPotionEffect(
        new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false, false)
    );
}
```

### `ExplosionEffect`

Location resolution priority:

```java
private Location resolveLocation(AbilityContext context, Player source, Targeting targeting) {
    if (targeting.getType() == TargetingType.AOE) {
        return ((AoeTargeting) targeting).getOrigin();       // 1. AOE impact point
    }
    if (targeting.getType() == TargetingType.SINGLE) {
        var targets = targeting.resolve(context);
        if (!targets.isEmpty()) {
            return targets.get(0).getLocation();             // 2. entity location
        }
    }
    return source.getLocation();                             // 3. source fallback
}
```

Then:

```java
location.getWorld().createExplosion(location, 4.0f, false, false, source);
//                                             power  fire  blockDmg  source
```

---

## `FragmentRegistry`

```java
public final class FragmentRegistry {
    private static final Map<String, Fragment> FRAGMENTS = new HashMap<>();

    static {
        register(new InvisibilityFragment());
        register(new SpeedFragment());
        register(new ExplosionFragment());
    }

    public static Fragment get(String id) { return FRAGMENTS.get(id); }
}
```

To add a new Fragment: create the class, call `register(new MyFragment())` here.

---

## `FragmentItem`

```java
// Create a physical fragment item:
ItemStack item = FragmentItem.create(invisibilityFragment);

// Check if an item is a fragment:
boolean isFragment = FragmentItem.isFragmentItem(item);

// Retrieve the Fragment definition from a physical item:
Fragment fragment = FragmentItem.getFragment(item);
```

PDC keys on the item:
```
xpotriad:item_type   = "fragment"
xpotriad:fragment_id = "invisibility"
```

---

## `Main` (Plugin Entry Point)

```java
@Override
public void onEnable() {
    AbilityItem.initialize(this);     // registers NamespacedKeys
    FragmentItem.initialize(this);    // registers NamespacedKeys

    abilityEngine      = new AbilityEngine(this);
    activationRegistry = new ActivationRegistry();

    getServer().getPluginManager().registerEvents(
        new AbilityListener(abilityEngine, activationRegistry, this),
        this
    );
}
```

### `/xptest` command

Gives the player a ready-to-use MELEE ability weapon and three physical fragment items.  
No delays are configured — timing comes from the fragment definitions.

```java
Ability ability = new Ability(Ability.WeaponType.MELEE);
ability.setFragment(Ability.Stage.PRE_CAST,  invisibility);   // 10t execution
ability.setFragment(Ability.Stage.CAST,      speed);          // 20t execution
ability.setFragment(Ability.Stage.POST_CAST, explosion);      // 0t  execution

ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);
AbilityItem.engrave(weapon, ability);

player.getInventory().addItem(weapon);
player.getInventory().addItem(FragmentItem.create(invisibility));
player.getInventory().addItem(FragmentItem.create(speed));
player.getInventory().addItem(FragmentItem.create(explosion));
```
