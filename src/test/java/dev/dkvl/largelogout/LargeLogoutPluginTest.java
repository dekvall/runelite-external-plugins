package dev.dkvl.largelogout;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LargeLogoutPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LargeLogoutPlugin.class);
		RuneLite.main(args);
	}
}