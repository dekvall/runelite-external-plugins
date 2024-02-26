package dekvall.fullscreen;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Fullscreen")
public interface FullscreenConfig extends Config
{
	@ConfigItem(
		keyName = "fullscreenMode",
		name = "Mode",
		description = "Fullscreen mode"
	)
	default Mode FullscreenMode()
	{
		return Mode.EXCLUSIVE;
	}
}
