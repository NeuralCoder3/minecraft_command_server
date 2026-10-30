package neuralcoder3.servermod.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import neuralcoder3.servermod.Servermod;
import neuralcoder3.servermod.config.ConfigManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
            case "getplayers":
                handleGetPlayersCommand(command, writer);
                break;
            case "getblocksfull":
                handleGetBlocksFullCommand(command, writer);
                break;
            case "getblocks":
                handleGetBlocksCommand(command, writer);
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
            Component chatMessage = Component.literal("[EXTERNAL] " + message);
            minecraftServer.getPlayerList().broadcastSystemMessage(chatMessage, false);
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

            ServerLevel world = minecraftServer.overworld();
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
                    world.setBlockAndUpdate(pos, block.defaultBlockState());
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
            
            return BuiltInRegistries.BLOCK.getValue(blockId);
        } catch (Exception e) {
            return null;
        }
    }

    private void handleGetPlayersCommand(JsonObject command, PrintWriter writer) {
        if (minecraftServer == null) {
            sendErrorResponse(writer, "Minecraft server not available");
            return;
        }

        try {
            List<PlayerInfo> players = new ArrayList<>();
            
            for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                String dimensionName = "unknown";
                if (player.level() != null) {
                    Identifier dimensionId = player.level().dimension().identifier();
                    dimensionName = dimensionId.toString();
                }
                
                PlayerInfo playerInfo = new PlayerInfo(
                    player.getName().getString(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    dimensionName
                );
                players.add(playerInfo);
            }

            // Send response
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("count", players.size());
            response.add("players", gson.toJsonTree(players));
            
            writer.println(gson.toJson(response));
            
            // Log to console
            Servermod.LOGGER.info("Queried {} players", players.size());

        } catch (Exception e) {
            sendErrorResponse(writer, "Error querying players: " + e.getMessage());
            Servermod.LOGGER.error("Error in getplayers command", e);
        }
    }

    private void handleGetBlocksFullCommand(JsonObject command, PrintWriter writer) {
        if (!command.has("x") || !command.has("y") || !command.has("z")) {
            sendErrorResponse(writer, "Missing 'x', 'y', or 'z' field for getblocks command");
            return;
        }

        if (minecraftServer == null) {
            sendErrorResponse(writer, "Minecraft server not available");
            return;
        }

        try {
            int centerX = command.get("x").getAsInt();
            int centerY = command.get("y").getAsInt();
            int centerZ = command.get("z").getAsInt();

            // Validate coordinates
            if (centerY < -64 || centerY > 320) {
                sendErrorResponse(writer, "Y coordinate out of bounds: " + centerY);
                return;
            }

            ServerLevel world = minecraftServer.overworld();
            List<BlockInfo> blocks = new ArrayList<>();

            // Query 16x16x16 area around the center position
            int radius = 8; // Half of 16
            for (int x = centerX - radius; x < centerX + radius; x++) {
                for (int y = centerY - radius; y < centerY + radius; y++) {
                    for (int z = centerZ - radius; z < centerZ + radius; z++) {
                        // Skip if Y is out of world bounds
                        if (y < -64 || y > 320) {
                            continue;
                        }

                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockState = world.getBlockState(pos);
                        Block block = blockState.getBlock();

                        if (blockState.isAir()) {
                            continue;
                        }
                        
                        // Get block name
                        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                        String blockName = blockId.toString();
                        
                        BlockInfo blockInfo = new BlockInfo(x, y, z, blockName);
                        blocks.add(blockInfo);
                    }
                }
            }

            // Send response
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("center_x", centerX);
            response.addProperty("center_y", centerY);
            response.addProperty("center_z", centerZ);
            response.addProperty("count", blocks.size());
            response.add("blocks", gson.toJsonTree(blocks));
            
            writer.println(gson.toJson(response));
            
            // Log to console
            Servermod.LOGGER.info("Queried {} blocks around ({}, {}, {})", 
                blocks.size(), centerX, centerY, centerZ);

        } catch (Exception e) {
            sendErrorResponse(writer, "Error querying blocks: " + e.getMessage());
            Servermod.LOGGER.error("Error in getblocksfull command", e);
        }
    }

    private void handleGetBlocksCommand(JsonObject command, PrintWriter writer) {
        if (!command.has("x") || !command.has("y") || !command.has("z")) {
            sendErrorResponse(writer, "Missing 'x', 'y', or 'z' field for getblocks command");
            return;
        }

        if (minecraftServer == null) {
            sendErrorResponse(writer, "Minecraft server not available");
            return;
        }

        try {
            int centerX = command.get("x").getAsInt();
            int centerY = command.get("y").getAsInt();
            int centerZ = command.get("z").getAsInt();
            boolean useRLE = command.has("RLE") && command.get("RLE").getAsBoolean();

            // Validate coordinates
            if (centerY < -64 || centerY > 320) {
                sendErrorResponse(writer, "Y coordinate out of bounds: " + centerY);
                return;
            }

            ServerLevel world = minecraftServer.overworld();
            List<String> blocks = new ArrayList<>();

            // Query 16x16x16 area around the center position
            int radius = 8; // Half of 16
            for (int x = centerX - radius; x < centerX + radius; x++) {
                for (int y = centerY - radius; y < centerY + radius; y++) {
                    for (int z = centerZ - radius; z < centerZ + radius; z++) {
                        // Skip if Y is out of world bounds
                        if (y < -64 || y > 320) {
                            blocks.add(""); // Add empty string for out-of-bounds blocks
                            continue;
                        }

                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockState = world.getBlockState(pos);
                        if (blockState.isAir()) {
                            blocks.add(""); // Add empty string for air blocks
                            continue;
                        }
                        Block block = blockState.getBlock();
                        
                        // Get block name (short format without minecraft: prefix)
                        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                        String blockName = blockId.getPath(); // Get just the path part (e.g., "stone" instead of "minecraft:stone")
                        
                        blocks.add(blockName);
                    }
                }
            }

            // Send response
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("center_x", centerX);
            response.addProperty("center_y", centerY);
            response.addProperty("center_z", centerZ);
            response.addProperty("count", blocks.size());
            response.addProperty("RLE", useRLE);
            
            if (useRLE) {
                // Apply run-length encoding
                List<RLEBlock> rleBlocks = performRLE(blocks);
                response.add("blocks", gson.toJsonTree(rleBlocks));
                response.addProperty("rle_count", rleBlocks.size());
                response.addProperty("compression_ratio", String.format("%.2f", (double) rleBlocks.size() / blocks.size()));
            } else {
                response.add("blocks", gson.toJsonTree(blocks));
            }
            
            writer.println(gson.toJson(response));
            
            // Log to console
            if (useRLE) {
                Servermod.LOGGER.info("Queried {} blocks around ({}, {}, {}) with RLE compression: {} RLE entries", 
                    blocks.size(), centerX, centerY, centerZ, performRLE(blocks).size());
            } else {
                Servermod.LOGGER.info("Queried {} blocks around ({}, {}, {})", 
                    blocks.size(), centerX, centerY, centerZ);
            }

        } catch (Exception e) {
            sendErrorResponse(writer, "Error querying blocks: " + e.getMessage());
            Servermod.LOGGER.error("Error in getblocks command", e);
        }
    }

    private List<RLEBlock> performRLE(List<String> blocks) {
        List<RLEBlock> rleBlocks = new ArrayList<>();
        
        if (blocks.isEmpty()) {
            return rleBlocks;
        }
        
        String currentBlock = blocks.get(0);
        int count = 1;
        
        for (int i = 1; i < blocks.size(); i++) {
            String block = blocks.get(i);
            if (block.equals(currentBlock)) {
                count++;
            } else {
                rleBlocks.add(new RLEBlock(count, currentBlock));
                currentBlock = block;
                count = 1;
            }
        }
        
        // Add the last run
        rleBlocks.add(new RLEBlock(count, currentBlock));
        
        return rleBlocks;
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
