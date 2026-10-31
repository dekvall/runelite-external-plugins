package dekvall.fullscreen;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.OSType;

/**
 * Owns exactly one frame and the display device used on entry. Uses only AWT/Swing (no
 * native code): borderless replaces the window's peer with an undecorated one, so the
 * controller always stops GPU renderers first; exclusive uses
 * {@link GraphicsDevice#setFullScreenWindow}.
 */
@Slf4j
final class FullscreenWindow implements FullscreenController.Window
{
	/** The windowed placement fullscreen will restore, in RuneLite's own client-bounds terms. */
	static final class Placement
	{
		final Rectangle content;
		final boolean maximized;

		Placement(Rectangle content, boolean maximized)
		{
			this.content = content;
			this.maximized = maximized;
		}

		/** Matches ClientUI's "x:y:width:height:c" format (content area, excluding insets). */
		String clientBoundsConfig()
		{
			return content.x + ":" + content.y + ":" + content.width + ":" + content.height + ":c";
		}
	}

	private final Frame frame;
	private final Supplier<GraphicsConfiguration> monitor;
	private Supplier<GraphicsDevice[]> screens = () -> GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
	private GraphicsConfiguration preparedMonitor;
	private Mode preparedMode;
	private final ComponentAdapter boundsListener;
	private Rectangle normalBounds;
	private Snapshot saved;
	private Rectangle fullscreenBounds;
	private GraphicsDevice exclusiveDevice;
	/** Display covered by borderless; its bounds are re-read so resolution changes are followed. */
	private GraphicsDevice borderlessDevice;
	private Mode mode;
	private boolean fitting;
	private boolean fitQueued;
	private boolean peerWasDisposed;
	private final Timer displayTimer;

	private static final class Snapshot
	{
		private final Rectangle bounds;
		private final int state;
		private final boolean undecorated;
		private boolean resizable;
		private boolean alwaysOnTop;
		private final Insets insets;

		private Snapshot(Frame frame, int state, Rectangle bounds)
		{
			this.state = state;
			this.bounds = new Rectangle(bounds);
			undecorated = frame.isUndecorated();
			resizable = frame.isResizable();
			alwaysOnTop = frame.isAlwaysOnTop();
			Insets current = frame.getInsets();
			insets = current == null ? new Insets(0, 0, 0, 0) : (Insets) current.clone();
		}
	}

	FullscreenWindow(Frame frame)
	{
		this(frame, frame::getGraphicsConfiguration);
	}

	FullscreenWindow(Frame frame, Supplier<GraphicsConfiguration> monitor)
	{
		this.frame = frame;
		this.monitor = monitor;
		normalBounds = frame.getBounds();
		boundsListener = new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent event)
			{
				boundsChanged();
			}

