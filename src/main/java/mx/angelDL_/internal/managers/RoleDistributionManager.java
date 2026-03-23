package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;


public class RoleDistributionManager {
    private final Main plugin;
    private boolean rolesDistributed = false;

    public RoleDistributionManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean distributeRoles() {
        if (rolesDistributed) {
            Bukkit.broadcastMessage(plugin.lang().get("role_distribution.already_distributed"));
            return false;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (onlinePlayers.size() < 2) {
            Bukkit.broadcastMessage(plugin.lang().get("role_distribution.not_enough"));
            return false;
        }

        java.util.Collections.shuffle(onlinePlayers);

        List<String> priorityRoles = plugin.getConfig().getStringList("role_distribution.priority_roles");
        String defaultRole = plugin.getConfig().getString("role_distribution.default_role", "investigator");

        Bukkit.broadcastMessage(plugin.lang().get("role_distribution.started"));

        int assignedRoles = 0;
        for (int i = 0; i < Math.min(priorityRoles.size(), onlinePlayers.size()); i++) {
            Player player = onlinePlayers.get(i);
            String role = priorityRoles.get(i);
            plugin.getRoleApi().setRole(player, role);
            sendSpecialRoleMessage(player, role);
            assignedRoles++;
        }

        for (int i = assignedRoles; i < onlinePlayers.size(); i++) {
            Player player = onlinePlayers.get(i);
            plugin.getRoleApi().setRole(player, defaultRole);
            player.sendMessage(plugin.lang().get("role_distribution.assigned", Map.of("role", defaultRole)));
        }

        rolesDistributed = true;
        Bukkit.broadcastMessage(plugin.lang().get("role_distribution.completed"));
        return true;
    }

    private void sendSpecialRoleMessage(Player player, String role) {
        String displayName = plugin.lang().get("role_names." + role.toLowerCase());
        player.sendMessage(plugin.lang().get("role_distribution.assigned", Map.of("role", displayName)));

        switch (role.toLowerCase()) {
            case "kira":
                player.sendMessage("§4§l¡ERES KIRA!");
                player.sendMessage("§cTienes el poder de matar con la Death Note");
                player.sendMessage("§7Usa click derecho con la Death Note para abrir el menú");
                break;
            case "l":
                player.sendMessage("§9§l¡ERES L!");
                player.sendMessage("§bEres el mejor detective del mundo");
                player.sendMessage("§7Tienes acceso a misiones y puedes ver roles reales");
                break;
            case "mikami":
                player.sendMessage("§c§l¡ERES MIKAMI!");
                player.sendMessage("§eEres el seguidor de Kira");
                player.sendMessage("§7Puedes robar identificaciones y ayudar a Kira");
                break;
            case "mello":
                player.sendMessage("§6§l¡ERES MELLO!");
                player.sendMessage("§eEres el rival de Near");
                player.sendMessage("§7Tienes habilidades especiales de combate");
                break;
            case "near":
                player.sendMessage("§f§l¡ERES NEAR!");
                player.sendMessage("§7Eres el sucesor de L");
                player.sendMessage("§7Puedes convertirte en L si L muere");
                break;
            case "watari":
                player.sendMessage("§3§l¡ERES WATARI!");
                player.sendMessage("§bEres el apoyo de L");
                player.sendMessage("§7Puedes ver la ubicación de L");
                break;
        }
    }

    public void resetDistribution() {
        rolesDistributed = false;
    }

    public boolean areRolesDistributed() {
        return rolesDistributed;
    }
}