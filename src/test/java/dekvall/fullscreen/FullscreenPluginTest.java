package dekvall.fullscreen;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FullscreenPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FullscreenPlugin.class);
		RuneLite.main(args);
	}
}