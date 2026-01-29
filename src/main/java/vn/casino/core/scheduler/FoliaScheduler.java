package vn.casino.core.scheduler;

import com.tcoded.folialib.FoliaLib;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class FoliaScheduler {

    private final FoliaLib foliaLib;

    public void runAsync(Runnable runnable) {
        foliaLib.getImpl().runAsync(t -> runnable.run());
    }

    public void runAsyncLater(Runnable runnable, long delayTicks) {
        foliaLib.getImpl().runLaterAsync(t -> runnable.run(), delayTicks);
    }

    public void runAsyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        foliaLib.getImpl().runTimerAsync(t -> runnable.run(), delayTicks, periodTicks);
    }

    public void runSync(Runnable runnable) {
        foliaLib.getImpl().runNextTick(t -> runnable.run());
    }

    public void runSyncLater(Runnable runnable, long delayTicks) {
        foliaLib.getImpl().runLater(t -> runnable.run(), delayTicks);
    }

    public void runSyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        foliaLib.getImpl().runTimer(t -> runnable.run(), delayTicks, periodTicks);
    }

    public void runLater(Runnable runnable, long delayTicks) {
        runSyncLater(runnable, delayTicks);
    }

    public void runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        runSyncTimer(runnable, delayTicks, periodTicks);
    }

    public void runAtLocation(Location location, Runnable runnable) {
        foliaLib.getImpl().runAtLocation(location, t -> runnable.run());
    }

    public void runAtLocationLater(Location location, Runnable runnable, long delayTicks) {
        foliaLib.getImpl().runAtLocationLater(location, t -> runnable.run(), delayTicks);
    }

    public void runAtLocationTimer(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        foliaLib.getImpl().runAtLocationTimer(location, t -> runnable.run(), delayTicks, periodTicks);
    }

    public void runAtEntity(Entity entity, Runnable runnable) {
        foliaLib.getImpl().runAtEntity(entity, t -> runnable.run());
    }

    public void runAtEntityLater(Entity entity, Runnable runnable, long delayTicks) {
        foliaLib.getImpl().runAtEntityLater(entity, t -> runnable.run(), delayTicks);
    }

    public void runAtEntityTimer(Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        foliaLib.getImpl().runAtEntityTimer(entity, t -> runnable.run(), delayTicks, periodTicks);
    }

    public void runForPlayer(Player player, Consumer<Player> task) {
        runAtEntity(player, () -> task.accept(player));
    }

    public void runForPlayerLater(Player player, Consumer<Player> task, long delayTicks) {
        runAtEntityLater(player, () -> task.accept(player), delayTicks);
    }

    public void runForPlayerTimer(Player player, Consumer<Player> task, long delayTicks, long periodTicks) {
        runAtEntityTimer(player, () -> task.accept(player), delayTicks, periodTicks);
    }

    public void scheduleWithFixedDelay(Runnable task, long initialDelayMs, long delayMs) {
        runAsyncTimer(() -> {
            try {
                task.run();
                TimeUnit.MILLISECONDS.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, initialDelayMs / 50, delayMs / 50);
    }

    public void cancelAllTasks() {
        // FoliaLib handles task lifecycle automatically
        // No manual cancellation needed
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }
}
