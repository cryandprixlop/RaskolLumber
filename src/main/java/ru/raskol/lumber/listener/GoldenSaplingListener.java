package ru.raskol.lumber.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.service.AxeService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Золотой саженец нельзя посадить — он материал легендарных крафтов. */
public final class GoldenSaplingListener implements Listener {

    private final RaskolLumber plugin;
    private final AxeService axeService;
    private final Map<UUID, Long> lastWarn = new HashMap<>();

    public GoldenSaplingListener(RaskolLumber plugin, AxeService axeService) {
        this.plugin = plugin;
        this.axeService = axeService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!axeService.isGoldenSapling(event.getItem())) return;

        event.setCancelled(true);
        warn(event.getPlayer(),
                "§cЗолотой саженец нельзя посадить! §7Он нужен для легендарных крафтов.");
    }

    private void warn(Player player, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastWarn.get(player.getUniqueId());
        if (prev != null && now - prev < 3000) return;
        lastWarn.put(player.getUniqueId(), now);
        player.sendMessage(message);
    }
}
