# Server Mod - External Command Interface

This Minecraft Fabric mod provides an external command interface that allows you to send commands to your Minecraft server via a network socket.

Modloader: Fabric
Java version: 21+
Minecraft version: 1.21.8

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

place_blocks_command = {
    "command": "placeblocks",
    "blocks": [
        {"x": 0, "y": 100, "z": 0, "block": "stone"},
        {"x": 1, "y": 100, "z": 0, "block": "dirt"},
        {"x": 2, "y": 100, "z": 0, "block": "grass_block"}
    ]
}
send_custom_command(place_blocks_command)
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
