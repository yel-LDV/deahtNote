package mx.angelDL_.internal.utils;

import mx.angelDL_.internal.config.ConfigManager;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class ParticleSystem {
    private final ConfigManager configManager;

    public ParticleSystem(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void spawnKiraParticles(Player player) {
        if (!configManager.isKiraParticlesEnabled()) return;

        Location loc = player.getLocation().add(0, 1, 0);
        Particle particle = configManager.getKiraParticleType();
        int count = configManager.getKiraParticleCount();
        double speed = configManager.getKiraParticleSpeed();

        try {
            // Usar FLAME por defecto para evitar errores con REDSTONE
            if (particle == Particle.HEART) {
                particle = Particle.FLAME; // Fallback seguro
            }

            player.getWorld().spawnParticle(
                    particle,
                    loc,
                    count,
                    0.5, 0.5, 0.5, // Offset
                    speed
            );
        } catch (Exception e) {
            // Si hay error, usar partícula por defecto
            player.getWorld().spawnParticle(
                    Particle.FLAME,
                    loc,
                    3,
                    0.3, 0.5, 0.3,
                    0.1
            );
        }
    }

    public void spawnStealParticles(Player victim, Player thief) {
        try {
            // Partículas para víctima - humo
            victim.getWorld().spawnParticle(
                    Particle.SMOKE,
                    victim.getLocation().add(0, 1, 0),
                    10,
                    0.5, 1, 0.5,
                    0.1
            );

            // Partículas para ladrón - felicidad
            thief.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    thief.getLocation().add(0, 1, 0),
                    5,
                    0.3, 1, 0.3,
                    0.05
            );
        } catch (Exception e) {
            // Ignorar errores de partículas
        }
    }
}