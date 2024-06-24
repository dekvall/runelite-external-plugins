/*
 * Copyright (c) 2020, dekvall
 * Copyright (c) 2024, Macweese <https://github.com/Macweese>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.dkvl.largelogout;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Large Logout"
)
public class LargeLogoutPlugin extends Plugin
{
	enum State
	{
		HIDDEN,
		VISIBLE,
		SHUTDOWN
	}

	boolean hasFavorite1;
	boolean hasFavorite2;

	@Inject
	private Client client;

	@Inject
	@Getter
	private LargeLogoutConfig config;

	@Inject
	private ClientThread clientThread;

	private static InterfaceManager interfaceManager;

	@Override
	protected void startUp()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		interfaceManager = new InterfaceManager(client, this);
		clientThread.invokeLater(() ->
		{
			hasFavorite1 = client.getVarbitValue(Varbits.WORLDHOPPER_FAVORITE_1) != 0;
			hasFavorite2 = client.getVarbitValue(Varbits.WORLDHOPPER_FAVORITE_2) != 0;
			interfaceManager.enlargeLogoutButton();
			interfaceManager.enlargeLogoutWorldSwitcher();
			interfaceManager.redraw();
			interfaceManager.refreshScrollbar();
		});
	}

	@Override
	protected void shutDown()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (interfaceManager == null)
		{
			interfaceManager = new InterfaceManager(client, this);
		}
		clientThread.invokeLater(() ->
		{
			interfaceManager.restoreLogout();
			interfaceManager.restoreLogoutWorldSwitcher();
			interfaceManager.restoreWidget(client.getWidget(Widgets.WORLD_SWITCHER_FAVORITE_1), WidgetProperty.WORLD_SWITCHER_FAVORITE_1, State.SHUTDOWN);
			interfaceManager.restoreWidget(client.getWidget(Widgets.WORLD_SWITCHER_FAVORITE_2), WidgetProperty.WORLD_SWITCHER_FAVORITE_2, State.SHUTDOWN);
			interfaceManager.redraw();
			interfaceManager.refreshScrollbar();
		});
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("largelogout") || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			interfaceManager.restoreLogoutWorldSwitcher();
			interfaceManager.enlargeLogoutWorldSwitcher();
			interfaceManager.redraw();
			interfaceManager.refreshScrollbar();
		});
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// The order of these events matter
		if (event.getScriptId() == Scripts.LOGOUT_LAYOUT_UPDATE)
		{
			if (interfaceManager == null)
			{
				interfaceManager = new InterfaceManager(client, this);
			}
			interfaceManager.enlargeLogoutButton();
			interfaceManager.restoreLogoutWorldSwitcher();
		}
		if (event.getScriptId() == Scripts.WORLD_SWITCHER_DRAW
			|| event.getScriptId() == Scripts.WORLD_SWITCHER_INIT
			|| event.getScriptId() == Scripts.WORLD_SWITCHER_LOADED)
		{
			if (interfaceManager == null)
			{
				interfaceManager = new InterfaceManager(client, this);
			}
			interfaceManager.enlargeLogoutWorldSwitcher();
		}
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == Varbits.WORLDHOPPER_FAVORITE_1 || event.getVarbitId() == Varbits.WORLDHOPPER_FAVORITE_2)
		{
			hasFavorite1 = client.getVarbitValue(Varbits.WORLDHOPPER_FAVORITE_1) != 0;
			hasFavorite2 = client.getVarbitValue(Varbits.WORLDHOPPER_FAVORITE_2) != 0;

			if (interfaceManager == null)
			{
				interfaceManager = new InterfaceManager(client, this);
			}

			if (hasFavorite1 || hasFavorite2)
			{
				interfaceManager.restoreLogoutWorldSwitcher();
				if (config.enlargeWorldSwitcherLogout() == WorldSwitcherMode.NO_FAVORITES)
				{
					return;
				}
			}

			interfaceManager.enlargeLogoutWorldSwitcher();
		}
	}

	@Provides
	LargeLogoutConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LargeLogoutConfig.class);
	}
}
