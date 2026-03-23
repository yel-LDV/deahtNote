package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MikamiGiveIDListener implements Listener {
    private final Main plugin;

    public MikamiGiveIDListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Verificar si es Mikami golpeando a Kira de frente
        String damagerRole = plugin.getRoleApi().getRole(damager.getUniqueId());
        String victimRole = plugin.getRoleApi().getRole(victim.getUniqueId());

        if (!damagerRole.equalsIgnoreCase("mikami") || !victimRole.equalsIgnoreCase("kira")) return;

        // Verificar si es golpe frontal
        if (!isFrontalHit(damager, victim)) return;

        // Verificar si Mikami tiene una ID para dar
        ItemStack idToGive = getIdentificationPaper(damager);
        if (idToGive == null) return;

        // Dar la ID a Kira
        event.setCancelled(true); // Cancelar daño

        if (victim.getInventory().firstEmpty() == -1) {
            damager.sendMessage("§cEl inventario de Kira está lleno");
            return;
        }

        // Transferir ID
        removeIdentificationPaper(damager, idToGive);
        victim.getInventory().addItem(idToGive);

        // Mensajes
        damager.sendMessage("§aHas entregado una identificación a Kira");
        victim.sendMessage("§aMikami te ha entregado una identificación");
    }

    private boolean isFrontalHit(Player damager, Player victim) {
        // Calcular ángulo entre la dirección de la víctima y el atacante
        double victimYaw = victim.getLocation().getYaw();
        double damagerYaw = damager.getLocation().getYaw();

        double angleDiff = Math.abs(victimYaw - damagerYaw) % 360;
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        return angleDiff < 90; // Golpe frontal si está dentro de 90 grados
    }

    private ItemStack getIdentificationPaper(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isIdentificationPaper(item)) {
                return item;
            }
        }
        return null;
    }

    private void removeIdentificationPaper(Player player, ItemStack paper) {
        player.getInventory().remove(paper);
    }

    private boolean isIdentificationPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().startsWith("§eID: ");
    }
}
