package ru.raskol.lumber.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.hook.SelectionHook;
import ru.raskol.lumber.hook.TownyHook;
import ru.raskol.lumber.model.AxeTier;
import ru.raskol.lumber.model.LumberZone;
import ru.raskol.lumber.model.License;
import ru.raskol.lumber.model.Region;
import ru.raskol.lumber.service.AxeService;
import ru.raskol.lumber.service.LicenseService;
import ru.raskol.lumber.store.LicenseStore;
import ru.raskol.lumber.store.LumberStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LumberCommand implements CommandExecutor, TabCompleter {

    private final RaskolLumber plugin;
    private final LumberStore store;
    private final SelectionHook selection;
    private final AxeService axeService;
    private final LicenseService licenseService;
    private final LicenseStore licenseStore;
    private final TownyHook towny;

    public LumberCommand(RaskolLumber plugin, LumberStore store, SelectionHook selection,
                         AxeService axeService, LicenseService licenseService,
                         LicenseStore licenseStore, TownyHook towny) {
        this.plugin = plugin;
        this.store = store;
        this.selection = selection;
        this.axeService = axeService;
        this.licenseService = licenseService;
        this.licenseStore = licenseStore;
        this.towny = towny;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "create"   -> handleCreate(sender, args);
            case "delete"   -> handleDelete(sender, args);
            case "list"     -> handleList(sender);
            case "reload"   -> handleReload(sender);
            case "axe"      -> handleAxe(sender, args);
            case "fragment" -> handleFragment(sender, args);
            case "golden"   -> handleGolden(sender, args);
            case "license"  -> handleLicense(sender, args);
            default         -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== RaskolLumber ===");
        sender.sendMessage("§e/lumber license buy §7— купить лицензию лесоруба");
        sender.sendMessage("§e/lumber license status §7— статус лицензии");
        sender.sendMessage("§e/lumber license setprice <цена> §7— (король) цена в своём городе");
        sender.sendMessage("§e/lumber create <имя> §7— создать зону из WE-выделения");
        sender.sendMessage("§e/lumber delete <имя> §7— удалить зону");
        sender.sendMessage("§e/lumber list §7— список зон и очередь посадок");
        sender.sendMessage("§e/lumber axe give <tier> <игрок> [кол-во] §7— выдать топор");
        sender.sendMessage("§e/lumber fragment give <tier> <игрок> [кол-во] §7— выдать фрагмент");
        sender.sendMessage("§e/lumber golden give <игрок> [кол-во] §7— выдать золотой саженец");
        sender.sendMessage("§e/lumber reload §7— перезагрузить конфиг");
        sender.sendMessage("§7Тиры: VANILLA, IMPROVED, LUMBERJACK, OLAF");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("lumber.admin")) return true;
        sender.sendMessage("§cНет прав.");
        return false;
    }

    /* ================= license ================= */

    private void handleLicense(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§c/lumber license <buy|status|setprice>"); return; }
        switch (args[1].toLowerCase()) {
            case "buy" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
                p.sendMessage(licenseService.buy(p).message);
            }
            case "status" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
                License lic = licenseStore.get(p.getUniqueId().toString());
                if (lic == null || !lic.isValid()) {
                    p.sendMessage("§7Лицензия неактивна. Купи: §e/lumber license buy");
                } else {
                    p.sendMessage("§aЛицензия лесоруба активна. Осталось: §e" + lic.remainingFormatted());
                }
            }
            case "setprice" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
                if (args.length < 3) { p.sendMessage("§c/lumber license setprice <цена>"); return; }
                String town = towny.residentTownName(p);
                if (town == null && !p.hasPermission("lumber.admin")) {
                    p.sendMessage("§cТы должен быть жителем города.");
                    return;
                }
                if (!p.hasPermission("lumber.admin") && !towny.isKing(p)) {
                    p.sendMessage("§cТолько король может менять цену лицензии.");
                    return;
                }
                double price;
                try { price = Double.parseDouble(args[2]); }
                catch (NumberFormatException e) { p.sendMessage("§cЦена должна быть числом."); return; }
                if (price < 0) { p.sendMessage("§cЦена не может быть отрицательной."); return; }
                if (town != null) licenseStore.setTownPrice(town, price);
                p.sendMessage("§aЦена лицензии лесоруба для " + town + " теперь §b" + price + "§a.");
            }
            default -> sender.sendMessage("§cНеизвестная подкоманда.");
        }
    }

    /* ================= зоны ================= */

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
        sender.sendMessage("§aКонфиг перезагружен. Зоны и очереди не тронуты.");
    }

    /* ================= выдача ================= */

    private void handleAxe(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/lumber axe give <VANILLA|IMPROVED|LUMBERJACK|OLAF> <игрок> [кол-во]");
            return;
        }
        AxeTier tier;
        try { tier = AxeTier.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cНеизвестный тир."); return; }
        Player target = plugin.getServer().getPlayer(args[3]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = parseInt(args.length >= 5 ? args[4] : "1", 1);
        for (int i = 0; i < amount; i++) target.getInventory().addItem(axeService.createAxe(tier));
        sender.sendMessage("§aВыдано " + amount + " × " + tier.displayName + " §aигроку " + target.getName());
    }

    private void handleFragment(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/lumber fragment give <IMPROVED|LUMBERJACK|OLAF> <игрок> [кол-во]");
            return;
        }
        AxeTier tier;
        try { tier = AxeTier.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cНеизвестный тир."); return; }
        if (tier == AxeTier.VANILLA) { sender.sendMessage("§cУ обычного топора нет фрагмента."); return; }
        Player target = plugin.getServer().getPlayer(args[3]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = parseInt(args.length >= 5 ? args[4] : "1", 1);
        target.getInventory().addItem(axeService.createFragment(tier, amount));
        sender.sendMessage("§aВыдано " + amount + " × " + tier.fragmentDisplayName + " §aигроку " + target.getName());
    }

    private void handleGolden(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/lumber golden give <игрок> [кол-во]");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = parseInt(args.length >= 4 ? args[3] : "1", 1);
        target.getInventory().addItem(axeService.createGoldenSapling(amount));
        sender.sendMessage("§aВыдано " + amount + " × §6✦ Золотой саженец ✦ §aигроку " + target.getName());
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("license", "create", "delete", "list", "reload",
                    "axe", "fragment", "golden"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "license" -> filter(Arrays.asList("buy", "status", "setprice"), args[1]);
                case "axe", "fragment", "golden" -> filter(List.of("give"), args[1]);
                case "delete" -> zoneNames(args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("axe") && args[1].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("VANILLA", "IMPROVED", "LUMBERJACK", "OLAF"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("fragment") && args[1].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("IMPROVED", "LUMBERJACK", "OLAF"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> zoneNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (LumberZone z : store.all()) names.add(z.name);
        return filter(names, prefix);
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
