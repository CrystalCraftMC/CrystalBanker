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
		getLogger().info(ChatColor.GRAY + "CrystalBanker has been initialized!");
		try {
			File database = new File(getDataFolder(), "config.yml");
			if (!database.exists()) saveDefaultConfig();
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

		if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (isPlayer(sender) && sender.isOp()) reloadMethod(sender);
			else if (!isPlayer(sender)) reloadMethod(sender);
		}

		if (isPlayer(sender) && sender.hasPermission("crystalbanker.transaction")) {
			Player player = (Player) sender;
			Inventory inv = player.getInventory();

			if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && isInt(args[1], player) && (args[0].equalsIgnoreCase("deposit")) || args[0].equalsIgnoreCase("store")) {
				depositMethod(player, args[1]);
				return true;
			}
			else if (cmd.getName().equalsIgnoreCase("crystalbanker") && (args[0].equalsIgnoreCase("deposit")) || args[0].equalsIgnoreCase("store")){
				player.sendMessage(ChatColor.BLUE + "You can deposit " + player.getLevel() + " Levels. Note the number of levels you are allowed to deposit at a time depends on your inventory space.");
			}
			else if (cmd.getName().equalsIgnoreCase("crystalbanker") && args.length == 2 && isInt(args[1], player) && (args[0].equalsIgnoreCase("withdraw") || args[0].equalsIgnoreCase("use"))) {
					withdrawMethod(player, args[1], inv);
					return true;
				}
			else if(cmd.getName().equalsIgnoreCase("crystalbanker") && (args[0].equalsIgnoreCase("withdraw") || args[0].equalsIgnoreCase("use"))){
				player.sendMessage(ChatColor.BLUE + "You can withdraw up to " + countBottles(player) + " bottles.");
			}
			else{
				player.sendMessage(ChatColor.DARK_GRAY + "Usage: /CrystalBanker deposit [number of levels]");
				player.sendMessage(ChatColor.DARK_GRAY + "Usage: /CrystalBanker withdraw [number of bottles]");
			}
		}
		return false;
	}

	private boolean isPlayer(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
			return false;
		} else {
			return true;
		}
	}

	private static float orbsPerBottle() {
		Random rand = new Random();
		return (rand.nextFloat() * (11 - 3) + 3);
	}

	private static int toInt(Player player, String bottlesToUse) {
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

	private static void bottlesToXP(int amountToRemove, Player player) {
		int leftToRemove = amountToRemove;
		PlayerInventory pi = player.getInventory();
		ItemStack[] is = pi.getContents();
		for(int i = 0; i < pi.getSize(); i++) {
			if(is[i] != null && !is[i].isSimilar(new ItemStack(Material.AIR))) {
				if(is[i].isSimilar(new ItemStack(Material.EXP_BOTTLE))) {
					if (leftToRemove >= 0) {//if more to remove then...
						if (is[i].getAmount() < leftToRemove) {//if found
							leftToRemove -= is[i].getAmount();
							is[i].setType(null);
						} else if (is[i].getAmount() == leftToRemove) {
							is[i].setType(null);
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
		if (leftToRemove == 0) {//Add levels
			int xpOrbTotal = 0;
			for (int i = 0; i < amountToRemove; i++) {//translate everything into orbs
				int xpOrb = Math.round(orbsPerBottle());
				xpOrbTotal += xpOrb;
			}
			player.sendMessage(ChatColor.DARK_AQUA + "You withdrawal of " + amountToRemove + " bottles has been processed. Thank you for supporting Crystal Banks!");
			player.giveExp(xpOrbTotal);
		}
	}

	private boolean reloadMethod(CommandSender sender){
		this.reloadConfig();
		sender.sendMessage(ChatColor.GRAY + "Configuration reloaded!");
		return true;
	}

	private static boolean isInt(String bottlesToUse, Player p) {
		try {
			Integer.parseInt(bottlesToUse.trim());
		} catch (NumberFormatException nFE) {
			p.sendMessage(ChatColor.RED + "You need to input an integer number. (USE: 1, 2, etc) (NOT: 2.13, 2/3, etc)");
			return false;
		}
		return true;
	}
	//ch
	private boolean withdrawMethod(Player player, String args, Inventory inv){
		int amountToRemove = toInt(player, args);
		if (countBottles(player) < amountToRemove) {
			player.sendMessage(ChatColor.RED + "You don't have enough xp bottles ("+ countBottles(player) +") in your inventory to exchange the amount you entered (" + amountToRemove + ") .");
			return false;
		} else if (countBottles(player) >= amountToRemove) {
			bottlesToXP(amountToRemove, player);
			return true;
		}
		return false;
	}
	//ch
	private static int countBottles(Player player) {
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
	//ch
	private boolean depositMethod(Player player, String args){
		int storeLevels = toInt(player, args);
		int currentLevel = player.getLevel();
		if (countXP(player, storeLevels)) {
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
	private static boolean countXP(Player p, int amountToRemove) {
		if (p.getLevel() >= amountToRemove)
			return true;
		else {
			p.sendMessage(ChatColor.RED + "You have " + p.getLevel() + " Levels. You tried to deposit " + amountToRemove + " Levels.");
			p.sendMessage(ChatColor.RED + "Crystal Banks forgives the overdraft, and waives all fees. Next time please ensure you enter the correct amount!");
			return false;
		}
	}
	//ch
	private static boolean xpToBottles(Player player, int xpToRemove, int uL) {
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
				return true;
			} else {
				player.sendMessage(ChatColor.RED + "Due to lack of inventory space your transaction was canceled!"); 
				player.sendMessage(ChatColor.RED + "Crystal Banks forgives you, but next time make sure to have enough room in your inventory!");
				return false;
			}
		} else {
			return false;
		}
	}
	//ch
	private static int inventorySpaceV2(Player player) {
		PlayerInventory pi = player.getInventory();
		ItemStack[] is = pi.getContents(); //returns array of ItemStacks[] from inv
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
	private int formulaTierOne(int useLevel, int currentLevel){
		int amountToRemove = 0;
		for(int i = 0; i < useLevel; i++){
			amountToRemove += (2*(currentLevel-i) + 7);
		}
		return amountToRemove; //Gives XP: Tier 1 Only
	}
	//Tier Two is the formula for leveling up for levels 17-31
	private int forumlaTierTwo(int useLevel, int currentLevel){
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
	private static int forumlaTierThree(int useLevel, int currentLevel){
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
				if ((useLevel - useOnTier3) <= tier2Levels) {//leaving 16 or more levels left
					for(int i = 0; i < (useLevel - useOnTier3); i++){
						total += (5*(tier2Levels-i) + 42);
					}
					return total; //returns tier 2 and 3 XP that needs to be removed(counted and bottled)!
				}
				int useOnTier2 = (useLevel - useOnTier3) - 16; //int tier1Levels = (currentLevel - useOnTier3 - useOnTier2);
				if((useLevel - useOnTier3) > currentLevel - useOnTier3 - 16) {//enough useLevels to access tier 1 this does not imply that the other methods agree
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
