package com.blockycraft.blockyfactions;

import com.blockycraft.blockyfactions.api.BlockyFactionsAPI;
import com.blockycraft.blockyfactions.commands.FactionsCommandManager;
import com.blockycraft.blockyfactions.config.ConfigManager;
import com.blockycraft.blockyfactions.listeners.FactionPvpListener;
import com.blockycraft.blockyfactions.managers.FactionManager;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockyFactions extends JavaPlugin {
    
    private FactionManager factionManager;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        // Carrega configuracoes
        this.configManager = new ConfigManager(this);
        
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
        System.out.println("[BlockyFactions] Comandos registrados.");
    }
    
    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        FactionPvpListener pvpListener = new FactionPvpListener(this);
        
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, pvpListener, Event.Priority.Highest, this);
        
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
}
