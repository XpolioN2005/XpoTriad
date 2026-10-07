package dev.xpolion.xpotriad.fragment;

import org.bukkit.entity.EntityType;
import org.bukkit.loot.LootTable;

/**
 * Generic representation of a loot source, independent of the event
 * that produced it.
 *
 * Key conventions (vanilla "minecraft:" namespace is stripped):
 *   LOOT_TABLE / DISPENSE_LOOT -> actual loot-table key, e.g. "chests/ancient_city"
 *   ENTITY                     -> "entities/<entity id>", e.g. "entities/zombie"
 *
 * Non-vanilla namespaces are kept as-is (e.g. "mypack:chests/loot").
 */
public record LootSource(Type type, String key) {

    public enum Type {
        LOOT_TABLE,
        DISPENSE_LOOT,
        ENTITY
    }

    private static final String MINECRAFT_NAMESPACE = "minecraft:";

    public LootSource {
        if (type == null) {
            throw new IllegalArgumentException("Source type cannot be null");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Source key cannot be null or blank");
        }
        key = stripMinecraft(key);
    }

    public static LootSource lootTable(LootTable table) {
        return new LootSource(Type.LOOT_TABLE, table.getKey().asString());
    }

    public static LootSource dispensedLoot(LootTable table) {
        return new LootSource(Type.DISPENSE_LOOT, table.getKey().asString());
    }

    public static LootSource entity(EntityType entityType) {
        return new LootSource(
                Type.ENTITY,
                "entities/" + stripMinecraft(entityType.getKey().asString())
        );
    }

    private static String stripMinecraft(String key) {
        return key.startsWith(MINECRAFT_NAMESPACE)
                ? key.substring(MINECRAFT_NAMESPACE.length())
                : key;
    }
}
