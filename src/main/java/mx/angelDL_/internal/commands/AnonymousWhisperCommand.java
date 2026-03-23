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
import java.util.stream.Collectors;

/**
 * Comando para enviar susurros anónimos como L
 */
public class AnonymousWhisperCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public AnonymousWhisperCommand(Main plugin) {
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

        // Verificar que hay suficientes argumentos
        if (args.length < 2) {
            player.sendMessage("§cUso: /" + label + " <jugador> <mensaje>");
            player.sendMessage("§7Envía un susurro anónimo a un jugador específico.");
            player.sendMessage("§7§oSolo L puede usar este comando.");
            return true;
        }

        // Obtener el jugador objetivo
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cJugador no encontrado: " + args[0]);
            return true;
        }

        // Verificar que no se esté enviando un mensaje a sí mismo
        if (target.equals(player)) {
            player.sendMessage("§cNo puedes enviarte susurros anónimos a ti mismo.");
            return true;
        }

        // Construir el mensaje
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }

        String message = messageBuilder.toString();

        // Enviar susurro a través del AnonymousMessenger
        // La verificación de si es L se hace dentro de AnonymousMessenger
        boolean success = plugin.getAnonymousMessenger().sendAnonymousWhisper(player, target, message);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;

            // Solo mostrar autocompletado si el jugador es L y no está en cooldown
            if (plugin.getAnonymousMessenger().canSendAnonymousMessages(player)) {
                // Sugerir nombres de jugadores online (excluyendo al remitente)
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(sender))
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}