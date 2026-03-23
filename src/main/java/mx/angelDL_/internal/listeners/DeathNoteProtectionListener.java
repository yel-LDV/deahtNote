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

public class DeathNoteProtectionListener implements Listener {
    private final Main plugin;
    private final ConfigManager configManager;

    public DeathNoteProtectionListener(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Verificar si está intentando duplicar la Death Note
        if (isDeathNote(clicked) && isDeathNote(cursor)) {
            event.setCancelled(true);
            player.sendMessage("§cNo puedes tener múltiples Death Notes");
            return;
        }

        // Verificar tanto el item clickeado como el del cursor
        if (isDeathNote(clicked) || isDeathNote(cursor)) {
            // Permitir solo movimientos dentro del inventario del jugador
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().getType() == InventoryType.PLAYER) {
                // Verificar que no esté intentando duplicar
                if (isDeathNote(clicked) && isDeathNote(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§cNo puedes tener múltiples Death Notes");
                }
            } else {
                event.setCancelled(true);
                player.sendMessage("§cNo puedes mover la Death Note fuera de tu inventario");
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isDeathNote(event.getItemDrop().getItemStack())) {
            // ❌ ELIMINAR esta verificación de rol - NADIE puede tirar la Death Note
            // String role = plugin.getRoleApi().getRole(player.getUniqueId());
            // if (!role.equalsIgnoreCase("kira")) {
            event.setCancelled(true);
            player.sendMessage("§cNo puedes tirar la Death Note");
            // }
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Prevenir cambiar Death Note a la mano secundaria
        if (isDeathNote(event.getMainHandItem()) || isDeathNote(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage("§cNo puedes cambiar la Death Note de mano");
        }
    }

    private boolean isDeathNote(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals("§4Death Note");
    }
}