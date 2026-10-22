package ru.raskol.lumber.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.hook.SelectionHook;
import ru.raskol.lumber.model.LumberZone;
import ru.raskol.lumber.model.Region;
import ru.raskol.lumber.store.LumberStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LumberCommand implements CommandExecutor, TabCompleter {

    private final RaskolLumber plugin;
    private final LumberStore store;
    private final SelectionHook selection;

    public LumberCommand(RaskolLumber plugin, LumberStore store, SelectionHook selection) {
        this.plugin = plugin;
        this.store = store;
        this.selection = selection;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list"   -> handleList(sender);
            case "reload" -> handleReload(sender);
            default       -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== RaskolLumber ===");
        sender.sendMessage("§e/lumber create <имя> §7— создать зону из WE-выделения");
        sender.sendMessage("§e/lumber delete <имя> §7— удалить зону");
        sender.sendMessage("§e/lumber list §7— список зон");
        sender.sendMessage("§e/lumber reload §7— перезагрузить конфиг");
        sender.sendMessage("§7Деревья в зонах восстанавливаются через "
                + plugin.getConfig().getInt("regrow-seconds", 600) + " сек.");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("lumber.admin")) return true;
        sender.sendMessage("§cНет прав.");
        return false;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
        if (args.length < 2) { p.sendMessage("§c/lumber create <имя>"); return; }
        String name = args[1];
        if (store.get(name) != null) { p.sendMessage("§cЗона " + name + " уже существует."); return; }

        Region sel = selection.getSelection(p);
        if (sel == null) { p.sendMessage("§cСначала сделай выделение WorldEdit."); return; }

        LumberZone z = new LumberZone();
        z.name = name;
        z.region = sel;
        store.addZone(z);
        p.sendMessage("§aЛесопилка §e" + name + " §aсоздана. Регион: "
                + sel.x1 + "," + sel.y1 + "," + sel.z1 + " -> " + sel.x2 + "," + sel.y2 + "," + sel.z2);
        plugin.getLogger().info("[Lumber] zone created: " + name + " by " + p.getName());
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 2) { sender.sendMessage("§c/lumber delete <имя>"); return; }
        String name = args[1];
        if (store.get(name) == null) { sender.sendMessage("§cЗоны " + name + " нет."); return; }
        store.removeZone(name);
        sender.sendMessage("§cЛесопилка " + name + " удалена.");
    }

    private void handleList(CommandSender sender) {
        List<LumberZone> all = store.all();
        if (all.isEmpty()) { sender.sendMessage("§7Лесопилок нет."); return; }
        sender.sendMessage("§6=== Лесопилок: " + all.size() + " ===");
        for (LumberZone z : all) {
            Region r = z.region;
            sender.sendMessage("§e" + z.name + " §7— " + r.world + " ["
                    + r.x1 + "," + r.y1 + "," + r.z1 + " → " + r.x2 + "," + r.y2 + "," + r.z2 + "]");
        }
        sender.sendMessage("§7В очереди на посадку: §e" + store.pending().size() + " §7деревьев.");
    }

    private void handleReload(CommandSender sender) {
        if (!admin(sender)) return;
        plugin.reloadConfig();
        sender.sendMessage("§aКонфиг перезагружен. Зоны и очередь посадок остались в lumber.yml.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "delete", "list", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> names = new ArrayList<>();
            for (LumberZone z : store.all()) names.add(z.name);
            return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
    }
}
