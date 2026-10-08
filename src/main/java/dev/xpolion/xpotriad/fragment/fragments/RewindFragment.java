package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.RewindEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Snapshots the caster's position and health at activation and restores
 * them 100 ticks later.
 *
 * executionTime    = 15 ticks
 * rarity           = LEGENDARY
 * type             = MELEE
 * cooldownModifier = +20 ticks
 * target           = SELF
 * effect           = RewindState runtime (single snapshot, trigger at 100 ticks)
 */
public final class RewindFragment extends Fragment {

    public RewindFragment() {
        super(
                "rewind",
                "Rewind Fragment",
                List.of(Component.text("Reverts you to a previous state.", NamedTextColor.GRAY)),
                15L,
                Rarity.LEGENDARY,
                Type.MELEE,
                20L,
                new RewindEffect()
        );
    }
}