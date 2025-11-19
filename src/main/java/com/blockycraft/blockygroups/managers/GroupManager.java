package com.blockycraft.blockygroups.managers;

import com.blockycraft.blockygroups.BlockyGroups;
import com.blockycraft.blockygroups.config.ConfigManager;
import com.blockycraft.blockygroups.data.Group;
import com.blockycraft.blockygroups.lang.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.*;
public class GroupManager {

    private final BlockyGroups plugin;
    private final ConfigManager config;
    private final LanguageManager langManager;
    private final Map<String, Group> groups = new HashMap<>();
    private final Map<String, String> playerToGroupMap = new HashMap<>();
    private final Map<String, String> pendingInvites = new HashMap<>();

    public GroupManager(BlockyGroups plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.langManager = plugin.getLanguageManager();
    }

    public boolean createGroup(String name, String tag, Player leader, String lang) {
        String randomColor = generateRandomHexColor();
        return createGroupInternal(name, tag, leader, randomColor, lang);
    }

    public boolean createGroup(String name, String tag, Player leader, String colorHex, String lang) {
        if (!isValidHexCode(colorHex)) {
            leader.sendMessage(langManager.get(lang, "error.invalid-color", colorHex));
            return false;
        }
        return createGroupInternal(name, tag, leader, colorHex, lang);
    }

    private boolean createGroupInternal(String name, String tag, Player leader, String colorHex, String lang) {
        if (groups.containsKey(name.toLowerCase()) || isTagInUse(tag)) {
            leader.sendMessage(langManager.get(lang, "error.group-exists"));
            return false;
        }

        if (playerToGroupMap.containsKey(leader.getName().toLowerCase())) {
            leader.sendMessage(langManager.get(lang, "error.already-in-group"));
            return false;
        }

        if (name.length() > 20 || tag.length() > 5 || !name.matches("^[a-zA-Z0-9]+$") || !tag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage(langManager.get(lang, "error.invalid-name-tag"));
            return false;
        }

        Group newGroup = new Group(name, tag, leader.getName());
        newGroup.setColorHex(colorHex.toUpperCase());

        groups.put(name.toLowerCase(), newGroup);
        playerToGroupMap.put(leader.getName().toLowerCase(), name);

        saveGroupToFile(newGroup);
        leader.sendMessage(langManager.get(lang, "success.group-created", name, colorHex.toUpperCase()));
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

    public void leaveGroup(Player player, String lang) {
        Group group = getPlayerGroup(player.getName());
        if (group == null) {
            player.sendMessage(langManager.get(lang, "error.not-in-group"));
            return;
        }

        String playerName = player.getName();

        if (group.getLeader().equalsIgnoreCase(playerName)) {
            List<String> officials = group.getOfficials();
            if (officials.isEmpty()) {
                dissolveGroup(group, player, lang);
            } else {
                String newLeaderName = officials.get(0);
                group.setLeader(newLeaderName);
                group.getOfficials().remove(newLeaderName.toLowerCase());
                playerToGroupMap.remove(playerName.toLowerCase());

                saveGroupToFile(group);

                player.sendMessage(langManager.get(lang, "success.left-leadership", group.getName(), newLeaderName));

                Player newLeader = plugin.getServer().getPlayer(newLeaderName);
                if (newLeader != null && newLeader.isOnline()) {
                    String newLeaderLang = plugin.getGeoIPManager().getPlayerLanguage(newLeader);
                    newLeader.sendMessage(langManager.get(newLeaderLang, "notification.new-leader-promoted", group.getName()));
                }

                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    if (group.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(newLeaderName)) {
                        String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                        onlinePlayer.sendMessage(langManager.get(memberLang, "notification.leader-left", playerName, newLeaderName));
                    }
                }
            }
        } else {
            group.removeMember(playerName);
            playerToGroupMap.remove(playerName.toLowerCase());
            saveGroupToFile(group);

            player.sendMessage(langManager.get(lang, "success.left-group", group.getName()));

            Player leader = plugin.getServer().getPlayer(group.getLeader());
            if (leader != null && leader.isOnline()) {
                String leaderLang = plugin.getGeoIPManager().getPlayerLanguage(leader);
                leader.sendMessage(langManager.get(leaderLang, "notification.player-left", playerName));
            }
        }
    }

