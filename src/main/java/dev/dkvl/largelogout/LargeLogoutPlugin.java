package dev.dkvl.largelogout;

import com.google.inject.Provides;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
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
	static final String CONFIG_GROUP = "largelogout";

	private static final int ORIG_CORNER_SIDE = 36;
	private static final int ORIG_BUTTONS_PANE_HEIGHT = 132;
	private static final int ORIG_BUTTONS_PANE_Y_OFFSET = 16;

	private static final int ORIG_LOGOUT_BUTTON_WIDTH = 144;
	private static final int ORIG_LOGOUT_BUTTON_HEIGHT = 36;
	private static final int ORIG_WORLD_SWITCHER_BUTTON_HEIGHT = 36;
	private static final int WIDGET_SPACING = 10;

	private static final int ORIG_WORLD_SWITCHER_MIDDLE_HEIGHT = 55;
	private static final int ORIG_WORLD_SWITCHER_BOTTOM_HEIGHT = 32;
	private static final int ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_WIDTH = 21;
	private static final int ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_HEIGHT = 30;
	private static final int ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_X = 2;

	private static final int SCRIPT_LOGOUT_LAYOUT_UPDATE = 2243;
	private static final int SCRIPT_WORLD_SWITCHER_DRAW = 892;

	@Inject
	private Client client;

	@Inject
	private LargeLogoutConfig config;

	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::enlargeLogoutButton);
			clientThread.invokeLater(() ->
			{
				enlargeWorldSwitcherLogoutButton();
				updateUniverse(InterfaceID.Worldswitcher.UNIVERSE);
			});
		}
	}

	@Override
	protected void shutDown()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::restoreLogoutLayout);
			clientThread.invokeLater(() ->
			{
				restoreWorldSwitcherLayout();
				updateUniverse(InterfaceID.Worldswitcher.UNIVERSE);
			});
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()) || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (config.resizeWorldSwitcherLogout())
		{
			clientThread.invokeLater(() ->
			{
				enlargeWorldSwitcherLogoutButton();
				updateUniverse(InterfaceID.Worldswitcher.UNIVERSE);
			});
		}
		else
		{
			clientThread.invokeLater(() ->
			{
				restoreWorldSwitcherLayout();
				updateUniverse(InterfaceID.Worldswitcher.UNIVERSE);
			});
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == SCRIPT_LOGOUT_LAYOUT_UPDATE)
		{
			enlargeLogoutButton();
		}
		else if (event.getScriptId() == SCRIPT_WORLD_SWITCHER_DRAW)
		{
			enlargeWorldSwitcherLogoutButton();
		}
	}

	private void enlargeLogoutButton()
	{
		if (!canResizeLogout())
		{
			return;
		}

		client.getWidget(InterfaceID.Logout.SATISFACTION).setHidden(true);
		client.getWidget(InterfaceID.Logout.LOGOUT_TEXT).setHidden(true);

		fillParentWith(client.getWidget(InterfaceID.Logout.LOGOUT_BUTTONS));

		client.getWidget(InterfaceID.Logout.WORLD_SWITCHER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setOriginalY(WIDGET_SPACING)
			.revalidate();

		Widget logoutButton = client.getWidget(InterfaceID.Logout.LOGOUT);

		logoutButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setOriginalHeight(ORIG_WORLD_SWITCHER_BUTTON_HEIGHT + WIDGET_SPACING + 4)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setOriginalWidth(0)
			.revalidate();

		scaleButton(logoutButton, logoutButton.getWidth() * 5 / 6);
	}

	private void restoreLogoutLayout()
	{
		if (!canResizeLogout())
		{
			return;
		}

		client.getWidget(InterfaceID.Logout.SATISFACTION).setHidden(false);
		client.getWidget(InterfaceID.Logout.LOGOUT_TEXT).setHidden(false);

		client.getWidget(InterfaceID.Logout.LOGOUT_BUTTONS)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setOriginalHeight(ORIG_BUTTONS_PANE_HEIGHT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setOriginalY(ORIG_BUTTONS_PANE_Y_OFFSET)
			.revalidate();

		client.getWidget(InterfaceID.Logout.WORLD_SWITCHER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setOriginalY(0)
			.revalidate();

		Widget logoutButton = client.getWidget(InterfaceID.Logout.LOGOUT);

		logoutButton
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setOriginalHeight(ORIG_LOGOUT_BUTTON_HEIGHT)
			.setWidthMode(WidgetSizeMode.ABSOLUTE)
			.setOriginalWidth(ORIG_LOGOUT_BUTTON_WIDTH)
			.revalidate();

		scaleButton(logoutButton, ORIG_CORNER_SIDE);
	}

	private void enlargeWorldSwitcherLogoutButton()
	{
		if (!canResizeWorldSwitcher())
		{
			return;
		}

		if (!config.resizeWorldSwitcherLogout())
		{
			return;
		}

		client.getWidget(InterfaceID.Worldswitcher.MIDDLE)
			.setOriginalHeight(116)
			.revalidateScroll();

		client.getWidget(InterfaceID.Worldswitcher.BOTTOM)
			.setOriginalHeight(93)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.LOGOUT)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setOriginalX(0)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setOriginalWidth(0)
			.setOriginalHeight(32)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setSpriteTiling(true)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.FAVOURITE_1)
			.setOriginalWidth(0)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setOriginalHeight(16)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.FAVOURITE_2)
			.setOriginalWidth(0)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setOriginalHeight(16)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setOriginalY(15)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.NOFAVOURITES)
			.setOriginalWidth(0)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setOriginalHeight(32)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.revalidate();
	}

	private void restoreWorldSwitcherLayout()
	{
		if (!canResizeWorldSwitcher())
		{
			return;
		}

		client.getWidget(InterfaceID.Worldswitcher.MIDDLE)
			.setOriginalHeight(ORIG_WORLD_SWITCHER_MIDDLE_HEIGHT)
			.revalidateScroll();

		client.getWidget(InterfaceID.Worldswitcher.BOTTOM)
			.setOriginalHeight(ORIG_WORLD_SWITCHER_BOTTOM_HEIGHT)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.LOGOUT)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT)
			.setOriginalX(ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_X)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setOriginalWidth(ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_WIDTH)
			.setOriginalHeight(ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_HEIGHT)
			.setWidthMode(WidgetSizeMode.ABSOLUTE)
			.setHeightMode(WidgetSizeMode.ABSOLUTE)
			.setSpriteTiling(false)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.FAVOURITE_1)
			.setOriginalWidth(ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_WIDTH)
			.setHeightMode(WidgetSizeMode.ABSOLUTE_16384THS)
			.setOriginalHeight(8192)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.FAVOURITE_2)
			.setOriginalWidth(ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_WIDTH)
			.setHeightMode(WidgetSizeMode.ABSOLUTE_16384THS)
			.setOriginalHeight(8192)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setOriginalY(0)
			.revalidate();

		client.getWidget(InterfaceID.Worldswitcher.NOFAVOURITES)
			.setOriginalWidth(ORIG_WORLD_SWITCHER_LOGOUT_BUTTON_WIDTH)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setOriginalHeight(0)
			.setHeightMode(WidgetSizeMode.MINUS)
			.revalidate();
	}

	private boolean canResizeWorldSwitcher()
	{
		boolean layoutIsExpected = worldSwitcherLayoutIsExpected();
		boolean isBetaWorld = isBetaWorld();

		return layoutIsExpected && !isBetaWorld;
	}

	private boolean worldSwitcherLayoutIsExpected()
	{
		Widget layout = client.getWidget(InterfaceID.Worldswitcher.UNIVERSE);
		if (layout == null)
		{
			return false;
		}

		Widget[] children = layout.getStaticChildren();
		if (children == null)
		{
			return false;
		}

		return children.length == 5
			&& Arrays.stream(children)
			.allMatch(w -> w.getId() == InterfaceID.Worldswitcher.UNIVERSE_GRAPHIC0
				|| w.getId() == InterfaceID.Worldswitcher.TOP
				|| w.getId() == InterfaceID.Worldswitcher.MIDDLE
				|| w.getId() == InterfaceID.Worldswitcher.BOTTOM
				|| w.getId() == InterfaceID.Worldswitcher.TOOLTIP);
	}

	private boolean canResizeLogout()
	{
		boolean layoutIsExpected = logoutLayoutIsExpected();
		boolean isBetaWorld = isBetaWorld();

		return layoutIsExpected && !isBetaWorld;
	}

	private boolean logoutLayoutIsExpected()
	{
		Widget layout = client.getWidget(InterfaceID.Logout.UNIVERSE);
		if (layout == null)
		{
			return false;
		}

		Widget[] children = layout.getStaticChildren();
		if (children == null)
		{
			return false;
		}

		return children.length == 2
			&& Arrays.stream(children)
				.allMatch(w -> w.getId() == InterfaceID.Logout.LOGOUT_BUTTONS || w.getId() == InterfaceID.Logout.SATISFACTION);
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

	private void updateUniverse(int widgetId)
	{
		Widget w = client.getWidget(widgetId);
		if (w == null)
		{
			return;
		}

		Object[] args = w.getOnVarTransmitListener();
		if (args == null)
		{
			return;
		}
		client.runScript(args);
	}

	private boolean isBetaWorld()
	{
		return client.getWorldType().contains(WorldType.BETA_WORLD)
			|| client.getWorldType().contains(WorldType.NOSAVE_MODE);
	}

	@Provides
	LargeLogoutConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LargeLogoutConfig.class);
	}
}
