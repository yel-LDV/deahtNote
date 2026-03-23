package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {
    private final Main plugin;
    private final ConfigManager configManager;

    public ActionBarManager(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        startActionBarTask();
    }

    /**
     * Inicia la tarea de actualización de Action Bars
     */
    private void startActionBarTask() {
        if (!configManager.config.getBoolean("action_bars.enabled", true)) return;

        int interval = configManager.config.getInt("action_bars.update_interval", 5) * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllActionBars();
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    /**
     * Actualiza todas las action bars
     */
    private void updateAllActionBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerActionBar(player);
        }
    }

    /**
     * Actualiza la action bar de un jugador específico
     */
    private void updatePlayerActionBar(Player player) {
        String role = plugin.getRoleApi().getRole(player.getUniqueId());
        String actionBarMessage = getActionBarMessage(player, role);

        if (actionBarMessage != null && !actionBarMessage.isEmpty()) {
            sendActionBar(player, actionBarMessage);
        }
    }

    /**
     * Obtiene el mensaje de action bar según el rol
     */
    private String getActionBarMessage(Player player, String role) {
        switch (role.toLowerCase()) {
            case "watari":
                if (configManager.config.getBoolean("action_bars.watari.enabled", true)) {
                    Player l = findPlayerWithRole("l");
                    if (l != null) {
                        String format = configManager.config.getString("action_bars.watari.format", "&9L: {player}");
                        return format.replace("{player}", l.getName()).replace('&', '§');
                    }
                }
                break;

            case "l":
                if (configManager.config.getBoolean("action_bars.l.enabled", true)) {
                    Player watari = findPlayerWithRole("watari");
                    if (watari != null) {
                        String format = configManager.config.getString("action_bars.l.format", "&9Watari: {player}");
                        return format.replace("{player}", watari.getName()).replace('&', '§');
                    }
                }
                break;

            case "kira":
                if (configManager.config.getBoolean("action_bars.kira.enabled", true)) { // Cambiado a true
                    Player mikami = findPlayerWithRole("mikami");
                    if (mikami != null) {
                        String format = configManager.config.getString("action_bars.kira.format", "&cMikami: {player}");
                        return format.replace("{player}", mikami.getName()).replace('&', '§');
                    } else {
                        return "&cMikami: No encontrado".replace('&', '§');
                    }
                }
                break;

            case "mikami":
                if (configManager.config.getBoolean("action_bars.mikami.enabled", true)) {
                    Player kira = findPlayerWithRole("kira");
                    if (kira != null) {
                        String format = configManager.config.getString("action_bars.mikami.format", "&4Kira: {player}");
                        return format.replace("{player}", kira.getName()).replace('&', '§');
                    }
                }
                break;
        }

        return null;
    }

    /**
     * Envía un mensaje a la action bar del jugador
     */
    public void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(net.kyori.adventure.text.Component.text(message));
        } catch (Exception e) {
            // Fallback para versiones antiguas
            try {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
            } catch (Exception e2) {
                plugin.getLogger().warning("No se pudo enviar action bar a " + player.getName());
            }
        }
    }

    /**
     * Encuentra un jugador con un rol específico
     */
    private Player findPlayerWithRole(String targetRole) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String role = plugin.getRoleApi().getRole(player.getUniqueId());
            if (role.equalsIgnoreCase(targetRole) && player.isOnline() && player.getHealth() > 0) {
                return player;
            }
        }
        return null;
    }
}