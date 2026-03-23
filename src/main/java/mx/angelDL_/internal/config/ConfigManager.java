package mx.angelDL_.internal.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;

public class ConfigManager {
    public final FileConfiguration config;

    public ConfigManager(FileConfiguration config) {
        this.config = config;
    }

    // debugmode
    public boolean getDebugMode() {
        return config.getBoolean("debugmode", false);
    }

    // Cooldowns
    public int getDeathNoteCooldown() {
        return config.getInt("cooldowns.death_note_kill", 60);
    }

    public int getMelloCooldown() {
        return config.getInt("cooldowns.mello_ability", 20);
    }

    public int getStealCooldown() {
        return config.getInt("cooldowns.steal_identification", 120);
    }

    // Kira Particles
    public boolean isKiraParticlesEnabled() {
        return config.getBoolean("roles.kira.particles.enabled", true);
    }

    public Particle getKiraParticleType() {
        try {
            return Particle.valueOf(config.getString("roles.kira.particles.type", "REDSTONE"));
        } catch (Exception e) {
            return Particle.HEART;
        }
    }

    public int getKiraParticleCount() {
        return config.getInt("roles.kira.particles.count", 5);
    }

    public double getKiraParticleSpeed() {
        return config.getDouble("roles.kira.particles.speed", 0.1);
    }

    // Sounds
    public Sound getKiraOpenMenuSound() {
        try {
            return Sound.valueOf(config.getString("roles.kira.sounds.open_menu", "BLOCK_END_PORTAL_SPAWN"));
        } catch (Exception e) {
            return Sound.BLOCK_END_PORTAL_SPAWN;
        }
    }

    public Sound getKiraKillSound() {
        try {
            return Sound.valueOf(config.getString("roles.kira.sounds.kill", "ENTITY_WITHER_DEATH"));
        } catch (Exception e) {
            return Sound.ENTITY_WITHER_DEATH;
        }
    }

    // Mello
    public double getMelloDamageKiraL() {
        return config.getDouble("roles.mello.damage.kira_l", 2.0);
    }

    public double getMelloDamageOthers() {
        return config.getDouble("roles.mello.damage.others", 4.0);
    }

    public boolean shouldPenalizeAllyKill() {
        return config.getBoolean("roles.mello.ally_penalty", true);
    }

    // Messages
    public String getMessage(String path) {
        return config.getString("messages." + path, "&cMensaje no configurado").replace('&', '§');
    }

    public String getRoleDisplayName(String role) {
        return config.getString("messages.roles." + role.toLowerCase(), "&7" + role).replace('&', '§');
    }

    // Action Bar
    public int getActionBarInterval() {
        return config.getInt("action_bar.update_interval", 5);
    }

    public String getWatariActionBar() {
        return config.getString("action_bar.watari_format", "&9L: {player}").replace('&', '§');
    }

    public String getLActionBar() {
        return config.getString("action_bar.l_format", "&9Watari: {player}").replace('&', '§');
    }

    // Death Note Item
    public Material getDeathNoteItem() {
        try {
            return Material.valueOf(config.getString("roles.kira.death_note_item", "WRITTEN_BOOK"));
        } catch (Exception e) {
            return Material.WRITTEN_BOOK;
        }
    }

    // Death Note Protection
    public boolean isDeathNoteProtected() {
        return config.getBoolean("death_note.protected", true);
    }
}