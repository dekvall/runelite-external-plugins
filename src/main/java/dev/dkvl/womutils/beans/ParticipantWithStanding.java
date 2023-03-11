package dev.dkvl.womutils.beans;

import java.util.Date;
import lombok.Data;

@Data
public class ParticipantWithStanding
{
	int playerId;
	int competitionId;
	String teamName;
	Date createdAt;
	Date updatedAt;
	CompetitionProgress progress;
	int rank;
	Competition competition;
}
