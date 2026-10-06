package dev.xpolion.xpotriad.fragment.fragments;

import org.bukkit.ChatColor;

import dev.xpolion.xpotriad.effect.effects.HealEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import java.util.List;

/**
 * Grants the source player regeneration.
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = 0 ticks (no change to base cooldown)
 */
public final class HealFragment extends Fragment {

    public HealFragment() {
        super(
                "heal",
                "Heal Fragment",
                List.of(ChatColor.GRAY + "Grants regeneration."),
                10L,
                Rarity.COMMON,
                Type.MELEE,
                0L,
                new HealEffect()
        );
    }
}