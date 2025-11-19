package com.blockycraft.blockygroups.data;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private String name;
    private String tag;
    private String leader;
    private List<String> officials;
    private List<String> members;
    private String treasuryPlayer;
    private double netWorth;
    private boolean pvpEnabled;
    private String colorHex;

    /** NOVO CAMPO: localização da base (World;x;y;z;yaw;pitch) */
    private String baseLocation = "";

    public Group(String name, String tag, String leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.officials = new ArrayList<>();
        this.members = new ArrayList<>();
        this.treasuryPlayer = "";
        this.netWorth = 0.0;
        this.pvpEnabled = false;
        this.colorHex = "#FFFFFF";
        this.baseLocation = "";
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
    public String getColorHex() { return colorHex; }
    public String getBaseLocation() { return baseLocation; }

    // Setters
    public void setTag(String tag) { this.tag = tag; }
    public void setLeader(String leader) { this.leader = leader; }
    public void setTreasuryPlayer(String treasuryPlayer) { this.treasuryPlayer = treasuryPlayer; }
    public void setNetWorth(double netWorth) { this.netWorth = netWorth; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public void setBaseLocation(String baseLocation) { this.baseLocation = baseLocation; }

    public void addMember(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        if (!isMember(playerName)) {
            members.add(lowerCasePlayerName);
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
        members.remove(lowerCasePlayerName);
        if (!officials.contains(lowerCasePlayerName)) {
            officials.add(lowerCasePlayerName);
        }
    }

    public void demotePlayer(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        officials.remove(lowerCasePlayerName);
        if (!members.contains(lowerCasePlayerName)) {
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

    public boolean isLeaderOrOfficer(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        return leader.equalsIgnoreCase(lowerCasePlayerName) ||
                officials.contains(lowerCasePlayerName);
    }

    /**
     * Calcula o número total de jogadores, contando todos os cargos.
     * Inclui: 1 Líder + Oficiais + Membros + 1 Tesoureiro (se definido).
     */
    public int getSize() {
        int size = 1; // 1 para o líder
        size += officials.size();
        size += members.size();
        if (treasuryPlayer != null && !treasuryPlayer.isEmpty()) {
            size++;
        }
        return size;
    }
}
