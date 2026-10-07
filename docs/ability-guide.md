# XpoTriad Engine API & Ability Development Guide

API specification for authoring abilities, fragments, fragment items, effects, runtime states, particle animations, and fragment loot in XpoTriad.

---

## 1. Engine Interfaces & Component Summary

| Interface / Class      | Category       | Role                                                                                                                                   |
| ---------------------- | -------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| `Effect`               | Gameplay       | One-shot execution logic triggered during ability application.                                                                         |
| `Ability`              | Gameplay       | Stage-to-fragment map (`PRE_CAST` / `CAST` / `POST_CAST`) with cooldown calculation from fragment modifiers.                            |
| `Fragment`             | Gameplay       | Immutable definition of a gameplay unit executed per stage. Holds execution timing, lore, rarity, cooldown modifier, and its `Effect`. |
| `FragmentItem`         | Items & PDC    | Factory and utility for generating physical fragment `ItemStack`s and reading fragment metadata.                                       |
| `LootSource`           | Gameplay       | Generic loot-source descriptor (`Type` + key) independent of the event that produced it.                                               |
| `FragmentRegistry`     | Registry       | Global fragment lookup plus source-rule loot rolling (spawn chance → rarity → uniform fragment) for multi-source drops.               |
| `FragmentLootListener` | Loot           | Translates loot events (chests, dispensed loot, mob deaths) into `LootSource`s and injects rolled fragment items.                     |
| `AbilityContext`       | Gameplay       | Context container providing the execution source (`Player`) and ability information.                                                   |
| `AbilityListener`      | Gameplay       | Handles right-click activation, cooldown checks, ability execution, and action-bar feedback.                                           |
| `RuntimeState`         | Gameplay       | Persistent gameplay condition surviving beyond the initial `Effect.apply()`.                                                           |
| `RuntimeHandle`        | Gameplay       | Idempotent control handle for registered `RuntimeState` instances.                                                                     |
| `RuntimeManager`       | Engine Service | Entry point to register and start `RuntimeState` instances.                                                                            |
| `ParticleAnimation`    | Visuals        | Frame-based render contract for particle visual effects.                                                                               |
| `ParticleContext`      | Visuals        | Immutable frame snapshot containing resolved origin, total elapsed time, and frame delta.                                              |
| `ParticleAttachment`   | Visuals        | Enum specifying target tracking (`WORLD` for fixed coordinates, `ENTITY` for dynamic entity tracking).                                 |
| `ParticleHandle`       | Visuals        | Idempotent control handle for persistent particle animations.                                                                          |
| `ParticleSystem`       | Engine Service | Entry point to play one-shot or persistent particle animations.                                                                        |

---

## 2. Interface Specifications

### `Effect`

```java
package dev.xpolion.xpotriad.effect;

import dev.xpolion.xpotriad.ability.AbilityContext;

public interface Effect {

    void apply(AbilityContext context);

}
```

### `Ability`

```java
package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;

public final class Ability {

    public static final long BASE_COOLDOWN_TICKS = 20L;
    public static final long MIN_COOLDOWN_TICKS = 0L;
    public static final long MAX_COOLDOWN_TICKS = 300L;

    public enum Stage {
        PRE_CAST,
        CAST,
        POST_CAST
    }

    public Ability();

    public void setFragment(Stage stage, Fragment fragment);

    public Fragment getFragment(Stage stage);

    // Sums BASE_COOLDOWN_TICKS with every assigned fragment's
    // cooldown modifier, clamped to [MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS].
    public long calculateCooldown();

}
```

An `Ability` is a stage-to-fragment map. Unset stages are skipped during execution with no timing buffer.

### `Fragment`

