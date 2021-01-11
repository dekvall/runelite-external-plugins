package dev.dkvl.womutils;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("womutils")
public interface WomUtilsConfig extends Config
{
	@ConfigSection(
			name = "Group",
			description = "The group configurations",
			position = 1,
			closedByDefault = false
	)
	String groupConfig = "groupConfig";

	@ConfigItem(
		keyName = "updateIgnored",
		name = "Update Ignored",
		description = "Send name updates for people on your ignore list"
	)
	default boolean updateIgnored()
	{
		return true;
	}

	@ConfigItem(
			keyName = "menuOptions",
			name = "Menu options",
			description = "Show member and import options in menus",
			position = 0,
			section = groupConfig
	)
	default boolean menuOptions()
	{
		return true;
	}

	@ConfigItem(
			keyName = "groupId",
			name = "Group Id",
			description = "The group id in WOM",
			position = 1,
			section = groupConfig
	)
	default int groupId()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "verificationCode",
			name = "Verification code",
			description = "Verification code for the WOM group",
			secret = true,
			position = 2,
			section = groupConfig
	)
	default String verificationCode()
	{
		return "";
	}

}
