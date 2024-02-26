package dekvall.fullscreen;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("Fullscreen")
public interface FullscreenConfig extends Config
{
	@ConfigItem(
		keyName = "fullscreenMode",
		name = "Mode",
		description = "Fullscreen mode",
		position = 0
	)
	default Mode fullscreenMode()
	{
		return Mode.EXCLUSIVE;
	}

	@ConfigItem(
		keyName = "fullscreenHotKey",
		name = "Hotkey",
		description = "Hotkey for toggling fullscreen mode",
		position = 1
	)
	default Keybind fullscreenHotKey()
	{
		return Keybind.NOT_SET;
	}
}
