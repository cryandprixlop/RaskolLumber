package ru.raskol.lumber.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.AxeTier;
import ru.raskol.lumber.service.AxeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Рулетка лута при рубке брёвен топором:
 * золотой саженец -> фрагмент следующего топора -> множитель брёвен.
 * Одна рулетка на дерево (дедуп по колонне x,z в окне 2 сек).
 * Шансы берутся из конфига loot.*, а при его отсутствии — из встроенных дефолтов.
 */
public final class AxeLootListener implements Listener {

    private static final long ROLL_WINDOW_MS = 2000;

    /** Дефолты по спеке — работают, даже если серверный конфиг устарел. */
    private static final Map<String, Double> DEFAULTS = Map.ofEntries(
            Map.entry("vanilla.golden-chance", 0.001),
            Map.entry("vanilla.next-fragment-chance", 5.0),
            Map.entry("vanilla.double-chance", 2.0),
            Map.entry("improved.golden-chance", 0.002),
            Map.entry("improved.next-fragment-chance", 4.0),
            Map.entry("improved.double-chance", 4.0),
            Map.entry("lumberjack.golden-chance", 0.01),
            Map.entry("lumberjack.next-fragment-chance", 4.0),
            Map.entry("lumberjack.double-chance", 20.0),
            Map.entry("olaf.golden-chance", 0.5),
            Map.entry("olaf.double-chance", 35.0),
            Map.entry("olaf.triple-chance", 15.0),
            Map.entry("olaf.quad-chance", 10.0)
    );

    private final RaskolLumber plugin;
    private final AxeService axeService;
    private final Random random = new Random();
    private final Map<String, Long> lastRoll = new HashMap<>();

    public AxeLootListener(RaskolLumber plugin, AxeService axeService) {
        this.plugin = plugin;
        this.axeService = axeService;
        if (plugin.getConfig().getConfigurationSection("loot") == null) {
            plugin.getLogger().warning("[Lumber] В серверном config.yml нет секции 'loot'! "
                    + "Удали plugins/RaskolLumber/config.yml и перезапусти, "
                    + "либо используй встроенные дефолты (они активны сейчас).");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent event) {
        Material mat = event.getBlock().getType();
        List<String> logs = plugin.getConfig().getStringList("logs");
        if (!logs.contains(mat.name())) return;

        Player player = event.getPlayer();
        AxeTier tier = axeService.tierOf(player.getInventory().getItemInMainHand());
        if (tier == null) return; // рубит не топором — бонусов нет

        // Одна рулетка на дерево: дедуп по колонне (x,z) в окне 2 сек
        String col = event.getBlock().getWorld().getName() + ":"
                + event.getBlock().getX() + ":" + event.getBlock().getZ();
        long now = System.currentTimeMillis();
        Long prev = lastRoll.get(col);
        if (prev != null && now - prev < ROLL_WINDOW_MS) return;
        lastRoll.put(col, now);
        if (lastRoll.size() > 5000) {
            lastRoll.entrySet().removeIf(e -> now - e.getValue() > 60_000);
        }

        rollLoot(player, tier, mat, event.getBlock().getLocation());
    }

    private void rollLoot(Player player, AxeTier tier, Material log, Location loc) {
        String section = sectionOf(tier);
        double roll = random.nextDouble(100.0);
        double acc = 0.0;

        // 1) Золотой саженец
        double golden = chance(section, "golden-chance");
        acc += golden;
        if (roll < acc) {
            give(player, axeService.createGoldenSapling(1));
            player.sendMessage("§6✦ Редчайшая находка: §eЗолотой саженец§6!");
            plugin.getLogger().info("[Lumber] " + player.getName()
                    + " нашёл золотой саженец (" + tier.name() + ")");
            return;
        }

        // 2) Фрагмент следующего топора
        double fragChance = chance(section, "next-fragment-chance");
        acc += fragChance;
        if (roll < acc) {
            AxeTier fragTier = tier.dropsFragment();
            if (fragTier != null) {
                give(player, axeService.createFragment(fragTier, 1));
                player.sendMessage("§7⚙ Найден " + fragTier.fragmentDisplayName + "§7!");
                plugin.getLogger().info("[Lumber] " + player.getName()
                        + " нашёл фрагмент " + fragTier.name());
            }
            return;
        }

        // 3) Множитель брёвен (отдельный ролл)
        double quad = chance(section, "quad-chance");
        double triple = chance(section, "triple-chance");
        double dbl = chance(section, "double-chance");
        double mRoll = random.nextDouble(100.0);
        int multiplier = 1;
        if (mRoll < quad) multiplier = 4;
        else if (mRoll < quad + triple) multiplier = 3;
        else if (mRoll < quad + triple + dbl) multiplier = 2;

        if (multiplier > 1) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(log, multiplier - 1));
            player.sendMessage("§e✦ Богатое дерево! §7×" + multiplier + " брёвен");
            plugin.getLogger().info("[Lumber] " + player.getName()
                    + ": множитель ×" + multiplier + " (" + tier.name() + ")");
        }
    }

    /** Шанс из конфига, а при отсутствии ключа — встроенный дефолт по спеке. */
    private double chance(String section, String key) {
        return plugin.getConfig().getDouble("loot." + section + "." + key,
                DEFAULTS.getOrDefault(section + "." + key, 0.0));
    }

    private String sectionOf(AxeTier tier) {
        return switch (tier) {
            case VANILLA -> "vanilla";
            case IMPROVED -> "improved";
            case LUMBERJACK -> "lumberjack";
            case OLAF -> "olaf";
        };
    }

    private void give(Player player, ItemStack item) {
        if (item == null) return;
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }
}
