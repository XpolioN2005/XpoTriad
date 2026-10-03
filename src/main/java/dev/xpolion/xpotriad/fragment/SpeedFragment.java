package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effects.SpeedEffect;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public final class SpeedFragment extends Fragment {

    private final SpeedEffect effect = new SpeedEffect();

    public SpeedFragment() {
        super(
                "speed",
                ChatColor.AQUA + "Speed Fragment",
                Material.FEATHER,
                List.of(
                        ChatColor.GRAY + "Grants increased movement speed."
                ),
                true,
                20L
        );
    }

    @Override
    public void execute(AbilityContext context) {
        effect.apply(context);
    }
}