package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.effects.InvisibilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Material;

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
                ChatColor.LIGHT_PURPLE + "Invisibility Fragment",
                Material.AMETHYST_SHARD,
                List.of(ChatColor.GRAY + "Grants invisibility."),
                true,
                10L,
                Rarity.UNCOMMON,
                10L,
                new InvisibilityEffect()
        );
    }
}