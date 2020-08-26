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
	name = "Not Empty"
)
public class NotEmptyPlugin extends Plugin
{
	static final String CONFIG_GROUP = "notempty";

	@Inject
	private Client client;

	@Inject
	private NotEmptyConfig config;

	private Set<String> removedEntries;

	@Override
	protected void startUp() throws Exception
	{
		createFilter();
		log.info("Not Empty started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		removedEntries.clear();
		log.info("Not Empty stopped!");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			createFilter();
		}
	}

	private void createFilter()
	{
		removedEntries = Text.fromCSV(config.removedEntries())
				.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
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

			if (!isItemEntry(type) || !removedEntries.contains(option))
			{
				cleaned.add(entry);
			}
		}
		client.setMenuEntries(cleaned.toArray(new MenuEntry[0]));
	}

	private boolean isItemEntry(int type)
	{
		MenuAction action = MenuAction.of(type);

		switch (action)
		{
			// Not using USE or FIRST_OPTION to prevent potential abuse
			case ITEM_SECOND_OPTION:
			case ITEM_THIRD_OPTION:
			case ITEM_FOURTH_OPTION:
			case ITEM_FIFTH_OPTION:
			case ITEM_DROP:
			case EXAMINE_ITEM:
				return true;
			default:
				return false;
		}
	}

	@Provides
	NotEmptyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NotEmptyConfig.class);
	}
}
