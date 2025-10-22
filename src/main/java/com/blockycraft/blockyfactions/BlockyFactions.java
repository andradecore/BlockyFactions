package com.blockycraft.blockyfactions;

import com.blockycraft.blockyfactions.api.BlockyFactionsAPI;
import com.blockycraft.blockyfactions.commands.FactionsCommandManager;
import com.blockycraft.blockyfactions.managers.FactionManager;
// import com.blockycraft.blockywar.api.BlockyWarAPI; // Import removido (nao usado neste arquivo)
import org.bukkit.plugin.PluginManager; 
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import java.io.File;

public class BlockyFactions extends JavaPlugin {

    private static BlockyFactions instance; 
    private FactionManager factionManager;
    private int maxMembers;
    private boolean warHookEnabled = false; 

    @Override
    public void onEnable() {
        instance = this; 
        
        loadConfiguration();
        
        this.factionManager = new FactionManager(this);
        this.factionManager.loadFactions();

        BlockyFactionsAPI.initialize(this);
        
        // --- MODIFICADO ---
        // Agenda o setup do hook com BlockyWar para 1 tick depois,
        // garantindo que BlockyWar ja esteja totalmente carregado.
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                setupWarHook(); 
            }
        }, 1L); // 1L = Atraso de 1 tick
        // --- FIM DA MODIFICACAO ---

        registerCommands();

        System.out.println("[BlockyFactions] Plugin ativado com sucesso!");
    }

    private void loadConfiguration() {
        File configFile = new File(getDataFolder(), "config.yml");
        Configuration config = getConfiguration();

        if (!configFile.exists()) {
            System.out.println("[BlockyFactions] Criando config.yml padrao...");
            getDataFolder().mkdirs(); 
            config.setProperty("factions.max-members", 10);
            config.save();
        } else {
            config.load(); 
        }
        
        this.maxMembers = config.getInt("factions.max-members", 10);
        System.out.println("[BlockyFactions] Limite maximo de membros por faccao definido como: " + this.maxMembers);
    }

    /**
     * Verifica a presenca do BlockyWar.
     * Este metodo agora e chamado 1 tick apos o onEnable().
     */
    private void setupWarHook() {
        PluginManager pm = getServer().getPluginManager();
        // Neste ponto, BlockyWar ja deve estar habilitado se existir
        if (pm.isPluginEnabled("BlockyWar")) { 
            try {
                 Class.forName("com.blockycraft.blockywar.api.BlockyWarAPI"); 
                this.warHookEnabled = true;
                System.out.println("[BlockyFactions] Hook com BlockyWar ativado."); // Mensagem correta deve aparecer agora
            } catch (ClassNotFoundException e) {
                System.out.println("[BlockyFactions] ERRO: BlockyWar encontrado, mas BlockyWarAPI nao pode ser carregada.");
                this.warHookEnabled = false; 
            }
        } else {
            System.out.println("[BlockyFactions] BlockyWar nao encontrado. Integracao de notificacao de dissolucao desativada.");
            this.warHookEnabled = false;
        }
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
        instance = null; 
    }

    // --- Getters ---

    public static BlockyFactions getInstance() { 
        return instance;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public int getMaxMembers() {
        return maxMembers;
    }
    
    public boolean isWarHookEnabled() {
        // Retorna o estado atualizado do hook (que foi definido no setupWarHook agendado)
        return warHookEnabled;
    }
}