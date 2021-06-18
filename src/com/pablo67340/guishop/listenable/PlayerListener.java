package com.pablo67340.guishop.listenable;

import java.util.Objects;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;

import org.bukkit.command.CommandSender;
import org.bukkit.event.*;

import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.util.Config;

public final class PlayerListener implements Listener {

    /**
     * An instance of a {@link PlayerListener} that will be used to handle this
     * specific object reference from other classes, even though methods here
     * will be static.
     */
    public static final PlayerListener INSTANCE = new PlayerListener();

    public Menu openShop(Player player) {
        Menu menu = new Menu(player);
        menu.open(player);
        return menu;
    }

    /**
     * Print the usage of the plugin to the player.
     *
     * @param sender - The player the help text will be sent to
     */
    public void printUsage(CommandSender sender) {
        GUIShop.sendMessage(sender, "&l&aGUIShop &l&fcommands:");
        GUIShop.sendMessage(sender, "&a{required} &7- &a[optional]");
        GUIShop.sendMessage(sender, "&7---------------------------");
        GUIShop.sendMessage(sender, "&7/guishop &ereload/r &7- &aReloads the configuration files");
        GUIShop.sendMessage(sender, "&7/guishop &eedit/e [shop name] [page] &7- &aOpens in Editor Mode");
        GUIShop.sendMessage(sender, "&7/guishop &eprice/p {price} &7- &aSet item in hand's buy price");
        GUIShop.sendMessage(sender, "&7/guishop &esell/s {price} &7- &aSet item in hand's sell price");
        GUIShop.sendMessage(sender, "&7/guishop &eshopname/sn {name} &7- &aSet item in hand's Shop-Name");
        GUIShop.sendMessage(sender, "&7/guishop &ebuyname/bn {name} &7- &aSet item in hand's Buy-Name");
        GUIShop.sendMessage(sender, "&7/guishop &ename {name} &7- &aSet an item's Menu Name. Used for items in menu.");
        GUIShop.sendMessage(sender, "&7/guishop &ets {target shop} &7- &aSet an item's Target Shop. Used for items in menu.");
        GUIShop.sendMessage(sender, "&7/guishop &eenchant/en {enchants} &7- &aSet item in hand's Enchantments");
        GUIShop.sendMessage(sender, "&7/guishop &easll {line} &7- &aAdd Shop Lore Line");
        GUIShop.sendMessage(sender, "&7/guishop &edsll {lineNumber} &7- &aDelete Shop Lore Line. Starts at 0");
        GUIShop.sendMessage(sender, "&7/guishop &eesll {lineNumber} {line} &7- &aEdit Shop Lore Line. Starts at 0");
        GUIShop.sendMessage(sender, "&7/guishop &eabll {line} &7- &aAdd Buy Lore Line");
        GUIShop.sendMessage(sender, "&7/guishop &edbll {lineNumber} &7- &aDelete Buy Lore Line. Starts at 0");
        GUIShop.sendMessage(sender, "&7/guishop &eebll {lineNumber} {line} &7- &aEdit Buy Lore Line. Starts at 0");
        GUIShop.sendMessage(sender, "&7/guishop &eac {command} &7- &aAdd Command to item");
        GUIShop.sendMessage(sender, "&7/guishop &edc {lineNumber} &7- &aDelete Command by line. Starts at 0");
        GUIShop.sendMessage(sender, "&7/guishop &eec {lineNumber} {cmd} &7- &aEdit Command by line. Starts at 0");
        GUIShop.sendMessage(sender, "&7/guishop &emt {type} &7- &aSet an item's mob type. Used for Spawners/Eggs.");
        GUIShop.sendMessage(sender, "&7/guishop &et {type} &7- &aSet an item's type. BLANK, SHOP, COMMAND, DUMMY");
        GUIShop.sendMessage(sender, "&7/guishop &enbt {nbt} &7- &aSet an item's NBT. Must be a valid one!");
        GUIShop.sendMessage(sender, "&7/guishop &eprintnbt &7- &aPrints the nbt of the item you are holding. Usefull for custom items.");
        GUIShop.sendMessage(sender, "&7/guishop &et {type} &7- &aSet an item's type. BLANK, SHOP, COMMAND, DUMMY");
        GUIShop.sendMessage(sender, "&7---------------------------");
    }

    // When the inventory closes
    // When the player clicks a sign
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        // If the block exists
        if (block != null) {
            // If the block has a state
            block.getState();
            // If the block state is a Sign
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                String line1 = ChatColor.translateAlternateColorCodes('&', sign.getLine(0));
                // Check if the sign is a GUIShop sign
                if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',
                        Objects.requireNonNull(GUIShop.INSTANCE.getMainConfig().getString("sign-title"))))) {
                    // If the player has Permission to use sign
                    if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use")
                            || player.isOp()) {
                        e.setCancelled(true);
                        Menu menu = new Menu(player);
                        menu.open(player);
                    } else {
                        e.setCancelled(true);
                        player.sendMessage(Config.getPrefix() + " " + Config.getNoPermission());
                    }
                }
            }
        }
    }

    /**
     * Custom MobSpawner placement method.
     *
     * @param event The event type we're listening to
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (Item.isSpawnerItem(item)) {

            NBTTagCompound cmp = ItemNBTUtil.getTag(item);
            if (cmp.hasKey("GUIShopSpawner")) {

                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {

                    String mobId = cmp.getString("GUIShopSpawner");
                    Block block = event.getBlockPlaced();
                    CreatureSpawner cs = (CreatureSpawner) block.getState();

                    GUIShop.debugLog("Applying mob type " + mobId);

                    /*
                     * Although valueOf is almost always safe here because
                     * we used EntityType.name() when setting the NBT tag,
                     * it's possible the user might change server versions,
                     * in which case the EntityType enum may have changed.
                     */
                    try {
                        cs.setSpawnedType(EntityType.valueOf(mobId));
                        cs.update();
                    } catch (IllegalArgumentException veryRareException) {
                        GUIShop.log("Detected outdated mob spawner ID: " + mobId + " placed by " + event.getPlayer());
                    }

                }, 1L);
            }
        }
    }
}
