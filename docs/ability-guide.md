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
| `FragmentRegistry`     | Registry       | Global fragment lookup plus weighted rarity and fragment loot selection for registered loot tables.                                    |
| `FragmentLootListener` | Loot           | Injects a rolled fragment item into supported generated loot according to the configured table chance.                                 |
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

import org.bukkit.loot.LootTables;

public final class FragmentRegistry {

    public static Fragment get(String id);

    public static Fragment rollLoot(LootTables lootTable);

}
```

`FragmentRegistry` owns fragment definitions and weighted loot configuration.

Loot selection is performed in two stages:

1. Roll a `Fragment.Rarity` using the rarity weights configured for the loot table.
2. Roll a fragment from that loot table whose `Fragment.getRarity()` matches the selected rarity.

If the selected rarity has no registered fragments for that loot table, no fragment is returned.

#### Fragment registration

Fragments can be registered with loot entries through the compact chained API:

```java
register(new SpeedFragment())
    .loot(LootTables.ANCIENT_CITY, 10);

register(new ExplosionFragment())
    .loot(LootTables.ANCIENT_CITY, 5);

register(new MarkFragment())
    .loot(LootTables.ANCIENT_CITY, 2)
    .loot(LootTables.ANCIENT_CITY_ICE_BOX, 5);
```

The registry stores the fragment by its ID and returns a registration entry so loot configuration can be chained.

#### Rarity registration

Rarity weights are configured separately:

```java
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.COMMON, 60);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.UNCOMMON, 25);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.RARE, 10);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.EPIC, 4);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.LEGENDARY, 1);
```

Weights are relative, not fixed percentages. A total weight of `100` with values `60, 25, 10, 4, 1` corresponds to 60%, 25%, 10%, 4%, and 1%.

Fragment loot weights are likewise relative within the selected rarity.

Both rarity weights and fragment loot weights must be positive.

### `FragmentLootListener`

```java
package dev.xpolion.xpotriad.fragment;

public final class FragmentLootListener implements Listener {

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event);

}
```

`FragmentLootListener` hooks into `LootGenerateEvent` and injects a fragment item into supported generated loot.

The flow is:

```text
LootGenerateEvent
      |
      v
Resolve LootTables
      |
      v
Check table drop chance
      |
      v
FragmentRegistry.rollLoot(...)
      |
      v
FragmentItem.create(...)
      |
      v
Add to generated loot
```

The current implementation defines:

```java
private static final double ANCIENT_CITY_CHANCE = 0.15;
```

Unsupported loot tables currently have a chance of `0.0`.

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

Fragment loot has two independent configuration layers.

### 5.1 Loot-table rarity weights

These weights decide which rarity is selected first:

```java
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.COMMON, 60);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.UNCOMMON, 25);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.RARE, 10);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.EPIC, 4);
rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.LEGENDARY, 1);
```

### 5.2 Fragment weights

These weights decide which fragment is selected after the rarity is known:

```java
register(new SpeedFragment())
    .loot(LootTables.ANCIENT_CITY, 10);

register(new ExplosionFragment())
    .loot(LootTables.ANCIENT_CITY, 5);

register(new MarkFragment())
    .loot(LootTables.ANCIENT_CITY, 2)
    .loot(LootTables.ANCIENT_CITY_ICE_BOX, 5);
```

A fragment's own `Rarity` determines which rarity bucket it belongs to. The `.loot(...)` weight controls its relative chance among fragments of that rarity.

Registering a fragment does not automatically make it obtainable from loot. It must also be attached to a loot table with `.loot(...)`.

The current registry configuration contains:

- `SpeedFragment` in `ANCIENT_CITY` with weight `10`.
- `ExplosionFragment` in `ANCIENT_CITY` with weight `5`.
- `MarkFragment` in `ANCIENT_CITY` with weight `2`.
- `MarkFragment` in `ANCIENT_CITY_ICE_BOX` with weight `5`.
- `ANCIENT_CITY` rarity weights of `60 / 25 / 10 / 4 / 1` for `COMMON / UNCOMMON / RARE / EPIC / LEGENDARY`.

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
 |     +-- Rarity weights
 |     +-- Fragment loot weights
 |
 +-- FragmentLootListener
       |
       +-- LootGenerateEvent
       +-- Table chance
       +-- rollLoot(...)
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
LootGenerateEvent
        |
        v
FragmentLootListener
        |
        +-- Check loot-table chance
        |
        v
FragmentRegistry.rollLoot(...)
        |
        +-- Roll rarity
        |
        +-- Roll matching fragment
        |
        v
FragmentItem.create(...)
        |
        v
Generated loot
```

The loot system determines how fragments enter the game. The ability system determines how those fragments behave after being engraved into an ability.
