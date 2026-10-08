package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.MeteorEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Launches a giant fireball toward the selected point: 20 damage,
 * 4-block AOE, 40-tick lifetime, no terrain destruction.
 *
 * executionTime    = 20 ticks
 * rarity           = LEGENDARY
 * type             = RANGED
 * cooldownModifier = +25 ticks
 * target           = POINT
 * effect           = MeteorState runtime (tracks the projectile)
 */
public final class MeteorFragment extends Fragment {

    public MeteorFragment() {
        super(
                "meteor",
                "Meteor Fragment",
                List.of(Component.text("Calls down a devastating meteor.", NamedTextColor.GRAY)),
                20L,
                Rarity.LEGENDARY,
                Type.RANGED,
                25L,
                new MeteorEffect()
        );
    }
}