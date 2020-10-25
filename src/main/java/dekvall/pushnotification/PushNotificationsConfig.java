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
}
