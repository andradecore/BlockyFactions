package com.blockycraft.blockyfactions.api;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;

public class BlockyFactionsAPI {

    private static BlockyFactions plugin;

    public static void initialize(BlockyFactions instance) {
        plugin = instance;
    }

    public static boolean arePlayersInSameFaction(String playerName1, String playerName2) {
        if (plugin == null) return false;

        Faction faction1 = plugin.getFactionManager().getPlayerFaction(playerName1);
        Faction faction2 = plugin.getFactionManager().getPlayerFaction(playerName2);

        if (faction1 == null || faction2 == null) {
            return false;
        }

        return faction1.getName().equalsIgnoreCase(faction2.getName());
    }

    public static String getFactionTreasuryPlayer(String playerName) {
        if (plugin == null) return null;
        
        Faction faction = plugin.getFactionManager().getPlayerFaction(playerName);
        if (faction == null) {
            return null;
        }

        String treasuryPlayer = faction.getTreasuryPlayer();
        if (treasuryPlayer == null || treasuryPlayer.isEmpty()) {
            return null;
        }

        return treasuryPlayer;
    }


    public static String getPlayerFactionTag(String playerName) {
        if (plugin == null) return null;

        Faction faction = plugin.getFactionManager().getPlayerFaction(playerName);
        if (faction == null) {
            return null;
        }
        return faction.getTag();
    }
}