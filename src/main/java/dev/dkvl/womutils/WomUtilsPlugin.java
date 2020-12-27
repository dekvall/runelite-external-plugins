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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
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
	private LinkedBlockingQueue<NameChangeEntry> queue = new LinkedBlockingQueue<>();

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

			NameChangeEntry entry = new NameChangeEntry(Text.toJagexName(prev), Text.toJagexName(name));

			if (isChangeAlreadyRegistered(entry))
			{
				return;
			}

			registerNameChange(entry);
		}
	}

	private boolean isChangeAlreadyRegistered(NameChangeEntry entry)
	{
		String expected = cache.get(entry.getNewName());
		// We can't just check the key because people can change back and forth between names
		return expected != null && expected.equals(entry.getOldName());
	}

	private void registerNameChange(NameChangeEntry entry)
	{
		cache.put(entry.getNewName(), entry.getOldName());
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

		sendNameChanges(queue.toArray(new NameChangeEntry[0]));
		// I am not 100 % this clear is thread safe, but i think so
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

	private void sendNameChanges(NameChangeEntry[] changes)
	{
		HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host("api.wiseoldman.net")
			.addPathSegment("names")
			.addPathSegment("bulk")
			.build();

		String payload = gson.toJson(changes);

		RequestBody body = RequestBody.create(
			MediaType.parse("application/json; charset=utf-8"),
			payload
		);

		Request request = new Request.Builder()
			.header("User-Agent", "RuneLite")
			.url(url)
			.post(body)
			.build();
		sendRequest(request);
		log.info("Submitted the following name changes to WOM: {}", payload);
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
