# Installation

This guide covers how to install GUIShop on your server.

## Requirements

- Minecraft server running Paper or Spigot 1.13+
- Java 17 or higher
- Vault plugin
- An economy plugin compatible with Vault (EssentialsX, CMI, etc.)

## Basic Installation

1. **Download GUIShop** from SpigotMC or build from source
2. **Place the JAR file** in your server's `plugins` folder
3. **Install Vault** if not already installed
4. **Install an economy plugin** (EssentialsX, CMI, etc.)
5. **Restart your server**

## Optional Dependencies

### PacketEvents (Recommended)
Required for the Worth Display feature that shows item sell values in item lore.

1. Download PacketEvents from [SpigotMC](https://www.spigotmc.org/resources/packetevents-api.80279/)
2. Place in your `plugins` folder
3. Restart server

### PlaceholderAPI
Enables placeholder support in shop names and lores.

1. Download PlaceholderAPI from SpigotMC
2. Place in your `plugins` folder
3. Restart server

## Post-Installation

After installation, GUIShop will create the following files in `plugins/GUIShop/`:

```
GUIShop/
  config.yml       - Main configuration
  shops.yml        - Shop layouts and items
  menu.yml         - Main menu configuration
  messages.yml     - Plugin messages
  worth.yml        - Worth display settings
  internal_messages.yml - System messages
```

## First Steps

1. Open `shops.yml` to customize your shops
2. Configure prices in `config.yml`
3. Use `/gs reload` to apply changes
4. Test with `/shop` command

## Upgrading

When upgrading GUIShop:

1. Back up your configuration files
2. Replace the plugin JAR
3. Restart the server
4. Check for any new configuration options

GUIShop will preserve your existing configuration while adding new options with defaults.
