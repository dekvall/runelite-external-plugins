package dev.dkvl.lastrecall;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Slf4j
@PluginDescriptor(
	name = "Last Recall"
)
public class LastRecallPlugin extends Plugin
{
	private static final Joiner pipe = Joiner.on("|");
	private static final List<String> REGIONS = Stream.of(RegionShield.values()).map(RegionShield::getRegion).collect(Collectors.toList());
	private static final Pattern LAST_RECALL_STORE_PATTERN = Pattern.compile("Your Crystal of memories stores a memory of your last teleport from <col=ff0000>(" + pipe.join(REGIONS) + ")</col>.");
	private static final Pattern LAST_RECALL_MEMORY_PATTERN = Pattern.compile("You have a memory of teleporting from (" + pipe.join(REGIONS) +") stored in your Crystal of memories.");

	private static final String LAST_RECALL_FORGET_MESSAGE = "You rub the Crystal of memories and it brings you back to a place you remember.";
	private static final String LAST_RECALL_FORGOTTEN_MESSAGE = "You don't remember teleporting anywhere recently, try teleporting somewhere to store a new memory.";


	@Inject
	private Client client;

	@Inject
	private LastRecallConfig config;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MemoryOverlay memoryOverlay;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Last Recall started!");
		WorldPoint recallPoint = config.location();
		setNewMemory(recallPoint);
		overlayManager.add(memoryOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		worldMapPointManager.removeIf(LastRecallWorldMapPoint.class::isInstance);
		overlayManager.remove(memoryOverlay);
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

		Matcher mStore = LAST_RECALL_STORE_PATTERN.matcher(message);
		Matcher mMemory = LAST_RECALL_MEMORY_PATTERN.matcher(message);


		if (mStore.find())
		{
			String region = mStore.group(1);
			config.region(region);
			WorldPoint p = WorldPoint.fromLocal(client, client.getLocalPlayer().getLocalLocation());
			setNewMemory(p);
			updateMessageIfPossible(event.getMessageNode(), region, p);
		}
		else if (mMemory.find())
		{
			String region = mMemory.group(1);
			config.region(region);
			WorldPoint p = config.location();
			updateMessageIfPossible(event.getMessageNode(), region, p);
		}
		else if (message.equals(LAST_RECALL_FORGET_MESSAGE) || message.equals(LAST_RECALL_FORGOTTEN_MESSAGE))
		{
			setNewMemory(null);
			config.region(null);
		}
	}

	private void updateMessageIfPossible(MessageNode node, String region, @Nullable WorldPoint p)
	{
		if (p == null)
		{
			return;
		}

		NamedRegion namedRegion = NamedRegion.fromWorldPoint(p);

		if (namedRegion != null)
		{
			final String message = node.getValue();
			String newMessage = message.replace(region, namedRegion.getName());
			node.setValue(newMessage);
			chatMessageManager.update(node);
			client.refreshChat();
		}
	}

	@Provides
	LastRecallConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LastRecallConfig.class);
	}
}
