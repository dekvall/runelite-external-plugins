package dev.dkvl.womutils.ui;

import dev.dkvl.womutils.WomUtilsPlugin;
import dev.dkvl.womutils.beans.Competition;
import dev.dkvl.womutils.beans.Metric;
import dev.dkvl.womutils.beans.Participant;
import java.awt.Color;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.util.ColorUtil;
import net.runelite.http.api.hiscore.HiscoreSkillType;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class CompetitionInfobox extends Timer
{
	final Competition comp;
	final WomUtilsPlugin plugin;
	final Participant player;

	public CompetitionInfobox(Competition comp, Participant player, WomUtilsPlugin plugin)
	{
		super(comp.durationLeft().toMillis(), ChronoUnit.MILLIS, comp.getMetric().loadImage(), plugin);
		this.comp = comp;
		this.player = player;
		this.plugin = plugin;
	}

	@Override
	public String getTooltip()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(comp.getTitle()).append("</br>")
			.append("Metric: ").append(comp.getMetric().getName()).append("</br>")
			.append(comp.getTimeStatus());
		if (player != null)
		{
			sb.append("</br>");
			double progress = player.getProgress().getGained();
			if (progress > 0)
			{
				final DecimalFormat df;
				if (comp.getMetric() == Metric.EHB || comp.getMetric() == Metric.EHP)
				{
					// These are the only ones actually in decimal
					df = new DecimalFormat("####.##");
				}
				else
				{
					df = new DecimalFormat("###,###,###");
				}
				String formattedProgress = df.format(progress);

				String colored = ColorUtil.wrapWithColorTag(formattedProgress, Color.GREEN);
				sb.append("Gained ").append(colored);

				switch (comp.getMetric())
				{
					case EHB:
					case EHP:
						sb.append(" hours");
						break;
					default:
						sb.append(getUnitForType(comp.getMetric().getType()));
				}
			}
		}
		return sb.toString();
	}

	private String getUnitForType(HiscoreSkillType type)
	{
		if (type == null)
		{
			return "";
		}
		switch (type)
		{
			case SKILL:
				return " xp";
			case BOSS:
				return " kills";
			case ACTIVITY:
				return " points";
			default:
				return "";
		}
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

		if (timeLeft.toDays() > 9)
		{
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "d'd'");
		}
		else if (timeLeft.toDays() > 0)
		{
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "d'd'H'h'");
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
