package com.blockycraft.blockyfactions.managers;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.config.ConfigManager;
import com.blockycraft.blockyfactions.data.Faction;
import com.blockycraft.blockyfactions.lang.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.*;
public class FactionManager {

    private final BlockyFactions plugin;
    private final ConfigManager config;
    private final LanguageManager langManager;
    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<String, String> playerToFactionMap = new HashMap<>();
    private final Map<String, String> pendingInvites = new HashMap<>();

    public FactionManager(BlockyFactions plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.langManager = plugin.getLanguageManager();
    }

    public boolean createFaction(String name, String tag, Player leader, String lang) {
        String randomColor = generateRandomHexColor();
        return createFactionInternal(name, tag, leader, randomColor, lang);
    }

    public boolean createFaction(String name, String tag, Player leader, String colorHex, String lang) {
        if (!isValidHexCode(colorHex)) {
            leader.sendMessage(langManager.get(lang, "error.invalid-color", colorHex));
            return false;
        }
        return createFactionInternal(name, tag, leader, colorHex, lang);
    }

    private boolean createFactionInternal(String name, String tag, Player leader, String colorHex, String lang) {
        if (factions.containsKey(name.toLowerCase()) || isTagInUse(tag)) {
            leader.sendMessage(langManager.get(lang, "error.faction-exists"));
            return false;
        }

        if (playerToFactionMap.containsKey(leader.getName().toLowerCase())) {
            leader.sendMessage(langManager.get(lang, "error.already-in-faction"));
            return false;
        }

        if (name.length() > 20 || tag.length() > 5 || !name.matches("^[a-zA-Z0-9]+$") || !tag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage(langManager.get(lang, "error.invalid-name-tag"));
            return false;
        }

        Faction newFaction = new Faction(name, tag, leader.getName());
        newFaction.setColorHex(colorHex.toUpperCase());

        factions.put(name.toLowerCase(), newFaction);
        playerToFactionMap.put(leader.getName().toLowerCase(), name);

        saveFactionToFile(newFaction);
        leader.sendMessage(langManager.get(lang, "success.faction-created", name, colorHex.toUpperCase()));
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

    public void leaveFaction(Player player, String lang) {
        Faction faction = getPlayerFaction(player.getName());
        if (faction == null) {
            player.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return;
        }

        String playerName = player.getName();

        if (faction.getLeader().equalsIgnoreCase(playerName)) {
            List<String> officials = faction.getOfficials();
            if (officials.isEmpty()) {
                dissolveFaction(faction, player, lang);
            } else {
                String newLeaderName = officials.get(0);
                faction.setLeader(newLeaderName);
                faction.getOfficials().remove(newLeaderName.toLowerCase());
                playerToFactionMap.remove(playerName.toLowerCase());

                saveFactionToFile(faction);

                player.sendMessage(langManager.get(lang, "success.left-leadership", faction.getName(), newLeaderName));

                Player newLeader = plugin.getServer().getPlayer(newLeaderName);
                if (newLeader != null && newLeader.isOnline()) {
                    String newLeaderLang = plugin.getGeoIPManager().getPlayerLanguage(newLeader);
                    newLeader.sendMessage(langManager.get(newLeaderLang, "notification.new-leader-promoted", faction.getName()));
                }

                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    if (faction.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(newLeaderName)) {
                        String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                        onlinePlayer.sendMessage(langManager.get(memberLang, "notification.leader-left", playerName, newLeaderName));
                    }
                }
            }
        } else {
            faction.removeMember(playerName);
            playerToFactionMap.remove(playerName.toLowerCase());
            saveFactionToFile(faction);

            player.sendMessage(langManager.get(lang, "success.left-faction", faction.getName()));

            Player leader = plugin.getServer().getPlayer(faction.getLeader());
            if (leader != null && leader.isOnline()) {
                String leaderLang = plugin.getGeoIPManager().getPlayerLanguage(leader);
                leader.sendMessage(langManager.get(leaderLang, "notification.player-left", playerName));
            }
        }
    }

