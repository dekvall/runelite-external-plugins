package dev.dkvl.womutils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Provides;
import dev.dkvl.womutils.beans.GroupMemberAddition;
import dev.dkvl.womutils.beans.GroupMemberRemoval;
import dev.dkvl.womutils.beans.Member;
import dev.dkvl.womutils.beans.MemberInfo;
import dev.dkvl.womutils.beans.NameChangeEntry;
import dev.dkvl.womutils.beans.WomPlayer;
import dev.dkvl.womutils.beans.WomStatus;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IndexedSprite;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Nameable;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.PlayerMenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xpupdater.XpUpdaterConfig;
import net.runelite.client.plugins.xpupdater.XpUpdaterPlugin;
import net.runelite.client.task.Schedule;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@PluginDependency(XpUpdaterPlugin.class)
@PluginDescriptor(
	name = "Wise Old Man",
	tags = {"wom", "utils", "group", "xp"},
	description = "Helps you manage your wiseoldman.net group."
)
public class WomUtilsPlugin extends Plugin
{
	static final String CONFIG_GROUP = "womutils";
	private static final File WORKING_DIR;
	private static final String NAME_CHANGES = "name-changes.json";

	private static final String ADD_MEMBER = "Add Member";
	private static final String REMOVE_MEMBER = "Remove Member";

	private static final String IMPORT_MEMBERS = "Import";
	private static final String BROWSE_GROUP = "Browse";
	private static final String MENU_TARGET = "WOM Group";

	private static final Color SUCCESS = new Color(170, 255, 40);
	private static final Color ERROR = new Color(204, 66, 66);

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

	private static final int ICON_WIDTH = 12;

	private static final int XP_THRESHOLD = 10000;

	private String currentLayouting;
	private int modIconsStart = -1;

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

	@Inject
	XpUpdaterConfig xpUpdaterConfig;

	private Map<String, String> nameChanges = new HashMap<>();
	private LinkedBlockingQueue<NameChangeEntry> queue = new LinkedBlockingQueue<>();
	private final HashSet<String> groupMembers = new HashSet<>();

	private String lastUsername;
	private boolean fetchXp;
	private long lastXp;

