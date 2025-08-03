package dekvall.pushnotification;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
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
	private OkHttpClient okHttpClient;

	@Inject
	private PushNotificationsConfig config;

	@Override
	protected void startUp()
	{
		log.info("Push Notifications started!");
	}

	@Override
	protected void shutDown()
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
		if (!config.forwardNotifications())
		{
			return;
		}

		push(event.getMessage());
	}

	@Subscribe
	public void onPluginMessage(PluginMessage event)
	{
		if (!"push-notifications".equals(event.getNamespace()) || !"notify".equals(event.getName()))
		{
			return;
		}

		Object maybeMessage = event.getData().get("message");
		if (!(maybeMessage instanceof String))
		{
			return;
		}

		if (!config.forwardPluginMessages())
		{
			return;
		}

		String message = (String)maybeMessage;
		push(message);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!event.getCommand().equalsIgnoreCase("sendpush"))
		{
			return;
		}

		final String message;
		if (event.getArguments().length == 0)
		{
			message = "Test";
		}
		else
		{
			message = String.join(" ", event.getArguments());
		}
		push(message);
	}

	private void push(String message)
	{
		handlePushbullet(message);
		handlePushover(message);
		handleGotify(message);
		handlePushcut(message);
	}

	private void handlePushbullet(String message)
	{
		if (Strings.isNullOrEmpty(config.pushbulletToken()))
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
			.add("title", message)
			.add("type", "note")
			.build();

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.header("Access-Token", config.pushbulletToken())
			.header("Content-Type", "application/json")
			.post(push)
			.url(url)
			.build();

		sendRequest("Pushbullet", request);
	}

	private void handlePushover(String message)
	{
		if (Strings.isNullOrEmpty(config.pushoverToken()) || Strings.isNullOrEmpty(config.pushoverUser()))
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
			.add("token", config.pushoverToken())
			.add("user", config.pushoverUser())
			.add("message", message)
			.build();

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.post(push)
			.url(url)
			.build();

		sendRequest("Pushover", request);
	}

	private void handleGotify(String message)
	{
		if (Strings.isNullOrEmpty(config.gotifyUrl()) || Strings.isNullOrEmpty(config.gotifyToken()))
		{
			return;
		}

		HttpUrl parsedUrl = HttpUrl.parse(config.gotifyUrl());

		if (parsedUrl == null)
		{
			log.warn("Invalid Gotify URL, expected format: http or https://<host>:<port>/message");
			return;
		}

		HttpUrl url = parsedUrl.newBuilder()
			.addQueryParameter("token", config.gotifyToken())
			.build();
		
		RequestBody push = new FormBody.Builder()
			.add("title", message)
			.add("message", message)
			.add("priority", String.valueOf(config.gotifyPriority()))
			.build();

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.post(push)
			.url(url)
			.build();

		sendRequest("Gotify", request);
	}

	private void handlePushcut(String message)
	{
		if (Strings.isNullOrEmpty(config.pushcutSecret()) || Strings.isNullOrEmpty(config.pushcutNotification()))
		{
			return;
		}

		// Webhook reference: https://www.pushcut.io/support/notifications#webhook-url
		HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host("api.pushcut.io")
			.addPathSegment(config.pushcutSecret())
			.addPathSegment("notifications")
			.addPathSegment(config.pushcutNotification())
			.build();

		JsonObject json = new JsonObject();
		json.addProperty("text", message);

		RequestBody push = RequestBody.create(
			MediaType.parse("application/json"),
			json.toString()
		);

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.post(push)
			.url(url)
			.build();

		sendRequest("Pushcut", request);
	}

	private void sendRequest(String platform, Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error sending {} notification, caused by {}.", platform, e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (!response.isSuccessful())
				{
					log.warn("Error sending {} notification, received HTTP {} response", platform, response.code());
				}
				response.close();
			}
		});
	}
}
