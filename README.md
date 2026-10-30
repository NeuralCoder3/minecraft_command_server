# Server Mod - External Command Interface

This Minecraft Fabric mod provides an external command interface that allows you to send commands to your Minecraft server via a network socket.

Modloader: Fabric
Java version: 25+
Minecraft version: 26.3

## Installation

1. Build the mod using Gradle:
   ```bash
   ./gradlew build
   ```

2. Copy the generated JAR file from `build/libs/` to your Minecraft server's `mods/` folder

3. Start your Minecraft server

## Configuration

The mod creates a configuration file at `config/server-mod.json` with the following structure:

```json
{
  "port": 8080,
  "enabled": true
}
```

- `port`: The port number to listen on (default: 8080)
- `enabled`: Whether the command server is enabled (default: true)

## Usage

### Available Commands

#### Message Command
Sends a text message to the Minecraft console.

**Request:**
```json
{
  "command": "message",
  "text": "Hello from external script!"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Message sent to Minecraft chat"
}
```

#### Place Blocks Command
Places blocks in the world at specified coordinates.

**Request:**
```json
{
  "command": "placeblocks",
  "blocks": [
    {
      "x": 0,
      "y": 100,
      "z": 0,
      "block": "stone"
    },
    {
      "x": 1,
      "y": 100,
      "z": 0,
      "block": "minecraft:dirt"
    }
  ]
}
```

**Response:**
```json
{
  "status": "success",
  "placed": 2,
  "failed": 0
}
```

**Error Response (if some blocks fail):**
```json
{
  "status": "success",
  "placed": 1,
  "failed": 1,
  "errors": "Invalid block: invalid_block_name; "
}
```

#### Get Players Command
Queries the names and locations of all online players.

**Request:**
```json
{
  "command": "getplayers"
}
```

**Response:**
```json
{
  "status": "success",
  "count": 2,
  "players": [
    {
      "name": "Player1",
      "x": 100.5,
      "y": 64.0,
      "z": 200.3,
      "dimension": "minecraft:overworld"
    },
    {
      "name": "Player2",
      "x": -50.2,
      "y": 70.1,
      "z": 150.8,
      "dimension": "minecraft:overworld"
    }
  ]
}
```

#### Get Blocks Command (Efficient Array Format)
Queries blocks in a 16x16x16 area around a specified position. Returns a compact array of block names for maximum efficiency. Supports optional run-length encoding (RLE) compression.

**Request (Standard Format):**
```json
{
  "command": "getblocks",
  "x": 0,
  "y": 100,
  "z": 0
}
```

**Request (With RLE Compression):**
```json
{
  "command": "getblocks",
  "x": 0,
  "y": 100,
  "z": 0,
  "RLE": true
}
```

**Response (Standard Format):**
```json
{
  "status": "success",
  "center_x": 0,
  "center_y": 100,
  "center_z": 0,
  "count": 4096,
  "RLE": false,
  "blocks": [
    "air",
    "stone",
    "dirt",
    "grass_block"
  ]
}
```

**Response (With RLE Compression):**
```json
{
  "status": "success",
  "center_x": 0,
  "center_y": 100,
  "center_z": 0,
  "count": 4096,
  "RLE": true,
  "rle_count": 12,
  "compression_ratio": "0.00",
  "blocks": [
    {
      "count": 1000,
      "block": ""
    },
    {
      "count": 50,
      "block": "stone"
    },
    {
      "count": 25,
      "block": "dirt"
    }
  ]
}
```

#### Get Blocks Full Command (Detailed Format)
Queries blocks in a 16x16x16 area around a specified position. Returns detailed information including coordinates for each block.

**Request:**
```json
{
  "command": "getblocksfull",
  "x": 0,
  "y": 100,
  "z": 0
}
```

**Response:**
```json
{
  "status": "success",
  "center_x": 0,
  "center_y": 100,
  "center_z": 0,
  "count": 4096,
  "blocks": [
    {
      "x": -8,
      "y": 92,
      "z": -8,
      "block": "minecraft:dirt"
    },
    {
      "x": -7,
      "y": 92,
      "z": -8,
      "block": "minecraft:stone"
    }
  ]
}
```

