package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class RankedParticipant
{
	Participant participant;
	int competitionRank;
}
