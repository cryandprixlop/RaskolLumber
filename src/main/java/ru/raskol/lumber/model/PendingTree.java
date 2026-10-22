package ru.raskol.lumber.model;

import org.bukkit.TreeType;

/** Запланированная посадка: где и когда посадить, какого типа. */
public final class PendingTree {
    public String world;
    public int x, y, z;
    public TreeType type;
    public long growAtEpochSeconds;
}
