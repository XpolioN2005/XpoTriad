package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effects.InvisibilityEffect;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public final class InvisibilityFragment extends Fragment {

    private final InvisibilityEffect effect = new InvisibilityEffect();

    public InvisibilityFragment() {
        super(
                "invisibility",
                ChatColor.LIGHT_PURPLE + "Invisibility Fragment",
                Material.AMETHYST_SHARD,
                List.of(
                        ChatColor.GRAY + "Grants invisibility."
                ),
                true,
                10L
        );
    }

    @Override
    public void execute(AbilityContext context) {
        effect.apply(context);
    }
}