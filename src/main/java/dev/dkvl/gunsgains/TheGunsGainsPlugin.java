package dev.dkvl.gunsgains;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "The Guns' Gains"
)
public class TheGunsGainsPlugin extends Plugin
{
	private static ZonedDateTime RELEASE_DATE = ZonedDateTime.of(
		2007, 2, 19, 11, 0, 0, 0, ZoneId.of("GMT"));

	private NPC theGuns;

	@Override
	protected void startUp() throws Exception
	{
		log.info("The Guns' Gains started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		theGuns = null;
		log.info("The Guns' Gains stopped!");
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (event.getNpc().getId() == NpcID.THE_GUNS)
		{
			theGuns = event.getNpc();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc().getId() == NpcID.THE_GUNS)
		{
			theGuns = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (theGuns != null)
		{
			theGuns.setOverheadText(calculateRepCount());
		}
	}

	private String calculateRepCount()
	{
		// 100 reps every 3 mins
		long millis = ChronoUnit.MILLIS.between(RELEASE_DATE, ZonedDateTime.now(ZoneId.of("GMT")));
		long reps = millis * 100 / 180_000;
		return NumberFormat.getNumberInstance(Locale.US).format(reps);
	}
}
