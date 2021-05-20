package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;
import java.util.logging.Level;

import com.playmonumenta.epicstructures.StructurePlugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;
import com.playmonumenta.epicstructures.utils.MessagingUtils;
import com.playmonumenta.epicstructures.utils.StructureUtils;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;

import dev.jorel.commandapi.arguments.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class LoadStructure {
	public static void register(StructurePlugin plugin) {
		final String command = "loadstructure";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("path", new TextArgument());
		arguments.put("position", new LocationArgument());
		arguments.put("rotation", new FloatArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				load(sender, plugin, (String)args[0], (Location)args[1], false, (Float) args[2]);
			})
			.register();

		arguments.put("includeEntities", new BooleanArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				load(sender, plugin, (String)args[0], (Location)args[1], (Boolean)args[3], (Float) args[2]);
			})
			.register();

		arguments.put("cleanLight", new BooleanArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				load(sender, plugin, (String)args[0], (Location)args[1], (Boolean)args[3], (Boolean)args[4], (Float) args[2]);
			})
			.register();
	}

	public static void load(CommandSender sender, StructurePlugin plugin, String path, Location loadLoc, boolean includeEntities) throws WrapperCommandSyntaxException {
		load(sender, plugin, path, loadLoc, includeEntities, 0);
	}

	public static void load(CommandSender sender, StructurePlugin plugin, String path, Location loadLoc, boolean includeEntities, float rotation) throws WrapperCommandSyntaxException {
		load(sender, plugin, path, loadLoc, includeEntities, rotation, null);
	}

	public static void load(CommandSender sender, StructurePlugin plugin, String path, Location loadLoc, boolean includeEntities, boolean cleanLight, float rotation) throws WrapperCommandSyntaxException {
		load(sender, plugin, path, loadLoc, includeEntities, rotation, cleanLight, null);
	}

	public static void load(CommandSender sender, StructurePlugin plugin,
							String path, Location loadLoc, boolean includeEntities, float rotation, Runnable runnable) throws WrapperCommandSyntaxException {
		load(sender, plugin, path, loadLoc, includeEntities, rotation, true, runnable);
	}

	public static void load(CommandSender sender, StructurePlugin plugin,
							String path, Location loadLoc, boolean includeEntities, float rotation, boolean cleanLight, Runnable runnable) throws WrapperCommandSyntaxException {
		CommandUtils.getAndValidateSchematicPath(plugin, path, true);

		if (plugin.mStructureManager == null) {
			return;
		}

		BlockVector3 loadPos = BlockVector3.at(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());

		// Load the schematic asynchronously (this might access the disk!)
		// Then switch back to the main thread to initiate pasting
		new BukkitRunnable() {
			@Override
			public void run() {
				final BlockArrayClipboard clipboard;

				try {
					clipboard = plugin.mStructureManager.loadSchematic(path);
				} catch (Exception e) {
					plugin.asyncLog(Level.SEVERE, "Failed to load schematic '" + path + "'", e);

					if (sender != null) {
						sender.sendMessage(ChatColor.RED + "Failed to load structure");
						MessagingUtils.sendStackTrace(sender, e);
					}
					return;
				}

				/* Once the schematic is loaded, this task is used to paste it */
				new BukkitRunnable() {
					@Override
					public void run() {
						StructureUtils.paste(plugin, clipboard, loadLoc.getWorld(), loadPos, includeEntities, rotation, cleanLight, runnable);

						if (sender != null) {
							sender.sendMessage("Loaded structure '" + path + "' at " + loadPos);
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
}
