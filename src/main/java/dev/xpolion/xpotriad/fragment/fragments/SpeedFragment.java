package dev.xpolion.xpotriad.fragment.fragments;

import org.bukkit.ChatColor;

import dev.xpolion.xpotriad.effect.effects.SpeedEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;

import java.util.List;

/**
 * Grants the source player a speed boost.
 *
 * executionTime    = 20 ticks
 * rarity           = COMMON
 * cooldownModifier = 0 ticks (no change to base cooldown)
 */
public final class SpeedFragment extends Fragment {

    public SpeedFragment() {
        super(
                "speed",
                "Speed Fragment",
                List.of(ChatColor.GRAY + "Grants increased movement speed."),
                20L,
                Rarity.COMMON,
                0L,
                new SpeedEffect()
        );
    }
}