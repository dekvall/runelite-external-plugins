package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class ParticipantWithCompetition
{
    int playerId;
    int competitionId;
    String teamName;
    String createdAt;
    String updatedAt;
    Competition competition;
}
