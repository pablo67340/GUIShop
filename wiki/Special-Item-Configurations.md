# Special Item Configurations

This guide covers how to configure special item types like enchanted books, potions, spawners, fireworks, and player heads.

## Enchantments

Add enchantments to any item using the `enchantments` property.

### Format
```yaml
enchantments: 'ENCHANT_NAME:LEVEL ENCHANT_NAME:LEVEL'
```

### Example
```yaml
'0':
  type: SHOP
  id: DIAMOND_SWORD
  buy-price: 1000
  enchantments: 'SHARPNESS:5 UNBREAKING:3 FIRE_ASPECT:2'
```

### Enchanted Books
```yaml
'1':
  type: SHOP
  id: ENCHANTED_BOOK
  buy-price: 500
  enchantments: 'MENDING:1'
```

### Common Enchantment Names

| Enchantment | Short Name | Applies To |
|-------------|------------|------------|
| SHARPNESS | SHARP | Swords, Axes |
| SMITE | SMITE | Swords, Axes |
| FIRE_ASPECT | FIRE_ASPECT | Swords |
| KNOCKBACK | KNOCKBACK | Swords |
| UNBREAKING | DURA | All items |
| EFFICIENCY | EFF | Tools |
| SILK_TOUCH | SILK_TOUCH | Tools |
| FORTUNE | FORTUNE | Tools |
| PROTECTION | PROTECTION | Armor |
| THORNS | THORNS | Armor |
| MENDING | MENDING | All items |

For a complete list, see the [Bukkit Enchantment documentation](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html).

---

## Potions

Configure potions using the `potion-info` property.

### Format
```yaml
potion-info: 'POTION_TYPE:AMPLIFIER:EXTENDED:UPGRADED'
```

| Field | Description | Values |
|-------|-------------|--------|
| POTION_TYPE | The potion effect | See list below |
| AMPLIFIER | Effect level (0-based) | `0` = Level 1, `1` = Level 2 |
| EXTENDED | Extended duration | `true` or `false` |
| UPGRADED | Upgraded (stronger) | `true` or `false` |

### Example
```yaml
'0':
  type: SHOP
  id: POTION
  buy-price: 200
  shop-name: '&dSpeed Potion'
  potion-info: 'SPEED:1:true:false'  # Speed II, Extended
```

### Splash and Lingering Potions
```yaml
'1':
  type: SHOP
  id: SPLASH_POTION
  buy-price: 300
  potion-info: 'HEALING:1:false:true'  # Instant Health II

'2':
  type: SHOP
  id: LINGERING_POTION
  buy-price: 400
  potion-info: 'POISON:0:true:false'  # Poison, Extended
```

### Common Potion Types

| Potion Type | Effect |
|-------------|--------|
| SPEED | Increases movement speed |
| SLOWNESS | Decreases movement speed |
| STRENGTH | Increases melee damage |
| HEALING | Instant health |
| HARMING | Instant damage |
| REGENERATION | Regenerates health over time |
| FIRE_RESISTANCE | Immunity to fire |
| WATER_BREATHING | Breathe underwater |
| INVISIBILITY | Become invisible |
| NIGHT_VISION | See in the dark |
| POISON | Take damage over time |

---

## Spawners

Configure mob spawners using the `mob-type` property.

### Format
```yaml
mob-type: ENTITY_TYPE
```

### Example
```yaml
'0':
  type: SHOP
  id: SPAWNER
  buy-price: 50000
  sell-price: 10000
  mob-type: ZOMBIE
  shop-name: '&cZombie Spawner'
```

### Common Mob Types

| Mob Type | Description |
|----------|-------------|
| ZOMBIE | Zombie spawner |
| SKELETON | Skeleton spawner |
| SPIDER | Spider spawner |
| CREEPER | Creeper spawner |
| ENDERMAN | Enderman spawner |
| BLAZE | Blaze spawner |
| IRON_GOLEM | Iron Golem spawner |
| PIG | Pig spawner |
| COW | Cow spawner |
| SHEEP | Sheep spawner |

Use `/gs parsemob <type>` to validate mob types.

---

## Fireworks

Configure fireworks using the `firework-info` property.

### Format
```yaml
firework-info: 'TYPE:COLORS:FADE_COLORS:TRAIL:FLICKER:POWER'
```

| Field | Description | Values |
|-------|-------------|--------|
| TYPE | Firework shape | `BALL`, `BALL_LARGE`, `BURST`, `CREEPER`, `STAR` |
| COLORS | Primary colors | Comma-separated: `RED,BLUE,GREEN` |
| FADE_COLORS | Fade colors | Comma-separated: `WHITE,YELLOW` |
| TRAIL | Has trail effect | `true` or `false` |
| FLICKER | Has flicker effect | `true` or `false` |
| POWER | Flight duration (1-3) | `1`, `2`, or `3` |

### Example
```yaml
'0':
  type: SHOP
  id: FIREWORK_ROCKET
  buy-price: 100
  shop-name: '&6Festive Firework'
  firework-info: 'BALL_LARGE:RED,BLUE,WHITE:YELLOW:true:true:2'
```

### Available Colors

`WHITE`, `SILVER`, `GRAY`, `BLACK`, `RED`, `MAROON`, `YELLOW`, `OLIVE`, `LIME`, `GREEN`, `AQUA`, `TEAL`, `BLUE`, `NAVY`, `FUCHSIA`, `PURPLE`, `ORANGE`

---

## Player Heads

Configure player heads using the `skull-uuid` property.

### Format
```yaml
skull-uuid: 'player-uuid-or-name'
```

### Example
```yaml
'0':
  type: SHOP
  id: PLAYER_HEAD
  buy-price: 500
  shop-name: '&6Notch Head'
  skull-uuid: '069a79f4-44e9-4726-a5be-fca90e38aaf5'  # Notch's UUID
```

### Using Player Names
```yaml
skull-uuid: 'Notch'  # Will fetch the skin at runtime
```

### Custom Textures
You can also use base64 texture values for custom heads.

---

## Custom Model Data

For resource pack support, use `custom-model` to set CustomModelData:

```yaml
'0':
  type: SHOP
  id: DIAMOND
  buy-price: 100
  custom-model: 1001
  shop-name: '&bCustom Diamond'
```

---

## Item Flags

Hide specific item attributes using `item-flags`:

```yaml
'0':
  type: SHOP
  id: DIAMOND_SWORD
  buy-price: 500
  enchantments: 'SHARPNESS:5'
  item-flags: 'HIDE_ENCHANTS HIDE_ATTRIBUTES'
```

### Available Flags

| Flag | Hides |
|------|-------|
| HIDE_ENCHANTS | Enchantments |
| HIDE_ATTRIBUTES | Attack damage, attack speed, etc. |
| HIDE_UNBREAKABLE | Unbreakable tag |
| HIDE_DESTROYS | "Can destroy" blocks |
| HIDE_PLACED_ON | "Can be placed on" blocks |
| HIDE_POTION_EFFECTS | Potion effects |
| HIDE_DYE | Leather armor dye color |

---

## Custom NBT

Add custom NBT data using `custom-nbt`:

```yaml
'0':
  type: SHOP
  id: STICK
  buy-price: 1000
  custom-nbt: '{CustomTag:"MyValue",Damage:0}'
  shop-name: '&6Magic Wand'
```

This NBT will be merged with the item when purchased.
