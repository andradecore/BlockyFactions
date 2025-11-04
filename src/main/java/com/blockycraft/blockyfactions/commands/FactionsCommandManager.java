package com.blockycraft.blockyfactions.commands;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.config.ConfigManager;
import com.blockycraft.blockyfactions.data.Faction;
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
    private final ConfigManager config;

    // Mapa <player, timestamp> para cooldown de dano de teleporte
    private final Map<String, Long> teleportCooldowns = new HashMap<>();

    public FactionsCommandManager(BlockyFactions plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // --- Comando de SETBASE ---
        if (label.equalsIgnoreCase("fac") && args.length > 0 && args[0].equalsIgnoreCase("setbase")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(config.getMessage("error.only-players"));
                return true;
            }
            Player player = (Player) sender;
            Faction faction = plugin.getFactionManager().getPlayerFaction(player.getName());

            if (faction == null || !faction.getLeader().equalsIgnoreCase(player.getName())) {
                player.sendMessage("§cApenas o líder da facção pode definir a base.");
                return true;
            }

            Location loc = player.getLocation();
            String baseLocString = String.format("%s;%f;%f;%f;%f;%f",
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            plugin.getFactionManager().setFactionBase(faction, baseLocString);
            player.sendMessage(config.getMessage("success.base-set"));
            return true;
        }

        // --- Comando de TELEPORTE PARA BASE ---
        if (label.equalsIgnoreCase("fac") && args.length > 0 && args[0].equalsIgnoreCase("base")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(config.getMessage("error.only-players"));
                return true;
            }
            Player player = (Player) sender;
            Faction faction = plugin.getFactionManager().getPlayerFaction(player.getName());

            if (faction == null) {
                player.sendMessage("§cVocê não pertence a nenhuma facção.");
                return true;
            }
            String baseLocString = faction.getBaseLocation();
            if (baseLocString == null || baseLocString.isEmpty()) {
                player.sendMessage("§cSua facção não possui uma base definida.");
                return true;
            }
            // Checa cooldown de dano de jogador
            long now = System.currentTimeMillis();
            Long lastDamage = teleportCooldowns.get(player.getName().toLowerCase());
            if (lastDamage != null && (now - lastDamage) < 30_000) {
                long segundos = (30_000 - (now - lastDamage)) / 1000;
                player.sendMessage("§cVocê recebeu dano recentemente. Aguarde " + segundos + " segundos para teletransportar.");
                return true;
            }
            Location loc = stringToLocation(baseLocString);
            player.teleport(loc);
            player.sendMessage("§bVoce foi teletransportado para a base da sua faccao.");
            return true;
        }

        // --- COMANDO /fc PRIVADO ---
        if (label.equalsIgnoreCase("fc")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(config.getMessage("error.only-players"));
                return true;
            }
            Player player = (Player) sender;
            Faction faction = plugin.getFactionManager().getPlayerFaction(player.getName());
            if (faction == null) {
                player.sendMessage("§cVoce nao pertence a nenhuma faccao.");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage("§bUse: /fc ");
                return true;
            }
            String mensagem = String.join(" ", args);
            String formatada = "§f[§bPrivado§f]§9 " + player.getName() + ":§b " + mensagem;
            // Junta membros, oficiais, líder, tesoureiro
            Set<String> membros = new HashSet<>();
            membros.addAll(faction.getMembers());
            membros.addAll(faction.getOfficials());
            membros.add(faction.getLeader());
            String tesoureiro = faction.getTreasuryPlayer();
            if (tesoureiro != null && !tesoureiro.isEmpty()) {
                membros.add(tesoureiro);
            }
            boolean enviado = false;
            for (String nick : membros) {
                Player p = plugin.getServer().getPlayer(nick);
                if (p != null && p.isOnline()) {
                    p.sendMessage(formatada);
                    enviado = true;
                }
            }
            if (!enviado) {
                player.sendMessage("§cNenhum outro membro da sua faccao esta online para receber a mensagem.");
            }
            return true;
        }

        // --- DEMAIS SUBCOMANDOS ---
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
        } catch (NumberFormatException e) {}
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
            for (Faction f : rankedFactions) {
                player.sendMessage(config.getRankMessage("entry", rank, f.getTag(), f.getName(), f.getNetWorth()));
                rank++;
                if (rank > 10) { break; }
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
            String rankParam = args[2];
            plugin.getFactionManager().setPlayerRank(player, targetName, rankParam);
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

    // Utilitário para converter format string location -> Location
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

    // Listener para registrar dano de outros jogadores
    public void setLastDamage(String playerName) {
        teleportCooldowns.put(playerName.toLowerCase(), System.currentTimeMillis());
    }

    private void showHelp(Player player, int page) {
        switch (page) {
            case 1:
                player.sendMessage(config.getHelpMessage("page1.header"));
                player.sendMessage(config.getHelpMessage("page1.help"));
                player.sendMessage(config.getHelpMessage("page1.criar"));
                player.sendMessage(config.getHelpMessage("page1.fc"));
                player.sendMessage(config.getHelpMessage("page1.sair"));
                player.sendMessage(config.getHelpMessage("page1.entrar"));
                player.sendMessage(config.getHelpMessage("page1.list"));
                player.sendMessage(config.getHelpMessage("page1.rank"));
                player.sendMessage(config.getHelpMessage("page1.base"));
                player.sendMessage(config.getHelpMessage("page1.footer"));
                break;
            case 2:
                player.sendMessage(config.getHelpMessage("page2.header"));
                player.sendMessage(config.getHelpMessage("page2.section-officer"));
                player.sendMessage(config.getHelpMessage("page2.setbase"));
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
