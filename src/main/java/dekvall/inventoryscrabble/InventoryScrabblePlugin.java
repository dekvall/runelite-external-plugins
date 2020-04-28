package dekvall.inventoryscrabble;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.graalvm.compiler.replacements.InstanceOfSnippetsTemplates;

@Slf4j
@PluginDescriptor(
	name = "Inventory Scrabble"
)
public class InventoryScrabblePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InventoryScrabbleConfig config;

	@Inject
	private ItemManager itemManager;

	private List<String> itemNames;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Inventory Scrabble started!");
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::gatherItemNames);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		itemNames.clear();
		log.info("Inventory Scrabble stopped!");
	}

	private void gatherItemNames()
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);

		if (inventory != null)
		{
			itemNames = Arrays.stream(inventory.getItems())
				.map(item -> itemManager.getItemComposition(item.getId()).getName())
				.collect(Collectors.toList());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			gatherItemNames();
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

	@Provides
	InventoryScrabbleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryScrabbleConfig.class);
	}
}
