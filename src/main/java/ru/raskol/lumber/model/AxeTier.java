package ru.raskol.lumber.model;

/** Тиры топоров. VANILLA = обычный ванильный (без PDC-тега). */
public enum AxeTier {
    VANILLA("§7Обычный топор", "§7", null),
    NEWCOMER("§eТопор новичка", "§e", null),
    LUMBERJACK("§bТопор дровосека", "§b", "NEWCOMER"),
    OLAF("§5Топор Олафа", "§5", "LUMBERJACK");

    public final String displayName;
    public final String color;
    public final String previousTier;

    AxeTier(String displayName, String color, String previousTier) {
        this.displayName = displayName;
        this.color = color;
        this.previousTier = previousTier;
    }

    public static AxeTier fromName(String name) {
        if (name == null) return VANILLA;
        try {
            return AxeTier.valueOf(name);
        } catch (IllegalArgumentException e) {
            return VANILLA;
        }
    }

    /** Какой фрагмент даёт этот тир (для следующего тира). */
    public AxeTier droppedFragment() {
        for (AxeTier t : values()) {
            if (name().equals(t.previousTier)) return t;
        }
        return null;
    }

    /** Какой фрагмент нужен для крафта этого тира. */
    public AxeTier requiredFragment() {
        return fromName(previousTier);
    }
}
