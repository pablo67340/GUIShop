# Commands and Permissions

This page documents all commands and permissions available in GUIShop.

## Player Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/shop` | `/buy` | Open the shop menu | `guishop.use` |
| `/sell` | - | Open the sell GUI | `guishop.sell` |
| `/value` | `/val`, `/gvalue` | Check item buy/sell values | `guishop.value` |

## Admin Commands

All admin commands use the base command `/guishop` (alias: `/gs`).

### General Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs reload` | Reload all configuration files | `guishop.reload` |
| `/gs toggleworth` | Toggle worth display for yourself | `guishop.use` |

### In-Game Editor Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs edit` | Enter creator mode (opens menu) | `guishop.creator` |
| `/gs edit <shop>` | Edit a specific shop | `guishop.creator` |
| `/gs edit <shop> <page>` | Edit a specific page of a shop | `guishop.creator` |

### Item Configuration Commands

These commands modify the item you are holding:

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs buyprice <price>` | Set buy price (use `false` to disable buying) | `guishop.admin` |
| `/gs sellprice <price>` | Set sell price (use `false` to disable selling) | `guishop.admin` |
| `/gs shopname <name>` | Set display name in shop | `guishop.admin` |
| `/gs buyname <name>` | Set name on purchased item | `guishop.admin` |
| `/gs enchant <enchant:level>` | Add enchantment | `guishop.admin` |
| `/gs addlore <text>` | Add a lore line | `guishop.admin` |
| `/gs setslot <slot>` | Set slot position | `guishop.admin` |

### Utility Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs parsemob <type>` | Validate a mob type for spawners | `guishop.admin` |
| `/gs iteminfo` | Display comprehensive info about held item (PDC, enchants, lore) | `guishop.admin` |

## Permissions

### Basic Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `guishop.use` | op | Access to GUIShop commands |
| `guishop.sell` | true | Access to the sell command |
| `guishop.value` | true | Access to the value command |

### Admin Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `guishop.admin` | op | Full admin access |
| `guishop.reload` | op | Reload configuration |
| `guishop.creator` | op | In-game shop editor access |

### Shop Item Permissions

You can restrict access to specific shop items:

| Permission | Description |
|------------|-------------|
| `guishop.shop.*` | Access to all shop items |
| `guishop.shop.<shop>` | Access to all items in a specific shop |
| `guishop.shop.<shop>.<item>` | Access to a specific item |

### Setting Per-Item Permissions

In `shops.yml`, add the `permission` property to any item:

```yaml
'0':
  type: SHOP
  id: DIAMOND_SWORD
  buy-price: 1000
  permission: 'shop.vip.sword'  # Only players with this permission can buy
```

## Command Registration Modes

In `config.yml`, you can configure how commands are registered:

```yaml
commands-mode: 'REGISTER'
```

| Mode | Description |
|------|-------------|
| `REGISTER` | Commands registered with server (tab completion, may conflict) |
| `INTERCEPT` | Commands intercepted (no conflicts, no tab completion) |
| `NONE` | Commands not registered (use Bukkit's commands.yml) |
