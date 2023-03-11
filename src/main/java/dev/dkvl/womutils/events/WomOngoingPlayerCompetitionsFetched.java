package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.ParticipantWithStanding;
import lombok.Value;

@Value
public class WomOngoingPlayerCompetitionsFetched
{
	String username;
	ParticipantWithStanding[] competitions;
}
