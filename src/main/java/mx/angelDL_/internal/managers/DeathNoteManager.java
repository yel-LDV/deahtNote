package mx.angelDL_.internal.managers;

import mx.angelDL_.internal.config.ConfigManager;
import mx.angelDL_.Main;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DeathNoteManager {
    private final Main plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Long> deathNoteCooldowns = new HashMap<>();
    private final Set<UUID> processingDeaths = new HashSet<>(); // BANDERA PARA EVITAR BUCLE

    public DeathNoteManager(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void openDeathNoteMenu(Player player) {
        // Verificar cooldown
        if (hasCooldown(player)) {
            long remaining = getRemainingCooldown(player);
            player.sendMessage(
                    configManager.getMessage("death_note.kill_cooldown").replace("{time}", String.valueOf(remaining)));
            return;
        }

        // Sonido al abrir
        player.playSound(player.getLocation(), configManager.getKiraOpenMenuSound(), 1.0f, 1.0f);

        // Crear inventario
        Inventory menu = Bukkit.createInventory(player, 54, "Death Note");

        // Agregar cabezas de todos los jugadores
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId()))
                continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(target);

            // Determinar qué rol mostrar
            String targetRole = plugin.getRoleApi().getRole(target.getUniqueId());
            String displayRole = hasIdentification(player, target.getName())
                    ? configManager.getRoleDisplayName(targetRole)
                    : configManager.getMessage("roles.unknown");

            meta.setDisplayName("§c" + target.getName());
            meta.setLore(Arrays.asList(
                    "§8Click derecho para matar"));
            skull.setItemMeta(meta);
            menu.addItem(skull);
        }

        player.openInventory(menu);
    }

    public void killPlayer(Player kira, Player target) {
        // Verificar identificación
        if (!hasIdentification(kira, target.getName())) {
            kira.sendMessage(
                    configManager.getMessage("death_note.no_identification").replace("{player}", target.getName()));
            return;
        }

        // VERIFICAR SI EL JUGADOR ESTÁ VIVO - CORRECCIÓN CLAVE
        if (!plugin.getRoleApi().isAlive(target.getUniqueId())) {
            kira.sendMessage("§cEl jugador " + target.getName() + " ya está muerto");
            return;
        }

        // VERIFICAR SI YA ESTÁ MUERTO EN EL JUEGO
        if (target.isDead()) {
            kira.sendMessage("§cEl jugador " + target.getName() + " ya está muerto");
            return;
        }

        // Aplicar cooldown
        deathNoteCooldowns.put(kira.getUniqueId(), System.currentTimeMillis());
        int timingKill = plugin.getConfig().getInt("roles.kira.kill_timing", 40);

        // MARCAR como muerte en proceso
        processingDeaths.add(target.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // DOBLE VERIFICACIÓN - asegurar que aún esté vivo (en el sistema)
                    if (!plugin.getRoleApi().isAlive(target.getUniqueId())) {
                        kira.sendMessage("§cNo se pudo matar a " + target.getName() + " - ya está muerto");
                        return;
                    }

                    // Cambiar estado a muerto en el cache PRIMERO
                    plugin.getPlayerCache().getPlayerData(target.getUniqueId()).ifPresent(data -> {
                        data.setAlive(false);
                    });

                    // Si está online, matarlo y efectos
                    if (target.isOnline() && !target.isDead()) {
                        target.setHealth(0.0);
                        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 30, 0.5, 1, 0.5, 0.1);

                        // Efectos para Kira
                        kira.playSound(kira.getLocation(), configManager.getKiraKillSound(), 1.0f, 1.0f);
                        kira.sendMessage(configManager.getMessage("death_note.kill_success").replace("{player}",
                                target.getName()));
                    } else {
                        kira.sendMessage("§e" + target.getName() + " morirá al entrar al servidor.");
                    }

                } finally {
                    // Limpiar la bandera después de un breve momento
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        processingDeaths.remove(target.getUniqueId());
                    }, 10L); // 10 ticks después
                }
            }
        }, (timingKill * 20L));
    }

    // Método para verificar si una muerte está siendo procesada
    public boolean isProcessingDeath(UUID playerId) {
        return processingDeaths.contains(playerId);
    }

    private boolean hasIdentification(Player player, String targetName) {
        // Verificar si el jugador tiene la ID del objetivo
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().equals("§eID: " + targetName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCooldown(Player player) {
        if (!deathNoteCooldowns.containsKey(player.getUniqueId()))
            return false;
        long lastUse = deathNoteCooldowns.get(player.getUniqueId());
        return System.currentTimeMillis() - lastUse < (configManager.getDeathNoteCooldown() * 1000L);
    }

    private long getRemainingCooldown(Player player) {
        if (!deathNoteCooldowns.containsKey(player.getUniqueId()))
            return 0;
        long lastUse = deathNoteCooldowns.get(player.getUniqueId());
        return Math.max(0, configManager.getDeathNoteCooldown() - ((System.currentTimeMillis() - lastUse) / 1000));
    }

    public ItemStack createDeathNoteItem() {
        ItemStack book = new ItemStack(configManager.getDeathNoteItem());
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName("§4Death Note");
        meta.setLore(Arrays.asList(
                "§8Libro de la muerte",
                "§7Click derecho para abrir"));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        book.setItemMeta(meta);
        return book;
    }
}