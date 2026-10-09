package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.LineSnipeEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Fires a fast piercing shot at the first target in line.
 *
 * executionTime    = 8 ticks
 * rarity           = RARE
 * type             = RANGED
 * cooldownModifier = +10 ticks
 * target           = LINE
 * effect           = LineSnipeState runtime (30 blocks, 20 ticks, damage 8)
 */
public final class LineSnipeFragment extends Fragment {

    public LineSnipeFragment() {
        super(
                "line_snipe",
                "Line Snipe Fragment",
                List.of(Component.text("Fires a piercing shot at the first target in line.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("line_snipe", 8L),
                Rarity.RARE,
                Type.RANGED,
                BalanceConfig.get().cooldownModifier("line_snipe", 10L),
                new LineSnipeEffect()
        );
    }
}