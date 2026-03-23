package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RoleSwapManager {
    private final Main plugin;
    private final Map<UUID, Long> swapCooldowns = new HashMap<>();
    private final Set<UUID> processingSwaps = new HashSet<>();

    public RoleSwapManager(Main plugin) {
        this.plugin = plugin;
    }

    public void giveSwapItemToKira(Player player) {
        if (!plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase("kira")) {
            return;
        }

        if (hasSwapItem(player)) {
            return;
        }

        ItemStack swapItem = createSwapItem();

        // Slot 7 (índice 6)
        if (player.getInventory().getItem(6) == null) {
            player.getInventory().setItem(6, swapItem);
        } else {
            int freeSlot = player.getInventory().firstEmpty();
            if (freeSlot != -1) {
                player.getInventory().setItem(freeSlot, swapItem);
            } else {
                player.getWorld().dropItem(player.getLocation(), swapItem);
                player.sendMessage("§eEl item de intercambio fue dropeado al suelo - inventario lleno");
            }
        }

        player.sendMessage(getMessage("item_received"));
    }

    public boolean executeRoleSwap(Player kira) {
        UUID kiraId = kira.getUniqueId();

        if (hasCooldown(kiraId)) {
            long remaining = getRemainingCooldown(kiraId);
            kira.sendMessage(getMessage("swap_cooldown").replace("{time}", String.valueOf(remaining)));
            return false;
        }

        if (processingSwaps.contains(kiraId)) {
            return false;
        }

        processingSwaps.add(kiraId);

        try {
            Player mikami = findMikami();
            if (mikami == null) {
                kira.sendMessage(getMessage("no_mikami"));
                return false;
            }

            swapCooldowns.put(kiraId, System.currentTimeMillis());
            swapRoles(kira, mikami);
            playSwapEffects(kira, mikami);
            removeSwapItems(kira);

            return true;

        } finally {
            processingSwaps.remove(kiraId);
        }
    }

    private void swapRoles(Player kira, Player mikami) {
        String kiraOldRole = plugin.getRoleApi().getRole(kira.getUniqueId());
        String mikamiOldRole = plugin.getRoleApi().getRole(mikami.getUniqueId());

        plugin.getRoleApi().setRole(kira, "mikami");
        plugin.getRoleApi().setRole(mikami, "kira");

        String successMessage = getMessage("swap_success");

        kira.sendMessage(successMessage
                .replace("{old_role}", kiraOldRole)
                .replace("{new_role}", "mikami"));

        mikami.sendMessage(successMessage
                .replace("{old_role}", mikamiOldRole)
                .replace("{new_role}", "kira"));

        Bukkit.broadcastMessage("§6¡Intercambio de roles! §e" + kira.getName() + " §6y §e" + mikami.getName() + " §6han intercambiado sus roles");
    }

    private Player findMikami() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase("mikami") &&
                    plugin.getRoleApi().isAlive(player.getUniqueId())) {
                return player;
            }
        }
        return null;
    }

    public ItemStack createSwapItem() {
        String materialName = plugin.getConfig().getString("role_swap_system.swap_item.material", "NETHER_STAR");
        Material material = Material.valueOf(materialName.toUpperCase());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(plugin.getConfig().getString("role_swap_system.swap_item.name", "§6Intercambio de Roles"));

        List<String> lore = new ArrayList<>();
        int cooldown = plugin.getConfig().getInt("role_swap_system.cooldown", 300);

        for (String line : plugin.getConfig().getStringList("role_swap_system.swap_item.lore")) {
            lore.add(line.replace("{cooldown}", String.valueOf(cooldown)));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public boolean hasSwapItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSwapItem(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSwapItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String expectedName = plugin.getConfig().getString("role_swap_system.swap_item.name", "§6Intercambio de Roles");
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }

    public void removeSwapItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSwapItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        if (isSwapItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    private void playSwapEffects(Player kira, Player mikami) {
        String particleType = plugin.getConfig().getString("role_swap_system.effects.particles", "PORTAL");
        String soundType = plugin.getConfig().getString("role_swap_system.effects.sound", "ENTITY_ENDERMAN_TELEPORT");

        kira.getWorld().spawnParticle(org.bukkit.Particle.valueOf(particleType), kira.getLocation(), 30, 1, 2, 1, 0.1);
        mikami.getWorld().spawnParticle(org.bukkit.Particle.valueOf(particleType), mikami.getLocation(), 30, 1, 2, 1, 0.1);

        kira.playSound(kira.getLocation(), org.bukkit.Sound.valueOf(soundType), 1.0f, 1.0f);
        mikami.playSound(mikami.getLocation(), org.bukkit.Sound.valueOf(soundType), 1.0f, 1.0f);
    }

    private boolean hasCooldown(UUID playerId) {
        if (!swapCooldowns.containsKey(playerId)) return false;
        long lastUse = swapCooldowns.get(playerId);
        return System.currentTimeMillis() - lastUse < (plugin.getConfig().getLong("role_swap_system.cooldown", 300) * 1000L);
    }

    private long getRemainingCooldown(UUID playerId) {
        if (!swapCooldowns.containsKey(playerId)) return 0;
        long lastUse = swapCooldowns.get(playerId);
        return Math.max(0, plugin.getConfig().getLong("role_swap_system.cooldown", 300) - ((System.currentTimeMillis() - lastUse) / 1000));
    }

    private String getMessage(String key) {
        return plugin.getConfig().getString("role_swap_system.messages." + key, "§cMensaje no configurado: " + key);
    }
}