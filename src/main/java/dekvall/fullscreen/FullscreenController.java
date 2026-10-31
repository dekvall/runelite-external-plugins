package dekvall.fullscreen;

import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;

/** Serializes window and renderer changes on the EDT. */
@Slf4j
final class FullscreenController
{
	interface Window
	{
		void validate(Mode mode);
		boolean needsRendererRestart(Mode mode);
		void enter(Mode mode);
		void exit();
		default boolean moveToMonitor(Mode mode)
		{
			return false;
		}
	}

	interface Renderers
	{
		void suspend(Runnable ready, Consumer<Exception> failed);
		void resume();
	}

	private final Window window;
	private final Renderers renderers;
	private final Runnable changed;
	private final Consumer<Exception> error;
	private boolean running = true;
	private boolean transitioning;
	private Mode activeMode;
	private Mode queuedMode;
	private boolean queuedChange;
	private boolean queuedMonitorChange;

	FullscreenController(Window window, Renderers renderers, Runnable changed, Consumer<Exception> error)
	{
		this.window = window;
		this.renderers = renderers;
		this.changed = changed;
		this.error = error;
	}

	boolean isActive()
	{
		return activeMode != null;
	}

	boolean isTransitioning()
	{
		return transitioning;
	}

	Mode currentMode()
	{
		return activeMode == null ? Mode.WINDOWED : activeMode;
	}

	void toggle(Mode mode)
	{
		requireEdt();
		if (running && !transitioning)
		{
			change(isActive() ? null : mode == Mode.WINDOWED ? Mode.BORDERLESS : mode);
		}
	}

	void changeMode(Mode mode)
	{
		requireEdt();
		mode = mode == Mode.WINDOWED ? null : mode;
		if (!running)
		{
			return;
		}
		if (transitioning)
		{
			queuedMode = mode;
			queuedChange = true;
		}
		else if (activeMode != mode)
		{
			change(mode);
		}
	}

	void close()
	{
		requireEdt();
		running = false;
		queuedMode = null;
		queuedChange = false;
		queuedMonitorChange = false;
		if (!transitioning && isActive())
		{
			change(null);
		}
	}

	/**
	 * Re-applies the active fullscreen mode on the currently selected monitor.
	 * The mode comes from the controller, never from the settings dropdown, so a
	 * stale dropdown value cannot exit fullscreen or override a queued mode change.
	 */
	void changeMonitor()
	{
		requireEdt();
		if (!running)
		{
			return;
		}
		if (transitioning)
		{
			queuedMonitorChange = true;
		}
		else if (isActive())
		{
			change(activeMode);
		}
	}

	private void change(Mode target)
	{
		if (target != null)
		{
			try
			{
				window.validate(target);
			}
			catch (RuntimeException | LinkageError ex)
			{
				changed.run();
				error.accept(asException(ex));
				return;
			}
		}
		boolean restart = (isActive() && window.needsRendererRestart(activeMode))
			|| (target != null && window.needsRendererRestart(target));
		transitioning = true;
		long requestedAt = System.nanoTime();
		log.info("Fullscreen request: {} -> {}, renderer restart={}", activeMode, target, restart);
		changed.run();
		Runnable ready = () ->
		{
			boolean completed = false;
			try
			{
				boolean keepFullscreen = running && target != null && target == activeMode
					&& !(queuedChange && queuedMode == null) && window.moveToMonitor(target);
				if (isActive() && !keepFullscreen)
				{
					window.exit();
					activeMode = null;
				}
				if (running && target != null && !keepFullscreen && !(queuedChange && queuedMode == null))
				{
					activeMode = target;
					window.enter(target);
				}
				completed = true;
			}
			catch (RuntimeException | LinkageError ex)
			{
				try
				{
					window.exit();
					activeMode = null;
				}
				catch (RuntimeException | LinkageError rollback)
				{
					ex.addSuppressed(rollback);
				}
				error.accept(asException(ex));
			}
			finally
			{
				try
				{
					if (restart)
					{
						renderers.resume();
					}
				}
				catch (RuntimeException restartFailure)
				{
					queuedMode = null;
					queuedChange = false;
					error.accept(restartFailure);
				}
				finally
				{
					transitioning = false;
					log.info("Fullscreen request settled: active mode={}, elapsed={} ms",
						activeMode, (System.nanoTime() - requestedAt) / 1000000);
					Mode next = queuedMode;
					boolean modeChanged = queuedChange;
					boolean monitorChanged = queuedMonitorChange;
					queuedMode = null;
					queuedChange = false;
					queuedMonitorChange = false;
					if (running && completed && modeChanged && next != activeMode)
					{
						change(next);
					}
					else if (running && completed && monitorChanged && isActive())
					{
						change(activeMode);
					}
					else
					{
						changed.run();
					}
				}
			}
		};
		if (restart)
		{
			Consumer<Exception> failed = ex ->
			{
				transitioning = false;
				queuedMode = null;
				queuedChange = false;
				queuedMonitorChange = false;
				changed.run();
				error.accept(ex);
			};
			try
			{
				renderers.suspend(ready, failed);
			}
			catch (RuntimeException ex)
			{
				try
				{
					renderers.resume();
				}
				catch (RuntimeException restartFailure)
				{
					ex.addSuppressed(restartFailure);
				}
				failed.accept(ex);
			}
		}
		else
		{
			ready.run();
		}
	}

	private static Exception asException(Throwable ex)
	{
		return ex instanceof Exception ? (Exception) ex : new IllegalStateException("Fullscreen is unavailable", ex);
	}

	static void requireEdt()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Fullscreen transitions must run on the EDT");
		}
	}
}
