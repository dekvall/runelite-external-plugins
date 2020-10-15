package dekvall.planksack;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Plank Sack"
)
public class PlankSackPlugin extends Plugin
{
	private static final Pattern PLANK_SACK_CHECK_PATTERN = Pattern.compile(
		"Basic planks: <col=ef1020>(\\d+)</col>, Oak planks: <col=ef1020>(\\d+)</col>, Teak planks: <col=ef1020>(\\d+)</col>, Mahogany planks: <col=ef1020>(\\d+)</col>");

	@Inject
	private Client client;

	@Inject
	private PlankSackConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PlankSackOverlay overlay;

	private int prevXp = -1;
	private int diff;

	@Getter
	private Map<Plank, Integer> count = new HashMap<>();

	private boolean hammering;
	private boolean usedPlanks;
	private boolean filledSack;
	private boolean emptiedSack;
	private Plank type;

	private Multiset<Integer> lastInventory = null;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Plank Sack started!");
		overlayManager.add(overlay);
		reset();
	}

	@Override
	protected void shutDown() throws Exception
	{
		reset();
		overlayManager.remove(overlay);
		log.info("Plank Sack stopped!");
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.CONSTRUCTION)
		{
			return;
		}

		if (prevXp == -1)
		{
			prevXp = event.getXp();
			return;
		}

		int currXp = event.getXp();
		diff = currXp - prevXp;

		if (hammering)
		{
			// Assuming no construction object uses more than 10 planks
			if (diff % Plank.REGULAR.getXp() == 0)
			{
				type = Plank.REGULAR;
			}
			else if (diff % Plank.MAHOGANY.getXp() == 0)
			{
				type = Plank.MAHOGANY;
			}
			else if (diff % Plank.OAK.getXp() == 0 && diff % Plank.TEAK.getXp() == 0)
			{
				// Teaks and oaks clash since oak:teak is 2:3 in xp
				// Assume we use teak, because who cares and clashes are at:
				//
				// Number of planks for same xp
				// OAK  : 3	6 9
				// TEAK : 2 4 6
				// They are probably doing teak benches anyway
				type = Plank.TEAK;
			}
			else if (diff % Plank.OAK.getXp() == 0)
			{
				type = Plank.OAK;
			}
			else if (diff % Plank.TEAK.getXp() == 0)
			{
				type = Plank.TEAK;
			}
			usedPlanks = true;
			int c = count.get(type);
			count.put(type, c - diff / type.getXp());
		}

		prevXp = currXp;
		hammering = false;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor != client.getLocalPlayer() || actor.getAnimation() != 3676)
		{
			return;
		}
		hammering = true;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() != client.getItemContainer(InventoryID.INVENTORY))
		{
			return;
		}

		Multiset<Integer> currentInventory = snapshotInventory();

		if (lastInventory == null)
		{
			lastInventory = currentInventory;
			return;
		}

		if (!filledSack && !usedPlanks && !emptiedSack)
		{
			return;
		}

		Multiset<Integer> diff = HashMultiset.create();
		if (filledSack || usedPlanks)
		{
			diff = Multisets.difference(lastInventory, currentInventory);
		}
		else if (emptiedSack)
		{
			diff = Multisets.difference(currentInventory, lastInventory);
		}

		for (Multiset.Entry<Integer> e : diff.entrySet())
		{
			Plank type = Plank.of(e.getElement());

			if (type != null)
			{
				// Add the removed planks back to the sack, because they came from the inventory
				int c = count.get(type);
				if (filledSack || usedPlanks)
				{
					// When we use planks and also have an inventory change, it means we used some from our inventory
					// which means that we have removed too many planks from the bag on the stat change, so we restore some.
					count.put(type, c + e.getCount());
				}
				else if (emptiedSack)
				{
					count.put(type, c - e.getCount());
				}
				// break because we don't support multiple plank types
				break;
			}
		}

		filledSack = false;
		emptiedSack = false;
		usedPlanks = false;
		lastInventory = currentInventory;
	}

	private Multiset<Integer> snapshotInventory()
	{
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (itemContainer != null)
		{
			final Multiset<Integer> invSnapshot = HashMultiset.create();
			Arrays.stream(itemContainer.getItems())
				.forEach(item -> invSnapshot.add(item.getId(), item.getQuantity()));
			return invSnapshot;
		}
		return null;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!(event.getMenuAction() == MenuAction.ITEM_FIRST_OPTION || event.getMenuAction() == MenuAction.ITEM_FOURTH_OPTION)
			|| !(event.getMenuTarget().equals("<col=ff9040>Plank sack") || event.getMenuTarget().equals("Fill"))) // Left-click filling has 'Fill' as target
		{
			return;
		}

		if (event.getMenuOption().equals("Fill"))
		{
			filledSack = true;
		}
		else if (event.getMenuOption().equals("Empty"))
		{
			emptiedSack = true;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		Matcher matcher = PLANK_SACK_CHECK_PATTERN.matcher(event.getMessage());
		if(matcher.find())
		{
			int basic = Integer.parseInt(matcher.group(1));
			int oak = Integer.parseInt(matcher.group(2));
			int teak = Integer.parseInt(matcher.group(3));
			int mahogany = Integer.parseInt(matcher.group(4));

			setCounts(basic, oak, teak, mahogany);
		}
	}

	private void setCounts(int basic, int oak, int teak, int mahogany)
	{
		count.put(Plank.REGULAR, basic);
		count.put(Plank.OAK, oak);
		count.put(Plank.TEAK, teak);
		count.put(Plank.MAHOGANY, mahogany);
	}

	private void reset()
	{
		setCounts(0, 0, 0, 0);
	}

	@Provides
	PlankSackConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlankSackConfig.class);
	}
}
