package com.blockycraft.blockyfactions.data;

import java.util.ArrayList;
import java.util.List;

public class Faction {

    private String name;
    private String tag;
    private String leader;
    private List<String> officials;
    private List<String> members;
    private String treasuryPlayer; // O jogador "fundo"
    private double netWorth;
    private boolean pvpEnabled;

    public Faction(String name, String tag, String leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.officials = new ArrayList<String>();
        this.members = new ArrayList<String>();
        this.treasuryPlayer = "";
        this.netWorth = 0.0;
        this.pvpEnabled = false;
    }

    // Getters
    public String getName() { return name; }
    public String getTag() { return tag; }
    public String getLeader() { return leader; }
    public List<String> getOfficials() { return officials; }
    public List<String> getMembers() { return members; }
    public String getTreasuryPlayer() { return treasuryPlayer; }
    public double getNetWorth() { return netWorth; }
    public boolean isPvpEnabled() { return pvpEnabled; }

    // Setters
    public void setTag(String tag) { this.tag = tag; }
    public void setLeader(String leader) { this.leader = leader; }
    public void setTreasuryPlayer(String treasuryPlayer) { this.treasuryPlayer = treasuryPlayer; }
    public void setNetWorth(double netWorth) { this.netWorth = netWorth; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    
    public void addMember(String playerName) {
        if (!isMember(playerName)) {
            members.add(playerName.toLowerCase());
        }
    }

    public void removeMember(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        members.remove(lowerCasePlayerName);
        officials.remove(lowerCasePlayerName);
        if (treasuryPlayer.equalsIgnoreCase(lowerCasePlayerName)) {
            treasuryPlayer = "";
        }
    }
    
    public void promotePlayer(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        if (members.contains(lowerCasePlayerName) && !officials.contains(lowerCasePlayerName)) {
            officials.add(lowerCasePlayerName);
            members.remove(lowerCasePlayerName);
        }
    }
    
    public void demotePlayer(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        if (officials.contains(lowerCasePlayerName)) {
            officials.remove(lowerCasePlayerName);
            members.add(lowerCasePlayerName);
        }
    }

    public boolean isMember(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        return leader.equalsIgnoreCase(lowerCasePlayerName) || 
               officials.contains(lowerCasePlayerName) || 
               members.contains(lowerCasePlayerName) ||
               treasuryPlayer.equalsIgnoreCase(lowerCasePlayerName);
    }
}