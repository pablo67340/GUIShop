# Commands and Permissions

This page documents all commands and permissions available in GUIShop.

## Player Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/shop` | `/buy` | Open the shop menu | `guishop.use` |
| `/sell` | - | Open the sell GUI | `guishop.sell` |
| `/value` | `/val`, `/gvalue` | Check item buy/sell values | `guishop.value` |

### Economy Commands (Internal Economy Only)

When using GUIShop's internal economy (enabled in `economy.yml`):

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/bal` | `/balance`, `/money` | Check your balance | `guishop.economy.balance` |
| `/bal <player>` | - | Check another player's balance | `guishop.economy.balance.others` |
| `/pay <player> <amount>` | - | Send money to a player | `guishop.economy.pay` |
| `/togglepay` | - | Toggle payment notifications | `guishop.economy.pay` |

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
| `/gs edit` | Enter creator mode for the main menu | `guishop.creator` |
| `/gs edit menu` | Enter creator mode for the main menu | `guishop.creator` |
| `/gs edit transaction` | Edit the transaction GUI layout | `guishop.creator` |
| `/gs edit <shop>` | Enter creator mode for a specific shop | `guishop.creator` |
| `/gs edit <shop> <page>` | Edit a specific page of a shop | `guishop.creator` |

**Editor Modes:**

| Mode | Command | Description |
|------|---------|-------------|
| **Menu Editor** | `/gs edit` or `/gs edit menu` | Edit the main shop menu |
| **Shop Editor** | `/gs edit <shopname>` | Edit a specific shop |
| **Transaction Editor** | `/gs edit transaction` | Edit the buy/sell transaction GUI |

**How the In-Game Editor Works:**
1. Enter creator mode with `/gs edit` or `/gs edit <shop>`
2. **Left-click** to drag and drop items to rearrange layout
3. **Right-click** or **Shift+click** any item to open the Item Editor GUI
4. Click on any setting in the editor to modify it
5. Changes save automatically when you close the inventory

See [In-Game Item Editor](In-Game-Item-Editor) for detailed documentation.

### Economy Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs eco give <player> <amount>` | Give money to a player | `guishop.admin` |
| `/gs eco take <player> <amount>` | Take money from a player | `guishop.admin` |
| `/gs eco set <player> <amount>` | Set a player's balance | `guishop.admin` |
| `/gs eco balance <player>` | Check a player's balance | `guishop.admin` |
| `/gs eco reset <player>` | Reset to starting balance | `guishop.admin` |

**Amount Abbreviations:** All economy commands support abbreviated amounts:
- `1k` = 1,000
- `1.5m` = 1,500,000
- `1b` = 1,000,000,000
- `1t` = 1,000,000,000,000

See [Internal Economy](Internal-Economy) for full documentation.

### Dynamic Pricing Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs market status` | Show dynamic pricing system status | `guishop.admin` |
| `/gs market info <item>` | Check an item's current market status | `guishop.admin` |
| `/gs market reset <item>` | Reset an item to base price | `guishop.admin` |
| `/gs market resetall` | Reset all items to base prices | `guishop.admin` |

**Aliases:** `/gs dp`, `/gs dynamicpricing`

See [Dynamic Pricing](Dynamic-Pricing) for full documentation.

### Utility Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gs parsemob <type>` | Validate a mob type for spawners | `guishop.admin` |
| `/gs iteminfo` | Display comprehensive info about held item | `guishop.admin` |

#### Item Info Command

The `/gs iteminfo` command displays detailed information about the item you're holding:

- **Material and Item Type** - Base material and GUIShop item type
- **Display Name** - Custom name if set
- **Lore** - All lore lines
- **Enchantments** - Listed in config-ready format (e.g., `SHARPNESS:5`)
- **Potion Info** - Type, duration, amplifier in config-ready format
- **Firework Info** - Flight duration, explosions in config-ready format
- **PDC Data** - All Persistent Data Container values
- **NBT Data** - Custom NBT tags

This is extremely useful for:
- Debugging item configurations
- Getting the exact format needed for `shops.yml`
- Verifying enchantments and potion effects
- Checking PDC data stored on items

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
