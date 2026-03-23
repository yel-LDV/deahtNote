package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class IdentificationProtectionListener implements Listener {
    private final Main plugin;
    private final ConfigManager configManager;

    public IdentificationProtectionListener(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Verificar tanto el item clickeado como el del cursor
        if (isIdentificationPaper(clicked) || isIdentificationPaper(cursor)) {
            // Permitir solo movimientos dentro del inventario del jugador
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().getType() == InventoryType.PLAYER) {
                // Permitido - movimiento interno dentro del inventario del jugador
            } else {
                event.setCancelled(true);
                player.sendMessage(configManager.getMessage("protection.cannot_move"));
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isIdentificationPaper(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("protection.cannot_drop"));
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Prevenir cambiar IDs a la mano secundaria
        if (isIdentificationPaper(event.getMainHandItem()) || isIdentificationPaper(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("protection.cannot_swap"));
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Limpiar TODOS los items del inventario al morir
        event.getDrops().clear();

        // Enviar mensaje configurable
        String message = configManager.getMessage("death.inventory_cleared");
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    private boolean isIdentificationPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().startsWith("§eID: ");
    }
}