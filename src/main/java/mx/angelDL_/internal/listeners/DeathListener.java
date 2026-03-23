package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import mx.angelDL_.internal.managers.VictoryConditionManager;
import mx.angelDL_.internal.systemRol.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeathListener implements Listener {
    private final Main plugin;
    private final ConfigManager configManager;
    private final VictoryConditionManager victoryManager;

    public DeathListener(Main plugin, ConfigManager configManager, VictoryConditionManager victoryManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.victoryManager = victoryManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        String deadRole = plugin.getRoleApi().getRole(deadPlayer.getUniqueId());

        plugin.getPlayerCache().getPlayerData(deadPlayer.getUniqueId()).ifPresent(data -> {
            data.setAlive(false);
        });

        String roleDisplay = plugin.lang().get("role_names." + deadRole.toLowerCase());
        String deathFormat = configManager.config.getString("death_messages.format", "&c{player} ha muerto. Era {role}");
        event.setDeathMessage(
            deathFormat.replace("{player}", deadPlayer.getName()).replace("{role}", roleDisplay).replace('&', '§')
        );

        victoryManager.checkVictoryOnDeath(event);

        if (configManager.config.getBoolean("identification.clear_on_death", true)) {
            removeOnlyPluginItems(event);
        }
    }

    private void removeOnlyPluginItems(PlayerDeathEvent event) {
        List<ItemStack> toKeep = new ArrayList<>();
        for (ItemStack item : event.getDrops()) {
            if (!isPluginItem(item)) {
                toKeep.add(item);
            }
        }
        event.getDrops().clear();
        event.getDrops().addAll(toKeep);
    }

    private boolean isPluginItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String displayName = item.getItemMeta().getDisplayName();
        if (item.getType() == Material.WRITTEN_BOOK && displayName.equals("§4Death Note")) return true;
        if (item.getType() == Material.PAPER && displayName.startsWith("§eID: ")) return true;
        String activationName = plugin.getConfig().getString("shinigami_eye_deal.activation_item.name", "§4Sangre del Shinigami");
        String usedName = plugin.getConfig().getString("shinigami_eye_deal.used_item.name", "§8Pacto Consumido");
        if (displayName.equals(activationName) || displayName.equals(usedName)) return true;
        String missionName = plugin.getConfig().getString("mission_system.mission_item.name", "§bMisión de L");
        if (displayName.equals(missionName)) return true;
        String swapName = plugin.getConfig().getString("role_swap_system.swap_item.name", "§6Intercambio de Roles");
        if (displayName.equals(swapName)) return true;
        return false;
    }
}