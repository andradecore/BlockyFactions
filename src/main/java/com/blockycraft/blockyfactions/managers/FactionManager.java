package com.blockycraft.blockyfactions.managers;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;
import com.blockycraft.blockywar.api.BlockyWarAPI; // <-- Import adicionado
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
    private final Map<String, Faction> factions = new HashMap<String, Faction>();
    private final Map<String, String> playerToFactionMap = new HashMap<String, String>();
    private final Map<String, String> pendingInvites = new HashMap<String, String>();

    public FactionManager(BlockyFactions plugin) {
        this.plugin = plugin;
    }

    // --- LÓGICA DE CRIAÇÃO ---

    public boolean createFaction(String name, String tag, Player leader) {
        String randomColor = generateRandomHexColor();
        return createFactionInternal(name, tag, leader, randomColor);
    }

    public boolean createFaction(String name, String tag, Player leader, String colorHex) {
        if (!isValidHexCode(colorHex)) {
            leader.sendMessage("§bO codigo de cor '" + colorHex + "' e invalido. Use o formato #RRGGBB.");
            return false;
        }
        return createFactionInternal(name, tag, leader, colorHex);
    }

    private boolean createFactionInternal(String name, String tag, Player leader, String colorHex) {
        if (factions.containsKey(name.toLowerCase()) || isTagInUse(tag)) {
            leader.sendMessage("§bUma faccao com este nome ou tag ja existe.");
            return false;
        }

        if (playerToFactionMap.containsKey(leader.getName().toLowerCase())) {
            leader.sendMessage("§bVoce ja pertence a uma faccao. Saia da sua faccao atual para criar uma nova.");
            return false;
        }

        if (name.length() > 20 || tag.length() > 5 || !name.matches("^[a-zA-Z0-9]+$") || !tag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage("§bNome ou tag invalidos. Use apenas letras e numeros, sem espacos. Maximo de 20 caracteres para o nome e 5 para a tag.");
            return false;
        }

        Faction newFaction = new Faction(name, tag, leader.getName());
        newFaction.setColorHex(colorHex.toUpperCase());
        
        factions.put(name.toLowerCase(), newFaction);
        playerToFactionMap.put(leader.getName().toLowerCase(), name);
        
        saveFactionToFile(newFaction);

        leader.sendMessage("§aFaccao '" + name + "' criada com sucesso com a cor " + colorHex.toUpperCase() + "!");
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
            player.sendMessage("§bVoce nao pertence a nenhuma faccao.");
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
                
                player.sendMessage("§aVoce deixou a lideranca da faccao " + faction.getName() + ". " + newLeaderName + " e o novo lider.");
                Player newLeader = plugin.getServer().getPlayerExact(newLeaderName); // Usa getPlayerExact
                if (newLeader != null && newLeader.isOnline()) {
                    newLeader.sendMessage("§aO lider deixou a faccao. Voce foi promovido a novo lider da faccao " + faction.getName() + "!");
                }
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    if (faction.isMember(onlinePlayer.getName()) && 
                        !onlinePlayer.getName().equalsIgnoreCase(newLeaderName) && 
                        !onlinePlayer.getName().equalsIgnoreCase(playerName)) {   
                        onlinePlayer.sendMessage("§fO lider " + playerName + " deixou a faccao. " + newLeaderName + " e o novo lider!");
                    }
                }
            }
        } else { 
            faction.removeMember(playerName); 
            playerToFactionMap.remove(playerName.toLowerCase()); 
            saveFactionToFile(faction); 
            player.sendMessage("§aVoce saiu da faccao " + faction.getName() + ".");
            Player leader = plugin.getServer().getPlayerExact(faction.getLeader()); // Usa getPlayerExact
            if (leader != null && leader.isOnline()) {
                leader.sendMessage("§fO jogador " + playerName + " saiu da sua faccao.");
            }
        }
    }

    public List<Faction> getRankedFactions() {
        List<Faction> factionsList = new ArrayList<Faction>(factions.values());
        factionsList.sort(new Comparator<Faction>() {
            @Override
            public int compare(Faction f1, Faction f2) {
                reloadFactionNetWorth(f1);
                reloadFactionNetWorth(f2);
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
        } else {
            faction.setNetWorth(0.0);
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
            inviter.sendMessage("§bVoce nao esta em uma faccao para poder convidar alguem.");
            return;
        }

        if (faction.getSize() >= plugin.getMaxMembers()) {
            inviter.sendMessage("§bSua faccao atingiu o limite maximo de " + plugin.getMaxMembers() + " membros e nao pode convidar mais ninguem.");
            return;
        }

        String inviterNameLower = inviter.getName().toLowerCase();
        if (!faction.getLeader().equalsIgnoreCase(inviterNameLower) && !faction.getOfficials().contains(inviterNameLower)) {
            inviter.sendMessage("§bVoce precisa ser o lider ou um oficial para convidar jogadores.");
            return;
        }
        if (getPlayerFaction(targetName) != null) {
            inviter.sendMessage("§bO jogador " + targetName + " ja pertence a uma faccao.");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(targetName); 
        if (target == null || !target.isOnline()) {
            inviter.sendMessage("§bO jogador " + targetName + " nao esta online.");
            return;
        }
        pendingInvites.put(target.getName().toLowerCase(), faction.getName());
        inviter.sendMessage("§aVoce convidou " + target.getName() + " para a sua faccao.");
        target.sendMessage("§fVoce foi convidado para entrar na faccao " + faction.getName() + ".");
        target.sendMessage("§fDigite §b/fac entrar " + faction.getName() + " §fpara aceitar.");
    }

    public void joinFaction(Player player, String factionName) {
        String playerNameLower = player.getName().toLowerCase();
        if (getPlayerFaction(playerNameLower) != null) {
            player.sendMessage("§bVoce ja esta em uma faccao.");
            return;
        }
        String invitedToFaction = pendingInvites.get(playerNameLower);
        if (invitedToFaction == null || !invitedToFaction.equalsIgnoreCase(factionName)) {
            player.sendMessage("§bVoce nao tem um convite para a faccao " + factionName + ".");
            return;
        }
        Faction faction = getFactionByName(invitedToFaction);
        if (faction == null) {
            player.sendMessage("§bEsta faccao nao existe mais.");
            pendingInvites.remove(playerNameLower); 
            return;
        }

        if (faction.getSize() >= plugin.getMaxMembers()) {
            player.sendMessage("§bA faccao '" + faction.getName() + "' esta cheia e nao e possivel entrar.");
            pendingInvites.remove(playerNameLower); 
            return;
        }

        pendingInvites.remove(playerNameLower); 
        faction.addMember(player.getName()); 
        playerToFactionMap.put(playerNameLower, faction.getName()); 
        saveFactionToFile(faction); 
        
        player.sendMessage("§aVoce entrou na faccao " + faction.getName() + "!");
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName()) && !onlinePlayer.getName().equalsIgnoreCase(player.getName())) { 
                onlinePlayer.sendMessage("§f" + player.getName() + " entrou para a faccao!");
            }
        }
    }

    public void listFactionInfo(Player viewer, String factionName) {
        Faction faction = getFactionByName(factionName);
        if (faction == null) {
            viewer.sendMessage("§bA faccao '" + factionName + "' nao foi encontrada.");
            return;
        }
        reloadFactionNetWorth(faction); 
        
        viewer.sendMessage("§f--- Informacoes da Faccao ---");
        viewer.sendMessage("§bNome: §f" + faction.getName() + " §7[" + faction.getTag() + "]");
        viewer.sendMessage("§bCor: §f" + faction.getColorHex());
        viewer.sendMessage("§bLider: §f" + faction.getLeader());
        
        String treasuryPlayer = faction.getTreasuryPlayer();
        viewer.sendMessage("§bTesoureiro: §f" + (treasuryPlayer == null || treasuryPlayer.isEmpty() ? "Nenhum definido" : treasuryPlayer));
        
        viewer.sendMessage("§bPatrimonio: §f" + String.format("%.2f", faction.getNetWorth()) + " barras"); 
        viewer.sendMessage("§bPVP Interno: " + (faction.isPvpEnabled() ? "§cAtivado" : "§aDesativado")); 
        
        String officials = String.join(", ", faction.getOfficials());
        viewer.sendMessage("§bOficiais (" + faction.getOfficials().size() + "): §f" + (officials.isEmpty() ? "Nenhum" : officials));
        
        ArrayList<String> plainMembers = new ArrayList<>();
        for (String member : faction.getMembers()) {
             if (!faction.getOfficials().contains(member.toLowerCase()) && 
                 !faction.getLeader().equalsIgnoreCase(member) &&
                 !(faction.getTreasuryPlayer() != null && faction.getTreasuryPlayer().equalsIgnoreCase(member))) { // Verifica null antes de equalsIgnoreCase
                 plainMembers.add(member);
             }
        }
        String members = String.join(", ", plainMembers);
        viewer.sendMessage("§bMembros (" + plainMembers.size() + "): §f" + (members.isEmpty() ? "Nenhum" : members));
        
        viewer.sendMessage("§bTotal de Membros: §f" + faction.getSize() + "/" + plugin.getMaxMembers());
        viewer.sendMessage("§f--------------------------");
    }

    public void kickPlayer(Player kicker, String targetName) {
        Faction faction = getPlayerFaction(kicker.getName());
        if (faction == null) {
            kicker.sendMessage("§bVoce nao esta em uma faccao.");
            return;
        }
        String kickerNameLower = kicker.getName().toLowerCase();
        if (!faction.getLeader().equalsIgnoreCase(kickerNameLower) && !faction.getOfficials().contains(kickerNameLower)) {
            kicker.sendMessage("§bVoce precisa ser o lider ou um oficial para expulsar jogadores.");
            return;
        }
        Faction targetFaction = getPlayerFaction(targetName);
        if (targetFaction == null || !targetFaction.getName().equals(faction.getName())) {
            kicker.sendMessage("§bO jogador " + targetName + " nao esta na sua faccao.");
            return;
        }
        String targetNameLower = targetName.toLowerCase();
        if (faction.getLeader().equalsIgnoreCase(targetNameLower)) {
            kicker.sendMessage("§bVoce nao pode expulsar o lider da faccao.");
            return;
        }
        if (faction.getOfficials().contains(targetNameLower) && !faction.getLeader().equalsIgnoreCase(kickerNameLower)) {
            kicker.sendMessage("§bOficiais nao podem expulsar outros oficiais. Apenas o lider pode.");
            return;
        }
        
        faction.removeMember(targetName); 
        playerToFactionMap.remove(targetNameLower); 
        saveFactionToFile(faction); 
        
        kicker.sendMessage("§aVoce expulsou " + targetName + " da faccao.");
        Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§bVoce foi expulso da faccao " + faction.getName() + ".");
        }
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName())) { 
                 onlinePlayer.sendMessage("§f" + targetName + " foi expulso da faccao por " + kicker.getName() + ".");
            }
        }
    }

    public void setPlayerRank(Player promoter, String targetName, String rank) {
        Faction faction = getPlayerFaction(promoter.getName());
        if (faction == null) { promoter.sendMessage("§bVoce nao esta em uma faccao."); return; }
        if (!faction.getLeader().equalsIgnoreCase(promoter.getName())) { promoter.sendMessage("§bApenas o lider da faccao pode alterar cargos."); return; }
        
        String targetNameLower = targetName.toLowerCase();
        String treasuryPlayer = faction.getTreasuryPlayer(); // Cache para evitar null checks repetidos
        if ((treasuryPlayer != null && targetNameLower.equalsIgnoreCase(treasuryPlayer)) || targetNameLower.equalsIgnoreCase(promoter.getName())) {
            promoter.sendMessage("§bVoce nao pode alterar o cargo deste jogador.");
            return;
        }
        
        boolean isCurrentOfficial = faction.getOfficials().contains(targetNameLower);
        boolean isCurrentMember = faction.getMembers().contains(targetNameLower);

        if (!isCurrentOfficial && !isCurrentMember) { 
             promoter.sendMessage("§b" + targetName + " nao e um membro valido para alterar o cargo."); 
             return; 
        }

        if (rank.equalsIgnoreCase("oficial")) {
            if (isCurrentOfficial) { promoter.sendMessage("§b" + targetName + " ja e um oficial."); return; }
            faction.promotePlayer(targetName); 
            saveFactionToFile(faction);
            promoter.sendMessage("§a" + targetName + " foi promovido a Oficial.");
        } else if (rank.equalsIgnoreCase("membro")) {
             if (!isCurrentOfficial) { promoter.sendMessage("§b" + targetName + " ja e um membro."); return; }
            faction.demotePlayer(targetName); 
            saveFactionToFile(faction);
            promoter.sendMessage("§a" + targetName + " foi rebaixado a Membro.");
        } else {
            promoter.sendMessage("§bCargo invalido. Use 'oficial' ou 'membro'.");
            return;
        }
        Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§fSeu cargo na faccao foi alterado para " + rank.substring(0, 1).toUpperCase() + rank.substring(1) + ".");
        }
    }

    public void setTreasuryPlayer(Player leader, String targetName) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) { leader.sendMessage("§bVoce nao esta em uma faccao."); return; }
        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) { leader.sendMessage("§bApenas o lider pode definir o tesoureiro da faccao."); return; }

        String oldTesoureiro = faction.getTreasuryPlayer();
        String targetNameLower = targetName.toLowerCase();

        if (targetName.equalsIgnoreCase("nenhum")) {
            if (oldTesoureiro != null && !oldTesoureiro.isEmpty()) {
                faction.setTreasuryPlayer(""); 
                faction.addMember(oldTesoureiro); 
                saveFactionToFile(faction);
                leader.sendMessage("§aO cargo de Tesoureiro da faccao esta vago.");
            } else {
                leader.sendMessage("§bNenhum tesoureiro esta definido."); 
            }
            return;
        }
        
        if(targetNameLower.equalsIgnoreCase(leader.getName())) {
             leader.sendMessage("§bO Lider nao pode ser o Tesoureiro.");
             return;
        }
        if(faction.getOfficials().contains(targetNameLower)) {
             leader.sendMessage("§bUm Oficial nao pode ser o Tesoureiro.");
             return;
        }
        if(!faction.getMembers().contains(targetNameLower)) { 
             leader.sendMessage("§bO jogador " + targetName + " nao e um membro valido para ser Tesoureiro."); 
             return; 
        }

        if (oldTesoureiro != null && !oldTesoureiro.isEmpty()) {
            faction.addMember(oldTesoureiro); 
        }
        
        faction.setTreasuryPlayer(targetName); 
        faction.getMembers().remove(targetNameLower); 
        saveFactionToFile(faction);
        leader.sendMessage("§aVoce definiu " + targetName + " como o tesoureiro da faccao.");
        
        Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
        if(targetPlayer != null && targetPlayer.isOnline()) {
             targetPlayer.sendMessage("§aVoce foi definido como Tesoureiro da sua faccao!");
        }
        if(oldTesoureiro != null && !oldTesoureiro.isEmpty()) {
            Player oldTargetPlayer = plugin.getServer().getPlayerExact(oldTesoureiro);
             if(oldTargetPlayer != null && oldTargetPlayer.isOnline()) {
                 oldTargetPlayer.sendMessage("§fVoce nao e mais o Tesoureiro da faccao.");
             }
        }
    }

    public void transferLeadership(Player oldLeader, String newLeaderName) {
        Faction faction = getPlayerFaction(oldLeader.getName());
        if (faction == null) { oldLeader.sendMessage("§bVoce nao esta em uma faccao."); return; }
        if (!faction.getLeader().equalsIgnoreCase(oldLeader.getName())) { oldLeader.sendMessage("§bVoce nao e o lider desta faccao."); return; }
        
        String newLeaderNameLower = newLeaderName.toLowerCase();
        String treasuryPlayer = faction.getTreasuryPlayer();
        
        if ((treasuryPlayer != null && newLeaderNameLower.equalsIgnoreCase(treasuryPlayer)) || newLeaderNameLower.equalsIgnoreCase(oldLeader.getName())) {
            oldLeader.sendMessage("§bVoce nao pode transferir a lideranca para este jogador.");
            return;
        }
        
        boolean isOfficial = faction.getOfficials().contains(newLeaderNameLower);
        boolean isMember = faction.getMembers().contains(newLeaderNameLower);

        if (!isOfficial && !isMember) {
            oldLeader.sendMessage("§b" + newLeaderName + " nao e um membro ou oficial valido para receber a lideranca.");
            return;
        }
        
        String oldLeaderName = oldLeader.getName();
        
        faction.setLeader(newLeaderName); 
        
        if (isOfficial) {
            faction.getOfficials().remove(newLeaderNameLower);
        } else {
            faction.getMembers().remove(newLeaderNameLower);
        }
        
        faction.addMember(oldLeaderName); 
        
        saveFactionToFile(faction);
        
        oldLeader.sendMessage("§aVoce transferiu a lideranca da faccao para " + newLeaderName + ".");
        Player newLeader = plugin.getServer().getPlayerExact(newLeaderName);
        if (newLeader != null && newLeader.isOnline()) {
            newLeader.sendMessage("§aVoce agora e o novo lider da faccao " + faction.getName() + "!");
        }
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (faction.isMember(onlinePlayer.getName()) && 
                !onlinePlayer.getName().equalsIgnoreCase(newLeaderName) && 
                !onlinePlayer.getName().equalsIgnoreCase(oldLeaderName)) { 
                 onlinePlayer.sendMessage("§f" + newLeaderName + " e o novo lider da faccao!");
            }
        }
    }

    public void setFactionPvp(Player leader, String status) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) {
            leader.sendMessage("§bVoce nao esta em uma faccao.");
            return;
        }
        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage("§bApenas o lider pode alterar o status de PVP da faccao.");
            return;
        }
        if (status.equalsIgnoreCase("on")) {
            if (faction.isPvpEnabled()) {
                leader.sendMessage("§bPVP interno ja esta ATIVADO.");
            } else {
                faction.setPvpEnabled(true);
                leader.sendMessage("§cPVP entre membros da faccao agora esta ATIVADO.");
                saveFactionToFile(faction);
            }
        } else if (status.equalsIgnoreCase("off")) {
             if (!faction.isPvpEnabled()) {
                leader.sendMessage("§aPVP interno ja esta DESATIVADO.");
            } else {
                faction.setPvpEnabled(false);
                leader.sendMessage("§aPVP entre membros da faccao agora esta DESATIVADO.");
                saveFactionToFile(faction);
            }
        } else {
            leader.sendMessage("§bUse: /fac pvp <on|off>");
        }
    }

    public void setFactionTag(Player leader, String newTag) {
        Faction faction = getPlayerFaction(leader.getName());
        if (faction == null) {
            leader.sendMessage("§bVoce nao esta em uma faccao.");
            return;
        }
        if (!faction.getLeader().equalsIgnoreCase(leader.getName())) {
            leader.sendMessage("§bApenas o lider pode alterar a tag da faccao.");
            return;
        }
        if (newTag.length() > 5 || !newTag.matches("^[a-zA-Z0-9]+$")) {
            leader.sendMessage("§bTag invalida. Use apenas letras e numeros, sem espacos. Maximo de 5 caracteres.");
            return;
        }
        if (isTagInUse(newTag) && !faction.getTag().equalsIgnoreCase(newTag)) { 
            leader.sendMessage("§bA tag '" + newTag + "' ja esta em uso.");
            return;
        }
        faction.setTag(newTag);
        saveFactionToFile(faction);
        leader.sendMessage("§aA tag da sua faccao foi alterada para [" + newTag + "].");
    }

    /**
     * Dissolve uma faccao. Chamado quando o ultimo membro (lider) sai.
     * @param faction A faccao a ser dissolvida.
     * @param leader O jogador lider que causou a dissolucao.
     */
    private void dissolveFaction(Faction faction, Player leader) {
        String factionName = faction.getName(); 
        
        // ---> NOTIFICACAO PARA BLOCKYWAR <---
        // Verifica se o hook esta ativo antes de chamar a API
        if (plugin.isWarHookEnabled()) {
            BlockyWarAPI.notifyFactionDissolved(factionName); 
        }
        // ---> FIM DA NOTIFICACAO <---

        factions.remove(factionName.toLowerCase());
        
        playerToFactionMap.remove(leader.getName().toLowerCase());
        
        File factionFile = new File(plugin.getDataFolder() + "/factions", factionName.toLowerCase() + ".yml");
        if (factionFile.exists()) {
            if (!factionFile.delete()) {
                 System.out.println("[BlockyFactions] ERRO: Nao foi possivel deletar o arquivo da faccao dissolvida: " + factionFile.getPath());
            }
        }
        leader.sendMessage("§bComo ultimo membro, voce dissolveu a faccao " + factionName + ".");
        System.out.println("[BlockyFactions] Faccao '" + factionName + "' dissolvida por " + leader.getName());
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
        File factionsDir = new File(plugin.getDataFolder(), "factions");
         if (!factionsDir.exists()) {
            factionsDir.mkdirs();
        }
        
        File factionFile = new File(factionsDir, faction.getName().toLowerCase() + ".yml");
        Configuration factionConfig = new Configuration(factionFile);
        
        factionConfig.setProperty("nome", faction.getName());
        factionConfig.setProperty("tag", faction.getTag());
        factionConfig.setProperty("cor", faction.getColorHex());
        factionConfig.setProperty("lider", faction.getLeader());
        factionConfig.setProperty("oficiais", faction.getOfficials()); 
        factionConfig.setProperty("membros", faction.getMembers());   
        factionConfig.setProperty("tesoureiro", faction.getTreasuryPlayer());
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
        
        factions.clear(); 
        playerToFactionMap.clear();
        
        for (File factionFile : files) {
            if (factionFile.getName().endsWith(".yml")) {
                Configuration factionConfig = new Configuration(factionFile);
                factionConfig.load();
                String name = factionConfig.getString("nome");
                String tag = factionConfig.getString("tag");
                String leader = factionConfig.getString("lider");
                
                if (name == null || tag == null || leader == null) {
                    System.out.println("[BlockyFactions] ERRO: Arquivo de faccao invalido (faltando nome, tag ou lider): " + factionFile.getName());
                    continue; 
                }
                
                Faction faction = new Faction(name, tag, leader);
                faction.setColorHex(factionConfig.getString("cor", "#FFFFFF")); 
                
                List<String> officials = factionConfig.getStringList("oficiais", new ArrayList<String>());
                List<String> members = factionConfig.getStringList("membros", new ArrayList<String>());
                faction.getOfficials().addAll(officials);
                faction.getMembers().addAll(members);

                faction.setTreasuryPlayer(factionConfig.getString("tesoureiro", "")); 
                faction.setNetWorth(factionConfig.getDouble("net_worth", 0.0)); 
                faction.setPvpEnabled(factionConfig.getBoolean("pvp-habilitado", false)); 
                
                factions.put(name.toLowerCase(), faction);
                
                playerToFactionMap.put(leader.toLowerCase(), name);
                for (String official : officials) {
                    playerToFactionMap.put(official.toLowerCase(), name);
                }
                for (String member : members) {
                    playerToFactionMap.put(member.toLowerCase(), name);
                }
                 String treasuryPlayer = faction.getTreasuryPlayer();
                 if (treasuryPlayer != null && !treasuryPlayer.isEmpty()) {
                    playerToFactionMap.put(treasuryPlayer.toLowerCase(), name);
                }
            }
        }
        System.out.println("[BlockyFactions] " + factions.size() + " faccoes carregadas.");
    }

    public void saveFactions() {
        // Log reduzido para evitar spam no console durante desligamentos normais
        // System.out.println("[BlockyFactions] Salvando " + factions.size() + " faccoes...");
        for (Faction faction : factions.values()) {
            saveFactionToFile(faction);
        }
        // System.out.println("[BlockyFactions] Fim do salvamento.");
    }

    public Faction getFactionByName(String name) {
        if (name == null) return null;
        return factions.get(name.toLowerCase());
    }

    public Faction getPlayerFaction(String playerName) {
        if (playerName == null) return null;
        String factionName = playerToFactionMap.get(playerName.toLowerCase());
        if (factionName != null) {
            return getFactionByName(factionName); 
        }
        return null;
    }
}