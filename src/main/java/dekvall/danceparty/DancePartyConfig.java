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
}
