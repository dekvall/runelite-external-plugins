package dekvall.pushnotification;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "Push Notifications"
)
public class PushNotificationsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PushNotificationsConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Push Notifications started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Push Notifications stopped!");
	}

	@Provides
	PushNotificationsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PushNotificationsConfig.class);
	}

	@Subscribe
	public void onNotificationFired(NotificationFired event)
	{
		handlePushbullet(event);
		handlePushover(event);
	}

	private void handlePushbullet(NotificationFired event)
	{
		if(Strings.isNullOrEmpty(config.pushbullet()))
		{
			return;
		}

		HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host("api.pushbullet.com")
			.addPathSegment("v2")
			.addPathSegment("pushes")
			.build();

		RequestBody push = new FormBody.Builder()
			.add("body", "You should probably do something about that..")
			.add("title", event.getMessage())
			.add("type", "note")
			.build();

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.header("Access-Token", config.pushbullet())
			.header("Content-Type", "application/json")
			.header("User-Agent", "RuneLite")
			.post(push)
			.url(url)
			.build();

		sendRequest("Pushbullet", request);
	}

	private void handlePushover(NotificationFired event)
	{
		if(Strings.isNullOrEmpty(config.pushover_api()) || Strings.isNullOrEmpty(config.pushover_user()))
		{
			return;
		}

		HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host("api.pushover.net")
			.addPathSegment("1")
			.addPathSegment("messages.json")
			.build();

		RequestBody push = new FormBody.Builder()
			.add("token", config.pushover_api())
			.add("user", config.pushover_user())
			.add("message", event.getMessage())
			.build();

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.header("Content-Type", "application/json")
			.header("User-Agent", "RuneLite")
			.post(push)
			.url(url)
			.build();

		sendRequest("Pushover", request);
	}

	private static void sendRequest(String platform, Request request)
	{
		RuneLiteAPI.CLIENT.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error sending {} notification, caused by {}.", platform, e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}
}
