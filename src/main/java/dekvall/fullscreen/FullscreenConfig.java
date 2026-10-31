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
		description = "Select a mode to switch immediately; Windowed exits fullscreen",
		position = 0
	)
	default Mode fullscreenMode()
	{
		return Mode.EXCLUSIVE;
	}

	@ConfigItem(
		keyName = "fullscreenHotKey",
		name = "Hotkey",
		description = "Toggle fullscreen, including on the login screen",
		position = 2
	)
	default Keybind fullscreenHotKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "fullscreenOnStartup",
		name = "Fullscreen on startup",
		description = "Enter fullscreen when this plugin starts, using the last fullscreen mode if Mode is Windowed",
		position = 3
	)
	default boolean fullscreenOnStartup()
	{
		return false;
	}

	@ConfigItem(
		keyName = "monitorTarget",
		name = "Monitor",
		description = "Secondary uses the first non-primary display. For a specific display, right-click the fullscreen toolbar button and choose 'Choose fullscreen monitor'",
		position = 1
	)
	default MonitorTarget monitorTarget()
	{
		return MonitorTarget.CURRENT;
	}

	@ConfigItem(keyName = "lastFullscreenMode", name = "Last fullscreen mode",
		description = "Fullscreen mode used by the hotkey after selecting Windowed", hidden = true)
	default Mode lastFullscreenMode()
	{
		return Mode.EXCLUSIVE;
	}

	@ConfigItem(keyName = "preferredMonitor", name = "Preferred monitor", description = "Saved display identifier", hidden = true)
	default String preferredMonitor()
	{
		return "";
	}
}
