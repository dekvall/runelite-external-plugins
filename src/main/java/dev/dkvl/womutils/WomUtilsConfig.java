package dev.dkvl.womutils;

import java.awt.Color;
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

	@ConfigSection(
		name = "Lookup",
		description = "Lookup menu option configurations",
		position = 2
	)
	String lookupConfig = "lookupConfig";

	@ConfigSection(
		name = "Competitions",
		description = "Competition configurations",
		position = 3
	)
	String competitionConfig = "competitionConfig";

	@ConfigSection(
		name = "Event codeword",
		description = "Event codeword configurations",
		position = 4
	)
	String eventCodeword = "eventCodeword";

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
		keyName = "virtualLevels",
		name = "Virtual levels",
		description = "Show virtual levels in the side bar on lookup",
		position = 2,
		section = lookupConfig
	)
	default boolean virtualLevels() { return false; }

	@ConfigItem(
		keyName = "relativeTime",
		name = "Relative time",
		description = "Display last updated time relative to current date and time",
		position = 3,
		section = lookupConfig
	)
	default boolean relativeTime() { return false; }

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
		description = "Add Browse WOM Group menu option to the clan chat tab",
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
		keyName = "syncClanButton",
		name = "Sync Clan button",
		description = "Add a sync clan button to the clan members list in settings if a group is configured",
		position = 5,
		section = groupConfig
	)
	default boolean syncClanButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "groupId",
		name = "Group Id",
		description = "The group id in WOM",
		position = 6,
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
		position = 7,
		section = groupConfig
	)
	default String verificationCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "competitionLoginMessage",
		name = "Login info",
		description = "Show current and upcoming competition info when logging in",
		position = 8,
		section = competitionConfig
	)
	default boolean competitionLoginMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "timerOngoing",
		name = "Timer Ongoing",
		description = "Displays timers for ongoing competitions",
		position = 9,
		section = competitionConfig
	)
	default boolean timerOngoing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "timerUpcoming",
		name = "Timer Upcoming",
		description = "Display timers for upcoming competitions",
		position = 10,
		section = competitionConfig
	)
	default boolean timerUpcoming()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendCompetitionNotification",
		name = "Competition Notifications",
		description = "Sends notifications at start and end times for competitions",
		position = 11,
		section = competitionConfig
	)
	default boolean sendCompetitionNotification()
	{
		return false;
	}

	@ConfigItem(
		keyName = "displayCodeword",
		name = "Display codeword",
		description = "Displays an event codeword overlay",
		position = 12,
		section = eventCodeword
	)
	default boolean displayCodeword()
	{
		return false;
	}

	@ConfigItem(
		keyName = "configuredCodeword",
		name = "Codeword",
		description = "Event codeword",
		position = 13,
		section = eventCodeword
	)
	default String configuredCodeword()
	{
		return "WOMCodeword";
	}

	@ConfigItem(
		keyName = "showTimestamp",
		name = "Show timestamp",
		description = "Attach a timestamp to the codeword",
		position = 14,
		section = eventCodeword
	)
	default boolean showTimestamp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "codewordColor",
		name = "Codeword color",
		description = "Overlay codeword color",
		position = 15,
		section = eventCodeword
	)
	default Color codewordColor()
	{
		return new Color(0x00FF6A);
	}

	@ConfigItem(
		keyName = "timestampColor",
		name = "Timestamp color",
		description = "Overlay timestamp color",
		position = 16,
		section = eventCodeword
	)
	default Color timestampColor()
	{
		return new Color(0xFFFFFF);
	}

	@ConfigItem(
		keyName = "hiddenCompetitionIds",
		name = "",
		description = "",
		hidden = true
	)
	default String hiddenCompetitionIds()
	{
		return "[]";
	}

	@ConfigItem(
		keyName = "hiddenCompetitionIds",
		name = "",
		description = "",
		hidden = true
	)
	void hiddenCompetitionIds(String value);
}
