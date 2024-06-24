/*
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

import java.util.Arrays;
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;

public class InterfaceManager
{
	private static final int ORIG_LOGOUT_BUTTON_HEIGHT = 36;
	private static final int WIDGET_SPACING = 10;

	private Widget worldSwitcher;
	private Widget worldSwitcherWindow;
	private Widget worldSwitcherWindowSub;
	private Widget worldSwitcherSeparator;
	private Widget worldSwitcherListContainer;
	private Widget worldSwitcherList;
	private Widget worldSwitcherScrollbar;
	private Widget worldSwitcherBottomPanel;
	private Widget worldSwitcherPlaceholderText;
	private Widget worldSwitcherFavorite1;
	private Widget worldSwitcherFavorite2;
	private Widget worldSwitcherLogout;

	private Widget logoutLayout;
	private Widget buttonPane;
	private Widget infoText;
	private Widget switchButton;
	private Widget logoutButton;
	private Widget logoutButtonC;
	private Widget logoutButtonL;
	private Widget logoutButtonR;
	private Widget logoutButtonText;
	private Widget reviewPane;

	private static Widget worldSwitcherLogoutSeparator;
	private static Widget worldSwitcherLogoutButtonL;
	private static Widget worldSwitcherLogoutButtonC;
	private static Widget worldSwitcherLogoutButtonR;
	private static Widget worldSwitcherLogoutButtonText;
	private static Widget worldSwitcherLogoutButtonBackground;

	@Inject
	private Client client;

	private LargeLogoutPlugin plugin;

	private LargeLogoutConfig config;

	InterfaceManager(Client client, LargeLogoutPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		config = plugin.getConfig();
		load();
	}

	void load()
	{
		worldSwitcher = client.getWidget(Widgets.WORLD_SWITCHER);
		worldSwitcherWindow = client.getWidget(Widgets.WORLD_SWITCHER_WINDOW);
		worldSwitcherWindowSub = client.getWidget(Widgets.WORLD_SWITCHER_WINDOW_SUB);
		worldSwitcherSeparator = client.getWidget(Widgets.WORLD_SWITCHER_SEPARATOR);
		worldSwitcherListContainer = client.getWidget(Widgets.WORLD_SWITCHER_LIST_CONTAINER);
		worldSwitcherList = client.getWidget(Widgets.WORLD_SWITCHER_WORLD_LIST);
		worldSwitcherScrollbar = client.getWidget(Widgets.WORLD_SWITCHER_SCROLLBAR);
		worldSwitcherBottomPanel = client.getWidget(Widgets.WORLD_SWITCHER_BOTTOM_PANEL);
		worldSwitcherPlaceholderText = client.getWidget(Widgets.WORLD_SWITCHER_PLACEHOLDER_TEXT);
		worldSwitcherFavorite1 = client.getWidget(Widgets.WORLD_SWITCHER_FAVORITE_1);
		worldSwitcherFavorite2 = client.getWidget(Widgets.WORLD_SWITCHER_FAVORITE_2);
		worldSwitcherLogout = client.getWidget(Widgets.WORLD_SWITCHER_LOGOUT);

		logoutLayout = client.getWidget(Widgets.LOGOUT_LAYOUT);
		buttonPane = client.getWidget(Widgets.BUTTON_PANE);
		infoText = client.getWidget(Widgets.INFO_TEXT);
		switchButton = client.getWidget(Widgets.SWITCH_BUTTON);
		logoutButton = client.getWidget(Widgets.LOGOUT_BUTTON);
		logoutButtonC = client.getWidget(Widgets.LOGOUT_BUTTON_C);
		logoutButtonL = client.getWidget(Widgets.LOGOUT_BUTTON_L);
		logoutButtonR = client.getWidget(Widgets.LOGOUT_BUTTON_R);
		logoutButtonText = client.getWidget(Widgets.LOGOUT_BUTTON_TEXT);
		reviewPane = client.getWidget(Widgets.REVIEW_PANE);
	}

	private void restoreInterface()
	{
		load();
		restoreWidget(worldSwitcherWindow, WidgetProperty.WORLD_SWITCHER_WINDOW);
		restoreWidget(worldSwitcherWindowSub, null);
		restoreWidget(worldSwitcherSeparator, WidgetProperty.WORLD_SWITCHER_SEPARATOR);
		restoreWidget(worldSwitcherBottomPanel, WidgetProperty.WORLD_SWITCHER_BOTTOM_PANEL);
		restoreWidget(worldSwitcherPlaceholderText, WidgetProperty.WORLD_SWITCHER_PLACEHOLDER_TEXT);
		restoreWidget(worldSwitcherFavorite1, WidgetProperty.WORLD_SWITCHER_FAVORITE_1);
		restoreWidget(worldSwitcherFavorite2, WidgetProperty.WORLD_SWITCHER_FAVORITE_2);
		restoreWidget(worldSwitcherLogout, WidgetProperty.WORLD_SWITCHER_LOGOUT);
		restoreWidget(worldSwitcherListContainer, null);
		restoreWidget(worldSwitcherList, null);
		restoreWidget(worldSwitcherScrollbar, null);
		if (worldSwitcherScrollbar != null)
		{
			Arrays.stream(worldSwitcherScrollbar.getDynamicChildren()).filter(Objects::nonNull).forEach(w -> restoreWidget(w, null));
		}
	}

	void restoreLogout()
	{
		load();
		restoreWidget(buttonPane, WidgetProperty.BUTTON_PANE);
		restoreWidget(infoText, WidgetProperty.INFO_TEXT);
		restoreWidget(switchButton, WidgetProperty.SWITCH_BUTTON);
		restoreWidget(logoutButton, WidgetProperty.LOGOUT_BUTTON);
		restoreWidget(logoutButtonC, WidgetProperty.LOGOUT_BUTTON_C);
		restoreWidget(logoutButtonL, WidgetProperty.LOGOUT_BUTTON_L);
		restoreWidget(logoutButtonR, WidgetProperty.LOGOUT_BUTTON_R);
		restoreWidget(logoutButtonText, WidgetProperty.LOGOUT_BUTTON_TEXT);
		restoreWidget(reviewPane, null);

		if (logoutLayout == null)
		{
			return;
		}

		Object[] args = logoutLayout.getOnLoadListener();
		// adjust for int overflow
		for (int i = 0; i < args.length; i++)
		{
			int j = (Integer) args[i];
			args[i] = (j < 0) ? (Integer.MAX_VALUE & j) : j;
		}
		client.runScript(args);
	}

	private void restoreWidget(Widget widget, WidgetProperty widgetProperty)
	{
		restoreWidget(widget, widgetProperty, LargeLogoutPlugin.State.HIDDEN);
	}

	void restoreWidget(Widget widget, WidgetProperty widgetProperty, LargeLogoutPlugin.State state)
	{
		if (widget == null)
		{
			return;
		}

		boolean hidden = false;

		if (widgetProperty == WidgetProperty.WORLD_SWITCHER_FAVORITE_1)
		{
			hidden = !plugin.hasFavorite1;
		}
		else if (widgetProperty == WidgetProperty.WORLD_SWITCHER_FAVORITE_2)
		{
			hidden = !plugin.hasFavorite2;
		}
		else if (widgetProperty == WidgetProperty.WORLD_SWITCHER_PLACEHOLDER_TEXT)
		{
			hidden = plugin.hasFavorite1 || plugin.hasFavorite2;
		}

		// stupid hack
		// if favorite world slot is hidden and the plugin is disabled,
		// the favorite world slot will remain hidden even after un-/marking a (new) world as favorite,
		// so the favorite world slots must be unhidden on shutdown even if they are empty
		if (state == LargeLogoutPlugin.State.SHUTDOWN)
		{
			hidden = false;
		}

		if (widgetProperty != null)
		{
			widget.setType(widgetProperty.getType());
			widget.setContentType(widgetProperty.getContentType());
			widget.setHidden(hidden);
			widget.setText(widgetProperty.getText());
			widget.setTextColor(widgetProperty.getTextColor());
			widget.setOpacity(widgetProperty.getOpacity());
			widget.setSpriteId(widgetProperty.getSpriteId());
			widget.setSpriteTiling(widgetProperty.isSpriteTiling());
			widget.setOriginalX(widgetProperty.getOriginalX());
			widget.setOriginalY(widgetProperty.getOriginalY());
			widget.setOriginalWidth(widgetProperty.getOriginalWidth());
			widget.setOriginalHeight(widgetProperty.getOriginalHeight());
			widget.setXPositionMode(widgetProperty.getXPositionMode());
			widget.setYPositionMode(widgetProperty.getYPositionMode());
			widget.setWidthMode(widgetProperty.getWidthMode());
			widget.setHeightMode(widgetProperty.getHeightMode());
		}

		widget.revalidate();
	}

	void redraw()
	{
		load();
		if (worldSwitcher == null)
		{
			return;
		}
		final Object[] args = worldSwitcher.getOnVarTransmitListener();
		client.runScript(args);
	}

	void refreshScrollbar()
	{
		load();
		if (worldSwitcherList == null || worldSwitcherScrollbar == null)
		{
			return;
		}
		final int thumbPos = client.getVarcIntValue(69);

		final Object[] args = new Object[]{Scripts.SCROLLBAR_RESIZE, worldSwitcherScrollbar.getId(), worldSwitcherList.getId(), thumbPos};
		client.runScript(args);
	}

	void redrawFavorites()
	{
		worldSwitcherFavorite1.setHidden(!plugin.hasFavorite1)
			.setOriginalX(0)
			.setOriginalY(2383)
			.setOriginalWidth(0)
			.setOriginalHeight(16)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.BOTTOM_16384THS)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.ABSOLUTE);
		worldSwitcherFavorite1.revalidate();

		worldSwitcherFavorite2.setHidden(!plugin.hasFavorite2)
			.setOriginalX(0)
			.setOriginalY(0)
			.setOriginalWidth(0)
			.setOriginalHeight(16)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.BOTTOM_16384THS)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.ABSOLUTE);
		worldSwitcherFavorite2.revalidate();
	}

	void enlargeLogoutWorldSwitcher()
	{
		load();
		if (worldSwitcherLogout == null)
		{
			return;
		}

		worldSwitcherLogout.setSpriteId(-1)
			.setXPositionMode(0)
			.setYPositionMode(0)
			.setOriginalWidth(185)
			.setOriginalHeight(75)
			.revalidate();

		if (worldSwitcherPlaceholderText != null)
		{
			worldSwitcherPlaceholderText.setHidden(true);
		}

		redrawFavorites();

		if (config.enlargeWorldSwitcherLogout() == WorldSwitcherMode.NEVER
			|| (config.enlargeWorldSwitcherLogout() == WorldSwitcherMode.NO_FAVORITES && (plugin.hasFavorite1 || plugin.hasFavorite2)))
		{
			restoreLogoutWorldSwitcher();
			return;
		}

		if (worldSwitcherBottomPanel == null)
		{
			return;
		}
		worldSwitcherBottomPanel.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setOriginalY(148)
			.setOriginalHeight(113)
			.revalidate();

		if (worldSwitcherWindow != null)
		{
			worldSwitcherWindow.setOriginalHeight(140).revalidate();
		}
		if (worldSwitcherWindowSub != null)
		{
			worldSwitcherWindowSub.revalidate();

			worldSwitcherListContainer.revalidate();

			for (Widget child : worldSwitcherListContainer.getStaticChildren())
			{
				child.revalidate();
			}
			for (Widget child : worldSwitcherScrollbar.getDynamicChildren())
			{
				child.revalidate();
			}
		}

		if (worldSwitcherLogoutButtonBackground == null)
		{
			worldSwitcherLogoutButtonBackground = worldSwitcherBottomPanel.createChild(WidgetType.GRAPHIC);
		}
		worldSwitcherLogoutButtonBackground.setType(WidgetType.RECTANGLE);
		worldSwitcherLogoutButtonBackground.setHidden(false)
			.setContentType(0)
			.setTextColor(0xFFFFFF)
			.setOpacity(255)
			.setFilled(true)
			.setOriginalWidth(186)
			.setOriginalHeight(75)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setWidthMode(WidgetSizeMode.ABSOLUTE)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.revalidate();

		if (worldSwitcherLogoutButtonL == null)
		{
			worldSwitcherLogoutButtonL = worldSwitcherBottomPanel.createChild(WidgetType.GRAPHIC);
		}
		worldSwitcherLogoutButtonL.setHidden(false)
			.setOriginalWidth(36)
			.setOriginalHeight(36)
			.setSpriteId(SpriteID.STATS_TILE_HALF_LEFT_SELECTED)
			.setSpriteTiling(false)
			.setOriginalX(23)
			.setOriginalY(8192)
			.setYPositionMode(WidgetPositionMode.BOTTOM_16384THS)
			.revalidate();

		if (worldSwitcherLogoutButtonC == null)
		{
			worldSwitcherLogoutButtonC = worldSwitcherBottomPanel.createChild(WidgetType.GRAPHIC);
		}
		worldSwitcherLogoutButtonC.setHidden(false)
			.setOriginalWidth(94)
			.setOriginalHeight(36)
			.setSpriteId(SpriteID.UNKNOWN_BUTTON_MIDDLE_SELECTED)
			.setSpriteTiling(true)
			.setOriginalX(56)
			.setOriginalY(8192)
			.setYPositionMode(WidgetPositionMode.BOTTOM_16384THS)
			.revalidate();

		if (worldSwitcherLogoutButtonR == null)
		{
			worldSwitcherLogoutButtonR = worldSwitcherBottomPanel.createChild(WidgetType.GRAPHIC);
		}
		worldSwitcherLogoutButtonR.setHidden(false)
			.setOriginalWidth(36)
			.setOriginalHeight(36)
			.setSpriteId(SpriteID.STATS_TILE_HALF_RIGHT_SELECTED)
			.setSpriteTiling(false)
			.setOriginalX(133)
			.setOriginalY(8192)
			.setYPositionMode(WidgetPositionMode.BOTTOM_16384THS)
			.revalidate();

		if (worldSwitcherLogoutButtonText == null)
		{
			worldSwitcherLogoutButtonText = worldSwitcherBottomPanel.createChild(WidgetType.TEXT);
		}
		worldSwitcherLogoutButtonText.setHidden(false)
			.setFontId(496)
			.setText("Logout")
			.setTextColor(0xF7F0DF)
			.setTextShadowed(true)
			.setOriginalHeight(75)
			.setXTextAlignment(WidgetTextAlignment.CENTER)
			.setYTextAlignment(WidgetTextAlignment.CENTER)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setContentType(205)
			.revalidate();

		worldSwitcherLogout.setOnMouseRepeatListener((JavaScriptCallback) ev ->
		{
			if (worldSwitcherLogoutButtonBackground == null || worldSwitcherLogoutButtonText == null)
			{
				return;
			}

			worldSwitcherLogoutButtonBackground.setOpacity(220);
			worldSwitcherLogoutButtonText.setTextColor(0xFF0000);
		});
		worldSwitcherLogout.setOnMouseLeaveListener((JavaScriptCallback) ev ->
		{
			if (worldSwitcherLogoutButtonBackground == null || worldSwitcherLogoutButtonText == null)
			{
				return;
			}

			worldSwitcherLogoutButtonBackground.setOpacity(255);
			worldSwitcherLogoutButtonText.setTextColor(0xF7F0DF);
		});

		if (worldSwitcherLogoutSeparator == null)
		{
			worldSwitcherLogoutSeparator = worldSwitcherBottomPanel.createChild(WidgetType.RECTANGLE);
		}
		worldSwitcherLogoutSeparator.setHidden(!(plugin.hasFavorite1 || plugin.hasFavorite2))
			.setTextColor(0x73654A)
			.setOpacity(50)
			.setFilled(true)
			.setOriginalY(plugin.hasFavorite1 ? 4639 : 2383) // 32 or 16 px offset from bottom
			.setOriginalHeight(1)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.BOTTOM_16384THS)
			.setWidthMode(WidgetSizeMode.MINUS)
			.revalidate();
	}

	void restoreLogoutWorldSwitcher()
	{
		load();
		if (worldSwitcherLogoutButtonL != null)
		{
			worldSwitcherLogoutButtonL.setHidden(true);
		}
		if (worldSwitcherLogoutButtonC != null)
		{
			worldSwitcherLogoutButtonC.setHidden(true);
		}
		if (worldSwitcherLogoutButtonR != null)
		{
			worldSwitcherLogoutButtonR.setHidden(true);
		}
		if (worldSwitcherLogoutButtonText != null)
		{
			worldSwitcherLogoutButtonText.setHidden(true);
		}
		if (worldSwitcherLogoutButtonBackground != null)
		{
			worldSwitcherLogoutButtonBackground.setHidden(true);
		}
		if (worldSwitcherLogoutSeparator != null)
		{
			worldSwitcherLogoutSeparator.setHidden(true);
		}
		worldSwitcherLogoutButtonL = null;
		worldSwitcherLogoutButtonC = null;
		worldSwitcherLogoutButtonR = null;
		worldSwitcherLogoutSeparator = null;
		worldSwitcherLogoutButtonText = null;
		worldSwitcherLogoutButtonBackground = null;

		restoreInterface();

		if (logoutButton != null)
		{
			logoutButton.setOnMouseOverListener((JavaScriptCallback) ev -> logoutButton.setOpacity(80));
			logoutButton.setOnMouseLeaveListener((JavaScriptCallback) ev -> logoutButton.setOpacity(0));
		}
	}

	void enlargeLogoutButton()
	{
		load();
		if (logoutLayout == null)
		{
			return;
		}

		reviewPane.setHidden(true);
		infoText.setHidden(true);

		fillParentWith(buttonPane);

		switchButton
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setOriginalY(WIDGET_SPACING)
			.revalidate();

		logoutButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setOriginalHeight(ORIG_LOGOUT_BUTTON_HEIGHT + 2 * WIDGET_SPACING)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setOriginalWidth(0)
			.revalidate();

		scaleButton(logoutButton, logoutButton.getWidth() * 5 / 6);
	}


	private void fillParentWith(Widget w)
	{
		w.setHeightMode(WidgetSizeMode.MINUS)
			.setOriginalHeight(0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setOriginalWidth(0)
			.setOriginalX(0)
			.setOriginalY(0)
			.revalidate();
	}

	private void scaleButton(Widget button, int cornerWidth)
	{
		Widget[] children = button.getStaticChildren();
		Widget middle = children[0];
		Widget left = children[1];
		Widget right = children[2];
		Widget textbox = children[3];

		middle.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.revalidate();

		fillParentWith(textbox);

		stretchCorner(left, cornerWidth, WidgetPositionMode.ABSOLUTE_LEFT);
		stretchCorner(right, cornerWidth, WidgetPositionMode.ABSOLUTE_RIGHT);
	}

	private void stretchCorner(Widget corner, int width, int positionMode)
	{
		corner.setXPositionMode(positionMode)
			.setOriginalX(0)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setOriginalHeight(0)
			.setWidthMode(WidgetSizeMode.ABSOLUTE)
			.setOriginalWidth(width)
			.setSpriteTiling(false)
			.revalidate();
	}
}
