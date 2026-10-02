package dev.xpolion.xpotriad.effects;

import dev.xpolion.xpotriad.AbilityContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ExplosionEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player player = context.getPlayer();
        Location location = player.getLocation();

        location.getWorld().createExplosion(
            location,
            4.0f,
            false,
            false,
            player
        );
    }
}