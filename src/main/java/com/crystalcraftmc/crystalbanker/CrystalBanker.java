/*
 * Copyright (c) 2019 CrystalCraftMC
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.crystalcraftmc.crystalbanker;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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
 * @version 2019.01.26.v1.0.4
 */
public class CrystalBanker extends JavaPlugin {

    private FileConfiguration config;

    /**
     * Method executed on server startup.
     */
    public void onEnable() {
        getLogger().info("CrystalBanker has been initialized!");
        try {
            File database = new File(getDataFolder(), "config.yml");
            if (!database.exists()) {
                saveDefaultConfig();
            }
            config = getConfig();
        } catch (Exception e1) {
            getLogger().info("CrystalBanker failed to initialize.");
            e1.printStackTrace();
        }
    }

    /**
     * Method executed on server shutdown.
     */
    public void onDisable() {
        getLogger().info("CrystalBanker has stopped.");
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

        if (!(sender instanceof Player)) {
            String txt = config.getString("non-player-fail");
            txt = ChatColor.translateAlternateColorCodes('&', txt);
            sender.sendMessage(txt);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("crystalbanker")) {
            Player p = (Player) sender;

            if (args.length > 0 && args.length < 3) {

                if (args[0].equalsIgnoreCase("reload") && p.hasPermission("crystalbanker.reload")) {
                    this.reloadConfig();
                    config = this.getConfig();
                    String txt = config.getString("reload-config");
                    txt = ChatColor.translateAlternateColorCodes('&', txt);
                    p.sendMessage(txt);
                    return true;
                }

                if (args[0].equalsIgnoreCase("levelup") && p.hasPermission("crystalbanker.levelup")) {
                    //TODO MOVING message functions out of layers to top level? Maybe
                    int amountToRemove = countBottles(p);
                    withdrawMethod(p, amountToRemove); //todo make void function, ensure all usages comply
                    return true;
                }

                if (args[0].equalsIgnoreCase("zero") && p.hasPermission("crystalbanker.zero")) {
                    zero(p);
                    return true;
                }

                if (args[0].equalsIgnoreCase("deposit") && p.hasPermission("crystalbanker.deposit")) {
                    if (args.length == 2 && isInt(args[1], p)) {
                        int storeLevels = toInt(p, args[1]);
                        depositMethod(p, storeLevels);
                        return true;
                    } else {
                        String part1 = config.getString("failed-deposit-levels-1");
                        part1 = ChatColor.translateAlternateColorCodes('&', part1);
                        String part2 = config.getString("failed-deposit-levels-2");
                        part2 = ChatColor.translateAlternateColorCodes('&', part2);
                        p.sendMessage( part1 + p.getLevel() + part2 );

                        String extra = config.getString("failed-deposit-levels-extra");
                        extra = ChatColor.translateAlternateColorCodes('&', extra);
                        p.sendMessage(extra);

                        String usage = config.getString("failed-deposit-levels-usage");
                        usage = ChatColor.translateAlternateColorCodes('&', usage);
                        p.sendMessage(usage);
                        return false;
                    }
                }

                if (args[0].equalsIgnoreCase("withdraw") && p.hasPermission("crystalbanker.withdraw")) {
                    if (args.length == 2 && isInt(args[1], p)) {
                        int amountToRemove = toInt(p, args[1]);
                        withdrawMethod(p, amountToRemove);
                        return true;
                    } else {
                        String part1 = config.getString("failed-withdraw-cmd-1");
                        part1 = ChatColor.translateAlternateColorCodes('&', part1);
                        String part2 = config.getString("failed-withdraw-cmd-2");
                        part2 = ChatColor.translateAlternateColorCodes('&', part2);
                        p.sendMessage( part1 + countBottles(p) + part2 );

                        String usage = config.getString("failed-withdraw-cmd-usage");
                        usage = ChatColor.translateAlternateColorCodes('&', usage);
                        p.sendMessage(usage);
                        return true;
                    }
                }
            }
        }
        String help = config.getString("help");
        help = ChatColor.translateAlternateColorCodes('&', help);
        sender.sendMessage(help);
        return false;
    }


    /**
     * Calculates the approximate number of orbs to be given per bottle of experience. Randomness is introduced in an
     * attempt to replicate normal gameplay if you were breaking them regularly.
     *
     * @return the number of experience orbs for a bottle of experience
     */
    //TODO Confirm this random is used well, this is the tie in for taxing
    private double orbsPerBottle() {
        Random rand = new Random();
        return ((rand.nextFloat() * (11 - 3)) + 3);
    }

