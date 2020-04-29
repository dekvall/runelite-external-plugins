package dekvall.inventoryscrabble;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Inventory Scrabble"
)
public class InventoryScrabblePlugin extends Plugin
{

	private static final Set<Integer> TUTORIAL_ISLAND_REGIONS = ImmutableSet.of(12336, 12335, 12592, 12080, 12079, 12436);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InventoryScrabbleConfig config;

	@Inject
	private ItemManager itemManager;

	private boolean onTutorialIsland;
	private Multiset<Character> counts;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Inventory Scrabble started!");
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() -> {
				gatherItemNames();
				checkArea();
			});
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		counts.clear();
		log.info("Inventory Scrabble stopped!");
	}

	private void gatherItemNames()
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);

		if (inventory != null)
		{
			counts = HashMultiset.create();
			Arrays.stream(inventory.getItems())
				.map(item -> itemManager.getItemComposition(item.getId())
					.getName()
					.toLowerCase())
				.filter(name -> name.equals("null"))
				.map(name -> Text.removeTags(name)
					.replaceAll("[^a-z]", "")
					.charAt(0))
				.forEach(c -> counts.add(c));
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			gatherItemNames();
			checkArea();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY))
		{
			gatherItemNames();
		}
	}

	@Subscribe(priority = -1)
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (onTutorialIsland)
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		List<MenuEntry> cleaned = new ArrayList<>();

		Set<String> checked = new HashSet<>();
		Set<String> okTargets = new HashSet<>();

		for (MenuEntry entry : menuEntries)
		{
			int type = entry.getType();

			if (isNpcEntry(type))
			{
				String target = entry.getTarget();

				if (!checked.contains(target))
				{
					Multiset<Character> targetChars = cleanTarget(target);
					if (targetChars.entrySet().stream()
						.noneMatch(e -> e.getCount() > counts.count(e.getElement())))
					{
						okTargets.add(target);
					}
					checked.add(target);
				}

				if (!okTargets.contains(target))
				{
					continue;
				}
			}
			cleaned.add(entry);
		}

		MenuEntry[] newEntries = cleaned.toArray(new MenuEntry[0]);
		client.setMenuEntries(newEntries);
	}

	Multiset<Character> cleanTarget(String target)
	{
		String noTags = Text.removeTags(target).toLowerCase();

		// Do not include level in the comparison
		int idx = noTags.indexOf('(');

		String name = noTags;
		if (idx != -1)
		{
			name = noTags.substring(0, idx);
		}

		Multiset<Character> targetCount = HashMultiset.create();
		char[] chars = name.replaceAll("[^a-z]", "").toCharArray();
		for (char c : chars)
		{
			targetCount.add(c);
		}

		return targetCount;
	}

	boolean isNpcEntry(int type)
	{
		if (type >= 2000)
		{
			type -= 2000;
		}

		MenuAction action = MenuAction.of(type);

		switch (action)
		{
			case SPELL_CAST_ON_NPC:
			case ITEM_USE_ON_NPC:
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	private void checkArea()
	{
		final Player player = client.getLocalPlayer();
		if (player != null && TUTORIAL_ISLAND_REGIONS.contains(player.getWorldLocation().getRegionID()))
		{
			onTutorialIsland = true;
		}
	}

	@Provides
	InventoryScrabbleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryScrabbleConfig.class);
	}
}
