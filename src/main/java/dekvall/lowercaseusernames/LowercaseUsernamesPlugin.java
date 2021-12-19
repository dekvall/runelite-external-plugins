package dekvall.lowercaseusernames;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;
import static net.runelite.api.MenuAction.WALK;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Lowercase Usernames"
)
public class LowercaseUsernamesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private LowercaseUsernamesConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Lowercase Usernames started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Lowercase Usernames stopped!");
	}

	@Subscribe(priority=1) //if we go first we only have to care about col tags
	public void onClientTick(ClientTick event)
	{
		if (client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		boolean modified = false;


		for (MenuEntry entry : menuEntries)
		{
			MenuAction type = entry.getType();

			if (!isPlayerEntry(type))
			{
				continue;
			}

			int identifier = entry.getIdentifier();

			Player[] players = client.getCachedPlayers();
			Player player = null;

			// 'Walk here' identifiers are offset by 1 because the default
			// identifier for this option is 0, which is also a player index.
			if (type == WALK)
			{
				identifier--;
			}

			if (identifier >= 0 && identifier < players.length)
			{
				player = players[identifier];
			}

			if (player == null)
			{
				continue;
			}

			String target = entry.getTarget();
			String cased = config.uppercase() ? target.toUpperCase() : target.toLowerCase();
			// A target has tags that we want to preserve

			StringBuilder sb = new StringBuilder();
			boolean insideTag = false;

			for (int i = 0; i < target.length(); i++)
			{
				char part = target.charAt(i);
				// All tags have to start with < and there are no nested tags so this should work
				if (part == '<' || part == '>')
				{
					insideTag = !insideTag;
				}

				if (insideTag)
				{
					sb.append(part);
				}
				else
				{
					sb.append(cased.charAt(i));
				}
			}

			String newTarget = sb.toString();
			entry.setTarget(newTarget);
			modified = true;
		}

		if (modified)
		{
			client.setMenuEntries(menuEntries);
		}
	}

	boolean isPlayerEntry(MenuAction action)
	{
		switch (action)
		{
			case WALK:
			case SPELL_CAST_ON_PLAYER:
			case ITEM_USE_ON_PLAYER:
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case PLAYER_EIGTH_OPTION:
			case RUNELITE_PLAYER:
				return true;
			default:
				return false;
		}
	}

	@Provides
	LowercaseUsernamesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LowercaseUsernamesConfig.class);
	}
}
