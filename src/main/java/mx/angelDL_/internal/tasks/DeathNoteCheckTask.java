package mx.angelDL_.internal.tasks;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathNoteCheckTask extends BukkitRunnable {
    private final Main plugin;
    private final ConfigManager configManager;

    public DeathNoteCheckTask(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Verificar si el jugador es Kira
                String role = plugin.getRoleApi().getRole(player.getUniqueId());
                boolean isKira = role.equalsIgnoreCase("kira");

                if (isKira) {
                    // KIRA - Verificar y corregir Death Note
                    handleKiraPlayer(player);
                } else {
                    // NO KIRA - Remover todas las Death Notes
                    handleNonKiraPlayer(player);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error en tarea de verificación de Death Note: " + e.getMessage());
        }
    }

    /**
     * Maneja jugadores con rol Kira
     */
    private void handleKiraPlayer(Player player) {
        int deathNoteCount = countDeathNotes(player);

        if (deathNoteCount == 0) {
            // Kira no tiene Death Note, dársela
            giveSingleDeathNote(player);
        } else if (deathNoteCount > 1) {
            // Kira tiene múltiples Death Notes, eliminar duplicados
            removeDuplicateDeathNotes(player);
            player.sendMessage("§cSe han removido Death Notes duplicadas de tu inventario");
        } else {
            // Kira tiene exactamente 1 Death Note, verificar que esté en slot 8
            if (!hasDeathNoteInSlot8(player)) {
                moveDeathNoteToSlot8(player);
            }
        }

    }
    /**
     * Verifica si tiene Death Note en el slot 8
     */
    private boolean hasDeathNoteInSlot8(Player player) {
        ItemStack slot8Item = player.getInventory().getItem(8);
        return isDeathNote(slot8Item);
    }

    /**
     * Mueve la Death Note al slot 8
     */
    private void moveDeathNoteToSlot8(Player player) {
        ItemStack deathNote = null;
        int currentSlot = -1;

        // Buscar la Death Note
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDeathNote(item)) {
                deathNote = item;
                currentSlot = i;
                break;
            }
        }

        if (deathNote != null && currentSlot != 8) {
            // Guardar el item que está actualmente en el slot 8
            ItemStack slot8Item = player.getInventory().getItem(8);

            // Mover Death Note al slot 8
            player.getInventory().setItem(8, deathNote);

            // Mover el item del slot 8 a la posición original de la Death Note
            if (slot8Item != null) {
                player.getInventory().setItem(currentSlot, slot8Item);
            } else {
                player.getInventory().setItem(currentSlot, null);
            }
        }
    }

    /**
     * Maneja jugadores que NO son Kira
     */
    private void handleNonKiraPlayer(Player player) {
        int deathNoteCount = countDeathNotes(player);

        if (deathNoteCount > 0) {
            // Jugador no Kira tiene Death Note(s), remover todas
            removeAllDeathNotes(player);
            if (deathNoteCount > 1) {
                plugin.getLogger().info("Removidas " + deathNoteCount + " Death Notes de " + player.getName() + " (no es Kira)");
            }
        }
    }

    /**
     * Cuenta cuántas Death Notes tiene el jugador
     */
    private int countDeathNotes(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDeathNote(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Remueve Death Notes duplicadas, dejando solo una
     */
    private void removeDuplicateDeathNotes(Player player) {
        boolean foundFirst = false;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDeathNote(item)) {
                if (!foundFirst) {
                    // Mantener la primera Death Note encontrada
                    foundFirst = true;
                } else {
                    // Remover duplicados
                    player.getInventory().setItem(i, null);
                }
            }
        }

        // Asegurar que la Death Note restante esté en slot 9
        if (foundFirst) {
            moveDeathNoteToSlot9(player);
        }
    }

    /**
     * Remueve todas las Death Notes del jugador
     */
    private void removeAllDeathNotes(Player player) {
        int removedCount = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDeathNote(item)) {
                player.getInventory().setItem(i, null);
                removedCount++;
            }
        }

        // También verificar el cursor
        if (isDeathNote(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            removedCount++;
        }

        if (removedCount > 0 && !plugin.getRoleApi().getRole( player.getUniqueId() ).equalsIgnoreCase( "kira" )) {
            player.sendMessage("§cSe te han removido " + removedCount + " Death Notes porque no eres Kira");
        }
    }

    /**
     * Da una sola Death Note al jugador
     */
    private void giveSingleDeathNote(Player player) {
        // Primero verificar que no tenga ya una (doble verificación)
        if (countDeathNotes(player) == 0) {
            ItemStack deathNote = createDeathNoteItem();

            // Intentar poner en slot 9 primero
            if (player.getInventory().getItem(8) == null) {
                player.getInventory().setItem(8, deathNote);
            } else {
                // Slot 9 ocupado, buscar otro slot libre
                int freeSlot = player.getInventory().firstEmpty();
                if (freeSlot != -1) {
                    player.getInventory().setItem(freeSlot, deathNote);
                } else {
                    // Inventario lleno, dropear al suelo
                    player.getWorld().dropItem(player.getLocation(), deathNote);
                    player.sendMessage("§eTu Death Note fue dropeada al suelo porque tu inventario está lleno");
                }
            }

            player.sendMessage("§4Se te ha entregado una Death Note");
        }
    }

    /**
     * Verifica si tiene Death Note en el slot 9
     */
    private boolean hasDeathNoteInSlot9(Player player) {
        ItemStack slot9Item = player.getInventory().getItem(8);
        return isDeathNote(slot9Item);
    }

    /**
     * Mueve la Death Note al slot 9
     */
    private void moveDeathNoteToSlot9(Player player) {
        ItemStack deathNote = null;
        int currentSlot = -1;

        // Buscar la Death Note
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDeathNote(item)) {
                deathNote = item;
                currentSlot = i;
                break;
            }
        }

        if (deathNote != null && currentSlot != 8) {
            // Guardar el item que está actualmente en el slot 9
            ItemStack slot9Item = player.getInventory().getItem(8);

            // Mover Death Note al slot 9
            player.getInventory().setItem(8, deathNote);

            // Mover el item del slot 9 a la posición original de la Death Note
            if (slot9Item != null) {
                player.getInventory().setItem(currentSlot, slot9Item);
            } else {
                player.getInventory().setItem(currentSlot, null);
            }
        }
    }

    /**
     * Verifica si un item es la Death Note
     */
    private boolean isDeathNote(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals("§4Death Note");
    }

    /**
     * Crea el item Death Note
     */
    private ItemStack createDeathNoteItem() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName("§4Death Note");
        meta.setLore(java.util.Arrays.asList(
                "§8Libro de la muerte",
                "§7Click derecho para abrir el menú de muerte",
                "§7Necesitas la identificación de tu víctima",
                "§7Solo puedes tener una Death Note"
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        book.setItemMeta(meta);
        return book;
    }
}