# GUIShop Modded Edition

Proper shop screens for NeoForge, built on over a decade of GUIShop.

**Minecraft 1.21.1, NeoForge 21.1.267 or newer. Required on both the client and the server.**

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/pablo67340/GUIShop)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange)](https://www.curseforge.com/projects/1829693)
[![Discord](https://img.shields.io/badge/Discord-Support-purple)](https://discord.gg/wm6zMzdBwf)

> This is the `neoforge-port` branch. It holds the NeoForge mod, plus the shared
> core both editions now build on. For the Bukkit plugin's own documentation,
> see [`master`](https://github.com/pablo67340/GUIShop/tree/master).

GUIShop has been a Bukkit shop plugin since 2012. This is the NeoForge edition,
and it is not a port in name only. A plugin can only draw inside a chest
inventory, so every shop ever built with GUIShop was a grid of 54 slots with
filler panes around the edges and arrows glued to the bottom row. A mod has the
whole screen, so that is what this uses.

## Features

- **Shops are real screens** - they reflow to the size of your window, search, sort, scroll, and show the same item tooltips the rest of the game does
- **No fixed grid** - no filler panes, no page arrow in slot 48, no slot numbers to assign
- **Quantity selector** - click any item to buy or sell a chosen amount
- **Bulk selling** - `/sell` opens a container to tip items into; closing it sells what the shops buy, pays once for the lot, and hands the rest straight back
- **Editing without shift clicking** - press Edit, click an item, and get a large live preview beside every property it has
- **Registry-backed pickers** - items, blocks, mobs, enchantments and sounds from any installed mod are offered, and none of them can be mistyped
- **Sound preview** - a play button beside each sound, so you hear it before choosing it
- **Shop management in game** - add and delete shops and items from the interface; clicking a newly added shop creates it and puts you inside it
- **Settings in game** - a tabbed screen with client preferences for everyone and server settings for anyone permitted
- **Full item metadata** - enchantments, potions and tipped arrows, fireworks, player heads, spawner mobs, custom model data, item flags and raw NBT
- **Per player balances** - there is no Vault on NeoForge, so the mod keeps its own
- **Server authoritative** - prices are never read from the client, and permission is rechecked on the server for every change

## Install

Drop the jar in `mods/` on **both** the client and the server. The screens are
drawn client side and every price and permission decision is made server side,
so both halves have to be present. Single player works with no extra setup.

## Commands

| Command | Description |
|---------|-------------|
| `/shop` | Open the shop menu |
| `/shop <name>` | Open a shop directly |
| `/shop info [shop]` | Print the loaded config as text, for console and diagnostics |
| `/shop reload` | Reload shops and settings from disk (permission level 2) |
| `/sell` | Open the bulk sell container |

## Permissions

The editor and the server settings are available to anyone holding
`guishop.edit`, to operators, and to the owner of a single player world. The
node is registered with the NeoForge permission API, so LuckPerms and anything
else implementing it will see it.

Permission is checked again on the server for every single change. Nothing is
trusted because the client offered it.

## Configuration

Config lives in `<game>/guishop/`:

| Path | Contents |
|------|----------|
| `shops/<Name>.json` | One file per shop |
| `menu.json` | The menu players land on |
| `settings.json` | Titles, price lore formats, currency, sounds, disabled worlds |
| `messages.json` | Everything said to a player |
| `balances.json` | Player balances |

Client preferences are separate, in `<game>/config/guishop-client.json`, since
they belong to the player rather than the server.

### A shop file

```json
{
  "title": "&a&lBlocks",
  "items": [
    {
      "id": "STONE",
      "type": "ITEM",
      "buy-price": 10,
      "sell-price": 5,
      "shop-name": "&7Stone",
      "lore": ["&fA basic building block"]
    },
    {
      "id": "DIAMOND_SWORD",
      "type": "ITEM",
      "buy-price": 500,
      "enchantments": "SHARPNESS:5 UNBREAKING:3"
    }
  ]
}
```

You will rarely need to look at this. Everything in it can be set from the
editor.

### Coming from the Bukkit plugin

Drop your existing `shops/` folder and `menu.yml` into `<game>/guishop/` and
they are converted to JSON on first start. Every original is kept as a
`.yml.bak` beside it, so nothing is destroyed and you can always see what you
had.

Slot numbers in an old config are read and then ignored. There is no grid to
place things on any more, so ordering is the order of the entries in the file.

## Not in this build yet

This is an alpha, and these all exist in the Bukkit plugin but have not been
brought across:

- Worth display, which writes sell values into item lore
- Statistics tracking and the placeholder surfaces built on it
- Economy commands: `/bal`, `/pay`, and the admin give, take and set commands
- Bundled default configs, so a fresh install currently wants configs from an existing GUIShop setup to start from

## Building from source

```bash
git clone https://github.com/pablo67340/GUIShop.git
cd GUIShop
git checkout neoforge-port
./gradlew build
```

| Module | Output | Java |
|--------|--------|------|
| `:guishop-core` | `guishop-core/build/libs/guishop-core-<version>.jar` | 17 |
| `:guishop-bukkit` | `guishop-bukkit/build/libs/GUIShop-<version>.jar` | 17 |
| `:guishop-neoforge` | `guishop-neoforge/build/libs/guishop-neoforge-<version>.jar` | 21 |

Gradle provisions all three toolchains itself, so only one JDK needs to be
installed to build the lot. `./gradlew :guishop-neoforge:runClient` launches a
development client with the mod loaded.

## Project layout

```
guishop-core/      Shop model, pricing, config POJOs. No platform API on its classpath
guishop-bukkit/    The Paper/Spigot/Bukkit plugin
guishop-neoforge/  The NeoForge mod: screens, payloads, server services
```

Both editions sit on one shared core, so pricing, the config model and the shop
logic are a single implementation rather than two that drift apart. Anything
platform specific reaches its host through `GuiShopContext`, which is the only
seam between them.

## The Bukkit plugin

Still maintained, still separate, and unaffected by any of this. If you run
Paper, Spigot, Folia or Bukkit you want
[GUIShop](https://www.spigotmc.org/resources/guishop.2451/) rather than this
mod. Its documentation lives in the
[Wiki](https://github.com/pablo67340/GUIShop/wiki) and on the
[`master`](https://github.com/pablo67340/GUIShop/tree/master) branch.

## Support

- [GitHub Issues](https://github.com/pablo67340/GUIShop/issues)
- [Discord Server](https://discord.gg/wm6zMzdBwf)

## License

See [LICENSE](LICENSE).

---

Developed by **pablo67340**
