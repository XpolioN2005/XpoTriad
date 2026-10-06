package dev.xpolion.xpotriad.fragment;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.loot.LootTables;

public final class FragmentLootListener implements Listener {

    private static final double ANCIENT_CITY_CHANCE = 0.15;

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {

        LootTables lootTable = getLootTable(event);

        if (lootTable == null) {
            return;
        }

        double chance = getChance(lootTable);

        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        Fragment fragment = FragmentRegistry.rollLoot(lootTable);

        if (fragment == null) {
            return;
        }

        event.getLoot().add(FragmentItem.create(fragment));
    }

    private LootTables getLootTable(LootGenerateEvent event) {
        for (LootTables table : LootTables.values()) {
            if (table.getKey().equals(event.getLootTable().getKey())) {
                return table;
            }
        }

        return null;
    }

    private double getChance(LootTables lootTable) {
        if (lootTable == LootTables.ANCIENT_CITY) {
            return ANCIENT_CITY_CHANCE;
        }

        return 0.0;
    }
}