package ru.raskol.lumber.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.raskol.lumber.RaskolLumber;
import ru.raskol.lumber.model.LumberZone;
import ru.raskol.lumber.model.PendingTree;
import ru.raskol.lumber.model.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LumberStore {

    private final RaskolLumber plugin;
    private final Map<String, LumberZone> zones = new LinkedHashMap<>();
    private final List<PendingTree> pending = new ArrayList<>();

    public LumberStore(RaskolLumber plugin) { this.plugin = plugin; }

    public void load() {
        zones.clear();
        pending.clear();
        File file = new File(plugin.getDataFolder(), "lumber.yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection zs = cfg.getConfigurationSection("zones");
        if (zs != null) {
            for (String name : zs.getKeys(false)) {
                ConfigurationSection s = zs.getConfigurationSection(name);
                if (s == null) continue;
                LumberZone z = new LumberZone();
                z.name = name;
                z.region = readRegion(s.getConfigurationSection("region"));
                if (z.region != null) zones.put(name, z);
            }
        }

        ConfigurationSection ps = cfg.getConfigurationSection("pending");
        if (ps != null) {
            for (String key : ps.getKeys(false)) {
                ConfigurationSection s = ps.getConfigurationSection(key);
                if (s == null) continue;
                PendingTree p = new PendingTree();
                p.world = s.getString("world", "world");
                p.x = s.getInt("x");
                p.y = s.getInt("y");
                p.z = s.getInt("z");
                p.logMaterial = s.getString("log", "OAK_LOG");
                p.growAtEpochSeconds = s.getLong("grow-at", 0);
                pending.add(p);
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (LumberZone z : zones.values()) {
            ConfigurationSection s = cfg.createSection("zones." + z.name);
            writeRegion(s.createSection("region"), z.region);
        }
        for (int i = 0; i < pending.size(); i++) {
            PendingTree p = pending.get(i);
            ConfigurationSection s = cfg.createSection("pending." + i);
            s.set("world", p.world);
            s.set("x", p.x);
            s.set("y", p.y);
            s.set("z", p.z);
            s.set("log", p.logMaterial);
            s.set("grow-at", p.growAtEpochSeconds);
        }
        try {
            cfg.save(new File(plugin.getDataFolder(), "lumber.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("[Lumber] не удалось сохранить lumber.yml: " + e.getMessage());
        }
    }

    private Region readRegion(ConfigurationSection s) {
        if (s == null) return null;
        Region r = new Region();
        r.world = s.getString("world", "world");
        r.x1 = s.getInt("x1"); r.y1 = s.getInt("y1"); r.z1 = s.getInt("z1");
        r.x2 = s.getInt("x2"); r.y2 = s.getInt("y2"); r.z2 = s.getInt("z2");
        return r;
    }

    private void writeRegion(ConfigurationSection s, Region r) {
        s.set("world", r.world);
        s.set("x1", r.x1); s.set("y1", r.y1); s.set("z1", r.z1);
        s.set("x2", r.x2); s.set("y2", r.y2); s.set("z2", r.z2);
    }

    public void addZone(LumberZone z) { zones.put(z.name, z); save(); }
    public void removeZone(String name) { zones.remove(name); save(); }
    public LumberZone get(String name) { return zones.get(name); }
    public List<LumberZone> all() { return new ArrayList<>(zones.values()); }

    public void addPending(PendingTree p) { pending.add(p); save(); }
    public List<PendingTree> pending() { return pending; }

    /** Уже запланирована посадка в этой колонне (x,z)? Одна посадка на дерево. */
    public boolean hasPendingColumn(String world, int x, int z) {
        for (PendingTree p : pending) {
            if (p.world.equals(world) && p.x == x && p.z == z) return true;
        }
        return false;
    }
}
