package neuralcoder3.servermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neuralcoder3.servermod.config.ConfigManager;
import neuralcoder3.servermod.network.CommandServer;

public class Servermod implements ModInitializer {
	public static final String MOD_ID = "server-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static CommandServer commandServer;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Server Mod...");

		// Load configuration
		ConfigManager.loadConfig();

		// Register server lifecycle events
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LOGGER.info("Server starting - initializing command server");
			commandServer = new CommandServer();
			commandServer.start(server);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LOGGER.info("Server stopping - shutting down command server");
			if (commandServer != null) {
				commandServer.stop();
			}
		});

		LOGGER.info("Server Mod initialized successfully!");
	}
}