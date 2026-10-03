package dev.xpolion.xpotriad.targeting;

import dev.xpolion.xpotriad.ability.AbilityContext;
import org.bukkit.entity.Entity;

import java.util.List;

public interface Targeting {

    TargetingType getType();

    List<Entity> resolve(AbilityContext context);
}
