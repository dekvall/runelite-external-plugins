package dekvall.notempty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Not Empty",
	description = "Never empty potions again!"
)
public class NotEmptyPlugin extends Plugin
{
	private static final String DRINK_PATTERN = ".*\\(\\d\\)";

	@Inject
	private Client client;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Not Empty started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Not Empty stopped!");
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();
		List<MenuEntry> cleaned = new ArrayList<>();

		for (MenuEntry entry : menuEntries)
		{
			String option = entry.getOption().toLowerCase();

			if (entry.getType() != MenuAction.ITEM_FOURTH_OPTION
				|| !"empty".equals(option)
				|| !Pattern.matches(DRINK_PATTERN, entry.getTarget()))
			{
				cleaned.add(entry);
			}
		}
		client.setMenuEntries(cleaned.toArray(new MenuEntry[0]));
	}
}
