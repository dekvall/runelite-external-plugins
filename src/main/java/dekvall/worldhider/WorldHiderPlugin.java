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
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.Ignore;
import net.runelite.api.NameableContainer;
import net.runelite.api.SpriteID;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.ColorUtil;

import static net.runelite.api.widgets.WidgetID.WORLD_SWITCHER_GROUP_ID;

@Slf4j
@PluginDescriptor(
	name = "World Hider"
)
public class WorldHiderPlugin extends Plugin
{
	private final static int SCRIPT_FRIEND_UPDATE = 125;
	private final static int SCRIPT_WORLD_SWITCHER_DRAW = 892;
	private final static int SCRIPT_WORLD_SWITCHER_TITLE = 7271;

	private final static int SCRIPT_BUILD_CC = 1658;
	private final static int VARP_MEMBERSHIP_DAYS = 1780;

	private final static int[] SPRITE_FLAGS = {
		SpriteID.WORLD_SWITCHER_REGION_USA,
		SpriteID.WORLD_SWITCHER_REGION_UK,
		SpriteID.WORLD_SWITCHER_REGION_AUSTRALIA,
		SpriteID.WORLD_SWITCHER_REGION_GERMANY,
		4936, // US WEST
		4937, // US EAST
	};

	private final static String MENU_ENTRY_HIDDEN = JagexColors.MENU_TARGET_TAG + "XXX" + "</col>";

	@Inject
	private Client client;

	@Inject
	private WorldHiderConfig config;

	@Inject
	private ClientThread clientThread;

	private int randomWorld = getRandomWorld();

	@Override
	protected void startUp()
	{
		log.info("World Hider started!");
	}

	@Override
	protected void shutDown()
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
		final boolean isMember = client.getVarpValue(VARP_MEMBERSHIP_DAYS) > 0;

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
			case SCRIPT_FRIEND_UPDATE:
				clientThread.invokeLater(this::recolorFriends);
				break;
			case SCRIPT_WORLD_SWITCHER_DRAW:
				clientThread.invoke(this::hideHopperWorlds);
				clientThread.invoke(this::hideConfigurationPanelWorlds);
				// Fall through
			case SCRIPT_WORLD_SWITCHER_TITLE:
				clientThread.invoke(this::killWorldHopper);
				break;
			case SCRIPT_BUILD_CC:
				clientThread.invoke(this::hideClanWorlds);
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
		Widget worldHopper = client.getWidget(WORLD_SWITCHER_GROUP_ID, 3);

		if (worldHopper == null)
		{
			return;
		}

		Widget configPanel = client.getWidget(821, 0);
		String title;

		if (configPanel == null || configPanel.isHidden())
		{
			title = "Current world - " + (config.randomWorld() ? randomWorld : "XXX");
		}
		else
		{
			title = ColorUtil.wrapWithColorTag("Configuring...", ColorUtil.fromHex("9F9F9F"));
		}
		worldHopper.setOnVarTransmitListener((Object[]) null);

		Widget worldList = client.getWidget(WORLD_SWITCHER_GROUP_ID, 18);

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
			if (entry.getText().startsWith("W"))
			{
				if (config.massHide())
				{
					entry.setText("WXXX");
				}
				entry.setTextColor(0xFFFF64);
			}
		}
	}

	private void hideHopperWorlds()
	{
		if (!config.hideList())
		{
			return;
		}

		Widget list = client.getWidget(WORLD_SWITCHER_GROUP_ID, 19);
		hideWorldInfo(list);

		Widget worlds = client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST);
		hideWorldMenuEntries(worlds);

		Widget scrollbar = client.getWidget(WORLD_SWITCHER_GROUP_ID, 20);
		hideScrollbar(scrollbar);

		Widget worldTooltip = client.getWidget(WORLD_SWITCHER_GROUP_ID, 26);

		if (worldTooltip != null)
		{
			worldTooltip.setHidden(true);
		}
	}

	// This is for the interface that opens when
	// opening the world switcher settings
	private void hideConfigurationPanelWorlds()
	{
		if (!config.hideConfigurationPanel())
		{
			return;
		}

		Widget list = client.getWidget(821, 21);
		hideWorldInfo(list);

		Widget scrollbar = client.getWidget(821, 22);
		hideScrollbar(scrollbar);

		Widget worlds = client.getWidget(821, 20);
		hideWorldMenuEntries(worlds);
	}

	private void hideWorldMenuEntries(Widget worldList)
	{
		if (worldList == null || worldList.getDynamicChildren() == null)
		{
			return;
		}

		Arrays.stream(worldList.getDynamicChildren()).forEach(w -> w.setName(MENU_ENTRY_HIDDEN));
	}

	private void hideWorldInfo(Widget worldList)
	{
		if (worldList == null)
		{
			return;
		}

		Widget[] world = worldList.getDynamicChildren();

		for (Widget widget : world)
		{
			if (widget.getType() == WidgetType.TEXT)
			{
				widget.setText("XXX");
			}
			else if (widget.getType() == WidgetType.GRAPHIC && Arrays.stream(SPRITE_FLAGS).anyMatch(s -> s == widget.getSpriteId()) && config.hideFlags())
			{
				widget.setSpriteId(SpriteID.WORLD_SWITCHER_REGION_NONE);
			}
		}
	}

	private void hideScrollbar(Widget scrollbar)
	{
		if (scrollbar == null || !config.hideScrollbar())
		{
			return;
		}

		Widget[] scrollbarComponents = scrollbar.getDynamicChildren();

		// This widget is expected to have 6 children,
		// check anyway to prevent potential ArrayIndexOutOfBoundsException
		if (scrollbarComponents.length == 6)
		{
			scrollbarComponents[1].setSpriteId(-1);   // Middle of the scroll thumb
			scrollbarComponents[2].setSpriteId(-1);   // Top of the scroll thumb
			scrollbarComponents[3].setSpriteId(-1);   // Bottom of the scroll thumb
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
