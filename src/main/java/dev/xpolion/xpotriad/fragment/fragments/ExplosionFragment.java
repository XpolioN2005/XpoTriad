package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.effects.ExplosionEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Creates an explosion near the source player.
 *
 * executionTime    = 0 ticks
 * rarity           = RARE
 * type             = MELEE
 * cooldownModifier = +40 ticks
 */
public final class ExplosionFragment extends Fragment {

    public ExplosionFragment() {
        super(
                "explosion",
                "Explosion Fragment",
                List.of(Component.text("Creates an explosion.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("explosion", 0L),
                Rarity.RARE,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("explosion", 40L),
                new ExplosionEffect()
        );
    }
}