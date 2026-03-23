package mx.angelDL_.internal.tasks;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class ActionBarTask extends BukkitRunnable {
    private final Main plugin;

    public ActionBarTask(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String role = plugin.getRoleApi().getRole(player.getUniqueId());

            if (role.equalsIgnoreCase("watari")) {
                Player l = findPlayerWithRole("l");
                if (l != null) {
                    String message = plugin.lang().get("action_bar.watari_format", Map.of("player", l.getName()));
                    sendActionBar(player, message);
                }
            } else if (role.equalsIgnoreCase("l")) {
                Player watari = findPlayerWithRole("watari");
                if (watari != null) {
                    String message = plugin.lang().get("action_bar.l_format", Map.of("player", watari.getName()));
                    sendActionBar(player, message);
                }
            }
        }
    }

    private Player findPlayerWithRole(String role) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase(role)) {
                return player;
            }
        }
        return null;
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
    }
}