    public List<Group> getRankedGroups() {
        List<Group> groupsList = new ArrayList<>(groups.values());
        groupsList.sort(Comparator.comparingDouble(Group::getNetWorth).reversed());
        return groupsList;
    }

    public void reloadGroupNetWorth(Group group) {
        if (group == null) return;

        File groupFile = new File(plugin.getDataFolder() + "/groups", group.getName().toLowerCase() + ".yml");
        if (groupFile.exists()) {
            Configuration groupConfig = new Configuration(groupFile);
            groupConfig.load();
            double newNetWorth = groupConfig.getDouble("net_worth", 0.0);
            group.setNetWorth(newNetWorth);
        }
    }

    public void reloadAllGroupsNetWorth() {
        for (Group group : groups.values()) {
            reloadGroupNetWorth(group);
        }
    }

    public void invitePlayer(Player inviter, String targetName, String lang) {
        Group group = getPlayerGroup(inviter.getName());
        if (group == null) {
            inviter.sendMessage(langManager.get(lang, "error.not-in-group"));
            return;
        }

        if (group.getSize() >= config.getMaxMembers()) {
            inviter.sendMessage(langManager.get(lang, "error.group-full", config.getMaxMembers()));
            return;
        }

        if (!group.isLeaderOrOfficer(inviter.getName())) {
            inviter.sendMessage(langManager.get(lang, "error.no-permission-invite"));
            return;
        }

        if (getPlayerGroup(targetName) != null) {
            inviter.sendMessage(langManager.get(lang, "error.player-already-in-group", targetName));
            return;
        }

        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            inviter.sendMessage(langManager.get(lang, "error.player-not-online", targetName));
            return;
        }

