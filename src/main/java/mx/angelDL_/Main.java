package mx.angelDL_;

import mx.angelDL_.internal.commands.*;
import mx.angelDL_.internal.config.ConfigManager;
import mx.angelDL_.internal.database.DatabaseManager;
import mx.angelDL_.internal.events.PlayersJoinEvents;
import mx.angelDL_.internal.gui.MissionGUI;
import mx.angelDL_.internal.lang.LanguageManager;
import mx.angelDL_.internal.listeners.*;
import mx.angelDL_.internal.managers.*;
import mx.angelDL_.internal.systemRol.*;
import mx.angelDL_.internal.tasks.ActionBarTask;
import mx.angelDL_.internal.tasks.DeathNoteCheckTask;
import mx.angelDL_.internal.tasks.KiraParticleTask;
import mx.angelDL_.internal.utils.ParticleSystem;
import mx.angelDL_.internal.utils.SoundSystem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin {
    private static Main plugin;
    private PlayerCache playerCache;
    private RoleAPI roleAPI;
    private File scoreboardFile;
    private FileConfiguration scoreboardConfig;

    private ConfigManager configManager;
    private DeathNoteManager deathNoteManager;
    private MelloManager melloManager;
    private ParticleSystem particleSystem;
    private SoundSystem soundSystem;
    private RoleConversionManager roleConversionManager;
    private VictoryConditionManager victoryConditionManager;
    private ActionBarManager actionBarManager;
    private AnonymousMessenger anonymousMessenger;
    private MissionManager missionManager;
    private MissionGUI missionGUI;
    private ShinigamiDealManager shinigamiDealManager;
    private RoleSwapManager roleSwapManager;
    private RoleDistributionManager roleDistributionManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private int playersOnline;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        languageManager = new LanguageManager(this);
        databaseManager = new DatabaseManager(this);
        setupManagers();
        loadPlayersFromDatabase();
        registerEvents();
        registerCommands();
        startTasks();
        getLogger().info("[DeathNote] ha encendido correctamente!!");
        playerCache.loadOnlinePlayers("Investigador");
    }

    private void loadPlayersFromDatabase() {
        List<Map<String, Object>> allPlayers = databaseManager.loadAllPlayers();
        for (Map<String, Object> data : allPlayers) {
            try {
                UUID uuid = UUID.fromString((String) data.get("uuid"));
                String name = (String) data.get("name");
                String role = (String) data.get("role");
                boolean alive = (Boolean) data.get("alive");
                long lastStealTime = (Long) data.get("last_steal_time");
                boolean hasDeal = (Boolean) data.get("has_shinigami_deal");

                PlayerData pd = new PlayerData(name, role, alive);
                pd.setLastStealTime(lastStealTime);
                List<String> ids = databaseManager.loadIdentifications(uuid);
                for (String id : ids) {
                    pd.addIdentification(id);
                }
                playerCache.putPlayerData(uuid, pd);

                if (hasDeal) {
                    shinigamiDealManager.loadPlayerData(uuid, true);
                }
            } catch (Exception e) {
                getLogger().warning("Error loading player from DB: " + e.getMessage());
            }
        }
        getLogger().info("[DeathNote] Loaded " + allPlayers.size() + " players from database.");
    }

    private void saveAllPlayersToDatabase() {
        for (Map.Entry<UUID, PlayerData> entry : playerCache.getAllEntries()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            boolean hasDeal = shinigamiDealManager.getPlayerDealState(uuid);
            databaseManager.savePlayer(uuid, data.getPlayerName(), data.getRole(), data.isAlive(), data.getLastStealTime(), hasDeal);
            databaseManager.saveIdentifications(uuid, data.getIdentifications());
        }
        getLogger().info("[DeathNote] All player data saved to database.");
    }

    private void setupManagers() {
        this.configManager = new ConfigManager(getConfig());
        this.victoryConditionManager = new VictoryConditionManager(this, configManager);
        this.actionBarManager = new ActionBarManager(this, configManager);
        this.deathNoteManager = new DeathNoteManager(this, configManager);
        this.melloManager = new MelloManager(configManager);
        this.particleSystem = new ParticleSystem(configManager);
        this.soundSystem = new SoundSystem(configManager);
        this.roleConversionManager = new RoleConversionManager(plugin);
        this.roleSwapManager = new RoleSwapManager(this);
        this.shinigamiDealManager = new ShinigamiDealManager(this);
        this.playerCache = new PlayerCache(plugin);
        this.roleAPI = new RoleAPI(playerCache);
        this.anonymousMessenger = new AnonymousMessenger(this, roleAPI);
        this.missionManager = new MissionManager(this);
        this.missionGUI = new MissionGUI(this, missionManager);
        this.roleDistributionManager = new RoleDistributionManager(this);
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayersJoinEvents(this), this);
        getServer().getPluginManager().registerEvents(new DeathNoteListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new IdentificationProtectionListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new MikamiGiveIDListener(this), this);
        getServer().getPluginManager().registerEvents(new StealIdentificationListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new DeathNoteProtectionListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this, configManager, victoryConditionManager), this);
        getServer().getPluginManager().registerEvents(missionGUI, this);
        getServer().getPluginManager().registerEvents(new MissionItemListener(this, missionManager, missionGUI), this);
        getServer().getPluginManager().registerEvents(new ShinigamiDealListener(this), this);
        getServer().getPluginManager().registerEvents(new ShinigamiItemProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new RoleSwapListener(this), this);
        getServer().getPluginManager().registerEvents(new RoleSwapProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new WatariAndLListener(this), this);
    }

    private void registerCommands() {
        getCommand("s").setExecutor(new AnonymousMessageCommand(this));
        getCommand("m").setExecutor(new AnonymousWhisperCommand(this));
        getCommand("debug").setExecutor(new DebugCommand(this));
        getCommand("distributeroles").setExecutor(new DistributeRolesCommand(this));
        getCommand("missiontp").setExecutor(new MissionTeleportCommand(this, missionGUI));

        getCommand("missiondebug").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                missionManager.debugGUIState(player);
                player.sendMessage(lang().get("debug.header"));
                return true;
            }
            return false;
        });

        getCommand("missionclear").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                missionManager.forceClearGUI(player);
                player.sendMessage("§aGUI limpiada forzadamente");
                return true;
            }
            return false;
        });

        getCommand("testplayers").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                missionGUI.openPlayerSelection(player, "plaza_central");
                return true;
            }
            return false;
        });
    }

    private void startTasks() {
        new KiraParticleTask(this, configManager).runTaskTimer(this, 0L, 20L);
        new ActionBarTask(this, configManager).runTaskTimer(this, 0L, configManager.getActionBarInterval() * 20L);
        new DeathNoteCheckTask(this, configManager).runTaskTimer(this, 0L, 40L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (missionManager != null) {
                    missionManager.updateBossBarForAll();
                }
            }
        }.runTaskTimer(this, 0L, 200L);
    }

    @Override
    public void onDisable() {
        saveAllPlayersToDatabase();
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("[DeathNote] se ha apagado correctamente!");
    }

    public LanguageManager lang() {
        return languageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public int getPlayersOnline() {
        return playersOnline;
    }

    public void addPlayerOnline(int count) {
        playersOnline = playersOnline + count;
    }

    public RoleAPI getRoleApi() {
        return roleAPI;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DeathNoteManager getDeathNoteManager() {
        return deathNoteManager;
    }

    public MelloManager getMelloManager() {
        return melloManager;
    }

    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }

    public SoundSystem getSoundSystem() {
        return soundSystem;
    }

    public RoleConversionManager getRoleConversionManager() {
        return roleConversionManager;
    }

    public VictoryConditionManager getVictoryConditionManager() {
        return victoryConditionManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public AnonymousMessenger getAnonymousMessenger() {
        return anonymousMessenger;
    }

    public static Main getInstance() {
        return plugin;
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public ShinigamiDealManager getShinigamiDealManager() {
        return shinigamiDealManager;
    }

    public RoleSwapManager getRoleSwapManager() {
        return roleSwapManager;
    }

    public RoleDistributionManager getRoleDistributionManager() {
        return roleDistributionManager;
    }
}