package com.blockycraft.blockyfactions.commands;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;
import com.blockycraft.blockyfactions.lang.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class FactionsCommandManager implements CommandExecutor {
    private final BlockyFactions plugin;
    private final LanguageManager langManager;

    public FactionsCommandManager(BlockyFactions plugin) {
        this.plugin = plugin;
        this.langManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        String lang = "en";
        if (sender instanceof Player) {
            lang = plugin.getGeoIPManager().getPlayerLanguage((Player) sender);
        }

        if (commandName.equals("fac") || commandName.equals("f") || commandName.equals("faccion")) {
            return handleFacCommand(sender, args, lang);
        } else if (commandName.equals("fc") || commandName.equals("cf")) {
            return handleFcCommand(sender, args, lang);
        }
        return false;
    }

    private boolean handleFacCommand(CommandSender sender, String[] args, String lang) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(langManager.get(lang, "error.only-players"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("setbase")) {
            return handleSetBaseCommand(player, lang);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("base")) {
            return handleBaseCommand(player, lang);
        }

        if (args.length == 0) {
            showHelp(player, 1, lang);
            return true;
        }
        try {
            int page = Integer.parseInt(args[0]);
            showHelp(player, page, lang);
            return true;
        } catch (NumberFormatException e) {}

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("criar") || subCommand.equals("create") || subCommand.equals("crear")) {
            if (args.length < 3) {
                player.sendMessage(langManager.get(lang, "usage.criar"));
                return true;
            }
            String tag = args[1];
            String name = args[2];
            if (args.length == 3) {
                plugin.getFactionManager().createFaction(name, tag, player, lang);
            } else {
                String color = args[3];
                plugin.getFactionManager().createFaction(name, tag, player, color, lang);
            }
        } else if (subCommand.equals("rank")) {
            plugin.getFactionManager().reloadAllFactionsNetWorth();
            List<Faction> rankedFactions = plugin.getFactionManager().getRankedFactions();
            if (rankedFactions.isEmpty()) {
                player.sendMessage(langManager.get(lang, "error.ranking-empty"));
                return true;
            }
            player.sendMessage(langManager.get(lang, "rank.header"));
            int rank = 1;
            for (Faction f : rankedFactions) {
                player.sendMessage(langManager.get(lang, "rank.entry", rank, f.getTag(), f.getName(), f.getNetWorth()));
                rank++;
                if (rank > 10) { break; }
            }
            player.sendMessage(langManager.get(lang, "rank.footer"));
        } else if (subCommand.equals("list")) {
            if (args.length == 1) {
                Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getName());
                if (playerFaction == null) {
                    player.sendMessage(langManager.get(lang, "usage.list-other"));
                    return true;
                }
                plugin.getFactionManager().listFactionInfo(player, playerFaction.getName(), lang);
            } else {
                String factionToList = args[1];
                plugin.getFactionManager().listFactionInfo(player, factionToList, lang);
            }
        } else if (subCommand.equals("convidar") || subCommand.equals("invite") || subCommand.equals("invitar")) {
            if (args.length < 2) {
                player.sendMessage(langManager.get(lang, "usage.convidar"));
                return true;
            }
            String targetName = args[1];
            plugin.getFactionManager().invitePlayer(player, targetName, lang);
        } else if (subCommand.equals("entrar") || subCommand.equals("join")) {
            if (args.length < 2) {
                player.sendMessage(langManager.get(lang, "usage.entrar"));
                return true;
            }
            String factionName = args[1];
            plugin.getFactionManager().joinFaction(player, factionName, lang);
        } else if (subCommand.equals("expulsar") || subCommand.equals("kick")) {
            if (args.length < 2) {
                player.sendMessage(langManager.get(lang, "usage.expulsar"));
                return true;
            }
            String targetName = args[1];
            plugin.getFactionManager().kickPlayer(player, targetName, lang);
        } else if (subCommand.equals("promover") || subCommand.equals("promote")) {
            if (args.length < 3) {
                player.sendMessage(langManager.get(lang, "usage.promover"));
                return true;
            }
            String targetName = args[1];
            String rankParam = args[2];
            plugin.getFactionManager().setPlayerRank(player, targetName, rankParam, lang);
        } else if (subCommand.equals("pvp")) {
            if (args.length < 2) {
                player.sendMessage(langManager.get(lang, "usage.pvp"));
                return true;
            }
            plugin.getFactionManager().setFactionPvp(player, args[1], lang);
        } else if (subCommand.equals("tag")) {
            if (args.length < 2) {
                player.sendMessage(langManager.get(lang, "usage.tag"));
                return true;
            }
            plugin.getFactionManager().setFactionTag(player, args[1], lang);
        } else if (subCommand.equals("sair") || subCommand.equals("leave") || subCommand.equals("salir")) {
            plugin.getFactionManager().leaveFaction(player, lang);
        } else {
            player.sendMessage(langManager.get(lang, "error.unknown-command"));
        }
        return true;
    }

    private boolean handleFcCommand(CommandSender sender, String[] args, String lang) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(langManager.get(lang, "error.only-players"));
            return true;
        }
        Player player = (Player) sender;
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getName());
        if (faction == null) {
            player.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§bUse: /fc <message>");
            return true;
        }
        String message = String.join(" ", args);
        String formattedMessage = "§f[§bPrivado§f]§9 " + player.getName() + ":§b " + message;

        Set<String> members = new HashSet<>();
        members.addAll(faction.getMembers());
        members.addAll(faction.getOfficials());
        members.add(faction.getLeader());
        String treasurer = faction.getTreasuryPlayer();
        if (treasurer != null && !treasurer.isEmpty()) {
            members.add(treasurer);
        }

        boolean sent = false;
        for (String nick : members) {
            Player p = plugin.getServer().getPlayer(nick);
            if (p != null && p.isOnline()) {
                p.sendMessage(formattedMessage);
                sent = true;
            }
        }
        if (!sent) {
            player.sendMessage("§cNenhum outro membro da sua faccao esta online para receber a mensagem.");
        }
        return true;
    }

    private boolean handleSetBaseCommand(Player player, String lang) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getName());
        if (faction == null || !faction.getLeader().equalsIgnoreCase(player.getName())) {
            player.sendMessage("§cApenas o líder da facção pode definir a base.");
            return true;
        }
        Location loc = player.getLocation();
        String baseLocString = String.format("%s;%f;%f;%f;%f;%f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        plugin.getFactionManager().setFactionBase(faction, baseLocString);
        player.sendMessage(langManager.get(lang, "success.base-set"));
        return true;
    }

    private boolean handleBaseCommand(Player player, String lang) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getName());
        if (faction == null) {
            player.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return true;
        }
        String baseLocString = faction.getBaseLocation();
        if (baseLocString == null || baseLocString.isEmpty()) {
            player.sendMessage(langManager.get(lang, "error.no-base-defined"));
            return true;
        }
        long now = System.currentTimeMillis();
        Long lastDamage = plugin.getTeleportCooldowns().get(player.getName().toLowerCase());
        if (lastDamage != null && (now - lastDamage) < 30_000) {
            long seconds = (30_000 - (now - lastDamage)) / 1000;
            player.sendMessage(langManager.get(lang, "error.teleport-cooldown", seconds));
            return true;
        }
        Location loc = stringToLocation(baseLocString);
        player.teleport(loc);
        player.sendMessage(langManager.get(lang, "success.teleport-base"));
        return true;
    }

    private Location stringToLocation(String baseLocString) {
        String[] parts = baseLocString.split(";");
        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void showHelp(Player player, int page, String lang) {
        switch (page) {
            case 1:
                player.sendMessage(langManager.get(lang, "help.page1.header"));
                player.sendMessage(langManager.get(lang, "help.page1.criar"));
                player.sendMessage(langManager.get(lang, "help.page1.fc"));
                player.sendMessage(langManager.get(lang, "help.page1.sair"));
                player.sendMessage(langManager.get(lang, "help.page1.entrar"));
                player.sendMessage(langManager.get(lang, "help.page1.list"));
                player.sendMessage(langManager.get(lang, "help.page1.rank"));
                player.sendMessage(langManager.get(lang, "help.page1.base"));
                player.sendMessage(langManager.get(lang, "help.page1.footer"));
                break;
            case 2:
                player.sendMessage(langManager.get(lang, "help.page2.header"));
                player.sendMessage(langManager.get(lang, "help.page2.setbase"));
                player.sendMessage(langManager.get(lang, "help.page2.convidar"));
                player.sendMessage(langManager.get(lang, "help.page2.expulsar"));
                player.sendMessage(langManager.get(lang, "help.page2.section-leader"));
                player.sendMessage(langManager.get(lang, "help.page2.promover"));
                player.sendMessage(langManager.get(lang, "help.page2.promover-info"));
                player.sendMessage(langManager.get(lang, "help.page2.tag"));
                player.sendMessage(langManager.get(lang, "help.page2.pvp"));
                player.sendMessage(langManager.get(lang, "help.page2.footer"));
                break;
            default:
                player.sendMessage(langManager.get(lang, "error.page-not-found"));
                break;
        }
    }
}
