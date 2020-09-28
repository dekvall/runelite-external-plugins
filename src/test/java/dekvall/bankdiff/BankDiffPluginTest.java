package dekvall.bankdiff;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankDiffPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankDiffPlugin.class);
		RuneLite.main(args);
	}
}