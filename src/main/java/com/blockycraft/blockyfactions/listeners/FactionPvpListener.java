package com.blockycraft.blockyfactions.listeners;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;
import com.blockycraft.blockyfactions.commands.FactionsCommandManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;

public class FactionPvpListener extends EntityListener {

    private final BlockyFactions plugin;
    // Precisa da referência ao FactionsCommandManager para registrar dano
    private final FactionsCommandManager commandManager;

    public FactionPvpListener(BlockyFactions plugin) {
        this.plugin = plugin;
        // Supondo que o manager seja obtido via plugin.getCommand("fac").getExecutor()
        this.commandManager = (FactionsCommandManager) plugin.getCommand("fac").getExecutor();
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        // Verifica se o evento foi cancelado antes
        if (event.isCancelled()) {
            return;
        }

        // Verifica se é um evento de dano causado por entidade
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return;
        }

        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;

        // Verifica se ambos são jogadores
        Entity damager = damageEvent.getDamager();
        Entity victim = event.getEntity();

        if (!(damager instanceof Player) || !(victim instanceof Player)) {
            return;
        }

        Player attacker = (Player) damager;
        Player target = (Player) victim;

        // --- REGISTRA O DANO SOFRIDO PELO TARGET ---
        commandManager.setLastDamage(target.getName());

        // Obtém as facções de ambos os jogadores
        Faction attackerFaction = plugin.getFactionManager().getPlayerFaction(attacker.getName());
        Faction targetFaction = plugin.getFactionManager().getPlayerFaction(target.getName());

        // Se algum não está em facção, permite o dano
        if (attackerFaction == null || targetFaction == null) {
            return;
        }

        // Se estão na mesma facção
        if (attackerFaction.getName().equalsIgnoreCase(targetFaction.getName())) {
            // Se o PVP está desativado, cancela o dano
            if (!attackerFaction.isPvpEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage("§bPVP entre membros da faccao esta desativado.");
            }
            // Se o PVP está ativado, permite o dano (não faz nada)
        }
    }
}
