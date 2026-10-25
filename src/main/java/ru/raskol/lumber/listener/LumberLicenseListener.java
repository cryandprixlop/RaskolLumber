package ru.raskol.lumber.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.service.LicenseService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Без лицензии лесоруба брёвна не ломаются (HIGH — раньше лута и регена). */
public final class LumberLicenseListener implements Listener {

    private final RaskolLumber plugin;
    private final LicenseService licenseService;
    private final Map<UUID, Long> lastWarn = new HashMap<>();

    public LumberLicenseListener(RaskolLumber plugin, LicenseService licenseService) {
        this.plugin = plugin;
        this.licenseService = licenseService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent event) {
        String mat = event.getBlock().getType().name();
        if (!plugin.getConfig().getStringList("logs").contains(mat)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("lumber.admin")) return;
        if (licenseService.hasValidLicense(player)) return;

        event.setCancelled(true);
        warn(player, "§cДля вырубки леса нужна лицензия лесоруба! §e/lumber license buy");
    }

    private void warn(Player player, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastWarn.get(player.getUniqueId());
        if (prev != null && now - prev < 3000) return;
        lastWarn.put(player.getUniqueId(), now);
        player.sendMessage(message);
    }
}