        pendingInvites.put(target.getName().toLowerCase(), group.getName());
        inviter.sendMessage(langManager.get(lang, "success.invited", target.getName()));
        String targetLang = plugin.getGeoIPManager().getPlayerLanguage(target);
        target.sendMessage(langManager.get(targetLang, "notification.invited-to-group", group.getName()));
        target.sendMessage(langManager.get(targetLang, "notification.invite-instruction", group.getName()));
    }

    public void joinGroup(Player player, String groupName, String lang) {
        String playerName = player.getName().toLowerCase();

        if (getPlayerGroup(playerName) != null) {
            player.sendMessage(langManager.get(lang, "error.already-in-group"));
            return;
        }

        String invitedToGroup = pendingInvites.get(playerName);
        if (invitedToGroup == null || !invitedToGroup.equalsIgnoreCase(groupName)) {
            player.sendMessage(langManager.get(lang, "error.no-invite", groupName));
            return;
        }

        Group group = getGroupByName(invitedToGroup);
        if (group == null) {
            player.sendMessage(langManager.get(lang, "error.group-disbanded"));
            pendingInvites.remove(playerName);
            return;
        }

        if (group.getSize() >= config.getMaxMembers()) {
            player.sendMessage(langManager.get(lang, "error.group-full-join", group.getName()));
            pendingInvites.remove(playerName);
            return;
        }

        pendingInvites.remove(playerName);
        group.addMember(player.getName());
        playerToGroupMap.put(playerName, group.getName());
        saveGroupToFile(group);

        player.sendMessage(langManager.get(lang, "success.joined", group.getName()));

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (group.isMember(onlinePlayer.getName())) {
                String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                onlinePlayer.sendMessage(langManager.get(memberLang, "notification.player-joined", player.getName()));
            }
        }
    }

    public void listGroupInfo(Player viewer, String groupName, String lang) {
        Group group = getGroupByName(groupName);
        if (group == null) {
            viewer.sendMessage(langManager.get(lang, "error.group-not-found", groupName));
            return;
        }

        reloadGroupNetWorth(group);

        viewer.sendMessage(langManager.get(lang, "info.header"));
        viewer.sendMessage(langManager.get(lang, "info.name", group.getName(), group.getTag()));
        viewer.sendMessage(langManager.get(lang, "info.color", group.getColorHex()));
        viewer.sendMessage(langManager.get(lang, "info.leader", group.getLeader()));

        String treasuryPlayer = group.getTreasuryPlayer();
        if (treasuryPlayer == null || treasuryPlayer.isEmpty()) {
            viewer.sendMessage(langManager.get(lang, "info.treasurer-none"));
        } else {
            viewer.sendMessage(langManager.get(lang, "info.treasurer", treasuryPlayer));
        }

        viewer.sendMessage(langManager.get(lang, "info.networth", group.getNetWorth()));
        viewer.sendMessage(group.isPvpEnabled() ? langManager.get(lang, "info.pvp-enabled") : langManager.get(lang, "info.pvp-disabled"));

        String officials = String.join(", ", group.getOfficials());
        if (officials.isEmpty()) { officials = langManager.get(lang, "info.none"); }
        viewer.sendMessage(langManager.get(lang, "info.officials", group.getOfficials().size(), officials));

        ArrayList<String> plainMembers = new ArrayList<>(group.getMembers());
        String members = String.join(", ", plainMembers);
        if (members.isEmpty()) { members = langManager.get(lang, "info.none"); }
        viewer.sendMessage(langManager.get(lang, "info.members", plainMembers.size(), members));

        viewer.sendMessage(langManager.get(lang, "info.total-members", group.getSize(), config.getMaxMembers()));
        viewer.sendMessage(langManager.get(lang, "info.footer"));
    }

    public void kickPlayer(Player kicker, String targetName, String lang) {
        Group group = getPlayerGroup(kicker.getName());
        if (group == null) {
            kicker.sendMessage(langManager.get(lang, "error.not-in-group"));
            return;
        }

        if (!group.isLeaderOrOfficer(kicker.getName())) {
            kicker.sendMessage(langManager.get(lang, "error.no-permission-kick"));
            return;
        }

        Group targetGroup = getPlayerGroup(targetName);
        if (targetGroup == null || !targetGroup.getName().equals(group.getName())) {
            kicker.sendMessage(langManager.get(lang, "error.player-not-in-your-group", targetName));
            return;
        }

        String targetNameLower = targetName.toLowerCase();
        if (group.getLeader().equalsIgnoreCase(targetNameLower)) {
            kicker.sendMessage(langManager.get(lang, "error.cannot-kick-leader"));
            return;
        }

        if (group.getOfficials().contains(targetNameLower) && !group.getLeader().equalsIgnoreCase(kicker.getName())) {
            kicker.sendMessage(langManager.get(lang, "error.cannot-kick-officer"));
            return;
        }

        group.removeMember(targetName);
        playerToGroupMap.remove(targetNameLower);
        saveGroupToFile(group);

        kicker.sendMessage(langManager.get(lang, "success.kicked", targetName));

        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            String targetLang = plugin.getGeoIPManager().getPlayerLanguage(targetPlayer);
            targetPlayer.sendMessage(langManager.get(targetLang, "error.not-in-group"));
        }

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (group.isMember(onlinePlayer.getName())) {
                String memberLang = plugin.getGeoIPManager().getPlayerLanguage(onlinePlayer);
                onlinePlayer.sendMessage(langManager.get(memberLang, "notification.player-kicked", targetName, kicker.getName()));
            }
        }
    }

    public void setPlayerRank(Player promoter, String targetName, String rank, String lang) {
        Group group = getPlayerGroup(promoter.getName());
        if (group == null) {
            promoter.sendMessage(langManager.get(lang, "error.not-in-group"));
            return;
        }

        if (!group.getLeader().equalsIgnoreCase(promoter.getName())) {
            promoter.sendMessage(langManager.get(lang, "error.no-permission-promote"));
            return;
        }

        String targetNameLower = targetName.toLowerCase();

        if (!group.isMember(targetName)) {
            promoter.sendMessage(langManager.get(lang, "error.player-not-member", targetName));
            return;
        }

        if (group.getLeader().equalsIgnoreCase(targetName)) {
            promoter.sendMessage(langManager.get(lang, "error.cannot-promote-leader", targetName));
            return;
        }

        rank = rank.toLowerCase();

        if (rank.equals("lider") || rank.equals("leader")) {
            transferLeadershipInternal(promoter, group, targetName, lang);
        } else if (rank.equals("oficial") || rank.equals("officer")) {
            if (group.getOfficials().contains(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.already-rank", targetName, "oficial"));
                return;
            }

            if (group.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                group.setTreasuryPlayer("");
            }

            group.promotePlayer(targetName);
            saveGroupToFile(group);

            promoter.sendMessage(langManager.get(lang, "success.promoted", targetName, "Oficial"));
            notifyPlayer(targetName, langManager.get(plugin.getGeoIPManager().getPlayerLanguage(plugin.getServer().getPlayer(targetName)), "notification.rank-changed", "OFICIAL"));

        } else if (rank.equals("tesoureiro") || rank.equals("treasurer")) {
            if (group.getOfficials().contains(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.officer-cannot-be-treasurer"));
                return;
            }

            if (group.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.already-rank", targetName, "tesoureiro"));
                return;
            }

            String oldTreasurer = group.getTreasuryPlayer();
            if (!oldTreasurer.isEmpty()) {
                group.addMember(oldTreasurer);
            }

            group.setTreasuryPlayer(targetName);
            group.getMembers().remove(targetNameLower);
            saveGroupToFile(group);

            promoter.sendMessage(langManager.get(lang, "success.treasurer-set", targetName));
            notifyPlayer(targetName, langManager.get(plugin.getGeoIPManager().getPlayerLanguage(plugin.getServer().getPlayer(targetName)), "notification.rank-changed", "TESOUREIRO"));

        } else if (rank.equals("membro") || rank.equals("member")) {
            if (group.getMembers().contains(targetNameLower)) {
                promoter.sendMessage(langManager.get(lang, "error.already-rank", targetName, "membro"));
                return;
            }

            if (group.getOfficials().contains(targetNameLower)) {
                group.demotePlayer(targetName);
            } else if (group.getTreasuryPlayer().equalsIgnoreCase(targetNameLower)) {
                group.setTreasuryPlayer("");
                group.addMember(targetName);
            }

            saveGroupToFile(group);

            promoter.sendMessage(langManager.get(lang, "success.demoted", targetName, "Membro"));
            notifyPlayer(targetName, langManager.get(plugin.getGeoIPManager().getPlayerLanguage(plugin.getServer().getPlayer(targetName)), "notification.rank-changed", "MEMBRO"));

        } else {
            promoter.sendMessage(langManager.get(lang, "error.invalid-rank"));
        }
    }

    private void transferLeadershipInternal(Player oldLeader, Group group, String newLeaderName, String lang) {
        if (group.getTreasuryPlayer().equalsIgnoreCase(newLeaderName)) {
            oldLeader.sendMessage(langManager.get(lang, "error.cannot-transfer-to-treasurer"));
            return;
        }

        String oldLeaderName = oldLeader.getName();

        group.getOfficials().remove(newLeaderName.toLowerCase());
        group.getMembers().remove(newLeaderName.toLowerCase());

        group.setLeader(newLeaderName);
        group.addMember(oldLeaderName);

        saveGroupToFile(group);

        oldLeader.sendMessage(langManager.get(lang, "success.leadership-transferred", newLeaderName));

        Player newLeader = plugin.getServer().getPlayer(newLeaderName);
        if (newLeader != null && newLeader.isOnline()) {
            String newLeaderLang = plugin.getGeoIPManager().getPlayerLanguage(newLeader);
            newLeader.sendMessage(langManager.get(newLeaderLang, "notification.new-leader-self", group.getName()));
        }

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (group.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(newLeaderName)) {
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

    public void setGroupPvp(Player leader, String status, String lang) {
        Group group = getPlayerGroup(leader.getName());
        if (group == null) {
            leader.sendMessage(langManager.get(lang, "error.not-in-group"));
            return;
        }

        if (!group.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage(langManager.get(lang, "error.no-permission-pvp"));
            return;
        }

        if (status.equalsIgnoreCase("on")) {
            group.setPvpEnabled(true);
            leader.sendMessage(langManager.get(lang, "success.pvp-enabled"));
        } else if (status.equalsIgnoreCase("off")) {
            group.setPvpEnabled(false);
            leader.sendMessage(langManager.get(lang, "success.pvp-disabled"));
        } else {
            leader.sendMessage(langManager.get(lang, "usage.pvp"));
            return;
        }

        saveGroupToFile(group);
    }

    public void setGroupTag(Player leader, String newTag, String lang) {
        Group group = getPlayerGroup(leader.getName());
        if (group == null) {
            leader.sendMessage(langManager.get(lang, "error.not-in-group"));
            return;
        }

        if (!group.getLeader().equalsIgnoreCase(leader.getName())) {
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

        group.setTag(newTag);
        saveGroupToFile(group);
        leader.sendMessage(langManager.get(lang, "success.tag-changed", newTag));
    }

    private void dissolveGroup(Group group, Player leader, String lang) {
        groups.remove(group.getName().toLowerCase());
        playerToGroupMap.remove(leader.getName().toLowerCase());

        for (String official : group.getOfficials()) {
            playerToGroupMap.remove(official.toLowerCase());
        }
        for (String member : group.getMembers()) {
            playerToGroupMap.remove(member.toLowerCase());
        }
        if (!group.getTreasuryPlayer().isEmpty()) {
            playerToGroupMap.remove(group.getTreasuryPlayer().toLowerCase());
        }

        File groupFile = new File(plugin.getDataFolder() + "/groups", group.getName().toLowerCase() + ".yml");
        if (groupFile.exists()) {
            groupFile.delete();
        }

        leader.sendMessage(langManager.get(lang, "success.group-disbanded", group.getName()));
    }

    private boolean isTagInUse(String tag) {
        for (Group group : groups.values()) {
            if (group.getTag().equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }

    public void setGroupBase(com.blockycraft.blockygroups.data.Group group, String baseLocation) {
        if (group == null) return;
        group.setBaseLocation(baseLocation);
        saveGroupToFile(group);
    }

    public String getGroupBaseLocation(com.blockycraft.blockygroups.data.Group group) {
        if (group == null) return "";
        return group.getBaseLocation();
    }

    public void saveGroupToFile(Group group) {
        File groupFile = new File(plugin.getDataFolder() + "/groups", group.getName().toLowerCase() + ".yml");
        Configuration groupConfig = new Configuration(groupFile);

        groupConfig.setProperty("nome", group.getName());
        groupConfig.setProperty("tag", group.getTag());
        groupConfig.setProperty("cor", group.getColorHex());
        groupConfig.setProperty("lider", group.getLeader());
        groupConfig.setProperty("oficiais", group.getOfficials());
        groupConfig.setProperty("membros", group.getMembers());
        groupConfig.setProperty("tesoureiro", group.getTreasuryPlayer());
        groupConfig.setProperty("net_worth", group.getNetWorth());
        groupConfig.setProperty("pvp_habilitado", group.isPvpEnabled());
        groupConfig.setProperty("base", group.getBaseLocation());

        groupConfig.save();
    }

    public void loadGroups() {
        File groupsDir = new File(plugin.getDataFolder(), "groups");
        if (!groupsDir.exists()) {
            groupsDir.mkdirs();
            return;
        }

        File[] files = groupsDir.listFiles();
        if (files == null) return;

        for (File groupFile : files) {
            if (groupFile.getName().endsWith(".yml")) {
                Configuration groupConfig = new Configuration(groupFile);
                groupConfig.load();

                String name = groupConfig.getString("nome");
                String tag = groupConfig.getString("tag");
                String leader = groupConfig.getString("lider");

                if (name == null || tag == null || leader == null) continue;

                Group group = new Group(name, tag, leader);
                group.setColorHex(groupConfig.getString("cor", "#FFFFFF"));
                group.getOfficials().addAll(groupConfig.getStringList("oficiais", new ArrayList<>()));
                group.getMembers().addAll(groupConfig.getStringList("membros", new ArrayList<>()));
                group.setTreasuryPlayer(groupConfig.getString("tesoureiro", ""));
                group.setNetWorth(groupConfig.getDouble("net_worth", 0.0));
                group.setPvpEnabled(groupConfig.getBoolean("pvp_habilitado", false));
                group.setBaseLocation(groupConfig.getString("base", ""));

                groups.put(name.toLowerCase(), group);
                playerToGroupMap.put(leader.toLowerCase(), name);

                for (String official : group.getOfficials()) {
                    playerToGroupMap.put(official.toLowerCase(), name);
                }
                for (String member : group.getMembers()) {
                    playerToGroupMap.put(member.toLowerCase(), name);
                }
                if (!group.getTreasuryPlayer().isEmpty()) {
                    playerToGroupMap.put(group.getTreasuryPlayer().toLowerCase(), name);
                }
            }
        }

        System.out.println("[BlockyGroups] " + groups.size() + " grupos carregadas.");
    }

    public void saveGroups() {
        for (Group group : groups.values()) {
            saveGroupToFile(group);
        }
        System.out.println("[BlockyGroups] Todas as " + groups.size() + " grupos foram salvas.");
    }

    public Group getGroupByName(String name) {
        return groups.get(name.toLowerCase());
    }

    public Group getPlayerGroup(String playerName) {
        String groupName = playerToGroupMap.get(playerName.toLowerCase());
        if (groupName != null) {
            return getGroupByName(groupName);
        }
        return null;
    }
}