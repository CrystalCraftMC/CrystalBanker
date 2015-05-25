/*
 * Copyright 2015 CrystalCraftMC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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

public class CrystalBanker extends JavaPlugin{

	public void onEnable() {
		getLogger().info("CrystalBanker has been initialized!");
		try {
			File database = new File(getDataFolder(), "config.yml");
			if (!database.exists()) 
				saveDefaultConfig();
		} catch (Exception e1) {
			getLogger().info("CrystalBanker _failed_ to initialize.");
			e1.printStackTrace();
		}
	}

	public void onDisable() {
		getLogger().info("CrystalBanker has been stopped.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		} else if (cmd.getName().equalsIgnoreCase("crystalbanker")) {
			Player p = (Player) sender;
		

			if (args.length > 0 && args.length < 3) {
				if (args[0].equalsIgnoreCase("reload") && p.hasPermission("crystalbanker.reload")) {
					this.reloadConfig();
					sender.sendMessage(ChatColor.GRAY + "The configuration was reloaded!");
				} else if (args[0].equalsIgnoreCase("zero") && p.hasPermission("crystalbanker.zero")) {
					int amountToRemove = countBottles(p);
					withdrawMethod(p, amountToRemove, p.getInventory());
					return true;
				} else if (args[0].equalsIgnoreCase("cash") && p.hasPermission("crystalbanker.cash")) {//TODO tracker
					cash(p);
					return true;
				} else if (args[0].equalsIgnoreCase("deposit") && p.hasPermission("crystalbanker.deposit")) {
					if (args.length == 2 && isInt(args[1], p)) {
						int storeLevels = toInt(p, args[1]);
						depositMethod(p, storeLevels);
						return true;
					} else {
						p.sendMessage(net.md_5.bungee.api.ChatColor.RED + "You can deposit " + p.getLevel() + " levels.\n"
								+ net.md_5.bungee.api.ChatColor.GRAY +"Note: The number of levels you are allowed to deposit at a time depends on having enough inventory space.");
						sender.sendMessage(ChatColor.GOLD + "Usage: /crystalbanker deposit [number of levels]");
						return false;
					}
				} else if (args[0].equalsIgnoreCase("withdraw") && p.hasPermission("crystalbanker.withdraw")) {
					if (args.length == 2 && isInt(args[1], p)) {
						int amountToRemove = toInt(p, args[1]);
						withdrawMethod(p, amountToRemove, p.getInventory());
						return true;
					} else {
						p.sendMessage(net.md_5.bungee.api.ChatColor.BLUE + "You can withdraw up to " + countBottles(p) + " bottles.");
						sender.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "Usage: /crystalbanker withdraw [number of bottles]");
						return true;
					}
				} else return false;
			}
		}
		sender.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "CrystalBanker Help:\n" +
				net.md_5.bungee.api.ChatColor.BLUE + "/crystalbanker deposit [number of levels]\n" +
				"/crystalbanker withdraw [number of bottles]\n" +
				"/crystalbanker zero " + net.md_5.bungee.api.ChatColor.GRAY + "(to cash in all XP bottles)");
		return true;
	}


	/**
	 * Determines how many XP orbs per bottle should be produced.
	 * @return double
	 */
	private double orbsPerBottle() {
		Random rand = new Random();
		return ((rand.nextFloat()*(11-3)) + 3);
	}

	private boolean isInt(String bottlesToUse, CommandSender sender) {
		try {
			Integer.parseInt(bottlesToUse.trim());
		} catch (NumberFormatException nFE) {
			sender.sendMessage(ChatColor.RED + "You need to input an integer number.");
			return false;
		}
		return true;
	}

	private int toInt(Player player, String bottlesToUse) {
		int number;
		try {
			String temp = bottlesToUse.trim();
			number = Integer.parseInt(temp);
		} catch (NumberFormatException nFE) {
			player.sendMessage(ChatColor.RED + "There was a problem with the value you entered. Try again.");
			return 0;
		}
		return number;
	}

	private boolean bottlesToXP(int amountToRemove, Player player) {
		int leftToRemove = amountToRemove;
		PlayerInventory pi = player.getInventory();
		ItemStack[] is = pi.getContents();
		for(int i = 0; i < pi.getSize(); i++) {
			if(is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
				if(is[i].isSimilar(new ItemStack(Material.EXP_BOTTLE))) {
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
			player.sendMessage(ChatColor.DARK_AQUA + "You withdrawal of " + amountToRemove + " bottles has been processed. Thank you for your business!");
			return true;
		}
		return false;
	}

	private boolean withdrawMethod(Player player, int amountToRemove, Inventory inv){
		if (countBottles(player) < amountToRemove) {
			player.sendMessage(ChatColor.RED + "You don't have enough XP bottles ("+ countBottles(player) +") in your inventory to exchange the amount you entered (" + amountToRemove + ").");
			return true;
		} else if (countBottles(player) >= amountToRemove) {
			bottlesToXP(amountToRemove, player);
			return true;
		}
		return false;
	}

	private int countBottles(Player player) {
		PlayerInventory pi = player.getInventory();
		ItemStack[] is = pi.getContents();
		int tally = 0;
		for(int i = 0; i < pi.getSize(); i++) {
			if(is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
				if(is[i].isSimilar(new ItemStack(Material.EXP_BOTTLE))) {
					tally += is[i].getAmount();
				}
			}
		}
		return tally;
	}

	//TODO REBUILD ALL METHODS USING LEVELS!
	private boolean depositMethod(Player player, int storeLevels){
		int currentLevel = player.getLevel();
		if (countXP(player, storeLevels)) {
			if (currentLevel <= 16) {
				xpToBottles(player, formulaTierOne(storeLevels, currentLevel), storeLevels);
				return true;
			} else if (currentLevel <= 31) {
				xpToBottles(player, formulaTierTwo(storeLevels, currentLevel), storeLevels);
				return true;
			} else if (currentLevel >= 32) {
				xpToBottles(player, formulaTierThree(storeLevels, currentLevel), storeLevels);
				return true;
			}
		}
		return false;
	}

	//TODO THIS MAY NEED TO BE DONE IF WE NO LONGER USE FULL LEVELS TO ACCESS ITEMS
	private boolean countXP(Player p, int amountToRemove) {
		if (p.getLevel() >= amountToRemove)
			return true;
		else {
			p.sendMessage(ChatColor.RED + "You have " + p.getLevel() + " levels. You tried to deposit " + amountToRemove + " levels.");
			p.sendMessage(ChatColor.RED + "CrystalBanker forgives the overdraft and waives all fees. Next time, please ensure you enter the correct amount!");
			return false;
		}
	}

	private void cash(Player p) {//TODO Build a fill inventory with EXP bottles METHODS
		int currentXP = p.getTotalExperience();
		//more EXP than zero
		if (currentXP > 0) {
			int bottles = 0;
			float trackXP = currentXP;
			int maxBottleAmount = inventorySpaceV2(p);

			while (bottles < maxBottleAmount && (trackXP > 0)) {
				trackXP -= orbsPerBottle();
				bottles++;
			}
			if(trackXP < 0) {
				trackXP = 0;
				bottles--;
			}
			if (bottles > maxBottleAmount) {
				bottles--;
				p.sendMessage(ChatColor.DARK_AQUA + "Your complete deposit of " + currentXP + " orbs has been processed. Thank you for your business!");
				p.sendMessage(ChatColor.DARK_AQUA + "Balance Statement: You have " + trackXP + " orbs remaining, and gained " + bottles + " bottles.");
				p.setExp((float)0);
				p.setLevel(0);
				p.setTotalExperience(0);
				p.giveExp(Math.round(trackXP));
				p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
				p.sendMessage(ChatColor.DARK_AQUA + "Your complete deposit of " + currentXP + " orbs has been processed. Thank you for your business!");
				p.sendMessage(ChatColor.DARK_AQUA + "Balance Statement: You have " + trackXP + " of "+ p.getTotalExperience()  + " orbs remaining, and gained " + bottles + " bottles.");
				return;
			} else {
				p.setExp((float)0);
				p.setLevel(0);
				p.setTotalExperience(0);
				p.giveExp(Math.round(trackXP));
				p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
				p.sendMessage(ChatColor.DARK_AQUA + "Your deposit of " + (currentXP-trackXP) + " orbs has been processed. Thank you for your business!");
				p.sendMessage(ChatColor.DARK_AQUA + "Balance Statement: You have " + trackXP + " of "+ p.getTotalExperience()  + " orbs remaining, and gained " + bottles + " bottles.");
				return;
			}
		} else {
			p.sendMessage(ChatColor.GRAY + "You don't have any experience points.");
		}
	}

	private void xpToBottles(Player player, int xpToRemove, int uL) {
		int currentXP = player.getTotalExperience();
		if (currentXP > (currentXP - xpToRemove)) {
			int bottles = 0;
			float trackXP = currentXP;
			while (trackXP > (currentXP - xpToRemove)) {
				trackXP -= orbsPerBottle();
				bottles++;
			}
			if(trackXP < 0) {
				trackXP = 0;
				bottles--;
			}
			if (inventorySpaceV2(player) >= bottles) {
				player.sendMessage(ChatColor.DARK_AQUA + "Your deposit of " + (xpToRemove) + " orbs has been processed. Thank you for your business!");
				//!!!!FLAG NOTE NEW CHANGE TEST THROUGHLY BEFORE PUBLISHING!!!!
				
				player.setExp((float) 0);
				player.setLevel(0);
				player.setTotalExperience(0);
				player.giveExp((int) trackXP);
				player.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
			} else {
				player.sendMessage(ChatColor.RED + "Due to lack of inventory space, your transaction was canceled!");
			}
		} 
	}

	private int inventorySpaceV2(Player player) {
		PlayerInventory pi = player.getInventory();
		ItemStack[] is = pi.getContents(); //returns array of ItemStacks[] from inv
		int tally = 0;
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

	//DO NOT TOUCH THIS COMMENT: FOR LEVELS 1, 2, 3, ..., 14, 15, 16? TEST
	private static int formulaTierOne(int useLevel, int currentLevel){
		int amountToRemove = 0;
		for(int i = 0; i < useLevel; i++){
			amountToRemove += (2*(currentLevel-i) + 5);
		}
		return amountToRemove; //Gives XP: Tier 1 Only
	}
	//DO NOT TOUCH THIS COMMENT: FOR LEVELS 1, 2, ..., 16?, 17, 18, ..., 30, 31?, 32? TEST
	private static int formulaTierTwo(int useLevel, int currentLevel){
		int amountToRemove = 0;
		if(useLevel <= currentLevel - 16){
			int tempLevel = currentLevel - 16;
			for(int i = 0; i < useLevel; i++){
				amountToRemove += (5*(tempLevel-i) + 37);
			}
			return amountToRemove; //Gives XP: Tier 2 Only
		}
		else if (useLevel > currentLevel - 16){
			int tempLevel = currentLevel - 16;
			for(int i = 0; i < tempLevel; i++){
				amountToRemove += (5*(tempLevel-i) + 37);
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
	private int formulaTierThree(int useLevel, int currentLevel){
		int targetLevel = currentLevel - useLevel;
		if (targetLevel >= 0){
			if(useLevel <= currentLevel - 31){//result level will be 31 or more
				int temp = 0;
				for(int i = 0; i < useLevel; i++){
					temp += 9*((currentLevel-31)-i) + 112;
				}
				return temp;
			}
			else if (useLevel > currentLevel - 31){//resultant level will be less than 31
				int useOnTier3 = currentLevel - 31;//levels to use with formula 3
				int total = 0;
				for(int i = 1; i <= useOnTier3; i++){
					total += (9*(useOnTier3-i) + 121);//add tier 3 xp to bottle
				}
				total += formulaTierTwo((useLevel - useOnTier3), (currentLevel - useOnTier3));
				return total;
			}
		}
		return 0;
	}
}
