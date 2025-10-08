package neuralcoder3.servermod.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import neuralcoder3.servermod.Servermod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static ModConfig config = ModConfig.createDefault();
    private static final Path CONFIG_PATH = Paths.get("config", "server-mod.json");
    private static final Gson gson = new Gson();

    public static ModConfig getConfig() {
        return config;
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                config = parseConfig(json);
                Servermod.LOGGER.info("Loaded config: port={}, enabled={}", config.port, config.enabled);
            } else {
                saveConfig();
                Servermod.LOGGER.info("Created default config file at {}", CONFIG_PATH);
            }
        } catch (IOException e) {
            Servermod.LOGGER.error("Failed to load config, using defaults", e);
            config = ModConfig.createDefault();
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = gson.toJson(config);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            Servermod.LOGGER.error("Failed to save config", e);
        }
    }

    private static ModConfig parseConfig(String json) {
        try {
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            
            int port = 8080;
            boolean enabled = true;
            
            if (jsonObject.has("port") && jsonObject.get("port").isJsonPrimitive()) {
                port = jsonObject.get("port").getAsInt();
            }
            
            if (jsonObject.has("enabled") && jsonObject.get("enabled").isJsonPrimitive()) {
                enabled = jsonObject.get("enabled").getAsBoolean();
            }
            
            return new ModConfig(port, enabled);
        } catch (JsonSyntaxException e) {
            Servermod.LOGGER.warn("Invalid JSON in config file, using defaults", e);
            return ModConfig.createDefault();
        }
    }
}
