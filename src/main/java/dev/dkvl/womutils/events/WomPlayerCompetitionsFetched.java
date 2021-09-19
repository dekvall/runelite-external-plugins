package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.Competition;
import lombok.Value;

@Value
public class WomPlayerCompetitionsFetched
{
	String username;
	Competition[] competitions;
}
