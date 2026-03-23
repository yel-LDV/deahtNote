package mx.angelDL_.internal.systemRol;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import mx.angelDL_.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maneja el sistema de mensajes anónimos para L
 */
public class AnonymousMessenger {

    private final Main plugin;
    private final RoleAPI roleAPI;
    private final Map<UUID, Long> cooldowns;
    private final long MESSAGE_COOLDOWN = 30000L; // 30 segundos

    public AnonymousMessenger(Main plugin, RoleAPI roleAPI) {
        this.plugin = plugin;
        this.roleAPI = roleAPI;
        this.cooldowns = new HashMap<>();
    }

    /**
     * Envía un mensaje anónimo a todos los jugadores
     */
    public boolean sendAnonymousMessage(Player sender, String message) {
        UUID senderUUID = sender.getUniqueId();

        // VERIFICACIÓN SOLO PARA L - CORREGIDO
        if (!isL(sender)) {
            sender.sendMessage("§cSolo L puede enviar mensajes anónimos.");
            return false;
        }

        // Verificar cooldown
        if (isOnCooldown(senderUUID)) {
            long remaining = getRemainingCooldown(senderUUID);
            sender.sendMessage("§cDebes esperar " + remaining + " segundos antes de enviar otro mensaje anónimo.");
            return false;
        }

        // Verificar que el mensaje no esté vacío
        if (message == null || message.trim().isEmpty()) {
            sender.sendMessage("§cEl mensaje no puede estar vacío.");
            return false;
        }

        // Enviar mensaje anónimo
        broadcastAnonymousMessage(message);

        // Aplicar cooldown
        applyCooldown(senderUUID);

        // Mensaje de confirmación al remitente
        sender.sendMessage("§7[Mensaje anónimo enviado]");

        return true;
    }

    /**
     * Envía un susurro anónimo a un jugador específico
     */
    public boolean sendAnonymousWhisper(Player sender, Player target, String message) {
        UUID senderUUID = sender.getUniqueId();

        // VERIFICACIÓN SOLO PARA L - CORREGIDO
        if (!isL(sender)) {
            sender.sendMessage("§cSolo L puede enviar susurros anónimos.");
            return false;
        }

        // Verificar cooldown
        if (isOnCooldown(senderUUID)) {
            long remaining = getRemainingCooldown(senderUUID);
            sender.sendMessage("§cDebes esperar " + remaining + " segundos antes de enviar otro mensaje anónimo.");
            return false;
        }

        // Verificar que el mensaje no esté vacío
        if (message == null || message.trim().isEmpty()) {
            sender.sendMessage("§cEl mensaje no puede estar vacío.");
            return false;
        }

        // Verificar que el objetivo existe
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cJugador objetivo no encontrado o no está en línea.");
            return false;
        }

        // Enviar susurro anónimo
        sendPrivateAnonymousMessage(target, message);

        // Aplicar cooldown
        applyCooldown(senderUUID);

        // Mensaje de confirmación al remitente
        sender.sendMessage("§7[Susurro anónimo enviado a " + target.getName() + "]");

        return true;
    }

    /**
     * Transmite un mensaje anónimo a todos los jugadores
     */
    private void broadcastAnonymousMessage(String message) {
        String formattedMessage = formatAnonymousMessage(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }

        // Log en consola
        Bukkit.getConsoleSender().sendMessage("§8[L-Anónimo] " + message);
    }

    /**
     * Envía un mensaje anónimo privado a un jugador
     */
    private void sendPrivateAnonymousMessage(Player target, String message) {
        String formattedMessage = formatPrivateAnonymousMessage(message);
        target.sendMessage(formattedMessage);

        // Log en consola
        Bukkit.getConsoleSender().sendMessage("§8[L-Susurro a " + target.getName() + "] " + message);
    }

    /**
     * Formatea el mensaje anónimo público
     */
    private String formatAnonymousMessage(String message) {
        return plugin.getConfig().getString("anonymous_message.format", "§8[§7?§8] §7") + message;
    }

    /**
     * Formatea el mensaje anónimo privado
     */
    private String formatPrivateAnonymousMessage(String message) {
        return plugin.getConfig().getString("anonymous_whisper.format", "§8[§7Susurro Misterioso§8] §7") + message;
    }

    /**
     * Verifica si un jugador es L - MÉTODO MEJORADO
     */
    private boolean isL(Player player) {
        String role = roleAPI.getRole(player.getUniqueId());
        boolean isAlive = roleAPI.isAlive(player.getUniqueId());
        return "l".equalsIgnoreCase(role) && isAlive;
    }

    /**
     * Verifica si un jugador está en cooldown
     */
    private boolean isOnCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return false;

        long lastMessage = cooldowns.get(uuid);
        return System.currentTimeMillis() - lastMessage < MESSAGE_COOLDOWN;
    }

    /**
     * Obtiene el tiempo restante de cooldown en segundos
     */
    private long getRemainingCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return 0;

        long lastMessage = cooldowns.get(uuid);
        long elapsed = System.currentTimeMillis() - lastMessage;
        return Math.max((MESSAGE_COOLDOWN - elapsed) / 1000, 0);
    }

    /**
     * Aplica cooldown a un jugador
     */
    private void applyCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    /**
     * Obtiene el cooldown restante para un jugador (para comandos de debug)
     */
    public long getRemainingCooldownForPlayer(Player player) {
        return getRemainingCooldown(player.getUniqueId());
    }

    /**
     * Limpia cooldowns (para recargas o reinicios)
     */
    public void clearCooldowns() {
        cooldowns.clear();
    }

    /**
     * Verifica si un jugador puede enviar mensajes anónimos (para tab completions)
     */
    public boolean canSendAnonymousMessages(Player player) {
        return isL(player) && !isOnCooldown(player.getUniqueId());
    }
}