    public List<Faction> getRankedFactions() {
        List<Faction> factionsList = new ArrayList<>(factions.values());
        factionsList.sort(Comparator.comparingDouble(Faction::getNetWorth).reversed());
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

    public void invitePlayer(Player inviter, String targetName, String lang) {
        Faction faction = getPlayerFaction(inviter.getName());
        if (faction == null) {
            inviter.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return;
        }

        if (faction.getSize() >= config.getMaxMembers()) {
            inviter.sendMessage(langManager.get(lang, "error.faction-full", config.getMaxMembers()));
            return;
        }

        if (!faction.isLeaderOrOfficer(inviter.getName())) {
            inviter.sendMessage(langManager.get(lang, "error.no-permission-invite"));
            return;
        }

        if (getPlayerFaction(targetName) != null) {
            inviter.sendMessage(langManager.get(lang, "error.player-already-in-faction", targetName));
            return;
        }

        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            inviter.sendMessage(langManager.get(lang, "error.player-not-online", targetName));
            return;
        }

        pendingInvites.put(target.getName().toLowerCase(), faction.getName());
        inviter.sendMessage(langManager.get(lang, "success.invited", target.getName()));
        String targetLang = plugin.getGeoIPManager().getPlayerLanguage(target);
        target.sendMessage(langManager.get(targetLang, "notification.invited-to-faction", faction.getName()));
        target.sendMessage(langManager.get(targetLang, "notification.invite-instruction", faction.getName()));
    }

