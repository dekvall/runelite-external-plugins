package dev.dkvl.womutils.ui;

import dev.dkvl.womutils.WomUtilsPlugin;
import dev.dkvl.womutils.beans.Competition;
import java.awt.Color;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.runelite.client.ui.overlay.infobox.Timer;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class CompetitionInfobox extends Timer
{
	final Competition comp;
	final WomUtilsPlugin plugin;

	public CompetitionInfobox(Competition comp, WomUtilsPlugin plugin)
	{
		super(comp.durationLeft().toMillis(), ChronoUnit.MILLIS, comp.getMetric().loadImage(), plugin);
		this.plugin = plugin;
		this.comp = comp;
	}

	@Override
	public String getTooltip()
	{
		return comp.getStatus();
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}

	@Override
	public String getText()
	{
		Duration timeLeft = comp.durationLeft();

		if (timeLeft.toDays() > 0)
		{
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "d'd'HH'h'");
		}
		else if (timeLeft.toHours() > 0)
		{
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "HH:mm");
		}
		else
		{
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "mm:ss");
		}
	}
}
