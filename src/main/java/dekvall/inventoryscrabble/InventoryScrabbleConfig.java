package dekvall.inventoryscrabble;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(InventoryScrabblePlugin.CONFIG_GROUP)
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

	@ConfigItem(
		keyName = "hardMode",
		name = "Hard mode",
		description = "Apply limitations when interacting with objects"
	)
	default boolean hardMode()
	{
		return false;
	}
}
