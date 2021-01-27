package dev.dkvl.womutils;

import com.google.gson.Gson;
import dev.dkvl.womutils.beans.NameChangeEntry;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
class WomClient
{
	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;


	void sendNameChanges(NameChangeEntry[] changes)
	{
		Request request = createRequest(changes, "names", "bulk");
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

	void sendRequest(Request request, Callback callback)
	{
		okHttpClient.newCall(request).enqueue(callback);
	}

	Request createRequest(Object payload, String... pathSegments)
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

	Request createRequest(String... pathSegments)
	{
		HttpUrl url = buildUrl(pathSegments);
		return new Request.Builder()
			.header("User-Agent", "RuneLite")
			.url(url)
			.build();
	}

	HttpUrl buildUrl(String[] pathSegments)
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

}
