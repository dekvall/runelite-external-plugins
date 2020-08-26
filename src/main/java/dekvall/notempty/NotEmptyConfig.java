package dekvall.notempty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(NotEmptyPlugin.CONFIG_GROUP)
public interface NotEmptyConfig extends Config
{
	@ConfigItem(
		keyName = "removedEntries",
		name = "Removed entries",
		description = "CSV of entries that should be removed, you cannot remove the first option of items"
	)
	default String removedEntries()
	{
		return "Empty";
	}
}
