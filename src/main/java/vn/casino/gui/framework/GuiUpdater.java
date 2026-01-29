package vn.casino.gui.framework;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import vn.casino.core.scheduler.FoliaScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages scheduled GUI updates for real-time countdown timers.
 * Handles Folia-compatible entity-bound scheduling.
 */
public class GuiUpdater {

    private final FoliaScheduler scheduler;
    private final Map<UUID, AtomicBoolean> activeTasks;

    public GuiUpdater(FoliaScheduler scheduler) {
        this.scheduler = scheduler;
        this.activeTasks = new ConcurrentHashMap<>();
    }

    /**
     * Start a countdown timer that updates GUI title every second.
     *
     * @param player Player viewing the GUI
     * @param gui GUI to update
     * @param seconds Initial countdown seconds
     * @param titleFormat Title format with {time} placeholder
     * @param onComplete Callback when countdown reaches 0
     */
    public void startCountdown(
        Player player,
        ChestGui gui,
        int seconds,
        String titleFormat,
        Runnable onComplete
    ) {
        // Stop any existing countdown for this player
        stopCountdown(player.getUniqueId());

        AtomicInteger remaining = new AtomicInteger(seconds);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        scheduler.runAtEntityTimer(player, () -> {
            if (cancelled.get()) {
                return;
            }

            int secs = remaining.get();

            if (secs <= 0) {
                stopCountdown(player.getUniqueId());
                if (onComplete != null) {
                    scheduler.runAtEntity(player, onComplete);
                }
                return;
            }

            // Update title with remaining time
            String formattedTime = formatTime(secs);
            String title = titleFormat.replace("{time}", formattedTime);
            Component titleComponent = MiniMessage.miniMessage().deserialize(title);

            gui.setTitle(title);
            gui.update();

            remaining.decrementAndGet();
        }, 0L, 20L); // Run every second (20 ticks)

        activeTasks.put(player.getUniqueId(), cancelled);
    }

    /**
     * Start a countdown with simple numeric display.
     *
     * @param player Player viewing the GUI
     * @param gui GUI to update
     * @param seconds Initial countdown seconds
     * @param onComplete Callback when countdown reaches 0
     */
    public void startSimpleCountdown(
        Player player,
        ChestGui gui,
        int seconds,
        Runnable onComplete
    ) {
        startCountdown(player, gui, seconds, "<yellow>Time: {time}s</yellow>", onComplete);
    }

    /**
     * Update GUI periodically without countdown.
     *
     * @param player Player viewing the GUI
     * @param gui GUI to update
     * @param updateTask Update task to run
     * @param intervalTicks Update interval in ticks
     */
    public void startPeriodicUpdate(
        Player player,
        ChestGui gui,
        Runnable updateTask,
        long intervalTicks
    ) {
        stopCountdown(player.getUniqueId());

        AtomicBoolean cancelled = new AtomicBoolean(false);

        scheduler.runAtEntityTimer(player, () -> {
            if (cancelled.get()) {
                return;
            }
            updateTask.run();
            gui.update();
        }, 0L, intervalTicks);

        activeTasks.put(player.getUniqueId(), cancelled);
    }

    /**
     * Stop countdown for specific player.
     *
     * @param playerId Player UUID
     */
    public void stopCountdown(UUID playerId) {
        AtomicBoolean cancelled = activeTasks.remove(playerId);
        if (cancelled != null) {
            cancelled.set(true);
        }
    }

    /**
     * Stop all active countdowns.
     */
    public void stopAll() {
        activeTasks.values().forEach(cancelled -> cancelled.set(true));
        activeTasks.clear();
    }

    /**
     * Check if player has active countdown.
     *
     * @param playerId Player UUID
     * @return true if countdown active
     */
    public boolean hasActiveCountdown(UUID playerId) {
        return activeTasks.containsKey(playerId);
    }

    /**
     * Format seconds into MM:SS format.
     *
     * @param totalSeconds Total seconds
     * @return Formatted time string
     */
    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
