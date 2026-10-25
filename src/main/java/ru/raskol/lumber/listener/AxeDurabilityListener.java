package ru.raskol.lumber.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.service.AxeService;

/**
 * Кастомные топоры (Улучшенный, Дровосека, Олафа) неломаемые:
 * отменяем любую поломку по PDC-тегу — работает и для топоров,
 * выданных до введения unbreakable.
 */
public final class AxeDurabilityListener implements Listener {

    private final RaskolLumber plugin;
    private final AxeService axeService;

    public AxeDurabilityListener(RaskolLumber plugin, AxeService axeService) {
        this.plugin = plugin;
        this.axeService = axeService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (axeService.isCustomAxe(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