    public void joinFaction(Player player, String factionName, String lang) {
        String playerName = player.getName().toLowerCase();

        if (getPlayerFaction(playerName) != null) {
            player.sendMessage(langManager.get(lang, "error.already-in-faction"));
            return;
        }

        String invitedToFaction = pendingInvites.get(playerName);
        if (invitedToFaction == null || !invitedToFaction.equalsIgnoreCase(factionName)) {
            player.sendMessage(langManager.get(lang, "error.no-invite", factionName));
            return;
        }

        Faction faction = getFactionByName(invitedToFaction);
        if (faction == null) {
            player.sendMessage(langManager.get(lang, "error.faction-disbanded"));
            pendingInvites.remove(playerName);
            return;
        }

        if (faction.getSize() >= config.getMaxMembers()) {
            player.sendMessage(langManager.get(lang, "error.faction-full-join", faction.getName()));
            pendingInvites.remove(playerName);
            return;
        }

        pendingInvites.remove(playerName);
        faction.addMember(player.getName());
        playerToFactionMap.put(playerName, faction.getName());
        saveFactionToFile(faction);

        player.sendMessage(langManager.get(lang, "success.joined", faction.getName()));

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName())) {
                String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                onlinePlayer.sendMessage(langManager.get(memberLang, "notification.player-joined", player.getName()));
            }
        }
    }

    public void listFactionInfo(Player viewer, String factionName, String lang) {
        Faction faction = getFactionByName(factionName);
        if (faction == null) {
            viewer.sendMessage(langManager.get(lang, "error.faction-not-found", factionName));
            return;
        }

        reloadFactionNetWorth(faction);

        viewer.sendMessage(langManager.get(lang, "info.header"));
        viewer.sendMessage(langManager.get(lang, "info.name", faction.getName(), faction.getTag()));
        viewer.sendMessage(langManager.get(lang, "info.color", faction.getColorHex()));
        viewer.sendMessage(langManager.get(lang, "info.leader", faction.getLeader()));

        String treasuryPlayer = faction.getTreasuryPlayer();
        if (treasuryPlayer == null || treasuryPlayer.isEmpty()) {
            viewer.sendMessage(langManager.get(lang, "info.treasurer-none"));
        } else {
            viewer.sendMessage(langManager.get(lang, "info.treasurer", treasuryPlayer));
        }

        viewer.sendMessage(langManager.get(lang, "info.networth", faction.getNetWorth()));
        viewer.sendMessage(faction.isPvpEnabled() ? langManager.get(lang, "info.pvp-enabled") : langManager.get(lang, "info.pvp-disabled"));

        String officials = String.join(", ", faction.getOfficials());
        if (officials.isEmpty()) { officials = langManager.get(lang, "info.none"); }
        viewer.sendMessage(langManager.get(lang, "info.officials", faction.getOfficials().size(), officials));

        ArrayList<String> plainMembers = new ArrayList<>(faction.getMembers());
        String members = String.join(", ", plainMembers);
        if (members.isEmpty()) { members = langManager.get(lang, "info.none"); }
        viewer.sendMessage(langManager.get(lang, "info.members", plainMembers.size(), members));

        viewer.sendMessage(langManager.get(lang, "info.total-members", faction.getSize(), config.getMaxMembers()));
        viewer.sendMessage(langManager.get(lang, "info.footer"));
    }

    public void kickPlayer(Player kicker, String targetName, String lang) {
        Faction faction = getPlayerFaction(kicker.getName());
        if (faction == null) {
            kicker.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return;
        }

        if (!faction.isLeaderOrOfficer(kicker.getName())) {
            kicker.sendMessage(langManager.get(lang, "error.no-permission-kick"));
            return;
        }

        Faction targetFaction = getPlayerFaction(targetName);
        if (targetFaction == null || !targetFaction.getName().equals(faction.getName())) {
            kicker.sendMessage(langManager.get(lang, "error.player-not-in-your-faction", targetName));
            return;
        }

        String targetNameLower = targetName.toLowerCase();
        if (faction.getLeader().equalsIgnoreCase(targetNameLower)) {
            kicker.sendMessage(langManager.get(lang, "error.cannot-kick-leader"));
            return;
        }

        if (faction.getOfficials().contains(targetNameLower) && !faction.getLeader().equalsIgnoreCase(kicker.getName())) {
            kicker.sendMessage(langManager.get(lang, "error.cannot-kick-officer"));
            return;
        }

        faction.removeMember(targetName);
        playerToFactionMap.remove(targetNameLower);
        saveFactionToFile(faction);

        kicker.sendMessage(langManager.get(lang, "success.kicked", targetName));

        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            String targetLang = plugin.getGeoIPManager().getPlayerLanguage(targetPlayer);
            targetPlayer.sendMessage(langManager.get(targetLang, "error.not-in-faction"));
        }

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName())) {
                String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                onlinePlayer.sendMessage(langManager.get(memberLang, "notification.player-kicked", targetName, kicker.getName()));
            }
        }
    }

    public void setPlayerRank(Player promoter, String targetName, String rank, String lang) {
        Faction faction = getPlayerFaction(promoter.getName());
        if (faction == null) {
            promoter.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return;
        }

        if (!faction.getLeader().equalsIgnoreCase(promoter.getName())) {
            promoter.sendMessage(langManager.get(lang, "error.no-permission-promote"));
            return;
        }

        String targetNameLower = targetName.toLowerCase();

        if (!faction.isMember(targetName)) {
            promoter.sendMessage(langManager.get(lang, "error.player-not-member", targetName));
            return;
        }

        if (faction.getLeader().equalsIgnoreCase(targetName)) {
            promoter.sendMessage(langManager.get(lang, "error.cannot-promote-leader", targetName));
            return;
        }

        rank = rank.toLowerCase();

        if (rank.equals("lider") || rank.equals("leader")) {
            transferLeadershipInternal(promoter, faction, targetName, lang);
        } else if (rank.equals("oficial") || rank.equals("officer")) {
            if (faction.getOfficials().contains(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.already-rank", targetName, "oficial"));
                return;
            }

            if (faction.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                faction.setTreasuryPlayer("");
            }

            faction.promotePlayer(targetName);
            saveFactionToFile(faction);

            promoter.sendMessage(langManager.get(lang, "success.promoted", targetName, "Oficial"));
            notifyPlayer(targetName, langManager.get(plugin.getGeoIPManager().getPlayerLanguage(plugin.getServer().getPlayer(targetName)), "notification.rank-changed", "OFICIAL"));

        } else if (rank.equals("tesoureiro") || rank.equals("treasurer")) {
            if (faction.getOfficials().contains(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.officer-cannot-be-treasurer"));
                return;
            }

            if (faction.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.already-rank", targetName, "tesoureiro"));
                return;
            }

            String oldTreasurer = faction.getTreasuryPlayer();
            if (!oldTreasurer.isEmpty()) {
                faction.addMember(oldTreasurer);
            }

            faction.setTreasuryPlayer(targetName);
            faction.getMembers().remove(targetNameLower);
            saveFactionToFile(faction);

            promoter.sendMessage(langManager.get(lang, "success.treasurer-set", targetName));
            notifyPlayer(targetName, langManager.get(plugin.getGeoIPManager().getPlayerLanguage(plugin.getServer().getPlayer(targetName)), "notification.rank-changed", "TESOUREIRO"));

        } else if (rank.equals("membro") || rank.equals("member")) {
            if (faction.getMembers().contains(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.already-rank", targetName, "membro"));
                return;
            }

            if (faction.getOfficials().contains(targetNameLower)) {
                faction.demotePlayer(targetName);
            } else if (faction.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                faction.setTreasuryPlayer("");
                faction.addMember(targetName);
            }

            saveFactionToFile(faction);

            promoter.sendMessage(langManager.get(lang, "success.demoted", targetName, "Membro"));
            notifyPlayer(targetName, langManager.get(plugin.getGeoIPManager().getPlayerLanguage(plugin.getServer().getPlayer(targetName)), "notification.rank-changed", "MEMBRO"));

        } else {
            promoter.sendMessage(langManager.get(lang, "error.invalid-rank"));
        }
    }

    private void transferLeadershipInternal(Player oldLeader, Faction faction, String newLeaderName, String lang) {
        if (faction.getTreasuryPlayer().equalsIgnoreCase(newLeaderName)) {
            oldLeader.sendMessage(langManager.get(lang, "error.cannot-transfer-to-treasurer"));
            return;
        }

        String oldLeaderName = oldLeader.getName();

        faction.getOfficials().remove(newLeaderName.toLowerCase());
        faction.getMembers().remove(newLeaderName.toLowerCase());

        faction.setLeader(newLeaderName);
        faction.addMember(oldLeaderName);

        saveFactionToFile(faction);

        oldLeader.sendMessage(langManager.get(lang, "success.leadership-transferred", newLeaderName));

        Player newLeader = plugin.getServer().getPlayer(newLeaderName);
        if (newLeader != null && newLeader.isOnline()) {
            String newLeaderLang = plugin.getGeoIPManager().getPlayerLanguage(newLeader);
            newLeader.sendMessage(langManager.get(newLeaderLang, "notification.new-leader-self", faction.getName()));
        }

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(newLeaderName)) {
                String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                onlinePlayer.sendMessage(langManager.get(memberLang, "notification.new-leader", newLeaderName));
            }
        }
    }

    private void notifyPlayer(String playerName, String message) {
        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null && target.isOnline()) {
            target.sendMessage(message);
        }
    }

    public void setFactionPvp(Player leader, String status, String lang) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) {
            leader.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return;
        }

        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage(langManager.get(lang, "error.no-permission-pvp"));
            return;
        }

        if (status.equalsIgnoreCase("on")) {
            faction.setPvpEnabled(true);
            leader.sendMessage(langManager.get(lang, "success.pvp-enabled"));
        } else if (status.equalsIgnoreCase("off")) {
            faction.setPvpEnabled(false);
            leader.sendMessage(langManager.get(lang, "success.pvp-disabled"));
        } else {
            leader.sendMessage(langManager.get(lang, "usage.pvp"));
            return;
        }

        saveFactionToFile(faction);
    }

    public void setFactionTag(Player leader, String newTag, String lang) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) {
            leader.sendMessage(langManager.get(lang, "error.not-in-faction"));
            return;
        }

        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage(langManager.get(lang, "error.no-permission-tag"));
            return;
        }

        if (newTag.length() > 5 || !newTag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage(langManager.get(lang, "error.invalid-tag"));
            return;
        }

        if (isTagInUse(newTag)) {
            leader.sendMessage(langManager.get(lang, "error.tag-in-use", newTag));
            return;
        }

        faction.setTag(newTag);
        saveFactionToFile(faction);
        leader.sendMessage(langManager.get(lang, "success.tag-changed", newTag));
    }

    private void dissolveFaction(Faction faction, Player leader, String lang) {
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

        leader.sendMessage(langManager.get(lang, "success.faction-disbanded", faction.getName()));
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
        factionConfig.setProperty("base", faction.getBaseLocation());

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
                faction.setBaseLocation(factionConfig.getString("base", ""));

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