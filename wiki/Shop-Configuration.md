# Shop Configuration

This guide covers how to configure shops in `shops.yml`.

## Basic Structure

The `shops.yml` file contains all your shop definitions. Each shop has pages, and each page has items in specific slots.

```yaml
shops:
  ShopName:
    pages:
      Page0:
        '0':   # Slot 0
          type: SHOP
          id: STONE
          buy-price: 10
          sell-price: 5
        '1':   # Slot 1
          type: SHOP
          id: COBBLESTONE
          buy-price: 5
          sell-price: 2
      Page1:
        '0':
          # More items...
```

## Item Properties

### Required Properties

| Property | Description | Example |
|----------|-------------|---------|
| `id` | Material ID | `DIAMOND_SWORD`, `STONE`, `POTION` |
| `buy-price` | Purchase price (or `false`) | `100.0`, `false` |
| `sell-price` | Sell price (or `false`) | `50.0`, `false` |

### Optional Properties

| Property | Description | Example |
|----------|-------------|---------|
| `type` | Item type | `SHOP`, `COMMAND`, `BLANK`, `DUMMY` |
| `shop-name` | Display name in shop | `'&6Diamond Sword'` |
| `buy-name` | Name on purchased item | `'&bMy Sword'` |
| `shop-lore` | Lore shown in shop | List of strings |
| `buy-lore` | Lore on purchased item | List of strings |
| `quantity` | Stack size given | `64` |
| `enchantments` | Item enchantments | `'SHARP:5 DURA:3'` |
| `permission` | Required permission | `'shop.vip'` |
| `disable-qty` | Disable quantity selector | `true`/`false` |

## Item Types

### SHOP (Default)
Standard purchasable/sellable item.

```yaml
'0':
  type: SHOP
  id: DIAMOND
  buy-price: 100
  sell-price: 50
```

### COMMAND
Executes commands when purchased. The item is not given to the player.

```yaml
'1':
  type: COMMAND
  id: NETHER_STAR
  shop-name: '&6VIP Rank'
  buy-price: 10000
  commands:
    - 'lp user {PLAYER_NAME} parent set vip'
    - 'broadcast {PLAYER_NAME} purchased VIP!'
```

**Placeholders for commands:**
- `{PLAYER_NAME}` - Player's name
- `{PLAYER_UUID}` - Player's UUID

### BLANK
Empty slot with no functionality.

```yaml
'2':
  type: BLANK
```

### DUMMY
Decorative item that cannot be purchased.

```yaml
'3':
  type: DUMMY
  id: GRAY_STAINED_GLASS_PANE
  shop-name: ' '
```

## Slot Numbers

Inventory slots are numbered 0-53 for a 6-row inventory:

```
 0  1  2  3  4  5  6  7  8
 9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

## Multi-Page Shops

Create multiple pages by adding `Page1`, `Page2`, etc:

```yaml
shops:
  Blocks:
    pages:
      Page0:
        '0':
          id: STONE
          # ...
      Page1:
        '0':
          id: GRANITE
          # ...
```

Navigation buttons are automatically added when multiple pages exist.

## Linking to Other Shops

Use `target-shop` to create navigation items:

```yaml
'8':
  type: DUMMY
  id: CHEST
  shop-name: '&eGo to Tools Shop'
  target-shop: 'Tools'
```

## Price Formatting

Prices support decimals:

```yaml
buy-price: 100.50
sell-price: 25.25
```

Use `false` to disable buying or selling:

```yaml
buy-price: false      # Cannot be bought
sell-price: 100       # Can only be sold
```

## Complete Example

```yaml
shops:
  Tools:
    pages:
      Page0:
        '0':
          type: SHOP
          id: IRON_PICKAXE
          buy-price: 150
          sell-price: 30
          shop-name: '&7Iron Pickaxe'
          shop-lore:
            - '&fA sturdy pickaxe'
            - '&ffor mining stone'
        '1':
          type: SHOP
          id: DIAMOND_PICKAXE
          buy-price: 500
          sell-price: 100
          shop-name: '&bDiamond Pickaxe'
          enchantments: 'EFFICIENCY:3 UNBREAKING:2'
        '8':
          type: DUMMY
          id: BARRIER
          shop-name: '&cBack to Menu'
          target-shop: 'MAIN_MENU'
```
