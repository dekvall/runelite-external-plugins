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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
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
	description = "Borderless or exclusive fullscreen with safe window restoration.",
	tags = {"window", "monitor", "borderless", "exclusive"},
	internalName = "fullscreen"
)
public class FullscreenPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientUI clientUI;
	@Inject private ConfigManager configManager;
	@Inject private KeyManager keyManager;
	@Inject private FullscreenConfig config;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ClientThread clientThread;
	@Inject private PluginManager pluginManager;

	private Frame frame;
	private FullscreenWindow window;
	private FullscreenController controller;
	private HotkeyListener hotkeyListener;
	private NavigationButton button;
	private boolean enabled;
	private boolean closing;
	private boolean errorDialogOpen;
	private boolean updatingMonitorSelection;
	private boolean updatingModeSelection;
	/** Crash marker for the fullscreen change in progress (automatic startup or manual). */
	private StartupRecovery startupRecovery;
	private Timer startupTimer;
	private Timer windowReadyTimer;
	/** Profile whose Fullscreen settings this session applies; written on the profile-switch thread. */
	private volatile long sessionProfile;
	/** Only return focus to RuneLite when it was focused when the request began. */
	private boolean refocusAfterTransition;
	/** Re-enabled while a shutdown exit was still waiting; retry that exit once if it fails. */
	private boolean retryCloseOnSettle;
	/** Automatic entry reached settled fullscreen; later errors are not startup failures. */
	private boolean startupSettled;

	@Override
	protected void startUp()
	{
		onEdt(() ->
		{
			enabled = true;
			if (controller == null)
			{
				closing = false;
				initialize();
			}
			else if (closing && !controller.isTransitioning())
			{
				// A renderer timeout may have prevented safe shutdown. Keep its snapshot
				// and retry restoration before starting a new session.
				controller.close();
			}
			else if (closing)
			{
				// The pending shutdown exit may still time out. Without a retry the plugin
				// would stay enabled with no toolbar button or hotkey for the fullscreen window.
				retryCloseOnSettle = true;
			}
		});
	}

	private void initialize()
	{
		frame = findClientFrame();
		if (frame == null)
		{
			if (windowReadyTimer == null)
			{
				int[] attempts = {0};
				windowReadyTimer = new Timer(250, event ->
				{
					if (!enabled || closing)
					{
						stopWaitingForWindow();
					}
					else if (findClientFrame() != null)
					{
						stopWaitingForWindow();
						initialize();
					}
					else if (++attempts[0] >= 40)
					{
						stopWaitingForWindow();
						report(new IllegalStateException("RuneLite's main window could not be found."));
					}
				});
				windowReadyTimer.start();
			}
			return;
		}
		stopWaitingForWindow();
		closing = false;
		retryCloseOnSettle = false;
		sessionProfile = currentProfileId();
		Frame sessionFrame = frame;
		window = new FullscreenWindow(frame, () ->
		{
			GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			return MonitorSelector.select(sessionFrame.getGraphicsConfiguration(), config.monitorTarget(),
				config.preferredMonitor(), environment.getScreenDevices(), environment.getDefaultScreenDevice());
		});
		controller = new FullscreenController(window,
			new RendererSuspender(pluginManager, clientThread, client),
			this::transitionChanged, this::report);
		FullscreenController session = controller;
		hotkeyListener = createHotkeyListener(config::fullscreenHotKey, () ->
		{
			if (controller == session)
			{
				toggle();
			}
		});
		keyManager.registerKeyListener(hotkeyListener);
		refreshControls();
		if (!recoverFromInterruptedChange() && config.fullscreenOnStartup())
		{
			// Windowed in the dropdown is the previous session's live state, not a
			// preference: toggle() then uses the last fullscreen mode that was entered,
			// and the dropdown is synchronized to it once entry settles.
			attemptAutomaticStartup();
		}
		showWindowedAtStartup();
	}

	/**
	 * A fullscreen mode saved by the previous session would otherwise stay selected while
	 * the window is windowed, and re-selecting it would do nothing (no change event).
	 * Only done when automatic entry is off, so this never changes startup behavior.
	 */
	private void showWindowedAtStartup()
	{
		Mode shown = config.fullscreenMode();
		if (controller == null || controller.isActive() || controller.isTransitioning() || config.fullscreenOnStartup()
			|| shown == null || shown == Mode.WINDOWED)
		{
			return;
		}
		if (config.lastFullscreenMode() != shown)
		{
			configManager.setConfiguration("Fullscreen", "lastFullscreenMode", shown);
		}
		updatingModeSelection = true;
		try
		{
			configManager.setConfiguration("Fullscreen", "fullscreenMode", Mode.WINDOWED);
		}
		finally
		{
			updatingModeSelection = false;
		}
	}

	private void stopWaitingForWindow()
	{
		if (windowReadyTimer != null)
		{
			windowReadyTimer.stop();
			windowReadyTimer = null;
		}
	}

	StartupRecovery createStartupRecovery() throws IOException
	{
		// .runelite/plugin-data/fullscreen/startup-<profile>, through RuneLite's Filepath.
		return new StartupRecovery(getPluginDirectory().joinSegment("startup-" + currentProfileId()));
	}

	private long currentProfileId()
	{
		ConfigProfile currentProfile = configManager.getProfile();
		return currentProfile == null ? 0 : currentProfile.getId();
	}

	private void attemptAutomaticStartup()
	{
		if (!canEnter())
		{
			return;
		}
		try
		{
			startupSettled = false;
			startupRecovery = createStartupRecovery();
			StartupRecovery.Result result = startupRecovery.begin();
			if (result != StartupRecovery.Result.READY)
			{
				finishStartup(false);
				if (result == StartupRecovery.Result.RECOVERED)
				{
					// Only if the launch check raced with another client that left a marker.
					configManager.setConfiguration("Fullscreen", "fullscreenOnStartup", false);
					report(new IllegalStateException(INTERRUPTED_CHANGE + INTERRUPTED_STARTUP_OFF + INTERRUPTED_ADVICE));
				}
				else
				{
					report(new IllegalStateException("Another RuneLite client is starting fullscreen with this profile. "
						+ "This client will start windowed; you can toggle fullscreen manually."));
				}
				return;
			}
			toggle();
		}
		catch (IOException | RuntimeException ex)
		{
			report(new IllegalStateException("Automatic fullscreen could not start safely. "
				+ "You can toggle fullscreen manually. " + ex.getMessage(), ex));
		}
	}

	static final String INTERRUPTED_CHANGE = "RuneLite closed unexpectedly while changing fullscreen last time, "
		+ "so it started in a normal window.";
	static final String INTERRUPTED_STARTUP_OFF = "\nFullscreen on startup has been turned off. You can turn it back on "
		+ "in the Fullscreen settings once fullscreen works reliably.";
	static final String INTERRUPTED_ADVICE = "\nYou can still use the hotkey or the toolbar button. If this keeps "
		+ "happening, updating your graphics driver may help.";

	/**
	 * A marker left behind means RuneLite closed in the middle of a fullscreen change (for
	 * example a graphics-driver crash). Start windowed instead of repeating it at every launch.
	 */
	private boolean recoverFromInterruptedChange()
	{
		StartupRecovery.Result result;
		try
		{
			result = createStartupRecovery().check();
		}
		catch (IOException | RuntimeException ex)
		{
			log.warn("Unable to check the fullscreen recovery marker", ex);
			return false;
		}
		if (result != StartupRecovery.Result.RECOVERED)
		{
			return false;
		}
		boolean startup = config.fullscreenOnStartup();
		if (startup)
		{
			configManager.setConfiguration("Fullscreen", "fullscreenOnStartup", false);
		}
		report(new IllegalStateException(INTERRUPTED_CHANGE + (startup ? INTERRUPTED_STARTUP_OFF : "") + INTERRUPTED_ADVICE));
		return true;
	}

	/** Writes the marker before a manual change stops renderers or touches the window. */
	private void guardChange()
	{
		for (int attempt = 0; attempt < 2; attempt++)
		{
			try
			{
				StartupRecovery guard = createStartupRecovery();
				StartupRecovery.Result result = guard.begin();
				if (result == StartupRecovery.Result.READY)
				{
					startupRecovery = guard;
					// Errors in a manual change must never turn automatic startup off.
					startupSettled = true;
					return;
				}
				if (result == StartupRecovery.Result.BUSY)
				{
					// Another client using this profile holds the marker; it guards itself.
					return;
				}
				// RECOVERED: a stale marker was removed; write a fresh one.
			}
			catch (IOException | RuntimeException ex)
			{
				log.warn("Unable to write the fullscreen recovery marker; this change is unguarded", ex);
				return;
			}
		}
	}

	private void finishStartup(boolean clearMarker)
	{
		if (startupTimer != null)
		{
			startupTimer.stop();
			startupTimer = null;
		}
		StartupRecovery recovery = startupRecovery;
		startupRecovery = null;
		if (recovery != null)
		{
			try
			{
				recovery.finish(clearMarker);
			}
			catch (IOException ex)
			{
				log.warn("Unable to finish fullscreen startup recovery marker", ex);
			}
		}
	}

	static HotkeyListener createHotkeyListener(Supplier<Keybind> keybind, Runnable toggle)
	{
		HotkeyListener listener = new HotkeyListener(keybind)
		{
			@Override
			public void hotkeyPressed()
			{
				SwingUtilities.invokeLater(toggle);
			}
		};
		listener.setEnabledOnLoginScreen(true);
		return listener;
	}

	private void transitionChanged()
	{
		refreshControls();
		syncModeSelection();
		if (startupRecovery == null && controller != null && controller.isTransitioning() && enabled && !closing)
		{
			// The controller reports a change before it stops renderers or touches the window.
			guardChange();
		}
		if (startupRecovery != null && controller != null)
		{
			if (startupTimer != null)
			{
				startupTimer.stop();
			}
			if (!controller.isTransitioning() && (controller.isActive() || startupSettled))
			{
				// Leave the flushed marker armed long enough to catch delayed driver crashes
				// while renderers restart, not just the window change itself. Leaving
				// fullscreen restarts renderers too, so it is guarded the same way.
				StartupRecovery attempt = startupRecovery;
				startupSettled = true;
				startupTimer = new Timer(10000, event -> clientThread.invokeLater((Runnable) () ->
					SwingUtilities.invokeLater(() ->
					{
						if (startupRecovery == attempt && controller != null && !controller.isTransitioning())
						{
							finishStartup(true);
						}
					})));
				startupTimer.setRepeats(false);
				startupTimer.start();
			}
			else if (!controller.isTransitioning())
			{
				StartupRecovery attempt = startupRecovery;
				// Failure callbacks run immediately after the state update. Give them
				// the chance to disable automatic startup before treating this as exit.
				SwingUtilities.invokeLater(() ->
				{
					if (startupRecovery == attempt && controller != null
						&& !controller.isTransitioning() && !controller.isActive())
					{
						finishStartup(true);
					}
				});
			}
		}
		if (enabled && !closing && controller != null && !controller.isTransitioning() && refocusAfterTransition)
		{
			// Return keyboard focus to the game after window changes, but never pull
			// RuneLite in front of an application the user switched to meanwhile.
			refocusAfterTransition = false;
			clientUI.forceFocus();
		}
	}

	private void beginRequest()
	{
		refocusAfterTransition = frame != null && frame.isFocused();
	}

	private void toggle()
	{
		if (!enabled || closing || controller == null || controller.isTransitioning())
		{
			return;
		}
		if (!controller.isActive() && !canEnter())
		{
			return;
		}
		Mode mode = config.fullscreenMode();
		if (mode == Mode.WINDOWED)
		{
			mode = config.lastFullscreenMode();
			if (mode == null || mode == Mode.WINDOWED)
			{
				mode = Mode.BORDERLESS;
			}
		}
		beginRequest();
		controller.toggle(mode);
	}

	private void syncModeSelection()
	{
		if (!enabled || closing || controller == null || controller.isTransitioning())
		{
			return;
		}
		Mode actual = controller.currentMode();
		if (actual != Mode.WINDOWED && config.lastFullscreenMode() != actual)
		{
			// Remember only modes that were actually entered. A rejected selection must
			// not become the mode the hotkey and toolbar retry.
			configManager.setConfiguration("Fullscreen", "lastFullscreenMode", actual);
		}
		if (actual != null && config.fullscreenMode() != actual)
		{
			updatingModeSelection = true;
			try
			{
				configManager.setConfiguration("Fullscreen", "fullscreenMode", actual);
			}
			finally
			{
				updatingModeSelection = false;
			}
		}
	}

	enum ChromeBlock
	{
		/** Custom window chrome is off on the running window. */
		NONE,
		/** "Enable custom window chrome" is on; it can be turned off for the next start. */
		SETTING_ON,
		/** The setting is already off but RuneLite has not restarted yet. */
		RESTART_PENDING
	}

	private RuneLiteConfig runeLiteConfig()
	{
		return configManager.getConfig(RuneLiteConfig.class);
	}

	private boolean canEnter()
	{
		ChromeBlock chrome = customChromeBlock(frame, runeLiteConfig().enableCustomChrome());
		if (chrome == ChromeBlock.SETTING_ON)
		{
			report(new IllegalStateException("Disable 'Enable custom window chrome' in RuneLite settings, "
				+ "then restart RuneLite before entering fullscreen."));
			return false;
		}
		if (chrome == ChromeBlock.RESTART_PENDING)
		{
			report(new IllegalStateException(
				"Custom window chrome is still active. Restart RuneLite to finish turning it off, then enter fullscreen."));
			return false;
		}
		return true;
	}

	/**
	 * The RuneLite setting only takes effect after a restart, so check both the saved
	 * setting and the decorations actually installed on the running window.
	 */
	static ChromeBlock customChromeBlock(Frame frame, boolean configured)
	{
		if (configured)
		{
			return ChromeBlock.SETTING_ON;
		}
		if (frame instanceof JFrame)
		{
			JFrame swingFrame = (JFrame) frame;
			JRootPane root = swingFrame.getRootPane();
			// RuneLite installs its toolbar as a menu bar only for custom chrome, and FlatLaf
			// marks the root pane when it replaces the native title bar.
			boolean active = swingFrame.getJMenuBar() != null || (root != null
				&& (Boolean.TRUE.equals(root.getClientProperty("JRootPane.useWindowDecorations"))
				|| root.getWindowDecorationStyle() != JRootPane.NONE));
			if (active)
			{
				return ChromeBlock.RESTART_PENDING;
			}
		}
		return ChromeBlock.NONE;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("runelite".equals(event.getGroup()) && !"clientBounds".equals(event.getKey())
			&& !"clientMaximized".equals(event.getKey()))
		{
			// ClientUI re-applies "Always on top" for any of its settings, and "Lock window
			// size" via Frame.setResizable, which re-adds a native resize border. Both run in
			// an invokeLater from its own ConfigChanged handler; queue behind that.
			boolean resizeLock = "lockWindowSize".equals(event.getKey());
			SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() ->
			{
				if (enabled && !closing && window != null && controller != null && controller.isActive()
					&& !controller.isTransitioning())
				{
					try
					{
						window.windowSettingsChanged(resizeLock);
					}
					catch (RuntimeException | LinkageError ex)
					{
						log.warn("Unable to keep fullscreen decorations after a RuneLite window setting changed", ex);
					}
				}
			}));
			return;
		}
		if ("Fullscreen".equals(event.getGroup()) && "fullscreenMode".equals(event.getKey()) && updatingModeSelection)
		{
			return;
		}
		boolean windowKey = "fullscreenMode".equals(event.getKey()) || "monitorTarget".equals(event.getKey())
			|| "preferredMonitor".equals(event.getKey());
		if ("Fullscreen".equals(event.getGroup()) && windowKey && currentProfileId() != sessionProfile)
		{
			// ConfigManager.switchProfile posts the new profile's differing values before
			// ProfileChanged. Those are saved preferences, not requests to change the window.
			log.debug("Ignoring {} from profile switch", event.getKey());
			return;
		}
		boolean modeChanged = "fullscreenMode".equals(event.getKey());
		boolean monitorChanged = "monitorTarget".equals(event.getKey()) || "preferredMonitor".equals(event.getKey());
		if ("Fullscreen".equals(event.getGroup()) && (modeChanged || monitorChanged))
		{
			onEdt(() ->
			{
				if (enabled && !closing && controller != null && !updatingMonitorSelection)
				{
					if (monitorChanged)
					{
						beginRequest();
						controller.changeMonitor();
					}
					else
					{
						Mode requested = config.fullscreenMode();
						if (requested != Mode.WINDOWED && !controller.isActive() && !canEnter())
						{
							syncModeSelection();
							return;
						}
						beginRequest();
						controller.changeMode(requested);
					}
				}
			});
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		sessionProfile = currentProfileId();
	}

	private void refreshControls()
	{
		if (button != null)
		{
			clientToolbar.removeNavigation(button);
			button = null;
		}
		if (controller == null)
		{
			return;
		}
		if (closing && enabled && retryCloseOnSettle && !controller.isTransitioning() && controller.isActive())
		{
			retryCloseOnSettle = false;
			controller.close();
			return;
		}
		if (closing && !controller.isTransitioning() && !controller.isActive())
		{
			window.detach();
			controller = null;
			window = null;
			if (enabled)
			{
				initialize();
			}
			return;
		}
		if (!enabled || closing || controller.isTransitioning())
		{
			return;
		}
		boolean active = controller.isActive();
		FullscreenController session = controller;
		button = NavigationButton.builder()
			.tooltip((active ? "Disable fullscreen" : "Enable fullscreen") + " (right-click to choose monitor)")
			.popup(Collections.singletonMap("Choose fullscreen monitor", () -> onEdt(() -> chooseMonitor(session))))
			.icon(ImageUtil.loadImageResource(getClass(), active ? "fullscreen_off.png" : "fullscreen_on.png"))
			.onClick(() -> onEdt(() ->
			{
				if (controller == session)
				{
					toggle();
				}
			}))
			.build();
		clientToolbar.addNavigation(button);

	}

	/**
	 * Same as clicking the title bar's X: RuneLite's own close handling runs, including
	 * its exit confirmation and window-bounds save.
	 */


	private void chooseMonitor(FullscreenController session)
	{
		if (!enabled || closing || controller != session || controller.isTransitioning())
		{
			return;
		}
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		List<MonitorSelector.Choice> choices = MonitorSelector.choices(
			environment.getScreenDevices(), environment.getDefaultScreenDevice());
		MonitorSelector.Choice current = choices.stream()
			.filter(choice -> choice.matches(config.monitorTarget(), config.preferredMonitor()))
			.findFirst().orElse(choices.get(0));
		Object answer = JOptionPane.showInputDialog(frame, "Enter fullscreen on:", "Fullscreen monitor",
			JOptionPane.QUESTION_MESSAGE, null, choices.toArray(), current);
		if (answer instanceof MonitorSelector.Choice && enabled && !closing && controller == session)
		{
			MonitorSelector.Choice choice = (MonitorSelector.Choice) answer;
			updatingMonitorSelection = true;
			try
			{
				configManager.setConfiguration("Fullscreen", "preferredMonitor", choice.deviceId);
				configManager.setConfiguration("Fullscreen", "monitorTarget", choice.target);
			}
			finally
			{
				updatingMonitorSelection = false;
			}
			// The user just answered RuneLite's own dialog.
			refocusAfterTransition = true;
			controller.changeMonitor();
		}
		if (enabled && controller == session)
		{
			clientUI.forceFocus();
		}
	}

	private void report(Exception ex)
	{
		if (startupRecovery != null && !startupSettled)
		{
			// Only failures of the automatic entry itself disable it. Once fullscreen has
			// settled, the marker keeps watching for native crashes, but an unrelated error
			// (for example a manual mode change) must not turn startup entry off.
			finishStartup(true);
			configManager.setConfiguration("Fullscreen", "fullscreenOnStartup", false);
			ex = new IllegalStateException(ex.getMessage() + "\nFullscreen on startup has been turned off.", ex);
		}
		log.warn("Unable to change fullscreen mode", ex);
		Exception failure = ex;
		if (enabled && !errorDialogOpen)
		{
			errorDialogOpen = true;
			SwingUtilities.invokeLater(() ->
			{
				try
				{
					if (enabled)
					{
						JOptionPane.showMessageDialog(frame, failure.getMessage(),
							"Unable to change fullscreen mode", JOptionPane.ERROR_MESSAGE);
					}
				}
				finally
				{
					errorDialogOpen = false;
					if (enabled)
					{
						clientUI.forceFocus();
					}
				}
			});
		}
	}

	static Frame findClientFrame()
	{
		Frame result = null;
		for (Frame candidate : Frame.getFrames())
		{
			if (candidate instanceof ContainableFrame && candidate.isDisplayable())
			{
				if (candidate.isActive())
				{
					return candidate;
				}
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * RuneLite exits without stopping plugins. Its window-bounds save has already run
	 * and recorded the fullscreen rectangle as the normal window; replace that with
	 * the placement fullscreen will restore, before ConfigManager flushes (priority).
	 */
	@Subscribe(priority = 100)
	public void onClientShutdown(ClientShutdown event)
	{
		Runnable cleanup = () ->
		{
			// A clean exit during the startup grace period is not an interrupted startup.
			finishStartup(true);
			FullscreenWindow.Placement placement = window == null || controller == null || !controller.isActive()
				? null : window.restorePlacement();
			if (placement != null)
			{
				configManager.setConfiguration("runelite", "clientBounds", placement.clientBoundsConfig());
				if (placement.maximized)
				{
					configManager.setConfiguration("runelite", "clientMaximized", true);
				}
				else
				{
					configManager.unsetConfiguration("runelite", "clientMaximized");
				}
			}
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			cleanup.run();
		}
		else
		{
			// ClientUI posts this from the EDT; anything else is best effort.
			SwingUtilities.invokeLater(cleanup);
		}
	}

	@Override
	protected void shutDown()
	{
		onEdt(() ->
		{
			enabled = false;
			closing = true;
			retryCloseOnSettle = false;
			stopWaitingForWindow();
			finishStartup(true);
			if (hotkeyListener != null)
			{
				keyManager.unregisterKeyListener(hotkeyListener);
				hotkeyListener = null;
			}
			if (controller != null)
			{
				controller.close();
			}
			refreshControls();

		});
	}

	private static void onEdt(Runnable action)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			action.run();
		}
		else
		{
			SwingUtilities.invokeLater(action);
		}
	}

	@Provides
	FullscreenConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(FullscreenConfig.class);
	}
}
