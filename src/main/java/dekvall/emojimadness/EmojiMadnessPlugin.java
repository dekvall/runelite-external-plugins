/*
 * Copyright (c) 2019, Lotto <https://github.com/devLotto>
 * Copyright (c) 2020, dekvall <https://github.com/dekvall>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dekvall.emojimadness;

import com.google.common.base.Joiner;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IndexedSprite;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Emoji Madness",
	description = "Enables shortcodes for emojis"
)
public class EmojiMadnessPlugin extends Plugin
{
	private static final Pattern TAG_REGEXP = Pattern.compile("<[^>]*>");
	private static final Pattern WHITESPACE_REGEXP = Pattern.compile("[\\s\\u00A0]");

	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private EmojiMadnessConfig config;

	private int modIconsStart = -1;

	@Provides
	EmojiMadnessConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EmojiMadnessConfig.class);
	}

	@Override
	protected void startUp()
	{
		loadEmojiIcons();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			loadEmojiIcons();
		}
	}

//	@Subscribe
//	public void onConfigChanged(ConfigChanged event)
//	{
//		if (event.getGroup().equals("emojimadness"))
//		{
//			return;
//		}
//	}

	private void loadEmojiIcons()
	{
		final IndexedSprite[] modIcons = client.getModIcons();
		if (modIconsStart != -1 || modIcons == null)
		{
			return;
		}

		final Emoji[] emojis = Emoji.values();
		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + emojis.length);
		modIconsStart = modIcons.length;

		for (int i = 0; i < emojis.length; i++)
		{
			final Emoji emoji = emojis[i];

			try
			{
				final BufferedImage image = emoji.loadImage();
				final IndexedSprite sprite = ImageUtil.getImageIndexedSprite(image, client);
				newModIcons[modIconsStart + i] = sprite;
			}
			catch (Exception ex)
			{
				log.warn("Failed to load the sprite for emoji " + emoji, ex);
			}
		}

		log.debug("Adding emoji icons");
		client.setModIcons(newModIcons);
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (client.getGameState() != GameState.LOGGED_IN || modIconsStart == -1)
		{
			return;
		}

		switch (chatMessage.getType())
		{
			case PUBLICCHAT:
			case MODCHAT:
			case FRIENDSCHAT:
			case PRIVATECHAT:
			case PRIVATECHATOUT:
			case MODPRIVATECHAT:
				break;
			default:
				return;
		}

		final MessageNode messageNode = chatMessage.getMessageNode();
		final String message = messageNode.getValue();
		final String updatedMessage = updateMessage(message);

		if (updatedMessage == null)
		{
			return;
		}

		messageNode.setRuneLiteFormatMessage(updatedMessage);
		chatMessageManager.update(messageNode);
		client.refreshChat();
	}

	@Subscribe
	public void onOverheadTextChanged(final OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof Player))
		{
			return;
		}

		final String message = event.getOverheadText();
		final String updatedMessage = updateMessage(message);

		if (updatedMessage == null)
		{
			return;
		}

		event.getActor().setOverheadText(updatedMessage);
	}

	@Nullable
	String updateMessage(final String message)
	{
		final String[] messageWords = WHITESPACE_REGEXP.split(message);
		List<String> newMessageWords = new ArrayList<>();

		boolean editedMessage = false;
		for (int i = 0; i < messageWords.length; i++)
		{
			// Remove tags except for <lt> and <gt>
			final String trigger = removeTags(messageWords[i]);

			List<List<String>> phrases = Emoji.getTriggers(trigger.toLowerCase());
			newMessageWords.add(messageWords[i]);

			if (phrases == null)
			{
				continue;
			}

			List<String> splitTrigger = null;
			middle:
			for (List<String> words : phrases)
			{
				if (i + words.size() > messageWords.length)
				{
					// Longest phrases first
					continue;
				}

				for (int j = 0; j < words.size(); j++)
				{
					if (!words.get(j).equals(messageWords[i+j].toLowerCase()))
					{
						continue middle;
					}
				}

				splitTrigger = words;
				// if we find a match we are done
				break;
			}

			if (splitTrigger == null)
			{
				continue;
			}

			String triggerPhrase = Joiner.on(" ").join(splitTrigger);

			final Emoji emoji = Emoji.getEmoji(triggerPhrase);
			final int emojiId = modIconsStart + emoji.ordinal();

			// The trigger is still in new message words
			newMessageWords.remove(newMessageWords.lastIndexOf(messageWords[i]));
			if (config.appendMode())
			{
				// Append the emoji to the phrase in a bracket
				newMessageWords.addAll(splitTrigger);
				newMessageWords.add("[<img=" + emojiId + ">]");
			}
			else
			{
				// Replace the phrase
				newMessageWords.add("<img=" + emojiId + ">");
			}

			i += splitTrigger.size() - 1;
			editedMessage = true;
		}

		// If we haven't edited the message any, don't update it.
		if (!editedMessage)
		{
			return null;
		}

		return Joiner.on(" ").join(newMessageWords);
	}

	/**
	 * Remove tags, except for &lt;lt&gt; and &lt;gt&gt;
	 *
	 * @return
	 */
	private static String removeTags(String str)
	{
		StringBuffer stringBuffer = new StringBuffer();
		Matcher matcher = TAG_REGEXP.matcher(str);
		while (matcher.find())
		{
			matcher.appendReplacement(stringBuffer, "");
			String match = matcher.group(0);
			switch (match)
			{
				case "<lt>":
				case "<gt>":
					stringBuffer.append(match);
					break;
			}
		}
		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
	}
}

