package dev.dkvl.womutils;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(WomUtilsPlugin.CONFIG_GROUP)
public interface WomUtilsConfig extends Config
{
	@ConfigSection(
		name = "Group",
		description = "The group configurations",
		position = 1
	)
	String groupConfig = "groupConfig";

	@ConfigItem(
		keyName = "showIcons",
		name = "Show icons",
		description = "Show icons on friend and clan chat",
		position = 0,
		section = groupConfig
	)
	default boolean showicons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "menuOptions",
		name = "Menu options",
		description = "Show member and import options in menus",
		position = 1,
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
		position = 2,
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
		position = 3,
		section = groupConfig
	)
	default String verificationCode()
	{
		return "";
	}

}
