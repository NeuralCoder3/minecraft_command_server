#!/usr/bin/env python3
"""
Test script for the Server Mod command interface.
This script demonstrates how to send commands to the mod's network server.
"""

import socket
import json
import time

def send_command(host='localhost', port=8080, command='message', text='Hello from external script!'):
    """Send a command to the server mod."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((host, port))
            cmd = { "command": command, "text": text }
            s.send((json.dumps(cmd) + '\n').encode('utf-8'))
            response = s.recv(1024).decode('utf-8')
            print(f"Response: {response}")
            
    except ConnectionRefusedError:
        print(f"Could not connect to server on {host}:{port}")
        print("Make sure the Minecraft server is running with the mod loaded")
    except Exception as e:
        print(f"Error: {e}")

def send_custom_command(command_data, host='localhost', port=8080):
    """Send a custom command object to the server mod."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((host, port))
            s.send((json.dumps(command_data) + '\n').encode('utf-8'))
            response = s.recv(1024).decode('utf-8')
            print(f"Response: {response}")
            
    except ConnectionRefusedError:
        print(f"Could not connect to server on {host}:{port}")
        print("Make sure the Minecraft server is running with the mod loaded")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    print("Server Mod Command Test Script")
    print("==============================")
    
    # Send a message
    print("\n1. Testing with message:")
    send_command(text="This is a test message from Python!")
    
    # Place some blocks
    print("\n2. Testing placeblocks command:")
    placeblocks_command = {
        "command": "placeblocks",
        "blocks": [
            {"x": 0, "y": 130, "z": 0, "block": "stone"},
            {"x": 1, "y": 130, "z": 0, "block": "minecraft:dirt"},
            {"x": 2, "y": 130, "z": 0, "block": "grass_block"},
            {"x": 3, "y": 130, "z": 0, "block": "oak_planks"}
        ]
    }
    send_custom_command(placeblocks_command)
    
    print("\nDone! Check your Minecraft server console for the messages and world for placed blocks.")
