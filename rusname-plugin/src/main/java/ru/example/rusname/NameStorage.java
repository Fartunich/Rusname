package ru.example.rusname;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Хранит соответствие UUID игрока -> "Имя Фамилия" в файле names.yml.
 */
public class NameStorage {

    private final RusNamePlugin plugin;
    private final File file;
    private final YamlConfiguration config;

    public NameStorage(RusNamePlugin plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.file = new File(plugin.getDataFolder(), "names.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось создать names.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasName(UUID uuid) {
        return config.contains(uuid.toString());
    }

    /** Возвращает "Имя Фамилия" либо null, если не задано. */
    public String getFullName(UUID uuid) {
        return config.getString(uuid.toString());
    }

    public void setFullName(UUID uuid, String firstName, String lastName) {
        config.set(uuid.toString(), firstName + " " + lastName);
        save();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить names.yml", e);
        }
    }
}
