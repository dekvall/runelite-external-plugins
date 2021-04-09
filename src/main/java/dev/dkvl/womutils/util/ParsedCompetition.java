package dev.dkvl.womutils.util;

import dev.dkvl.womutils.beans.Competition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Getter
public class ParsedCompetition
{
	private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private String name;
	private Date startDate;
	private Date endDate;


	public static ParsedCompetition of(Competition comp)
	{
		Date start = null;
		Date end = null;
		try
		{
			start = format.parse(comp.getStartsAt());
			end = format.parse(comp.getEndsAt());
		}
		catch (ParseException e)
		{
			log.error("Could not parse date", e);
		}

		log.info("{}", start);

		return new ParsedCompetition(comp.getTitle(), start, end);
	}
}
