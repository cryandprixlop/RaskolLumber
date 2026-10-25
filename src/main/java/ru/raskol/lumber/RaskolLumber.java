package ru.raskol.lumber;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.lumber.command.LumberCommand;
import ru.raskol.lumber.hook.SelectionHook;
import ru.raskol.lumber.listener.AxeCraftListener;
import ru.raskol.lumber.listener.AxeDurabilityListener;
import ru.raskol.lumber.listener.AxeLootListener;
import ru.raskol.lumber.listener.GoldenSaplingListener;
import ru.raskol.lumber.listener.LumberListener;
import ru.raskol.lumber.model.AxeTier;
import ru.raskol.lumber.service.AxeService;
import ru.raskol.lumber.store.LumberStore;
import ru.raskol.lumber.task.TreeScheduler;

public final class RaskolLumber extends JavaPlugin {

    private LumberStore store;
    private SelectionHook selectionHook;
    private TreeScheduler scheduler;
    private AxeService axeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new LumberStore(this);
        store.load();
        selectionHook = new SelectionHook(this);
        scheduler = new TreeScheduler(this, store);
        axeService = new AxeService(this);

        // Восстановить отложенные посадки после рестарта
        scheduler.rescheduleAll();
        // Резервная проверка очереди
        getServer().getScheduler().runTaskTimer(this, scheduler, 200L, 200L);

        // Рецепты крафта топоров из фрагментов
        registerAxeRecipes();

        PluginCommand cmd = getCommand("lumber");
        if (cmd != null) {
            LumberCommand executor = new LumberCommand(this, store, selectionHook, axeService);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(
                new LumberListener(this, store), this);
        getServer().getPluginManager().registerEvents(
                new AxeLootListener(this, axeService), this);
        getServer().getPluginManager().registerEvents(
                new AxeCraftListener(this, axeService), this);
        getServer().getPluginManager().registerEvents(
                new GoldenSaplingListener(this, axeService), this);
        getServer().getPluginManager().registerEvents(
                new AxeDurabilityListener(this, axeService), this);

        getLogger().info("RaskolLumber v" + getDescription().getVersion()
                + " включён. Зон: " + store.all().size()
                + ", в очереди посадок: " + store.pending().size()
                + ". WorldEdit: " + (selectionHook.isAvailable() ? "да" : "нет")
                + ". Регенерация: " + getConfig().getInt("regrow-seconds", 600) + " сек.");
    }

    /** 3 фрагмента в ряд -> топор тира (исход рулетки решает AxeCraftListener). */
    private void registerAxeRecipes() {
        int count = 0;
        for (AxeTier tier : new AxeTier[]{AxeTier.IMPROVED, AxeTier.LUMBERJACK, AxeTier.OLAF}) {
            NamespacedKey key = new NamespacedKey(this, "axe_craft_" + tier.name().toLowerCase());
            if (getServer().getRecipe(key) != null) continue;
            ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.IRON_AXE));
            recipe.shape("   ", "FFF", "   ");
            recipe.setIngredient('F', Material.FLINT);
            getServer().addRecipe(recipe);
            count++;
        }
        getLogger().info("[Lumber] рецептов топоров зарегистрировано: " + count);
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
        getLogger().info("RaskolLumber выключен.");
    }

    public LumberStore getStore() { return store; }
    public TreeScheduler getScheduler() { return scheduler; }
    public AxeService getAxeService() { return axeService; }
}
