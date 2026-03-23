package mx.angelDL_.internal.tasks;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class KiraParticleTask extends BukkitRunnable {
    private final Main plugin;
    private final ConfigManager configManager;

    public KiraParticleTask(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isKira(player) && hasDeathNoteInHand(player) && configManager.isKiraParticlesEnabled()) {
                plugin.getParticleSystem().spawnKiraParticles(player);
            }
        }
    }

    private boolean isKira(Player player) {
        String role = plugin.getRoleApi().getRole(player.getUniqueId());
        return role.equalsIgnoreCase("kira");
    }

    private boolean hasDeathNoteInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item != null &&
                item.getType() == configManager.getDeathNoteItem() &&
                item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§4Death Note");
    }
}