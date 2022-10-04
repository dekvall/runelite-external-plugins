package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.ParticipationWithCompetition;
import lombok.Value;

@Value
public class WomPlayerCompetitionsFetched
{
	String username;
	ParticipationWithCompetition[] competitions;
}
