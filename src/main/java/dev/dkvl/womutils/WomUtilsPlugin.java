package dev.dkvl.womutils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Provides;
import dev.dkvl.womutils.beans.MemberInfo;
import dev.dkvl.womutils.beans.NameChangeEntry;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Nameable;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
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
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import okhttp3.HttpUrl;

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
	private static final String LOOKUP = "WOM Lookup";

	private static final String KICK_OPTION = "Kick";

	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message", "Add ignore", "Remove friend", "Delete", KICK_OPTION);

	private static final ImmutableList<WidgetMenuOption> WIDGET_IMPORT_MENU_OPTIONS =
		new ImmutableList.Builder<WidgetMenuOption>()
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON))
		.build();

	private static final ImmutableList<WidgetMenuOption> WIDGET_BROWSE_MENU_OPTIONS =
		new ImmutableList.Builder<WidgetMenuOption>()
			.add(new WidgetMenuOption(BROWSE_GROUP,
				MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB))
			.add(new WidgetMenuOption(BROWSE_GROUP,
				MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB))
			.add(new WidgetMenuOption(BROWSE_GROUP,
				MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON))
			.build();
	// RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON is actually wrong and will act as a placeholder for now.
	// I think the one we want is 164.38, but it needs to be added to core to use.

	private static final int XP_THRESHOLD = 10000;

	@Inject
	private Client client;

	@Inject
	private WomUtilsConfig config;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Gson gson;

	@Inject
	private WomIconHandler iconHandler;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private WomClient womClient;

	@Inject
	XpUpdaterConfig xpUpdaterConfig;

	WomPanel womPanel;

	@Inject
	ClientToolbar clientToolbar;

	private Map<String, String> nameChanges = new HashMap<>();
	private LinkedBlockingQueue<NameChangeEntry> queue = new LinkedBlockingQueue<>();
	private final Map<String, MemberInfo> groupMembers = new HashMap<>();

	private String lastUsername;
	private boolean fetchXp;
	private long lastXp;

	private NavigationButton navButton;

	static
	{
		WORKING_DIR = new File(RuneLite.RUNELITE_DIR, "wom-utils");
		WORKING_DIR.mkdirs();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Wise Old Man started!");

		// This will work, idk why really, but ok
		womPanel = injector.getInstance(WomPanel.class);
		try
		{
			loadFile();
		}
		catch (IOException e)
		{
			log.error("Could not load previous name changes");
		}

		iconHandler.loadIcons();
		womClient.importGroupMembersTo(groupMembers);

		if (config.playerLookupOption())
		{
			menuManager.addPlayerMenuItem(LOOKUP);
		}

		if (config.importGroup())
		{
			addGroupMenuOptions(WIDGET_IMPORT_MENU_OPTIONS);
		}

		if (config.browseGroup())
		{
			addGroupMenuOptions(WIDGET_BROWSE_MENU_OPTIONS);
		}


		if (client.getGameState() == GameState.LOGGED_IN)
		{
			iconHandler.rebuildLists(groupMembers, config.showicons());
		}

		for (WomCommand c : WomCommand.values())
		{
			chatCommandManager.registerCommandAsync(c.getCommand(), this::commandHandler);
		}

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "wom-icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Wise Old Man")
			.icon(icon)
			.priority(5)
			.panel(womPanel)
			.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{

		removeGroupMenuOptions();
		menuManager.removePlayerMenuItem(LOOKUP);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			iconHandler.rebuildLists(groupMembers, false);
		}

		for (WomCommand c : WomCommand.values())
		{
			chatCommandManager.unregisterCommand(c.getCommand());
		}
		clientToolbar.removeNavigation(navButton);
		womPanel.shutdown();

		log.info("Wise Old Man stopped!");
	}

	private void commandHandler(ChatMessage chatMessage, String s)
	{
		// TODO: Handle individual ehp/ehbs.

		WomCommand cmd = WomCommand.fromCommand(s);

		if (cmd == null)
		{
			return;
		}

		commandLookup(cmd, chatMessage);
	}

	private void commandLookup(WomCommand command, ChatMessage chatMessage)
	{
		ChatMessageType type = chatMessage.getType();

		String player;

		if (type == ChatMessageType.PRIVATECHATOUT)
		{
			player = client.getLocalPlayer().getName();
		}
		else
		{
			player = Text.sanitize(chatMessage.getName());
		}

		womClient.commandLookup(player, command, chatMessage);
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

		womClient.submitNameChanges(queue.toArray(new NameChangeEntry[0]));
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

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.addRemoveMember() && !config.menuLookupOption())
		{
			return;
		}

		int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
		String option = event.getOption();

		if (!AFTER_OPTIONS.contains(option)
			// prevent duplicate menu options in friends list
			|| (option.equals("Delete") && groupId != WidgetInfo.IGNORE_LIST.getGroupId()))
		{
			return;
		}

		boolean addModifyMember = config.addRemoveMember()
			&& config.groupId() > 0
			&& !Strings.isNullOrEmpty(config.verificationCode())
			&& (groupId == WidgetInfo.FRIENDS_CHAT.getGroupId()
				|| groupId == WidgetInfo.FRIENDS_LIST.getGroupId());

		boolean addMenuLookup = config.menuLookupOption()
			&& (groupId == WidgetInfo.FRIENDS_LIST.getGroupId()
			|| groupId == WidgetInfo.FRIENDS_CHAT.getGroupId()
			// prevent from adding for Kick option (interferes with the raiding party one)
			|| groupId == WidgetInfo.CHATBOX.getGroupId() && !KICK_OPTION.equals(option)
			|| groupId == WidgetInfo.RAIDING_PARTY.getGroupId()
			|| groupId == WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId()
			|| groupId == WidgetInfo.IGNORE_LIST.getGroupId());

		int offset = (addModifyMember ? 1:0) + (addMenuLookup ? 1:0);

		if (offset == 0)
		{
			return;
		}

		MenuEntry[] entries = client.getMenuEntries();
		entries = Arrays.copyOf(entries, entries.length + offset);

		if (addModifyMember)
		{
			MenuEntry modifyMember = entries[entries.length - offset] = ModifiedMenuEntry.of(event);
			String name = Text.toJagexName(Text.removeTags(event.getTarget()).toLowerCase());
			modifyMember.setOption(groupMembers.containsKey(name) ? REMOVE_MEMBER : ADD_MEMBER);
			modifyMember.setType(MenuAction.RUNELITE.getId());
			offset--;
		}

		if (addMenuLookup)
		{
			MenuEntry womLookup = entries[entries.length - offset] = ModifiedMenuEntry.of(event);
			womLookup.setOption(LOOKUP);
			womLookup.setType(MenuAction.RUNELITE.getId());
			womLookup.setIdentifier(event.getIdentifier() + offset);
		}

		client.setMenuEntries(entries);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE && event.getMenuAction() != MenuAction.RUNELITE_PLAYER)
		{
			return;
		}

		String username = Text.toJagexName(Text.removeTags(event.getMenuTarget()));

		switch (event.getMenuOption())
		{
			case ADD_MEMBER:
				womClient.addGroupMember(username, groupMembers);
				break;
			case REMOVE_MEMBER:
				womClient.removeGroupMember(username, groupMembers);
				break;
			case LOOKUP:
			{
				final String target;
				if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
				{
					Player player = client.getCachedPlayers()[event.getId()];
					if (player == null)
					{
						return;
					}
					target = player.getName();
				}
				else
				{
					target = Text.removeTags(event.getMenuTarget());
				}
				lookupPlayer(target);
			}
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

		switch (event.getMenuOption())
		{
			case IMPORT_MEMBERS:
				womClient.importGroupMembersTo(groupMembers);
				break;
			case BROWSE_GROUP:
				openGroupInBrowser();
				break;
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

		menuManager.removePlayerMenuItem(LOOKUP);
		if (config.playerLookupOption())
		{
			menuManager.addPlayerMenuItem(LOOKUP);
		}

		removeGroupMenuOptions();
		if (config.groupId() > 0) {
			if (config.browseGroup())
			{
				addGroupMenuOptions(WIDGET_BROWSE_MENU_OPTIONS);
			}

			if (config.importGroup())
			{
				addGroupMenuOptions(WIDGET_IMPORT_MENU_OPTIONS);
			}
		}

		if ((event.getKey().equals("showIcons") || event.getKey().equals("showFlags"))
			&& client.getGameState() == GameState.LOGGED_IN)
		{
			iconHandler.rebuildLists(groupMembers, config.showicons());
		}

	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD)
		{
			iconHandler.rebuildFriendsChatList(!config.showicons(), groupMembers);
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!config.showicons() || !iconHandler.iconsAreLoaded())
		{
			return;
		}

		iconHandler.handleScriptEvent(event, groupMembers);
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

	private void addGroupMenuOptions(ImmutableList<WidgetMenuOption> menuOptions)
	{
		for (WidgetMenuOption option : menuOptions)
		{
			menuManager.addManagedCustomMenu(option);
		}
	}

	private void removeGroupMenuOptions()
	{
		for (WidgetMenuOption option : WIDGET_BROWSE_MENU_OPTIONS)
		{
			menuManager.removeManagedCustomMenu(option);
		}

		for (WidgetMenuOption option : WIDGET_IMPORT_MENU_OPTIONS)
		{
			menuManager.removeManagedCustomMenu(option);
		}
	}

	private void update(String username)
	{
		if (!xpUpdaterConfig.wiseoldman())
		{
			// Send update requests even if the user has forgot to enable player updates in the core plugin
			womClient.updatePlayer(username);
		}
	}

	private void lookupPlayer(String playerName)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (!navButton.isSelected())
			{
				navButton.getOnSelect().run();
			}
			womPanel.lookup(playerName);
		});
	}

	@Provides
	WomUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WomUtilsConfig.class);
	}

	@Override
	public void configure(Binder binder)
	{
		binder.bind(WomIconHandler.class);
		binder.bind(NameAutocompleter.class);
		binder.bind(WomClient.class);
	}
}
