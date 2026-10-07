package dev.xpolion.xpotriad.fragment;

import java.util.List;

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
        List<Fragment> drops = FragmentRegistry.roll(
                LootSource.lootTable(event.getLootTable()),
                FragmentRegistry.MAX_FRAGMENTS_PER_INTERACTION
        );

        for (Fragment fragment : drops) {
            event.getLoot().add(FragmentItem.create(fragment));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispenseLoot(BlockDispenseLootEvent event) {
        List<Fragment> drops = FragmentRegistry.roll(
                LootSource.dispensedLoot(event.getLootTable()),
                FragmentRegistry.MAX_FRAGMENTS_PER_INTERACTION
        );

        for (Fragment fragment : drops) {
            event.getDispensedLoot().add(FragmentItem.create(fragment));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null) {
            return;
        }

        List<Fragment> drops = FragmentRegistry.roll(
                LootSource.entity(event.getEntity().getType()),
                FragmentRegistry.MAX_FRAGMENTS_PER_INTERACTION
        );

        for (Fragment fragment : drops) {
            event.getDrops().add(FragmentItem.create(fragment));
        }
    }
}