```java
package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public abstract class Fragment {

    public enum Type {
        MELEE,
        RANGED
    }

    public enum Rarity {

        COMMON(ChatColor.WHITE),
        UNCOMMON(ChatColor.GREEN),
        RARE(ChatColor.AQUA),
        EPIC(ChatColor.LIGHT_PURPLE),
        LEGENDARY(ChatColor.GOLD);

        public ChatColor getColor();
    }

    protected Fragment(
        String id,
        String name,
        List<String> lore,
        long executionTime,
        Rarity rarity,
        Type type,
        long cooldownModifier,
        Effect effect
    );

    public final String getId();
    public final String getName();
    public final String getDisplayName();
    public final Material getMaterial(); // Always FLOW_POTTERY_SHERD
    public final List<String> getLore();
    public final boolean hasGlint(); // Always true
    public final long getExecutionTime();
    public final Rarity getRarity();
    public final Type getType();
    public final long getCooldownModifier();
    public final Effect getEffect();
    public final void execute(AbilityContext context);

}
```

`Rarity` is part of the fragment definition and is also used by the loot system when selecting a fragment.

`Type` classifies the fragment's execution style (`MELEE` or `RANGED`) and is rendered in the physical fragment item's lore (`Type: <MELEE|RANGED>`).

### `FragmentItem`

```java
package dev.xpolion.xpotriad.fragment;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class FragmentItem {

    // Must be called during plugin startup before any other method.
    // Initializes the PDC keys used to tag and read fragment items.
    public static void initialize(JavaPlugin plugin);

    public static ItemStack create(Fragment fragment);

    public static boolean isFragmentItem(ItemStack item);

    public static String getFragmentId(ItemStack item);

    public static Fragment getFragment(ItemStack item);

}
```

### `FragmentRegistry`

```java
package dev.xpolion.xpotriad.fragment;

import java.util.List;

public final class FragmentRegistry {

    /** Maximum fragments that can drop from a single loot interaction. */
    public static final int MAX_FRAGMENTS_PER_INTERACTION = 3;

    public static Fragment get(String id);

    public static Fragment roll(LootSource source);

    public static List<Fragment> roll(LootSource source, int max);

}
```

`FragmentRegistry` owns fragment definitions and ALL fragment loot logic.

Loot flow (single entry point):

```text
resolve source config (exact -> prefix -> defaults)
      |
      v
spawn chance roll
      |
      v
weighted rarity roll
      |
      v
uniform fragment of that rarity (no fragment weights)
```

If the chance roll fails, the source is ineligible, or the rolled rarity has no registered fragments, no fragment is returned.

Eligibility:

- Every `chests/...` loot-table key is eligible by default.
- All other sources (entities, non-chest dispensed loot) are opt-in: they require at least one exact or prefix rule.

#### Fragment registration

Fragments are registered by id (duplicate ids throw):

```java
register(new SpeedFragment());
register(new ExplosionFragment());
register(new MarkFragment());
```

Registering makes a fragment part of every rarity pool it belongs to. There are no per-fragment loot weights — within a selected rarity every registered fragment of that rarity is equally likely.

#### Source rules (compact config API)

Rules are declared in the static block through three private helpers:

```java
chance("chests/ancient_city", 0.40);           // spawn chance, 0.0 - 1.0
rarities("chests/ancient_city", Map.of(...));   // rarity weights
maxDrops("entities/warden", 1);                 // per-source drop cap
```

Key convention: a key ending with `/` or `_` is a **prefix rule** (matched by `startsWith`, longest match wins); any other key is an **exact rule**. Exact rules take priority over prefix rules (per field: exact value, else prefix value, else default).

Defaults when no rule matches:

| Setting  | Default                                      |
| -------- | -------------------------------------------- |
| chance   | `0.15`                                       |
| rarities | `COMMON 61 / UNCOMMON 25 / RARE 10 / EPIC 4` |
| maxDrops | `MAX_FRAGMENTS_PER_INTERACTION` (3)          |

LEGENDARY is omitted from the default rarities (= weight 0): default/common structures cannot drop Legendary. Zero weight = omit the rarity key; explicit weights must be positive.

#### Multi-drop behavior

`roll(source, max)` calls the single roll up to `max` times and **stops at the first miss** (chance failure, ineligible source, or empty rarity pool). Duplicates are allowed. A per-source `maxDrops` rule can lower — never raise — the cap:

```java
int limit = Math.min(max, resolveMaxDrops(exact, prefix));
```

