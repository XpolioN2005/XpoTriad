package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.AbilityContext;
import dev.xpolion.xpotriad.Fragment;
import dev.xpolion.xpotriad.effects.SpeedEffect;

public final class SpeedFragment extends Fragment {

    private final SpeedEffect effect = new SpeedEffect();

    public SpeedFragment() {
        super("speed", "Speed");
    }

    @Override
    public void execute(AbilityContext context) {
        effect.apply(context);
    }
}