package mx.angelDL_.internal.managers;

import mx.angelDL_.Main;
import mx.angelDL_.internal.systemRol.RoleAPI;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MissionManager {
    private final Main plugin;
    private final RoleAPI roleAPI;
    private final Map<String, MissionZone> zones = new HashMap<>();
    private final Map<UUID, ActiveMission> activeMissions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> missionCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> playersInGUI = new HashSet<>();
    private final File zonesFile;
    private FileConfiguration zonesConfig;

    public MissionManager(Main plugin) {
        this.plugin = plugin;
        this.roleAPI = plugin.getRoleApi();
        this.zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        loadZones();
    }

    public static class MissionZone {
        public final String name;
        public final String world;
        public final int centerX, centerZ;
        public final int radius;
        public final int duration;
        public final Material icon;
        public final boolean pauseEnabled;
        public final Location teleportLocation;

        public MissionZone(String name, String world, int centerX, int centerZ, int radius,
                           int duration, Material icon, boolean pauseEnabled, Location teleportLocation) {
            this.name = name;
            this.world = world;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.duration = duration;
            this.icon = icon;
            this.pauseEnabled = pauseEnabled;
            this.teleportLocation = teleportLocation;
        }

        public boolean isInZone(Location loc) {
            if (!loc.getWorld().getName().equals(world)) return false;

            double distance = Math.sqrt(
                    Math.pow(loc.getX() - centerX, 2) +
                            Math.pow(loc.getZ() - centerZ, 2)
            );

            return distance <= radius;
        }

        public String getCoordinatesText() {
            return String.format("(%d, %d) radio %d", centerX, centerZ, radius);
        }

        public int getMinX() { return centerX - radius; }
        public int getMinZ() { return centerZ - radius; }
        public int getMaxX() { return centerX + radius; }
        public int getMaxZ() { return centerZ + radius; }
    }

    public static class ActiveMission {
        public final UUID lPlayer;
        public final List<UUID> assignedPlayers;
        public final MissionZone zone;
        public final BossBar bossBar;
        public int timeRemaining;
        public int progress; // ✅ NUEVO: Progreso actual (0-100)
        public boolean isPaused;
        public final long startTime;
        public final Map<UUID, String> playerNames;
        public final Map<UUID, Boolean> playersInZone; // ✅ NUEVO: Estado de cada jugador en zona

        public ActiveMission(UUID lPlayer, List<UUID> assignedPlayers, MissionZone zone, BossBar bossBar) {
            this.lPlayer = lPlayer;
            this.assignedPlayers = assignedPlayers;
            this.zone = zone;
            this.bossBar = bossBar;
            this.timeRemaining = zone.duration;
            this.progress = 0; // ✅ NUEVO: Iniciar en 0%
            this.isPaused = false;
            this.startTime = System.currentTimeMillis();
            this.playerNames = new HashMap<>();
            this.playersInZone = new HashMap<>(); // ✅ NUEVO: Inicializar mapa

            for (UUID playerId : assignedPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    playerNames.put(playerId, player.getName());
                    playersInZone.put(playerId, false); // ✅ NUEVO: Inicialmente fuera de zona
                }
            }
        }

        public String getPlayerNamesText() {
            List<String> names = new ArrayList<>();
            for (UUID playerId : assignedPlayers) {
                String name = playerNames.get(playerId);
                if (name != null) {
                    names.add(name);
                }
            }
            return String.join(", ", names);
        }

        // ✅ NUEVO: Verificar si todos están en zona
        public boolean allPlayersInZone() {
            for (Boolean inZone : playersInZone.values()) {
                if (!inZone) return false;
            }
            return true;
        }

        // ✅ NUEVO: Verificar si al menos uno está en zona
        public boolean anyPlayerInZone() {
            for (Boolean inZone : playersInZone.values()) {
                if (inZone) return true;
            }
            return false;
        }

        // ✅ NUEVO: Obtener cantidad de jugadores en zona
        public int getPlayersInZoneCount() {
            int count = 0;
            for (Boolean inZone : playersInZone.values()) {
                if (inZone) count++;
            }
            return count;
        }
    }

    public void loadZones() {
        if (!zonesFile.exists()) {
            plugin.saveResource("zones.yml", false);
        }
        zonesConfig = YamlConfiguration.loadConfiguration(zonesFile);
        zones.clear();

        if (zonesConfig.contains("zones")) {
            for (String key : zonesConfig.getConfigurationSection("zones").getKeys(false)) {
                String path = "zones." + key + ".";
                try {
                    // Obtener ubicación de teletransporte
                    Location teleportLoc = new Location(
                            Bukkit.getWorld(zonesConfig.getString(path + "world", "world")),
                            zonesConfig.getDouble(path + "teleport_location.x", 0.5),
                            zonesConfig.getDouble(path + "teleport_location.y", 64.0),
                            zonesConfig.getDouble(path + "teleport_location.z", 0.5),
                            (float) zonesConfig.getDouble(path + "teleport_location.yaw", 0.0),
                            (float) zonesConfig.getDouble(path + "teleport_location.pitch", 0.0)
                    );

                    MissionZone zone = new MissionZone(
                            zonesConfig.getString(path + "name", key),
                            zonesConfig.getString(path + "world", "world"),
                            zonesConfig.getInt(path + "center_x"),
                            zonesConfig.getInt(path + "center_z"),
                            zonesConfig.getInt(path + "radius", 50),
                            zonesConfig.getInt(path + "duration", 60),
                            Material.valueOf(zonesConfig.getString(path + "icon", "STONE")),
                            zonesConfig.getBoolean(path + "pause_enabled", true),
                            teleportLoc
                    );
                    zones.put(key, zone);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error cargando zona " + key + ": " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Cargadas " + zones.size() + " zonas de misión");
    }

    public boolean canOpenGUI(Player player) {
        if (activeMissions.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("already_has_mission"));
            return false;
        }
        return true;
    }

    public void addPlayerToGUI(Player player) {
        playersInGUI.add(player.getUniqueId());
    }

    public void removePlayerFromGUI(Player player) {
        playersInGUI.remove(player.getUniqueId());
    }

    public void forceClearGUI(Player player) {
        playersInGUI.remove(player.getUniqueId());
    }

    public void debugGUIState(Player player) {
        player.sendMessage(plugin.lang().get("debug.header"));
        player.sendMessage("§eEn GUI: " + playersInGUI.contains(player.getUniqueId()));
        player.sendMessage("§eMisión activa: " + activeMissions.containsKey(player.getUniqueId()));
    }

    public boolean teleportToZone(Player player, String zoneId) {
        MissionZone zone = zones.get(zoneId);
        if (zone == null) {
            player.sendMessage(getMessage("zone_not_found"));
            return false;
        }

        if (zone.teleportLocation.getWorld() == null) {
            player.sendMessage("§cMundo de la zona no encontrado");
            return false;
        }

        player.teleport(zone.teleportLocation);
        player.sendMessage("§aTeletransportado a: " + zone.name);
        return true;
    }

    public boolean assignMission(Player lPlayer, String zoneId, List<Player> selectedPlayers) {
        if (!roleAPI.getRole(lPlayer.getUniqueId()).equalsIgnoreCase("l")) {
            lPlayer.sendMessage(plugin.lang().get("mission.role_required"));
            return false;
        }

        if (!canOpenGUI(lPlayer)) {
            return false;
        }

        if (hasCooldown(lPlayer)) {
            long remaining = getRemainingCooldown(lPlayer);
            lPlayer.sendMessage(getMessage("cooldown").replace("{time}", String.valueOf(remaining)));
            return false;
        }

        MissionZone zone = zones.get(zoneId);
        if (zone == null) {
            lPlayer.sendMessage(getMessage("zone_not_found"));
            return false;
        }

        int maxPlayers = plugin.getConfig().getInt("mission_system.mission.max_players", 2);
        if (selectedPlayers.size() != maxPlayers) {
            lPlayer.sendMessage(getMessage("need_players").replace("{max_players}", String.valueOf(maxPlayers)));
            return false;
        }

        for (Player player : selectedPlayers) {
            if (!roleAPI.isAlive(player.getUniqueId())) {
                lPlayer.sendMessage(getMessage("player_dead").replace("{player}", player.getName()));
                return false;
            }
        }

        List<UUID> assignedUUIDs = new ArrayList<>();
        for (Player player : selectedPlayers) {
            assignedUUIDs.add(player.getUniqueId());
        }

        String bossBarTitle = getBossBarTitle(zone, selectedPlayers);
        BossBar bossBar = Bukkit.createBossBar(bossBarTitle, BarColor.BLUE, BarStyle.SEGMENTED_10);
        bossBar.setProgress(0.0); // ✅ NUEVO: Iniciar en 0% de progreso

        ActiveMission mission = new ActiveMission(lPlayer.getUniqueId(), assignedUUIDs, zone, bossBar);

        updateBossBarPlayers(mission);

        activeMissions.put(lPlayer.getUniqueId(), mission);
        missionCooldowns.put(lPlayer.getUniqueId(), System.currentTimeMillis());

        removePlayerFromGUI(lPlayer);

        startMissionTimer(mission);

        lPlayer.sendMessage(plugin.lang().get("mission.assigned", java.util.Map.of("zone", zone.name)));
        for (Player player : selectedPlayers) {
            player.sendMessage(plugin.lang().get("mission.received", java.util.Map.of("zone", zone.name)));
            player.sendMessage(plugin.lang().get("mission.instructions", java.util.Map.of("duration", String.valueOf(zone.duration))));
        }
        return true;
    }

    private void updateBossBarPlayers(ActiveMission mission) {
        mission.bossBar.removeAll();

        Player lPlayer = Bukkit.getPlayer(mission.lPlayer);
        if (lPlayer != null && lPlayer.isOnline()) {
            mission.bossBar.addPlayer(lPlayer);
        }

        for (UUID playerId : mission.assignedPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                mission.bossBar.addPlayer(player);
            }
        }
    }

    private String getBossBarTitle(MissionZone zone, List<Player> players) {
        String playerNames = players.stream()
                .map(Player::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        return String.format("§b%s §7| §f%s §7| §a%d seg",
                zone.name, playerNames, zone.duration);
    }

    private void startMissionTimer(ActiveMission mission) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeMissions.containsValue(mission)) {
                    this.cancel();
                    return;
                }

                boolean allInZone = true;
                int playersInZone = 0;
                List<String> playerStatus = new ArrayList<>();

                // Actualizar estado de jugadores en zona
                for (UUID playerId : mission.assignedPlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline() || !roleAPI.isAlive(playerId)) {
                        missionFailed(mission, getMessage("mission_failed").replace("{reason}", getMessage("reason_disconnected")));
                        this.cancel();
                        return;
                    }

                    boolean inZone = mission.zone.isInZone(player.getLocation());
                    mission.playersInZone.put(playerId, inZone);

                    if (inZone) {
                        playersInZone++;
                        playerStatus.add("§a" + mission.playerNames.get(playerId));
                    } else {
                        allInZone = false;
                        playerStatus.add("§c" + mission.playerNames.get(playerId));
                    }
                }

                // ✅ CORREGIDO: Manejar progreso y verificar si debe cancelarse
                boolean shouldCancel = handleMissionProgress(mission, playersInZone, allInZone);
                if (shouldCancel) {
                    this.cancel();
                    return;
                }

                updateBossBar(mission, playersInZone, playerStatus, allInZone);

                // Lógica de pausa/reinicio mejorada
                handlePauseLogic(mission, allInZone);

                long maxDuration = plugin.getConfig().getLong("mission_system.mission.max_duration", 300) * 1000;
                if (System.currentTimeMillis() - mission.startTime > maxDuration) {
                    missionFailed(mission, getMessage("mission_failed").replace("{reason}", getMessage("reason_timeout")));
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ✅ CORRECCIÓN: Manejar el progreso de la misión - DEVUELVE boolean PARA SABER SI CANCELAR
    private boolean handleMissionProgress(ActiveMission mission, int playersInZone, boolean allInZone) {
        if (allInZone) {
            // Ambos jugadores en zona: progreso normal
            if (!mission.isPaused) {
                mission.timeRemaining--;

                // Calcular progreso basado en tiempo transcurrido
                double progressPercentage = ((double)(mission.zone.duration - mission.timeRemaining) / mission.zone.duration);
                mission.progress = (int) (progressPercentage * 100);

                if (mission.timeRemaining <= 0) {
                    missionCompleted(mission);
                    return true; // ✅ INDICAR QUE SE DEBE CANCELAR
                }
            }
        } else if (playersInZone > 0) {
            // Solo un jugador en zona: progreso lento (50% velocidad)
            if (!mission.isPaused) {
                mission.timeRemaining--;

                // Progreso más lento cuando solo uno está en zona
                double progressPercentage = ((double)(mission.zone.duration - mission.timeRemaining) / mission.zone.duration) * 0.5;
                mission.progress = (int) (progressPercentage * 100);

                if (mission.timeRemaining <= 0) {
                    missionCompleted(mission);
                    return true; // ✅ INDICAR QUE SE DEBE CANCELAR
                }
            }
        } else {
            // Ningún jugador en zona: no hay progreso
            // El tiempo no disminuye y el progreso se mantiene
        }
        return false; // ✅ NO CANCELAR
    }

    // ✅ NUEVO: Manejar lógica de pausa/reinicio
    private void handlePauseLogic(ActiveMission mission, boolean allInZone) {
        if (mission.zone.pauseEnabled) {
            // Zona con pausa habilitada
            if (!allInZone && !mission.isPaused) {
                mission.isPaused = true;
            } else if (allInZone && mission.isPaused) {
                mission.isPaused = false;
            }
        } else {
            // Zona SIN pausa habilitada - reiniciar si nadie está en zona
            if (!mission.anyPlayerInZone() && mission.progress > 0) {
                // Reiniciar progreso si nadie está en zona
                mission.timeRemaining = mission.zone.duration;
                mission.progress = 0;
                mission.isPaused = false;

                // Notificar a los jugadores
                notifyProgressReset(mission);
            } else if (mission.anyPlayerInZone() && mission.isPaused) {
                mission.isPaused = false;
            }
        }
    }

    private void notifyProgressReset(ActiveMission mission) {
        String msg = plugin.lang().get("mission.progress_reset");
        Player lPlayer = Bukkit.getPlayer(mission.lPlayer);
        if (lPlayer != null) lPlayer.sendMessage(msg);
        for (UUID playerId : mission.assignedPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) player.sendMessage(msg);
        }
    }

    private void updateBossBar(ActiveMission mission, int playersInZone, List<String> playerStatus, boolean allInZone) {
        double progress = (double) mission.progress / 100.0;

        String status;
        BarColor color;

        if (mission.isPaused) {
            status = plugin.lang().get("mission_status.paused");
            color = BarColor.YELLOW;
        } else if (!allInZone && playersInZone > 0) {
            status = plugin.lang().get("mission_status.slow");
            color = BarColor.YELLOW;
        } else if (allInZone) {
            status = plugin.lang().get("mission_status.normal");
            color = BarColor.GREEN;
        } else {
            status = plugin.lang().get("mission_status.stopped");
            color = BarColor.RED;
        }

        String playersText = String.join(" §7| ", playerStatus);
        String coordinates = mission.zone.getCoordinatesText();

        // ✅ NUEVO: Mostrar progreso en porcentaje
        String title = String.format("§b%s §7| %s §7| §f%d§7/§f%d §7| %s §7| §e%d%% §7| §e%s",
                mission.zone.name, playersText, playersInZone, mission.assignedPlayers.size(),
                status, mission.progress, coordinates);

        mission.bossBar.setTitle(title);
        mission.bossBar.setProgress(progress);
        mission.bossBar.setColor(color);

        updateBossBarPlayers(mission);
    }

    private void missionCompleted(ActiveMission mission) {
        Player lPlayer = Bukkit.getPlayer(mission.lPlayer);
        if (lPlayer != null) {
            int rewardExp = plugin.getConfig().getInt("mission_system.mission.reward_exp", 50);
            lPlayer.sendMessage(getMessage("mission_completed").replace("{exp}", String.valueOf(rewardExp)));
            lPlayer.giveExp(rewardExp);
        }

        for (UUID playerId : mission.assignedPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                int rewardExp = plugin.getConfig().getInt("mission_system.mission.reward_exp", 50);
                player.sendMessage(getMessage("mission_completed").replace("{exp}", String.valueOf(rewardExp)));
                player.giveExp(rewardExp);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mission.bossBar.removeAll();
        }, 100L);

        activeMissions.remove(mission.lPlayer);

        Bukkit.broadcastMessage(getMessage("mission_broadcast").replace("{zone}", mission.zone.name));
    }

    private void missionFailed(ActiveMission mission, String reason) {
        Player lPlayer = Bukkit.getPlayer(mission.lPlayer);

        if (lPlayer != null && lPlayer.isOnline() &&
                plugin.getRoleApi().getRole(lPlayer.getUniqueId()).equalsIgnoreCase("l")) {
            double currentHealth = lPlayer.getHealth();
            lPlayer.setHealth(Math.max(1.0, currentHealth - 2.0));
            lPlayer.sendMessage(plugin.lang().get("mission.penalty"));
            lPlayer.sendMessage("§7" + reason);
            lPlayer.playSound(lPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }

        if (lPlayer != null) lPlayer.sendMessage(reason);

        for (UUID playerId : mission.assignedPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) player.sendMessage(reason);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> mission.bossBar.removeAll(), 100L);
        activeMissions.remove(mission.lPlayer);
    }

    public boolean hasCooldown(Player player) {
        if (!missionCooldowns.containsKey(player.getUniqueId())) return false;
        long cooldown = plugin.getConfig().getLong("mission_system.mission_item.cooldown", 60) * 1000;
        return System.currentTimeMillis() - missionCooldowns.get(player.getUniqueId()) < cooldown;
    }

    public long getRemainingCooldown(Player player) {
        if (!missionCooldowns.containsKey(player.getUniqueId())) return 0;
        long cooldown = plugin.getConfig().getLong("mission_system.mission_item.cooldown", 60) * 1000;
        return Math.max(0, (cooldown - (System.currentTimeMillis() - missionCooldowns.get(player.getUniqueId()))) / 1000);
    }

    public ItemStack getMissionItem() {
        String materialName = plugin.getConfig().getString("mission_system.mission_item.material", "COMPASS");
        Material material = Material.valueOf(materialName.toUpperCase());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfig().getString("mission_system.mission_item.name", "§bMisión de L"));

        List<String> lore = plugin.getConfig().getStringList("mission_system.mission_item.lore");
        meta.setLore(lore);

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }


    public Map<String, MissionZone> getZones() {
        return Collections.unmodifiableMap(zones);
    }

    public void updateBossBarForAll() {
        for (ActiveMission mission : activeMissions.values()) {
            updateBossBarPlayers(mission);
        }
    }

    public String getMessage(String key) {
        String langKey = "mission." + key.replace("_", ".");
        String langValue = plugin.lang().get(langKey);
        if (langValue.startsWith("[")) {
            return plugin.getConfig().getString("mission_system.messages." + key, "§cMensaje no configurado: " + key);
        }
        return langValue;
    }

    public String getGUIMessage(String key) {
        return plugin.getConfig().getString("mission_system.gui." + key, "§c" + key);
    }

    public List<String> getGUILore(String key) {
        return plugin.getConfig().getStringList("mission_system.gui." + key);
    }
}