package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.CheatDeathEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Grants complete incoming damage immunity for 40 ticks.
 *
 * executionTime    = 8 ticks
 * rarity           = LEGENDARY
 * type             = MELEE
 * cooldownModifier = +20 ticks
 * target           = SELF
 * effect           = CheatDeathState runtime (intercepts damage while active)
 */
public final class CheatDeathFragment extends Fragment {

    public CheatDeathFragment() {
        super(
                "cheat_death",
                "Cheat Death Fragment",
                List.of(Component.text("Become immune to damage for a brief moment.", NamedTextColor.GRAY)),
                8L,
                Rarity.LEGENDARY,
                Type.MELEE,
                20L,
                new CheatDeathEffect()
        );
    }
}