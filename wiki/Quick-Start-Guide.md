# Quick Start Guide

Get GUIShop up and running in minutes with this quick start guide.

## Step 1: Install Dependencies

1. Install **Vault** plugin
2. Install an economy plugin (EssentialsX, CMI, etc.)
3. (Optional) Install **PacketEvents** for worth display

## Step 2: Install GUIShop

1. Download GUIShop
2. Place in your `plugins` folder
3. Restart the server

## Step 3: Basic Configuration

GUIShop comes with example shops. To customize:

### Edit shops.yml

Open `plugins/GUIShop/shops.yml` and modify the example shops:

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
          shop-name: '&7Stone'
        '1':
          type: SHOP
          id: COBBLESTONE
          buy-price: 5
          sell-price: 2
          shop-name: '&7Cobblestone'
```

### Edit menu.yml

Configure the main menu in `plugins/GUIShop/menu.yml`:

```yaml
menu:
  pages:
    Page0:
      '4':
        id: GRASS_BLOCK
        shop-name: '&aBlocks Shop'
        target-shop: 'Blocks'
        shop-lore:
          - '&7Click to browse blocks'
```

## Step 4: Apply Changes

```
/gs reload
```

## Step 5: Test

1. Type `/shop` to open the menu
2. Click a shop to browse items
3. Click an item to buy
4. Type `/sell` to open sell GUI

## Common Tasks

### Add a New Shop

1. Add to `shops.yml`:
```yaml
shops:
  MyNewShop:
    pages:
      Page0:
        '0':
          type: SHOP
          id: DIAMOND
          buy-price: 100
          sell-price: 50
```

2. Add menu entry in `menu.yml`:
```yaml
'5':
  id: DIAMOND
  shop-name: '&bMy New Shop'
  target-shop: 'MyNewShop'
```

3. Reload: `/gs reload`

### Add Sell-Only Items

Set `buy-price: false` to make items sell-only:

```yaml
'10':
  type: SHOP
  id: ROTTEN_FLESH
  buy-price: false
  sell-price: 1
```

These items will not appear in shops but can be sold.

### Add Command Items

Sell commands instead of items:

```yaml
'20':
  type: COMMAND
  id: NETHER_STAR
  shop-name: '&6VIP Rank'
  buy-price: 10000
  commands:
    - 'lp user {PLAYER_NAME} parent set vip'
```

### Enable Worth Display

1. Install PacketEvents
2. Set `enabled: true` in `worth.yml`
3. Restart server

Items will now show their sell value in lore.

## In-Game Editor

Use the in-game editor for quick changes:

1. Run `/gs edit` to enter creator mode
2. Navigate to the shop/slot you want to edit
3. Use commands to modify:
   - `/gs buyprice 100`
   - `/gs sellprice 50`
   - `/gs shopname &bMy Item`
4. Changes are saved automatically

## Next Steps

- Read [Shop Configuration](Shop-Configuration) for advanced options
- See [Special Item Configurations](Special-Item-Configurations) for potions, spawners, etc.
- Check [Commands and Permissions](Commands-and-Permissions) for all available commands
