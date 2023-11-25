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
import net.runelite.api.ScriptID;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.ColorUtil;

import static net.runelite.api.widgets.InterfaceID.WORLD_SWITCHER;

@Slf4j
@PluginDescriptor(
	name = "World Hider"
)
public class WorldHiderPlugin extends Plugin
{
	private final static int SCRIPT_FRIEND_UPDATE = 125;
	private final static int SCRIPT_GROUPING_REBUILD = 435;
	private final static int SCRIPT_WORLD_SWITCHER_DRAW = 892;
	private final static int SCRIPT_WORLD_SWITCHER_TITLE = 7271;

	private final static int COMPONENT_WORLD_SWITCHER_PANEL = 821;

	private final static int[] SPRITE_FLAGS = {
		SpriteID.WORLD_SWITCHER_REGION_USA,
		SpriteID.WORLD_SWITCHER_REGION_UK,
		SpriteID.WORLD_SWITCHER_REGION_AUSTRALIA,
		SpriteID.WORLD_SWITCHER_REGION_GERMANY,
		4936, // US WEST
		4937, // US EAST
	};

	private final static String MENU_ENTRY_HIDDEN = JagexColors.MENU_TARGET_TAG + "XXX" + ColorUtil.CLOSING_COLOR_TAG;
	private static final String WORLD_REGEX = "^W\\d{1,3}$";

	@Inject
	private Client client;

	@Inject
	private WorldHiderConfig config;

	@Inject
	private ClientThread clientThread;

	private int randomWorld = getRandomWorld();

	private Widget worldSwitcher;
	private Widget worldSwitcherScrollbar;
	private Widget worldSwitcherPanel;
	private Widget worldSwitcherPanelScrollbar;

