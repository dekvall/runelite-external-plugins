package dekvall.inventoryscrabble;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("inventoryscrabble")
public interface InventoryScrabbleConfig extends Config
{
	@ConfigItem(
		keyName = "wornItems",
		name = "Allow worn items",
		description = "Allow worn items to count when solving the puzzle"
	)
	default boolean wornItems()
	{
		return false;
	}
}
