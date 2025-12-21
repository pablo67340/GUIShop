# Menu Configuration

This guide covers how to configure the main shop menu in `menu.yml`.

## Basic Structure

The menu is the first screen players see when opening the shop. It contains navigation items that link to different shops.

```yaml
Menu:
  pages:
    Page0:
      items:
        '0':
          id: GRASS_BLOCK
          name: '&aBlocks Shop'
          target-shop: 'Blocks'
          lore:
            - '&7Click to browse blocks'
        '1':
          id: DIAMOND_PICKAXE
          name: '&bTools Shop'
          target-shop: 'Tools'
```

## Item Properties

### Required Properties

| Property | Description | Example |
|----------|-------------|---------|
| `id` | Material ID of the display item | `DIAMOND`, `CHEST`, `GRASS_BLOCK` |

### Optional Properties

| Property | Description | Example |
|----------|-------------|---------|
| `name` | Display name of the item | `'&aBlocks Shop'` |
| `lore` | List of lore lines | See example below |
| `target-shop` | Shop to open when clicked | `'Blocks'` |
| `skull-uuid` | UUID or Base64 texture for player heads | See below |
| `enchantments` | Visual enchantments | `'DURABILITY:1'` |
| `potion-info` | Potion appearance for potion items | See below |

## Linking to Shops

Use `target-shop` to make an item open a specific shop:

```yaml
'4':
  id: DIAMOND
  name: '&bOres Shop'
  target-shop: 'Ores'
  lore:
    - '&7Buy and sell ores'
    - '&7and ingots here!'
```

The `target-shop` value must match a shop name in `shops.yml`.

## Decorative Items

Create decorative/filler items without any functionality:

```yaml
'0':
  id: GRAY_STAINED_GLASS_PANE
  name: ' '
```

Items without `target-shop` are purely decorative and do nothing when clicked.

## Multi-Page Menus

Create multiple pages by adding `Page1`, `Page2`, etc:

```yaml
Menu:
  pages:
    Page0:
      items:
        '13':
          id: DIAMOND
          name: '&bPage 1 Shop'
          target-shop: 'Shop1'
    Page1:
      items:
        '13':
          id: EMERALD
          name: '&aPage 2 Shop'
          target-shop: 'Shop2'
```

Navigation buttons are automatically added when multiple pages exist.

## Using Player Heads

### With Player UUID
```yaml
'13':
  id: PLAYER_HEAD
  skull-uuid: '069a79f4-44e9-4726-a5be-fca90e38aaf5'
  name: '&6Special Shop'
  target-shop: 'Special'
```

### With Base64 Texture
```yaml
'13':
  id: PLAYER_HEAD
  skull-uuid: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6...'
  name: '&6Custom Head Shop'
  target-shop: 'Custom'
```

## Using Potions as Display Items

```yaml
'21':
  id: POTION
  potion-info:
    type: INSTANT_DAMAGE
    splash: true
    extended: false
    upgraded: true
  name: '&4Potion Shop'
  target-shop: 'Potions'
```

## Slot Numbers

Menu slots are numbered 0-53 for a 6-row inventory:

```
 0  1  2  3  4  5  6  7  8
 9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

## Complete Example

```yaml
Menu:
  pages:
    Page0:
      items:
        # Decorative border
        '0':
          id: BLUE_STAINED_GLASS_PANE
          name: ' '
        '1':
          id: BLUE_STAINED_GLASS_PANE
          name: ' '
        # ... more border items ...
        
        # Shop entries
        '13':
          id: GRASS_BLOCK
          name: '&aBlocks'
          target-shop: 'Blocks'
          lore:
            - '&7Building materials'
            - '&7and decorations'
        '21':
          id: DIAMOND_PICKAXE
          name: '&bTools'
          target-shop: 'Tools'
          lore:
            - '&7Pickaxes, shovels,'
            - '&7and more!'
        '22':
          id: DIAMOND_SWORD
          name: '&cWeapons'
          target-shop: 'Weapons'
          lore:
            - '&7Swords and combat gear'
        '23':
          id: DIAMOND_CHESTPLATE
          name: '&9Armor'
          target-shop: 'Armor'
          lore:
            - '&7Protective equipment'
```

## Tips

- Use glass panes as decorative borders for a cleaner look
- Center important shop entries for better visibility
- Use consistent color schemes for item names
- Keep lore descriptions short and informative
- Test your menu layout in-game with `/gs reload`
