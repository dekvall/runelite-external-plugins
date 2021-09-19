package dev.dkvl.womutils.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompetitionInfo extends Competition
{
	double totalGained; // Needs to be double since we alse have ehp and ehb metrics
	Participant[] participants;
}
