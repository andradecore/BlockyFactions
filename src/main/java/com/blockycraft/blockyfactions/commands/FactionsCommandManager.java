package com.blockycraft.blockyfactions.commands;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

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
        
        } else if (subCommand.equals("list")) {
            if (args.length == 1) {
                Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getName());
                if (playerFaction == null) {
                    player.sendMessage("§cVoce nao esta em uma faccao. Use /fac list <nome-da-faccao> para ver informacoes de outra faccao.");
                    return true;
                }
                plugin.getFactionManager().listFactionInfo(player, playerFaction.getName());
            } else {
                String factionToList = args[1];
                plugin.getFactionManager().listFactionInfo(player, factionToList);
            }

        } else if (subCommand.equals("expulsar")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /fac expulsar <jogador>");
                return true;
            }
            String targetName = args[1];
            plugin.getFactionManager().kickPlayer(player, targetName);

        } else if (subCommand.equals("promover")) {
            if (args.length < 3) {
                player.sendMessage("§cUse: /fac promover <jogador> <oficial|membro>");
                return true;
            }
            String targetName = args[1];
            String rank = args[2];
            plugin.getFactionManager().setPlayerRank(player, targetName, rank);
        
        } else if (subCommand.equals("pvp")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /fac pvp <on|off>");
                return true;
            }
            plugin.getFactionManager().setFactionPvp(player, args[1]);

        } else if (subCommand.equals("fundo")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /fac fundo <jogador|nenhum>");
                return true;
            }
            plugin.getFactionManager().setTreasuryPlayer(player, args[1]);

        } else if (subCommand.equals("tag")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /fac tag <nova-tag>");
                return true;
            }
            plugin.getFactionManager().setFactionTag(player, args[1]);

        } else if (subCommand.equals("lider")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /fac lider <jogador>");
                return true;
            }
            plugin.getFactionManager().transferLeadership(player, args[1]);

        } else if (subCommand.equals("rank")) {
            List<Faction> rankedFactions = plugin.getFactionManager().getRankedFactions();
            
            if (rankedFactions.isEmpty()) {
                player.sendMessage("§cO ranking de faccoes esta vazio.");
                return true;
            }
            
            player.sendMessage("§e--- Ranking de Faccoes (por Patrimonio) ---");
            int rank = 1;
            for (Faction faction : rankedFactions) {
                player.sendMessage("§b#" + rank + ". §7[" + faction.getTag() + "] §f" + faction.getName() + " §e- " + faction.getNetWorth() + " barras");
                rank++;
                if (rank > 10) {
                    break;
                }
            }
            player.sendMessage("§e------------------------------------");

        } else if (subCommand.equals("sair")) {
            plugin.getFactionManager().leaveFaction(player);

        } else {
            player.sendMessage("§cComando desconhecido. Use /fac ajuda para ver a lista de comandos.");
        }

        return true;
    }
}