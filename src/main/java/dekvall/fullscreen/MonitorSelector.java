package dekvall.fullscreen;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

final class MonitorSelector
{
	static GraphicsConfiguration select(GraphicsConfiguration current, MonitorTarget target,
		String preferredId, GraphicsDevice[] devices, GraphicsDevice primary)
	{
		if (target == MonitorTarget.PRIMARY && primary != null)
		{
			return primary.getDefaultConfiguration();
		}
		if (target == MonitorTarget.SELECTED)
		{
			for (GraphicsDevice device : devices)
			{
				if (device.getIDstring().equals(preferredId))
				{
					return device.getDefaultConfiguration();
				}
			}
		}
		if (target == MonitorTarget.SECONDARY && primary != null)
		{
			for (GraphicsDevice device : devices)
			{
				if (!device.getIDstring().equals(primary.getIDstring()))
				{
					return device.getDefaultConfiguration();
				}
			}
		}
		return current;
	}

	static List<Choice> choices(GraphicsDevice[] devices, GraphicsDevice primary)
	{
		List<Choice> choices = new ArrayList<>();
		choices.add(new Choice(MonitorTarget.CURRENT, "", "Current monitor"));
		choices.add(new Choice(MonitorTarget.PRIMARY, "", "Primary monitor"));
		choices.add(new Choice(MonitorTarget.SECONDARY, "", "Secondary monitor"));
		for (int index = 0; index < devices.length; index++)
		{
			GraphicsDevice device = devices[index];
			Rectangle bounds = device.getDefaultConfiguration().getBounds();
			choices.add(new Choice(MonitorTarget.SELECTED, device.getIDstring(),
				"Display " + (index + 1) + (device == primary ? " (primary)" : "")
					+ " — " + bounds.width + " × " + bounds.height + " at " + bounds.x + ", " + bounds.y));
		}
		return choices;
	}

	static final class Choice
	{
		final MonitorTarget target;
		final String deviceId;
		private final String label;

		Choice(MonitorTarget target, String deviceId, String label)
		{
			this.target = target;
			this.deviceId = deviceId;
			this.label = label;
		}

		boolean matches(MonitorTarget target, String preferredId)
		{
			return this.target == target && (target != MonitorTarget.SELECTED || deviceId.equals(preferredId));
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
