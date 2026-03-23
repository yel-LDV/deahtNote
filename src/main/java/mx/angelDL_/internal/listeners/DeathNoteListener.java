package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import mx.angelDL_.internal.managers.DeathNoteManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DeathNoteListener implements Listener {
    private final Main plugin;
    private final ConfigManager configManager;
    private final DeathNoteManager deathNoteManager;

    public DeathNoteListener(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.deathNoteManager = plugin.getDeathNoteManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Verificar si es el Death Note
        if (!isDeathNote(item)) return;

        // Verificar que sea click derecho
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        // Verificar rol de Kira
        String playerRole = plugin.getRoleApi().getRole(player.getUniqueId());
        if (!playerRole.equalsIgnoreCase("kira")) {
            player.sendMessage("§cSolo Kira puede usar la Death Note");
            return;
        }

        // Abrir menú de Death Note
        deathNoteManager.openDeathNoteMenu(player);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        // Verificar si el inventario es el Death Note
        if (event.getView().getTitle().equals("Death Note") && clicked != null) {
            event.setCancelled(true);

            // Verificar si es una cabeza de jugador
            if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta.hasDisplayName()) {
                    String playerName = meta.getDisplayName().replace("§c", "");
                    Player target = plugin.getServer().getPlayerExact(playerName);

                    if (target != null && target.isOnline()) {
                        // Matar al jugador seleccionado
                        deathNoteManager.killPlayer(player, target);
                        player.closeInventory();
                    }
                }
            }
        }
    }

    private boolean isDeathNote(ItemStack item) {
        if (item == null || item.getType() != configManager.getDeathNoteItem()) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals("§4Death Note");
    }
}