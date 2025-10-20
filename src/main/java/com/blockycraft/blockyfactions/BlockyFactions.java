package com.blockycraft.blockyfactions;

import com.blockycraft.blockyfactions.api.BlockyFactionsAPI;
import com.blockycraft.blockyfactions.commands.FactionsCommandManager;
import com.blockycraft.blockyfactions.managers.FactionManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import java.io.File;

public class BlockyFactions extends JavaPlugin {

    private FactionManager factionManager;
    private int maxMembers;

    @Override
    public void onEnable() {
        loadConfiguration();
        
        this.factionManager = new FactionManager(this);
        this.factionManager.loadFactions();

        BlockyFactionsAPI.initialize(this);

        registerCommands();

        System.out.println("[BlockyFactions] Plugin ativado com sucesso!");
        System.out.println("[BlockyFactions] Servidor Beta 1.7.3 detectado. Usando modo de compatibilidade.");
    }

    private void loadConfiguration() {
        File configFile = new File(getDataFolder(), "config.yml");
        Configuration config = getConfiguration();

        if (!configFile.exists()) {
            System.out.println("[BlockyFactions] Criando config.yml padrao...");
            config.setProperty("factions.max-members", 10);
            config.save();
        }
        
        config.load();
        this.maxMembers = config.getInt("factions.max-members", 10);
        System.out.println("[BlockyFactions] Limite maximo de membros por faccao definido como: " + this.maxMembers);
    }

    private void registerCommands() {
        getCommand("fac").setExecutor(new FactionsCommandManager(this));
        System.out.println("[BlockyFactions] Comandos registrados.");
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

    public int getMaxMembers() {
        return maxMembers;
    }
}