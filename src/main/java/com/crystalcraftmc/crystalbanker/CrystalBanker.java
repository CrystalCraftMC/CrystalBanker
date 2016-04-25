/*
 * Copyright (c) 2016 CrystalCraftMC
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.crystalcraftmc.crystalbanker;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

/**
 * CrystalBanker.java
 * <p>
 * This class handles the conversion of Minecraft experience orbs to experience bottles, or from experience bottles
 * to experience orbs. Using the Bukkit/Spigot API, the plugin attempts to do a "best guess" for an accurate
 * conversion between the two mediums. Players find this useful as it saves them time from bottling their experience
 * orbs or from breaking them.
 *
 * @author Ivan Frasure
 * @author Justin W. Flory
 * @version 2016.04.25.v1
 */
public class CrystalBanker extends JavaPlugin {

    /**
     * Method executed on server startup.
     */
    public void onEnable() {
        getLogger().info(ChatColor.GRAY + "CrystalBanker has been initialized!");
        try {
            File database = new File(getDataFolder(), "config.yml");
            if (!database.exists()) saveDefaultConfig();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Method executed on server shutdown.
     */
    public void onDisable() {
        getLogger().info(ChatColor.GRAY + "CrystalBanker has been stopped by the server.");
    }

    /**
     * Command listener method that handles all commands used by the plugin.
     *
     * @param sender the sender of the command
     * @param cmd    the base command used
     * @param label  the command label
     * @param args   the arguments used with theb ase command
     * @return true if command executed successfully
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Reload the plugin configuration
        if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConf(sender);
            return true;
        }

        // Handles transactions
        else if (isPlayer(sender) && sender.hasPermission("crystalbanker.transaction")) {

            // Variables
            Player player = (Player) sender;
            Inventory inv = player.getInventory();

            if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && args[0].equalsIgnoreCase("deposit")) {
                depositOrbs(player, args[1]);
                return true;
            } else if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && args[0].equalsIgnoreCase("withdraw")) {
                withdrawMethod(player, args[1], inv);
                return true;
            }
        }
        getLogger().info(ChatColor.RED + "Plugin failed to recognize commands.");
        return false;
    }

    /**
     * Checks to see if the command sender is indeed a player.
     *
     * @param sender the sender of the command we want to check
     * @return true if the sender is a player
     */
    private boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculates the approximate number of orbs to be given per bottle of experience. Randomness is introduced in an
     * attempt to replicate normal gameplay if you were breaking them regularly.
     *
     * @return the number of experience orbs for a bottle of experience
     */
    private static float orbsPerBottle() {
        Random rand = new Random();
        return (rand.nextFloat() * (11 - 3) + 3);
    }

    /**
     * Checks to make sure the value passed for the bottles to use is actually an integer. If not, it trims it and
     * returns the trimmed value.
     *
     * @param player       the player using the command
     * @param bottlesToUse the number of bottles to be used for converting
     * @return int an integer value of the number of bottles to use
     */
    private int isInt(Player player, String bottlesToUse) {
        int number;
        try {
            number = Integer.parseInt(bottlesToUse.trim());
            this.getLogger().finest("*** DEBUG ***\n" + "Your args[1] was: " + number + "\n*** DEBUG ***");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "There was a problem with the value you entered. Try again.");
            return 0;
        }
        return number;
    }

    /**
     * Takes experience bottles and converts them back into experience orbs for the player. It converts the number of
     * bottles specified by the player.
     *
     * @param amountToRemove the number of bottles to convert to experience orbs
     * @param player         the player who is converting bottles
     * @param inv            the inventory of the player who is converting bottles
     */
    private void bottlesToXP(int amountToRemove, Player player, Inventory inv) {
        this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                "bottlesToXP() fired. Searching and removing numeric values.\n*** DEBUG ***");

        // Variables
        int leftToRemove = amountToRemove;
        ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);

