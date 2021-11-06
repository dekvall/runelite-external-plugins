package dev.dkvl.womutils.ui;

import dev.dkvl.womutils.WomUtilsPlugin;
import dev.dkvl.womutils.beans.Competition;
import dev.dkvl.womutils.beans.Metric;
import dev.dkvl.womutils.beans.Participant;
import dev.dkvl.womutils.beans.RankedParticipant;
import dev.dkvl.womutils.util.Utils;
import java.awt.Color;
import java.text.DecimalFormat;
import java.time.Duration;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;
import net.runelite.http.api.hiscore.HiscoreSkillType;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class CompetitionInfobox extends InfoBox
{
	final Competition comp;
	final WomUtilsPlugin plugin;
	final Participant player;
	final int rank;

	private static final Color ACTIVE_COLOR = new Color(0x51f542);

	public CompetitionInfobox(Competition comp, RankedParticipant rp, WomUtilsPlugin plugin)
	{
		super(comp.getMetric().loadImage(), plugin);
		this.comp = comp;
		this.player = rp != null ? rp.getParticipant() : null;
		this.rank = rp != null ? rp.getCompetitionRank() : -1;
		this.plugin = plugin;

		this.getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_INFOBOX, WomUtilsPlugin.SHOW_ALL_COMPETITIONS, "Wise Old Man"));
		this.getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_INFOBOX, WomUtilsPlugin.HIDE_COMPETITION_INFOBOX, comp.getTitle()));
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
				String coloredRank = ColorUtil.wrapWithColorTag(Utils.ordinalOf(rank), Color.GREEN);
				sb.append("Ranked: ").append(coloredRank);

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
				String coloredProgress = ColorUtil.wrapWithColorTag(formattedProgress, Color.GREEN);
				sb.append(" (Gained ").append(coloredProgress);

				switch (comp.getMetric())
				{
					case EHB:
					case EHP:
						sb.append(" hours");
						break;
					default:
						sb.append(getUnitForType(comp.getMetric().getType()));
				}
				sb.append(")");
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
		return comp.isActive() ? ACTIVE_COLOR : Color.YELLOW;
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
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "H'h'm'm'");
		}
		else
		{
			return DurationFormatUtils.formatDuration(timeLeft.toMillis(), "mm:ss");
		}
	}

	@Override
	public boolean render()
	{
		return shouldShow() && !isHidden();
	}

	@Override
	public boolean cull()
	{
		return comp.hasEnded();
	}

	public boolean shouldShow()
	{
		return plugin.isShowTimerOngoing() && comp.isActive()
			|| plugin.isShowTimerUpcoming() && !comp.hasStarted()
					&& comp.durationLeft().toDays() <= plugin.getUpcomingInfoboxesMaxDays();
	}

	public boolean isHidden()
	{
		return plugin.getHiddenCompetitions().contains(comp.getId());
	}

	public int getLinkedCompetitionId()
	{
		return comp.getId();
	}
}
