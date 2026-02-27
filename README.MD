# GUIShop

The ultimate GUI-based shop plugin for Minecraft servers. Say goodbye to confusing sign shops and chest-based systems. GUIShop provides players with an intuitive shopping experience while giving administrators complete control over pricing, items, and layout.

**Fully compatible with Paper/Spigot/Folia/Bukkit 1.13 - 1.21.11**

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/pablo67340/GUIShop)
[![Wiki](https://img.shields.io/badge/Wiki-Documentation-green)](https://github.com/pablo67340/GUIShop/wiki)
[![Discord](https://img.shields.io/badge/Discord-Support-purple)](https://discord.gg/wm6zMzdBwf)
[![SpigotMC](https://img.shields.io/badge/SpigotMC-Download-orange)](https://www.spigotmc.org/resources/guishop.2451/)

## Features

- **Easy-to-Use GUI System** - Players browse shops through clean, organized inventory menus
- **Multi-Page Shops** - Create shops with unlimited pages for large item catalogs
- **Dynamic Pricing** - Optional supply/demand system with per-item overrides and linked pricing (DIAMOND_BLOCK affects DIAMOND prices)
- **Worth Display System** - Shows item sell values directly in item lore (requires PacketEvents)
- **Full GUI-Based Item Editor** - Edit prices, names, enchantments through intuitive GUIs
- **Drag-and-Drop Shop Building** - Place and rearrange items by simply dragging them
- **Built-In Economy** - Optional internal economy system (no external economy plugin needed!)
- **Command Items** - Sell commands that execute when purchased
- **Custom Items** - Full support for enchantments, potions, fireworks, spawners, and custom NBT
- **Native Spawner Support** - Configure and sell mob spawners with any entity type
- **Native Player Head Support** - Use player UUIDs or Base64 skin textures
- **Per-Item Permissions** - Restrict specific items to certain player groups
- **PlaceholderAPI Support** - Use placeholders in shop names and lores
- **Statistics Tracking** - Track player purchases and sales with PlaceholderAPI integration
- **Vault Integration** - Works with any Vault-compatible economy plugin
- **Folia Support** - Full compatibility with Paper's regionized multithreading fork

## Requirements

**Required:**
- Java 17+
- Vault

**Optional:**
- Economy Plugin (EssentialsX, CMI, etc.) - OR use GUIShop's built-in economy!
- PacketEvents (for worth display feature)
- PlaceholderAPI (for placeholder support)

## Installation

1. Download GUIShop from [SpigotMC](https://www.spigotmc.org/resources/guishop.2451/)
2. Place the JAR in your `plugins` folder
3. Install Vault and an economy plugin (or enable GUIShop's internal economy)
4. Restart your server
5. Configure shops in `plugins/GUIShop/shops/` folder

## Quick Start

```yaml
# Example shop item in shops/Blocks.yml
pages:
  Page0:
    '0':
      type: SHOP
      id: DIAMOND
      buy-price: 100
      sell-price: 50
      shop-name: '&bDiamond'
```

See the [Wiki](https://github.com/pablo67340/GUIShop/wiki) for comprehensive documentation.

## Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/shop`, `/buy` | Open the shop menu |
| `/sell` | Open the sell GUI |
| `/value` | Check item buy/sell values |
| `/bal`, `/balance` | Check your balance (internal economy) |
| `/pay <player> <amount>` | Send money (internal economy) |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/gs reload` | Reload all configuration files |
| `/gs edit` | Enter editor mode for the menu |
| `/gs edit menu` | Enter editor mode for the menu |
| `/gs edit transaction` | Enter editor mode for the transaction GUI |
| `/gs edit <shop>` | Enter editor mode for a specific shop |
| `/gs toggleworth` | Toggle worth display for yourself |
| `/gs iteminfo` | Display comprehensive info about held item |

### Economy Admin Commands

| Command | Description |
|---------|-------------|
| `/gs eco give <player> <amount>` | Give money to a player |
| `/gs eco take <player> <amount>` | Take money from a player |
| `/gs eco set <player> <amount>` | Set a player's balance |
| `/gs eco balance <player>` | Check a player's balance |
| `/gs eco reset <player>` | Reset to starting balance |

All amounts support abbreviations: `1k`, `1.5M`, `100B`, etc.

## In-Game Editor

GUIShop features a powerful GUI-based editor:

1. Enter creator mode with `/gs edit` or `/gs edit <shop>`
2. **Left-click** to drag and drop items to rearrange layout
3. **Right-click** or **Shift+click** any item to open the Item Editor GUI
4. Click on any setting to modify it through chat input or nested GUIs
5. Changes save automatically when you close the inventory

### Editor Modes

| Mode | Command | Description |
|------|---------|-------------|
| Menu Editor | `/gs edit` | Edit the main shop menu |
| Shop Editor | `/gs edit <shop>` | Edit a specific shop |
| Transaction Editor | `/gs edit transaction` | Edit the buy/sell GUI layout |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `guishop.use` | Access to GUIShop | op |
| `guishop.sell` | Access to /sell command | true |
| `guishop.value` | Access to /value command | true |
| `guishop.reload` | Reload configuration | op |
| `guishop.creator` | In-game editor access | op |
| `guishop.admin` | Full admin access | op |
| `guishop.shop.*` | Access to all shop items | op |

## Documentation

- [Installation Guide](https://github.com/pablo67340/GUIShop/wiki/Installation)
- [Quick Start Guide](https://github.com/pablo67340/GUIShop/wiki/Quick-Start-Guide)
- [Shop Configuration](https://github.com/pablo67340/GUIShop/wiki/Shop-Configuration)
- [In-Game Item Editor](https://github.com/pablo67340/GUIShop/wiki/In-Game-Item-Editor)
- [Command Items](https://github.com/pablo67340/GUIShop/wiki/Command-Items)
- [Dynamic Pricing](https://github.com/pablo67340/GUIShop/wiki/Dynamic-Pricing)
- [Special Item Configurations](https://github.com/pablo67340/GUIShop/wiki/Special-Item-Configurations)
- [Internal Economy](https://github.com/pablo67340/GUIShop/wiki/Internal-Economy)
- [Worth Display](https://github.com/pablo67340/GUIShop/wiki/Worth-Display-Configuration)
- [Statistics API](https://github.com/pablo67340/GUIShop/wiki/Statistics-API)
- [API Documentation](https://github.com/pablo67340/GUIShop/wiki/API-Documentation)

## API for Developers

GUIShop provides a comprehensive API:

```java
// Check if an item can be sold
boolean canSell = GUIShopAPI.canBeSold(itemStack);

// Get prices
BigDecimal buyPrice = GUIShopAPI.getBuyPrice(itemStack, quantity);
BigDecimal sellPrice = GUIShopAPI.getSellPrice(itemStack, quantity);

// Sell items programmatically
GUIShopAPI.sellItems(player, SellType.COMMAND, items);

// Worth display integration
GUIShopAPI.setExternalWorthCheck(player -> myPlugin.hasWorthDisabled(player));

// Statistics
StatisticsManager stats = GUIShop.getINSTANCE().getStatisticsManager();
double totalSpent = stats.getPlayerCache(player).getTotalSpent();
```

See [API Documentation](https://github.com/pablo67340/GUIShop/wiki/API-Documentation) for details.

## PlaceholderAPI Placeholders

```
%guishop_total_spent%           - Money spent (with commas)
%guishop_total_spent_formatted% - Money spent (abbreviated: 1.5M)
%guishop_total_earned%          - Money earned (with commas)
%guishop_total_earned_formatted% - Money earned (abbreviated)
%guishop_items_bought%          - Items purchased count
%guishop_items_sold%            - Items sold count
%guishop_top_bought_1%          - #1 most bought item
%guishop_top_sold_1%            - #1 most sold item
```

## Building from Source

```bash
git clone https://github.com/pablo67340/GUIShop.git
cd GUIShop
mvn clean install
```

The compiled JAR will be in the `target` folder.

## Support

- [GitHub Issues](https://github.com/pablo67340/GUIShop/issues)
- [Discord Server](https://discord.gg/wm6zMzdBwf)

## License

See [LICENSE](LICENSE) file.

---

Developed by **pablo67340**