Boss sources use this to drop exactly one fragment:

```java
chance("entities/warden", 1.0);
rarities("entities/warden", Map.of(Fragment.Rarity.LEGENDARY, 100));
maxDrops("entities/warden", 1);
```

Rarity weights are relative, not fixed percentages: a total of `100` with values `61, 25, 10, 4` corresponds to 61%, 25%, 10%, 4%.

### `LootSource`

```java
package dev.xpolion.xpotriad.fragment;

import org.bukkit.entity.EntityType;
import org.bukkit.loot.LootTable;

public record LootSource(Type type, String key) {

    public enum Type {
        LOOT_TABLE,
        DISPENSE_LOOT,
        ENTITY
    }

    public static LootSource lootTable(LootTable table);

    public static LootSource dispensedLoot(LootTable table);

    public static LootSource entity(EntityType entityType);

}
```

Generic representation of a loot source, independent of the event that produced it.

Key conventions (vanilla `minecraft:` namespace is stripped):

- `LOOT_TABLE` / `DISPENSE_LOOT` → actual loot-table key, e.g. `chests/ancient_city`.
- `ENTITY` → `entities/<entity id>`, e.g. `entities/zombie`.
- Non-vanilla namespaces are kept as-is (e.g. `mypack:chests/loot`).

### `FragmentLootListener`

```java
package dev.xpolion.xpotriad.fragment;

public final class FragmentLootListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event);

    @EventHandler(ignoreCancelled = true)
    public void onDispenseLoot(BlockDispenseLootEvent event);

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event);

}
```

The listener only translates Minecraft events into `LootSource` values and passes them to `FragmentRegistry` — it contains NO chance or rarity logic.

- `onLootGenerate` → `LootSource.lootTable(...)` → adds drops to `event.getLoot()` (chests, barrels, minecarts — `LootGenerateEvent` does not fire for entity or fishing loot).
- `onDispenseLoot` → `LootSource.dispensedLoot(...)` → adds drops to `event.getDispensedLoot()` (e.g. trial chamber vaults).
- `onEntityDeath` → `LootSource.entity(...)` → adds drops to `event.getDrops()`; requires a player killer (mob-vs-mob deaths are ignored).

The flow is:

```text
Loot event (chests / dispense / mob death)
      |
      v
Translate to LootSource
      |
      v
FragmentRegistry.roll(source, MAX_FRAGMENTS_PER_INTERACTION)
      |
      v
FragmentItem.create(...) for each rolled fragment
      |
      v
Add to the event's loot / drops list
```

All chance, rarity, and max-drop values live in `FragmentRegistry` configuration.

The listener is registered during plugin startup:

```java
getServer().getPluginManager().registerEvents(
    new FragmentLootListener(),
    this
);
```

### `RuntimeState`

```java
package dev.xpolion.xpotriad.runtime;

public interface RuntimeState {

    void tick();

    boolean isFinished();

    void stop();

}
```

### `RuntimeHandle`

```java
package dev.xpolion.xpotriad.runtime;

public interface RuntimeHandle {

    void stop();

    boolean isActive();

}
```

### `RuntimeManager`

```java
package dev.xpolion.xpotriad.runtime;

import org.bukkit.plugin.java.JavaPlugin;

public final class RuntimeManager {

    public RuntimeManager(JavaPlugin plugin);

    // Starts the 1-tick scheduler loop that drives registered states.
    // Must be called during plugin startup.
    public void start();

    public RuntimeHandle start(RuntimeState state);

    // Stops the scheduler loop and all active states.
    public void stop();

}
```

### `ParticleAnimation`

```java
package dev.xpolion.xpotriad.particle;

public interface ParticleAnimation {

    void render(ParticleContext context);

}
```

### `ParticleContext`

```java
package dev.xpolion.xpotriad.particle;

import org.bukkit.Location;

public final class ParticleContext {

    public Location getOrigin();

    public double getElapsedSeconds();

    public double getDeltaSeconds();

}
```

### `ParticleAttachment`

```java
package dev.xpolion.xpotriad.particle;

public enum ParticleAttachment {

    WORLD,

    ENTITY

}
```

