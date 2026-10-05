package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

/**
 * Immutable definition of a gameplay unit executed per stage.
 *
 * Fields:
 *   id               – registry key
 *   name             – raw display name
 *   displayName      – coloured display name based on rarity
 *   material         – physical item material (FLOW_POTTERY_SHERD for all fragments)
 *   lore             – physical item lore lines
 *   glint            – enchantment glint override (always true)
 *   executionTime    – minimum ticks this fragment occupies (engine adds 5-tick buffer)
 *   rarity           – rarity tier of this fragment
 *   type             – fragment execution classification (MELEE or RANGED)
 *   cooldownModifier – ticks added to (or subtracted from) the base ability cooldown
 *   effect           – the Effect that runs when this fragment executes
 */
public abstract class Fragment {

    public enum Type {
        MELEE,
        RANGED
    }

    public enum Rarity {
        COMMON(ChatColor.WHITE),
        UNCOMMON(ChatColor.GREEN),
        RARE(ChatColor.AQUA),
        EPIC(ChatColor.LIGHT_PURPLE),
        LEGENDARY(ChatColor.GOLD);

        private final ChatColor color;

        Rarity(ChatColor color) {
            this.color = color;
        }

        public ChatColor getColor() {
            return color;
        }
    }

    private static final Material DEFAULT_MATERIAL = Material.FLOW_POTTERY_SHERD;

    private final String id;
    private final String name;
    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final boolean glint;
    private final long executionTime;
    private final Rarity rarity;
    private final Type type;
    private final long cooldownModifier;
    private final Effect effect;

    protected Fragment(
            String id,
            String name,
            List<String> lore,
            long executionTime,
            Rarity rarity,
            Type type,
            long cooldownModifier,
            Effect effect
    ) {
        if (executionTime < 0) {
            throw new IllegalArgumentException("executionTime cannot be negative");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("rarity cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        this.id               = id;
        this.name             = name;
        this.displayName      = rarity.getColor() + name;
        this.material         = DEFAULT_MATERIAL;
        this.lore             = List.copyOf(lore);
        this.glint            = true;
        this.executionTime    = executionTime;
        this.rarity           = rarity;
        this.type             = type;
        this.cooldownModifier = cooldownModifier;
        this.effect           = effect;
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final Material getMaterial() {
        return material;
    }

    public final List<String> getLore() {
        return lore;
    }

    public final boolean hasGlint() {
        return glint;
    }

    public final long getExecutionTime() {
        return executionTime;
    }

    public final Rarity getRarity() {
        return rarity;
    }

    public final Type getType() {
        return type;
    }

    public final long getCooldownModifier() {
        return cooldownModifier;
    }

    public final Effect getEffect() {
        return effect;
    }

    public final void execute(AbilityContext context) {
        effect.apply(context);
    }
}
