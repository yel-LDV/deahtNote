package mx.angelDL_.internal.managers;

import mx.angelDL_.internal.config.ConfigManager;

public class MelloManager {
    private final ConfigManager configManager;

    public MelloManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public double getMelloDamage(String targetRole) {
        if (targetRole.equalsIgnoreCase("kira") || targetRole.equalsIgnoreCase("l")) {
            return configManager.config.getDouble("roles.mello.damage.kira_l", 2.0);
        } else {
            return configManager.config.getDouble("roles.mello.damage.others", 4.0);
        }
    }

    public boolean shouldPenalizeAllyKill() {
        return configManager.config.getBoolean("roles.mello.ally_penalty", true);
    }
}