			@Override
			public void componentMoved(ComponentEvent event)
			{
				boundsChanged();
			}
		};
		frame.addComponentListener(boundsListener);
		displayTimer = new Timer(1000, event -> boundsChanged());
	}

	@Override
	public boolean needsRendererRestart(Mode mode)
	{
		// Borderless recreates the window's native peer; never do that while a GPU
		// renderer owns its surface. Exclusive keeps the previous conservative behavior.
		return true;
	}

	@Override
	public void validate(Mode requestedMode)
	{
		FullscreenController.requireEdt();
		GraphicsConfiguration configuration = monitor.get();
		validateConfiguration(requestedMode, configuration);
		// Mode changes temporarily restore the original window before re-entry.
		// Keep the chosen display from before that restoration, especially CURRENT.
		preparedMonitor = configuration;
		preparedMode = requestedMode;
	}

	private void validateConfiguration(Mode requestedMode, GraphicsConfiguration configuration)
	{
		if (configuration == null || !frame.isDisplayable())
		{
			throw new IllegalStateException("RuneLite's window is not ready.");
		}
		GraphicsDevice device = configuration.getDevice();
		if (requestedMode == Mode.EXCLUSIVE
			&& (!device.isFullScreenSupported() || OSType.getOSType() == OSType.MacOS))
		{
			throw new IllegalStateException("Exclusive fullscreen is unavailable. Select Borderless in Fullscreen settings.");
		}
	}

	@Override
	public void enter(Mode requestedMode)
	{
		FullscreenController.requireEdt();
		if (saved != null)
		{
			throw new IllegalStateException("The previous window state must be restored before entering fullscreen again.");
		}
		GraphicsConfiguration configuration = preparedMonitor != null && preparedMode == requestedMode
			? preparedMonitor : monitor.get();
		preparedMonitor = null;
		preparedMode = null;
		validateConfiguration(requestedMode, configuration);
		GraphicsDevice device = configuration.getDevice();

		// Leave the maximized state first. Windows reports the restored rectangle as soon as
		// the state changes, which is the only portable way to learn it when RuneLite has
		// been maximized for the whole session.
		int state = frame.getExtendedState();
		Rectangle restored = frame.getBounds();
		if ((state & Frame.MAXIMIZED_BOTH) != 0)
		{
			Rectangle maximized = new Rectangle(restored);
			frame.setExtendedState(Frame.NORMAL);
			Rectangle now = frame.getBounds();
			restored = now.equals(maximized) ? normalBounds : now;
		}
		else if (state != Frame.NORMAL)
		{
			restored = normalBounds;
		}
		saved = new Snapshot(frame, state, restored);
		fullscreenBounds = new Rectangle(configuration.getBounds());
		mode = requestedMode;
		if (requestedMode == Mode.EXCLUSIVE)
		{
			exclusiveDevice = device;
			device.setFullScreenWindow(frame);
		}
		else
		{
			// The controller waits for GPU release before this peer replacement.
			borderlessDevice = device;
			peerWasDisposed = true;
			frame.dispose();
			frame.setUndecorated(true);
			frame.setResizable(false);
			frame.setBounds(fullscreenBounds);
			frame.setVisible(true);
			peerWasDisposed = false;
			displayTimer.start();
		}
		fit();
	}

	@Override
	public boolean moveToMonitor(Mode requestedMode)
	{
		FullscreenController.requireEdt();
		if (requestedMode != Mode.BORDERLESS || mode != Mode.BORDERLESS || saved == null)
		{
			return false;
		}
		GraphicsConfiguration configuration = preparedMonitor != null && preparedMode == requestedMode
			? preparedMonitor : monitor.get();
		validateConfiguration(requestedMode, configuration);
		preparedMonitor = null;
		preparedMode = null;
		// The window is already undecorated: moving it needs no new peer, and the original
		// placement snapshot stays untouched for exit.
		borderlessDevice = configuration.getDevice();
		fullscreenBounds = new Rectangle(configuration.getBounds());
		fit();
		return true;
	}

	@Override
	public void exit()
	{
		FullscreenController.requireEdt();
		if (saved == null)
		{
			return;
		}
		mode = null; // Stop the bounds listener before restoring the original window.
		displayTimer.stop();
		if (exclusiveDevice != null)
		{
			if (exclusiveDevice.getFullScreenWindow() == frame)
			{
				exclusiveDevice.setFullScreenWindow(null);
			}
			exclusiveDevice = null;
		}
		if (!frame.isDisplayable() && !peerWasDisposed)
		{
			// RuneLite closed its window while a renderer callback was pending.
			// Cleanup must not recreate an application window that has been closed.
			clearSavedState();
			return;
		}
		if (frame.getExtendedState() != Frame.NORMAL)
		{
			frame.setExtendedState(Frame.NORMAL);
		}
		if (frame.isUndecorated() != saved.undecorated)
		{
			peerWasDisposed = true;
			frame.dispose();
			frame.setUndecorated(saved.undecorated);
		}
		frame.setResizable(saved.resizable);
		frame.setAlwaysOnTop(saved.alwaysOnTop);
		frame.setBounds(saved.bounds);
		if (!frame.isVisible())
		{
			frame.setVisible(true);
		}
		peerWasDisposed = false;
		if (saved.state != Frame.NORMAL)
		{
			frame.setExtendedState(saved.state);
		}
		normalBounds = new Rectangle(saved.bounds);
		clearSavedState();
	}

	/**
	 * RuneLite changed "Lock window size" or "Always on top" while fullscreen. Keep the
	 * user's new choice for exit, and keep the borderless window non-resizable.
	 */
	void windowSettingsChanged(boolean resizeLockChanged)
	{
		FullscreenController.requireEdt();
		if (saved == null || mode == null)
		{
			return;
		}
		saved.alwaysOnTop = frame.isAlwaysOnTop();
		if (!resizeLockChanged)
		{
			return;
		}
		saved.resizable = frame.isResizable();
		if (mode == Mode.BORDERLESS && frame.isResizable())
		{
			frame.setResizable(false);
		}
	}

	/** Null unless fullscreen holds a snapshot of the windowed placement. */
	Placement restorePlacement()
	{
		if (saved == null)
		{
			return null;
		}
		Insets insets = saved.insets;
		Rectangle content = new Rectangle(saved.bounds.x + insets.left, saved.bounds.y + insets.top,
			saved.bounds.width - insets.left - insets.right, saved.bounds.height - insets.top - insets.bottom);
		return new Placement(content, (saved.state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
	}

	/** Tests supply their own displays. */
	void setScreens(Supplier<GraphicsDevice[]> screens)
	{
		this.screens = screens;
	}

	private void clearSavedState()
	{
		saved = null;
		borderlessDevice = null;
		fullscreenBounds = null;
		peerWasDisposed = false;
	}

	void detach()
	{
		preparedMonitor = null;
		preparedMode = null;
		displayTimer.stop();
		frame.removeComponentListener(boundsListener);
	}

	private void boundsChanged()
	{
		if (!frame.isDisplayable())
		{
			displayTimer.stop();
			return;
		}
		if (mode == null)
		{
			if (saved == null && frame.getExtendedState() == Frame.NORMAL)
			{
				normalBounds = frame.getBounds();
			}
			return;
		}
		// RuneLite may resize the frame when its sidebar changes, and display settings may
		// change while fullscreen. Refit after layout, without packing the window.
		if (!fitting && !fitQueued)
		{
			fitQueued = true;
			SwingUtilities.invokeLater(() ->
			{
				fitQueued = false;
				if (mode != null && (frame.getExtendedState() & Frame.ICONIFIED) == 0)
				{
					try
					{
						fit();
					}
					catch (RuntimeException ex)
					{
						displayTimer.stop();
						log.warn("Unable to maintain fullscreen monitor bounds", ex);
					}
				}
			});
		}
	}

	private void fit()
	{
		if (mode == Mode.BORDERLESS && borderlessDevice != null)
		{
			fullscreenBounds = currentBounds(borderlessDevice);
		}
		if (fullscreenBounds.equals(frame.getBounds()))
		{
			return;
		}
		fitting = true;
		try
		{
			frame.setBounds(fullscreenBounds);
		}
		finally
		{
			fitting = false;
		}
	}

	/** Re-reads the display's bounds; uses the window's display if the original was disconnected. */
	private Rectangle currentBounds(GraphicsDevice device)
	{
		try
		{
			GraphicsDevice target = null;
			for (GraphicsDevice candidate : screens.get())
			{
				if (candidate == device || candidate.getIDstring().equals(device.getIDstring()))
				{
					target = candidate;
					break;
				}
			}
			if (target == null)
			{
				target = frame.getGraphicsConfiguration().getDevice();
			}
			return new Rectangle(target.getDefaultConfiguration().getBounds());
		}
		catch (RuntimeException headlessOrChanging)
		{
			return new Rectangle(fullscreenBounds);
		}
	}
}