### Example Usage

#### Python Example
```python
import socket
import json

def send_message(host='localhost', port=8080, message='Hello!'):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((host, port))
        cmd = {"command": "message", "text": message}
        s.send((json.dumps(cmd) + '\n').encode('utf-8'))
        response = s.recv(1024).decode('utf-8')
        print(f"Response: {response}")

send_message(message="Hello from Python!")

# Place some blocks
place_blocks_command = {
    "command": "placeblocks",
    "blocks": [
        {"x": 0, "y": 100, "z": 0, "block": "stone"},
        {"x": 1, "y": 100, "z": 0, "block": "dirt"},
        {"x": 2, "y": 100, "z": 0, "block": "grass_block"}
    ]
}
send_custom_command(place_blocks_command)

# Query players
getplayers_command = {"command": "getplayers"}
send_custom_command(getplayers_command)

# Query blocks around a position (efficient array format)
getblocks_command = {
    "command": "getblocks",
    "x": 0,
    "y": 100,
    "z": 0
}
send_custom_command(getblocks_command)

# Query blocks around a position (with RLE compression)
getblocks_rle_command = {
    "command": "getblocks",
    "x": 0,
    "y": 100,
    "z": 0,
    "RLE": True
}
send_custom_command(getblocks_rle_command)

# Query blocks around a position (detailed format)
getblocksfull_command = {
    "command": "getblocksfull",
    "x": 0,
    "y": 100,
    "z": 0
}
send_custom_command(getblocksfull_command)
```

#### Bash Example
```bash
echo '{"command": "message", "text": "Hello from bash!"}' | nc localhost 8080
```

## Block Names and Coordinates

### Block Names
Block names can be specified in two formats:
- **Short format**: `stone`, `dirt`, `grass_block` (automatically prefixed with `minecraft:`)
- **Full format**: `minecraft:stone`, `minecraft:dirt`, `minecraft:grass_block`

### Common Block Names
- `stone`, `dirt`, `grass_block`, `cobblestone`
- `oak_planks`, `birch_planks`, `spruce_planks`
- `glass`, `iron_block`, `gold_block`, `diamond_block`
- `redstone_block`, `lapis_block`, `emerald_block`
- `sand`, `gravel`, `clay`, `terracotta`

## Query Commands

### Player Information
The `getplayers` command returns:
- **Player name**: The display name of the player
- **Coordinates**: Exact X, Y, Z position (with decimal precision)
- **Dimension**: The dimension the player is currently in (e.g., `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`)

### Block Information
The mod provides two block query commands:

#### `getblocks` (Efficient Array Format)
- **Area**: Queries a 16x16x16 cube around the specified center position
- **Total blocks**: Returns up to 4096 blocks (16³)
- **Format**: Compact array of block names (e.g., `["air", "stone", "dirt"]`)
- **Block names**: Short format without namespace (e.g., `stone` instead of `minecraft:stone`)
- **Use case**: When you need maximum performance and don't need individual coordinates
- **Data size**: Much smaller response size (~10-20KB vs ~200-400KB for detailed format)
- **RLE Compression**: Optional run-length encoding for even better compression (e.g., `[{"count": 1000, "block": "air"}]`)
- **RLE Benefits**: Can reduce data size by 80-95% for areas with many identical consecutive blocks

#### `getblocksfull` (Detailed Format)
- **Area**: Queries a 16x16x16 cube around the specified center position
- **Total blocks**: Returns up to 4096 blocks (16³)
- **Format**: Array of objects with coordinates and block names
- **Block names**: Full format with namespace (e.g., `minecraft:stone`)
- **Coordinates**: Each block includes its exact X, Y, Z coordinates
- **Use case**: When you need to know the exact position of each block
- **World bounds**: Automatically skips blocks outside the world height limits
