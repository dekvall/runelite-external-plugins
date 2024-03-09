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
import java.awt.Rectangle;
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
	private GraphicsConfiguration gc;
	private Mode fullscreenMode;
	private boolean isActivated;

	private HotkeyListener hotkeyListener = createHotkeyListener();

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
	}

	private void disableExclusive()
	{
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
				clientFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
				clientFrame.setAlwaysOnTop(true);
				clientFrame.setResizable(false);
				clientFrame.setSize(gc.getBounds().getSize());
				clientFrame.setLocation(gc.getBounds().getLocation());
				clientFrame.pack();
				clientFrame.setVisible(true);

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
				clientFrame.dispose();
				clientFrame.setUndecorated(false);
				clientFrame.setExtendedState(prevExtState);
				clientFrame.setAlwaysOnTop(false);
				clientFrame.setResizable(true);
				clientFrame.pack();
				clientFrame.setVisible(true);

				clientFrame.setBounds(prevBounds);
				clientFrame.setLocation(prevBounds.getLocation());

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
