package dekvall.emojimadness;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EmojiMadnessPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EmojiMadnessPlugin.class);
		RuneLite.main(args);
	}
}