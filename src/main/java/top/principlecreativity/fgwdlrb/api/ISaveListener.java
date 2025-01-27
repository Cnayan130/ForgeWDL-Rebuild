/*
 * This file is part of the World Downloader API.
 * http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2520465
 *
 * Copyright (c) 2017 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see http://stopmodreposts.org/
 *
 * You are free to include the World Downloader API within your own mods, as
 * permitted via the MMPLv2.
 */
package top.principlecreativity.fgwdlrb.api;

import net.minecraft.world.storage.SaveHandler;

import java.io.File;

/**
 * Interface for {@link IWDLMod}s that want to save additional data with the
 * world.
 *
 * A new progress bar step is created for each implementation.
 */
public interface ISaveListener extends IWDLMod {
	/**
	 * Called after all of the chunks in the world have been saved.
	 *
	 * @param worldFolder
	 *            The base file for the world, as returned by
	 *            {@link SaveHandler#getWorldDirectory()};
	 */
	public abstract void afterChunksSaved(File worldFolder);
}
