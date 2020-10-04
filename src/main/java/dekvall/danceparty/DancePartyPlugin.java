package dekvall.danceparty;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Dance Party"
)
public class DancePartyPlugin extends Plugin
{
	private static int LEVELUP_DANCE_DURATION = 40; // Ticks
	@Inject
	private Client client;

	@Inject
	private DancePartyConfig config;

	private Set<Player> players = new HashSet<>();

	private Random rand = new Random();

	private int levelUpTick;

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
				levelUpTick = client.getTickCount();
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

	void applyAnimationIfPossible(Player player)
	{
		if (player.getAnimation() != -1
			|| config.disableInPvp() && client.getVar(Varbits.PVP_SPEC_ORB) == 1
			|| config.partyOnLevelup()
				&& (levelUpTick == 0 || client.getTickCount() - levelUpTick > LEVELUP_DANCE_DURATION))
		{
			return;
		}

		if (config.workoutMode())
		{
			setPlayerMoveFrom(WorkoutMove.values(), player);
		}
		else
		{
			setPlayerMoveFrom(DanceMove.values(), player);
		}
	}

	private void setPlayerMoveFrom(Move [] moves, Player player)
	{
		Move move = moves[rand.nextInt(moves.length)];
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
