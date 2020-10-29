package dev.dkvl.lastrecall;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Slf4j
@PluginDescriptor(
	name = "Last Recall"
)
public class LastRecallPlugin extends Plugin
{
	private static final String LAST_RECALL_STORE_MESSAGE_START = "Your Crystal of memories stores a memory of your last teleport from";
	private static final String LAST_RECALL_FORGET_MESSAGE = "You rub the Crystal of memories and it brings you back to a place you remember.";
	private static final String LAST_RECALL_FORGOTTEN_MESSAGE = "You don't remember teleporting anywhere recently, try teleporting somewhere to store a new memory.";

	@Inject
	private Client client;

	@Inject
	private LastRecallConfig config;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Last Recall started!");
		WorldPoint recallPoint = config.location();
		setNewMemory(recallPoint);
	}

	@Override
	protected void shutDown() throws Exception
	{
		worldMapPointManager.removeIf(LastRecallWorldMapPoint.class::isInstance);
		log.info("Last Recall stopped!");
	}

	private void setNewMemory(WorldPoint point)
	{
		worldMapPointManager.removeIf(LastRecallWorldMapPoint.class::isInstance);

		if (point == null)
		{
			return;
		}
		worldMapPointManager.add(new LastRecallWorldMapPoint(point));
		config.location(point);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();

		if (message.startsWith(LAST_RECALL_STORE_MESSAGE_START))
		{
			WorldPoint location = WorldPoint.fromLocal(client, client.getLocalPlayer().getLocalLocation());
			setNewMemory(location);
		}
		else if (message.equals(LAST_RECALL_FORGET_MESSAGE) || message.equals(LAST_RECALL_FORGOTTEN_MESSAGE))
		{
			setNewMemory(null);
		}
	}

	@Provides
	LastRecallConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LastRecallConfig.class);
	}
}
