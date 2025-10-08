package neuralcoder3.servermod.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import neuralcoder3.servermod.Servermod;
import neuralcoder3.servermod.config.ConfigManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandServer {
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean running = false;
    private final Gson gson = new Gson();
    private MinecraftServer minecraftServer;

    public void start(MinecraftServer server) {
        this.minecraftServer = server;
        if (running) {
            Servermod.LOGGER.warn("Command server is already running");
            return;
        }

        int port = ConfigManager.getConfig().port;
        boolean enabled = ConfigManager.getConfig().enabled;

        if (!enabled) {
            Servermod.LOGGER.info("Command server is disabled in config");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            executorService = Executors.newFixedThreadPool(4);
            running = true;

            Servermod.LOGGER.info("Command server started on port {}", port);

            // Start accepting connections in a separate thread
            executorService.submit(this::acceptConnections);

        } catch (IOException e) {
            Servermod.LOGGER.error("Failed to start command server on port {}", port, e);
        }
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Servermod.LOGGER.error("Error closing server socket", e);
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        Servermod.LOGGER.info("Command server stopped");
    }

    private void acceptConnections() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    Servermod.LOGGER.error("Error accepting client connection", e);
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = reader.readLine()) != null && running) {
                try {
                    JsonObject command = gson.fromJson(line, JsonObject.class);
                    handleCommand(command, writer);
                } catch (JsonSyntaxException e) {
                    Servermod.LOGGER.warn("Received invalid JSON command: {}", line);
                    sendErrorResponse(writer, "Invalid JSON format");
                }
            }
        } catch (IOException e) {
            Servermod.LOGGER.debug("Client disconnected: {}", e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Servermod.LOGGER.debug("Error closing client socket", e);
            }
        }
    }

    private void handleCommand(JsonObject command, PrintWriter writer) {
        if (!command.has("command")) {
            sendErrorResponse(writer, "Missing 'command' field");
            return;
        }

        String commandType = command.get("command").getAsString();

        switch (commandType) {
            case "message":
                handleMessageCommand(command, writer);
                break;
            case "placeblocks":
                handlePlaceBlocksCommand(command, writer);
                break;
            default:
                sendErrorResponse(writer, "Unknown command: " + commandType);
                break;
        }
    }

    private void handleMessageCommand(JsonObject command, PrintWriter writer) {
        if (!command.has("text")) {
            sendErrorResponse(writer, "Missing 'text' field for message command");
            return;
        }

        String message = command.get("text").getAsString();
        
        // Send to Minecraft chat
        if (minecraftServer != null) {
            Text chatMessage = Text.literal("[EXTERNAL] " + message);
            minecraftServer.getPlayerManager().broadcast(chatMessage, false);
            Servermod.LOGGER.info("[EXTERNAL] {} (sent to chat)", message);
        } else {
            Servermod.LOGGER.warn("Minecraft server not available, logging to console only");
            Servermod.LOGGER.info("[EXTERNAL] {}", message);
        }
        
        // Send success response
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.addProperty("message", "Message sent to Minecraft chat");
        writer.println(gson.toJson(response));
    }

    private void handlePlaceBlocksCommand(JsonObject command, PrintWriter writer) {
        if (!command.has("blocks")) {
            sendErrorResponse(writer, "Missing 'blocks' field for placeblocks command");
            return;
        }

        if (minecraftServer == null) {
            sendErrorResponse(writer, "Minecraft server not available");
            return;
        }

        try {
            // Parse the blocks array from JSON
            String blocksJson = gson.toJson(command.get("blocks"));
            BlockPlacement[] blocks = gson.fromJson(blocksJson, BlockPlacement[].class);
            
            if (blocks == null || blocks.length == 0) {
                sendErrorResponse(writer, "No blocks provided");
                return;
            }

            ServerWorld world = minecraftServer.getOverworld();
            int placedCount = 0;
            int failedCount = 0;
            StringBuilder errors = new StringBuilder();

            for (BlockPlacement blockPlacement : blocks) {
                try {
                    // Validate block type
                    Block block = getBlockFromString(blockPlacement.block);
                    if (block == null) {
                        failedCount++;
                        errors.append("Invalid block: ").append(blockPlacement.block).append("; ");
                        continue;
                    }

                    // Validate coordinates (basic bounds check)
                    if (blockPlacement.y < -64 || blockPlacement.y > 320) {
                        failedCount++;
                        errors.append("Y coordinate out of bounds: ").append(blockPlacement.y).append("; ");
                        continue;
                    }

                    // Place the block
                    BlockPos pos = new BlockPos(blockPlacement.x, blockPlacement.y, blockPlacement.z);
                    world.setBlockState(pos, block.getDefaultState());
                    placedCount++;

                } catch (Exception e) {
                    failedCount++;
                    errors.append("Error placing block at (").append(blockPlacement.x)
                          .append(",").append(blockPlacement.y).append(",").append(blockPlacement.z)
                          .append("): ").append(e.getMessage()).append("; ");
                }
            }

            // Send response
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("placed", placedCount);
            response.addProperty("failed", failedCount);
            
            if (failedCount > 0) {
                response.addProperty("errors", errors.toString());
            }
            
            writer.println(gson.toJson(response));
            
            // Log to console
            Servermod.LOGGER.info("Placed {} blocks, {} failed", placedCount, failedCount);
            if (failedCount > 0) {
                Servermod.LOGGER.warn("Block placement errors: {}", errors.toString());
            }

        } catch (JsonSyntaxException e) {
            sendErrorResponse(writer, "Invalid JSON format for blocks array");
        } catch (Exception e) {
            sendErrorResponse(writer, "Error processing placeblocks command: " + e.getMessage());
            Servermod.LOGGER.error("Error in placeblocks command", e);
        }
    }

    private Block getBlockFromString(String blockName) {
        try {
            // Handle common block names without namespace
            if (!blockName.contains(":")) {
                blockName = "minecraft:" + blockName;
            }
            
            Identifier blockId = Identifier.tryParse(blockName);
            if (blockId == null) {
                return null;
            }
            
            return Registries.BLOCK.get(blockId);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendErrorResponse(PrintWriter writer, String errorMessage) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", errorMessage);
        writer.println(gson.toJson(response));
    }

    public boolean isRunning() {
        return running;
    }
}
