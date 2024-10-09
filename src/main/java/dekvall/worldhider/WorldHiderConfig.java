/*
 * Copyright (c) 2020, dekvall <https://github.com/dekvall>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dekvall.worldhider;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("worldhider")
public interface WorldHiderConfig extends Config
{
	@ConfigItem(
		keyName = "hideFlags",
		name = "Hide Flags",
		description = "Hides Flag for each world by making them all the same",
		position = 0
	)
	default boolean hideFlags()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideFavorites",
		name = "Hide Favorites",
		description = "Hides Favorite worlds",
		position = 1
	)
	default boolean hideFavorites()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideScrollbar",
		name = "Hide Scrollbar",
		description = "Hides Scrollbar",
		position = 2
	)
	default boolean hideScrollbar()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideList",
		name = "Hide List",
		description = "Hides value in world hopper list",
		position = 3
	)
	default boolean hideList()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideListConfig",
		name = "Hide World Panel",
		description = "Hides the worlds in the configuring panel",
		position = 4
	)
	default boolean hideConfigurationPanel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "massHide",
		name = "Mass hide",
		description = "Hide world of friends and clanmates",
		position = 5
	)
	default boolean massHide()
	{
		return false;
	}
}
