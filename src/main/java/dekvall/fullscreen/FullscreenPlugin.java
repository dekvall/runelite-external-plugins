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
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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
	private Rectangle rejectedBounds;
	// Parts of the monitor the window manager won't let the window cover, such as GNOME's top bar
	private Insets panelInsets;
	// Whether the window has been where it should be yet, i.e. whether to correct it
	private boolean reachedFullscreenBounds;
	private GraphicsConfiguration gc;
	private Mode fullscreenMode;
	private boolean isActivated;

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

		keyManager.registerKeyListener(hotkeyListener);
		clientToolbar.addNavigation(navButtonEnable);
		gc = clientUI.getGraphicsConfiguration();
	}

	private void enableFullscreen()
	{
		if (!canEnable())
		{
			return;
		}

		gc = clientUI.getGraphicsConfiguration();
		prevExtState = clientFrame.getExtendedState();
		prevBounds = clientFrame.getBounds();
		fullscreenMode = config.fullscreenMode();

		if (fullscreenMode == Mode.EXCLUSIVE)
		{
			enableExclusive();
		}
		else
		{
			enableBorderless();
		}

		isActivated = true;
		clientToolbar.removeNavigation(navButtonEnable);
		clientToolbar.addNavigation(navButtonDisable);
	}

	private void disableFullscreen()
	{
		gc = clientUI.getGraphicsConfiguration();
		if (fullscreenMode == Mode.EXCLUSIVE)
		{
			disableExclusive();
		}
		else
		{
			disableBorderless();
		}
		isActivated = false;
		clientToolbar.removeNavigation(navButtonDisable);
		clientToolbar.addNavigation(navButtonEnable);
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
		stopGpuPlugins();
		clientThread.invokeLater(() ->
		{
			if (client.isGpu())
			{
				return false;
			}

			SwingUtilities.invokeLater(() ->
			{
				clientFrame.dispose();
				clientFrame.setUndecorated(true);
				// On Windows a maximized frame only covers the work area (the monitor minus the taskbar),
				// so keep the frame in the normal state and size it to the whole monitor instead.
				clientFrame.setExtendedState(Frame.NORMAL);
				clientFrame.setAlwaysOnTop(true);
				clientFrame.setResizable(false);
				// pack() recreates the native window but also resizes the frame to its preferred size,
				// so the monitor bounds must be applied after it.
				clientFrame.pack();
				clientFrame.setBounds(monitorBounds());
				clientFrame.setVisible(true);
				keepFullscreenBounds();

				// Triggering fullscreen via hotkey will set the listener in a state of consuming all key events until
				// the same hotkey is pressed again, which is not ideal, so we swap out the old listener for a new one.
				// There are probably better ways to handle this, but this is for the hub!
				swapOutHotkeyListener();
				restoreGpuPlugins();
				clientUI.forceFocus();
			});

			return true;
		});
	}

	private void disableBorderless()
	{
		stopGpuPlugins();
		clientThread.invokeLater(() ->
		{
			if (client.isGpu())
			{
				return false;
			}

			SwingUtilities.invokeLater(() ->
			{
				releaseFullscreenBounds();
				clientFrame.dispose();
				clientFrame.setUndecorated(false);
				clientFrame.setExtendedState(Frame.NORMAL);
				clientFrame.setAlwaysOnTop(false);
				clientFrame.setResizable(true);
				clientFrame.pack();
				clientFrame.setVisible(true);

				// pack() resets the size, so apply the saved bounds after it, and the saved state last so a
				// previously maximized window is maximized by the window manager rather than just sized like it.
				clientFrame.setBounds(prevBounds);
				clientFrame.setExtendedState(prevExtState);

				// Triggering fullscreen via hotkey will set the listener in a state of consuming all key events until
				// the same hotkey is pressed again, which is not ideal, so we swap out the old listener for a new one.
				// There are probably better ways to handle this, but this is for the hub!
				swapOutHotkeyListener();
				restoreGpuPlugins();
				clientUI.forceFocus();
			});

			return true;
		});
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
		if (isActivated)
		{
			disableFullscreen();
			isActivated = false;
		}

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
