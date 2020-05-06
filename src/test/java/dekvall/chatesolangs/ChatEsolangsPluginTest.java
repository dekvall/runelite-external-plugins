package dekvall.chatesolangs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ChatEsolangsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatEsolangsPlugin.class);
		RuneLite.main(args);
	}
}