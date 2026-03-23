package mx.angelDL_.internal.events;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

public class PlayersJoinEvents implements Listener {
    private Main plugin;
    private String permisionStaff;
    private int minPlayer;
    private int maxPlayer;

    public PlayersJoinEvents(Main plugin) {
        this.plugin = plugin;
        this.minPlayer = plugin.getConfig().getInt("play.min-players");
        this.maxPlayer = plugin.getConfig().getInt("play.max-players");
        this.permisionStaff = plugin.getConfig().getString("play.permision", "deathnote.staff");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        cleanPlayerOnJoin(p);

        String defaultRole = "Investigador";
        if (!plugin.getPlayerCache().isRegistered(p.getUniqueId())) {
            plugin.getPlayerCache().registerPlayer(p, defaultRole);
        }

        checkAndFixDeathNote(p);
        checkAndGiveMissionItem(p);

        e.setJoinMessage(plugin.lang().get("join.message", Map.of(
            "player", p.getName(),
            "min", String.valueOf(minPlayer)
        )));

        plugin.addPlayerOnline(1);

        if (plugin.getPlayersOnline() >= minPlayer) {
            Bukkit.broadcastMessage(plugin.lang().get("join.game_can_start"));
        }

        String rolActual = plugin.getRoleApi().getRole(p.getUniqueId());
        plugin.getLogger().info("Player " + p.getName() + " joined with role: " + rolActual);

        if (plugin.getRoleApi().getRole(p.getUniqueId()).equalsIgnoreCase("kira")) {
            plugin.getShinigamiDealManager().giveActivationItemToKira(p);
        }
    }

    private void cleanPlayerOnJoin(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setItemOnCursor(null);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        plugin.getShinigamiDealManager().forceCleanDeal(player);
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
        player.setLevel(0);
        player.setExp(0.0f);
    }

    private void checkAndGiveMissionItem(Player player) {
        String playerRole = plugin.getRoleApi().getRole(player.getUniqueId());
        if (playerRole.equalsIgnoreCase("l")) {
            giveMissionItemToL(player);
        } else {
            removeMissionItemFromPlayer(player);
        }
    }

    private void giveMissionItemToL(Player player) {
        removeMissionItemFromPlayer(player);
        if (hasMissionItem(player)) return;
        if (plugin.getMissionManager() != null) {
            ItemStack missionItem = plugin.getMissionManager().getMissionItem();
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), missionItem);
                player.sendMessage(plugin.lang().get("join.mission_item_dropped"));
            } else {
                player.getInventory().addItem(missionItem);
            }
            player.sendMessage(plugin.lang().get("role.received_mission_item"));
        }
    }

    private void removeMissionItemFromPlayer(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMissionItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        if (isMissionItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    private boolean hasMissionItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMissionItem(item)) return true;
        }
        return false;
    }

    private boolean isMissionItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String expectedName = plugin.getConfig().getString("mission_system.mission_item.name", "§bMisión de L");
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }

    private void checkAndFixDeathNote(Player player) {
        String playerRole = plugin.getRoleApi().getRole(player.getUniqueId());
        if (playerRole.equalsIgnoreCase("kira")) {
            giveDeathNoteToKira(player);
        } else {
            removeDeathNoteFromPlayer(player);
        }
    }

    private void giveDeathNoteToKira(Player player) {
        removeDeathNoteFromPlayer(player);
        if (hasDeathNote(player)) {
            moveDeathNoteToSlot9(player);
            return;
        }
        ItemStack deathNote = createDeathNoteItem();
        if (player.getInventory().getItem(8) == null) {
            player.getInventory().setItem(8, deathNote);
        } else {
            int freeSlot = player.getInventory().firstEmpty();
            if (freeSlot != -1) {
                player.getInventory().setItem(freeSlot, deathNote);
            } else {
                player.getWorld().dropItem(player.getLocation(), deathNote);
                player.sendMessage(plugin.lang().get("join.id_dropped"));
            }
        }
        player.sendMessage(plugin.lang().get("role.received_death_note"));
    }

    private void removeDeathNoteFromPlayer(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDeathNote(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        if (isDeathNote(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    private void moveDeathNoteToSlot9(Player player) {
        ItemStack deathNote = null;
        int currentSlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDeathNote(item)) {
                deathNote = item;
                currentSlot = i;
                break;
            }
        }
        if (deathNote != null && currentSlot != 8) {
            ItemStack slot9Item = player.getInventory().getItem(8);
            player.getInventory().setItem(8, deathNote);
            if (currentSlot != -1) {
                player.getInventory().setItem(currentSlot, slot9Item);
            }
        }
    }

    private boolean hasDeathNote(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDeathNote(item)) return true;
        }
        return false;
    }

    private boolean isDeathNote(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals("§4Death Note");
    }

    private ItemStack createDeathNoteItem() {
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

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        cleanPlayerOnRespawn(player);

        plugin.getPlayerCache().getPlayerData(player.getUniqueId()).ifPresent(data -> {
            data.giveDefaultIdentification();
            player.sendMessage(plugin.lang().get("identification.default_received"));
        });

        String playerRole = plugin.getRoleApi().getRole(player.getUniqueId());
        if (playerRole.equalsIgnoreCase("kira")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> giveDeathNoteToKira(player), 5L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getShinigamiDealManager().giveActivationItemToKira(player), 5L);
        }
        if (playerRole.equalsIgnoreCase("l")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> giveMissionItemToL(player), 5L);
        }
    }

    private void cleanPlayerOnRespawn(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }
}