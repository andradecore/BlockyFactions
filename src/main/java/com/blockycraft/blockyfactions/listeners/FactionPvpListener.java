package com.blockycraft.blockyfactions.listeners;

import com.blockycraft.blockyfactions.BlockyFactions;
import com.blockycraft.blockyfactions.data.Faction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FactionPvpListener implements Listener {

    private final BlockyFactions plugin;

    public FactionPvpListener(BlockyFactions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (!(damager instanceof Player) || !(victim instanceof Player)) {
            return;
        }

        Player attacker = (Player) damager;
        Player target = (Player) victim;

        plugin.setLastDamage(target.getName());

        Faction attackerFaction = plugin.getFactionManager().getPlayerFaction(attacker.getName());
        Faction targetFaction = plugin.getFactionManager().getPlayerFaction(target.getName());

        if (attackerFaction == null || targetFaction == null) {
            return;
        }

        if (attackerFaction.getName().equalsIgnoreCase(targetFaction.getName())) {
            if (!attackerFaction.isPvpEnabled()) {
                event.setCancelled(true);
                String lang = plugin.getGeoIPManager().getPlayerLanguage(attacker);
                attacker.sendMessage(plugin.getLanguageManager().get(lang, "error.pvp-disabled"));
            }
        }
    }
}
