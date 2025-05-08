package dekvall.pushnotification;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("pushnotifications")
public interface PushNotificationsConfig extends Config
{
	@ConfigSection(
		name = "Pushbullet",
		description = "Pushbullet Settings",
		position = 0
	)
	String pushbulletSection = "pushbullet";

	@ConfigItem(
		keyName = "pushbullet",
		name = "Pushbullet token",
		description = "API token for pushbullet",
		section = pushbulletSection
	)
	String pushbullet();

	@ConfigSection(
		name = "Pushover",
		description = "Pushover Settings",
		position = 1
	)
	String pushoverSection = "pushover";

	@ConfigItem(
		keyName = "pushover_user",
		name = "Pushover user key",
		description = "User key for Pushover",
		section = pushoverSection
	)
	String pushover_user();

	@ConfigItem(
		keyName = "pushover_api",
		name = "Pushover API token",
		description = "API token for Pushover",
		section = pushoverSection
	)
	String pushover_api();

	@ConfigSection(
		name = "Gotify",
		description = "Gotify Settings",
		position = 2
	)
	String gotifySection = "gotify";

	@ConfigItem(
		keyName = "gotify_url",
		name = "Gotify URL",
		description = "URL for Gotify server, example: http://10.0.0.30:8080/message",
		section = gotifySection
	)
	String gotify_url();

	@ConfigItem(
		keyName = "gotify_token",
		name = "Gotify token",
		description = "Token for Gotify server",
		section = gotifySection
	)
	String gotify_token();

	@ConfigItem(
		keyName = "gotify_priority",
		name = "Gotify Priority",
		description = "Priority for Gotify notification",
		section = gotifySection
	)
	default int gotify_priority() {
		return 5;
	}
}
