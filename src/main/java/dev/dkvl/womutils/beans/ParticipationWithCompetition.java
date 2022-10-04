package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class ParticipationWithCompetition
{
    int playerId;
    int competitionId;
    String teamName;
    String createdAt;
    String updatedAt;
    Competition competition;
}
