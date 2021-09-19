package dev.dkvl.womutils.beans;

import java.util.Date;
import lombok.Data;

@Data
public class Participant
{
	long exp;
	int id;
	String username;
	String displayName;
	PlayerType type;
	PlayerBuild build;
	String country;
	boolean flagged;
	double ehp;
	double ehb;
	double ttm;
	double tt200m;
	Date lastImportedAt;
	Date lastChangedAt;
	Date registeredAt;
	Date updatedAt;
	String teamName;
	CompetitionProgress progress;
	CompetitionHistoryEntry[] history;
}
