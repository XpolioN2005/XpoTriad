package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.BarrierEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Creates a temporary circular boundary around the selected point.
 *
 * executionTime    = 12 ticks
 * rarity           = RARE
 * type             = MELEE
 * cooldownModifier = +12 ticks
 * target           = POINT
 * effect           = BarrierState runtime (r3, 100 ticks, prevents crossing)
 */
public final class BarrierFragment extends Fragment {

    public BarrierFragment() {
        super(
                "barrier",
                "Barrier Fragment",
                List.of(Component.text("Creates a temporary boundary.", NamedTextColor.GRAY)),
                12L,
                Rarity.RARE,
                Type.MELEE,
                12L,
                new BarrierEffect()
        );
    }
}