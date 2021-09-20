package dev.dkvl.womutils.beans;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.Data;
import org.apache.commons.lang3.time.DurationFormatUtils;

@Data
public class Competition
{
	int id;
	String title;
	Metric metric;
	int score;
	Date startsAt;
	Date endsAt;
	CompetitionType type;
	int groupId;
	Date createdAt;
	Date updatedAt;
	String duration;
	int particiantCount;

	public boolean isActive()
	{
		return hasStarted() && !hasEnded();
	}

	public boolean hasEnded()
	{
		return endsAt.before(new Date());
	}

	public boolean hasStarted()
	{
		return startsAt.before(new Date());
	}

	public Duration durationLeft()
	{
		if (isActive())
		{
			return Duration.between(Instant.now(), endsAt.toInstant());
		}
		else if (!hasStarted())
		{
			return Duration.between(Instant.now(), startsAt.toInstant());
		}
		else
		{
			return Duration.ZERO;
		}
	}

	private String durationLeftPretty()
	{
		return DurationFormatUtils.formatDurationWords(durationLeft().toMillis(), true, true);
	}

	public String getStatus()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Competition: ").append(title).append(" ");
		if (isActive())
		{
			sb.append("ends in ").append(durationLeftPretty());
		}
		else if (!hasStarted())
		{
			sb.append("starts in ").append(durationLeftPretty());
		}

		return sb.toString();
	}

	public String getTimeStatus()
	{
		StringBuilder sb = new StringBuilder();

		if (isActive())
		{
			sb.append("Ends in ").append(durationLeftPretty());
		}
		else if (!hasStarted())
		{
			sb.append("Starts in ").append(durationLeftPretty());
		}
		else
		{
			sb.append("Ended");
		}
		return sb.toString();
	}
}
