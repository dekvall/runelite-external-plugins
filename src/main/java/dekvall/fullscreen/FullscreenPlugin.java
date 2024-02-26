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
import javax.inject.Inject;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;
import net.runelite.client.util.OSType;

@Slf4j
@PluginDescriptor(
	name = "Fullscreen",
	description = "Requires custom chrome being off.",
	enabledByDefault = false
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
	private FullscreenConfig config;
	private Frame frame;
	private Frame clientFrame;
	private int prevExtState;
	private Rectangle prevBounds;
	private GraphicsConfiguration gc;
	private Mode activatedMode;
	private boolean isActivated;

	@Override
	protected void startUp() throws Exception
	{
		gc = clientUI.getGraphicsConfiguration();
		frame = Frame.getFrames()[0];

		clientFrame = getClientFrame();
		if (clientFrame == null)
		{
			return;
		}

		enableFullscreen();
	}

	private void enableFullscreen()
	{
		if (!canEnable())
		{
			return;
		}

		prevExtState = clientFrame.getExtendedState();
		prevBounds = clientFrame.getBounds();
		activatedMode = config.FullscreenMode();

		if (activatedMode == Mode.EXCLUSIVE)
		{
			enableExclusive();
		}
		else
		{
			enableBorderless();
		}

		isActivated = true;
	}

	private void disableFullscreen()
	{
		if (isActivated && client.isGpu() && activatedMode == Mode.BORDERLESS)
		{
			return;
		}

		if (activatedMode == Mode.EXCLUSIVE)
		{
			disableExclusive();
		}
		else
		{
			disableBorderless();
		}
		isActivated = false;
	}

	private boolean canEnable()
	{
		if (configManager.getConfig(RuneLiteConfig.class).enableCustomChrome())
		{
			showError("You must disable custom chrome to enable fullscreen");
			return false;
		}

		if (config.FullscreenMode() == Mode.EXCLUSIVE && (!gc.getDevice().isFullScreenSupported() || OSType.getOSType() == OSType.MacOS))
		{
			showError("Fullscreen exclusive mode is not available on your device");
			return false;
		}

		if (isActivated && activatedMode == Mode.BORDERLESS || client.isGpu() && config.FullscreenMode() == Mode.BORDERLESS)
		{
			showError("GPU plugins must be disabled when toggling borderless fullscreen, ex. 117HD, GPU or Region Locker");
			return false;
		}
		return true;
	}

	private void showError(String message)
	{
		JOptionPane.showMessageDialog(frame, message,
			"Could not enter fullscreen mode",
			JOptionPane.ERROR_MESSAGE);
		log.info(message);
	}

	private void enableExclusive()
	{
		clientFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
		gc.getDevice().setFullScreenWindow(clientFrame);
	}

	private void disableExclusive()
	{
		gc.getDevice().setFullScreenWindow(null);
		clientFrame.setExtendedState(prevExtState);
	}

	private void enableBorderless()
	{
		if (client.isGpu())
		{
			return;
		}

		if (clientFrame.isDisplayable())
		{
			clientFrame.dispose();
		}

		clientFrame.setUndecorated(true);
		clientFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
		clientFrame.setAlwaysOnTop(true);
		clientFrame.setResizable(false);
		clientFrame.setSize(gc.getBounds().getSize());
		clientFrame.setLocation(gc.getBounds().getLocation());
		clientFrame.pack();
		clientFrame.setVisible(true);
		clientUI.requestFocus();
	}

	private void disableBorderless()
	{
		if (client.isGpu())
		{
			return;
		}

		clientFrame.dispose();
		clientFrame.setExtendedState(prevExtState);
		clientFrame.setUndecorated(false);
		clientFrame.setAlwaysOnTop(false);
		clientFrame.setResizable(true);
		clientFrame.pack();
		clientFrame.setVisible(true);
		clientFrame.setBounds(prevBounds);
		clientFrame.setLocation(prevBounds.getLocation());

		clientUI.requestFocus();
	}

	private Frame getClientFrame()
	{
		Frame clientFrame = null;
		// Dirty hack
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
	protected void shutDown() throws Exception
	{
		disableFullscreen();
	}

	@Provides
	FullscreenConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FullscreenConfig.class);
	}
}
