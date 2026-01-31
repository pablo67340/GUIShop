# Command Items

Command items execute commands when purchased. They can be used to sell ranks, kits, permissions, or any other command-based reward.

## Basic Configuration

```yaml
'0':
  type: COMMAND
  id: NETHER_STAR
  shop-name: '&6VIP Rank'
  buy-price: 10000
  commands:
    - 'lp user {PLAYER_NAME} parent set vip'
    - 'broadcast &a{PLAYER_NAME} just purchased VIP!'
```

## Available Placeholders

GUIShop provides built-in placeholders for command items:

| Placeholder | Description |
|-------------|-------------|
| `{PLAYER_NAME}` | Player's name |
| `{PLAYER_UUID}` | Player's UUID |
| `{PLAYER_WORLD}` | Player's current world |
| `{PLAYER_BALANCE}` | Player's economy balance |
| `%player%` | Player's name (alternate format) |
| `%player_name%` | Player's name |
| `%player_uuid%` | Player's UUID |
| `%player_world%` | Player's current world |
| `%player_balance%` | Player's economy balance |

### PlaceholderAPI Support

If PlaceholderAPI is installed, you can use any PAPI placeholder:

```yaml
commands:
  - 'say {PLAYER_NAME} has %vault_eco_balance% coins!'
  - 'give {PLAYER_NAME} diamond %player_level%'
```

## Sudo Mode (Run as Player)

By default, commands run as console. Set `sudo: true` to run commands as the player instead:

```yaml
'0':
  type: COMMAND
  id: ENDER_CHEST
  shop-name: '&dPersonal Enderchest'
  buy-price: 500
  sudo: true  # Run as player, not console
  commands:
    - 'enderchest'
```

### When to Use Sudo

| Scenario | Use Sudo? |
|----------|-----------|
| Giving permissions (LuckPerms, etc.) | No - console needed |
| Running player-only commands | Yes |
| Commands that check player permissions | Yes |
| Server broadcasts | No |
| Teleporting the player | Either works |

## Multiple Commands

Commands execute in order, one after another:

```yaml
'0':
  type: COMMAND
  id: DIAMOND_BLOCK
  shop-name: '&bStarter Kit'
  buy-price: 1000
  commands:
    - 'give {PLAYER_NAME} diamond_sword 1'
    - 'give {PLAYER_NAME} diamond_pickaxe 1'
    - 'give {PLAYER_NAME} cooked_beef 64'
    - 'msg {PLAYER_NAME} &aYou received the Starter Kit!'
```

## Examples

### Rank Purchase
```yaml
'0':
  type: COMMAND
  id: EMERALD
  shop-name: '&a&lVIP Rank'
  shop-lore:
    - '&7Unlock VIP perks!'
    - '&7- Fly in hub'
    - '&7- Special chat prefix'
  buy-price: 50000
  commands:
    - 'lp user {PLAYER_NAME} parent set vip'
    - 'broadcast &6&l{PLAYER_NAME} &ejust purchased &a&lVIP&e!'
    - 'msg {PLAYER_NAME} &aCongrats on VIP! Relog to apply perks.'
```

### Kit with Cooldown
```yaml
'1':
  type: COMMAND
  id: CHEST
  shop-name: '&eDaily Kit'
  buy-price: 0
  sudo: true
  commands:
    - 'kit daily'
```

### Crate Key
```yaml
'2':
  type: COMMAND
  id: TRIPWIRE_HOOK
  shop-name: '&5&lMystery Crate Key'
  buy-price: 5000
  commands:
    - 'crates give {PLAYER_NAME} mystery 1'
```

### Custom Player Head Token
```yaml
'3':
  type: COMMAND
  id: PLAYER_HEAD
  skull-uuid: 'eyJ0ZXh0dXJlcyI6...'
  shop-name: '&6Cosmetic Token'
  buy-price: 2500
  commands:
    - 'tokens give {PLAYER_NAME} 1'
```

### Sudo Command (Opens GUI for Player)
```yaml
'4':
  type: COMMAND
  id: COMPASS
  shop-name: '&bWarp Menu'
  buy-price: 0
  sudo: true
  commands:
    - 'warps'
```

## Command Item Properties

| Property | Required | Description |
|----------|----------|-------------|
| `type: COMMAND` | Yes | Identifies this as a command item |
| `id` | Yes | Material for the icon |
| `commands` | Yes | List of commands to execute |
| `buy-price` | No | Price (0 or omit for free) |
| `sudo` | No | Run as player instead of console (default: false) |
| `shop-name` | No | Display name in shop |
| `shop-lore` | No | Description lines |
| `permission` | No | Required permission to buy |

## Troubleshooting

### Commands Not Executing

1. Check server console for error messages
2. Verify placeholder format (`{PLAYER_NAME}` not `{player_name}`)
3. Test the command manually in console first
4. Ensure the command plugin is loaded

### Placeholders Not Replacing

- Use `{PLAYER_NAME}` format (case-insensitive)
- For PlaceholderAPI: ensure PAPI is installed and the expansion is loaded
- Check for typos in placeholder names

### Permission Errors

- If the command requires player permissions, use `sudo: true`
- If it requires console/op permissions, use `sudo: false` (default)

## Editor Support

When using the in-game editor (`/gs edit`):

1. Right-click a command item to open the Item Editor
2. Click "Commands" to manage the command list
3. Click "Item Type" and select COMMAND
4. The sudo option can be set via the editor (coming soon) or config file
