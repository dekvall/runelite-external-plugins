package dev.dkvl.lastrecall;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LastRecallPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LastRecallPlugin.class);
		RuneLite.main(args);
	}
}