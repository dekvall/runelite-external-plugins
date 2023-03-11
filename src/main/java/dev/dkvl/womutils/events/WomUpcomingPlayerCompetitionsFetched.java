package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.ParticipantWithCompetition;
import lombok.Value;

@Value
public class WomUpcomingPlayerCompetitionsFetched
{
	String username;
	ParticipantWithCompetition[] competitions;
}
