package dev.dkvl.tipoftheday;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TipOfTheDayPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TipOfTheDayPlugin.class);
		RuneLite.main(args);
	}
}