package com.blockycraft.blockyfactions.config;

import com.blockycraft.blockyfactions.BlockyFactions;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ConfigManager {

    private final BlockyFactions plugin;
    private final File configFile;
    private Properties properties;

    public ConfigManager(BlockyFactions plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.properties");
        this.properties = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        // Cria o diretório se não existir
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Se o arquivo não existe, copia o padrão dos resources
        if (!configFile.exists()) {
            copyDefaultConfig();
        }

        // Carrega as propriedades do arquivo
        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            System.out.println("[BlockyFactions] Configurações carregadas de config.properties");
        } catch (IOException e) {
            System.err.println("[BlockyFactions] Erro ao carregar config.properties: " + e.getMessage());
        }
    }

    private void copyDefaultConfig() {
        try {
            InputStream defaultConfig = plugin.getClass().getResourceAsStream("/config.properties");
            if (defaultConfig == null) {
                System.err.println("[BlockyFactions] Arquivo config.properties padrão não encontrado nos resources!");
                return;
            }

            OutputStream output = new FileOutputStream(configFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = defaultConfig.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.close();
            defaultConfig.close();

            System.out.println("[BlockyFactions] Arquivo config.properties padrão criado.");
        } catch (IOException e) {
            System.err.println("[BlockyFactions] Erro ao criar config.properties: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        properties.clear();
        loadConfig();
    }

    // Métodos utilitários para obter configurações

    public int getMaxMembers() {
        return getInt("factions.max-members", 10);
    }

    public String getMessage(String key) {
        return colorize(getString("msg." + key, "Mensagem não encontrada: " + key));
    }

    public String getMessage(String key, Object... args) {
        String message = getMessage(key);
        return colorize(String.format(message, args));
    }

    public String getHelpMessage(String key) {
        return colorize(getString("help." + key, ""));
    }

    public String getInfoMessage(String key) {
        return colorize(getString("info." + key, ""));
    }

    public String getInfoMessage(String key, Object... args) {
        String message = getInfoMessage(key);
        return colorize(String.format(message, args));
    }

    public String getRankMessage(String key) {
        return colorize(getString("rank." + key, ""));
    }

    public String getRankMessage(String key, Object... args) {
        String message = getRankMessage(key);
        return colorize(String.format(message, args));
    }

    // Método para aplicar cor (usar sempre antes de enviar mensagem ao jogador)
    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    // Métodos básicos de propriedades

    private String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
