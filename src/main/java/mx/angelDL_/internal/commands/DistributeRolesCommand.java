package mx.angelDL_.internal.commands;

import mx.angelDL_.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DistributeRolesCommand implements CommandExecutor {
    private final Main plugin;

    public DistributeRolesCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deathnote.distribute")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando");
            return true;
        }

        if (plugin.getRoleDistributionManager().areRolesDistributed()) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
                plugin.getRoleDistributionManager().resetDistribution();
                sender.sendMessage("§aDistribución de roles reiniciada");
                return true;
            }
            sender.sendMessage("§cLos roles ya han sido distribuidos. Usa §e/distributeroles reset §cpara reiniciar");
            return true;
        }

        boolean success = plugin.getRoleDistributionManager().distributeRoles();
        if (!success) {
            sender.sendMessage("§cNo se pudieron distribuir los roles");
        }

        return true;
    }
}