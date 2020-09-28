package dekvall.bankdiff;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BankDiffPlugin.CONFIG_GROUP)
public interface BankDiffConfig extends Config
{
	@ConfigItem(
		keyName = "restoreOpacity",
		name = "Restore placeholder opacity",
		description = "Keep the original item sprite even when you run out of an item"
	)
	default boolean restoreOpacity()
	{
		return true;
	}
}
