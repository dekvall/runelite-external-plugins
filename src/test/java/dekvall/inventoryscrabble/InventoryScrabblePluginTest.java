package dekvall.inventoryscrabble;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InventoryScrabblePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InventoryScrabblePlugin.class);
		RuneLite.main(args);
	}
}