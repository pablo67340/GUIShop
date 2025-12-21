# Messages Configuration

This guide covers how to customize plugin messages in `messages.yml`.

## Overview

The `messages.yml` file contains all text displayed by GUIShop. Every message is fully customizable and supports color codes.

## Color Codes

GUIShop supports standard Minecraft color codes:

| Code | Color | Code | Color |
|------|-------|------|-------|
| `&0` | Black | `&8` | Dark Gray |
| `&1` | Dark Blue | `&9` | Blue |
| `&2` | Dark Green | `&a` | Green |
| `&3` | Dark Aqua | `&b` | Aqua |
| `&4` | Dark Red | `&c` | Red |
| `&5` | Dark Purple | `&d` | Light Purple |
| `&6` | Gold | `&e` | Yellow |
| `&7` | Gray | `&f` | White |

### Formatting Codes

| Code | Effect |
|------|--------|
| `&l` | Bold |
| `&o` | Italic |
| `&n` | Underline |
| `&m` | Strikethrough |
| `&k` | Obfuscated |
| `&r` | Reset |

## Common Messages

### Prefix
```yaml
prefix: '&f[&cGUIShop&f]'
```
Displayed before most messages.

### Purchase Messages
```yaml
# When a player buys something
purchase: '&fA purchase was made, and &c%taken% &fwas taken from your account.'

# When a player doesn't have enough money
not-enough-money: '&fYou need &c%needed% &fto purchase this!'
```

### Sell Messages
```yaml
# When a player sells items
sell: '&fYour items were sold, and &a%money% &fwas added to your account.'

# When some items couldn't be sold
cant-sell: '&c%count% item(s) of your items failed to sell &fand have been returned to your inventory.'
```

### Error Messages
```yaml
# No permission
no-permission: '&cNo permission!'

# Full inventory
full-inventory: '&cPlease empty your inventory!'

# Something went wrong
something-wrong: '&cSomething went wrong, contact an admin.'
```

## Currency Formatting

```yaml
# Symbol before the amount
currency-prefix: '$'

# Symbol after the amount (usually empty)
currency-suffix: ''
```

This results in prices displayed as `$100` or `$1,000`.

## Placeholders

Many messages support placeholders that are replaced with dynamic values:

| Placeholder | Description |
|-------------|-------------|
| `%taken%` | Amount deducted from player |
| `%money%` | Amount added to player |
| `%needed%` | Required amount to purchase |
| `%count%` | Number of items |
| `%shop%` | Shop name |
| `%amount%` | Item quantity |
| `%max%` | Maximum allowed quantity |
| `%price%` | Item price |
| `%name%` | Item or player name |
| `%type%` | Mob type or item type |

## Command Messages

Each admin command has its own message section:

```yaml
reload:
  execute: '&aGUIShop reloaded!'
  entry: '&7/guishop &ereload &7- &aReload the configurations'

buy-price:
  invalid-input: '&cPlease enter a valid value!'
  successful: '&fBuy-price set: &a%price%'
  removed: '&cnone'
```

## Example Customization

### Before (Default)
```yaml
purchase: '&fA purchase was made, and &c%taken% &fwas taken from your account.'
```

### After (Custom)
```yaml
purchase: '&a&lPURCHASE COMPLETE! &7You spent &e%taken%&7.'
```

## Tips

- Always test messages in-game after changes
- Use `/gs reload` to apply changes without restart
- Keep messages concise for better readability
- Be consistent with your color scheme
- Use `\n` for multi-line messages where supported
