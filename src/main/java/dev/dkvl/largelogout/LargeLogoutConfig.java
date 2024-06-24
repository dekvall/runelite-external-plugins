package dev.dkvl.largelogout;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("largelogout")
public interface LargeLogoutConfig extends Config
{
	@ConfigItem(
		name = "World switcher",
		keyName = "enlargeWorldSwitcherLogout",
		description = "Enlarge the logout button in the world switcher menu"
	)
	default WorldSwitcherMode enlargeWorldSwitcherLogout()
	{
		return WorldSwitcherMode.ALWAYS;
	}
}