	@Override
	protected void startUp()
	{
		log.info("World Hider started!");

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			updateInterface(worldSwitcher);
			updateInterface(worldSwitcherPanel);
		});
	}

	@Override
	protected void shutDown()
	{
		log.info("World Hider stopped!");

		clientThread.invokeLater(() ->
		{
			updateInterface(worldSwitcher);
			updateInterface(worldSwitcherPanel);
			resetWorldSwitcherTitle();
		});
		hideScrollbar(false);
	}

	@Provides
	WorldHiderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldHiderConfig.class);
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getKey().equals("worldhider") && client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			updateInterface(worldSwitcher);
			updateInterface(worldSwitcherPanel);
		});
		hideScrollbar(config.hideScrollbar());
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
		final boolean isMember = client.getVarpValue(VarPlayer.MEMBERSHIP_DAYS) > 0;

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
				recolorFriends();
				break;
			case SCRIPT_WORLD_SWITCHER_DRAW:
				getWidgets();
				hideHopperWorlds();
				hideConfigurationPanelWorlds();
				// Fall through
			case SCRIPT_WORLD_SWITCHER_TITLE:
				killWorldHopper();
				break;
			case SCRIPT_GROUPING_REBUILD:
			case ScriptID.CLAN_SIDEPANEL_DRAW:
			case ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD:
				hideCommunityLists();
				break;
		}
	}

	private void recolorFriends()
	{
		Widget friendsList = client.getWidget(ComponentID.FRIEND_LIST_TITLE);

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
		Widget worldHopper = client.getWidget(WORLD_SWITCHER, 3);

		if (worldHopper == null)
		{
			return;
		}

		Widget configPanel = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 0);
		String title;

		if (configPanel == null || configPanel.isHidden())
		{
			title = "Current world - " + (config.randomWorld() ? randomWorld : "XXX");
		}
		else
		{
			title = ColorUtil.wrapWithColorTag("Configuring...", ColorUtil.fromHex("9F9F9F"));
		}

		worldHopper.setText(title);
		worldHopper.setOnVarTransmitListener((Object[]) null);

		Widget worldList = client.getWidget(WORLD_SWITCHER, 18);

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

	private void hideCommunityLists()
	{
		// Default group channel for game activities
		Widget grouping = client.getWidget(76, 16);
		hideCommunityWorlds(grouping);

		// Guest clan chat channel
		Widget guestChannel = client.getWidget(ComponentID.CLAN_GUEST_MEMBERS);
		hideCommunityWorlds(guestChannel);

		// Clan chat channel
		Widget clanChannel = client.getWidget(ComponentID.CLAN_MEMBERS);
		hideCommunityWorlds(clanChannel);

		// Friends chat channel
		Widget chatChannel = client.getWidget(ComponentID.FRIENDS_CHAT_LIST);
		hideCommunityWorlds(chatChannel);
	}

	private void hideCommunityWorlds(Widget widget)
	{
		if (widget == null)
		{
			return;
		}

		Widget[] entries = widget.getDynamicChildren();
		final int COLOR = widget.getId() == ComponentID.FRIENDS_CHAT_LIST ? 0xFFFF64 : 0xFFFF00;

		for (Widget entry : entries)
		{
			if (entry.getText().matches(WORLD_REGEX) && entry.getType() == WidgetType.TEXT)
			{
				if (config.massHide())
				{
					entry.setText("WXXX");
				}
				entry.setTextColor(COLOR);
			}
		}
	}

	private void hideHopperWorlds()
	{
		Widget worlds = client.getWidget(ComponentID.WORLD_SWITCHER_WORLD_LIST);
		hideWorldMenuEntries(worlds, config.hideList());

		if (!config.hideList())
		{
			return;
		}

		Widget list = client.getWidget(WORLD_SWITCHER, 19);
		hideWorldInfo(list);

		worldSwitcherScrollbar = client.getWidget(WORLD_SWITCHER, 20);
		hideScrollbar(config.hideScrollbar());

		Widget bottomContainer = client.getWidget(WORLD_SWITCHER, 21);
		hideFavorites(bottomContainer);

		Widget worldTooltip = client.getWidget(WORLD_SWITCHER, 26);

		if (worldTooltip != null)
		{
			worldTooltip.setHidden(true);
		}
	}

	private void hideConfigurationPanelWorlds()
	{
		Widget worlds = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 20);
		hideWorldMenuEntries(worlds, config.hideConfigurationPanel());

		if (!config.hideConfigurationPanel())
		{
			return;
		}

		Widget list = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 21);
		hideWorldInfo(list);

		worldSwitcherPanelScrollbar = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 22);
		hideScrollbar(config.hideScrollbar());

		Widget favorites = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 23);
		hideFavorites(favorites);
	}

	private void hideWorldMenuEntries(Widget worldList, boolean hidden)
	{
		if (worldList == null || worldList.getDynamicChildren() == null)
		{
			return;
		}

		Arrays.stream(worldList.getDynamicChildren()).forEach(w ->
		{
			if (hidden)
			{
				w.setName(MENU_ENTRY_HIDDEN);
			}
			else
			{
				int world = Arrays.asList(worldList.getDynamicChildren()).indexOf(w);
				String menuEntry = JagexColors.MENU_TARGET_TAG + world + ColorUtil.CLOSING_COLOR_TAG;
				w.setName(menuEntry);
			}
		});
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
			else if (config.hideFlags()
				&& widget.getType() == WidgetType.GRAPHIC
				&& Arrays.stream(SPRITE_FLAGS).anyMatch(s -> s == widget.getSpriteId()))
			{
				widget.setSpriteId(SpriteID.WORLD_SWITCHER_REGION_NONE);
			}
		}
	}

	private void hideScrollbar(boolean hidden)
	{
		hideScrollbar(worldSwitcherScrollbar, (hidden && config.hideList()));
		hideScrollbar(worldSwitcherPanelScrollbar, (hidden && config.hideConfigurationPanel()));
	}

	private void hideScrollbar(Widget widget, boolean hidden)
	{
		if (widget == null)
		{
			return;
		}

		Widget[] scrollbarComponents = widget.getDynamicChildren();

		// This widget is expected to have 6 children,
		// check anyway to prevent potential ArrayIndexOutOfBoundsException
		if (scrollbarComponents.length == 6)
		{
			scrollbarComponents[1].setSpriteId(hidden ? -1 : SpriteID.SCROLLBAR_THUMB_MIDDLE);  // Middle of the scroll thumb
			scrollbarComponents[2].setSpriteId(hidden ? -1 : SpriteID.SCROLLBAR_THUMB_TOP);     // Top of the scroll thumb
			scrollbarComponents[3].setSpriteId(hidden ? -1 : SpriteID.SCROLLBAR_THUMB_BOTTOM);  // Bottom of the scroll thumb
		}
	}

	private void hideFavorites(Widget widget)
	{
		if (widget == null || !config.hideFavorites())
		{
			return;
		}

		Widget[] favorites = widget.getStaticChildren();

		for (Widget world : favorites)
		{
			hideWorldInfo(world);
		}
	}

	private void updateInterface(Widget component)
	{
		if (component == null || component.isHidden())
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			Object[] args = component.getOnVarTransmitListener();
			client.runScript(args);
		});
	}

	private void resetWorldSwitcherTitle()
	{
		Widget worldSwitcherTitle = client.getWidget(WORLD_SWITCHER, 3);

		if (worldSwitcherTitle == null)
		{
			return;
		}

		// Hardcoded because the ops are set to null while the plugin is enabled
		// So running the script with args pulled from the component would equal to nop
		final int SCRIPT_UPDATE_CURRENT_WORLD_TITLE = 7270;
		final int COMPONENT = 4521987;

		client.runScript(SCRIPT_UPDATE_CURRENT_WORLD_TITLE, COMPONENT);
	}

	private void getWidgets()
	{
		worldSwitcher = client.getWidget(WORLD_SWITCHER, 0);
		worldSwitcherPanel = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 1);
	}

	private int getRandomWorld()
	{
		return ThreadLocalRandom.current().nextInt(301, 500);
	}

	private void setFriendsListTitle(final String title)
	{
		Widget friendListTitleWidget = client.getWidget(ComponentID.FRIEND_LIST_TITLE);
		if (friendListTitleWidget != null)
		{
			friendListTitleWidget.setText(title);
		}
	}

	private void setIgnoreListTitle(final String title)
	{
		Widget ignoreTitleWidget = client.getWidget(ComponentID.IGNORE_LIST_TITLE);
		if (ignoreTitleWidget != null)
		{
			ignoreTitleWidget.setText(title);
		}
	}
}
