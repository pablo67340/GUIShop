<div align="center">

![GUIShop Banner](https://bryces.site/guishop/banner.jpg)

# GUIShop
### The Ultimate GUI-Based Shop Plugin for Minecraft Servers

*Fully compatible with Paper/Spigot 1.21.10*

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/pablo67340/GUIShop)
[![Wiki](https://img.shields.io/badge/Wiki-Documentation-green)](https://github.com/pablo67340/GUIShop/wiki)
[![Discord](https://img.shields.io/badge/Discord-Support-purple)](https://discord.gg/wm6zMzdBwf)

</div>

---

## Overview

GUIShop is a powerful, feature-rich shop plugin that allows server owners to create beautiful GUI-based shops with extensive customization options. Say goodbye to confusing sign shops and chest-based systems. GUIShop provides players with an intuitive shopping experience while giving administrators complete control over pricing, items, and layout.

---

## Key Features

- **Easy-to-Use GUI System** - Players browse shops through clean, organized inventory menus
- **Multi-Page Shops** - Create shops with unlimited pages for large item catalogs
- **Dynamic Pricing** - Optional supply/demand based pricing system
- **Worth Display System** - Shows item sell values directly in item lore (requires PacketEvents)
- **Full GUI-Based Item Editor** - Edit item prices, names, enchantments, and more through an intuitive GUI
- **Drag-and-Drop Shop Building** - Place and rearrange items in shops by simply dragging them
- **Command Items** - Sell commands that execute when purchased
- **Custom Items** - Full support for enchantments, potions, fireworks, and custom model data
- **Native Spawner Support** - Configure and sell mob spawners with any entity type
- **Native Player Head Support** - Use player UUIDs or Base64 skin textures for custom heads
- **PDC Support** - Uses Bukkit's Persistent Data Container API for reliable item identification
- **Per-Item Permissions** - Restrict specific items to certain player groups
- **PlaceholderAPI Support** - Use placeholders in shop names and lores
- **Vault Integration** - Works with any Vault-compatible economy plugin
- **Built-In Economy** - Optional internal economy system (no external economy plugin needed!)
- **Alternate Sell GUI** - Sell items without leaving the shop
- **Transaction Logging** - Track all purchases and sales

---

## Screenshots

<details>
<summary>Click to view screenshots</summary>

*Main Menu*

![Main Menu](https://bryces.site/guishop/menu.jpg)

*Shop View*

![Shop View](https://bryces.site/guishop/shop.jpg)

*Item Editor*

![Item Editor](https://bryces.site/guishop/editor.jpg)

*Quantity Selector*

![Quantity Selector](https://bryces.site/guishop/quantity.jpg)

*Worth Display*

![Worth Display](https://bryces.site/guishop/worth.jpg)

</details>

---

## Commands

### Player Commands
```
/shop, /buy          - Open the shop menu
/sell                - Open the sell GUI
/value, /val         - Check the buy/sell value of held item
```

### Admin Commands
```
/gs reload           - Reload all configuration files
/gs edit             - Enter creator mode for the main menu
/gs edit menu        - Enter creator mode for the main menu
/gs edit [shop]      - Enter creator mode for a specific shop
/gs edit [shop] [page] - Edit a specific page of a shop
/gs parsemob <type>  - Validate a mob type for spawners
/gs toggleworth      - Toggle worth display for yourself (session only)
/gs iteminfo         - Display comprehensive info about held item
```

### Economy Commands
(Only available when internal economy is enabled)

**Player Commands:**
```
/bal, /balance, /money          - Check your balance
/bal <player>                   - Check another player's balance
/pay <player> <amount>          - Send money to a player
```

**Admin Commands:**
```
/gs eco give <player> <amount>  - Give money to a player
/gs eco take <player> <amount>  - Take money from a player
/gs eco set <player> <amount>   - Set a player's balance
/gs eco balance <player>        - Check a player's balance
/gs eco reset <player>          - Reset to starting balance
```
All amounts support abbreviations: 1k, 1.5M, 100B, etc.

### Item Info Command

The `/gs iteminfo` command is a powerful debugging and configuration helper. Hold any item and run the command to see:

- Material type and display name
- All lore lines
- Enchantments in config-ready format (e.g., `SHARPNESS:5 UNBREAKING:3`)
- Potion info in config-ready format (type, splash, extended, upgraded)
- Firework info in config-ready format (flight, explosions, colors)
- All PDC (Persistent Data Container) values
- Custom NBT data

This makes it easy to configure complex items - just create the item you want, then use `/gs iteminfo` to get the exact format needed for your `shops.yml`.

---

## Permissions

```
guishop.use          - Access to the shop (default: op)
guishop.shop.*       - Access to all shop items (default: op)
guishop.shop.<shop>.<item> - Access to specific items
guishop.reload       - Permission to reload the plugin (default: op)
guishop.creator      - Access to the in-game shop editor (default: op)
guishop.value        - Access to /value command (default: true)
guishop.sell         - Access to /sell command (default: true)
guishop.admin        - Full admin access (default: op)
```

---

## Configuration

GUIShop uses several configuration files for maximum flexibility:

- **config.yml** - General plugin settings, commands, titles, buttons
- **shops.yml** - Shop layouts and item definitions
- **menu.yml** - Main menu configuration
- **messages.yml** - All plugin messages (fully customizable)
- **worth.yml** - Worth display system configuration

<details>
<summary>Example Shop Item Configuration</summary>

```yaml
shops:
  Blocks:
    pages:
      Page0:
        '0':
          type: SHOP              # Item type (SHOP, COMMAND, BLANK, DUMMY)
          id: STONE               # Material ID
          buy-price: 10.0         # Price to buy (or 'false' to disable)
          sell-price: 5.0         # Price to sell (or 'false' to disable)
          shop-name: '&7Stone'    # Display name in shop
          buy-name: '&7Stone'     # Name on purchased item
          shop-lore:              # Lore shown in shop
            - '&fA basic building block'
          enchantments: 'DURA:1 SHARP:2'  # Enchantments (space separated)
          quantity: 64            # Stack size to give
          permission: 'shop.vip'  # Required permission (optional)
```

</details>

---

## GUI-Based Item Editor

GUIShop features a powerful, fully GUI-based item editor that eliminates the need for manual config editing. Configure every aspect of shop items through intuitive click-based menus.

### How It Works

1. Enter creator mode with `/gs edit` (for menu) or `/gs edit <shop>` (for shops)
2. **Left-click** to drag and drop items - move items within the shop, rearrange positions, or place new items from your inventory into the shop
3. **Right-click** or **Shift+click** any item to open the Item Editor GUI
4. Click on any setting to modify it through chat input or nested selection GUIs
5. Changes save automatically when you close the inventory

### Creator Mode Controls
```
Left-click     - Pick up / place items (works between shop and your inventory)
Right-click    - Open Item Editor for the clicked item
Shift+click    - Open Item Editor for the clicked item
```

### Item Editor Features

- **Buy/Sell Prices** - Set prices with support for abbreviated formats (1k, 1.5M, 100B)
- **Item Type** - Switch between SHOP, COMMAND, and DUMMY types
- **Display Names** - Set shop display name and purchased item name
- **Custom Lore** - Add descriptive lore lines to items
- **Enchantments** - Add/remove enchantments through a visual enchantment picker
- **Potion Effects** - Configure potion type, duration, and amplifier via GUI
- **Firework Properties** - Set flight duration, explosion shapes, colors, and effects
- **Spawner Mob Type** - Select mob type from a visual entity picker
- **Commands** - Configure commands to run on purchase
- **Permissions** - Set required permissions per item
- **Quantity** - Set stack size given on purchase

<details>
<summary>Item Editor Screenshots</summary>

*Item Editor Main View*

![Item Editor](https://bryces.site/guishop/editor.jpg)

*Enchantment Selector*

![Enchantment Editor](https://bryces.site/guishop/enchant-editor.jpg)

*Potion Editor*

![Potion Editor](https://bryces.site/guishop/potion-editor.jpg)

</details>

---

## Worth Display System

GUIShop includes a powerful worth display feature that shows item sell values directly in the item lore. This uses packet manipulation to display worth client-side only, meaning your actual items are never modified.

**Requirements:** PacketEvents plugin

<details>
<summary>Worth Display Configuration (worth.yml)</summary>

```yaml
enabled: true
format: "&7Worth: &a%worth%"
position: BOTTOM
add-blank-line: true
only-show-sellable: true
not-sellable-format: "&7Worth: &cNot sellable"

# Hide worth in specific inventories
blacklisted-inventories:
  - "Auction House"
  - "Crate Preview"

# Hide worth on items with specific names
blacklisted-item-names:
  - "Crate Key"
  - "Vote Token"

# Hide worth on equipped armor
hide-armor-slots: true
```

</details>

---

## Advanced Item Types

### Command Items
Sell commands that execute when purchased:
```yaml
'19':
  type: COMMAND
  id: NETHER_STAR
  shop-name: '&6VIP Rank'
  buy-price: 10000
  commands:
    - 'lp user {PLAYER_NAME} parent set vip'
    - 'broadcast {PLAYER_NAME} purchased VIP!'
```

### Enchanted Books
```yaml
'20':
  type: SHOP
  id: ENCHANTED_BOOK
  buy-price: 1000
  enchantments: 'SHARP:5 FIRE_ASPECT:2'
```

### Potions
```yaml
'21':
  type: SHOP
  id: POTION
  buy-price: 500
  potion-info:
    type: SPEED
    splash: false
    extended: true
    upgraded: false
```

### Spawners
Native spawner support with any valid entity type (use `/gs parsemob <type>` to validate):
```yaml
'22':
  type: SHOP
  id: SPAWNER
  buy-price: 50000
  mob-type: ZOMBIE
  shop-name: '&6Zombie Spawner'
```

### Fireworks
```yaml
'23':
  type: SHOP
  id: FIREWORK_ROCKET
  buy-price: 100
  firework-info:
    flight: 2
    explosions:
      - shape: ball_large
        flicker: true
        trail: true
        colors: [11743532, 2437522]
        fade-colors: [1973019, 15790320]
```

### Player Heads
Support for player UUIDs or Base64 skin textures:
```yaml
# Using player UUID
'24':
  type: SHOP
  id: PLAYER_HEAD
  buy-price: 1000
  skull-uuid: 'player-uuid-here'

# Using Base64 skin texture (from minecraft-heads.com, etc.)
'25':
  type: SHOP
  id: PLAYER_HEAD
  buy-price: 500
  skull-uuid: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjMTIzIn19fQ=='
```

---

## Statistics & PlaceholderAPI

GUIShop tracks player shop statistics and integrates with PlaceholderAPI for use in scoreboards, holograms, and chat.

### Tracked Statistics
- Total money spent (buying from shops)
- Total money earned (selling items)
- Total items bought/sold
- Top 3 most bought items per player
- Top 3 most sold items per player

### Available Placeholders
```
%guishop_total_spent%           - Money spent (with commas)
%guishop_total_spent_formatted% - Money spent (abbreviated: 1.5M)
%guishop_total_earned%          - Money earned (with commas)
%guishop_total_earned_formatted% - Money earned (abbreviated)
%guishop_items_bought%          - Items purchased count
%guishop_items_bought_formatted% - Items purchased (abbreviated)
%guishop_items_sold%            - Items sold count
%guishop_items_sold_formatted%  - Items sold (abbreviated)
%guishop_top_bought_1%          - #1 most bought item (Material: Qty)
%guishop_top_sold_1%            - #1 most sold item (Material: Qty)
```

Statistics are stored in SQLite (`plugins/GUIShop/Data/player_statistics.db`).

---

## Developer API

GUIShop provides a comprehensive API for developers to integrate with:

<details>
<summary>API Examples</summary>

```java
// Check if an item can be sold
boolean canSell = GUIShopAPI.canBeSold(itemStack);

// Get buy/sell prices
BigDecimal buyPrice = GUIShopAPI.getBuyPrice(itemStack, quantity);
BigDecimal sellPrice = GUIShopAPI.getSellPrice(itemStack, quantity);

// Sell items programmatically
GUIShopAPI.sellItems(player, SellType.COMMAND, items);

// Worth Display API
BigDecimal worth = GUIShopAPI.getItemWorth(itemStack);
BigDecimal stackWorth = GUIShopAPI.getStackWorth(itemStack);

// Per-player worth toggle
GUIShopAPI.toggleWorthForPlayer(player);
GUIShopAPI.enableWorthForPlayer(player);
GUIShopAPI.disableWorthForPlayer(player);

// Hook for persistent per-player worth settings
GUIShopAPI.setExternalWorthCheck(player -> {
    return myPlugin.hasWorthDisabled(player.getUniqueId());
});
```

</details>

Full API documentation available on the [Wiki](https://github.com/pablo67340/GUIShop/wiki).

---

## Dependencies

**Required:**
- Vault (for economy API)

**Optional:**
- Economy Plugin (EssentialsX, CMI, etc.) - OR use GUIShop's built-in economy!
- PacketEvents (for worth display feature)
- PlaceholderAPI (for placeholder support)

> **Note:** GUIShop includes its own economy system! Enable it in `economy.yml` to use GUIShop as your server's economy without needing EssentialsX or similar plugins.

---

## Installation

1. Download GUIShop and place it in your plugins folder
2. Install Vault and an economy plugin (EssentialsX, CMI, etc.) - OR enable GUIShop's internal economy
3. (Optional) Install PacketEvents for worth display
4. (Optional) Install PlaceholderAPI for placeholder support
5. Restart your server
6. Configure shops in `plugins/GUIShop/shops.yml`
7. Use `/gs reload` to apply changes

---

## Support

- [Wiki Documentation](https://github.com/pablo67340/GUIShop/wiki)
- [GitHub Issues](https://github.com/pablo67340/GUIShop/issues)
- [Discord Server](https://discord.gg/wm6zMzdBwf)

---

<div align="center">

Developed by **pablo67340**

If you enjoy GUIShop, please consider leaving a review!

</div>
