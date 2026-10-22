package ru.raskol.lumber.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.PendingTree;
import ru.raskol.lumber.store.LumberStore;

import java.util.HashMap;
import java.util.Map;

/** Планирует регенерацию деревьев. Проверяет очередь каждую секунду. */
public final class TreeScheduler implements Runnable {

    private final RaskolLumber plugin;
    private final LumberStore store;
    private final Map<String, BukkitTask> delayed = new HashMap<>();

    public TreeScheduler(RaskolLumber plugin, LumberStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    /** Запланировать посадку через N секунд. */
    public void schedule(PendingTree p, int seconds) {
        long ticks = Math.max(1L, seconds * 20L);
        BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, () -> grow(p), ticks);
        delayed.put(key(p), t);
    }

    /** При загрузке: перепланировать оставшиеся задачи. */
    public void rescheduleAll() {
        long now = System.currentTimeMillis() / 1000L;
        for (PendingTree p : store.pending()) {
            long remain = p.growAtEpochSeconds - now;
            if (remain <= 0) {
                grow(p);
            } else {
                BukkitTask t = Bukkit.getScheduler()
                        .runTaskLater(plugin, () -> grow(p), remain * 20L);
                delayed.put(key(p), t);
            }
        }
    }

    /** Выращиваем дерево и убираем из очереди. */
    private void grow(PendingTree p) {
        World w = Bukkit.getWorld(p.world);
        if (w == null) {
            plugin.getLogger().warning("[Lumber] мир " + p.world + " не найден, пропускаю посадку.");
            store.pending().remove(p);
            store.save();
            return;
        }

        Location loc = new Location(w, p.x, p.y, p.z);
        // Освобождаем место: если там что-то есть — очищаем (воздух).
        org.bukkit.block.Block base = w.getBlockAt(p.x, p.y, p.z);
        if (!base.getType().isAir() && base.getType().isSolid()) {
            // Место занято — сдвигаемся вверх, чтобы не ломать постройки
            loc = new Location(w, p.x, p.y + 1, p.z);
        }

        boolean ok = w.generateTree(loc, p.type);
        if (ok) {
            plugin.getLogger().info("[Lumber] tree regrown: " + p.type.name()
                    + " at " + p.x + "," + p.y + "," + p.z);
        } else {
            plugin.getLogger().info("[Lumber] regrow failed (место занято): "
                    + p.x + "," + p.y + "," + p.z);
        }

        store.pending().remove(p);
        delayed.remove(key(p));
        store.save();
    }

    private String key(PendingTree p) {
        return p.world + ":" + p.x + ":" + p.y + ":" + p.z + ":" + p.type;
    }

    @Override
    public void run() {
        // Резервная проверка на случай сбоя планировщика
        long now = System.currentTimeMillis() / 1000L;
        for (PendingTree p : new java.util.ArrayList<>(store.pending())) {
            String k = key(p);
            if (!delayed.containsKey(k) && p.growAtEpochSeconds <= now) {
                grow(p);
            }
        }
    }
}
