/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 CrystalCraftMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.crystalcraftmc.crystalbanker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

public class CrystalBanker extends JavaPlugin {

    public void onEnable() {
        getLogger().info(ChatColor.GRAY + "CrystalBanker has been initialized!");

        try {
            File database = new File(getDataFolder(), "config.yml");
            if (!database.exists()) saveDefaultConfig();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void onDisable() {
        getLogger().info(ChatColor.GRAY + "CrystalBanker has been stopped by the server.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = (Player) sender;
        Inventory inv = player.getInventory();

        if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            this.reloadConfig();
            player.sendMessage(ChatColor.GRAY + "Configuration reloaded!");
        }

        if (isPlayer(sender) && player.hasPermission("crystalbanker.transaction")) {

            if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && args[0].equalsIgnoreCase("deposit") && isInt(args[1])) {
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: /crystalbanker deposit");

                    if(countXP(player)){ //No need to check amount of xp compared to the arg[2]... this is done inside the xpToBottles
						int storeLevels = Integer.parseInt(args[1].trim());
						int currentLevel = player.getLevel();
						int clXP = player.getTotalExperience();
						int xpTally = 0;
						if(currentLevel <= 16){ 
							xpToBottles(player, formulaTierOne(storeLevels, currentLevel, clXP, xpTally));
							
                        return true;
                    } else if (currentLevel <= 31) {
                        xpToBottles(player, forumlaTierTwo(storeLevels, currentLevel, clXP, xpTally));
                        return true;
                    } else if (currentLevel >= 32) {
                        xpToBottles(player, forumlaTierThree(storeLevels, currentLevel, clXP, xpTally));
                        return true;
                    }
                } else return false;
            } else if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && args[0].equalsIgnoreCase("withdraw") && isInt(args[1])) {//the int represents the bottle amount to be used
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: /crystalbanker withdraw # fired");
                int amountToRemove = Integer.parseInt(args[1]);
                if (countBottles(player, inv, amountToRemove) >= amountToRemove) {
                    bottlesToXP(amountToRemove, player, inv);
                    return true;
                } else return false;
            } else getLogger().info(ChatColor.RED + "Plugin failed to recognize commands.");
        }
        return false;
    }

