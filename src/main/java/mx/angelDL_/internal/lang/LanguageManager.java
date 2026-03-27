package mx.angelDL_.internal.lang;

import mx.angelDL_.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LanguageManager {

    private final Main plugin;
    private FileConfiguration langConfig;
    private String currentLanguage;

    public LanguageManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currentLanguage = plugin.getConfig().getString("language", "es").toLowerCase();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        saveLangFile("es");
        saveLangFile("en");
        saveLangFile("fr");

        File langFile = new File(langFolder, currentLanguage + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + currentLanguage + ".yml. Falling back to es.yml.");
            langFile = new File(langFolder, "es.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        InputStream defStream = plugin.getResource("lang/" + currentLanguage + ".yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaults);
        }
        plugin.getLogger().info("Language loaded: " + currentLanguage);
    }

    private void saveLangFile(String lang) {
        File file = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
    }

    public String get(String key) {
        String value = langConfig.getString(key);
        if (value == null) {
            plugin.getLogger().warning("Missing lang key: " + key);
            return "[" + key + "]";
        }
        return value.replace('&', '§');
    }

    public String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
