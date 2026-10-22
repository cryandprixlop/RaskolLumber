package ru.raskol.lumber.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.PendingTree;
import ru.raskol.lumber.store.LumberStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Регенерация: дерево собирается вручную из материала срубленного ствола.
 * Никакого generateTree — гарантированно брёвна, а не земля.
 */
public final class TreeScheduler implements Runnable {

    private final RaskolLumber plugin;
    private final LumberStore store;
    private final Map<String, BukkitTask> delayed = new HashMap<>();
    private final Random random = new Random();

    public TreeScheduler(RaskolLumber plugin, LumberStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void schedule(PendingTree p, int seconds) {
        long ticks = Math.max(1L, seconds * 20L);
        BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, () -> grow(p), ticks);
        delayed.put(key(p), t);
    }

    public void rescheduleAll() {
        long now = System.currentTimeMillis() / 1000L;
        for (PendingTree p : new ArrayList<>(store.pending())) {
            long remain = p.growAtEpochSeconds - now;
            if (remain <= 0) grow(p);
            else delayed.put(key(p), Bukkit.getScheduler()
                    .runTaskLater(plugin, () -> grow(p), remain * 20L));
        }
    }

    private void grow(PendingTree p) {
        World w = Bukkit.getWorld(p.world);
        if (w == null) {
            plugin.getLogger().warning("[Lumber] мир " + p.world + " не найден, посадка пропущена.");
            store.pending().remove(p);
            store.save();
            return;
        }

        Material log = Material.matchMaterial(p.logMaterial);
        if (log == null) log = Material.OAK_LOG;
        Material leaf = leafOf(log);

        // Привязка к земле: спускаемся от сохранённой точки до сплошного блока
        int gy = p.y;
        while (gy > w.getMinHeight() + 1) {
            Material m = w.getBlockAt(p.x, gy, p.z).getType();
            boolean ground = m.isSolid() && !isLog(m) && !isLeaf(m);
            if (ground) break;
            gy--;
        }
        int baseY = gy + 1;

        // Ствол
        boolean conifer = (log == Material.SPRUCE_LOG);
        int height = conifer ? 5 + random.nextInt(3) : 4 + random.nextInt(3);
        int placed = 0;
        for (int i = 0; i < height; i++) {
            if (setLog(w, p.x, baseY + i, p.z, log)) placed++;
            else break;
        }
        if (placed == 0) {
            plugin.getLogger().info("[Lumber] regrow skipped (место занято): "
                    + p.x + "," + baseY + "," + p.z);
        } else {
            int top = baseY + placed - 1;
            if (conifer) canopyConifer(w, p.x, top, p.z, leaf);
            else canopyBlob(w, p.x, top, p.z, leaf);
            plugin.getLogger().info("[Lumber] tree regrown: " + log.name()
                    + " at " + p.x + "," + baseY + "," + p.z + " (h=" + placed + ")");
        }

        store.pending().remove(p);
        delayed.remove(key(p));
        store.save();
    }

    /* ========== Крона ========== */

    private void canopyBlob(World w, int x, int top, int z, Material leaf) {
        square(w, x, top - 2, z, leaf);
        square(w, x, top - 1, z, leaf);
        plus(w, x, top, z, leaf);
        plus(w, x, top + 1, z, leaf);
    }

    private void canopyConifer(World w, int x, int top, int z, Material leaf) {
        setLeaf(w, x, top + 1, z, leaf);
        plus(w, x, top, z, leaf);
        square(w, x, top - 1, z, leaf);
        plus(w, x, top - 2, z, leaf);
        square(w, x, top - 3, z, leaf);
    }

    private void square(World w, int x, int y, int z, Material leaf) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                setLeaf(w, x + dx, y, z + dz, leaf);
            }
        }
    }

    private void plus(World w, int x, int y, int z, Material leaf) {
        setLeaf(w, x, y, z, leaf);
        setLeaf(w, x + 1, y, z, leaf);
        setLeaf(w, x - 1, y, z, leaf);
        setLeaf(w, x, y, z + 1, leaf);
        setLeaf(w, x, y, z - 1, leaf);
    }

    /* ========== Размещение блоков ========== */

    private boolean setLog(World w, int x, int y, int z, Material log) {
        Block b = w.getBlockAt(x, y, z);
        Material m = b.getType();
        if (m == Material.AIR || isLeaf(m) || m == Material.GRASS || m == Material.TALL_GRASS
                || m == Material.FERN || m == Material.SNOW
                || m.name().endsWith("_SAPLING")) {
            b.setType(log, false);
            return true;
        }
        return false;
    }

    private void setLeaf(World w, int x, int y, int z, Material leaf) {
        Block b = w.getBlockAt(x, y, z);
        if (b.getType() == Material.AIR) b.setType(leaf, false);
    }

    private boolean isLog(Material m) { return m.name().endsWith("_LOG") || m.name().endsWith("_WOOD"); }
    private boolean isLeaf(Material m) { return m.name().endsWith("_LEAVES"); }

    private Material leafOf(Material log) {
        return switch (log) {
            case SPRUCE_LOG -> Material.SPRUCE_LEAVES;
            case BIRCH_LOG -> Material.BIRCH_LEAVES;
            case JUNGLE_LOG -> Material.JUNGLE_LEAVES;
            case ACACIA_LOG -> Material.ACACIA_LEAVES;
            case DARK_OAK_LOG -> Material.DARK_OAK_LEAVES;
            case MANGROVE_LOG -> Material.MANGROVE_LEAVES;
            case CHERRY_LOG -> Material.CHERRY_LEAVES;
            default -> Material.OAK_LEAVES;
        };
    }

    private String key(PendingTree p) {
        return p.world + ":" + p.x + ":" + p.z;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis() / 1000L;
        for (PendingTree p : new ArrayList<>(store.pending())) {
            if (!delayed.containsKey(key(p)) && p.growAtEpochSeconds <= now) {
                grow(p);
            }
        }
    }
}
