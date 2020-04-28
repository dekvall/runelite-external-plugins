package dekvall.lowercaseusernames;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LowercaseUsernamesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LowercaseUsernamesPlugin.class);
		RuneLite.main(args);
	}
}