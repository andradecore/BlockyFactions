package com.blockycraft.blockyfactions;

import com.blockycraft.blockyfactions.commands.FactionsCommandManager;
import com.blockycraft.blockyfactions.managers.FactionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockyFactions extends JavaPlugin {

    private FactionManager factionManager;

    @Override
    public void onEnable() {
        // Inicializa os managers
        this.factionManager = new FactionManager(this);
        this.factionManager.loadFactions();

        // Registra os comandos
        registerCommands();

        System.out.println("[BlockyFactions] Plugin ativado com sucesso!");
        System.out.println("[BlockyFactions] Servidor Beta 1.7.3 detectado. Usando modo de compatibilidade.");
    }

    private void registerCommands() {
        getCommand("fac").setExecutor(new FactionsCommandManager(this));
        System.out.println("[BlockyFactions] Comandos registrados.");
    }

    @Override
    public void onDisable() {
        // Salva os dados das facções
        if (this.factionManager != null) {
            this.factionManager.saveFactions();
        }
        
        System.out.println("[BlockyFactions] Plugin desativado.");
    }

    // Getters para os managers
    public FactionManager getFactionManager() {
        return factionManager;
    }
}