package dev.dkvl.womutils;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

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
		description = "Show icons in friend list and clan chat for people who are in the WOM group",
		position = 0,
		section = groupConfig
	)
	default boolean showicons()
	{
		return true;
	}

	@ConfigItem(
		keyName= "showFlags",
		name = "Show Flags",
		description = "Show flags instead of the group icon where possible for your group members, requires icons to be enabled.",
		position = 1,
		section = groupConfig
	)
	default boolean showFlags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "menuOptions",
		name = "Menu options",
		description = "Show member and import options in menus",
		position = 2,
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
		position = 3,
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
		position = 4,
		section = groupConfig
	)
	default String verificationCode()
	{
		return "";
	}

}
