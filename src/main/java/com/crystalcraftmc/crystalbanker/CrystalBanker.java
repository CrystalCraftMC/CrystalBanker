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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

public class CrystalBanker extends JavaPlugin{

	public void onEnable() {
		getLogger().info(ChatColor.GRAY + "CrystalBanker has been initialized!");
		try {
			File database = new File(getDataFolder(), "config.yml");
			if (!database.exists()) 
				saveDefaultConfig();
		} catch (Exception e1) {
			getLogger().info(ChatColor.DARK_RED + "CrystalBanker _failed_ to initialize.");
			e1.printStackTrace();
		}
	}

	public void onDisable() {
		getLogger().info(ChatColor.GRAY + "CrystalBanker has been stopped by the server.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("crystalbanker") && sender.isOp() && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			reloadMethod(sender);
			return true;
		}
		if (isPlayer(sender) && sender.hasPermission("crystalbanker.transaction") && cmd.getName().equalsIgnoreCase("crystalbanker")) {
			Player player = (Player) sender;
			if (args.length == 2) {
				if ((args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("store")) && isInt(args[1], player)) {
					int storeLevels = toInt(player, args[1]);
					depositMethod(player, storeLevels);
					return true;
				}
				else if ((args[0].equalsIgnoreCase("withdraw") || args[0].equalsIgnoreCase("use")) && isInt(args[1], sender)) {
					int amountToRemove = toInt(player, args[1]);
					withdrawMethod(player, amountToRemove);
					return true;
				}
			}
			if(args.length == 1) {
				if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("store")) {
					player.sendMessage(ChatColor.BLUE + "You can deposit " + player.getLevel() + " Levels. Note the number of levels you are allowed to deposit at a time depends on your inventory space.");
					return true;
				}
				else if (args[0].equalsIgnoreCase("withdraw") || args[0].equalsIgnoreCase("use")) {
					player.sendMessage(ChatColor.BLUE + "You can withdraw up to " + countBottles(player) + " bottles.");
					return true;
				}
			}
		}
		helpMsg(sender);
		return false;
	}

	private void helpMsg(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "Usage: /CrystalBanker deposit [levels]");
		sender.sendMessage(ChatColor.GOLD + "Usage: /CrystalBanker withdraw [bottles]");
	}

	private boolean isPlayer(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
			return false;
		} else 
			return true;
	}

	private int orbsPerBottle() {
		Random rand = new Random();
		return (rand.nextInt(11 - 3) + 3);
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
				int xpOrb = orbsPerBottle();
				xpOrbTotal += xpOrb;
			}
			player.giveExp(xpOrbTotal);
			player.sendMessage(ChatColor.DARK_AQUA + "You withdrawal of " + amountToRemove + " bottles has been processed. Thank you for supporting Crystal Banks!");
			return true;
		}
		return false;
	}

	private void reloadMethod(CommandSender sender){
		this.reloadConfig();
		sender.sendMessage(ChatColor.GRAY + "Configuration reloaded!");
	}

	private boolean withdrawMethod(Player player, int amountToRemove){
		if (countBottles(player) < amountToRemove) {
			player.sendMessage(ChatColor.RED + "You don't have enough xp bottles ("+ countBottles(player) +") in your inventory to exchange the amount you entered (" + amountToRemove + ") .");
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

	private boolean countXP(Player p, int amountToRemove) {
		if (p.getLevel() >= amountToRemove)
			return true;
		else {
			p.sendMessage(ChatColor.RED + "You have " + p.getLevel() + " Levels. You tried to deposit " + amountToRemove + " Levels.");
			p.sendMessage(ChatColor.RED + "Crystal Banks forgives the overdraft, and waives all fees. Next time please ensure you enter the correct amount!");
			return false;
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
			if (inventorySpaceV2(player) >= bottles) {
				player.sendMessage(ChatColor.DARK_AQUA + "You deposited " + (xpToRemove) + " experience has been processed. Thank you for supporting Crystal Banks!");
				player.setLevel(player.getLevel() - uL);
				player.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, bottles));
			} else {
				player.sendMessage(ChatColor.RED + "Due to lack of inventory space your transaction was canceled!"); 
				player.sendMessage(ChatColor.RED + "Crystal Banks forgives you, but next time make sure to have enough room in your inventory!");
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
	//Tier One is the formula for leveling up for levels 16
	private int formulaTierOne(int useLevel, int currentLevel){
		int amountToRemove = 0;
		for(int i = 0; i < useLevel; i++){
			amountToRemove += (2*(currentLevel-i) + 5);
		}
		return amountToRemove; //Gives XP: Tier 1 Only
	}
	//Tier Two is the formula for leveling up for levels 17-31
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
			tempLevel = 16;
			if (useTier1 >= 0) {
				for(int i = 0; i < useTier1; i++){
					amountToRemove += (2*(tempLevel-i) + 5);
				}
				return amountToRemove;
			}//Gives XP: Tier 1 and 2
		}
		return amountToRemove;
	}
	//Tier Three is the formula for leveling up for levels 32+
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
