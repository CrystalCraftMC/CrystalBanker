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

import java.io.File;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class CrystalBanker extends JavaPlugin {
	//ch
	public void onEnable() {
		getLogger().info(ChatColor.GRAY + "CrystalBanker has been initialized!");
		try {
			File database = new File(getDataFolder(), "config.yml");
			if (!database.exists()) saveDefaultConfig();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	//ch
	public void onDisable() {
		getLogger().info(ChatColor.GRAY + "CrystalBanker has been stopped by the server.");
	}
	//ch
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			reloadMethod(sender);
		}

		if (isPlayer(sender) && sender.hasPermission("crystalbanker.transaction")) {
			Player player = (Player) sender;
			Inventory inv = player.getInventory();

			if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && args[0].equalsIgnoreCase("deposit")) {
				depositMethod(player, args[1]);
			} 

			else if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && args[0].equalsIgnoreCase("withdraw")) {
				withdrawMethod(player, args[1], inv);
			} 
			else getLogger().info(ChatColor.RED + "Plugin failed to recognize commands.");
		}
		return false;
	}
	//ch
	private boolean isPlayer(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
			return false;
		} else {
			return true;
		}
	}
	//ch
	private static float orbsPerBottle() {
		Random rand = new Random();
		return (rand.nextFloat() * (11 - 3) + 3);
	}

	private static int isInt(Player player, String bottlesToUse) {
		int number;
		try {
			String temp = bottlesToUse.trim();
			number = Integer.parseInt(temp);
			Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Your args[1] was: " + number);
		} catch (NumberFormatException nFE) {
			player.sendMessage(ChatColor.RED + "There was a problem with the value you entered. Try again.");
			return 0;
		}
		return number;
	}

	private static void bottlesToXP(int amountToRemove, Player player, Inventory inv) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: bottlesToXP has been fired. Searching and removing numeric values.");
		int leftToRemove = amountToRemove;
		ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);
		for (int i = 0; i < inv.getSize(); i++) {//search inventory for xp bottles
			ItemStack invSlot = inv.getItem(i);
			if (invSlot.isSimilar(xpBottle)) {//if xpBottles then...
				if (leftToRemove >= 0) {//if more to remove then...
					if (invSlot.getAmount() < leftToRemove) {//if found
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
			else continue;
		}

		//Add levels
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Preparing to translate numeric values into experience.");
		if (leftToRemove == 0) {
			int xpOrbTotal = 0;
			for (int i = 0; i < amountToRemove; i++) {//translate everything into orbs
				int xpOrb = Math.round(orbsPerBottle());
				Bukkit.broadcastMessage(ChatColor.GRAY + "Debug: Values of Orbs being output: " + i + ". " + xpOrb);
				xpOrbTotal += xpOrb;
			}
			Bukkit.broadcastMessage(ChatColor.GRAY + "Debug: Values of TotalOrbs output: " + xpOrbTotal);
			player.sendMessage(ChatColor.DARK_AQUA + "You withdrawal has been processed. Thank you for supporting Crystal Banks!");
			player.giveExp(xpOrbTotal);
			Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Total Player xp after additional TotalOrbs: " + player.getTotalExperience());
		}
	}

	private boolean reloadMethod(CommandSender sender){
		this.reloadConfig();
		sender.sendMessage(ChatColor.GRAY + "Configuration reloaded!");
		return true;
	}

	//ch
	private boolean withdrawMethod(Player player, String args, Inventory inv){
		int amountToRemove = isInt(player, args);
		if (countBottles(player, inv, amountToRemove)) {
			bottlesToXP(amountToRemove, player, inv);
			return true;
		}
		else return false;
	}
	//ch
	private static boolean countBottles(Player player, Inventory inv, int amountToRemove) {
         Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Count bottles fired");
         int tally = 0;
         ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);
         for (int i = 0; i < inv.getSize(); i++) {
             ItemStack invSlot = inv.getItem(i);
             if (invSlot.getType().equals(xpBottle.getType())) tally += invSlot.getAmount();
         }
         if (tally < amountToRemove) {
             Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Bottles counted: " + tally);
             player.sendMessage(ChatColor.RED + "You don't have enough xp bottles in your inventory to exchange the amount you entered.");
             return false;
         } else if (tally >= amountToRemove) {
             player.sendMessage(ChatColor.GRAY + "XP Bottles counted: " + tally);
             Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Bottles counted: " + tally);
             return false;
         }
         return true;
     }
	//ch
	private boolean depositMethod(Player player, String args){
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: /crystalbanker deposit");
		int storeLevels = isInt(player, args);
		int currentLevel = player.getLevel();
		if (countXP(player)) {
			if (currentLevel <= 16) {
				xpToBottles(player, formulaTierOne(storeLevels, currentLevel), storeLevels);
				return true;
			} else if (currentLevel <= 31) {
				xpToBottles(player, forumlaTierTwo(storeLevels, currentLevel), storeLevels);
				return true;
			} else if (currentLevel >= 32) {
				xpToBottles(player, forumlaTierThree(storeLevels, currentLevel), storeLevels);
				return true;
			}
		}
		return false;
	}
	//ch
	private static boolean countXP(Player player) {
		int currentXP = player.getTotalExperience();
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: Player's current XP amount (not level) is: " + currentXP);
		if (currentXP > 7)
			return true;
		else {
			player.sendMessage(ChatColor.RED + "You have less than 1 level! Please have at least 1 level before retrying.");
			return false;
		}
	}
	//ch
	private static void xpToBottles(Player player, int xpToRemove, int uL) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "Debug: XP to bottles fired.");
		int currentXP = player.getTotalExperience();
		if (currentXP > (currentXP - xpToRemove)) {
			int bottles = 0;
			float trackXP = currentXP;
			while (trackXP > (currentXP - xpToRemove)) {
				trackXP -= orbsPerBottle();
				bottles++;
			}
			if (inventorySpaceV2(player) >= bottles) {
				player.sendMessage(ChatColor.DARK_AQUA + "You deposited " + (xpToRemove) + " experience has been processed. Thank you for supporting Crystal Banks!");
				player.setLevel(player.getLevel() - uL);
				player.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
			} else {
				player.sendMessage(ChatColor.RED + "Due to lack inventory space your transaction was canceled! " + "Crystal Banks forgives your awful teasing, but please! Next time! Have enough room in your inventory!");
			}
		} else {
			player.sendMessage(ChatColor.RED + "Due to lack of XP your deposit was canceled! " + "Crystal Banks forgives the overdraft, and waives all fees. Next time please ensure you enter the correct amount!");
		}
	}
	//ch
	private static int inventorySpaceV2(Player player) {
			PlayerInventory pi = player.getInventory();
			ItemStack[] is = pi.getContents(); //returns array of ItemStacks[] from inv
			player.sendMessage(String.format("%s%d", "amount of indexes: ", is.length));
			int tally = 0;
			//ArrayList<Integer> emptySlots = new ArrayList<Integer>();
			for(int i = 0; i < pi.getSize(); i++) {
				if(is[i] == null || is[i].isSimilar(new ItemStack(Material.AIR))) {
					tally += 64;
				}
			}
			for(int i = 0; i < pi.getSize(); i++) { // pi = player inventory object
				if(is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
					if(is[i].isSimilar(new ItemStack(Material.EXP_BOTTLE))) {
						tally += 64-(is[i].getAmount());
					}
				}
			}
			return tally;
		}

	//Tier One is the formula for leveling up for levels 1-16
	public int formulaTierOne(int useLevel, int currentLevel){
		int targetLevel = currentLevel - useLevel;
		int amountToRemove = 0;
		if (targetLevel >= 0) {
			for(int i = 0; i < useLevel; i++){
				amountToRemove += (2*(useLevel-i) + 7);
			}
		}
		return amountToRemove; //Gives XP: Tier 1 Only
	}

	//Tier Two is the formula for leveling up for levels 17-31
	public int forumlaTierTwo(int useLevel, int currentLevel){
		int amountToRemove = 0;
		if(useLevel <= currentLevel - 16){
			int tempLevel = currentLevel - 16;
			for(int i = 0; i < useLevel; i++){
				amountToRemove += (5*(tempLevel-i) + 42);
			}
			return amountToRemove; //Gives XP: Tier 2 Only
		}
		else if (useLevel > currentLevel - 16){
			int tempLevel = currentLevel - 16;
			for(int i = 0; i < tempLevel; i++){
				amountToRemove += (5*(tempLevel-i) + 42);
			}
			int useTier1 = useLevel - tempLevel;
			tempLevel = 16;
			if (useTier1 >= 0) {
				for(int i = 0; i < useTier1; i++){
					amountToRemove += (2*(tempLevel-i) + 7);
				}
				return amountToRemove;
			}//Gives XP: Tier 1 and 2
		}
		return amountToRemove;
	}

	//Tier Three is the formula for leveling up for levels 32+
	public static int forumlaTierThree(int useLevel, int currentLevel){
		int targetLevel = currentLevel - useLevel;
		if (targetLevel >= 0){
			if(useLevel <= currentLevel - 31){//result level will be 31 or more
				int tempLevel = currentLevel - 31;
				int temp = 0;
				for(int i = 0; i < useLevel; i++){
					temp += 9*(tempLevel-i) + 121;
				}
				return temp;
				//returns tier 3 XP that needs to be removed(counted and bottled)!
			}
			else if (useLevel > currentLevel - 31){//result intent level will be less than 31
				int useOnTier3 = currentLevel - 31;//levels to use with formula 3
				int total = 0;
				for(int i = 0; i <= useOnTier3; i++){
					total += (9*(useOnTier3-i) + 121);//add tier 3 xp to bottle
				}
				useLevel -= useOnTier3;
				int tier2Levels = (currentLevel - useOnTier3 - 16);
				if (useLevel <= tier2Levels) {//leaving 16 or more levels left
					for(int i = 0; i < useLevel; i++){
						total += (5*(tier2Levels-i) + 42);
					}
					return total; //returns tier 2 and 3 XP that needs to be removed(counted and bottled)!
				}
				int useOnTier2 = useLevel - 16; //				int tier1Levels = (currentLevel - useOnTier3 - useOnTier2);
				if(useLevel > currentLevel - useOnTier3 - 16) {//enough useLevels to access tier 1 this does not imply that the other methods agree
					for(int i = 0; i < useOnTier2; i++){
						total += 5*(useOnTier2-i) + 42;
					}
					int useOnTier1 = useLevel - useOnTier2 - useOnTier3;
					int tempLevel = currentLevel - useOnTier2 -useOnTier3;
					for(int i = 0; i < useOnTier1; i++){
						total += (2*(tempLevel-i) + 7);
					}
					return total; //returns tier 1 (if any) 2 and 3 XP that needs to be removed(counted and bottled)!		
				}
			}
		}
		return 0;
	}
}

