package dekvall.pushnotification;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PushNotificationsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PushNotificationsPlugin.class);
		RuneLite.main(args);
	}
}