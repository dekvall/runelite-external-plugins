/*
 * Copyright (c) 2020, dekvall <https://github.com/dekvall>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dekvall.worldhider;

import com.google.inject.Provides;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.Ignore;
import net.runelite.api.NameableContainer;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "World Hider"
)
public class WorldHiderPlugin extends Plugin
{
	private final static int WORLD_HOPPER_BUILD = 892;
	private final static int DRAW_FRIEND_ENTRIES = 125;
	private final static int BUILD_CC = 1658;


	@Inject
	private Client client;

	@Inject
	private WorldHiderConfig config;

	@Inject
	private ClientThread clientThread;

	private int randomWorld = getRandomWorld();

	@Override
	protected void startUp() throws Exception
    {
		log.info("World Hider started!");
    }

	@Override
	protected void shutDown() throws Exception
	{
		log.info("World Hider stopped!");
	}

	@Provides
	WorldHiderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldHiderConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			randomWorld = getRandomWorld();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		// The friends list plugin interferes with this, so i run it a lot
		final boolean isMember = client.getVar(VarPlayer.MEMBERSHIP_DAYS) > 0;

		final NameableContainer<Friend> friendContainer = client.getFriendContainer();
		final int friendCount = friendContainer.getCount();
		if (friendCount >= 0)
		{
			final int limit = isMember ? 400 : 200;

			final String title = "Friends - W" +
				(config.randomWorld() ? randomWorld : "XXX") +
				" (" +
				friendCount +
				"/" +
				limit +
				")";

			setFriendsListTitle(title);
		}

		final NameableContainer<Ignore> ignoreContainer = client.getIgnoreContainer();
		final int ignoreCount = ignoreContainer.getCount();
		if (ignoreCount >= 0)
		{
			final int limit = isMember ? 400 : 200;

			final String title = "Ignores - W" +
				(config.randomWorld() ? randomWorld : "XXX") +
				" (" +
				ignoreCount +
				"/" +
				limit +
				")";

			setIgnoreListTitle(title);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		switch (event.getScriptId())
		{
			case DRAW_FRIEND_ENTRIES:
				clientThread.invokeLater(this::recolorFriends);
				break;
			case WORLD_HOPPER_BUILD:
				clientThread.invokeLater(this::killWorldHopper);
				break;
			case BUILD_CC:
				clientThread.invokeLater(this::hideClanWorlds);
				break;
		}
	}

	private void recolorFriends()
	{
		Widget friendsList = client.getWidget(429, 11);

		if (friendsList == null)
		{
			return;
		}

		Widget[] friends = friendsList.getDynamicChildren();

		for (int i = 0; i < friends.length; i+=2)
		{
			if (!friends[i].getText().contains("Offline") && friends[i].getName().isEmpty())
			{
				if (config.massHide())
				{
					friends[i].setText("World XXX");
				}
				friends[i].setTextColor(0xFFFF00);
			}
		}
	}

	private void killWorldHopper()
	{
		Widget worldHopper = client.getWidget(69, 2);

		if (worldHopper == null)
		{
			return;
		}

		worldHopper.setText("Current World - " + (config.randomWorld() ? randomWorld : "XXX"));

		Widget worldList = client.getWidget(69, 17);

		if (worldList == null)
		{
			return;
		}

		for (Widget entry : worldList.getDynamicChildren())
		{
			if (entry.getTextColor() == 0xDC10D)
			{
				entry.setTextColor(0);
			}
		}
	}

	private void hideClanWorlds()
	{
		Widget clan = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);

		if (clan == null)
		{
			return;
		}

		Widget[] entries = clan.getDynamicChildren();

		for (Widget entry : entries)
		{
			if (entry.getText().startsWith("World"))
			{
				if (config.massHide())
				{
					entry.setText("World XXX");
				}
				entry.setTextColor(0xFFFF64);
			}
		}
	}

	private int getRandomWorld()
	{
		return ThreadLocalRandom.current().nextInt(301, 500);
	}

	private void setFriendsListTitle(final String title)
	{
		Widget friendListTitleWidget = client.getWidget(WidgetInfo.FRIEND_CHAT_TITLE);
		if (friendListTitleWidget != null)
		{
			friendListTitleWidget.setText(title);
		}
	}

	private void setIgnoreListTitle(final String title)
	{
		Widget ignoreTitleWidget = client.getWidget(WidgetInfo.IGNORE_TITLE);
		if (ignoreTitleWidget != null)
		{
			ignoreTitleWidget.setText(title);
		}
	}
}
