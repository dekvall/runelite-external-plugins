package dekvall.pushnotification;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pushnotifications")
public interface PushNotificationsConfig extends Config
{
	@ConfigItem(
		keyName = "pushbullet",
		name = "Pushbullet token",
		description = "API token for pushbullet"
	)
	String pushbullet();
}
