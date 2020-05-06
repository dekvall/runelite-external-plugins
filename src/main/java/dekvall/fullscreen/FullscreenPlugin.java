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

import java.awt.Frame;
import java.awt.GraphicsDevice;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;

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
	private ConfigManager configManager;

	private GraphicsDevice gd;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Fullscreen started!");
		gd = clientUI.getGraphicsConfiguration().getDevice();
		Frame tempParent = Frame.getFrames()[0];

		if (configManager.getConfig(RuneLiteConfig.class).enableCustomChrome())
		{
			log.info("You must disable custom chrome to enable fullscreen");
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(tempParent,
				"You must disable custom chrome to enable fullscreen",
				"Could not enter fullscreen mode",
				JOptionPane.ERROR_MESSAGE));
			return;
		}

		if (!gd.isFullScreenSupported())
		{
			log.info("Fullscreen is not supported on your device, sorry :(");
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(tempParent,
				"Fullscreen is not supported on your device, sorry :(",
				"Could not enter fullscreen mode",
				JOptionPane.ERROR_MESSAGE));
			return;
		}

		//Dirty hack
		Frame[] frames = Frame.getFrames();
		for (Frame frame : frames)
		{
			if (frame instanceof ContainableFrame)
			{
				gd.setFullScreenWindow(frame);
				return;
			}
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		gd.setFullScreenWindow(null);
		log.info("Fullscreen stopped!");
	}
}
