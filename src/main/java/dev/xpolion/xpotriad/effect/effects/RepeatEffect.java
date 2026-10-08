package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.fragment.Fragment;

/**
 * Re-executes the previous applicable fragment one additional time.
 *
 * Targeting: NONE — the repeated fragment re-resolves its own target
 * against the SAME AbilityContext (same source, same ability).
 *
 * Chain depth is 1: a Repeat never executes another Repeat, and no
 * independent runtime is created by Repeat itself. If the repeated
 * fragment needs a runtime, it creates its own exactly as it did the
 * first time.
 */
public final class RepeatEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Fragment previous = context.getLastExecutedFragment();

        if (previous == null) {
            return; // Nothing executed yet this activation.
        }

        if (previous.getEffect() instanceof RepeatEffect) {
            return; // Depth guard: Repeat never executes a Repeat.
        }

        previous.execute(context);
    }
}