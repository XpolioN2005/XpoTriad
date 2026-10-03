package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.targeting.AoeTargeting;
import dev.xpolion.xpotriad.targeting.SingleTargeting;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * Handles both melee and ranged ability activation.
 *
 * MELEE: EntityDamageByEntityEvent – player hits entity with an XpoTriad MELEE item.
 * RANGED: PlayerInteractEvent (right-click) – spawns a custom projectile tagged with context_id.
 *         ProjectileHitEvent – retrieves context, finalizes targeting, runs engine.
 */
public final class AbilityListener implements Listener {

    /** Default AOE radius used when a ranged projectile hits a block. */
    private static final double DEFAULT_AOE_RADIUS = 5.0;

    private final AbilityEngine abilityEngine;
    private final ActivationRegistry activationRegistry;
    private final NamespacedKey contextIdKey;

    public AbilityListener(
            AbilityEngine abilityEngine,
            ActivationRegistry activationRegistry,
            JavaPlugin plugin
    ) {
        this.abilityEngine      = abilityEngine;
        this.activationRegistry = activationRegistry;
        this.contextIdKey       = new NamespacedKey(plugin, "context_id");
    }

    // -------------------------------------------------------------------------
    // MELEE – EntityDamageByEntityEvent
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AbilityItem.isAbilityItem(item)) {
            return;
        }

        Ability ability = AbilityItem.read(item);

        if (ability == null || ability.getWeaponType() != Ability.WeaponType.MELEE) {
            return;
        }

        Entity victim = event.getEntity();

        AbilityContext context = new AbilityContext(
                player,
                ability,
                new SingleTargeting(victim)
        );

        abilityEngine.execute(context);
    }

    // -------------------------------------------------------------------------
    // RANGED – launch custom projectile on right-click
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        org.bukkit.event.block.Action action = event.getAction();

        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AbilityItem.isAbilityItem(item)) {
            return;
        }

        Ability ability = AbilityItem.read(item);

        if (ability == null || ability.getWeaponType() != Ability.WeaponType.RANGED) {
            return;
        }

        event.setCancelled(true);

        // Create a preliminary context; targeting will be finalized on projectile hit.
        // Use SingleTargeting(null) as placeholder – the projectile carries context_id.
        UUID contextId = UUID.randomUUID();

        AbilityContext context = new AbilityContext(
                player,
                ability,
                new SingleTargeting(null)
        );

        activationRegistry.register(contextId, context);

        // Launch a snowball as the custom projectile
        Projectile projectile = player.launchProjectile(
                org.bukkit.entity.Snowball.class
        );

        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        pdc.set(contextIdKey, PersistentDataType.STRING, contextId.toString());
    }

    // -------------------------------------------------------------------------
    // RANGED – handle projectile hit
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        String contextIdStr = pdc.get(contextIdKey, PersistentDataType.STRING);

        if (contextIdStr == null) {
            return;
        }

        UUID contextId;

        try {
            contextId = UUID.fromString(contextIdStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        AbilityContext originalContext = activationRegistry.remove(contextId);

        if (originalContext == null) {
            return;
        }

        // Finalize targeting based on what was hit
        AbilityContext finalContext;

        if (event.getHitEntity() != null) {
            // Projectile hit a living entity
            finalContext = new AbilityContext(
                    originalContext.getSource(),
                    originalContext.getAbility(),
                    new SingleTargeting(event.getHitEntity())
            );
        } else {
            // Projectile hit a block or void – use AOE at impact location
            Location impactLocation = projectile.getLocation();

            finalContext = new AbilityContext(
                    originalContext.getSource(),
                    originalContext.getAbility(),
                    new AoeTargeting(impactLocation, DEFAULT_AOE_RADIUS)
            );
        }

        abilityEngine.execute(finalContext);
    }
}