### `ParticleHandle`

```java
package dev.xpolion.xpotriad.particle;

public interface ParticleHandle {

    void stop();

    boolean isActive();

}
```

### `ParticleSystem`

```java
package dev.xpolion.xpotriad.particle;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParticleSystem {

    public ParticleSystem(JavaPlugin plugin);

    // Starts the 1-tick scheduler loop that drives persistent animations.
    // Must be called during plugin startup.
    public void start();

    // One-shot particle animation
    public void play(ParticleAnimation animation, Location origin);

    public void play(ParticleAnimation animation, Entity entity);

    // Persistent particle animation
    public ParticleHandle playPersistent(
        ParticleAnimation animation,
        Location location,
        double durationSeconds
    );

    public ParticleHandle playPersistent(
        ParticleAnimation animation,
        Entity entity,
        double durationSeconds
    );

    public ParticleHandle playPersistent(
        ParticleAnimation animation,
        ParticleAttachment attachment,
        Location location,
        Entity entity,
        double durationSeconds
    );

    // Stops the scheduler loop and all active persistent animations.
    public void stop();

}
```

---

## 3. Ability Activation

`AbilityListener` handles right-click activation of engraved ability items.

### Activation rules

- Protection plugins are respected through `event.useItemInHand()`.
- The item must contain a valid ability.
- The ability must be in the `READY` state.
- The item's cooldown is checked before execution.
- The cooldown is applied only after the ability executes successfully.
- A failed execution does not burn the cooldown.
- Vanilla interaction is cancelled only when an ability actually fires.
- Cooldown and activation state can be displayed in the action bar.

The activation result can represent four states:

```java
private enum State {
    EMPTY,
    READY,
    COOLDOWN,
    FIRED
}
```

### Action-bar feedback

Off-hand is represented by `<` and main-hand by `>`.

Examples:

```text
< 2.0s | Fired >
< Fired | 1.5s >
< Fired | Ready >
< Fired
  Ready >
```

Cooldown values are displayed in seconds with one decimal place.

---

## 4. End-to-End Ability & Fragment Construction Example

The following example demonstrates how an ability can combine an `Effect`, `Fragment`, `FragmentItem`, `RuntimeState`, and entity-attached `ParticleAnimation`.

```java
import dev.xpolion.xpotriad.ability.Ability;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.ability.AbilityItem;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentItem;
import dev.xpolion.xpotriad.particle.*;
import dev.xpolion.xpotriad.runtime.*;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

// 1. One-shot particle animation

public class SparkleBurstAnimation implements ParticleAnimation {

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        origin.getWorld().spawnParticle(
            Particle.END_ROD,
            origin,
            20,
            0.5,
            0.5,
            0.5,
            0.1
        );
    }

}

// 2. Persistent procedural particle animation

public class OrbitRingAnimation implements ParticleAnimation {

    private final double radius = 1.2;

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin().add(0, 1.0, 0);

        double angle = context.getElapsedSeconds() * 5.0;

        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;

        origin.getWorld().spawnParticle(
            Particle.FLAME,
            origin.add(x, 0, z),
            1,
            0,
            0,
            0,
            0
        );
    }

}

// 3. Persistent gameplay runtime state

public class EmpoweredState implements RuntimeState {

    private final Player target;
    private final ParticleHandle particleHandle;

    private double durationSeconds = 5.0;
    private boolean finished = false;

    public EmpoweredState(
        Player target,
        ParticleSystem particleSystem
    ) {
        this.target = target;

        this.particleHandle = particleSystem.playPersistent(
            new OrbitRingAnimation(),
            target,
            durationSeconds
        );
    }

    @Override
    public void tick() {
        if (!target.isValid() || target.isDead()) {
            stop();
            return;
        }

        durationSeconds -= 0.05;

        if (durationSeconds <= 0) {
            stop();
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void stop() {
        finished = true;

        if (particleHandle.isActive()) {
            particleHandle.stop();
        }
    }

}

// 4. Ability effect entry point

public class EmpowerEffect implements Effect {

    private final RuntimeManager runtimeManager;
    private final ParticleSystem particleSystem;

    public EmpowerEffect(
        RuntimeManager runtimeManager,
        ParticleSystem particleSystem
    ) {
        this.runtimeManager = runtimeManager;
        this.particleSystem = particleSystem;
    }

    @Override
    public void apply(AbilityContext context) {
        Player player = context.getSource();

        particleSystem.play(
            new SparkleBurstAnimation(),
            player.getLocation()
        );

        RuntimeState state = new EmpoweredState(
            player,
            particleSystem
        );

        RuntimeHandle handle = runtimeManager.start(state);
    }

}

// 5. Fragment class wrapping the effect

public class EmpowerFragment extends Fragment {

    public EmpowerFragment(
        RuntimeManager runtimeManager,
        ParticleSystem particleSystem
    ) {
        super(
            "empower",
            "Empowerment Fragment",
            List.of(ChatColor.GRAY + "Grants a temporary fire aura."),
            10,
            Rarity.RARE,
            Type.MELEE,
            5,
            new EmpowerEffect(
                runtimeManager,
                particleSystem
            )
        );
    }

}

// 6. Creating fragment items and engraving onto weapons

public class AbilityBuilderExample {

    public static void giveEmpoweredWeapon(
        Player player,
        EmpowerFragment fragment
    ) {
        ItemStack fragmentItem = FragmentItem.create(fragment);

        player.getInventory().addItem(fragmentItem);

        Ability ability = new Ability();

        ability.setFragment(
            Ability.Stage.CAST,
            fragment
        );

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);

        AbilityItem.engrave(
            sword,
            ability
        );

        player.getInventory().addItem(sword);
    }

}
```

