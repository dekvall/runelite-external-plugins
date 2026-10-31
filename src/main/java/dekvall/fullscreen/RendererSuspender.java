package dekvall.fullscreen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;

/** Never destroys the AWT peer while a renderer owns it. */
@Slf4j
final class RendererSuspender implements FullscreenController.Renderers
{
	private final PluginManager pluginManager;
	private final ClientThread clientThread;
	private final Client client;
	private final int timeoutMillis;
	private final List<Plugin> stopped = new ArrayList<>();
	private final Set<Plugin> candidates = new LinkedHashSet<>();
	private long request;

	RendererSuspender(PluginManager pluginManager, ClientThread clientThread, Client client)
	{
		this(pluginManager, clientThread, client, 5000);
	}

	RendererSuspender(PluginManager pluginManager, ClientThread clientThread, Client client, int timeoutMillis)
	{
		this.pluginManager = pluginManager;
		this.clientThread = clientThread;
		this.client = client;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public void suspend(Runnable ready, Consumer<Exception> failed)
	{
		FullscreenController.requireEdt();
		long token = ++request;
		try
		{
			candidates.clear();
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if (plugin instanceof GpuPlugin)
				{
					candidates.add(plugin);
					candidates.addAll(pluginManager.conflictsForPlugin(plugin));
				}
			}
		}
		catch (Exception ex)
		{
			failAndRestore(failed, ex);
			return;
		}

		Timer timeout = new Timer(timeoutMillis, event ->
		{
			if (request == token)
			{
				++request;
				failAndRestore(failed, new IllegalStateException(
					"The GPU renderer did not stop within five seconds. Fullscreen was left unchanged."));
			}
		});
		timeout.setRepeats(false);
		timeout.start();
		poll(token, timeout, ready, failed);
	}

	/**
	 * Reads the GPU state on the client thread, then on the EDT:
	 * <ul>
	 * <li>an active renderer whose OpenGL setup has finished ({@code client.isGpu()}) is stopped;</li>
	 * <li>an active renderer that is still starting is left alone until it finishes. Its setup
	 * is queued on the client thread and waits for a valid canvas; stopping it then, and
	 * replacing the window, lets that stale setup run later on the new window beside the
	 * restarted renderer. One of them fails, and GPU's failure handling turns it off in
	 * RuneLite's settings;</li>
	 * <li>with no active renderer and no GPU left, the window may change.</li>
	 * </ul>
	 * A renderer restarted by RuneLite meanwhile (profile switch, plugin hub refresh) goes
	 * through the same steps. The EDT deadline bounds the whole wait.
	 */
	private void poll(long token, Timer timeout, Runnable ready, Consumer<Exception> failed)
	{
		if (request != token)
		{
			return;
		}
		try
		{
			clientThread.invokeLater((Runnable) () ->
			{
				try
				{
					boolean gpu = client.isGpu();
					SwingUtilities.invokeLater(() ->
					{
						if (request != token)
						{
							return;
						}
						boolean active;
						try
						{
							active = stopStartedRenderers(gpu);
						}
						catch (Exception ex)
						{
							abort(token, timeout, failed, ex);
							return;
						}
						if (!gpu && !active)
						{
							++request;
							timeout.stop();
							ready.run();
						}
						else
						{
							Timer retry = new Timer(50, event -> poll(token, timeout, ready, failed));
							retry.setRepeats(false);
							retry.start();
						}
					});
				}
				catch (RuntimeException ex)
				{
					SwingUtilities.invokeLater(() -> abort(token, timeout, failed, ex));
				}
			});
		}
		catch (RuntimeException ex)
		{
			abort(token, timeout, failed, ex);
		}
	}

	/** Stops active renderers once they have finished starting; true while any is active. */
	private boolean stopStartedRenderers(boolean gpu) throws Exception
	{
		boolean active = false;
		for (Plugin plugin : candidates)
		{
			if (!pluginManager.isPluginActive(plugin))
			{
				continue;
			}
			if (!gpu)
			{
				// Still starting (or failing, after which it turns itself off): wait.
				active = true;
				continue;
			}
			boolean added = !stopped.contains(plugin);
			if (added)
			{
				// PluginManager removes a plugin from its active list before invoking
				// shutDown(), which may throw. Retain it for recovery in that case.
				stopped.add(plugin);
			}
			else
			{
				log.info("Renderer {} restarted during a fullscreen transition; stopping it again",
					plugin.getClass().getSimpleName());
			}
			if (!pluginManager.stopPlugin(plugin))
			{
				if (added)
				{
					stopped.remove(plugin);
				}
				active |= pluginManager.isPluginActive(plugin);
			}
			// Otherwise its teardown runs on the client thread; client.isGpu() turns false.
		}
		return active;
	}

	private void abort(long token, Timer timeout, Consumer<Exception> failed, Exception ex)
	{
		if (request == token)
		{
			++request;
			timeout.stop();
			failAndRestore(failed, ex);
		}
	}

	private void failAndRestore(Consumer<Exception> failed, Exception failure)
	{
		try
		{
			resume();
		}
		catch (RuntimeException restartFailure)
		{
			failure.addSuppressed(restartFailure);
			failure = new IllegalStateException(failure.getMessage() + "\n" + restartFailure.getMessage(), failure);
		}
		failed.accept(failure);
	}

	@Override
	public void resume()
	{
		FullscreenController.requireEdt();
		List<Plugin> restore = new ArrayList<>(stopped);
		stopped.clear();
		IllegalStateException failure = null;
		for (Plugin plugin : restore)
		{
			try
			{
				if (pluginManager.getPlugins().contains(plugin) && pluginManager.isPluginEnabled(plugin))
				{
					pluginManager.startPlugin(plugin);
				}
			}
			catch (Exception ex)
			{
				log.error("Unable to restart GPU plugin {}", plugin.getClass().getSimpleName(), ex);
				if (failure == null)
				{
					failure = new IllegalStateException(
						"A GPU renderer could not restart. Disable and re-enable your GPU/117 HD plugin. See RuneLite logs for details.");
				}
				failure.addSuppressed(ex);
			}
		}
		if (failure != null)
		{
			throw failure;
		}
	}
}
