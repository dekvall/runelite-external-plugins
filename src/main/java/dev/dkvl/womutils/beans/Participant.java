package dev.dkvl.womutils.beans;

import java.util.Date;
import lombok.Data;

@Data
public class Participant
{
	int playerId;
	int competitionId;
	String teamName;
	Date createdAt;
	Date updatedAt;
	PlayerInfo player;
	CompetitionProgress progress;
}
