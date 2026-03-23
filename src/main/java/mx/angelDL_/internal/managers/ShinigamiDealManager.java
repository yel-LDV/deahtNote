package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ShinigamiDealManager {
    private final Main plugin;
    private final Set<UUID> playersWithDeal = new HashSet<>();
    private final Map<UUID, Double> originalHealth = new HashMap<>();

    public ShinigamiDealManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Activar el pacto Shinigami para un jugador
     */
    public boolean activateDeal(Player player) {
        UUID playerId = player.getUniqueId();

        // Verificar que sea Kira
        if (!plugin.getRoleApi().getRole(playerId).equalsIgnoreCase("kira")) {
            player.sendMessage("§cSolo Kira puede hacer el pacto Shinigami");
            return false;
        }

        // Verificar que no tenga ya el pacto
        if (hasShinigamiDeal(playerId)) {
            player.sendMessage("§cYa has hecho el pacto Shinigami");
            return false;
        }

        // Aplicar efectos del pacto
        applyDealEffects(player);

        // Guardar estado
        playersWithDeal.add(playerId);
        originalHealth.put(playerId, player.getMaxHealth());

        // Efectos visuales y de sonido
        playActivationEffects(player);

        // Cambiar el item a versión usada
        replaceWithUsedItem(player);

        player.sendMessage("§4⚡ ¡HAS HECHO EL PACTO SHINIGAMI!");
        player.sendMessage("§cHas perdido la mitad de tu vida permanentemente");
        player.sendMessage("§aAhora puedes robar identificaciones más rápido y ver la vida de otros");

        plugin.getLogger().info("Shinigami Deal activado para: " + player.getName());
        return true;
    }

    /**
     * Revertir el pacto Shinigami para un jugador
     */
    public boolean revertDeal(Player player) {
        UUID playerId = player.getUniqueId();

        // Verificar que tenga el pacto activo
        if (!hasShinigamiDeal(playerId)) {
            return false; // No tiene pacto activo
        }

        // Revertir efectos del pacto
        revertDealEffects(player);

        // Remover estado
        playersWithDeal.remove(playerId);
        originalHealth.remove(playerId);

        // Remover items del pacto del inventario
        removeShinigamiItems(player);

        player.sendMessage("§aEl pacto Shinigami ha sido revocado");
        plugin.getLogger().info("Shinigami Deal revertido para: " + player.getName());
        return true;
    }

    /**
     * Forzar limpieza completa del pacto (para cambio de rol)
     */
    public void forceCleanDeal(Player player) {
        UUID playerId = player.getUniqueId();

        if (hasShinigamiDeal(playerId)) {
            // Si tiene pacto activo, revertirlo
            revertDeal(player);
        } else {
            // Si no tiene pacto, solo limpiar items
            removeShinigamiItems(player);
        }
    }

    /**
     * Verificar y limpiar estado del pacto al unirse
     */
    public void cleanDealStateOnJoin(Player player) {
        UUID playerId = player.getUniqueId();

        // Si el jugador no es Kira, limpiar cualquier estado residual del pacto
        if (!plugin.getRoleApi().getRole(playerId).equalsIgnoreCase("kira")) {
            playersWithDeal.remove(playerId);
            originalHealth.remove(playerId);
            removeShinigamiItems(player);
        } else {
            // Si es Kira pero no tiene el pacto, limpiar items usados
            if (!hasShinigamiDeal(playerId)) {
                removeUsedItems(player);
            }
        }
    }

    /**
     * Cambiar item de activación por item usado
     */
    private void replaceWithUsedItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isActivationItem(item)) {
                player.getInventory().setItem(i, createUsedItem());
                break;
            }
        }
    }

    /**
     * Aplicar efectos del pacto al jugador
     */
    private void applyDealEffects(Player player) {
        // Reducir vida máxima a la mitad
        double newMaxHealth = 10.0; // 5 corazones
        player.setMaxHealth(newMaxHealth);

        // Si tiene más vida que el nuevo máximo, ajustar
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }

        // Efecto visual temporal
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 1));
    }

    /**
     * Revertir efectos del pacto al jugador
     */
    private void revertDealEffects(Player player) {
        // Restaurar vida máxima a 20 corazones (valor normal)
        player.setMaxHealth(20.0);

        // Si la vida actual es menor que 20, mantenerla; si es mayor, ajustar a 20
        if (player.getHealth() > 20.0) {
            player.setHealth(20.0);
        }

        // Remover efecto de brillo si existe
        player.removePotionEffect(PotionEffectType.GLOWING);
    }

    /**
     * Reproducir efectos de activación
     */
    private void playActivationEffects(Player player) {
        Location loc = player.getLocation();

        // Partículas
        player.getWorld().spawnParticle(
                Particle.valueOf(plugin.getConfig().getString("shinigami_eye_deal.effects.activation_particles", "SOUL_FIRE_FLAME")),
                loc, 50, 1, 2, 1, 0.1
        );

        // Sonido
        player.playSound(
                loc,
                Sound.valueOf(plugin.getConfig().getString("shinigami_eye_deal.effects.activation_sound", "ENTITY_WITHER_SPAWN")),
                1.0f, 0.8f
        );
    }

    /**
     * Verificar si un jugador tiene el pacto
     */
    public boolean hasShinigamiDeal(UUID playerId) {
        return playersWithDeal.contains(playerId);
    }

    /**
     * Obtener cooldown de robo en segundos (configurable)
     */
    public long getStealCooldown(UUID playerId) {
        if (hasShinigamiDeal(playerId)) {
            // Cooldown rápido con pacto
            return plugin.getConfig().getLong("shinigami_eye_deal.benefits.steal_cooldown", 15);
        }
        // Cooldown normal - usar stealing.cooldown en lugar de cooldowns.steal_identification
        return plugin.getConfig().getLong("stealing.cooldown", 120);
    }

    /**
     * Obtener cooldown de robo en segundos (para mensajes)
     */
    public long getStealCooldownSeconds(UUID playerId) {
        if (hasShinigamiDeal(playerId)) {
            return plugin.getConfig().getLong("shinigami_eye_deal.benefits.steal_cooldown", 15);
        }
        return plugin.getConfig().getLong("stealing.cooldown", 120);
    }

    /**
     * Obtener texto de vida para mostrar
     */
    public String getHealthDisplay(Player target) {
        if (!plugin.getConfig().getBoolean("shinigami_eye_deal.benefits.show_health_display", true)) {
            return "";
        }

        double health = target.getHealth();
        double maxHealth = target.getMaxHealth();
        int hearts = (int) Math.ceil(health / 2);
        int maxHearts = (int) Math.ceil(maxHealth / 2);

        String color = getHealthColor(health, maxHealth);
        String heartSymbol = plugin.getConfig().getString("shinigami_eye_deal.effects.health_display_color", "§c❤");

        return String.format("%s%s§7/%s%s", color, hearts, color, maxHearts);
    }

    /**
     * Obtener color basado en la vida
     */
    private String getHealthColor(double health, double maxHealth) {
        double percentage = health / maxHealth;

        if (percentage >= 0.7) return "§a"; // Verde
        if (percentage >= 0.4) return "§e"; // Amarillo
        if (percentage >= 0.2) return "§6"; // Naranja
        return "§c"; // Rojo
    }

    /**
     * Crear item de activación del pacto (ROJO)
     */
    public ItemStack createActivationItem() {
        String materialName = plugin.getConfig().getString("shinigami_eye_deal.activation_item.material", "RED_DYE");
        Material material = Material.valueOf(materialName.toUpperCase());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Nombre
        meta.setDisplayName(plugin.getConfig().getString("shinigami_eye_deal.activation_item.name", "§4Sangre del Shinigami"));

        // Lore
        List<String> lore = new ArrayList<>();
        int cooldown = plugin.getConfig().getInt("shinigami_eye_deal.benefits.steal_cooldown", 15);

        for (String line : plugin.getConfig().getStringList("shinigami_eye_deal.activation_item.lore")) {
            lore.add(line.replace("{cooldown}", String.valueOf(cooldown)));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Crear item usado del pacto (GRIS)
     */
    public ItemStack createUsedItem() {
        String materialName = plugin.getConfig().getString("shinigami_eye_deal.used_item.material", "GRAY_DYE");
        Material material = Material.valueOf(materialName.toUpperCase());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Nombre
        meta.setDisplayName(plugin.getConfig().getString("shinigami_eye_deal.used_item.name", "§8Pacto Consumido"));

        // Lore
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("shinigami_eye_deal.used_item.lore")) {
            lore.add(line);
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Dar item de activación a Kira automáticamente
     */
    public void giveActivationItemToKira(Player player) {
        if (!plugin.getRoleApi().getRole(player.getUniqueId()).equalsIgnoreCase("kira")) {
            return;
        }

        // Verificar que no tenga ya el pacto activado
        if (hasShinigamiDeal(player.getUniqueId())) {
            giveUsedItem(player); // Dar item usado si ya tiene el pacto
            return;
        }

        // Verificar que no tenga ya el item
        if (hasActivationItem(player) || hasUsedItem(player)) {
            return;
        }

        ItemStack activationItem = createActivationItem();

        // Buscar slot libre
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(activationItem);
        } else {
            // Inventario lleno, dropear al suelo
            player.getWorld().dropItem(player.getLocation(), activationItem);
            player.sendMessage("§eEl item del pacto fue dropeado al suelo - inventario lleno");
        }

        player.sendMessage("§4¡Has recibido el item del pacto Shinigami!");
        player.sendMessage("§7Usa click derecho para activar el pacto (IRREVERSIBLE)");
    }

    /**
     * Dar item usado a jugador que ya tiene el pacto
     */
    private void giveUsedItem(Player player) {
        if (hasUsedItem(player)) {
            return;
        }

        ItemStack usedItem = createUsedItem();

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(usedItem);
        }
    }

    /**
     * Verificar si el jugador tiene el item de activación (ROJO)
     */
    public boolean hasActivationItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isActivationItem(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verificar si el jugador tiene el item usado (GRIS)
     */
    public boolean hasUsedItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isUsedItem(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verificar si un item es el de activación del pacto (ROJO)
     */
    public boolean isActivationItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String expectedName = plugin.getConfig().getString("shinigami_eye_deal.activation_item.name", "§4Sangre del Shinigami");
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }

    /**
     * Verificar si un item es el usado del pacto (GRIS)
     */
    public boolean isUsedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String expectedName = plugin.getConfig().getString("shinigami_eye_deal.used_item.name", "§8Pacto Consumido");
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }

    /**
     * Remover todos los items del pacto Shinigami (activación y usados)
     */
    public void removeShinigamiItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isActivationItem(item) || isUsedItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        // También verificar el cursor
        if (isActivationItem(player.getItemOnCursor()) || isUsedItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }


    /**
     * Remover solo items usados del pacto
     */
    private void removeUsedItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isUsedItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        if (isUsedItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    /**
     * Cargar datos del pacto (para reinicios)
     */
    public void loadPlayerData(UUID playerId, boolean hasDeal) {
        if (hasDeal) {
            playersWithDeal.add(playerId);
        }
    }

    /**
     * Obtener si el jugador tiene pacto (para guardar datos)
     */
    public boolean getPlayerDealState(UUID playerId) {
        return hasShinigamiDeal(playerId);
    }
    /**
     * Obtener tiempo de proximidad para robar (configurable)
     */
    public int getProximityTime(UUID playerId) {
        if (hasShinigamiDeal(playerId)) {
            return plugin.getConfig().getInt("shinigami_eye_deal.benefits.proximity_time", 10);
        }
        return plugin.getConfig().getInt("stealing.proximity_time", 30);
    }
}