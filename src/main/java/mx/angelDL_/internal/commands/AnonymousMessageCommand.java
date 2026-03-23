package mx.angelDL_.internal.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import mx.angelDL_.Main;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando para enviar mensajes anónimos como L
 */
public class AnonymousMessageCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public AnonymousMessageCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verificar que el remitente es un jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // ELIMINADA VERIFICACIÓN DE PERMISOS GENERALES
        // SOLO L PUEDE USAR ESTE COMANDO - VERIFICACIÓN EN AnonymousMessenger

        // Verificar que hay argumentos
        if (args.length == 0) {
            player.sendMessage("§cUso: /" + label + " <mensaje>");
            player.sendMessage("§7Envía un mensaje anónimo a todos los jugadores.");
            player.sendMessage("§7§oSolo L puede usar este comando.");
            return true;
        }

        // Construir el mensaje
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }

        String message = messageBuilder.toString();

        // Enviar mensaje a través del AnonymousMessenger
        // La verificación de si es L se hace dentro de AnonymousMessenger
        boolean success = plugin.getAnonymousMessenger().sendAnonymousMessage(player, message);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No sugerir autocompletado para mensajes
        return new ArrayList<>();
    }
}