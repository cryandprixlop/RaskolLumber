package ru.raskol.lumber;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.lumber.command.LumberCommand;
import ru.raskol.lumber.hook.SelectionHook;
import ru.raskol.lumber.hook.TownyHook;
import ru.raskol.lumber.hook.VaultHook;
import ru.raskol.lumber.listener.AxeCraftListener;
import ru.raskol.lumber.listener.AxeDurabilityListener;
import ru.raskol.lumber.listener.AxeLootListener;
import ru.raskol.lumber.listener.GoldenSaplingListener;
import ru.raskol.lumber.listener.LumberLicenseListener;
import ru.raskol.lumber.listener.LumberListener;
import ru.raskol.lumber.model.AxeTier;
import ru.raskol.lumber.service.AxeService;
import ru.raskol.lumber.service.LicenseService;
import ru.raskol.lumber.store.LicenseStore;
import ru.raskol.lumber.store.LumberStore;
import ru.raskol.lumber.task.TreeScheduler;

public final class RaskolLumber extends JavaPlugin {

    private LumberStore store;
    private LicenseStore licenseStore;
    private SelectionHook selectionHook;
    private TownyHook townyHook;
    private VaultHook vaultHook;
    private TreeScheduler scheduler;
    private AxeService axeService;
    private LicenseService licenseService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new LumberStore(this);
        store.load();
        licenseStore = new LicenseStore(this);
        licenseStore.load();
        selectionHook = new SelectionHook(this);
        townyHook = new TownyHook(this);
        vaultHook = new VaultHook(this);
        scheduler = new TreeScheduler(this, store);
        axeService = new AxeService(this);
        licenseService = new LicenseService(this, licenseStore, vaultHook, townyHook);

        scheduler.rescheduleAll();
        getServer().getScheduler().runTaskTimer(this, scheduler, 200L, 200L);

        registerAxeRecipes();

        PluginCommand cmd = getCommand("lumber");
        if (cmd != null) {
            LumberCommand executor = new LumberCommand(this, store, selectionHook,
                    axeService, licenseService, licenseStore, townyHook);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(new LumberListener(this, store), this);
        getServer().getPluginManager().registerEvents(new LumberLicenseListener(this, licenseService), this);
        getServer().getPluginManager().registerEvents(new AxeLootListener(this, axeService), this);
        getServer().getPluginManager().registerEvents(new AxeCraftListener(this, axeService), this);
        getServer().getPluginManager().registerEvents(new GoldenSaplingListener(this, axeService), this);
        getServer().getPluginManager().registerEvents(new AxeDurabilityListener(this, axeService), this);

        getLogger().info("RaskolLumber v" + getDescription().getVersion()
                + " включён. Зон: " + store.all().size()
                + ", посадок в очереди: " + store.pending().size()
                + ", лицензий активно: " + countLicenses()
                + ". Towny: " + (townyHook.isAvailable() ? "да" : "нет")
                + ". Vault: " + (vaultHook.isAvailable() ? "да" : "нет") + ".");
    }

    private int countLicenses() {
        int count = 0;
        for (ru.raskol.lumber.model.License lic : new java.util.ArrayList<>(java.util.List.of())) {
            // placeholder to avoid unused warning — real count below
        }
        // считаем через store: лицензии хранятся в LicenseStore
        return licenseStore == null ? 0 : licenseStore.activeCount();
    }

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
        if (licenseStore != null) licenseStore.save();
        getLogger().info("RaskolLumber выключен.");
    }

    public LumberStore getStore() { return store; }
    public TreeScheduler getScheduler() { return scheduler; }
    public AxeService getAxeService() { return axeService; }
    public LicenseService getLicenseService() { return licenseService; }
}