---

## 5. Fragment Loot Configuration

Fragment loot is driven by source rules in `FragmentRegistry`. Roll order:

1. Resolve rules for the source key (exact → prefix → defaults).
2. Roll the fragment spawn chance. On a miss, return nothing.
3. Roll the rarity using the resolved weights.
4. Select a registered fragment of that rarity uniformly at random (no fragment-level weights).
5. Repeat up to the resolved max-drop cap, stopping at the first miss.

### 5.1 Source rules

```java
// exact key (no trailing '/' or '_')
chance("chests/ancient_city", 0.40);
rarities("chests/ancient_city", Map.of(...));

// prefix key (trailing '/' or '_') — covers a whole group
rarities("chests/trial_chambers/", Map.of(...));
chance("chests/bastion_", 0.30);

// per-source drop cap (bosses: exactly one)
maxDrops("entities/warden", 1);
```

### 5.2 Eligibility and current configuration

Every vanilla `chests/...` table is eligible by default (15% chance, `61/25/10/4`, no Legendary). Non-chest sources are opt-in — each rule below also makes its source eligible. Unlisted mobs (including Ghast, Piglin, and Zombified Piglin) never drop fragments.

#### Structure loot

| Source | Key rule | Chance | COMMON | UNCOMMON | RARE | EPIC | LEGENDARY |
| ------ | -------- | ------ | ------ | -------- | ---- | ---- | --------- |
| Default structures | *(defaults)* | 15% | 61 | 25 | 10 | 4 | 0 |
| Village | `chests/village/` (prefix) | 15% | 55 | 30 | 10 | 4 | 1 |
| Nether Fortress | `chests/nether_bridge` | 20% | 45 | 30 | 15 | 8 | 2 |
| Woodland Mansion | `chests/woodland_mansion` | 25% | 30 | 25 | 20 | 22 | 3 |
| Trial Chambers | `chests/trial_chambers/` (prefix) | 25% | 30 | 25 | 20 | 22 | 3 |
| Bastion | `chests/bastion_` (prefix) | 30% | 30 | 25 | 25 | 15 | 5 |
| End City | `chests/end_city_treasure` | 35% | 20 | 20 | 33 | 20 | 7 |
| Ancient City | `chests/ancient_city` + `chests/ancient_city_ice_box` | 40% | 15 | 15 | 30 | 30 | 10 |

