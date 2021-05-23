package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class PlayerInfo
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
    String lastImportedAt;
    String lastChangedAt;
    String registeredAt;
    String updatedAt;
    int combatLevel;
    Snapshot latestSnapshot;
}