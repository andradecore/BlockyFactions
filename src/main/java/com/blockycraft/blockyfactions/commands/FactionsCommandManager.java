package com.blockycraft.blockyfactions.commands;

import com.blockycraft.blockyfactions.BlockyFactions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FactionsCommandManager implements CommandExecutor {

    private final BlockyFactions plugin;

    public FactionsCommandManager(BlockyFactions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando so pode ser utilizado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // TODO: Mostrar mensagem de ajuda principal
            player.sendMessage("§eUse /fac <comando> - Veja a lista de comandos com /fac ajuda");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("criar")) {
            if (args.length < 3) {
                player.sendMessage("§cUse: /fac criar <tag> <nome-da-faccao>");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String tag = args[1];
            String name = sb.toString().trim();
            plugin.getFactionManager().createFaction(name, tag, player);
            
        } else if (subCommand.equals("convidar")) {
             if (args.length < 2) {
                player.sendMessage("§cUse: /fac convidar <jogador>");
                return true;
            }
            String targetName = args[1];
            plugin.getFactionManager().invitePlayer(player, targetName);

        } else if (subCommand.equals("entrar")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /fac entrar <nome-da-faccao>");
                return true;
            }
            String factionName = args[1];
            plugin.getFactionManager().joinFaction(player, factionName);
        
        } else if (subCommand.equals("sair")) {
            plugin.getFactionManager().leaveFaction(player);

        } else {
            player.sendMessage("§cComando desconhecido. Use /fac ajuda para ver a lista de comandos.");
        }

        return true;
    }
}