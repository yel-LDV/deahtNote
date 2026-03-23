package mx.angelDL_.internal.gui;

import mx.angelDL_.Main;
import mx.angelDL_.internal.managers.MissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MissionGUI implements Listener {
    private final Main plugin;
    private final MissionManager missionManager;
    private final Map<UUID, String> selectedZones = new HashMap<>();
    private final Map<UUID, List<UUID>> selectedPlayers = new HashMap<>();

    public MissionGUI(Main plugin, MissionManager missionManager) {
        this.plugin = plugin;
        this.missionManager = missionManager;
    }

    public void openTeleportGUI(Player player) {
        try {
            String title = "§bTeletransporte a Misiones";
            Inventory gui = Bukkit.createInventory(player, 54, title);

            int slot = 0;
            List<String> zoneKeys = new ArrayList<>(missionManager.getZones().keySet());

            plugin.getLogger().info("DEBUG: Creando GUI de teletransporte con " + zoneKeys.size() + " zonas");

            for (String zoneId : zoneKeys) {
                if (slot >= 54) break;

                MissionManager.MissionZone zone = missionManager.getZones().get(zoneId);

                ItemStack item = new ItemStack(zone.icon);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§b" + zone.name);

                List<String> lore = new ArrayList<>();
                lore.add("§7Mundo: §f" + zone.world);
                lore.add("§7Ubicación: §f" + zone.getCoordinatesText());
                lore.add("§7Duración: §f" + zone.duration + " segundos");
                lore.add("");
                lore.add("§aClick para teletransportarte");

                meta.setLore(lore);
                item.setItemMeta(meta);

                gui.setItem(slot, item);
                slot++;
            }

            player.openInventory(gui);
            plugin.getLogger().info("DEBUG: GUI de teletransporte ABIERTA para " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("ERROR al abrir GUI de teletransporte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openZoneSelection(Player player) {
        try {
            // CORRECCIÓN: Limpiar estado previo primero
            missionManager.forceClearGUI(player);
            selectedZones.remove(player.getUniqueId());
            selectedPlayers.remove(player.getUniqueId());

            if (!missionManager.canOpenGUI(player)) {
                plugin.getLogger().info("DEBUG: No puede abrir selección de zonas");
                return;
            }

            missionManager.addPlayerToGUI(player);

            String title = missionManager.getGUIMessage("zone_selection");
            Inventory gui = Bukkit.createInventory(player, 54, title);

            int slot = 0;
            List<String> zoneKeys = new ArrayList<>(missionManager.getZones().keySet());

            plugin.getLogger().info("DEBUG: Creando selección de zonas con " + zoneKeys.size() + " zonas");

            for (String zoneId : zoneKeys) {
                if (slot >= 54) break;

                MissionManager.MissionZone zone = missionManager.getZones().get(zoneId);

                ItemStack item = new ItemStack(zone.icon);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§b" + zone.name);

                List<String> lore = new ArrayList<>();
                for (String line : missionManager.getGUILore("zone_lore")) {
                    String formattedLine = line
                            .replace("{duration}", String.valueOf(zone.duration))
                            .replace("{world}", zone.world)
                            .replace("{min_x}", String.valueOf(zone.getMinX()))
                            .replace("{min_z}", String.valueOf(zone.getMinZ()))
                            .replace("{max_x}", String.valueOf(zone.getMaxX()))
                            .replace("{max_z}", String.valueOf(zone.getMaxZ()))
                            .replace("{pause_status}", zone.pauseEnabled ? "Sí" : "No");
                    lore.add(formattedLine);
                }

                meta.setLore(lore);
                item.setItemMeta(meta);

                gui.setItem(slot, item);
                slot++;
            }

            player.openInventory(gui);
            plugin.getLogger().info("DEBUG: Selección de zonas ABIERTA para " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("ERROR al abrir selección de zonas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openPlayerSelection(Player player, String zoneId) {
        try {
            plugin.getLogger().info("DEBUG: Abriendo selección de jugadores para " + player.getName() + ", zona: " + zoneId);

            // ✅ CORRECCIÓN CRÍTICA: Limpiar estado previo ANTES de crear nuevo
            missionManager.removePlayerFromGUI(player);
            selectedZones.remove(player.getUniqueId());
            selectedPlayers.remove(player.getUniqueId());

            // ✅ CORRECCIÓN: Crear NUEVAS selecciones
            missionManager.addPlayerToGUI(player);
            selectedZones.put(player.getUniqueId(), zoneId);
            selectedPlayers.put(player.getUniqueId(), new ArrayList<>());

            int maxPlayers = plugin.getConfig().getInt("mission_system.mission.max_players", 2);
            String title = missionManager.getGUIMessage("player_selection").replace("{max_players}", String.valueOf(maxPlayers));
            Inventory gui = Bukkit.createInventory(player, 54, title);

            int slot = 0;
            int onlinePlayers = 0;

            for (Player target : Bukkit.getOnlinePlayers()) {
                if (slot >= 53) break; // ✅ CORRECCIÓN: Dejar espacio para el botón de confirmación

                if (!plugin.getRoleApi().isAlive(target.getUniqueId())) continue;

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(target);

                if (target.getUniqueId().equals(player.getUniqueId())) {
                    meta.setDisplayName("§6" + target.getName() + " (TÚ)");
                } else {
                    meta.setDisplayName("§a" + target.getName());
                }

                List<String> lore = new ArrayList<>();
                for (String line : missionManager.getGUILore("player_lore")) {
                    String formattedLine = line.replace("{role}", plugin.getRoleApi().getRole(target.getUniqueId()));
                    lore.add(formattedLine);
                }

                meta.setLore(lore);
                skull.setItemMeta(meta);
                gui.setItem(slot, skull);
                slot++;
                onlinePlayers++;
            }

            plugin.getLogger().info("DEBUG: " + onlinePlayers + " jugadores online agregados al GUI");
            plugin.getLogger().info("DEBUG: Selecciones creadas - Zona: " + zoneId +
                    ", Jugadores: " + selectedPlayers.get(player.getUniqueId()).size());

            updateConfirmationItem(gui, 0);

            player.openInventory(gui);
            plugin.getLogger().info("DEBUG: Selección de jugadores ABIERTA para " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("ERROR al abrir selección de jugadores: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        plugin.getLogger().info("DEBUG: Click en inventario - Título: '" + title + "', Slot: " + event.getSlot());

        // Solo procesar clicks en el inventario superior
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (event.getSlot() < 0 || event.getSlot() >= 54) {
            return;
        }

        event.setCancelled(true);

        // CORRECCIÓN: Manejar diferentes GUIs
        if (title.equals(missionManager.getGUIMessage("zone_selection"))) {
            plugin.getLogger().info("DEBUG: Procesando click en selección de zona");
            handleZoneSelection(player, event.getSlot());
        } else if (title.startsWith("Seleccionar")) {
            plugin.getLogger().info("DEBUG: Procesando click en selección de jugadores");
            handlePlayerSelection(player, event.getSlot());
        } else if (title.equals("§bTeletransporte a Misiones")) {
            plugin.getLogger().info("DEBUG: Procesando click en teletransporte");
            handleTeleportSelection(player, event.getSlot());
        }
    }

    private void handleTeleportSelection(Player player, int slot) {
        Inventory inv = player.getOpenInventory().getTopInventory();

        if (slot < 0 || slot >= inv.getSize()) return;

        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;

        List<String> zoneKeys = new ArrayList<>(missionManager.getZones().keySet());
        if (slot < zoneKeys.size()) {
            String zoneId = zoneKeys.get(slot);
            plugin.getLogger().info("DEBUG: Teletransportando a zona: " + zoneId);
            missionManager.teleportToZone(player, zoneId);
            player.closeInventory();
        }
    }

    private void handleZoneSelection(Player player, int slot) {
        Inventory inv = player.getOpenInventory().getTopInventory();

        if (slot < 0 || slot >= inv.getSize()) return;

        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;

        List<String> zoneKeys = new ArrayList<>(missionManager.getZones().keySet());
        plugin.getLogger().info("DEBUG: Zonas disponibles: " + zoneKeys);

        if (slot < zoneKeys.size()) {
            String zoneId = zoneKeys.get(slot);
            MissionManager.MissionZone zone = missionManager.getZones().get(zoneId);

            if (zone != null) {
                plugin.getLogger().info("DEBUG: Abriendo selección de jugadores para: " + zone.name);
                openPlayerSelection(player, zoneId);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        plugin.getLogger().info("DEBUG: Cerrando inventario: " + title);

        // ✅ CORRECCIÓN: Solo limpiar si NO es una navegación entre GUIs del sistema
        if (title.equals(missionManager.getGUIMessage("zone_selection")) ||
                title.startsWith("Seleccionar")) {

            // Pequeño delay para evitar conflictos con apertura de nuevo GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Verificar si el jugador no tiene otro GUI abierto del sistema
                if (!player.getOpenInventory().getTitle().equals(missionManager.getGUIMessage("zone_selection")) &&
                        !player.getOpenInventory().getTitle().startsWith("Seleccionar")) {

                    missionManager.removePlayerFromGUI(player);
                    selectedZones.remove(player.getUniqueId());
                    selectedPlayers.remove(player.getUniqueId());
                    plugin.getLogger().info("DEBUG: Limpiadas selecciones para " + player.getName());
                }
            }, 2L);
        }
    }

    private void handlePlayerSelection(Player player, int slot) {
        UUID playerId = player.getUniqueId();

        // ✅ CORRECCIÓN: Verificar que las selecciones existen
        if (!selectedPlayers.containsKey(playerId)) {
            plugin.getLogger().warning("DEBUG: No hay lista de seleccionados para " + player.getName());
            player.sendMessage("§cError: No se encontraron selecciones. Por favor, cierra y reabre el menú.");
            return;
        }

        // Slot de confirmación
        if (slot == 53) {
            plugin.getLogger().info("DEBUG: Confirmando misión desde slot 53");
            confirmMission(player);
            return;
        }

        Inventory inv = player.getOpenInventory().getTopInventory();
        if (slot < 0 || slot >= inv.getSize()) return;

        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
        if (target == null) return;

        UUID targetId = target.getUniqueId();
        List<UUID> selected = selectedPlayers.get(playerId);

        if (selected.contains(targetId)) {
            // Deseleccionar
            selected.remove(targetId);
            if (targetId.equals(playerId)) {
                meta.setDisplayName("§6" + target.getName() + " (TÚ)");
                player.sendMessage("§7Te has deseleccionado");
            } else {
                meta.setDisplayName("§a" + target.getName());
                player.sendMessage("§7" + target.getName() + " deseleccionado");
            }
            item.setItemMeta(meta);
        } else {
            // Seleccionar
            int maxPlayers = plugin.getConfig().getInt("mission_system.mission.max_players", 2);
            if (selected.size() >= maxPlayers) {
                player.sendMessage("§cYa has seleccionado " + maxPlayers + " jugadores");
                return;
            }
            selected.add(targetId);
            meta.setDisplayName("§6" + target.getName() + " §a(SELECCIONADO)");
            item.setItemMeta(meta);
            if (targetId.equals(playerId)) {
                player.sendMessage("§6Te has seleccionado a ti mismo");
            } else {
                player.sendMessage("§a" + target.getName() + " seleccionado");
            }
        }

        updateConfirmationItem(inv, selected.size());
    }

    private void updateConfirmationItem(Inventory inv, int selectedCount) {
        ItemStack confirm = new ItemStack(Material.GRAY_DYE);
        int maxPlayers = plugin.getConfig().getInt("mission_system.mission.max_players", 2);

        if (selectedCount >= maxPlayers) {
            confirm = new ItemStack(Material.LIME_DYE);
        }

        ItemMeta meta = confirm.getItemMeta();
        String name = "§aConfirmar Misión (" + selectedCount + "/" + maxPlayers + ")";
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        if (selectedCount == 0) {
            lore.add("§cSelecciona " + maxPlayers + " jugadores (puedes incluirte)");
        } else if (selectedCount < maxPlayers) {
            int remaining = maxPlayers - selectedCount;
            lore.add("§eSelecciona " + remaining + " jugador(es) más (puedes incluirte)");
        } else {
            lore.add("§a¡Listo para confirmar!");
        }

        meta.setLore(lore);
        confirm.setItemMeta(meta);
        inv.setItem(53, confirm);
    }

    private void confirmMission(Player player) {
        UUID playerId = player.getUniqueId();
        String zoneId = selectedZones.get(playerId);
        List<UUID> selectedUUIDs = selectedPlayers.get(playerId);

        plugin.getLogger().info("DEBUG: Confirmando misión - Zona: " + zoneId +
                ", Jugadores seleccionados: " + (selectedUUIDs != null ? selectedUUIDs.size() : "null"));

        if (selectedUUIDs == null || selectedUUIDs.size() == 0) {
            player.sendMessage("§cDebes seleccionar jugadores primero");
            return;
        }

        int maxPlayers = plugin.getConfig().getInt("mission_system.mission.max_players", 2);
        if (selectedUUIDs.size() != maxPlayers) {
            player.sendMessage("§cDebes seleccionar exactamente " + maxPlayers + " jugadores");
            return;
        }

        List<Player> selectedPlayersList = new ArrayList<>();
        for (UUID uuid : selectedUUIDs) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                selectedPlayersList.add(target);
            }
        }

        if (selectedPlayersList.size() != maxPlayers) {
            player.sendMessage("§cAlgunos jugadores seleccionados no están disponibles");
            return;
        }

        boolean success = missionManager.assignMission(player, zoneId, selectedPlayersList);

        if (success) {
            player.closeInventory();
            missionManager.removePlayerFromGUI(player);
            selectedZones.remove(playerId);
            selectedPlayers.remove(playerId);
            plugin.getLogger().info("DEBUG: Misión confirmada exitosamente");
        } else {
            player.sendMessage("§cNo se pudo asignar la misión");
        }
    }
}