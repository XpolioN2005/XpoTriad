package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.AbilityContext;
import dev.xpolion.xpotriad.Fragment;
import dev.xpolion.xpotriad.effects.InvisibilityEffect;

public final class InvisibilityFragment extends Fragment {

    private final InvisibilityEffect effect = new InvisibilityEffect();

    public InvisibilityFragment() {
        super("invisibility", "Invisibility");
    }

    @Override
    public void execute(AbilityContext context) {
        effect.apply(context);
    }
}