package dev.dkvl.womutils.web;

import com.google.gson.Gson;
import dev.dkvl.womutils.beans.GroupInfoWithMemberships;
import dev.dkvl.womutils.beans.NameChangeEntry;
import dev.dkvl.womutils.beans.ParticipantWithStanding;
import dev.dkvl.womutils.beans.WomStatus;
import dev.dkvl.womutils.beans.ParticipantWithCompetition;
import dev.dkvl.womutils.beans.GroupMemberAddition;
import dev.dkvl.womutils.beans.Member;
import dev.dkvl.womutils.beans.GroupMemberRemoval;
import dev.dkvl.womutils.beans.PlayerInfo;
import dev.dkvl.womutils.beans.WomPlayerUpdate;
import dev.dkvl.womutils.events.WomOngoingPlayerCompetitionsFetched;
import dev.dkvl.womutils.events.WomUpcomingPlayerCompetitionsFetched;
import dev.dkvl.womutils.ui.WomIconHandler;
import dev.dkvl.womutils.WomUtilsConfig;
import dev.dkvl.womutils.events.WomGroupMemberAdded;
import dev.dkvl.womutils.events.WomGroupMemberRemoved;
import dev.dkvl.womutils.events.WomGroupSynced;
import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class WomClient
{
	@Inject
	private OkHttpClient okHttpClient;

	private Gson gson;

	@Inject
	private WomIconHandler iconHandler;

	@Inject
	private Client client;

	@Inject
	private WomUtilsConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	private static final Color SUCCESS = new Color(170, 255, 40);
	private static final Color ERROR = new Color(204, 66, 66);

	private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##");

	@Inject
	public WomClient(Gson gson)
	{
		this.gson = gson.newBuilder()
				.setDateFormat(DateFormat.FULL, DateFormat.FULL)
				.create();
	}

	public void submitNameChanges(NameChangeEntry[] changes)
	{
		Request request = createRequest(changes, HttpMethod.POST, "names", "bulk");
		sendRequest(request);
		log.info("Submitted {} name changes to WOM", changes.length);
	}

	void sendRequest(Request request)
	{
		sendRequest(request, r -> {});
	}

	void sendRequest(Request request, Consumer<Response> consumer)
	{
		sendRequest(request, new WomCallback(consumer));
	}

	void sendRequest(Request request, Consumer<Response> consumer, Consumer<Exception> exceptionConsumer)
	{
		sendRequest(request, new WomCallback(consumer, exceptionConsumer));
	}

	void sendRequest(Request request, Callback callback)
	{
		okHttpClient.newCall(request).enqueue(callback);
	}

	private Request createRequest(Object payload, String... pathSegments)
	{
		return createRequest(payload, HttpMethod.POST, pathSegments);
	}

	private Request createRequest(Object payload, HttpMethod httpMethod, String... pathSegments)
	{
		HttpUrl url = buildUrl(pathSegments);
		RequestBody body = RequestBody.create(
			MediaType.parse("application/json; charset=utf-8"),
			gson.toJson(payload)
		);

		Request.Builder requestBuilder = new Request.Builder()
			.header("User-Agent", "WiseOldMan RuneLite Plugin")
			.url(url);

		if (httpMethod == HttpMethod.PUT)
		{
			return requestBuilder.put(body).build();
		}
		else if (httpMethod == HttpMethod.DELETE)
		{
			return requestBuilder.delete(body).build();
		}


		return requestBuilder.post(body).build();
	}

	private Request createRequest(String... pathSegments)
	{
		HttpUrl url = buildUrl(pathSegments);
		return new Request.Builder()
			.header("User-Agent", "WiseOldMan RuneLite Plugin")
			.url(url)
			.build();
	}

	private HttpUrl buildUrl(String[] pathSegments)
	{
		HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
			.scheme("https");

		boolean isSeasonal = client.getWorldType().contains(WorldType.SEASONAL);

		if (isSeasonal)
		{
			urlBuilder.host("league.wiseoldman.net")
				.addPathSegment("api");
		}
		else
		{
			urlBuilder.host("api.wiseoldman.net");
		}

		urlBuilder.addPathSegment("v2");

		for (String pathSegment : pathSegments)
		{
			if (pathSegment.startsWith("?"))
			{
				// A query param
				String[] kv = pathSegment.substring(1).split("=");
				urlBuilder.addQueryParameter(kv[0], kv[1]);
			}
			else
			{
				urlBuilder.addPathSegment(pathSegment);
			}
		}


		return urlBuilder.build();
	}

	public void importGroupMembers()
	{
		if (config.groupId() > 0)
		{
			Request request = createRequest("groups", "" + config.groupId());
			sendRequest(request, this::importMembersCallback);
		}
	}

	private void importMembersCallback(Response response)
	{
		if (!response.isSuccessful())
		{
			return;
		}

		GroupInfoWithMemberships groupInfo = parseResponse(response, GroupInfoWithMemberships.class);
		postEvent(new WomGroupSynced(groupInfo, true));
	}

	private void syncClanMembersCallBack(Response response)
	{
		final String message;

		if (response.isSuccessful())
		{
			GroupInfoWithMemberships data = parseResponse(response, GroupInfoWithMemberships.class);
			postEvent(new WomGroupSynced(data));
		}
		else
		{
			WomStatus data = parseResponse(response, WomStatus.class);
			message = "Error: " + data.getMessage();
			sendResponseToChat(message, ERROR);
		}
	}

	private void removeMemberCallback(Response response, String username)
	{
		final String message;
		final WomStatus data = parseResponse(response, WomStatus.class);

		if (response.isSuccessful())
		{
			postEvent(new WomGroupMemberRemoved(username));
		}
		else
		{
			message = "Error: " + data.getMessage();
			sendResponseToChat(message, ERROR);
		}
	}

	private void addMemberCallback(Response response, String username)
	{
		final String message;

		if (response.isSuccessful())
		{
			postEvent(new WomGroupMemberAdded(username));
		}
		else
		{
			WomStatus data = parseResponse(response, WomStatus.class);
			message = "Error: " + data.getMessage();
			sendResponseToChat(message, ERROR);
		}
	}

	private void playerOngoingCompetitionsCallback(String username, Response response)
	{
		if (response.isSuccessful())
		{
			ParticipantWithStanding[] comps = parseResponse(response, ParticipantWithStanding[].class);
			postEvent(new WomOngoingPlayerCompetitionsFetched(username, comps));
		}
		else
		{
			WomStatus data = parseResponse(response, WomStatus.class);
			String message = "Error: " + data.getMessage();
			sendResponseToChat(message, ERROR);
		}
	}
	private void playerUpcomingCompetitionsCallback(String username, Response response)
	{
		if (response.isSuccessful())
		{
			ParticipantWithCompetition[] comps = parseResponse(response, ParticipantWithCompetition[].class);
			postEvent(new WomUpcomingPlayerCompetitionsFetched(username, comps));
		}
		else
		{
			WomStatus data = parseResponse(response, WomStatus.class);
			String message = "Error: " + data.getMessage();
			sendResponseToChat(message, ERROR);
		}
	}


	private <T> T parseResponse(Response r, Class<T> clazz)
	{
		return parseResponse(r, clazz, false);
	}

	private <T> T parseResponse(Response r, Class<T> clazz, boolean nullIferror)
	{
		if (nullIferror && !r.isSuccessful())
		{
			return null;
		}

		String body;
		try
		{
			body = r.body().string();
		}
		catch (IOException e)
		{
			log.error("Could not read response {}", e.getMessage());
			return null;
		}

		return gson.fromJson(body, clazz);
	}

	private void sendResponseToChat(String message, Color color)
	{
		ChatMessageBuilder cmb = new ChatMessageBuilder();
		cmb.append("[WOM] ");
		cmb.append(color, message);

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(cmb.build())
			.build());
	}

	public void syncClanMembers(ArrayList<Member> clanMembers)
	{
		GroupMemberAddition payload = new GroupMemberAddition(config.verificationCode(), clanMembers);
		Request request = createRequest(payload, HttpMethod.PUT, "groups", "" + config.groupId());
		sendRequest(request, this::syncClanMembersCallBack);
	}

	public void addGroupMember(String username)
	{
		ArrayList<Member> memberToAdd = new ArrayList<>();
		memberToAdd.add(new Member(username.toLowerCase(), "member"));

		GroupMemberAddition payload = new GroupMemberAddition(config.verificationCode(), memberToAdd);
		Request request = createRequest(payload, "groups", "" + config.groupId(), "members");
		sendRequest(request, r -> addMemberCallback(r, username));
	}

	public void removeGroupMember(String username)
	{
		GroupMemberRemoval payload = new GroupMemberRemoval(config.verificationCode(), new String[] {username.toLowerCase()});
		Request request = createRequest(payload, HttpMethod.DELETE,"groups", "" + config.groupId(), "members");
		sendRequest(request, r -> removeMemberCallback(r, username));
	}

	public void commandLookup(String username, WomCommand command, ChatMessage chatMessage)
	{
		Request request = createRequest("players", username);
		sendRequest(request, r -> commandCallback(r, command, chatMessage));
	}

	private void commandCallback(Response response, WomCommand command, ChatMessage chatMessage)
	{
		if (!response.isSuccessful())
		{
			return;
		}

		final PlayerInfo info = parseResponse(response, PlayerInfo.class);

		final double time;

		// TODO: Write something proper that doesn't use reflection
		try
		{
			Field field = info.getClass().getDeclaredField(command.getField());
			field.setAccessible(true);
			time = (double) field.get(info);
		}
		catch (Throwable e)
		{
			log.warn("{}", e.getMessage());
			return;
		}

		String value = NUMBER_FORMAT.format(time);

		String message = new ChatMessageBuilder()
			.append(ChatColorType.NORMAL)
			.append(command.getMessage())
			.append(ChatColorType.HIGHLIGHT)
			.append(value)
			.append(".")
			.build();

		final MessageNode messageNode = chatMessage.getMessageNode();
		messageNode.setRuneLiteFormatMessage(message);
		client.refreshChat();
	}

	private void postEvent(Object event)
	{
		// Handle callbacks on the client thread
		clientThread.invokeLater(() -> eventBus.post(event));
	}

	public void fetchUpcomingPlayerCompetitions(String username)
	{
		Request request = createRequest("players", username, "competitions", "?status=upcoming");
		sendRequest(request, r -> playerUpcomingCompetitionsCallback(username, r));
	}

	public void fetchOngoingPlayerCompetitions(String username)
	{
		Request request = createRequest("players", username, "competitions", "standings", "?status=ongoing");
		sendRequest(request, r -> playerOngoingCompetitionsCallback(username, r));
	}

	public void updatePlayer(String username, long accountHash)
	{
		Request request = createRequest(new WomPlayerUpdate(accountHash), "players", username);
		sendRequest(request);
	}

	public CompletableFuture<PlayerInfo> lookupAsync(String username)
	{
		CompletableFuture<PlayerInfo> future = new CompletableFuture<>();
		Request request = createRequest("players", username);
		sendRequest(request, r-> future.complete(parseResponse(r, PlayerInfo.class, true)), future::completeExceptionally);
		return future;
	}

	public CompletableFuture<PlayerInfo> updateAsync(String username)
	{
		CompletableFuture<PlayerInfo> future = new CompletableFuture<>();
		Request request = createRequest(new Object(), "players", username);
		sendRequest(request, r-> future.complete(parseResponse(r, PlayerInfo.class, true)), future::completeExceptionally);
		return future;
	}
}
