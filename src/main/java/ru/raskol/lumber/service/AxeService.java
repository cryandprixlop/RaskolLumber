package ru.raskol.lumber.service;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.AxeTier;

import java.util.ArrayList;
import java.util.List;

/** Создание и распознавание топоров, фрагментов и золотого саженца. */
public final class AxeService {

    private final RaskolLumber plugin;
    public final NamespacedKey axeTierKey;
    public final NamespacedKey axeFragmentKey;
    public final NamespacedKey goldenSaplingKey;

    public AxeService(RaskolLumber plugin) {
        this.plugin = plugin;
        this.axeTierKey = new NamespacedKey(plugin, "axe_tier");
        this.axeFragmentKey = new NamespacedKey(plugin, "axe_fragment");
        this.goldenSaplingKey = new NamespacedKey(plugin, "golden_sapling");
    }

    /* ================= Топоры ================= */

    public boolean isAxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }

    /** Тир топора; null если это не топор. */
    public AxeTier tierOf(ItemStack item) {
        if (!isAxe(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return AxeTier.VANILLA;
        String tag = meta.getPersistentDataContainer().get(axeTierKey, PersistentDataType.STRING);
        if (tag == null) return AxeTier.VANILLA;
        try {
            return AxeTier.valueOf(tag);
        } catch (IllegalArgumentException e) {
            return AxeTier.VANILLA;
        }
    }

    public ItemStack createAxe(AxeTier tier) {
        Material base = switch (tier) {
            case IMPROVED -> Material.IRON_AXE;
            case LUMBERJACK -> Material.DIAMOND_AXE;
            case OLAF -> Material.NETHERITE_AXE;
            default -> Material.IRON_AXE;
        };
        ItemStack item = new ItemStack(base);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(tier.displayName);
        List<String> lore = new ArrayList<>();
        lore.add(tier.color + "Инструмент дровосека уровня " + tier.name());
        if (tier == AxeTier.OLAF) {
            lore.add("§7Легендарный топор северного мастера");
        }
        meta.setLore(lore);
        if (tier != AxeTier.VANILLA) {
            meta.getPersistentDataContainer().set(axeTierKey, PersistentDataType.STRING, tier.name());
        }
        item.setItemMeta(meta);
        return item;
    }

    /* ================= Фрагменты ================= */

    public ItemStack createFragment(AxeTier tier, int amount) {
        if (tier == AxeTier.VANILLA) return null;
        ItemStack item = new ItemStack(Material.FLINT, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(tier.fragmentDisplayName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Собери §e3 §7фрагмента для крафта:");
        lore.add("§7" + tier.displayName);
        lore.add("§8Шанс успеха крафта: 25%");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(axeFragmentKey, PersistentDataType.STRING, tier.name());
        item.setItemMeta(meta);
        return item;
    }

    /** Тир фрагмента; null если это не наш фрагмент. */
    public AxeTier fragmentTierOf(ItemStack item) {
        if (item == null || item.getType() != Material.FLINT) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String tag = meta.getPersistentDataContainer().get(axeFragmentKey, PersistentDataType.STRING);
        if (tag == null) return null;
        try {
            return AxeTier.valueOf(tag);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* ================= Золотой саженец ================= */

    public ItemStack createGoldenSapling(int amount) {
        ItemStack item = new ItemStack(Material.OAK_SAPLING, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§6✦ Золотой саженец ✦");
        List<String> lore = new ArrayList<>();
        lore.add("§eРедчайшая находка дровосека");
        lore.add("§cНельзя посадить!");
        lore.add("§7Материал легендарных крафтов");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(goldenSaplingKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isGoldenSapling(ItemStack item) {
        if (item == null || item.getType() != Material.OAK_SAPLING) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(goldenSaplingKey, PersistentDataType.BYTE);
    }
}
