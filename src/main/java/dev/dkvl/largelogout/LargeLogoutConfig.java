package dev.dkvl.largelogout;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LargeLogoutPlugin.CONFIG_GROUP)
public interface LargeLogoutConfig extends Config
{
	@ConfigItem(
		keyName = "resizeWorldSwitcherLogout",
		name = "Resize world switcher logout",
		description = "Resize the world switcher logout button"
	)
	default boolean resizeWorldSwitcherLogout()
	{
		return false;
	}
}
