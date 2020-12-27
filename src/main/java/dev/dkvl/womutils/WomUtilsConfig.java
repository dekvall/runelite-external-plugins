package dev.dkvl.womutils;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("womutils")
public interface WomUtilsConfig extends Config
{
	@ConfigItem(
		keyName = "updateIgnored",
		name = "Update Ignored",
		description = "Send name updates for people on your ignore list"
	)
	default boolean updateIgnored()
	{
		return true;
	}
}
