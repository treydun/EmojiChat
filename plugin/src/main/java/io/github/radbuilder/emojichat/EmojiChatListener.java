package io.github.radbuilder.emojichat;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * EmojiChat listener class.
 *
 * @author RadBuilder
 * @since 1.0
 */
class EmojiChatListener implements Listener {
	/**
	 * EmojiChat main class instance.
	 */
	private EmojiChat plugin;
	
	/**
	 * Creates the EmojiChat listener class with the main class instance.
	 *
	 * @param plugin The EmojiChat main class instance.
	 */
	EmojiChatListener(EmojiChat plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		// Send the player an alert if there's an update available
		if (player.hasPermission("emojichat.updates") && plugin.updateChecker.updateAvailable) {
			player.sendMessage(ChatColor.AQUA + "An update for EmojiChat is available.");
			player.sendMessage(ChatColor.AQUA + "Current version: " + ChatColor.GOLD + plugin.updateChecker.currentVersion
					+ ChatColor.AQUA + ". Latest version: " + ChatColor.GOLD + plugin.updateChecker.latestVersion + ChatColor.AQUA + ".");
		}
		
		// Send the player the resource pack
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (player.hasPermission("emojichat.see")) { // If the player can see emojis
				try {
					player.setResourcePack(plugin.PACK_URL, plugin.PACK_SHA1); // If the Spigot version supports loading cached versions
				} catch (Exception | NoSuchMethodError e) {
					player.setResourcePack(plugin.PACK_URL); // If the Spigot version doesn't support loading cached versions
				}
			}
		}, 20L); // Give time for the player to join
	}
	
	@EventHandler
	void onChat(AsyncPlayerChatEvent event) {
		if (!event.getPlayer().hasPermission("emojichat.use"))
			return; // Don't do anything if they don't have permission
		
		String message = event.getMessage();
		
		// Replaces shorthand ("shortcuts" in config) with correct emoji shortcuts
		for (String key : plugin.getEmojiHandler().getShortcuts().keySet()) {
			plugin.getMetricsHandler().addShortcutUsed(StringUtils.countMatches(message, key));
			message = message.replace(key, plugin.getEmojiHandler().getShortcuts().get(key));
		}
		
		// Replace shortcuts with emojis
		for (String key : plugin.getEmojiHandler().getEmojis().keySet()) {
			plugin.getMetricsHandler().addEmojiUsed(StringUtils.countMatches(message, key));
			message = message.replace(key, (plugin.getEmojiHandler().fixColoring() ? ChatColor.RESET : "") + plugin.getEmojiHandler().getEmojis().get(key));
		}
		
		if (plugin.getEmojiHandler().verifyDisabledList()) { // If the message should be checked for disabled characters
			for (String disabledCharacter : plugin.getEmojiHandler().getDisabledCharacters()) {
				if (message.contains(disabledCharacter)) { // Message contains a disabled character
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "Oops! You can't use disabled emoji characters!");
					return;
				}
			}
		}
		
		event.setMessage(message);
	}
	
	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getTitle().contains("Emoji List")) {
			event.setCancelled(true);
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.DIAMOND && event.getCurrentItem().hasItemMeta()
					&& event.getCurrentItem().getItemMeta().hasDisplayName()) { // Make sure the item clicked is a page change item
				try {
					int currentPage = Integer.parseInt(event.getInventory().getTitle().split(" ")[3]) - 1; // Get the page number from the title
					
					if (event.getCurrentItem().getItemMeta().getDisplayName().contains("<-")) { // Back button
						event.getWhoClicked().openInventory(plugin.emojiChatGui.getInventory(currentPage - 1));
					} else { // Next button
						event.getWhoClicked().openInventory(plugin.emojiChatGui.getInventory(currentPage + 1));
					}
				} catch (Exception e) { // Something happened, not sure what, so just reset their page to 0
					event.getWhoClicked().openInventory(plugin.emojiChatGui.getInventory(0));
				}
			}
		}
	}
}