    //this cause mayhem and destruction to everyone... no, no, not the method, what the method represents, if return true;
    public boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return false;
        } else {
            return true;
        }
    }

    //Determines how many orbs are in a bottle: the value is between 3 and 11. THIS METHOD SHOULD (ALMOST) ALWAYS be in a FOR loop
    public static float orbsPerBottle() {
        Random rand = new Random();
        return (rand.nextFloat() * (11 - 3)) + 3; //float helps the player, and increases player xp gain by 7% (a difference of 10k xp over 20,000 bottles)
    }

    //searches inv and counts to see how many xp bottles you have in your inventory (needed method)
    public static int countBottles(Player player, Inventory inv, int amountToRemove) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Count bottles fired");
        int tally = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack invSlot = inv.getItem(i);
            ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);
            if (invSlot == null) continue;
            else if (!invSlot.getType().equals(xpBottle.getType())) continue;
            else if (invSlot.getType().equals(xpBottle.getType())) tally += invSlot.getAmount();
            else break;
        }
        if (tally < amountToRemove) {
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Bottles counted: " + tally);
            player.sendMessage(ChatColor.RED + "You don't have enough xp bottles in your inventory to exchange the amount you entered.");
        } else if (tally >= amountToRemove) {
            player.sendMessage(ChatColor.GRAY + "XP Bottles counted: " + tally);
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Bottles counted: " + tally);
            return tally;
        }
        return 0;
    }

    //makes sure that the 2nd argument of the /crystalbanker command is an integer
    public static boolean isInt(String bottlesToUse) {
        try {
            Integer.parseInt(bottlesToUse.trim());
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Your args[2] was: " + bottlesToUse);
        } catch (NumberFormatException nFE) {
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Your args[2] failed to convert to an int");
            return false;
        }
        return true;
    }

    //changes bottles to xp which can be spent at the shops
    public static void bottlesToXP(int amountToRemove, Player player, Inventory inv) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: bottlesToXP has been fired. Searching and removing numeric values.");
        int leftToRemove = amountToRemove;
        for (int i = 0; i < inv.getSize(); i++) {//search inventory for xp bottles
            ItemStack invSlot = inv.getItem(i);
            ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);
            if (!invSlot.getType().equals(xpBottle.getType()) || invSlot == null) continue;//ignore if not xp
            else if (invSlot.getType().equals(xpBottle.getType())) {//if xpBottles then...
                if (leftToRemove >= 0) {//if more to remove then...
                    if (invSlot.getAmount() < leftToRemove) {//if found
                        leftToRemove -= invSlot.getAmount();
                        inv.clear(i);
                    } else if (invSlot.getAmount() == leftToRemove) {
                        leftToRemove -= invSlot.getAmount();
                        inv.clear(i);
                        break;
                    } else if (invSlot.getAmount() > leftToRemove) {
                        invSlot.setAmount((invSlot.getAmount() - leftToRemove));
                        break;
                    }
                }
            }
        }

        //Add levels
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Preparing to translate numeric values into experience.");
        if (leftToRemove == 0) {
            int xpOrbTotal = 0;
            for (int i = 0; i < amountToRemove; i++) {//translate everything into orbs
                int xpOrb = Math.round(orbsPerBottle());
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Values of Orbs being output: " + i + ". " + xpOrb);
                xpOrbTotal += xpOrb;
            }
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Values of TotalOrbs output: " + xpOrbTotal);
            player.sendMessage(ChatColor.DARK_AQUA + "You withdrawal has been processed. Thank you for supporting Crystal Banks!");
            player.giveExp(xpOrbTotal);
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Player xp from TotalOrbs: " + player.getTotalExperience());
        }
    }

    //makes sure there are at least 7 levels on player, and that the change will not result in negative XP.
    public static boolean countXP(Player player) {
        int currentXP = player.getTotalExperience();
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Player's current XP amount (not level) is: " + currentXP);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: To use command must have at least positive amount, and (not strict in some manners, but more than 7 levels)");
        if (currentXP > 272)
            return true;//CANT DECIDE if there should be a limit to how many levels you must have before using the command ...cool down for this command should  be 10 seconds
        else {
            player.sendMessage(ChatColor.RED + "You have less than 7 levels! Please have at least 7 levels before retrying.");
            return false;
        }
    }

    //stores a players xp into bottles
    public static void xpToBottles(Player player, int totalXPToChange) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: XP to bottles fired.");
        int currentXP = player.getTotalExperience();
        if (currentXP > totalXPToChange) {//amount of player xp is checked already so need to worry if player entered more than player has.
            float resultXP = 0;
            float trackXP = currentXP;
            int i = 0;
            int targetXP = currentXP - totalXPToChange;
            for (i = 0; trackXP > targetXP; i++) {
                trackXP -= orbsPerBottle();
                if (targetXP > 0 && targetXP <= trackXP) resultXP = trackXP;
                else break;
            }
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: number of bottles to award: " + i + "Debug: newXP (amount) (levels): " + "(" + resultXP + ")" + "(" + player.getLevel() + ")");
            int bottles = i;
            if (inventorySpace(player, player.getInventory()) >= i) {
                player.sendMessage(ChatColor.DARK_AQUA + "You deposit of " + totalXPToChange + " experience has been processed. Thank you for supporting Crystal Banks!");
                player.setTotalExperience(Math.round(resultXP));
                player.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
            } else {
                player.sendMessage(ChatColor.RED + "Due to lack of XP your deposit was canceled! " + "Crystal Banks forgives the overdraft, and waives all fees. Next time please ensure you enter the correct amount!");
                return;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Due to lack inventory space your transaction was canceled! " + "Crystal Banks forgives your awful teasing, but please! Next time! Have enough room in your inventory!");
            return;
        }
    }

    public static int inventorySpace(Player player, Inventory inv) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Count inventory space.");
        int tally = 0;
        ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack invSlot = inv.getItem(i);
            if (!invSlot.getType().equals(xpBottle.getType())) continue;
            else if (invSlot.getType().equals(null)) tally += 64;
            else if (invSlot.getType().equals(xpBottle.getType())) {
                if (invSlot.getAmount() >= 64) {
                    tally += (64 - invSlot.getAmount());
                } else continue;
            } else break;
        }
        return tally;
    }

    //these last three methods are the just formulas, and conversion rates of level to XP.
    //Tier One is the formula for leveling up for levels 1-16
    public int formulaTierOne(int useLevel, int currentLevel, int clXP, int xpTally) {
        int targetLevel = currentLevel - useLevel;
        if (targetLevel >= 0) xpTally = clXP - (17 * (targetLevel));
        return xpTally; //Gives XP: Tier 1 Only
    }

    //Tier Two is the formula for leveling up for levels 17-31
    public int forumlaTierTwo(int useLevel, int currentLevel, int clXP, int xpTally) {
        if (useLevel <= currentLevel - 16) {
            int tempLevel = currentLevel - 16;
            int temp = 0;
            for (int i = 0; i < useLevel; i++) {
                temp += (3 * (tempLevel - i) + 17);
            }
            xpTally = clXP - temp;
            return xpTally;//Gives XP: Tier 2 Only
        } else if (useLevel > currentLevel - 16) {
            int tempLevel = currentLevel - 16;
            int temp = 0;
            for (int i = 0; i < tempLevel; i++) {
                temp += (3 * (tempLevel - i) + 17);
            }
            temp += 17 * (useLevel - tempLevel);
            xpTally = clXP - temp;
            return xpTally;
        }//Gives XP: Tier 1 and 2
        return 0;
    }

    //Tier Three is the formula for leveling up for levels 32+
    public static int forumlaTierThree(int useLevel, int currentLevel, int clXP, int xpTally) {
        int targetLevel = currentLevel - useLevel;
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug:Experiance at Target Level: " + targetLevel);//how many levels left
        if (targetLevel >= 0) {
            if (useLevel <= currentLevel - 31) {//result level will be 31 or more
                int tempLevel = currentLevel - 31;
                int temp = 0;
                for (int i = 0; i < useLevel; i++) {
                    temp += 10 * (tempLevel - i) + 62;
                }
                xpTally = clXP - temp;
                return xpTally;//returns tier 3 XP that needs to be removed(counted and bottled)!
            } else if (useLevel > currentLevel - 31) {//result intent level will be less than 31
                int useOnTier3 = currentLevel - 31;//levels to use with formula 3
                int total = 0;
                for (int i = 0; i <= useOnTier3; i++) {
                    total += (10 * (useOnTier3 - i) + 62);//add tier 3 xp to bottle
                }
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug:Levels used tier 3: " + useOnTier3);
                useLevel -= useOnTier3;
                int tier2Levels = (currentLevel - useOnTier3 - 16);
                if (useLevel <= tier2Levels) {//leaving 16 or more levels left
                    for (int i = 0; i < useLevel; i++) {
                        total += (3 * (tier2Levels - i) + 17);
                    }
                    xpTally = clXP - total;
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug:Levels used tier 2:" + useLevel);
                    return xpTally;//returns tier 2 and 3 XP that needs to be removed(counted and bottled)!
                }
                int useOnTier2 = useLevel - 16;
                if (useLevel > currentLevel - useOnTier3 - 16) {//enough useLevels to access tier 1 this does not imply that the other methods agree
                    for (int i = 0; i < useOnTier2; i++) {
                        total += 3 * (useOnTier2 - i) + 17;
                    }
                    total += 17 * (useLevel - useOnTier2);
                    xpTally = clXP - total;
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug:Levels used tier 2: " + useOnTier2);
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug:Levels used tier 1: " + (useLevel - useOnTier2));
                    return xpTally; //returns tier 1 (if any) 2 and 3 XP that needs to be removed(counted and bottled)!
                }
            }
        }
        return 0;
    }
}
