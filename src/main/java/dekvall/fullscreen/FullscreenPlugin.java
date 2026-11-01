/*
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
package dekvall.fullscreen;

import com.google.inject.Provides;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.OSType;

@Slf4j
@PluginDescriptor(
	name = "Fullscreen",
	description = "Requires custom custom chrome to be disabled"
)
public class FullscreenPlugin extends Plugin
{
	private static final int REVERT_SECONDS = 15;
	private static final String KEEP_OPTION = "Keep";
	private static final String REVERT_OPTION = "Revert";
	private static final long GPU_STOP_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

	@Inject
	private ClientUI clientUI;
	@Inject
	private Client client;
	@Inject
	private ConfigManager configManager;
	@Inject
	private KeyManager keyManager;
	@Inject
	private FullscreenConfig config;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ClientThread clientThread;
	@Inject
	PluginManager pluginManager;
	private Frame clientFrame;
	private int prevExtState;
	private Rectangle prevBounds;
	private boolean prevResizable;
	private Rectangle rejectedBounds;
	// Parts of the monitor the window manager won't let the window cover, such as GNOME's top bar
	private Insets panelInsets;
	// Whether the window has been where it should be yet, i.e. whether to correct it
	private boolean reachedFullscreenBounds;
	private GraphicsConfiguration gc;
	private Mode fullscreenMode;
	// isActivated and running are also read on the client thread, while borderless waits for the GPU renderer to stop
	private volatile boolean isActivated;
	// Borderless mode waits for the GPU plugins to stop before changing the window, so it is in progress for
	// a while after enableFullscreen()/disableFullscreen() return.
	private boolean transitioning;
	private volatile boolean running;

	private HotkeyListener hotkeyListener = createHotkeyListener();

	// RuneLite widens the frame by the sidebar's width when the sidebar is opened (with the default
	// 'Keep game size' resize type), which pushes a fullscreen window past the edge of its monitor.
	private final ComponentListener fullscreenBoundsKeeper = new ComponentAdapter()
	{
		@Override
		public void componentResized(ComponentEvent e)
		{
			restoreFullscreenBounds();
		}

		@Override
		public void componentMoved(ComponentEvent e)
		{
			restoreFullscreenBounds();
		}
	};

	private final BufferedImage iconEnable = ImageUtil.loadImageResource(getClass(), "fullscreen_on.png");
	private final BufferedImage iconDisable = ImageUtil.loadImageResource(getClass(), "fullscreen_off.png");
	private final NavigationButton navButtonEnable = NavigationButton.builder()
		.tooltip("Enable fullscreen")
		.icon(iconEnable)
		.onClick(this::enableFullscreen)
		.build();
	private final NavigationButton navButtonDisable = NavigationButton.builder()
		.tooltip("Disable fullscreen")
		.icon(iconDisable)
		.onClick(this::disableFullscreen)
		.build();

	@Override
	protected void startUp()
	{
		clientFrame = getClientFrame();
		if (clientFrame == null)
		{
			return;
		}

		running = true;
		keyManager.registerKeyListener(hotkeyListener);
		clientToolbar.addNavigation(isActivated ? navButtonDisable : navButtonEnable);
	}

	private void enableFullscreen()
	{
		if (isActivated || transitioning)
		{
			return;
		}

		// Read once here and kept until fullscreen is disabled, so leaving fullscreen uses the same monitor
		gc = clientUI.getGraphicsConfiguration();
		if (!canEnable())
		{
			return;
		}

		prevExtState = clientFrame.getExtendedState();
		prevBounds = clientFrame.getBounds();
		prevResizable = clientFrame.isResizable();
		fullscreenMode = config.fullscreenMode();

		if (fullscreenMode == Mode.EXCLUSIVE)
		{
			enableExclusive();
		}
		else
		{
			enableBorderless();
		}

		setActivated(true);

		if (fullscreenMode == Mode.EXCLUSIVE && !config.exclusiveConfirmed())
		{
			// Queued so it runs after the caller has finished, with the plugin state above already updated.
			SwingUtilities.invokeLater(this::confirmExclusive);
		}
	}

	/**
	 * Some setups show a black screen in exclusive mode, leaving the user unable to see the button
	 * to turn it off. Like the Windows display settings dialog, revert unless the user can see this
	 * dialog and confirm it in time.
	 */
	private void confirmExclusive()
	{
		JOptionPane pane = new JOptionPane(revertMessage(REVERT_SECONDS), JOptionPane.QUESTION_MESSAGE,
			JOptionPane.DEFAULT_OPTION, null, new Object[]{KEEP_OPTION, REVERT_OPTION}, REVERT_OPTION);
		JDialog dialog = pane.createDialog(clientFrame, "Keep exclusive fullscreen?");

		int[] secondsLeft = {REVERT_SECONDS};
		Timer countdown = new Timer(1000, e ->
		{
			if (--secondsLeft[0] <= 0)
			{
				dialog.setVisible(false);
			}
			else
			{
				pane.setMessage(revertMessage(secondsLeft[0]));
			}
		});
		countdown.start();
		dialog.setVisible(true);
		countdown.stop();
		dialog.dispose();

		if (KEEP_OPTION.equals(pane.getValue()))
		{
			configManager.setConfiguration(FullscreenConfig.GROUP, "exclusiveConfirmed", true);
		}
		else if (isActivated && fullscreenMode == Mode.EXCLUSIVE)
		{
			disableFullscreen();
		}
	}

	private static String revertMessage(int seconds)
	{
		return "Exclusive fullscreen will be turned off in " + seconds + " seconds unless you keep it.";
	}

	private void disableFullscreen()
	{
		if (!isActivated || transitioning)
		{
			return;
		}

		if (fullscreenMode == Mode.EXCLUSIVE)
		{
			disableExclusive();
		}
		else
		{
			disableBorderless();
		}
		setActivated(false);
	}

	private void setActivated(boolean activated)
	{
		isActivated = activated;
		clientToolbar.removeNavigation(activated ? navButtonEnable : navButtonDisable);
		if (running)
		{
			clientToolbar.addNavigation(activated ? navButtonDisable : navButtonEnable);
		}
	}

	private boolean canEnable()
	{
		if (configManager.getConfig(RuneLiteConfig.class).enableCustomChrome())
		{
			showError("You must disable custom chrome to in 'RuneLite' settings to enable fullscreen");
			return false;
		}

		if (config.fullscreenMode() == Mode.EXCLUSIVE && (!gc.getDevice().isFullScreenSupported() || OSType.getOSType() == OSType.MacOS))
		{
			showError("Fullscreen mode 'Exclusive' is not available on your device");
			return false;
		}

		return true;
	}

	private void showError(String message)
	{
		JOptionPane.showMessageDialog(clientFrame, message,
			"Unable to toggle fullscreen mode",
			JOptionPane.ERROR_MESSAGE);
		log.info(message);
	}

	private void enableExclusive()
	{
		gc.getDevice().setFullScreenWindow(clientFrame);
		keepFullscreenBounds();
	}

	private void disableExclusive()
	{
		releaseFullscreenBounds();
		gc.getDevice().setFullScreenWindow(null);
	}

	private void enableBorderless()
	{
		changeWindowWithoutGpu(() ->
		{
			clientFrame.dispose();
			clientFrame.setUndecorated(true);
			// On Windows a maximized frame only covers the work area (the monitor minus the taskbar),
			// so keep the frame in the normal state and size it to the whole monitor instead.
			clientFrame.setExtendedState(Frame.NORMAL);
			clientFrame.setResizable(false);
			// pack() recreates the native window but also resizes the frame to its preferred size,
			// so the monitor bounds must be applied after it.
			clientFrame.pack();
			clientFrame.setBounds(monitorBounds());
			clientFrame.setVisible(true);
			keepFullscreenBounds();
		});
	}

	private void disableBorderless()
	{
		changeWindowWithoutGpu(() ->
		{
			releaseFullscreenBounds();
			clientFrame.dispose();
			clientFrame.setUndecorated(false);
			clientFrame.setExtendedState(Frame.NORMAL);
			// Always-on-top is left untouched so RuneLite's own setting applies, and resizable goes back
			// to what it was since RuneLite turns it off when 'Lock window size' is enabled.
			clientFrame.setResizable(prevResizable);
			clientFrame.pack();
			clientFrame.setVisible(true);

			// pack() resets the size, so apply the saved bounds after it, and the saved state last so a
			// previously maximized window is maximized by the window manager rather than just sized like it.
			clientFrame.setBounds(prevBounds);
			clientFrame.setExtendedState(prevExtState);
		});
	}

	/**
	 * Borderless recreates the native window, which the GPU plugins' OpenGL surface doesn't survive, so they are
	 * stopped first and changeWindow runs once the client reports the GPU renderer is gone.
	 */
	private void changeWindowWithoutGpu(Runnable changeWindow)
	{
		stopGpuPlugins();
		transitioning = true;
		long started = System.nanoTime();
		clientThread.invokeLater(() ->
		{
			boolean gpu = client.isGpu();
			boolean timedOut = System.nanoTime() - started >= GPU_STOP_TIMEOUT_NANOS;
			if (gpu && !timedOut)
			{
				return false;
			}

			// Once the plugin is off nothing else can turn borderless off, so it is left even with the GPU renderer
			// still running. isActivated is already false when this transition is the one leaving fullscreen.
			boolean mustLeave = !running && !isActivated;
			if (gpu && !mustLeave)
			{
				// e.g. a GPU plugin we don't know about; without this the toggle would wait, and ignore all others, forever
				SwingUtilities.invokeLater(this::gpuDidNotStop);
				return true;
			}

			SwingUtilities.invokeLater(() ->
			{
				try
				{
					changeWindow.run();
				}
				finally
				{
					// Otherwise one exception leaves the plugin ignoring every toggle, with the GPU plugins off
					finishBorderlessTransition();
				}
			});
			return true;
		});
	}

	private void gpuDidNotStop()
	{
		transitioning = false;
		restoreGpuPlugins();
		// The window was never changed, so undo the toggle
		setActivated(!isActivated);
		if (running)
		{
			showError("The GPU renderer did not stop, so fullscreen was not changed. Turn off your GPU plugins and try again.");
		}
	}

	private void finishBorderlessTransition()
	{
		transitioning = false;
		if (!running)
		{
			if (isActivated)
			{
				// The plugin was turned off while entering fullscreen; shutDown() couldn't leave it then.
				// The GPU plugins are restored at the end of that transition instead.
				disableFullscreen();
			}
			else
			{
				restoreGpuPlugins();
			}
			return;
		}

		// Triggering fullscreen via hotkey will set the listener in a state of consuming all key events until
		// the same hotkey is pressed again, which is not ideal, so we swap out the old listener for a new one.
		// There are probably better ways to handle this, but this is for the hub!
		swapOutHotkeyListener();
		restoreGpuPlugins();
		clientUI.forceFocus();
	}

	private void keepFullscreenBounds()
	{
		panelInsets = new Insets(0, 0, 0, 0);
		rejectedBounds = null;
		// The window is left alone until it gets to its fullscreen bounds. Borderless puts it there before this, but
		// exclusive mode leaves it to the system, and on Linux the window manager takes a few steps to make a window
		// fullscreen. Asking for the bounds in between makes it treat the window as a normal one and push it below
		// its panels.
		reachedFullscreenBounds = clientFrame.getBounds().equals(fullscreenBounds());
		clientFrame.addComponentListener(fullscreenBoundsKeeper);
	}

	private void releaseFullscreenBounds()
	{
		clientFrame.removeComponentListener(fullscreenBoundsKeeper);
	}

	private void restoreFullscreenBounds()
	{
		Rectangle fullscreenBounds = fullscreenBounds();
		Rectangle bounds = clientFrame.getBounds();
		int dx = bounds.x - fullscreenBounds.x;
		int dy = bounds.y - fullscreenBounds.y;
		// A small move without a resize comes from the window manager, not from RuneLite, which always resizes. Only
		// in borderless: exclusive mode is the system's own fullscreen, which covers panels anyway, and on Linux the
		// window manager moves and resizes the window in steps while entering it, which would look like a panel.
		if (fullscreenMode == Mode.BORDERLESS && (dx != 0 || dy != 0)
			&& bounds.getSize().equals(fullscreenBounds.getSize())
			&& Math.abs(dx) < fullscreenBounds.width / 4 && Math.abs(dy) < fullscreenBounds.height / 4)
		{
			// Window managers such as GNOME's keep windows out of their panels by moving them without shrinking them,
			// which pushes the far edge of the window off the monitor. Leave the panel free and fit into the rest.
			if (dx > 0)
			{
				panelInsets.left += dx;
			}
			else
			{
				panelInsets.right -= dx;
			}
			if (dy > 0)
			{
				panelInsets.top += dy;
			}
			else
			{
				panelInsets.bottom -= dy;
			}
			fullscreenBounds = fullscreenBounds();
		}

		if (bounds.equals(fullscreenBounds))
		{
			rejectedBounds = null;
			reachedFullscreenBounds = true;
			return;
		}

		if (!reachedFullscreenBounds)
		{
			return;
		}

		// If the window system answers our correction with the same bounds again, give up instead of
		// fighting it in an endless resize loop.
		if (bounds.equals(rejectedBounds))
		{
			return;
		}

		rejectedBounds = bounds;
		clientFrame.setBounds(fullscreenBounds);
	}

	private Rectangle fullscreenBounds()
	{
		Rectangle monitor = monitorBounds();
		return new Rectangle(monitor.x + panelInsets.left, monitor.y + panelInsets.top,
			monitor.width - panelInsets.left - panelInsets.right, monitor.height - panelInsets.top - panelInsets.bottom);
	}

	/**
	 * Read from the device every time rather than once, since the user can change the monitor's resolution or
	 * scaling while fullscreen, and the window should follow.
	 */
	private Rectangle monitorBounds()
	{
		return gc.getDevice().getDefaultConfiguration().getBounds();
	}

	private void restoreGpuPlugins()
	{
		for (Plugin p : getPluginsUsingGpu())
		{
			boolean isEnabled = pluginManager.isPluginEnabled(p);
			if (isEnabled)
			{
				try
				{
					pluginManager.startPlugin(p);
				}
				catch (PluginInstantiationException ex)
				{
					log.error("Error starting plugin", ex);
				}
			}
		}
	}

	private void stopGpuPlugins()
	{
		for (Plugin p : getPluginsUsingGpu())
		{
			try
			{
				pluginManager.stopPlugin(p);
			}
			catch (PluginInstantiationException ex)
			{
				log.error("Error stopping plugin", ex);
			}
		}
	}

	private List<Plugin> getPluginsUsingGpu()
	{
		GpuPlugin gpuPlugin = getCoreGpuPlugin();
		List<Plugin> conflicts = pluginManager.conflictsForPlugin(gpuPlugin);

		List<Plugin> pluginsUsingGpu = new ArrayList<>();
		pluginsUsingGpu.add(gpuPlugin);
		pluginsUsingGpu.addAll(conflicts);
		return pluginsUsingGpu;
	}

	private GpuPlugin getCoreGpuPlugin()
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (p instanceof GpuPlugin)
			{
				return (GpuPlugin) p;
			}
		}
		return null;
	}

	private static Frame getClientFrame()
	{
		Frame clientFrame = null;
		Frame[] frames = Frame.getFrames();
		for (Frame frame : frames)
		{
			if (frame instanceof ContainableFrame)
			{
				clientFrame = frame;
				break;
			}
		}
		return clientFrame;
	}

	@Override
	protected void shutDown()
	{
		running = false;
		// If a borderless transition is in progress this does nothing, and finishBorderlessTransition() exits
		// fullscreen once it is done.
		disableFullscreen();

		clientToolbar.removeNavigation(navButtonEnable);
		clientToolbar.removeNavigation(navButtonDisable);
		keyManager.unregisterKeyListener(hotkeyListener);
	}

	private void swapOutHotkeyListener()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		hotkeyListener = createHotkeyListener();
		keyManager.registerKeyListener(hotkeyListener);
	}

	private HotkeyListener createHotkeyListener()
	{
		return new HotkeyListener(() -> config.fullscreenHotKey())
		{
			@Override
			public void hotkeyPressed()
			{
				if (!isActivated)
				{
					enableFullscreen();
				}
				else
				{
					disableFullscreen();
				}
			}
		};
	}

	@Provides
	FullscreenConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FullscreenConfig.class);
	}
}
