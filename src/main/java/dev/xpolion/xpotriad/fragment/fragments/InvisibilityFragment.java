package dev.xpolion.xpotriad.fragment.fragments;

import org.bukkit.ChatColor;

import dev.xpolion.xpotriad.effect.effects.InvisibilityEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;

import java.util.List;

/**
 * Grants the source player invisibility.
 *
 * executionTime    = 10 ticks
 * rarity           = UNCOMMON
 * cooldownModifier = +10 ticks
 */
public final class InvisibilityFragment extends Fragment {

    public InvisibilityFragment() {
        super(
                "invisibility",
                "Invisibility Fragment",
                List.of(ChatColor.GRAY + "Grants invisibility."),
                10L,
                Rarity.UNCOMMON,
                10L,
                new InvisibilityEffect()
        );
    }
}