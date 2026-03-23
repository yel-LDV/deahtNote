package mx.angelDL_.internal.commands;

import mx.angelDL_.Main;
import mx.angelDL_.internal.gui.MissionGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MissionTeleportCommand implements CommandExecutor {
    private final Main plugin;
    private final MissionGUI missionGUI;

    public MissionTeleportCommand(Main plugin, MissionGUI missionGUI) {
        this.plugin = plugin;
        this.missionGUI = missionGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("deathnote.mission.teleport")) {
            player.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        missionGUI.openTeleportGUI(player);
        return true;
    }
}