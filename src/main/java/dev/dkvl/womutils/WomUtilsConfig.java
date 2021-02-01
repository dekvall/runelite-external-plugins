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

	@ConfigSection(
		name = "Lookup",
		description = "Lookup menu option configurations",
		position = 2
	)
	String lookupConfig = "lookupConfig";

	@ConfigItem(
		keyName = "playerLookupOption",
		name = "Player option",
		description = "Add WOM Lookup option to players",
		position = 0,
		section = lookupConfig
	)
	default boolean playerLookupOption() { return true; }

	@ConfigItem(
		keyName = "menuLookupOption",
		name = "Menu option",
		description = "Add WOM Lookup option to menus",
		position = 1,
		section = lookupConfig
	)
	default boolean menuLookupOption() { return true; }

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
		name = "Show flags",
		description = "Show flags instead of the group icon where possible for your group members, requires icons to be enabled.",
		position = 1,
		section = groupConfig
	)
	default boolean showFlags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "importGroup",
		name = "Import Group option",
		description = "Add Import WOM Group menu option to the clan chat tab",
		position = 2,
		section = groupConfig
	)
	default boolean importGroup()
	{
		return true;
	}

	@ConfigItem(
		keyName = "browseGroup",
		name = "Browse Group option",
		description = "Add Browse WOM Group menu option to th eclan chat tab",
		position = 3,
		section = groupConfig
	)
	default boolean browseGroup()
	{
		return true;
	}

	@ConfigItem(
		keyName = "addRemoveMember",
		name = "Add/Remove Member option",
		description = "Add options to add & remove players from group, to clan chat and friend list",
		position = 4,
		section = groupConfig
	)
	default boolean addRemoveMember()
	{
		return true;
	}

	@ConfigItem(
		keyName = "groupId",
		name = "Group Id",
		description = "The group id in WOM",
		position = 5,
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
		position = 6,
		section = groupConfig
	)
	default String verificationCode()
	{
		return "";
	}

}