	static
	{
		WORKING_DIR = new File(RuneLite.RUNELITE_DIR, "wom-utils");
		WORKING_DIR.mkdirs();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Wise Old Man started!");
		try
		{
			loadFile();

		}
		catch (IOException e)
		{
			log.error("Could not load previous name changes");
		}

		clientThread.invokeLater(this::loadIcons);
		importGroupMembers();

		if (config.menuOptions())
		{
			addCustomOptions();
		}

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			rebuildLists();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		removeCustomOptions();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				rebuildFriendsChatList(true);
				rebuildFriendsList();
			});
		}

		log.info("Wise Old Man stopped!");
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
		sendRequest(request, r -> {});
	}

	private void sendRequest(Request request, Consumer<Response> consumer)
	{
		sendRequest(request, new WomCallback(consumer));
	}

	private void sendRequest(Request request, Callback callback)
	{
		okHttpClient.newCall(request).enqueue(callback);
	}

	private void removeCallback(Response response, String username)
	{
		final String message;
		final String body = readResponse(response.body());

		if (body == null)
		{
			return;
		}

		if (response.isSuccessful())
		{
			groupMembers.remove(username.toLowerCase());
			message = "Player removed: " + username;
			rebuildLists();
		}
		else
		{
			WomStatus status = gson.fromJson(body, WomStatus.class);
			message = "Error: " + status.getMessage();
		}

		Color color = response.isSuccessful() ? SUCCESS : ERROR;
		sendResponseToChat(message, color);
	}

	private void addCallback(Response response, String username)
	{
		final String message;
		final String body = readResponse(response.body());

		if (body == null)
		{
			return;
		}

		if (response.isSuccessful())
		{
			message = "New player added: " + username;
			groupMembers.add(username.toLowerCase());
			rebuildLists();
		}
		else
		{
			WomStatus status = gson.fromJson(body, WomStatus.class);
			message = "Error: " + status.getMessage();
		}

		Color color = response.isSuccessful() ? SUCCESS : ERROR;
		sendResponseToChat(message, color);
	}

	private String readResponse(ResponseBody body)
	{
		try
		{
			return body.string();
		}
		catch (IOException e)
		{
			log.error("Could not read response {}", e.getMessage());
			return null;
		}
	}

	private void sendResponseToChat(String message, Color color)
	{

		ChatMessageBuilder cmb = new ChatMessageBuilder();
		cmb.append(color, message);

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(cmb.build())
			.build());
	}

	private void memberCallback(Response response)
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
			log.error("Error when reading response {}", e.getMessage());
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

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			rebuildLists();
		}
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

		if (groupId != WidgetInfo.FRIENDS_CHAT.getGroupId()
			&& groupId != WidgetInfo.FRIENDS_LIST.getGroupId()
			|| !AFTER_OPTIONS.contains(event.getOption()))
		{
			return;
		}

		MenuEntry[] entries = client.getMenuEntries();
		entries = Arrays.copyOf(entries, entries.length + 1);
		MenuEntry modifyMember = entries[entries.length - 1] = ModifiedMenuEntry.of(event);

		String name = Text.toJagexName(Text.removeTags(event.getTarget()).toLowerCase());
		modifyMember.setOption(groupMembers.contains(name) ? REMOVE_MEMBER : ADD_MEMBER);
		modifyMember.setType(MenuAction.RUNELITE.getId());
		client.setMenuEntries(entries);
	}

	@Subscribe
	public void onPlayerMenuOptionClicked(PlayerMenuOptionClicked event)
	{
		String username = Text.toJagexName(event.getMenuTarget());
		String usernameLower = username.toLowerCase();
		final String endpoint;
		final Object payload;
		final Callback callback;

		switch (event.getMenuOption())
		{
			case ADD_MEMBER:
				endpoint = "add-members";
				payload = new GroupMemberAddition(config.verificationCode(), new Member[] {new Member(usernameLower)});
				callback = new WomCallback(r -> addCallback(r, username));
				break;
			case REMOVE_MEMBER:
				endpoint = "remove-members";
				payload = new GroupMemberRemoval(config.verificationCode(), new String[] {usernameLower});
				callback = new WomCallback(r -> removeCallback(r, username));
				break;
			default:
				return;
		}
		Request request = createRequest(payload, "groups", "" + config.groupId(), endpoint);
		sendRequest(request, callback);
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
		if (!event.getGroup().equals(CONFIG_GROUP))
		{
			return;
		}

		if (config.menuOptions() && config.groupId() > 0)
		{
			addCustomOptions();
		}
		else
		{
			removeCustomOptions();
		}

		if (event.getKey().equals("showIcons") && client.getGameState() == GameState.LOGGED_IN)
		{
			rebuildLists();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD)
		{
			rebuildFriendsChatList();
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!config.showicons() || modIconsStart == -1)
		{
			return;
		}

		switch (event.getEventName())
		{
			case "friend_cc_settext":
				String[] stringStack = client.getStringStack();
				int stringStackSize = client.getStringStackSize();
				final String rsn = stringStack[stringStackSize - 1];
				final String sanitized = Text.toJagexName(Text.removeTags(rsn).toLowerCase());
				currentLayouting = sanitized;
				if (groupMembers.contains(sanitized))
				{
					CountryIcon icon = CountryIcon.getIcon("default");
					int iconIdx = modIconsStart + icon.ordinal();
					stringStack[stringStackSize - 1] = rsn + " <img=" + iconIdx + ">";
				}
				break;
			case "friend_cc_setposition":
				if (currentLayouting == null || !groupMembers.contains(currentLayouting))
				{
					return;
				}

				int[] intStack = client.getIntStack();
				int intStackSize = client.getIntStackSize();
				int xpos = intStack[intStackSize - 4];
				xpos += ICON_WIDTH + 1;
				intStack[intStackSize - 4] = xpos;
				break;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			if (!Objects.equals(client.getUsername(), lastUsername))
			{
				lastUsername = client.getUsername();
				fetchXp = true;
			}
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			Player local = client.getLocalPlayer();
			if (local == null)
			{
				return;
			}

			long totalXp = client.getOverallExperience();
			// Don't submit update unless xp threshold is reached
			if (Math.abs(totalXp - lastXp) > XP_THRESHOLD)
			{
				log.debug("Submitting update for {}", local.getName());
				update(local.getName());
				lastXp = totalXp;
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (fetchXp)
		{
			lastXp = client.getOverallExperience();
			fetchXp = false;
		}
	}

	private void rebuildFriendsList()
	{
		client.runScript(
			ScriptID.FRIENDS_UPDATE,
			WidgetInfo.FRIEND_LIST_FULL_CONTAINER.getPackedId(),
			WidgetInfo.FRIEND_LIST_SORT_BY_NAME_BUTTON.getPackedId(),
			WidgetInfo.FRIEND_LIST_SORT_BY_LAST_WORLD_CHANGE_BUTTON.getPackedId(),
			WidgetInfo.FRIEND_LIST_SORT_BY_WORLD_BUTTON.getPackedId(),
			WidgetInfo.FRIEND_LIST_LEGACY_SORT_BUTTON.getPackedId(),
			WidgetInfo.FRIEND_LIST_NAMES_CONTAINER.getPackedId(),
			WidgetInfo.FRIEND_LIST_SCROLL_BAR.getPackedId(),
			WidgetInfo.FRIEND_LIST_LOADING_TEXT.getPackedId(),
			WidgetInfo.FRIEND_LIST_PREVIOUS_NAME_HOLDER.getPackedId()
		);
	}

	private void rebuildFriendsChatList()
	{
		rebuildFriendsChatList(false);
	}

	private void rebuildFriendsChatList(boolean disable)
	{
		Widget containerWidget = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);
		if (containerWidget == null)
		{
			return;
		}

		Widget[] children = containerWidget.getChildren();
		if (children == null)
		{
			return;
		}

		for (int i = 0; i < children.length; i+=3)
		{
			String name = children[i].getName();
			String sanitized = Text.toJagexName(Text.removeTags(name));

			String newName;
			if (disable || !groupMembers.contains(sanitized.toLowerCase()) || !config.showicons())
			{
				newName = sanitized;
			}
			else
			{
				CountryIcon icon = CountryIcon.getIcon("default");
				int iconIdx = modIconsStart + icon.ordinal();
				newName = sanitized + " <img=" + iconIdx + ">";
			}

			children[i].setText(newName);
		}
	}

	private void rebuildLists()
	{
		clientThread.invokeLater(() ->
		{
			rebuildFriendsList();
			rebuildFriendsChatList();
		});
	}

	private boolean loadIcons()
	{
		final IndexedSprite[] modIcons = client.getModIcons();
		if (modIconsStart != -1 || modIcons == null)
		{
			return false;
		}

		final CountryIcon[] countryIcons = CountryIcon.values();
		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + countryIcons.length);
		modIconsStart = modIcons.length;

		for (int i = 0; i < countryIcons.length; i++)
		{
			CountryIcon icon = countryIcons[i];
			BufferedImage image = icon.loadImage();
			IndexedSprite sprite = ImageUtil.getImageIndexedSprite(image, client);
			newModIcons[modIconsStart + i] = sprite;
		}

		client.setModIcons(newModIcons);

		return true;
	}

	private void addCustomOptions()
	{
		for (WidgetMenuOption option : WIDGET_MENU_OPTIONS)
		{
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

	private void importGroupMembers()
	{
		Request request = createRequest("groups", "" + config.groupId(), "members");
		sendRequest(request, this::memberCallback);
	}

	private void update(String username)
	{
		if (!xpUpdaterConfig.wiseoldman())
		{
			// Send update requests even if the user has forgot to enable player updates in the core plugin
			Request request = createRequest(new WomPlayer(username), "players", "track");
			sendRequest(request);
		}
	}

	@Provides
	WomUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WomUtilsConfig.class);
	}
}
