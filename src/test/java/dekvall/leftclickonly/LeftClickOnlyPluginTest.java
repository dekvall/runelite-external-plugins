package dekvall.leftclickonly;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LeftClickOnlyPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LeftClickOnlyPlugin.class);
		RuneLite.main(args);
	}
}