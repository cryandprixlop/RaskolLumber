package ru.raskol.lumber.model;

import org.bukkit.Location;

public final class Region {

    public String world;
    public int x1, y1, z1, x2, y2, z2;

    public Region() {}

    public Region(String world, int ax, int ay, int az, int bx, int by, int bz) {
        this.world = world;
        x1 = Math.min(ax, bx); y1 = Math.min(ay, by); z1 = Math.min(az, bz);
        x2 = Math.max(ax, bx); y2 = Math.max(ay, by); z2 = Math.max(az, bz);
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !world.equals(loc.getWorld().getName())) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }
}
