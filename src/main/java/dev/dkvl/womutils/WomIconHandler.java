package dev.dkvl.womutils;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
class WomIconHandler
{
	private final Client client;
	private final ClientThread clientThread;

	private int modIconsStart = -1;

	private String currentLayouting;

	@Inject
	WomIconHandler(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	private boolean loadCountryIcons()
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

	boolean iconsAreLoaded()
	{
		return modIconsStart != -1;
	}

	void loadIcons()
	{
		clientThread.invokeLater(this::loadCountryIcons);
	}

	void handleScriptEvent(ScriptCallbackEvent event, Set<String> groupMembers)
	{
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
				xpos += CountryIcon.ICON_WIDTH + 1;
				intStack[intStackSize - 4] = xpos;
				break;
		}
	}

	void rebuildFriendsChatList(boolean disable, Set<String> groupMembers)
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
			String sanitized = Text.removeTags(name);

			if (!disable && groupMembers.contains(sanitized.toLowerCase()))
			{
				CountryIcon icon = CountryIcon.getIcon("default");
				int iconIdx = modIconsStart + icon.ordinal();
				String newName = sanitized + " <img=" + iconIdx + ">";
				children[i].setText(newName);
			}
			else
			{
				children[i].setText(sanitized);
			}
		}
	}

	void rebuildLists(Set<String> members, boolean showIcons)
	{
		clientThread.invokeLater(() ->
		{
			rebuildFriendsList();
			rebuildFriendsChatList(!showIcons, members);
		});
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
}
