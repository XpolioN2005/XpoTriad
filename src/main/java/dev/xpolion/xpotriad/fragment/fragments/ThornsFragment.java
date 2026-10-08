package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.ThornsEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Reflects 30% of incoming melee damage back to the attacker for 100 ticks.
 *
 * executionTime    = 10 ticks
 * rarity           = UNCOMMON
 * type             = MELEE
 * cooldownModifier = +7 ticks
 * target           = SELF
 * effect           = ThornsState runtime (waits for incoming attacks)
 */
public final class ThornsFragment extends Fragment {

    public ThornsFragment() {
        super(
                "thorns",
                "Thorns Fragment",
                List.of(Component.text("Returns damage to attackers.", NamedTextColor.GRAY)),
                10L,
                Rarity.UNCOMMON,
                Type.MELEE,
                7L,
                new ThornsEffect()
        );
    }
}