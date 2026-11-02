package dekvall.fullscreen;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(FullscreenConfig.GROUP)
public interface FullscreenConfig extends Config
{
	String GROUP = "Fullscreen";

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

	@ConfigItem(
		keyName = "exclusiveConfirmed",
		name = "",
		description = "Whether the user has confirmed that exclusive fullscreen works on their setup",
		hidden = true
	)
	default boolean exclusiveConfirmed()
	{
		return false;
	}
}
