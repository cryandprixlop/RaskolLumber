package ru.raskol.lumber.listener;

import org.bukkit.Material;
import org.bukkit.TreeType;
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

        // В зоне ли?
        LumberZone zone = null;
        for (LumberZone z : store.all()) {
            if (z.region != null && z.region.contains(broken.getLocation())) {
                zone = z;
                break;
            }
        }
        if (zone == null) return;

        // Топор?
        if (plugin.getConfig().getBoolean("require-axe", true)) {
            Player p = event.getPlayer();
            Material hand = p.getInventory().getItemInMainHand().getType();
            if (!plugin.getConfig().getStringList("axes").contains(hand.name())) return;
        }

        // Дроп?
        if (!plugin.getConfig().getBoolean("allow-player-drop", true)) {
            event.setDropItems(false);
        }

        // Спускаемся до основания ствола: одна посадка на одно дерево
        Block base = broken;
        while (logs.contains(base.getRelative(BlockFace.DOWN).getType().name())) {
            base = base.getRelative(BlockFace.DOWN);
        }
        String world = base.getWorld().getName();
        if (store.hasPendingAt(world, base.getX(), base.getY(), base.getZ())) return;

        TreeType type = treeTypeOf(mat);
        if (type == null) return;

        int delaySec = plugin.getConfig().getInt("regrow-seconds", 600);

        PendingTree p = new PendingTree();
        p.world = world;
        p.x = base.getX();
        p.y = base.getY();
        p.z = base.getZ();
        p.type = type;
        p.growAtEpochSeconds = System.currentTimeMillis() / 1000L + delaySec;
        store.addPending(p);

        plugin.getScheduler().schedule(p, delaySec);
        plugin.getLogger().info("[Lumber] scheduled regrow: " + type.name() + " at "
                + p.x + "," + p.y + "," + p.z + " in " + delaySec + "s (zone " + zone.name + ")");
    }

    public static TreeType treeTypeOf(Material log) {
        return switch (log) {
            case OAK_LOG -> TreeType.TREE;
            case SPRUCE_LOG -> TreeType.REDWOOD;
            case BIRCH_LOG -> TreeType.BIRCH;
            case JUNGLE_LOG -> TreeType.JUNGLE;
            case ACACIA_LOG -> TreeType.ACACIA;
            case DARK_OAK_LOG -> TreeType.DARK_OAK;
            case MANGROVE_LOG -> TreeType.MANGROVE;
            case CHERRY_LOG -> TreeType.CHERRY;
            default -> null;
        };
    }
}
