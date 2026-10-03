package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.effects.ExplosionEffect;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

/**
 * Creates an explosion near the source player.
 *
 * executionTime    = 0 ticks
 * rarity           = RARE
 * cooldownModifier = +40 ticks
 */
public final class ExplosionFragment extends Fragment {

    public ExplosionFragment() {
        super(
                "explosion",
                ChatColor.RED + "Explosion Fragment",
                Material.FIRE_CHARGE,
                List.of(ChatColor.GRAY + "Creates an explosion."),
                true,
                0L,
                Rarity.RARE,
                40L,
                new ExplosionEffect()
        );
    }
}