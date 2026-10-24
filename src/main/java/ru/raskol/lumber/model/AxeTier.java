package ru.raskol.lumber.model;

/**
 * Тиры топоров. VANILLA = обычный ванильный топор (без PDC-тега).
 * dropsFragment — какой фрагмент роняет этот топор.
 * craftFragment — какой фрагмент нужен для крафта этого топора.
 */
public enum AxeTier {

    VANILLA("§7Обычный топор", "§7", "IMPROVED", null, null),
    IMPROVED("§eУлучшенный топор", "§e", "LUMBERJACK", "IMPROVED", "§eФрагмент улучшенного топора"),
    LUMBERJACK("§bТопор дровосека", "§b", "OLAF", "LUMBERJACK", "§bФрагмент топора дровосека"),
    OLAF("§5Топор Олафа", "§5", null, "OLAF", "§5Фрагмент топора Олафа");

    public final String displayName;
    public final String color;
    public final String fragmentDisplayName;
    private final String dropsFragmentName;
    private final String craftFragmentName;

    AxeTier(String displayName, String color, String dropsFragmentName,
            String craftFragmentName, String fragmentDisplayName) {
        this.displayName = displayName;
        this.color = color;
        this.dropsFragmentName = dropsFragmentName;
        this.craftFragmentName = craftFragmentName;
        this.fragmentDisplayName = fragmentDisplayName;
    }

    public static AxeTier fromName(String name) {
        if (name == null) return VANILLA;
        try {
            return AxeTier.valueOf(name);
        } catch (IllegalArgumentException e) {
            return VANILLA;
        }
    }

    /** Фрагмент какого тира роняет этот топор (null = ничего). */
    public AxeTier dropsFragment() {
        return dropsFragmentName == null ? null : valueOf(dropsFragmentName);
    }

    /** Фрагмент какого тира нужен для крафта этого топора (null = не крафтится). */
    public AxeTier craftFragment() {
        return craftFragmentName == null ? null : valueOf(craftFragmentName);
    }
}
