package dev.dkvl.womutils;

import java.io.IOException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

@Slf4j
@RequiredArgsConstructor
class WomCallback implements Callback
{
	@Override
	public void onFailure(Call call, IOException e)
	{
		log.warn("Error submitting request, caused by {}.", e.getMessage());
	}

	@Override
	public void onResponse(Call call, Response response)
	{
		responseConsumer.accept(response);
		response.close();
	}

	private final Consumer<Response> responseConsumer;
}
