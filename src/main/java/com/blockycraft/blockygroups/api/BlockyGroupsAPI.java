package com.blockycraft.blockygroups.api;

import com.blockycraft.blockygroups.BlockyGroups;
import com.blockycraft.blockygroups.data.Group;

public class BlockyGroupsAPI {

    private static BlockyGroups plugin;

    public static void initialize(BlockyGroups instance) {
        plugin = instance;
    }

    public static boolean arePlayersInSameGroup(String playerName1, String playerName2) {
        if (plugin == null) return false;

        Group group1 = plugin.getGroupManager().getPlayerGroup(playerName1);
        Group group2 = plugin.getGroupManager().getPlayerGroup(playerName2);

        if (group1 == null || group2 == null) {
            return false;
        }

        return group1.getName().equalsIgnoreCase(group2.getName());
    }

    public static String getGroupTreasuryPlayer(String playerName) {
        if (plugin == null) return null;
        
        Group group = plugin.getGroupManager().getPlayerGroup(playerName);
        if (group == null) {
            return null;
        }

        String treasuryPlayer = group.getTreasuryPlayer();
        if (treasuryPlayer == null || treasuryPlayer.isEmpty()) {
            return null;
        }

        return treasuryPlayer;
    }


    public static String getPlayerGroupTag(String playerName) {
        if (plugin == null) return null;

        Group group = plugin.getGroupManager().getPlayerGroup(playerName);
        if (group == null) {
            return null;
        }
        return group.getTag();
    }
}