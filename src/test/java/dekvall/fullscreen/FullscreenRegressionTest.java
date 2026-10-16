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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FullscreenRegressionTest
{
	private FullscreenPlugin plugin;
	private Frame frame;
	private ClientUI ui;
	private KeyManager keys;
	private ClientToolbar toolbar;
	private Client client;
	private FullscreenConfig config;
	private GraphicsDevice device;
	private final List<BooleanSupplier> callbacks = new ArrayList<>();
	private final Rectangle monitor = new Rectangle(-1920, 120, 1920, 1080);
	private final Rectangle original = new Rectangle(100, 150, 900, 700);

	@Before
	public void setUp() throws Exception
	{
		plugin = new FullscreenPlugin();
		frame = mock(Frame.class);
		ui = mock(ClientUI.class);
		keys = mock(KeyManager.class);
		toolbar = mock(ClientToolbar.class);
		client = mock(Client.class);
		config = mock(FullscreenConfig.class);
		GraphicsConfiguration graphics = mock(GraphicsConfiguration.class);
		device = mock(GraphicsDevice.class);
		when(graphics.getDevice()).thenReturn(device);
		when(graphics.getBounds()).thenReturn(monitor);
		when(ui.getGraphicsConfiguration()).thenReturn(graphics);
		when(frame.getBounds()).thenReturn(original);
		when(frame.getExtendedState()).thenReturn(Frame.MAXIMIZED_BOTH);
		when(frame.isAlwaysOnTop()).thenReturn(true);
		when(frame.isResizable()).thenReturn(false);
		when(config.fullscreenMode()).thenReturn(Mode.BORDERLESS);
		ConfigManager configs = mock(ConfigManager.class);
		when(configs.getConfig(RuneLiteConfig.class)).thenReturn(mock(RuneLiteConfig.class));
		PluginManager plugins = mock(PluginManager.class);
		when(plugins.getPlugins()).thenReturn(java.util.Collections.singletonList(mock(GpuPlugin.class)));
		ClientThread thread = mock(ClientThread.class);
		doAnswer(invocation -> { callbacks.add(invocation.getArgument(0)); return null; })
			.when(thread).invokeLater(any(BooleanSupplier.class));
		set("clientFrame", frame);
		set("clientUI", ui);
		set("keyManager", keys);
		set("clientToolbar", toolbar);
		set("client", client);
		set("config", config);
		set("configManager", configs);
		set("pluginManager", plugins);
		set("clientThread", thread);
		set("running", true);
	}

	@Test
	public void borderlessUsesFullMonitorBoundsAfterPack() throws Exception
	{
		// Model pack() resetting the size, reproducing the reported regression.
		final Rectangle[] actual = {original};
		doAnswer(i -> { actual[0] = original; return null; }).when(frame).pack();
		doAnswer(i -> { actual[0] = i.getArgument(0); return null; }).when(frame).setBounds(any(Rectangle.class));
		onEdt("enableFullscreen");
		completeTransition();
		assertEquals(monitor, actual[0]);
		verify(frame).setExtendedState(Frame.NORMAL);
		verify(frame, never()).setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	@Test
	public void exitRestoresWindowFlagsAndState() throws Exception
	{
		onEdt("enableFullscreen");
		completeTransition();
		clearInvocations(frame);
		onEdt("disableFullscreen");
		completeTransition();
		verify(frame).setUndecorated(false);
		verify(frame).setBounds(original);
		verify(frame).setAlwaysOnTop(true);
		verify(frame).setResizable(false);
		verify(frame).setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	@Test
	public void duplicateRequestsDoNotOverwriteSavedBounds() throws Exception
	{
		onEdt("enableFullscreen");
		onEdt("enableFullscreen");
		onEdt("disableFullscreen");
		assertEquals(1, callbacks.size());
		verify(frame, times(1)).getBounds();
		completeTransition();
	}

	@Test
	public void shutdownDuringEntryRestoresWindowWithoutRegisteringHotkey() throws Exception
	{
		onEdt("enableFullscreen");
		SwingUtilities.invokeAndWait(() -> plugin.shutDown());
		clearInvocations(keys, toolbar);
		completeTransition(); // Entry finishes; shutdown now schedules exit.
		assertEquals(1, callbacks.size());
		completeTransition();
		verify(frame).setUndecorated(false);
		verify(keys, never()).registerKeyListener(any());
		verify(toolbar, never()).addNavigation(any(NavigationButton.class));
		assertFalse((Boolean) get("isActivated"));
		assertFalse((Boolean) get("transitioning"));
	}

	@Test
	public void waitsForGpuBeforeRecreatingWindow() throws Exception
	{
		when(client.isGpu()).thenReturn(true);
		onEdt("enableFullscreen");
		assertFalse(callbacks.get(0).getAsBoolean());
		SwingUtilities.invokeAndWait(() -> {});
		verify(frame, never()).dispose();
		when(client.isGpu()).thenReturn(false);
		completeTransition();
		verify(frame).dispose();
	}

	@Test
	public void hotkeyDispatchesWindowChangesToEdt() throws Exception
	{
		doAnswer(i -> { assertTrue(SwingUtilities.isEventDispatchThread()); return original; })
			.when(frame).getBounds();
		((HotkeyListener) get("hotkeyListener")).hotkeyPressed();
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(1, callbacks.size());
		completeTransition();
	}

	@Test
	public void exclusiveExitUsesOriginalGraphicsDevice() throws Exception
	{
		when(config.fullscreenMode()).thenReturn(Mode.EXCLUSIVE);
		when(device.isFullScreenSupported()).thenReturn(true);
		onEdt("enableFullscreen");
		when(ui.getGraphicsConfiguration()).thenReturn(mock(GraphicsConfiguration.class));
		onEdt("disableFullscreen");
		verify(device).setFullScreenWindow(frame);
		verify(device).setFullScreenWindow(null);
	}

	private void completeTransition() throws Exception
	{
		assertTrue(callbacks.remove(0).getAsBoolean());
		SwingUtilities.invokeAndWait(() -> {});
	}

	private void onEdt(String name) throws Exception
	{
		Method method = FullscreenPlugin.class.getDeclaredMethod(name);
		method.setAccessible(true);
		SwingUtilities.invokeAndWait(() -> {
			try { method.invoke(plugin); }
			catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
		});
	}

	private void set(String name, Object value) throws Exception
	{
		Field field = FullscreenPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private Object get(String name) throws Exception
	{
		Field field = FullscreenPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(plugin);
	}
}
