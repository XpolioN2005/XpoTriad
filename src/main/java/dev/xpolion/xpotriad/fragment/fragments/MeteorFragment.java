package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.MeteorEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Bombards a static zone around the caster: count meteors fall from the
 * sky into zone-radius, each dealing AOE damage with no terrain damage.
 *
 * executionTime    = 20 ticks
 * rarity           = LEGENDARY
 * type             = RANGED
 * cooldownModifier = +25 ticks
 * target           = SELF (static zone fixed at the cast position)
 * effect           = MeteorState runtime (schedules the barrage)
 */
public final class MeteorFragment extends Fragment {

    public MeteorFragment() {
        super(
                "meteor",
                "Meteor Fragment",
                List.of(Component.text("Calls down a devastating meteor.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("meteor", 20L),
                Rarity.LEGENDARY,
                Type.RANGED,
                BalanceConfig.get().cooldownModifier("meteor", 25L),
                new MeteorEffect()
        );
    }
}