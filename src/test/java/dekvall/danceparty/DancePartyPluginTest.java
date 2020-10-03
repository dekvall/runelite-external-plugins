package dekvall.danceparty;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DancePartyPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DancePartyPlugin.class);
		RuneLite.main(args);
	}
}