package dev.dkvl.gunsgains;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TheGunsGainsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TheGunsGainsPlugin.class);
		RuneLite.main(args);
	}
}