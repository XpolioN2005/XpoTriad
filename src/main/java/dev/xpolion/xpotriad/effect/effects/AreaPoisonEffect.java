package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.particle.animations.PoisonCloudAnimation;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Poisons every enemy inside a targeted area and leaves a green ground cloud.
 *
 * Targeting: POINT — block raycast up to the point-range (flows.md
 * convention), else the point point-range blocks along the view ray.
 * Poison is applied once at cast; the lingering cloud is a finite persistent
 * visual (no runtime required).
 */
public final class AreaPoisonEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double pointRange = cfg.pointRange();
        double radius = cfg.effectDouble("area_poison", "radius", 4.0);
        int durationTicks = cfg.effectInt("area_poison", "potion-duration-ticks", 100);
        int amplifier = cfg.effectInt("area_poison", "amplifier", 0);
        int cloudTicks = cfg.effectInt("area_poison", "cloud-duration-ticks", 100);

        World world = source.getWorld();
        Location eye = source.getEyeLocation();
        Vector direction = eye.getDirection();

        Location point;
        RayTraceResult hit = world.rayTraceBlocks(
                eye,
                direction,
                pointRange,
                FluidCollisionMode.NEVER,
                true
        );

        if (hit != null && hit.getHitPosition() != null) {
            point = hit.getHitPosition().toLocation(world);
        } else {
            point = eye.clone().add(direction.clone().multiply(pointRange));
        }

        List<LivingEntity> targets =
                TargetResolver.entitiesNear(point, radius, source);

        for (LivingEntity entity : targets) {
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.POISON,
                    durationTicks,
                    amplifier,
                    false,
                    true,
                    false
            ));
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);
        plugin.getParticleSystem().playPersistent(
                new PoisonCloudAnimation(radius),
                point.clone(),
                cloudTicks / 20.0
        );
    }
}
