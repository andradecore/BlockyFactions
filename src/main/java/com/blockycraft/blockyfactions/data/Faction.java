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

    // Construtor para criar uma nova facção
    public Faction(String name, String tag, String leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.officials = new ArrayList<String>();
        this.members = new ArrayList<String>();
        this.treasuryPlayer = ""; // Inicia sem fundo definido
        this.netWorth = 0.0;
        this.pvpEnabled = false;
    }

    // Getters (para ler os dados)
    public String getName() { return name; }
    public String getTag() { return tag; }
    public String getLeader() { return leader; }
    public List<String> getOfficials() { return officials; }
    public List<String> getMembers() { return members; }
    public String getTreasuryPlayer() { return treasuryPlayer; }
    public double getNetWorth() { return netWorth; }
    public boolean isPvpEnabled() { return pvpEnabled; }

    // Setters (para modificar os dados)
    public void setTag(String tag) { this.tag = tag; }
    public void setLeader(String leader) { this.leader = leader; }
    public void setTreasuryPlayer(String treasuryPlayer) { this.treasuryPlayer = treasuryPlayer; }
    public void setNetWorth(double netWorth) { this.netWorth = netWorth; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    
    // Métodos para gerenciar membros e oficiais
    public void addMember(String playerName) {
        if (!members.contains(playerName.toLowerCase())) {
            members.add(playerName.toLowerCase());
        }
    }

    public void removeMember(String playerName) {
        members.remove(playerName.toLowerCase());
        officials.remove(playerName.toLowerCase()); // Garante que ele também seja removido de oficial
    }
    
    public void promotePlayer(String playerName) {
        if (members.contains(playerName.toLowerCase()) && !officials.contains(playerName.toLowerCase())) {
            officials.add(playerName.toLowerCase());
        }
    }
    
    public void demotePlayer(String playerName) {
        officials.remove(playerName.toLowerCase());
    }

    public boolean isMember(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        return leader.equalsIgnoreCase(lowerCasePlayerName) || 
               officials.contains(lowerCasePlayerName) || 
               members.contains(lowerCasePlayerName);
    }
}