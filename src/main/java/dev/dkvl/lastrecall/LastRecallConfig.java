package dev.dkvl.lastrecall;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("lastrecall")
public interface LastRecallConfig extends Config
{
	@ConfigItem(
		keyName = "lastrecallLocation",
		name = "",
		description = "",
		hidden = true
	)
	default WorldPoint location()
	{
		return null;
	}

	@ConfigItem(
		keyName = "lastrecallLocation",
		name = "",
		description = ""
	)
	void location(WorldPoint location);
}
