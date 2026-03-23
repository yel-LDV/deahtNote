package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WatariAndLListener implements Listener {
    private final Main plugin;

    public WatariAndLListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWatariHitL(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Verificar si es Watari golpeando a L
        String damagerRole = plugin.getRoleApi().getRole(damager.getUniqueId());
        String victimRole = plugin.getRoleApi().getRole(victim.getUniqueId());

        plugin.getLogger().info("WatariHitL: Damager=" + damagerRole + ", Victim=" + victimRole);

        if (!damagerRole.equalsIgnoreCase("watari") || !victimRole.equalsIgnoreCase("l")) {
            return;
        }

        // ✅ CORRECCIÓN: ELIMINAR VERIFICACIÓN DE GOLPE FRONTAL
        // Cualquier golpe funciona, no importa la dirección

        // Entregar documento a L
        event.setCancelled(true); // Cancelar daño

        // Llamar al método del VictoryConditionManager
        plugin.getVictoryConditionManager().handleWatariGiveID(damager, victim);
    }

    @EventHandler
    public void onLInspectDocument(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Verificar si es L
        String playerRole = plugin.getRoleApi().getRole(player.getUniqueId());
        if (!playerRole.equalsIgnoreCase("l")) return;

        // Verificar si está clickeando con un documento
        if (item == null || item.getType() != Material.PAPER) return;

        // Verificar si es una identificación usando el método público
        if (!plugin.getVictoryConditionManager().isIdentificationPaper(item)) return;

        // Inspeccionar documento
        plugin.getVictoryConditionManager().inspectIdentificationAsL(player, item);
        player.sendMessage("§aHas inspeccionado el documento y revelado el rol del jugador");
        event.setCancelled(true);

        plugin.getLogger().info("LInspectDocument: L inspeccionó un documento");
    }
}