package ru.raskol.lumber.model;

/** Запланированная посадка: где, когда и из какого материала. */
public final class PendingTree {
    public String world;
    public int x, y, z;
    public String logMaterial = "OAK_LOG";   // материал ствола срубленного дерева
    public long growAtEpochSeconds;
}
