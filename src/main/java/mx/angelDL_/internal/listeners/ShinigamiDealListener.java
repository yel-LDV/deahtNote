package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ShinigamiDealListener implements Listener {
    private final Main plugin;

    public ShinigamiDealListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Verificar si es el item de activación (ROJO)
        if (!plugin.getShinigamiDealManager().isActivationItem(item)) {
            return;
        }

        // Verificar que sea click derecho
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        // Activar el pacto - NO se necesita remover el item manualmente
        // porque activateDeal() ya lo cambia por la versión usada
        plugin.getShinigamiDealManager().activateDeal(player);
    }
}