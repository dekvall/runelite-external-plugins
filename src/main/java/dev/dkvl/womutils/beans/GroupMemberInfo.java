package dev.dkvl.womutils.beans;

import lombok.Value;

@Value
public class GroupMemberInfo
{
    int id;
    String username;
    String displayName;
    String type;
    String build;
    String country;
    boolean flagged;
    long exp;
    double ehp;
    double ehb;
    double ttm;
    double tt200m;
    String registeredAt;
    String updatedAt;
    String lastChangedAt;
    String lastImportedAt;
}