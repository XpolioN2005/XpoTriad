package dev.xpolion.xpotriad.fragment.fragments;

import org.bukkit.ChatColor;

import dev.xpolion.xpotriad.effect.effects.ExplosionEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

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
                List.of(ChatColor.GRAY + "Creates an explosion."),
                0L,
                Rarity.RARE,
                Type.MELEE,
                40L,
                new ExplosionEffect()
        );
    }
}