package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sequences and executes the three stages of an Ability.
 *
 * Timing per stage:
 *   fragment.getExecutionTime() + 5 ticks before advancing to the next stage.
 *
 * If a stage has no Fragment, advance immediately (no buffer applied).
 *
 * The engine never searches for targets, performs raytrace, or inspects collisions.
 */
public final class AbilityEngine {

    /** Framework-controlled tick buffer added after each fragment's own execution time. */
    private static final long STAGE_BUFFER_TICKS = 5L;

    private final JavaPlugin plugin;

    public AbilityEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(AbilityContext context) {
        executeStage(context, Ability.Stage.PRE_CAST, 0L);
    }

    private void executeStage(AbilityContext context, Ability.Stage stage, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!context.getSource().isOnline()) {
                return;
            }

            Fragment fragment = context.getAbility().getFragment(stage);

            long nextDelay;

            if (fragment != null) {
                fragment.execute(context);
                nextDelay = fragment.getExecutionTime() + STAGE_BUFFER_TICKS;
            } else {
                // Empty stage: advance immediately (0 delay), no buffer
                nextDelay = 0L;
            }

            Ability.Stage nextStage = getNextStage(stage);

            if (nextStage != null) {
                executeStage(context, nextStage, nextDelay);
            }
        }, delayTicks);
    }

    private Ability.Stage getNextStage(Ability.Stage stage) {
        return switch (stage) {
            case PRE_CAST -> Ability.Stage.CAST;
            case CAST -> Ability.Stage.POST_CAST;
            case POST_CAST -> null;
        };
    }
}
