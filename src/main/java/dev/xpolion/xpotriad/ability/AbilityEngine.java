package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.fragment.Fragment;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sequences and executes the three stages of an Ability.
 *
 * Timing per stage:
 *   fragment.getExecutionTime() + stage-buffer ticks before advancing to the next stage.
 *
 * If a stage has no Fragment, advance immediately (no buffer applied).
 *
 * The engine never searches for targets, performs raytrace, or inspects collisions.
 *
 * <p>When {@code onComplete} is supplied, it runs exactly once after the last
 * occupied stage has executed (or immediately for an empty ability). This is
 * where the cooldown is applied, so the cooldown only starts counting down
 * after every fragment in the ability has run.
 */
public final class AbilityEngine {

    /** Fallback framework buffer if the balance config is not yet loaded. */
    private static final long STAGE_BUFFER_TICKS = 5L;

    private final JavaPlugin plugin;

    public AbilityEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(AbilityContext context) {
        execute(context, null);
    }

    /**
     * Executes the ability sequence.
     *
     * @param onComplete run on the main thread after the final stage executes;
     *                   may be null. Used to apply the deferred cooldown.
     */
    public void execute(AbilityContext context, Runnable onComplete) {
        executeStage(context, Ability.Stage.PRE_CAST, 0L, onComplete);
    }

    private void executeStage(AbilityContext context, Ability.Stage stage, long delayTicks, Runnable onComplete) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!context.getSource().isOnline()) {
                // Player left mid-sequence: nothing to apply, but still release
                // the completion hook so the cooldown write is not lost silently.
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            Fragment fragment = context.getAbility().getFragment(stage);

            long nextDelay;

            if (fragment != null) {
                fragment.execute(context);
                // Record execution history for Repeat (runs after execute so
                // RepeatEffect still sees the PREVIOUS fragment while applying).
                context.setLastExecutedFragment(fragment);
                nextDelay = fragment.getExecutionTime() + stageBuffer();
            } else {
                // Empty stage: advance immediately (0 delay), no buffer
                nextDelay = 0L;
            }

            Ability.Stage nextStage = getNextStage(stage);

            if (nextStage != null) {
                executeStage(context, nextStage, nextDelay, onComplete);
            } else if (onComplete != null) {
                onComplete.run();
            }
        }, delayTicks);
    }

    private long stageBuffer() {
        return BalanceConfig.get().stageBufferTicks();
    }

    private Ability.Stage getNextStage(Ability.Stage stage) {
        return switch (stage) {
            case PRE_CAST -> Ability.Stage.CAST;
            case CAST -> Ability.Stage.POST_CAST;
            case POST_CAST -> null;
        };
    }
}
