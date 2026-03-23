package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import mx.angelDL_.internal.systemRol.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class VictoryConditionManager {
    private final Main plugin;
    private final ConfigManager configManager;
    private boolean gameEnded = false;
    private final Set<UUID> processingDeaths = new HashSet<>();

    public VictoryConditionManager(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void checkVictoryOnDeath(PlayerDeathEvent event) {
        if (gameEnded) return;

        Player deadPlayer = event.getEntity();
        UUID deadPlayerId = deadPlayer.getUniqueId();

        if (processingDeaths.contains(deadPlayerId)) return;
        processingDeaths.add(deadPlayerId);

        try {
            Player killer = deadPlayer.getKiller();
            String deadRole = plugin.getRoleApi().getRole(deadPlayerId);

            handleRoleConversionsAndChains(deadPlayer, deadRole);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (killer != null && plugin.getRoleApi().isAlive(killer.getUniqueId())) {
                        checkKillerVictory(killer, deadPlayer, deadRole);
                    }
                    checkGeneralVictoryConditions();
                    checkLastManStanding();
                } finally {
                    processingDeaths.remove(deadPlayerId);
                }
            }, 2L);

        } catch (Exception e) {
            processingDeaths.remove(deadPlayerId);
            plugin.getLogger().warning("Error in checkVictoryOnDeath: " + e.getMessage());
        }
    }

    private void handleRoleConversionsAndChains(Player deadPlayer, String deadRole) {
        boolean chainEnabled = configManager.config.getBoolean("death_chain.mello_near_link", true);

        switch (deadRole.toLowerCase()) {
            case "l":
                handleLDeath(deadPlayer.getUniqueId());
                break;
            case "near":
                if (chainEnabled) handleNearDeath(deadPlayer.getUniqueId());
                break;
            case "mello":
                if (chainEnabled) handleMelloDeath(deadPlayer.getUniqueId());
                break;
            case "kira":
                handleKiraDeath(deadPlayer);
                break;
        }
    }

    private void handleLDeath(UUID lUUID) {
        Player lPlayer = Bukkit.getPlayer(lUUID);
        if (lPlayer != null) {
            lPlayer.sendMessage(plugin.lang().get("succession.l_death_to_l"));
        }

        Player nearPlayer = findPlayerWithRole("near");
        if (nearPlayer != null && plugin.getRoleApi().isAlive(nearPlayer.getUniqueId())) {
            plugin.getRoleConversionManager().convertNearToL(nearPlayer);
            nearPlayer.sendMessage(plugin.lang().get("succession.l_death_to_near"));
            Bukkit.broadcastMessage(plugin.lang().get("succession.l_death_broadcast",
                Map.of("player", nearPlayer.getName())));
        } else {
            Bukkit.broadcastMessage(plugin.lang().get("succession.l_death_no_successor"));
        }
    }

    private void handleNearDeath(UUID nearUUID) {
        Player nearPlayer = Bukkit.getPlayer(nearUUID);
        if (nearPlayer != null) {
            nearPlayer.sendMessage(plugin.lang().get("succession.near_death_to_near"));
        }

        Player melloPlayer = findPlayerWithRole("mello");
        if (melloPlayer != null && plugin.getRoleApi().isAlive(melloPlayer.getUniqueId()) &&
                !processingDeaths.contains(melloPlayer.getUniqueId())) {
            processingDeaths.add(melloPlayer.getUniqueId());
            plugin.getRoleApi().killPlayer(melloPlayer.getUniqueId());
            melloPlayer.sendMessage(plugin.lang().get("succession.near_death_to_mello"));
            Bukkit.broadcastMessage(plugin.lang().get("succession.near_death_broadcast"));
        }
    }

    private void handleMelloDeath(UUID melloUUID) {
        Player melloPlayer = Bukkit.getPlayer(melloUUID);
        if (melloPlayer != null) {
            melloPlayer.sendMessage(plugin.lang().get("succession.mello_death_to_mello"));
        }

        Player nearPlayer = findPlayerWithRole("near");
        if (nearPlayer != null && plugin.getRoleApi().isAlive(nearPlayer.getUniqueId()) &&
                !processingDeaths.contains(nearPlayer.getUniqueId())) {
            processingDeaths.add(nearPlayer.getUniqueId());
            plugin.getRoleApi().killPlayer(nearPlayer.getUniqueId());
            nearPlayer.sendMessage(plugin.lang().get("succession.mello_death_to_near"));
            Bukkit.broadcastMessage(plugin.lang().get("succession.mello_death_broadcast"));
        }
    }

    private void handleKiraDeath(Player kiraPlayer) {
        kiraPlayer.sendMessage(plugin.lang().get("succession.kira_death_to_kira"));
        Bukkit.broadcastMessage(plugin.lang().get("succession.kira_death_broadcast"));
    }

    private void checkKillerVictory(Player killer, Player victim, String victimRole) {
        if (gameEnded) return;
        String killerRole = plugin.getRoleApi().getRole(killer.getUniqueId());

        if (killerRole.equalsIgnoreCase("mello") &&
                (victimRole.equalsIgnoreCase("kira") || victimRole.equalsIgnoreCase("l"))) {
            declareVictory("mello", killer);
            return;
        }

        if (killerRole.equalsIgnoreCase("l") && victimRole.equalsIgnoreCase("kira")) {
            declareVictory("l", killer);
            return;
        }

        if (killerRole.equalsIgnoreCase("investigator") && victimRole.equalsIgnoreCase("kira")) {
            declareVictory("investigator", killer);
        }
    }

    private void checkGeneralVictoryConditions() {
        if (gameEnded) return;

        if (!isAnyLAlive() && !isAnyNearAlive()) {
            Player kira = findPlayerWithRole("kira");
            if (kira != null && plugin.getRoleApi().isAlive(kira.getUniqueId())) {
                declareVictory("kira", kira);
                return;
            }
        }

        if (isKiraDead()) {
            Player l = findPlayerWithRole("l");
            if (l != null && plugin.getRoleApi().isAlive(l.getUniqueId())) {
                declareVictory("l", l);
            }
        }
    }

    private void checkLastManStanding() {
        if (gameEnded) return;

        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().isAlive(player.getUniqueId())) {
                alivePlayers.add(player);
            }
        }

        if (alivePlayers.size() == 1) {
            Player lastPlayer = alivePlayers.get(0);
            String role = plugin.getRoleApi().getRole(lastPlayer.getUniqueId());
            declareVictory(role, lastPlayer);
        }
    }

    private void declareVictory(String winnerRole, Player winner) {
        if (gameEnded) return;
        gameEnded = true;

        String victoryMessage = plugin.lang().get("victory." + winnerRole.toLowerCase());
        Bukkit.broadcastMessage(plugin.lang().get("victory.header"));
        Bukkit.broadcastMessage(victoryMessage);
        Bukkit.broadcastMessage(plugin.lang().get("victory.winner",
            Map.of("player", winner.getName(), "role", winnerRole)));
        Bukkit.broadcastMessage(plugin.lang().get("victory.footer"));
    }

    private boolean isAnyLAlive() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase("l") &&
                    plugin.getRoleApi().isAlive(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnyNearAlive() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase("near") &&
                    plugin.getRoleApi().isAlive(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isKiraDead() {
        return isRoleDead("kira");
    }

    private boolean isRoleDead(String role) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase(role) &&
                    plugin.getRoleApi().isAlive(player.getUniqueId())) {
                return false;
            }
        }
        return true;
    }

    private Player findPlayerWithRole(String role) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase(role) &&
                    plugin.getRoleApi().isAlive(player.getUniqueId())) {
                return player;
            }
        }
        return null;
    }

    public void resetGame() {
        gameEnded = false;
        processingDeaths.clear();
    }

    public void handleWatariGiveID(Player watari, Player l) {
        String watariRole = plugin.getRoleApi().getRole(watari.getUniqueId());
        String lRole = plugin.getRoleApi().getRole(l.getUniqueId());

        if (!watariRole.equalsIgnoreCase("watari") || !lRole.equalsIgnoreCase("l")) {
            watari.sendMessage(plugin.lang().get("watari.not_watari_or_l"));
            return;
        }

        ItemStack idToGive = getIdentificationPaperExceptOwn(watari);
        if (idToGive == null) {
            watari.sendMessage(plugin.lang().get("watari.no_ids_to_give"));
            return;
        }

        String ownerName = getIdentificationOwnerName(idToGive);
        if (ownerName == null || ownerName.trim().isEmpty()) {
            watari.sendMessage(plugin.lang().get("watari.cannot_get_owner"));
            return;
        }

        ownerName = ownerName.trim();
        boolean transferSuccess = transferIdentification(watari, l, ownerName);

        if (!transferSuccess) {
            watari.sendMessage(plugin.lang().get("watari.transfer_failed"));
            return;
        }

        watari.sendMessage(plugin.lang().get("watari.give_success", Map.of("player", ownerName)));
        l.sendMessage(plugin.lang().get("watari.l_receive", Map.of("player", ownerName)));
    }

    private boolean transferIdentification(Player from, Player to, String ownerName) {
        Optional<PlayerData> fromDataOpt = plugin.getRoleApi().getPlayerCache().getPlayerData(from.getUniqueId());
        Optional<PlayerData> toDataOpt = plugin.getRoleApi().getPlayerCache().getPlayerData(to.getUniqueId());

        if (fromDataOpt.isEmpty() || toDataOpt.isEmpty()) return false;

        PlayerData fromData = fromDataOpt.get();
        PlayerData toData = toDataOpt.get();

        boolean fromHasId = false;
        String exactIdName = ownerName;

        for (String id : fromData.getIdentifications()) {
            if (id.trim().equals(ownerName)) {
                fromHasId = true;
                exactIdName = id;
                break;
            }
        }

        if (!fromHasId) return false;

        for (String id : toData.getIdentifications()) {
            if (id.trim().equals(ownerName)) return false;
        }

        boolean removedFromCache = fromData.removeIdentification(exactIdName);
        if (!removedFromCache) return false;

        plugin.getRoleApi().removePhysicalIdentification(from, ownerName);

        boolean addedToCache = toData.addIdentification(ownerName);
        if (!addedToCache) {
            fromData.addIdentification(exactIdName);
            return false;
        }

        ItemStack modifiedId = createLInspectedIdentification(ownerName);
        boolean addedPhysical = to.getInventory().addItem(modifiedId).isEmpty();

        if (!addedPhysical) {
            to.getWorld().dropItem(to.getLocation(), modifiedId);
            to.sendMessage(plugin.lang().get("identification.dropped"));
        }

        return true;
    }

    private ItemStack getIdentificationPaperExceptOwn(Player player) {
        String playerName = player.getName().trim();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isIdentificationPaper(item)) {
                String ownerName = getIdentificationOwnerName(item);
                if (ownerName != null && !ownerName.trim().equalsIgnoreCase(playerName)) {
                    return item.clone();
                }
            }
        }
        return null;
    }

    private ItemStack createLInspectedIdentification(String playerName) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();

        Player targetPlayer = Bukkit.getPlayerExact(playerName);
        String targetRole = plugin.lang().get("role_names.unknown");

        if (targetPlayer != null && targetPlayer.isOnline()) {
            String roleName = plugin.getRoleApi().getRole(targetPlayer.getUniqueId());
            targetRole = plugin.lang().get("role_names." + roleName.toLowerCase());
        }

        meta.setDisplayName("§eID: " + playerName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Documento de identidad");
        lore.add("§8-------------------");
        lore.add("§eRol: §6" + targetRole);
        lore.add("§7Inspeccionado por L");
        lore.add("§7Entregado por Watari");
        meta.setLore(lore);
        paper.setItemMeta(meta);
        return paper;
    }

    private String getIdentificationOwnerName(ItemStack identification) {
        if (identification == null || !identification.hasItemMeta()) return null;
        ItemMeta meta = identification.getItemMeta();
        if (!meta.hasDisplayName()) return null;
        String displayName = meta.getDisplayName();
        if (displayName.startsWith("§eID: ")) {
            return displayName.substring(5);
        }
        return null;
    }

    public void inspectIdentificationAsL(Player l, ItemStack identification) {
        String lRole = plugin.getRoleApi().getRole(l.getUniqueId());
        if (!lRole.equalsIgnoreCase("l")) {
            l.sendMessage(plugin.lang().get("watari.l_inspect"));
            return;
        }

        if (!isIdentificationPaper(identification)) {
            l.sendMessage(plugin.lang().get("watari.not_id"));
            return;
        }

        String ownerName = getIdentificationOwnerName(identification);
        if (ownerName == null || ownerName.trim().isEmpty()) {
            l.sendMessage(plugin.lang().get("watari.cannot_get_owner"));
            return;
        }

        ownerName = ownerName.trim();
        String finalOwnerName = ownerName;
        boolean hasInCache = plugin.getRoleApi().getPlayerCache().getPlayerData(l.getUniqueId())
            .map(data -> {
                for (String id : data.getIdentifications()) {
                    if (id.trim().equals(finalOwnerName)) return true;
                }
                return false;
            })
            .orElse(false);

        if (!hasInCache) {
            l.sendMessage(plugin.lang().get("watari.no_cache"));
            return;
        }

        ItemStack inspectedId = createLInspectedIdentification(ownerName);
        replaceIdentificationInInventory(l, identification, inspectedId);
        l.sendMessage(plugin.lang().get("watari.inspected", Map.of("player", ownerName)));
    }

    private void replaceIdentificationInInventory(Player player, ItemStack oldItem, ItemStack newItem) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.isSimilar(oldItem)) {
                player.getInventory().setItem(i, newItem);
                break;
            }
        }
    }

    public boolean isIdentificationPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().startsWith("§eID: ");
    }
}