package ru.raskol.lumber.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.AxeTier;
import ru.raskol.lumber.service.AxeService;

import java.util.Random;

/**
 * Крафт топоров: 3 фрагмента нужного тира в верстаке.
 * Шанс успеха 25% (конфиг axe.craft-success-chance), провал = фрагменты сгорают.
 */
public final class AxeCraftListener implements Listener {

    private final RaskolLumber plugin;
    private final AxeService axeService;
    private final Random random = new Random();

    public AxeCraftListener(RaskolLumber plugin, AxeService axeService) {
        this.plugin = plugin;
        this.axeService = axeService;
    }

    /** Превью результата в верстаке: показываем топор только при валидных фрагментах. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe sr)) return;
        if (!isOurRecipe(sr)) return;

        AxeTier target = targetTier(sr);
        if (target == null || !validateMatrix(event.getInventory(), target)) {
            event.getInventory().setResult(null);
            return;
        }
        event.getInventory().setResult(axeService.createAxe(target));
    }

    /** Сам крафт: отменяем ванильный, съедаем 3 фрагмента, рулетка 25%. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe sr)) return;
        if (!isOurRecipe(sr)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AxeTier target = targetTier(sr);
        if (target == null || !validateMatrix(event.getInventory(), target)) return;

        event.setCancelled(true);
        consumeFragments(event.getInventory(), target);

        double chance = plugin.getConfig().getDouble("axe.craft-success-chance", 25.0);
        if (random.nextDouble(100.0) < chance) {
            give(player, axeService.createAxe(target));
            player.sendMessage("§a✔ Ковка удалась! Получен: " + target.displayName);
            plugin.getLogger().info("[Lumber] craft OK: " + player.getName() + " -> " + target.name());
        } else {
            player.sendMessage("§c✖ Ковка провалилась: фрагменты сгорели в горне!");
            plugin.getLogger().info("[Lumber] craft FAIL: " + player.getName() + " -> " + target.name());
        }
    }

    private boolean isOurRecipe(ShapedRecipe sr) {
        return sr.getKey().getNamespace().equals(plugin.getName().toLowerCase())
                && sr.getKey().getKey().startsWith("axe_craft_");
    }

    private AxeTier targetTier(ShapedRecipe sr) {
        String key = sr.getKey().getKey();
        if (!key.startsWith("axe_craft_")) return null;
        try {
            return AxeTier.valueOf(key.substring("axe_craft_".length()).toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** В верстаке должны лежать только фрагменты нужного тира, минимум 3. */
    private boolean validateMatrix(CraftingInventory inv, AxeTier target) {
        AxeTier fragTier = target.craftFragment();
        if (fragTier == null) return false;
        int count = 0;
        for (ItemStack s : inv.getMatrix()) {
            if (s == null || s.getType() == Material.AIR) continue;
            if (axeService.fragmentTierOf(s) != fragTier) return false;
            count++;
        }
        return count >= 3;
    }

    private void consumeFragments(CraftingInventory inv, AxeTier target) {
        ItemStack[] matrix = inv.getMatrix();
        int remaining = 3;
        for (int i = 0; i < matrix.length && remaining > 0; i++) {
            ItemStack s = matrix[i];
            if (s == null || s.getType() == Material.AIR) continue;
            if (axeService.fragmentTierOf(s) == null) continue;
            int take = Math.min(s.getAmount(), remaining);
            if (take >= s.getAmount()) {
                matrix[i] = null;
            } else {
                s.setAmount(s.getAmount() - take);
            }
            remaining -= take;
        }
        inv.setMatrix(matrix);
    }

    private void give(Player player, ItemStack item) {
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }
}
