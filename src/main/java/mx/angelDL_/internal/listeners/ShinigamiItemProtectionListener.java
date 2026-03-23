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
import org.bukkit.inventory.meta.ItemMeta;

public class ShinigamiItemProtectionListener implements Listener {
    private final Main plugin;

    public ShinigamiItemProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Verificar items del pacto Shinigami
        if (isShinigamiItem(clicked) || isShinigamiItem(cursor)) {
            // Permitir solo movimientos dentro del inventario del jugador
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().getType() == InventoryType.PLAYER) {
                // Permitido - movimiento interno
            } else {
                event.setCancelled(true);
                player.sendMessage("§cNo puedes mover los items del pacto fuera de tu inventario");
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isShinigamiItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            player.sendMessage("§cNo puedes tirar los items del pacto Shinigami");
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Prevenir cambiar items del pacto a la mano secundaria
        if (isShinigamiItem(event.getMainHandItem()) || isShinigamiItem(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage("§cNo puedes cambiar los items del pacto de mano");
        }
    }

    /**
     * Verificar si es un item del pacto Shinigami (activación o usado)
     */
    private boolean isShinigamiItem(ItemStack item) {
        if (item == null) return false;

        return plugin.getShinigamiDealManager().isActivationItem(item) ||
                plugin.getShinigamiDealManager().isUsedItem(item);
    }
}