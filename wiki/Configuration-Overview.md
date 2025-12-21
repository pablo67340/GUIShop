# Configuration Overview

GUIShop uses multiple configuration files to provide maximum flexibility. This page provides an overview of all configuration files.

## Configuration Files

| File | Purpose |
|------|---------|
| `config.yml` | Main plugin settings, commands, buttons, titles |
| `shops.yml` | Shop layouts and item definitions |
| `menu.yml` | Main menu configuration |
| `messages.yml` | All player-facing messages |
| `worth.yml` | Worth display system settings |
| `internal_messages.yml` | System/admin messages |

## config.yml

The main configuration file contains:

### Commands
```yaml
# Commands to open the shop (without /)
buy-commands:
  - 'buy'
  - 'shop'

# Commands to open sell GUI
sell-commands:
  - 'sell'

# How commands are registered
# REGISTER = Full registration with tab complete
# INTERCEPT = Intercept only (no conflicts)
# NONE = Don't register (use commands.yml)
commands-mode: 'REGISTER'
```

### Features
```yaml
# Disable physical back button in shop
disable-back-button: true

# Disable escape to go back
disable-escape-back: false

# Enable alternate sell GUI (right-click to sell)
alternate-sell-enable: true

# Enable purchase sounds
enable-sound: true

# Enable in-game shop editor
ingame-config: true

# Enable dynamic pricing
dynamic-pricing: false

# Enable debug logging
debug-mode: false

# Log all transactions
transaction-log: false

# Hide items without buy price from shop GUI
hide-non-buyable: false

# Require signs to open shop
signs-only: false
```

### Buttons
```yaml
buttons:
  back:
    id: 'RED_STAINED_GLASS_PANE'
    shop-name: '&cBack'
    name: '&cBack'
    shop-lore: '&fGo back to the menu'
    lore: '&fClose the menu'
    slot: 54
  forward:
    id: 'ARROW'
    shop-name: '&cNext page'
    slot: 52
  backward:
    id: 'ARROW'
    shop-name: '&cPrevious page'
    slot: 48
```

### Titles
```yaml
titles:
  menu: 'Menu %page-number%'
  shop: 'Menu &f> &r%shopname%'
  sell: 'Menu &f> &rSell'
  qty: '&4Select amount'
  alt-sell: 'Menu &f> &rSell'
  value: '&2Item values'
```

### Lore Templates
```yaml
lores:
  buy: '&fBuy: &c%amount%'
  sell: '&fSell: &c%amount%'
  free: '&fBuy: &aFREE'
  cannot-buy: '&cCannot buy'
  cannot-sell: '&cCannot sell'
```

## shops.yml

Contains all shop definitions. See [Shop Configuration](Shop-Configuration) for details.

```yaml
shops:
  Blocks:
    pages:
      Page0:
        '0':
          type: SHOP
          id: STONE
          buy-price: 10
          sell-price: 5
```

## menu.yml

Defines the main menu that appears when using `/shop`:

```yaml
menu:
  pages:
    Page0:
      '0':
        id: GRASS_BLOCK
        shop-name: '&aBlocks'
        target-shop: 'Blocks'
      '1':
        id: DIAMOND_SWORD
        shop-name: '&bTools'
        target-shop: 'Tools'
```

## messages.yml

Contains all player-facing messages:

```yaml
messages:
  prefix: '&7[&6Shop&7] '
  no-permission: '&cYou do not have permission!'
  not-enough-money: '&cYou do not have enough money!'
  purchase-success: '&aYou purchased %amount% x %item% for %price%!'
  sell-success: '&aYou sold %amount% x %item% for %price%!'
  currency-prefix: '$'
  currency-suffix: ''
```

## worth.yml

Configures the worth display feature. See [Worth Display Configuration](Worth-Display-Configuration) for details.

```yaml
enabled: true
format: "&7Worth: &a%worth%"
position: BOTTOM
hide-armor-slots: true
```

## Reloading Configuration

Use `/gs reload` to reload all configuration files without restarting the server.

**Note:** Some changes may require a full restart:
- Commands registration mode changes
- Adding/removing PacketEvents dependency

## Configuration Tips

1. **Use color codes** with `&` prefix (e.g., `&a` for green)
2. **Quote strings** with special characters
3. **Test changes** with `/gs reload` before restarting
4. **Back up configs** before major changes
5. **Check console** for errors after reload