    /**
     * Checks to see if the number of bottles to use for conversion is an integer.
     *
     * @param bottlesToUse the number of bottles to convert
     * @param sender the player whose input is being validated
     * @return true if the number is an integer
     */
    private boolean isInt(String bottlesToUse, CommandSender sender) {
        try {
            Integer.parseInt(bottlesToUse.trim());
        } catch (NumberFormatException nFE) {
            String check = config.getString("integer-check");
            check = ChatColor.translateAlternateColorCodes('&', check);
            sender.sendMessage(check);
            return false;
        }
        return true;
    }

    /**
     * Checks to make sure the value passed for the bottles to use is actually an integer. If not, it trims it and
     * returns the trimmed value.
     *
     * @param player       the player using the command
     * @param bottlesToUse the number of bottles to be used for converting
     * @return int an integer value of the number of bottles to use
     */
    private int toInt(Player player, String bottlesToUse) {
        int number;
        try {
            String temp = bottlesToUse.trim();
            number = Integer.parseInt(temp);
        } catch (NumberFormatException nFE) {
            String check = config.getString("integer-check");
            check = ChatColor.translateAlternateColorCodes('&', check);
            player.sendMessage(check);
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
     */
    //TODO review function to ensure good distribution of orbs, maybe add configurable tax on orbs?
    private void bottlesToXP(int amountToRemove, Player player) {
        int leftToRemove = amountToRemove;
        PlayerInventory pi = player.getInventory();
        ItemStack[] is = pi.getContents();
        for (int i = 0; i < pi.getSize(); i++) {
            if (is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
                if (is[i].isSimilar(new ItemStack(Material.EXPERIENCE_BOTTLE))) {
                    if (leftToRemove > 0) {//if more to remove then...
                        if (is[i].getAmount() < leftToRemove) {//if found
                            leftToRemove -= is[i].getAmount();
                            pi.clear(i);
                        } else if (is[i].getAmount() == leftToRemove) {
                            pi.clear(i);
                            leftToRemove = 0;
                            break;
                        } else if (is[i].getAmount() > leftToRemove) {
                            is[i].setAmount((is[i].getAmount() - leftToRemove));
                            leftToRemove = 0;
                            break;
                        }
                    }
                }
            }
        }
        if (leftToRemove == 0) {
            int xpOrbTotal = 0;
            for (int i = 0; i < amountToRemove; i++) {
                int xpOrb = (int) Math.round(orbsPerBottle());
                xpOrbTotal += xpOrb;
            }
            player.giveExp(xpOrbTotal);

            String part1 = config.getString("withdraw-success-1");
            part1 = ChatColor.translateAlternateColorCodes('&', part1);
            String part2 = config.getString("withdraw-success-2");
            part2 = ChatColor.translateAlternateColorCodes('&', part2);
            player.sendMessage( part1 + amountToRemove + part2 );
        }
    }

    /**
     * Withdraw experience orbs from experience bottles.
     *
     * @param player         the player withdrawing orbs
     * @param amountToRemove the amount of bottles to use for withdrawing
     * @return true if the withdrawal happens successfully
     */
    private void withdrawMethod(Player player, int amountToRemove) {
        if (countBottles(player) < amountToRemove) {
            String part1 = config.getString("withdraw-fail-exp-1");
            part1 = ChatColor.translateAlternateColorCodes('&', part1);
            String part2 = config.getString("withdraw-fail-exp-2");
            part2 = ChatColor.translateAlternateColorCodes('&', part2);
            String part3 = config.getString("withdraw-fail-exp-3");
            part3 = ChatColor.translateAlternateColorCodes('&', part3);
            player.sendMessage( part1 + countBottles(player) + part2 + amountToRemove + part3);
            return;
        } else if (countBottles(player) >= amountToRemove) {
            bottlesToXP(amountToRemove, player);
            return;
        }
    }

    /**
     * Counts the number of bottles in the player's inventory before proceeding with a transaction.
     *
     * @param player the player who is being checked
     * @return true if the player has enough bottles
     */
    private int countBottles(Player player) {
        PlayerInventory pi = player.getInventory(); //TODO Make sure slot count is correct
        ItemStack[] is = pi.getContents(); //TODO see previous todo
        int tally = 0;
        for (int i = 0; i < pi.getSize(); i++) {
            if (is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) { //todo make sure air is still correct
                if (is[i].isSimilar(new ItemStack(Material.EXPERIENCE_BOTTLE))) {
                    tally += is[i].getAmount();
                }
            }
        }
        return tally;
    }

    //TODO REBUILD ALL METHODS USING LEVELS!
    /**
     * Returns experience bottles based on the amount of experience orbs the player has.
     *
     * @param player the player who is converting orbs to bottles
     * @param storeLevels the number of orbs/levels to convert to bottles
     * @return true if the transaction successfully completes
     */
    private boolean depositMethod(Player player, int storeLevels) {
        int currentLevel = player.getLevel();
        if (countXP(player, storeLevels)) {
            if (currentLevel <= 16) {
                expToBottles(player, formulaTierOne(storeLevels, currentLevel));
                return true;
            } else if (currentLevel <= 31) {
                expToBottles(player, formulaTierTwo(storeLevels, currentLevel));
                return true;
            } else if (currentLevel >= 32) {
                expToBottles(player, formulaTierThree(storeLevels, currentLevel));
                return true;
            }
        }
        return false;
    }

    //TODO THIS MAY NEED TO BE DONE IF WE NO LONGER USE FULL LEVELS TO ACCESS ITEMS
    /**
     * Checks to make sure the player has at least one level of experience.
     *
     * @param p the player being checked
     * @return true if the player has greater than seven orbs (i.e. one level)
     */
    private boolean countXP(Player p, int amountToRemove) {
        if (p.getLevel() >= amountToRemove)
            return true;
        else {
            String part1 = config.getString("deposit-fail-exp-1");
            part1 = ChatColor.translateAlternateColorCodes('&', part1);
            String part2 = config.getString("deposit-fail-exp-2");
            part2 = ChatColor.translateAlternateColorCodes('&', part2);
            String part3 = config.getString("deposit-fail-exp-3");
            part3 = ChatColor.translateAlternateColorCodes('&', part3);
            p.sendMessage( part1 +  p.getLevel() + part2 + amountToRemove + part3);

            String extra = config.getString("deposit-fail-exp-extra");
            extra = ChatColor.translateAlternateColorCodes('&', extra);
            p.sendMessage( extra);
            return false;
        }
    }

    /**
     * Cashes in all of a player's experience orbs in exchange for experience bottles.
     *
     * @param p the player cashing in their orbs
     */
    private void zero(Player p) {//TODO Build a fill inventory with EXP bottles METHODS
        int currentXP = p.getTotalExperience();
        //more EXP than zero
        if (currentXP > 0) {
            int bottles = 0;
            float trackXP = currentXP;
            int maxBottleAmount = numInvFreeSpace(p);
            while (bottles < maxBottleAmount && (trackXP > 0)) {
                trackXP -= orbsPerBottle();
                bottles++;
            }
            if (trackXP < 0) {
                trackXP = 0;
                bottles--;
            }
            if (bottles > maxBottleAmount) {
                bottles--;
            }
            p.setExp((float) 0);
            p.setLevel(0);
            p.setTotalExperience(0);
            p.giveExp(Math.round(trackXP));
            p.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, bottles));

            String part1 = config.getString("zero-success-0-1");
            part1 = ChatColor.translateAlternateColorCodes('&', part1);
            String part2 = config.getString("zero-success-0-2");
            part2 = ChatColor.translateAlternateColorCodes('&', part2);
            p.sendMessage( part1 +  (currentXP - trackXP) + part2);

            part1 = config.getString("zero-success-1-1");
            part1 = ChatColor.translateAlternateColorCodes('&', part1);
            part2 = config.getString("zero-success-1-2");
            part2 = ChatColor.translateAlternateColorCodes('&', part2);
            String part3 = config.getString("zero-success-1-3");
            part3 = ChatColor.translateAlternateColorCodes('&', part3);
            p.sendMessage( part1 +  trackXP + " of " + p.getTotalExperience() + part2 + bottles + part3);

        } else {
            String fail = config.getString("zero-fail");
            fail = ChatColor.translateAlternateColorCodes('&', fail);
            p.sendMessage(fail);
        }
    }

