package dekvall.bankdiff;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
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

	private static final int BANKSPACE_ITEM = 6512;
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

	@Inject
	private Gson gson;

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
				|| (event.getActionParam1() >> 16) != InterfaceID.BANK)
		{
			return;
		}

		int place = 1;

		if (!snapshot.isEmpty())
		{
			createEntry(TOGGLE_VIEW, event, place++);
		}

		if (!config.diffViewToggled())
		{
			createEntry(CREATE_SNAPSHOT, event, place);
		}

	}

	private void createEntry(String option, MenuEntryAdded event, int place)
	{
		client.createMenuEntry(-place)
			.setOption(option)
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.setIdentifier(event.getIdentifier())
			.setParam0(event.getActionParam0())
			.setParam1(event.getActionParam1())
			.onClick(this::onClickEvent);
	}

	private void onClickEvent(MenuEntry entry)
	{
		if (entry.getOption().equals(CREATE_SNAPSHOT))
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
		else if (entry.getOption().equals(TOGGLE_VIEW))
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
		Widget bankItems = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
		if (bankItems == null)
		{
			return;
		}
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null)
		{
			return;
		}
		negativeOneCounts.clear();
		for (Widget widget : bankItems.getDynamicChildren())
		{
			if (widget.getIndex() > bank.size() - 1)
			{
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
		Widget bankContainer = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
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
