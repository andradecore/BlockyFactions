package com.blockycraft.blockyfactions.managers;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.config.ConfigManager;
import com.blockycraft.blockyfactions.data.Faction;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FactionManager {
    
    private final BlockyFactions plugin;
    private final ConfigManager config;
    private final Map<String, Faction> factions = new HashMap<String, Faction>();
    private final Map<String, String> playerToFactionMap = new HashMap<String, String>();
    private final Map<String, String> pendingInvites = new HashMap<String, String>();
    
    public FactionManager(BlockyFactions plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }
    
    // --- LÓGICA DE CRIAÇÃO ATUALIZADA ---
    
    public boolean createFaction(String name, String tag, Player leader) {
        String randomColor = generateRandomHexColor();
        return createFactionInternal(name, tag, leader, randomColor);
    }
    
    public boolean createFaction(String name, String tag, Player leader, String colorHex) {
        if (!isValidHexCode(colorHex)) {
            leader.sendMessage(config.getMessage("error.invalid-color", colorHex));
            return false;
        }
        return createFactionInternal(name, tag, leader, colorHex);
    }
    
    private boolean createFactionInternal(String name, String tag, Player leader, String colorHex) {
        if (factions.containsKey(name.toLowerCase()) || isTagInUse(tag)) {
            leader.sendMessage(config.getMessage("error.faction-exists"));
            return false;
        }
        
        if (playerToFactionMap.containsKey(leader.getName().toLowerCase())) {
            leader.sendMessage(config.getMessage("error.already-in-faction"));
            return false;
        }
        
        if (name.length() > 20 || tag.length() > 5 || !name.matches("^[a-zA-Z0-9]+$") || !tag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage(config.getMessage("error.invalid-name-tag"));
            return false;
        }
        
        Faction newFaction = new Faction(name, tag, leader.getName());
        newFaction.setColorHex(colorHex.toUpperCase());
        
        factions.put(name.toLowerCase(), newFaction);
        playerToFactionMap.put(leader.getName().toLowerCase(), name);
        
        saveFactionToFile(newFaction);
        leader.sendMessage(config.getMessage("success.faction-created", name, colorHex.toUpperCase()));
        return true;
    }
    
    private String generateRandomHexColor() {
        Random random = new Random();
        int nextInt = random.nextInt(0xFFFFFF + 1);
        return String.format("#%06X", nextInt);
    }
    
    private boolean isValidHexCode(String code) {
        if (code == null) return false;
        return code.matches("^#[a-fA-F0-9]{6}$");
    }
    
    public void leaveFaction(Player player) {
        Faction faction = getPlayerFaction(player.getName());
        if (faction == null) {
            player.sendMessage(config.getMessage("error.not-in-faction"));
            return;
        }
        
        String playerName = player.getName();
        
        if (faction.getLeader().equalsIgnoreCase(playerName)) {
            List<String> officials = faction.getOfficials();
            if (officials.isEmpty()) {
                dissolveFaction(faction, player);
            } else {
                String newLeaderName = officials.get(0);
                faction.setLeader(newLeaderName);
                faction.getOfficials().remove(newLeaderName.toLowerCase());
                playerToFactionMap.remove(playerName.toLowerCase());
                
                saveFactionToFile(faction);
                
                player.sendMessage(config.getMessage("success.left-leadership", faction.getName(), newLeaderName));
                
                Player newLeader = plugin.getServer().getPlayer(newLeaderName);
                if (newLeader != null && newLeader.isOnline()) {
                    newLeader.sendMessage(config.getMessage("notification.new-leader-promoted", faction.getName()));
                }
                
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    if (faction.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(newLeaderName)) {
                        onlinePlayer.sendMessage(config.getMessage("notification.leader-left", playerName, newLeaderName));
                    }
                }
            }
        } else {
            faction.removeMember(playerName);
            playerToFactionMap.remove(playerName.toLowerCase());
            saveFactionToFile(faction);
            
            player.sendMessage(config.getMessage("success.left-faction", faction.getName()));
            
            Player leader = plugin.getServer().getPlayer(faction.getLeader());
            if (leader != null && leader.isOnline()) {
                leader.sendMessage(config.getMessage("notification.player-left", playerName));
            }
        }
    }
    
    public List<Faction> getRankedFactions() {
        List<Faction> factionsList = new ArrayList<Faction>(factions.values());
        factionsList.sort(new Comparator<Faction>() {
            @Override
            public int compare(Faction f1, Faction f2) {
                return Double.compare(f2.getNetWorth(), f1.getNetWorth());
            }
        });
        return factionsList;
    }
    
    public void reloadFactionNetWorth(Faction faction) {
        if (faction == null) return;
        
        File factionFile = new File(plugin.getDataFolder() + "/factions", faction.getName().toLowerCase() + ".yml");
        if (factionFile.exists()) {
            Configuration factionConfig = new Configuration(factionFile);
            factionConfig.load();
            double newNetWorth = factionConfig.getDouble("net_worth", 0.0);
            faction.setNetWorth(newNetWorth);
        }
    }
    
    public void reloadAllFactionsNetWorth() {
        for (Faction faction : factions.values()) {
            reloadFactionNetWorth(faction);
        }
    }
    
    public void invitePlayer(Player inviter, String targetName) {
        Faction faction = getPlayerFaction(inviter.getName());
        if (faction == null) {
            inviter.sendMessage(config.getMessage("error.not-in-faction"));
            return;
        }
        
        if (faction.getSize() >= config.getMaxMembers()) {
            inviter.sendMessage(config.getMessage("error.faction-full", config.getMaxMembers()));
            return;
        }
        
        if (!faction.isLeaderOrOfficer(inviter.getName())) {
            inviter.sendMessage(config.getMessage("error.no-permission-invite"));
            return;
        }
        
        if (getPlayerFaction(targetName) != null) {
            inviter.sendMessage(config.getMessage("error.player-already-in-faction", targetName));
            return;
        }
        
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            inviter.sendMessage(config.getMessage("error.player-not-online", targetName));
            return;
        }
        
        pendingInvites.put(target.getName().toLowerCase(), faction.getName());
        inviter.sendMessage(config.getMessage("success.invited", target.getName()));
        target.sendMessage(config.getMessage("notification.invited-to-faction", faction.getName()));
        target.sendMessage(config.getMessage("notification.invite-instruction", faction.getName()));
    }
    
    public void joinFaction(Player player, String factionName) {
        String playerName = player.getName().toLowerCase();
        
        if (getPlayerFaction(playerName) != null) {
            player.sendMessage(config.getMessage("error.already-in-faction"));
            return;
        }
        
        String invitedToFaction = pendingInvites.get(playerName);
        if (invitedToFaction == null || !invitedToFaction.equalsIgnoreCase(factionName)) {
            player.sendMessage(config.getMessage("error.no-invite", factionName));
            return;
        }
        
        Faction faction = getFactionByName(invitedToFaction);
        if (faction == null) {
            player.sendMessage(config.getMessage("error.faction-disbanded"));
            pendingInvites.remove(playerName);
            return;
        }
        
        if (faction.getSize() >= config.getMaxMembers()) {
            player.sendMessage(config.getMessage("error.faction-full-join", faction.getName()));
            pendingInvites.remove(playerName);
            return;
        }
        
        pendingInvites.remove(playerName);
        faction.addMember(player.getName());
        playerToFactionMap.put(playerName, faction.getName());
        saveFactionToFile(faction);
        
        player.sendMessage(config.getMessage("success.joined", faction.getName()));
        
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName())) {
                onlinePlayer.sendMessage(config.getMessage("notification.player-joined", player.getName()));
            }
        }
    }
    
    public void listFactionInfo(Player viewer, String factionName) {
        Faction faction = getFactionByName(factionName);
        if (faction == null) {
            viewer.sendMessage(config.getMessage("error.faction-not-found", factionName));
            return;
        }
        
        reloadFactionNetWorth(faction);
        
        viewer.sendMessage(config.getInfoMessage("header"));
        viewer.sendMessage(config.getInfoMessage("name", faction.getName(), faction.getTag()));
        viewer.sendMessage(config.getInfoMessage("color", faction.getColorHex()));
        viewer.sendMessage(config.getInfoMessage("leader", faction.getLeader()));
        
        String treasuryPlayer = faction.getTreasuryPlayer();
        if (treasuryPlayer == null || treasuryPlayer.isEmpty()) {
            viewer.sendMessage(config.getInfoMessage("treasurer-none"));
        } else {
            viewer.sendMessage(config.getInfoMessage("treasurer", treasuryPlayer));
        }
        
        viewer.sendMessage(config.getInfoMessage("networth", faction.getNetWorth()));
        viewer.sendMessage(faction.isPvpEnabled() ? config.getInfoMessage("pvp-enabled") : config.getInfoMessage("pvp-disabled"));
        
        String officials = String.join(", ", faction.getOfficials());
        if (officials.isEmpty()) { officials = config.getInfoMessage("none"); }
        viewer.sendMessage(config.getInfoMessage("officials", faction.getOfficials().size(), officials));
        
        ArrayList<String> plainMembers = new ArrayList<String>(faction.getMembers());
        String members = String.join(", ", plainMembers);
        if (members.isEmpty()) { members = config.getInfoMessage("none"); }
        viewer.sendMessage(config.getInfoMessage("members", plainMembers.size(), members));
        
        viewer.sendMessage(config.getInfoMessage("total-members", faction.getSize(), config.getMaxMembers()));
        viewer.sendMessage(config.getInfoMessage("footer"));
    }
    
    public void kickPlayer(Player kicker, String targetName) {
        Faction faction = getPlayerFaction(kicker.getName());
        if (faction == null) {
            kicker.sendMessage(config.getMessage("error.not-in-faction"));
            return;
        }
        
        if (!faction.isLeaderOrOfficer(kicker.getName())) {
            kicker.sendMessage(config.getMessage("error.no-permission-kick"));
            return;
        }
        
        Faction targetFaction = getPlayerFaction(targetName);
        if (targetFaction == null || !targetFaction.getName().equals(faction.getName())) {
            kicker.sendMessage(config.getMessage("error.player-not-in-your-faction", targetName));
            return;
        }
        
        String targetNameLower = targetName.toLowerCase();
        if (faction.getLeader().equalsIgnoreCase(targetNameLower)) {
            kicker.sendMessage(config.getMessage("error.cannot-kick-leader"));
            return;
        }
        
        if (faction.getOfficials().contains(targetNameLower) && !faction.getLeader().equalsIgnoreCase(kicker.getName())) {
            kicker.sendMessage(config.getMessage("error.cannot-kick-officer"));
            return;
        }
        
        faction.removeMember(targetName);
        playerToFactionMap.remove(targetNameLower);
        saveFactionToFile(faction);
        
        kicker.sendMessage(config.getMessage("success.kicked", targetName));
        
        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(config.getMessage("error.player-not-in-your-faction", faction.getName()));
        }
        
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName())) {
                onlinePlayer.sendMessage(config.getMessage("notification.player-kicked", targetName, kicker.getName()));
            }
        }
    }
    
    public void setPlayerRank(Player promoter, String targetName, String rank) {
        Faction faction = getPlayerFaction(promoter.getName());
        if (faction == null) {
            promoter.sendMessage(config.getMessage("error.not-in-faction"));
            return;
        }
        
        if (!faction.getLeader().equalsIgnoreCase(promoter.getName())) {
            promoter.sendMessage(config.getMessage("error.no-permission-promote"));
            return;
        }
        
        String targetNameLower = targetName.toLowerCase();
        
        if (!faction.isMember(targetName)) {
            promoter.sendMessage(config.getMessage("error.player-not-member", targetName));
            return;
        }
        
        if (faction.getLeader().equalsIgnoreCase(targetName)) {
            promoter.sendMessage(config.getMessage("error.cannot-promote-leader", targetName));
            return;
        }
        
        rank = rank.toLowerCase();
        
        if (rank.equals("lider")) {
            transferLeadershipInternal(promoter, faction, targetName);
        } else if (rank.equals("oficial")) {
            if (faction.getOfficials().contains(targetNameLower)) {
                promoter.sendMessage(config.getMessage("error.already-rank", targetName, "oficial"));
                return;
            }
            
            if (faction.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                faction.setTreasuryPlayer("");
            }
            
            faction.promotePlayer(targetName);
            saveFactionToFile(faction);
            
            promoter.sendMessage(config.getMessage("success.promoted", targetName, "Oficial"));
            notifyPlayer(targetName, config.getMessage("notification.rank-changed", "OFICIAL"));
            
        } else if (rank.equals("tesoureiro")) {
            if (faction.getOfficials().contains(targetNameLower)) {
                promoter.sendMessage(config.getMessage("error.officer-cannot-be-treasurer"));
                return;
            }
            
            if (faction.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                promoter.sendMessage(config.getMessage("error.already-rank", targetName, "tesoureiro"));
                return;
            }
            
            String oldTreasurer = faction.getTreasuryPlayer();
            if (!oldTreasurer.isEmpty()) {
                faction.addMember(oldTreasurer);
            }
            
            faction.setTreasuryPlayer(targetName);
            faction.getMembers().remove(targetNameLower);
            saveFactionToFile(faction);
            
            promoter.sendMessage(config.getMessage("success.treasurer-set", targetName));
            notifyPlayer(targetName, config.getMessage("notification.rank-changed", "TESOUREIRO"));
            
        } else if (rank.equals("membro")) {
            if (faction.getMembers().contains(targetNameLower)) {
                promoter.sendMessage(config.getMessage("error.already-rank", targetName, "membro"));
                return;
            }
            
            if (faction.getOfficials().contains(targetNameLower)) {
                faction.demotePlayer(targetName);
            } else if (faction.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                faction.setTreasuryPlayer("");
                faction.addMember(targetName);
            }
            
            saveFactionToFile(faction);
            
            promoter.sendMessage(config.getMessage("success.demoted", targetName, "Membro"));
            notifyPlayer(targetName, config.getMessage("notification.rank-changed", "MEMBRO"));
            
        } else {
            promoter.sendMessage(config.getMessage("error.invalid-rank"));
        }
    }
    
    private void transferLeadershipInternal(Player oldLeader, Faction faction, String newLeaderName) {
        if (faction.getTreasuryPlayer().equalsIgnoreCase(newLeaderName)) {
            oldLeader.sendMessage(config.getMessage("error.cannot-transfer-to-treasurer"));
            return;
        }
        
        String oldLeaderName = oldLeader.getName();
        
        faction.getOfficials().remove(newLeaderName.toLowerCase());
        faction.getMembers().remove(newLeaderName.toLowerCase());
        
        faction.setLeader(newLeaderName);
        faction.addMember(oldLeaderName);
        
        saveFactionToFile(faction);
        
        oldLeader.sendMessage(config.getMessage("success.leadership-transferred", newLeaderName));
        
        Player newLeader = plugin.getServer().getPlayer(newLeaderName);
        if (newLeader != null && newLeader.isOnline()) {
            newLeader.sendMessage(config.getMessage("notification.new-leader-self", faction.getName()));
        }
        
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(newLeaderName)) {
                onlinePlayer.sendMessage(config.getMessage("notification.new-leader", newLeaderName));
            }
        }
    }
    
    private void notifyPlayer(String playerName, String message) {
        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null && target.isOnline()) {
            target.sendMessage(message);
        }
    }
    
    public void setFactionPvp(Player leader, String status) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) {
            leader.sendMessage(config.getMessage("error.not-in-faction"));
            return;
        }
        
        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage(config.getMessage("error.no-permission-pvp"));
            return;
        }
        
        if (status.equalsIgnoreCase("on")) {
            faction.setPvpEnabled(true);
            leader.sendMessage(config.getMessage("success.pvp-enabled"));
        } else if (status.equalsIgnoreCase("off")) {
            faction.setPvpEnabled(false);
            leader.sendMessage(config.getMessage("success.pvp-disabled"));
        } else {
            leader.sendMessage(config.getMessage("usage.pvp"));
            return;
        }
        
        saveFactionToFile(faction);
    }
    
    public void setFactionTag(Player leader, String newTag) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) {
            leader.sendMessage(config.getMessage("error.not-in-faction"));
            return;
        }
        
        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage(config.getMessage("error.no-permission-tag"));
            return;
        }
        
        if (newTag.length() > 5 || !newTag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage(config.getMessage("error.invalid-tag"));
            return;
        }
        
        if (isTagInUse(newTag)) {
            leader.sendMessage(config.getMessage("error.tag-in-use", newTag));
            return;
        }
        
        faction.setTag(newTag);
        saveFactionToFile(faction);
        leader.sendMessage(config.getMessage("success.tag-changed", newTag));
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
        if (!faction.getTreasuryPlayer().isEmpty()) {
            playerToFactionMap.remove(faction.getTreasuryPlayer().toLowerCase());
        }
        
        File factionFile = new File(plugin.getDataFolder() + "/factions", faction.getName().toLowerCase() + ".yml");
        if (factionFile.exists()) {
            factionFile.delete();
        }
        
        leader.sendMessage(config.getMessage("success.faction-disbanded", faction.getName()));
    }
    
    private boolean isTagInUse(String tag) {
        for (Faction faction : factions.values()) {
            if (faction.getTag().equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }

    public void setFactionBase(com.blockycraft.blockyfactions.data.Faction faction, String baseLocation) {
        if (faction == null) return;
        faction.setBaseLocation(baseLocation);
        saveFactionToFile(faction);
    }

    // NOVO MÉTODO: Retorna localização da base
    public String getFactionBaseLocation(com.blockycraft.blockyfactions.data.Faction faction) {
        if (faction == null) return "";
        return faction.getBaseLocation();
    }
    
    public void saveFactionToFile(Faction faction) {
        File factionFile = new File(plugin.getDataFolder() + "/factions", faction.getName().toLowerCase() + ".yml");
        Configuration factionConfig = new Configuration(factionFile);

        factionConfig.setProperty("nome", faction.getName());
        factionConfig.setProperty("tag", faction.getTag());
        factionConfig.setProperty("cor", faction.getColorHex());
        factionConfig.setProperty("lider", faction.getLeader());
        factionConfig.setProperty("oficiais", faction.getOfficials());
        factionConfig.setProperty("membros", faction.getMembers());
        factionConfig.setProperty("tesoureiro", faction.getTreasuryPlayer());
        factionConfig.setProperty("net_worth", faction.getNetWorth());
        factionConfig.setProperty("pvp_habilitado", faction.isPvpEnabled());
        factionConfig.setProperty("base", faction.getBaseLocation()); // NOVO: localização da base

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
                faction.setColorHex(factionConfig.getString("cor", "#FFFFFF"));
                faction.getOfficials().addAll(factionConfig.getStringList("oficiais", new ArrayList<>()));
                faction.getMembers().addAll(factionConfig.getStringList("membros", new ArrayList<>()));
                faction.setTreasuryPlayer(factionConfig.getString("tesoureiro", ""));
                faction.setNetWorth(factionConfig.getDouble("net_worth", 0.0));
                faction.setPvpEnabled(factionConfig.getBoolean("pvp_habilitado", false));
                faction.setBaseLocation(factionConfig.getString("base", "")); // NOVO: carrega base
                
                factions.put(name.toLowerCase(), faction);
                playerToFactionMap.put(leader.toLowerCase(), name);
                
                for (String official : faction.getOfficials()) {
                    playerToFactionMap.put(official.toLowerCase(), name);
                }
                for (String member : faction.getMembers()) {
                    playerToFactionMap.put(member.toLowerCase(), name);
                }
                if (!faction.getTreasuryPlayer().isEmpty()) {
                    playerToFactionMap.put(faction.getTreasuryPlayer().toLowerCase(), name);
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
