package ru.raskol.lumber;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.lumber.command.LumberCommand;
import ru.raskol.lumber.hook.SelectionHook;
import ru.raskol.lumber.listener.LumberListener;
import ru.raskol.lumber.store.LumberStore;
import ru.raskol.lumber.task.TreeScheduler;

public final class RaskolLumber extends JavaPlugin {

    private LumberStore store;
    private SelectionHook selectionHook;
    private TreeScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new LumberStore(this);
        store.load();
        selectionHook = new SelectionHook(this);
        scheduler = new TreeScheduler(this, store);

        // Восстановить отложенные посадки из lumber.yml
        scheduler.rescheduleAll();

        // Резервная проверка очереди раз в 10 секунд
        getServer().getScheduler().runTaskTimer(this, scheduler, 200L, 200L);

        PluginCommand cmd = getCommand("lumber");
        if (cmd != null) {
            LumberCommand executor = new LumberCommand(this, store, selectionHook);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(
                new LumberListener(this, store), this);

        getLogger().info("RaskolLumber v" + getDescription().getVersion()
                + " включён. Зон: " + store.all().size()
                + ", в очереди посадок: " + store.pending().size()
                + ". WorldEdit: " + (selectionHook.isAvailable() ? "да" : "нет")
                + ". Регенерация: " + getConfig().getInt("regrow-seconds", 600) + " сек.");
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
        getLogger().info("RaskolLumber выключен.");
    }

    public LumberStore getStore() { return store; }
    public TreeScheduler getScheduler() { return scheduler; }
}
