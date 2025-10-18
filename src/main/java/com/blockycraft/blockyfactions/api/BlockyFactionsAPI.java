package com.blockycraft.blockyfactions.api;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;

public class BlockyFactionsAPI {

    private static BlockyFactions plugin;

    // Este método será chamado pela classe principal do plugin para que a API tenha acesso a ela.
    public static void initialize(BlockyFactions instance) {
        plugin = instance;
    }

    /**
     * Verifica se dois jogadores estão na mesma facção.
     * @param playerName1 Nome do primeiro jogador.
     * @param playerName2 Nome do segundo jogador.
     * @return true se ambos os jogadores estiverem na mesma facção, false caso contrário.
     */
    public static boolean arePlayersInSameFaction(String playerName1, String playerName2) {
        if (plugin == null) return false;

        Faction faction1 = plugin.getFactionManager().getPlayerFaction(playerName1);
        Faction faction2 = plugin.getFactionManager().getPlayerFaction(playerName2);

        // Se qualquer um dos jogadores não tiver facção, eles não estão na mesma facção.
        if (faction1 == null || faction2 == null) {
            return false;
        }

        // Compara o nome das facções.
        return faction1.getName().equalsIgnoreCase(faction2.getName());
    }

    /**
     * Obtém o nome do jogador definido como 'Fundo' (tesoureiro) da facção de um jogador.
     * @param playerName O nome do jogador cuja facção será consultada.
     * @return O nome do jogador tesoureiro, ou null se o jogador não tiver facção ou a facção não tiver fundo definido.
     */
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
}