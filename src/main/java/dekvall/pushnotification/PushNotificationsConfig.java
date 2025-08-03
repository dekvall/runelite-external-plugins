package dekvall.pushnotification;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("pushnotifications")
public interface PushNotificationsConfig extends Config
{
	@ConfigSection(
		name = "Filters",
		description = "Forwarding filters",
		position = 0
	)
	String filtersSection = "filters";

	@ConfigItem(
		keyName = "forward_notifications",
		name = "Forward notifications",
		description = "Forward all notifications",
		section = filtersSection
	)
	default boolean forwardNotifications()
	{
		return true;
	};

	@ConfigItem(
		keyName = "forward_plugin_messages",
		name = "Forward plugin messages",
		description = "Forward all plugin messages",
		section = filtersSection
	)
	default boolean forwardPluginMessages()
	{
		return true;
	}

	@ConfigSection(
		name = "Pushbullet",
		description = "Pushbullet Settings",
		position = 1
	)
	String pushbulletSection = "pushbullet";

	@ConfigItem(
		keyName = "pushbullet",
		name = "Pushbullet token",
		description = "API token for pushbullet",
		section = pushbulletSection
	)
	String pushbulletToken();

	@ConfigSection(
		name = "Pushover",
		description = "Pushover Settings",
		position = 2
	)
	String pushoverSection = "pushover";

	@ConfigItem(
		keyName = "pushover_user",
		name = "Pushover user key",
		description = "User key for Pushover",
		section = pushoverSection
	)
	String pushoverUser();

	@ConfigItem(
		keyName = "pushover_api",
		name = "Pushover API token",
		description = "API token for Pushover",
		section = pushoverSection
	)
	String pushoverToken();

	@ConfigSection(
		name = "Gotify",
		description = "Gotify Settings",
		position = 3
	)
	String gotifySection = "gotify";

	@ConfigItem(
		keyName = "gotify_url",
		name = "Gotify URL",
		description = "URL for Gotify server, example: http://10.0.0.30:8080/message",
		section = gotifySection
	)
	String gotifyUrl();

	@ConfigItem(
		keyName = "gotify_token",
		name = "Gotify token",
		description = "Token for Gotify server",
		section = gotifySection
	)
	String gotifyToken();

	@ConfigItem(
		keyName = "gotify_priority",
		name = "Gotify Priority",
		description = "Priority for Gotify notification",
		section = gotifySection
	)
	default int gotifyPriority() {
		return 5;
	}

	@ConfigSection(
		name = "Pushcut",
		description = "Pushcut Settings",
		position = 4
	)
	String pushcutSection = "pushcut";

	@ConfigItem(
		keyName = "pushcut_notification",
		name = "Pushcut Notification",
		description = "Name or reference of the Pushcut notification to trigger",
		section = pushcutSection
	)
	String pushcutNotification();

	@ConfigItem(
		keyName = "pushcut_secret",
		name = "Pushcut Secret",
		description = "Pushcut webhook secret",
		section = pushcutSection
	)
	String pushcutSecret();
}
