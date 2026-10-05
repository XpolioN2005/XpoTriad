# XpoTriad Engine API & Ability Development Guide

> API specification for authoring abilities, fragments, fragment items, effects, runtime states, and particle animations in XpoTriad.

---

## 1. Engine Interfaces & Component Summary

| Interface / Class | Category | Role |
|---|---|---|
| `Effect` | Gameplay | One-shot execution logic triggered during ability application. |
| `Fragment` | Gameplay | Immutable definition of a gameplay unit executed per stage (wraps `Effect`, execution timing, lore, rarity color formatting, and cooldown modifiers). |
| `FragmentItem` | Items & PDC | Factory and utility to generate physical `ItemStack` representations (`FLOW_POTTERY_SHERD` with glint) and inspect PDC metadata. |
| `FragmentRegistry` | Registry | Global registry lookup mapping fragment IDs to `Fragment` instances. |
| `AbilityContext` | Gameplay | Context container providing execution source (`Player`) and ability info. |
| `RuntimeState` | Gameplay | Persistent gameplay condition surviving beyond initial `Effect.apply()`. |
| `RuntimeHandle` | Gameplay | Idempotent control handle for registered `RuntimeState` instances. |
| `RuntimeManager` | Engine Service | Entry point to register and start `RuntimeState` instances. |
| `ParticleAnimation` | Visuals | Frame-based render contract for particle visual effects. |
| `ParticleContext` | Visuals | Immutable frame snapshot containing resolved origin, total elapsed time, and frame delta. |
| `ParticleAttachment` | Visuals | Enum specifying target tracking (`WORLD` for fixed coordinates, `ENTITY` for dynamic entity tracking). |
| `ParticleHandle` | Visuals | Idempotent control handle for persistent particle animations. |
| `ParticleSystem` | Engine Service | Entry point to play one-shot or persistent particle animations. |

---

## 2. Interface Specifications

### `Effect`
```java
package dev.xpolion.xpotriad.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;

public interface Effect {
    void apply(AbilityContext context);
}
```

### `Fragment`
```java
package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effects.Effect;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.util.List;

public abstract class Fragment {

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
        long cooldownModifier,
        Effect effect
    );

    public final String getId();
    public final String getName();
    public final String getDisplayName();
    public final Material getMaterial(); // Always FLOW_POTTERY_SHERD
    public final List<String> getLore();
    public final boolean hasGlint();     // Always true
    public final long getExecutionTime();
    public final Rarity getRarity();
    public final long getCooldownModifier();
    public final Effect getEffect();
    public final void execute(AbilityContext context);
}
```

### `FragmentItem`
```java
package dev.xpolion.xpotriad.fragment;

import org.bukkit.inventory.ItemStack;

public final class FragmentItem {

    public static ItemStack create(Fragment fragment);
    public static boolean isFragmentItem(ItemStack item);
    public static String getFragmentId(ItemStack item);
    public static Fragment getFragment(ItemStack item);
}
```

### `FragmentRegistry`
```java
package dev.xpolion.xpotriad.fragment;

public final class FragmentRegistry {

    public static Fragment get(String id);
}
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

public final class RuntimeManager {
    public RuntimeHandle start(RuntimeState state);
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

public final class ParticleSystem {
    // One-shot particle animation
    public void play(ParticleAnimation animation, Location origin);
    public void play(ParticleAnimation animation, Entity entity);

    // Persistent particle animation
    public ParticleHandle playPersistent(ParticleAnimation animation, Location location, double durationSeconds);
    public ParticleHandle playPersistent(ParticleAnimation animation, Entity entity, double durationSeconds);
    public ParticleHandle playPersistent(
        ParticleAnimation animation,
        ParticleAttachment attachment,
        Location location,
        Entity entity,
        double durationSeconds
    );
}
```

---

## 3. End-to-End Ability & Fragment Construction Example

Below is a complete implementation example demonstrating how a developer uses the Engine API to construct an ability by wiring an **`Effect`**, a **`Fragment`**, physical **`FragmentItem`** generation, a persistent **`RuntimeState`**, and an entity-attached **`ParticleAnimation`**.

```java
import dev.xpolion.xpotriad.ability.Ability;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.ability.AbilityItem;
import dev.xpolion.xpotriad.effects.Effect;
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

// 1. One-Shot Particle Animation
public class SparkleBurstAnimation implements ParticleAnimation {
    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();
        origin.getWorld().spawnParticle(Particle.END_ROD, origin, 20, 0.5, 0.5, 0.5, 0.1);
    }
}

// 2. Persistent Procedural Particle Animation
public class OrbitRingAnimation implements ParticleAnimation {
    private final double radius = 1.2;

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin().add(0, 1.0, 0);
        double angle = context.getElapsedSeconds() * 5.0; // Procedural rotation based on elapsed time

        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;

        origin.getWorld().spawnParticle(Particle.FLAME, origin.add(x, 0, z), 1, 0, 0, 0, 0);
    }
}

// 3. Persistent Gameplay Runtime State
public class EmpoweredState implements RuntimeState {
    private final Player target;
    private final ParticleHandle particleHandle;
    private double durationSeconds = 5.0;
    private boolean finished = false;

    public EmpoweredState(Player target, ParticleSystem particleSystem) {
        this.target = target;
        // Attach persistent visual animation to the target Entity
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
            particleHandle.stop(); // Stop visual animation when gameplay state ends
        }
    }
}

// 4. Ability Effect Entry Point
public class EmpowerEffect implements Effect {
    private final RuntimeManager runtimeManager;
    private final ParticleSystem particleSystem;

    public EmpowerEffect(RuntimeManager runtimeManager, ParticleSystem particleSystem) {
        this.runtimeManager = runtimeManager;
        this.particleSystem = particleSystem;
    }

    @Override
    public void apply(AbilityContext context) {
        Player player = context.getSource();

        // Execute immediate one-shot visual
        particleSystem.play(new SparkleBurstAnimation(), player.getLocation());

        // Start persistent gameplay condition and visual tracking
        RuntimeState state = new EmpoweredState(player, particleSystem);
        RuntimeHandle handle = runtimeManager.start(state);
    }
}

// 5. Fragment Class wrapping the Effect
public class EmpowerFragment extends Fragment {
    public EmpowerFragment(RuntimeManager runtimeManager, ParticleSystem particleSystem) {
        super(
            "empower",
            "Empowerment Fragment",    // Name without manual ChatColor (color derived from Rarity)
            List.of(ChatColor.GRAY + "Grants a temporary fire aura."),
            10,                         // executionTime (ticks)
            Rarity.RARE,                // Rarity tier (determines name color -> ChatColor.AQUA)
            5,                          // cooldownModifier (+5 ticks)
            new EmpowerEffect(runtimeManager, particleSystem)
        );
    }
}

// 6. Creating Fragment Items & Engraving onto Weapons
public class AbilityBuilderExample {

    public static void giveEmpoweredWeapon(
        Player player,
        EmpowerFragment fragment
    ) {
        // Create physical Fragment ItemStack (Material.FLOW_POTTERY_SHERD with glint)
        ItemStack fragmentItem = FragmentItem.create(fragment);
        player.getInventory().addItem(fragmentItem);

        // Build 3-Stage Ability and Engrave onto Melee Weapon
        Ability ability = new Ability(Ability.WeaponType.MELEE);
        ability.setFragment(Ability.Stage.CAST, fragment);

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        AbilityItem.engrave(sword, ability);

        player.getInventory().addItem(sword);
    }
}
```
