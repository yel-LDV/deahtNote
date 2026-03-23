package mx.angelDL_.internal.tasks;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class HealthDisplayTask extends BukkitRunnable {
    private final Main plugin;

    public HealthDisplayTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getShinigamiDealManager().hasShinigamiDeal(player.getUniqueId())) {
                continue;
            }

            List<String> healthLines = new ArrayList<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target.getUniqueId().equals(player.getUniqueId())) continue;
                String healthText = plugin.getShinigamiDealManager().getHealthDisplay(target);
                if (!healthText.isEmpty()) {
                    healthLines.add("§7" + target.getName() + ": " + healthText);
                }
            }

            if (!healthLines.isEmpty()) {
                String combined = String.join(" §8| ", healthLines);
                plugin.getActionBarManager().sendActionBar(player, combined);
            }
        }
    }
}