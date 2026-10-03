package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effects.ExplosionEffect;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public final class ExplosionFragment extends Fragment {

    private final ExplosionEffect effect = new ExplosionEffect();

    public ExplosionFragment() {
        super(
                "explosion",
                ChatColor.RED + "Explosion Fragment",
                Material.FIRE_CHARGE,
                List.of(
                        ChatColor.GRAY + "Creates an explosion."
                ),
                true,
                0L
        );
    }

    @Override
    public void execute(AbilityContext context) {
        effect.apply(context);
    }
}