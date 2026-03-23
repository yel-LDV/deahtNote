package mx.angelDL_.internal.systemRol;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RoleAPI {

    private final PlayerCache cache;
    private final String idDisplayPrefix = "§eID: ";
    private final Main plugin;

    public RoleAPI(PlayerCache cache) {
        this.cache = cache;
        this.plugin = Main.getInstance();
    }

    public String getRole(UUID uuid) {
        return cache.getPlayerData(uuid).map(PlayerData::getRole).orElse("UNKNOWN");
    }

    public long countRole(String role) {
        return cache.getAllData().stream()
            .filter(data -> data.getRole().equalsIgnoreCase(role))
            .count();
    }

    public boolean isAlive(UUID uuid) {
        return cache.getPlayerData(uuid).map(PlayerData::isAlive).orElse(false);
    }

    public boolean killPlayer(UUID uuid) {
        if (!isAlive(uuid)) return false;

        Optional<PlayerData> opt = cache.getPlayerData(uuid);
        if (opt.isEmpty()) return false;

        PlayerData data = opt.get();
        data.setAlive(false);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && !player.isDead()) {
            player.setHealth(0.0);
        }
        return true;
    }

    public void giveMissionItem(Player player) {
        if (hasMissionItem(player)) {
            player.sendMessage(plugin.lang().get("role.already_mission"));
            return;
        }
        removeMissionItem(player);
        if (plugin.getMissionManager() != null) {
            ItemStack missionItem = plugin.getMissionManager().getMissionItem();
            if (player.getInventory().getItem(8) == null) {
                player.getInventory().setItem(8, missionItem);
            } else {
                int freeSlot = player.getInventory().firstEmpty();
                if (freeSlot != -1) {
                    player.getInventory().setItem(freeSlot, missionItem);
                } else {
                    player.getWorld().dropItem(player.getLocation(), missionItem);
                    player.sendMessage(plugin.lang().get("role.inv_full_mission"));
                }
            }
            player.sendMessage(plugin.lang().get("role.received_mission_item"));
        }
    }

    public boolean hasMissionItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMissionItem(item)) return true;
        }
        return false;
    }

    public void removeMissionItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMissionItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public boolean isMissionItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String expectedName = plugin.getConfig().getString("mission_system.mission_item.name", "§bMisión de L");
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }

    public boolean setRole(Player player, String role) {
        UUID uuid = player.getUniqueId();
        Optional<PlayerData> opt = cache.getPlayerData(uuid);

        if (opt.isEmpty()) {
            cache.registerPlayer(player, role);
            opt = cache.getPlayerData(uuid);
        }

        if (opt.isEmpty()) {
            player.sendMessage(plugin.lang().get("role.set_error"));
            return false;
        }

        PlayerData data = opt.get();
        String oldRole = data.getRole();

        if (!oldRole.equalsIgnoreCase(role)) {
            data.setRole(role);

            if (oldRole.equalsIgnoreCase("kira") && !role.equalsIgnoreCase("kira")) {
                plugin.getShinigamiDealManager().forceCleanDeal(player);
                player.sendMessage(plugin.lang().get("role.shinigami_revoked"));
                plugin.getRoleSwapManager().removeSwapItems(player);
            }

            if (role.equalsIgnoreCase("kira")) {
                plugin.getRoleSwapManager().giveSwapItemToKira(player);
            }

            if (role.equalsIgnoreCase("l")) {
                giveMissionItem(player);
            }
            if (role.equalsIgnoreCase("kira")) {
                giveDeathNote(player);
                plugin.getShinigamiDealManager().giveActivationItemToKira(player);
            }

            if (oldRole.equalsIgnoreCase("kira") && !role.equalsIgnoreCase("kira")) {
                removeDeathNote(player);
                player.sendMessage(plugin.lang().get("role.death_note_removed"));
            }
            if (oldRole.equalsIgnoreCase("l") && !role.equalsIgnoreCase("l")) {
                removeMissionItem(player);
                player.sendMessage(plugin.lang().get("role.mission_item_removed"));
            }
        }

        player.sendMessage(plugin.lang().get("role.changed", Map.of("old", oldRole, "new", role)));
        return true;
    }

    public void giveDeathNote(Player player) {
        if (hasDeathNote(player)) {
            player.sendMessage(plugin.lang().get("role.already_has_id"));
            return;
        }
        removeDeathNote(player);
        ItemStack deathNote = createDeathNoteItem();
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), deathNote);
            player.sendMessage(plugin.lang().get("role.inv_full_death_note"));
        } else {
            if (player.getInventory().getItem(8) == null) {
                player.getInventory().setItem(8, deathNote);
            } else {
                int freeSlot = player.getInventory().firstEmpty();
                player.getInventory().setItem(freeSlot, deathNote);
            }
        }
        player.sendMessage(plugin.lang().get("role.received_death_note"));
    }

    public boolean hasDeathNote(Player player) {
        return countDeathNotes(player) > 0;
    }

    private int countDeathNotes(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDeathNote(item)) count++;
        }
        return count;
    }

    public void removeDeathNote(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.WRITTEN_BOOK &&
                    item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§4Death Note")) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public ItemStack createDeathNoteItem() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName("§4Death Note");
        meta.setLore(Arrays.asList(
            "§8Libro de la muerte",
            "§7Click derecho para abrir el menú de muerte",
            "§7Necesitas la identificación de tu víctima"
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        book.setItemMeta(meta);
        return book;
    }

    public boolean isDeathNote(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals("§4Death Note");
    }

    public void giveDeathNoteToKira(Player player) {
        if (player != null && player.isOnline()) {
            giveDeathNote(player);
        }
    }

    public boolean removeIdentification(Player owner, String targetNameOwner) {
        UUID ownerId = owner.getUniqueId();
        Optional<PlayerData> opt = cache.getPlayerData(ownerId);
        if (opt.isEmpty()) return false;

        PlayerData data = opt.get();
        boolean hasInCache = data.getIdentifications().contains(targetNameOwner);

        if (!hasInCache) {
            for (String id : data.getIdentifications()) {
                if (id.trim().equals(targetNameOwner.trim())) {
                    hasInCache = true;
                    targetNameOwner = id;
                    break;
                }
            }
        }

        if (!hasInCache) return false;

        return data.removeIdentification(targetNameOwner);
    }

    public boolean addIdentification(Player player, String targetNameOwner) {
        UUID playerId = player.getUniqueId();
        Optional<PlayerData> opt = cache.getPlayerData(playerId);
        if (opt.isEmpty()) return false;

        PlayerData data = opt.get();
        boolean alreadyHas = data.getIdentifications().contains(targetNameOwner);
        if (!alreadyHas) {
            for (String id : data.getIdentifications()) {
                if (id.trim().equals(targetNameOwner.trim())) {
                    alreadyHas = true;
                    break;
                }
            }
        }

        if (alreadyHas) return false;

        return data.addIdentification(targetNameOwner);
    }

    public boolean removePhysicalIdentification(Player player, String targetNameOwner) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals("§eID: " + targetNameOwner)) {
                player.getInventory().setItem(i, null);
                return true;
            }
        }
        return false;
    }

    public boolean addPhysicalIdentification(Player player, String targetNameOwner) {
        ItemStack newId = PlayerData.createIdentificationPaper(targetNameOwner, "§eID: ");
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), newId);
            player.sendMessage(plugin.lang().get("identification.dropped"));
            return false;
        } else {
            player.getInventory().addItem(newId);
            return true;
        }
    }

    public PlayerCache getPlayerCache() {
        return cache;
    }

    public boolean stealIdentification(Player thief, Player target, String idOwnerName) {
        UUID thiefId = thief.getUniqueId();
        UUID targetId = target.getUniqueId();

        Optional<PlayerData> targetDataOpt = cache.getPlayerData(targetId);
        Optional<PlayerData> thiefDataOpt = cache.getPlayerData(thiefId);
        if (targetDataOpt.isEmpty() || thiefDataOpt.isEmpty()) {
            thief.sendMessage(plugin.lang().get("steal.error_no_data"));
            return false;
        }

        PlayerData targetData = targetDataOpt.get();
        PlayerData thiefData = thiefDataOpt.get();

        if (!targetData.getIdentifications().contains(idOwnerName)) {
            thief.sendMessage(plugin.lang().get("steal.target_no_id"));
            return false;
        }

        boolean preventDuplicates = plugin.getConfig().getBoolean("stealing.prevent_duplicate_ids", true);
        if (preventDuplicates && thiefData.getIdentifications().contains(idOwnerName)) {
            thief.sendMessage(plugin.lang().get("steal.already_have", Map.of("player", idOwnerName)));
            return false;
        }

        boolean victimLosesId = plugin.getConfig().getBoolean("stealing.victim_loses_id", true);

        if (victimLosesId) {
            boolean removed = targetData.removeIdentification(idOwnerName);
            if (!removed) {
                thief.sendMessage(plugin.lang().get("steal.victim_lose_fail"));
                return false;
            }
        }

        boolean added = thiefData.addIdentification(idOwnerName);
        if (!added) {
            if (victimLosesId) targetData.addIdentification(idOwnerName);
            thief.sendMessage(plugin.lang().get("steal.thief_add_fail"));
            return false;
        }

        updateInventories(thief, target, idOwnerName, victimLosesId);
        return true;
    }

    private void updateInventories(Player thief, Player target, String idOwnerName, boolean victimLosesId) {
        if (victimLosesId && target.isOnline()) {
            removePhysicalIdentification(target, idOwnerName);
        }
        if (thief.isOnline() && !hasPhysicalIdentification(thief, idOwnerName)) {
            ItemStack stolenId = PlayerData.createIdentificationPaper(idOwnerName, idDisplayPrefix);
            if (thief.getInventory().firstEmpty() == -1) {
                thief.getWorld().dropItem(thief.getLocation(), stolenId);
                thief.sendMessage(plugin.lang().get("identification.stolen_inv_full"));
            } else {
                thief.getInventory().addItem(stolenId);
            }
        }
    }

    private boolean hasPhysicalIdentification(Player player, String idOwnerName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(idDisplayPrefix + idOwnerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasIdentification(UUID playerId, String targetNameOwner) {
        Optional<PlayerData> opt = cache.getPlayerData(playerId);
        if (opt.isEmpty()) return false;
        PlayerData data = opt.get();
        for (String id : data.getIdentifications()) {
            if (id.trim().equals(targetNameOwner.trim())) return true;
        }
        return false;
    }
}