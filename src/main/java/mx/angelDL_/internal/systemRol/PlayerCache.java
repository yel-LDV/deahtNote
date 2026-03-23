package mx.angelDL_.internal.systemRol;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();
    private final Main plugin;

    public PlayerCache(Main plugin) {
        this.plugin = plugin;
    }

    public boolean registerPlayer(Player player, String role) {
        UUID uuid = player.getUniqueId();
        if (dataMap.containsKey(uuid)) {
            PlayerData data = dataMap.get(uuid);
            data.setRole(role);
            return true;
        }
        PlayerData data = new PlayerData(player.getName(), role, true);
        data.giveDefaultIdentification();
        dataMap.put(uuid, data);
        return true;
    }

    public boolean isRegistered(UUID uuid) {
        return dataMap.containsKey(uuid);
    }

    public Optional<PlayerData> getPlayerData(UUID uuid) {
        return Optional.ofNullable(dataMap.get(uuid));
    }

    public Collection<PlayerData> getAllData() {
        return dataMap.values();
    }

    public Set<Map.Entry<UUID, PlayerData>> getAllEntries() {
        return dataMap.entrySet();
    }

    public void putPlayerData(UUID uuid, PlayerData data) {
        dataMap.put(uuid, data);
    }

    public void removePlayer(UUID uuid) {
        dataMap.remove(uuid);
    }

    public void clear() {
        dataMap.clear();
    }

    public void registerSteal(UUID uuid) {
        getPlayerData(uuid).ifPresent(d -> d.setLastStealTime(System.currentTimeMillis()));
    }

    public boolean canSteal(UUID uuid) {
        Optional<PlayerData> opt = getPlayerData(uuid);
        if (opt.isEmpty()) return false;
        PlayerData d = opt.get();
        if (d.getLastStealTime() == 0) return true;
        long stealCooldown = plugin.getShinigamiDealManager().getStealCooldown(uuid) * 1000L;
        long timeSinceLastSteal = System.currentTimeMillis() - d.getLastStealTime();
        return timeSinceLastSteal >= stealCooldown;
    }

    public long getRemainingStealCooldown(UUID uuid) {
        Optional<PlayerData> opt = getPlayerData(uuid);
        if (opt.isEmpty()) return 0;
        PlayerData d = opt.get();
        if (d.getLastStealTime() == 0) return 0;
        long stealCooldown = plugin.getShinigamiDealManager().getStealCooldown(uuid) * 1000L;
        long elapsed = System.currentTimeMillis() - d.getLastStealTime();
        long remaining = (stealCooldown - elapsed) / 1000;
        return Math.max(remaining, 0);
    }

    public void loadOnlinePlayers(String defaultRole) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isRegistered(p.getUniqueId())) {
                registerPlayer(p, defaultRole);
            }
        }
    }
}