package com.blockycraft.blockyfactions.managers;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;

import org.bukkit.util.config.Configuration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactionManager {

    private final BlockyFactions plugin;
    private final Map<String, Faction> factions = new HashMap<String, Faction>();
    private final Map<String, String> playerToFactionMap = new HashMap<String, String>();
    private final Map<String, String> pendingInvites = new HashMap<String, String>(); // Key: Nome do Convidado, Value: Nome da Facçao

    public FactionManager(BlockyFactions plugin) {
        this.plugin = plugin;
    }

    public boolean createFaction(String name, String tag, Player leader) {
        if (factions.containsKey(name.toLowerCase()) || isTagInUse(tag)) {
            leader.sendMessage("§cUma faccao com este nome ou tag ja existe.");
            return false;
        }

        if (playerToFactionMap.containsKey(leader.getName().toLowerCase())) {
            leader.sendMessage("§cVoce ja pertence a uma faccao. Saia da sua faccao atual para criar uma nova.");
            return false;
        }

        if (name.length() > 20 || tag.length() > 5 || !name.matches("^[a-zA-Z0-9]+$") || !tag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage("§cNome ou tag invalidos. Use apenas letras e numeros, sem espacos. Maximo de 20 caracteres para o nome e 5 para a tag.");
            return false;
        }

        Faction newFaction = new Faction(name, tag, leader.getName());
        
        factions.put(name.toLowerCase(), newFaction);
        playerToFactionMap.put(leader.getName().toLowerCase(), name);
        
        saveFactionToFile(newFaction);

        leader.sendMessage("§aFaccao '" + name + "' criada com sucesso!");
        return true;
    }
    
    public void leaveFaction(Player player) {
        Faction faction = getPlayerFaction(player.getName());

        if (faction == null) {
            player.sendMessage("§cVoce nao pertence a nenhuma faccao.");
            return;
        }

        String playerName = player.getName();

        if (faction.getLeader().equalsIgnoreCase(playerName)) {
            dissolveFaction(faction, player);
        } else {
            faction.removeMember(playerName);
            playerToFactionMap.remove(playerName.toLowerCase());
            
            saveFactionToFile(faction);
            
            player.sendMessage("§aVoce saiu da faccao " + faction.getName() + ".");
            Player leader = plugin.getServer().getPlayer(faction.getLeader());
            if (leader != null && leader.isOnline()) {
                leader.sendMessage("§eO jogador " + playerName + " saiu da sua faccao.");
            }
        }
    }

    public void invitePlayer(Player inviter, String targetName) {
        Faction faction = getPlayerFaction(inviter.getName());

        if (faction == null) {
            inviter.sendMessage("§cVoce nao esta em uma faccao para poder convidar alguem.");
            return;
        }

        String inviterName = inviter.getName().toLowerCase();
        if (!faction.getLeader().equalsIgnoreCase(inviterName) && !faction.getOfficials().contains(inviterName)) {
            inviter.sendMessage("§cVoce precisa ser o lider ou um oficial para convidar jogadores.");
            return;
        }
        
        if (getPlayerFaction(targetName) != null) {
            inviter.sendMessage("§cO jogador " + targetName + " ja pertence a uma faccao.");
            return;
        }

        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            inviter.sendMessage("§cO jogador " + targetName + " nao esta online.");
            return;
        }

        pendingInvites.put(target.getName().toLowerCase(), faction.getName());

        inviter.sendMessage("§aVoce convidou " + target.getName() + " para a sua faccao.");
        target.sendMessage("§eVoce foi convidado para entrar na faccao " + faction.getName() + ".");
        target.sendMessage("§eDigite §b/fac entrar " + faction.getName() + " §epara aceitar.");
    }

    public void joinFaction(Player player, String factionName) {
        String playerName = player.getName().toLowerCase();

        if (getPlayerFaction(playerName) != null) {
            player.sendMessage("§cVoce ja esta em uma faccao.");
            return;
        }

        String invitedToFaction = pendingInvites.get(playerName);
        if (invitedToFaction == null || !invitedToFaction.equalsIgnoreCase(factionName)) {
            player.sendMessage("§cVoce nao tem um convite para a faccao " + factionName + ".");
            return;
        }
        
        Faction faction = getFactionByName(invitedToFaction);
        if (faction == null) {
            player.sendMessage("§cEsta faccao nao existe mais.");
            pendingInvites.remove(playerName);
            return;
        }
        
        pendingInvites.remove(playerName);
        faction.addMember(player.getName());
        playerToFactionMap.put(playerName, faction.getName());

        saveFactionToFile(faction);

        player.sendMessage("§aVoce entrou na faccao " + faction.getName() + "!");

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName())) {
                onlinePlayer.sendMessage("§e" + player.getName() + " entrou para a faccao!");
            }
        }
    }

    private void dissolveFaction(Faction faction, Player leader) {
        factions.remove(faction.getName().toLowerCase());
        
        playerToFactionMap.remove(leader.getName().toLowerCase());
        for (String official : faction.getOfficials()) {
            playerToFactionMap.remove(official.toLowerCase());
        }
        for (String member : faction.getMembers()) {
            playerToFactionMap.remove(member.toLowerCase());
        }

        File factionFile = new File(plugin.getDataFolder() + "/factions", faction.getName().toLowerCase() + ".yml");
        if (factionFile.exists()) {
            factionFile.delete();
        }

        leader.sendMessage("§cComo lider, voce dissolveu a faccao " + faction.getName() + ".");
    }

    private boolean isTagInUse(String tag) {
        for (Faction faction : factions.values()) {
            if (faction.getTag().equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }
    
    public void saveFactionToFile(Faction faction) {
        File factionFile = new File(plugin.getDataFolder() + "/factions", faction.getName().toLowerCase() + ".yml");
        Configuration factionConfig = new Configuration(factionFile);

        factionConfig.setProperty("nome", faction.getName());
        factionConfig.setProperty("tag", faction.getTag());
        factionConfig.setProperty("lider", faction.getLeader());
        factionConfig.setProperty("oficiais", faction.getOfficials());
        factionConfig.setProperty("membros", faction.getMembers());
        factionConfig.setProperty("fundo", faction.getTreasuryPlayer());
        factionConfig.setProperty("net_worth", faction.getNetWorth());
        factionConfig.setProperty("pvp-habilitado", faction.isPvpEnabled());

        factionConfig.save();
    }

    public void loadFactions() {
        File factionsDir = new File(plugin.getDataFolder(), "factions");
        if (!factionsDir.exists()) {
            factionsDir.mkdirs();
            return;
        }

        File[] files = factionsDir.listFiles();
        if (files == null) return;

        for (File factionFile : files) {
            if (factionFile.getName().endsWith(".yml")) {
                Configuration factionConfig = new Configuration(factionFile);
                factionConfig.load();

                String name = factionConfig.getString("nome");
                String tag = factionConfig.getString("tag");
                String leader = factionConfig.getString("lider");

                if (name == null || tag == null || leader == null) continue;

                Faction faction = new Faction(name, tag, leader);
                
                faction.getOfficials().addAll(factionConfig.getStringList("oficiais", new ArrayList<String>()));
                faction.getMembers().addAll(factionConfig.getStringList("membros", new ArrayList<String>()));
                faction.setTreasuryPlayer(factionConfig.getString("fundo", ""));
                faction.setNetWorth(factionConfig.getDouble("net_worth", 0.0));
                faction.setPvpEnabled(factionConfig.getBoolean("pvp-habilitado", false));

                factions.put(name.toLowerCase(), faction);
                playerToFactionMap.put(leader.toLowerCase(), name);
                for (String official : faction.getOfficials()) {
                    playerToFactionMap.put(official.toLowerCase(), name);
                }
                for (String member : faction.getMembers()) {
                    playerToFactionMap.put(member.toLowerCase(), name);
                }
            }
        }
        System.out.println("[BlockyFactions] " + factions.size() + " faccoes carregadas.");
    }

    public void saveFactions() {
        for (Faction faction : factions.values()) {
            saveFactionToFile(faction);
        }
        System.out.println("[BlockyFactions] Todas as " + factions.size() + " faccoes foram salvas.");
    }

    public Faction getFactionByName(String name) {
        return factions.get(name.toLowerCase());
    }

    public Faction getPlayerFaction(String playerName) {
        String factionName = playerToFactionMap.get(playerName.toLowerCase());
        if (factionName != null) {
            return getFactionByName(factionName);
        }
        return null;
    }
}