#### Mob loot

| Mob | Key | Chance | COMMON | UNCOMMON | RARE | EPIC | LEGENDARY |
| --- | --- | ------ | ------ | -------- | ---- | ---- | --------- |
| Warden / Ender Dragon / Wither | `entities/warden`, `entities/ender_dragon`, `entities/wither` | 100% | 0 | 0 | 0 | 0 | 100 |
| Ravager | `entities/ravager` | 35% | 10 | 20 | 25 | 40 | 5 |
| Evoker | `entities/evoker` | 35% | 10 | 20 | 25 | 40 | 5 |
| Elder Guardian | `entities/elder_guardian` | 25% | 5 | 20 | 45 | 25 | 5 |
| Piglin Brute | `entities/piglin_brute` | 20% | 5 | 25 | 50 | 18 | 2 |
| Shulker | `entities/shulker` | 15% | 5 | 25 | 50 | 18 | 2 |
| Blaze | `entities/blaze` | 12% | 5 | 30 | 45 | 18 | 2 |
| Wither Skeleton | `entities/wither_skeleton` | 15% | 5 | 30 | 45 | 18 | 2 |
| Pillager | `entities/pillager` | 8% | 5 | 50 | 35 | 9 | 1 |
| Vindicator | `entities/vindicator` | 10% | 5 | 40 | 40 | 14 | 1 |
| Zombie | `entities/zombie` | 0.5% | 70 | 25 | 5 | 0 | 0 |
| Skeleton | `entities/skeleton` | 0.5% | 70 | 25 | 5 | 0 | 0 |
| Creeper | `entities/creeper` | 0.5% | 70 | 25 | 5 | 0 | 0 |

Notes:

- Weights are relative and do not need to sum to 100; every configured row above sums to 100.
- Legendary is capped at 10% for normal weight-ratio sources; bosses are exempt and always drop exactly one Legendary fragment (`maxDrops 1`).
- Zero values are expressed by omitting the rarity key in code (e.g. default structures simply have no `LEGENDARY` entry).
- Rarity progression: rarer/harder structures get higher fragment chances (15% → 40%).

---

## 6. Plugin Startup Integration

`Main` registers `FragmentLootListener` during plugin startup:

```java
getServer().getPluginManager().registerEvents(
    new FragmentLootListener(),
    this
);
```

This keeps generated-loot handling separate from fragment definition and loot selection.

```text
Main
 |
 +-- FragmentRegistry
 |     |
 |     +-- Fragment definitions
 |     +-- Source rules (chance, rarities, maxDrops)
 |
 +-- FragmentLootListener
       |
       +-- LootGenerateEvent
       +-- BlockDispenseLootEvent
       +-- EntityDeathEvent
       +-- Translate -> LootSource
       +-- roll(source, MAX_FRAGMENTS_PER_INTERACTION)
       +-- FragmentItem.create(...)
```

---

## 7. Current Ability Development Flow

Ability execution and fragment acquisition are separate systems.

### Ability execution

```text
Right-click engraved ability item
        |
        v
AbilityListener
        |
        +-- Protection check
        |
        +-- Read ability
        |
        +-- Check cooldown
        |
        v
AbilityEngine
        |
        v
AbilityContext
        |
        v
Fragment.execute(...)
        |
        v
Effect.apply(...)
        |
        +-- Immediate gameplay effect
        |
        +-- Optional RuntimeState
        |
        +-- Optional persistent ParticleHandle
```

### Fragment acquisition

```text
Loot event (chests / dispense / mob death)
        |
        v
FragmentLootListener
        |
        +-- Translate event -> LootSource
        |
        v
FragmentRegistry.roll(source, 3)
        |
        +-- Eligibility (chests by default, others opt-in)
        +-- Spawn chance
        +-- Roll rarity (weighted)
        +-- Uniform fragment of that rarity
        +-- Repeat up to max drops (stop at first miss)
        |
        v
FragmentItem.create(...) per drop
        |
        v
Generated loot / drops
```

The loot system determines how fragments enter the game. The ability system determines how those fragments behave after being engraved into an ability.
