package dekvall.bankdiff;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "Bank Diff"
)
public class BankDiffPlugin extends Plugin
{
	static final String CONFIG_GROUP = "bankdiff";
	private static final Gson gson = new Gson();

	private static final int BANKSPACE_ITEM = 6512;
	private static final int MAX_BANK_SLOTS = 816;
	@Inject
	private Client client;

	@Inject
	private BankDiffConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NegativeOneItemOverlay negativeOneItemOverlay;

	private final Map<Integer, Integer> snapshot = new HashMap<>();

	@Getter
	private final Set<Integer> negativeOneCounts = new HashSet<>();

	private static final String CREATE_SNAPSHOT = "Take diff snapshot";
	private static final String TOGGLE_VIEW = "Toggle diff view";

	@Override
	protected void startUp() throws Exception
	{
		log.info("Bank Diff started!");
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			grabSnapshot();
		}
		overlayManager.add(negativeOneItemOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		snapshot.clear();
		overlayManager.remove(negativeOneItemOverlay);
		log.info("Bank Diff stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			grabSnapshot();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			snapshot.clear();
		}
	}

	private void grabSnapshot()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, getConfigKey(), String.class);

		Map<Integer, Integer> fromConfig = gson.fromJson(json, new TypeToken<Map<Integer, Integer>>() {}.getType());
		snapshot.clear();

		if (fromConfig != null)
		{
			snapshot.putAll(fromConfig);
		}
		else
		{
			// Disable the view if no diff snapshot exists
			config.diffViewToggled(false);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getType() != MenuAction.CC_OP.getId() || !event.getOption().equals("Show menu")
				|| (event.getActionParam1() >> 16) != WidgetID.BANK_GROUP_ID)
		{
			return;
		}

		MenuEntry[] entries = client.getMenuEntries();

		int offset = config.diffViewToggled() || snapshot.isEmpty() ? 1 : 2;
		entries = Arrays.copyOf(entries, entries.length + offset);
		int place = 1;

		if (!snapshot.isEmpty())
		{
			entries[entries.length - place++] = createEntry(TOGGLE_VIEW, event);
		}

		if (!config.diffViewToggled())
		{
			entries[entries.length - place] = createEntry(CREATE_SNAPSHOT, event);
		}

		client.setMenuEntries(entries);
	}

	private MenuEntry createEntry(String option, MenuEntryAdded event)
	{
		MenuEntry entry = new MenuEntry();
		entry.setOption(option);
		entry.setTarget("");
		entry.setType(MenuAction.RUNELITE.getId());
		entry.setIdentifier(event.getIdentifier());
		entry.setParam0(event.getActionParam0());
		entry.setParam1(event.getActionParam1());

		return entry;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getMenuAction() != MenuAction.RUNELITE)
				|| (event.getWidgetId() >> 16) != WidgetID.BANK_GROUP_ID
				|| !(event.getMenuOption().equals(CREATE_SNAPSHOT) || event.getMenuOption().equals(TOGGLE_VIEW)))
		{
			return;
		}

		if (event.getMenuOption().equals(CREATE_SNAPSHOT))
		{
			ItemContainer bank = client.getItemContainer(InventoryID.BANK);

			snapshot.clear();

			for (Item item : bank.getItems())
			{
				int id = itemManager.canonicalize(item.getId());
				Integer current = snapshot.getOrDefault(id, 0);
				snapshot.put(item.getId(), item.getQuantity() + current);
			}

			configManager.setConfiguration(CONFIG_GROUP, getConfigKey(), gson.toJson(snapshot));
		}
		else if (event.getMenuOption().equals(TOGGLE_VIEW))
		{
			config.diffViewToggled(!config.diffViewToggled());
			layoutBank();
		}
	}

	private String getConfigKey()
	{
		return client.getUsername() + ".snapshot";
	}

	private void diffAgainstSnapshot()
	{
		Widget bankItems = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		negativeOneCounts.clear();

		for (Widget widget : bankItems.getDynamicChildren())
		{
			if (widget.getIndex() > MAX_BANK_SLOTS - 1)
			{
				// Apparently the banktag symbols
				// are around index 844, which is out of the bank anyway.
				// return so it won't diff those since they have 10k quantity
				// Although, i don't think it matters.
				return;
			}

			if (widget.getItemId() == BANKSPACE_ITEM)
			{
				continue;
			}

			int id = itemManager.canonicalize(widget.getItemId());

			int origQuantity = snapshot.getOrDefault(id, 0);
			int newQuantity = widget.getItemQuantity();
			int diff = newQuantity - origQuantity;

			if (diff == 0)
			{
				widget.setHidden(true);
			}
			else
			{
				widget.setItemId(id);
				widget.setItemQuantity(diff);

				if (config.restoreOpacity())
				{
					widget.setOpacity(0);
				}
			}

			if (diff == -1)
			{
				// Numbers for quantities set to -1 do not show
				// so we render our own
				negativeOneCounts.add(id);
			}
		}
	}

	public void layoutBank()
	{
		Widget bankContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		if (bankContainer == null || bankContainer.isHidden())
		{
			return;
		}

		Object[] scriptArgs = bankContainer.getOnInvTransmitListener();
		if (scriptArgs == null)
		{
			return;
		}

		client.runScript(scriptArgs);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_BUILD)
		{
			if (config.diffViewToggled())
			{
				diffAgainstSnapshot();
			}
		}
	}

	@Provides
	BankDiffConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankDiffConfig.class);
	}
}
