package dev.dkvl.womutils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Provides;
import dev.dkvl.womutils.beans.*;

import java.io.File;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import javax.inject.Inject;

import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.PlayerMenuOptionClicked;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import okhttp3.*;

import java.io.IOException;

@Slf4j
@PluginDescriptor(
	name = "Wom Utils"
)
public class WomUtilsPlugin extends Plugin
{
	static final String CONFIG_GROUP = "womutils";
	private static final File WORKING_DIR;
	private static final String NAME_CHANGES = "name-changes.json";

	private static final String ADD_MEMBER = "Add member";
	private static final String REMOVE_MEMBER = "Remove member";

	private static final String IMPORT_MEMBERS = "Import";
	private static final String BROWSE_GROUP = "Browse";
	private static final String MENU_TARGET = "WOM Group";

	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Add ignore", "Remove friend", "Delete");

	private static final ImmutableList<WidgetMenuOption> WIDGET_MENU_OPTIONS =
		new ImmutableList.Builder<WidgetMenuOption>()
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON))
		.add(new WidgetMenuOption(BROWSE_GROUP,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(BROWSE_GROUP,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(BROWSE_GROUP,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON))
		.build();
	// RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON is actually wrong and will act as a placeholder for now.
	// I think the one we want is 164.38, but it needs to be added to core to use.

	@Inject
	private Client client;

	@Inject
	private WomUtilsConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Gson gson;

	private Map<String, String> nameChanges = new HashMap<>();
	private LinkedBlockingQueue<NameChangeEntry> queue = new LinkedBlockingQueue<>();
	private final HashSet<String> groupMembers = new HashSet<>();

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
			importGroupMembers();
			if (config.menuOptions())
			{
				addCustomOptions();
			}

		}
		catch (IOException e)
		{
			log.error("Could not load previous name changes");
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		removeCustomOptions();
		log.info("Wom Utils stopped!");
	}

	@Subscribe
	public void onNameableNameChanged(NameableNameChanged nameableNameChanged)
	{
		final Nameable nameable = nameableNameChanged.getNameable();

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

	private boolean isChangeAlreadyRegistered(NameChangeEntry entry)
	{
		String expected = nameChanges.get(entry.getNewName());
		// We can't just check the key because people can change back and forth between names
		return expected != null && expected.equals(entry.getOldName());
	}

	private void registerNameChange(NameChangeEntry entry)
	{
		nameChanges.put(entry.getNewName(), entry.getOldName());
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
		clientThread.invoke(queue::clear);

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
			nameChanges = gson.fromJson(json, typeOfHashMap);
		}
	}

	private void saveFile() throws IOException
	{
		String changes = gson.toJson(this.nameChanges);
		File file = new File(WORKING_DIR, NAME_CHANGES);
		Files.asCharSink(file, Charsets.UTF_8).write(changes);
	}

	private void sendNameChanges(NameChangeEntry[] changes)
	{
		Request request = createRequest(changes, "names", "bulk");
		sendRequest(request);
		log.info("Submitted {} name changes to WOM", changes.length);
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

	private void sendPlayerRequest(Request request, String username)
	{
		okHttpClient.newCall(request).enqueue(
			createPlayerCallback(username)
		);
	}

	private Callback createPlayerCallback(final String username)
	{
		return new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error submitting request, caused by {}.", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				final String message;
				String body = response.body().string();

				if (response.isSuccessful())
				{
					// A success here gives a weird response of the entire group,
					// so we don;t care about it
					if (groupMembers.remove(username))
					{
						message = "Player removed: " + username;
					}
					else
					{
						message = "New player added: " + username;
						groupMembers.add(username);
					}
				}
				else
				{
					WomStatus status = gson.fromJson(body, WomStatus.class);
					message = status.getMessage();
				}

				ChatMessageBuilder cmb = new ChatMessageBuilder();
				cmb.append(ChatColorType.HIGHLIGHT).append(message);

				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(cmb.build())
					.build());
				response.close();
			}
		};
	}

	private void sendMembersRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error while fetching members list {}.", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (!response.isSuccessful())
				{
					response.close();
					return;
				}

				MemberInfo[] members = new MemberInfo[0];

				try
				{
					members = gson.fromJson(response.body().string(), MemberInfo[].class);
				}
				catch(IOException e)
				{
					log.error("Error when getting response {}", e.getMessage());
				}
				finally
				{
					response.close();
				}

				groupMembers.clear();
				for (MemberInfo m : members)
				{
					groupMembers.add(m.getUsername());
				}
			}
		});
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.menuOptions()
			|| config.groupId() < 1
			|| Strings.isNullOrEmpty(config.verificationCode()))
		{
			return;
		}

		int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
		String option = event.getOption();
		String target = event.getTarget();

		if (groupId != WidgetInfo.FRIENDS_CHAT.getGroupId()
			&& groupId != WidgetInfo.FRIENDS_LIST.getGroupId()
			|| !AFTER_OPTIONS.contains(option))
		{
			return;
		}

		MenuEntry[] entries = client.getMenuEntries();
		entries = Arrays.copyOf(entries, entries.length + 1);
		MenuEntry addMember = entries[entries.length - 1] = new MenuEntry();
		String name = Text.toJagexName(Text.removeTags(target).toLowerCase());

		if (groupMembers.contains(name))
		{
			addMember.setOption(REMOVE_MEMBER);
		}
		else
		{
			addMember.setOption(ADD_MEMBER);
		}
		addMember.setTarget(target);
		addMember.setType(MenuAction.RUNELITE.getId());
		addMember.setParam0(event.getActionParam0());
		addMember.setParam1(event.getActionParam1());
		addMember.setIdentifier(event.getIdentifier());

		client.setMenuEntries(entries);
	}

	@Subscribe
	public void onPlayerMenuOptionClicked(PlayerMenuOptionClicked event)
	{
		String target = Text.toJagexName(event.getMenuTarget()).toLowerCase();
		switch (event.getMenuOption())
		{
			case ADD_MEMBER:
				addGroupMember(target);
				return;
			case REMOVE_MEMBER:
				removeGroupMember(target);
				return;
		}
	}

	@Subscribe
	public void onWidgetMenuOptionClicked(final WidgetMenuOptionClicked event)
	{
		WidgetInfo widget = event.getWidget();
		if (widget != WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB
			&& widget != WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB
			&& widget != WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON
			|| config.groupId() < 1)
		{
			return;
		}

		String opt = event.getMenuOption();
		if (opt.equals(IMPORT_MEMBERS))
		{
			importGroupMembers();
		}
		else if (opt.equals(BROWSE_GROUP))
		{
			openGroupInBrowser();
		}
	}

	private void openGroupInBrowser()
	{
		String url = new HttpUrl.Builder()
		.scheme("https")
		.host("wiseoldman.net")
		.addPathSegment("groups")
		.addPathSegment("" + config.groupId())
		.build()
		.toString();

		SwingUtilities.invokeLater(() -> LinkBrowser.browse(url));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			if (config.menuOptions() && config.groupId() > 0)
			{
				addCustomOptions();
			}
			else
			{
				removeCustomOptions();
			}
		}
	}

	private void addCustomOptions()
	{
		for (WidgetMenuOption option : WIDGET_MENU_OPTIONS)
		{
			log.info("Adding {}", option.getWidget());
			menuManager.addManagedCustomMenu(option);
		}
	}

	private void removeCustomOptions()
	{
		for (WidgetMenuOption option : WIDGET_MENU_OPTIONS)
		{
			menuManager.removeManagedCustomMenu(option);
		}
	}

	private Request createRequest(Object payload, String... pathSegments)
	{
		HttpUrl url = buildUrl(pathSegments);
		RequestBody body = RequestBody.create(
			MediaType.parse("application/json; charset=utf-8"),
			gson.toJson(payload)
		);

		return new Request.Builder()
			.header("User-Agent", "RuneLite")
			.url(url)
			.post(body)
			.build();
	}

	private Request createRequest(String... pathSegments)
	{
		HttpUrl url = buildUrl(pathSegments);
		return new Request.Builder()
			.header("User-Agent", "RuneLite")
			.url(url)
			.build();
	}

	private HttpUrl buildUrl(String[] pathSegments)
	{
		HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
			.scheme("https")
			.host("api.wiseoldman.net");

		for (String pathSegment : pathSegments)
		{
			urlBuilder.addPathSegment(pathSegment);
		}

		return urlBuilder.build();
	}

	private void addGroupMember(String username)
	{
		Member[] member = { new Member(username, "member") };
		GroupMemberAddition gme = new GroupMemberAddition(config.verificationCode(), member);

		Request request = createRequest(gme, "groups", "" + config.groupId(), "add-members");
		sendPlayerRequest(request, username);
	}

	private void removeGroupMember(String username)
	{
		String[] members = {username};
		GroupMemberRemoval gme = new GroupMemberRemoval(config.verificationCode(), members);
		Request request = createRequest(gme, "groups", "" + config.groupId(), "remove-members");
		sendPlayerRequest(request, username);
	}

	private void importGroupMembers()
	{
		Request request = createRequest("groups", "" + config.groupId(), "members");
		sendMembersRequest(request);
	}

	@Provides
	WomUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WomUtilsConfig.class);
	}
}
