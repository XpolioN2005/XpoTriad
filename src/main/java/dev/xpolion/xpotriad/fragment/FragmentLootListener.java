package dev.xpolion.xpotriad.fragment;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;

/**
 * Translates Minecraft loot events into LootSource values and passes them
 * to FragmentRegistry. This listener contains NO chance or rarity logic.
 */
public final class FragmentLootListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        Fragment fragment = FragmentRegistry.roll(
                LootSource.lootTable(event.getLootTable())
        );

        if (fragment == null) {
            return;
        }

        event.getLoot().add(FragmentItem.create(fragment));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispenseLoot(BlockDispenseLootEvent event) {
        Fragment fragment = FragmentRegistry.roll(
                LootSource.dispensedLoot(event.getLootTable())
        );

        if (fragment == null) {
            return;
        }

        event.getDispensedLoot().add(FragmentItem.create(fragment));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null) {
            return;
        }

        Fragment fragment = FragmentRegistry.roll(
                LootSource.entity(event.getEntity().getType())
        );

        if (fragment == null) {
            return;
        }

        event.getDrops().add(FragmentItem.create(fragment));
    }
}