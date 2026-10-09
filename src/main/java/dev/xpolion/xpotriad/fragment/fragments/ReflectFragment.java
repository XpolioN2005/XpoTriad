package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.ReflectEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Reflects incoming projectiles for 80 ticks (projectiles only —
 * melee passes through untouched, so Reflect is never a pseudo
 * Cheat Death).
 *
 * executionTime    = 10 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = +14 ticks
 * target           = SELF
 * effect           = ReflectState runtime (listens for incoming projectiles)
 */
public final class ReflectFragment extends Fragment {

    public ReflectFragment() {
        super(
                "reflect",
                "Reflect Fragment",
                List.of(Component.text("Reflects incoming attacks.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("reflect", 10L),
                Rarity.EPIC,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("reflect", 14L),
                new ReflectEffect()
        );
    }
}