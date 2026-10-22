package ru.raskol.lumber.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.LumberZone;
import ru.raskol.lumber.model.PendingTree;
import ru.raskol.lumber.store.LumberStore;

import java.util.List;

public final class LumberListener implements Listener {

    private final RaskolLumber plugin;
    private final LumberStore store;

    public LumberListener(RaskolLumber plugin, LumberStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Material mat = broken.getType();

        List<String> logs = plugin.getConfig().getStringList("logs");
        if (!logs.contains(mat.name())) return;

        LumberZone zone = null;
        for (LumberZone z : store.all()) {
            if (z.region != null && z.region.contains(broken.getLocation())) {
                zone = z;
                break;
            }
        }
        if (zone == null) return;

        if (plugin.getConfig().getBoolean("require-axe", true)) {
            Player p = event.getPlayer();
            Material hand = p.getInventory().getItemInMainHand().getType();
            if (!plugin.getConfig().getStringList("axes").contains(hand.name())) return;
        }

        if (!plugin.getConfig().getBoolean("allow-player-drop", true)) {
            event.setDropItems(false);
        }

        // Одна посадка на колонну (x,z): TreeChop ломает всё дерево за тик
        String world = broken.getWorld().getName();
        if (store.hasPendingColumn(world, broken.getX(), broken.getZ())) return;

        int delaySec = plugin.getConfig().getInt("regrow-seconds", 600);

        PendingTree p = new PendingTree();
        p.world = world;
        p.x = broken.getX();
        p.y = broken.getY();
        p.z = broken.getZ();
        p.logMaterial = mat.name();
        p.growAtEpochSeconds = System.currentTimeMillis() / 1000L + delaySec;
        store.addPending(p);

        plugin.getScheduler().schedule(p, delaySec);
        plugin.getLogger().info("[Lumber] scheduled regrow: " + p.logMaterial + " at "
                + p.x + "," + p.y + "," + p.z + " in " + delaySec + "s (zone " + zone.name + ")");
    }
}
