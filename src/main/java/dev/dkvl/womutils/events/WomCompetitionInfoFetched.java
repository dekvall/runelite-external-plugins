package dev.dkvl.womutils.events;

import dev.dkvl.womutils.beans.CompetitionInfo;
import lombok.Value;

@Value
public class WomCompetitionInfoFetched
{
	CompetitionInfo comp;
}
