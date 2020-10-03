package dekvall.danceparty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("danceparty")
public interface DancePartyConfig extends Config
{
	@ConfigItem(
			keyName = "workoutMode",
			name = "Workout Mode",
			description = "#1 OSRS fitness inspiration"
	)
	default boolean workoutMode()
	{
		return false;
	}

	@ConfigItem(
			keyName = "disableInPvp",
			name = "Disable in PvP",
			description = "Disable dance moves when entering dangerous situations :("
	)
	default boolean disableInPvp()
	{
		return false;
	}
}
