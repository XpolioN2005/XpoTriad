package dev.xpolion.xpotriad;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AbilityEngine {

    private final JavaPlugin plugin;

    public AbilityEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, Ability ability) {
        executeStage(player, ability, Ability.Stage.PRE_CAST);
    }

    private void executeStage(
            Player player,
            Ability ability,
            Ability.Stage stage
    ) {
        long delay = ability.getDelay(stage);

        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> {
                Fragment fragment = ability.getFragment(stage);

                if (fragment != null) {
                    AbilityContext context =
                        new AbilityContext(player, ability);

                    fragment.execute(context);
                }

                Ability.Stage nextStage = getNextStage(stage);

                if (nextStage != null) {
                    executeStage(player, ability, nextStage);
                }
            },
            delay
        );
    }

    private Ability.Stage getNextStage(Ability.Stage stage) {
        return switch (stage) {
            case PRE_CAST -> Ability.Stage.CAST;
            case CAST -> Ability.Stage.POST_CAST;
            case POST_CAST -> null;
        };
    }
}