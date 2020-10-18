package dekvall.danceparty;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Dance Party"
)
public class DancePartyPlugin extends Plugin
{
	private static final int DANCE_DURATION_BARROWS = 8; // 8 ticks = 4.8 seconds
	private static final int DANCE_DURATION_BOSSKILL = 8; // 4.8 seconds
	private static final int DANCE_DURATION_LEVELUP = 40; // 24 seconds
	private static final int DANCE_DURATION_PETDROP = 60; // 36 seconds
	private static final int DANCE_DURATION_RAIDDONE = 20; // 12 seconds

	private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+)");
	private static final Pattern BOSSKILL_MESSAGE_PATTERN = Pattern.compile("Your (.+) kill count is: <col=ff0000>(\\d+)</col>.");
	private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of("You have a funny feeling like you're being followed",
			"You feel something weird sneaking into your backpack",
			"You have a funny feeling like you would have been followed");

	@Inject
	private Client client;

	@Inject
	private DancePartyConfig config;

	private final Set<Player> players = new HashSet<>();
	private final Random rand = new Random();

	private static int forceDanceTick = 0;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Dance Party started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		players.clear();
		log.info("Dance Party stopped!");
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		if (event.getPlayer() != client.getLocalPlayer())
		{
			players.add(event.getPlayer());
		}
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		players.remove(event.getPlayer());
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		switch (event.getActor().getGraphic())
		{
			// Levelup Fireworks
			case 199:
			case 1388:
			case 1389:
				forceDanceTick = client.getTickCount() + DANCE_DURATION_LEVELUP;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		for (Player player : players)
		{
			applyAnimationIfPossible(player);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM
			&& event.getType() != ChatMessageType.TRADE
			&& event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION)
		{
			return;
		}

		String chatMessage = event.getMessage();

		if (config.partyOnRaidDone()){
			if (chatMessage.startsWith("Your Barrows chest count is"))
			{
				Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
				if (m.find())
				{
					forceDanceTick = client.getTickCount() + DANCE_DURATION_BARROWS;
					return;
				}
			}

			if (chatMessage.startsWith("Your completed Chambers of Xeric count is:")
				|| chatMessage.startsWith("Your completed Chambers of Xeric Challenge Mode count is:")
				|| chatMessage.startsWith("Your completed Theatre of Blood count is:"))
			{
				Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
				if (m.find())
				{
					forceDanceTick = client.getTickCount() + DANCE_DURATION_RAIDDONE;
					return;
				}
			}
		}

		if (config.partyOnPetDrop() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
		{
			forceDanceTick = client.getTickCount() + DANCE_DURATION_PETDROP;
			return;
		}

		if (config.partyOnBossKill())
		{
			Matcher m = BOSSKILL_MESSAGE_PATTERN.matcher(chatMessage);
			if (m.matches())
			{
				forceDanceTick = client.getTickCount() + DANCE_DURATION_BOSSKILL;
			}
		}
	}

	void applyAnimationIfPossible(Player player)
	{
		if (player.getAnimation() != -1
			|| config.danceChance() < rand.nextInt(100)
			|| config.disableInPvp() && client.getVar(Varbits.PVP_SPEC_ORB) == 1
			|| ((config.partyOnBossKill() || config.partyOnLevelup() || config.partyOnPetDrop() || config.partyOnRaidDone())
				&& forceDanceTick < client.getTickCount()))
		{
			return;
		}

		List<Move> moves = new ArrayList<>(Arrays.asList(Move.values()));

		for (int i = moves.size() - 1; i >= 0; i--)
		{
			if (!moves.get(i).isEnabled(config)) {
				moves.remove(i);
			}
		}

		if (moves.size() == 0)
		{
			return;
		}

		Move move = moves.get(rand.nextInt(moves.size()));
		player.setAnimation(move.getAnimId());
		player.setActionFrame(0);

		if (move.getGfxId() != -1)
		{
			player.setGraphic(move.getGfxId());
			player.setSpotAnimFrame(0);
		}
	}

	@Provides
	DancePartyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DancePartyConfig.class);
	}
}
