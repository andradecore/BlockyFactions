package com.blockycraft.blockyfactions.commands;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.config.ConfigManager;
import com.blockycraft.blockyfactions.data.Faction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class FactionsCommandManager implements CommandExecutor {
    
    private final BlockyFactions plugin;
    private final ConfigManager config;
    
    public FactionsCommandManager(BlockyFactions plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("error.only-players"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player, 1);
            return true;
        }
        
        try {
            int page = Integer.parseInt(args[0]);
            showHelp(player, page);
            return true;
        } catch (NumberFormatException e) {
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("criar")) {
            if (args.length < 3) {
                player.sendMessage(config.getMessage("usage.criar"));
                return true;
            }
            String tag = args[1];
            String name = args[2];
            
            if (args.length == 3) {
                plugin.getFactionManager().createFaction(name, tag, player);
            } else {
                String color = args[3];
                plugin.getFactionManager().createFaction(name, tag, player, color);
            }
        } else if (subCommand.equals("rank")) {
            plugin.getFactionManager().reloadAllFactionsNetWorth();
            List<Faction> rankedFactions = plugin.getFactionManager().getRankedFactions();
            
            if (rankedFactions.isEmpty()) {
                player.sendMessage(config.getMessage("error.ranking-empty"));
                return true;
            }
            
            player.sendMessage(config.getRankMessage("header"));
            int rank = 1;
            for (Faction faction : rankedFactions) {
                player.sendMessage(config.getRankMessage("entry", rank, faction.getTag(), faction.getName(), faction.getNetWorth()));
                rank++;
                if (rank > 10) {
                    break;
                }
            }
            player.sendMessage(config.getRankMessage("footer"));
        } else if (subCommand.equals("list")) {
            if (args.length == 1) {
                Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getName());
                if (playerFaction == null) {
                    player.sendMessage(config.getMessage("usage.list-other"));
                    return true;
                }
                plugin.getFactionManager().listFactionInfo(player, playerFaction.getName());
            } else {
                String factionToList = args[1];
                plugin.getFactionManager().listFactionInfo(player, factionToList);
            }
        } else if (subCommand.equals("convidar")) {
            if (args.length < 2) {
                player.sendMessage(config.getMessage("usage.convidar"));
                return true;
            }
            String targetName = args[1];
            plugin.getFactionManager().invitePlayer(player, targetName);
        } else if (subCommand.equals("entrar")) {
            if (args.length < 2) {
                player.sendMessage(config.getMessage("usage.entrar"));
                return true;
            }
            String factionName = args[1];
            plugin.getFactionManager().joinFaction(player, factionName);
        } else if (subCommand.equals("expulsar")) {
            if (args.length < 2) {
                player.sendMessage(config.getMessage("usage.expulsar"));
                return true;
            }
            String targetName = args[1];
            plugin.getFactionManager().kickPlayer(player, targetName);
        } else if (subCommand.equals("promover")) {
            if (args.length < 3) {
                player.sendMessage(config.getMessage("usage.promover"));
                return true;
            }
            String targetName = args[1];
            String rank = args[2];
            plugin.getFactionManager().setPlayerRank(player, targetName, rank);
        } else if (subCommand.equals("pvp")) {
            if (args.length < 2) {
                player.sendMessage(config.getMessage("usage.pvp"));
                return true;
            }
            plugin.getFactionManager().setFactionPvp(player, args[1]);
        } else if (subCommand.equals("tag")) {
            if (args.length < 2) {
                player.sendMessage(config.getMessage("usage.tag"));
                return true;
            }
            plugin.getFactionManager().setFactionTag(player, args[1]);
        } else if (subCommand.equals("sair")) {
            plugin.getFactionManager().leaveFaction(player);
        } else {
            player.sendMessage(config.getMessage("error.unknown-command"));
        }
        
        return true;
    }
    
    private void showHelp(Player player, int page) {
        switch (page) {
            case 1:
                player.sendMessage(config.getHelpMessage("page1.header"));
                player.sendMessage(config.getHelpMessage("page1.help"));
                player.sendMessage(config.getHelpMessage("page1.criar"));
                player.sendMessage(config.getHelpMessage("page1.sair"));
                player.sendMessage(config.getHelpMessage("page1.entrar"));
                player.sendMessage(config.getHelpMessage("page1.list"));
                player.sendMessage(config.getHelpMessage("page1.rank"));
                player.sendMessage(config.getHelpMessage("page1.footer"));
                break;
            case 2:
                player.sendMessage(config.getHelpMessage("page2.header"));
                player.sendMessage(config.getHelpMessage("page2.section-officer"));
                player.sendMessage(config.getHelpMessage("page2.convidar"));
                player.sendMessage(config.getHelpMessage("page2.expulsar"));
                player.sendMessage(config.getHelpMessage("page2.section-leader"));
                player.sendMessage(config.getHelpMessage("page2.promover"));
                player.sendMessage(config.getHelpMessage("page2.promover-info"));
                player.sendMessage(config.getHelpMessage("page2.tag"));
                player.sendMessage(config.getHelpMessage("page2.pvp"));
                player.sendMessage(config.getHelpMessage("page2.footer"));
                break;
            default:
                player.sendMessage(config.getMessage("error.page-not-found"));
                break;
        }
    }
}
