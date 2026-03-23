package mx.angelDL_.internal.commands;

import mx.angelDL_.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DebugCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    private final List<String> validRoles = Arrays.asList(
            "kira", "l", "watari", "mikami", "mello", "near", "investigador");

    public DebugCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        if (!plugin.getConfigManager().getDebugMode()) {
            sender.sendMessage("§cEste comando esta desabilitado, ve a la config y activalo (swhic false -> true)");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "getrole":
                handleGetRole(sender, args);
                break;

            case "setrole":
                handleSetRole(sender, args);
                break;

            case "countrole":
                handleCountRole(sender, args);
                break;

            case "listroles":
                handleListRoles(sender);
                break;
            case "anonymouscooldown":
                handleAnonymousCooldown(sender, args);
                break;

            case "playerinfo":
                handlePlayerInfo(sender, args);
                break;

            case "reloadconfig":
                handleReloadConfig(sender);
                break;

            case "givedeathnote":
                handleGiveDeathNote(sender, args);
                break;

            case "checkcooldown":
                handleCheckCooldown(sender, args);
                break;

            case "setalive":
                handleSetAlive(sender, args);
                break;

            default:
                sender.sendMessage("§cComando desconocido. Usa /debug para ver ayuda.");
                break;
        }

        return true;
    }

    private void handleAnonymousCooldown(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }

        Player player = (Player) sender;
        long cooldown = plugin.getAnonymousMessenger().getRemainingCooldownForPlayer(player);

        if (cooldown > 0) {
            sender.sendMessage("§eCooldown de mensajes anónimos: §c" + cooldown + "s");
        } else {
            sender.sendMessage("§ePuedes enviar mensajes anónimos ahora.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Debug Commands ===");
        sender.sendMessage("§e/debug getrole [jugador] §7- Ver tu rol o el de otro jugador");
        sender.sendMessage("§e/debug setrole <rol> [jugador] §7- Cambiar rol");
        sender.sendMessage("§e/debug countrole <rol> §7- Contar jugadores con un rol");
        sender.sendMessage("§e/debug listroles §7- Listar todos los roles y sus conteos");
        sender.sendMessage("§e/debug playerinfo [jugador] §7- Información detallada del jugador");
        sender.sendMessage("§e/debug setalive <true/false> [jugador] §7- Cambiar estado de vida");
        sender.sendMessage("§e/debug reloadconfig §7- Recargar configuración");
        sender.sendMessage("§e/debug givedeathnote [jugador] §7- Dar Death Note a un jugador");
        sender.sendMessage("§e/debug checkcooldown <tipo> [jugador] §7- Ver cooldowns");
        sender.sendMessage("§6======================");
        sender.sendMessage("§7Roles válidos: §e" + String.join("§7, §e", validRoles));
    }

    private void handleGetRole(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores o especifica un jugador.");
                return;
            }
            Player player = (Player) sender;
            String role = plugin.getRoleApi().getRole(player.getUniqueId());
            boolean isAlive = plugin.getRoleApi().isAlive(player.getUniqueId());
            sender.sendMessage("§aTu rol es: §e" + role + " §7(Estado: " + (isAlive ? "§aVivo" : "§cMuerto") + "§7)");
        } else if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + args[1]);
                return;
            }
            String role = plugin.getRoleApi().getRole(target.getUniqueId());
            boolean isAlive = plugin.getRoleApi().isAlive(target.getUniqueId());
            sender.sendMessage("§aRol de §e" + target.getName() + "§a: §e" + role + " §7(Estado: "
                    + (isAlive ? "§aVivo" : "§cMuerto") + "§7)");
        } else {
            sender.sendMessage("§cUso: /debug getrole [jugador]");
        }
    }

    private void handleSetRole(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /debug setrole <rol> [jugador]");
            sender.sendMessage("§7Roles válidos: §e" + String.join("§7, §e", validRoles));
            return;
        }

        String role = args[1].toLowerCase();

        // Verificar si el rol es válido
        if (!isValidRole(role)) {
            sender.sendMessage("§cRol inválido: " + args[1]);
            sender.sendMessage("§7Roles válidos: §e" + String.join("§7, §e", validRoles));
            return;
        }

        Player target;

        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEspecifica un jugador: /debug setrole <rol> <jugador>");
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + args[2]);
                return;
            }
        }

        boolean success = plugin.getRoleApi().setRole(target, role);
        if (success) {
            String newRole = plugin.getRoleApi().getRole(target.getUniqueId());
            sender.sendMessage("§aRol cambiado exitosamente.");
            sender.sendMessage("§e" + target.getName() + "§a ahora tiene el rol: §e" + newRole);

            // VERIFICAR SI ES KIRA Y DAR DEATH NOTE INMEDIATAMENTE
            if (role.equalsIgnoreCase("kira")) {
                plugin.getRoleApi().giveDeathNote(target);
                sender.sendMessage("§4Death Note entregada a " + target.getName());
            }

            if (!target.equals(sender)) {
                target.sendMessage("§aTu rol ha sido cambiado a: §e" + newRole);
                if (role.equalsIgnoreCase("kira")) {
                    target.sendMessage("§4¡Has recibido la Death Note!");
                }
            }
        } else {
            sender.sendMessage("§cError al cambiar el rol.");
        }
    }

    private void handleCountRole(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUso: /debug countrole <rol>");
            sender.sendMessage("§7Roles válidos: §e" + String.join("§7, §e", validRoles));
            return;
        }

        String role = args[1].toLowerCase();

        if (!isValidRole(role)) {
            sender.sendMessage("§cRol inválido: " + args[1]);
            sender.sendMessage("§7Roles válidos: §e" + String.join("§7, §e", validRoles));
            return;
        }

        long count = plugin.getRoleApi().countRole(role);
        sender.sendMessage("§aJugadores con rol §e" + role + "§a: §e" + count);
    }

    private void handleListRoles(CommandSender sender) {
        sender.sendMessage("§6=== Conteo de Roles ===");
        int totalAlive = 0;
        int totalOnline = Bukkit.getOnlinePlayers().size();

        for (String role : validRoles) {
            long count = plugin.getRoleApi().countRole(role);

            // Contar jugadores vivos con este rol
            int aliveCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerRole = plugin.getRoleApi().getRole(player.getUniqueId());
                boolean isAlive = plugin.getRoleApi().isAlive(player.getUniqueId());
                if (playerRole.equalsIgnoreCase(role) && isAlive) {
                    aliveCount++;
                    totalAlive++;
                }
            }

            sender.sendMessage("§e" + role + "§7: §a" + count + " §7(§2" + aliveCount + " vivos§7)");
        }

        sender.sendMessage("§6======================");
        sender.sendMessage("§7Total de jugadores online: §e" + totalOnline);
        sender.sendMessage("§7Total de jugadores vivos: §a" + totalAlive);
        sender.sendMessage("§7Total de jugadores muertos: §c" + (totalOnline - totalAlive));
    }

    private void handlePlayerInfo(CommandSender sender, String[] args) {
        Player target;

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEspecifica un jugador: /debug playerinfo <jugador>");
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + args[1]);
                return;
            }
        }

        UUID uuid = target.getUniqueId();
        String role = plugin.getRoleApi().getRole(uuid);
        boolean isAlive = plugin.getRoleApi().isAlive(uuid);

        sender.sendMessage("§6=== Información de " + target.getName() + " ===");
        sender.sendMessage("§eUUID: §7" + uuid);
        sender.sendMessage("§eRol: §a" + role);
        sender.sendMessage("§eEstado: §" + (isAlive ? "aVivo" : "cMuerto"));
        sender.sendMessage("§eHealth: §c" + target.getHealth() + " ❤️");
        sender.sendMessage("§eOnline: §" + (target.isOnline() ? "aSí" : "cNo"));

        // Información de identificaciones
        plugin.getPlayerCache().getPlayerData(uuid).ifPresent(data -> {
            List<String> ids = data.getIdentifications();
            sender.sendMessage("§eIdentificaciones: §a" + ids.size());
            if (!ids.isEmpty()) {
                sender.sendMessage("§7- " + String.join("§7, §7", ids));
            }
        });
    }

    private void handleSetAlive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /debug setalive <true/false> [jugador]");
            return;
        }

        boolean aliveState;
        try {
            aliveState = Boolean.parseBoolean(args[1]);
        } catch (Exception e) {
            sender.sendMessage("§cValor inválido. Usa 'true' o 'false'");
            return;
        }

        Player target;

        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEspecifica un jugador: /debug setalive <true/false> <jugador>");
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + args[2]);
                return;
            }
        }

        // Cambiar estado de vida
        plugin.getPlayerCache().getPlayerData(target.getUniqueId()).ifPresent(data -> {
            data.setAlive(aliveState);
            sender.sendMessage("§aEstado de vida de §e" + target.getName() + "§a cambiado a: §"
                    + (aliveState ? "aVivo" : "cMuerto"));

            if (!target.equals(sender)) {
                target.sendMessage("§aTu estado de vida ha sido cambiado a: §" + (aliveState ? "aVivo" : "cMuerto"));
            }
        });
    }

    private void handleReloadConfig(CommandSender sender) {
        if (!sender.hasPermission("deathnote.reload")) {
            sender.sendMessage("§cNo tienes permiso para recargar la configuración.");
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage("§aConfiguración recargada exitosamente.");
    }

    private void handleGiveDeathNote(CommandSender sender, String[] args) {
        Player target;

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEspecifica un jugador: /debug givedeathnote <jugador>");
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + args[1]);
                return;
            }
        }

        if (plugin.getRoleApi() != null) {
            plugin.getRoleApi().giveDeathNote(target);
            sender.sendMessage("§aDeath Note dada a §e" + target.getName());
            if (!target.equals(sender)) {
                target.sendMessage("§aHas recibido una Death Note.");
            }
        } else {
            sender.sendMessage("§cError: RoleAPI no está inicializado.");
        }
    }

    private void handleCheckCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /debug checkcooldown <deathnote|steal|mello> [jugador]");
            return;
        }

        Player target;
        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEspecifica un jugador: /debug checkcooldown <tipo> <jugador>");
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + args[2]);
                return;
            }
        }

        String type = args[1].toLowerCase();
        switch (type) {
            case "deathnote":
                sender.sendMessage("§eCooldown Death Note de §a" + target.getName() + "§e: §cNo implementado aún");
                break;
            case "steal":
                long stealCooldown = plugin.getPlayerCache().getRemainingStealCooldown(target.getUniqueId());
                sender.sendMessage("§eCooldown de robo de §a" + target.getName() + "§e: §c" + stealCooldown + "s");
                break;
            case "mello":
                sender.sendMessage("§eCooldown Mello de §a" + target.getName() + "§e: §cNo implementado aún");
                break;
            default:
                sender.sendMessage("§cTipo de cooldown inválido. Usa: deathnote, steal o mello");
                break;
        }
    }

    /**
     * Verifica si un rol es válido
     */
    private boolean isValidRole(String role) {
        return validRoles.contains(role.toLowerCase());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("getrole", "setrole", "countrole", "listroles",
                    "playerinfo", "setalive", "reloadconfig", "givedeathnote", "checkcooldown"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "getrole":
                case "playerinfo":
                case "givedeathnote":
                case "setalive":
                    // Completar con nombres de jugadores online
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "setrole":
                case "countrole":
                    // Completar con roles válidos
                    completions.addAll(validRoles);
                    break;
                case "checkcooldown":
                    completions.addAll(Arrays.asList("deathnote", "steal", "mello"));
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "setrole":
                case "setalive":
                    // Completar con nombres de jugadores
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
            }
        }

        return completions;
    }
}