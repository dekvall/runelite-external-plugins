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
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.SpriteID;
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

	private final static int COMPONENT_WORLD_SWITCHER = 69;
	private final static int COMPONENT_WORLD_SWITCHER_PANEL = 821;
	private final static int COMPONENT_FRIENDS_LIST = 429;

	private final static int[] SPRITE_FLAGS = {
		SpriteID.WORLD_SWITCHER_REGION_USA,
		SpriteID.WORLD_SWITCHER_REGION_UK,
		SpriteID.WORLD_SWITCHER_REGION_AUSTRALIA,
		SpriteID.WORLD_SWITCHER_REGION_GERMANY,
		4936, // US WEST
		4937, // US EAST
	};

	private static final String MENU_ENTRY_HIDDEN = JagexColors.MENU_TARGET_TAG + "XXX" + ColorUtil.CLOSING_COLOR_TAG;
	private static final String WORLD_REGEX = "\\bW\\d{3}\\b";
	private static final String WORLD_REGEX_LONG = "\\bWorld \\d{3}\\b";

	@Inject
	private Client client;

	@Inject
	private WorldHiderConfig config;

	@Inject
	private ClientThread clientThread;

	private Widget worldSwitcherScrollbar;
	private Widget worldSwitcherPanelScrollbar;

	@Override
	protected void startUp()
	{
		log.info("World Hider started!");

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		updateInterfaces();
	}

	@Override
	protected void shutDown()
	{
		log.info("World Hider stopped!");

		updateInterfaces();

		hideWorldSwitcherMenuEntries(false);
		hideWorldSwitcherPanelMenuEntries(false);
		hideScrollbar(false);
		hideToolTip(false);
		updateFriendsListTitle(false);
		updateIgnoreListTitle(false);
	}

	@Provides
	WorldHiderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldHiderConfig.class);
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("worldhider") || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		updateInterfaces();
		hideScrollbar(config.hideScrollbar());
	}

	@Subscribe(priority = -2.2F)
	public void onScriptPostFired(ScriptPostFired event)
	{
		switch (event.getScriptId())
		{
			case ScriptID.FRIENDS_UPDATE:
				updateFriendsListTitle(true);
				break;
			case ScriptID.IGNORE_UPDATE:
				updateIgnoreListTitle(true);
				break;
			case SCRIPT_FRIEND_UPDATE:
				recolorFriends();
				break;
			case SCRIPT_WORLD_SWITCHER_DRAW:
				hideWorldSwitcherWorlds();
				hideConfigurationPanelWorlds();
				// Fall through
			case SCRIPT_WORLD_SWITCHER_TITLE:
				hideWorldSwitcher();
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
		Widget namesContainer = client.getWidget(ComponentID.FRIEND_LIST_NAMES_CONTAINER);

		if (namesContainer == null)
		{
			return;
		}

		Widget[] friends = namesContainer.getDynamicChildren();

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

	private void hideWorldSwitcher()
	{
		Widget worldSwitcher = client.getWidget(WORLD_SWITCHER, 3);

		if (worldSwitcher == null)
		{
			return;
		}

		Widget configPanel = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 0);
		String title;

		if (configPanel == null || configPanel.isHidden())
		{
			title = "Current world - XXX";
		}
		else
		{
			title = ColorUtil.wrapWithColorTag("Configuring...", ColorUtil.fromHex("9F9F9F"));
		}

		worldSwitcher.setText(title);

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

		for (int i = 0; i < entries.length; i++)
		{
			// The world text is the 2nd index per row (player),
			// and there are 4 widgets per row (player) in the list
			if (i % 4 != 1)
			{
				continue;
			}

			Widget entry = entries[i];
			if (entry.getType() == WidgetType.TEXT && entry.getText().matches(WORLD_REGEX))
			{
				if (config.massHide())
				{
					entry.setText("WXXX");
				}
				entry.setTextColor(COLOR);
			}
		}
	}

	private void hideWorldSwitcherWorlds()
	{
		hideWorldSwitcherMenuEntries(config.hideList());

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

		hideToolTip(true);
	}

	private void hideToolTip(boolean hidden)
	{
		Widget worldTooltip = client.getWidget(WORLD_SWITCHER, 26);

		if (worldTooltip != null)
		{
			worldTooltip.setHidden(hidden);
		}
	}

	private void hideConfigurationPanelWorlds()
	{
		hideWorldSwitcherPanelMenuEntries(config.hideConfigurationPanel());

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

	private void hideWorldSwitcherMenuEntries(boolean hidden)
	{
		Widget worldList = client.getWidget(ComponentID.WORLD_SWITCHER_WORLD_LIST);
		updateMenuEntriesOfDynamicChildren(worldList, hidden);
	}

	private void hideWorldSwitcherPanelMenuEntries(boolean hidden)
	{
		Widget clickableList = client.getWidget(COMPONENT_WORLD_SWITCHER_PANEL, 20);
		updateMenuEntriesOfDynamicChildren(clickableList, hidden);
	}

	private void updateMenuEntriesOfDynamicChildren(Widget container, boolean hidden)
	{
		if (container != null && container.getDynamicChildren() != null)
		{
			Widget[] children = container.getDynamicChildren();
			for (int i = 0; i < children.length; i++)
			{
				Widget w = children[i];
				if (w == null) continue;

				String name = hidden
					? MENU_ENTRY_HIDDEN
					: JagexColors.MENU_TARGET_TAG + i + ColorUtil.CLOSING_COLOR_TAG;
				w.setName(name);
			}
		}
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

	private void updateInterfaces()
	{
		clientThread.invokeLater(() ->
		{
			updateInterface(COMPONENT_WORLD_SWITCHER, 0);
			updateInterface(COMPONENT_WORLD_SWITCHER, 3);
			updateInterface(COMPONENT_WORLD_SWITCHER_PANEL, 1);
			updateInterface(COMPONENT_FRIENDS_LIST, 0);
		});
	}

	private void updateInterface(int group, int child)
	{
		Widget component = client.getWidget(group, child);
		if (component == null)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			Object[] args = component.getOnVarTransmitListener();
			client.runScript(args);
		});
	}

	private void updateFriendsListTitle(boolean hidden)
	{
		Widget friendListTitleWidget = client.getWidget(ComponentID.FRIEND_LIST_TITLE);
		if (friendListTitleWidget != null)
		{
			String title = friendListTitleWidget.getText();
			String newTitle = createNewPlayerListTitle(title, hidden);
			friendListTitleWidget.setText(newTitle);
		}
	}

	private void updateIgnoreListTitle(boolean hidden)
	{
		Widget ignoreListTitleWidget = client.getWidget(ComponentID.IGNORE_LIST_TITLE);
		if (ignoreListTitleWidget != null)
		{
			String title = ignoreListTitleWidget.getText();
			String newTitle = createNewPlayerListTitle(title, hidden);
			ignoreListTitleWidget.setText(newTitle);
		}
	}

	private String createNewPlayerListTitle(String title, boolean hidden)
	{
		final String newTitle;
		if (!hidden)
		{
			int world = client.getWorld();
			newTitle = title.replaceFirst("WXXX", "W" + world)
				.replaceFirst("World XXX", "World " + world);
		}
		else
		{
			newTitle = title.replaceFirst(WORLD_REGEX, "WXXX")
				.replaceFirst(WORLD_REGEX_LONG, "World XXX");
		}
		return newTitle;
	}
}
