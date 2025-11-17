package com.blockycraft.blockyfactions;

import com.blockycraft.blockyfactions.api.BlockyFactionsAPI;
import com.blockycraft.blockyfactions.commands.FactionsCommandManager;
import com.blockycraft.blockyfactions.config.ConfigManager;
import com.blockycraft.blockyfactions.geoip.GeoIPManager;
import com.blockycraft.blockyfactions.lang.LanguageManager;
import com.blockycraft.blockyfactions.listeners.FactionPvpListener;
import com.blockycraft.blockyfactions.managers.FactionManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BlockyFactions extends JavaPlugin {

    private FactionManager factionManager;
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
        this.factionManager = new FactionManager(this);
        this.factionManager.loadFactions();

        // Inicializa API
        BlockyFactionsAPI.initialize(this);

        // Registra comandos e eventos
        registerCommands();
        registerEvents();

        System.out.println("[BlockyFactions] Plugin ativado com sucesso!");
        System.out.println("[BlockyFactions] Servidor Beta 1.7.3 detectado. Usando modo de compatibilidade.");
    }

    private void registerCommands() {
        getCommand("fac").setExecutor(new FactionsCommandManager(this));
        getCommand("fc").setExecutor(new FactionsCommandManager(this)); // Registro do comando /fc
        System.out.println("[BlockyFactions] Comandos registrados.");
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new FactionPvpListener(this), this);
        System.out.println("[BlockyFactions] Listeners de eventos registrados (PVP protection).");
    }

    @Override
    public void onDisable() {
        if (this.factionManager != null) {
            this.factionManager.saveFactions();
        }

        System.out.println("[BlockyFactions] Plugin desativado.");
    }

    public FactionManager getFactionManager() {
        return factionManager;
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
