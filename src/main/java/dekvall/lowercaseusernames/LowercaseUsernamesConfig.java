package dekvall.lowercaseusernames;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("lowercaseusernames")
public interface LowercaseUsernamesConfig extends Config
{
	@ConfigItem(
		keyName = "uppercase",
		name = "Uppercase names",
		description = "Uppercase names instead of lowercasing them"
	)
	default boolean uppercase()
	{
		return false;
	}
}
