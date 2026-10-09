package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks summoned wolves and removes them when their lifetime ends.
 *
 * Lifetime = 200 ticks. Lifecycle: start -> count down -> 200 ticks ->
 * cleanup (removes every summoned wolf). Early stop: the caster becomes
 * invalid (wolves are still removed). No listeners or particles held.
 */
public final class HoundCallState implements RuntimeState {

    private final Player source;
    private final List<Wolf> wolves;

    private int ticksRemaining;
    private volatile boolean finished = false;

    public HoundCallState(Player source, List<Wolf> wolves, JavaPlugin plugin, int lifetimeTicks) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (wolves == null) {
            throw new IllegalArgumentException("Wolves cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        this.source = source;
        this.wolves = new ArrayList<>(wolves);
        this.ticksRemaining = lifetimeTicks;
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        if (!source.isValid() || source.isDead()) {
            stop();
            return;
        }

        ticksRemaining--;

        if (ticksRemaining <= 0) {
            stop();
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void stop() {
        if (finished) {
            return;
        }

        finished = true;

        for (Wolf wolf : wolves) {
            if (wolf != null && wolf.isValid()) {
                wolf.remove();
            }
        }

        wolves.clear();
    }
}