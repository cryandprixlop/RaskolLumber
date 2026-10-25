package ru.raskol.lumber.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.raskol.lumber.RaskolLumber;

public final class VaultHook {

    private final RaskolLumber plugin;
    private Economy economy;

    public VaultHook(RaskolLumber plugin) {
        this.plugin = plugin;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (rsp != null) this.economy = rsp.getProvider();
    }

    public boolean isAvailable() { return economy != null; }

    public double balance(Player p) { return isAvailable() ? economy.getBalance(p) : 0.0; }
    public String format(double a) { return isAvailable() ? economy.format(a) : String.valueOf(a); }
    public boolean has(Player p, double a) { return isAvailable() && economy.has(p, a); }
    public boolean withdraw(Player p, double a) {
        return isAvailable() && economy.withdrawPlayer(p, a).transactionSuccess();
    }
    public boolean deposit(Player p, double a) {
        return isAvailable() && economy.depositPlayer(p, a).transactionSuccess();
    }
    public boolean createAccount(String name) {
        if (!isAvailable()) return false;
        try { return economy.hasAccount(name) || economy.createPlayerAccount(name); }
        catch (Throwable t) { return false; }
    }
    public boolean depositAccount(String name, double amount) {
        return isAvailable() && economy.depositPlayer(name, amount).transactionSuccess();
    }
}
