package com.blockycraft.blockygroups.listeners;

import com.blockycraft.blockygroups.BlockyGroups;
import com.blockycraft.blockygroups.data.Group;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class GroupPvpListener implements Listener {

    private final BlockyGroups plugin;

    public GroupPvpListener(BlockyGroups plugin) {
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

        Group attackerGroup = plugin.getGroupManager().getPlayerGroup(attacker.getName());
        Group targetGroup = plugin.getGroupManager().getPlayerGroup(target.getName());

        if (attackerGroup == null || targetGroup == null) {
            return;
        }

        if (attackerGroup.getName().equalsIgnoreCase(targetGroup.getName())) {
            if (!attackerGroup.isPvpEnabled()) {
                event.setCancelled(true);
                String lang = plugin.getGeoIPManager().getPlayerLanguage(attacker);
                attacker.sendMessage(plugin.getLanguageManager().get(lang, "error.pvp-disabled"));
            }
        }
    }
}
