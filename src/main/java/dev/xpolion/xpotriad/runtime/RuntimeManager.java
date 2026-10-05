package dev.xpolion.xpotriad.runtime;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RuntimeManager {

    private final JavaPlugin plugin;
    private final List<StateHandleImpl> activeStates = new CopyOnWriteArrayList<>();
    private BukkitTask task;

    public RuntimeManager(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public RuntimeHandle start(RuntimeState state) {
        if (state == null) {
            throw new IllegalArgumentException("RuntimeState cannot be null");
        }
        StateHandleImpl handle = new StateHandleImpl(state);
        activeStates.add(handle);
        return handle;
    }

    private void tick() {
        for (StateHandleImpl handle : activeStates) {
            if (handle.stopped) {
                activeStates.remove(handle);
                continue;
            }

            try {
                handle.state.tick();
            } catch (Exception e) {
                plugin.getLogger().severe("Error ticking RuntimeState: " + e.getMessage());
                handle.stop();
                continue;
            }

            if (handle.state.isFinished() || handle.stopped) {
                handle.stop();
            }
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (StateHandleImpl handle : activeStates) {
            handle.stopInternal();
        }
        activeStates.clear();
    }

    private class StateHandleImpl implements RuntimeHandle {
        private final RuntimeState state;
        private volatile boolean stopped = false;

        private StateHandleImpl(RuntimeState state) {
            this.state = state;
        }

        @Override
        public void stop() {
            if (stopped) {
                return;
            }
            stopped = true;
            stopInternal();
            activeStates.remove(this);
        }

        private void stopInternal() {
            if (!stopped) {
                stopped = true;
            }
            try {
                state.stop();
            } catch (Exception e) {
                plugin.getLogger().severe("Error stopping RuntimeState: " + e.getMessage());
            }
        }

        @Override
        public boolean isActive() {
            return !stopped && activeStates.contains(this) && !state.isFinished();
        }
    }
}
