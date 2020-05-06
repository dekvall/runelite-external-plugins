package dekvall.chatesolangs;

import com.google.inject.Provides;
import dekvall.chatesolangs.interpreters.Brainfuck;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Chat Esolangs"
)
public class ChatEsolangsPlugin extends Plugin
{
	private static String BRAINFUCK_COMMAND = "?bf";
	private static final Pattern TAG_REGEXP = Pattern.compile("<[^>]*>");

	@Inject
	private Client client;

	@Inject
	private ChatEsolangsConfig config;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	private List<MessageNode> nodes = new ArrayList<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Chat Esolangs started!");
		//chatCommandManager.registerCommand(BRAINFUCK_COMMAND, this::brainfuck);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Chat Esolangs stopped!");
		chatCommandManager.unregisterCommand(BRAINFUCK_COMMAND);
	}

	@Subscribe
	public void onChatMessage(ChatMessage message)
	{
		String text = message.getMessage();

		if (message.getType() != ChatMessageType.PUBLICCHAT)
		{
			return;
		}

		if(text.startsWith("Eval"))
		{
			interpretMap();
		}
		else
		{
			nodes.add(message.getMessageNode());
		}

//		if (text.startsWith("?Bf"))
//		{
//			String result = Brainfuck.interpret(text.substring(3));
//			message.getMessageNode().setValue(result);
//		}
	}

	private void interpretMap()
	{
		for (MessageNode node : nodes)
		{
			log.info(node.getValue());
			log.info(unescapeTags(node.getValue()));
			String res = Brainfuck.interpret(unescapeTags(node.getValue()));
			String response = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append(res)
				.build();

			node.setValue(response);
		}
		client.refreshChat();
		nodes.clear();
	}

	@Provides
	ChatEsolangsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatEsolangsConfig.class);
	}

	private void brainfuck(ChatMessage chatMessage, String message)
	{
		if (message.length() <= BRAINFUCK_COMMAND.length())
		{
			return;
		}

		ChatMessageType type = chatMessage.getType();
		String code = message.substring(BRAINFUCK_COMMAND.length() + 1);

		String result = Brainfuck.interpret(code);

		String response = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(result)
			.build();

		final MessageNode messageNode = chatMessage.getMessageNode();
		messageNode.setRuneLiteFormatMessage(response);
		chatMessageManager.update(messageNode);
		client.refreshChat();
	}

	/**
	 * Unescape a string for widgets, replacing &lt;lt&gt; and &lt;gt&gt; with their unescaped counterparts
	 */
	public static String unescapeTags(String str)
	{
		StringBuffer out = new StringBuffer();
		Matcher matcher = TAG_REGEXP.matcher(str);

		while (matcher.find())
		{
			matcher.appendReplacement(out, "");
			String match = matcher.group(0);
			switch (match)
			{
				case "<lt>":
					out.append("<");
					break;
				case "<gt>":
					out.append(">");
					break;
				case "<br>":
					out.append("\n");
					break;
				default:
					out.append(match);
			}
		}
		matcher.appendTail(out);

		return out.toString();
	}
}
