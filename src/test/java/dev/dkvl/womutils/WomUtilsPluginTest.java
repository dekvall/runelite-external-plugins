package dev.dkvl.womutils;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WomUtilsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WomUtilsPlugin.class);
		RuneLite.main(args);
	}
}