package mx.angelDL_.internal.utils;

import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundSystem {
    private final ConfigManager configManager;

    public SoundSystem(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void playKiraOpenMenuSound(Player player) {
        Sound sound = configManager.getKiraOpenMenuSound();
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public void playStealSound(Player player) {
        // Sonido de robo según config
    }
}