package ru.example.rusname;

import org.bukkit.plugin.java.JavaPlugin;

public final class RusNamePlugin extends JavaPlugin {

    private NameStorage nameStorage;

    @Override
    public void onEnable() {
        this.nameStorage = new NameStorage(this);

        NameListener listener = new NameListener(this, nameStorage);
        getServer().getPluginManager().registerEvents(listener, this);

        SetNameCommand setNameCommand = new SetNameCommand(this, nameStorage, listener);
        getCommand("setname").setExecutor(setNameCommand);

        getLogger().info("RusNamePlugin включен.");
    }

    @Override
    public void onDisable() {
        if (nameStorage != null) {
            nameStorage.save();
        }
        getLogger().info("RusNamePlugin выключен.");
    }

    public NameStorage getNameStorage() {
        return nameStorage;
    }
}
