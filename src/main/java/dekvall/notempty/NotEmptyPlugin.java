package dekvall.notempty;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Not Empty",
	description = "Never empty potions again!"
)
public class NotEmptyPlugin extends Plugin
{

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
			int type = entry.getType();
			String option = entry.getOption().toLowerCase();

			if (type != MenuAction.ITEM_FOURTH_OPTION.getId()
				|| !"empty".equals(option))
			{
				cleaned.add(entry);
			}
		}
		client.setMenuEntries(cleaned.toArray(new MenuEntry[0]));
	}
}
