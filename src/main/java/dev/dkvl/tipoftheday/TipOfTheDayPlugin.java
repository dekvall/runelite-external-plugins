package dev.dkvl.tipoftheday;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.inject.Provides;
import dev.dkvl.tipoftheday.beans.TipData;
import dev.dkvl.tipoftheday.beans.TipEntry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Slf4j
@PluginDescriptor(
	name = "Tip of the Day"
)
public class TipOfTheDayPlugin extends Plugin
{
	private static final int OPEN_URL = 2399;
	@Inject
	private Client client;

	@Inject
	private TipOfTheDayConfig config;

	private String url = "https://runelite.net";
	private TipData tips;
	private Random rand = new Random();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Tip of the Day started!");
		URL url = Resources.getResource("tips.json");
		String text = Resources.toString(url, StandardCharsets.UTF_8);
		tips = new Gson().fromJson(text, TipData.class);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Tip of the Day stopped!");
	}


	private void updateUrl(TipEntry entry)
	{
		if (entry.getUrl() != null)
		{
			url = entry.getUrl();
		}
		else
		{
			url = "https://github.com/runelite/runelite/wiki/" + entry.getPluginName().replace(" ", "-");
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Tip of the Day says " + config.greeting(), null);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() != WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID)
		{
			return;
		}
		Widget titleWidget = client.getWidget(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID, 6);
		titleWidget.setText("Tip of the day");

		Widget messageWidget = client.getWidget(WidgetInfo.LOGIN_CLICK_TO_PLAY_SCREEN_MESSAGE_OF_THE_DAY);
		int tipId = rand.nextInt(tips.getTips().length);
		TipEntry tip = tips.getTips()[tipId];
		updateUrl(tip);
		messageWidget.setText(breakAtGoodPlaces(tip.getTip()));
	}

	/**
	 * Breaks the string up so it always fits the widget
	 *
	 * TODO: Check actual string width to not exceed the bounds
	 * @param tip
	 * @return
	 */
	private String breakAtGoodPlaces(String tip)
	{
		return Text.escapeJagex(tip);
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		if (scriptPreFired.getScriptId() != OPEN_URL)
		{
			return;
		}

		client.getStringStack()[0] = url;
	}

	@Provides
	TipOfTheDayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TipOfTheDayConfig.class);
	}
}
