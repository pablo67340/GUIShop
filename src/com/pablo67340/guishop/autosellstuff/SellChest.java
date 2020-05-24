package com.pablo67340.guishop.autosellstuff;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.listenable.Sell;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class SellChest implements Listener, CommandExecutor, TabCompleter {

    public static final SimpleDateFormat format = new SimpleDateFormat("MM-dd hh:mm:ss");

    List<ChestLocation> chestLocations = new ArrayList<>();
    Main plugin;
    Logger logger;

    public SellChest(Main main) {
        this.plugin = main;
        logger = plugin.getLogger();
        main.getServer().getPluginManager().registerEvents(this, main);
        main.getCommand("sellchest").setExecutor(this);
        main.getCommand("sellchest").setTabCompleter(this);
        initialize();
        System.out.println("Loaded sellchest");
    }

    public void initialize() {
        chestLocations.clear();
        File file = plugin.getSellChestF();
        TypeReference<List<ChestLocation>> typeRef
                = new TypeReference<List<ChestLocation>>() {
        };
        try {
            chestLocations = ChestLocation.getMapper().readValue(file, typeRef);
        } catch (IOException e) {
            logger.warning("Could not load sell chest config!");
            e.printStackTrace();
        }

        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndSellEverything, 1L, 40L);
    }

    public void save() {
        try {
            ChestLocation.getMapper().writeValue(plugin.getSellChestF(), chestLocations);
            if (false) logger.info("Saved chest positions!");
        } catch (IOException e) {
            logger.warning("Could not save sell chest config!");
            e.printStackTrace();
        }
    }

    private ItemStack createChest() {
        return createChest("Auto Sell Chest");
    }

    private ItemStack createChest(String name) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(254);
            meta.setLore(Collections.singletonList("A chest that sells stuff"));
            meta.setDisplayName(name);
            chest.setItemMeta(meta);
        }
        return chest;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.CHEST) return;
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasCustomModelData()
                && item.getItemMeta().getCustomModelData() == 254) {
            ChestLocation target = new ChestLocation(event.getBlockPlaced().getLocation(),
                    event.getPlayer().getUniqueId());
            if (!chestLocations.contains(target)) {
                logger.info("Adding sellchest at " + target + " belonging to "
                        + event.getPlayer().getUniqueId());
                chestLocations.add(target);
                save();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {

        if (event.getBlock().getType() != Material.CHEST) return;

        ChestLocation loc = new ChestLocation(event.getBlock().getLocation(),
                event.getPlayer().getUniqueId());
        if (chestLocations.contains(loc)) {
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR); // kill the chest
            ItemStack item = createChest(event.getPlayer().getName() + "'s AutoSell Chest"); //
            // and replace it with the
            // one with a unique meta
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
            chestLocations.remove(loc);

            logger.info("Removing sell chest from " + loc);
            save();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTransfer(InventoryMoveItemEvent e) {
        if (!e.getDestination().getType().equals(InventoryType.CHEST)) {
            return;
        }
        Location eventLoc = e.getDestination().getLocation();
        // we need to find a chest at this location
        UUID ownerUUID = null;
        for (ChestLocation cl : chestLocations) {
            if (cl.isAtSameLocation(eventLoc)) {
                ownerUUID = cl.owner;
            }
        }
        if (ownerUUID == null) {
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);

        log("[" + format.format(new Date()) + "] [" + owner.getName() + "] [SellChest] selling " + e.getItem() + "!");

        List<ItemStack> unsellable = Sell.sellItems(null,
                new ItemStack[]{e.getItem()}, owner, false);

        unsellable.remove(e.getItem());

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                () -> {
                    // so if we sold the stuff, remove it from the chest
                    // then add back the stuff we couldn't
                    e.getDestination().removeItem(e.getItem());
                    for (ItemStack i : unsellable) {
                        e.getDestination().addItem(i);
                    }
                }, 1);

        save();
    }

    public void checkAndSellEverything() {
        for (ChestLocation c : chestLocations) {
            World world = Bukkit.getWorld(c.worldName);
            if (world == null) {
                return;
            }
            Block b = world.getBlockAt((int) c.x, (int) c.y, (int) c.z);
            if (b.getType() != Material.CHEST) {
                return;
            }
            Chest chest = (Chest) b.getState();
            ItemStack[] contents = chest.getInventory().getContents();
            if (contents.length < 1) return;

            List<ItemStack> ret = new ArrayList<>();
            for (ItemStack i : contents) {
                if (i != null) ret.add(i);
            }

            OfflinePlayer p = Bukkit.getOfflinePlayer(c.getOwner());
            System.out.println("Selling " + ret);
            log("[" + format.format(new Date()) + "] [" + p.getName() + "] [SellChest] selling " + ret + "!");

            List<ItemStack> unsellable = Sell.sellItems(null, contents,
                    p, false);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                    () -> {
                        // so if we sold the stuff, remove it from the chest
                        // then add back the stuff we couldn't
                        chest.getInventory().clear();
                        for (ItemStack i : unsellable) {
                            chest.getInventory().addItem(i);
                        }
                    }, 1);

        }
    }

    public static BufferedWriter econLogWriter;

    static {
        try {
            econLogWriter =
                    new BufferedWriter(new FileWriter(Main.getINSTANCE().getAutoSellerLogFile(),
                            true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String o) {
        if (econLogWriter != null) {
            try {
                econLogWriter.write(o);
                econLogWriter.newLine();
                econLogWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("sellchest")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                if (args.length > 1) {
                    if (sender.hasPermission("sellchest.command")) {
                        Player p = Bukkit.getPlayer(args[1]);

                        if (p != null) {
                            p.getInventory().addItem(createChest(p.getName() + "'s AutoSell " +
                                    "Chest"));
                        } else {
                            logger.warning("[SellChest] player was null!");
                            return false;
                        }
                        logger.info("[SellChest] Gave " + p.getName()
                                + " a sell chest!");
                    } else {
                        sender.sendMessage("You do not have permission to give yourself a " +
                                "SellChest!");
                    }
                }
            } else {
                sender.sendMessage("[" + ChatColor.DARK_AQUA + "SellChest" + ChatColor.RESET +
                        "]" + ChatColor.COLOR_CHAR + "6" +
                        "No Player Given!");
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("sellchest")) {
            if (args.length == 1) return Collections.singletonList("give");
            return sender.getServer().getOnlinePlayers().stream().map(CommandSender::getName)
                    .collect(Collectors.toList());
        } else return null;
    }

}
