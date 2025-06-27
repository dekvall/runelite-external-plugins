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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Large Logout"
)
public class LargeLogoutPlugin extends Plugin
{
	private static final int ORIG_CORNER_SIDE = 36;
	private static final int ORIG_BUTTONS_PANE_HEIGHT = 132;
	private static final int ORIG_BUTTONS_PANE_Y_OFFSET = 16;

	private static final int ORIG_LOGOUT_BUTTON_WIDTH = 144;
	private static final int ORIG_LOGOUT_BUTTON_HEIGHT = 36;
	private static final int ORIG_WORLD_SWITCHER_BUTTON_HEIGHT = 36;
	private static final int WIDGET_SPACING = 10;

	private static final int SCRIPT_LOGOUT_LAYOUT_UPDATE = 2243;

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
		}
	}

	@Override
	protected void shutDown()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::restoreLogoutLayout);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == SCRIPT_LOGOUT_LAYOUT_UPDATE)
		{
			enlargeLogoutButton();
		}
	}

	private void restoreLogoutLayout()
	{
		if (!canResize())
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

		client.getWidget(InterfaceID.Logout.WORLD_SWITCHER).
			setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
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

	private void enlargeLogoutButton()
	{
		if (!canResize())
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

	private boolean canResize()
	{
		boolean layoutIsExpected = layoutIsExpected();
		boolean isBetaWorld = client.getWorldType().contains(WorldType.BETA_WORLD)
			|| client.getWorldType().contains(WorldType.NOSAVE_MODE);

		return layoutIsExpected && !isBetaWorld;
	}

	private boolean layoutIsExpected()
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

	@Provides
	LargeLogoutConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LargeLogoutConfig.class);
	}
}
