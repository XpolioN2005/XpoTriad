package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;

/**
 * Delays the next stage by holding the engine.
 *
 * Targeting: NONE. No gameplay effect and NO runtime are created:
 * the AbilityEngine naturally holds progression for this fragment's
 * executionTime (20 ticks) plus its fixed stage buffer before advancing.
 */
public final class DelayEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        // Intentionally empty — the engine's executionTime hold is the delay.
    }
}