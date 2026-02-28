# Shop Configuration

This guide covers how to configure shops in the `plugins/GUIShop/shops/` folder.

## Basic Structure

Each shop is stored as a separate YAML file in the `shops/` folder. For example, `shops/Blocks.yml` defines the "Blocks" shop.

```yaml
# Shop title - displayed at top of inventory
title: '&8&lBlocks'

# Shop-level rows (optional) - applies to ALL pages as default
rows: 6

pages:
  Page0:
    # Page-level rows (optional) - overrides shop-level for this page
    # rows: 5
    items:
      '10':   # Slot 10
        type: SHOP
        id: STONE
        buy-price: 10
        sell-price: 5
  Page1:
    rows: 4  # Different size for this page
    items:
      '10':
        # More items...
```

## Shop Properties

| Property | Required | Description |
|----------|----------|-------------|
| `title` | Yes | Display title at top of inventory |
| `rows` | No* | Shop-level rows (1-6). Applies to all pages |
| `pages` | Yes | Map of pages containing items |

## Page Properties

| Property | Required | Description |
|----------|----------|-------------|
| `rows` | No* | Page-level rows (1-6). Overrides shop-level |
| `items` | Yes | Map of slot numbers to item configurations |

**\*Rows Configuration:**
- Set `rows` at **shop level** as a shorthand to apply the same size to all pages
- Set `rows` at **page level** to override for specific pages
- If neither is set, rows auto-calculate based on item count (max 6)

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
| `type` | Item type | `SHOP`, `COMMAND`, `SHOP_SHORTCUT`, `DUMMY`, `BLANK` |
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
Standard purchasable/sellable item. Shows buy/sell prices in lore.

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
  sudo: false  # Run as console (default) or player (true)
  commands:
    - 'lp user {PLAYER_NAME} parent set vip'
    - 'broadcast {PLAYER_NAME} purchased VIP!'
```

**Placeholders for commands:**
- `{PLAYER_NAME}` - Player's name
- `{PLAYER_UUID}` - Player's UUID
- `{PLAYER_WORLD}` - Player's current world
- `{PLAYER_BALANCE}` - Player's economy balance

**Sudo Mode:** Set `sudo: true` to run commands as the player instead of console. Useful for commands that require player permissions.

See [Command Items](Command-Items) for more details.

### SHOP_SHORTCUT
Links to another shop. Used for navigation between shops.

```yaml
'2':
  type: SHOP_SHORTCUT
  id: COMPASS
  shop-name: '&eGo to Tools'
  target-shop: 'Tools'
```

### DUMMY
Decorative item that cannot be purchased. Used for visual elements like borders or dividers.

```yaml
'3':
  type: DUMMY
  id: GRAY_STAINED_GLASS_PANE
  shop-name: ' '
```

### BLANK
Empty slot with no item or functionality.

```yaml
'4':
  type: BLANK
```

## Inventory Rows

The `rows` setting controls the inventory size. Valid values are 1-6.

| Rows | Max Slot | Total Slots |
|------|----------|-------------|
| 1 | 8 | 9 |
| 2 | 17 | 18 |
| 3 | 26 | 27 |
| 4 | 35 | 36 |
| 5 | 44 | 45 |
| 6 | 53 | 54 |

**Note:** Items placed in slots beyond the configured rows will be skipped with a warning.

## Slot Numbers

Inventory slots are numbered 0-53 for a 6-row inventory:

```
Row 1:  0  1  2  3  4  5  6  7  8
Row 2:  9 10 11 12 13 14 15 16 17
Row 3: 18 19 20 21 22 23 24 25 26
Row 4: 27 28 29 30 31 32 33 34 35
Row 5: 36 37 38 39 40 41 42 43 44
Row 6: 45 46 47 48 49 50 51 52 53
```

**Recommended Layout:** Use slots 10-16, 19-25, 28-34 for items (7 items per row with side padding). Reserve the bottom row (45-53) for navigation buttons.

## Multi-Page Shops

Create multiple pages by adding `Page1`, `Page2`, etc:

```yaml
title: '&8&lBlocks'
rows: 6

pages:
  Page0:
    items:
      '10':
        id: STONE
        buy-price: 10
        sell-price: 2
  Page1:
    items:
      '10':
        id: GRANITE
        buy-price: 15
        sell-price: 3
```

Navigation buttons are automatically added when multiple pages exist.

## Linking to Other Shops

Use `target-shop` to create navigation items:

```yaml
'16':
  type: DUMMY
  id: CHEST
  shop-name: '&eGo to Tools Shop'
  target-shop: 'Tools'
```

The shop name must match the filename (without `.yml`). For example, `target-shop: 'Tools'` opens `shops/Tools.yml`.

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

Create `shops/Tools.yml`:

```yaml
# Shop display title
title: '&8&lTools'

# Fixed 5 rows (slots 0-44)
rows: 5

pages:
  Page0:
    items:
      '10':
        type: SHOP
        id: IRON_PICKAXE
        buy-price: 150
        sell-price: 30
        shop-name: '&7Iron Pickaxe'
        shop-lore:
          - '&fA sturdy pickaxe'
          - '&ffor mining stone'
      '11':
        type: SHOP
        id: DIAMOND_PICKAXE
        buy-price: 500
        sell-price: 100
        shop-name: '&bDiamond Pickaxe'
        enchantments: 'EFFICIENCY:3 UNBREAKING:2'
      '16':
        type: COMMAND
        id: NETHER_STAR
        shop-name: '&6VIP Kit'
        buy-price: 10000
        sudo: false
        commands:
          - 'give {PLAYER_NAME} diamond 64'
          - 'msg {PLAYER_NAME} &aYou received the VIP kit!'
```

## Error Handling

If items are placed in slots beyond the configured rows, GUIShop will:

1. Log an error to the console
2. Display a red warning message to admin players when they open the shop
3. Skip the out-of-bounds items (they won't appear in the shop)

This helps prevent configuration mistakes from breaking the shop.