        // Searches inventory for XP bottles and removes the number specified that we are looking for
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack invSlot = inv.getItem(i);
            if (invSlot.isSimilar(xpBottle)) {
                if (leftToRemove >= 0) {
                    if (invSlot.getAmount() < leftToRemove) {
                        leftToRemove -= invSlot.getAmount();
                        inv.clear(i);
                    } else if (invSlot.getAmount() == leftToRemove) {
                        inv.clear(i);
                        leftToRemove = 0;
                        break;
                    } else if (invSlot.getAmount() > leftToRemove) {
                        invSlot.setAmount((invSlot.getAmount() - leftToRemove));
                        leftToRemove = 0;
                        break;
                    }
                }
            }
        }

        this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                "Preparing to translate numeric values into experience.");

        // Adds levels to player's inventory based on what was taken
        if (leftToRemove == 0) {
            int xpOrbTotal = 0;
            for (int i = 0; i < amountToRemove; i++) {//translate everything into orbs
                int xpOrb = Math.round(orbsPerBottle());
                this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                        "Values of orbs being output: " + i + ". " + xpOrb + "\n*** DEBUG ***");
                xpOrbTotal += xpOrb;
            }

            // Confirm the player's transaction and give them orbs
            player.sendMessage(ChatColor.DARK_AQUA + "Your withdrawal has been processed. Thank you for supporting " +
                    "the " + this.getConfig().get("bank-name") + "!");
            player.giveExp(xpOrbTotal);

            // Debug print messages
            this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                    "Values of TotalOrbs output: " + xpOrbTotal + "\n*** DEBUG ***");
            this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                    "Total player experience after additional TotalOrbs: " + player.getTotalExperience() + "\n*** DEBUG ***");
        }
    }

    /**
     * Reloads the configuration file for CrystalBanker.
     *
     * @param sender the command sender to be sent a message
     * @return true if it reloads successfully (no way for it not to right now)
     */
    private boolean reloadConf(CommandSender sender) {
        this.reloadConfig();
        sender.sendMessage(ChatColor.GRAY + "Configuration reloaded!");
        return true;
    }

    /**
     * Withdraw experience orbs from experience bottles.
     *
     * @param player       the player withdrawing orbs
     * @param bottlesToUse the amount of bottles to use for withdrawing
     * @param inv          the inventory of the player withdrawing
     * @return true if the withdrawal happens successfully
     */
    private boolean withdrawMethod(Player player, String bottlesToUse, Inventory inv) {
        int amountToRemove = isInt(player, bottlesToUse);
        if (countBottles(player, inv, amountToRemove)) {
            bottlesToXP(amountToRemove, player, inv);
            return true;
        } else return false;
    }

    /**
     * Counts the number of bottles in the player's inventory before proceeding with a transaction.
     *
     * @param player the player who is being checked
     * @param inv the inventory of the player being checked
     * @param amountToRemove the minimum number of bottles needed for the transaction
     * @return true if the player has enough bottles
     */
    private boolean countBottles(Player player, Inventory inv, int amountToRemove) {

        // Variables
        int tally = 0;
        ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);

        // Scan player inventory and tally number of experience bottles
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack invSlot = inv.getItem(i);
            if (invSlot.getType().equals(xpBottle.getType())) tally += invSlot.getAmount();
        }

        // If the player has enough, remove bottles; if not, fail the check
        if (tally < amountToRemove) {
            this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                    "Bottles counted: " + tally + "\n*** DEBUG ***");
            player.sendMessage(ChatColor.RED + "You don't have enough experience bottles in your inventory to " +
                    "exchange the amount you requested.");
            return false;
        } else if (tally >= amountToRemove) {
            player.sendMessage(ChatColor.GRAY + "Experience bottles counted: " + tally);
            this.getLogger().finest(ChatColor.RED + "*** DEBUG ***\n" +
                    "Bottles counted: " + tally + "\n*** DEBUG ***");
            return false;
        }
        return true;
    }

    /**
     * Returns experience bottles based on the amount of experience orbs the player has.
     *
     * @param player the player who is converting orbs to bottles
     * @param numOrbsToConvert the number of orbs to convert to bottles
     * @return true if the transaction successfully completes
     */
    private boolean depositOrbs(Player player, String numOrbsToConvert) {

        // Variables
        int storeLevels = isInt(player, numOrbsToConvert);
        int currentLevel = player.getLevel();

        // If the player has enough orbs, cash them in for bottles
        if (countXP(player)) {
            if (currentLevel <= 16) {
                expToBottles(player, formulaTierOne(storeLevels, currentLevel), storeLevels);
                return true;
            } else if (currentLevel <= 31) {
                expToBottles(player, forumlaTierTwo(storeLevels, currentLevel), storeLevels);
                return true;
            } else if (currentLevel >= 32) {
                expToBottles(player, forumlaTierThree(storeLevels, currentLevel), storeLevels);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to make sure the player has at least one level of experience.
     *
     * @param p the player being checked
     * @return true if the player has greater than seven orbs (i.e. one level)
     */
    private boolean countXP(Player p) {
        int currentXP = p.getTotalExperience();
        if (currentXP > 7) return true;
        else {
            p.sendMessage(ChatColor.RED + "You have less than one level! Make sure you have at least one level of " +
                    "experience before trying again.");
            return false;
        }
    }

    /**
     * Convert experience orbs to bottles.
     *
     * @param player the player who is converting orbs to bottles
     * @param expToRemove the amount of experience orbs to remove from the player
     * @param numToConvert the number of orbs to be removed from the player
     */
    private void expToBottles(Player player, int expToRemove, int numToConvert) {

        // Variables
        int currentExp = player.getTotalExperience();

        // Checks if current experience matches up with the amount to remove
        if (currentExp > (currentExp - expToRemove)) {
            int bottles = 0;
            float trackXP = currentExp;
            while (trackXP > (currentExp - expToRemove)) {
                trackXP -= orbsPerBottle();
                bottles++;
            }
            if (numInvFreeSpace(player) >= bottles) {
                player.sendMessage(ChatColor.DARK_AQUA + "You deposited " + (expToRemove) + " experience has been processed. Thank you for supporting Crystal Banks!");
                player.setLevel(player.getLevel() - numToConvert);
                player.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
            } else {
                player.sendMessage(ChatColor.RED + "Due to lack inventory space your transaction was canceled! " + "Crystal Banks forgives your awful teasing, but please! Next time! Have enough room in your inventory!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Due to lack of XP your deposit was canceled! " + "Crystal Banks forgives the overdraft, and waives all fees. Next time please ensure you enter the correct amount!");
        }
    }

    /**
     * Calculates the number of free spaces in the player's inventory that can be used to fill with experience bottles.
     *
     * @param player the player whose inventory is being checked
     * @return the number of tallied free spaces for bottles
     */
    private int numInvFreeSpace(Player player) {

        // Variables
        int tally = 0;
        PlayerInventory inv = player.getInventory();
        ItemStack[] is = inv.getContents(); //returns array of ItemStacks[] from inv

        player.sendMessage(String.format("%s%d", "amount of indexes: ", is.length));
        //ArrayList<Integer> emptySlots = new ArrayList<Integer>();
        for (int i = 0; i < inv.getSize(); i++) {
            if (is[i] == null || is[i].isSimilar(new ItemStack(Material.AIR))) tally += 64;
        }
        for (int i = 0; i < inv.getSize(); i++) { // pi = player inventory object
            if (is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
                if (is[i].isSimilar(new ItemStack(Material.EXP_BOTTLE))) {
                    tally += 64 - (is[i].getAmount());
                }
            }
        }
        return tally;
    }

    /**
     * Tier One conversions for players with levels between 1-16.
     *
     * @param useLevel the amount of levels requested to be converted by the player
     * @param currentLevel the current level of experience of the player
     * @return the amount of experience orbs to remove
     */
    private int formulaTierOne(int useLevel, int currentLevel) {
        int targetLevel = currentLevel - useLevel;
        int amountToRemove = 0;
        if (targetLevel >= 0) {
            for (int i = 0; i < useLevel; i++) {
                amountToRemove += (2 * (useLevel - i) + 7);
            }
        }
        return amountToRemove; //Gives XP: Tier 1 Only
    }

    /**
     * Tier Two conversions for players with levels 1-16.
     *
     * @param useLevel the amount of levels requested to be converted by the player
     * @param currentLevel the current level of experience of the player
     * @return the amount of experience orbs to remove
     */
    private int forumlaTierTwo(int useLevel, int currentLevel) {
        int amountToRemove = 0;
        if (useLevel <= currentLevel - 16) {
            int tempLevel = currentLevel - 16;
            for (int i = 0; i < useLevel; i++) {
                amountToRemove += (5 * (tempLevel - i) + 42);
            }
            return amountToRemove; //Gives XP: Tier 2 Only
        } else if (useLevel > currentLevel - 16) {
            int tempLevel = currentLevel - 16;
            for (int i = 0; i < tempLevel; i++) {
                amountToRemove += (5 * (tempLevel - i) + 42);
            }
            int useTier1 = useLevel - tempLevel;
            tempLevel = 16;
            if (useTier1 >= 0) {
                for (int i = 0; i < useTier1; i++) {
                    amountToRemove += (2 * (tempLevel - i) + 7);
                }
                return amountToRemove;
            }//Gives XP: Tier 1 and 2
        }
        return amountToRemove;
    }

    /**
     * Tier Three conversions for players with greater than 32 levels.
     *
     * @param useLevel the amount of levels requested to be converted by the player
     * @param currentLevel the current level of experience of the player
     * @return the amount of experience orbs to remove
     */
    private int forumlaTierThree(int useLevel, int currentLevel) {
        int targetLevel = currentLevel - useLevel;
        if (targetLevel >= 0) {
            if (useLevel <= currentLevel - 31) {//result level will be 31 or more
                int tempLevel = currentLevel - 31;
                int temp = 0;
                for (int i = 0; i < useLevel; i++) {
                    temp += 9 * (tempLevel - i) + 121;
                }
                return temp;
                //returns tier 3 XP that needs to be removed(counted and bottled)!
            } else if (useLevel > currentLevel - 31) {//result intent level will be less than 31
                int useOnTier3 = currentLevel - 31;//levels to use with formula 3
                int total = 0;
                for (int i = 0; i <= useOnTier3; i++) {
                    total += (9 * (useOnTier3 - i) + 121);//add tier 3 xp to bottle
                }
                useLevel -= useOnTier3;
                int tier2Levels = (currentLevel - useOnTier3 - 16);
                if (useLevel <= tier2Levels) {//leaving 16 or more levels left
                    for (int i = 0; i < useLevel; i++) {
                        total += (5 * (tier2Levels - i) + 42);
                    }
                    return total; //returns tier 2 and 3 XP that needs to be removed(counted and bottled)!
                }
                int useOnTier2 = useLevel - 16; //				int tier1Levels = (currentLevel - useOnTier3 - useOnTier2);
                if (useLevel > currentLevel - useOnTier3 - 16) {//enough useLevels to access tier 1 this does not imply that the other methods agree
                    for (int i = 0; i < useOnTier2; i++) {
                        total += 5 * (useOnTier2 - i) + 42;
                    }
                    int useOnTier1 = useLevel - useOnTier2 - useOnTier3;
                    int tempLevel = currentLevel - useOnTier2 - useOnTier3;
                    for (int i = 0; i < useOnTier1; i++) {
                        total += (2 * (tempLevel - i) + 7);
                    }
                    return total; //returns tier 1 (if any) 2 and 3 XP that needs to be removed(counted and bottled)!
                }
            }
        }
        return 0;
    }
}

