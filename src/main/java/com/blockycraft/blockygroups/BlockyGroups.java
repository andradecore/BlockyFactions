package com.blockycraft.blockygroups;

import com.blockycraft.blockygroups.api.BlockyGroupsAPI;
import com.blockycraft.blockygroups.commands.GroupsCommandManager;
import com.blockycraft.blockygroups.config.ConfigManager;
import com.blockycraft.blockygroups.geoip.GeoIPManager;
import com.blockycraft.blockygroups.lang.LanguageManager;
import com.blockycraft.blockygroups.listeners.GroupPvpListener;
import com.blockycraft.blockygroups.managers.GroupManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BlockyGroups extends JavaPlugin {

    private GroupManager groupManager;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private GeoIPManager geoIPManager;

    @Override
    public void onEnable() {
        // Carrega configuracoes
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.geoIPManager = new GeoIPManager();

        // Inicializa managers
        this.groupManager = new GroupManager(this);
        this.groupManager.loadGroups();

        // Inicializa API
        BlockyGroupsAPI.initialize(this);

        // Registra comandos e eventos
        registerCommands();
        registerEvents();

        System.out.println("[BlockyGroups] Plugin ativado com sucesso!");
        System.out.println("[BlockyGroups] Servidor Beta 1.7.3 detectado. Usando modo de compatibilidade.");
    }

    private void registerCommands() {
        getCommand("grp").setExecutor(new GroupsCommandManager(this));
        getCommand("gc").setExecutor(new GroupsCommandManager(this)); // Registro do comando /gc
        System.out.println("[BlockyGroups] Comandos registrados.");
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new GroupPvpListener(this), this);
        System.out.println("[BlockyGroups] Listeners de eventos registrados (PVP protection).");
    }

    @Override
    public void onDisable() {
        if (this.groupManager != null) {
            this.groupManager.saveGroups();
        }

        System.out.println("[BlockyGroups] Plugin desativado.");
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public GeoIPManager getGeoIPManager() {
        return geoIPManager;
    }

    private final Map<String, Long> teleportCooldowns = new HashMap<>();

    public void setLastDamage(String playerName) {
        teleportCooldowns.put(playerName.toLowerCase(), System.currentTimeMillis());
    }

    public Map<String, Long> getTeleportCooldowns() {
        return teleportCooldowns;
    }
}
