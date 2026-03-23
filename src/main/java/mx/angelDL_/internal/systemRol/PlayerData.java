package mx.angelDL_.internal.systemRol;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa los datos de un jugador: su rol, estado, cooldowns e identificaciones.
 */
public class PlayerData {

    private final String playerName;
    private String role;
    private boolean alive;
    private long lastStealTime;
    private final List<String> identifications;

    public PlayerData(String playerName, String role, boolean alive) {
        this.playerName = playerName;
        this.role = role;
        this.alive = alive;
        this.lastStealTime = 0L;
        this.identifications = new ArrayList<>();
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public long getLastStealTime() {
        return lastStealTime;
    }

    public void setLastStealTime(long lastStealTime) {
        this.lastStealTime = lastStealTime;
    }

    public List<String> getIdentifications() {
        return new ArrayList<>(identifications); // Devolver copia para evitar modificaciones externas
    }

    /**
     * Da la identificación propia al jugador.
     */
    public void giveDefaultIdentification() {
        identifications.clear();
        identifications.add(playerName);

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null && player.isOnline()) {
            // Remover identificaciones viejas primero - MÉTODO CORREGIDO
            removeOldIdentifications(player);

            // Dar nueva identificación
            ItemStack newId = createIdentificationPaper(playerName, "§eID: ");
            if (player.getInventory().firstEmpty() == -1) {
                // Inventario lleno, dropear al suelo
                player.getWorld().dropItem(player.getLocation(), newId);
                player.sendMessage("§eTu identificación fue dropeada al suelo porque tu inventario está lleno");
            } else {
                player.getInventory().addItem(newId);
            }
        }
    }

    /**
     * Método CORREGIDO para remover identificaciones viejas
     */
    private void removeOldIdentifications(Player player) {
        // Crear una lista temporal de slots a limpiar
        List<Integer> slotsToRemove = new ArrayList<>();

        // Buscar todas las identificaciones en el inventario
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isIdentificationPaper(item)) {
                slotsToRemove.add(i);
            }
        }

        // Limpiar los slots identificados
        for (int slot : slotsToRemove) {
            player.getInventory().setItem(slot, null);
        }
    }

    /**
     * Añade una identificación al jugador (SOLO CACHE).
     */
    public boolean addIdentification(String name) {
        if (identifications.contains(name)) {
            // Ya existe esta identificación
            return false;
        }
        identifications.add(name);
        return true;
    }

    /**
     * Añade una identificación al jugador Y crea el item físico.
     */
    public boolean addIdentificationWithItem(String name) {
        if (identifications.contains(name)) {
            return false;
        }
        identifications.add(name);

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null && player.isOnline()) {
            player.getInventory().addItem(createIdentificationPaper(name, "§eID: "));
        }
        return true;
    }

    /**
     * Elimina una identificación del jugador (SOLO CACHE).
     */
    public boolean removeIdentification(String name) {
        boolean removed = identifications.remove(name);
        return removed;
    }

    /**
     * Elimina una identificación del jugador Y remueve el item físico.
     */
    public boolean removeIdentificationWithItem(String name) {
        boolean removed = identifications.remove(name);
        if (removed) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && player.isOnline()) {
                removeIdentificationFromInventory(player, name);
            }
        }
        return removed;
    }

    /**
     * Método CORREGIDO para remover identificación del inventario
     */
    private void removeIdentificationFromInventory(Player player, String name) {
        // Buscar la identificación en el inventario
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isIdentificationPaper(item) &&
                    item.getItemMeta().getDisplayName().equals("§eID: " + name)) {
                player.getInventory().setItem(i, null);
                break; // Solo remover una coincidencia
            }
        }
    }

    /**
     * Crea un papel con el nombre del jugador como identificación.
     */
    public static ItemStack createIdentificationPaper(String name, String prefix) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(prefix + name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Documento de identidad");
        meta.setLore(lore);
        paper.setItemMeta(meta);
        return paper;
    }

    /**
     * Verifica si un item es una identificación
     */
    private boolean isIdentificationPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().startsWith("§eID: ");
    }
}