    /**
     * Convert experience orbs to bottles.
     *
     * @param player the player who is converting orbs to bottles
     * @param expToRemove the amount of experience orbs to remove from the player
     */
    private void expToBottles(Player player, int expToRemove) {
        int currentXP = player.getTotalExperience();//TODO 1.13.2 re-verify totalExperience works correctly, as intended
        if (currentXP > (currentXP - expToRemove)) {
            int bottles = 0;
            float trackXP = currentXP;
            while (trackXP > (currentXP - expToRemove)) {
                trackXP -= orbsPerBottle();
                bottles++;
            }
            if (trackXP < 0) {
                trackXP = 0;
                bottles--;
            }
            if (numInvFreeSpace(player) >= bottles) {
                String part1 = config.getString("deposit-success-1");
                part1 = ChatColor.translateAlternateColorCodes('&', part1);
                String part2 = config.getString("deposit-success-2");
                part2 = ChatColor.translateAlternateColorCodes('&', part2);
                player.sendMessage( part1 +  expToRemove + part2);

                //todo !!!!FLAG NOTE NEW CHANGE TEST 1.13.2 THOROUGHLY BEFORE PUBLISHING!!!!
                //todo re-verify what these do for 1.13.2

                player.setExp((float) 0);
                player.setLevel(0);
                player.setTotalExperience(0);
                player.giveExp((int) trackXP);
                player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, bottles));
            } else {
                String fail = config.getString("inventory-fail");
                fail = ChatColor.translateAlternateColorCodes('&', fail);
                player.sendMessage(fail);
            }
        }
    }

    /**
     * Calculates the number of free spaces in the player's inventory that can be used to fill with experience bottles.
     *
     * @param player the player whose inventory is being checked
     * @return the number of tallied free spaces for bottles
     */
    //todo re-verify for 1.13.2
    private int numInvFreeSpace(Player player) {
        PlayerInventory pi = player.getInventory();
        ItemStack[] is = pi.getContents(); //returns array of ItemStacks[] from inv
        int tally = 0;
        for (int i = 0; i < pi.getSize(); i++) {
            if (is[i] == null || is[i].isSimilar(new ItemStack(Material.AIR))) {
                tally += 64;
            }
        }
        for (int i = 0; i < pi.getSize(); i++) { // pi = player inventory object
            if (is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
                if (is[i].isSimilar(new ItemStack(Material.EXPERIENCE_BOTTLE))) {
                    tally += 64 - (is[i].getAmount());
                }
            }
        }
        return tally;
    }

    //DO NOT TOUCH THIS COMMENT: FOR LEVELS 1, 2, 3, ..., 14, 15, 16? TEST
    /**
     * Tier One conversions for players with levels between 1-16.
     *
     * @param useLevel the amount of levels requested to be converted by the player
     * @param currentLevel the current level of experience of the player
     * @return the amount of experience orbs to remove
     */
    private static int formulaTierOne(int useLevel, int currentLevel) {
        int amountToRemove = 0;
        for (int i = 0; i < useLevel; i++) {
            amountToRemove += (2 * (currentLevel - i) + 5);
        }
        return amountToRemove; //Gives XP: Tier 1 Only
    }

    //DO NOT TOUCH THIS COMMENT: FOR LEVELS 1, 2, ..., 16?, 17, 18, ..., 30, 31?, 32? TEST
    /**
     * Tier Two conversions for players with levels 1-16.
     *
     * @param useLevel the amount of levels requested to be converted by the player
     * @param currentLevel the current level of experience of the player
     * @return the amount of experience orbs to remove
     */
    private static int formulaTierTwo(int useLevel, int currentLevel) {
        int amountToRemove = 0;
        if (useLevel <= currentLevel - 16) {
            int tempLevel = currentLevel - 16;
            for (int i = 0; i < useLevel; i++) {
                amountToRemove += (5 * (tempLevel - i) + 37);
            }
            return amountToRemove; //Gives XP: Tier 2 Only
        } else if (useLevel > currentLevel - 16) {
            int tempLevel = currentLevel - 16;
            for (int i = 0; i < tempLevel; i++) {
                amountToRemove += (5 * (tempLevel - i) + 37);
            }
            int useTier1 = useLevel - tempLevel;
            amountToRemove += formulaTierOne(useTier1, 16);
            //TODO OLD FUNCTION KEEP INCASE OF ISSUES			tempLevel = 16;
            //			if (useTier1 >= 0) {
            //				for(int i = 0; i < useTier1; i++){
            //					amountToRemove += (2*(tempLevel-i) + 5);
            //				}
            //				return amountToRemove;
            //}Gives XP: Tier 1 and 2
        }
        return amountToRemove;
    }

    //DO NOT TOUCH THIS COMMENT: FOR LEVELS 1, 2, 3, ..., 14, 15, 16? TEST
    /**
     * Tier Three conversions for players with greater than 32 levels.
     *
     * @param useLevel the amount of levels requested to be converted by the player
     * @param currentLevel the current level of experience of the player
     * @return the amount of experience orbs to remove
     */
    private int formulaTierThree(int useLevel, int currentLevel) {
        int targetLevel = currentLevel - useLevel;
        if (targetLevel >= 0) {
            if (useLevel <= currentLevel - 31) {//result level will be 31 or more
                int temp = 0;
                for (int i = 0; i < useLevel; i++) {
                    temp += 9 * ((currentLevel - 31) - i) + 112;
                }
                return temp;
            } else if (useLevel > currentLevel - 31) {//resultant level will be less than 31
                int useOnTier3 = currentLevel - 31;//levels to use with formula 3
                int total = 0;
                for (int i = 1; i <= useOnTier3; i++) {
                    total += (9 * (useOnTier3 - i) + 121);//add tier 3 xp to bottle
                }
                total += formulaTierTwo((useLevel - useOnTier3), (currentLevel - useOnTier3));
                return total;
            }
        }
        return 0;
    }
}
