package dev.dkvl.womutils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import dev.dkvl.womutils.beans.GroupMember;
import dev.dkvl.womutils.beans.GroupMemberAddition;
import dev.dkvl.womutils.beans.GroupMemberRemoval;
import dev.dkvl.womutils.beans.NameChangeEntry;
import java.io.File;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.PlayerMenuOptionClicked;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
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
import net.runelite.client.util.Text;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;

@Slf4j
@PluginDescriptor(
	name = "Wom Utils"
)
public class WomUtilsPlugin extends Plugin
{
	private static final File WORKING_DIR;
	private static final String NAME_CHANGES = "name-changes.json";
	private static final String ADD_MEMBER = "Add member";
	private static final String REMOVE_MEMBER = "Remove member";
	private static final String IMPORT_MEMBERS = "Import";
	private static final String MENU_TARGET = "Group members";
	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Delete", "Add ignore", "Remove friend");
	private static final WidgetMenuOption FIXED_FRIENDS_TAB_IMPORT = new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB);
	private static final WidgetMenuOption RESIZABLE_FRIENDS_TAB_IMPORT = new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB);

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

	private Gson gson = new Gson();

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
				addImportMenuOption();
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
		removeImportMenuOption();
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
		// I am not 100 % sure this clear is thread safe, but i think so
		// Should probably also check if the request succeeds before clearing, but meh
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

	private void sendPlayerRequest(Request request, String target, boolean add)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error adding new member {}.", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) {
				try
				{
					buildErrorMessage(response, add);
				}
				catch (Exception e)
				{
					if (add)
					{
						groupMembers.add(target);
					} else {
						groupMembers.remove(target);
					}
				}
				response.close();
			}
		});
	}

	private void buildErrorMessage(Response response, boolean add) throws IOException {
		Type messageType = new TypeToken<JsonObject>() {}.getType();
		String responseBody = response.body().string();
		JsonObject data = gson.fromJson(responseBody, messageType);
		String errorMessage = data.get("message").toString().replace("\"", "");

		if (!add && errorMessage.contains("Successfully"))
		{
			return;
		}

		ChatMessageBuilder cmb = new ChatMessageBuilder();
		cmb.append(ChatColorType.HIGHLIGHT).append(errorMessage);

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(cmb.build())
				.build());
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
			public void onResponse(Call call, Response response) throws IOException {
				groupMembers.clear();
				Type typeOfArrayList = new TypeToken<ArrayList<GroupMember>>() {}.getType();
				String responseBody = response.body().string();
				ArrayList<GroupMember> data = gson.fromJson(responseBody, typeOfArrayList);
				response.close();

				for (GroupMember gm : data) {
					groupMembers.add(gm.getUsername());
				}
			}
		});
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.menuOptions())
		{
			return;
		}

		int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
		String option = event.getOption();
		String target = event.getTarget();

		if (groupId == WidgetInfo.FRIENDS_CHAT.getGroupId()
				|| groupId == WidgetInfo.FRIENDS_LIST.getGroupId())
		{

			if (!AFTER_OPTIONS.contains(option))
			{
				return;
			}

			final MenuEntry addMember = new MenuEntry();
			if (groupMembers.contains(Text.toJagexName(Text.removeTags(target).toLowerCase())))
			{
				addMember.setOption(REMOVE_MEMBER);
			} else {
				addMember.setOption(ADD_MEMBER);
			}
			addMember.setTarget(target);
			addMember.setType(MenuAction.RUNELITE.getId());
			addMember.setParam0(event.getActionParam0());
			addMember.setParam1(event.getActionParam1());
			addMember.setIdentifier(event.getIdentifier());

			insertMenuEntry(addMember, client.getMenuEntries());
		}
	}

	@Subscribe
	public void onPlayerMenuOptionClicked(PlayerMenuOptionClicked event)
	{
		String target = Text.toJagexName(event.getMenuTarget()).toLowerCase();
		if (event.getMenuOption().equals(ADD_MEMBER))
		{
			addGroupMember(target);
		}
		else if (event.getMenuOption().equals(REMOVE_MEMBER))
		{
			removeGroupMember(target);
		}
	}

	@Subscribe
	public void onWidgetMenuOptionClicked(final WidgetMenuOptionClicked event)
	{
		WidgetInfo widget = event.getWidget();
		if (widget == WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB
				|| widget == WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB)
		{
			importGroupMembers();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("womutils"))
		{
			if (config.menuOptions()) {
				addImportMenuOption();
			} else {
				removeImportMenuOption();
			}
		}
	}


	private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
	{
		MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
		int menuEntryCount = newMenu.length;
		ArrayUtils.swap(newMenu,  menuEntryCount - 1, menuEntryCount - 2);
		client.setMenuEntries(newMenu);
	}

	private void addImportMenuOption()
	{
		menuManager.addManagedCustomMenu(FIXED_FRIENDS_TAB_IMPORT);
		menuManager.addManagedCustomMenu(RESIZABLE_FRIENDS_TAB_IMPORT);
	}

	private void removeImportMenuOption()
	{
		menuManager.removeManagedCustomMenu(FIXED_FRIENDS_TAB_IMPORT);
		menuManager.removeManagedCustomMenu(RESIZABLE_FRIENDS_TAB_IMPORT);
	}

	private void addGroupMember(String username)
	{
		ArrayList<JsonObject> members = new ArrayList<>();
		JsonObject member = new JsonObject();
		member.addProperty("username", username);
		member.addProperty("role", "member");
		members.add(member);

		GroupMemberAddition gme = new GroupMemberAddition(config.verificationCode(), members);

		HttpUrl url = new HttpUrl.Builder()
				.scheme("https")
				.host("api.wiseoldman.net")
				.addPathSegment("groups")
				.addPathSegment(String.valueOf(config.groupId()))
				.addPathSegment("add-members")
				.build();

		String payload = gson.toJson(gme);

		RequestBody body = RequestBody.create(
				MediaType.parse("application/json; charset=utf-8"),
				payload
		);

		Request request = new Request.Builder()
				.header("User-Agent", "RuneLite")
				.url(url)
				.post(body)
				.build();
		sendPlayerRequest(request, username, true);
	}

	private void removeGroupMember(String username)
	{
		ArrayList<String> members = new ArrayList<>();
		members.add(username);
		GroupMemberRemoval gme = new GroupMemberRemoval(config.verificationCode(), members);

		HttpUrl url = new HttpUrl.Builder()
				.scheme("https")
				.host("api.wiseoldman.net")
				.addPathSegment("groups")
				.addPathSegment(String.valueOf(config.groupId()))
				.addPathSegment("remove-members")
				.build();

		String payload = gson.toJson(gme);

		RequestBody body = RequestBody.create(
				MediaType.parse("application/json; charset=utf-8"),
				payload
		);

		Request request = new Request.Builder()
				.header("User-Agent", "RuneLite")
				.url(url)
				.post(body)
				.build();

		sendPlayerRequest(request, username, false);
	}

	private void importGroupMembers()
	{
		HttpUrl url = new HttpUrl.Builder()
				.scheme("https")
				.host("api.wiseoldman.net")
				.addPathSegment("groups")
				.addPathSegment(String.valueOf(config.groupId()))
				.addPathSegment("members")
				.build();

		Request request = new Request.Builder()
				.header("User-Agent", "RuneLite")
				.url(url)
				.build();

		sendMembersRequest(request);
	}

	@Provides
	WomUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WomUtilsConfig.class);
	}
}
