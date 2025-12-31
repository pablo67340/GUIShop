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
- **In-Game Editor** - Configure shops directly from within the game
- **Command Items** - Sell commands that execute when purchased
- **Custom Items** - Full support for enchantments, potions, fireworks, and custom model data
- **Native Spawner Support** - Configure and sell mob spawners with any entity type
- **Native Player Head Support** - Use player UUIDs or Base64 skin textures for custom heads
- **PDC Support** - Uses Bukkit's Persistent Data Container API for reliable item identification
- **Per-Item Permissions** - Restrict specific items to certain player groups
- **PlaceholderAPI Support** - Use placeholders in shop names and lores
- **Vault Integration** - Works with any Vault-compatible economy plugin
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
/gs edit [shop]      - Enter creator mode to edit shops in-game
/gs edit [shop] [page] - Edit a specific page of a shop
/gs buyprice <price> - Set buy price of held item (use 'false' to disable)
/gs sellprice <price> - Set sell price of held item (use 'false' to disable)
/gs shopname <name>  - Set the display name in shop
/gs buyname <name>   - Set the name on purchased item
/gs enchant <enchant:level> - Add enchantment to held item
/gs addlore <text>   - Add a line to the item's lore
/gs setslot <slot>   - Set slot position for current item
/gs parsemob <type>  - Validate a mob type for spawners
/gs toggleworth      - Toggle worth display for yourself (session only)
/gs iteminfo         - Display comprehensive info about held item (PDC, lore, etc.)
```

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
- Vault (for economy support)

**Optional:**
- PacketEvents (for worth display feature)
- PlaceholderAPI (for placeholder support)

---

## Installation

1. Download GUIShop and place it in your plugins folder
2. Install Vault and an economy plugin (EssentialsX, CMI, etc.)
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
