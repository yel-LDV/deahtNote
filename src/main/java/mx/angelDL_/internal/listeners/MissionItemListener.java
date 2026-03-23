package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import mx.angelDL_.internal.gui.MissionGUI;
import mx.angelDL_.internal.managers.MissionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class MissionItemListener implements Listener {
    private final Main plugin;
    private final MissionManager missionManager;
    private final MissionGUI missionGUI;

    public MissionItemListener(Main plugin, MissionManager missionManager, MissionGUI missionGUI) {
        this.plugin = plugin;
        this.missionManager = missionManager;
        this.missionGUI = missionGUI;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isMissionItem(item)) return;

        // Verificar que sea L
        if (!plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase("l")) {
            player.sendMessage(missionManager.getMessage("no_permission"));
            event.setCancelled(true);
            return;
        }

        // NUEVA VERIFICACIÓN: No puede abrir si ya tiene misión activa o está en GUI
        if (!missionManager.canOpenGUI(player)) {
            event.setCancelled(true);
            return;
        }

        // Verificar cooldown
        if (missionManager.hasCooldown(player)) {
            long remaining = missionManager.getRemainingCooldown(player);
            player.sendMessage(missionManager.getMessage("cooldown").replace("{time}", String.valueOf(remaining)));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        missionGUI.openZoneSelection(player);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isMissionItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(missionManager.getMessage("cannot_drop_item"));
        }
    }

    private boolean isMissionItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String expectedName = plugin.getConfig().getString("mission_system.mission_item.name", "§bMisión de L");
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }
}