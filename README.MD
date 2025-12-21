# GUIShop

The ultimate GUI-based shop plugin for Minecraft servers. Say goodbye to confusing sign shops and chest-based systems. GUIShop provides players with an intuitive shopping experience while giving administrators complete control over pricing, items, and layout.

**Fully compatible with Paper/Spigot 1.13 - 1.21.10**

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/pablo67340/GUIShop)
[![Wiki](https://img.shields.io/badge/Wiki-Documentation-green)](https://github.com/pablo67340/GUIShop/wiki)
[![Discord](https://img.shields.io/badge/Discord-Support-purple)](https://discord.gg/wm6zMzdBwf)
[![SpigotMC](https://img.shields.io/badge/SpigotMC-Download-orange)](https://www.spigotmc.org/resources/guishop.2451/)

## Features

- **Easy-to-Use GUI System** - Players browse shops through clean, organized inventory menus
- **Multi-Page Shops** - Create shops with unlimited pages for large item catalogs
- **Dynamic Pricing** - Optional supply/demand based pricing system
- **Worth Display System** - Shows item sell values directly in item lore (requires PacketEvents)
- **In-Game Editor** - Configure shops directly from within the game
- **Command Items** - Sell commands that execute when purchased
- **Custom Items** - Full support for enchantments, potions, fireworks, spawners, and custom NBT
- **Per-Item Permissions** - Restrict specific items to certain player groups
- **PlaceholderAPI Support** - Use placeholders in shop names and lores
- **Vault Integration** - Works with any Vault-compatible economy plugin

## Requirements

**Required:**
- Java 17+
- Vault

**Optional:**
- PacketEvents (for worth display feature)
- PlaceholderAPI (for placeholder support)

## Installation

1. Download GUIShop from [SpigotMC](https://www.spigotmc.org/resources/guishop.2451/)
2. Place the JAR in your `plugins` folder
3. Install Vault and an economy plugin
4. Restart your server
5. Configure shops in `plugins/GUIShop/shops.yml`

## Quick Start

```yaml
# Example shop item in shops.yml
shops:
  Blocks:
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

| Command | Description |
|---------|-------------|
| `/shop` | Open the shop menu |
| `/sell` | Open the sell GUI |
| `/value` | Check item buy/sell values |
| `/gs reload` | Reload configuration |
| `/gs edit [shop]` | Enter in-game editor |

## Permissions

| Permission | Description |
|------------|-------------|
| `guishop.use` | Access to GUIShop |
| `guishop.sell` | Access to /sell command |
| `guishop.value` | Access to /value command |
| `guishop.reload` | Reload configuration |
| `guishop.creator` | In-game editor access |

## Documentation

- [Installation Guide](https://github.com/pablo67340/GUIShop/wiki/Installation)
- [Shop Configuration](https://github.com/pablo67340/GUIShop/wiki/Shop-Configuration)
- [Special Item Configurations](https://github.com/pablo67340/GUIShop/wiki/Special-Item-Configurations)
- [Worth Display](https://github.com/pablo67340/GUIShop/wiki/Worth-Display-Configuration)
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
```

See [API Documentation](https://github.com/pablo67340/GUIShop/wiki/API-Documentation) for details.

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
