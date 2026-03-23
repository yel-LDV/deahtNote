package mx.angelDL_.internal.listeners;

import mx.angelDL_.Main;
import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class StealIdentificationListener implements Listener {
    private final Main plugin;
    private final ConfigManager configManager;

    // Mapa para rastrear robos en progreso: Jugador -> (Víctima, Tiempo inicio)
    private final Map<UUID, StealAttempt> activeSteals = new HashMap<>();

    public StealIdentificationListener(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        // Iniciar tarea para verificar proximidad cada segundo
        startProximityCheckTask();
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player thief = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Verificar si el atacante puede robar (Mikami o Kira)
        String thiefRole = plugin.getRoleApi().getRole(thief.getUniqueId());
        if (!thiefRole.equalsIgnoreCase("mikami") && !thiefRole.equalsIgnoreCase("kira")) {
            return;
        }

        // ✅ USAR COOLDOWN DINÁMICO DEL NUEVO SISTEMA
        if (!plugin.getPlayerCache().canSteal(thief.getUniqueId())) {
            long remaining = plugin.getPlayerCache().getRemainingStealCooldown(thief.getUniqueId());
            String cooldownMessage = configManager.getMessage("steal.cooldown")
                    .replace("{time}", String.valueOf(remaining));
            thief.sendMessage(cooldownMessage);
            return;
        }

        // Verificar que es un golpe por la espalda
        if (!isBackstab(thief, victim)) {
            thief.sendMessage(configManager.getMessage("steal.backstab_required"));
            return;
        }

        // Verificar que la víctima tenga su identificación
        if (!hasOwnIdentification(victim)) {
            thief.sendMessage(configManager.getMessage("steal.no_identification"));
            return;
        }

        // Verificar si ya tiene la identificación
        if (configManager.config.getBoolean("stealing.prevent_duplicate_ids", true) &&
                hasIdentification(thief, victim.getName())) {
            thief.sendMessage(configManager.getMessage("steal.already_have_id").replace("{player}", victim.getName()));
            return;
        }

        // Verificar si requiere proximidad
        if (configManager.config.getBoolean("stealing.require_proximity", true)) {
            startProximitySteal(thief, victim);
            event.setCancelled(true); // Cancelar daño durante el robo
        } else {
            // Robo instantáneo (comportamiento anterior)
            executeSteal(thief, victim);
        }
    }

    private void startProximitySteal(Player thief, Player victim) {
        UUID thiefId = thief.getUniqueId();
        UUID victimId = victim.getUniqueId();

        // Cancelar robo anterior si existe
        if (activeSteals.containsKey(thiefId)) {
            activeSteals.remove(thiefId);
        }

        // ✅ CAMBIAR: Usar tiempo dinámico del pacto Shinigami
        int proximityTime = plugin.getShinigamiDealManager().getProximityTime(thief.getUniqueId());

        StealAttempt attempt = new StealAttempt(victimId, System.currentTimeMillis(), proximityTime);
        activeSteals.put(thiefId, attempt);

        thief.sendMessage(configManager.getMessage("steal.proximity_started")
                .replace("{player}", victim.getName())
                .replace("{time}", String.valueOf(proximityTime))); // ← Ahora será 10s con pacto
    }

    /**
     * Ejecuta el robo final
     */
    private void executeSteal(Player thief, Player victim) {
        // Intentar robar la identificación
        boolean success = plugin.getRoleApi().stealIdentification(thief, victim, victim.getName());

        if (success) {
            // ✅ USAR COOLDOWN DINÁMICO CON PACTO SHINIGAMI
            long stealCooldown = plugin.getShinigamiDealManager().getStealCooldown(thief.getUniqueId());
            String successMessage = configManager.getMessage("steal.success")
                    .replace("{player}", victim.getName())
                    .replace("{cooldown}", String.valueOf(stealCooldown));

            thief.sendMessage(successMessage);

            // Notificar a la víctima
            if (configManager.config.getBoolean("stealing.victim_loses_id", true)) {
                String thiefRole = plugin.getRoleApi().getRole(thief.getUniqueId());
                String roleDisplay = configManager.getRoleDisplayName(thiefRole.toLowerCase());
                victim.sendMessage(configManager.getMessage("steal.victim_notified").replace("{thief_role}", roleDisplay));
            }

            // Aplicar efectos visuales y sonidos
            plugin.getParticleSystem().spawnStealParticles(victim, thief);
            plugin.getSoundSystem().playStealSound(thief);

            // Registrar cooldown
            plugin.getPlayerCache().registerSteal(thief.getUniqueId());
        }
    }

    /**
     * Verifica si el golpe fue por la espalda
     */
    private boolean isBackstab(Player attacker, Player victim) {
        // Obtener la dirección de la víctima
        Vector victimDirection = victim.getLocation().getDirection();
        // Obtener la dirección del atacante hacia la víctima
        Vector toVictim = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();

        // Calcular el ángulo entre la dirección de la víctima y la dirección del atacante
        double angle = victimDirection.angle(toVictim);

        // Si el ángulo es menor a 90 grados (π/2 radianes), es por la espalda
        return angle < Math.PI / 2;
    }

    /**
     * Verifica si el jugador tiene su propia identificación
     */
    private boolean hasOwnIdentification(Player player) {
        String playerName = player.getName();
        return plugin.getPlayerCache().getPlayerData(player.getUniqueId())
                .map(data -> data.getIdentifications().contains(playerName))
                .orElse(false);
    }

    /**
     * Verifica si el jugador tiene la identificación de otro jugador
     */
    private boolean hasIdentification(Player player, String targetName) {
        return plugin.getPlayerCache().getPlayerData(player.getUniqueId())
                .map(data -> data.getIdentifications().contains(targetName))
                .orElse(false);
    }

    /**
     * Inicia la tarea de verificación de proximidad
     */
    private void startProximityCheckTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, StealAttempt>> iterator = activeSteals.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, StealAttempt> entry = iterator.next();
                UUID thiefId = entry.getKey();
                StealAttempt attempt = entry.getValue();

                Player thief = plugin.getServer().getPlayer(thiefId);
                Player victim = plugin.getServer().getPlayer(attempt.getVictimId());

                // Verificar si los jugadores están online
                if (thief == null || victim == null || !thief.isOnline() || !victim.isOnline()) {
                    iterator.remove();
                    continue;
                }

                // Verificar distancia
                double radius = configManager.config.getDouble("stealing.proximity_radius", 30.0);
                double distance = thief.getLocation().distance(victim.getLocation());

                if (distance > radius) {
                    // Jugador se alejó demasiado
                    thief.sendMessage(configManager.getMessage("steal.proximity_cancelled").replace("{player}", victim.getName()));
                    iterator.remove();
                    continue;
                }

                // Verificar si ha pasado el tiempo requerido
                long elapsedTime = (System.currentTimeMillis() - attempt.getStartTime()) / 1000;

                if (elapsedTime >= attempt.getRequiredTime()) {
                    // Tiempo completado, ejecutar robo
                    executeSteal(thief, victim);
                    iterator.remove();
                }
            }
        }, 0L, 20L); // Ejecutar cada segundo (20 ticks)
    }

    /**
     * Clase interna para representar un intento de robo
     */
    private static class StealAttempt {
        private final UUID victimId;
        private final long startTime;
        private final int requiredTime;

        public StealAttempt(UUID victimId, long startTime, int requiredTime) {
            this.victimId = victimId;
            this.startTime = startTime;
            this.requiredTime = requiredTime;
        }

        public UUID getVictimId() { return victimId; }
        public long getStartTime() { return startTime; }
        public int getRequiredTime() { return requiredTime; }
    }
}