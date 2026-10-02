package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.AbilityContext;
import dev.xpolion.xpotriad.Fragment;
import dev.xpolion.xpotriad.effects.ExplosionEffect;

public final class ExplosionFragment extends Fragment {

    private final ExplosionEffect effect = new ExplosionEffect();

    public ExplosionFragment() {
        super("explosion", "Explosion");
    }

    @Override
    public void execute(AbilityContext context) {
        effect.apply(context);
    }
}