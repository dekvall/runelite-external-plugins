package dekvall.danceparty;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
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
	@Inject
	private Client client;

	@Inject
	private DancePartyConfig config;

	private Set<Player> players = new HashSet<>();

	private Random rand = new Random();

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
	public void onGameTick(GameTick tick)
	{
		if (config.disableInPvp() && client.getVar(Varbits.PVP_SPEC_ORB) == 1)
		{
			return;
		}

		for (Player player : players)
		{
			if (player.getAnimation() == -1)
			{
				if (config.workoutMode())
				{
					setPlayerMoveFrom(WorkoutMove.values(), player);
				}
				else
				{
					setPlayerMoveFrom(DanceMove.values(), player);
				}
			}
		}
	}

	private void setPlayerMoveFrom(Move [] moves, Player player)
	{
		int anim = moves[rand.nextInt(moves.length)].getAnimId();
		player.setAnimation(anim);
		player.setActionFrame(0);
	}

	@Provides
	DancePartyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DancePartyConfig.class);
	}
}
