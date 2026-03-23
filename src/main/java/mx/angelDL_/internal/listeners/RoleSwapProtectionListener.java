package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class RoleSwapProtectionListener implements Listener {
    private final Main plugin;

    public RoleSwapProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Verificar items de intercambio
        if (plugin.getRoleSwapManager().isSwapItem(clicked) || plugin.getRoleSwapManager().isSwapItem(cursor)) {
            // Permitir solo movimientos dentro del inventario del jugador
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().getType() == InventoryType.PLAYER) {
                // Permitido - movimiento interno dentro del inventario del jugador
            } else {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfig().getString("role_swap_system.messages.cannot_move", "§cNo puedes mover el item de intercambio"));
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getRoleSwapManager().isSwapItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfig().getString("role_swap_system.messages.cannot_drop", "§cNo puedes tirar el item de intercambio"));
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Prevenir cambiar items de intercambio a la mano secundaria
        if (plugin.getRoleSwapManager().isSwapItem(event.getMainHandItem()) ||
                plugin.getRoleSwapManager().isSwapItem(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfig().getString("role_swap_system.messages.cannot_move", "§cNo puedes mover el item de intercambio"));
        }
    }
}