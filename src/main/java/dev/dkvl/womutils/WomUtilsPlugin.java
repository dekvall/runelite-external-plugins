package dev.dkvl.womutils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Provides;
import dev.dkvl.womutils.beans.Competition;
import dev.dkvl.womutils.beans.CompetitionInfo;
import dev.dkvl.womutils.beans.MemberInfo;
import dev.dkvl.womutils.beans.NameChangeEntry;
import dev.dkvl.womutils.beans.Participant;
import dev.dkvl.womutils.events.WomCompetitionInfoFetched;
import dev.dkvl.womutils.events.WomGroupMemberAdded;
import dev.dkvl.womutils.events.WomGroupMemberRemoved;
import dev.dkvl.womutils.events.WomGroupSynced;
import dev.dkvl.womutils.events.WomPlayerCompetitionsFetched;
import dev.dkvl.womutils.panel.NameAutocompleter;
import dev.dkvl.womutils.panel.WomPanel;
import dev.dkvl.womutils.ui.CompetitionInfobox;
import dev.dkvl.womutils.ui.SyncButton;
import dev.dkvl.womutils.ui.WomIconHandler;
import dev.dkvl.womutils.util.DelayedAction;
import dev.dkvl.womutils.util.ModifiedMenuEntry;
import dev.dkvl.womutils.web.WomClient;
import dev.dkvl.womutils.web.WomCommand;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
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
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
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
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
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

	private static final String ADD_MEMBER = "Add member";
	private static final String REMOVE_MEMBER = "Remove member";

	private static final String IMPORT_MEMBERS = "Import";
	private static final String BROWSE_GROUP = "Browse";
	private static final String MENU_TARGET = "WOM group";
	private static final String LOOKUP = "WOM lookup";

	private static final String KICK_OPTION = "Kick";

	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message", "Add ignore", "Remove friend", "Delete", KICK_OPTION);

	// 164.38 is the Friend_Chat_TAB in resizable-modern
	private static final int RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_TAB_ID = WidgetInfo.PACK(WidgetID.RESIZABLE_VIEWPORT_BOTTOM_LINE_GROUP_ID, 38);

	private static final ImmutableList<WidgetMenuOption> WIDGET_IMPORT_MENU_OPTIONS =
		new ImmutableList.Builder<WidgetMenuOption>()
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB))
		.add(new WidgetMenuOption(IMPORT_MEMBERS,
			MENU_TARGET, RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_TAB_ID))
		.build();

	private static final ImmutableList<WidgetMenuOption> WIDGET_BROWSE_MENU_OPTIONS =
		new ImmutableList.Builder<WidgetMenuOption>()
			.add(new WidgetMenuOption(BROWSE_GROUP,
				MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB))
			.add(new WidgetMenuOption(BROWSE_GROUP,
				MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB))
			.add(new WidgetMenuOption(BROWSE_GROUP,
				MENU_TARGET, RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_TAB_ID))
			.build();

	private static final int XP_THRESHOLD = 10_000;

	private static final int CLAN_SIDEPANEL_DRAW = 4397;
	private static final int CLAN_SETTINGS_MEMBERS_DRAW = 4232;

	private static final int CLAN_SETTINGS_INFO_PAGE_WIDGET = 690;
	private static final int CLAN_SETTINGS_INFO_PAGE_WIDGET_ID = WidgetInfo.PACK(CLAN_SETTINGS_INFO_PAGE_WIDGET, 2);
	private static final int CLAN_SETTINGS_MEMBERS_PAGE_WIDGET = 693;
	private static final int CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET, 2);

	private static final int CLAN_SETTINGS_MEMBERS_WIDGET = WidgetInfo.PACK(693, 10);
	private static final int CLAN_OPTIONS_RANKS_WIDGET = WidgetInfo.PACK(693, 11);

	private static final Color SUCCESS = new Color(170, 255, 40);

	private boolean levelupThisSession = false;

	@Inject
	private Client client;

	@Inject
	private WomUtilsConfig config;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private Gson gson;

	@Inject
	private WomIconHandler iconHandler;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private WomClient womClient;

	@Inject
	private XpUpdaterConfig xpUpdaterConfig;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	@Inject
	private Notifier notifier;

	private WomPanel womPanel;

	@Inject
	ClientToolbar clientToolbar;

	private Map<String, String> nameChanges = new HashMap<>();
	private LinkedBlockingQueue<NameChangeEntry> queue = new LinkedBlockingQueue<>();
	private Map<String, MemberInfo> groupMembers = new HashMap<>();
	private List<Competition> playerCompetitions = new ArrayList<>();
	private List<CompetitionInfobox> competitionInfoboxes = new ArrayList<>();
	private List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
	private Map<Integer, CompetitionInfo> competitionInfoMap = new HashMap<>();

	private String lastUsername;
	private boolean fetchXp;
	private long lastXp;
	private boolean visitedLoginScreen;
	private boolean recentlyLoggedIn;
	private String playerName;

	private NavigationButton navButton;

	private SyncButton syncButton;

	private final Map<Skill, Integer> previousSkillLevels = new EnumMap<>(Skill.class);

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
		womClient.importGroupMembers();

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

		clientThread.invoke(this::saveCurrentLevels);
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
		clearInfoboxes();
		cancelNotifications();
		previousSkillLevels.clear();
		competitionInfoMap.clear();
		levelupThisSession = false;

		log.info("Wise Old Man stopped!");
	}

	private void saveCurrentLevels()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (Skill s : Skill.values())
		{
			previousSkillLevels.put(s, client.getRealSkillLevel(s));
		}
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
				|| groupId == WidgetInfo.FRIENDS_LIST.getGroupId()
				|| groupId == WidgetID.CLAN_GROUP_ID
				|| groupId == WidgetID.CLAN_GUEST_GROUP_ID);

		boolean addMenuLookup = config.menuLookupOption()
			&& (groupId == WidgetInfo.FRIENDS_LIST.getGroupId()
			|| groupId == WidgetInfo.FRIENDS_CHAT.getGroupId()
			|| groupId == WidgetID.CLAN_GROUP_ID
			|| groupId == WidgetID.CLAN_GUEST_GROUP_ID
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
				womClient.addGroupMember(username);
				break;
			case REMOVE_MEMBER:
				womClient.removeGroupMember(username);
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
	public void onStatChanged(StatChanged event)
	{
		Skill s = event.getSkill();
		int levelAfter = client.getRealSkillLevel(s);
		int levelBefore = previousSkillLevels.getOrDefault(s, -1);

		if (levelBefore != -1 && levelAfter > levelBefore)
		{
			levelupThisSession = true;
		}
		previousSkillLevels.put(s, levelAfter);
	}

	@Subscribe
	public void onWidgetMenuOptionClicked(final WidgetMenuOptionClicked event)
	{
		// Using widgetId since getWidget is nullable
		int widgetId = event.getWidgetId();
		if (widgetId != WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB.getPackedId()
			&& widgetId != WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB.getPackedId()
			&& widgetId != WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON.getPackedId()
			&& widgetId != RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_TAB_ID
			|| config.groupId() < 1)
		{
			return;
		}

		switch (event.getMenuOption())
		{
			case IMPORT_MEMBERS:
				womClient.importGroupMembers();
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
		if (config.groupId() > 0)
		{
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

		if (event.getKey().equals("timerOngoing") || event.getKey().equals("timerUpcoming"))
		{
			updateInfoboxes();
		}

		if (event.getKey().equals("sendCompetitionNotification"))
		{
			updateScheduledNotifications();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD)
		{
			iconHandler.rebuildMemberList(!config.showicons(), groupMembers, WidgetInfo.FRIENDS_CHAT_LIST);
		}
		else if (event.getScriptId() == CLAN_SIDEPANEL_DRAW)
		{
			iconHandler.rebuildMemberList(!config.showicons(), groupMembers, WidgetInfo.CLAN_MEMBER_LIST);
			iconHandler.rebuildMemberList(!config.showicons(), groupMembers, WidgetInfo.CLAN_GUEST_MEMBER_LIST);
		}
		else if (event.getScriptId() == CLAN_SETTINGS_MEMBERS_DRAW)
		{
			iconHandler.rebuildSettingsMemberList(!config.showicons(), groupMembers);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if(!config.syncClanButton() || config.groupId() <= 0 || Strings.isNullOrEmpty(config.verificationCode()))
		{
			return;
		}

		switch (widgetLoaded.getGroupId())
		{
			case CLAN_SETTINGS_MEMBERS_PAGE_WIDGET:
				clientThread.invoke(() -> createSyncButton(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID));
				break;
			case CLAN_SETTINGS_INFO_PAGE_WIDGET:
				clientThread.invoke(() -> createSyncButton(CLAN_SETTINGS_INFO_PAGE_WIDGET_ID));
				break;
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

			recentlyLoggedIn = true;
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			visitedLoginScreen = true;
			Player local = client.getLocalPlayer();
			if (local == null)
			{
				return;
			}

			long totalXp = client.getOverallExperience();
			// Don't submit update unless xp threshold is reached
			if (Math.abs(totalXp - lastXp) > XP_THRESHOLD || levelupThisSession)
			{
				log.debug("Submitting update for {}", local.getName());
				update(local.getName());
				lastXp = totalXp;
				levelupThisSession = false;
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

		if(visitedLoginScreen && recentlyLoggedIn)
		{
			womClient.fetchPlayerCompetitions(client.getLocalPlayer().getName());
			recentlyLoggedIn = false;
			visitedLoginScreen = false;
		}
	}

	private void addGroupMenuOptions(List<WidgetMenuOption> menuOptions)
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

	@Subscribe
	public void onWomGroupSynced(WomGroupSynced event)
	{
		Map<String, MemberInfo> old = new HashMap<>(groupMembers);

		groupMembers.clear();
		for (MemberInfo m : event.getMembers())
		{
			groupMembers.put(m.getUsername(), m);
		}
		onGroupUpdate();
		if (!event.isSilent())
		{
			String message = compareChanges(old, groupMembers);
			sendResponseToChat(message, SUCCESS);
		}
	}

	@Subscribe
	public void onWomGroupMemberAdded(WomGroupMemberAdded event)
	{
		MemberInfo member = event.getMember();
		groupMembers.put(member.getUsername().toLowerCase(), member);
		onGroupUpdate();

		String message = "New player added: " + event.getUsername(); // Correctly capitalized
		sendResponseToChat(message, SUCCESS);
	}

	@Subscribe
	public void onWomGroupMemberRemoved(WomGroupMemberRemoved event)
	{
		groupMembers.remove(event.getUsername().toLowerCase());
		onGroupUpdate();
		String message = "Player removed: " + event.getUsername();
		sendResponseToChat(message, SUCCESS);
	}

	@Subscribe
	public void onWomPlayerCompetitionsFetched(WomPlayerCompetitionsFetched event)
	{
		playerCompetitions = Arrays.asList(event.getCompetitions());
		playerName = event.getUsername();
		for (Competition c : playerCompetitions)
		{
			if (!c.hasEnded() && config.competitionLoginMessage())
			{
				sendResponseToChat(c.getStatus(), Color.RED);
			}
			if (c.isActive())
			{
				womClient.fetchCompetitionInfo(c.getId());
			}
		}
		updateInfoboxes();
		updateScheduledNotifications();

		log.debug("Fetched {} competitions for player {}", event.getCompetitions().length, event.getUsername());
	}

	@Subscribe
	public void onWomCompetitionInfoFetched(WomCompetitionInfoFetched event)
	{
		CompetitionInfo comp = event.getComp();
		competitionInfoMap.put(comp.getId(), comp);
		updateInfoboxes();
	}

	private void updateInfoboxes()
	{
		clearInfoboxes();
		for (Competition c : playerCompetitions)
		{
			if (c.isActive() && config.timerOngoing() || !c.hasStarted() && config.timerUpcoming())
			{
				Participant player = getParticipant(c.getId(), playerName);
				competitionInfoboxes.add(new CompetitionInfobox(c, player, this));
			}
		}

		for (CompetitionInfobox b: competitionInfoboxes)
		{
			infoBoxManager.addInfoBox(b);
		}
	}

	private void clearInfoboxes()
	{
		for (CompetitionInfobox b : competitionInfoboxes)
		{
			infoBoxManager.removeInfoBox(b);
		}
		competitionInfoboxes.clear();
	}

	@Nullable
	private Participant getParticipant(int compid, String username)
	{
		CompetitionInfo compInfo = competitionInfoMap.getOrDefault(compid, null);
		if (compInfo == null)
		{
			return null;
		}

		for (Participant p : compInfo.getParticipants())
		{
			if (p.getUsername().equalsIgnoreCase(username))
			{
				return p;
			}
		}
		return null;
	}

	private void updateScheduledNotifications()
	{
		cancelNotifications();

		if (!config.sendCompetitionNotification())
		{
			return;
		}

		List<DelayedAction> delayedActions = new ArrayList<>();

		for (Competition c : playerCompetitions)
		{
			if (!c.hasStarted())
			{
				delayedActions.add(new DelayedAction(c.durationLeft().minusHours(1), () ->
					notifier.notify(c.getStatus())));
				delayedActions.add(new DelayedAction(c.durationLeft().minusMinutes(15), () ->
					notifier.notify(c.getStatus())));
				delayedActions.add(new DelayedAction(c.durationLeft().plusSeconds(5), () ->
					{
						notifier.notify(c.getTitle() + " has started!");
						womClient.updatePlayer(playerName);
					})
				);
			}
			else if (c.isActive())
			{
				delayedActions.add(new DelayedAction(c.durationLeft().minusHours(1), () ->
					notifier.notify(c.getStatus())));
				delayedActions.add(new DelayedAction(c.durationLeft().minusMinutes(15), () ->
					{
						notifier.notify(c.getStatus());
						// Update player 15 mins before end so there is at least one final datapoint
						womClient.updatePlayer(playerName);
					})
				);
				delayedActions.add(new DelayedAction(c.durationLeft().minusMinutes(4), () ->
					notifier.notify(c.getTitle() + " is ending soon, logout now to record your final datapoint!")));
				delayedActions.add(new DelayedAction(c.durationLeft().plusSeconds(5), () ->
					notifier.notify(c.getTitle() + " is over, thanks for playing!")));
			}
		}

		for (DelayedAction action : delayedActions)
		{
			if (!action.getDelay().isNegative())
			{
				scheduledFutures.add(scheduledExecutorService.schedule(action.getRunnable(),
					action.getDelay().getSeconds(), TimeUnit.SECONDS));
			}
		}
	}

	private void cancelNotifications()
	{
		for (ScheduledFuture<?> sf : scheduledFutures)
		{
			sf.cancel(false);
		}
		scheduledFutures.clear();
	}

	private void onGroupUpdate()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			iconHandler.rebuildLists(groupMembers, config.showicons());
		}
	}

	private String compareChanges(Map<String, MemberInfo> oldMembers, Map<String, MemberInfo> newMembers)
	{
		int membersAdded = 0;
		int ranksChanged = 0;
		for (String username : newMembers.keySet())
		{
			if (oldMembers.containsKey(username))
			{
				if (!newMembers.get(username).getRole().equals(oldMembers.get(username).getRole()))
				{
					ranksChanged += 1;
				}
			}
			else
			{
				membersAdded += 1;
			}
		}

		int membersRemoved = oldMembers.size() + membersAdded - newMembers.size();

		return String.format("Synced %d clan members. %d added, %d removed, %d ranks changed.",
			newMembers.size(), membersAdded, membersRemoved, ranksChanged);
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

	private void createSyncButton(int w)
	{
		new SyncButton(client, womClient, chatboxPanelManager, w, groupMembers);
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
