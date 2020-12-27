package dev.dkvl.womutils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Provides;
import dev.dkvl.womutils.beans.NameChangeEntry;
import java.io.File;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.util.Text;
import okhttp3.*;

import java.io.IOException;

@Slf4j
@PluginDescriptor(
	name = "Wom Utils"
)
public class WomUtilsPlugin extends Plugin
{
	private static final File WORKING_DIR;
	private static final String NAME_CHANGES = "name-changes.json";

	@Inject
	private Client client;

	@Inject
	private WomUtilsConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	private Gson gson = new Gson();

	private Map<String, String> cache = new HashMap<>();
	private List<NameChangeEntry> queue = new ArrayList<>();

	static
	{
		WORKING_DIR = new File(RuneLite.RUNELITE_DIR, "wom-utils");
		WORKING_DIR.mkdirs();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Wom Utils started!");
		try
		{
			loadFile();
		}
		catch (IOException e)
		{
			log.error("Could not load previous name changes");
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Wom Utils stopped!");
	}

	@Subscribe
	public void onNameableNameChanged(NameableNameChanged nameableNameChanged)
	{
		final Nameable nameable = nameableNameChanged.getNameable();

		if (nameable instanceof FriendsChatMember
			|| nameable instanceof Friend
			|| config.updateIgnored() && nameable instanceof Ignore)
		{
			String name = nameable.getName();
			String prev = nameable.getPrevName();

			if (Strings.isNullOrEmpty(prev) || name.startsWith("[#"))
			{
				return;
			}

			name = Text.toJagexName(name);
			prev = Text.toJagexName(prev);

			if (cache.get(name) != null && cache.get(name).equals(prev))
			{
				// Name change already registered, ignore
				// We can't just check the key because people can change back and forth between names
				return;
			}

			cache.put(name, prev);
			log.info("Logged a name change from {} to {}", prev, name);
			queueNameChangeUpdate(prev, name);
		}
	}

	private void queueNameChangeUpdate(String oldName, String newName)
	{
		NameChangeEntry entry = new NameChangeEntry(oldName, newName);
		queue.add(entry);
	}

	@Schedule(
		period = 30,
		unit = ChronoUnit.MINUTES
	)
	public void sendUpdate()
	{
		if (queue.isEmpty())
		{
			return;
		}

		sendNameChanges(queue);
		// This can probably race if we add changes to the queue
		// at this point in time, so we risk clearing changes that we haven't submitted
		// Let's just not care rn
		queue.clear();

		try
		{
			saveFile();
		}
		catch (IOException e)
		{
			log.error("Could not write name changes to filesystem");
		}
	}

	private void loadFile() throws IOException
	{
		File file = new File(WORKING_DIR, NAME_CHANGES);
		if (file.exists())
		{
			String json = Files.asCharSource(file, Charsets.UTF_8).read();
			Type typeOfHashMap = new TypeToken<Map<String, String>>() {}.getType();
			cache = gson.fromJson(json, typeOfHashMap);
		}
	}

	private void saveFile() throws IOException
	{
		String changes = gson.toJson(cache);
		File file = new File(WORKING_DIR, NAME_CHANGES);
		Files.asCharSink(file, Charsets.UTF_8).write(changes);
	}

	private void sendNameChanges(List<NameChangeEntry> changes)
	{
		HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host("api.wiseoldman.net")
			.addPathSegment("names")
			.addPathSegment("bulk")
			.build();

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json; charset=utf-8"),
			gson.toJson(changes)
		);

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.url(url)
			.post(body)
			.build();

		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error submitting name change, caused by {}.", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	@Provides
	WomUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WomUtilsConfig.class);
	}
}
