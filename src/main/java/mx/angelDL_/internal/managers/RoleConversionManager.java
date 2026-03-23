package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RoleConversionManager {
    private final Main plugin;

    public RoleConversionManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean convertKiraToMikami(Player kira, Player newKira) {
        // Verificar que el que ejecuta es Kira y el objetivo es Mikami
        String kiraRole = plugin.getRoleApi().getRole(kira.getUniqueId());
        String newKiraRole = plugin.getRoleApi().getRole(newKira.getUniqueId());

        if (!kiraRole.equalsIgnoreCase("kira") || !newKiraRole.equalsIgnoreCase("mikami")) {
            return false;
        }

        // Intercambiar roles
        plugin.getRoleApi().setRole(kira, "mikami");
        plugin.getRoleApi().setRole(newKira, "kira");

        // Transferir Death Note
        transferDeathNote(kira, newKira);

        // Mensajes
        kira.sendMessage("§cHas transferido el rol de Kira a " + newKira.getName());
        newKira.sendMessage("§4¡Ahora eres Kira!");
        Bukkit.broadcastMessage("§6¡El rol de Kira ha sido transferido!");

        return true;
    }

    // Agregar este método:
    public void convertNearToL(Player near) {
        plugin.getRoleApi().setRole(near, "l");
    }

    private void transferDeathNote(Player oldKira, Player newKira) {
        // Remover Death Note del Kira anterior
        for (ItemStack item : oldKira.getInventory().getContents()) {
            if (item != null &&
                    item.getType() == plugin.getConfigManager().getDeathNoteItem() &&
                    item.hasItemMeta() &&
                    item.getItemMeta().getDisplayName().equals("§4Death Note")) {
                oldKira.getInventory().remove(item);
                break;
            }
        }

        // Dar Death Note al nuevo Kira
        newKira.getInventory().addItem(plugin.getDeathNoteManager().createDeathNoteItem());
    }

}