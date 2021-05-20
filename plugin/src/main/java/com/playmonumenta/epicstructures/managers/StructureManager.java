package com.playmonumenta.epicstructures.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.playmonumenta.epicstructures.StructurePlugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class StructureManager {
	private final ConcurrentSkipListMap<String, BlockArrayClipboard> mSchematics = new ConcurrentSkipListMap<String, BlockArrayClipboard>();
	private final StructurePlugin mPlugin;
	private final org.bukkit.World mWorld;
	private final ClipboardFormat format;
	private final boolean mUseStructureCache;


	public StructureManager(StructurePlugin plugin, org.bukkit.World world, boolean useStructureCache) {
		mPlugin = plugin;
		mWorld = world;
		format = ClipboardFormats.findByAlias("sponge");
		mUseStructureCache = useStructureCache;
	}

	/* It *should* be safe to call this async */
	public BlockArrayClipboard loadSchematic(String baseName) throws Exception {
		BlockArrayClipboard clipboard = null;
		if (mUseStructureCache) {
			clipboard = mSchematics.get(baseName);
		}
		if (clipboard == null) {
			// Schematic not already loaded - need to read it from disk and load it into RAM

			File file = CommandUtils.getAndValidateSchematicPath(mPlugin, baseName, true);

			Clipboard newClip = format.load(file);
			if (newClip instanceof BlockArrayClipboard) {
				clipboard = (BlockArrayClipboard)newClip;
			} else if (newClip instanceof DiskOptimizedClipboard) {
				clipboard = ((DiskOptimizedClipboard)newClip).toClipboard();
			} else {
				throw new Exception("Loaded unknown clipboard type: " + newClip.getClass().toString());
			}

			if (mUseStructureCache) {
				// Cache the schematic for fast access later
				mSchematics.put(baseName, clipboard);
			}
		}

		return clipboard;
	}

	public void saveSchematic(String baseName, BlockVector3 minpos, BlockVector3 maxpos, Runnable whenDone) throws Exception {
		saveSchematic(baseName, minpos, maxpos, whenDone, mWorld);
	}

	public void saveSchematic(String baseName, BlockVector3 minpos, BlockVector3 maxpos, Runnable whenDone, org.bukkit.World bukkitWorld) throws Exception {
		File file = CommandUtils.getAndValidateSchematicPath(mPlugin, baseName, false);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		World world = new BukkitWorld(bukkitWorld);
		CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);
		Clipboard clipboard = new BlockArrayClipboard(cReg);
		clipboard.setOrigin(cReg.getMinimumPoint());

		/* Copy the region (including entities and biomes) into the clipboard object */
		EditSession extent = new EditSessionBuilder(world)
			.autoQueue(true)
			.fastmode(true)
			.combineStages(true)
			.changeSetNull()
			.checkMemory(false)
			.allowedRegionsEverywhere()
			.limitUnlimited()
			.build();
		ForwardExtentCopy copy = new ForwardExtentCopy(extent, cReg, clipboard, cReg.getMinimumPoint());
		copy.setCopyingEntities(true);
		copy.setCopyingBiomes(false); // TODO: Re enable when FAWE supports this again
		Operations.completeLegacy(copy);

		format.write(new FileOutputStream(file), clipboard);

		if (mUseStructureCache) {
			// Re-load the schematic from disk into the cache
			mSchematics.remove(baseName);
			loadSchematic(baseName);
		}

		/*
		 * Cache has been updated - but the respawning structures need to be updated too
		 * to get this new cached copy
		 */
		mPlugin.saveConfig();
		mPlugin.reloadConfig();
	}

	public void createWorld(Player player) {
		TaskManager.IMP.async(() -> {
			WorldCreator worldCreator = new WorldCreator(player.getName() + "_Async" + UUID.randomUUID().toString());
			worldCreator.environment(org.bukkit.World.Environment.NORMAL);
			worldCreator.generateStructures(false);
			worldCreator.type(WorldType.FLAT);
			// Create or load a world async with the provided WorldCreator settings
			AsyncWorld world = AsyncWorld.create(worldCreator);
			// AsyncWorld world = AsyncWorld.wrap(bukkitWorld); // Or wrap existing world
			Block block = world.getBlockAt(0, 10, 0);
			block.setType(Material.BEDROCK);
			// When you are done
			world.commit();
		});
	}
}
